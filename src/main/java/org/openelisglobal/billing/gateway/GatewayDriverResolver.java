package org.openelisglobal.billing.gateway;

import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Resolves the driver component for a configured gateway.
 *
 * <p>
 * The drivers are registered as Spring components, so a new gateway is added by
 * writing its driver class and listing it here; nothing else in the payment
 * path needs to learn about it. Returning {@code null} for an unknown or
 * disabled driver is deliberate: the caller reports "no gateway available"
 * rather than failing with a driver it cannot use.
 */
@Component
public class GatewayDriverResolver {

    @Autowired
    private PaystackDriver paystackDriver;

    @Autowired
    private HubtelDriver hubtelDriver;

    @Autowired
    private MtnMomoDriver mtnMomoDriver;

    public PaymentGatewayDriver resolve(GatewayDriver driver) {
        if (driver == null) {
            return null;
        }
        switch (driver) {
            case PAYSTACK:
                return paystackDriver;
            case HUBTEL:
                return hubtelDriver;
            case MTN_MOMO:
                return mtnMomoDriver;
            default:
                return null;
        }
    }
}
