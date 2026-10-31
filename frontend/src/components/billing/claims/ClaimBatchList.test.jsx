// ========== MOCKS (BEFORE IMPORTS - Vitest hoisting) ==========

vi.mock("../../../services/claimService", () => ({
  buildClaimBatch: vi.fn(),
  getClaimBatchesByOrg: vi.fn(),
  getClaimBatch: vi.fn(),
  getClaimBatchXml: vi.fn(),
  submitClaimFeedback: vi.fn(),
}));

// ========== IMPORTS ==========

import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";

import ClaimBatchList from "./ClaimBatchList";
import {
  buildClaimBatch,
  getClaimBatch,
  getClaimBatchXml,
  getClaimBatchesByOrg,
} from "../../../services/claimService";
import messages from "../../../languages/en.json";

const renderComponent = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages} defaultLocale="en">
        <ClaimBatchList />
      </IntlProvider>
    </BrowserRouter>,
  );

const BATCH = {
  id: 1,
  organizationId: 7,
  batchNumber: "BATCH-001",
  batchDate: "2024-01-15",
  status: "DRAFT",
  totalClaims: 3,
  totalAmount: 150,
  lastupdated: "2024-01-15T09:00:00Z",
};

describe("ClaimBatchList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getClaimBatchesByOrg.mockResolvedValue([BATCH]);
    buildClaimBatch.mockResolvedValue({ ...BATCH, id: 2 });
    getClaimBatch.mockResolvedValue({ ...BATCH, id: 1 });
    getClaimBatchXml.mockResolvedValue("<ClaimBatch/>");
  });

  it("renders batches for the organization from the mocked service", async () => {
    renderComponent();

    await userEvent.type(screen.getByTestId("org-id-input"), "7");

    await waitFor(() =>
      expect(screen.getByTestId("claim-batch-row")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("claim-batch-row")).toHaveTextContent(
      "BATCH-001",
    );
    expect(getClaimBatchesByOrg).toHaveBeenCalledWith(7);
  });

  it("calls buildClaimBatch when the build button is clicked", async () => {
    renderComponent();

    await userEvent.type(screen.getByTestId("org-id-input"), "7");
    await waitFor(() =>
      expect(screen.getByTestId("claim-batch-row")).toBeInTheDocument(),
    );

    await userEvent.click(screen.getByTestId("build-batch-button"));

    expect(buildClaimBatch).toHaveBeenCalledWith(7, undefined, undefined);
  });
});
