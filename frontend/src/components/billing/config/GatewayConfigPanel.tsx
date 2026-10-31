import React, { useCallback, useEffect, useState } from "react";
import { Button, Tag, Toggle, TextInput, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getGatewayConfig,
  saveGatewayConfig,
  type BranchGatewayConfig,
  type GatewayDriver,
} from "../../../services/billingConfigService";
import "./GatewayConfigPanel.css";

const GATEWAY_DRIVERS: GatewayDriver[] = ["PAYSTACK", "HUBTEL", "MTN_MOMO"];
const MASKED = "••••••••••••••••";

const GatewayConfigPanel = () => {
  const intl = useIntl();
  const [orgId, setOrgId] = useState<number | null>(null);
  const [driver, setDriver] = useState<GatewayDriver>("PAYSTACK");
  const [config, setConfig] = useState<BranchGatewayConfig | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [loaded, setLoaded] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  // save form fields (secrets deliberately default blank)
  const [displayName, setDisplayName] = useState<string>("");
  const [publicKey, setPublicKey] = useState<string>("");
  const [secretKey, setSecretKey] = useState<string>("");
  const [webhookSecret, setWebhookSecret] = useState<string>("");
  const [isEnabled, setIsEnabled] = useState<boolean>(true);
  const [testMode, setTestMode] = useState<boolean>(false);
  const [metadata, setMetadata] = useState<string>("");
  const [saving, setSaving] = useState<boolean>(false);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);

  const loadConfig = useCallback(() => {
    if (!orgId) return;
    setError(null);
    setLoading(true);
    getGatewayConfig(orgId, driver, (response) => {
      setLoading(false);
      setLoaded(true);
      setConfig(response ?? null);
      if (response) {
        setDisplayName(response.displayName ?? "");
        setPublicKey(response.publicKey ?? "");
        // secrets are encrypted on the server: never prefill, leave blank
        setSecretKey("");
        setWebhookSecret("");
        setIsEnabled(response.isEnabled ?? true);
        setTestMode(response.testMode ?? false);
        setMetadata(response.metadata ?? "");
      } else {
        setDisplayName("");
        setPublicKey("");
        setSecretKey("");
        setWebhookSecret("");
        setIsEnabled(true);
        setTestMode(false);
        setMetadata("");
      }
    });
  }, [orgId, driver]);

  useEffect(() => {
    loadConfig();
  }, [loadConfig]);

  const handleSave = () => {
    setSaveError(null);
    setSaveMessage(null);
    if (!orgId) {
      setSaveError(
        intl.formatMessage({
          id: "billing.config.gateway.error.orgIdRequired",
          defaultMessage: "Organization ID is required",
        }),
      );
      return;
    }
    setSaving(true);
    saveGatewayConfig(
      {
        organizationId: orgId,
        driver,
        displayName,
        publicKey,
        secretKey,
        webhookSecret,
        isEnabled,
        testMode,
        metadata,
      },
      (response) => {
        setSaving(false);
        if (response) {
          setSaveMessage(
            intl.formatMessage({
              id: "billing.config.gateway.save.success",
              defaultMessage: "Gateway config saved",
            }),
          );
          setConfig(response);
          // secrets stay blank after save so they are not revealed
          setSecretKey("");
          setWebhookSecret("");
        } else {
          setSaveError(
            intl.formatMessage({
              id: "billing.config.gateway.error.saveFailed",
              defaultMessage: "Failed to save gateway config",
            }),
          );
        }
      },
    );
  };

  const mask = (value: string | null | undefined): string => {
    if (!value) return "—";
    return MASKED;
  };

  const boolLabel = (value: boolean | null | undefined): string => {
    if (value === null || value === undefined) return "—";
    return value
      ? intl.formatMessage({ id: "billing.config.gateway.enabled", defaultMessage: "Enabled" })
      : intl.formatMessage({ id: "billing.config.gateway.disabled", defaultMessage: "Disabled" });
  };

  return (
    <section className="gatewayConfigPanel" data-testid="gateway-config-panel">
      <h3 className="gatewayConfigPanel__heading">
        {intl.formatMessage({
          id: "billing.config.gateway.title",
          defaultMessage: "Gateway Configuration",
        })}
      </h3>
      {error && (
        <div className="gatewayConfigPanel__error" role="alert">{error}</div>
      )}
      <div className="gatewayConfigPanel__toolbar">
        <TextInput
          id="gateway-org-id"
          type="number"
          labelText={intl.formatMessage({
            id: "billing.config.gateway.label.orgId",
            defaultMessage: "Organization ID",
          })}
          placeholder={intl.formatMessage({
            id: "billing.config.gateway.placeholder.orgId",
            defaultMessage: "e.g. 42",
          })}
          value={String(orgId ?? "")}
          onChange={(event) => setOrgId(event.target.value ? Number(event.target.value) : null)}
        />
        <label className="gatewayConfigPanel__label">
          {intl.formatMessage({
            id: "billing.config.gateway.label.driver",
            defaultMessage: "Driver",
          })}
        </label>
        <select
          id="gateway-driver-dropdown"
          className="gatewayConfigPanel__select"
          value={driver}
          onChange={(event) => setDriver(event.target.value as GatewayDriver)}
          data-testid="gateway-driver-dropdown"
        >
          {GATEWAY_DRIVERS.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
        <Button
          kind="primary"
          size="sm"
          onClick={loadConfig}
          disabled={loading || !orgId}
        >
          {intl.formatMessage({
            id: "billing.config.gateway.load",
            defaultMessage: "Load",
          })}
        </Button>
      </div>
      {loaded && (
        <>
          <div className="gatewayConfigPanel__summary">
            <h4 className="gatewayConfigPanel__summaryTitle">
              {intl.formatMessage({
                id: "billing.config.gateway.summary.title",
                defaultMessage: "Existing Config",
              })}
            </h4>
            <dl className="gatewayConfigPanel__summaryList">
              <dt>{intl.formatMessage({ id: "billing.config.gateway.summary.driver", defaultMessage: "Driver" })}</dt>
              <dd>{config?.driver || "—"}</dd>
              <dt>{intl.formatMessage({ id: "billing.config.gateway.summary.displayName", defaultMessage: "Display Name" })}</dt>
              <dd>{config?.displayName || "—"}</dd>
              <dt>{intl.formatMessage({ id: "billing.config.gateway.summary.publicKey", defaultMessage: "Public Key" })}</dt>
              <dd>{config?.publicKey ? config.publicKey : "—"}</dd>
              <dt>{intl.formatMessage({ id: "billing.config.gateway.summary.secretKey", defaultMessage: "Secret Key" })}</dt>
              <dd>{config?.secretKey ? mask(config.secretKey) : "—"} <span className="gatewayConfigPanel__maskedNote">(encrypted — never revealed)</span></dd>
              <dt>{intl.formatMessage({ id: "billing.config.gateway.summary.webhookSecret", defaultMessage: "Webhook Secret" })}</dt>
              <dd>{config?.webhookSecret ? mask(config.webhookSecret) : "—"} <span className="gatewayConfigPanel__maskedNote">(encrypted — never revealed)</span></dd>
              <dt>{intl.formatMessage({ id: "billing.config.gateway.summary.enabled", defaultMessage: "Enabled" })}</dt>
              <dd>{boolLabel(config?.isEnabled)}</dd>
              <dt>{intl.formatMessage({ id: "billing.config.gateway.summary.testMode", defaultMessage: "Test Mode" })}</dt>
              <dd>{boolLabel(config?.testMode)}</dd>
            </dl>
          </div>
          <div className="gatewayConfigPanel__form">
            <h4 className="gatewayConfigPanel__formTitle">
              {intl.formatMessage({
                id: "billing.config.gateway.save.title",
                defaultMessage: "Save Gateway Config",
              })}
            </h4>
            <p className="gatewayConfigPanel__secretNote">
              {intl.formatMessage({
                id: "billing.config.gateway.secretNote",
                defaultMessage: "Leave secretKey / webhookSecret blank to keep the existing encrypted values.",
              })}
            </p>
            <TextInput
              id="gateway-display-name"
              labelText={intl.formatMessage({
                id: "billing.config.gateway.label.displayName",
                defaultMessage: "Display Name",
              })}
              placeholder={intl.formatMessage({
                id: "billing.config.gateway.placeholder.displayName",
                defaultMessage: "e.g. Paystack Production",
              })}
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
            />
            <TextInput
              id="gateway-public-key"
              labelText={intl.formatMessage({
                id: "billing.config.gateway.label.publicKey",
                defaultMessage: "Public Key",
              })}
              placeholder={intl.formatMessage({
                id: "billing.config.gateway.placeholder.publicKey",
                defaultMessage: "pk_test_...",
              })}
              value={publicKey}
              onChange={(event) => setPublicKey(event.target.value)}
            />
            <TextInput
              id="gateway-secret-key"
              labelText={intl.formatMessage({
                id: "billing.config.gateway.label.secretKey",
                defaultMessage: "Secret Key",
              })}
              placeholder={intl.formatMessage({
                id: "billing.config.gateway.placeholder.secretKey",
                defaultMessage: "leave blank to keep existing",
              })}
              value={secretKey}
              onChange={(event) => setSecretKey(event.target.value)}
              type="password"
            />
            <TextInput
              id="gateway-webhook-secret"
              labelText={intl.formatMessage({
                id: "billing.config.gateway.label.webhookSecret",
                defaultMessage: "Webhook Secret",
              })}
              placeholder={intl.formatMessage({
                id: "billing.config.gateway.placeholder.webhookSecret",
                defaultMessage: "leave blank to keep existing",
              })}
              value={webhookSecret}
              onChange={(event) => setWebhookSecret(event.target.value)}
              type="password"
            />
            <div className="gatewayConfigPanel__fieldRow">
              <label className="gatewayConfigPanel__checkboxLabel">
                <Toggle
                  id="gateway-enabled-toggle"
                  toggled={isEnabled}
                  onToggle={(checked) => setIsEnabled(checked)}
                  labelText={intl.formatMessage({
                    id: "billing.config.gateway.label.isEnabled",
                    defaultMessage: "Enabled",
                  })}
                />
              </label>
              <label className="gatewayConfigPanel__checkboxLabel">
                <Toggle
                  id="gateway-testmode-toggle"
                  toggled={testMode}
                  onToggle={(checked) => setTestMode(checked)}
                  labelText={intl.formatMessage({
                    id: "billing.config.gateway.label.testMode",
                    defaultMessage: "Test Mode",
                  })}
                />
              </label>
            </div>
            <TextArea
              id="gateway-metadata"
              labelText={intl.formatMessage({
                id: "billing.config.gateway.label.metadata",
                defaultMessage: "Metadata (JSON)",
              })}
              placeholder={intl.formatMessage({
                id: "billing.config.gateway.placeholder.metadata",
                defaultMessage: '{"region": "west-africa"}',
              })}
              value={metadata}
              onChange={(event) => setMetadata(event.target.value)}
              rows={2}
            />
            <div className="gatewayConfigPanel__actions">
              {saveMessage && (
                <span className="gatewayConfigPanel__saveMessage">{saveMessage}</span>
              )}
              {saveError && (
                <span className="gatewayConfigPanel__saveError">{saveError}</span>
              )}
              <Button
                kind="primary"
                size="md"
                onClick={handleSave}
                disabled={saving || !orgId}
                data-testid="gateway-submit"
              >
                {intl.formatMessage({
                  id: "billing.config.gateway.submit",
                  defaultMessage: "Save Config",
                })}
              </Button>
            </div>
          </div>
        </>
      )}
    </section>
  );
};

export default GatewayConfigPanel;
