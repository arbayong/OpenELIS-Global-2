package org.openelisglobal.billing.claim.claimit;

/**
 * The raw result of a CLAIM-it HTTP request: the HTTP status code and the
 * response body as a string. Bodies may contain claim data and must never be
 * logged.
 */
public class ClaimitHttpResponse {

    private final int status;
    private final String body;

    public ClaimitHttpResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public int status() {
        return status;
    }

    public String body() {
        return body;
    }
}
