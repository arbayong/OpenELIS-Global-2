package org.openelisglobal.billing.claim.service;

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
}
