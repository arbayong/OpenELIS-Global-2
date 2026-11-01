package org.openelisglobal.billing.claim.claimit;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ClaimitClientImpl implements ClaimitClient {

    private static final String TOKEN_PATH = "/token";
    private static final String CLAIMS_PATH = "/claims";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final String MISSING_TOKEN_MESSAGE = "CLAIM-it token response did not contain a token.";
    private static final String INVALID_CREDENTIALS_MESSAGE = "CLAIM-it token generation failed: invalid credentials.";

    private static final String[] CLAIM_ID_KEYS = {
            "claimID", "claimId", "claim_identification_number", "claimIdentificationNumber"
    };

    private final ObjectMapper objectMapper;
    private final ClaimitTransport transport;

    @Autowired
    public ClaimitClientImpl(ObjectMapper objectMapper) {
        this(objectMapper, defaultTransport());
    }

    public ClaimitClientImpl(ObjectMapper objectMapper, ClaimitTransport transport) {
        this.objectMapper = objectMapper;
        this.transport = transport;
    }

    @Override
    public String authenticate(String baseUrl, String username, String password) {
        try {
            String url = stripTrailingSlash(baseUrl) + TOKEN_PATH
                    + "?username=" + URLEncoder.encode(username != null ? username : "", StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(password != null ? password : "", StandardCharsets.UTF_8);
            ClaimitHttpResponse response = transport.request("POST", url, Map.of(), "");
            int status = response.status();
            String body = response.body();

            if (status == 200) {
                try {
                    JsonNode node = objectMapper.readTree(body);
                    JsonNode token = node == null ? null : node.get("token");
                    if (token != null && token.isTextual() && token.asText().length() > 0) {
                        return token.asText();
                    }
                } catch (JsonProcessingException ignored) {
                    // fall through to the missing-token error
                }
                throw new ClaimitClientException(MISSING_TOKEN_MESSAGE);
            }

            if (status == 406 || status == 401) {
                throw new ClaimitClientException(INVALID_CREDENTIALS_MESSAGE);
            }

            throw new ClaimitClientException("CLAIM-it authentication failed with HTTP " + status + ".");
        } catch (ClaimitClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClaimitClientException("CLAIM-it authentication transport failure.", e);
        }
    }

    @Override
    public ClaimitImportReport submitClaims(String baseUrl, String token, String xml) {
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Bearer " + token);
            headers.put("Content-Type", "text/xml");

            ClaimitHttpResponse response = transport.request("POST", stripTrailingSlash(baseUrl) + CLAIMS_PATH, headers, xml);
            int status = response.status();
            String body = response.body();

            ClaimitImportReport report;
            if (body == null || body.isEmpty()) {
                report = emptyReport();
            } else {
                try {
                    JsonNode root = objectMapper.readTree(body);
                    report = (root != null && root.isObject()) ? parseReport(root) : emptyReport();
                } catch (JsonProcessingException e) {
                    if (status >= 400) {
                        throw new ClaimitClientException("CLAIM-it claim submission returned HTTP " + status + ".", e);
                    }
                    report = reportWithRawBody(body);
                }
            }
            return report;
        } catch (ClaimitClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClaimitClientException("CLAIM-it claim submission transport failure.", e);
        }
    }

    private static ClaimitTransport defaultTransport() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        return (method, url, headers, body) -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(REQUEST_TIMEOUT)
                        .method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
                if (headers != null) {
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        builder.header(header.getKey(), header.getValue());
                    }
                }
                HttpResponse<String> httpResponse = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                return new ClaimitHttpResponse(httpResponse.statusCode(), httpResponse.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ClaimitClientException("CLAIM-it transport request interrupted.", e);
            } catch (IOException e) {
                throw new ClaimitClientException("CLAIM-it transport request failed.", e);
            }
        };
    }

    private static String stripTrailingSlash(String baseUrl) {
        return baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
    }

    private ClaimitImportReport parseReport(JsonNode root) {
        ClaimitImportReport report = new ClaimitImportReport();
        List<ClaimitImportClaim> claims = parseClaims(root);
        report.setClaims(claims);
        report.setPassedClaims(intOrDefault(root.get("passedClaims"), countOutcome(claims, "PASSED")));
        report.setFailedClaims(intOrDefault(root.get("failedClaims"), countOutcome(claims, "ERROR")));
        report.setWarnedClaims(intOrDefault(root.get("warnedClaims"), countOutcome(claims, "WARNING")));
        report.setSavedClaims(intOrDefault(root.get("savedClaims"), claims.size()));
        report.setTotalCost(totalCost(root.get("totalCost")));
        report.setReportDate(textOrNull(root.get("reportDate")));
        report.setTimeTaken(textOrNull(root.get("timeTaken")));
        report.setSuccess(isSuccess(root.get("success")));
        return report;
    }

    private List<ClaimitImportClaim> parseClaims(JsonNode root) {
        List<ClaimitImportClaim> claims = new ArrayList<>();
        JsonNode rawClaims = root.get("claims");
        if (rawClaims == null || !rawClaims.isArray()) {
            return claims;
        }
        for (JsonNode entry : rawClaims) {
            if (entry == null || !entry.isObject()) {
                continue;
            }
            JsonNode claim = entry.has("claim") && entry.get("claim").isObject() ? entry.get("claim") : entry;
            ClaimitImportClaim claimBean = new ClaimitImportClaim();
            claimBean.setIndex(parseIndex(claim, claims.size()));
            claimBean.setClaimId(pickClaimId(claim));
            claimBean.setCost(textOrNull(claim.get("cost")));
            claimBean.setInitCost(textOrNull(claim.get("initCost")));
            claimBean.setOutcome(mapOutcome(claim.get("outcome")));
            claimBean.setErrors(normalizeIssues(claim.get("errors")));
            claimBean.setWarnings(normalizeIssues(claim.get("warnings")));
            claimBean.setSource(sourceText(claim));
            claims.add(claimBean);
        }
        return claims;
    }

    private static int parseIndex(JsonNode claim, int fallback) {
        JsonNode index = claim.get("index");
        if (index != null && index.isNumber()) {
            return index.asInt();
        }
        if (index != null && index.isTextual()) {
            try {
                return Integer.parseInt(index.asText());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String pickClaimId(JsonNode claim) {
        for (String key : CLAIM_ID_KEYS) {
            JsonNode value = claim.get(key);
            if (value != null && value.isTextual() && value.asText().length() > 0) {
                return value.asText();
            }
        }
        return null;
    }

    private static String mapOutcome(JsonNode value) {
        if (value == null || !value.isTextual()) {
            return "UNKNOWN";
        }
        switch (value.asText().toUpperCase()) {
            case "PASSED":
                return "PASSED";
            case "WARNING":
                return "WARNING";
            case "ERROR":
                return "ERROR";
            default:
                return "UNKNOWN";
        }
    }

    private List<ClaimitIssue> normalizeIssues(JsonNode node) {
        List<ClaimitIssue> issues = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return issues;
        }
        for (JsonNode item : node) {
            ClaimitIssue issue = new ClaimitIssue();
            if (item != null && item.isObject()) {
                issue.setCode(textOrNull(item.get("code")) == null ? "" : textOrNull(item.get("code")));
                issue.setInfo(textOrNull(item.get("info")) == null ? "" : textOrNull(item.get("info")));
                JsonNode help = item.get("help");
                issue.setHelp(help != null && help.isTextual() ? help.asText() : null);
            } else {
                issue.setCode("");
                issue.setInfo("");
                issue.setHelp(null);
            }
            issues.add(issue);
        }
        return issues;
    }

    private static String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private String sourceText(JsonNode claim) {
        try {
            return claim == null ? null : objectMapper.writeValueAsString(claim);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static int intOrDefault(JsonNode node, int fallback) {
        return (node != null && node.isNumber()) ? node.asInt() : fallback;
    }

    private static int countOutcome(List<ClaimitImportClaim> claims, String outcome) {
        int count = 0;
        for (ClaimitImportClaim claim : claims) {
            if (outcome.equals(claim.getOutcome())) {
                count++;
            }
        }
        return count;
    }

    private static String totalCost(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.asText();
        }
        return null;
    }

    private static boolean isSuccess(JsonNode node) {
        if (node == null) {
            return false;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return node.isIntegralNumber() && node.intValue() == 1;
    }

    private static ClaimitImportReport emptyReport() {
        ClaimitImportReport report = new ClaimitImportReport();
        report.setClaims(new ArrayList<>());
        report.setPassedClaims(0);
        report.setFailedClaims(0);
        report.setWarnedClaims(0);
        report.setSavedClaims(0);
        report.setSuccess(false);
        return report;
    }

    private ClaimitImportReport reportWithRawBody(String body) {
        ClaimitImportReport report = emptyReport();
        ClaimitImportClaim claim = new ClaimitImportClaim();
        claim.setIndex(0);
        claim.setOutcome("UNKNOWN");
        claim.setErrors(new ArrayList<>());
        claim.setWarnings(new ArrayList<>());
        claim.setSource(body);
        List<ClaimitImportClaim> claims = new ArrayList<>();
        claims.add(claim);
        report.setClaims(claims);
        report.setSavedClaims(claims.size());
        report.setSuccess(false);
        return report;
    }
}
