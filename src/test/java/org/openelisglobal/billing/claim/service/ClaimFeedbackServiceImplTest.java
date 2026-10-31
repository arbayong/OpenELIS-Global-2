package org.openelisglobal.billing.claim.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.claim.ClaimFeedbackItem;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.BillingPaymentDAO;
import org.openelisglobal.billing.dao.ClaimBatchDAO;
import org.openelisglobal.billing.dao.ClaimLineDAO;
import org.openelisglobal.billing.dao.DailyRevenueSummaryDAO;
import org.openelisglobal.billing.service.PaymentService;
import org.openelisglobal.billing.service.RevenueReportServiceImpl;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingInvoiceStatus;
import org.openelisglobal.billing.valueholder.BillingLineStatus;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.billing.valueholder.ClaimBatchStatus;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.billing.valueholder.ClaimLineStatus;
import org.openelisglobal.billing.valueholder.PaymentMethod;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ClaimFeedbackServiceImplTest {

    @Mock
    private ClaimBatchDAO claimBatchDAO;

    @Mock
    private ClaimLineDAO claimLineDAO;

    @Mock
    private BillingInvoiceLineDAO billingInvoiceLineDAO;

    @Mock
    private BillingInvoiceDAO billingInvoiceDAO;

    @Mock
    private BillingPaymentDAO billingPaymentDAO;

    @Mock
    private NhisXmlService nhisXmlService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private ClaimFeedbackServiceImpl service;

    @Mock
    private DailyRevenueSummaryDAO dailyRevenueSummaryDAO;

    @InjectMocks
    private RevenueReportServiceImpl reportService;

    @Before
    public void setUp() {
        when(paymentService.recordPayment(any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new BillingPayment());
    }

    private ClaimBatch batch() {
        ClaimBatch batch = new ClaimBatch();
        batch.setId(1);
        batch.setOrganizationId(7);
        batch.setBatchNumber("BATCH-1");
        batch.setStatus(ClaimBatchStatus.READY);
        return batch;
    }

    private ClaimLine line(Integer id, Integer invoiceLineId, BigDecimal claimed, BigDecimal approved) {
        ClaimLine line = new ClaimLine();
        line.setId(id);
        line.setClaimBatchId(1);
        line.setInvoiceLineId(invoiceLineId);
        line.setPatientId("p1");
        line.setClaimedAmount(claimed);
        line.setApprovedAmount(approved);
        line.setStatus(ClaimLineStatus.PENDING);
        return line;
    }

    private BillingInvoiceLine invoiceLine(Integer id, Integer invoiceId) {
        BillingInvoiceLine line = new BillingInvoiceLine();
        line.setId(id);
        line.setInvoiceId(invoiceId);
        line.setLineTotal(new BigDecimal("100.00"));
        line.setLineStatus(BillingLineStatus.ACTIVE);
        return line;
    }

    @Test
    public void applyFeedback_recordsSettlementPaymentPerInvoice_withApprovedTotal() {
        ClaimBatch batch = batch();
        ClaimLine line1 = line(1, 10, new BigDecimal("100.00"), null);
        ClaimLine line2 = line(2, 11, new BigDecimal("60.00"), null);
        BillingInvoiceLine invLine1 = invoiceLine(10, 1);
        BillingInvoiceLine invLine2 = invoiceLine(11, 2);
        BillingInvoice invoice1 = new BillingInvoice();
        invoice1.setId(1);
        invoice1.setOrganizationId(7);
        BillingInvoice invoice2 = new BillingInvoice();
        invoice2.setId(2);
        invoice2.setOrganizationId(7);

        ClaimFeedbackItem item1 = new ClaimFeedbackItem();
        item1.setClaimLineId(1);
        item1.setApprovedAmount(new BigDecimal("50.00"));
        item1.setStatus("APPROVED");
        ClaimFeedbackItem item2 = new ClaimFeedbackItem();
        item2.setClaimLineId(2);
        item2.setApprovedAmount(new BigDecimal("30.00"));
        item2.setStatus("APPROVED");

        when(nhisXmlService.parseFeedback("xml")).thenReturn(List.of(item1, item2));
        when(claimBatchDAO.get(1)).thenReturn(Optional.of(batch));
        when(claimLineDAO.getByClaimBatchId(1)).thenReturn(List.of(line1, line2));
        when(billingInvoiceLineDAO.get(10)).thenReturn(Optional.of(invLine1));
        when(billingInvoiceLineDAO.get(11)).thenReturn(Optional.of(invLine2));
        when(claimBatchDAO.update(batch)).thenReturn(batch);
        when(billingInvoiceDAO.get(1)).thenReturn(Optional.of(invoice1));
        when(billingInvoiceDAO.get(2)).thenReturn(Optional.of(invoice2));
        when(billingInvoiceLineDAO.getByInvoiceId(1)).thenReturn(List.of(invLine1));
        when(billingInvoiceLineDAO.getByInvoiceId(2)).thenReturn(List.of(invLine2));

        service.applyFeedback(1, "xml");

        verify(paymentService).recordPayment(1, new BigDecimal("50.00"), PaymentMethod.BANK_TRANSFER, null,
                "nhis:1", "nhis-settlement");
        verify(paymentService).recordPayment(2, new BigDecimal("30.00"), PaymentMethod.BANK_TRANSFER, null,
                "nhis:1", "nhis-settlement");

        ArgumentCaptor<BillingPayment> payCap = ArgumentCaptor.forClass(BillingPayment.class);
        verify(billingPaymentDAO, times(2)).update(payCap.capture());
        assertEquals(2, payCap.getAllValues().size());
        for (BillingPayment p : payCap.getAllValues()) {
            assertNotNull(p.getMetadata());
            assertTrue(p.getMetadata().contains("claimBatchId"));
            assertTrue(p.getMetadata().contains("lineCount"));
        }
    }

    @Test
    public void applyFeedback_settlementFailure_doesNotFailFeedback() {
        ClaimBatch batch = batch();
        ClaimLine line1 = line(1, 10, new BigDecimal("100.00"), null);
        BillingInvoiceLine invLine1 = invoiceLine(10, 1);
        BillingInvoice invoice1 = new BillingInvoice();
        invoice1.setId(1);
        invoice1.setOrganizationId(7);

        ClaimFeedbackItem item1 = new ClaimFeedbackItem();
        item1.setClaimLineId(1);
        item1.setApprovedAmount(new BigDecimal("50.00"));
        item1.setStatus("APPROVED");

        when(nhisXmlService.parseFeedback("xml")).thenReturn(List.of(item1));
        when(claimBatchDAO.get(1)).thenReturn(Optional.of(batch));
        when(claimLineDAO.getByClaimBatchId(1)).thenReturn(List.of(line1));
        when(billingInvoiceLineDAO.get(10)).thenReturn(Optional.of(invLine1));
        when(claimBatchDAO.update(batch)).thenReturn(batch);
        when(billingInvoiceDAO.get(1)).thenReturn(Optional.of(invoice1));
        when(billingInvoiceLineDAO.getByInvoiceId(1)).thenReturn(List.of(invLine1));
        when(paymentService.recordPayment(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("gateway down"));

        ClaimBatch result = service.applyFeedback(1, "xml");

        assertNotNull(result);
        assertEquals(ClaimBatchStatus.PAID, result.getStatus());
        verify(claimBatchDAO).update(batch);
    }

    @Test
    public void applyFeedback_recomputesInvoiceStatusToPaid_whenFullyApproved() {
        ClaimBatch batch = batch();
        ClaimLine line1 = line(1, 10, new BigDecimal("100.00"), null);
        BillingInvoiceLine invLine1 = invoiceLine(10, 1);
        invLine1.setLineTotal(new BigDecimal("100.00"));
        BillingInvoice invoice1 = new BillingInvoice();
        invoice1.setId(1);
        invoice1.setOrganizationId(7);
        invoice1.setSubtotal(new BigDecimal("200.00"));
        invoice1.setTotal(new BigDecimal("200.00"));
        invoice1.setAmountPaid(new BigDecimal("100.00"));
        invoice1.setStatus(BillingInvoiceStatus.ISSUED);

        ClaimFeedbackItem item1 = new ClaimFeedbackItem();
        item1.setClaimLineId(1);
        item1.setApprovedAmount(new BigDecimal("100.00"));
        item1.setStatus("APPROVED");

        when(nhisXmlService.parseFeedback("xml")).thenReturn(List.of(item1));
        when(claimBatchDAO.get(1)).thenReturn(Optional.of(batch));
        when(claimLineDAO.getByClaimBatchId(1)).thenReturn(List.of(line1));
        when(billingInvoiceLineDAO.get(10)).thenReturn(Optional.of(invLine1));
        when(claimBatchDAO.update(batch)).thenReturn(batch);
        when(billingInvoiceDAO.get(1)).thenReturn(Optional.of(invoice1));
        when(billingInvoiceLineDAO.getByInvoiceId(1)).thenReturn(List.of(invLine1));

        service.applyFeedback(1, "xml");

        ArgumentCaptor<BillingInvoice> invCap = ArgumentCaptor.forClass(BillingInvoice.class);
        verify(billingInvoiceDAO).update(invCap.capture());
        BillingInvoice updated = invCap.getValue();
        assertEquals(new BigDecimal("100.00"), updated.getSubtotal());
        assertEquals(new BigDecimal("100.00"), updated.getTotal());
        assertEquals(new BigDecimal("100.00"), updated.getAmountPaid());
        assertEquals(BillingInvoiceStatus.PAID, updated.getStatus());
    }

    @Test
    public void claimReconciliation_mapsBatchToSubmittedAndApprovedAmounts() {
        ClaimBatch batch = new ClaimBatch();
        batch.setId(1);
        batch.setOrganizationId(7);
        batch.setBatchNumber("BATCH-1");
        batch.setBatchDate(Date.valueOf("2026-01-15"));
        batch.setStatus(ClaimBatchStatus.PAID);
        batch.setTotalClaims(3);

        ClaimLine l1 = new ClaimLine();
        l1.setId(1);
        l1.setClaimBatchId(1);
        l1.setStatus(ClaimLineStatus.APPROVED);
        l1.setClaimedAmount(new BigDecimal("100.00"));
        l1.setApprovedAmount(new BigDecimal("100.00"));

        ClaimLine l2 = new ClaimLine();
        l2.setId(2);
        l2.setClaimBatchId(1);
        l2.setStatus(ClaimLineStatus.REJECTED);
        l2.setClaimedAmount(new BigDecimal("50.00"));
        l2.setApprovedAmount(null);

        ClaimLine l3 = new ClaimLine();
        l3.setId(3);
        l3.setClaimBatchId(1);
        l3.setStatus(ClaimLineStatus.PAID);
        l3.setClaimedAmount(new BigDecimal("200.00"));
        l3.setApprovedAmount(new BigDecimal("150.00"));

        when(claimBatchDAO.getAllMatchingOrdered("organizationId", 7, "id", true)).thenReturn(List.of(batch));
        when(claimLineDAO.getByClaimBatchId(1)).thenReturn(List.of(l1, l2, l3));

        List<MapRow> rows = toRows(reportService.getClaimReconciliation(7, Date.valueOf("2026-01-01"),
                Date.valueOf("2026-01-31")));

        assertEquals(1, rows.size());
        MapRow row = rows.get(0);
        assertEquals("BATCH-1", row.batchNumber);
        assertEquals(ClaimBatchStatus.PAID, row.status);
        assertEquals(Integer.valueOf(3), row.totalClaims);
        assertEquals(Integer.valueOf(3), row.submittedCount);
        assertEquals(new BigDecimal("350.00"), row.submittedAmount);
        assertEquals(Integer.valueOf(2), row.approvedCount);
        assertEquals(new BigDecimal("250.00"), row.approvedAmount);
        assertEquals(Integer.valueOf(1), row.rejectedCount);
        assertEquals(new BigDecimal("50.00"), row.rejectedAmount);
        assertEquals(Integer.valueOf(1), row.paidCount);
        assertEquals(new BigDecimal("150.00"), row.paidAmount);
    }

    private static List<MapRow> toRows(List<Map<String, Object>> raw) {
        List<MapRow> rows = new ArrayList<>();
        for (Map<String, Object> r : raw) {
            rows.add(new MapRow(r));
        }
        return rows;
    }

    private static class MapRow {
        final String batchNumber;
        final Date batchDate;
        final ClaimBatchStatus status;
        final Integer totalClaims;
        final Integer submittedCount;
        final BigDecimal submittedAmount;
        final Integer approvedCount;
        final BigDecimal approvedAmount;
        final Integer rejectedCount;
        final BigDecimal rejectedAmount;
        final Integer paidCount;
        final BigDecimal paidAmount;

        MapRow(Map<String, Object> r) {
            this.batchNumber = (String) r.get("batchNumber");
            this.batchDate = (Date) r.get("batchDate");
            this.status = (ClaimBatchStatus) r.get("status");
            this.totalClaims = (Integer) r.get("totalClaims");
            this.submittedCount = (Integer) r.get("submittedCount");
            this.submittedAmount = (BigDecimal) r.get("submittedAmount");
            this.approvedCount = (Integer) r.get("approvedCount");
            this.approvedAmount = (BigDecimal) r.get("approvedAmount");
            this.rejectedCount = (Integer) r.get("rejectedCount");
            this.rejectedAmount = (BigDecimal) r.get("rejectedAmount");
            this.paidCount = (Integer) r.get("paidCount");
            this.paidAmount = (BigDecimal) r.get("paidAmount");
        }
    }
}
