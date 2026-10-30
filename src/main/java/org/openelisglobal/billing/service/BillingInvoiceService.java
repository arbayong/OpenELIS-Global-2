package org.openelisglobal.billing.service;

import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;

public interface BillingInvoiceService {

    BillingInvoice createInvoice(SamplePatientUpdateData updateData, String currentUserId);

    BillingInvoice getInvoiceForOrder(String externalOrderId);
}
