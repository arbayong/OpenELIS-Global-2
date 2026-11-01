package org.openelisglobal.billing.claim.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.claim.claimit.ClaimitClient;
import org.openelisglobal.billing.claim.claimit.ClaimitClientException;
import org.openelisglobal.billing.claim.claimit.ClaimitEncounterClaim;
import org.openelisglobal.billing.claim.claimit.ClaimitEncounterXmlService;
import org.openelisglobal.billing.claim.claimit.ClaimitImportReport;
import org.openelisglobal.billing.claim.claimit.ClaimitReferralInfo;
import org.openelisglobal.billing.claim.service.impl.ClaimitValidationServiceImpl;
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
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ClaimitValidationServiceImplTest {

    private static final Integer INVOICE_ID = 77;
    private static final Integer LINE_ID = 9;

    @Mock
    private BillingInvoiceDAO billingInvoiceDAO;
    @Mock
    private BillingInvoiceLineDAO billingInvoiceLineDAO;
    @Mock
    private PatientPolicyDAO patientPolicyDAO;
    @Mock
    private PatientDAO patientDAO;
    @Mock
    private ClaimitSettingsDAO claimitSettingsDAO;
    @Mock
    private GatewayConfigService gatewayConfigService;
    @Mock
    private ClaimitClient claimitClient;
    @Mock
    private ClaimitEncounterXmlService claimitEncounterXmlService;
    @Mock
    private TariffVersionDAO tariffVersionDAO;
    @Mock
    private GdrgMappingDAO gdrgMappingDAO;

    @InjectMocks
    private ClaimitValidationServiceImpl service;

    private BillingInvoice invoice;
    private BillingInvoiceLine line;
    private Patient patient;
    private ClaimitSettings settings;

    @Before
    public void setUp() {
        invoice = new BillingInvoice();
        invoice.setId(INVOICE_ID);
        invoice.setPatientId("P-1");
        invoice.setIssuedAt(Timestamp.valueOf("2026-03-10 00:00:00"));

        line = new BillingInvoiceLine();
        line.setId(LINE_ID);
        line.setLocalCode("CBC");
        line.setLineTotal(BigDecimal.valueOf(50));
        line.setLineStatus(BillingLineStatus.ACTIVE);

        patient = new Patient();
        patient.setEpiLastName("Doe");
        patient.setEpiFirstName("Jane");
        patient.setEpiMiddleName("Mary");
        patient.setGender("Female");
        // CLAIM-it only checks member-number format (8 or 10 numbers), so the
        // fixture uses a well-formed 8-digit number like the live tests did
        patient.setExternalId("28492019");
        // setBirthDate routes through DateUtil, whose static init needs the
        // Spring context; set the field directly in a plain unit test.
        try {
            java.lang.reflect.Field field = Patient.class.getDeclaredField("birthDate");
            field.setAccessible(true);
            field.set(patient, Timestamp.valueOf("1990-05-20 00:00:00"));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }

        settings = new ClaimitSettings();
        settings.setId(1);
        settings.setBaseUrl("http://claimit.local/json-api");
        settings.setUsername("facility");
        settings.setPasswordEncrypted("ENC-PW");

        when(billingInvoiceDAO.get(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(billingInvoiceLineDAO.getByInvoiceId(INVOICE_ID)).thenReturn(List.of(line));
        when(claimitSettingsDAO.getAll()).thenReturn(List.of(settings));
        // without this stub the password argument is null, Mockito's anyString()
        // stops matching, and the client chain silently returns null
        when(gatewayConfigService.decryptSecret(anyString())).thenReturn("ENC-PW");
        // a default G-DRG row so the CBC line resolves to a code and passes
        // validation; individual tests override it
        GdrgMapping defaultMapping = new GdrgMapping();
        defaultMapping.setLocalCode("CBC");
        defaultMapping.setGdrgCode("INVE01D");
        defaultMapping.setRequiresPreauthorization(Boolean.FALSE);
        when(gdrgMappingDAO.getActiveByLocalCode(anyString(), any(Date.class), any()))
                .thenReturn(List.of(defaultMapping));
        when(claimitClient.authenticate(anyString(), anyString(), anyString())).thenReturn("TOKEN");
        ClaimitImportReport report = new ClaimitImportReport();
        report.setSuccess(true);
        report.setPassedClaims(1);
        report.setFailedClaims(0);
        report.setSavedClaims(1);
        when(claimitClient.submitClaims(anyString(), anyString(), anyString())).thenReturn(report);
        when(claimitEncounterXmlService.encodeEncounterXml(anyList())).thenReturn("<claims/>");
        when(patientDAO.get(anyString())).thenReturn(Optional.empty());
        when(tariffVersionDAO.getActiveVersionsCovering(any(Date.class))).thenReturn(List.of());
        when(gdrgMappingDAO.getActiveByLocalCode(anyString(), any(Date.class), anyInt()))
                .thenReturn(List.of());
    }

    private void memberFromPolicy(String memberNumber) {
        PatientPolicy policy = new PatientPolicy();
        policy.setMemberNumber(memberNumber);
        when(patientPolicyDAO.getActiveByPatientId("P-1")).thenReturn(List.of(policy));
    }

    private void memberFromPatientExternalId() {
        when(patientPolicyDAO.getActiveByPatientId("P-1")).thenReturn(List.of());
        when(patientDAO.get("P-1")).thenReturn(Optional.of(patient));
    }

    /**
     * CLAIM-it's encounter API parses dates as MM/dd/yyyy — verified live
     * 2026-11-01, when a 01/10/2026 service date appeared in the CLAIM-it claim
     * list under JAN 2026 as 10 SAT, and every day>12 date failed to parse. The
     * monthly batch spec uses DD/MM/YYYY instead, so the paths differ on purpose.
     */
    @Test
    public void validateInvoice_datesAreMonthFirst_forTheEncounterApi() {
        memberFromPatientExternalId();
        ArgumentCaptor<List<ClaimitEncounterClaim>> captor = ArgumentCaptor.forClass(List.class);

        service.validateInvoice(INVOICE_ID, "CHK-1", null, "Ref Facility", "FAC-1", "recorder");

        verify(claimitEncounterXmlService).encodeEncounterXml(captor.capture());
        ClaimitEncounterClaim claim = captor.getValue().get(0);
        // invoice issued 2026-03-10, patient born 1990-05-20
        assertEquals("03/10/2026", claim.getDateOfService());
        assertEquals("03/10/2026", claim.getInvestigations().get(0).getServiceDate());
        assertEquals("05/20/1990", claim.getDateOfBirth());
    }

    @Test
    public void validateInvoice_happyPath_buildsDiaClaimAndReturnsReport() {
        memberFromPatientExternalId();
        ArgumentCaptor<List<ClaimitEncounterClaim>> captor = ArgumentCaptor.forClass(List.class);

        ClaimitImportReport out = service.validateInvoice(INVOICE_ID, "CHK-1", "DOC-1", "Ref Facility", "FAC-1",
                "recorder");

        assertTrue(out.isSuccess());
        assertEquals(1, out.getPassedClaims());
        verify(claimitEncounterXmlService).encodeEncounterXml(captor.capture());
        ClaimitEncounterClaim claim = captor.getValue().get(0);
        assertEquals("CLM-77-9", claim.getClaimID());
        assertEquals("CHK-1", claim.getClaimCheckCode());
        assertEquals("28492019", claim.getMemberNo());
        assertEquals("28492019", claim.getCardSerialNo());
        assertEquals("Doe", claim.getSurname());
        assertEquals("Jane Mary", claim.getOtherNames());
        assertEquals("05/20/1990", claim.getDateOfBirth());
        assertEquals("F", claim.getGender());
        assertEquals("28492019", claim.getHospitalRecNo());
        assertEquals("DIA", claim.getTypeOfService());
        assertEquals("No", claim.getIsUnbundled());
        assertEquals("CFU", claim.getTypeOfAttendance());
        assertEquals("DISC", claim.getServiceOutcome());
        assertEquals("03/10/2026", claim.getDateOfService());
        assertEquals(List.of("INVE"), claim.getSpecialtyAttended());
        assertEquals("DOC-1", claim.getPhysicianID());
        assertNotNull(claim.getReferralInfo());
        assertEquals("CHK-1", claim.getReferralInfo().getClaimCheckCode());
        assertEquals("FAC-1", claim.getReferralInfo().getFacilityID());
        assertEquals("Ref Facility", claim.getReferralInfo().getFacilityName());
        assertEquals(1, claim.getInvestigations().size());
        assertEquals("03/10/2026", claim.getInvestigations().get(0).getServiceDate());
        assertEquals("INVE01D", claim.getInvestigations().get(0).getGdrgCode());
    }

    @Test
    public void validateInvoice_memberFromPolicy_usesPolicyMember() {
        memberFromPolicy("2849201911");
        ArgumentCaptor<List<ClaimitEncounterClaim>> captor = ArgumentCaptor.forClass(List.class);
        service.validateInvoice(INVOICE_ID, null, null, "Ref Facility", "FAC-1", "recorder");
        verify(claimitEncounterXmlService).encodeEncounterXml(captor.capture());
        ClaimitEncounterClaim claim = captor.getValue().get(0);
        assertEquals("2849201911", claim.getMemberNo());
        assertNull(claim.getSurname());
        assertNull(claim.getCardSerialNo());
        assertNotNull(claim.getReferralInfo());
        assertEquals("Ref Facility", claim.getReferralInfo().getFacilityName());
    }

    @Test
    public void validateInvoice_missingReferralInfo_rejects() {
        // CLAIM-it rejects a diagnostic claim with no referral details at all
        memberFromPatientExternalId();
        try {
            service.validateInvoice(INVOICE_ID, null, null, null, null, "recorder");
            throw new AssertionError("Expected LIMSRuntimeException");
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("invalidReferralInfo"));
        }
        verify(claimitEncounterXmlService, never()).encodeEncounterXml(any());
    }

    @Test
    public void validateInvoice_referralInfoWithOnlyClaimCheckCode_omitsFacilityName() {
        memberFromPatientExternalId();
        ArgumentCaptor<List<ClaimitEncounterClaim>> captor = ArgumentCaptor.forClass(List.class);
        service.validateInvoice(INVOICE_ID, "CHK-1", null, null, null, "recorder");
        verify(claimitEncounterXmlService).encodeEncounterXml(captor.capture());
        ClaimitEncounterClaim claim = captor.getValue().get(0);
        ClaimitReferralInfo referral = claim.getReferralInfo();
        assertNotNull(referral);
        assertEquals("CHK-1", referral.getClaimCheckCode());
        assertNull(referral.getFacilityName());
        assertNull(referral.getFacilityID());
    }

    @Test
    public void validateInvoice_memberNumberNotFormatted_rejects() {
        memberFromPolicy("M-1");
        try {
            service.validateInvoice(INVOICE_ID, "CHK-1", null, "Ref Facility", null, "recorder");
            throw new AssertionError("Expected LIMSRuntimeException");
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("invalidMemberNo"));
        }
        verify(claimitEncounterXmlService, never()).encodeEncounterXml(any());
    }

    @Test
    public void validateInvoice_preauthorizationRequired_rejectsWithoutClaimCheckCode() {
        // NHIA requires preauthorization for costly investigations; CLAIM-it
        // cannot know that (it works offline), so we refuse to submit without it
        memberFromPatientExternalId();
        GdrgMapping mapping = new GdrgMapping();
        mapping.setLocalCode("CBC");
        mapping.setGdrgCode("INVE33D");
        mapping.setDescription("CT Scan");
        mapping.setRequiresPreauthorization(Boolean.TRUE);
        when(gdrgMappingDAO.getActiveByLocalCode(eq("CBC"), any(Date.class), any()))
                .thenReturn(List.of(mapping));
        try {
            service.validateInvoice(INVOICE_ID, null, null, "Ref Facility", "FAC-1", "recorder");
            throw new AssertionError("Expected LIMSRuntimeException");
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("preauthorization"));
        }
        verify(claimitEncounterXmlService, never()).encodeEncounterXml(any());
    }

    @Test
    public void validateInvoice_preauthorizationRequired_isSatisfiedByTheClaimCheckCode() {
        memberFromPatientExternalId();
        GdrgMapping mapping = new GdrgMapping();
        mapping.setLocalCode("CBC");
        mapping.setGdrgCode("INVE33D");
        mapping.setRequiresPreauthorization(Boolean.TRUE);
        when(gdrgMappingDAO.getActiveByLocalCode(eq("CBC"), any(Date.class), any()))
                .thenReturn(List.of(mapping));

        ClaimitImportReport report = new ClaimitImportReport();
        report.setSuccess(true);
        when(claimitClient.submitClaims(anyString(), anyString(), anyString())).thenReturn(report);

        assertTrue(service.validateInvoice(INVOICE_ID, "AUTH-2026-001", null, "Ref Facility", "FAC-1", "recorder")
                .isSuccess());
    }

    @Test
    public void validateInvoice_genderMale_defaultsToM() {
        memberFromPatientExternalId();
        patient.setGender("Male");
        ArgumentCaptor<List<ClaimitEncounterClaim>> captor = ArgumentCaptor.forClass(List.class);
        service.validateInvoice(INVOICE_ID, "CHK-1", null, "Ref Facility", "FAC-1", "recorder");
        verify(claimitEncounterXmlService).encodeEncounterXml(captor.capture());
        assertEquals("M", captor.getValue().get(0).getGender());
    }

    @Test
    public void validateInvoice_missingInvoice_throws() {
        when(billingInvoiceDAO.get(999)).thenReturn(Optional.empty());
        try {
            service.validateInvoice(999, null, null, null, null, "recorder");
            throw new AssertionError("Expected LIMSRuntimeException");
        } catch (LIMSRuntimeException e) {
            assertEquals("Invoice not found: 999", e.getMessage());
        }
    }

    @Test
    public void validateInvoice_missingSettings_throws() {
        when(claimitSettingsDAO.getAll()).thenReturn(List.of());
        memberFromPatientExternalId();
        try {
            service.validateInvoice(INVOICE_ID, "CHK-1", null, "Ref Facility", "FAC-1", "recorder");
            throw new AssertionError("Expected LIMSRuntimeException");
        } catch (LIMSRuntimeException e) {
            assertEquals("CLAIM-it settings are not configured", e.getMessage());
        }
    }

    @Test
    public void validateInvoice_missingMemberNumber_throws() {
        when(patientPolicyDAO.getActiveByPatientId("P-1")).thenReturn(List.of(new PatientPolicy()));
        when(patientDAO.get("P-1")).thenReturn(Optional.of(patient));
        patient.setExternalId("");
        try {
            service.validateInvoice(INVOICE_ID, "CHK-1", null, "Ref Facility", "FAC-1", "recorder");
            throw new AssertionError("Expected LIMSRuntimeException");
        } catch (LIMSRuntimeException e) {
            assertEquals("No NHIS member number for patient P-1", e.getMessage());
        }
    }

    @Test
    public void validateInvoice_noActiveLines_throws() {
        when(billingInvoiceLineDAO.getByInvoiceId(INVOICE_ID)).thenReturn(List.of());
        try {
            service.validateInvoice(INVOICE_ID, "CHK-1", null, "Ref Facility", "FAC-1", "recorder");
            throw new AssertionError("Expected LIMSRuntimeException");
        } catch (LIMSRuntimeException e) {
            assertEquals("Invoice 77 has no active lines", e.getMessage());
        }
    }

    @Test
    public void validateInvoice_versionResolves_mappingUsesVersionId() {
        memberFromPolicy("2849201911");
        TariffVersion version = new TariffVersion();
        version.setId(5);
        when(tariffVersionDAO.getActiveVersionsCovering(any(Date.class))).thenReturn(List.of(version));
        GdrgMapping mapping = new GdrgMapping();
        mapping.setGdrgCode("G-1");
        when(gdrgMappingDAO.getActiveByLocalCode(eq("CBC"), any(Date.class), eq(5))).thenReturn(List.of(mapping));

        service.validateInvoice(INVOICE_ID, "CHK-1", null, "Ref Facility", "FAC-1", "recorder");

        verify(gdrgMappingDAO).getActiveByLocalCode(eq("CBC"), any(Date.class), eq(5));
        verify(gdrgMappingDAO, never()).getActiveByLocalCode(anyString(), any(Date.class), isNull());
        ArgumentCaptor<List<ClaimitEncounterClaim>> captor = ArgumentCaptor.forClass(List.class);
        verify(claimitEncounterXmlService).encodeEncounterXml(captor.capture());
        assertEquals("G-1", captor.getValue().get(0).getInvestigations().get(0).getGdrgCode());
    }

    @Test
    public void validateInvoice_versionScopedEmpty_usesUnversionedFallback() {
        memberFromPolicy("2849201911");
        TariffVersion version = new TariffVersion();
        version.setId(5);
        when(tariffVersionDAO.getActiveVersionsCovering(any(Date.class))).thenReturn(List.of(version));
        when(gdrgMappingDAO.getActiveByLocalCode(eq("CBC"), any(Date.class), eq(5))).thenReturn(List.of());
        GdrgMapping mapping = new GdrgMapping();
        mapping.setGdrgCode("G-2");
        when(gdrgMappingDAO.getActiveByLocalCode(eq("CBC"), any(Date.class), isNull())).thenReturn(List.of(mapping));

        service.validateInvoice(INVOICE_ID, "CHK-1", null, "Ref Facility", "FAC-1", "recorder");

        verify(gdrgMappingDAO).getActiveByLocalCode(eq("CBC"), any(Date.class), isNull());
        ArgumentCaptor<List<ClaimitEncounterClaim>> captor = ArgumentCaptor.forClass(List.class);
        verify(claimitEncounterXmlService).encodeEncounterXml(captor.capture());
        assertEquals("G-2", captor.getValue().get(0).getInvestigations().get(0).getGdrgCode());
    }

    @Test
    public void validateInvoice_clientException_wrapsInLimsRuntimeExceptionNamingInvoice() {
        memberFromPolicy("2849201911");
        ClaimitClientException clientException = new ClaimitClientException("down");
        when(claimitClient.submitClaims(anyString(), anyString(), anyString())).thenThrow(clientException);
        try {
            service.validateInvoice(INVOICE_ID, "CHK-1", null, "Ref Facility", "FAC-1", "recorder");
            throw new AssertionError("Expected LIMSRuntimeException");
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage().contains("invoice 77"));
            assertEquals(clientException, e.getCause());
        }
    }

    @Test
    public void saveSettings_encryptsPasswordAndInserts() {
        when(claimitSettingsDAO.getAll()).thenReturn(List.of());
        when(gatewayConfigService.encryptSecret("secret-pw")).thenReturn("ENC");

        service.saveSettings("http://x", "user", "secret-pw", "prov-id", "prov-name", "clinic", "region", "tier",
                "recorder");

        verify(gatewayConfigService).encryptSecret("secret-pw");
        ArgumentCaptor<ClaimitSettings> captor = ArgumentCaptor.forClass(ClaimitSettings.class);
        verify(claimitSettingsDAO).insert(captor.capture());
        assertEquals("ENC", captor.getValue().getPasswordEncrypted());
        assertNotNull(captor.getValue().getLastUpdated());
    }

    @Test
    public void saveSettings_blankPasswordKeepsExistingAndUpdates() {
        when(claimitSettingsDAO.getAll()).thenReturn(List.of(settings));
        when(gatewayConfigService.encryptSecret(anyString())).thenReturn("ENC");

        service.saveSettings("http://x", "user", "", "prov-id", "prov-name", "clinic", "region", "tier", "recorder");

        verify(gatewayConfigService, never()).encryptSecret(anyString());
        verify(claimitSettingsDAO).update(settings);
        assertEquals("ENC-PW", settings.getPasswordEncrypted());
    }

    @Test
    public void getSettings_neverReturnsPasswordEncrypted() {
        ClaimitSettings stored = new ClaimitSettings();
        stored.setId(1);
        stored.setBaseUrl("http://x");
        stored.setUsername("user");
        stored.setPasswordEncrypted("SECRET");
        stored.setProviderId("p");
        when(claimitSettingsDAO.getAll()).thenReturn(List.of(stored));

        ClaimitSettings out = service.getSettings();
        assertEquals("http://x", out.getBaseUrl());
        assertEquals("user", out.getUsername());
        assertNull(out.getPasswordEncrypted());
    }

    private void assertNotNull(Object value) {
        if (value == null) {
            throw new AssertionError("Expected non-null value");
        }
    }
}
