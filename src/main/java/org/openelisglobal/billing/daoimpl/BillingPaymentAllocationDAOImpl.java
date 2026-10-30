package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.BillingPaymentAllocationDAO;
import org.openelisglobal.billing.valueholder.BillingPaymentAllocation;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BillingPaymentAllocationDAOImpl extends BaseDAOImpl<BillingPaymentAllocation, Integer>
        implements BillingPaymentAllocationDAO {

    public BillingPaymentAllocationDAOImpl() {
        super(BillingPaymentAllocation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingPaymentAllocation> getByPaymentId(Integer paymentId) {
        if (paymentId == null) {
            return new ArrayList<>();
        }
        String sql = "from BillingPaymentAllocation a where a.paymentId = :paymentId";
        try {
            TypedQuery<BillingPaymentAllocation> query = entityManager.createQuery(sql, BillingPaymentAllocation.class);
            query.setParameter("paymentId", paymentId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingPaymentAllocation getByPaymentId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingPaymentAllocation> getByInvoiceLineId(Integer invoiceLineId) {
        if (invoiceLineId == null) {
            return new ArrayList<>();
        }
        String sql = "from BillingPaymentAllocation a where a.invoiceLineId = :invoiceLineId";
        try {
            TypedQuery<BillingPaymentAllocation> query = entityManager.createQuery(sql, BillingPaymentAllocation.class);
            query.setParameter("invoiceLineId", invoiceLineId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingPaymentAllocation getByInvoiceLineId()", e);
        }
    }
}
