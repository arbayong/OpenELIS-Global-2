package org.openelisglobal.billing.claim.controller.rest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.billing.claim.claimit.ClaimitImportClaim;
import org.openelisglobal.billing.claim.claimit.ClaimitImportReport;
import org.openelisglobal.billing.claim.claimit.ClaimitIssue;
import org.openelisglobal.billing.claim.service.ClaimitValidationService;
import org.openelisglobal.billing.security.BillingPermissions;
import org.openelisglobal.billing.valueholder.ClaimitSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/billing/claims")
public class ClaimitController {

    @Autowired
    private ClaimitValidationService claimitValidationService;

    @PostMapping("/live-validate")
    @PreAuthorize(BillingPermissions.MANAGE_CLAIMS)
    public Map<String, Object> liveValidate(@RequestParam Integer invoiceId,
            @RequestParam(required = false) String claimCheckCode,
            @RequestParam(required = false) String physicianId,
            @RequestParam(required = false) String referringFacilityName,
            @RequestParam(required = false) String referringFacilityId,
            @RequestParam(required = false) String recordedBy) {
        ClaimitImportReport report = claimitValidationService.validateInvoice(invoiceId, claimCheckCode, physicianId,
                referringFacilityName, referringFacilityId, recordedBy);

        List<ClaimitImportClaim> claims = report.getClaims() == null ? List.of() : report.getClaims();
        String status = claims.isEmpty() ? "NO_CLAIMS" : claims.get(0).getOutcome();

        List<Map<String, Object>> claimList = new ArrayList<Map<String, Object>>();
        for (ClaimitImportClaim claim : claims) {
            Map<String, Object> claimMap = new LinkedHashMap<String, Object>();
            claimMap.put("index", claim.getIndex());
            claimMap.put("claimId", claim.getClaimId());
            claimMap.put("outcome", claim.getOutcome());
            claimMap.put("cost", claim.getCost());
            claimMap.put("initCost", claim.getInitCost());

            List<ClaimitIssue> errors = claim.getErrors() == null ? List.of() : claim.getErrors();
            List<Map<String, String>> errorMaps = new ArrayList<Map<String, String>>();
            for (ClaimitIssue error : errors) {
                Map<String, String> errorMap = new LinkedHashMap<String, String>();
                errorMap.put("code", error.getCode());
                errorMap.put("info", error.getInfo());
                errorMaps.add(errorMap);
            }
            claimMap.put("errors", errorMaps);

            List<ClaimitIssue> warnings = claim.getWarnings() == null ? List.of() : claim.getWarnings();
            List<Map<String, String>> warningMaps = new ArrayList<Map<String, String>>();
            for (ClaimitIssue warning : warnings) {
                Map<String, String> warningMap = new LinkedHashMap<String, String>();
                warningMap.put("code", warning.getCode());
                warningMap.put("info", warning.getInfo());
                warningMaps.add(warningMap);
            }
            claimMap.put("warnings", warningMaps);

            claimList.add(claimMap);
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("status", status);
        payload.put("success", report.isSuccess());
        payload.put("passedClaims", report.getPassedClaims());
        payload.put("failedClaims", report.getFailedClaims());
        payload.put("savedClaims", report.getSavedClaims());
        payload.put("totalCost", report.getTotalCost());
        payload.put("claims", claimList);
        return payload;
    }

    @GetMapping("/claimit/settings")
    @PreAuthorize(BillingPermissions.MANAGE_CONFIG)
    public Map<String, Object> getSettings() {
        ClaimitSettings settings = claimitValidationService.getSettings();
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("configured", settings != null);
        if (settings != null) {
            payload.put("baseUrl", settings.getBaseUrl());
            payload.put("username", settings.getUsername());
            payload.put("providerId", settings.getProviderId());
            payload.put("providerName", settings.getProviderName());
            payload.put("facilityType", settings.getFacilityType());
            payload.put("region", settings.getRegion());
            payload.put("providerTier", settings.getProviderTier());
        }
        return payload;
    }

    @PutMapping("/claimit/settings")
    @PreAuthorize(BillingPermissions.MANAGE_CONFIG)
    public Map<String, Object> saveSettings(@RequestBody Map<String, String> body) {
        claimitValidationService.saveSettings(body.get("baseUrl"), body.get("username"), body.get("password"),
                body.get("providerId"), body.get("providerName"), body.get("facilityType"), body.get("region"),
                body.get("providerTier"), body.get("recordedBy"));
        return getSettings();
    }
}
