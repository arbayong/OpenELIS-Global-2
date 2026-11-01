package org.openelisglobal.billing.claim.service.impl;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.openelisglobal.billing.claim.claimit.ClaimitClient;
import org.openelisglobal.billing.claim.claimit.ClaimitClientException;
import org.openelisglobal.billing.claim.claimit.ClaimitEncounterClaim;
import org.openelisglobal.billing.claim.claimit.ClaimitEncounterXmlService;
import org.openelisglobal.billing.claim.claimit.ClaimitImportReport;
import org.openelisglobal.billing.claim.claimit.ClaimitInvestigation;
import org.openelisglobal.billing.claim.claimit.ClaimitReferralInfo;
import org.openelisglobal.billing.claim.service.ClaimitValidationService;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.ClaimitImportRecordDAO;
import org.openelisglobal.billing.dao.ClaimitSettingsDAO;
import org.openelisglobal.billing.dao.GdrgMappingDAO;
import org.openelisglobal.billing.dao.PatientPolicyDAO;
import org.openelisglobal.billing.dao.TariffVersionDAO;
import org.openelisglobal.billing.gateway.GatewayConfigService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingLineStatus;
import org.openelisglobal.billing.valueholder.ClaimitImportRecord;
import org.openelisglobal.billing.valueholder.ClaimitSettings;
import org.openelisglobal.billing.valueholder.GdrgMapping;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.openelisglobal.billing.valueholder.TariffVersion;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClaimitValidationServiceImpl implements ClaimitValidationService {

    /**
     * CLAIM-it's encounter API takes dates as MM/dd/yyyy — verified live on
     * 2026-11-01: a payload sent as 01/10/2026 (1 October, the documented
     * DD/MM/YYYY form) appeared in the CLAIM-it claim list under JAN 2026 as
     * 10 SAT, and every date whose day exceeded 12 failed to parse. This is the
     * opposite of the monthly batch spec, which specifies DD/MM/YYYY, so the
     * two paths deliberately format dates differently.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private BillingInvoiceDAO billingInvoiceDAO;

    @Autowired
    private BillingInvoiceLineDAO billingInvoiceLineDAO;

    @Autowired
    private PatientPolicyDAO patientPolicyDAO;

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private ClaimitSettingsDAO claimitSettingsDAO;

    @Autowired
    private GatewayConfigService gatewayConfigService;

    @Autowired
    private ClaimitClient claimitClient;

    @Autowired
    private ClaimitEncounterXmlService claimitEncounterXmlService;

    @Autowired
    private TariffVersionDAO tariffVersionDAO;

    @Autowired
    private GdrgMappingDAO gdrgMappingDAO;

    @Autowired
    private ClaimitImportRecordDAO claimitImportRecordDAO;

    @Override
    @Transactional
    public ClaimitImportReport validateInvoice(Integer invoiceId, String claimCheckCode, String physicianId,
            String referringFacilityName, String referringFacilityId, String recordedBy) {
        BillingInvoice invoice = billingInvoiceDAO.get(invoiceId)
                .orElseThrow(() -> new LIMSRuntimeException("Invoice not found: " + invoiceId));

        List<BillingInvoiceLine> activeLines = activeLines(invoice.getId());
        if (activeLines.isEmpty()) {
            throw new LIMSRuntimeException("Invoice " + invoiceId + " has no active lines");
        }

        String memberNo = resolveMemberNo(invoice.getPatientId());

        Patient patient = null;
        try {
            patient = patientDAO.get(invoice.getPatientId()).orElse(null);
        } catch (Exception e) {
            patient = null;
        }

        ClaimitSettings settings = claimitSettingsDAO.getAll().stream().findFirst()
                .orElseThrow(() -> new LIMSRuntimeException("CLAIM-it settings are not configured"));
        String baseUrl = settings.getBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new LIMSRuntimeException("CLAIM-it base URL is not configured");
        }

        java.util.Date now = new java.util.Date();
        Date serviceDate = invoice.getIssuedAt() == null ? new Date(now.getTime())
                : new Date(invoice.getIssuedAt().getTime());
        String dateOfService = formatDate(serviceDate);

        BillingInvoiceLine firstLine = activeLines.get(0);
        ClaimitEncounterClaim claim = new ClaimitEncounterClaim();
        claim.setClaimID("CLM-" + invoiceId + "-" + firstLine.getId());
        if (claimCheckCode != null && !claimCheckCode.isEmpty()) {
            claim.setClaimCheckCode(claimCheckCode);
        }
        claim.setMemberNo(memberNo);
        if (patient != null) {
            claim.setCardSerialNo(patient.getExternalId());
            claim.setSurname(patient.getEpiLastName());
            String otherNames = ((patient.getEpiFirstName() == null ? "" : patient.getEpiFirstName()) + " "
                    + (patient.getEpiMiddleName() == null ? "" : patient.getEpiMiddleName())).trim();
            claim.setOtherNames(otherNames.isEmpty() ? null : otherNames);
            claim.setDateOfBirth(patient.getBirthDate() == null ? "" : formatDate(new Date(patient.getBirthDate().getTime())));
            String gender = patient.getGender();
            claim.setGender("female".equalsIgnoreCase(gender) ? "F" : "M");
            claim.setHospitalRecNo(patient.getExternalId());
        }
        claim.setTypeOfService("DIA");
        claim.setIsUnbundled("No");
        claim.setTypeOfAttendance("CFU");
        claim.setServiceOutcome("DISC");
        claim.setDateOfService(dateOfService);
        claim.setSpecialtyAttended(List.of("INVE"));
        if (physicianId != null && !physicianId.isEmpty()) {
            claim.setPhysicianID(physicianId);
        }

        List<ClaimitInvestigation> investigations = new ArrayList<ClaimitInvestigation>();
        List<GdrgMapping> resolvedMappings = new ArrayList<GdrgMapping>();
        for (BillingInvoiceLine line : activeLines) {
            ClaimitInvestigation investigation = new ClaimitInvestigation();
            investigation.setServiceDate(dateOfService);
            GdrgMapping resolved = resolveMapping(line.getLocalCode(), serviceDate);
            resolvedMappings.add(resolved);
            investigation.setGdrgCode(resolved == null ? null : resolved.getGdrgCode());
            investigations.add(investigation);
        }
        claim.setInvestigations(investigations);

        if ((referringFacilityName != null && !referringFacilityName.isEmpty())
                || (referringFacilityId != null && !referringFacilityId.isEmpty())
                || (claimCheckCode != null && !claimCheckCode.isEmpty())) {
            ClaimitReferralInfo referralInfo = new ClaimitReferralInfo();
            referralInfo.setClaimCheckCode(claimCheckCode);
            referralInfo.setFacilityID(referringFacilityId);
            referralInfo.setFacilityName(referringFacilityName);
            claim.setReferralInfo(referralInfo);
        }

        validateBeforeSubmit(claim, resolvedMappings);

        String xml = claimitEncounterXmlService.encodeEncounterXml(List.of(claim));

        String token;
        ClaimitImportReport report;
        try {
            token = claimitClient.authenticate(baseUrl, settings.getUsername(),
                    gatewayConfigService.decryptSecret(settings.getPasswordEncrypted()));
            report = claimitClient.submitClaims(baseUrl, token, xml);
        } catch (ClaimitClientException e) {
            throw new LIMSRuntimeException("CLAIM-it validation failed for invoice " + invoiceId, e);
        }

        recordImportReport(report, invoiceId, recordedBy, xml);

        LogEvent.logInfo("ClaimitValidationServiceImpl", "validateInvoice",
                "Invoice " + invoiceId + " submitted to CLAIM-it: success=" + report.isSuccess()
                        + " passed=" + report.getPassedClaims() + " failed=" + report.getFailedClaims()
                        + " warned=" + report.getWarnedClaims() + " saved=" + report.getSavedClaims());

        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimitSettings getSettings() {
        List<ClaimitSettings> all = claimitSettingsDAO.getAll();
        ClaimitSettings source = all == null || all.isEmpty() ? null : all.get(0);
        if (source == null) {
            return null;
        }
        ClaimitSettings settings = new ClaimitSettings();
        settings.setId(source.getId());
        settings.setBaseUrl(source.getBaseUrl());
        settings.setUsername(source.getUsername());
        // passwordEncrypted deliberately left null: never leak the encrypted secret
        settings.setProviderId(source.getProviderId());
        settings.setProviderName(source.getProviderName());
        settings.setFacilityType(source.getFacilityType());
        settings.setRegion(source.getRegion());
        settings.setProviderTier(source.getProviderTier());
        settings.setLastUpdated(source.getLastUpdated());
        return settings;
    }

    @Override
    @Transactional
    public void saveSettings(String baseUrl, String username, String password, String providerId, String providerName,
            String facilityType, String region, String providerTier, String recordedBy) {
        List<ClaimitSettings> all = claimitSettingsDAO.getAll();
        ClaimitSettings settings = all == null || all.isEmpty() ? new ClaimitSettings() : all.get(0);
        settings.setBaseUrl(baseUrl);
        settings.setUsername(username);
        settings.setProviderId(providerId);
        settings.setProviderName(providerName);
        settings.setFacilityType(facilityType);
        settings.setRegion(region);
        settings.setProviderTier(providerTier);
        if (password != null && !password.trim().isEmpty()) {
            settings.setPasswordEncrypted(gatewayConfigService.encryptSecret(password));
        }
        settings.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        if (settings.getId() == null) {
            claimitSettingsDAO.insert(settings);
        } else {
            claimitSettingsDAO.update(settings);
        }
    }

    private List<BillingInvoiceLine> activeLines(Integer invoiceId) {
        List<BillingInvoiceLine> active = new ArrayList<BillingInvoiceLine>();
        List<BillingInvoiceLine> lines = billingInvoiceLineDAO.getByInvoiceId(invoiceId);
        if (lines != null) {
            for (BillingInvoiceLine line : lines) {
                if (BillingLineStatus.ACTIVE.equals(line.getLineStatus())) {
                    active.add(line);
                }
            }
        }
        return active;
    }

    private String resolveMemberNo(String patientId) {
        List<PatientPolicy> policies = patientPolicyDAO.getActiveByPatientId(patientId);
        if (policies != null) {
            for (PatientPolicy policy : policies) {
                String memberNumber = policy.getMemberNumber();
                if (memberNumber != null && !memberNumber.trim().isEmpty()) {
                    return memberNumber;
                }
            }
        }
        Patient patient = patientDAO.get(patientId).orElse(null);
        if (patient != null) {
            String externalId = patient.getExternalId();
            if (externalId != null && !externalId.trim().isEmpty()) {
                return externalId;
            }
        }
        throw new LIMSRuntimeException("No NHIS member number for patient " + patientId);
    }

    private String resolveGdrgCode(String localCode, Date serviceDate) {
        GdrgMapping resolved = resolveMapping(localCode, serviceDate);
        return resolved == null ? null : resolved.getGdrgCode();
    }

    private GdrgMapping resolveMapping(String localCode, Date serviceDate) {
        List<TariffVersion> versions = tariffVersionDAO.getActiveVersionsCovering(serviceDate);
        Integer versionId = versions == null || versions.isEmpty() ? null : versions.get(0).getId();
        List<GdrgMapping> mappings = gdrgMappingDAO.getActiveByLocalCode(localCode, serviceDate, versionId);
        if ((mappings == null || mappings.isEmpty()) && versionId != null) {
            mappings = gdrgMappingDAO.getActiveByLocalCode(localCode, serviceDate, null);
        }
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        return mappings.get(0);
    }

    /**
     * Rules CLAIM-it enforces locally, applied before we spend a round trip on
     * a payload we already know it will reject — all verified live on
     * 2026-11-01. The preauthorization rule is ours, not CLAIM-it's: it works
     * offline and can only check structure and the rules it holds locally, so
     * codes that NHIA requires preauthorization for are flagged on the mapping
     * and enforced here, or the claim fails at NHIA's final verification.
     */
    private void validateBeforeSubmit(ClaimitEncounterClaim claim, List<GdrgMapping> resolvedMappings) {
        if (claim.getMemberNo() == null || !claim.getMemberNo().matches("\\d{8}|\\d{10}")) {
            throw new LIMSRuntimeException(
                    "Member number must be 8 or 10 numbers (CLAIM-it rule: invalidMemberNo)");
        }
        if (claim.getReferralInfo() == null) {
            throw new LIMSRuntimeException(
                    "A referred-in lab claim needs referral details (CLAIM-it rule: invalidReferralInfo)");
        }
        List<ClaimitInvestigation> investigations = claim.getInvestigations();
        if (investigations == null || investigations.isEmpty()) {
            throw new LIMSRuntimeException(
                    "Diagnostic claims need at least one investigation (CLAIM-it rule: investigationEntriesRequired)");
        }
        for (ClaimitInvestigation investigation : investigations) {
            if (investigation.getGdrgCode() == null || investigation.getGdrgCode().isEmpty()) {
                throw new LIMSRuntimeException(
                        "Every investigation needs an NHIA G-DRG code (CLAIM-it rule: invalidInvestigationEntryGdrgCode)");
            }
        }
        /*
         * Preauthorization is NHIA's rule, not CLAIM-it's: an offline CLAIM-it
         * cannot know it, so it passes an MRI with no preauthorization code and
         * NHIA rejects the claim at final verification. The mapping flags the
         * investigations that need one and we refuse to submit without it.
         */
        if (resolvedMappings != null && (claim.getClaimCheckCode() == null || claim.getClaimCheckCode().isEmpty())) {
            for (GdrgMapping mapping : resolvedMappings) {
                if (mapping != null && Boolean.TRUE.equals(mapping.getRequiresPreauthorization())) {
                    throw new LIMSRuntimeException(
                            "Investigation " + mapping.getGdrgCode() + " (" + mapping.getDescription()
                                    + ") needs NHIA preauthorization before it can be claimed");
                }
            }
        }
    }

    /**
     * Persist the import report — the feedback CLAIM-it gives for a submission.
     * This is the only record of what CLAIM-it said, so it is kept per claim
     * with the raw report for audit, because NHIA's final verdict months later
     * has to be reconciled against what we actually sent.
     */
    private void recordImportReport(ClaimitImportReport report, Integer invoiceId, String recordedBy, String xml) {
        try {
            List<org.openelisglobal.billing.claim.claimit.ClaimitImportClaim> claims = report.getClaims();
            if (claims == null || claims.isEmpty()) {
                return;
            }
            for (org.openelisglobal.billing.claim.claimit.ClaimitImportClaim claim : claims) {
                ClaimitImportRecord record = new ClaimitImportRecord();
                record.setClaimId(claim.getClaimId());
                record.setInvoiceId(invoiceId);
                record.setOutcome(claim.getOutcome());
                record.setCost(claim.getCost());
                record.setInitCost(claim.getInitCost());
                record.setPassedClaims(report.getPassedClaims());
                record.setFailedClaims(report.getFailedClaims());
                record.setWarnedClaims(report.getWarnedClaims());
                record.setSavedClaims(report.getSavedClaims());
                record.setReportDate(report.getReportDate());
                record.setTimeTaken(report.getTimeTaken());
                record.setErrors(formatIssues(claim.getErrors()));
                record.setWarnings(formatIssues(claim.getWarnings()));
                record.setRawReport(xml);
                record.setSubmittedBy(recordedBy);
                record.setSubmittedAt(new Timestamp(System.currentTimeMillis()));
                claimitImportRecordDAO.insert(record);
            }
        } catch (Exception e) {
            // the claim already reached CLAIM-it; losing the audit row must not
            // turn a successful submission into a failure
            LogEvent.logError("ClaimitValidationServiceImpl", "recordImportReport", e.getMessage());
        }
    }

    private static String formatIssues(
            List<org.openelisglobal.billing.claim.claimit.ClaimitIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (org.openelisglobal.billing.claim.claimit.ClaimitIssue issue : issues) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(issue.getCode()).append(": ").append(issue.getInfo());
        }
        return sb.toString();
    }

    private String formatDate(Date date) {
        return date == null ? "" : DATE_FORMAT.format(date);
    }
}
