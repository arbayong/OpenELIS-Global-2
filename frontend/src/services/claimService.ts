/**
 * Claim Service API Client
 *
 * NHIS claim batch build/retrieve/XML/feedback for the OpenELIS billing module.
 * JSON endpoints reuse fetchFromOpenElisServer. The XML endpoint is read as
 * plain text; the feedback endpoint posts the raw XML document.
 */

import { fetchFromOpenElisServer } from "../components/utils/Utils";
import config from "../config.json";

export type ClaimBatchStatus = "DRAFT" | "READY" | "SUBMITTED" | "PAID" | "REJECTED";
export type ClaimLineStatus = "PENDING" | "APPROVED" | "REJECTED" | "PAID";

export interface ClaimLineDto {
  id: number;
  claimBatchId: number;
  invoiceLineId?: number | null;
  patientId?: string | null;
  memberNumber?: string | null;
  serviceCode?: string | null;
  claimedAmount?: number | null;
  approvedAmount?: number | null;
  status: ClaimLineStatus;
}

export interface ClaimBatchDto {
  id: number;
  organizationId: number;
  batchNumber: string;
  batchDate?: string | null;
  status: ClaimBatchStatus;
  totalClaims: number;
  totalAmount?: number | null;
  xmlPath?: string | null;
  lastupdated?: string | null;
  lines?: ClaimLineDto[] | null;
}

const CLAIMS_BASE = "/rest/billing/claims";

export const buildClaimBatch = (
  orgId: number,
  from?: string | null,
  to?: string | null,
): Promise<ClaimBatchDto> => {
  const params = [`orgId=${orgId}`];
  if (from) {
    params.push(`from=${encodeURIComponent(from)}`);
  }
  if (to) {
    params.push(`to=${encodeURIComponent(to)}`);
  }
  return fetchFromOpenElisServer<ClaimBatchDto>(
    `${CLAIMS_BASE}/build?${params.join("&")}`,
  );
};

export const getClaimBatch = (batchId: number): Promise<ClaimBatchDto> =>
  fetchFromOpenElisServer<ClaimBatchDto>(`${CLAIMS_BASE}/${batchId}`);

/** Claim lines of a batch; the batch DTO itself carries no lines. */
export const getClaimBatchLines = (
  batchId: number,
): Promise<ClaimLineDto[]> =>
  fetchFromOpenElisServer<ClaimLineDto[]>(`${CLAIMS_BASE}/${batchId}/lines`);

export const getClaimBatchesByOrg = (
  orgId: number,
): Promise<ClaimBatchDto[]> =>
  fetchFromOpenElisServer<ClaimBatchDto[]>(`${CLAIMS_BASE}/org/${orgId}`);

/** NHIA v8.6 claim XML returned as text/xml — read as text, not JSON. */
export const getClaimBatchXml = (batchId: number): Promise<string> =>
  fetch(`${config.serverBaseUrl}${CLAIMS_BASE}/${batchId}/xml`, {
    credentials: "include",
    method: "GET",
  }).then((response) => {
    if (!response.ok) {
      throw new Error(
        `GET ${CLAIMS_BASE}/${batchId}/xml failed: HTTP ${response.status}`,
      );
    }
    return response.text();
  });

/** Posts the raw NHIA feedback XML document (Content-Type: application/xml). */
export const submitClaimFeedback = (
  batchId: number,
  xml: string,
): Promise<ClaimBatchDto> =>
  fetch(`${config.serverBaseUrl}${CLAIMS_BASE}/${batchId}/feedback`, {
    credentials: "include",
    method: "POST",
    headers: {
      "Content-Type": "application/xml",
      "X-CSRF-Token": localStorage.getItem("CSRF") || "",
    },
    body: xml,
  }).then((response) => {
    if (!response.ok) {
      throw new Error(
        `POST ${CLAIMS_BASE}/${batchId}/feedback failed: HTTP ${response.status}`,
      );
    }
    return response.json() as Promise<ClaimBatchDto>;
  });
