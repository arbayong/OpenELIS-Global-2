package org.openelisglobal.billing.service;

import org.openelisglobal.billing.valueholder.BillingPayer;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.openelisglobal.billing.valueholder.PayerType;
import org.springframework.stereotype.Service;

@Service
public class PayerResolver {

    public PayerType resolvePayerType(String patientId, PolicyService policyService) {
        try {
            PatientPolicy policy = policyService.getActivePolicyForPatient(patientId);
            if (policy == null) {
                return PayerType.CASH;
            }
            BillingPayer payer = policyService.getPayerForPatient(patientId);
            String type = payer == null ? null : payer.getType();
            if (type == null) {
                return PayerType.CASH;
            }
            String lower = type.toLowerCase();
            if ("nhis".equals(lower)) {
                return PayerType.NHIS;
            }
            if ("private".equals(lower)) {
                return PayerType.PRIVATE;
            }
            return PayerType.CASH;
        } catch (Exception e) {
            return PayerType.CASH;
        }
    }

    public String resolveInsuranceNumber(String patientId, PolicyService policyService) {
        try {
            PatientPolicy policy = policyService.getActivePolicyForPatient(patientId);
            if (policy == null) {
                return null;
            }
            return policy.getMemberNumber();
        } catch (Exception e) {
            return null;
        }
    }
}
