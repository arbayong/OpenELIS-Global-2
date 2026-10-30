package org.openelisglobal.billing.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "daily_revenue_summary")
public class DailyRevenueSummary extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_revenue_summary_generator")
    @SequenceGenerator(name = "daily_revenue_summary_generator", sequenceName = "daily_revenue_summary_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "organization_id")
    private Integer organizationId;

    @Column(name = "business_date")
    private Date businessDate;

    @Column(name = "gross_revenue", precision = 12, scale = 2)
    private BigDecimal grossRevenue;

    @Column(name = "cash_revenue", precision = 12, scale = 2)
    private BigDecimal cashRevenue;

    @Column(name = "mobile_money_revenue", precision = 12, scale = 2)
    private BigDecimal mobileMoneyRevenue;

    @Column(name = "gateway_revenue", precision = 12, scale = 2)
    private BigDecimal gatewayRevenue;

    @Column(name = "insurance_revenue", precision = 12, scale = 2)
    private BigDecimal insuranceRevenue;

    @Column(name = "nhis_revenue", precision = 12, scale = 2)
    private BigDecimal nhisRevenue;

    @Column(name = "private_revenue", precision = 12, scale = 2)
    private BigDecimal privateRevenue;

    @Column(name = "refunds", precision = 12, scale = 2)
    private BigDecimal refunds;

    @Column(name = "invoice_count")
    private Integer invoiceCount;

    @Column(name = "patient_count")
    private Integer patientCount;

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

    public Date getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(Date businessDate) {
        this.businessDate = businessDate;
    }

    public BigDecimal getGrossRevenue() {
        return grossRevenue;
    }

    public void setGrossRevenue(BigDecimal grossRevenue) {
        this.grossRevenue = grossRevenue;
    }

    public BigDecimal getCashRevenue() {
        return cashRevenue;
    }

    public void setCashRevenue(BigDecimal cashRevenue) {
        this.cashRevenue = cashRevenue;
    }

    public BigDecimal getMobileMoneyRevenue() {
        return mobileMoneyRevenue;
    }

    public void setMobileMoneyRevenue(BigDecimal mobileMoneyRevenue) {
        this.mobileMoneyRevenue = mobileMoneyRevenue;
    }

    public BigDecimal getGatewayRevenue() {
        return gatewayRevenue;
    }

    public void setGatewayRevenue(BigDecimal gatewayRevenue) {
        this.gatewayRevenue = gatewayRevenue;
    }

    public BigDecimal getInsuranceRevenue() {
        return insuranceRevenue;
    }

    public void setInsuranceRevenue(BigDecimal insuranceRevenue) {
        this.insuranceRevenue = insuranceRevenue;
    }

    public BigDecimal getNhisRevenue() {
        return nhisRevenue;
    }

    public void setNhisRevenue(BigDecimal nhisRevenue) {
        this.nhisRevenue = nhisRevenue;
    }

    public BigDecimal getPrivateRevenue() {
        return privateRevenue;
    }

    public void setPrivateRevenue(BigDecimal privateRevenue) {
        this.privateRevenue = privateRevenue;
    }

    public BigDecimal getRefunds() {
        return refunds;
    }

    public void setRefunds(BigDecimal refunds) {
        this.refunds = refunds;
    }

    public Integer getInvoiceCount() {
        return invoiceCount;
    }

    public void setInvoiceCount(Integer invoiceCount) {
        this.invoiceCount = invoiceCount;
    }

    public Integer getPatientCount() {
        return patientCount;
    }

    public void setPatientCount(Integer patientCount) {
        this.patientCount = patientCount;
    }
}
