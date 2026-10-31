// ========== MOCKS (BEFORE IMPORTS - Vitest hoisting) ==========

vi.mock("../../../services/claimService", () => ({
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

import ClaimFeedbackModal from "./ClaimFeedbackModal";
import { submitClaimFeedback } from "../../../services/claimService";

const renderComponent = (props) =>
  render(
    <IntlProvider locale="en" messages={{}} defaultLocale="en">
      <ClaimFeedbackModal {...props} />
    </IntlProvider>,
  );

const defaultProps = {
  batchId: 7,
  open: true,
  onClose: vi.fn(),
  onSuccess: vi.fn(),
};

describe("ClaimFeedbackModal", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    submitClaimFeedback.mockResolvedValue({
      id: 7,
      organizationId: 3,
      batchNumber: "BATCH-001",
      status: "SUBMITTED",
      totalClaims: 3,
    });
  });

  it("is hidden when open is false", () => {
    const { queryByTestId } = renderComponent({
      ...defaultProps,
      open: false,
    });
    expect(queryByTestId("feedback-xml-input")).not.toBeInTheDocument();
  });

  it("submits the pasted XML and notifies the parent on success", async () => {
    const onSuccess = vi.fn();
    renderComponent({ ...defaultProps, open: true, onSuccess });

    const xml =
      "<ClaimFeedback><item claimLineId=\"1\" status=\"APPROVED\"/></ClaimFeedback>";
    await userEvent.type(
      screen.getByTestId("feedback-xml-input"),
      xml,
    );
    await userEvent.click(screen.getByTestId("feedback-submit-button"));

    await waitFor(() =>
      expect(submitClaimFeedback).toHaveBeenCalledWith(7, xml),
    );
    await waitFor(() => expect(onSuccess).toHaveBeenCalled());
  });

  it("does not submit when the XML is empty", async () => {
    renderComponent({ ...defaultProps, open: true });
    await userEvent.click(screen.getByTestId("feedback-submit-button"));
    expect(submitClaimFeedback).not.toHaveBeenCalled();
  });
});
