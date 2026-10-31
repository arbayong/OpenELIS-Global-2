package org.openelisglobal.billing.claim.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.billing.claim.ClaimFeedbackItem;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.BillingPaymentDAO;
import org.openelisglobal.billing.dao.ClaimBatchDAO;
import org.openelisglobal.billing.dao.ClaimLineDAO;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingInvoiceStatus;
import org.openelisglobal.billing.valueholder.BillingLineStatus;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.billing.valueholder.ClaimBatchStatus;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.billing.valueholder.ClaimLineStatus;
import org.openelisglobal.billing.valueholder.PaymentMethod;
import org.openelisglobal.billing.service.PaymentService;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class ClaimFeedbackServiceImpl implements ClaimFeedbackService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Automated NHIS settlement is not driven by a counter user. */
    private static final String SETTLEMENT_RECORDED_BY = "nhis-settlement";

    @Autowired
    private ClaimBatchDAO claimBatchDAO;

    @Autowired
    private ClaimLineDAO claimLineDAO;

    @Autowired
    private BillingInvoiceLineDAO billingInvoiceLineDAO;

    @Autowired
    private BillingInvoiceDAO billingInvoiceDAO;

    @Autowired
    private BillingPaymentDAO billingPaymentDAO;

    @Autowired
    private NhisXmlService nhisXmlService;

    @Autowired
    private PaymentService paymentService;

    @Override
    @Transactional
    public ClaimBatch applyFeedback(Integer batchId, String xml) {
        try {
            List<ClaimFeedbackItem> items = nhisXmlService.parseFeedback(xml);

            ClaimBatch batch = claimBatchDAO.get(batchId)
                    .orElseThrow(() -> new LIMSRuntimeException("Claim batch not found: " + batchId));

            List<ClaimLine> lines = claimLineDAO.getByClaimBatchId(batchId);
            if (lines == null) {
                lines = new ArrayList<ClaimLine>();
            }

            for (ClaimFeedbackItem item : items) {
                if (item.getClaimLineId() == null) {
                    continue;
                }

                ClaimLine line = null;
                for (ClaimLine candidate : lines) {
                    if (candidate.getId().equals(item.getClaimLineId())) {
                        line = candidate;
                        break;
                    }
                }
                if (line == null) {
                    continue;
                }

                if (item.getApprovedAmount() != null) {
                    line.setApprovedAmount(item.getApprovedAmount());
                }

                ClaimLineStatus status = mapStatus(item.getStatus());
                if (status != null) {
                    line.setStatus(status);
                }
                claimLineDAO.update(line);

                recomputeInvoiceLine(line);
            }

            int approved = 0;
            int rejected = 0;
            for (ClaimLine line : lines) {
                if (line.getStatus() == ClaimLineStatus.APPROVED || line.getStatus() == ClaimLineStatus.PAID) {
                    approved++;
                } else if (line.getStatus() == ClaimLineStatus.REJECTED) {
                    rejected++;
                }
            }

            int total = lines.size();
            if (total > 0 && approved == total) {
                batch.setStatus(ClaimBatchStatus.PAID);
            } else if (total > 0 && rejected == total) {
                batch.setStatus(ClaimBatchStatus.REJECTED);
            } else {
                batch.setStatus(ClaimBatchStatus.SUBMITTED);
            }

            claimBatchDAO.update(batch);

            recordSettlementPayments(batch, lines);
            reconcileInvoices(lines);

            return batch;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    /**
     * Record one NHIS settlement payment per invoice for the amounts approved in
     * this feedback. A failure here is logged and swallowed so it can never roll
     * back the feedback that already marked the claim lines.
     */
    private void recordSettlementPayments(ClaimBatch batch, List<ClaimLine> lines) {
        Map<Integer, BigDecimal> approvedByInvoice = new LinkedHashMap<>();
        Map<Integer, Integer> lineCountByInvoice = new LinkedHashMap<>();

        for (ClaimLine line : lines) {
            if (line.getInvoiceLineId() == null) {
                continue;
            }
            ClaimLineStatus status = line.getStatus();
            if (status != ClaimLineStatus.APPROVED && status != ClaimLineStatus.PAID) {
                continue;
            }
            BigDecimal approved = line.getApprovedAmount();
            if (approved == null || approved.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BillingInvoiceLine invoiceLine = billingInvoiceLineDAO.get(line.getInvoiceLineId())
                    .orElse(null);
            if (invoiceLine == null || invoiceLine.getInvoiceId() == null) {
                continue;
            }

            Integer invoiceId = invoiceLine.getInvoiceId();
            approvedByInvoice.merge(invoiceId, approved, BigDecimal::add);
            lineCountByInvoice.merge(invoiceId, 1, Integer::sum);
        }

        for (Map.Entry<Integer, BigDecimal> entry : approvedByInvoice.entrySet()) {
            Integer invoiceId = entry.getKey();
            BigDecimal total = entry.getValue();
            if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            int lineCount = lineCountByInvoice.getOrDefault(invoiceId, 0);
            try {
                String metadata = objectMapper.writeValueAsString(
                        Map.of("claimBatchId", batch.getId(), "lineCount", lineCount));
                BillingPayment payment = paymentService.recordPayment(
                        invoiceId, total, PaymentMethod.BANK_TRANSFER, null,
                        "nhis:" + batch.getId(), SETTLEMENT_RECORDED_BY);
                if (payment != null) {
                    payment.setMetadata(metadata);
                    billingPaymentDAO.update(payment);
                }
            } catch (Exception e) {
                LogEvent.logError("ClaimFeedbackServiceImpl", "recordSettlementPayments", e.getMessage());
            }
        }
    }

    /**
     * Reconcile each affected invoice's totals from its active lines and refresh
     * its payment status. Invoices whose lines are all void are skipped.
     */
    private void reconcileInvoices(List<ClaimLine> lines) {
        Map<Integer, List<ClaimLine>> linesByInvoice = new LinkedHashMap<>();
        for (ClaimLine line : lines) {
            if (line.getInvoiceLineId() == null) {
                continue;
            }
            BillingInvoiceLine invoiceLine = billingInvoiceLineDAO.get(line.getInvoiceLineId())
                    .orElse(null);
            if (invoiceLine == null || invoiceLine.getInvoiceId() == null) {
                continue;
            }
            linesByInvoice.computeIfAbsent(invoiceLine.getInvoiceId(), k -> new ArrayList<>()).add(line);
        }

        for (Map.Entry<Integer, List<ClaimLine>> entry : linesByInvoice.entrySet()) {
            Integer invoiceId = entry.getKey();
            try {
                BillingInvoice invoice = billingInvoiceDAO.get(invoiceId).orElse(null);
                if (invoice == null) {
                    continue;
                }

                List<BillingInvoiceLine> invoiceLines = billingInvoiceLineDAO.getByInvoiceId(invoiceId);
                if (invoiceLines == null || invoiceLines.isEmpty()) {
                    continue;
                }

                boolean allVoid = true;
                BigDecimal subtotal = BigDecimal.ZERO;
                for (BillingInvoiceLine invoiceLine : invoiceLines) {
                    if (invoiceLine.getLineStatus() == BillingLineStatus.VOID) {
                        continue;
                    }
                    allVoid = false;
                    if (invoiceLine.getLineStatus() == BillingLineStatus.ACTIVE) {
                        subtotal = subtotal.add(invoiceLine.getLineTotal() == null ? BigDecimal.ZERO
                                : invoiceLine.getLineTotal());
                    }
                }
                if (allVoid) {
                    continue;
                }

                BigDecimal total = subtotal;
                BigDecimal amountPaid = invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid();
                if (amountPaid.compareTo(total) > 0) {
                    amountPaid = total;
                }

                invoice.setSubtotal(subtotal);
                invoice.setTotal(total);
                invoice.setAmountPaid(amountPaid);
                if (amountPaid.compareTo(total) >= 0) {
                    invoice.setStatus(BillingInvoiceStatus.PAID);
                } else if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                    invoice.setStatus(BillingInvoiceStatus.PARTIALLY_PAID);
                } else {
                    invoice.setStatus(BillingInvoiceStatus.ISSUED);
                }
                billingInvoiceDAO.update(invoice);
            } catch (Exception e) {
                LogEvent.logError("ClaimFeedbackServiceImpl", "reconcileInvoices", e.getMessage());
            }
        }
    }

    private ClaimLineStatus mapStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return ClaimLineStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void recomputeInvoiceLine(ClaimLine line) {
        if (line.getInvoiceLineId() == null) {
            return;
        }

        BillingInvoiceLine invoiceLine = billingInvoiceLineDAO.get(line.getInvoiceLineId())
                .orElse(null);
        if (invoiceLine == null) {
            return;
        }

        BigDecimal claimed = line.getClaimedAmount() == null ? BigDecimal.ZERO : line.getClaimedAmount();
        BigDecimal approved = line.getApprovedAmount() == null ? BigDecimal.ZERO : line.getApprovedAmount();

        if (line.getStatus() == ClaimLineStatus.APPROVED || line.getStatus() == ClaimLineStatus.PAID) {
            invoiceLine.setInsuranceExpectedAmount(approved);
            invoiceLine.setPatientResponsibilityAmount(claimed.subtract(approved).max(BigDecimal.ZERO));
        } else if (line.getStatus() == ClaimLineStatus.REJECTED) {
            invoiceLine.setInsuranceExpectedAmount(BigDecimal.ZERO);
            invoiceLine.setPatientResponsibilityAmount(claimed);
        }

        billingInvoiceLineDAO.update(invoiceLine);
    }
}
