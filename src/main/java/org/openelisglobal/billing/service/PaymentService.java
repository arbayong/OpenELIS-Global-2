package org.openelisglobal.billing.service;

import java.math.BigDecimal;
import org.openelisglobal.billing.gateway.PaymentGatewayDriver;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.openelisglobal.billing.valueholder.PaymentMethod;

public interface PaymentService {

    BillingPayment recordPayment(Integer invoiceId, BigDecimal amount, PaymentMethod method, GatewayDriver gateway,
            String providerTransactionId, String recordedBy);

    BillingInvoice voidInvoiceForCancellation(Integer invoiceId, String reason, String recordedBy);
}
