package org.openelisglobal.billing.claim.service;

import org.openelisglobal.billing.claim.ClaimFeedbackItem;
import org.openelisglobal.billing.valueholder.ClaimBatch;

public interface ClaimFeedbackService {

    ClaimBatch applyFeedback(Integer batchId, String xml);
}
