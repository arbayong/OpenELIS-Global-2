package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.DailyRevenueSummaryDAO;
import org.openelisglobal.billing.valueholder.DailyRevenueSummary;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DailyRevenueSummaryDAOImpl extends BaseDAOImpl<DailyRevenueSummary, Integer>
        implements DailyRevenueSummaryDAO {

    public DailyRevenueSummaryDAOImpl() {
        super(DailyRevenueSummary.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyRevenueSummary> getByOrganizationIdAndDate(Integer organizationId, Date businessDate) {
        if (organizationId == null || businessDate == null) {
            return new ArrayList<>();
        }
        String sql = "from DailyRevenueSummary d where d.organizationId = :organizationId and d.businessDate = :businessDate";
        try {
            TypedQuery<DailyRevenueSummary> query = entityManager.createQuery(sql, DailyRevenueSummary.class);
            query.setParameter("organizationId", organizationId);
            query.setParameter("businessDate", businessDate);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DailyRevenueSummary getByOrganizationIdAndDate()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyRevenueSummary> getByOrganizationId(Integer organizationId) {
        if (organizationId == null) {
            return new ArrayList<>();
        }
        String sql = "from DailyRevenueSummary d where d.organizationId = :organizationId order by d.businessDate";
        try {
            TypedQuery<DailyRevenueSummary> query = entityManager.createQuery(sql, DailyRevenueSummary.class);
            query.setParameter("organizationId", organizationId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DailyRevenueSummary getByOrganizationId()", e);
        }
    }
}
