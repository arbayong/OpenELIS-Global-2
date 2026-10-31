package org.openelisglobal.billing.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openelisglobal.billing.valueholder.BranchGatewayConfig;
import org.openelisglobal.billing.valueholder.GatewayDriver;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared HTTP client for payment gateway calls.
 *
 * <p>
 * Uses the JDK {@link HttpClient} (no new dependency; the same transport is
 * already used by the analyzer bridge integration in this repo) and works
 * unchanged inside the Tomcat WAR. Resolves the branch configuration for the
 * organization, decrypts the secret key via
 * {@link GatewayConfigService#decryptSecret}, builds the gateway-specific
 * authorization header, sends JSON and parses JSON responses. Secrets and
 * request bodies are never logged.
 * </p>
 */
@Component
public class GatewayClientImpl implements GatewayClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

    @Autowired
    private GatewayConfigService configService;

    @Override
    public GatewayResponse post(Integer organizationId, GatewayDriver driver, String baseUrl, String path, Object body) {
        return post(organizationId, driver, baseUrl, path, body, null);
    }

    @Override
    public GatewayResponse get(Integer organizationId, GatewayDriver driver, String baseUrl, String path) {
        return get(organizationId, driver, baseUrl, path, null);
    }

    @Override
    public GatewayResponse post(Integer organizationId, GatewayDriver driver, String baseUrl, String path, Object body,
            Map<String, String> extraHeaders) {
        try {
            BranchGatewayConfig config = configService.getConfig(organizationId, driver);
            if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
                return new GatewayResponse(0, "no enabled gateway configuration for organization " + organizationId, null);
            }
            String secret = configService.decryptSecret(config.getSecretKey());
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Bearer " + secret);
            if (driver == GatewayDriver.MTN_MOMO && config.getPublicKey() != null) {
                // MTN MoMo assumption: public_key is the Ocp-Apim-Subscription-Key.
                headers.put("Ocp-Apim-Subscription-Key", config.getPublicKey());
            }
            if (extraHeaders != null) {
                headers.putAll(extraHeaders);
            }
            String json = body == null ? null : MAPPER.writeValueAsString(body);
            return send("POST", baseUrl + path, json, headers);
        } catch (Exception e) {
            LogEvent.logWarn("GatewayClientImpl", "post", "request preparation failed: " + safeMessage(e));
            return new GatewayResponse(0, "request preparation failed", null);
        }
    }

    @Override
    public GatewayResponse get(Integer organizationId, GatewayDriver driver, String baseUrl, String path,
            Map<String, String> extraHeaders) {
        try {
            BranchGatewayConfig config = configService.getConfig(organizationId, driver);
            if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
                return new GatewayResponse(0, "no enabled gateway configuration for organization " + organizationId, null);
            }
            String secret = configService.decryptSecret(config.getSecretKey());
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Bearer " + secret);
            if (driver == GatewayDriver.MTN_MOMO && config.getPublicKey() != null) {
                headers.put("Ocp-Apim-Subscription-Key", config.getPublicKey());
            }
            if (extraHeaders != null) {
                headers.putAll(extraHeaders);
            }
            return send("GET", baseUrl + path, null, headers);
        } catch (Exception e) {
            LogEvent.logWarn("GatewayClientImpl", "get", "request preparation failed: " + safeMessage(e));
            return new GatewayResponse(0, "request preparation failed", null);
        }
    }

    private GatewayResponse send(String method, String url, String jsonBody, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).timeout(READ_TIMEOUT);
            if (jsonBody != null) {
                builder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
                builder.header("Content-Type", "application/json");
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            if (headers != null) {
                headers.forEach(builder::header);
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new GatewayResponse(response.statusCode(), response.body() == null ? "" : response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new GatewayResponse(0, "request interrupted", null);
        } catch (Exception e) {
            // Never log headers (they carry bearer tokens) or the request body.
            LogEvent.logWarn("GatewayClientImpl", "send", method + " " + url + " failed: " + safeMessage(e));
            return new GatewayResponse(0, "request failed", null);
        }
    }

    private static String safeMessage(Exception e) {
        return e == null ? "unknown error" : (e.getMessage() == null ? e.getClass().getName() : e.getMessage());
    }
}
