package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.FacilityCredentialingDAO;
import org.openelisglobal.billing.valueholder.FacilityCredentialing;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FacilityCredentialingDAOImpl extends BaseDAOImpl<FacilityCredentialing, Integer>
        implements FacilityCredentialingDAO {

    public FacilityCredentialingDAOImpl() {
        super(FacilityCredentialing.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacilityCredentialing> getByOrganizationId(Integer organizationId) {
        if (organizationId == null) {
            return new ArrayList<>();
        }
        String sql = "from FacilityCredentialing f where f.organizationId = :organizationId";
        try {
            TypedQuery<FacilityCredentialing> query = entityManager.createQuery(sql, FacilityCredentialing.class);
            query.setParameter("organizationId", organizationId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in FacilityCredentialing getByOrganizationId()", e);
        }
    }
}
