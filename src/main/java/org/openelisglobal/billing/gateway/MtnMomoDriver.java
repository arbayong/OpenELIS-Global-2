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
 * MTN Mobile Money (MoMo) payment gateway driver (Collection API).
 *
 * <p>
 * <b>Vendor API assumptions (unverified). No live MTN MoMo API documents or
 * credentials are available yet; every constant below is a documented
 * assumption that must be corrected against the official MTN MoMo API
 * reference before go-live:</b>
 * <ul>
 *   <li>Base URL (live): {@code https://momodeveloper.mtn.com}</li>
 *   <li>Base URL (sandbox): {@code https://sandbox.momodeveloper.mtn.com}</li>
 *   <li>initialize (request to pay): POST
 *   {@code /collection/v1_0/requesttopay} with {@code amount} as a string of
 *   integer minor units, {@code currency} {@code GHS},
 *   {@code externalId} and {@code payerMessage}; headers
 *   {@code X-Reference} (unique reference), {@code X-Target-Environment}
 *   ({@code sandbox} or {@code production}), {@code Ocp-Apim-Subscription-Key}
 *   and {@code Authorization: Bearer &lt;access token&gt;}. MTN returns 202
 *   with no body; the reference sent in {@code X-Reference} is the checkout
 *   reference returned to the caller.</li>
 *   <li>verify: GET {@code /collection/v1_0/requesttopay/{reference}};
 *   {@code status} {@code "SUCCESS"} means paid, {@code "FAILED"} means not
 *   paid, {@code "PENDING"} means not yet.</li>
 *   <li>refund (reversal): POST
 *   {@code /collection/v1_0/requesttopay/{reference}} (assumption; the real
 *   reversal path may differ); 2xx means reversed.</li>
 *   <li>auth: {@code Authorization: Bearer &lt;secret_key&gt;} where
 *   secret_key is assumed to hold a bearer access token, plus
 *   {@code Ocp-Apim-Subscription-Key} from public_key.</li>
 *   <li>webhook: MTN callback body assumption {@code
 *   {"status":"SUCCESS","externalReference":"...","amount":"...","currency":
 *   "GHS"}}; {@code status} {@code "SUCCESS"} means paid. No shared-secret
 *   scheme is assumed for callback authenticity (assumption: callbacks are
 *   trusted via source IP / callback allowlist), so parsed callbacks are
 *   marked valid.</li>
 * </ul>
 * <p>
 * No secrets or request bodies are ever logged.
 * </p>
 * <p>
 * <b>Interface gap:</b> see {@link PaystackDriver} — {@code verify} and
 * {@code refund} carry no organization context and resolve configuration
 * without an organization. Payer (MSISDN) details are not carried by the
 * driver signature and are not sent (assumption/gap to confirm against MTN
 * requirements).
 * </p>
 */
@Component
public class MtnMomoDriver implements PaymentGatewayDriver {

    private static final String GATEWAY_NAME = "mtn_momo";
    private static final String BASE_URL = "https://momodeveloper.mtn.com";
    private static final String SANDBOX_BASE_URL = "https://sandbox.momodeveloper.mtn.com";
    private static final String CURRENCY = "GHS";
    private static final String PATH_INITIATE = "/collection/v1_0/requesttopay";
    private static final String PATH_STATUS = "/collection/v1_0/requesttopay/";
    private static final String PATH_REVERSAL = "/collection/v1_0/requesttopay/";

    private final GatewayClient client;
    private final GatewayConfigService configService;
    private final BillingConfigService billingConfigService;
    private final BillingInvoiceDAO invoiceDAO;

    @Autowired
    public MtnMomoDriver(GatewayClient client, GatewayConfigService configService,
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
            LogEvent.logWarn(MtnMomoDriver.class.getName(), "initialize",
                    "no organization found for invoice " + invoiceId);
            return null;
        }
        BranchGatewayConfig config = configService.getConfig(organizationId, GatewayDriver.MTN_MOMO);
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            LogEvent.logWarn(MtnMomoDriver.class.getName(), "initialize",
                    "no enabled MTN MoMo configuration for organization " + organizationId);
            return null;
        }
        String baseUrl = Boolean.TRUE.equals(config.getTestMode()) ? SANDBOX_BASE_URL : BASE_URL;
        String reference = "INV-" + invoiceId + "-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", minorUnitsString(amount));
        body.put("currency", CURRENCY);
        body.put("externalId", reference);
        body.put("payerMessage", "OpenELIS invoice " + invoiceId);
        body.put("payeeNote", "OpenELIS payment");
        Map<String, String> extraHeaders = new LinkedHashMap<>();
        extraHeaders.put("X-Reference", reference);
        extraHeaders.put("X-Target-Environment", Boolean.TRUE.equals(config.getTestMode()) ? "sandbox" : "production");

        GatewayClient.GatewayResponse response = client.post(organizationId, GatewayDriver.MTN_MOMO, baseUrl,
                PATH_INITIATE, body, extraHeaders);
        if (!response.isSuccess()) {
            LogEvent.logWarn(MtnMomoDriver.class.getName(), "initialize",
                    "MTN MoMo request-to-pay failed, status " + response.statusCode);
            return null;
        }
        return reference;
    }

    @Override
    public boolean verify(String providerTransactionId) {
        if (!billingConfigService.isGatewayEnabled(GATEWAY_NAME)) {
            return false;
        }
        BranchGatewayConfig config = configService.getConfig(null, GatewayDriver.MTN_MOMO);
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            LogEvent.logWarn(MtnMomoDriver.class.getName(), "verify",
                    "organization context is not carried by verify; no MTN MoMo configuration available");
            return false;
        }
        String baseUrl = Boolean.TRUE.equals(config.getTestMode()) ? SANDBOX_BASE_URL : BASE_URL;
        GatewayClient.GatewayResponse response = client.get(null, GatewayDriver.MTN_MOMO, baseUrl,
                PATH_STATUS + providerTransactionId, null);
        if (!response.isSuccess() || response.json == null) {
            return false;
        }
        String status = text(response.json.path("status"));
        return "SUCCESS".equalsIgnoreCase(status);
    }

    @Override
    public boolean refund(String providerTransactionId, BigDecimal amount) {
        if (!billingConfigService.isGatewayEnabled(GATEWAY_NAME)) {
            return false;
        }
        BranchGatewayConfig config = configService.getConfig(null, GatewayDriver.MTN_MOMO);
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            LogEvent.logWarn(MtnMomoDriver.class.getName(), "refund",
                    "organization context is not carried by refund; no MTN MoMo configuration available");
            return false;
        }
        String baseUrl = Boolean.TRUE.equals(config.getTestMode()) ? SANDBOX_BASE_URL : BASE_URL;
        GatewayClient.GatewayResponse response = client.post(null, GatewayDriver.MTN_MOMO, baseUrl,
                PATH_REVERSAL + providerTransactionId, null);
        return response.isSuccess();
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
        // Assumption: MTN MoMo callbacks carry no verifiable shared-secret
        // signature in the documents we have; authenticity is handled by
        // source IP / callback allowlist.
        result.setValid(true);
        JsonNode node = parse(payload);
        if (node == null) {
            result.setValid(false);
            result.setMessage("unparseable payload");
            return result;
        }
        result.setReference(text(node.path("externalReference")));
        result.setStatus(mapMtnStatus(text(node.path("status"))));
        result.setAmount(majorUnits(text(node.path("amount"))));
        result.setEvent("mtnmomo.callback");
        return result;
    }

    private Integer organizationId(Integer invoiceId) {
        if (invoiceId == null) {
            return null;
        }
        return invoiceDAO.get(invoiceId).map(BillingInvoice::getOrganizationId).orElse(null);
    }

    private static String minorUnitsString(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).toBigInteger().toString();
    }

    private static BigDecimal majorUnits(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static String mapMtnStatus(String status) {
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return GatewayCallbackResult.STATUS_SUCCESS;
        }
        if ("FAILED".equalsIgnoreCase(status)) {
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
}
