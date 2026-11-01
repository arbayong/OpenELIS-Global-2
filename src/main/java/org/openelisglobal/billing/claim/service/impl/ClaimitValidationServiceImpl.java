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
import org.openelisglobal.billing.dao.ClaimitSettingsDAO;
import org.openelisglobal.billing.dao.GdrgMappingDAO;
import org.openelisglobal.billing.dao.PatientPolicyDAO;
import org.openelisglobal.billing.dao.TariffVersionDAO;
import org.openelisglobal.billing.gateway.GatewayConfigService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingLineStatus;
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

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

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
        for (BillingInvoiceLine line : activeLines) {
            ClaimitInvestigation investigation = new ClaimitInvestigation();
            investigation.setServiceDate(dateOfService);
            investigation.setGdrgCode(resolveGdrgCode(line.getLocalCode(), serviceDate));
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
        List<TariffVersion> versions = tariffVersionDAO.getActiveVersionsCovering(serviceDate);
        Integer versionId = versions == null || versions.isEmpty() ? null : versions.get(0).getId();
        List<GdrgMapping> mappings = gdrgMappingDAO.getActiveByLocalCode(localCode, serviceDate, versionId);
        if ((mappings == null || mappings.isEmpty()) && versionId != null) {
            mappings = gdrgMappingDAO.getActiveByLocalCode(localCode, serviceDate, null);
        }
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        return mappings.get(0).getGdrgCode();
    }

    private String formatDate(Date date) {
        return date == null ? "" : DATE_FORMAT.format(date);
    }
}
