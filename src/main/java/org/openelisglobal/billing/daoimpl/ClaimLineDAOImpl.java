package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.ClaimLineDAO;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ClaimLineDAOImpl extends BaseDAOImpl<ClaimLine, Integer> implements ClaimLineDAO {

    public ClaimLineDAOImpl() {
        super(ClaimLine.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimLine> getByClaimBatchId(Integer claimBatchId) {
        if (claimBatchId == null) {
            return new ArrayList<>();
        }
        String sql = "from ClaimLine l where l.claimBatchId = :claimBatchId";
        try {
            TypedQuery<ClaimLine> query = entityManager.createQuery(sql, ClaimLine.class);
            query.setParameter("claimBatchId", claimBatchId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ClaimLine getByClaimBatchId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimLine> getByInvoiceLineId(Integer invoiceLineId) {
        if (invoiceLineId == null) {
            return new ArrayList<>();
        }
        String sql = "from ClaimLine l where l.invoiceLineId = :invoiceLineId";
        try {
            TypedQuery<ClaimLine> query = entityManager.createQuery(sql, ClaimLine.class);
            query.setParameter("invoiceLineId", invoiceLineId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ClaimLine getByInvoiceLineId()", e);
        }
    }
}
