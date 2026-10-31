package org.openelisglobal.billing.claim.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.openelisglobal.billing.claim.ClaimFeedbackItem;
import org.openelisglobal.billing.service.BillingConfigService;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.billing.valueholder.FacilityCredentialing;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Service
@Transactional
public class NhisXmlServiceImpl implements NhisXmlService {

    @Autowired
    private BillingConfigService billingConfigService;

    private String nhisXmlVersion() {
        String version = billingConfigService.getNhisXmlVersion();
        return version == null || version.isEmpty() ? BillingConfigService.DEFAULT_NHIS_XML_VERSION : version;
    }

    @Override
    public String generateClaimXml(ClaimBatch batch, List<ClaimLine> lines, FacilityCredentialing cred) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("ClaimBatch");
            root.setAttribute("version", nhisXmlVersion());
            root.setAttribute("batchNumber", batch.getBatchNumber() == null ? "" : batch.getBatchNumber());
            root.setAttribute("providerAccreditationNumber", cred == null || cred.getAccreditationNumber() == null ? "" : cred.getAccreditationNumber());
            root.setAttribute("providerLicense", cred == null || cred.getTechnicalHeadLicenseNumber() == null ? "" : cred.getTechnicalHeadLicenseNumber());
            root.setAttribute("submissionDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            doc.appendChild(root);

            Element claims = doc.createElement("Claims");
            root.appendChild(claims);

            if (lines != null) {
                for (ClaimLine line : lines) {
                    Element claim = doc.createElement("Claim");
                    claim.setAttribute("id", line.getId() == null ? "" : String.valueOf(line.getId()));
                    claim.setAttribute("memberNumber", line.getMemberNumber() == null ? "" : line.getMemberNumber());
                    claim.setAttribute("patientId", line.getPatientId() == null ? "" : line.getPatientId());
                    claim.setAttribute("serviceCode", line.getServiceCode() == null ? "" : line.getServiceCode());
                    claim.setAttribute("claimedAmount", line.getClaimedAmount() == null ? "0.00" : line.getClaimedAmount().toString());
                    claims.appendChild(claim);
                }
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    public List<ClaimFeedbackItem> parseFeedback(String xml) {
        List<ClaimFeedbackItem> items = new ArrayList<>();
        try {
            if (xml == null || xml.trim().isEmpty()) {
                return items;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            String version = root.getAttribute("version");
            if (version == null || version.isEmpty()) {
                version = nhisXmlVersion();
            }

            NodeList claims = doc.getElementsByTagName("Claim");
            for (int i = 0; i < claims.getLength(); i++) {
                Node node = claims.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) node;

                String id = element.getAttribute("id");
                if ((id == null || id.isEmpty()) && getChild(element, "id") != null) {
                    id = getChild(element, "id").getTextContent();
                }
                if (id == null || id.trim().isEmpty()) {
                    continue;
                }

                Integer claimLineId;
                try {
                    claimLineId = Integer.parseInt(id.trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                String approvedAmountAttr = element.getAttribute("approvedAmount");
                BigDecimal approvedAmount = null;
                if (approvedAmountAttr == null || approvedAmountAttr.isEmpty()) {
                    Element amtEl = getChild(element, "approvedAmount");
                    if (amtEl != null && amtEl.getTextContent() != null) {
                        approvedAmountAttr = amtEl.getTextContent().trim();
                    }
                }
                if (approvedAmountAttr != null && !approvedAmountAttr.isEmpty()) {
                    try {
                        approvedAmount = new BigDecimal(approvedAmountAttr);
                    } catch (NumberFormatException e) {
                        approvedAmount = null;
                    }
                }

                String status = element.getAttribute("status");
                if ((status == null || status.isEmpty()) && getChild(element, "status") != null) {
                    status = getChild(element, "status").getTextContent();
                }

                ClaimFeedbackItem item = new ClaimFeedbackItem();
                item.setClaimLineId(claimLineId);
                item.setApprovedAmount(approvedAmount);
                item.setStatus(status);
                items.add(item);
            }

            return items;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    private Element getChild(Element parent, String name) {
        NodeList nodes = parent.getElementsByTagName(name);
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            return (Element) node;
        }
        return null;
    }
}
