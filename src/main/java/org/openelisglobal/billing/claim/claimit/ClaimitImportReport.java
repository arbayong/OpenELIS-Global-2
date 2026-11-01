package org.openelisglobal.billing.claim.claimit;

import java.util.List;

/**
 * The JSON import report returned by CLAIM-it after a claim submission.
 */
public class ClaimitImportReport {

    private String reportDate;
    private String timeTaken;
    private int passedClaims;
    private int failedClaims;
    private int warnedClaims;
    private int savedClaims;
    private String totalCost;
    private boolean success;
    private List<ClaimitImportClaim> claims;

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(String timeTaken) {
        this.timeTaken = timeTaken;
    }

    public int getPassedClaims() {
        return passedClaims;
    }

    public void setPassedClaims(int passedClaims) {
        this.passedClaims = passedClaims;
    }

    public int getFailedClaims() {
        return failedClaims;
    }

    public void setFailedClaims(int failedClaims) {
        this.failedClaims = failedClaims;
    }

    public int getWarnedClaims() {
        return warnedClaims;
    }

    public void setWarnedClaims(int warnedClaims) {
        this.warnedClaims = warnedClaims;
    }

    public int getSavedClaims() {
        return savedClaims;
    }

    public void setSavedClaims(int savedClaims) {
        this.savedClaims = savedClaims;
    }

    public String getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(String totalCost) {
        this.totalCost = totalCost;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<ClaimitImportClaim> getClaims() {
        return claims;
    }

    public void setClaims(List<ClaimitImportClaim> claims) {
        this.claims = claims;
    }
}
