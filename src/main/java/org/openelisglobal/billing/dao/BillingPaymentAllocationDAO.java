package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.BillingPaymentAllocation;
import org.openelisglobal.common.dao.BaseDAO;

public interface BillingPaymentAllocationDAO extends BaseDAO<BillingPaymentAllocation, Integer> {

    List<BillingPaymentAllocation> getByPaymentId(Integer paymentId);

    List<BillingPaymentAllocation> getByInvoiceLineId(Integer invoiceLineId);
}
