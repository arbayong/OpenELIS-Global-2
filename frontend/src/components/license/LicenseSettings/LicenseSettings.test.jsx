/**
 * LicenseSettings Component Tests
 *
 * Test Strategy:
 * - data-testid for reliable element selection
 * - waitFor with queryBy* for async operations
 * - userEvent for interactions
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

vi.mock("../../../services/licenseService", () => ({
  activateLicense: vi.fn(),
  getLicenseFingerprint: vi.fn(),
  getLicenseStatus: vi.fn(),
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

import LicenseSettings from "./LicenseSettings";
import {
  activateLicense,
  getLicenseFingerprint,
  getLicenseStatus,
} from "../../../services/licenseService";
import messages from "../../../languages/en.json";

// ========== TEST SETUP ==========

const renderComponent = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages} defaultLocale="en">
        <LicenseSettings />
      </IntlProvider>
    </BrowserRouter>,
  );

describe("LicenseSettings", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getLicenseFingerprint.mockImplementation((callback) =>
      callback({ fingerprint: "ABC123" }),
    );
    getLicenseStatus.mockImplementation((callback) =>
      callback({
        status: "MISSING",
        trialValid: true,
        trialDaysRemaining: 9,
        features: [],
        expiresAt: null,
      }),
    );
    activateLicense.mockImplementation((key, callback) => callback(undefined));
  });

  it("shows the machine fingerprint and trial status on mount", async () => {
    renderComponent();

    await waitFor(() =>
      expect(screen.getByTestId("license-fingerprint")).toHaveTextContent(
        "ABC123",
      ),
    );
    expect(screen.getByTestId("license-status-tag")).toHaveTextContent(
      messages["license.status.missing"],
    );
    expect(screen.getByTestId("license-trial-days")).toHaveTextContent("9");
  });

  it("activates the license with the pasted key", async () => {
    activateLicense.mockImplementation((key, callback) =>
      callback({ status: "VALID" }),
    );
    renderComponent();

    await userEvent.type(
      screen.getByTestId("license-key-input"),
      "PASTED-KEY",
    );
    await userEvent.click(screen.getByTestId("license-activate-button"));

    expect(activateLicense).toHaveBeenCalledWith(
      "PASTED-KEY",
      expect.any(Function),
    );
    await waitFor(() =>
      expect(getLicenseStatus).toHaveBeenCalledTimes(2),
    );
  });

  it("shows the error notification when activation is rejected", async () => {
    activateLicense.mockImplementation((key, callback) =>
      callback({ status: "INVALID_SIGNATURE" }),
    );
    renderComponent();

    await userEvent.type(screen.getByTestId("license-key-input"), "BAD-KEY");
    await userEvent.click(screen.getByTestId("license-activate-button"));

    await waitFor(() =>
      expect(
        screen.getByText(messages["license.activate.failure"]),
      ).toBeInTheDocument(),
    );
  });

  it("shows a licensed status with expiry and features when activated", async () => {
    getLicenseStatus.mockImplementation((callback) =>
      callback({
        status: "VALID",
        trialValid: false,
        trialDaysRemaining: 0,
        features: ["billing", "nhis"],
        expiresAt: "2027-01-31",
      }),
    );
    renderComponent();

    await waitFor(() =>
      expect(screen.getByTestId("license-status-tag")).toHaveTextContent(
        messages["license.status.valid"],
      ),
    );
    expect(screen.getByText(/2027-01-31/)).toBeInTheDocument();
    expect(screen.getByTestId("license-features")).toHaveTextContent(
      "billing, nhis",
    );
  });
});
