package org.openelisglobal.billing.claim.claimit;

/**
 * Unchecked exception thrown when CLAIM-it authentication or claim submission
 * fails. Wraps transport failures with their original cause.
 */
public class ClaimitClientException extends RuntimeException {

    public ClaimitClientException(String message) {
        super(message);
    }

    public ClaimitClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
