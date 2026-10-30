package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BillingInvoiceDAOImpl extends BaseDAOImpl<BillingInvoice, Integer> implements BillingInvoiceDAO {

    public BillingInvoiceDAOImpl() {
        super(BillingInvoice.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingInvoice> getByExternalOrderId(String externalOrderId) {
        if (externalOrderId == null || externalOrderId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "from BillingInvoice b where b.externalOrderId = :externalOrderId";
        try {
            TypedQuery<BillingInvoice> query = entityManager.createQuery(sql, BillingInvoice.class);
            query.setParameter("externalOrderId", externalOrderId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingInvoice getByExternalOrderId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingInvoice> getByPatientId(String patientId) {
        if (patientId == null || patientId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "from BillingInvoice b where b.patientId = :patientId";
        try {
            TypedQuery<BillingInvoice> query = entityManager.createQuery(sql, BillingInvoice.class);
            query.setParameter("patientId", patientId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingInvoice getByPatientId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingInvoice> getByOrganizationId(Integer organizationId) {
        if (organizationId == null) {
            return new ArrayList<>();
        }
        String sql = "from BillingInvoice b where b.organizationId = :organizationId";
        try {
            TypedQuery<BillingInvoice> query = entityManager.createQuery(sql, BillingInvoice.class);
            query.setParameter("organizationId", organizationId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingInvoice getByOrganizationId()", e);
        }
    }
}
