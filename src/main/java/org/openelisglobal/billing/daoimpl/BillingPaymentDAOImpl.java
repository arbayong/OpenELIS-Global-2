package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.BillingPaymentDAO;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BillingPaymentDAOImpl extends BaseDAOImpl<BillingPayment, Integer> implements BillingPaymentDAO {

    public BillingPaymentDAOImpl() {
        super(BillingPayment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingPayment> getByOrganizationId(Integer organizationId) {
        if (organizationId == null) {
            return new ArrayList<>();
        }
        String sql = "from BillingPayment b where b.organizationId = :organizationId";
        try {
            TypedQuery<BillingPayment> query = entityManager.createQuery(sql, BillingPayment.class);
            query.setParameter("organizationId", organizationId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingPayment getByOrganizationId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingPayment> getByPatientId(String patientId) {
        if (patientId == null || patientId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "from BillingPayment b where b.patientId = :patientId";
        try {
            TypedQuery<BillingPayment> query = entityManager.createQuery(sql, BillingPayment.class);
            query.setParameter("patientId", patientId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingPayment getByPatientId()", e);
        }
    }
}
