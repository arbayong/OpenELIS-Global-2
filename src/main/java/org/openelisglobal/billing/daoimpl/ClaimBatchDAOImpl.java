package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.ClaimBatchDAO;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ClaimBatchDAOImpl extends BaseDAOImpl<ClaimBatch, Integer> implements ClaimBatchDAO {

    public ClaimBatchDAOImpl() {
        super(ClaimBatch.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimBatch> getByOrganizationIdAndStatus(Integer organizationId, String status) {
        if (organizationId == null || status == null || status.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "from ClaimBatch c where c.organizationId = :organizationId and c.status = :status";
        try {
            TypedQuery<ClaimBatch> query = entityManager.createQuery(sql, ClaimBatch.class);
            query.setParameter("organizationId", organizationId);
            query.setParameter("status", status);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ClaimBatch getByOrganizationIdAndStatus()", e);
        }
    }
}
