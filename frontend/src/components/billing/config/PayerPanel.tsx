import React, { useCallback, useEffect, useState } from "react";
import { Button, Tag, TextArea, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import {
  createPayer,
  deactivatePayer,
  getPayers,
  type BillingPayer,
  type PayerType,
} from "../../../services/billingConfigService";
import "./PayerPanel.css";

const PAYER_TYPES: PayerType[] = ["CASH", "NHIS", "PRIVATE"];
const RECORDED_BY = "billing-config-operator";

const PayerPanel = () => {
  const intl = useIntl();
  const [payers, setPayers] = useState<BillingPayer[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState<string>("");
  const [type, setType] = useState<PayerType>("CASH");
  const [config, setConfig] = useState<string>("{}");
  const [creating, setCreating] = useState<boolean>(false);

  const loadPayers = useCallback(() => {
    setLoading(true);
    setError(null);
    getPayers((response) => {
      setPayers(response ?? []);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    loadPayers();
  }, [loadPayers]);

  const handleNameChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    setName(event.target.value);
    setError(null);
  };

  const handleTypeChange = (
    event: React.ChangeEvent<HTMLSelectElement>,
  ) => {
    setType(event.target.value as PayerType);
  };

  const handleConfigChange = (
    event: React.ChangeEvent<HTMLTextAreaElement>,
  ) => {
    setConfig(event.target.value);
    setError(null);
  };

  const handleCreate = () => {
    setError(null);
    if (!name.trim()) {
      setError(
        intl.formatMessage({
          id: "billing.config.payer.error.nameRequired",
          defaultMessage: "Payer name is required",
        }),
      );
      return;
    }
    try {
      JSON.parse(config);
    } catch {
      setError(
        intl.formatMessage({
          id: "billing.config.payer.error.invalidConfig",
          defaultMessage: "Config must be valid JSON",
        }),
      );
      return;
    }
    setCreating(true);
    createPayer(
      {
        name: name.trim(),
        type,
        config,
        recordedBy: RECORDED_BY,
      },
      (response) => {
        setCreating(false);
        if (response) {
          setName("");
          setType("CASH");
          setConfig("{}");
          loadPayers();
        } else {
          setError(
            intl.formatMessage({
              id: "billing.config.payer.error.createFailed",
              defaultMessage: "Failed to create payer",
            }),
          );
        }
      },
    );
  };

  const handleDeactivate = (id: number | string) => {
    deactivatePayer(id, (response) => {
      if (response) {
        loadPayers();
      }
    });
  };

  const isActive = (payer: BillingPayer): boolean => {
    const value = payer.isActive;
    return value === "Y" || value === "true" || value === "1";
  };

  return (
    <section className="payerPanel" data-testid="payer-panel">
      <h3 className="payerPanel__heading">
        {intl.formatMessage({
          id: "billing.config.payer.title",
          defaultMessage: "Payers",
        })}
      </h3>
      {error && (
        <div className="payerPanel__error" role="alert">
          {error}
        </div>
      )}
      <table className="payerPanel__table" aria-label={intl.formatMessage({ id: "billing.config.payer.table.label", defaultMessage: "Payers" })}>
        <thead>
          <tr>
            <th>{intl.formatMessage({ id: "billing.config.payer.column.name", defaultMessage: "Name" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.payer.column.type", defaultMessage: "Type" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.payer.column.active", defaultMessage: "Active" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.payer.column.actions", defaultMessage: "Actions" })}</th>
          </tr>
        </thead>
        <tbody>
          {payers.length === 0 ? (
            <tr>
              <td colSpan={4} className="payerPanel__empty">
                {intl.formatMessage({
                  id: "billing.config.payer.empty",
                  defaultMessage: "No payers found",
                })}
              </td>
            </tr>
          ) : (
            payers.map((payer) => (
              <tr
                key={payer.id}
                data-testid="payer-row"
                data-payer-id={String(payer.id ?? "")}
                className="payerPanel__row"
              >
                <td>{payer.name || "—"}</td>
                <td>
                  <Tag size="sm">{payer.type || "—"}</Tag>
                </td>
                <td>
                  <Tag
                    type={isActive(payer) ? "green" : "gray"}
                    size="sm"
                  >
                    {isActive(payer)
                      ? intl.formatMessage({
                          id: "billing.config.payer.active",
                          defaultMessage: "Yes",
                        })
                      : intl.formatMessage({
                          id: "billing.config.payer.inactive",
                          defaultMessage: "No",
                        })}
                  </Tag>
                </td>
                <td>
                  <Button
                    kind="tertiary"
                    size="sm"
                    onClick={() => handleDeactivate(payer.id ?? "")}
                    disabled={loading || creating || !payer.id}
                  >
                    {intl.formatMessage({
                      id: "billing.config.payer.deactivate",
                      defaultMessage: "Deactivate",
                    })}
                  </Button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      <div className="payerPanel__form">
        <h4 className="payerPanel__formTitle">
          {intl.formatMessage({
            id: "billing.config.payer.create.title",
            defaultMessage: "Create Payer",
          })}
        </h4>
        <TextInput
          id="payer-name-input"
          labelText={intl.formatMessage({
            id: "billing.config.payer.label.name",
            defaultMessage: "Name",
          })}
          placeholder={intl.formatMessage({
            id: "billing.config.payer.placeholder.name",
            defaultMessage: "e.g. National Health Insurance Scheme",
          })}
          value={name}
          onChange={handleNameChange}
          data-testid="payer-name-input"
        />
        <label className="payerPanel__label">
          {intl.formatMessage({
            id: "billing.config.payer.label.type",
            defaultMessage: "Type",
          })}
        </label>
        <select
          className="payerPanel__select"
          value={type}
          onChange={handleTypeChange}
        >
          {PAYER_TYPES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <TextArea
          id="payer-config"
          labelText={intl.formatMessage({
            id: "billing.config.payer.label.config",
            defaultMessage: "Config (JSON)",
          })}
          placeholder={intl.formatMessage({
            id: "billing.config.payer.placeholder.config",
            defaultMessage: '{"key": "value"}',
          })}
          value={config}
          onChange={handleConfigChange}
          rows={3}
        />
        <div className="payerPanel__actions">
          <Button
            kind="primary"
            size="md"
            onClick={handleCreate}
            disabled={creating || !name.trim()}
            data-testid="payer-submit"
          >
            {intl.formatMessage({
              id: "billing.config.payer.submit",
              defaultMessage: "Create Payer",
            })}
          </Button>
        </div>
      </div>
    </section>
  );
};

export default PayerPanel;
