package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.BillingInvoiceLine;
import org.openelisglobal.common.dao.BaseDAO;

public interface BillingInvoiceLineDAO extends BaseDAO<BillingInvoiceLine, Integer> {

    List<BillingInvoiceLine> getByInvoiceId(Integer invoiceId);
}
