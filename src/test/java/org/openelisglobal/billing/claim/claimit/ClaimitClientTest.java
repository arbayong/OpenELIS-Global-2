package org.openelisglobal.billing.claim.claimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ClaimitClientTest {

    private static final String BASE = "http://localhost:31719/json-api";

    private FakeTransport transport;
    private ClaimitClient client;

    @Before
    public void setUp() {
        transport = new FakeTransport();
        client = new ClaimitClientImpl(new ObjectMapper(), transport);
    }

    @Test
    public void authenticateReturnsTokenAndSendsCredentialsAsQueryParams() {
        transport.response = new ClaimitHttpResponse(200, "{\"token\":\"abc-123\",\"success\":true}");

        String token = client.authenticate(BASE, "facility-user", "p@ssword");

        assertEquals("abc-123", token);
        assertEquals("POST", transport.lastMethod);
        assertTrue(transport.lastUrl.startsWith(BASE + "/token?"));
        assertTrue(transport.lastUrl.contains("username=facility-user"));
        assertTrue(transport.lastUrl.contains("password=p%40ssword"));
        assertEquals("", transport.lastBody);
        assertFalse(transport.lastHeaders.containsKey("Authorization"));
    }

    @Test
    public void authenticate406ThrowsInvalidCredentials() {
        transport.response = new ClaimitHttpResponse(406, "{\"error\":\"Token generation failed\"}");

        try {
            client.authenticate(BASE, "user", "wrong");
            fail("expected ClaimitClientException");
        } catch (ClaimitClientException e) {
            assertEquals("CLAIM-it token generation failed: invalid credentials.", e.getMessage());
        }
    }

    @Test
    public void authenticate401ThrowsInvalidCredentials() {
        transport.response = new ClaimitHttpResponse(401, "");

        try {
            client.authenticate(BASE, "user", "wrong");
            fail("expected ClaimitClientException");
        } catch (ClaimitClientException e) {
            assertEquals("CLAIM-it token generation failed: invalid credentials.", e.getMessage());
        }
    }

    @Test
    public void authenticateUnexpectedStatusThrowsHttpStatusMessage() {
        transport.response = new ClaimitHttpResponse(500, "internal error");

        try {
            client.authenticate(BASE, "user", "pass");
            fail("expected ClaimitClientException");
        } catch (ClaimitClientException e) {
            assertEquals("CLAIM-it authentication failed with HTTP 500.", e.getMessage());
        }
    }

    @Test
    public void authenticate200WithoutTokenThrowsMissingTokenMessage() {
        transport.response = new ClaimitHttpResponse(200, "{\"success\":true}");

        try {
            client.authenticate(BASE, "user", "pass");
            fail("expected ClaimitClientException");
        } catch (ClaimitClientException e) {
            assertEquals("CLAIM-it token response did not contain a token.", e.getMessage());
        }
    }

    @Test
    public void submitClaimsPostsXmlWithBearerAndTextXmlAndParsesReport() {
        String xml = "<claims><claim><claimID>REF-1</claimID></claim></claims>";
        String reportJson = "{\n"
                + "  \"reportDate\": \"2026-10-31\",\n"
                + "  \"passedClaims\": 1, \"failedClaims\": 1, \"warnedClaims\": 0, \"savedClaims\": 1,\n"
                + "  \"totalCost\": \"46.82\", \"timeTaken\": \"00:00:12\",\n"
                + "  \"success\": 1,\n"
                + "  \"claims\": [\n"
                + "    {\"claim\": {\"index\": 0, \"claimID\": \"REF-1\", \"cost\": \"23.41\", \"initCost\": \"0.00\", \"outcome\": \"PASSED\", \"errors\": [], \"warnings\": []}},\n"
                + "    {\"index\": 1, \"claimId\": \"REF-2\", \"cost\": \"23.41\", \"initCost\": \"0.00\", \"outcome\": \"error\", \"errors\": [{\"code\": \"240\", \"info\": \"DIA must specify at least one investigation\", \"help\": \"\"}], \"warnings\": []}\n"
                + "  ]\n"
                + "}";
        transport.response = new ClaimitHttpResponse(200, reportJson);

        ClaimitImportReport report = client.submitClaims(BASE, "tok-123", xml);

        assertEquals(BASE + "/claims", transport.lastUrl);
        assertEquals("POST", transport.lastMethod);
        assertEquals("Bearer tok-123", transport.lastHeaders.get("Authorization"));
        assertEquals("text/xml", transport.lastHeaders.get("Content-Type"));
        assertEquals(xml, transport.lastBody);

        assertTrue(report.isSuccess());
        assertEquals("2026-10-31", report.getReportDate());
        assertEquals("00:00:12", report.getTimeTaken());
        assertEquals("46.82", report.getTotalCost());
        assertEquals(1, report.getPassedClaims());
        assertEquals(1, report.getFailedClaims());
        assertEquals(0, report.getWarnedClaims());
        assertEquals(1, report.getSavedClaims());
        assertNotNull(report.getClaims());
        assertEquals(2, report.getClaims().size());

        ClaimitImportClaim passed = report.getClaims().get(0);
        assertEquals(0, passed.getIndex());
        assertEquals("REF-1", passed.getClaimId());
        assertEquals("23.41", passed.getCost());
        assertEquals("0.00", passed.getInitCost());
        assertEquals("PASSED", passed.getOutcome());
        assertTrue(passed.getErrors().isEmpty());
        assertTrue(passed.getWarnings().isEmpty());
        assertNotNull(passed.getSource());
        assertTrue(passed.getSource().contains("REF-1"));

        ClaimitImportClaim failed = report.getClaims().get(1);
        assertEquals(1, failed.getIndex());
        assertEquals("REF-2", failed.getClaimId());
        assertEquals("ERROR", failed.getOutcome());
        assertEquals(1, failed.getErrors().size());
        ClaimitIssue issue = failed.getErrors().get(0);
        assertEquals("240", issue.getCode());
        assertEquals("DIA must specify at least one investigation", issue.getInfo());
        assertEquals("", issue.getHelp());
        assertTrue(failed.getWarnings().isEmpty());
    }

    @Test
    public void submitClaimsDefaultsCountsFromParsedClaimsWhenAbsent() {
        String reportJson = "{\n"
                + "  \"claims\": [\n"
                + "    {\"claim\": {\"outcome\": \"PASSED\"}},\n"
                + "    {\"claim\": {\"outcome\": \"ERROR\"}},\n"
                + "    {\"claim\": {\"outcome\": \"warning\"}}\n"
                + "  ]\n"
                + "}";
        transport.response = new ClaimitHttpResponse(200, reportJson);

        ClaimitImportReport report = client.submitClaims(BASE, "tok", "<claims/>");

        assertFalse(report.isSuccess());
        // counts are derived from the parsed claim outcomes, as the reference does
        assertEquals(1, report.getPassedClaims());
        assertEquals(1, report.getFailedClaims());
        assertEquals(1, report.getWarnedClaims());
        assertEquals(3, report.getSavedClaims());
        assertEquals(3, report.getClaims().size());
        assertEquals(0, report.getClaims().get(0).getIndex());
        assertEquals(1, report.getClaims().get(1).getIndex());
        assertEquals(2, report.getClaims().get(2).getIndex());
        assertNull(report.getTotalCost());
    }

    @Test
    public void submitClaimsWithNonJsonBodyReturnsFailureReportWithoutThrowing() {
        transport.response = new ClaimitHttpResponse(200, "not a json document");

        ClaimitImportReport report = client.submitClaims(BASE, "tok", "<claims/>");

        assertFalse(report.isSuccess());
        assertEquals(1, report.getClaims().size());
        ClaimitImportClaim claim = report.getClaims().get(0);
        assertEquals(0, claim.getIndex());
        assertEquals("UNKNOWN", claim.getOutcome());
        assertEquals("not a json document", claim.getSource());
        assertEquals(0, report.getPassedClaims());
        assertEquals(0, report.getFailedClaims());
        assertEquals(0, report.getWarnedClaims());
        assertEquals(1, report.getSavedClaims());
    }

    @Test
    public void submitClaimsWithNonJsonBodyAndErrorStatusThrows() {
        transport.response = new ClaimitHttpResponse(400, "garbage response");

        try {
            client.submitClaims(BASE, "tok", "<claims/>");
            fail("expected ClaimitClientException");
        } catch (ClaimitClientException e) {
            assertTrue(e.getMessage().contains("HTTP 400"));
        }
    }

    /**
     * Fake transport: records the request and returns a canned response. No
     * network is used in any test case.
     */
    private static class FakeTransport implements ClaimitTransport {

        private String lastMethod;
        private String lastUrl;
        private Map<String, String> lastHeaders;
        private String lastBody;
        private ClaimitHttpResponse response = new ClaimitHttpResponse(200, "");

        @Override
        public ClaimitHttpResponse request(String method, String url, Map<String, String> headers, String body) {
            lastMethod = method;
            lastUrl = url;
            lastHeaders = headers == null ? null : new LinkedHashMap<>(headers);
            lastBody = body;
            return response;
        }
    }
}
