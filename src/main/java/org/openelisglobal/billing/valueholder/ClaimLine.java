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
}
