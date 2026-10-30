package org.openelisglobal.license.keygen;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openelisglobal.license.model.License;

public class KeyGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("usage: KeyGenerator <privateKeyPemFile> <fingerprintHash> <issuedTo> <expiresAt> <features>");
            System.exit(1);
        }
        License license = new License();
        license.setFingerprintHash(args[1]);
        license.setIssuedTo(args[2]);
        license.setExpiresAt(args[3].isEmpty() ? null : args[3]);
        license.setFeatures(parseFeatures(args[4]));
        license.setIssuedAt(java.time.LocalDate.now().toString());

        PrivateKey privateKey = loadPrivateKey(args[0]);
        License unsigned = new License();
        unsigned.setFingerprintHash(license.getFingerprintHash());
        unsigned.setIssuedTo(license.getIssuedTo());
        unsigned.setExpiresAt(license.getExpiresAt());
        unsigned.setFeatures(license.getFeatures());
        unsigned.setMaxBranches(license.getMaxBranches());
        unsigned.setIssuedAt(license.getIssuedAt());

        String json = new ObjectMapper().writeValueAsString(unsigned);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(json.getBytes(StandardCharsets.UTF_8));
        license.setSignature(Base64.getEncoder().encodeToString(signature.sign()));

        String keyContent = Base64.getEncoder()
                .encodeToString(new ObjectMapper().writeValueAsString(license).getBytes(StandardCharsets.UTF_8));
        System.out.println("signature=" + license.getSignature());
        System.out.println("license.key=" + keyContent);
    }

    private static List<String> parseFeatures(String featuresArg) {
        if (featuresArg == null || featuresArg.isEmpty()) {
            return List.of();
        }
        return java.util.Arrays.stream(featuresArg.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static PrivateKey loadPrivateKey(String pemFile) throws Exception {
        String pem = Files.readString(new File(pemFile).toPath(), StandardCharsets.UTF_8);
        byte[] der = Base64.getDecoder().decode(pemBody(pem));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static String pemBody(String pem) {
        StringBuilder sb = new StringBuilder();
        for (String line : pem.split("\\r?\\n")) {
            if (line.startsWith("-----")) {
                continue;
            }
            sb.append(line.trim());
        }
        return sb.toString();
    }
}
