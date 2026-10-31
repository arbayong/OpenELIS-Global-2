/**
 * InvoiceList Component Tests
 *
 * Test Strategy:
 * - vi.mock the billing service module BEFORE imports (Jest hoisting)
 * - data-testid for reliable element selection
 * - waitFor for async service-driven rendering
 * - Wrap render in IntlProvider + BrowserRouter
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

vi.mock("../../../services/billingInvoiceService", () => ({
  getBillingInvoices: vi.fn(),
}));

// ========== IMPORTS (Standard order) ==========

import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";

import InvoiceList from "./InvoiceList";
import { getBillingInvoices } from "../../../services/billingInvoiceService";
import messages from "../../../languages/en.json";

// ========== TEST SETUP ==========

const INVOICES = [
  {
    id: 1,
    invoiceNumber: "INV-1001",
    patientId: "P-100",
    status: "ISSUED",
    payerType: "CASH",
    currency: "GHS",
    total: 250,
    amountPaid: 0,
    issuedAt: "2026-01-15T10:00:00Z",
  },
  {
    id: 2,
    invoiceNumber: "INV-1002",
    patientId: "P-101",
    status: "PAID",
    payerType: "NHIS",
    currency: "GHS",
    total: 120,
    amountPaid: 120,
    issuedAt: "2026-01-16T11:00:00Z",
  },
  {
    id: 3,
    invoiceNumber: "INV-2001",
    patientId: "P-100",
    status: "DRAFT",
    payerType: "PRIVATE",
    currency: "GHS",
    total: 500,
    amountPaid: 100,
    issuedAt: "2026-01-17T12:00:00Z",
  },
];

const renderComponent = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages} defaultLocale="en">
        <InvoiceList />
      </IntlProvider>
    </BrowserRouter>,
  );

describe("InvoiceList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getBillingInvoices.mockImplementation((_filters, callback) =>
      callback(INVOICES),
    );
  });

  it("renders invoice rows from the mocked service", async () => {
    renderComponent();

    await waitFor(() =>
      expect(screen.getAllByTestId("invoice-row")).toHaveLength(3),
    );
    expect(screen.getByText("INV-1001")).toBeInTheDocument();
    expect(screen.getByText("INV-1002")).toBeInTheDocument();
    expect(screen.getByText("INV-2001")).toBeInTheDocument();
    expect(screen.getByTestId("invoice-list")).not.toBeNull();
  });

  it("filters rows by search term (invoice number or patient id)", async () => {
    renderComponent();

    await waitFor(() =>
      expect(screen.getAllByTestId("invoice-row")).toHaveLength(3),
    );
    await userEvent.type(screen.getByTestId("invoice-search"), "INV-1002");

    await waitFor(() =>
      expect(screen.getAllByTestId("invoice-row")).toHaveLength(1),
    );
    expect(screen.getByText("INV-1002")).toBeInTheDocument();
    expect(screen.queryByText("INV-1001")).not.toBeInTheDocument();
  });

  it("filters rows by status", async () => {
    renderComponent();

    await waitFor(() =>
      expect(screen.getAllByTestId("invoice-row")).toHaveLength(3),
    );
    fireEvent.change(screen.getByTestId("invoice-status-filter"), {
      target: { value: "DRAFT" },
    });

    await waitFor(() =>
      expect(screen.getAllByTestId("invoice-row")).toHaveLength(1),
    );
    expect(screen.getByText("INV-2001")).toBeInTheDocument();
  });
});
