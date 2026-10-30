package org.openelisglobal.billing.gateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.openelisglobal.billing.dao.BranchGatewayConfigDAO;
import org.openelisglobal.billing.valueholder.BranchGatewayConfig;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GatewayConfigServiceImpl implements GatewayConfigService {

    private static final String MASTER_KEY_PROPERTY = "aegis.gateway.masterKey";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    @Autowired
    private BranchGatewayConfigDAO gatewayConfigDAO;

    @Override
    public BranchGatewayConfig getConfig(Integer organizationId, GatewayDriver driver) {
        if (organizationId == null || driver == null) {
            return null;
        }
        try {
            List<BranchGatewayConfig> configs = gatewayConfigDAO.getEnabledByOrganizationId(organizationId);
            if (configs == null) {
                return null;
            }
            String driverName = driver.name();
            for (BranchGatewayConfig config : configs) {
                if (driverName.equalsIgnoreCase(config.getDriver())) {
                    return config;
                }
            }
            return null;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return null;
        }
    }

    @Override
    public String encryptSecret(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        String masterKey = System.getProperty(MASTER_KEY_PROPERTY);
        if (masterKey == null || masterKey.isBlank()) {
            return plaintext;
        }
        try {
            SecretKeySpec key = masterKey(masterKey);
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            LogEvent.logError(e);
            return null;
        }
    }

    @Override
    public String decryptSecret(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        String masterKey = System.getProperty(MASTER_KEY_PROPERTY);
        if (masterKey == null || masterKey.isBlank()) {
            return ciphertext;
        }
        try {
            SecretKeySpec key = masterKey(masterKey);
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined == null || combined.length <= IV_LENGTH) {
                return ciphertext;
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] actual = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, actual, 0, actual.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(actual), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ciphertext;
        }
    }

    private SecretKeySpec masterKey(String masterKey) {
        byte[] keyBytes = Base64.getDecoder().decode(masterKey);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            try {
                keyBytes = MessageDigest.getInstance("SHA-256").digest(keyBytes);
            } catch (Exception e) {
                LogEvent.logError(e);
                keyBytes = new byte[32];
            }
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
