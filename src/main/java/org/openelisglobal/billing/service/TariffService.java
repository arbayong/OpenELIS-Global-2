package org.openelisglobal.billing.service;

import java.math.BigDecimal;

public interface TariffService {

    BigDecimal getUnitPrice(String localCode, String payerType);
}
