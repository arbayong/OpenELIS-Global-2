/**
 * Billing Report Service API Client
 *
 * Typed clients for the revenue report endpoints implemented by
 * RevenueReportServiceImpl. All paths are relative to the configured
 * serverBaseUrl (/api/OpenELIS-Global) and follow the OpenELIS frontend
 * data-fetching pattern (getFromOpenElisServer / postToOpenElisServerJsonResponse).
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../components/utils/Utils";

export interface DailyRevenueSummary {
  id?: number | null;
  organizationId?: number | null;
  businessDate?: string | null;
  grossRevenue?: number | string | null;
  cashRevenue?: number | string | null;
  mobileMoneyRevenue?: number | string | null;
  gatewayRevenue?: number | string | null;
  insuranceRevenue?: number | string | null;
  nhisRevenue?: number | string | null;
  privateRevenue?: number | string | null;
  refunds?: number | string | null;
  invoiceCount?: number | null;
  patientCount?: number | null;
}

export interface RevenueByTestRow {
  testId?: string | null;
  testName?: string | null;
  localCode?: string | null;
  revenue?: number | string | null;
  [key: string]: unknown;
}

export interface BillingInvoice {
  id?: number | null;
  organizationId?: number | null;
  patientId?: string | null;
  externalOrderId?: string | null;
  invoiceNumber?: string | null;
  status?: string | null;
  payerType?: string | null;
  currency?: string | null;
  insuranceNumber?: string | null;
  issuedAt?: string | null;
  createdAt?: string | null;
  subtotal?: number | string | null;
  taxTotal?: number | string | null;
  discountTotal?: number | string | null;
  total?: number | string | null;
  amountPaid?: number | string | null;
  recordedBy?: string | null;
}

type ApiCallback<T> = (response: T | undefined) => void;

const withQuery = (
  base: string,
  params: Record<string, string | number | boolean>,
): string => {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.append(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `${base}?${query}` : base;
};

/** GET /rest/billing/reports/daily?orgId=&date= -> DailyRevenueSummary */
export const getDailySummary = (
  orgId: number,
  date: string,
  callback: ApiCallback<DailyRevenueSummary>,
  signal: AbortSignal | null = null,
): void => {
  getFromOpenElisServer<DailyRevenueSummary>(
    withQuery("/rest/billing/reports/daily", { orgId, date }),
    callback,
    signal,
  );
};

/** GET /rest/billing/reports/monthly?orgId=&year=&month= -> DailyRevenueSummary[] */
export const getMonthlySummary = (
  orgId: number,
  year: number,
  month: number,
  callback: ApiCallback<DailyRevenueSummary[]>,
  signal: AbortSignal | null = null,
): void => {
  getFromOpenElisServer<DailyRevenueSummary[]>(
    withQuery("/rest/billing/reports/monthly", { orgId, year, month }),
    callback,
    signal,
  );
};

/** GET /rest/billing/reports/by-test?orgId=&from=&to= -> RevenueByTestRow[] */
export const getRevenueByTest = (
  orgId: number,
  from: string,
  to: string,
  callback: ApiCallback<RevenueByTestRow[]>,
  signal: AbortSignal | null = null,
): void => {
  getFromOpenElisServer<RevenueByTestRow[]>(
    withQuery("/rest/billing/reports/by-test", { orgId, from, to }),
    callback,
    signal,
  );
};

/** GET /rest/billing/reports/outstanding?orgId= -> BillingInvoice[] */
export const getOutstanding = (
  orgId: number,
  callback: ApiCallback<BillingInvoice[]>,
  signal: AbortSignal | null = null,
): void => {
  getFromOpenElisServer<BillingInvoice[]>(
    withQuery("/rest/billing/reports/outstanding", { orgId }),
    callback,
    signal,
  );
};

/** POST /rest/billing/reports/recompute-daily?orgId=&date= -> DailyRevenueSummary */
export const recomputeDailySummary = (
  orgId: number,
  date: string,
  callback: ApiCallback<DailyRevenueSummary>,
): void => {
  postToOpenElisServerJsonResponse<DailyRevenueSummary>(
    withQuery("/rest/billing/reports/recompute-daily", { orgId, date }),
    "",
    callback,
  );
};
