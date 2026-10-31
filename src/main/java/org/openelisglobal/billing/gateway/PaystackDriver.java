package org.openelisglobal.billing.gateway;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
 * Paystack payment gateway driver.
 *
 * <p>
 * <b>Vendor API assumptions (unverified). No live Paystack API documents or
 * credentials are available yet; every constant below is a documented
 * assumption that must be corrected against the official Paystack API
 * reference before go-live:</b>
 * <ul>
 *   <li>Base URL (live): {@code https://api.paystack.co}</li>
 *   <li>Base URL (sandbox): Paystack publishes no separate sandbox host; test
 *   mode is selected by configuring test secret/webhook keys, so the same host
 *   is used for both modes.</li>
 *   <li>initialize: POST {@code /transaction/initialize} with
 *   {@code amount} in pesewa (minor units), {@code callback_url},
 *   {@code reference} and {@code metadata}; response
 *   {@code data.authorization_url} (checkout URL) and {@code data.reference}.
 *   Customer email is not carried by the driver signature and is not sent.</li>
 *   <li>verify: GET {@code /transaction/verify/{reference}};
 *   {@code data.status} equal to {@code "success"} means paid.</li>
 *   <li>refund: POST {@code /refund} with {@code transaction} and
 *   {@code amount} in pesewa; {@code data.status} equal to {@code "success"}
 *   means refunded.</li>
 *   <li>auth: {@code Authorization: Bearer &lt;secret_key&gt;}.</li>
 *   <li>webhook: the raw POST body is authenticated with HMAC-SHA512 using the
 *   webhook secret and compared with the {@code x-paystack-signature} header;
 *   event {@code charge.success} with {@code data.status} {@code "success"}
 *   means paid. Webhook amounts are in pesewa and are converted to major units.</li>
 * </ul>
 * <p>
 * No secrets or request bodies are ever logged.
 * </p>
 * <p>
 * <b>Interface gap:</b> {@link #verify(String)} and {@link #refund(String,
 * BigDecimal)} carry no organization or invoice context, so the branch
 * configuration cannot be scoped to a branch. They resolve configuration
 * without an organization and are no-ops in production until the interface is
 * extended with an organization identifier. Tests stub the configuration
 * directly.
 * </p>
 */
@Component
public class PaystackDriver implements PaymentGatewayDriver {

    private static final String GATEWAY_NAME = "paystack";
    private static final String BASE_URL = "https://api.paystack.co";
    private static final String SANDBOX_BASE_URL = BASE_URL;
    private static final String PATH_INITIALIZE = "/transaction/initialize";
    private static final String PATH_VERIFY = "/transaction/verify/";
    private static final String PATH_REFUND = "/refund";
    private static final BigDecimal MINOR_UNIT_MULTIPLIER = new BigDecimal("100");

    private final GatewayClient client;
    private final GatewayConfigService configService;
    private final BillingConfigService billingConfigService;
    private final BillingInvoiceDAO invoiceDAO;

    @Autowired
    public PaystackDriver(GatewayClient client, GatewayConfigService configService,
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
            LogEvent.logWarn(PaystackDriver.class.getName(), "initialize",
                    "no organization found for invoice " + invoiceId);
            return null;
        }
        BranchGatewayConfig config = configService.getConfig(organizationId, GatewayDriver.PAYSTACK);
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            LogEvent.logWarn(PaystackDriver.class.getName(), "initialize",
                    "no enabled Paystack configuration for organization " + organizationId);
            return null;
        }
        String baseUrl = Boolean.TRUE.equals(config.getTestMode()) ? SANDBOX_BASE_URL : BASE_URL;
        String reference = "INV-" + invoiceId + "-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", toPesewa(amount));
        body.put("callback_url", callbackUrl);
        body.put("reference", reference);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("invoice_id", invoiceId);
        body.put("metadata", metadata);

        GatewayClient.GatewayResponse response =
                client.post(organizationId, GatewayDriver.PAYSTACK, baseUrl, PATH_INITIALIZE, body);
        if (failed(response) || response.json == null) {
            LogEvent.logWarn(PaystackDriver.class.getName(), "initialize",
                    "Paystack initialize failed, status " + response.statusCode);
            return null;
        }
        JsonNode data = response.json.path("data");
        String url = text(data.path("authorization_url"));
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
        BranchGatewayConfig config = configService.getConfig(null, GatewayDriver.PAYSTACK);
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            LogEvent.logWarn(PaystackDriver.class.getName(), "verify",
                    "organization context is not carried by verify; no Paystack configuration available");
            return false;
        }
        String baseUrl = Boolean.TRUE.equals(config.getTestMode()) ? SANDBOX_BASE_URL : BASE_URL;
        GatewayClient.GatewayResponse response =
                client.get(null, GatewayDriver.PAYSTACK, baseUrl, PATH_VERIFY + providerTransactionId, null);
        if (failed(response) || response.json == null) {
            return false;
        }
        JsonNode data = response.json.path("data");
        return "success".equalsIgnoreCase(text(data.path("status")));
    }

    @Override
    public boolean refund(String providerTransactionId, BigDecimal amount) {
        if (!billingConfigService.isGatewayEnabled(GATEWAY_NAME)) {
            return false;
        }
        BranchGatewayConfig config = configService.getConfig(null, GatewayDriver.PAYSTACK);
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            LogEvent.logWarn(PaystackDriver.class.getName(), "refund",
                    "organization context is not carried by refund; no Paystack configuration available");
            return false;
        }
        String baseUrl = Boolean.TRUE.equals(config.getTestMode()) ? SANDBOX_BASE_URL : BASE_URL;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction", providerTransactionId);
        body.put("amount", toPesewa(amount));
        GatewayClient.GatewayResponse response =
                client.post(null, GatewayDriver.PAYSTACK, baseUrl, PATH_REFUND, body);
        if (failed(response) || response.json == null) {
            return false;
        }
        JsonNode data = response.json.path("data");
        return "success".equalsIgnoreCase(text(data.path("status")));
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
        BranchGatewayConfig config = configService.getConfig(null, GatewayDriver.PAYSTACK);
        String webhookSecret = config == null ? null : configService.decryptSecret(config.getWebhookSecret());
        if (signature == null) {
            result.setValid(false);
            result.setMessage("signature header not provided");
            return result;
        }
        if (webhookSecret == null) {
            result.setValid(false);
            result.setMessage("webhook secret not configured");
            return result;
        }
        String expected = hmacSha512Hex(webhookSecret, payload.getBytes(StandardCharsets.UTF_8));
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            result.setValid(false);
            result.setMessage("signature mismatch");
            return result;
        }
        result.setValid(true);
        JsonNode node = parse(payload);
        JsonNode data = node == null ? null : node.path("data");
        result.setEvent(node == null ? null : text(node.path("event")));
        result.setReference(data == null ? null : text(data.path("reference")));
        result.setStatus(mapPaystackStatus(data == null ? null : text(data.path("status"))));
        Long amountPesewa = data == null ? null : data.path("amount").asLong(-1L);
        result.setAmount(amountPesewa == null || amountPesewa < 0 ? null : majorUnits(amountPesewa));
        return result;
    }

    private Integer organizationId(Integer invoiceId) {
        if (invoiceId == null) {
            return null;
        }
        return invoiceDAO.get(invoiceId).map(BillingInvoice::getOrganizationId).orElse(null);
    }

    private static long toPesewa(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(MINOR_UNIT_MULTIPLIER).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static BigDecimal majorUnits(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, 2);
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String mapPaystackStatus(String status) {
        if (status == null) {
            return GatewayCallbackResult.STATUS_PENDING;
        }
        switch (status.toLowerCase()) {
            case "success":
                return GatewayCallbackResult.STATUS_SUCCESS;
            case "failed":
            case "abandoned":
                return GatewayCallbackResult.STATUS_FAILED;
            default:
                return GatewayCallbackResult.STATUS_PENDING;
        }
    }

    private static JsonNode parse(String payload) {
        try {
            return MAPPER.readTree(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private static String hmacSha512Hex(String secret, byte[] data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return hex(mac.doFinal(data));
        } catch (Exception e) {
            LogEvent.logError(e);
            return null;
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
