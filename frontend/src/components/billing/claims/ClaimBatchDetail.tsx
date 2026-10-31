import React, { useCallback, useEffect, useState } from "react";
import { Button, InlineNotification, Tag } from "@carbon/react";
import { Download } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { format } from "date-fns";
import {
  getClaimBatchLines,
  getClaimBatchXml,
  type ClaimBatchDto,
  type ClaimLineDto,
  type ClaimLineStatus,
} from "../../../services/claimService";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import ClaimFeedbackModal from "./ClaimFeedbackModal";
import "./ClaimBatchDetail.css";

const BATCH_STATUS_TAG: Record<
  ClaimBatchDto["status"],
  "green" | "red" | "blue" | "gray"
> = {
  DRAFT: "gray",
  READY: "blue",
  SUBMITTED: "blue",
  PAID: "green",
  REJECTED: "red",
};

const LINE_STATUS_TAG: Record<
  ClaimLineStatus,
  "green" | "red" | "gray"
> = {
  PENDING: "gray",
  APPROVED: "green",
  REJECTED: "red",
  PAID: "green",
};

const formatMoney = (value: number | null | undefined): string => {
  if (value == null) {
    return "—";
  }
  return value.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
};

const formatDate = (value: string | undefined | null): string => {
  if (!value) {
    return "—";
  }
  return format(new Date(value), "yyyy-MM-dd");
};

type ClaimBatchDetailProps = {
  batch: ClaimBatchDto;
  onBack: () => void;
  onRefresh: (batchId: number) => void;
};

const ClaimBatchDetail = ({
  batch,
  onBack,
  onRefresh,
}: ClaimBatchDetailProps) => {
  const intl = useIntl();
  const [lines, setLines] = useState<ClaimLineDto[]>([]);
  const [xml, setXml] = useState<string | null>(null);
  const [xmlLoading, setXmlLoading] = useState(false);
  const [xmlError, setXmlError] = useState("");
  const [feedbackOpen, setFeedbackOpen] = useState(false);

  const fetchXml = useCallback(() => {
    setXmlLoading(true);
    setXmlError("");
    getClaimBatchXml(batch.id)
      .then((text) => {
        setXml(text);
        setXmlLoading(false);
      })
      .catch(() => {
        setXmlError(
          intl.formatMessage({
            id: "claim.detail.xml.error",
            defaultMessage: "Failed to load claim XML.",
          }),
        );
        setXmlLoading(false);
      });
  }, [batch.id, intl]);

  useEffect(() => {
    setXml(null);
    setXmlError("");
    fetchXml();
  }, [batch.id, fetchXml]);

  useEffect(() => {
    let active = true;
    getClaimBatchLines(batch.id)
      .then((rows) => {
        if (active) {
          setLines(rows ?? []);
        }
      })
      .catch(() => {
        if (active) {
          setLines([]);
        }
      });
    return () => {
      active = false;
    };
  }, [batch.id]);

  const handleDownload = () => {
    if (!xml) {
      return;
    }
    const blob = new Blob([xml], { type: "text/xml" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `claim-batch-${batch.batchNumber}.xml`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleFeedbackOpen = () => {
    setFeedbackOpen(true);
  };

  const handleFeedbackSuccess = (updated: ClaimBatchDto) => {
    setFeedbackOpen(false);
    onRefresh(batch.id);
  };

  const statusLabel = (status: ClaimLineStatus) => {
    return intl.formatMessage(
      { id: `claim.line.status.${status}` },
      { defaultMessage: status },
    );
  };

  return (
    <div className="claimBatchDetail" data-testid="claim-batch-detail">
      <PageBreadCrumb
        breadcrumbs={[
          { label: "claim.batch.detail", isCurrentPage: true },
        ]}
      />
      <Button
        kind="tertiary"
        size="sm"
        onClick={onBack}
        data-testid="back-to-list-button"
      >
        {intl.formatMessage({
          id: "claim.detail.back",
          defaultMessage: "Back to batches",
        })}
      </Button>
      <h2 className="claimBatchDetail__heading">
        {intl.formatMessage({
          id: "claim.detail.title",
          defaultMessage: "Claim Batch Detail",
        })}
      </h2>
      <div className="claimBatchDetail__meta">
        <div>
          <span className="claimBatchDetail__label">
            {intl.formatMessage({
              id: "claim.detail.batch",
              defaultMessage: "Batch",
            })}
          </span>
          <span>{batch.batchNumber}</span>
        </div>
        <div>
          <span className="claimBatchDetail__label">
            {intl.formatMessage({
              id: "claim.detail.org",
              defaultMessage: "Organization",
            })}
          </span>
          <span>{batch.organizationId}</span>
        </div>
        <div>
          <span className="claimBatchDetail__label">
            {intl.formatMessage({
              id: "claim.detail.status",
              defaultMessage: "Status",
            })}
          </span>
          <Tag type={BATCH_STATUS_TAG[batch.status]} size="md">
            {intl.formatMessage(
              { id: `claim.batch.status.${batch.status}` },
              { defaultMessage: batch.status },
            )}
          </Tag>
        </div>
        <div>
          <span className="claimBatchDetail__label">
            {intl.formatMessage({
              id: "claim.detail.totalClaims",
              defaultMessage: "Lines",
            })}
          </span>
          <span>{batch.totalClaims ?? 0}</span>
        </div>
        <div>
          <span className="claimBatchDetail__label">
            {intl.formatMessage({
              id: "claim.detail.totalAmount",
              defaultMessage: "Total amount",
            })}
          </span>
          <span>{formatMoney(batch.totalAmount)}</span>
        </div>
        <div>
          <span className="claimBatchDetail__label">
            {intl.formatMessage({
              id: "claim.detail.period",
              defaultMessage: "Period",
            })}
          </span>
          <span>{formatDate(batch.batchDate)}</span>
        </div>
        <div>
          <span className="claimBatchDetail__label">
            {intl.formatMessage({
              id: "claim.detail.updated",
              defaultMessage: "Updated",
            })}
          </span>
          <span>{formatDate(batch.lastupdated)}</span>
        </div>
      </div>

      <div className="claimBatchDetail__actions">
        <Button
          kind="primary"
          size="md"
          renderIcon={Download}
          onClick={handleDownload}
          disabled={xmlLoading || !xml}
          data-testid="download-xml-button"
        >
          {intl.formatMessage({
            id: "claim.detail.downloadXml",
            defaultMessage: "Download XML",
          })}
        </Button>
        <Button
          kind="secondary"
          size="md"
          onClick={handleFeedbackOpen}
          disabled={xmlLoading}
          data-testid="feedback-open-button"
        >
          {intl.formatMessage({
            id: "claim.detail.openFeedback",
            defaultMessage: "Submit feedback",
          })}
        </Button>
      </div>

      {xmlError && (
        <InlineNotification
          kind="error"
          title={xmlError}
          lowContrast
          hideCloseButton
        />
      )}

      <div className="claimBatchDetail__xml">
        <h3>
          {intl.formatMessage({
            id: "claim.detail.xml",
            defaultMessage: "NHIA v8.6 claim XML",
          })}
        </h3>
        {xmlLoading && (
          <p>
            {intl.formatMessage({
              id: "claim.detail.loading",
              defaultMessage: "Loading XML…",
            })}
          </p>
        )}
        {!xmlLoading && xml && <pre data-testid="claim-xml">{xml}</pre>}
        {!xmlLoading && !xml && !xmlError && (
          <p className="claimBatchDetail__empty">
            {intl.formatMessage({
              id: "claim.detail.noXml",
              defaultMessage: "No XML available.",
            })}
          </p>
        )}
      </div>

      <h3>
        {intl.formatMessage({
          id: "claim.detail.lines",
          defaultMessage: "Claim lines",
        })}
      </h3>
      {lines.length === 0 && (
        <p className="claimBatchDetail__empty">
          {intl.formatMessage({
            id: "claim.detail.noLines",
            defaultMessage: "No claim lines available.",
          })}
        </p>
      )}
      {lines.length > 0 && (
        <table className="claimBatchDetail__table">
          <thead>
            <tr>
              <th>
                {intl.formatMessage({
                  id: "claim.line.patient",
                  defaultMessage: "Patient",
                })}
              </th>
              <th>
                {intl.formatMessage({
                  id: "claim.line.member",
                  defaultMessage: "Member",
                })}
              </th>
              <th>
                {intl.formatMessage({
                  id: "claim.line.service",
                  defaultMessage: "Service",
                })}
              </th>
              <th>
                {intl.formatMessage({
                  id: "claim.line.claimed",
                  defaultMessage: "Claimed",
                })}
              </th>
              <th>
                {intl.formatMessage({
                  id: "claim.line.approved",
                  defaultMessage: "Approved",
                })}
              </th>
              <th>
                {intl.formatMessage({
                  id: "claim.line.status",
                  defaultMessage: "Status",
                })}
              </th>
            </tr>
          </thead>
          <tbody>
            {lines.map((line) => (
              <tr key={line.id}>
                <td>{line.patientId ?? "—"}</td>
                <td>{line.memberNumber ?? "—"}</td>
                <td>{line.serviceCode ?? "—"}</td>
                <td>{formatMoney(line.claimedAmount)}</td>
                <td>{formatMoney(line.approvedAmount)}</td>
                <td>
                  <Tag type={LINE_STATUS_TAG[line.status]} size="md">
                    {statusLabel(line.status)}
                  </Tag>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <ClaimFeedbackModal
        batchId={batch.id}
        open={feedbackOpen}
        onClose={() => setFeedbackOpen(false)}
        onSuccess={handleFeedbackSuccess}
      />
    </div>
  );
};

export default ClaimBatchDetail;
