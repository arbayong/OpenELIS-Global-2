package org.openelisglobal.billing.gateway;

import java.math.BigDecimal;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;

@Component
public class PaystackDriver implements PaymentGatewayDriver {

    @Override
    public String initialize(Integer invoiceId, BigDecimal amount, String callbackUrl) {
        LogEvent.logWarn(PaystackDriver.class.getName(), "initialize", "Paystack real HTTP wiring is pending");
        return null;
    }

    @Override
    public boolean verify(String providerTransactionId) {
        LogEvent.logWarn(PaystackDriver.class.getName(), "verify", "Paystack real HTTP wiring is pending");
        return false;
    }

    @Override
    public boolean refund(String providerTransactionId, BigDecimal amount) {
        LogEvent.logWarn(PaystackDriver.class.getName(), "refund", "Paystack real HTTP wiring is pending");
        return false;
    }
}
