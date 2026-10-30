package org.openelisglobal.billing.dao;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.billing.valueholder.DailyRevenueSummary;
import org.openelisglobal.common.dao.BaseDAO;

public interface DailyRevenueSummaryDAO extends BaseDAO<DailyRevenueSummary, Integer> {

    List<DailyRevenueSummary> getByOrganizationIdAndDate(Integer organizationId, Date businessDate);

    List<DailyRevenueSummary> getByOrganizationId(Integer organizationId);
}
