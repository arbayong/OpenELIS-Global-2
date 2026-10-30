package org.openelisglobal.billing.claim.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.BillingPayerDAO;
import org.openelisglobal.billing.dao.ClaimBatchDAO;
import org.openelisglobal.billing.dao.ClaimLineDAO;
import org.openelisglobal.billing.dao.FacilityCredentialingDAO;
import org.openelisglobal.billing.dao.PatientPolicyDAO;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingInvoiceStatus;
import org.openelisglobal.billing.valueholder.BillingLineStatus;
import org.openelisglobal.billing.valueholder.BillingPayer;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.billing.valueholder.ClaimBatchStatus;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.billing.valueholder.ClaimLineStatus;
import org.openelisglobal.billing.valueholder.FacilityCredentialing;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.openelisglobal.billing.valueholder.PayerType;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClaimBatchServiceImpl implements ClaimBatchService {

    @Autowired
    private BillingInvoiceDAO billingInvoiceDAO;

    @Autowired
    private BillingInvoiceLineDAO billingInvoiceLineDAO;

    @Autowired
    private BillingPayerDAO billingPayerDAO;

    @Autowired
    private ClaimBatchDAO claimBatchDAO;

    @Autowired
    private ClaimLineDAO claimLineDAO;

    @Autowired
    private FacilityCredentialingDAO facilityCredentialingDAO;

    @Autowired
    private PatientPolicyDAO patientPolicyDAO;

    @Override
    @Transactional
    public ClaimBatch buildBatch(Integer organizationId, Date from, Date to) {
        try {
            if (organizationId == null) {
                throw new LIMSRuntimeException("Organization id is required");
            }

            List<BillingPayer> nhisPayers = billingPayerDAO.getActiveByType("nhis");
            if (nhisPayers == null || nhisPayers.isEmpty()) {
                throw new LIMSRuntimeException("No active NHIS payer configured");
            }

            List<FacilityCredentialing> credentials = facilityCredentialingDAO.getByOrganizationId(organizationId);
            FacilityCredentialing credential = null;
            if (credentials != null) {
                for (FacilityCredentialing c : credentials) {
                    if ("Y".equals(c.getIsActive())) {
                        credential = c;
                        break;
                    }
                }
            }
            if (credential == null) {
                throw new LIMSRuntimeException("No active facility credentialing for organization: " + organizationId);
            }

            java.util.Date now = new java.util.Date();
            Date batchDate = new java.sql.Date(now.getTime());
            String batchNumber = "NHIS-" + organizationId + "-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(now);

            ClaimBatch batch = new ClaimBatch();
            batch.setOrganizationId(organizationId);
            batch.setBatchNumber(batchNumber);
            batch.setBatchDate(batchDate);
            batch.setStatus(ClaimBatchStatus.READY);
            batch.setTotalClaims(0);
            batch.setTotalAmount(BigDecimal.ZERO);
            Integer batchId = claimBatchDAO.insert(batch);
            batch.setId(batchId);

            int totalClaims = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;

            List<BillingInvoice> invoices = billingInvoiceDAO.getByOrganizationId(organizationId);
            if (invoices != null) {
                for (BillingInvoice invoice : invoices) {
                    if (invoice.getStatus() != BillingInvoiceStatus.ISSUED
                            && invoice.getStatus() != BillingInvoiceStatus.PARTIALLY_PAID
                            && invoice.getStatus() != BillingInvoiceStatus.PAID) {
                        continue;
                    }
                    if (invoice.getPayerType() != PayerType.NHIS) {
                        continue;
                    }
                    Timestamp issuedAt = invoice.getIssuedAt();
                    if (issuedAt == null) {
                        continue;
                    }
                    if (from != null && issuedAt.before(from)) {
                        continue;
                    }
                    if (to != null && issuedAt.after(to)) {
                        continue;
                    }

                    List<BillingInvoiceLine> lines = billingInvoiceLineDAO.getByInvoiceId(invoice.getId());
                    if (lines == null) {
                        continue;
                    }
                    for (BillingInvoiceLine line : lines) {
                        if (line.getLineStatus() != BillingLineStatus.ACTIVE) {
                            continue;
                        }

                        List<PatientPolicy> policies = patientPolicyDAO.getActiveByPatientId(invoice.getPatientId());
                        PatientPolicy policy = null;
                        if (policies != null && !policies.isEmpty()) {
                            policy = policies.get(0);
                        }
                        if (policy == null) {
                            continue;
                        }
                        if (policy.getPayerId() == null) {
                            continue;
                        }
                        boolean nhisPayer = false;
                        for (BillingPayer payer : nhisPayers) {
                            if (payer.getId().equals(policy.getPayerId())) {
                                nhisPayer = true;
                                break;
                            }
                        }
                        if (!nhisPayer) {
                            continue;
                        }
                        String memberNumber = policy.getMemberNumber();
                        if (memberNumber == null || memberNumber.trim().isEmpty()) {
                            continue;
                        }

                        ClaimLine claimLine = new ClaimLine();
                        claimLine.setClaimBatchId(batchId);
                        claimLine.setInvoiceLineId(line.getId());
                        claimLine.setPatientId(invoice.getPatientId());
                        claimLine.setMemberNumber(memberNumber);
                        claimLine.setServiceCode(line.getLocalCode());
                        claimLine.setClaimedAmount(line.getLineTotal());
                        claimLine.setStatus(ClaimLineStatus.PENDING);
                        Integer claimLineId = claimLineDAO.insert(claimLine);
                        claimLine.setId(claimLineId);

                        line.setClaimLineId(String.valueOf(claimLineId));
                        billingInvoiceLineDAO.update(line);

                        totalClaims++;
                        totalAmount = totalAmount.add(line.getLineTotal() == null ? BigDecimal.ZERO : line.getLineTotal());
                    }
                }
            }

            batch.setTotalClaims(totalClaims);
            batch.setTotalAmount(totalAmount);
            claimBatchDAO.update(batch);
            return batch;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimBatch getBatch(Integer batchId) {
        try {
            return claimBatchDAO.get(batchId)
                    .orElseThrow(() -> new LIMSRuntimeException("Claim batch not found: " + batchId));
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimBatch> getBatches(Integer organizationId) {
        try {
            List<ClaimBatch> batches = claimBatchDAO.getAllMatchingOrdered("organizationId", organizationId, "id", true);
            return batches == null ? new ArrayList<ClaimBatch>() : batches;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }
}
