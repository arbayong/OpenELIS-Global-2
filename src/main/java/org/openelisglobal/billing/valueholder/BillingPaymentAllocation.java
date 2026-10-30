package org.openelisglobal.billing.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "billing_payment_allocation")
public class BillingPaymentAllocation extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "billing_payment_allocation_generator")
    @SequenceGenerator(name = "billing_payment_allocation_generator", sequenceName = "billing_payment_allocation_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "invoice_line_id")
    private Integer invoiceLineId;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public Integer getInvoiceLineId() {
        return invoiceLineId;
    }

    public void setInvoiceLineId(Integer invoiceLineId) {
        this.invoiceLineId = invoiceLineId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
