import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  Column,
  Grid,
  InlineNotification,
  Tag,
  TextArea,
  Tile,
} from "@carbon/react";
import { Copy, Save } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  activateLicense,
  getLicenseFingerprint,
  getLicenseStatus,
  type LicenseStatusResponse,
} from "../../../services/licenseService";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import "./LicenseSettings.css";

const TRIAL_STATUS_ID = "license.status.trial";

const LicenseSettings = () => {
  const intl = useIntl();
  const [status, setStatus] = useState<LicenseStatusResponse | null>(null);
  const [fingerprint, setFingerprint] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(true);
  const [licenseKey, setLicenseKey] = useState<string>("");
  const [activating, setActivating] = useState<boolean>(false);
  const [activationError, setActivationError] = useState<string | null>(null);
  const [activationMessage, setActivationMessage] = useState<string | null>(
    null,
  );
  const [copied, setCopied] = useState<boolean>(false);

  const loadStatus = useCallback(() => {
    setLoading(true);
    getLicenseStatus((response) => {
      setStatus(response ?? null);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    loadStatus();
    getLicenseFingerprint((response) => {
      setFingerprint(response?.fingerprint ?? "");
    });
  }, [loadStatus]);

  const statusLabel = (value?: string) => {
    if (!value) {
      return intl.formatMessage({ id: TRIAL_STATUS_ID });
    }
    return intl.formatMessage({ id: `license.status.${value.toLowerCase()}` }, {
      defaultMessage: value,
    });
  };

  const handleCopyFingerprint = async () => {
    try {
      await navigator.clipboard.writeText(fingerprint);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  const handleActivate = () => {
    if (!licenseKey.trim()) {
      setActivationError(intl.formatMessage({ id: "license.activate.empty" }));
      return;
    }
    setActivating(true);
    setActivationError(null);
    setActivationMessage(null);
    activateLicense(licenseKey, (response) => {
      setActivating(false);
      if (response?.status === "VALID") {
        setLicenseKey("");
        setActivationMessage(
          intl.formatMessage({ id: "license.activate.success" }),
        );
        loadStatus();
      } else {
        setActivationError(
          intl.formatMessage({ id: "license.activate.failure" }),
        );
      }
    });
  };

  const isValid = status?.status === "VALID";
  const trialDays =
    typeof status?.trialDaysRemaining === "number"
      ? status.trialDaysRemaining
      : null;

  return (
    <div className="licenseSettings">
      <PageBreadCrumb
        breadcrumbs={[
          {
            label: "license.title",
            isCurrentPage: true,
          },
        ]}
      />
      <h2 className="licenseSettings__heading">
        {intl.formatMessage({ id: "license.title" })}
      </h2>
      <Grid>
        <Column sm={4} md={8} lg={16}>
          <p className="licenseSettings__description">
            {intl.formatMessage({ id: "license.description" })}
          </p>
        </Column>

        <Column sm={4} md={8} lg={16}>
          <Tile className="licenseSettings__tile">
            <h3 className="licenseSettings__tileTitle">
              {intl.formatMessage({ id: "license.status.title" })}
            </h3>
            {loading ? (
              <p>{intl.formatMessage({ id: "license.loading" })}</p>
            ) : (
              <>
                <div className="licenseSettings__statusRow">
                  <Tag
                    type={isValid ? "green" : "red"}
                    size="md"
                    data-testid="license-status-tag"
                  >
                    {statusLabel(status?.status)}
                  </Tag>
                  {trialDays !== null && !isValid && (
                    <span data-testid="license-trial-days">
                      {intl.formatMessage(
                        { id: "license.trial.daysRemaining" },
                        { days: trialDays },
                      )}
                    </span>
                  )}
                </div>
                {status?.expiresAt && (
                  <p className="licenseSettings__expiry">
                    {intl.formatMessage(
                      { id: "license.expiresAt" },
                      { date: status.expiresAt },
                    )}
                  </p>
                )}
                {!!status?.features?.length && (
                  <p
                    className="licenseSettings__features"
                    data-testid="license-features"
                  >
                    {intl.formatMessage({ id: "license.features" })}
                    {": "}
                    {status.features.join(", ")}
                  </p>
                )}
              </>
            )}
          </Tile>
        </Column>

        <Column sm={4} md={8} lg={16}>
          <Tile className="licenseSettings__tile">
            <h3 className="licenseSettings__tileTitle">
              {intl.formatMessage({ id: "license.fingerprint.title" })}
            </h3>
            <p className="licenseSettings__hint">
              {intl.formatMessage({ id: "license.fingerprint.hint" })}
            </p>
            <div className="licenseSettings__fingerprintRow">
              <code
                className="licenseSettings__fingerprint"
                data-testid="license-fingerprint"
              >
                {fingerprint || "..."}
              </code>
              <Button
                kind="tertiary"
                size="md"
                renderIcon={Copy}
                onClick={handleCopyFingerprint}
                disabled={!fingerprint}
              >
                {intl.formatMessage({
                  id: copied
                    ? "license.fingerprint.copied"
                    : "license.fingerprint.copy",
                })}
              </Button>
            </div>
          </Tile>
        </Column>

        <Column sm={4} md={8} lg={16}>
          <Tile className="licenseSettings__tile">
            <h3 className="licenseSettings__tileTitle">
              {intl.formatMessage({ id: "license.activate.title" })}
            </h3>
            <p className="licenseSettings__hint">
              {intl.formatMessage({ id: "license.activate.hint" })}
            </p>
            <TextArea
              id="license-activation-key"
              labelText={intl.formatMessage({ id: "license.activate.label" })}
              placeholder={intl.formatMessage({
                id: "license.activate.placeholder",
              })}
              value={licenseKey}
              onChange={(event: React.ChangeEvent<HTMLTextAreaElement>) =>
                setLicenseKey(event.target.value)
              }
              rows={4}
              data-testid="license-key-input"
            />
            <div className="licenseSettings__activateRow">
              <Button
                kind="primary"
                size="md"
                renderIcon={Save}
                onClick={handleActivate}
                disabled={activating || !licenseKey.trim()}
                data-testid="license-activate-button"
              >
                {intl.formatMessage({ id: "license.activate.submit" })}
              </Button>
            </div>
            {activationError && (
              <InlineNotification
                kind="error"
                title={intl.formatMessage({ id: "license.activate.error" })}
                subtitle={activationError}
                lowContrast
                hideCloseButton
              />
            )}
            {activationMessage && (
              <InlineNotification
                kind="success"
                title={activationMessage}
                lowContrast
                hideCloseButton
              />
            )}
          </Tile>
        </Column>
      </Grid>
    </div>
  );
};

export default LicenseSettings;
