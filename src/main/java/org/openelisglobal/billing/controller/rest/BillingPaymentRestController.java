package org.openelisglobal.billing.controller.rest;

import java.math.BigDecimal;
import org.openelisglobal.billing.service.PaymentService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.openelisglobal.billing.valueholder.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/billing/payments")
public class BillingPaymentRestController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/record")
    public BillingPayment recordPayment(@RequestParam Integer invoiceId, @RequestParam BigDecimal amount,
            @RequestParam PaymentMethod method, @RequestParam(required = false) GatewayDriver gateway,
            @RequestParam(required = false) String providerTransactionId,
            @RequestParam(required = false) String recordedBy) {
        return paymentService.recordPayment(invoiceId, amount, method, gateway, providerTransactionId, recordedBy);
    }

    @PostMapping("/{invoiceId}/void")
    public BillingInvoice voidInvoice(@PathVariable Integer invoiceId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String recordedBy) {
        return paymentService.voidInvoiceForCancellation(invoiceId, reason, recordedBy);
    }
}
