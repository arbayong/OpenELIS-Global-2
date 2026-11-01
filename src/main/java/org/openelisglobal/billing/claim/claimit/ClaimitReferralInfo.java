package org.openelisglobal.billing.claim.claimit;

/**
 * Optional referral information carried on a CLAIM-it encounter claim. Each
 * field is emitted only when present.
 */
public class ClaimitReferralInfo {

    private String claimCheckCode;
    private String facilityID;
    private String facilityName;

    public String getClaimCheckCode() {
        return claimCheckCode;
    }

    public void setClaimCheckCode(String claimCheckCode) {
        this.claimCheckCode = claimCheckCode;
    }

    public String getFacilityID() {
        return facilityID;
    }

    public void setFacilityID(String facilityID) {
        this.facilityID = facilityID;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }
}
