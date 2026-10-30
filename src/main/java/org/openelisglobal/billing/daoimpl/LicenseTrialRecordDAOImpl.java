package org.openelisglobal.billing.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.LicenseTrialRecordDAO;
import org.openelisglobal.billing.valueholder.LicenseTrialRecord;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class LicenseTrialRecordDAOImpl extends BaseDAOImpl<LicenseTrialRecord, Integer>
        implements LicenseTrialRecordDAO {

    public LicenseTrialRecordDAOImpl() {
        super(LicenseTrialRecord.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LicenseTrialRecord> getByFingerprintHash(String fingerprintHash) {
        if (fingerprintHash == null || fingerprintHash.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "from LicenseTrialRecord l where l.fingerprintHash = :fingerprintHash";
        try {
            TypedQuery<LicenseTrialRecord> query = entityManager.createQuery(sql, LicenseTrialRecord.class);
            query.setParameter("fingerprintHash", fingerprintHash);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in LicenseTrialRecord getByFingerprintHash()", e);
        }
    }
}
