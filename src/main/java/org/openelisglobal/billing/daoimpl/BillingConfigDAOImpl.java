package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.BillingConfigDAO;
import org.openelisglobal.billing.valueholder.BillingConfig;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BillingConfigDAOImpl extends BaseDAOImpl<BillingConfig, Integer> implements BillingConfigDAO {

    public BillingConfigDAOImpl() {
        super(BillingConfig.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingConfig> getByConfigKey(String configKey) {
        if (configKey == null || configKey.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "from BillingConfig c where c.configKey = :configKey";
        try {
            TypedQuery<BillingConfig> query = entityManager.createQuery(sql, BillingConfig.class);
            query.setParameter("configKey", configKey);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BillingConfig getByConfigKey()", e);
        }
    }
}
