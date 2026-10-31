package org.openelisglobal.billing.listener;

import org.openelisglobal.billing.service.BillingConfigService;
import org.openelisglobal.billing.service.BillingInvoiceService;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BillingOrderCreatedEventListener {

    @Autowired
    private BillingInvoiceService billingInvoiceService;

    @Autowired
    private BillingConfigService billingConfigService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(SamplePatientUpdateDataCreatedEvent event) {
        try {
            if (!billingConfigService.isModuleEnabled()) {
                return;
            }
            SamplePatientUpdateData updateData = event.getUpdateData();
            if (updateData != null) {
                billingInvoiceService.createInvoice(updateData, updateData.getCurrentUserId());
            }
        } catch (Exception e) {
            String sampleNo = event.getUpdateData() != null ? event.getUpdateData().getAccessionNumber() : "unknown";
            LogEvent.logError(BillingOrderCreatedEventListener.class.getName(), "onOrderCreated",
                    "Error creating billing invoice for sample " + sampleNo + ": " + e.getMessage());
        }
    }
}
