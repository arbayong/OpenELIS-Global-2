package org.openelisglobal.billing.claim.claimit;

/**
 * CLAIM-it HTTP client: authenticates against {@code /token} and submits claim
 * XML to {@code /claims}, parsing the returned JSON import report.
 */
public interface ClaimitClient {

    /**
     * Authenticates with CLAIM-it and returns the bearer token.
     *
     * @param baseUrl    the CLAIM-it base URL, e.g. {@code http://localhost:31719/json-api}
     * @param username   the facility username
     * @param password   the facility password (never logged)
     * @return the token string
     * @throws ClaimitClientException on invalid credentials, HTTP errors, or transport failure
     */
    String authenticate(String baseUrl, String username, String password);

    /**
     * Submits claim XML to CLAIM-it and returns the parsed import report.
     *
     * @param baseUrl the CLAIM-it base URL
     * @param token   the bearer token from {@link #authenticate}
     * @param xml     the claim XML document
     * @return the import report (never {@code null})
     * @throws ClaimitClientException on transport failure, or on HTTP status >= 400 when the body is not parseable JSON
     */
    ClaimitImportReport submitClaims(String baseUrl, String token, String xml);
}
