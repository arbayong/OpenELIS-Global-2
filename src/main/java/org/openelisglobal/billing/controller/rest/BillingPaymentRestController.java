package org.openelisglobal.billing.controller.rest;

import java.math.BigDecimal;
import org.openelisglobal.billing.security.BillingPermissions;
import org.openelisglobal.billing.service.BillingConfigService;
import org.openelisglobal.billing.service.PaymentService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.openelisglobal.billing.valueholder.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/billing/payments")
@PreAuthorize(BillingPermissions.RECORD_PAYMENTS)
public class BillingPaymentRestController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BillingConfigService billingConfigService;

    @PostMapping("/record")
    public ResponseEntity<BillingPayment> recordPayment(@RequestParam Integer invoiceId,
            @RequestParam BigDecimal amount, @RequestParam PaymentMethod method,
            @RequestParam(required = false) GatewayDriver gateway,
            @RequestParam(required = false) String providerTransactionId,
            @RequestParam(required = false) String recordedBy) {
        if (gateway != null && !billingConfigService.isGatewayEnabled(gateway.name())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(null);
        }
        return ResponseEntity.ok(
                paymentService.recordPayment(invoiceId, amount, method, gateway, providerTransactionId, recordedBy));
    }

    @PostMapping("/{invoiceId}/void")
    public BillingInvoice voidInvoice(@PathVariable Integer invoiceId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String recordedBy) {
        return paymentService.voidInvoiceForCancellation(invoiceId, reason, recordedBy);
    }
}
