package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.TariffVersionDAO;
import org.openelisglobal.billing.valueholder.TariffVersion;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TariffVersionDAOImpl extends BaseDAOImpl<TariffVersion, Integer> implements TariffVersionDAO {

    public TariffVersionDAOImpl() {
        super(TariffVersion.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TariffVersion> getActiveVersionsCovering(Date serviceDate) {
        if (serviceDate == null) {
            return new ArrayList<>();
        }
        String sql = "from TariffVersion v where v.isActive = 'Y'"
                + " and (v.effectiveFrom is null or v.effectiveFrom <= :serviceDate)"
                + " and (v.effectiveTo is null or v.effectiveTo >= :serviceDate)";
        try {
            TypedQuery<TariffVersion> query = entityManager.createQuery(sql, TariffVersion.class);
            query.setParameter("serviceDate", serviceDate);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TariffVersion getActiveVersionsCovering()", e);
        }
    }
}
