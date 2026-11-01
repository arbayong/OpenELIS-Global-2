package org.openelisglobal.billing.claim.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.billing.claim.claimit.ClaimitImportReport;
import org.openelisglobal.billing.valueholder.ClaimitSettings;

/**
 * Live CLAIM-it validation: build a single encounter claim from an invoice,
 * submit it to the local CLAIM-it instance, and return its import report.
 * Also manages the reusable CLAIM-it settings row.
 */
public interface ClaimitValidationService {

    /**
     * Build one encounter claim from the given invoice and submit it to CLAIM-it.
     *
     * @param invoiceId            the billing invoice to claim
     * @param claimCheckCode       optional claim check code (OTAC)
     * @param physicianId          optional attending physician id
     * @param referringFacilityName optional referring facility name
     * @param referringFacilityId  optional referring facility id
     * @param recordedBy           who recorded the validation
     * @return the CLAIM-it import report
     * @throws LIMSRuntimeException when the invoice, its active lines, a member
     *                              number, or the CLAIM-it settings are missing
     */
    ClaimitImportReport validateInvoice(Integer invoiceId, String claimCheckCode, String physicianId,
            String referringFacilityName, String referringFacilityId, String recordedBy);

    /** The configured CLAIM-it settings with the encrypted password nulled out. */
    ClaimitSettings getSettings();

    /**
     * Create-or-update the single CLAIM-it settings row. A blank password keeps
     * the existing encrypted value.
     */
void saveSettings(String baseUrl, String username, String password, String providerId, String providerName,
        String facilityType, String region, String providerTier, String recordedBy);

/**
 * Submits several invoices as one CLAIM-it batch — the monthly vetting file.
 * Each invoice becomes one claim in the document, so one round trip validates
 * the month's encounters and CLAIM-it returns one report entry per claim.
 *
 * <p>
 * Referral details are supplied per invoice because they belong to the order,
 * not the batch: CLAIM-it rejects a diagnostic claim with no referral
 * information at all, and our invoices hold only the referring id. The map
 * value is {@code [claimCheckCode, referringFacilityName, referringFacilityId,
 * physicianId]}, any of which may be blank.
 *
 * @param invoiceIds      the invoices to claim together; order is preserved
 * @param referralDetails referral details keyed by invoice id
 * @param recordedBy      the operator performing the submission
 * @return the import report with one entry per invoice, in the same order
 * @throws LIMSRuntimeException when an invoice has no referral details entry
 */
ClaimitImportReport submitBatch(List<Integer> invoiceIds, Map<Integer, String[]> referralDetails,
        String recordedBy);

/**
 * Withdraws an invoice's claim from CLAIM-it because the order was cancelled
 * before the month closed. The encounter is deleted by the claimID we assigned
 * (the invoice's claim id), so the month's batch must not contain it.
 *
 * @param invoiceId the cancelled invoice
 * @param recordedBy the operator performing the cancellation
 */
void cancelClaim(Integer invoiceId, String recordedBy);
}
