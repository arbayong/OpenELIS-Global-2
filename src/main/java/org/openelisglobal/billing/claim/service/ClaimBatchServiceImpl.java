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
import org.openelisglobal.billing.dao.GdrgMappingDAO;
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
import org.openelisglobal.billing.valueholder.GdrgMapping;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.openelisglobal.billing.valueholder.PayerType;
import org.openelisglobal.billing.service.BillingConfigService;
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

    @Autowired
    private GdrgMappingDAO gdrgMappingDAO;

    @Autowired
    private BillingConfigService billingConfigService;

    @Override
    @Transactional
    public ClaimBatch buildBatch(Integer organizationId, Date from, Date to) {
        if (organizationId == null) {
            throw new LIMSRuntimeException("Organization id is required");
        }

        String reject = preflightCheck(organizationId);
        if (reject != null) {
            throw new LIMSRuntimeException(reject);
        }

        try {
            List<BillingPayer> nhisPayers = billingPayerDAO.getActiveByType("nhis");

            List<ClaimLine> claimLines = new ArrayList<ClaimLine>();
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
                        if (policy == null || policy.getPayerId() == null) {
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
                        claimLine.setInvoiceLineId(line.getId());
                        claimLine.setPatientId(invoice.getPatientId());
                        claimLine.setMemberNumber(memberNumber);
                        claimLine.setServiceCode(line.getLocalCode());
                        claimLine.setClaimedAmount(line.getLineTotal());
                        claimLine.setStatus(ClaimLineStatus.PENDING);
                        populateV86Fields(claimLine, invoice, line);
                        claimLines.add(claimLine);

                        totalClaims++;
                        totalAmount = totalAmount.add(line.getLineTotal() == null ? BigDecimal.ZERO : line.getLineTotal());
                    }
                }
            }

            if (claimLines.isEmpty()) {
                throw new LIMSRuntimeException("No eligible NHIS claim lines found for organization " + organizationId
                        + " in the selected date range");
            }

            String validationError = validateClaimLines(claimLines);
            if (validationError != null) {
                throw new LIMSRuntimeException(validationError);
            }

            java.util.Date now = new java.util.Date();
            Date batchDate = new java.sql.Date(now.getTime());
            String batchNumber = "NHIS-" + organizationId + "-"
                    + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(now);

            ClaimBatch batch = new ClaimBatch();
            batch.setOrganizationId(organizationId);
            batch.setBatchNumber(batchNumber);
            batch.setBatchDate(batchDate);
            batch.setStatus(ClaimBatchStatus.READY);
            batch.setTotalClaims(totalClaims);
            batch.setTotalAmount(totalAmount);
            Integer batchId = claimBatchDAO.insert(batch);
            batch.setId(batchId);

            for (ClaimLine claimLine : claimLines) {
                claimLine.setClaimBatchId(batchId);
                Integer claimLineId = claimLineDAO.insert(claimLine);
                claimLine.setId(claimLineId);

                BillingInvoiceLine line = billingInvoiceLineDAO.get(claimLine.getInvoiceLineId())
                        .orElse(null);
                if (line != null) {
                    line.setClaimLineId(String.valueOf(claimLineId));
                    billingInvoiceLineDAO.update(line);
                }
            }

            return batch;
        } catch (Exception e) {
            LogEvent.logError(e);
            if (e instanceof LIMSRuntimeException) {
                throw (LIMSRuntimeException) e;
            }
            throw new LIMSRuntimeException(e);
        }
    }

    /**
     * Pre-flight validation for building a claim batch. Returns null when the
     * batch may proceed, or a human-readable rejection message otherwise. Runs
     * before any batch or claim line is persisted so a failed batch is never
     * created.
     */
    /**
     * NHIA v8.6 batch claim fields for one invoice line. Defaults are the
     * outpatient-investigation shape a diagnostic centre submits. The GDRG code
     * is resolved from billing_gdrg_mapping — the claim XML carries it as
     * TreatmentCode, never our local code. The referral number and outpatient
     * code both come from the sample's referring id, which the invoice stored
     * as externalOrderId when the order was referred in from another facility.
     */
    private void populateV86Fields(ClaimLine claimLine, BillingInvoice invoice, BillingInvoiceLine line) {
        claimLine.setClaimIdentificationNumber("CLM-" + line.getId());
        claimLine.setServiceType("OUT");
        claimLine.setOutcomeType("DIS");
        claimLine.setAdmissionType("CRO");
        claimLine.setSpecialityCode("INVE");
        Timestamp issuedAt = invoice.getIssuedAt();
        Date admissionDate = issuedAt == null ? new Date(System.currentTimeMillis())
                : new Date(issuedAt.getTime());
        claimLine.setAdmissionDate(admissionDate);
        claimLine.setOutpatientCode(invoice.getExternalOrderId());
        claimLine.setReferralNo(invoice.getExternalOrderId());
        claimLine.setTotalCost(line.getLineTotal());
        claimLine.setTariff(line.getUnitPrice());
        List<GdrgMapping> mappings = gdrgMappingDAO.getActiveByLocalCode(line.getLocalCode(), admissionDate);
        if (mappings != null && !mappings.isEmpty()) {
            claimLine.setGdrgCode(mappings.get(0).getGdrgCode());
        }
    }

    /** NHIA allow-lists from the CLAIM-it survey (nhis-xml.ts). */
    private static final List<String> NHIS_ADMISSION_TYPES = List.of("CRO", "EME", "ACU");

    private static final List<String> NHIS_SPECIALITY_CODES = List.of("ASUR", "DENT", "ENTH", "INVE", "MEDI",
            "OBGY", "OPDC", "OPHT", "ORTH", "PAED", "PSUR", "RSUR", "ZOOM");

    /**
     * NHIA technical validation rules (error codes from the CLAIM-it survey)
     * applied before a batch is exported, so a file that would fail portal
     * validation is never produced. The claim check code (237) is deliberately
     * not enforced here: it comes from the patient's OTAC on the NHIA portal,
     * USSD or app and is captured per encounter.
     */
    private String validateClaimLines(List<ClaimLine> claimLines) {
        for (ClaimLine line : claimLines) {
            String claimId = line.getClaimIdentificationNumber() == null ? "?" : line.getClaimIdentificationNumber();
            if (line.getMemberNumber() == null || line.getMemberNumber().trim().length() < 8) {
                return "NHIA 203: member number is missing or shorter than 8 characters on claim " + claimId;
            }
            if (!NHIS_ADMISSION_TYPES.contains(line.getAdmissionType())) {
                return "NHIA 212: admission type " + line.getAdmissionType() + " is not one of CRO, EME, ACU (claim "
                        + claimId + ")";
            }
            if (!NHIS_SPECIALITY_CODES.contains(line.getSpecialityCode())) {
                return "NHIA 271: speciality code " + line.getSpecialityCode()
                        + " is not on the NHIA allow-list (claim " + claimId + ")";
            }
            if (line.getAdmissionDate() == null) {
                return "NHIA 214: admission date is missing on claim " + claimId;
            }
            if (line.getGdrgCode() == null || line.getGdrgCode().isEmpty()) {
                return "NHIA 224: no GDRG mapping for local code " + line.getServiceCode()
                        + " — add a row to billing_gdrg_mapping (claim " + claimId + ")";
            }
            BigDecimal total = line.getTotalCost() != null ? line.getTotalCost() : BigDecimal.ZERO;
            if (total.compareTo(BigDecimal.ZERO) == 0) {
                return "NHIA 111: claim " + claimId + " has a zero total cost";
            }
        }
        return null;
    }

    private String preflightCheck(Integer organizationId) {
        if (!billingConfigService.isNhisEnabled()) {
            return "NHIS claims are disabled (nhis.enabled is false)";
        }

        List<BillingPayer> nhisPayers = billingPayerDAO.getActiveByType("nhis");
        if (nhisPayers == null || nhisPayers.isEmpty()) {
            return "No active NHIS payer configured";
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
            return "No active facility credentialing for organization: " + organizationId;
        }
        if (credential.getAccreditationNumber() == null || credential.getAccreditationNumber().trim().isEmpty()) {
            return "Facility credentialing is missing accreditation number";
        }
        if (credential.getTechnicalHeadLicenseNumber() == null
                || credential.getTechnicalHeadLicenseNumber().trim().isEmpty()) {
            return "Facility credentialing is missing technical head licence number";
        }
        return null;
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
