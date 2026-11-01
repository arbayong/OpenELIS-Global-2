package org.openelisglobal.billing.claim.claimit;

import java.util.List;

/**
 * A single claim entry from a CLAIM-it import report.
 */
public class ClaimitImportClaim {

    private int index;
    private String claimId;
    private String cost;
    private String initCost;
    private String outcome;
    private List<ClaimitIssue> errors;
    private List<ClaimitIssue> warnings;
    private String source;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getInitCost() {
        return initCost;
    }

    public void setInitCost(String initCost) {
        this.initCost = initCost;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public List<ClaimitIssue> getErrors() {
        return errors;
    }

    public void setErrors(List<ClaimitIssue> errors) {
        this.errors = errors;
    }

    public List<ClaimitIssue> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ClaimitIssue> warnings) {
        this.warnings = warnings;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
