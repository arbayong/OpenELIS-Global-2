package org.openelisglobal.billing.gateway;

import org.openelisglobal.billing.valueholder.BranchGatewayConfig;
import org.openelisglobal.billing.valueholder.GatewayDriver;

public interface GatewayConfigService {

    BranchGatewayConfig getConfig(Integer organizationId, GatewayDriver driver);

    String encryptSecret(String plaintext);

    String decryptSecret(String ciphertext);
}
