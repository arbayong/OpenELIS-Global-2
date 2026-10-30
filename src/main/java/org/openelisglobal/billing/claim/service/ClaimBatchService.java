package org.openelisglobal.billing.claim.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.billing.valueholder.ClaimBatch;

public interface ClaimBatchService {

    ClaimBatch buildBatch(Integer organizationId, Date from, Date to);

    ClaimBatch getBatch(Integer batchId);

    List<ClaimBatch> getBatches(Integer organizationId);
}
