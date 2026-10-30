package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.LicenseTrialRecord;
import org.openelisglobal.common.dao.BaseDAO;

public interface LicenseTrialRecordDAO extends BaseDAO<LicenseTrialRecord, Integer> {

    List<LicenseTrialRecord> getByFingerprintHash(String fingerprintHash);
}
