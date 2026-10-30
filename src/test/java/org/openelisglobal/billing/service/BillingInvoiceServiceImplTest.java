package org.openelisglobal.billing.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingInvoiceStatus;
import org.openelisglobal.billing.valueholder.PayerType;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.valueholder.Test;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BillingInvoiceServiceImplTest {

    @Mock
    private BillingInvoiceDAO billingInvoiceDAO;

    @Mock
    private BillingInvoiceLineDAO billingInvoiceLineDAO;

    @Mock
    private TariffService tariffService;

    @Mock
    private PolicyService policyService;

    @Mock
    private PayerResolver payerResolver;

    @Mock
    private SamplePatientUpdateData updateData;

    @InjectMocks
    private BillingInvoiceServiceImpl service;

    private Sample sample;
    private Test test;

    @Before
    public void setUp() {
        sample = new Sample();
        sample.setReferringId("EXT-100");
        sample.setAccessionNumber("2026-00001");
        when(updateData.getSample()).thenReturn(sample);
        when(updateData.getPatientId()).thenReturn("pat-1");
        when(updateData.getCurrentUserId()).thenReturn("user-1");

        test = new Test();
        test.setId("t1");
        test.setDescription("Full Blood Count");
        test.setLoinc("718-7");
        test.setLocalCode("FBC");
        when(tariffService.getUnitPrice(any(), any())).thenReturn(new BigDecimal("50.00"));
        when(payerResolver.resolvePayerType(any(), any())).thenReturn(PayerType.CASH);
        when(payerResolver.resolveInsuranceNumber(any(), any())).thenReturn(null);
    }

    private SampleAddService.SampleTestCollection collectionWith(Test t) {
        SampleAddService.SampleTestCollection stc = new org.objenesis.ObjenesisStd()
                .newInstance(SampleAddService.SampleTestCollection.class);
        stc.tests = List.of(t);
        return stc;
    }

    @org.junit.Test
    public void createInvoice_returnsExistingInvoice_whenAlreadyInvoiced() {
        BillingInvoice existing = new BillingInvoice();
        existing.setId(7);
        existing.setExternalOrderId("EXT-100");
        when(billingInvoiceDAO.getByExternalOrderId("EXT-100")).thenReturn(List.of(existing));

        BillingInvoice result = service.createInvoice(updateData, "user-1");

        assertSame(existing, result);
        verify(billingInvoiceDAO, never()).insert(any());
        verify(billingInvoiceLineDAO, never()).insert(any());
    }

    @org.junit.Test
    public void createInvoice_createsInvoiceAndOneLinePerTest_whenNew() {
        when(billingInvoiceDAO.getByExternalOrderId("EXT-100")).thenReturn(Collections.emptyList());
        when(billingInvoiceDAO.insert(any(BillingInvoice.class))).thenReturn(1);
        when(updateData.getSampleItemsTests()).thenReturn(List.of(collectionWith(test)));

        BillingInvoice result = service.createInvoice(updateData, "user-1");

        assertNotNull(result);
        assertEquals(Integer.valueOf(1), result.getId());
        assertEquals("EXT-100", result.getExternalOrderId());
        assertEquals("pat-1", result.getPatientId());
        assertEquals(BillingInvoiceStatus.DRAFT, result.getStatus());

        ArgumentCaptor<BillingInvoice> invoiceCaptor = ArgumentCaptor.forClass(BillingInvoice.class);
        verify(billingInvoiceDAO).insert(invoiceCaptor.capture());
        assertEquals("EXT-100", invoiceCaptor.getValue().getExternalOrderId());

        ArgumentCaptor<BillingInvoiceLine> lineCaptor = ArgumentCaptor.forClass(BillingInvoiceLine.class);
        verify(billingInvoiceLineDAO).insert(lineCaptor.capture());
        BillingInvoiceLine line = lineCaptor.getValue();
        assertEquals(Integer.valueOf(1), line.getInvoiceId());
        assertEquals("t1", line.getTestId());
        assertEquals("Full Blood Count", line.getTestName());
        assertEquals("718-7", line.getLoincCode());
        assertEquals("FBC", line.getLocalCode());
        assertEquals(new BigDecimal("50.00"), line.getUnitPrice());
        assertEquals(new BigDecimal("50.00"), line.getLineTotal());
        assertEquals(new BigDecimal("50.00"), line.getPatientResponsibilityAmount());
        assertEquals(BigDecimal.ZERO, line.getInsuranceExpectedAmount());
        assertEquals(new BigDecimal("50.00"), result.getSubtotal());
        assertEquals(new BigDecimal("50.00"), result.getTotal());
        verify(billingInvoiceDAO).update(any(BillingInvoice.class));
    }

    @org.junit.Test
    public void createInvoice_returnsNull_whenSampleIsNull() {
        when(updateData.getSample()).thenReturn(null);

        assertNull(service.createInvoice(updateData, "user-1"));

        verify(billingInvoiceDAO, never()).insert(any());
    }

    @org.junit.Test
    public void createInvoice_fallsBackToAccessionNumber_whenReferringIdBlank() {
        sample.setReferringId("  ");
        when(billingInvoiceDAO.getByExternalOrderId("2026-00001")).thenReturn(Collections.emptyList());
        when(billingInvoiceDAO.insert(any(BillingInvoice.class))).thenReturn(2);
        when(updateData.getSampleItemsTests()).thenReturn(Collections.emptyList());

        BillingInvoice result = service.createInvoice(updateData, "user-1");

        assertEquals("2026-00001", result.getExternalOrderId());
        assertEquals(Integer.valueOf(2), result.getId());
    }

    @org.junit.Test
    public void createInvoice_nhisPayer_setsInsuranceSplitAndNumber() {
        when(billingInvoiceDAO.getByExternalOrderId("EXT-100")).thenReturn(Collections.emptyList());
        when(billingInvoiceDAO.insert(any(BillingInvoice.class))).thenReturn(3);
        when(updateData.getSampleItemsTests()).thenReturn(List.of(collectionWith(test)));
        when(payerResolver.resolvePayerType(any(), any())).thenReturn(PayerType.NHIS);
        when(payerResolver.resolveInsuranceNumber(any(), any())).thenReturn("NHIS-123");

        BillingInvoice result = service.createInvoice(updateData, "user-1");

        assertEquals(PayerType.NHIS, result.getPayerType());
        assertEquals("NHIS-123", result.getInsuranceNumber());
        ArgumentCaptor<BillingInvoiceLine> lineCaptor = ArgumentCaptor.forClass(BillingInvoiceLine.class);
        verify(billingInvoiceLineDAO).insert(lineCaptor.capture());
        assertEquals(BigDecimal.ZERO, lineCaptor.getValue().getPatientResponsibilityAmount());
        assertEquals(new BigDecimal("50.00"), lineCaptor.getValue().getInsuranceExpectedAmount());
    }

    @org.junit.Test
    public void getInvoiceForOrder_returnsFirstExisting() {
        BillingInvoice inv = new BillingInvoice();
        when(billingInvoiceDAO.getByExternalOrderId("X")).thenReturn(List.of(inv));

        assertSame(inv, service.getInvoiceForOrder("X"));
    }

    @org.junit.Test
    public void getInvoiceForOrder_returnsNull_whenNone() {
        when(billingInvoiceDAO.getByExternalOrderId("X")).thenReturn(Collections.emptyList());

        assertNull(service.getInvoiceForOrder("X"));
    }
}
