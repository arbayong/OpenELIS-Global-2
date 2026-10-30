package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.openelisglobal.common.dao.BaseDAO;

public interface PatientPolicyDAO extends BaseDAO<PatientPolicy, Integer> {

    List<PatientPolicy> getActiveByPatientId(String patientId);
}
