import React, { useCallback, useEffect, useState } from "react";
import { Button, Tag, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFacilityCredentials,
  type FacilityCredential,
} from "../../../services/billingConfigService";
import "./FacilityCredentialPanel.css";

const FacilityCredentialPanel = () => {
  const intl = useIntl();
  const [orgId, setOrgId] = useState<number | null>(null);
  const [credentials, setCredentials] = useState<FacilityCredential[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const handleLoad = () => {
    if (!orgId) {
      setError(
        intl.formatMessage({
          id: "billing.config.facility.error.orgIdRequired",
          defaultMessage: "Organization ID is required",
        }),
      );
      return;
    }
    setError(null);
    setLoading(true);
    getFacilityCredentials(orgId, (response) => {
      setCredentials(response ?? []);
      setLoading(false);
    });
  };

  const formatDate = (value: string | null | undefined): string => {
    if (!value) return "—";
    return value;
  };

  return (
    <section className="facilityCredentialPanel" data-testid="facility-credential-panel">
      <h3 className="facilityCredentialPanel__heading">
        {intl.formatMessage({
          id: "billing.config.facility.title",
          defaultMessage: "Facility Credentials",
        })}
      </h3>
      {error && (
        <div className="facilityCredentialPanel__error" role="alert">{error}</div>
      )}
      <div className="facilityCredentialPanel__toolbar">
        <TextInput
          id="facility-org-id"
          type="number"
          labelText={intl.formatMessage({
            id: "billing.config.facility.label.orgId",
            defaultMessage: "Organization ID",
          })}
          placeholder={intl.formatMessage({
            id: "billing.config.facility.placeholder.orgId",
            defaultMessage: "e.g. 42",
          })}
          value={String(orgId ?? "")}
          onChange={(event) => setOrgId(event.target.value ? Number(event.target.value) : null)}
        />
        <Button
          kind="primary"
          size="sm"
          onClick={handleLoad}
          disabled={loading}
        >
          {intl.formatMessage({
            id: "billing.config.facility.load",
            defaultMessage: "Load",
          })}
        </Button>
      </div>
      <table className="facilityCredentialPanel__table" aria-label={intl.formatMessage({ id: "billing.config.facility.table.label", defaultMessage: "Facility Credentials" })}>
        <thead>
          <tr>
            <th>{intl.formatMessage({ id: "billing.config.facility.column.technicalHead", defaultMessage: "Technical Head" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.facility.column.licenseNumber", defaultMessage: "License Number" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.facility.column.accreditationNumber", defaultMessage: "Accreditation #" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.facility.column.levelOfCare", defaultMessage: "Level of Care" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.facility.column.validity", defaultMessage: "Validity" })}</th>
          </tr>
        </thead>
        <tbody>
          {credentials.length === 0 ? (
            <tr>
              <td colSpan={5} className="facilityCredentialPanel__empty">
                {intl.formatMessage({
                  id: "billing.config.facility.empty",
                  defaultMessage: "No facility credentials found",
                })}
              </td>
            </tr>
          ) : (
            credentials.map((credential) => (
              <tr key={credential.id} className="facilityCredentialPanel__row">
                <td>{credential.technicalHeadName || "—"}</td>
                <td>{credential.technicalHeadLicenseNumber || "—"}</td>
                <td>{credential.accreditationNumber || "—"}</td>
                <td>{credential.levelOfCare || "—"}</td>
                <td>
                  {formatDate(credential.validFrom)} {intl.formatMessage({ id: "billing.config.facility.to", defaultMessage: "to" })} {formatDate(credential.validTo)}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      <p className="facilityCredentialPanel__note">
        {intl.formatMessage({
          id: "billing.config.facility.readonly",
          defaultMessage: "Facility credentials are read-only and imported from the facility accreditation source.",
        })}
      </p>
    </section>
  );
};

export default FacilityCredentialPanel;
