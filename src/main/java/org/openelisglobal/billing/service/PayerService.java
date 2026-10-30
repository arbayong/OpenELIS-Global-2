package org.openelisglobal.billing.service;

import java.util.List;
import org.openelisglobal.billing.valueholder.BillingPayer;

public interface PayerService {

    List<BillingPayer> getActivePayers();

    BillingPayer getActivePayerByType(String type);

    BillingPayer getNhisPayer();

    BillingPayer createPayer(String name, String type, String configJson, String recordedBy);

    BillingPayer deactivatePayer(Integer payerId);
}
