package org.openelisglobal.billing.gateway;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.openelisglobal.billing.valueholder.BranchGatewayConfig;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class GatewayConfigRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<BranchGatewayConfig> findEnabledByDriver(GatewayDriver driver) {
        if (driver == null) {
            return List.of();
        }
        TypedQuery<BranchGatewayConfig> query = entityManager.createQuery(
                "from BranchGatewayConfig b where b.driver = :driver and b.isEnabled = true",
                BranchGatewayConfig.class);
        query.setParameter("driver", driver.name());
        return query.getResultList();
    }
}
