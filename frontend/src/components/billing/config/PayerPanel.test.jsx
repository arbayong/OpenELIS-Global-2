/**
 * PayerPanel Component Tests
 *
 * Test Strategy:
 * - vi.mock the billing config service module BEFORE imports (Jest hoisting)
 * - data-testid for reliable element selection
 * - waitFor for async service-driven rendering
 * - Wrap render in IntlProvider + BrowserRouter
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

vi.mock("../../../services/billingConfigService", () => ({
  getPayers: vi.fn(),
  createPayer: vi.fn(),
  deactivatePayer: vi.fn(),
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

import PayerPanel from "./PayerPanel";
import { createPayer, deactivatePayer, getPayers } from "../../../services/billingConfigService";
import messages from "../../../languages/en.json";

// ========== TEST SETUP ==========

const PAYERS = [
  { id: 1, name: "National Health Insurance", type: "NHIS", config: "{}", isActive: "Y" },
  { id: 2, name: "Cash Patients", type: "CASH", config: "{}", isActive: "Y" },
  { id: 3, name: "Private Insurer", type: "PRIVATE", config: "{}", isActive: "N" },
];

const renderComponent = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages} defaultLocale="en">
        <PayerPanel />
      </IntlProvider>
    </BrowserRouter>,
  );

describe("PayerPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getPayers.mockImplementation((callback) => callback(PAYERS));
    createPayer.mockImplementation((_payer, callback) =>
      callback({ id: 99, name: "New Payer", type: "CASH", config: "{}", isActive: "Y" }),
    );
    deactivatePayer.mockImplementation((_id, callback) => callback({ id: 1, isActive: "N" }));
  });

  it("renders payer rows from the mocked service", async () => {
    renderComponent();

    await waitFor(() =>
      expect(screen.getAllByTestId("payer-row")).toHaveLength(3),
    );
    expect(screen.getByTestId("payer-panel")).not.toBeNull();
    expect(screen.getByText("National Health Insurance")).toBeInTheDocument();
    expect(screen.getByText("Cash Patients")).toBeInTheDocument();
    expect(screen.getByText("Private Insurer")).toBeInTheDocument();
  });

  it("create calls POST via the service on submit", async () => {
    renderComponent();

    await waitFor(() =>
      expect(screen.getAllByTestId("payer-row")).toHaveLength(3),
    );

    await userEvent.type(screen.getByTestId("payer-name-input"), "New Payer");
    await userEvent.click(screen.getByTestId("payer-submit"));

    await waitFor(() =>
      expect(createPayer).toHaveBeenCalledTimes(1),
    );
    expect(createPayer).toHaveBeenCalledWith(
      expect.objectContaining({ name: "New Payer" }),
      expect.any(Function),
    );
  });

  it("deactivate calls the service for a row", async () => {
    renderComponent();

    await waitFor(() =>
      expect(screen.getAllByTestId("payer-row")).toHaveLength(3),
    );

    const deactivateButtons = screen.getAllByRole("button", {
      name: /deactivate/i,
    });
    expect(deactivateButtons.length).toBeGreaterThan(0);
    await userEvent.click(deactivateButtons[0]);

    await waitFor(() =>
      expect(deactivatePayer).toHaveBeenCalledTimes(1),
    );
  });
});
