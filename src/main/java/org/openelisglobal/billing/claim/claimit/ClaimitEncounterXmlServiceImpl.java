package org.openelisglobal.billing.claim.claimit;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CLAIM-it encounter XML encoder, ported from
 * AegisDiagnostics/lib/server/claimit-encounter.ts.
 * <p>
 * Output shape:
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <claims>
 *   <claim>
 *     <claimID>...</claimID>
 *     <claimCheckCode>...</claimCheckCode>   <!-- only when present -->
 *     <memberNo>...</memberNo>
 *     <surname>...</surname>
 *     <otherNames>...</otherNames>           <!-- only when present -->
 *     <dateOfBirth>...</dateOfBirth>
 *     <gender>...</gender>
 *     <hospitalRecNo>...</hospitalRecNo>     <!-- only when present -->
 *     <typeOfService>...</typeOfService>
 *     <dateOfService>...</dateOfService>
 *     <isDependant>...</isDependant>         <!-- only when present -->
 *     <isUnbundled>...</isUnbundled>         <!-- only when present -->
 *     <includesPharmacy>...</includesPharmacy> <!-- only when present -->
 *     <typeOfAttendance>...</typeOfAttendance> <!-- only when present -->
 *     <serviceOutcome>...</serviceOutcome>   <!-- only when present -->
 *     <physicianID>...</physicianID>         <!-- only when present -->
 *     <preAuthorizationCodes>...</preAuthorizationCodes> <!-- only when present -->
 *     <specialtyAttended>...</specialtyAttended> <!-- repeated -->
 *     <investigation>
 *       <serviceDate>...</serviceDate>       <!-- only when present -->
 *       <gdrgCode>...</gdrgCode>             <!-- always, empty when null -->
 *     </investigation>
 *     <procedure>
 *       <serviceDate>...</serviceDate>       <!-- only when present -->
 *       <gdrgCode>...</gdrgCode>             <!-- always -->
 *       <icd10>...</icd10>                    <!-- always -->
 *       <diagnosis>...</diagnosis>           <!-- only when present -->
 *     </procedure>
 *     <referralInfo>                         <!-- only when present -->
 *       <claimCheckCode>...</claimCheckCode> <!-- only when present -->
 *       <facilityID>...</facilityID>         <!-- only when present -->
 *       <facilityName>...</facilityName>     <!-- only when present -->
 *     </referralInfo>
 *   </claim>
 * </claims>
 * }
 * </pre>
 * <b>No {@code <tariff>} element is ever emitted.</b> CLAIM-it computes the
 * cost from the G-DRG code itself; sending the code is the whole point of this
 * encounter path, so the tariff from the monthly batch XML has no place here.
 */
@Service
@Transactional
public class ClaimitEncounterXmlServiceImpl implements ClaimitEncounterXmlService {

    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String CLAIMS_OPEN = "<claims>";
    private static final String CLAIMS_CLOSE = "</claims>";
    private static final String CLAIM_OPEN = "  <claim>";
    private static final String CLAIM_CLOSE = "  </claim>";

    @Override
    public String encodeEncounterXml(List<ClaimitEncounterClaim> claims) {
        List<ClaimitEncounterClaim> safeClaims = claims == null ? List.of() : claims;

        StringBuilder out = new StringBuilder();
        out.append(XML_DECLARATION);
        out.append('\n');
        out.append(CLAIMS_OPEN);
        out.append('\n');

        for (ClaimitEncounterClaim claim : safeClaims) {
            appendClaim(out, claim);
        }

        out.append(CLAIMS_CLOSE);
        return out.toString();
    }

    private void appendClaim(StringBuilder out, ClaimitEncounterClaim claim) {
        out.append(CLAIM_OPEN);
        out.append('\n');

        appendElement(out, "claimID", claim.getClaimID(), true);
        appendElement(out, "claimCheckCode", claim.getClaimCheckCode(), false);
        appendElement(out, "memberNo", claim.getMemberNo(), true);
        appendElement(out, "surname", claim.getSurname(), true);
        appendElement(out, "otherNames", claim.getOtherNames(), false);
        appendElement(out, "dateOfBirth", claim.getDateOfBirth(), true);
        appendElement(out, "gender", claim.getGender(), true);
        appendElement(out, "hospitalRecNo", claim.getHospitalRecNo(), false);
        appendElement(out, "typeOfService", claim.getTypeOfService(), true);
        appendElement(out, "dateOfService", claim.getDateOfService(), true);
        appendElement(out, "isDependant", claim.getIsDependant(), false);
        appendElement(out, "isUnbundled", claim.getIsUnbundled(), false);
        appendElement(out, "includesPharmacy", claim.getIncludesPharmacy(), false);
        appendElement(out, "typeOfAttendance", claim.getTypeOfAttendance(), false);
        appendElement(out, "serviceOutcome", claim.getServiceOutcome(), false);
        appendElement(out, "physicianID", claim.getPhysicianID(), false);
        appendElement(out, "preAuthorizationCodes", claim.getPreAuthorizationCodes(), false);

        List<String> specialties = claim.getSpecialtyAttended();
        if (specialties != null) {
            for (String specialty : specialties) {
                appendElement(out, "specialtyAttended", specialty, true);
            }
        }

        List<ClaimitInvestigation> investigations = claim.getInvestigations();
        if (investigations != null) {
            for (ClaimitInvestigation investigation : investigations) {
                appendInvestigation(out, investigation);
            }
        }

        List<ClaimitProcedure> procedures = claim.getProcedures();
        if (procedures != null) {
            for (ClaimitProcedure procedure : procedures) {
                appendProcedure(out, procedure);
            }
        }

        ClaimitReferralInfo referralInfo = claim.getReferralInfo();
        if (referralInfo != null) {
            appendReferralInfo(out, referralInfo);
        }

        out.append(CLAIM_CLOSE);
        out.append('\n');
    }

    private void appendInvestigation(StringBuilder out, ClaimitInvestigation investigation) {
        out.append("    <investigation>");
        out.append('\n');
        appendElement(out, "serviceDate", investigation.getServiceDate(), false, 6);
        // gdrgCode is always emitted: CLAIM-it prices from the code itself, so
        // an absent code is still sent as an empty element.
        appendElement(out, "gdrgCode", investigation.getGdrgCode(), true, 6);
        out.append("    </investigation>");
        out.append('\n');
    }

    private void appendProcedure(StringBuilder out, ClaimitProcedure procedure) {
        out.append("    <procedure>");
        out.append('\n');
        appendElement(out, "serviceDate", procedure.getServiceDate(), false, 6);
        appendElement(out, "gdrgCode", procedure.getGdrgCode(), true, 6);
        appendElement(out, "icd10", procedure.getIcd10(), true, 6);
        appendElement(out, "diagnosis", procedure.getDiagnosis(), false, 6);
        out.append("    </procedure>");
        out.append('\n');
    }

    private void appendReferralInfo(StringBuilder out, ClaimitReferralInfo referralInfo) {
        out.append("    <referralInfo>");
        out.append('\n');
        appendElement(out, "claimCheckCode", referralInfo.getClaimCheckCode(), false, 6);
        appendElement(out, "facilityID", referralInfo.getFacilityID(), false, 6);
        appendElement(out, "facilityName", referralInfo.getFacilityName(), false, 6);
        out.append("    </referralInfo>");
        out.append('\n');
    }

    /**
     * Emits a single indented element. When {@code required} is true the
     * element is always emitted (empty when the value is null); when false it
     * is omitted entirely for a null or empty value. All values are XML-escaped.
     * {@code indent} is the number of leading spaces.
     */
    private static void appendElement(StringBuilder out, String name, String value, boolean required, int indent) {
        if (!required && (value == null || value.isEmpty())) {
            return;
        }
        String text = value == null ? "" : value;
        out.append(spaces(indent)).append("<").append(name).append(">").append(xmlEscape(text)).append("</").append(name).append(">");
        out.append('\n');
    }

    private static void appendElement(StringBuilder out, String name, String value, boolean required) {
        appendElement(out, name, value, required, 4);
    }

    private static String spaces(int count) {
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, ' ');
        return new String(chars);
    }

    private static String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
