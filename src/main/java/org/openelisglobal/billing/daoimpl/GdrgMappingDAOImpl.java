package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.GdrgMappingDAO;
import org.openelisglobal.billing.valueholder.GdrgMapping;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class GdrgMappingDAOImpl extends BaseDAOImpl<GdrgMapping, Integer> implements GdrgMappingDAO {

    public GdrgMappingDAOImpl() {
        super(GdrgMapping.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GdrgMapping> getActiveByLocalCode(String localCode, Date onDate, Integer versionId) {
        if (localCode == null || localCode.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "from GdrgMapping m where m.localCode = :localCode and m.isActive = 'Y'";
        if (versionId != null) {
            sql += " and m.versionId = :versionId";
        }
        if (onDate != null) {
            sql += " and (m.effectiveFrom is null or m.effectiveFrom <= :onDate)"
                    + " and (m.effectiveTo is null or m.effectiveTo >= :onDate)";
        }
        try {
            TypedQuery<GdrgMapping> query = entityManager.createQuery(sql, GdrgMapping.class);
            query.setParameter("localCode", localCode);
            if (versionId != null) {
                query.setParameter("versionId", versionId);
            }
            if (onDate != null) {
                query.setParameter("onDate", onDate);
            }
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in GdrgMapping getActiveByLocalCode()", e);
        }
    }
}
