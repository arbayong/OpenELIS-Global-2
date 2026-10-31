package org.openelisglobal.billing.claim.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.claim.ClaimFeedbackItem;
import org.openelisglobal.billing.service.BillingConfigService;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.billing.valueholder.ClaimBatchStatus;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.billing.valueholder.FacilityCredentialing;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The batch export must be the NHIA v8.6 Claim-It format
 * (Batch > GeneralInformation > Patients > PatientData > Claims > Claim >
 * Treatments), as reverse-engineered from the live CLAIM-it install — see
 * AegisDiagnostics/PLANNING_NHIS_CLAIMIT_INTEGRATION.md.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class NhisBatchXmlTest {

    @Mock
    private BillingConfigService billingConfigService;

    @Mock
    private PatientDAO patientDAO;

    @InjectMocks
    private NhisXmlServiceImpl service;

    private ClaimBatch batch() {
        ClaimBatch batch = new ClaimBatch();
        batch.setId(1);
        batch.setBatchNumber("NHIS-7-20260131");
        batch.setBatchDate(Date.valueOf("2026-01-31"));
        batch.setStatus(ClaimBatchStatus.READY);
        return batch;
    }

    private FacilityCredentialing cred() {
        FacilityCredentialing cred = new FacilityCredentialing();
        cred.setAccreditationNumber("ACC-123");
        cred.setEclaimAuthorizationNumber("ECLAIM-456");
        cred.setTechnicalHeadLicenseNumber("LIC-456");
        return cred;
    }

    private ClaimLine line() {
        ClaimLine line = new ClaimLine();
        line.setId(1);
        line.setPatientId("P-1");
        line.setMemberNumber("NHIS12345678");
        line.setServiceCode("SVC");
        line.setGdrgCode("GDRG-001");
        line.setClaimedAmount(new BigDecimal("100.00"));
        line.setTotalCost(new BigDecimal("100.00"));
        line.setTariff(new BigDecimal("100.00"));
        line.setClaimIdentificationNumber("CLM-1");
        line.setServiceType("OUT");
        line.setOutcomeType("DIS");
        line.setAdmissionType("CRO");
        line.setSpecialityCode("INVE");
        line.setAdmissionDate(Date.valueOf("2026-01-15"));
        line.setOutpatientCode("ORD-1");
        line.setReferralNo("REF-9");
        return line;
    }

    private Patient patient() throws Exception {
        Patient patient = new Patient();
        patient.setEpiLastName("Doe");
        patient.setEpiFirstName("Jane");
        patient.setGender("Male");
        patient.setExternalId("MRN-1");
        // setBirthDate routes through DateUtil, whose static init needs the
        // Spring context; set the field directly in a plain unit test.
        java.lang.reflect.Field f = Patient.class.getDeclaredField("birthDate");
        f.setAccessible(true);
        f.set(patient, Timestamp.valueOf("1990-05-20 00:00:00"));
        return patient;
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private static String text(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    @Test
    public void generateClaimXml_rootIsBatch_withVersionAndBatchInformation() throws Exception {
        // patient() sets birthDate reflectively, so it throws Exception
        when(billingConfigService.getNhisXmlVersion()).thenReturn("8.6");
        when(patientDAO.get("P-1")).thenReturn(Optional.of(patient()));

        Document doc = parse(service.generateClaimXml(batch(), List.of(line()), cred()));

        assertEquals("Batch", doc.getDocumentElement().getTagName());
        assertEquals("8.6", text(doc, "XMLFormatVersion"));
        assertEquals("NHIS-7-20260131", text(doc, "BatchNumber"));
        assertEquals("100.00", text(doc, "BatchAmount"));
        assertEquals("GHC", text(doc, "BatchCurrency"));
        assertEquals("1", text(doc, "ClaimsCount"));
        assertEquals("31/01/2026", text(doc, "CreationDate"));
        assertEquals("2026", text(doc, "ServiceYear"));
        assertEquals("01", text(doc, "ServiceMonth"));
        assertEquals("ACC-123", text(doc, "ProviderAccreditationNumber"));
        assertEquals("ECLAIM-456", text(doc, "eClaimAuthorizationNumber"));
    }

    @Test
    public void generateClaimXml_patientDataAndClaimUseV86Fields() throws Exception {
        when(billingConfigService.getNhisXmlVersion()).thenReturn("8.6");
        when(patientDAO.get("P-1")).thenReturn(Optional.of(patient()));

        Document doc = parse(service.generateClaimXml(batch(), List.of(line()), cred()));

        assertEquals("Doe", text(doc, "Surname"));
        assertEquals("Jane", text(doc, "OtherName"));
        assertEquals("20/05/1990", text(doc, "DateOfBirth"));
        assertEquals("No", text(doc, "Infant"));
        assertEquals("NHIS12345678", text(doc, "MemberNumber"));
        assertEquals("MRN-1", text(doc, "HospitalRecordNumber"));
        assertEquals("M", text(doc, "Gender"));

        assertEquals("CLM-1", text(doc, "ClaimIdentificationNumber"));
        assertEquals("OUT", text(doc, "ServiceType"));
        assertEquals("DIS", text(doc, "OutcomeType"));
        assertEquals("CRO", text(doc, "AdmissionType"));
        assertEquals("INVE", text(doc, "SpecialityCode"));
        assertEquals("15/01/2026", text(doc, "AdmissionDate"));
        assertEquals("ORD-1", text(doc, "OutPatientCode"));
        assertEquals("100.00", text(doc, "TotalCost"));
        assertEquals("REF-9", text(doc, "ReferralNo"));
        assertEquals("1", text(doc, "TreatmentsCount"));
        // The treatment code must be the NHIA GDRG code, never the local code
        assertEquals("GDRG-001", text(doc, "TreatmentCode"));
        assertEquals("Investigation", text(doc, "Type"));
        assertEquals("100.00", text(doc, "Tariff"));
    }

    @Test
    public void generateClaimXml_escapesSpecialCharacters() throws Exception {
        // see rootIsBatch note on patient()
        when(billingConfigService.getNhisXmlVersion()).thenReturn("8.6");
        Patient p = patient();
        p.setEpiLastName("A&B <Co>");
        when(patientDAO.get("P-1")).thenReturn(Optional.of(p));

        String xml = service.generateClaimXml(batch(), List.of(line()), cred());

        assertTrue(xml.contains("A&amp;B &lt;Co&gt;"));
        parse(xml); // must still be well-formed
    }

    @Test
    public void generateClaimXml_zeroLines_stillProducesValidBatch() throws Exception {
        when(billingConfigService.getNhisXmlVersion()).thenReturn("8.6");

        Document doc = parse(service.generateClaimXml(batch(), List.of(), cred()));

        assertEquals("Batch", doc.getDocumentElement().getTagName());
        assertEquals("0", text(doc, "ClaimsCount"));
        assertEquals("0.00", text(doc, "BatchAmount"));
        assertEquals(0, doc.getElementsByTagName("PatientData").getLength());
    }

    @Test
    public void parseFeedback_stillReadsClaimItems() {
        when(billingConfigService.getNhisXmlVersion()).thenReturn("8.6");

        String feedback = "<?xml version=\"1.0\"?><Batch><Patients><PatientData><Claims>"
                + "<Claim id=\"1\" approvedAmount=\"50.00\" status=\"APPROVED\"/>"
                + "</Claims></PatientData></Patients></Batch>";
        List<ClaimFeedbackItem> items = service.parseFeedback(feedback);

        assertEquals(1, items.size());
        assertEquals(Integer.valueOf(1), items.get(0).getClaimLineId());
        assertEquals(new BigDecimal("50.00"), items.get(0).getApprovedAmount());
        assertNotNull(items.get(0).getStatus());
    }
}
