/**
 * PaymentForm Component Tests
 *
 * Test Strategy:
 * - vi.mock the billing service module BEFORE imports (Jest hoisting)
 * - data-testid for reliable element selection
 * - waitFor for async service callback
 * - Wrap render in IntlProvider + BrowserRouter
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

vi.mock("../../../services/billingInvoiceService", () => ({
  recordPayment: vi.fn(),
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

import PaymentForm from "./PaymentForm";
import { recordPayment } from "../../../services/billingInvoiceService";
import messages from "../../../languages/en.json";

// ========== TEST SETUP ==========

const renderComponent = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages} defaultLocale="en">
        <PaymentForm open invoiceId="101" onClose={vi.fn()} />
      </IntlProvider>
    </BrowserRouter>,
  );

describe("PaymentForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("submits amount and method to the record-payment endpoint", async () => {
    recordPayment.mockImplementation((_params, callback) =>
      callback({ id: 50, amount: 150, method: "CARD" }),
    );
    renderComponent();

    await userEvent.type(screen.getByTestId("payment-amount-input"), "150");
    fireEvent.change(screen.getByTestId("payment-method-dropdown"), {
      target: { value: "CARD" },
    });
    await userEvent.click(screen.getByTestId("payment-submit"));

    await waitFor(() =>
      expect(recordPayment).toHaveBeenCalledWith(
        expect.objectContaining({
          invoiceId: "101",
          amount: 150,
          method: "CARD",
          recordedBy: "operator",
        }),
        expect.any(Function),
      ),
    );
  });
});
