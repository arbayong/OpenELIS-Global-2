package org.openelisglobal.billing.gateway;

import java.math.BigDecimal;

public interface PaymentGatewayDriver {

    String initialize(Integer invoiceId, BigDecimal amount, String callbackUrl);

    boolean verify(String providerTransactionId);

    boolean refund(String providerTransactionId, BigDecimal amount);
}
