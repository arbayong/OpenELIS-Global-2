package org.openelisglobal.billing.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.service.BillingInvoiceService;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BillingOrderCreatedEventListenerTest {

    @Mock
    private BillingInvoiceService billingInvoiceService;

    @Mock
    private SamplePatientUpdateData updateData;

    @InjectMocks
    private BillingOrderCreatedEventListener listener;

    @Test
    public void onOrderCreated_callsServiceWithUpdateDataAndUser() {
        SamplePatientUpdateDataCreatedEvent event = new SamplePatientUpdateDataCreatedEvent(this, updateData, null, null);
        when(updateData.getCurrentUserId()).thenReturn("user-9");

        listener.onOrderCreated(event);

        verify(billingInvoiceService).createInvoice(updateData, "user-9");
    }

    @Test
    public void onOrderCreated_swallowsServiceException() {
        SamplePatientUpdateDataCreatedEvent event = new SamplePatientUpdateDataCreatedEvent(this, updateData, null, null);
        when(updateData.getCurrentUserId()).thenReturn("user-9");
        doThrow(new RuntimeException("boom")).when(billingInvoiceService).createInvoice(any(), eq("user-9"));

        listener.onOrderCreated(event);

        verify(billingInvoiceService).createInvoice(updateData, "user-9");
    }

    @Test
    public void onOrderCreated_ignoresNullUpdateData() {
        SamplePatientUpdateDataCreatedEvent event = new SamplePatientUpdateDataCreatedEvent(this, null, null, null);

        listener.onOrderCreated(event);

        verify(billingInvoiceService, never()).createInvoice(any(), any());
    }
}
