package org.openelisglobal.billing.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingInvoiceStatus;
import org.openelisglobal.billing.valueholder.PayerType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BillingInvoiceServiceImpl implements BillingInvoiceService {

    @Autowired
    private BillingInvoiceDAO billingInvoiceDAO;

    @Autowired
    private BillingInvoiceLineDAO billingInvoiceLineDAO;

    @Override
    public BillingInvoice createInvoice(SamplePatientUpdateData updateData, String currentUserId) {
        Sample sample = updateData.getSample();
        if (sample == null) {
            return null;
        }

        String externalOrderId = resolveExternalOrderId(sample);
        if (externalOrderId == null || externalOrderId.trim().isEmpty()) {
            return null;
        }

        List<BillingInvoice> existing = billingInvoiceDAO.getByExternalOrderId(externalOrderId);
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }

        BillingInvoice invoice = new BillingInvoice();
        invoice.setExternalOrderId(externalOrderId);
        invoice.setPatientId(updateData.getPatientId());
        invoice.setStatus(BillingInvoiceStatus.DRAFT);
        invoice.setCurrency("GHS");
        invoice.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        invoice.setRecordedBy(currentUserId);
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice.setSubtotal(BigDecimal.ZERO);
        invoice.setTaxTotal(BigDecimal.ZERO);
        invoice.setDiscountTotal(BigDecimal.ZERO);
        invoice.setTotal(BigDecimal.ZERO);

        Integer invoiceId = billingInvoiceDAO.insert(invoice);
        invoice.setId(invoiceId);

        BigDecimal total = BigDecimal.ZERO;
        List<SampleTestCollection> collections = updateData.getSampleItemsTests();
        if (collections != null) {
            for (SampleTestCollection collection : collections) {
                if (collection.tests == null) {
                    continue;
                }
                for (Test test : collection.tests) {
                    BillingInvoiceLine line = new BillingInvoiceLine();
                    line.setInvoiceId(invoiceId);
                    line.setTestId(test.getId());
                    line.setTestName(test.getDescription());
                    line.setLoincCode(test.getLoinc());
                    line.setLocalCode(test.getLocalCode());
                    line.setQuantity(1);
                    billingInvoiceLineDAO.insert(line);
                }
            }
        }

        return invoice;
    }

    @Override
    @Transactional
    public BillingInvoice getInvoiceForOrder(String externalOrderId) {
        List<BillingInvoice> existing = billingInvoiceDAO.getByExternalOrderId(externalOrderId);
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }
        return null;
    }

    private String resolveExternalOrderId(Sample sample) {
        if (sample.getReferringId() != null && !sample.getReferringId().trim().isEmpty()) {
            return sample.getReferringId();
        }
        return sample.getAccessionNumber();
    }
}
