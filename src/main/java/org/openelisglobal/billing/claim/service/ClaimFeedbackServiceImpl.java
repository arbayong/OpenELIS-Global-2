package org.openelisglobal.billing.claim.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.claim.ClaimFeedbackItem;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.ClaimBatchDAO;
import org.openelisglobal.billing.dao.ClaimLineDAO;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.billing.valueholder.ClaimBatchStatus;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.billing.valueholder.ClaimLineStatus;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClaimFeedbackServiceImpl implements ClaimFeedbackService {

    @Autowired
    private ClaimBatchDAO claimBatchDAO;

    @Autowired
    private ClaimLineDAO claimLineDAO;

    @Autowired
    private BillingInvoiceLineDAO billingInvoiceLineDAO;

    @Autowired
    private NhisXmlService nhisXmlService;

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
            return batch;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
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
