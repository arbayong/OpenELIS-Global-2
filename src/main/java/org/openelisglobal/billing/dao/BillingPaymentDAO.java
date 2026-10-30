package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.BillingPayment;
import org.openelisglobal.common.dao.BaseDAO;

public interface BillingPaymentDAO extends BaseDAO<BillingPayment, Integer> {

    List<BillingPayment> getByOrganizationId(Integer organizationId);

    List<BillingPayment> getByPatientId(String patientId);
}
