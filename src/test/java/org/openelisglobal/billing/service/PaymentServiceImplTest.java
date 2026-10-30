package org.openelisglobal.billing.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
import org.openelisglobal.billing.valueholder.PaymentMethod;
import org.openelisglobal.billing.valueholder.PaymentType;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PaymentServiceImplTest {

    @Mock
    private BillingInvoiceDAO billingInvoiceDAO;
    @Mock
    private BillingInvoiceLineDAO billingInvoiceLineDAO;
    @Mock
    private BillingPaymentDAO billingPaymentDAO;
    @Mock
    private BillingPaymentAllocationDAO billingPaymentAllocationDAO;

    @InjectMocks
    private PaymentServiceImpl service;

    private BillingInvoice invoice;
    private BillingInvoiceLine line;

    @Before
    public void setUp() {
        invoice = new BillingInvoice();
        invoice.setId(1);
        invoice.setOrganizationId(1);
        invoice.setPatientId("p1");
        invoice.setTotal(new BigDecimal("100.00"));
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice.setStatus(BillingInvoiceStatus.ISSUED);

        line = new BillingInvoiceLine();
        line.setId(10);
        line.setInvoiceId(1);
        line.setLineTotal(new BigDecimal("100.00"));
        line.setAmountPaid(BigDecimal.ZERO);
        line.setLineStatus(BillingLineStatus.ACTIVE);

        when(billingInvoiceDAO.get(1)).thenReturn(Optional.of(invoice));
        when(billingInvoiceLineDAO.getByInvoiceId(1)).thenReturn(List.of(line));
        when(billingPaymentDAO.insert(any(BillingPayment.class))).thenReturn(100);
    }

    @Test
    public void recordPayment_fullPayment_marksPaidAndAllocatesToLine() {
        service.recordPayment(1, new BigDecimal("100.00"), PaymentMethod.CASH, null, "ref1", "u1");

        ArgumentCaptor<BillingPayment> payCaptor = ArgumentCaptor.forClass(BillingPayment.class);
        verify(billingPaymentDAO).insert(payCaptor.capture());
        BillingPayment payment = payCaptor.getValue();
        assertEquals(PaymentType.PAYMENT, payment.getType());
        assertEquals(new BigDecimal("100.00"), payment.getAmount());
        assertEquals(Integer.valueOf(1), payment.getOrganizationId());
        assertEquals("p1", payment.getPatientId());

        ArgumentCaptor<BillingPaymentAllocation> allocCaptor = ArgumentCaptor.forClass(BillingPaymentAllocation.class);
        verify(billingPaymentAllocationDAO).insert(allocCaptor.capture());
        assertEquals(new BigDecimal("100.00"), allocCaptor.getValue().getAmount());
        assertEquals(Integer.valueOf(10), allocCaptor.getValue().getInvoiceLineId());

        ArgumentCaptor<BillingInvoice> invCaptor = ArgumentCaptor.forClass(BillingInvoice.class);
        verify(billingInvoiceDAO).update(invCaptor.capture());
        assertEquals(new BigDecimal("100.00"), invCaptor.getValue().getAmountPaid());
        assertEquals(BillingInvoiceStatus.PAID, invCaptor.getValue().getStatus());
    }

    @Test
    public void recordPayment_partialPayment_marksPartiallyPaid() {
        service.recordPayment(1, new BigDecimal("40.00"), PaymentMethod.MOBILE_MONEY, null, "ref2", "u1");

        ArgumentCaptor<BillingInvoice> invCaptor = ArgumentCaptor.forClass(BillingInvoice.class);
        verify(billingInvoiceDAO).update(invCaptor.capture());
        assertEquals(new BigDecimal("40.00"), invCaptor.getValue().getAmountPaid());
        assertEquals(BillingInvoiceStatus.PARTIALLY_PAID, invCaptor.getValue().getStatus());

        ArgumentCaptor<BillingPaymentAllocation> allocCaptor = ArgumentCaptor.forClass(BillingPaymentAllocation.class);
        verify(billingPaymentAllocationDAO).insert(allocCaptor.capture());
        assertEquals(new BigDecimal("40.00"), allocCaptor.getValue().getAmount());
    }

    @Test
    public void voidInvoiceForCancellation_voidsLinesAndRecordsRefund() {
        invoice.setAmountPaid(new BigDecimal("30.00"));
        invoice.setStatus(BillingInvoiceStatus.PARTIALLY_PAID);

        BillingInvoice result = service.voidInvoiceForCancellation(1, "no stock", "u1");

        assertEquals(BillingInvoiceStatus.VOID, result.getStatus());
        ArgumentCaptor<BillingInvoiceLine> lineCaptor = ArgumentCaptor.forClass(BillingInvoiceLine.class);
        verify(billingInvoiceLineDAO).update(lineCaptor.capture());
        assertEquals(BillingLineStatus.VOID, lineCaptor.getValue().getLineStatus());

        ArgumentCaptor<BillingPayment> refundCaptor = ArgumentCaptor.forClass(BillingPayment.class);
        verify(billingPaymentDAO).insert(refundCaptor.capture());
        BillingPayment refund = refundCaptor.getValue();
        assertEquals(PaymentType.REFUND, refund.getType());
        assertEquals(new BigDecimal("-30.00"), refund.getAmount());
        assertEquals("cancel:1", refund.getProviderTransactionId());
    }

    @Test
    public void voidInvoice_alreadyVoid_doesNothing() {
        invoice.setStatus(BillingInvoiceStatus.VOID);

        BillingInvoice result = service.voidInvoiceForCancellation(1, "dup", "u1");

        assertEquals(BillingInvoiceStatus.VOID, result.getStatus());
        verify(billingPaymentDAO, never()).insert(any(BillingPayment.class));
        verify(billingInvoiceLineDAO, never()).update(any(BillingInvoiceLine.class));
    }
}
