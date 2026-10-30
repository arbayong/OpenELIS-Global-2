package org.openelisglobal.billing.service;

import java.sql.Date;
import org.openelisglobal.billing.valueholder.BillingPayer;
import org.openelisglobal.billing.valueholder.PatientPolicy;

public interface PolicyService {

    PatientPolicy getActivePolicyForPatient(String patientId);

    BillingPayer getPayerForPatient(String patientId);

    PatientPolicy linkPolicy(String patientId, Integer payerId, String memberNumber,
            String motherMemberNumber, Date from, Date to);

    PatientPolicy deactivatePolicy(Integer policyId);
}
