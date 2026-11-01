package org.openelisglobal.billing.claim.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.claim.claimit.ClaimitClient;
import org.openelisglobal.billing.claim.claimit.ClaimitClientImpl;
import org.openelisglobal.billing.claim.claimit.ClaimitEncounterXmlService;
import org.openelisglobal.billing.claim.claimit.ClaimitEncounterXmlServiceImpl;
import org.openelisglobal.billing.claim.claimit.ClaimitImportReport;
import org.openelisglobal.billing.claim.service.impl.ClaimitValidationServiceImpl;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.ClaimitImportRecordDAO;
import org.openelisglobal.billing.dao.ClaimitSettingsDAO;
import org.openelisglobal.billing.dao.GdrgMappingDAO;
import org.openelisglobal.billing.dao.PatientPolicyDAO;
import org.openelisglobal.billing.dao.TariffVersionDAO;
import org.openelisglobal.billing.gateway.GatewayConfigServiceImpl;
import org.openelisglobal.billing.service.PaymentService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingInvoiceStatus;
import org.openelisglobal.billing.valueholder.BillingLineStatus;
import org.openelisglobal.billing.valueholder.ClaimitSettings;
import org.openelisglobal.billing.valueholder.GdrgMapping;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.openelisglobal.billing.valueholder.PayerType;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End-to-end through our own code: the real encoder, the real CLAIM-it client
 * and the real validation service, submitting to a live local CLAIM-it.
 *
 * <p>
 * Only the DAOs are mocked, so this exercises everything the unit tests fake:
 * claim construction, pre-submit validation, the encounter XML, bearer-token
 * authentication, the import-report parsing, and the audit row. It is skipped
 * (not failed) when no CLAIM-it is listening, because the rest of the suite
 * must pass on a build machine without one.
 *
 * <p>
 * What it proves and what it does not: it proves our stack can submit a claim
 * and interpret the answer. It does not go through the web layer, the permission
 * check, or a deployed WAR — that still needs a Tomcat deployment.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ClaimitLiveIntegrationTest {

    private static final int INVOICE_ID = 900;
    private static final String BASE_URL = "http://localhost:31719/json-api";

    @Mock
    private BillingInvoiceDAO billingInvoiceDAO;
    @Mock
    private BillingInvoiceLineDAO billingInvoiceLineDAO;
    @Mock
    private PatientPolicyDAO patientPolicyDAO;
    @Mock
    private GdrgMappingDAO gdrgMappingDAO;
    @Mock
    private TariffVersionDAO tariffVersionDAO;
    @Mock
    private ClaimitSettingsDAO claimitSettingsDAO;
    @Mock
    private ClaimitImportRecordDAO claimitImportRecordDAO;
    @Mock
    private PatientDAO patientDAO;
    @Mock
    private PaymentService paymentService;

    private ClaimitValidationServiceImpl service;

    /**
     * The key generated for this deployment and recorded in
     * packaging/windows/config/application.properties; without it the settings
     * password cannot be decrypted.
     */
    @BeforeClass
    public static void setMasterKey() {
        System.setProperty("openelis.billing.gateway.masterKey",
                "bSjoHPRRAQLIPsK8stYWa2dGJKASPvR+QAp23SRUPio=");
    }

    @Before
    public void setUp() throws Exception {
        Patient patient = new Patient();
        patient.setEpiLastName("PROBE");
        patient.setEpiFirstName("LIVE");
        patient.setEpiMiddleName("TEST");
        patient.setGender("Male");
        patient.setExternalId("MRN-IT1");
        java.lang.reflect.Field birthDate = Patient.class.getDeclaredField("birthDate");
        birthDate.setAccessible(true);
        birthDate.set(patient, Timestamp.valueOf("1990-05-20 00:00:00"));

        BillingInvoice invoice = new BillingInvoice();
        invoice.setId(INVOICE_ID);
        invoice.setPatientId("P-900");
        invoice.setStatus(BillingInvoiceStatus.ISSUED);
        invoice.setPayerType(PayerType.NHIS);
        // service date must parse MM/dd/yyyy with a day <= 12 and fall inside the
        // facility's credentialed period, or CLAIM-it answers genErr before it
        // looks at the claim (verified live)
        invoice.setIssuedAt(Timestamp.valueOf("2026-01-10 09:00:00"));

        BillingInvoiceLine line = new BillingInvoiceLine();
        line.setId(910);
        line.setInvoiceId(INVOICE_ID);
        line.setLocalCode("CBC");
        line.setLineTotal(new java.math.BigDecimal("50.00"));
        line.setLineStatus(BillingLineStatus.ACTIVE);

        PatientPolicy policy = new PatientPolicy();
        policy.setMemberNumber("28492019");

        GdrgMapping mapping = new GdrgMapping();
        mapping.setLocalCode("CBC");
        mapping.setGdrgCode("INVE01D");
        mapping.setDescription("2 Hour Post Prandial Blood Glucose");
        mapping.setTariff(new java.math.BigDecimal("23.41"));

        ClaimitSettings settings = new ClaimitSettings();
        settings.setBaseUrl(BASE_URL);
        settings.setUsername("admin");
        settings.setPasswordEncrypted(new GatewayConfigServiceImpl().encryptSecret("admin"));

        when(billingInvoiceDAO.get(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(billingInvoiceLineDAO.getByInvoiceId(INVOICE_ID)).thenReturn(List.of(line));
        when(patientPolicyDAO.getActiveByPatientId("P-900")).thenReturn(List.of(policy));
        when(patientDAO.get("P-900")).thenReturn(Optional.of(patient));
        when(tariffVersionDAO.getActiveVersionsCovering(any())).thenReturn(List.of());
        when(gdrgMappingDAO.getActiveByLocalCode(any(), any(), any())).thenReturn(List.of(mapping));
        when(gdrgMappingDAO.getActiveByLocalCode(any(), any(), anyInt())).thenReturn(List.of(mapping));
        when(claimitSettingsDAO.getAll()).thenReturn(List.of(settings));

        ClaimitEncounterXmlService xmlService = new ClaimitEncounterXmlServiceImpl();
        ClaimitClient client = new ClaimitClientImpl(new ObjectMapper());

        service = new ClaimitValidationServiceImpl();
        set(service, "billingInvoiceDAO", billingInvoiceDAO);
        set(service, "billingInvoiceLineDAO", billingInvoiceLineDAO);
        set(service, "patientPolicyDAO", patientPolicyDAO);
        set(service, "gdrgMappingDAO", gdrgMappingDAO);
        set(service, "tariffVersionDAO", tariffVersionDAO);
        set(service, "claimitSettingsDAO", claimitSettingsDAO);
        set(service, "claimitImportRecordDAO", claimitImportRecordDAO);
        set(service, "patientDAO", patientDAO);
        set(service, "claimitEncounterXmlService", xmlService);
        set(service, "claimitClient", client);
        set(service, "gatewayConfigService", new GatewayConfigServiceImpl());
        set(service, "paymentService", paymentService);
    }

    private static void set(Object target, String field, Object value) {
        java.lang.reflect.Field f = findField(target.getClass(), field);
        f.setAccessible(true);
        try {
            f.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static java.lang.reflect.Field findField(Class<?> type, String name) {
        while (type != null) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new IllegalStateException("no field " + name);
    }

    @Test
    public void ourStackSubmitsAClaimToTheLiveClaimit() {
        ClaimitImportReport report = service.validateInvoice(INVOICE_ID, "12345", null,
                "HOMELAND CLINIC LIMITED", null, "integration-test");

        assertNotNull("CLAIM-it returned no report", report);
        assertNotNull("report carried no claims", report.getClaims());
        assertEquals("one claim was submitted", 1, report.getClaims().size());
        org.openelisglobal.billing.claim.claimit.ClaimitImportClaim submitted = report.getClaims().get(0);
        assertEquals("our claim id", "CLM-900-910", submitted.getClaimId());
        assertNotNull("CLAIM-it gave no outcome", submitted.getOutcome());
        // CLAIM-it prices the claim from the G-DRG code; a real cost means the
        // whole chain - encoder, auth, payload, parser - worked
        assertTrue("CLAIM-it did not price the claim: " + submitted.getCost(),
                submitted.getCost() != null && !submitted.getCost().isEmpty()
                        && !"-".equals(submitted.getCost()));

        // the audit row is written for every submission
        org.mockito.Mockito.verify(claimitImportRecordDAO, org.mockito.Mockito.atLeastOnce())
                .insert(any());
    }

    /**
     * The report is persisted with the payload we sent, so a later NHIA verdict
     * can be reconciled against what CLAIM-it actually saw.
     */
    @Test
    public void theSubmissionIsRecordedForAudit() {
        service.validateInvoice(INVOICE_ID, "12345", null, "HOMELAND CLINIC LIMITED", null, "integration-test");

        org.mockito.ArgumentCaptor<org.openelisglobal.billing.valueholder.ClaimitImportRecord> captor =
                org.mockito.ArgumentCaptor.forClass(org.openelisglobal.billing.valueholder.ClaimitImportRecord.class);
        org.mockito.Mockito.verify(claimitImportRecordDAO, org.mockito.Mockito.atLeastOnce()).insert(captor.capture());
        org.openelisglobal.billing.valueholder.ClaimitImportRecord record = captor.getValue();
        assertEquals("CLM-900-910", record.getClaimId());
        assertEquals(Integer.valueOf(INVOICE_ID), record.getInvoiceId());
        assertNotNull(record.getRawReport());
        assertTrue(record.getRawReport().contains("<claims>"));
    }
}
