package org.openelisglobal.billing.claim.claimit;

import java.util.Map;

/**
 * A CLAIM-it HTTP transport. Sends a request and returns the status and body.
 *
 * <p>
 * The production implementation posts with no timeout longer than 30s and
 * never logs the body (it may contain claim data).
 * </p>
 */
@FunctionalInterface
public interface ClaimitTransport {

    ClaimitHttpResponse request(String method, String url, Map<String, String> headers, String body);
}
