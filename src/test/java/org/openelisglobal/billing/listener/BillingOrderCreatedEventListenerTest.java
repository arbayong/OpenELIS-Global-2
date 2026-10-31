package org.openelisglobal.billing.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.service.BillingConfigService;
import org.openelisglobal.billing.service.BillingInvoiceService;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BillingOrderCreatedEventListenerTest {

    @Mock
    private BillingInvoiceService billingInvoiceService;

    @Mock
    private BillingConfigService billingConfigService;

    @Mock
    private SamplePatientUpdateData updateData;

    @InjectMocks
    private BillingOrderCreatedEventListener listener;

    @Before
    public void setUp() {
        /* The module flag defaults to on, so the existing cases exercise the
           gate as it ships; the disabled case is covered separately below. */
        when(billingConfigService.isModuleEnabled()).thenReturn(true);
    }

    @Test
    public void onOrderCreated_skipsWhenModuleDisabled() {
        when(billingConfigService.isModuleEnabled()).thenReturn(false);

        listener.onOrderCreated(new SamplePatientUpdateDataCreatedEvent(this, updateData, null, null));

        verify(billingInvoiceService, never()).createInvoice(any(), any());
    }

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
