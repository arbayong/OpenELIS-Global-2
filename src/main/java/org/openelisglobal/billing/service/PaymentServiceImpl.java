package org.openelisglobal.billing.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.BillingPaymentAllocationDAO;
import org.openelisglobal.billing.dao.BillingPaymentDAO;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingInvoiceStatus;
import org.openelisglobal.billing.valueholder.BillingLineStatus;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.billing.valueholder.BillingPaymentAllocation;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.openelisglobal.billing.valueholder.PaymentMethod;
import org.openelisglobal.billing.valueholder.PaymentType;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private BillingInvoiceDAO billingInvoiceDAO;

    @Autowired
    private BillingInvoiceLineDAO billingInvoiceLineDAO;

    @Autowired
    private BillingPaymentDAO billingPaymentDAO;

    @Autowired
    private BillingPaymentAllocationDAO billingPaymentAllocationDAO;

    @Override
    @Transactional
    public BillingPayment recordPayment(Integer invoiceId, BigDecimal amount, PaymentMethod method,
            GatewayDriver gateway, String providerTransactionId, String recordedBy) {
        try {
            BillingInvoice invoice = billingInvoiceDAO.get(invoiceId)
                    .orElseThrow(() -> new LIMSRuntimeException("Invoice not found: " + invoiceId));

            BillingPayment payment = new BillingPayment();
            payment.setOrganizationId(invoice.getOrganizationId());
            payment.setPatientId(invoice.getPatientId());
            payment.setMethod(method);
            payment.setGateway(gateway);
            payment.setType(PaymentType.PAYMENT);
            payment.setAmount(amount);
            payment.setProviderTransactionId(providerTransactionId);
            payment.setReceivedAt(new Timestamp(System.currentTimeMillis()));
            payment.setRecordedBy(recordedBy);
            Integer paymentId = billingPaymentDAO.insert(payment);
            payment.setId(paymentId);

            BigDecimal remaining = amount;
            List<BillingInvoiceLine> lines = billingInvoiceLineDAO.getByInvoiceId(invoiceId);
            if (lines != null) {
                for (BillingInvoiceLine line : lines) {
                    if (line.getLineStatus() != BillingLineStatus.ACTIVE) {
                        continue;
                    }
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    BigDecimal lineTotal = line.getLineTotal() == null ? BigDecimal.ZERO : line.getLineTotal();
                    BigDecimal linePaid = line.getAmountPaid() == null ? BigDecimal.ZERO : line.getAmountPaid();
                    BigDecimal lineDue = lineTotal.subtract(linePaid);
                    if (lineDue.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    BigDecimal alloc = remaining.compareTo(lineDue) <= 0 ? remaining : lineDue;
                    BillingPaymentAllocation allocation = new BillingPaymentAllocation();
                    allocation.setPaymentId(paymentId);
                    allocation.setInvoiceLineId(line.getId());
                    allocation.setAmount(alloc);
                    billingPaymentAllocationDAO.insert(allocation);
                    line.setAmountPaid(linePaid.add(alloc));
                    remaining = remaining.subtract(alloc);
                }
            }

            BigDecimal invoicePaid = invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid();
            BigDecimal newAmountPaid = invoicePaid.add(amount);
            invoice.setAmountPaid(newAmountPaid);
            BigDecimal total = invoice.getTotal() == null ? BigDecimal.ZERO : invoice.getTotal();
            if (newAmountPaid.compareTo(total) >= 0) {
                invoice.setStatus(BillingInvoiceStatus.PAID);
            } else if (newAmountPaid.compareTo(BigDecimal.ZERO) > 0) {
                invoice.setStatus(BillingInvoiceStatus.PARTIALLY_PAID);
            } else {
                invoice.setStatus(BillingInvoiceStatus.ISSUED);
            }
            billingInvoiceDAO.update(invoice);
            return payment;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    @Transactional
    public BillingInvoice voidInvoiceForCancellation(Integer invoiceId, String reason, String recordedBy) {
        try {
            BillingInvoice invoice = billingInvoiceDAO.get(invoiceId)
                    .orElseThrow(() -> new LIMSRuntimeException("Invoice not found: " + invoiceId));

            if (invoice.getStatus() == BillingInvoiceStatus.VOID) {
                return invoice;
            }

            invoice.setStatus(BillingInvoiceStatus.VOID);

            List<BillingInvoiceLine> lines = billingInvoiceLineDAO.getByInvoiceId(invoiceId);
            if (lines != null) {
                for (BillingInvoiceLine line : lines) {
                    line.setLineStatus(BillingLineStatus.VOID);
                    billingInvoiceLineDAO.update(line);
                }
            }

            BigDecimal amountPaid = invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid();
            if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                BillingPayment refund = new BillingPayment();
                refund.setOrganizationId(invoice.getOrganizationId());
                refund.setPatientId(invoice.getPatientId());
                refund.setMethod(PaymentMethod.CASH);
                refund.setGateway(null);
                refund.setType(PaymentType.REFUND);
                refund.setAmount(amountPaid.negate());
                refund.setProviderTransactionId("cancel:" + invoiceId);
                refund.setReceivedAt(new Timestamp(System.currentTimeMillis()));
                refund.setRecordedBy(recordedBy);
                billingPaymentDAO.insert(refund);
            }

            billingInvoiceDAO.update(invoice);
            return invoice;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }
}
