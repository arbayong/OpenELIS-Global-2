package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.BillingTariff;
import org.openelisglobal.common.dao.BaseDAO;

public interface BillingTariffDAO extends BaseDAO<BillingTariff, Integer> {

    List<BillingTariff> getActiveByLocalCode(String localCode);

    List<BillingTariff> getActiveByLocalCodeAndPayerType(String localCode, String payerType);
}
