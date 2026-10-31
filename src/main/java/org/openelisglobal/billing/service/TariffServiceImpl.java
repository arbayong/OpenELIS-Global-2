package org.openelisglobal.billing.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.billing.dao.BillingTariffDAO;
import org.openelisglobal.billing.valueholder.BillingTariff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TariffServiceImpl implements TariffService {

    @Autowired
    private BillingTariffDAO billingTariffDAO;

    @Autowired
    private BillingConfigService billingConfigService;

    /**
     * Resolve the unit price for a test.
     *
     * <p>
     * With {@code billing.tariff.lookup} disabled the lookup is skipped and the
     * line bills at zero, which is the configured behaviour for a site that
     * prices outside this module rather than an empty tariff meaning "free".
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getUnitPrice(String localCode, String payerType) {
        if (!billingConfigService.isTariffLookupEnabled()) {
            return null;
        }
        List<BillingTariff> tariffs = billingTariffDAO.getActiveByLocalCodeAndPayerType(localCode, payerType);
        if (tariffs == null || tariffs.isEmpty()) {
            List<BillingTariff> generalTariffs = billingTariffDAO.getActiveByLocalCode(localCode);
            if (generalTariffs != null && !generalTariffs.isEmpty()) {
                for (BillingTariff tariff : generalTariffs) {
                    if (tariff.getPayerType() == null) {
                        return tariff.getPrice();
                    }
                }
            }
            return null;
        }
        return tariffs.get(0).getPrice();
    }
}
