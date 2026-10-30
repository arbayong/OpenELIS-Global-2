package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BillingInvoiceLineDAOImpl extends BaseDAOImpl<BillingInvoiceLine, Integer>
        implements BillingInvoiceLineDAO {

    public BillingInvoiceLineDAOImpl() {
        super(BillingInvoiceLine.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingInvoiceLine> getByInvoiceId(Integer invoiceId) {
        if (invoiceId == null) {
            return new ArrayList<>();
        }
        String sql = "from BillingInvoiceLine l where l.invoiceId = :invoiceId";
        try {
            TypedQuery<BillingInvoiceLine> query = entityManager.createQuery(sql, BillingInvoiceLine.class);
            query.setParameter("invoiceId", invoiceId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingInvoiceLine getByInvoiceId()", e);
        }
    }
}
