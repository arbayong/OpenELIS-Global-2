package org.openelisglobal.billing.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.openelisglobal.billing.valueholder.GatewayDriver;

/**
 * Shared HTTP seam for payment gateway calls.
 *
 * <p>
 * Implementations resolve the branch configuration for the organization,
 * decrypt secrets via {@link GatewayConfigService#decryptSecret}, send JSON
 * with the Bearer/secret-key header each gateway expects, parse JSON responses
 * and route {@code testMode} configurations to the documented sandbox base URL.
 * The interface is a seam so driver tests can stub transport and never touch
 * the network.
 * </p>
 */
public interface GatewayClient {

    /**
     * POST a JSON-serialized body to {@code baseUrl + path}.
     *
     * @param organizationId branch organization used to resolve configuration;
     *        may be {@code null} when the caller carries no organization
     *        context (see {@link PaymentGatewayDriver#verify(String)})
     * @param driver gateway the call is for (selects the auth scheme)
     * @param baseUrl live or sandbox base URL, already selected by the caller
     *        from its documented constants according to
     *        {@link org.openelisglobal.billing.valueholder.BranchGatewayConfig#getTestMode()}
     * @param path endpoint path
     * @param body request body, serialized as JSON; {@code null} sends no body
     * @return the gateway response regardless of HTTP status; the caller
     *         decides what counts as success
     */
    GatewayResponse post(Integer organizationId, GatewayDriver driver, String baseUrl, String path, Object body);

    /**
     * GET from {@code baseUrl + path}. See {@link #post} for parameter
     * semantics.
     */
    GatewayResponse get(Integer organizationId, GatewayDriver driver, String baseUrl, String path);

    /**
     * POST with additional headers (e.g. {@code X-Reference},
     * {@code X-Target-Environment}).
     */
    GatewayResponse post(Integer organizationId, GatewayDriver driver, String baseUrl, String path, Object body,
            Map<String, String> extraHeaders);

    /**
     * GET with additional headers. See the POST variant for parameter
     * semantics.
     */
    GatewayResponse get(Integer organizationId, GatewayDriver driver, String baseUrl, String path,
            Map<String, String> extraHeaders);

    /**
     * Status and (parsed) body of a gateway call.
     */
    class GatewayResponse {
        public final int statusCode;
        public final String body;
        public final JsonNode json;

        public GatewayResponse(int statusCode, String body) {
            this(statusCode, body, parse(body));
        }

        GatewayResponse(int statusCode, String body, JsonNode json) {
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
            this.json = json;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }

        private static JsonNode parse(String body) {
            if (body == null || body.isBlank()) {
                return null;
            }
            try {
                return MAPPER.readTree(body);
            } catch (Exception e) {
                return null;
            }
        }

        private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
                new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
