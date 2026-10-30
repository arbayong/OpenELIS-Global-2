package org.openelisglobal.billing.gateway;

import java.math.BigDecimal;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;

@Component
public class MtnMomoDriver implements PaymentGatewayDriver {

    @Override
    public String initialize(Integer invoiceId, BigDecimal amount, String callbackUrl) {
        LogEvent.logWarn(MtnMomoDriver.class.getName(), "initialize", "MtnMomo real HTTP wiring is pending");
        return null;
    }

    @Override
    public boolean verify(String providerTransactionId) {
        LogEvent.logWarn(MtnMomoDriver.class.getName(), "verify", "MtnMomo real HTTP wiring is pending");
        return false;
    }

    @Override
    public boolean refund(String providerTransactionId, BigDecimal amount) {
        LogEvent.logWarn(MtnMomoDriver.class.getName(), "refund", "MtnMomo real HTTP wiring is pending");
        return false;
    }
}
