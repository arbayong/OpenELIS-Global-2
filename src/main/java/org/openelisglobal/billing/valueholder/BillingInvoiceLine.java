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
@Table(name = "billing_invoice_line")
public class BillingInvoiceLine extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "billing_invoice_line_generator")
    @SequenceGenerator(name = "billing_invoice_line_generator", sequenceName = "billing_invoice_line_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "invoice_id", nullable = false)
    private Integer invoiceId;

    @Column(name = "test_id", length = 10)
    private String testId;

    @Column(name = "test_name", length = 200)
    private String testName;

    @Column(name = "loinc_code", length = 20)
    private String loincCode;

    @Column(name = "local_code", length = 50)
    private String localCode;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "amount_paid", precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "patient_responsibility_amount", precision = 12, scale = 2)
    private BigDecimal patientResponsibilityAmount;

    @Column(name = "insurance_expected_amount", precision = 12, scale = 2)
    private BigDecimal insuranceExpectedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_status", nullable = false, length = 20)
    private BillingLineStatus lineStatus = BillingLineStatus.ACTIVE;

    @Column(name = "claim_line_id", length = 10)
    private String claimLineId;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getLoincCode() {
        return loincCode;
    }

    public void setLoincCode(String loincCode) {
        this.loincCode = loincCode;
    }

    public String getLocalCode() {
        return localCode;
    }

    public void setLocalCode(String localCode) {
        this.localCode = localCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getPatientResponsibilityAmount() {
        return patientResponsibilityAmount;
    }

    public void setPatientResponsibilityAmount(BigDecimal patientResponsibilityAmount) {
        this.patientResponsibilityAmount = patientResponsibilityAmount;
    }

    public BigDecimal getInsuranceExpectedAmount() {
        return insuranceExpectedAmount;
    }

    public void setInsuranceExpectedAmount(BigDecimal insuranceExpectedAmount) {
        this.insuranceExpectedAmount = insuranceExpectedAmount;
    }

    public BillingLineStatus getLineStatus() {
        return lineStatus;
    }

    public void setLineStatus(BillingLineStatus lineStatus) {
        this.lineStatus = lineStatus;
    }

    public String getClaimLineId() {
        return claimLineId;
    }

    public void setClaimLineId(String claimLineId) {
        this.claimLineId = claimLineId;
    }
}
