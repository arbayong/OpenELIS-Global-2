package org.openelisglobal.billing.claim.service;

import java.util.List;
import org.openelisglobal.billing.claim.ClaimFeedbackItem;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.billing.valueholder.FacilityCredentialing;

public interface NhisXmlService {

    String generateClaimXml(ClaimBatch batch, List<ClaimLine> lines, FacilityCredentialing cred);

    List<ClaimFeedbackItem> parseFeedback(String xml);
}
