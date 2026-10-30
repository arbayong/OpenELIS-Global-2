package org.openelisglobal.billing.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.BillingPayerDAO;
import org.openelisglobal.billing.valueholder.BillingPayer;
import org.openelisglobal.billing.valueholder.PayerType;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PayerServiceImpl implements PayerService {

    @Autowired
    private BillingPayerDAO billingPayerDAO;

    @Override
    @Transactional
    public List<BillingPayer> getActivePayers() {
        try {
            List<BillingPayer> active = new ArrayList<BillingPayer>();
            for (BillingPayer payer : billingPayerDAO.getAll()) {
                if ("Y".equals(payer.getIsActive())) {
                    active.add(payer);
                }
            }
            return active;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    @Transactional
    public BillingPayer getActivePayerByType(String type) {
        try {
            List<BillingPayer> payers = billingPayerDAO.getActiveByType(type);
            if (payers == null || payers.isEmpty()) {
                throw new LIMSRuntimeException("No active payer of type: " + type);
            }
            return payers.get(0);
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    @Transactional
    public BillingPayer getNhisPayer() {
        return getActivePayerByType(PayerType.NHIS.name());
    }

    @Override
    @Transactional
    public BillingPayer createPayer(String name, String type, String configJson, String recordedBy) {
        try {
            BillingPayer payer = new BillingPayer();
            payer.setName(name);
            payer.setType(type);
            payer.setConfig(configJson);
            payer.setIsActive("Y");
            Integer id = billingPayerDAO.insert(payer);
            payer.setId(id);
            return payer;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    @Transactional
    public BillingPayer deactivatePayer(Integer payerId) {
        try {
            BillingPayer payer = billingPayerDAO.get(payerId)
                    .orElseThrow(() -> new LIMSRuntimeException("Payer not found: " + payerId));
            payer.setIsActive("N");
            billingPayerDAO.update(payer);
            return payer;
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }
    }
}
