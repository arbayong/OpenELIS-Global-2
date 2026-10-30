package org.openelisglobal.billing.valueholder;

import java.math.BigDecimal;
import java.sql.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "billing_tariff")
public class BillingTariff extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "billing_tariff_generator")
    @SequenceGenerator(name = "billing_tariff_generator", sequenceName = "billing_tariff_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "local_code", length = 50)
    private String localCode;

    @Column(name = "test_id", length = 10)
    private String testId;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "payer_type", length = 20)
    private String payerType;

    @Column(name = "effective_from")
    private Date effectiveFrom;

    @Column(name = "effective_to")
    private Date effectiveTo;

    @Column(name = "is_active", length = 1)
    private String isActive;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getLocalCode() {
        return localCode;
    }

    public void setLocalCode(String localCode) {
        this.localCode = localCode;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPayerType() {
        return payerType;
    }

    public void setPayerType(String payerType) {
        this.payerType = payerType;
    }

    public Date getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Date effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Date getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Date effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
