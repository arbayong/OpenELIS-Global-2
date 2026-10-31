package org.openelisglobal.billing.controller.rest;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.openelisglobal.billing.security.BillingPermissions;
import org.openelisglobal.billing.service.RevenueReportService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.DailyRevenueSummary;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/billing/reports")
@PreAuthorize(BillingPermissions.VIEW_REPORTS)
public class ReportsRestController {

    @Autowired
    private RevenueReportService revenueReportService;

    @GetMapping("/daily")
    public DailyRevenueSummary daily(@RequestParam Integer orgId, @RequestParam String date) {
        try {
            return revenueReportService.getDailySummary(orgId, Date.valueOf(date));
        } catch (Exception e) {
            LogEvent.logError("ReportsRestController", "daily", e.getMessage());
            throw new LIMSRuntimeException("Failed to get daily revenue summary", e);
        }
    }

    @GetMapping("/monthly")
    public List<DailyRevenueSummary> monthly(@RequestParam Integer orgId, @RequestParam int year, @RequestParam int month) {
        try {
            return revenueReportService.getMonthlySummary(orgId, year, month);
        } catch (Exception e) {
            LogEvent.logError("ReportsRestController", "monthly", e.getMessage());
            throw new LIMSRuntimeException("Failed to get monthly revenue summary", e);
        }
    }

    @GetMapping("/by-test")
    public List<Map<String, Object>> byTest(@RequestParam Integer orgId, @RequestParam String from, @RequestParam String to) {
        try {
            return revenueReportService.getRevenueByTest(orgId, Date.valueOf(from), Date.valueOf(to));
        } catch (Exception e) {
            LogEvent.logError("ReportsRestController", "byTest", e.getMessage());
            throw new LIMSRuntimeException("Failed to get revenue by test", e);
        }
    }

    @GetMapping("/outstanding")
    public List<BillingInvoice> outstanding(@RequestParam Integer orgId) {
        try {
            return revenueReportService.getOutstanding(orgId);
        } catch (Exception e) {
            LogEvent.logError("ReportsRestController", "outstanding", e.getMessage());
            throw new LIMSRuntimeException("Failed to get outstanding invoices", e);
        }
    }

    @GetMapping("/claim-reconciliation")
    public List<Map<String, Object>> claimReconciliation(@RequestParam Integer orgId,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        try {
            Date fromDate = (from == null || from.isEmpty()) ? null : Date.valueOf(from);
            Date toDate = (to == null || to.isEmpty()) ? null : Date.valueOf(to);
            return revenueReportService.getClaimReconciliation(orgId, fromDate, toDate);
        } catch (Exception e) {
            LogEvent.logError("ReportsRestController", "claimReconciliation", e.getMessage());
            throw new LIMSRuntimeException("Failed to get claim reconciliation", e);
        }
    }

    @PostMapping("/recompute-daily")
    public DailyRevenueSummary recomputeDaily(@RequestParam Integer orgId, @RequestParam String date) {
        try {
            return revenueReportService.recomputeDailySummary(orgId, Date.valueOf(date));
        } catch (Exception e) {
            LogEvent.logError("ReportsRestController", "recomputeDaily", e.getMessage());
            throw new LIMSRuntimeException("Failed to recompute daily revenue summary", e);
        }
    }
}
