package org.openelisglobal.billing.claim.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@RunWith(MockitoJUnitRunner.Silent.class)
public class NhisXmlVersionTest {

    @Mock
    private BillingConfigService billingConfigService;

    @InjectMocks
    private NhisXmlServiceImpl service;

    private ClaimBatch batch() {
        ClaimBatch batch = new ClaimBatch();
        batch.setId(1);
        batch.setBatchNumber("BATCH-1");
        batch.setStatus(ClaimBatchStatus.READY);
        return batch;
    }

    private FacilityCredentialing cred() {
        FacilityCredentialing cred = new FacilityCredentialing();
        cred.setAccreditationNumber("ACC-123");
        cred.setTechnicalHeadLicenseNumber("LIC-456");
        return cred;
    }

    private ClaimLine line() {
        ClaimLine line = new ClaimLine();
        line.setId(1);
        line.setMemberNumber("M-1");
        line.setPatientId("P-1");
        line.setServiceCode("SVC");
        line.setClaimedAmount(new BigDecimal("100.00"));
        return line;
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    @Test
    public void generateClaimXml_putsConfiguredVersionOnRootElement() throws Exception {
        when(billingConfigService.getNhisXmlVersion()).thenReturn("8.6");

        String xml = service.generateClaimXml(batch(), List.of(line()), cred());

        Document doc = parse(xml);
        Element root = doc.getDocumentElement();
        assertEquals("ClaimBatch", root.getTagName());
        assertEquals("8.6", root.getAttribute("version"));
        assertEquals("BATCH-1", root.getAttribute("batchNumber"));
    }

    @Test
    public void generateClaimXml_defaultsTo8_6_whenVersionNotConfigured() throws Exception {
        String xml = service.generateClaimXml(batch(), List.of(line()), cred());

        assertEquals("8.6", parse(xml).getDocumentElement().getAttribute("version"));
    }

    @Test
    public void generateClaimXml_zeroLines_stillProducesValidVersionedDocument() throws Exception {
        when(billingConfigService.getNhisXmlVersion()).thenReturn("8.6");

        String xml = service.generateClaimXml(batch(), List.of(), cred());

        Document doc = parse(xml);
        Element root = doc.getDocumentElement();
        assertEquals("8.6", root.getAttribute("version"));
        assertNotNull(root.getElementsByTagName("Claims").item(0));
        assertEquals(0, root.getElementsByTagName("Claim").getLength());
    }

    @Test
    public void parseFeedback_readsVersionFromRoot_defaultingToConfigured() throws Exception {
        when(billingConfigService.getNhisXmlVersion()).thenReturn("8.6");

        String versioned = "<?xml version=\"1.0\"?><ClaimBatch version=\"9.0\"><Claims><Claim id=\"1\" approvedAmount=\"50.00\" status=\"APPROVED\"/></Claims></ClaimBatch>";
        List<ClaimFeedbackItem> items = service.parseFeedback(versioned);
        assertEquals(1, items.size());
        assertEquals(Integer.valueOf(1), items.get(0).getClaimLineId());

        String noVersion = "<?xml version=\"1.0\"?><ClaimBatch><Claims><Claim id=\"2\" status=\"REJECTED\"/></Claims></ClaimBatch>";
        List<ClaimFeedbackItem> items2 = service.parseFeedback(noVersion);
        assertEquals(1, items2.size());
        assertEquals(Integer.valueOf(2), items2.get(0).getClaimLineId());
    }
}
