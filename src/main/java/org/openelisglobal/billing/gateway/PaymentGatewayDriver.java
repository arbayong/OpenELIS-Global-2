package org.openelisglobal.billing.gateway;

import java.math.BigDecimal;

public interface PaymentGatewayDriver {

    String initialize(Integer invoiceId, BigDecimal amount, String callbackUrl);

    boolean verify(String providerTransactionId);

    boolean refund(String providerTransactionId, BigDecimal amount);

    /**
     * Parse an inbound gateway callback into a normalized result.
     *
     * <p>
     * The single-argument form is used when the gateway signature (sent as an
     * HTTP header by most vendors) is not available to the driver: the payload
     * is still parsed, but {@link GatewayCallbackResult#isValid()} returns
     * {@code false}. Use {@link #webhook(String, String)} with the signature
     * header when it is available so callback authenticity can be verified.
     * </p>
     */
    GatewayCallbackResult webhook(String payload);

    GatewayCallbackResult webhook(String payload, String signature);
}
