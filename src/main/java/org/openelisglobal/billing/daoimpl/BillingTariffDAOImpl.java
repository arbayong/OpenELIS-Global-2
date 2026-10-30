package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.billing.dao.BillingTariffDAO;
import org.openelisglobal.billing.valueholder.BillingTariff;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BillingTariffDAOImpl extends BaseDAOImpl<BillingTariff, Integer> implements BillingTariffDAO {

    public BillingTariffDAOImpl() {
        super(BillingTariff.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingTariff> getActiveByLocalCode(String localCode) {
        String sql = "from BillingTariff t where t.isActive = 'Y' and t.localCode = :localCode"
                + " and (t.effectiveFrom is null or t.effectiveFrom <= CURRENT_DATE)"
                + " and (t.effectiveTo is null or t.effectiveTo >= CURRENT_DATE)";
        try {
            TypedQuery<BillingTariff> query = entityManager.createQuery(sql, BillingTariff.class);
            query.setParameter("localCode", localCode);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingTariff getActiveByLocalCode()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingTariff> getActiveByLocalCodeAndPayerType(String localCode, String payerType) {
        String sql = "from BillingTariff t where t.isActive = 'Y' and t.localCode = :localCode and t.payerType = :payerType"
                + " and (t.effectiveFrom is null or t.effectiveFrom <= CURRENT_DATE)"
                + " and (t.effectiveTo is null or t.effectiveTo >= CURRENT_DATE)";
        try {
            TypedQuery<BillingTariff> query = entityManager.createQuery(sql, BillingTariff.class);
            query.setParameter("localCode", localCode);
            query.setParameter("payerType", payerType);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingTariff getActiveByLocalCodeAndPayerType()", e);
        }
    }
}
