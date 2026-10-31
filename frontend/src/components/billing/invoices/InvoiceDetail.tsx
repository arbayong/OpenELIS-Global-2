import React, { useCallback, useEffect, useState } from "react";
import { Button, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getBillingInvoice,
  recordPayment,
  voidInvoice,
  type BillingInvoiceDetail,
  type RecordPaymentParams,
} from "../../../services/billingInvoiceService";
import PaymentForm from "../payments/PaymentForm";
import { toLocalIsoDateTime } from "../../utils/Utils";
import "./InvoiceDetail.css";

interface InvoiceDetailProps {
  invoiceId: string | number;
  onClose: () => void;
}

const money = (value: number | null | undefined, currency: string | null | undefined = "GHS"): string => {
  if (value === null || value === undefined) {
    return "—";
  }
  try {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: currency ?? "GHS",
    }).format(value);
  } catch {
    return `${currency ?? "GHS"} ${(value as number).toFixed(2)}`;
  }
};

const InvoiceDetail = ({ invoiceId, onClose }: InvoiceDetailProps) => {
  const intl = useIntl();
  const [detail, setDetail] = useState<BillingInvoiceDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [openPaymentForm, setOpenPaymentForm] = useState<boolean>(false);
  const [voiding, setVoiding] = useState<boolean>(false);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    getBillingInvoice(String(invoiceId), (response) => {
      setDetail(response ?? null);
      setLoading(false);
    });
  }, [invoiceId]);

  useEffect(() => {
    load();
  }, [load]);

  const handleRecordPayment = (params: RecordPaymentParams) => {
    recordPayment(params, (response) => {
      if (response?.id) {
        load();
        setOpenPaymentForm(false);
      } else {
        setError(
          intl.formatMessage({
            id: "billing.payment.record.failed",
            defaultMessage: "Failed to record payment",
          }),
        );
      }
    });
  };

  const handleVoid = () => {
    if (
      !window.confirm(
        intl.formatMessage({
          id: "billing.invoice.void.confirm",
          defaultMessage: "Void this invoice?",
        }),
      )
    ) {
      return;
    }
    setVoiding(true);
    voidInvoice(
      String(invoiceId),
      { reason: "", recordedBy: "operator" },
      (response) => {
        setVoiding(false);
        if (response?.id) {
          load();
        } else {
          setError(
            intl.formatMessage({
              id: "billing.invoice.void.failed",
              defaultMessage: "Failed to void invoice",
            }),
          );
        }
      },
    );
  };

  const invoice = detail?.invoice ?? null;
  const lines = detail?.lines ?? [];
  const payments = detail?.payments ?? [];

  const outstanding = (invoice?.total ?? 0) - (invoice?.amountPaid ?? 0);
  const totalPaid = payments.reduce(
    (sum, item) => sum + (item.allocatedAmount ?? 0),
    0,
  );

  return (
    <div className="invoiceDetail" data-testid="invoice-detail">
      <Button
        kind="tertiary"
        size="sm"
        onClick={onClose}
        data-testid="back-to-list-button"
      >
        {intl.formatMessage({
          id: "billing.invoice.back",
          defaultMessage: "Back to invoices",
        })}
      </Button>
      {error && (
        <div className="invoiceDetail__error" role="alert">
          {error}
        </div>
      )}
      {loading ? (
        <p>
          {intl.formatMessage({
            id: "billing.invoice.loading",
            defaultMessage: "Loading invoice...",
          })}
        </p>
      ) : invoice ? (
        <>
          <div className="invoiceDetail__header">
            <h2 className="invoiceDetail__heading">
              {intl.formatMessage({
                id: "billing.invoice.detail.title",
                defaultMessage: "Invoice",
              })}
              {": "}
              {invoice.invoiceNumber || "—"}
            </h2>
            <div className="invoiceDetail__actions">
              <Button
                kind="primary"
                size="md"
                onClick={() => setOpenPaymentForm(true)}
                disabled={!["ISSUED", "PARTIALLY_PAID"].includes(
                  invoice.status ?? "",
                )}
                data-testid="record-payment-button"
              >
                {intl.formatMessage({
                  id: "billing.invoice.recordPayment",
                  defaultMessage: "Record payment",
                })}
              </Button>
              <Button
                kind="danger"
                size="md"
                onClick={handleVoid}
                disabled={voiding || invoice.status === "VOID"}
                data-testid="void-invoice-button"
              >
                {intl.formatMessage({
                  id: "billing.invoice.void",
                  defaultMessage: "Void invoice",
                })}
              </Button>
            </div>
          </div>
          <div className="invoiceDetail__grid">
            <div className="invoiceDetail__field">
              <span className="invoiceDetail__fieldLabel">
                {intl.formatMessage({
                  id: "billing.invoice.field.patient",
                  defaultMessage: "Patient",
                })}
              </span>
              <span className="invoiceDetail__fieldValue">
                {invoice.patientId || "—"}
              </span>
            </div>
            <div className="invoiceDetail__field">
              <span className="invoiceDetail__fieldLabel">
                {intl.formatMessage({
                  id: "billing.invoice.field.payer",
                  defaultMessage: "Payer type",
                })}
              </span>
              <span className="invoiceDetail__fieldValue">
                {invoice.payerType || "—"}
              </span>
            </div>
            <div className="invoiceDetail__field">
              <span className="invoiceDetail__fieldLabel">
                {intl.formatMessage({
                  id: "billing.invoice.field.status",
                  defaultMessage: "Status",
                })}
              </span>
              <span className="invoiceDetail__fieldValue">
                <Tag
                  type={
                    invoice.status === "PAID"
                      ? "green"
                      : invoice.status === "VOID"
                        ? "red"
                        : invoice.status === "DRAFT" ||
                          invoice.status === "PARTIALLY_PAID"
                          ? "gray"
                          : "blue"
                  }
                  size="sm"
                >
                  {String(invoice.status ?? "—")}
                </Tag>
              </span>
            </div>
            <div className="invoiceDetail__field">
              <span className="invoiceDetail__fieldLabel">
                {intl.formatMessage({
                  id: "billing.invoice.field.currency",
                  defaultMessage: "Currency",
                })}
              </span>
              <span className="invoiceDetail__fieldValue">
                {invoice.currency || "GHS"}
              </span>
            </div>
            <div className="invoiceDetail__field">
              <span className="invoiceDetail__fieldLabel">
                {intl.formatMessage({
                  id: "billing.invoice.field.issued",
                  defaultMessage: "Issued",
                })}
              </span>
              <span className="invoiceDetail__fieldValue">
                {toLocalIsoDateTime(invoice.issuedAt)}
              </span>
            </div>
            <div className="invoiceDetail__field">
              <span className="invoiceDetail__fieldLabel">
                {intl.formatMessage({
                  id: "billing.invoice.field.recordedBy",
                  defaultMessage: "Recorded by",
                })}
              </span>
              <span className="invoiceDetail__fieldValue">
                {invoice.recordedBy || "—"}
              </span>
            </div>
          </div>
          <div className="invoiceDetail__totals">
            <div className="invoiceDetail__total">
              <span className="invoiceDetail__totalLabel">
                {intl.formatMessage({
                  id: "billing.invoice.total.subtotal",
                  defaultMessage: "Subtotal",
                })}
              </span>
              <span className="invoiceDetail__totalValue">
                {money(invoice.subtotal, invoice.currency)}
              </span>
            </div>
            <div className="invoiceDetail__total">
              <span className="invoiceDetail__totalLabel">
                {intl.formatMessage({
                  id: "billing.invoice.total.tax",
                  defaultMessage: "Tax",
                })}
              </span>
              <span className="invoiceDetail__totalValue">
                {money(invoice.taxTotal, invoice.currency)}
              </span>
            </div>
            <div className="invoiceDetail__total">
              <span className="invoiceDetail__totalLabel">
                {intl.formatMessage({
                  id: "billing.invoice.total.discount",
                  defaultMessage: "Discount",
                })}
              </span>
              <span className="invoiceDetail__totalValue">
                {money(invoice.discountTotal, invoice.currency)}
              </span>
            </div>
            <div className="invoiceDetail__total">
              <span className="invoiceDetail__totalLabel">
                {intl.formatMessage({
                  id: "billing.invoice.total.total",
                  defaultMessage: "Total",
                })}
              </span>
              <span className="invoiceDetail__totalValue">
                {money(invoice.total, invoice.currency)}
              </span>
            </div>
            <div className="invoiceDetail__total">
              <span className="invoiceDetail__totalLabel">
                {intl.formatMessage({
                  id: "billing.invoice.total.paid",
                  defaultMessage: "Amount paid",
                })}
              </span>
              <span className="invoiceDetail__totalValue">
                {money(invoice.amountPaid, invoice.currency)}
              </span>
            </div>
            <div className="invoiceDetail__total">
              <span className="invoiceDetail__totalLabel">
                {intl.formatMessage({
                  id: "billing.invoice.total.outstanding",
                  defaultMessage: "Outstanding",
                })}
              </span>
              <span className="invoiceDetail__totalValue">
                {money(outstanding, invoice.currency)}
              </span>
            </div>
          </div>
          <h3>
            {intl.formatMessage({
              id: "billing.invoice.lines.title",
              defaultMessage: "Line items",
            })}
          </h3>
          {lines.length === 0 ? (
            <p className="invoiceDetail__empty">
              {intl.formatMessage({
                id: "billing.invoice.lines.empty",
                defaultMessage: "No line items",
              })}
            </p>
          ) : (
            <table className="invoiceDetail__table">
              <thead>
                <tr>
                  <th>
                    {intl.formatMessage({
                      id: "billing.invoice.line.test",
                      defaultMessage: "Test",
                    })}
                  </th>
                  <th>
                    {intl.formatMessage({
                      id: "billing.invoice.line.loinc",
                      defaultMessage: "LOINC",
                    })}
                  </th>
                  <th>
                    {intl.formatMessage({
                      id: "billing.invoice.line.local",
                      defaultMessage: "Local code",
                    })}
                  </th>
                  <th className="invoiceDetail__textRight">
                    {intl.formatMessage({
                      id: "billing.invoice.line.qty",
                      defaultMessage: "Qty",
                    })}
                  </th>
                  <th className="invoiceDetail__textRight">
                    {intl.formatMessage({
                      id: "billing.invoice.line.unitPrice",
                      defaultMessage: "Unit price",
                    })}
                  </th>
                  <th className="invoiceDetail__textRight">
                    {intl.formatMessage({
                      id: "billing.invoice.line.total",
                      defaultMessage: "Line total",
                    })}
                  </th>
                  <th className="invoiceDetail__textRight">
                    {intl.formatMessage({
                      id: "billing.invoice.line.paid",
                      defaultMessage: "Paid",
                    })}
                  </th>
                </tr>
              </thead>
              <tbody>
                {lines.map((line) => (
                  <tr key={line.id}>
                    <td>{line.testName || "—"}</td>
                    <td>{line.loincCode || "—"}</td>
                    <td>{line.localCode || "—"}</td>
                    <td className="invoiceDetail__textRight">
                      {line.quantity ?? 1}
                    </td>
                    <td className="invoiceDetail__textRight">
                      {money(line.unitPrice, invoice.currency)}
                    </td>
                    <td className="invoiceDetail__textRight">
                      {money(line.lineTotal, invoice.currency)}
                    </td>
                    <td className="invoiceDetail__textRight">
                      {money(line.amountPaid, invoice.currency)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <h3>
            {intl.formatMessage({
              id: "billing.invoice.payments.title",
              defaultMessage: "Payments",
            })}
          </h3>
          {payments.length === 0 ? (
            <p className="invoiceDetail__empty">
              {intl.formatMessage({
                id: "billing.invoice.payments.empty",
                defaultMessage: "No payments",
              })}
            </p>
          ) : (
            <table className="invoiceDetail__table">
              <thead>
                <tr>
                  <th>
                    {intl.formatMessage({
                      id: "billing.invoice.payment.id",
                      defaultMessage: "Payment #",
                    })}
                  </th>
                  <th>
                    {intl.formatMessage({
                      id: "billing.invoice.payment.method",
                      defaultMessage: "Method",
                    })}
                  </th>
                  <th>
                    {intl.formatMessage({
                      id: "billing.invoice.payment.gateway",
                      defaultMessage: "Gateway",
                    })}
                  </th>
                  <th className="invoiceDetail__textRight">
                    {intl.formatMessage({
                      id: "billing.invoice.payment.amount",
                      defaultMessage: "Amount",
                    })}
                  </th>
                  <th className="invoiceDetail__textRight">
                    {intl.formatMessage({
                      id: "billing.invoice.payment.allocated",
                      defaultMessage: "Allocated",
                    })}
                  </th>
                  <th>
                    {intl.formatMessage({
                      id: "billing.invoice.payment.received",
                      defaultMessage: "Received",
                    })}
                  </th>
                  <th>
                    {intl.formatMessage({
                      id: "billing.invoice.payment.recordedBy",
                      defaultMessage: "Recorded by",
                    })}
                  </th>
                </tr>
              </thead>
              <tbody>
                {payments.map((item) => (
                  <tr key={item.payment.id}>
                    <td>{String(item.payment.id ?? "—")}</td>
                    <td>{item.payment.method || "—"}</td>
                    <td>{item.payment.gateway || "—"}</td>
                    <td className="invoiceDetail__textRight">
                      {money(item.payment.amount, invoice.currency)}
                    </td>
                    <td className="invoiceDetail__textRight">
                      {money(item.allocatedAmount, invoice.currency)}
                    </td>
                    <td>{toLocalIsoDateTime(item.payment.receivedAt)}</td>
                    <td>{item.payment.recordedBy || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <p className="invoiceDetail__count">
            {intl.formatMessage({
              id: "billing.invoice.total.allocated",
              defaultMessage: "Total allocated: {amount}",
            })}
            {": "}
            {money(totalPaid, invoice.currency)}
          </p>
        </>
      ) : (
        <p className="invoiceDetail__empty">
          {intl.formatMessage({
            id: "billing.invoice.notFound",
            defaultMessage: "Invoice not found",
          })}
        </p>
      )}
        <PaymentForm
          open={openPaymentForm}
          invoiceId={invoiceId}
          onClose={() => setOpenPaymentForm(false)}
          onSuccess={load}
        />
    </div>
  );
};

export default InvoiceDetail;
