package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.common.dao.BaseDAO;

public interface ClaimLineDAO extends BaseDAO<ClaimLine, Integer> {

    List<ClaimLine> getByClaimBatchId(Integer claimBatchId);

    List<ClaimLine> getByInvoiceLineId(Integer invoiceLineId);
}
