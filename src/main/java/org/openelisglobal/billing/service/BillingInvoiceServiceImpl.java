package org.openelisglobal.billing.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.billing.dao.BillingConfigDAO;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.dao.BillingInvoiceLineDAO;
import org.openelisglobal.billing.valueholder.BillingConfig;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.billing.valueholder.BillingInvoiceStatus;
import org.openelisglobal.billing.valueholder.BillingPayer;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.openelisglobal.billing.valueholder.PayerType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class BillingInvoiceServiceImpl implements BillingInvoiceService {

    /**
     * Billing-config key holding the branch this installation bills for. OpenELIS
     * has no Branch entity and neither SystemUser nor Sample carries an
     * organization, so the branch is configured once per deployment rather than
     * derived from the user or the sample.
     */
    private static final String CONFIG_DEFAULT_ORGANIZATION_ID = "default.organizationId";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BillingInvoiceDAO billingInvoiceDAO;

    @Autowired
    private BillingInvoiceLineDAO billingInvoiceLineDAO;

    @Autowired
    private BillingConfigDAO billingConfigDAO;

    @Autowired
    private TariffService tariffService;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private PayerResolver payerResolver;

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

        String patientId = updateData.getPatientId();
        PayerType payerType = payerResolver.resolvePayerType(patientId, policyService);
        String insuranceNumber = payerResolver.resolveInsuranceNumber(patientId, policyService);
        Integer organizationId = resolveOrganizationId();
        String payerSnapshot = buildPayerSnapshot(patientId, payerType, insuranceNumber);

        BillingInvoice invoice = new BillingInvoice();
        invoice.setExternalOrderId(externalOrderId);
        invoice.setPatientId(patientId);
        if (organizationId != null) {
            invoice.setOrganizationId(organizationId);
        }
        invoice.setStatus(BillingInvoiceStatus.DRAFT);
        invoice.setPayerType(payerType);
        invoice.setInsuranceNumber(insuranceNumber);
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

        BigDecimal subtotal = BigDecimal.ZERO;
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
                    line.setPayerSnapshot(payerSnapshot);
                    BigDecimal unitPrice = tariffService.getUnitPrice(test.getLocalCode(), payerType.name());
                    BigDecimal lineTotal = unitPrice == null ? BigDecimal.ZERO
                            : unitPrice.multiply(BigDecimal.valueOf(line.getQuantity()));
                    line.setUnitPrice(unitPrice);
                    line.setLineTotal(lineTotal);
                    if (payerType == PayerType.CASH) {
                        line.setPatientResponsibilityAmount(lineTotal);
                        line.setInsuranceExpectedAmount(BigDecimal.ZERO);
                    } else {
                        line.setPatientResponsibilityAmount(BigDecimal.ZERO);
                        line.setInsuranceExpectedAmount(lineTotal);
                    }
                    billingInvoiceLineDAO.insert(line);
                    subtotal = subtotal.add(lineTotal);
                }
            }
        }

        invoice.setSubtotal(subtotal);
        invoice.setTotal(subtotal);
        billingInvoiceDAO.update(invoice);

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

    /**
     * The configured branch for this installation, or null when unset. An
     * unconfigured deployment keeps the previous behaviour (no branch stamped);
     * the claims, summaries and reports that filter by organization then find no
     * invoices, which is the configuration error the reports surface.
     */
    private Integer resolveOrganizationId() {
        try {
            List<BillingConfig> configs = billingConfigDAO.getByConfigKey(CONFIG_DEFAULT_ORGANIZATION_ID);
            if (configs == null || configs.isEmpty()) {
                return null;
            }
            String value = configs.get(0).getConfigValue();
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return Integer.valueOf(value.trim());
        } catch (Exception e) {
            LogEvent.logError(e);
            return null;
        }
    }

    /**
     * The payer and plan in force at order time, as compact JSON (plan §4.2).
     * Never fails invoice creation: a snapshot that cannot be built is recorded
     * as null, because billing the order matters more than auditing its coverage.
     */
    private String buildPayerSnapshot(String patientId, PayerType payerType, String insuranceNumber) {
        if (payerType == PayerType.CASH) {
            return null;
        }
        try {
            java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
            PatientPolicy policy = policyService.getActivePolicyForPatient(patientId);
            if (policy != null) {
                BillingPayer payer = policyService.getPayerForPatient(patientId);
                if (payer != null) {
                    snapshot.put("payerId", payer.getId());
                    snapshot.put("payerName", payer.getName());
                    snapshot.put("payerType", payer.getType());
                }
                snapshot.put("memberNumber", policy.getMemberNumber());
                snapshot.put("motherMemberNumber", policy.getMotherMemberNumber());
                snapshot.put("policyEffectiveFrom", String.valueOf(policy.getEffectiveFrom()));
                snapshot.put("policyEffectiveTo", String.valueOf(policy.getEffectiveTo()));
            }
            snapshot.put("resolvedPayerType", payerType.name());
            snapshot.put("insuranceNumber", insuranceNumber);
            String json = objectMapper.writeValueAsString(snapshot);
            return json.length() > 500 ? json.substring(0, 500) : json;
        } catch (Exception e) {
            LogEvent.logError(e);
            return null;
        }
    }
}
