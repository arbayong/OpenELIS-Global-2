import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Tag, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  getBillingInvoices,
  type BillingInvoice,
  type BillingInvoiceStatus,
} from "../../../services/billingInvoiceService";
import InvoiceDetail from "./InvoiceDetail";
import "./InvoiceList.css";

const STATUS_LABELS: Record<BillingInvoiceStatus, string> = {
  DRAFT: "Draft",
  ISSUED: "Issued",
  PARTIALLY_PAID: "Partially paid",
  PAID: "Paid",
  VOID: "Void",
};

const TAG_KIND: Record<
  BillingInvoiceStatus,
  "green" | "red" | "gray" | "blue"
> = {
  DRAFT: "gray",
  ISSUED: "blue",
  PARTIALLY_PAID: "gray",
  PAID: "green",
  VOID: "red",
};

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

const InvoiceList = () => {
  const intl = useIntl();
  const [invoices, setInvoices] = useState<BillingInvoice[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState<string>("");
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<string | null>(null);

  const loadInvoices = useCallback(() => {
    setLoading(true);
    setError(null);
    getBillingInvoices(
      {},
      (response) => {
        setInvoices(response ?? []);
        setLoading(false);
      },
    );
  }, []);

  useEffect(() => {
    loadInvoices();
  }, [loadInvoices]);

  const filteredInvoices = useMemo(() => {
    const query = search.toLowerCase();
    return invoices.filter((invoice) => {
      const matchSearch =
        !query ||
        invoice.invoiceNumber?.toLowerCase().includes(query) ||
        invoice.patientId?.toLowerCase().includes(query);
      const matchStatus = !statusFilter || invoice.status === statusFilter;
      return matchSearch && matchStatus;
    });
  }, [invoices, search, statusFilter]);

  const handleSelect = (id: number | string | undefined) => {
    if (id === undefined) {
      return;
    }
    setSelectedInvoiceId(String(id));
  };

  const outstanding = (invoice: BillingInvoice): number => {
    return (invoice.total ?? 0) - (invoice.amountPaid ?? 0);
  };

  return (
    <div className="invoiceList">
      <PageBreadCrumb
        breadcrumbs={[
          {
            label: "billing.title",
            isCurrentPage: true,
          },
        ]}
      />
      <h2 className="invoiceList__heading">
        {intl.formatMessage({
          id: "billing.invoices.title",
          defaultMessage: "Invoices",
        })}
      </h2>
      {error && (
        <div className="invoiceList__error" role="alert">
          {error}
        </div>
      )}
      <div className="invoiceList__filterBar">
        <TextInput
          id="invoice-search"
          labelText={intl.formatMessage({
            id: "billing.invoices.search.label",
            defaultMessage: "Search by invoice number or patient",
          })}
          placeholder={intl.formatMessage({
            id: "billing.invoices.search.placeholder",
            defaultMessage: "Search...",
          })}
          value={search}
          onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
            setSearch(event.target.value)
          }
          data-testid="invoice-search"
        />
        <label>
          {intl.formatMessage({
            id: "billing.invoices.status.label",
            defaultMessage: "Status",
          })}
        </label>
        <select
          data-testid="invoice-status-filter"
          value={statusFilter}
          onChange={(event: React.ChangeEvent<HTMLSelectElement>) =>
            setStatusFilter(event.target.value)
          }
        >
          <option value="">
            {intl.formatMessage({
              id: "billing.invoices.status.all",
              defaultMessage: "All",
            })}
          </option>
          {Object.entries(STATUS_LABELS).map(([status, label]) => (
            <option key={status} value={status}>
              {label}
            </option>
          ))}
        </select>
        <Button
          kind="primary"
          size="sm"
          onClick={loadInvoices}
          disabled={loading}
        >
          {intl.formatMessage({
            id: "billing.invoices.refresh",
            defaultMessage: "Refresh",
          })}
        </Button>
      </div>
      {loading ? (
        <p className="invoiceList__loading">
          {intl.formatMessage({
            id: "billing.invoices.loading",
            defaultMessage: "Loading invoices...",
          })}
        </p>
      ) : (
        <>
            <table
              className="invoiceList__table"
              data-testid="invoice-list"
              aria-label={intl.formatMessage({
              id: "billing.invoices.table.label",
              defaultMessage: "Invoices",
            })}
          >
            <thead>
              <tr>
                <th>
                  {intl.formatMessage({
                    id: "billing.invoices.column.invoice",
                    defaultMessage: "Invoice #",
                  })}
                </th>
                <th>
                  {intl.formatMessage({
                    id: "billing.invoices.column.patient",
                    defaultMessage: "Patient",
                  })}
                </th>
                <th>
                  {intl.formatMessage({
                    id: "billing.invoices.column.status",
                    defaultMessage: "Status",
                  })}
                </th>
                <th>
                  {intl.formatMessage({
                    id: "billing.invoices.column.payer",
                    defaultMessage: "Payer",
                  })}
                </th>
                <th>
                  {intl.formatMessage({
                    id: "billing.invoices.column.total",
                    defaultMessage: "Total",
                  })}
                </th>
                <th>
                  {intl.formatMessage({
                    id: "billing.invoices.column.paid",
                    defaultMessage: "Paid",
                  })}
                </th>
                <th>
                  {intl.formatMessage({
                    id: "billing.invoices.column.outstanding",
                    defaultMessage: "Outstanding",
                  })}
                </th>
                <th>
                  {intl.formatMessage({
                    id: "billing.invoices.column.issued",
                    defaultMessage: "Issued",
                  })}
                </th>
              </tr>
            </thead>
            <tbody>
              {filteredInvoices.length === 0 ? (
                <tr>
                  <td colSpan={8} className="invoiceList__empty">
                    {intl.formatMessage({
                      id: "billing.invoices.empty",
                      defaultMessage: "No invoices found",
                    })}
                  </td>
                </tr>
              ) : (
                filteredInvoices.map((invoice) => (
                  <tr
                    key={invoice.id}
                    data-testid="invoice-row"
                    data-invoice-id={String(invoice.id)}
                    className={
                      selectedInvoiceId === String(invoice.id)
                        ? "invoiceList__row--selected"
                        : undefined
                    }
                    onClick={() => handleSelect(invoice.id)}
                    style={{ cursor: "pointer" }}
                  >
                    <td>{invoice.invoiceNumber || "—"}</td>
                    <td>{invoice.patientId || "—"}</td>
                    <td>
                      <Tag
                        type={TAG_KIND[invoice.status ?? "DRAFT"] || "gray"}
                        size="sm"
                      >
                        {STATUS_LABELS[invoice.status ?? "DRAFT"]}
                      </Tag>
                    </td>
                    <td>{invoice.payerType || "—"}</td>
                    <td>{money(invoice.total, invoice.currency)}</td>
                    <td>{money(invoice.amountPaid, invoice.currency)}</td>
                    <td>{money(outstanding(invoice), invoice.currency)}</td>
                    <td>
                      {invoice.issuedAt
                        ? new Date(invoice.issuedAt).toLocaleString()
                        : "—"}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {filteredInvoices.length > 0 && (
            <p className="invoiceList__count">
              {intl.formatMessage(
                {
                  id: "billing.invoices.count",
                  defaultMessage: "{count} invoices",
                },
                { count: filteredInvoices.length },
              )}
            </p>
          )}
        </>
      )}
      {selectedInvoiceId && (
        <div className="invoiceList__detail">
          <InvoiceDetail
            invoiceId={selectedInvoiceId}
            onClose={() => setSelectedInvoiceId(null)}
          />
        </div>
      )}
    </div>
  );
};

export default InvoiceList;
