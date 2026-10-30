package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.common.dao.BaseDAO;

public interface BillingInvoiceDAO extends BaseDAO<BillingInvoice, Integer> {

    List<BillingInvoice> getByExternalOrderId(String externalOrderId);

    List<BillingInvoice> getByPatientId(String patientId);

    List<BillingInvoice> getByOrganizationId(Integer organizationId);
}
