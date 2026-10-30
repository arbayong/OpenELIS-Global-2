package org.openelisglobal.billing.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.billing.dao.BillingPayerDAO;
import org.openelisglobal.billing.dao.PatientPolicyDAO;
import org.openelisglobal.billing.valueholder.BillingPayer;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PolicyServiceImpl implements PolicyService {

    @Autowired
    private PatientPolicyDAO patientPolicyDAO;

    @Autowired
    private BillingPayerDAO billingPayerDAO;

    @Override
    @Transactional
    public PatientPolicy getActivePolicyForPatient(String patientId) {
        try {
            Date today = new Date(System.currentTimeMillis());
            List<PatientPolicy> policies = patientPolicyDAO.getActiveByPatientId(patientId);
            if (policies != null) {
                for (PatientPolicy policy : policies) {
                    if (isActive(policy, today)) {
                        return policy;
                    }
                }
            }
            throw new LIMSRuntimeException("No active policy for patient: " + patientId);
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    private boolean isActive(PatientPolicy policy, Date today) {
        if (!"Y".equals(policy.getActive())) {
            return false;
        }
        Date from = policy.getEffectiveFrom();
        if (from != null && today.before(from)) {
            return false;
        }
        Date to = policy.getEffectiveTo();
        if (to != null && today.after(to)) {
            return false;
        }
        return true;
    }

    @Override
    @Transactional
    public BillingPayer getPayerForPatient(String patientId) {
        try {
            PatientPolicy policy = getActivePolicyForPatient(patientId);
            Integer payerId = policy.getPayerId();
            if (payerId == null) {
                throw new LIMSRuntimeException("Policy has no payer: " + policy.getId());
            }
            return billingPayerDAO.get(payerId)
                    .orElseThrow(() -> new LIMSRuntimeException("Payer not found: " + payerId));
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    @Transactional
    public PatientPolicy linkPolicy(String patientId, Integer payerId, String memberNumber,
            String motherMemberNumber, Date from, Date to) {
        try {
            billingPayerDAO.get(payerId)
                    .orElseThrow(() -> new LIMSRuntimeException("Payer not found: " + payerId));
            PatientPolicy policy = new PatientPolicy();
            policy.setPatientId(patientId);
            policy.setPayerId(payerId);
            policy.setMemberNumber(memberNumber);
            policy.setMotherMemberNumber(motherMemberNumber);
            policy.setEffectiveFrom(from);
            policy.setEffectiveTo(to);
            policy.setActive("Y");
            Integer id = patientPolicyDAO.insert(policy);
            policy.setId(id);
            return policy;
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    @Transactional
    public PatientPolicy deactivatePolicy(Integer policyId) {
        try {
            PatientPolicy policy = patientPolicyDAO.get(policyId)
                    .orElseThrow(() -> new LIMSRuntimeException("Policy not found: " + policyId));
            policy.setActive("N");
            patientPolicyDAO.update(policy);
            return policy;
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }
}
