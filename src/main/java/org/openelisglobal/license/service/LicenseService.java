package org.openelisglobal.license.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.license.model.License;
import org.openelisglobal.license.model.LicenseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LicenseService {

    private static final String PUBLIC_KEY_RESOURCE = "license/public-key.pem";
    private static final String LICENSE_KEY_FILE = "license.key";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private FingerprintService fingerprintService;

    private License cached;

    public LicenseStatus validate() {
        License license = load();
        if (license == null) {
            return LicenseStatus.MISSING;
        }
        try {
            PublicKey publicKey = loadPublicKey();
            byte[] sigBytes = Base64.getDecoder().decode(license.getSignature());
            License unsigned = copyWithoutSignature(license);
            String json = new ObjectMapper().writeValueAsString(unsigned);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(json.getBytes(StandardCharsets.UTF_8));
            if (!signature.verify(sigBytes)) {
                return LicenseStatus.INVALID_SIGNATURE;
            }
        } catch (Exception e) {
            LogEvent.logError(e);
            return LicenseStatus.INVALID_SIGNATURE;
        }
        String fp = fingerprintService.computeFingerprint();
        if (!fp.equals(license.getFingerprintHash())) {
            return LicenseStatus.WRONG_MACHINE;
        }
        if (license.getExpiresAt() != null) {
            LocalDate expires = LocalDate.parse(license.getExpiresAt(), DATE_FMT);
            if (expires.isBefore(LocalDate.now())) {
                return LicenseStatus.EXPIRED;
            }
        }
        cached = license;
        return LicenseStatus.VALID;
    }

    public License getCurrentLicense() {
        if (cached == null) {
            validate();
        }
        return cached;
    }

    public boolean isFeatureEnabled(String feature) {
        License license = getCurrentLicense();
        return license != null && license.getFeatures() != null && license.getFeatures().contains(feature);
    }

    public void activate(String licenseKeyBase64) {
        try {
            File dir = new File(configDir());
            Files.createDirectories(dir.toPath());
            Files.writeString(new File(dir, LICENSE_KEY_FILE).toPath(), licenseKeyBase64.trim(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Failed to activate license", e);
        }
        cached = null;
        validate();
    }

    public License load() {
        if (cached != null) {
            return cached;
        }
        File file = new File(new File(configDir()), LICENSE_KEY_FILE);
        if (!file.exists()) {
            return null;
        }
        try {
            String key = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
            if (key.isEmpty()) {
                return null;
            }
            byte[] decoded = Base64.getDecoder().decode(key);
            License license = new ObjectMapper().readValue(new String(decoded, StandardCharsets.UTF_8), License.class);
            cached = license;
            return license;
        } catch (Exception e) {
            LogEvent.logError(e);
            return null;
        }
    }

    private PublicKey loadPublicKey() throws Exception {
        ClassPathResource resource = new ClassPathResource(PUBLIC_KEY_RESOURCE);
        byte[] der = Base64.getDecoder().decode(pemBody(resource.getInputStream()));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static String pemBody(java.io.InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("-----")) {
                    continue;
                }
                sb.append(line.trim());
            }
        }
        return sb.toString();
    }

    private License copyWithoutSignature(License license) {
        License copy = new License();
        copy.setFingerprintHash(license.getFingerprintHash());
        copy.setIssuedTo(license.getIssuedTo());
        copy.setExpiresAt(license.getExpiresAt());
        copy.setFeatures(license.getFeatures());
        copy.setMaxBranches(license.getMaxBranches());
        copy.setIssuedAt(license.getIssuedAt());
        return copy;
    }

    private String configDir() {
        return System.getProperty("openelis.billing.config.dir", "config");
    }
}
