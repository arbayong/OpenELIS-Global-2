package org.openelisglobal.billing.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.gateway.GatewayCallbackResult;
import org.openelisglobal.billing.gateway.GatewayDriverResolver;
import org.openelisglobal.billing.gateway.PaystackDriver;
import org.openelisglobal.billing.service.PaymentService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Payment gateway endpoints: start a checkout, confirm it, and take a callback.
 *
 * <p>
 * The invariant under test is that money is only credited after the gateway
 * confirms it: an unsigned callback, a failed verification, and a reference that
 * does not identify an invoice must all be acknowledged without recording a
 * payment.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class PaymentGatewayRestControllerTest {

    @Mock
    private GatewayDriverResolver driverResolver;

    @Mock
    private PaymentService paymentService;

    @Mock
    private BillingInvoiceDAO billingInvoiceDAO;

    @Mock
    private PaystackDriver paystackDriver;

    @InjectMocks
    private PaymentGatewayRestController controller;

    private BillingInvoice invoice;

    @Before
    public void setUp() {
        invoice = new BillingInvoice();
        invoice.setId(42);
        invoice.setTotal(new BigDecimal("50.00"));
        invoice.setAmountPaid(BigDecimal.ZERO);
        when(billingInvoiceDAO.get(42)).thenReturn(Optional.of(invoice));
    }

    @Test
    public void initialize_returnsCheckoutReference_whenDriverAccepts() {
        when(driverResolver.resolve(GatewayDriver.PAYSTACK)).thenReturn(paystackDriver);
        when(paystackDriver.initialize(eq(42), any(), any())).thenReturn("INV-42-abc");

        ResponseEntity<?> response = controller.initialize(42, GatewayDriver.PAYSTACK, "https://example/return");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void initialize_serviceUnavailable_whenDriverMissing() {
        when(driverResolver.resolve(GatewayDriver.PAYSTACK)).thenReturn(null);

        ResponseEntity<?> response = controller.initialize(42, GatewayDriver.PAYSTACK, null);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        verify(paystackDriver, never()).initialize(any(), any(), any());
    }

    @Test
    public void verify_recordsPayment_whenGatewayConfirmsReference() {
        BillingPayment payment = new BillingPayment();
        payment.setId(7);
        when(driverResolver.resolve(GatewayDriver.PAYSTACK)).thenReturn(paystackDriver);
        when(paystackDriver.verify("INV-42-abc")).thenReturn(true);
        when(paymentService.recordPayment(eq(42), any(), any(), any(), eq("INV-42-abc"), any()))
                .thenReturn(payment);

        ResponseEntity<?> response = controller.verify("INV-42-abc", GatewayDriver.PAYSTACK, "user-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentService).recordPayment(eq(42), eq(new BigDecimal("50.00")), any(), any(),
                eq("INV-42-abc"), any());
    }

    @Test
    public void verify_doesNotRecordPayment_whenGatewayDoesNotConfirm() {
        when(driverResolver.resolve(GatewayDriver.PAYSTACK)).thenReturn(paystackDriver);
        when(paystackDriver.verify("INV-42-abc")).thenReturn(false);

        ResponseEntity<?> response = controller.verify("INV-42-abc", GatewayDriver.PAYSTACK, "user-1");

        assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
        verify(paymentService, never()).recordPayment(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void verify_rejectsReference_withoutInvoice() {
        ResponseEntity<?> response = controller.verify("not-a-reference", GatewayDriver.PAYSTACK, "user-1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(paymentService, never()).recordPayment(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void webhook_ignoresCallback_whenSignatureInvalid() {
        when(driverResolver.resolve(GatewayDriver.PAYSTACK)).thenReturn(paystackDriver);
        GatewayCallbackResult callback = new GatewayCallbackResult();
        callback.setValid(false);
        callback.setMessage("signature mismatch");
        when(paystackDriver.webhook("{}", "bad-signature")).thenReturn(callback);

        ResponseEntity<?> response = controller.webhook(GatewayDriver.PAYSTACK, "{}", "bad-signature", null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentService, never()).recordPayment(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void webhook_recordsPayment_whenSignedAndConfirmed() {
        when(driverResolver.resolve(GatewayDriver.PAYSTACK)).thenReturn(paystackDriver);
        GatewayCallbackResult callback = new GatewayCallbackResult();
        callback.setValid(true);
        callback.setStatus(GatewayCallbackResult.STATUS_SUCCESS);
        callback.setReference("INV-42-abc");
        callback.setAmount(new BigDecimal("25.00"));
        when(paystackDriver.webhook("{}", "good-signature")).thenReturn(callback);
        when(paystackDriver.verify("INV-42-abc")).thenReturn(true);
        BillingPayment payment = new BillingPayment();
        when(paymentService.recordPayment(eq(42), any(), any(), any(), eq("INV-42-abc"), any())).thenReturn(payment);

        ResponseEntity<?> response = controller.webhook(GatewayDriver.PAYSTACK, "{}", "good-signature", null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentService).recordPayment(eq(42), eq(new BigDecimal("25.00")), any(), any(),
                eq("INV-42-abc"), eq("gateway-webhook"));
    }

    @Test
    public void webhook_ignoresCallback_whenGatewayVerificationFails() {
        when(driverResolver.resolve(GatewayDriver.PAYSTACK)).thenReturn(paystackDriver);
        GatewayCallbackResult callback = new GatewayCallbackResult();
        callback.setValid(true);
        callback.setStatus(GatewayCallbackResult.STATUS_SUCCESS);
        callback.setReference("INV-42-abc");
        when(paystackDriver.webhook("{}", "good-signature")).thenReturn(callback);
        when(paystackDriver.verify("INV-42-abc")).thenReturn(false);

        ResponseEntity<?> response = controller.webhook(GatewayDriver.PAYSTACK, "{}", "good-signature", null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentService, never()).recordPayment(any(), any(), any(), any(), any(), any());
    }
}
