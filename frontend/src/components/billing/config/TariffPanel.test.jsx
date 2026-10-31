/**
 * TariffPanel Component Tests
 *
 * Test Strategy:
 * - vi.mock the billing config service module BEFORE imports (Jest hoisting)
 * - data-testid for reliable element selection
 * - waitFor for async service-driven rendering
 * - Wrap render in IntlProvider + BrowserRouter
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

vi.mock("../../../services/billingConfigService", () => ({
  getTariffs: vi.fn(),
  getTariffPrice: vi.fn(),
  createTariff: vi.fn(),
}));

// ========== IMPORTS (Standard order) ==========

import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";

import TariffPanel from "./TariffPanel";
import { getTariffPrice, getTariffs, createTariff } from "../../../services/billingConfigService";
import messages from "../../../languages/en.json";

// ========== TEST SETUP ==========

const TARIFFS = [
  { id: 1, localCode: "MAL-001", testId: "TEST-123", price: 2500, payerType: "CASH", effectiveFrom: "2026-01-01", effectiveTo: "2026-12-31", isActive: "Y" },
];

const PRICE = { localCode: "MAL-001", payerType: "CASH", price: 2500 };

const renderComponent = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages} defaultLocale="en">
        <TariffPanel />
      </IntlProvider>
    </BrowserRouter>,
  );

describe("TariffPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getTariffs.mockImplementation((_filters, callback) => callback(TARIFFS));
    getTariffPrice.mockImplementation((_localCode, _payerType, callback) => callback(PRICE));
    createTariff.mockImplementation((_tariff, callback) => callback({ id: 2 }));
  });

  it("renders tariff rows from the mocked service", async () => {
    renderComponent();

    await waitFor(() =>
      expect(screen.getByTestId("tariff-panel")).not.toBeNull(),
    );
    expect(screen.getByText("MAL-001")).toBeInTheDocument();
    expect(screen.getByText("TEST-123")).toBeInTheDocument();
  });

  it("price lookup renders resolved price", async () => {
    renderComponent();

    await waitFor(() =>
      expect(screen.getByTestId("tariff-panel")).not.toBeNull(),
    );

    await userEvent.type(screen.getByTestId("tariff-price-input"), "MAL-001");
    await userEvent.click(screen.getByRole("button", { name: /lookup price/i }));

    await waitFor(() =>
      expect(getTariffPrice).toHaveBeenCalledWith("MAL-001", "CASH", expect.any(Function)),
    );
    await waitFor(() =>
      expect(screen.getByTestId("tariff-resolved-price")).toHaveTextContent(
        "2,500",
      ),
    );
  });
});
