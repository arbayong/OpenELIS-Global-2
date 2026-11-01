package org.openelisglobal.billing.claim.claimit;

/**
 * A single CLAIM-it encounter procedure line: service date (optional), G-DRG
 * code and ICD-10 (always emitted), and an optional free-text diagnosis.
 * CLAIM-it prices from the G-DRG code itself, so the code is always emitted
 * even when empty.
 */
public class ClaimitProcedure {

    private String serviceDate;
    private String gdrgCode;
    private String icd10;
    private String diagnosis;

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

    public String getIcd10() {
        return icd10;
    }

    public void setIcd10(String icd10) {
        this.icd10 = icd10;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }
}
