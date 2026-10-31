package org.openelisglobal.billing.controller.rest;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.gateway.GatewayCallbackResult;
import org.openelisglobal.billing.gateway.GatewayDriverResolver;
import org.openelisglobal.billing.gateway.PaymentGatewayDriver;
import org.openelisglobal.billing.security.BillingPermissions;
import org.openelisglobal.billing.service.PaymentService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.openelisglobal.billing.valueholder.PaymentMethod;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment-gateway endpoints: start a checkout, confirm it, refund it, and
 * receive the gateway's callback.
 *
 * <p>
 * The webhook is reachable without a session because the gateway calls it, not
 * a signed-in user; it is authenticated by the vendor signature the driver
 * checks against the per-branch webhook secret. Everything else is gated on the
 * payment-recording permission, and a payment is only ever recorded after
 * {@link PaymentGatewayDriver#verify} confirms it against the gateway — the
 * callback alone never credits an invoice.
 */
@RestController
@RequestMapping("/rest/billing/payments")
public class PaymentGatewayRestController {

    /** References are built by the drivers as {@code INV-<invoiceId>-<unique>}. */
    private static final Pattern INVOICE_FROM_REFERENCE = Pattern.compile("^INV-(\\d+)-");

    @Autowired
    private GatewayDriverResolver driverResolver;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BillingInvoiceDAO billingInvoiceDAO;

    /**
     * Start a gateway checkout for an invoice. Returns the gateway's checkout
     * URL (or reference when the gateway has no hosted page) for the operator
     * to open; the payment itself is created later by verify or the webhook.
     */
    @PostMapping("/initialize")
    @PreAuthorize(BillingPermissions.RECORD_PAYMENTS)
    public ResponseEntity<Map<String, Object>> initialize(@RequestParam Integer invoiceId,
            @RequestParam GatewayDriver gateway, @RequestParam(required = false) String callbackUrl) {
        BillingInvoice invoice = billingInvoiceDAO.get(invoiceId)
                .orElseThrow(() -> new LIMSRuntimeException("Invoice not found: " + invoiceId));
        PaymentGatewayDriver driver = driverResolver.resolve(gateway);
        if (driver == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "no driver available for " + gateway));
        }
        String url = callbackUrl == null || callbackUrl.isEmpty()
                ? "https://localhost/payments/return"
                : callbackUrl;
        String reference = driver.initialize(invoiceId, invoice.getTotal(), url);
        if (reference == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "gateway did not start a transaction"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reference", reference);
        body.put("gateway", gateway.name());
        body.put("amount", invoice.getTotal());
        return ResponseEntity.ok(body);
    }

    /**
     * Confirm a gateway transaction and record the payment on the invoice it
     * belongs to. The reference carries the invoice the drivers initialized, so
     * the operator only has to paste the reference back.
     */
    @PostMapping("/verify")
    @PreAuthorize(BillingPermissions.RECORD_PAYMENTS)
    public ResponseEntity<Map<String, Object>> verify(@RequestParam String reference,
            @RequestParam GatewayDriver gateway,
            @RequestParam(required = false) String recordedBy) {
        Matcher matcher = INVOICE_FROM_REFERENCE.matcher(reference == null ? "" : reference);
        if (!matcher.find()) {
            return ResponseEntity.badRequest().body(Map.of("error", "reference does not identify an invoice"));
        }
        Integer invoiceId = Integer.valueOf(matcher.group(1));
        BillingInvoice invoice = billingInvoiceDAO.get(invoiceId)
                .orElseThrow(() -> new LIMSRuntimeException("Invoice not found: " + invoiceId));

        PaymentGatewayDriver driver = driverResolver.resolve(gateway);
        if (driver == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "no driver available for " + gateway));
        }
        if (!driver.verify(reference)) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("error", "gateway has not confirmed the reference"));
        }

        BillingPayment payment = paymentService.recordPayment(invoiceId, outstanding(invoice),
                methodFor(gateway), gateway, reference, recordedBy);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", statusOf(invoiceId));
        body.put("paymentId", payment.getId());
        body.put("reference", reference);
        return ResponseEntity.ok(body);
    }

    /**
     * Refund a confirmed gateway transaction. The refund is created by the
     * payment service so revenue reports show it against the branch.
     */
    @PostMapping("/refund")
    @PreAuthorize(BillingPermissions.RECORD_PAYMENTS)
    public ResponseEntity<Map<String, Object>> refund(@RequestParam String reference,
            @RequestParam BigDecimal amount, @RequestParam GatewayDriver gateway,
            @RequestParam Integer invoiceId, @RequestParam(required = false) String recordedBy) {
        PaymentGatewayDriver driver = driverResolver.resolve(gateway);
        if (driver == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "no driver available"));
        }
        if (!driver.refund(reference, amount)) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "gateway refused the refund"));
        }
        try {
            paymentService.voidInvoiceForCancellation(invoiceId,
                    "gateway refund " + reference, recordedBy);
        } catch (Exception e) {
            // The gateway money already moved back; the invoice void is an
            // internal correction, so it must not fail the refund response.
            LogEvent.logError("PaymentGatewayRestController", "refund", e.getMessage());
        }
        return ResponseEntity.ok(Map.of("refunded", amount.toString(), "reference", reference));
    }

    /**
     * Gateway callback. Reachable without a session; authenticity comes from the
     * signature the driver verifies against the per-branch webhook secret.
     *
     * <p>
     * A valid success callback is still confirmed with
     * {@link PaymentGatewayDriver#verify} before the payment is recorded, so a
     * forged payload cannot credit an invoice even if it passes the signature
     * check. An unparsable callback is acknowledged so the gateway stops
     * retrying it — there is nothing to reconcile against.
     */
    @PostMapping("/gateway/webhook/{gateway}")
    public ResponseEntity<Map<String, Object>> webhook(@PathVariable GatewayDriver gateway,
            @RequestBody String payload,
            @RequestHeader(value = "X-Paystack-Signature", required = false) String paystackSignature,
            @RequestHeader(value = "X-Hubtel-Signature", required = false) String hubtelSignature,
            @RequestHeader(value = "X-Mtn-Momo-Signature", required = false) String mtnSignature) {
        PaymentGatewayDriver driver = driverResolver.resolve(gateway);
        if (driver == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "unknown gateway"));
        }
        String signature = paystackSignature != null ? paystackSignature
                : hubtelSignature != null ? hubtelSignature : mtnSignature;
        GatewayCallbackResult result = driver.webhook(payload, signature);
        if (result == null || !result.isValid()) {
            return ResponseEntity.ok(Map.of("status", "IGNORED", "reason",
                    result == null ? "no result" : result.getMessage() == null ? "invalid callback"
                            : result.getMessage()));
        }
        if (!GatewayCallbackResult.STATUS_SUCCESS.equalsIgnoreCase(result.getStatus())
                || result.getReference() == null) {
            return ResponseEntity.ok(Map.of("status", "IGNORED", "callbackStatus", String.valueOf(result.getStatus())));
        }
        if (!driver.verify(result.getReference())) {
            return ResponseEntity.ok(Map.of("status", "IGNORED", "reason", "gateway did not confirm the reference"));
        }
        Matcher matcher = INVOICE_FROM_REFERENCE.matcher(result.getReference());
        if (!matcher.find()) {
            return ResponseEntity.ok(Map.of("status", "IGNORED", "reason", "reference does not identify an invoice"));
        }
        Integer invoiceId = Integer.valueOf(matcher.group(1));
        BillingInvoice invoice = billingInvoiceDAO.get(invoiceId).orElse(null);
        if (invoice == null) {
            return ResponseEntity.ok(Map.of("status", "IGNORED", "reason", "invoice not found for reference"));
        }
        BigDecimal amount = result.getAmount() != null ? result.getAmount() : outstanding(invoice);
        paymentService.recordPayment(invoiceId, amount, methodFor(gateway), gateway, result.getReference(),
                "gateway-webhook");
        return ResponseEntity.ok(Map.of("status", "RECORDED", "invoiceId", String.valueOf(invoiceId),
                "reference", result.getReference()));
    }

    /** Alias so an operator can confirm a reference from a browser or script. */
    @GetMapping("/gateway/webhook/{gateway}")
    public ResponseEntity<Map<String, Object>> webhookHelp(@PathVariable GatewayDriver gateway) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("gateway", gateway.name());
        body.put("note", "POST the gateway callback body with its signature header here");
        return ResponseEntity.ok(body);
    }

    /** Outstanding balance of a loaded invoice; an over-payment is not attempted. */
    private static BigDecimal outstanding(BillingInvoice invoice) {
        BigDecimal total = invoice.getTotal() == null ? BigDecimal.ZERO : invoice.getTotal();
        BigDecimal paid = invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid();
        return total.subtract(paid).max(BigDecimal.ZERO);
    }

    /**
     * Gateway payments are card or mobile money by convention; a site that
     * settles Hubtel through a bank channel reclassifies it in the invoice
     * history rather than here.
     */
    private static PaymentMethod methodFor(GatewayDriver driver) {
        return driver == GatewayDriver.MTN_MOMO ? PaymentMethod.MOBILE_MONEY : PaymentMethod.CARD;
    }

    private String statusOf(Integer invoiceId) {
        return billingInvoiceDAO.get(invoiceId).map(BillingInvoice::getStatus).map(Enum::name)
                .orElse("UNKNOWN");
    }
}
