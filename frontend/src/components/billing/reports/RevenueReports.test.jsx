/**
 * RevenueReports Component Tests
 *
 * Test Strategy:
 * - vi.mock the service module BEFORE imports (Jest/Vitest hoisting)
 * - data-testid for reliable element selection
 * - waitFor for async service responses
 * - userEvent for interactions
 * - IntlProvider + BrowserRouter wrapper
 */

// ========== MOCKS (BEFORE IMPORTS - Vitest hoisting) ==========

vi.mock("../../../services/billingReportService", () => ({
  getDailySummary: vi.fn(),
  getMonthlySummary: vi.fn(),
  getRevenueByTest: vi.fn(),
  getOutstanding: vi.fn(),
  recomputeDailySummary: vi.fn(),
}));

// ========== IMPORTS (Standard order) ==========

import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { fireEvent, waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";

import RevenueReports from "./RevenueReports";
import {
  getDailySummary,
  getRevenueByTest,
  recomputeDailySummary,
} from "../../../services/billingReportService";

const renderComponent = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={{}} defaultLocale="en">
        <RevenueReports />
      </IntlProvider>
    </BrowserRouter>,
  );

const DAILY_SUMMARY = {
  id: 1,
  organizationId: 1,
  businessDate: "2024-01-15",
  grossRevenue: "1250.00",
  cashRevenue: "500.00",
  mobileMoneyRevenue: "300.00",
  gatewayRevenue: "200.00",
  insuranceRevenue: "100.00",
  nhisRevenue: "50.00",
  privateRevenue: "100.00",
  refunds: "0.00",
  invoiceCount: 5,
  patientCount: 4,
};

describe("RevenueReports", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getDailySummary.mockImplementation((_orgId, _date, callback) =>
      callback(DAILY_SUMMARY),
    );
    recomputeDailySummary.mockImplementation((_orgId, _date, callback) =>
      callback(DAILY_SUMMARY),
    );
    getRevenueByTest.mockImplementation((_orgId, _from, _to, callback) =>
      callback([
        { testId: "1", testName: "Glucose", localCode: "GLU", revenue: "45.00" },
      ]),
    );
  });

  it("loads and renders the daily summary tiles", async () => {
    renderComponent();

    await userEvent.type(screen.getByTestId("report-org-input"), "1");
    fireEvent.change(screen.getByTestId("report-date"), {
      target: { value: "2024-01-15" },
    });
    await userEvent.click(screen.getByTestId("report-load-daily"));

    await waitFor(() =>
      expect(screen.getByTestId("daily-summary-tiles")).toBeInTheDocument(),
    );
    expect(getDailySummary).toHaveBeenCalledWith(1, "2024-01-15", expect.any(Function));
    expect(screen.getByTestId("daily-tile-gross")).toHaveTextContent("1250.00");
    expect(screen.getByTestId("daily-tile-cash")).toHaveTextContent("500.00");
    expect(screen.getByTestId("daily-tile-invoice-count")).toHaveTextContent("5");
    expect(screen.getByTestId("daily-tile-patient-count")).toHaveTextContent("4");
  });

  it("posts to recompute daily summary when the recompute button is clicked", async () => {
    renderComponent();

    await userEvent.type(screen.getByTestId("report-org-input"), "1");
    fireEvent.change(screen.getByTestId("report-date"), {
      target: { value: "2024-01-15" },
    });
    await userEvent.click(screen.getByTestId("recompute-daily"));

    await waitFor(() =>
      expect(recomputeDailySummary).toHaveBeenCalledWith(
        1,
        "2024-01-15",
        expect.any(Function),
      ),
    );
    expect(screen.getByTestId("daily-tile-gross")).toHaveTextContent("1250.00");
  });

  it("CSV export creates a Blob URL", async () => {
    const createObjectURL = vi.fn(() => "blob:csv-export");
    const revokeObjectURL = vi.fn();
    const originalCreateObjectURL = global.URL.createObjectURL;
    const originalRevokeObjectURL = global.URL.revokeObjectURL;
    global.URL.createObjectURL = createObjectURL;
    global.URL.revokeObjectURL = revokeObjectURL;

    renderComponent();

    await userEvent.type(screen.getByTestId("report-org-input"), "1");
    fireEvent.change(screen.getByTestId("report-from"), {
      target: { value: "2024-01-01" },
    });
    fireEvent.change(screen.getByTestId("report-to"), {
      target: { value: "2024-01-31" },
    });
    await userEvent.click(screen.getByTestId("report-load-by-test"));

    await waitFor(() =>
      expect(screen.getByTestId("by-test-table")).toBeInTheDocument(),
    );

    await userEvent.click(screen.getByTestId("report-export-by-test"));

    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(createObjectURL).toHaveBeenCalledWith(expect.any(Blob));

    global.URL.createObjectURL = originalCreateObjectURL;
    global.URL.revokeObjectURL = originalRevokeObjectURL;
  });
});
