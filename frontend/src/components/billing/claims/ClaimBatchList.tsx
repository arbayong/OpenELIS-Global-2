import React, { useCallback, useEffect, useState } from "react";
import { Button, InlineNotification, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { format } from "date-fns";
import {
  buildClaimBatch,
  getClaimBatch,
  getClaimBatchesByOrg,
  type ClaimBatchDto,
  type ClaimBatchStatus,
} from "../../../services/claimService";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import ClaimBatchDetail from "./ClaimBatchDetail";
import "./ClaimBatchList.css";

const STATUS_TAG: Record<
  ClaimBatchStatus,
  "green" | "red" | "blue" | "gray"
> = {
  DRAFT: "gray",
  READY: "blue",
  SUBMITTED: "blue",
  PAID: "green",
  REJECTED: "red",
};

const formatDate = (value: string | undefined | null): string => {
  if (!value) {
    return "—";
  }
  return format(new Date(value), "yyyy-MM-dd");
};

const ClaimBatchList = () => {
  const intl = useIntl();
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [batches, setBatches] = useState<ClaimBatchDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState<ClaimBatchDto | null>(null);

  const loadBatches = useCallback(() => {
    const id = Number(orgId);
    if (!orgId.trim() || !Number.isFinite(id)) {
      setBatches([]);
      setSelected(null);
      return;
    }
    setLoading(true);
    setError("");
    getClaimBatchesByOrg(id)
      .then((data) => {
        setBatches(data ?? []);
        setLoading(false);
      })
      .catch(() => {
        setError(
          intl.formatMessage({
            id: "claim.batch.load.error",
            defaultMessage: "Failed to load claim batches.",
          }),
        );
        setLoading(false);
      });
  }, [orgId, intl]);

  useEffect(() => {
    loadBatches();
  }, [loadBatches]);

  const handleBuild = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const id = Number(orgId);
    if (!orgId.trim() || !Number.isFinite(id)) {
      return;
    }
    setLoading(true);
    setError("");
    buildClaimBatch(id, from || undefined, to || undefined)
      .then((batch) => {
        setSelected(batch);
        return loadBatches();
      })
      .catch(() => {
        setError(
          intl.formatMessage({
            id: "claim.batch.build.error",
            defaultMessage: "Failed to build claim batch.",
          }),
        );
        setLoading(false);
      });
  };

  const handleSelect = (batch: ClaimBatchDto) => {
    setSelected(batch);
  };

  if (selected) {
    return (
      <div className="claimBatchList" data-testid="claim-batch-list">
        <PageBreadCrumb
          breadcrumbs={[
            { label: "claim.batch.list", isCurrentPage: true },
          ]}
        />
        <Button
          kind="tertiary"
          size="sm"
          onClick={() => setSelected(null)}
          data-testid="back-to-list-button"
        >
          {intl.formatMessage({
            id: "claim.batch.back",
            defaultMessage: "Back to batches",
          })}
        </Button>
        <ClaimBatchDetail
          batch={selected}
          onBack={() => setSelected(null)}
          onRefresh={(id) => getClaimBatch(id).then(setSelected)}
        />
      </div>
    );
  }

  return (
    <div className="claimBatchList" data-testid="claim-batch-list">
      <PageBreadCrumb
        breadcrumbs={[
          { label: "claim.batch.list", isCurrentPage: true },
        ]}
      />
      <h2 className="claimBatchList__heading">
        {intl.formatMessage({
          id: "claim.batch.title",
          defaultMessage: "NHIS Claim Batches",
        })}
      </h2>
      <form onSubmit={handleBuild} className="claimBatchList__form">
        <div className="claimBatchList__field">
          <label className="claimBatchList__label">
            {intl.formatMessage({
              id: "claim.batch.orgId.label",
              defaultMessage: "Organization ID",
            })}
          </label>
          <input
            id="org-id-input"
            className="claimBatchList__input"
            placeholder={intl.formatMessage({
              id: "claim.batch.orgId.placeholder",
              defaultMessage: "e.g. 1",
            })}
            type="number"
            min="1"
            value={orgId}
            onChange={(event) => setOrgId(event.target.value)}
            data-testid="org-id-input"
          />
        </div>
        <div className="claimBatchList__field">
          <label className="claimBatchList__label">
            {intl.formatMessage({
              id: "claim.batch.from.label",
              defaultMessage: "From",
            })}
          </label>
          <input
            id="from-date-input"
            className="claimBatchList__input"
            type="date"
            value={from}
            onChange={(event) => setFrom(event.target.value)}
          />
        </div>
        <div className="claimBatchList__field">
          <label className="claimBatchList__label">
            {intl.formatMessage({
              id: "claim.batch.to.label",
              defaultMessage: "To",
            })}
          </label>
          <input
            id="to-date-input"
            className="claimBatchList__input"
            type="date"
            value={to}
            onChange={(event) => setTo(event.target.value)}
          />
        </div>
        <Button
          kind="primary"
          size="md"
          type="submit"
          disabled={loading}
          data-testid="build-batch-button"
        >
          {intl.formatMessage({
            id: "claim.batch.build",
            defaultMessage: "Build batch",
          })}
        </Button>
      </form>
      {loading && (
        <p>
          {intl.formatMessage({
            id: "claim.batch.loading",
            defaultMessage: "Loading…",
          })}
        </p>
      )}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          lowContrast
          hideCloseButton
        />
      )}
      {!loading &&
        batches.length === 0 &&
        orgId.trim() &&
        !error && (
          <p className="claimBatchList__empty">
            {intl.formatMessage({
              id: "claim.batch.empty",
              defaultMessage:
                "No claim batches found for this organization.",
            })}
          </p>
        )}
      {batches.length > 0 && (
        <table className="claimBatchList__table">
          <thead>
            <tr>
              <th>
                {intl.formatMessage({
                  id: "claim.batch.column.batch",
                  defaultMessage: "Batch",
                })}
              </th>
              <th>
                {intl.formatMessage({
                  id: "claim.batch.column.period",
                  defaultMessage: "Period",
                })}
              </th>
              <th>
                {intl.formatMessage({
                  id: "claim.batch.column.status",
                  defaultMessage: "Status",
                })}
              </th>
              <th>
                {intl.formatMessage({
                  id: "claim.batch.column.lines",
                  defaultMessage: "Lines",
                })}
              </th>
              <th>
                {intl.formatMessage({
                  id: "claim.batch.column.updated",
                  defaultMessage: "Updated",
                })}
              </th>
            </tr>
          </thead>
          <tbody>
            {batches.map((batch) => (
              <tr
                key={batch.id}
                data-testid="claim-batch-row"
                onClick={() => handleSelect(batch)}
                className="claimBatchList__row"
              >
                <td>{batch.batchNumber}</td>
                <td>{formatDate(batch.batchDate)}</td>
                <td>
                  <Tag
                    type={STATUS_TAG[batch.status]}
                    size="md"
                    data-testid="claim-batch-status-tag"
                  >
                    {intl.formatMessage(
                      { id: `claim.batch.status.${batch.status}` },
                      { defaultMessage: batch.status },
                    )}
                  </Tag>
                </td>
                <td>{batch.totalClaims ?? 0}</td>
                <td>{formatDate(batch.lastupdated)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default ClaimBatchList;
