import React, { useCallback, useState } from "react";
import { Button, InlineNotification, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";
import { submitClaimFeedback, type ClaimBatchDto } from "../../../services/claimService";

interface ClaimFeedbackModalProps {
  batchId: number;
  open: boolean;
  onClose: () => void;
  onSuccess: (batch: ClaimBatchDto) => void;
}

const ClaimFeedbackModal = ({
  batchId,
  open,
  onClose,
  onSuccess,
}: ClaimFeedbackModalProps) => {
  const intl = useIntl();
  const [xml, setXml] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!xml.trim()) {
      return;
    }
    setSubmitting(true);
    setError("");
    submitClaimFeedback(batchId, xml)
      .then((batch) => {
        onSuccess(batch);
      })
      .catch(() => {
        setError(
          intl.formatMessage({
            id: "claim.feedback.submit.error",
            defaultMessage: "Failed to submit feedback.",
          }),
        );
        setSubmitting(false);
      });
  };

  if (!open) {
    return null;
  }

  return (
    <div className="claimFeedbackModal__overlay" role="dialog" aria-modal="true">
      <div className="claimFeedbackModal__panel">
        <h3>
          {intl.formatMessage({
            id: "claim.feedback.title",
            defaultMessage: "Submit NHIA feedback",
          })}
        </h3>
        <p>
          {intl.formatMessage({
            id: "claim.feedback.hint",
            defaultMessage:
              "Paste the NHIA v8.6 response XML received from the payer gateway.",
          })}
        </p>
        <form onSubmit={handleSubmit} className="claimFeedbackModal__form">
          <TextArea
            id="feedback-xml-input"
            labelText={intl.formatMessage({
              id: "claim.feedback.xml.label",
              defaultMessage: "Response XML",
            })}
            value={xml}
            onChange={(event) => setXml(event.target.value)}
            rows={12}
            data-testid="feedback-xml-input"
          />
          <div className="claimFeedbackModal__actions">
            <Button
              kind="tertiary"
              size="md"
              onClick={onClose}
              disabled={submitting}
            >
              {intl.formatMessage({
                id: "claim.feedback.cancel",
                defaultMessage: "Cancel",
              })}
            </Button>
            <Button
              kind="primary"
              size="md"
              type="submit"
              disabled={submitting || !xml.trim()}
              data-testid="feedback-submit-button"
            >
              {intl.formatMessage({
                id: "claim.feedback.submit",
                defaultMessage: "Submit feedback",
              })}
            </Button>
          </div>
        </form>
        {error && (
          <InlineNotification
            kind="error"
            title={error}
            lowContrast
            hideCloseButton
          />
        )}
      </div>
    </div>
  );
};

export default ClaimFeedbackModal;
