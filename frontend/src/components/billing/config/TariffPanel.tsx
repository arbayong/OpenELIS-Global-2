import React, { useCallback, useEffect, useState } from "react";
import { Button, Tag, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import {
  createTariff,
  getTariffPrice,
  getTariffs,
  type BillingTariff,
  type PayerType,
} from "../../../services/billingConfigService";
import "./TariffPanel.css";

const PAYER_TYPES: PayerType[] = ["CASH", "NHIS", "PRIVATE"];

const TariffPanel = () => {
  const intl = useIntl();
  const [tariffs, setTariffs] = useState<BillingTariff[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  // price lookup
  const [priceLocalCode, setPriceLocalCode] = useState<string>("");
  const [pricePayerType, setPricePayerType] = useState<PayerType>("CASH");
  const [resolvedPrice, setResolvedPrice] = useState<number | null>(null);
  const [lookupLoading, setLookupLoading] = useState<boolean>(false);
  // create tariff
  const [localCode, setLocalCode] = useState<string>("");
  const [testId, setTestId] = useState<string>("");
  const [price, setPrice] = useState<string>("");
  const [tariffPayerType, setTariffPayerType] = useState<PayerType>("CASH");
  const [effectiveFrom, setEffectiveFrom] = useState<string>("");
  const [effectiveTo, setEffectiveTo] = useState<string>("");
  const [creating, setCreating] = useState<boolean>(false);

  const loadTariffs = useCallback(() => {
    setLoading(true);
    setError(null);
    getTariffs({}, (response) => {
      setTariffs(response ?? []);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    loadTariffs();
  }, [loadTariffs]);

  const handleLookupPrice = () => {
    if (!priceLocalCode.trim()) {
      setError(
        intl.formatMessage({
          id: "billing.config.tariff.error.localCodeRequired",
          defaultMessage: "Local code is required",
        }),
      );
      return;
    }
    setError(null);
    setLookupLoading(true);
    getTariffPrice(priceLocalCode.trim(), pricePayerType, (response) => {
      setLookupLoading(false);
      if (response?.price !== null && response?.price !== undefined) {
        setResolvedPrice(response.price);
      } else {
        setResolvedPrice(null);
        setError(
          intl.formatMessage({
            id: "billing.config.tariff.error.priceNotFound",
            defaultMessage: "No price found for this local code / payer type",
          }),
        );
      }
    });
  };

  const handleSubmit = () => {
    setError(null);
    if (!localCode.trim()) {
      setError(
        intl.formatMessage({
          id: "billing.config.tariff.error.localCodeRequired",
          defaultMessage: "Local code is required",
        }),
      );
      return;
    }
    if (!testId.trim()) {
      setError(
        intl.formatMessage({
          id: "billing.config.tariff.error.testIdRequired",
          defaultMessage: "Test ID is required",
        }),
      );
      return;
    }
    const priceValue = Number(price);
    if (!price || Number.isNaN(priceValue)) {
      setError(
        intl.formatMessage({
          id: "billing.config.tariff.error.priceRequired",
          defaultMessage: "A valid price is required",
        }),
      );
      return;
    }
    setCreating(true);
    createTariff(
      {
        localCode: localCode.trim(),
        testId: testId.trim(),
        price: priceValue,
        payerType: tariffPayerType,
        effectiveFrom,
        effectiveTo,
      },
      (response) => {
        setCreating(false);
        if (response) {
          setLocalCode("");
          setTestId("");
          setPrice("");
          setTariffPayerType("CASH");
          setEffectiveFrom("");
          setEffectiveTo("");
          loadTariffs();
        } else {
          setError(
            intl.formatMessage({
              id: "billing.config.tariff.error.createFailed",
              defaultMessage: "Failed to create tariff",
            }),
          );
        }
      },
    );
  };

  const formatPrice = (value: number | null | undefined): string => {
    if (value === null || value === undefined) {
      return "—";
    }
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "GHS",
    }).format(value);
  };

  return (
    <section className="tariffPanel" data-testid="tariff-panel">
      <h3 className="tariffPanel__heading">
        {intl.formatMessage({
          id: "billing.config.tariff.title",
          defaultMessage: "Tariffs",
        })}
      </h3>
      {error && (
        <div className="tariffPanel__error" role="alert">{error}</div>
      )}
      <div className="tariffPanel__lookup">
        <h4 className="tariffPanel__lookupTitle">
          {intl.formatMessage({
            id: "billing.config.tariff.lookup.title",
            defaultMessage: "Price Lookup",
          })}
        </h4>
        <TextInput
          id="tariff-price-input"
          labelText={intl.formatMessage({
            id: "billing.config.tariff.label.localCode",
            defaultMessage: "Local Code",
          })}
          placeholder={intl.formatMessage({
            id: "billing.config.tariff.placeholder.localCode",
            defaultMessage: "e.g. MAL-001",
          })}
          value={priceLocalCode}
          onChange={(event) => setPriceLocalCode(event.target.value)}
          data-testid="tariff-price-input"
        />
        <label className="tariffPanel__label">
          {intl.formatMessage({
            id: "billing.config.tariff.label.payerType",
            defaultMessage: "Payer Type",
          })}
        </label>
        <select
          className="tariffPanel__select"
          value={pricePayerType}
          onChange={(event) => setPricePayerType(event.target.value as PayerType)}
        >
          {PAYER_TYPES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <div className="tariffPanel__lookupActions">
          <Button
            kind="primary"
            size="sm"
            onClick={handleLookupPrice}
            disabled={lookupLoading || !priceLocalCode.trim()}
          >
            {intl.formatMessage({
              id: "billing.config.tariff.lookup.button",
              defaultMessage: "Lookup Price",
            })}
          </Button>
          {resolvedPrice !== null && (
            <span
              className="tariffPanel__resolvedPrice"
              data-testid="tariff-resolved-price"
            >
              {intl.formatMessage({
                id: "billing.config.tariff.resolvedPrice",
                defaultMessage: "Resolved price: {price}",
              }, { price: formatPrice(resolvedPrice) })}
            </span>
          )}
        </div>
      </div>
      <table className="tariffPanel__table" aria-label={intl.formatMessage({ id: "billing.config.tariff.table.label", defaultMessage: "Tariffs" })}>
        <thead>
          <tr>
            <th>{intl.formatMessage({ id: "billing.config.tariff.column.localCode", defaultMessage: "Local Code" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.tariff.column.testId", defaultMessage: "Test ID" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.tariff.column.price", defaultMessage: "Price" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.tariff.column.payerType", defaultMessage: "Payer Type" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.tariff.column.effectiveRange", defaultMessage: "Effective Range" })}</th>
          </tr>
        </thead>
        <tbody>
          {tariffs.length === 0 ? (
            <tr>
              <td colSpan={5} className="tariffPanel__empty">
                {intl.formatMessage({
                  id: "billing.config.tariff.empty",
                  defaultMessage: "No tariffs found",
                })}
              </td>
            </tr>
          ) : (
            tariffs.map((tariff) => (
              <tr key={tariff.id} className="tariffPanel__row">
                <td>{tariff.localCode || "—"}</td>
                <td>{tariff.testId || "—"}</td>
                <td>{formatPrice(tariff.price)}</td>
                <td>{tariff.payerType || "—"}</td>
                <td>
                  {tariff.effectiveFrom || "—"} {intl.formatMessage({ id: "billing.config.tariff.to", defaultMessage: "to" })} {tariff.effectiveTo || "—"}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      <div className="tariffPanel__form">
        <h4 className="tariffPanel__formTitle">
          {intl.formatMessage({
            id: "billing.config.tariff.create.title",
            defaultMessage: "Create Tariff",
          })}
        </h4>
        <div className="tariffPanel__fieldRow">
          <TextInput
            id="tariff-local-code"
            labelText={intl.formatMessage({
              id: "billing.config.tariff.label.localCode",
              defaultMessage: "Local Code",
            })}
            placeholder={intl.formatMessage({
              id: "billing.config.tariff.placeholder.localCode",
              defaultMessage: "e.g. MAL-001",
            })}
            value={localCode}
            onChange={(event) => setLocalCode(event.target.value)}
          />
          <TextInput
            id="tariff-test-id"
            labelText={intl.formatMessage({
              id: "billing.config.tariff.label.testId",
              defaultMessage: "Test ID",
            })}
            placeholder={intl.formatMessage({
              id: "billing.config.tariff.placeholder.testId",
              defaultMessage: "e.g. TEST-123",
            })}
            value={testId}
            onChange={(event) => setTestId(event.target.value)}
          />
        </div>
        <div className="tariffPanel__fieldRow">
          <TextInput
            id="tariff-price-field"
            type="number"
            labelText={intl.formatMessage({
              id: "billing.config.tariff.label.price",
              defaultMessage: "Price",
            })}
            placeholder="0.00"
            value={price}
            onChange={(event) => setPrice(event.target.value)}
          />
          <select
            className="tariffPanel__select"
            value={tariffPayerType}
            onChange={(event) =>
              setTariffPayerType(event.target.value as PayerType)
            }
          >
            {PAYER_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>
        <div className="tariffPanel__fieldRow">
          <TextInput
            id="tariff-effective-from"
            type="date"
            labelText={intl.formatMessage({
              id: "billing.config.tariff.label.effectiveFrom",
              defaultMessage: "Effective From",
            })}
            value={effectiveFrom}
            onChange={(event) => setEffectiveFrom(event.target.value)}
          />
          <TextInput
            id="tariff-effective-to"
            type="date"
            labelText={intl.formatMessage({
              id: "billing.config.tariff.label.effectiveTo",
              defaultMessage: "Effective To",
            })}
            value={effectiveTo}
            onChange={(event) => setEffectiveTo(event.target.value)}
          />
        </div>
        <div className="tariffPanel__actions">
          <Button
            kind="primary"
            size="md"
            onClick={handleSubmit}
            disabled={creating || !localCode.trim() || !testId.trim() || !price}
            data-testid="tariff-submit"
          >
            {intl.formatMessage({
              id: "billing.config.tariff.submit",
              defaultMessage: "Create Tariff",
            })}
          </Button>
        </div>
      </div>
    </section>
  );
};

export default TariffPanel;
