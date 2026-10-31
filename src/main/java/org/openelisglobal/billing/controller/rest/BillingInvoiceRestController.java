package org.openelisglobal.billing.controller.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.dao.BillingPaymentAllocationDAO;
import org.openelisglobal.billing.dao.BillingPaymentDAO;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.billing.valueholder.BillingPaymentAllocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only invoice endpoints backing the billing screens.
 *
 * Payments attach to invoices through BillingPaymentAllocation (payment ->
 * invoice line), so the invoice detail payload resolves payments by walking
 * the lines of the invoice.
 */
@RestController
@RequestMapping("/rest/billing/invoices")
public class BillingInvoiceRestController {

    @Autowired
    private BillingInvoiceDAO invoiceDAO;

    @Autowired
    private BillingInvoiceLineDAO invoiceLineDAO;

    @Autowired
    private BillingPaymentAllocationDAO allocationDAO;

    @Autowired
    private BillingPaymentDAO paymentDAO;

    @GetMapping
    public List<BillingInvoice> list(@RequestParam(required = false) Integer orgId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String patientId) {
        List<BillingInvoice> invoices;
        if (orgId != null) {
            invoices = invoiceDAO.getByOrganizationId(orgId);
        } else if (patientId != null && !patientId.isEmpty()) {
            invoices = invoiceDAO.getByPatientId(patientId);
        } else {
            invoices = invoiceDAO.getAll();
        }
        if (status == null || status.isEmpty()) {
            return invoices;
        }
        String wanted = status.trim().toUpperCase();
        List<BillingInvoice> filtered = new ArrayList<>();
        for (BillingInvoice invoice : invoices) {
            if (invoice.getStatus() != null && invoice.getStatus().name().equals(wanted)) {
                filtered.add(invoice);
            }
        }
        return filtered;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Integer id) {
        BillingInvoice invoice = invoiceDAO.get(id).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("invoice", invoice);
        result.put("lines", invoice == null ? List.of() : invoiceLineDAO.getByInvoiceId(id));
        result.put("payments", paymentsForInvoice(id));
        return result;
    }

    @GetMapping("/by-order/{externalOrderId}")
    public List<BillingInvoice> byOrder(@PathVariable String externalOrderId) {
        return invoiceDAO.getByExternalOrderId(externalOrderId);
    }

    private List<Map<String, Object>> paymentsForInvoice(Integer invoiceId) {
        Map<Integer, BigDecimal> allocatedByPaymentId = new LinkedHashMap<>();
        for (BillingInvoiceLine line : invoiceLineDAO.getByInvoiceId(invoiceId)) {
            if (line.getId() == null) {
                continue;
            }
            for (BillingPaymentAllocation allocation : allocationDAO.getByInvoiceLineId(line.getId())) {
                if (allocation.getPaymentId() == null || allocation.getAmount() == null) {
                    continue;
                }
                allocatedByPaymentId.merge(allocation.getPaymentId(), allocation.getAmount(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entry : allocatedByPaymentId.entrySet()) {
            BillingPayment payment = paymentDAO.get(entry.getKey()).orElse(null);
            if (payment == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("payment", payment);
            row.put("allocatedAmount", entry.getValue());
            rows.add(row);
        }
        return rows;
    }
}
