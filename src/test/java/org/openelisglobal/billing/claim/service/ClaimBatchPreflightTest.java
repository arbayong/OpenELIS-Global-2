package org.openelisglobal.billing.claim.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.BillingPayerDAO;
import org.openelisglobal.billing.dao.ClaimBatchDAO;
import org.openelisglobal.billing.dao.ClaimLineDAO;
import org.openelisglobal.billing.dao.FacilityCredentialingDAO;
import org.openelisglobal.billing.dao.PatientPolicyDAO;
import org.openelisglobal.billing.service.BillingConfigService;
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

@RunWith(MockitoJUnitRunner.Silent.class)
public class ClaimBatchPreflightTest {

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
    private ClaimBatchDAO claimBatchDAO;

    @Mock
    private ClaimLineDAO claimLineDAO;

    @InjectMocks
    private ClaimBatchServiceImpl service;

    private static final Integer ORG_ID = 7;

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
        BillingInvoice invoice = new BillingInvoice();
        invoice.setId(1);
        invoice.setPatientId("P-1");
        invoice.setStatus(BillingInvoiceStatus.DRAFT);
        invoice.setPayerType(PayerType.NHIS);
        invoice.setIssuedAt(Timestamp.valueOf("2026-01-15 10:00:00"));
        when(billingInvoiceDAO.getByOrganizationId(ORG_ID)).thenReturn(List.of(invoice));

        assertRejects("No eligible NHIS claim lines found for organization 7 in the selected date range");
    }

    @Test
    public void buildBatch_happyPath_createsReadyBatchWithOneClaimLine() {
        BillingInvoice invoice = new BillingInvoice();
        invoice.setId(1);
        invoice.setPatientId("P-1");
        invoice.setStatus(BillingInvoiceStatus.ISSUED);
        invoice.setPayerType(PayerType.NHIS);
        invoice.setIssuedAt(Timestamp.valueOf("2026-01-15 10:00:00"));

        BillingInvoiceLine line = new BillingInvoiceLine();
        line.setId(10);
        line.setInvoiceId(1);
        line.setLineTotal(new BigDecimal("100.00"));
        line.setLineStatus(BillingLineStatus.ACTIVE);
        line.setLocalCode("SVC");

        PatientPolicy policy = new PatientPolicy();
        policy.setPayerId(9);
        policy.setMemberNumber("M-1");

        when(billingInvoiceDAO.getByOrganizationId(ORG_ID)).thenReturn(List.of(invoice));
        when(billingInvoiceLineDAO.getByInvoiceId(1)).thenReturn(List.of(line));
        when(patientPolicyDAO.getActiveByPatientId("P-1")).thenReturn(List.of(policy));
        when(claimBatchDAO.insert(any(ClaimBatch.class))).thenReturn(100);
        when(claimLineDAO.insert(any(ClaimLine.class))).thenReturn(200);
        when(billingInvoiceLineDAO.get(10)).thenReturn(Optional.of(line));
        when(billingInvoiceLineDAO.update(any(BillingInvoiceLine.class))).thenReturn(line);

        ClaimBatch result = service.buildBatch(ORG_ID, Date.valueOf("2026-01-01"), Date.valueOf("2026-01-31"));

        assertNotNull(result);
        assertEquals(Integer.valueOf(100), result.getId());
        assertEquals(ClaimBatchStatus.READY, result.getStatus());
        assertEquals(Integer.valueOf(1), result.getTotalClaims());
        assertEquals(new BigDecimal("100.00"), result.getTotalAmount());

        verify(claimBatchDAO, times(1)).insert(any(ClaimBatch.class));
        verify(claimLineDAO, times(1)).insert(any(ClaimLine.class));
        verify(billingInvoiceLineDAO).update(line);
        assertTrue(line.getClaimLineId().equals("200"));
    }
}
