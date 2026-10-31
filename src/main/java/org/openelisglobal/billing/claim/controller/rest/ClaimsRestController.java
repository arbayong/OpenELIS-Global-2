package org.openelisglobal.billing.claim.controller.rest;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.billing.claim.service.ClaimBatchService;
import org.openelisglobal.billing.claim.service.ClaimFeedbackService;
import org.openelisglobal.billing.claim.service.NhisXmlService;
import org.openelisglobal.billing.dao.ClaimLineDAO;
import org.openelisglobal.billing.dao.FacilityCredentialingDAO;
import org.openelisglobal.billing.security.BillingPermissions;
import org.openelisglobal.billing.valueholder.ClaimBatch;
import org.openelisglobal.billing.valueholder.ClaimLine;
import org.openelisglobal.billing.valueholder.FacilityCredentialing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/billing/claims")
@PreAuthorize(BillingPermissions.VIEW_CLAIMS)
public class ClaimsRestController {

    @Autowired
    private ClaimBatchService claimBatchService;

    @Autowired
    private ClaimFeedbackService claimFeedbackService;

    @Autowired
    private NhisXmlService nhisXmlService;

    @Autowired
    private ClaimLineDAO claimLineDAO;

    @Autowired
    private FacilityCredentialingDAO facilityCredentialingDAO;

    @PostMapping("/build")
    @PreAuthorize(BillingPermissions.MANAGE_CLAIMS)
    public ClaimBatch build(@RequestParam Integer orgId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Date fromDate = (from == null || from.isEmpty()) ? null : Date.valueOf(from);
        Date toDate = (to == null || to.isEmpty()) ? null : Date.valueOf(to);
        return claimBatchService.buildBatch(orgId, fromDate, toDate);
    }

    @GetMapping("/{batchId}")
    public ClaimBatch getBatch(@PathVariable Integer batchId) {
        return claimBatchService.getBatch(batchId);
    }

    @GetMapping("/{batchId}/lines")
    public List<ClaimLine> getLines(@PathVariable Integer batchId) {
        return claimLineDAO.getByClaimBatchId(batchId);
    }

    @GetMapping(value = "/{batchId}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getXml(@PathVariable Integer batchId) {
        ClaimBatch batch = claimBatchService.getBatch(batchId);
        List<ClaimLine> lines = claimLineDAO.getByClaimBatchId(batchId);
        FacilityCredentialing cred = null;
        if (batch != null) {
            List<FacilityCredentialing> credentials = facilityCredentialingDAO.getByOrganizationId(batch.getOrganizationId());
            if (credentials != null) {
                for (FacilityCredentialing c : credentials) {
                    if ("Y".equals(c.getIsActive())) {
                        cred = c;
                        break;
                    }
                }
            }
        }
        return nhisXmlService.generateClaimXml(batch, lines, cred);
    }

    @PostMapping("/{batchId}/feedback")
    @PreAuthorize(BillingPermissions.MANAGE_CLAIMS)
    public ClaimBatch feedback(@PathVariable Integer batchId, @RequestBody String xml) {
        return claimFeedbackService.applyFeedback(batchId, xml);
    }

    @GetMapping("/org/{orgId}")
    public List<ClaimBatch> getBatches(@PathVariable Integer orgId) {
        return claimBatchService.getBatches(orgId);
    }
}
