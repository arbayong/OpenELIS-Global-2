package org.openelisglobal.billing.claim.service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
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
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;
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

    @Autowired
    private PatientDAO patientDAO;

    private String nhisXmlVersion() {
        String version = billingConfigService.getNhisXmlVersion();
        return version == null || version.isEmpty() ? BillingConfigService.DEFAULT_NHIS_XML_VERSION : version;
    }

    /**
     * NHIA v8.6 end-of-month batch XML (the Claim-It portal upload format),
     * ported from the live CLAIM-it survey — see
     * AegisDiagnostics/PLANNING_NHIS_CLAIMIT_INTEGRATION.md and
     * AegisDiagnostics/lib/server/nhis-xml.ts. Shape:
     * {@code Batch > GeneralInformation > (VersionInformation, BatchInformation,
     * ProviderInformation) > Patients > PatientData > Claims > Claim >
     * Treatments/Medicines}. Dates are dd/MM/yyyy (UTC), money is 2dp, currency
     * GHC. Each invoice line becomes one Claim with a single Investigation
     * treatment whose TreatmentCode is the NHIA GDRG code.
     */
    @Override
    public String generateClaimXml(ClaimBatch batch, List<ClaimLine> lines, FacilityCredentialing cred) {
        try {
            List<ClaimLine> safeLines = lines == null ? List.of() : lines;
            Map<String, List<ClaimLine>> byPatient = new LinkedHashMap<>();
            for (ClaimLine line : safeLines) {
                byPatient.computeIfAbsent(line.getPatientId() == null ? "" : line.getPatientId(),
                        k -> new ArrayList<>()).add(line);
            }

            BigDecimal batchAmount = BigDecimal.ZERO;
            for (ClaimLine line : safeLines) {
                batchAmount = batchAmount.add(line.getTotalCost() != null ? line.getTotalCost()
                        : line.getClaimedAmount() != null ? line.getClaimedAmount() : BigDecimal.ZERO);
            }

            Date batchDate = batch.getBatchDate() != null ? batch.getBatchDate()
                    : new Date(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTime(batchDate);

            StringBuilder out = new StringBuilder();
            out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.append("<Batch>\n");
            out.append("  <GeneralInformation>\n");
            out.append("    <VersionInformation>\n");
            out.append("      <XMLFormatVersion>").append(esc(nhisXmlVersion())).append("</XMLFormatVersion>\n");
            out.append("    </VersionInformation>\n");
            out.append("    <BatchInformation>\n");
            out.append("      <BatchNumber>").append(esc(batch.getBatchNumber())).append("</BatchNumber>\n");
            out.append("      <BatchAmount>").append(money(batchAmount)).append("</BatchAmount>\n");
            out.append("      <BatchCurrency>GHC</BatchCurrency>\n");
            out.append("      <ClaimsCount>").append(safeLines.size()).append("</ClaimsCount>\n");
            out.append("      <CreationDate>").append(nhisDate(batchDate)).append("</CreationDate>\n");
            out.append("      <ServiceYear>").append(cal.get(Calendar.YEAR)).append("</ServiceYear>\n");
            out.append("      <ServiceMonth>").append(String.format("%02d", cal.get(Calendar.MONTH) + 1))
                    .append("</ServiceMonth>\n");
            out.append("    </BatchInformation>\n");
            out.append("    <ProviderInformation>\n");
            if (cred != null && cred.getAccreditationNumber() != null && !cred.getAccreditationNumber().isEmpty()) {
                out.append("      <ProviderAccreditationNumber>").append(esc(cred.getAccreditationNumber()))
                        .append("</ProviderAccreditationNumber>\n");
            }
            if (cred != null && cred.getEclaimAuthorizationNumber() != null
                    && !cred.getEclaimAuthorizationNumber().isEmpty()) {
                out.append("      <eClaimAuthorizationNumber>").append(esc(cred.getEclaimAuthorizationNumber()))
                        .append("</eClaimAuthorizationNumber>\n");
            }
            out.append("    </ProviderInformation>\n");
            out.append("  </GeneralInformation>\n");
            out.append("  <Patients>\n");
            for (Map.Entry<String, List<ClaimLine>> entry : byPatient.entrySet()) {
                Patient patient = entry.getKey().isEmpty() ? null : patientDAO.get(entry.getKey()).orElse(null);
                appendPatientData(out, entry.getValue(), patient);
            }
            out.append("  </Patients>\n");
            out.append("</Batch>\n");
            return out.toString();
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    private void appendPatientData(StringBuilder out, List<ClaimLine> patientLines, Patient patient) {
        String memberNumber = firstNonBlank(patientLines, ClaimLine::getMemberNumber);
        out.append("    <PatientData>\n");
        out.append("      <Surname>").append(esc(patient == null ? "" : patient.getEpiLastName()))
                .append("</Surname>\n");
        String otherName = patient == null ? ""
                : joinNonBlank(" ", patient.getEpiFirstName(), patient.getEpiMiddleName());
        if (!otherName.isEmpty()) {
            out.append("      <OtherName>").append(esc(otherName)).append("</OtherName>\n");
        }
        if (patient != null && patient.getBirthDate() != null) {
            Date dob = new Date(patient.getBirthDate().getTime());
            out.append("      <DateOfBirth>").append(nhisDate(dob)).append("</DateOfBirth>\n");
            out.append("      <Infant>").append(isInfant(patient.getBirthDate()) ? "Yes" : "No")
                    .append("</Infant>\n");
        } else {
            out.append("      <DateOfBirth></DateOfBirth>\n");
            out.append("      <Infant>No</Infant>\n");
        }
        out.append("      <MemberNumber>").append(esc(memberNumber)).append("</MemberNumber>\n");
        if (patient != null && patient.getExternalId() != null && !patient.getExternalId().isEmpty()) {
            out.append("      <HospitalRecordNumber>").append(esc(patient.getExternalId()))
                    .append("</HospitalRecordNumber>\n");
        }
        out.append("      <Gender>").append(nhisGender(patient == null ? null : patient.getGender()))
                .append("</Gender>\n");
        out.append("      <Claims>\n");
        for (ClaimLine line : patientLines) {
            appendClaim(out, line);
        }
        out.append("      </Claims>\n");
        out.append("    </PatientData>\n");
    }

    private void appendClaim(StringBuilder out, ClaimLine line) {
        BigDecimal total = line.getTotalCost() != null ? line.getTotalCost()
                : line.getClaimedAmount() != null ? line.getClaimedAmount() : BigDecimal.ZERO;
        out.append("        <Claim>\n");
        out.append("          <ClaimIdentificationNumber>").append(esc(line.getClaimIdentificationNumber()))
                .append("</ClaimIdentificationNumber>\n");
        if (line.getClaimCheckCode() != null && !line.getClaimCheckCode().isEmpty()) {
            out.append("          <ClaimCheckCode>").append(esc(line.getClaimCheckCode())).append("</ClaimCheckCode>\n");
        }
        // Spec §10: ServiceType for a diagnostic centre is DIA (lab, not outpatient).
        out.append("          <ServiceType>").append(esc(nullTo(line.getServiceType(), "DIA")))
                .append("</ServiceType>\n");
        out.append("          <PharmacyIncluded>NO</PharmacyIncluded>\n");
        out.append("          <AllInclusive>YES</AllInclusive>\n");
        out.append("          <OutcomeType>").append(esc(nullTo(line.getOutcomeType(), "DIS")))
                .append("</OutcomeType>\n");
        out.append("          <AdmissionType>").append(esc(nullTo(line.getAdmissionType(), "CRO")))
                .append("</AdmissionType>\n");
        out.append("          <SpecialityCode>").append(esc(nullTo(line.getSpecialityCode(), "INVE")))
                .append("</SpecialityCode>\n");
        if (line.getAdmissionDate() != null) {
            out.append("          <AdmissionDate>").append(nhisDate(line.getAdmissionDate()))
                    .append("</AdmissionDate>\n");
        }
        if (line.getOutpatientCode() != null && !line.getOutpatientCode().isEmpty()) {
            out.append("          <OutPatientCode>").append(esc(line.getOutpatientCode()))
                    .append("</OutPatientCode>\n");
        }
        // Spec §10: InvestigationCode (primary billable G-DRG) only when present — DIA claims carry one.
        if (line.getGdrgCode() != null && !line.getGdrgCode().isEmpty()) {
            out.append("          <InvestigationCode>").append(esc(line.getGdrgCode()))
                    .append("</InvestigationCode>\n");
        }
        out.append("          <TotalCost>").append(money(total)).append("</TotalCost>\n");
        if (line.getReferralNo() != null && !line.getReferralNo().isEmpty()) {
            out.append("          <ReferralNo>").append(esc(line.getReferralNo())).append("</ReferralNo>\n");
        }
        out.append("          <TreatmentsCount>1</TreatmentsCount>\n");
        out.append("          <MedicinesCount>0</MedicinesCount>\n");
        out.append("          <Treatments>\n");
        out.append("            <Treatment>\n");
        if (line.getAdmissionDate() != null) {
            out.append("              <Date>").append(nhisDate(line.getAdmissionDate())).append("</Date>\n");
        }
        out.append("              <Type>Investigation</Type>\n");
        out.append("              <TreatmentCode>").append(esc(nullTo(line.getGdrgCode(), line.getServiceCode())))
                .append("</TreatmentCode>\n");
        // Spec §10: ICDCode is mandatory for Diagnosis only; empty for Investigation/Procedure — never emit it for DIA.
        out.append("              <Tariff>").append(money(line.getTariff() != null ? line.getTariff() : total))
                .append("</Tariff>\n");
        out.append("            </Treatment>\n");
        out.append("          </Treatments>\n");
        out.append("          <Medicines></Medicines>\n");
        out.append("        </Claim>\n");
    }

    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** NHIA date format: dd/MM/yyyy in UTC, matching the CLAIM-it encoder. */
    private static String nhisDate(Date date) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        return String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR));
    }

    private static String nhisGender(String gender) {
        if (gender == null || gender.isEmpty()) {
            return "";
        }
        if (gender.equalsIgnoreCase("male")) {
            return "M";
        }
        if (gender.equalsIgnoreCase("female")) {
            return "F";
        }
        return gender.substring(0, 1).toUpperCase();
    }

    private static boolean isInfant(java.sql.Timestamp birthDate) {
        Calendar birth = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        birth.setTime(birthDate);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int years = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            years--;
        }
        return years < 1;
    }

    private static String nullTo(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static String firstNonBlank(List<ClaimLine> lines, Function<ClaimLine, String> getter) {
        for (ClaimLine line : lines) {
            String value = getter.apply(line);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static String joinNonBlank(String separator, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
                sb.append(part);
            }
        }
        return sb.toString();
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
