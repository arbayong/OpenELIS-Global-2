package org.openelisglobal.billing.claim.claimit;

import java.util.ArrayList;
import java.util.List;

/**
 * A single CLAIM-it encounter claim, ready for XML encoding. Mirrors the
 * TypeScript {@code ClaimitEncounterClaim} in
 * AegisDiagnostics/lib/server/claimit-encounter.ts.
 */
public class ClaimitEncounterClaim {

    /** Elements the encoder guarantees to emit for any complete claim. Optional
     * elements (claimCheckCode, otherNames, hospitalRecNo, referralInfo, ...)
     * are omitted rather than emitted empty, so they are intentionally not
     * listed here. Mirrors CLAIMIT_ENCOUNTER_XSD_CHECKLIST in the TS reference. */
    public static final List<String> XSD_CHECKLIST = List.of(
            "claims",
            "claim",
            "claimID",
            "memberNo",
            "surname",
            "dateOfBirth",
            "gender",
            "typeOfService",
            "dateOfService",
            "gdrgCode");

    private String claimID;
    private String claimCheckCode;
    private String memberNo;
    private String cardSerialNo;
    private String surname;
    private String otherNames;
    private String dateOfBirth;
    private String gender;
    private String hospitalRecNo;
    private String isDependant;
    private String typeOfService;
    private String isUnbundled;
    private String includesPharmacy;
    private String typeOfAttendance;
    private String serviceOutcome;
    private String dateOfService;
    private List<String> specialtyAttended;
    private List<ClaimitInvestigation> investigations;
    private List<ClaimitProcedure> procedures;
    private String physicianID;
    private String preAuthorizationCodes;
    private ClaimitReferralInfo referralInfo;

    public String getClaimID() {
        return claimID;
    }

    public void setClaimID(String claimID) {
        this.claimID = claimID;
    }

    public String getClaimCheckCode() {
        return claimCheckCode;
    }

    public void setClaimCheckCode(String claimCheckCode) {
        this.claimCheckCode = claimCheckCode;
    }

    public String getMemberNo() {
        return memberNo;
    }

    public void setMemberNo(String memberNo) {
        this.memberNo = memberNo;
    }

    public String getCardSerialNo() {
        return cardSerialNo;
    }

    public void setCardSerialNo(String cardSerialNo) {
        this.cardSerialNo = cardSerialNo;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getOtherNames() {
        return otherNames;
    }

    public void setOtherNames(String otherNames) {
        this.otherNames = otherNames;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getHospitalRecNo() {
        return hospitalRecNo;
    }

    public void setHospitalRecNo(String hospitalRecNo) {
        this.hospitalRecNo = hospitalRecNo;
    }

    public String getIsDependant() {
        return isDependant;
    }

    public void setIsDependant(String isDependant) {
        this.isDependant = isDependant;
    }

    public String getTypeOfService() {
        return typeOfService;
    }

    public void setTypeOfService(String typeOfService) {
        this.typeOfService = typeOfService;
    }

    public String getIsUnbundled() {
        return isUnbundled;
    }

    public void setIsUnbundled(String isUnbundled) {
        this.isUnbundled = isUnbundled;
    }

    public String getIncludesPharmacy() {
        return includesPharmacy;
    }

    public void setIncludesPharmacy(String includesPharmacy) {
        this.includesPharmacy = includesPharmacy;
    }

    public String getTypeOfAttendance() {
        return typeOfAttendance;
    }

    public void setTypeOfAttendance(String typeOfAttendance) {
        this.typeOfAttendance = typeOfAttendance;
    }

    public String getServiceOutcome() {
        return serviceOutcome;
    }

    public void setServiceOutcome(String serviceOutcome) {
        this.serviceOutcome = serviceOutcome;
    }

    public String getDateOfService() {
        return dateOfService;
    }

    public void setDateOfService(String dateOfService) {
        this.dateOfService = dateOfService;
    }

    public List<String> getSpecialtyAttended() {
        return specialtyAttended;
    }

    public void setSpecialtyAttended(List<String> specialtyAttended) {
        this.specialtyAttended = specialtyAttended;
    }

    public List<ClaimitInvestigation> getInvestigations() {
        return investigations;
    }

    public void setInvestigations(List<ClaimitInvestigation> investigations) {
        this.investigations = investigations;
    }

    public List<ClaimitProcedure> getProcedures() {
        return procedures;
    }

    public void setProcedures(List<ClaimitProcedure> procedures) {
        this.procedures = procedures;
    }

    public String getPhysicianID() {
        return physicianID;
    }

    public void setPhysicianID(String physicianID) {
        this.physicianID = physicianID;
    }

    public String getPreAuthorizationCodes() {
        return preAuthorizationCodes;
    }

    public void setPreAuthorizationCodes(String preAuthorizationCodes) {
        this.preAuthorizationCodes = preAuthorizationCodes;
    }

    public ClaimitReferralInfo getReferralInfo() {
        return referralInfo;
    }

    public void setReferralInfo(ClaimitReferralInfo referralInfo) {
        this.referralInfo = referralInfo;
    }

    public ClaimitEncounterClaim() {
        specialtyAttended = new ArrayList<>();
        investigations = new ArrayList<>();
        procedures = new ArrayList<>();
    }
}
