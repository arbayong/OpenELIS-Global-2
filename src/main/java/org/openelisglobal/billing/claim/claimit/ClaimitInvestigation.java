package org.openelisglobal.billing.claim.claimit;

/**
 * A single CLAIM-it encounter investigation line: the service date (when
 * known) and the NHIA G-DRG code. CLAIM-it prices from the G-DRG code itself,
 * so the code is always emitted even when empty.
 */
public class ClaimitInvestigation {

    private String serviceDate;
    private String gdrgCode;

    public String getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(String serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getGdrgCode() {
        return gdrgCode;
    }

    public void setGdrgCode(String gdrgCode) {
        this.gdrgCode = gdrgCode;
    }
}
