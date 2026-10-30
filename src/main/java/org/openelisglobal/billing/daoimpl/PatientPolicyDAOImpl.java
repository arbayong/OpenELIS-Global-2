package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.PatientPolicyDAO;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PatientPolicyDAOImpl extends BaseDAOImpl<PatientPolicy, Integer> implements PatientPolicyDAO {

    public PatientPolicyDAOImpl() {
        super(PatientPolicy.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientPolicy> getActiveByPatientId(String patientId) {
        if (patientId == null || patientId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "from PatientPolicy p where p.patientId = :patientId and p.active = 'Y'";
        try {
            TypedQuery<PatientPolicy> query = entityManager.createQuery(sql, PatientPolicy.class);
            query.setParameter("patientId", patientId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PatientPolicy getActiveByPatientId()", e);
        }
    }
}
