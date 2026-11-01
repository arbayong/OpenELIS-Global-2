package org.openelisglobal.billing.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Persisted CLAIM-it import report (per-claim outcome, errors, warnings,
 * CLAIM-it's own cost) — the feedback trail. The report is the only record of
 * what CLAIM-it said, so it is kept here for audit.
 */
@Entity
@Table(name = "claimit_import_record")
public class ClaimitImportRecord extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "claimit_import_record_generator")
    @SequenceGenerator(name = "claimit_import_record_generator", sequenceName = "claimit_import_record_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "claim_id", length = 100)
    private String claimId;

    @Column(name = "invoice_id")
    private Integer invoiceId;

    @Column(name = "outcome", length = 20)
    private String outcome;

    @Column(name = "cost", length = 30)
    private String cost;

    @Column(name = "init_cost", length = 30)
    private String initCost;

    @Column(name = "passed_claims")
    private Integer passedClaims;

    @Column(name = "failed_claims")
    private Integer failedClaims;

    @Column(name = "warned_claims")
    private Integer warnedClaims;

    @Column(name = "saved_claims")
    private Integer savedClaims;

    @Column(name = "report_date", length = 40)
    private String reportDate;

    @Column(name = "time_taken", length = 20)
    private String timeTaken;

    @Column(name = "errors", columnDefinition = "TEXT")
    private String errors;

    @Column(name = "warnings", columnDefinition = "TEXT")
    private String warnings;

    @Column(name = "raw_report", columnDefinition = "MEDIUMTEXT")
    private String rawReport;

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @Column(name = "submitted_at")
    private Timestamp submittedAt;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
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

    public Integer getPassedClaims() {
        return passedClaims;
    }

    public void setPassedClaims(Integer passedClaims) {
        this.passedClaims = passedClaims;
    }

    public Integer getFailedClaims() {
        return failedClaims;
    }

    public void setFailedClaims(Integer failedClaims) {
        this.failedClaims = failedClaims;
    }

    public Integer getWarnedClaims() {
        return warnedClaims;
    }

    public void setWarnedClaims(Integer warnedClaims) {
        this.warnedClaims = warnedClaims;
    }

    public Integer getSavedClaims() {
        return savedClaims;
    }

    public void setSavedClaims(Integer savedClaims) {
        this.savedClaims = savedClaims;
    }

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

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public String getWarnings() {
        return warnings;
    }

    public void setWarnings(String warnings) {
        this.warnings = warnings;
    }

    public String getRawReport() {
        return rawReport;
    }

    public void setRawReport(String rawReport) {
        this.rawReport = rawReport;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }
}
