package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.BillingPayerDAO;
import org.openelisglobal.billing.valueholder.BillingPayer;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BillingPayerDAOImpl extends BaseDAOImpl<BillingPayer, Integer> implements BillingPayerDAO {

    public BillingPayerDAOImpl() {
        super(BillingPayer.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingPayer> getActiveByType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "from BillingPayer p where p.type = :type and p.isActive = 'Y'";
        try {
            TypedQuery<BillingPayer> query = entityManager.createQuery(sql, BillingPayer.class);
            query.setParameter("type", type);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingPayer getActiveByType()", e);
        }
    }
}
