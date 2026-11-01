package org.openelisglobal.billing.claim.claimit;

import java.util.List;

/**
 * Encodes a list of CLAIM-it encounter claims into the per-encounter claim XML
 * shape ({@code <claims><claim>...}), ported from
 * AegisDiagnostics/lib/server/claimit-encounter.ts. This is distinct from the
 * monthly batch XML (NhisXmlServiceImpl): here a lab claim is a flat
 * {@code <claims>} wrapper, not {@code Batch > ... > Claims > Claim}.
 */
public interface ClaimitEncounterXmlService {

    /**
     * Encodes the given claims into CLAIM-it encounter XML. Null or empty lists
     * produce an empty {@code <claims></claims>} envelope. Partially-filled
     * claims are encoded without throwing: optional elements are simply omitted,
     * so the caller can see exactly what CLAIM-it receives about an incomplete
     * claim.
     *
     * @param claims the claims to encode, never null in practice but treated as
     *            empty if null
     * @return the XML document as a string
     */
    String encodeEncounterXml(List<ClaimitEncounterClaim> claims);
}
