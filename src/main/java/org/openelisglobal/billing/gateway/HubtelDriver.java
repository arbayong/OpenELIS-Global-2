package org.openelisglobal.billing.gateway;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.service.BillingConfigService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BranchGatewayConfig;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Hubtel payment gateway driver.
 *
 * <p>
 * <b>Vendor API assumptions (unverified). No live Hubtel API documents or
 * credentials are available yet; every constant below is a documented
 * assumption that must be corrected against the official Hubtel API reference
 * before go-live:</b>
 * <ul>
 *   <li>Base URL (live): {@code https://api.hubtel.com}</li>
 *   <li>Base URL (sandbox): {@code https://sandbox.api.hubtel.com}</li>
 *   <li>initialize: POST {@code /api/v1/payments/request} with
 *   {@code amount} in minor units, {@code callback_url}, {@code reference}
 *   and {@code description}; response {@code data.checkout_url} (checkout URL)
 *   and {@code data.reference}.</li>
 *   <li>verify: GET {@code /api/v1/payments/{reference}/status};
 *   {@code data.status} {@code "success"} or {@code "completed"} means paid.</li>
 *   <li>refund: POST {@code /api/v1/payments/{reference}/refund} with
 *   {@code amount} in minor units; {@code data.status} {@code "refunded"} or
 *   {@code "success"} means refunded.</li>
 *   <li>auth: {@code Authorization: Bearer &lt;secret_key&gt;} (assumption;
 *   Hubtel may instead use Basic auth with clientId:clientSecret).</li>
 *   <li>webhook: Hubtel callback body assumption {@code
 *   {"responseCode":"00","reference":"...","amount":"...","status":"..."}};
 *   response code {@code "00"} (or {@code 200}) means success. No shared
 *   secret scheme is assumed for callback authenticity (assumption: callbacks
 *   are trusted via registered callback URL / source allowlist), so parsed
 *   callbacks are marked valid.</li>
 * </ul>
 * <p>
 * No secrets or request bodies are ever logged.
 * </p>
 * <p>
 * <b>Interface gap:</b> see {@link PaystackDriver} — {@code verify} and
 * {@code refund} carry no organization context and resolve configuration
 * without an organization.
 * </p>
 */
@Component
public class HubtelDriver implements PaymentGatewayDriver {

    private static final String GATEWAY_NAME = "hubtel";
    private static final String BASE_URL = "https://api.hubtel.com";
    private static final String SANDBOX_BASE_URL = "https://sandbox.api.hubtel.com";
    private static final String PATH_INITIALIZE = "/api/v1/payments/request";
    private static final String PATH_VERIFY = "/api/v1/payments/";
    private static final String PATH_VERIFY_SUFFIX = "/status";
    private static final String PATH_REFUND = "/api/v1/payments/";
    private static final String PATH_REFUND_SUFFIX = "/refund";
    private static final BigDecimal MINOR_UNIT_MULTIPLIER = new BigDecimal("100");

    private final GatewayClient client;
    private final GatewayConfigService configService;
    private final BillingConfigService billingConfigService;
    private final BillingInvoiceDAO invoiceDAO;

    @Autowired
    public HubtelDriver(GatewayClient client, GatewayConfigService configService,
            BillingConfigService billingConfigService, BillingInvoiceDAO invoiceDAO) {
        this.client = client;
        this.configService = configService;
        this.billingConfigService = billingConfigService;
        this.invoiceDAO = invoiceDAO;
    }

    @Override
    public String initialize(Integer invoiceId, BigDecimal amount, String callbackUrl) {
        if (!billingConfigService.isGatewayEnabled(GATEWAY_NAME)) {
            return null;
        }
        Integer organizationId = organizationId(invoiceId);
        if (organizationId == null) {
            LogEvent.logWarn(HubtelDriver.class.getName(), "initialize",
                    "no organization found for invoice " + invoiceId);
            return null;
        }
        BranchGatewayConfig config = configService.getConfig(organizationId, GatewayDriver.HUBTEL);
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            LogEvent.logWarn(HubtelDriver.class.getName(), "initialize",
                    "no enabled Hubtel configuration for organization " + organizationId);
            return null;
        }
        String baseUrl = Boolean.TRUE.equals(config.getTestMode()) ? SANDBOX_BASE_URL : BASE_URL;
        String reference = "INV-" + invoiceId + "-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", toMinorUnits(amount));
        body.put("callback_url", callbackUrl);
        body.put("reference", reference);
        body.put("description", "OpenELIS invoice " + invoiceId);

        GatewayClient.GatewayResponse response =
                client.post(organizationId, GatewayDriver.HUBTEL, baseUrl, PATH_INITIALIZE, body);
        if (failed(response) || response.json == null) {
            LogEvent.logWarn(HubtelDriver.class.getName(), "initialize",
                    "Hubtel initialize failed, status " + response.statusCode);
            return null;
        }
        JsonNode data = response.json.path("data");
        String url = text(data.path("checkout_url"));
        if (isBlank(url)) {
            url = text(data.path("reference"));
        }
        return isBlank(url) ? null : url;
    }

    @Override
    public boolean verify(String providerTransactionId) {
        if (!billingConfigService.isGatewayEnabled(GATEWAY_NAME)) {
            return false;
        }
        BranchGatewayConfig config = configService.getConfig(null, GatewayDriver.HUBTEL);
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            LogEvent.logWarn(HubtelDriver.class.getName(), "verify",
                    "organization context is not carried by verify; no Hubtel configuration available");
            return false;
        }
        String baseUrl = Boolean.TRUE.equals(config.getTestMode()) ? SANDBOX_BASE_URL : BASE_URL;
        GatewayClient.GatewayResponse response = client.get(null, GatewayDriver.HUBTEL, baseUrl,
                PATH_VERIFY + providerTransactionId + PATH_VERIFY_SUFFIX, null);
        if (failed(response) || response.json == null) {
            return false;
        }
        JsonNode data = response.json.path("data");
        String status = text(data.path("status"));
        return "success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status);
    }

    @Override
    public boolean refund(String providerTransactionId, BigDecimal amount) {
        if (!billingConfigService.isGatewayEnabled(GATEWAY_NAME)) {
            return false;
        }
        BranchGatewayConfig config = configService.getConfig(null, GatewayDriver.HUBTEL);
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            LogEvent.logWarn(HubtelDriver.class.getName(), "refund",
                    "organization context is not carried by refund; no Hubtel configuration available");
            return false;
        }
        String baseUrl = Boolean.TRUE.equals(config.getTestMode()) ? SANDBOX_BASE_URL : BASE_URL;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", toMinorUnits(amount));
        GatewayClient.GatewayResponse response = client.post(null, GatewayDriver.HUBTEL, baseUrl,
                PATH_REFUND + providerTransactionId + PATH_REFUND_SUFFIX, body);
        if (failed(response) || response.json == null) {
            return false;
        }
        JsonNode data = response.json.path("data");
        String status = text(data.path("status"));
        return "refunded".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status);
    }

    @Override
    public GatewayCallbackResult webhook(String payload) {
        return webhook(payload, null);
    }

    @Override
    public GatewayCallbackResult webhook(String payload, String signature) {
        GatewayCallbackResult result = new GatewayCallbackResult();
        if (!billingConfigService.isGatewayEnabled(GATEWAY_NAME)) {
            result.setValid(false);
            result.setMessage("gateway disabled");
            return result;
        }
        if (payload == null) {
            result.setValid(false);
            result.setMessage("empty payload");
            return result;
        }
        // Assumption: Hubtel callbacks carry no verifiable shared-secret
        // signature; authenticity is handled by registered callback URL.
        result.setValid(true);
        JsonNode node = parse(payload);
        if (node == null) {
            result.setValid(false);
            result.setMessage("unparseable payload");
            return result;
        }
        result.setReference(text(node.path("reference")));
        JsonNode data = node.path("data");
        String responseCode = text(data.path("responseCode"));
        if (isBlank(responseCode)) {
            responseCode = text(node.path("responseCode"));
        }
        String status = text(data.path("status"));
        if (isBlank(status)) {
            status = text(node.path("status"));
        }
        result.setStatus(mapHubtelStatus(responseCode, status));
        result.setAmount(majorUnits(text(data.path("amount")), text(node.path("amount"))));
        result.setEvent("hubtel.callback");
        return result;
    }

    private Integer organizationId(Integer invoiceId) {
        if (invoiceId == null) {
            return null;
        }
        return invoiceDAO.get(invoiceId).map(BillingInvoice::getOrganizationId).orElse(null);
    }

    private static long toMinorUnits(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(MINOR_UNIT_MULTIPLIER).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static BigDecimal majorUnits(String... values) {
        for (String value : values) {
            if (value != null) {
                try {
                    return new BigDecimal(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                } catch (NumberFormatException e) {
                    // try the next value
                }
            }
        }
        return null;
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String mapHubtelStatus(String responseCode, String status) {
        if ("00".equals(responseCode) || "200".equals(responseCode)
                || "success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
            return GatewayCallbackResult.STATUS_SUCCESS;
        }
        if ("failed".equalsIgnoreCase(status) || "declined".equalsIgnoreCase(status)) {
            return GatewayCallbackResult.STATUS_FAILED;
        }
        return GatewayCallbackResult.STATUS_PENDING;
    }

    private static JsonNode parse(String payload) {
        try {
            return MAPPER.readTree(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * A gateway call that produced no response (transport failure, missing
     * configuration, or an implementation that returns null) counts as a failed
     * call. Dereferencing a null response would turn a network problem into a
     * NullPointerException inside the payment flow, which is not a result the
     * caller can act on.
     */
    private static boolean failed(GatewayClient.GatewayResponse response) {
        return response == null || !response.isSuccess();
    }
}
