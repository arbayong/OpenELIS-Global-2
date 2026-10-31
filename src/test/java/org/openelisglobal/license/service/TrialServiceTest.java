package org.openelisglobal.license.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.dao.LicenseTrialRecordDAO;
import org.openelisglobal.billing.valueholder.LicenseTrialRecord;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TrialServiceTest {

    @Mock
    private LicenseTrialRecordDAO trialDao;

    @InjectMocks
    private TrialService trialService;

    private LicenseTrialRecord recordStartingOn(LocalDate start) {
        LicenseTrialRecord r = new LicenseTrialRecord();
        r.setId(1);
        r.setFingerprintHash("fp-abc");
        r.setTrialStartDate(Date.valueOf(start));
        return r;
    }

    @Test
    public void getOrCreateTrial_createsRecordWhenAbsent() {
        when(trialDao.getByFingerprintHash("fp-new")).thenReturn(Collections.emptyList());
        when(trialDao.insert(any(LicenseTrialRecord.class))).thenReturn(1);

        LicenseTrialRecord r = trialService.getOrCreateTrial("fp-new");

        assertNotNull(r);
        assertEquals("fp-new", r.getFingerprintHash());
        assertEquals(Date.valueOf(LocalDate.now()), r.getTrialStartDate());
        verify(trialDao).insert(any(LicenseTrialRecord.class));
    }

    @Test
    public void getOrCreateTrial_returnsExistingWithoutInsert() {
        LicenseTrialRecord existing = recordStartingOn(LocalDate.now());
        when(trialDao.getByFingerprintHash("fp-abc")).thenReturn(List.of(existing));

        LicenseTrialRecord r = trialService.getOrCreateTrial("fp-abc");

        assertEquals(Integer.valueOf(1), r.getId());
        verify(trialDao, never()).insert(any(LicenseTrialRecord.class));
    }

    @Test
    public void trial_isValidOnFirstDay_andRemaining14() {
        when(trialDao.getByFingerprintHash("fp-abc")).thenReturn(List.of(recordStartingOn(LocalDate.now())));

        assertTrue(trialService.isTrialValid("fp-abc"));
        assertEquals(14, trialService.trialDaysRemaining("fp-abc"));
    }

    @Test
    public void trial_isValidOnLastDay_ofTrialWindow() {
        /* The trial is 14 days, so day 13 (the 14th day of use) is still valid
           and day 14 has run out. */
        when(trialDao.getByFingerprintHash("fp-d13"))
                .thenReturn(List.of(recordStartingOn(LocalDate.now().minusDays(13))));
        assertTrue(trialService.isTrialValid("fp-d13"));
        assertEquals(1, trialService.trialDaysRemaining("fp-d13"));

        when(trialDao.getByFingerprintHash("fp-d14"))
                .thenReturn(List.of(recordStartingOn(LocalDate.now().minusDays(14))));
        assertFalse(trialService.isTrialValid("fp-d14"));
        assertEquals(0, trialService.trialDaysRemaining("fp-d14"));
    }

    @Test
    public void trial_expiredManyDaysAgo_isInvalid() {
        when(trialDao.getByFingerprintHash("fp-old"))
                .thenReturn(List.of(recordStartingOn(LocalDate.now().minusDays(60))));

        assertFalse(trialService.isTrialValid("fp-old"));
        assertEquals(0, trialService.trialDaysRemaining("fp-old"));
    }
}
