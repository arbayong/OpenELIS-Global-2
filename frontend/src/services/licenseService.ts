/**
 * License Service API Client
 *
 * License activation and status (trial clock, fingerprint, activation).
 * Follows the OpenELIS frontend data-fetching pattern
 * (getFromOpenElisServer / postToOpenElisServerJsonResponse).
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../components/utils/Utils";

export type LicenseStatusName =
  | "VALID"
  | "MISSING"
  | "INVALID_SIGNATURE"
  | "WRONG_MACHINE"
  | "EXPIRED"
  | (string & {});

export interface LicenseStatusResponse {
  status: LicenseStatusName;
  trialValid: boolean;
  trialDaysRemaining: number;
  features: string[];
  expiresAt: string | null;
}

export interface LicenseActivationResponse {
  status: LicenseStatusName;
}

export type ApiCallback<T = LicenseActivationResponse> = (
  response: T | undefined,
) => void;

/** Current license state: activated, trial, or locked. */
export const getLicenseStatus = (
  callback: (response: LicenseStatusResponse | undefined) => void,
  signal: AbortSignal | null = null,
): void => {
  getFromOpenElisServer<LicenseStatusResponse>(
    "/rest/license/status",
    callback,
    signal,
  );
};

/** Machine fingerprint the operator sends to the vendor for a key. */
export const getLicenseFingerprint = (
  callback: (response: { fingerprint: string } | undefined) => void,
  signal: AbortSignal | null = null,
): void => {
  getFromOpenElisServer<{ fingerprint: string }>(
    "/rest/license/fingerprint",
    callback,
    signal,
  );
};

/** Paste the vendor-issued key; revalidates and returns the new status. */
export const activateLicense = (
  key: string,
  callback: ApiCallback,
): void => {
  postToOpenElisServerJsonResponse<LicenseActivationResponse>(
    "/rest/license/activate",
    JSON.stringify({ key: key.trim() }),
    callback,
  );
};
