package org.openelisglobal.billing.controller.rest;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.billing.dao.BillingTariffDAO;
import org.openelisglobal.billing.dao.BranchGatewayConfigDAO;
import org.openelisglobal.billing.dao.FacilityCredentialingDAO;
import org.openelisglobal.billing.dao.GdrgMappingDAO;
import org.openelisglobal.billing.dao.TariffVersionDAO;
import org.openelisglobal.billing.dao.PatientPolicyDAO;
import org.openelisglobal.billing.gateway.GatewayConfigService;
import org.openelisglobal.billing.security.BillingPermissions;
import org.openelisglobal.billing.service.PayerService;
import org.openelisglobal.billing.service.PolicyService;
import org.openelisglobal.billing.service.TariffSeedService;
import org.openelisglobal.billing.service.TariffService;
import org.openelisglobal.billing.valueholder.BillingPayer;
import org.openelisglobal.billing.valueholder.BillingTariff;
import org.openelisglobal.billing.valueholder.BranchGatewayConfig;
import org.openelisglobal.billing.valueholder.FacilityCredentialing;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.openelisglobal.billing.valueholder.GdrgMapping;
import org.openelisglobal.billing.valueholder.TariffVersion;
import org.openelisglobal.billing.valueholder.PatientPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reference data and configuration endpoints for the billing screens: payers,
 * patient policies, tariffs, facility credentialing, and per-branch gateway
 * configuration. Mutating endpoints belong to admin users; the UI routes are
 * gated on the admin role.
 */
@RestController
@RequestMapping("/rest/billing")
@PreAuthorize(BillingPermissions.MANAGE_CONFIG)
public class BillingConfigRestController {

    @Autowired
    private PayerService payerService;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private TariffService tariffService;

    @Autowired
    private TariffSeedService tariffSeedService;

    @Autowired
    private GatewayConfigService gatewayConfigService;

    @Autowired
    private PatientPolicyDAO patientPolicyDAO;

    @Autowired
    private BillingTariffDAO tariffDAO;

    @Autowired
    private FacilityCredentialingDAO facilityCredentialingDAO;

    @Autowired
    private GdrgMappingDAO gdrgMappingDAO;

    @Autowired
    private TariffVersionDAO tariffVersionDAO;

    @Autowired
    private BranchGatewayConfigDAO gatewayConfigDAO;

    /* ------------------------------- payers ------------------------------- */

    @GetMapping("/payers")
    public List<BillingPayer> payers() {
        return payerService.getActivePayers();
    }

    @PostMapping("/payers")
    public BillingPayer createPayer(@RequestBody Map<String, String> body) {
        return payerService.createPayer(body.get("name"), body.get("type"), body.get("config"),
                body.get("recordedBy"));
    }

    @PostMapping("/payers/{payerId}/deactivate")
    public BillingPayer deactivatePayer(@PathVariable Integer payerId) {
        return payerService.deactivatePayer(payerId);
    }

    /* ------------------------------ policies ------------------------------ */

    @GetMapping("/policies")
    public List<PatientPolicy> policies(@RequestParam String patientId) {
        return patientPolicyDAO.getActiveByPatientId(patientId);
    }

    @PostMapping("/policies")
    public PatientPolicy linkPolicy(@RequestBody Map<String, String> body) {
        Date from = parseDate(body.get("effectiveFrom"));
        Date to = parseDate(body.get("effectiveTo"));
        Integer payerId = parseInteger(body.get("payerId"));
        return policyService.linkPolicy(body.get("patientId"), payerId, body.get("memberNumber"),
                body.get("motherMemberNumber"), from, to);
    }

    @PostMapping("/policies/{policyId}/deactivate")
    public PatientPolicy deactivatePolicy(@PathVariable Integer policyId) {
        return policyService.deactivatePolicy(policyId);
    }

    /* ------------------------------ tariffs ------------------------------- */

    @GetMapping("/tariffs")
    public List<BillingTariff> tariffs(@RequestParam(required = false) String localCode,
            @RequestParam(required = false) String payerType) {
        if (localCode != null && payerType != null && !payerType.isEmpty()) {
            return tariffDAO.getActiveByLocalCodeAndPayerType(localCode, payerType);
        }
        if (localCode != null && !localCode.isEmpty()) {
            return tariffDAO.getActiveByLocalCode(localCode);
        }
        return tariffDAO.getAll();
    }

    @GetMapping("/tariffs/price")
    public Map<String, Object> tariffPrice(@RequestParam String localCode,
            @RequestParam(required = false) String payerType) {
        BigDecimal price = tariffService.getUnitPrice(localCode, payerType);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("localCode", localCode);
        result.put("payerType", payerType);
        result.put("price", price);
        return result;
    }

    @PostMapping("/tariffs")
    public BillingTariff createTariff(@RequestBody Map<String, String> body) {
        BillingTariff tariff = new BillingTariff();
        tariff.setLocalCode(body.get("localCode"));
        tariff.setTestId(body.get("testId"));
        if (body.get("price") != null && !body.get("price").isEmpty()) {
            tariff.setPrice(new BigDecimal(body.get("price")));
        }
        tariff.setPayerType(body.get("payerType"));
        tariff.setEffectiveFrom(parseDate(body.get("effectiveFrom")));
        tariff.setEffectiveTo(parseDate(body.get("effectiveTo")));
        tariff.setIsActive("Y");
        tariffDAO.insert(tariff);
        return tariff;
    }

    /* ----------------------- tariff versions ---------------------- */

    @GetMapping("/tariff-versions")
    public List<TariffVersion> tariffVersions(@RequestParam(required = false) Date onDate) {
        if (onDate != null) {
            return tariffVersionDAO.getActiveVersionsCovering(onDate);
        }
        return tariffVersionDAO.getAll();
    }

    @PostMapping("/tariff-versions")
    public TariffVersion createTariffVersion(@RequestBody Map<String, String> body) {
        TariffVersion version = new TariffVersion();
        version.setVersion(body.get("version"));
        version.setName(body.get("name"));
        version.setProviderTier(body.get("providerTier"));
        version.setEffectiveFrom(parseDate(body.get("effectiveFrom")));
        version.setEffectiveTo(parseDate(body.get("effectiveTo")));
        Boolean active = body.get("isActive") == null ? true : Boolean.parseBoolean(body.get("isActive"));
        version.setIsActive(active ? "Y" : "N");
        version.setSource(body.get("source"));
        tariffVersionDAO.insert(version);
        return version;
    }

    @PostMapping("/tariff-versions/import-csv")
    public Map<String, Object> importTariffVersionCsv(@RequestBody Map<String, String> body) {
        String version = body.get("version");
        String providerTier = body.get("providerTier");
        Date effectiveFrom = parseDate(body.get("effectiveFrom"));
        Date effectiveTo = parseDate(body.get("effectiveTo"));
        String csv = body.get("csv");
        String recordedBy = body.get("recordedBy");

        TariffSeedService.ImportSummary summary = tariffSeedService.importInveTariffs(csv, version, providerTier,
                effectiveFrom, effectiveTo, recordedBy);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("versionId", summary.versionId);
        result.put("version", summary.version);
        result.put("importedRows", summary.importedRows);
        result.put("skippedRows", summary.skippedRows);
        result.put("warnings", summary.warnings);
        return result;
    }

    /* --------------------------- GDRG mappings ---------------------------- */

    @GetMapping("/gdrg-mappings")
    public List<GdrgMapping> gdrgMappings(@RequestParam(required = false) String localCode,
            @RequestParam(required = false) Integer versionId) {
        if (localCode != null && !localCode.isEmpty()) {
            return gdrgMappingDAO.getActiveByLocalCode(localCode, null, versionId);
        }
        return gdrgMappingDAO.getAll();
    }

    /**
     * Map a local test code to its NHIA GDRG tariff code. The claim XML carries
     * the GDRG code as TreatmentCode; without a mapping the batch fails NHIA
     * validation 224.
     */
    @PostMapping("/gdrg-mappings")
    public GdrgMapping createGdrgMapping(@RequestBody Map<String, String> body) {
        GdrgMapping mapping = new GdrgMapping();
        mapping.setLocalCode(body.get("localCode"));
        mapping.setGdrgCode(body.get("gdrgCode"));
        mapping.setDescription(body.get("description"));
        mapping.setEffectiveFrom(parseDate(body.get("effectiveFrom")));
        mapping.setEffectiveTo(parseDate(body.get("effectiveTo")));
        mapping.setIsActive("Y");
        mapping.setVersionId(parseInteger(body.get("versionId")));
        if (body.get("tariff") != null && !body.get("tariff").isEmpty()) {
            mapping.setTariff(new BigDecimal(body.get("tariff")));
        }
        gdrgMappingDAO.insert(mapping);
        return mapping;
    }

    /* ------------------------ facility credentialing ---------------------- */

    @GetMapping("/facility-credentials")
    public List<FacilityCredentialing> facilityCredentials(@RequestParam Integer orgId) {
        return facilityCredentialingDAO.getByOrganizationId(orgId);
    }

    /* --------------------------- gateway config --------------------------- */

    @GetMapping("/gateway-config")
    public BranchGatewayConfig gatewayConfig(@RequestParam Integer orgId,
            @RequestParam(required = false) String driver) {
        return gatewayConfigService.getConfig(orgId, parseDriver(driver));
    }

    @PostMapping("/gateway-config")
    public BranchGatewayConfig saveGatewayConfig(@RequestBody Map<String, String> body) {
        BranchGatewayConfig config = new BranchGatewayConfig();
        config.setOrganizationId(parseInteger(body.get("organizationId")));
        config.setDriver(parseDriver(body.get("driver")).name());
        config.setDisplayName(body.get("displayName"));
        config.setPublicKey(body.get("publicKey"));
        config.setSecretKey(gatewayConfigService.encryptSecret(body.get("secretKey")));
        config.setWebhookSecret(gatewayConfigService.encryptSecret(body.get("webhookSecret")));
        config.setIsEnabled(Boolean.parseBoolean(body.getOrDefault("isEnabled", "false")));
        config.setTestMode(Boolean.parseBoolean(body.getOrDefault("testMode", "true")));
        config.setMetadata(body.get("metadata"));
        gatewayConfigDAO.insert(config);
        return config;
    }

    private static GatewayDriver parseDriver(String driver) {
        if (driver == null || driver.isEmpty()) {
            return null;
        }
        return GatewayDriver.valueOf(driver.trim().toUpperCase());
    }

    private static Date parseDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Date.valueOf(value);
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Integer.valueOf(value);
    }
}
