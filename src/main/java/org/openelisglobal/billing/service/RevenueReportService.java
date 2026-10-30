package org.openelisglobal.billing.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.DailyRevenueSummary;

public interface RevenueReportService {

    DailyRevenueSummary recomputeDailySummary(Integer organizationId, Date businessDate);

    DailyRevenueSummary getDailySummary(Integer organizationId, Date businessDate);

    List<DailyRevenueSummary> getMonthlySummary(Integer organizationId, int year, int month);

    List<Map<String, Object>> getRevenueByTest(Integer organizationId, Date from, Date to);

    List<BillingInvoice> getOutstanding(Integer organizationId);
}
