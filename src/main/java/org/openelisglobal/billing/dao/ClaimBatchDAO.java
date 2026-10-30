package org.openelisglobal.billing.dao;

import java.util.List;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.common.dao.BaseDAO;

public interface ClaimBatchDAO extends BaseDAO<ClaimBatch, Integer> {

    List<ClaimBatch> getByOrganizationIdAndStatus(Integer organizationId, String status);
}
