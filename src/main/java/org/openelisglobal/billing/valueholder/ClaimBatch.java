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
import java.sql.Date;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "claim_batch")
public class ClaimBatch extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "claim_batch_generator")
    @SequenceGenerator(name = "claim_batch_generator", sequenceName = "claim_batch_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "organization_id")
    private Integer organizationId;

    @Column(name = "batch_number", length = 40)
    private String batchNumber;

    @Column(name = "batch_date")
    private Date batchDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ClaimBatchStatus status;

    @Column(name = "total_claims")
    private Integer totalClaims;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "xml_path", length = 500)
    private String xmlPath;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Integer organizationId) {
        this.organizationId = organizationId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Date getBatchDate() {
        return batchDate;
    }

    public void setBatchDate(Date batchDate) {
        this.batchDate = batchDate;
    }

    public ClaimBatchStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimBatchStatus status) {
        this.status = status;
    }

    public Integer getTotalClaims() {
        return totalClaims;
    }

    public void setTotalClaims(Integer totalClaims) {
        this.totalClaims = totalClaims;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getXmlPath() {
        return xmlPath;
    }

    public void setXmlPath(String xmlPath) {
        this.xmlPath = xmlPath;
    }
}
