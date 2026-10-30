package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.BranchGatewayConfigDAO;
import org.openelisglobal.billing.valueholder.BranchGatewayConfig;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BranchGatewayConfigDAOImpl extends BaseDAOImpl<BranchGatewayConfig, Integer>
        implements BranchGatewayConfigDAO {

    public BranchGatewayConfigDAOImpl() {
        super(BranchGatewayConfig.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchGatewayConfig> getEnabledByOrganizationId(Integer organizationId) {
        if (organizationId == null) {
            return new ArrayList<>();
        }
        String sql = "from BranchGatewayConfig b where b.organizationId = :organizationId and b.isEnabled = true";
        try {
            TypedQuery<BranchGatewayConfig> query = entityManager.createQuery(sql, BranchGatewayConfig.class);
            query.setParameter("organizationId", organizationId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BranchGatewayConfig getEnabledByOrganizationId()", e);
        }
    }
}
