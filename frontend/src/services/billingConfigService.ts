/**
 * Billing Configuration Service API Client
 *
 * Typed clients for the OpenELIS billing configuration module:
 *   GET    /rest/billing/payers                                 -> BillingPayer[]
 *   POST   /rest/billing/payers  body {name,type,config,recordedBy} -> BillingPayer
 *   POST   /rest/billing/payers/{id}/deactivate                 -> BillingPayer
 *   GET    /rest/billing/policies?patientId=                      -> PatientPolicy[]
 *   POST   /rest/billing/policies  body {...}                    -> PatientPolicy
 *   POST   /rest/billing/policies/{id}/deactivate                 -> PatientPolicy
 *   GET    /rest/billing/tariffs?localCode=&payerType=            -> BillingTariff[]
 *   GET    /rest/billing/tariffs/price?localCode=&payerType=      -> { localCode, payerType, price }
 *   POST   /rest/billing/tariffs  body {...}                      -> BillingTariff
 *   GET    /rest/billing/facility-credentials?orgId=              -> FacilityCredential[]
 *   GET    /rest/billing/gateway-config?orgId=&driver=            -> BranchGatewayConfig
 *   POST   /rest/billing/gateway-config  body {...}               -> BranchGatewayConfig
 *
 * Follows the OpenELIS frontend data-fetching pattern
 * (getFromOpenElisServer / postToOpenElisServerJsonResponse).
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../components/utils/Utils";

export type PayerType = "CASH" | "NHIS" | "PRIVATE" | (string & {});
export type GatewayDriver = "PAYSTACK" | "HUBTEL" | "MTN_MOMO" | (string & {});

export interface BillingPayer {
  id?: number;
  name?: string | null;
  type?: PayerType | null;
  config?: string | null;
  isActive?: string | null;
}

export interface PatientPolicy {
  id?: number;
  patientId?: string | null;
  payerId?: number | null;
  memberNumber?: string | null;
  motherMemberNumber?: string | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  active?: string | null;
}

export interface BillingTariff {
  id?: number;
  localCode?: string | null;
  testId?: string | null;
  price?: number | null;
  payerType?: PayerType | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  isActive?: string | null;
}

export interface TariffPrice {
  localCode?: string | null;
  payerType?: PayerType | null;
  price?: number | null;
}

export interface FacilityCredential {
  id?: number;
  organizationId?: number | null;
  technicalHeadName?: string | null;
  technicalHeadLicenseNumber?: string | null;
  prescribingLevel?: string | null;
  specialities?: string | null;
  accreditationNumber?: string | null;
  levelOfCare?: string | null;
  validFrom?: string | null;
  validTo?: string | null;
  isActive?: string | null;
  sourceFile?: string | null;
  importedAt?: string | null;
}

export interface BranchGatewayConfig {
  id?: number;
  organizationId?: number | null;
  driver?: GatewayDriver | null;
  displayName?: string | null;
  publicKey?: string | null;
  secretKey?: string | null;
  webhookSecret?: string | null;
  isEnabled?: boolean | null;
  testMode?: boolean | null;
  metadata?: string | null;
}

type ApiCallback<T = unknown> = (response: T | undefined) => void;

/** List all payers. */
export const getPayers = (
  callback: ApiCallback<BillingPayer[]>,
  signal: AbortSignal | null = null,
): void => {
  getFromOpenElisServer<BillingPayer[]>(
    "/rest/billing/payers",
    (response) => callback(response ?? []),
    signal,
  );
};

/** Create a payer. Body: { name, type, config, recordedBy }. */
export const createPayer = (
  payer: {
    name: string;
    type: PayerType;
    config: string;
    recordedBy: string;
  },
  callback: ApiCallback<BillingPayer>,
): void => {
  postToOpenElisServerJsonResponse<BillingPayer>(
    "/rest/billing/payers",
    JSON.stringify(payer),
    callback,
  );
};

/** Deactivate a payer. */
export const deactivatePayer = (
  payerId: number | string,
  callback: ApiCallback<BillingPayer>,
): void => {
  postToOpenElisServerJsonResponse<BillingPayer>(
    `/rest/billing/payers/${encodeURIComponent(String(payerId))}/deactivate`,
    JSON.stringify({}),
    callback,
  );
};

/** List patient policies for a patient. */
export const getPatientPolicies = (
  patientId: string,
  callback: ApiCallback<PatientPolicy[]>,
  signal: AbortSignal | null = null,
): void => {
  const params = new URLSearchParams();
  if (patientId) {
    params.append("patientId", patientId);
  }
  getFromOpenElisServer<PatientPolicy[]>(
    `/rest/billing/policies?${params.toString()}`,
    (response) => callback(response ?? []),
    signal,
  );
};

/** Link a policy to a patient. Body: { patientId, payerId, memberNumber, motherMemberNumber, effectiveFrom, effectiveTo }. */
export const createPatientPolicy = (
  policy: {
    patientId: string;
    payerId: number;
    memberNumber: string;
    motherMemberNumber: string;
    effectiveFrom: string;
    effectiveTo: string;
  },
  callback: ApiCallback<PatientPolicy>,
): void => {
  postToOpenElisServerJsonResponse<PatientPolicy>(
    "/rest/billing/policies",
    JSON.stringify(policy),
    callback,
  );
};

/** Deactivate a patient policy. */
export const deactivatePatientPolicy = (
  policyId: number | string,
  callback: ApiCallback<PatientPolicy>,
): void => {
  postToOpenElisServerJsonResponse<PatientPolicy>(
    `/rest/billing/policies/${encodeURIComponent(String(policyId))}/deactivate`,
    JSON.stringify({}),
    callback,
  );
};

/** List tariffs with optional localCode / payerType filters. */
export const getTariffs = (
  filters: { localCode?: string; payerType?: PayerType } = {},
  callback: ApiCallback<BillingTariff[]>,
  signal: AbortSignal | null = null,
): void => {
  const params = new URLSearchParams();
  if (filters.localCode) {
    params.append("localCode", filters.localCode);
  }
  if (filters.payerType) {
    params.append("payerType", filters.payerType);
  }
  const endpoint = params.toString()
    ? `/rest/billing/tariffs?${params.toString()}`
    : "/rest/billing/tariffs";
  getFromOpenElisServer<BillingTariff[]>(
    endpoint,
    (response) => callback(response ?? []),
    signal,
  );
};

/** Resolve a tariff price for a local code / payer type. */
export const getTariffPrice = (
  localCode: string,
  payerType: PayerType,
  callback: ApiCallback<TariffPrice>,
  signal: AbortSignal | null = null,
): void => {
  const params = new URLSearchParams();
  if (localCode) {
    params.append("localCode", localCode);
  }
  if (payerType) {
    params.append("payerType", payerType);
  }
  getFromOpenElisServer<TariffPrice>(
    `/rest/billing/tariffs/price?${params.toString()}`,
    callback,
    signal,
  );
};

/** Create a tariff. Body: { localCode, testId, price, payerType, effectiveFrom, effectiveTo }. */
export const createTariff = (
  tariff: {
    localCode: string;
    testId: string;
    price: number;
    payerType: PayerType;
    effectiveFrom: string;
    effectiveTo: string;
  },
  callback: ApiCallback<BillingTariff>,
): void => {
  postToOpenElisServerJsonResponse<BillingTariff>(
    "/rest/billing/tariffs",
    JSON.stringify(tariff),
    callback,
  );
};

/** List facility credentials for an organization. */
export const getFacilityCredentials = (
  orgId: number,
  callback: ApiCallback<FacilityCredential[]>,
  signal: AbortSignal | null = null,
): void => {
  getFromOpenElisServer<FacilityCredential[]>(
    `/rest/billing/facility-credentials?orgId=${orgId}`,
    (response) => callback(response ?? []),
    signal,
  );
};

/** Get gateway config for an org / driver. */
export const getGatewayConfig = (
  orgId: number,
  driver: GatewayDriver,
  callback: ApiCallback<BranchGatewayConfig | undefined>,
  signal: AbortSignal | null = null,
): void => {
  const params = new URLSearchParams();
  params.append("orgId", String(orgId));
  if (driver) {
    params.append("driver", driver);
  }
  getFromOpenElisServer<BranchGatewayConfig | undefined>(
    `/rest/billing/gateway-config?${params.toString()}`,
    callback,
    signal,
  );
};

/** Save gateway config. Body: { organizationId, driver, displayName, publicKey, secretKey, webhookSecret, isEnabled, testMode, metadata }. */
export const saveGatewayConfig = (
  config: {
    organizationId: number;
    driver: GatewayDriver;
    displayName: string;
    publicKey: string;
    secretKey: string;
    webhookSecret: string;
    isEnabled: boolean;
    testMode: boolean;
    metadata: string;
  },
  callback: ApiCallback<BranchGatewayConfig>,
): void => {
  postToOpenElisServerJsonResponse<BranchGatewayConfig>(
    "/rest/billing/gateway-config",
    JSON.stringify(config),
    callback,
  );
};
