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

    /**
     * Removes one submitted encounter from CLAIM-it, keyed by the claimID we
     * sent. Used when an order is cancelled before month end: the claim must
     * not go to NHIA with it. Idempotent — CLAIM-it reports success for a
     * claimID it no longer holds, so this is safe to retry.
     *
     * @param baseUrl the CLAIM-it base URL
     * @param token   the bearer token
     * @param claimId the claimID of the encounter to remove
     * @return true when CLAIM-it reports success
     * @throws ClaimitClientException on transport failure or an error response
     */
    boolean deleteClaim(String baseUrl, String token, String claimId);

    /**
     * Removes every encounter CLAIM-it holds for one service month, identified
     * by year and month. The recovery path for a batch that must not reach
     * NHIA; a month's claims are re-submitted afterwards.
     *
     * @param baseUrl the CLAIM-it base URL
     * @param token   the bearer token
     * @param year    the service year, e.g. 2026
     * @param month   the service month, 1-12
     * @return true when CLAIM-it reports success
     * @throws ClaimitClientException on transport failure or an error response
     */
    boolean deleteBatch(String baseUrl, String token, int year, int month);
}
