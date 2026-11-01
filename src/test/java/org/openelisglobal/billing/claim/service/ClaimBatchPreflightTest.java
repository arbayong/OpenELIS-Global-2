package org.openelisglobal.billing.claim.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.BillingPayerDAO;
import org.openelisglobal.billing.dao.ClaimBatchDAO;
import org.openelisglobal.billing.dao.ClaimLineDAO;
import org.openelisglobal.billing.dao.FacilityCredentialingDAO;
import org.openelisglobal.billing.dao.GdrgMappingDAO;
import org.openelisglobal.billing.dao.PatientPolicyDAO;
import org.openelisglobal.billing.dao.TariffVersionDAO;
import org.openelisglobal.billing.service.BillingConfigService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingInvoiceStatus;
import org.openelisglobal.billing.valueholder.BillingLineStatus;
import org.openelisglobal.billing.valueholder.BillingPayer;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.billing.valueholder.ClaimBatchStatus;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.billing.valueholder.FacilityCredentialing;
import org.openelisglobal.billing.valueholder.GdrgMapping;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.openelisglobal.billing.valueholder.PayerType;
import org.openelisglobal.billing.valueholder.TariffVersion;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Batch pre-flight must stop every claim the NHIA portal would reject, and the
 * DIA (diagnostic-centre) shape must come out of the claim build.
 *
 * <p>
 * Rules that depend on data (203, 224, 238, 111) are covered through
 * {@link ClaimBatchServiceImpl#buildBatch}; rules the builder always satisfies
 * itself (212, 214, 271) are covered directly through {@code validateClaimLines},
 * which is package-private for exactly that reason.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ClaimBatchPreflightTest {

    private static final Integer ORG_ID = 7;
    private static final String NHIS_MEMBER = "NHIS-1234";

    @Mock
    private BillingConfigService billingConfigService;

    @Mock
    private BillingPayerDAO billingPayerDAO;

    @Mock
    private FacilityCredentialingDAO facilityCredentialingDAO;

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
    private ClaimBatchDAO claimBatchDAO;

    @Mock
    private ClaimLineDAO claimLineDAO;

    @InjectMocks
    private ClaimBatchServiceImpl service;

    private BillingPayer nhisPayer() {
        BillingPayer payer = new BillingPayer();
        payer.setId(9);
        payer.setName("NHIS");
        payer.setType("nhis");
        payer.setIsActive("Y");
        return payer;
    }

    private FacilityCredentialing validCredential() {
        FacilityCredentialing cred = new FacilityCredentialing();
        cred.setIsActive("Y");
        cred.setAccreditationNumber("ACC-1");
        cred.setTechnicalHeadLicenseNumber("LIC-1");
        return cred;
    }

    @Before
    public void setUp() {
        when(billingConfigService.isNhisEnabled()).thenReturn(true);
        when(billingPayerDAO.getActiveByType("nhis")).thenReturn(List.of(nhisPayer()));
        when(facilityCredentialingDAO.getByOrganizationId(ORG_ID)).thenReturn(List.of(validCredential()));
    }

    private BillingInvoice eligibleInvoice(String patientId) {
        BillingInvoice invoice = new BillingInvoice();
        invoice.setId(1);
        invoice.setPatientId(patientId);
        invoice.setStatus(BillingInvoiceStatus.ISSUED);
        invoice.setPayerType(PayerType.NHIS);
        invoice.setIssuedAt(Timestamp.valueOf("2026-01-15 10:00:00"));
        return invoice;
    }

    private BillingInvoiceLine eligibleLine(String localCode, BigDecimal total) {
        BillingInvoiceLine line = new BillingInvoiceLine();
        line.setId(10);
        line.setInvoiceId(1);
        line.setLineTotal(total);
        line.setUnitPrice(total);
        line.setLineStatus(BillingLineStatus.ACTIVE);
        line.setLocalCode(localCode);
        return line;
    }

    private PatientPolicy policyFor(String memberNumber) {
        PatientPolicy policy = new PatientPolicy();
        policy.setPayerId(9);
        policy.setMemberNumber(memberNumber);
        return policy;
    }

    private GdrgMapping mapping(String localCode, String gdrgCode, BigDecimal tariff) {
        GdrgMapping mapping = new GdrgMapping();
        mapping.setLocalCode(localCode);
        mapping.setGdrgCode(gdrgCode);
        mapping.setTariff(tariff);
        return mapping;
    }

    /** Wire one eligible invoice line and return it, so tests can assert on it. */
    private BillingInvoiceLine stageOneEligibleLine(String localCode, BigDecimal lineTotal, String memberNumber,
            GdrgMapping mapped) {
        BillingInvoiceLine line = eligibleLine(localCode, lineTotal);
        when(billingInvoiceDAO.getByOrganizationId(ORG_ID)).thenReturn(List.of(eligibleInvoice("P-1")));
        when(billingInvoiceLineDAO.getByInvoiceId(1)).thenReturn(List.of(line));
        when(patientPolicyDAO.getActiveByPatientId("P-1")).thenReturn(List.of(policyFor(memberNumber)));
        when(claimBatchDAO.insert(any(ClaimBatch.class))).thenReturn(100);
        when(claimLineDAO.insert(any(ClaimLine.class))).thenReturn(200);
        when(billingInvoiceLineDAO.get(10)).thenReturn(Optional.of(line));
        when(billingInvoiceLineDAO.update(any(BillingInvoiceLine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tariffVersionDAO.getActiveVersionsCovering(any())).thenReturn(List.of());
        when(gdrgMappingDAO.getActiveByLocalCode(any(), any(), any()))
                .thenReturn(mapped == null ? List.of() : List.of(mapped));
        return line;
    }

    private void assertRejects(String expectedMessage) {
        try {
            service.buildBatch(ORG_ID, null, null);
        } catch (LIMSRuntimeException e) {
            assertEquals(expectedMessage, e.getMessage());
            verify(claimBatchDAO, never()).insert(any());
            return;
        }
        throw new AssertionError("Expected LIMSRuntimeException: " + expectedMessage);
    }

    /* ---------------- facility / configuration preconditions ---------------- */

    @Test
    public void buildBatch_nhisDisabled_rejects() {
        when(billingConfigService.isNhisEnabled()).thenReturn(false);
        assertRejects("NHIS claims are disabled (nhis.enabled is false)");
    }

    @Test
    public void buildBatch_noActiveNhisPayer_rejects() {
        when(billingPayerDAO.getActiveByType("nhis")).thenReturn(List.of());
        assertRejects("No active NHIS payer configured");
    }

    @Test
    public void buildBatch_noActiveFacilityCredentialing_rejects() {
        when(facilityCredentialingDAO.getByOrganizationId(ORG_ID)).thenReturn(List.of());
        assertRejects("No active facility credentialing for organization: 7");
    }

    @Test
    public void buildBatch_credentialMissingAccreditationNumber_rejects() {
        FacilityCredentialing cred = new FacilityCredentialing();
        cred.setIsActive("Y");
        cred.setAccreditationNumber(null);
        cred.setTechnicalHeadLicenseNumber("LIC-1");
        when(facilityCredentialingDAO.getByOrganizationId(ORG_ID)).thenReturn(List.of(cred));
        assertRejects("Facility credentialing is missing accreditation number");
    }

    @Test
    public void buildBatch_credentialMissingTechnicalHeadLicenceNumber_rejects() {
        FacilityCredentialing cred = new FacilityCredentialing();
        cred.setIsActive("Y");
        cred.setAccreditationNumber("ACC-1");
        cred.setTechnicalHeadLicenseNumber(null);
        when(facilityCredentialingDAO.getByOrganizationId(ORG_ID)).thenReturn(List.of(cred));
        assertRejects("Facility credentialing is missing technical head licence number");
    }

    @Test
    public void buildBatch_zeroEligibleClaimLines_rejects() {
        when(billingInvoiceDAO.getByOrganizationId(ORG_ID)).thenReturn(List.of(eligibleInvoice("P-1")));
        assertRejects("No eligible NHIS claim lines found for organization 7 in the selected date range");
    }

    /* --------------------- data rules reached via buildBatch --------------------- */

    @Test
    public void buildBatch_memberNumberShorterThan8_rejectsWithNhia203() {
        stageOneEligibleLine("SVC", new BigDecimal("100.00"), "M-1", mapping("SVC", "GDRG-001", new BigDecimal("100.00")));
        assertRejects("NHIA 203: member number is missing or shorter than 8 characters on claim CLM-10");
    }

    @Test
    public void buildBatch_missingMemberNumber_isNotAnEligibleLine() {
        // a policy with no member number never becomes a claim line, so the
        // batch reports eligibility rather than error 203 on a line that does
        // not exist; 203 itself is covered directly in validateClaimLines.
        stageOneEligibleLine("SVC", new BigDecimal("100.00"), null,
                mapping("SVC", "GDRG-001", new BigDecimal("100.00")));
        assertRejects("No eligible NHIS claim lines found for organization 7 in the selected date range");
    }

    @Test
    public void buildBatch_missingGdrgMapping_rejectsWithNhia224() {
        stageOneEligibleLine("SVC-UNMAPPED", new BigDecimal("100.00"), NHIS_MEMBER, null);
        assertRejects("NHIA 224: no GDRG mapping for local code SVC-UNMAPPED — add a row to billing_gdrg_mapping (claim CLM-10)");
    }

    @Test
    public void buildBatch_zeroTotalCost_rejectsWithNhia111() {
        stageOneEligibleLine("SVC", BigDecimal.ZERO, NHIS_MEMBER, mapping("SVC", "GDRG-001", BigDecimal.ZERO));
        assertRejects("NHIA 111: claim CLM-10 has a zero total cost");
    }

    /* ---------------------- the happy path and the DIA shape ---------------------- */

    @Test
    public void buildBatch_happyPath_createsReadyBatchWithOneDiaClaimLine() {
        BillingInvoiceLine staged = stageOneEligibleLine("SVC", new BigDecimal("100.00"), NHIS_MEMBER,
                mapping("SVC", "GDRG-001", new BigDecimal("100.00")));

        ClaimBatch result = service.buildBatch(ORG_ID, Date.valueOf("2026-01-01"), Date.valueOf("2026-01-31"));

        assertNotNull(result);
        assertEquals(Integer.valueOf(100), result.getId());
        assertEquals(ClaimBatchStatus.READY, result.getStatus());
        assertEquals(Integer.valueOf(1), result.getTotalClaims());
        assertEquals(new BigDecimal("100.00"), result.getTotalAmount());

        verify(claimBatchDAO, times(1)).insert(any(ClaimBatch.class));
        verify(claimLineDAO, times(1)).insert(any(ClaimLine.class));
        verify(billingInvoiceLineDAO).update(any(BillingInvoiceLine.class));
        assertEquals("200", staged.getClaimLineId());

        ArgumentCaptor<ClaimLine> captor = ArgumentCaptor.forClass(ClaimLine.class);
        verify(claimLineDAO).insert(captor.capture());
        ClaimLine built = captor.getValue();

        // DIA claim-level fields, as the spec requires for a diagnostic centre
        assertEquals("DIA", built.getServiceType());
        assertEquals("DIS", built.getOutcomeType());
        assertEquals("CRO", built.getAdmissionType());
        assertEquals("INVE", built.getSpecialityCode());
        assertEquals("GDRG-001", built.getGdrgCode());
        assertEquals("CLM-10", built.getClaimIdentificationNumber());
        assertNotNull(built.getAdmissionDate());
        // a diagnostic claim carries no ICD code, no medicine and no spell duration
        assertNull(built.getIcdCode());
        // amount claimed is the NHIA tariff, which also satisfies error 238
        assertEquals(new BigDecimal("100.00"), built.getTotalCost());
        assertEquals(new BigDecimal("100.00"), built.getTariff());
    }

    @Test
    public void buildBatch_claimsTheNhisTariff_notTheInvoicePrice() {
        // our price list bills 100, the NHIA schedule for this code is 80:
        // the claim must ask NHIA for 80, not 100 (error 238 enforces equality)
        stageOneEligibleLine("SVC", new BigDecimal("100.00"), NHIS_MEMBER,
                mapping("SVC", "GDRG-001", new BigDecimal("80.00")));

        service.buildBatch(ORG_ID, Date.valueOf("2026-01-01"), Date.valueOf("2026-01-31"));

        ArgumentCaptor<ClaimLine> captor = ArgumentCaptor.forClass(ClaimLine.class);
        verify(claimLineDAO).insert(captor.capture());
        assertEquals(new BigDecimal("80.00"), captor.getValue().getTotalCost());
        assertEquals(new BigDecimal("80.00"), captor.getValue().getTariff());
    }

    @Test
    public void buildBatch_usesMappingFromTheVersionCoveringTheServiceDate() {
        TariffVersion version = new TariffVersion();
        version.setId(5);
        version.setVersion("2026.1");
        when(tariffVersionDAO.getActiveVersionsCovering(any())).thenReturn(List.of(version));
        when(billingInvoiceDAO.getByOrganizationId(ORG_ID)).thenReturn(List.of(eligibleInvoice("P-1")));
        when(billingInvoiceLineDAO.getByInvoiceId(1))
                .thenReturn(List.of(eligibleLine("SVC", new BigDecimal("60.00"))));
        when(patientPolicyDAO.getActiveByPatientId("P-1")).thenReturn(List.of(policyFor(NHIS_MEMBER)));
        when(claimBatchDAO.insert(any(ClaimBatch.class))).thenReturn(100);
        when(claimLineDAO.insert(any(ClaimLine.class))).thenReturn(200);
        when(billingInvoiceLineDAO.get(10)).thenReturn(Optional.empty());
        when(billingInvoiceLineDAO.update(any(BillingInvoiceLine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // only the version-scoped call resolves; the any-version fallback must
        // not be used. Mockito honours the LAST matching stub, so the general
        // stub is registered first and the version-scoped one last.
        when(gdrgMappingDAO.getActiveByLocalCode(any(), any(), any()))
                .thenReturn(List.of(mapping("SVC", "GDRG-STALE", new BigDecimal("999.00"))));
        when(gdrgMappingDAO.getActiveByLocalCode(any(), any(), eq(5)))
                .thenReturn(List.of(mapping("SVC", "GDRG-2026-1", new BigDecimal("60.00"))));

        service.buildBatch(ORG_ID, Date.valueOf("2026-01-01"), Date.valueOf("2026-01-31"));

        ArgumentCaptor<ClaimLine> captor = ArgumentCaptor.forClass(ClaimLine.class);
        verify(claimLineDAO).insert(captor.capture());
        assertEquals("GDRG-2026-1", captor.getValue().getGdrgCode());
        assertEquals(new BigDecimal("60.00"), captor.getValue().getTotalCost());
        verify(gdrgMappingDAO, never()).getActiveByLocalCode(any(), any(), isNull());
    }

    @Test
    public void buildBatch_fallsBackToAnyVersion_whenScheduleHasNoRowForTheCode() {
        TariffVersion version = new TariffVersion();
        version.setId(5);
        when(tariffVersionDAO.getActiveVersionsCovering(any())).thenReturn(List.of(version));
        when(billingInvoiceDAO.getByOrganizationId(ORG_ID)).thenReturn(List.of(eligibleInvoice("P-1")));
        when(billingInvoiceLineDAO.getByInvoiceId(1))
                .thenReturn(List.of(eligibleLine("RARE", new BigDecimal("40.00"))));
        when(patientPolicyDAO.getActiveByPatientId("P-1")).thenReturn(List.of(policyFor(NHIS_MEMBER)));
        when(claimBatchDAO.insert(any(ClaimBatch.class))).thenReturn(100);
        when(claimLineDAO.insert(any(ClaimLine.class))).thenReturn(200);
        when(billingInvoiceLineDAO.get(10)).thenReturn(Optional.empty());
        when(billingInvoiceLineDAO.update(any(BillingInvoiceLine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(gdrgMappingDAO.getActiveByLocalCode(any(), any(), eq(5))).thenReturn(List.of());
        when(gdrgMappingDAO.getActiveByLocalCode(any(), any(), isNull()))
                .thenReturn(List.of(mapping("RARE", "GDRG-LEGACY", new BigDecimal("40.00"))));

        service.buildBatch(ORG_ID, Date.valueOf("2026-01-01"), Date.valueOf("2026-01-31"));

        ArgumentCaptor<ClaimLine> captor = ArgumentCaptor.forClass(ClaimLine.class);
        verify(claimLineDAO).insert(captor.capture());
        assertEquals("GDRG-LEGACY", captor.getValue().getGdrgCode());
    }

    /* ------------- rules the builder always satisfies, tested directly ------------- */

    private ClaimLine validClaimLine() {
        ClaimLine line = new ClaimLine();
        line.setId(1);
        line.setClaimIdentificationNumber("CLM-1");
        line.setMemberNumber(NHIS_MEMBER);
        line.setServiceType("DIA");
        line.setOutcomeType("DIS");
        line.setAdmissionType("CRO");
        line.setSpecialityCode("INVE");
        line.setAdmissionDate(Date.valueOf("2026-01-15"));
        line.setGdrgCode("GDRG-001");
        line.setTotalCost(new BigDecimal("23.41"));
        line.setTariff(new BigDecimal("23.41"));
        return line;
    }

    @Test
    public void validateClaimLines_acceptsAValidDiagnosticClaim() {
        assertNull(service.validateClaimLines(List.of(validClaimLine())));
    }

    @Test
    public void validateClaimLines_admissionTypeOffAllowList_rejectsWithNhia212() {
        ClaimLine line = validClaimLine();
        line.setAdmissionType("XYZ");
        String message = service.validateClaimLines(List.of(line));
        assertTrue(message, message.startsWith("NHIA 212: admission type"));
    }

    @Test
    public void validateClaimLines_specialityOffAllowList_rejectsWithNhia271() {
        ClaimLine line = validClaimLine();
        line.setSpecialityCode("LAB1");
        String message = service.validateClaimLines(List.of(line));
        assertTrue(message, message.startsWith("NHIA 271: speciality code"));
    }

    @Test
    public void validateClaimLines_missingAdmissionDate_rejectsWithNhia214() {
        ClaimLine line = validClaimLine();
        line.setAdmissionDate(null);
        String message = service.validateClaimLines(List.of(line));
        assertTrue(message, message.startsWith("NHIA 214: admission date is missing"));
    }

    @Test
    public void validateClaimLines_tariffDiffersFromTotalCost_rejectsWithNhia238() {
        ClaimLine line = validClaimLine();
        line.setTariff(new BigDecimal("20.00"));
        String message = service.validateClaimLines(List.of(line));
        assertTrue(message, message.startsWith("NHIA 238: total cost"));
    }

    @Test
    public void validateClaimLines_unmappedCode_rejectsWithNhia224() {
        ClaimLine line = validClaimLine();
        line.setGdrgCode(null);
        String message = service.validateClaimLines(List.of(line));
        assertTrue(message, message.contains("NHIA 224"));
    }

    @Test
    public void validateClaimLines_emptyList_isAccepted() {
        assertNull(service.validateClaimLines(List.of()));
    }

    @Test
    public void validateClaimLines_reportsTheFirstFailingClaim_notTheWholeBatch() {
        ClaimLine bad = validClaimLine();
        bad.setClaimIdentificationNumber("CLM-9");
        bad.setMemberNumber("SHORT");
        String message = service.validateClaimLines(List.of(validClaimLine(), bad));
        assertTrue(message, message.endsWith("claim CLM-9"));
    }

    @Test
    public void validateClaimLines_zeroTariffDoesNotTrigger238() {
        // 238 only compares a non-zero tariff against the total; a zero tariff
        // with a non-zero total is a different data problem (111 covers zero).
        ClaimLine line = validClaimLine();
        line.setTariff(BigDecimal.ZERO);
        String message = service.validateClaimLines(List.of(line));
        assertFalse(message != null && message.contains("238"));
    }
}
