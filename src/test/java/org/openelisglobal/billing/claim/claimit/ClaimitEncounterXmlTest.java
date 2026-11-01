package org.openelisglobal.billing.claim.claimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Plain JUnit 4 test of the CLAIM-it encounter XML encoder. No Spring context
 * — the service impl is instantiated directly. Assertions are plain string and
 * DOM checks (no data-testid attributes involved).
 */
public class ClaimitEncounterXmlTest {

    private ClaimitEncounterXmlService service = new ClaimitEncounterXmlServiceImpl();

    private ClaimitEncounterClaim fullClaim() {
        ClaimitEncounterClaim claim = new ClaimitEncounterClaim();
        claim.setClaimID("CLM-1001");
        claim.setClaimCheckCode("CHK-99");
        claim.setMemberNo("NHIS12345678");
        claim.setCardSerialNo("CARD-55");
        claim.setSurname("O'Brien");
        claim.setOtherNames("Mary Jane");
        claim.setDateOfBirth("1985-04-12");
        claim.setGender("F");
        claim.setHospitalRecNo("MRN-77");
        claim.setTypeOfService("DIA");
        claim.setDateOfService("2026-01-15");
        claim.setIsDependant("No");
        claim.setIsUnbundled("Yes");
        claim.setIncludesPharmacy("NO");
        claim.setTypeOfAttendance("CFU");
        claim.setServiceOutcome("DISC");
        claim.setPhysicianID("DOC-3");
        claim.setPreAuthorizationCodes("PRE-1");

        List<String> specialties = new ArrayList<>();
        specialties.add("INVE");
        specialties.add("PATH");
        claim.setSpecialtyAttended(specialties);

        ClaimitInvestigation inv1 = new ClaimitInvestigation();
        inv1.setServiceDate("2026-01-15");
        inv1.setGdrgCode("GDRG-001");
        ClaimitInvestigation inv2 = new ClaimitInvestigation();
        inv2.setServiceDate(null);
        inv2.setGdrgCode("GDRG-002");
        ClaimitInvestigation inv3 = new ClaimitInvestigation();
        inv3.setServiceDate("2026-01-15");
        inv3.setGdrgCode(null);
        List<ClaimitInvestigation> investigations = new ArrayList<>();
        investigations.add(inv1);
        investigations.add(inv2);
        investigations.add(inv3);
        claim.setInvestigations(investigations);

        ClaimitProcedure proc1 = new ClaimitProcedure();
        proc1.setServiceDate("2026-01-15");
        proc1.setGdrgCode("GDRG-010");
        proc1.setIcd10("K35.2");
        proc1.setDiagnosis("Acute appendicitis");
        ClaimitProcedure proc2 = new ClaimitProcedure();
        proc2.setServiceDate(null);
        proc2.setGdrgCode("GDRG-011");
        proc2.setIcd10("");
        proc2.setDiagnosis(null);
        List<ClaimitProcedure> procedures = new ArrayList<>();
        procedures.add(proc1);
        procedures.add(proc2);
        claim.setProcedures(procedures);

        ClaimitReferralInfo ref = new ClaimitReferralInfo();
        ref.setClaimCheckCode("REF-CHK");
        ref.setFacilityID("FAC-1");
        ref.setFacilityName("St. Mary's & Co.");
        claim.setReferralInfo(ref);

        return claim;
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    @Test
    public void encodeEncounterXml_fullyPopulatedClaim_emitsEveryElementInOrder() {
        String xml = service.encodeEncounterXml(List.of(fullClaim()));

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", lines(xml).get(0));
        assertEquals("<claims>", lines(xml).get(1));
        assertEquals("</claims>", lines(xml).get(lines(xml).size() - 1));

        // Exact element order within <claim>.
        List<String> order = List.of(
                "claimID", "claimCheckCode", "memberNo", "surname", "otherNames",
                "dateOfBirth", "gender", "hospitalRecNo", "typeOfService",
                "dateOfService", "isDependant", "isUnbundled", "includesPharmacy",
                "typeOfAttendance", "serviceOutcome", "physicianID",
                "preAuthorizationCodes", "specialtyAttended", "investigation",
                "procedure", "referralInfo");
        int last = -1;
        for (String name : order) {
            int pos = indexOfOpenTag(xml, name, last + 1);
            assertTrue("expected <" + name + "> after previous elements", pos > last);
            last = pos;
        }

        // Two specialties and three investigations/procedures each repeated.
        assertEquals(2, countOpenTags(xml, "specialtyAttended"));
        assertEquals(3, countOpenTags(xml, "investigation"));
        assertEquals(2, countOpenTags(xml, "procedure"));

        assertEquals("CLM-1001", textContent(xml, "claimID"));
        assertEquals("CHK-99", textContent(xml, "claimCheckCode"));
        assertEquals("NHIS12345678", textContent(xml, "memberNo"));
        assertEquals("O'Brien", textContent(xml, "surname"));
        assertEquals("Mary Jane", textContent(xml, "otherNames"));
        assertEquals("1985-04-12", textContent(xml, "dateOfBirth"));
        assertEquals("F", textContent(xml, "gender"));
        assertEquals("MRN-77", textContent(xml, "hospitalRecNo"));
        assertEquals("DIA", textContent(xml, "typeOfService"));
        assertEquals("2026-01-15", textContent(xml, "dateOfService"));
        assertEquals("No", textContent(xml, "isDependant"));
        assertEquals("Yes", textContent(xml, "isUnbundled"));
        assertEquals("NO", textContent(xml, "includesPharmacy"));
        assertEquals("CFU", textContent(xml, "typeOfAttendance"));
        assertEquals("DISC", textContent(xml, "serviceOutcome"));
        assertEquals("DOC-3", textContent(xml, "physicianID"));
        assertEquals("PRE-1", textContent(xml, "preAuthorizationCodes"));
        assertEquals("INVE", nthTextContent(xml, "specialtyAttended", 0));
        assertEquals("PATH", nthTextContent(xml, "specialtyAttended", 1));
        assertEquals("GDRG-001", nthTextContent(xml, "gdrgCode", 0));
        assertEquals("GDRG-002", nthTextContent(xml, "gdrgCode", 1));
        assertEquals("K35.2", nthTextContent(xml, "icd10", 0));
        assertEquals("Acute appendicitis", nthTextContent(xml, "diagnosis", 0));
        assertEquals("FAC-1", nthTextContent(xml, "facilityID", 0));
        // claimCheckCode inside referralInfo is the second <claimCheckCode> in
        // the document (the claim-level one comes first).
        assertEquals("REF-CHK", nthTextContent(xml, "claimCheckCode", 1));
        assertEquals("St. Mary's & Co.", nthTextContent(xml, "facilityName", 0));
    }

    @Test
    public void encodeEncounterXml_optionalElementsOmittedWhenUnset() {
        ClaimitEncounterClaim claim = new ClaimitEncounterClaim();
        claim.setClaimID("CLM-2");
        claim.setMemberNo("M");
        claim.setSurname("Doe");
        claim.setDateOfBirth("1990-01-01");
        claim.setGender("M");
        claim.setTypeOfService("DIA");
        claim.setDateOfService("2026-02-01");

        String xml = service.encodeEncounterXml(List.of(claim));

        assertFalse(xml.contains("<claimCheckCode>"));
        assertFalse(xml.contains("<otherNames>"));
        assertFalse(xml.contains("<hospitalRecNo>"));
        assertFalse(xml.contains("<isDependant>"));
        assertFalse(xml.contains("<isUnbundled>"));
        assertFalse(xml.contains("<includesPharmacy>"));
        assertFalse(xml.contains("<typeOfAttendance>"));
        assertFalse(xml.contains("<serviceOutcome>"));
        assertFalse(xml.contains("<physicianID>"));
        assertFalse(xml.contains("<preAuthorizationCodes>"));
        assertFalse(xml.contains("<specialtyAttended>"));
        assertFalse(xml.contains("<investigation>"));
        assertFalse(xml.contains("<procedure>"));
        assertFalse(xml.contains("<referralInfo>"));
        assertTrue(xml.contains("<claimID>CLM-2</claimID>"));
        assertTrue(xml.contains("<memberNo>M</memberNo>"));
        assertTrue(xml.contains("<surname>Doe</surname>"));
        assertTrue(xml.contains("<dateOfBirth>1990-01-01</dateOfBirth>"));
        assertTrue(xml.contains("<gender>M</gender>"));
        assertTrue(xml.contains("<typeOfService>DIA</typeOfService>"));
        assertTrue(xml.contains("<dateOfService>2026-02-01</dateOfService>"));
    }

    @Test
    public void encodeEncounterXml_referralInfoOmitsUnsetFields() {
        ClaimitEncounterClaim claim = new ClaimitEncounterClaim();
        claim.setClaimID("CLM-3");
        claim.setMemberNo("M");
        claim.setSurname("Doe");
        claim.setDateOfBirth("1990-01-01");
        claim.setGender("M");
        claim.setTypeOfService("DIA");
        claim.setDateOfService("2026-02-01");

        ClaimitReferralInfo ref = new ClaimitReferralInfo();
        ref.setFacilityName("Only Name");
        claim.setReferralInfo(ref);

        String xml = service.encodeEncounterXml(List.of(claim));

        assertTrue(xml.contains("<referralInfo>"));
        assertTrue(xml.contains("<facilityName>Only Name</facilityName>"));
        // The only <claimCheckCode> in this document is the claim-level one,
        // which is absent here; referralInfo must not leak one.
        assertFalse(xml.contains("<claimCheckCode>"));
        assertFalse(xml.contains("<facilityID>"));
        assertTrue(xml.indexOf("</referralInfo>") > xml.indexOf("<referralInfo>"));
    }

    @Test
    public void encodeEncounterXml_escapesSpecialCharacters() throws Exception {
        ClaimitEncounterClaim claim = new ClaimitEncounterClaim();
        claim.setClaimID("CLM-4");
        claim.setMemberNo("M");
        claim.setSurname("A & B <C> \"D\" 'E'");
        claim.setDateOfBirth("1990-01-01");
        claim.setGender("M");
        claim.setTypeOfService("DIA");
        claim.setDateOfService("2026-02-01");

        ClaimitReferralInfo ref = new ClaimitReferralInfo();
        ref.setFacilityName("X & Y <Z> \"W\" 'V'");
        claim.setReferralInfo(ref);

        String xml = service.encodeEncounterXml(List.of(claim));

        assertTrue(xml.contains("<surname>A &amp; B &lt;C&gt; &quot;D&quot; &apos;E&apos;</surname>"));
        assertTrue(xml.contains("<facilityName>X &amp; Y &lt;Z&gt; &quot;W&quot; &apos;V&apos;</facilityName>"));
        parse(xml); // must still be well-formed
    }

    @Test
    public void encodeEncounterXml_investigationWithNullGdrgCodeStillEmitsEmptyElement() {
        ClaimitEncounterClaim claim = new ClaimitEncounterClaim();
        claim.setClaimID("CLM-5");
        claim.setMemberNo("M");
        claim.setSurname("Doe");
        claim.setDateOfBirth("1990-01-01");
        claim.setGender("M");
        claim.setTypeOfService("DIA");
        claim.setDateOfService("2026-02-01");

        ClaimitInvestigation inv = new ClaimitInvestigation();
        inv.setServiceDate(null);
        inv.setGdrgCode(null);
        claim.setInvestigations(List.of(inv));

        String xml = service.encodeEncounterXml(List.of(claim));

        assertTrue(xml.contains("<investigation>"));
        assertTrue(xml.contains("<gdrgCode></gdrgCode>"));
        assertFalse(xml.contains("<serviceDate>"));
    }

    @Test
    public void encodeEncounterXml_noTariffElementAnywhere() {
        ClaimitEncounterClaim claim = fullClaim();
        String xml = service.encodeEncounterXml(List.of(claim));

        assertFalse("no <tariff element may be emitted", xml.toLowerCase().contains("<tariff"));
    }

    @Test
    public void encodeEncounterXml_outputIsWellFormed() throws Exception {
        ClaimitEncounterClaim claim = fullClaim();
        String xml = service.encodeEncounterXml(List.of(claim));

        Document doc = parse(xml);
        assertNotNull(doc.getDocumentElement());
        assertEquals("claims", doc.getDocumentElement().getTagName());
        NodeList claims = doc.getElementsByTagName("claim");
        assertEquals(1, claims.getLength());
    }

    @Test
    public void encodeEncounterXml_nullAndEmptyListsProduceEmptyEnvelope() {
        String xml = service.encodeEncounterXml(null);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<claims>\n</claims>", xml);

        xml = service.encodeEncounterXml(List.of());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<claims>\n</claims>", xml);
    }

    /* ---------- helpers ---------- */

    private List<String> lines(String xml) {
        List<String> result = new ArrayList<>();
        for (String line : xml.split("\n", -1)) {
            result.add(line);
        }
        return result;
    }

    private int indexOfOpenTag(String xml, String name, int from) {
        return xml.indexOf("<" + name + ">", from);
    }

    private int countOpenTags(String xml, String name) {
        String tag = "<" + name + ">";
        int count = 0;
        int from = 0;
        while (true) {
            int pos = xml.indexOf(tag, from);
            if (pos < 0) {
                return count;
            }
            count++;
            from = pos + tag.length();
        }
    }

    private String textContent(String xml, String dottedTag) {
        // Only used for top-level, non-repeated claim fields here.
        String tag = "<" + dottedTag + ">";
        int start = xml.indexOf(tag);
        if (start < 0) {
            return null;
        }
        start += tag.length();
        int end = xml.indexOf("</" + dottedTag + ">", start);
        return unescape(xml.substring(start, end));
    }

    /** Mirrors the reference decoder: assertions compare values, not entities. */
    private String unescape(String value) {
        return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }

    private String nthTextContent(String xml, String name, int n) {
        String tag = "<" + name + ">";
        String close = "</" + name + ">";
        int from = 0;
        int found = -1;
        for (int i = 0; i <= n; i++) {
            found = xml.indexOf(tag, from);
            if (found < 0) {
                return null;
            }
            from = found + tag.length();
        }
        int end = xml.indexOf(close, from);
        return unescape(xml.substring(from, end));
    }

}
