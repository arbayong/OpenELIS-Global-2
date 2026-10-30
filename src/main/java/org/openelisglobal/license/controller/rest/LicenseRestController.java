package org.openelisglobal.license.controller.rest;

import java.util.List;
import java.util.Map;
import org.openelisglobal.license.model.License;
import org.openelisglobal.license.model.LicenseStatus;
import org.openelisglobal.license.service.FingerprintService;
import org.openelisglobal.license.service.LicenseService;
import org.openelisglobal.license.service.TrialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/license")
public class LicenseRestController {

    @Autowired
    private FingerprintService fingerprintService;

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private TrialService trialService;

    @GetMapping("/fingerprint")
    public Map<String, Object> fingerprint() {
        return Map.of("fingerprint", fingerprintService.computeFingerprint());
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        LicenseStatus status = licenseService.validate();
        String fp = fingerprintService.computeFingerprint();
        License license = licenseService.getCurrentLicense();
        List<String> features = license == null ? List.of() : license.getFeatures();
        String expiresAt = license == null ? null : license.getExpiresAt();
        return Map.of("status", status.name(), "trialValid", trialService.isTrialValid(fp),
                "trialDaysRemaining", trialService.trialDaysRemaining(fp), "features", features,
                "expiresAt", expiresAt);
    }

    @PostMapping("/activate")
    public Map<String, Object> activate(@RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String key) {
        String licenseKey = key != null ? key : body != null ? body.get("key") : null;
        licenseService.activate(licenseKey);
        return Map.of("status", licenseService.validate().name());
    }
}
