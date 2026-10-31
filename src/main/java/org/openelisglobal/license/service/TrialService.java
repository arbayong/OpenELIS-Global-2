package org.openelisglobal.license.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.billing.dao.LicenseTrialRecordDAO;
import org.openelisglobal.billing.valueholder.LicenseTrialRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrialService {

    public static final int TRIAL_DAYS = 14;

    @Autowired
    private LicenseTrialRecordDAO trialDao;

    public boolean isTrialValid(String fingerprintHash) {
        LicenseTrialRecord record = getOrCreateTrial(fingerprintHash);
        if (record == null || record.getTrialStartDate() == null) {
            return false;
        }
        long elapsed = java.time.temporal.ChronoUnit.DAYS.between(record.getTrialStartDate().toLocalDate(), LocalDate.now());
        // strictly less than, so the trial is TRIAL_DAYS days rather than
        // granting access through day TRIAL_DAYS inclusive (15 days).
        return elapsed < TRIAL_DAYS;
    }

    public int trialDaysRemaining(String fingerprintHash) {
        LicenseTrialRecord record = getOrCreateTrial(fingerprintHash);
        if (record == null || record.getTrialStartDate() == null) {
            return 0;
        }
        long elapsed = java.time.temporal.ChronoUnit.DAYS.between(record.getTrialStartDate().toLocalDate(), LocalDate.now());
        long remaining = TRIAL_DAYS - elapsed;
        if (remaining < 0) {
            return 0;
        }
        return (int) remaining;
    }

    public LicenseTrialRecord getOrCreateTrial(String fingerprintHash) {
        List<LicenseTrialRecord> records = trialDao.getByFingerprintHash(fingerprintHash);
        if (records != null && !records.isEmpty()) {
            return records.get(0);
        }
        LicenseTrialRecord record = new LicenseTrialRecord();
        record.setFingerprintHash(fingerprintHash);
        record.setTrialStartDate(Date.valueOf(LocalDate.now()));
        record.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        trialDao.insert(record);
        return record;
    }
}
