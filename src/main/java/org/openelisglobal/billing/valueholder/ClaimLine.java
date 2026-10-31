package org.openelisglobal.billing.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "claim_line")
public class ClaimLine extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "claim_line_generator")
    @SequenceGenerator(name = "claim_line_generator", sequenceName = "claim_line_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "claim_batch_id")
    private Integer claimBatchId;

    @Column(name = "invoice_line_id")
    private Integer invoiceLineId;

    @Column(name = "patient_id", length = 10)
    private String patientId;

    @Column(name = "member_number", length = 60)
    private String memberNumber;

    @Column(name = "service_code", length = 50)
    private String serviceCode;

    @Column(name = "claimed_amount", precision = 12, scale = 2)
    private BigDecimal claimedAmount;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ClaimLineStatus status;

    /** NHIA v8.6 claim-level fields (ClaimIdentificationNumber, ServiceType, ...). */
    @Column(name = "claim_identification_number", length = 60)
    private String claimIdentificationNumber;

    @Column(name = "claim_check_code", length = 20)
    private String claimCheckCode;

    @Column(name = "service_type", length = 10)
    private String serviceType;

    @Column(name = "outcome_type", length = 10)
    private String outcomeType;

    @Column(name = "admission_type", length = 10)
    private String admissionType;

    @Column(name = "speciality_code", length = 10)
    private String specialityCode;

    @Column(name = "admission_date")
    private java.sql.Date admissionDate;

    @Column(name = "outpatient_code", length = 60)
    private String outpatientCode;

    @Column(name = "total_cost", precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "referral_no", length = 60)
    private String referralNo;

    /** NHIA GDRG tariff code for the treatment (never the local test code). */
    @Column(name = "gdrg_code", length = 60)
    private String gdrgCode;

    @Column(name = "icd_code", length = 20)
    private String icdCode;

    @Column(name = "tariff", precision = 12, scale = 2)
    private BigDecimal tariff;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClaimBatchId() {
        return claimBatchId;
    }

    public void setClaimBatchId(Integer claimBatchId) {
        this.claimBatchId = claimBatchId;
    }

    public Integer getInvoiceLineId() {
        return invoiceLineId;
    }

    public void setInvoiceLineId(Integer invoiceLineId) {
        this.invoiceLineId = invoiceLineId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getMemberNumber() {
        return memberNumber;
    }

    public void setMemberNumber(String memberNumber) {
        this.memberNumber = memberNumber;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public BigDecimal getClaimedAmount() {
        return claimedAmount;
    }

    public void setClaimedAmount(BigDecimal claimedAmount) {
        this.claimedAmount = claimedAmount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public ClaimLineStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimLineStatus status) {
        this.status = status;
    }

    public String getClaimIdentificationNumber() {
        return claimIdentificationNumber;
    }

    public void setClaimIdentificationNumber(String claimIdentificationNumber) {
        this.claimIdentificationNumber = claimIdentificationNumber;
    }

    public String getClaimCheckCode() {
        return claimCheckCode;
    }

    public void setClaimCheckCode(String claimCheckCode) {
        this.claimCheckCode = claimCheckCode;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getOutcomeType() {
        return outcomeType;
    }

    public void setOutcomeType(String outcomeType) {
        this.outcomeType = outcomeType;
    }

    public String getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(String admissionType) {
        this.admissionType = admissionType;
    }

    public String getSpecialityCode() {
        return specialityCode;
    }

    public void setSpecialityCode(String specialityCode) {
        this.specialityCode = specialityCode;
    }

    public java.sql.Date getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(java.sql.Date admissionDate) {
        this.admissionDate = admissionDate;
    }

    public String getOutpatientCode() {
        return outpatientCode;
    }

    public void setOutpatientCode(String outpatientCode) {
        this.outpatientCode = outpatientCode;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getReferralNo() {
        return referralNo;
    }

    public void setReferralNo(String referralNo) {
        this.referralNo = referralNo;
    }

    public String getGdrgCode() {
        return gdrgCode;
    }

    public void setGdrgCode(String gdrgCode) {
        this.gdrgCode = gdrgCode;
    }

    public String getIcdCode() {
        return icdCode;
    }

    public void setIcdCode(String icdCode) {
        this.icdCode = icdCode;
    }

    public BigDecimal getTariff() {
        return tariff;
    }

    public void setTariff(BigDecimal tariff) {
        this.tariff = tariff;
    }
}
