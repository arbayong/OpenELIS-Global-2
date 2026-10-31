package org.openelisglobal.billing.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.BillingPaymentDAO;
import org.openelisglobal.billing.dao.ClaimBatchDAO;
import org.openelisglobal.billing.dao.ClaimLineDAO;
import org.openelisglobal.billing.dao.DailyRevenueSummaryDAO;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingInvoiceStatus;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.billing.valueholder.ClaimLineStatus;
import org.openelisglobal.billing.valueholder.DailyRevenueSummary;
import org.openelisglobal.billing.valueholder.PaymentMethod;
import org.openelisglobal.billing.valueholder.PaymentType;
import org.openelisglobal.billing.valueholder.PayerType;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RevenueReportServiceImpl implements RevenueReportService {

    @Autowired
    private BillingInvoiceDAO billingInvoiceDAO;

    @Autowired
    private BillingPaymentDAO billingPaymentDAO;

    @Autowired
    private DailyRevenueSummaryDAO dailyRevenueSummaryDAO;

    @Autowired
    private BillingInvoiceLineDAO billingInvoiceLineDAO;

    @Autowired
    private ClaimBatchDAO claimBatchDAO;

    @Autowired
    private ClaimLineDAO claimLineDAO;

    @Override
    public DailyRevenueSummary recomputeDailySummary(Integer organizationId, Date businessDate) {
        try {
            List<BillingInvoice> invoices = billingInvoiceDAO.getByOrganizationId(organizationId);
            List<org.openelisglobal.billing.valueholder.BillingPayment> payments = billingPaymentDAO.getByOrganizationId(organizationId);

            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal cash = BigDecimal.ZERO;
            BigDecimal mobileMoney = BigDecimal.ZERO;
            BigDecimal gateway = BigDecimal.ZERO;
            BigDecimal insurance = BigDecimal.ZERO;
            BigDecimal nhis = BigDecimal.ZERO;
            BigDecimal privateRev = BigDecimal.ZERO;
            BigDecimal refunds = BigDecimal.ZERO;
            int invoiceCount = 0;
            List<String> patientIds = new ArrayList<>();

            for (BillingInvoice invoice : invoices) {
                if (invoice.getStatus() == BillingInvoiceStatus.VOID || invoice.getStatus() == BillingInvoiceStatus.DRAFT) {
                    continue;
                }
                Date invoiceDate = toDate(invoice.getIssuedAt());
                if (invoiceDate == null) {
                    invoiceDate = toDate(invoice.getCreatedAt());
                }
                if (invoiceDate == null || !invoiceDate.equals(businessDate)) {
                    continue;
                }
                BigDecimal total = invoice.getTotal() == null ? BigDecimal.ZERO : invoice.getTotal();
                gross = gross.add(total);
                invoiceCount++;
                if (invoice.getPatientId() != null && !invoice.getPatientId().isEmpty()) {
                    patientIds.add(invoice.getPatientId());
                }
                if (invoice.getPayerType() == PayerType.NHIS) {
                    nhis = nhis.add(total);
                    insurance = insurance.add(total);
                } else if (invoice.getPayerType() == PayerType.PRIVATE) {
                    privateRev = privateRev.add(total);
                    insurance = insurance.add(total);
                }
            }

            for (org.openelisglobal.billing.valueholder.BillingPayment payment : payments) {
                Date receivedDate = toDate(payment.getReceivedAt());
                if (receivedDate == null || !receivedDate.equals(businessDate)) {
                    continue;
                }
                BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
                if (payment.getType() == PaymentType.REFUND) {
                    refunds = refunds.add(amount.negate());
                } else if (payment.getType() == PaymentType.PAYMENT) {
                    if (payment.getMethod() == PaymentMethod.CASH) {
                        cash = cash.add(amount);
                    } else if (payment.getMethod() == PaymentMethod.MOBILE_MONEY) {
                        mobileMoney = mobileMoney.add(amount);
                    } else if (payment.getMethod() == PaymentMethod.GATEWAY) {
                        gateway = gateway.add(amount);
                    }
                }
            }

            int patientCount = (int) patientIds.stream().distinct().count();

            List<DailyRevenueSummary> existing = dailyRevenueSummaryDAO.getByOrganizationIdAndDate(organizationId, businessDate);
            DailyRevenueSummary summary;
            if (existing != null && !existing.isEmpty()) {
                summary = existing.get(0);
            } else {
                summary = new DailyRevenueSummary();
                summary.setOrganizationId(organizationId);
                summary.setBusinessDate(businessDate);
            }
            summary.setGrossRevenue(gross);
            summary.setCashRevenue(cash);
            summary.setMobileMoneyRevenue(mobileMoney);
            summary.setGatewayRevenue(gateway);
            summary.setInsuranceRevenue(insurance);
            summary.setNhisRevenue(nhis);
            summary.setPrivateRevenue(privateRev);
            summary.setRefunds(refunds);
            summary.setInvoiceCount(invoiceCount);
            summary.setPatientCount(patientCount);

            if (summary.getId() == null) {
                dailyRevenueSummaryDAO.insert(summary);
            } else {
                dailyRevenueSummaryDAO.update(summary);
            }
            return summary;
        } catch (Exception e) {
            LogEvent.logError("RevenueReportServiceImpl", "recomputeDailySummary", e.getMessage());
            throw new LIMSRuntimeException("Failed to recompute daily revenue summary", e);
        }
    }

    @Override
    public DailyRevenueSummary getDailySummary(Integer organizationId, Date businessDate) {
        try {
            List<DailyRevenueSummary> list = dailyRevenueSummaryDAO.getByOrganizationIdAndDate(organizationId, businessDate);
            if (list == null || list.isEmpty()) {
                return null;
            }
            return list.get(0);
        } catch (Exception e) {
            LogEvent.logError("RevenueReportServiceImpl", "getDailySummary", e.getMessage());
            throw new LIMSRuntimeException("Failed to get daily revenue summary", e);
        }
    }

    @Override
    public List<DailyRevenueSummary> getMonthlySummary(Integer organizationId, int year, int month) {
        try {
            List<DailyRevenueSummary> all = dailyRevenueSummaryDAO.getByOrganizationId(organizationId);
            if (all == null) {
                return new ArrayList<>();
            }
            return all.stream()
                    .filter(s -> s.getBusinessDate() != null)
                    .filter(s -> {
                        java.time.LocalDate ld = s.getBusinessDate().toLocalDate();
                        return ld.getYear() == year && ld.getMonthValue() == month;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LogEvent.logError("RevenueReportServiceImpl", "getMonthlySummary", e.getMessage());
            throw new LIMSRuntimeException("Failed to get monthly revenue summary", e);
        }
    }

    @Override
    public List<Map<String, Object>> getRevenueByTest(Integer organizationId, Date from, Date to) {
        try {
            List<BillingInvoice> invoices = billingInvoiceDAO.getByOrganizationId(organizationId);
            Map<String, Map<String, Object>> groups = new LinkedHashMap<>();

            for (BillingInvoice invoice : invoices) {
                if (invoice.getStatus() == BillingInvoiceStatus.VOID || invoice.getStatus() == BillingInvoiceStatus.DRAFT) {
                    continue;
                }
                Date invoiceDate = toDate(invoice.getIssuedAt());
                if (invoiceDate == null) {
                    invoiceDate = toDate(invoice.getCreatedAt());
                }
                if (invoiceDate == null || invoiceDate.before(from) || invoiceDate.after(to)) {
                    continue;
                }
                List<BillingInvoiceLine> lines = billingInvoiceLineDAO.getByInvoiceId(invoice.getId());
                if (lines == null) {
                    continue;
                }
                for (BillingInvoiceLine line : lines) {
                    String key = line.getTestId() != null ? line.getTestId() : line.getLocalCode();
                    if (key == null) {
                        continue;
                    }
                    Map<String, Object> group = groups.get(key);
                    if (group == null) {
                        group = new HashMap<>();
                        group.put("testId", line.getTestId());
                        group.put("testName", line.getTestName());
                        group.put("localCode", line.getLocalCode());
                        group.put("revenue", BigDecimal.ZERO);
                        groups.put(key, group);
                    }
                    BigDecimal lineTotal = line.getLineTotal() == null ? BigDecimal.ZERO : line.getLineTotal();
                    BigDecimal revenue = (BigDecimal) group.get("revenue");
                    group.put("revenue", revenue.add(lineTotal));
                }
            }

            return new ArrayList<>(groups.values());
        } catch (Exception e) {
            LogEvent.logError("RevenueReportServiceImpl", "getRevenueByTest", e.getMessage());
            throw new LIMSRuntimeException("Failed to get revenue by test", e);
        }
    }

    @Override
    public List<BillingInvoice> getOutstanding(Integer organizationId) {
        try {
            List<BillingInvoice> invoices = billingInvoiceDAO.getByOrganizationId(organizationId);
            if (invoices == null) {
                return new ArrayList<>();
            }
            return invoices.stream()
                    .filter(i -> i.getStatus() == BillingInvoiceStatus.ISSUED || i.getStatus() == BillingInvoiceStatus.PARTIALLY_PAID)
                    .filter(i -> i.getTotal() != null && i.getAmountPaid() != null && i.getTotal().compareTo(i.getAmountPaid()) > 0)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LogEvent.logError("RevenueReportServiceImpl", "getOutstanding", e.getMessage());
            throw new LIMSRuntimeException("Failed to get outstanding invoices", e);
        }
    }

    @Override
    public List<Map<String, Object>> getClaimReconciliation(Integer organizationId, Date from, Date to) {
        try {
            List<ClaimBatch> batches = claimBatchDAO.getAllMatchingOrdered("organizationId", organizationId, "id", true);
            if (batches == null) {
                return new ArrayList<>();
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ClaimBatch batch : batches) {
                Date batchDate = batch.getBatchDate();
                if (batchDate == null) {
                    continue;
                }
                if (from != null && batchDate.before(from)) {
                    continue;
                }
                if (to != null && batchDate.after(to)) {
                    continue;
                }

                List<ClaimLine> lines = claimLineDAO.getByClaimBatchId(batch.getId());
                if (lines == null) {
                    lines = new ArrayList<>();
                }

                int submittedCount = 0;
                BigDecimal submittedAmount = BigDecimal.ZERO;
                int approvedCount = 0;
                BigDecimal approvedAmount = BigDecimal.ZERO;
                int rejectedCount = 0;
                BigDecimal rejectedAmount = BigDecimal.ZERO;
                int paidCount = 0;
                BigDecimal paidAmount = BigDecimal.ZERO;

                for (ClaimLine line : lines) {
                    BigDecimal claimed = line.getClaimedAmount() == null ? BigDecimal.ZERO : line.getClaimedAmount();
                    BigDecimal approved = line.getApprovedAmount() == null ? BigDecimal.ZERO : line.getApprovedAmount();
                    submittedCount++;
                    submittedAmount = submittedAmount.add(claimed);

                    ClaimLineStatus status = line.getStatus();
                    if (status == ClaimLineStatus.APPROVED || status == ClaimLineStatus.PAID) {
                        approvedCount++;
                        approvedAmount = approvedAmount.add(approved);
                        if (status == ClaimLineStatus.PAID) {
                            paidCount++;
                            paidAmount = paidAmount.add(approved);
                        }
                    } else if (status == ClaimLineStatus.REJECTED) {
                        rejectedCount++;
                        rejectedAmount = rejectedAmount.add(claimed);
                    }
                }

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("batchNumber", batch.getBatchNumber());
                row.put("batchDate", batchDate);
                row.put("status", batch.getStatus());
                row.put("totalClaims", batch.getTotalClaims() != null ? batch.getTotalClaims() : lines.size());
                row.put("submittedCount", submittedCount);
                row.put("submittedAmount", submittedAmount);
                row.put("approvedCount", approvedCount);
                row.put("approvedAmount", approvedAmount);
                row.put("rejectedCount", rejectedCount);
                row.put("rejectedAmount", rejectedAmount);
                row.put("paidCount", paidCount);
                row.put("paidAmount", paidAmount);
                rows.add(row);
            }
            return rows;
        } catch (Exception e) {
            LogEvent.logError("RevenueReportServiceImpl", "getClaimReconciliation", e.getMessage());
            throw new LIMSRuntimeException("Failed to get claim reconciliation", e);
        }
    }

    private Date toDate(Timestamp timestamp) {
        return timestamp == null ? null : new Date(timestamp.getTime());
    }
}
