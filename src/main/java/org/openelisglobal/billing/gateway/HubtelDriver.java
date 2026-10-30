package org.openelisglobal.billing.gateway;

import java.math.BigDecimal;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;

@Component
public class HubtelDriver implements PaymentGatewayDriver {

    @Override
    public String initialize(Integer invoiceId, BigDecimal amount, String callbackUrl) {
        LogEvent.logWarn(HubtelDriver.class.getName(), "initialize", "Hubtel real HTTP wiring is pending");
        return null;
    }

    @Override
    public boolean verify(String providerTransactionId) {
        LogEvent.logWarn(HubtelDriver.class.getName(), "verify", "Hubtel real HTTP wiring is pending");
        return false;
    }

    @Override
    public boolean refund(String providerTransactionId, BigDecimal amount) {
        LogEvent.logWarn(HubtelDriver.class.getName(), "refund", "Hubtel real HTTP wiring is pending");
        return false;
    }
}
