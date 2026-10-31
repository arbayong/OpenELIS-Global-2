/**
 * Billing Invoice Service API Client
 *
 * Typed clients for the OpenELIS billing module:
 *   GET    /rest/billing/invoices?orgId=&status=&patientId=
 *   GET    /rest/billing/invoices/{id}
 *   GET    /rest/billing/invoices/by-order/{externalOrderId}
 *   POST   /rest/billing/payments/record?invoiceId=&amount=&method=&gateway=&providerTransactionId=&recordedBy=
 *   POST   /rest/billing/payments/{invoiceId}/void?reason=&recordedBy=
 *
 * Follows the OpenELIS frontend data-fetching pattern
 * (getFromOpenElisServer / postToOpenElisServerJsonResponse).
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../components/utils/Utils";

export type BillingInvoiceStatus =
  | "DRAFT"
  | "ISSUED"
  | "PARTIALLY_PAID"
  | "PAID"
  | "VOID"
  | (string & {});

export type PayerType = "CASH" | "NHIS" | "PRIVATE" | (string & {});

export type PaymentMethod =
  | "CASH"
  | "CARD"
  | "BANK_TRANSFER"
  | "MOBILE_MONEY"
  | "GATEWAY"
  | (string & {});

export type GatewayDriver = "PAYSTACK" | "HUBTEL" | "MTN_MOMO" | (string & {});

export type PaymentType = "PAYMENT" | "REFUND" | (string & {});

export interface BillingInvoice {
  id?: number;
  organizationId?: number | null;
  patientId?: string | null;
  externalOrderId?: string | null;
  invoiceNumber?: string | null;
  status?: BillingInvoiceStatus;
  payerType?: PayerType;
  currency?: string | null;
  insuranceNumber?: string | null;
  issuedAt?: string | null;
  createdAt?: string | null;
  subtotal?: number | null;
  taxTotal?: number | null;
  discountTotal?: number | null;
  total?: number | null;
  amountPaid?: number | null;
  recordedBy?: string | null;
}

export interface BillingInvoiceLine {
  id?: number;
  invoiceId?: number | null;
  testId?: string | null;
  testName?: string | null;
  loincCode?: string | null;
  localCode?: string | null;
  quantity?: number | null;
  unitPrice?: number | null;
  lineTotal?: number | null;
  amountPaid?: number | null;
  patientResponsibilityAmount?: number | null;
  insuranceExpectedAmount?: number | null;
  lineStatus?: string | null;
  claimLineId?: string | null;
  /** Payer and plan in force at order time (JSON); audit of billed coverage. */
  payerSnapshot?: string | null;
}

export interface BillingPayment {
  id?: number;
  organizationId?: number | null;
  patientId?: string | null;
  method?: PaymentMethod;
  gateway?: GatewayDriver | null;
  type?: PaymentType;
  amount?: number | null;
  providerTransactionId?: string | null;
  receivedAt?: string | null;
  recordedBy?: string | null;
  metadata?: string | null;
}

export interface PaymentAllocationView {
  payment: BillingPayment;
  allocatedAmount: number | null;
}

export interface BillingInvoiceDetail {
  invoice: BillingInvoice;
  lines: BillingInvoiceLine[];
  payments: PaymentAllocationView[];
}

export interface RecordPaymentParams {
  invoiceId: number | string;
  amount: number;
  method: PaymentMethod;
  gateway?: GatewayDriver;
  providerTransactionId?: string;
  recordedBy: string;
}

export interface VoidInvoiceParams {
  reason?: string;
  recordedBy: string;
}

type ApiCallback<T = unknown> = (response: T | undefined) => void;

/** List invoices with optional orgId / status / patientId filters. */
export const getBillingInvoices = (
  filters: {
    orgId?: number | string;
    status?: BillingInvoiceStatus;
    patientId?: string;
  } = {},
  callback: ApiCallback<BillingInvoice[]>,
  signal: AbortSignal | null = null,
): void => {
  const params = new URLSearchParams();
  if (filters.orgId !== undefined && filters.orgId !== null && filters.orgId !== "") {
    params.append("orgId", String(filters.orgId));
  }
  if (filters.status) {
    params.append("status", filters.status);
  }
  if (filters.patientId) {
    params.append("patientId", filters.patientId);
  }
  const endpoint = params.toString()
    ? `/rest/billing/invoices?${params.toString()}`
    : "/rest/billing/invoices";
  getFromOpenElisServer<BillingInvoice[]>(
    endpoint,
    (response) => callback(response ?? []),
    signal,
  );
};

/** Get a single invoice with its lines and payments. */
export const getBillingInvoice = (
  id: number | string,
  callback: ApiCallback<BillingInvoiceDetail | undefined>,
  signal: AbortSignal | null = null,
): void => {
  getFromOpenElisServer<BillingInvoiceDetail>(
    `/rest/billing/invoices/${encodeURIComponent(String(id))}`,
    callback,
    signal,
  );
};

/** Find invoices linked to an external order id. */
export const getBillingInvoicesByOrder = (
  externalOrderId: string,
  callback: ApiCallback<BillingInvoice[]>,
  signal: AbortSignal | null = null,
): void => {
  getFromOpenElisServer<BillingInvoice[]>(
    `/rest/billing/invoices/by-order/${encodeURIComponent(externalOrderId)}`,
    (response) => callback(response ?? []),
    signal,
  );
};

/** Record a payment against an invoice. All fields travel as query params. */
export const recordPayment = (
  params: RecordPaymentParams,
  callback: ApiCallback<BillingPayment>,
): void => {
  const query = new URLSearchParams();
  query.append("invoiceId", String(params.invoiceId));
  query.append("amount", String(params.amount));
  query.append("method", params.method);
  if (params.gateway) {
    query.append("gateway", params.gateway);
  }
  if (params.providerTransactionId) {
    query.append("providerTransactionId", params.providerTransactionId);
  }
  query.append("recordedBy", params.recordedBy);
  postToOpenElisServerJsonResponse<BillingPayment>(
    `/rest/billing/payments/record?${query.toString()}`,
    JSON.stringify({}),
    callback,
  );
};

/** Void an invoice. reason and recordedBy travel as query params. */
export const voidInvoice = (
  id: number | string,
  params: VoidInvoiceParams,
  callback: ApiCallback<BillingInvoice>,
): void => {
  const query = new URLSearchParams();
  if (params.reason) {
    query.append("reason", params.reason);
  }
  query.append("recordedBy", params.recordedBy);
  postToOpenElisServerJsonResponse<BillingInvoice>(
    `/rest/billing/payments/${encodeURIComponent(String(id))}/void?${query.toString()}`,
    JSON.stringify({}),
    callback,
  );
};
