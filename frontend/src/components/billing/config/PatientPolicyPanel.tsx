import React, { useCallback, useEffect, useState } from "react";
import { Button, Tag, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import {
  createPatientPolicy,
  deactivatePatientPolicy,
  getPatientPolicies,
  getPayers,
  type BillingPayer,
  type PatientPolicy,
} from "../../../services/billingConfigService";
import "./PatientPolicyPanel.css";

const PatientPolicyPanel = () => {
  const intl = useIntl();
  const [patientId, setPatientId] = useState<string>("");
  const [policies, setPolicies] = useState<PatientPolicy[]>([]);
  const [payers, setPayers] = useState<BillingPayer[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [policiesLoading, setPoliciesLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [payerId, setPayerId] = useState<number | null>(null);
  const [memberNumber, setMemberNumber] = useState<string>("");
  const [motherMemberNumber, setMotherMemberNumber] = useState<string>("");
  const [effectiveFrom, setEffectiveFrom] = useState<string>("");
  const [effectiveTo, setEffectiveTo] = useState<string>("");
  const [creating, setCreating] = useState<boolean>(false);

  const loadPayers = useCallback(() => {
    getPayers((response) => {
      setPayers(response ?? []);
    });
  }, []);

  useEffect(() => {
    loadPayers();
  }, [loadPayers]);

  const handleSearch = () => {
    if (!patientId.trim()) {
      setError(
        intl.formatMessage({
          id: "billing.config.policy.error.patientIdRequired",
          defaultMessage: "Patient ID is required",
        }),
      );
      return;
    }
    setError(null);
    setPoliciesLoading(true);
    getPatientPolicies(
      patientId.trim(),
      (response) => {
        setPolicies(response ?? []);
        setPoliciesLoading(false);
      },
    );
  };

  const handleSubmit = () => {
    setError(null);
    if (!patientId.trim()) {
      setError(
        intl.formatMessage({
          id: "billing.config.policy.error.patientIdRequired",
          defaultMessage: "Patient ID is required",
        }),
      );
      return;
    }
    if (!payerId) {
      setError(
        intl.formatMessage({
          id: "billing.config.policy.error.payerRequired",
          defaultMessage: "Payer is required",
        }),
      );
      return;
    }
    setCreating(true);
    createPatientPolicy(
      {
        patientId: patientId.trim(),
        payerId,
        memberNumber,
        motherMemberNumber,
        effectiveFrom,
        effectiveTo,
      },
      (response) => {
        setCreating(false);
        if (response) {
          setPayerId(null);
          setMemberNumber("");
          setMotherMemberNumber("");
          setEffectiveFrom("");
          setEffectiveTo("");
          handleSearch();
        } else {
          setError(
            intl.formatMessage({
              id: "billing.config.policy.error.createFailed",
              defaultMessage: "Failed to link policy",
            }),
          );
        }
      },
    );
  };

  const handleDeactivate = (id: number | string) => {
    deactivatePatientPolicy(id, (response) => {
      if (response) {
        handleSearch();
      }
    });
  };

  const isActive = (policy: PatientPolicy): boolean => {
    const value = policy.active;
    return value === "Y" || value === "true" || value === "1";
  };

  const payerName = (id: number | null): string => {
    if (!id) return "—";
    return payers.find((p) => p.id === id)?.name || `Payer ${id}`;
  };

  return (
    <section className="policyPanel" data-testid="policy-panel">
      <h3 className="policyPanel__heading">
        {intl.formatMessage({
          id: "billing.config.policy.title",
          defaultMessage: "Patient Policies",
        })}
      </h3>
      {error && (
        <div className="policyPanel__error" role="alert">{error}</div>
      )}
      <div className="policyPanel__search">
        <TextInput
          id="policy-patient-input"
          labelText={intl.formatMessage({
            id: "billing.config.policy.label.patientId",
            defaultMessage: "Patient ID",
          })}
          placeholder={intl.formatMessage({
            id: "billing.config.policy.placeholder.patientId",
            defaultMessage: "Enter patient ID",
          })}
          value={patientId}
          onChange={(event) => setPatientId(event.target.value)}
          data-testid="policy-patient-input"
        />
        <Button
          kind="primary"
          size="sm"
          onClick={handleSearch}
          disabled={policiesLoading}
        >
          {intl.formatMessage({
            id: "billing.config.policy.search",
            defaultMessage: "Search",
          })}
        </Button>
      </div>
      <table className="policyPanel__table" aria-label={intl.formatMessage({ id: "billing.config.policy.table.label", defaultMessage: "Patient Policies" })}>
        <thead>
          <tr>
            <th>{intl.formatMessage({ id: "billing.config.policy.column.payer", defaultMessage: "Payer" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.policy.column.member", defaultMessage: "Member #" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.policy.column.motherMember", defaultMessage: "Mother Member #" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.policy.column.effectiveFrom", defaultMessage: "Effective From" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.policy.column.effectiveTo", defaultMessage: "Effective To" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.policy.column.active", defaultMessage: "Active" })}</th>
            <th>{intl.formatMessage({ id: "billing.config.policy.column.actions", defaultMessage: "Actions" })}</th>
          </tr>
        </thead>
        <tbody>
          {policies.length === 0 && policiesLoading === false ? (
            <tr>
              <td colSpan={7} className="policyPanel__empty">
                {intl.formatMessage({
                  id: "billing.config.policy.empty",
                  defaultMessage: "No policies found for this patient",
                })}
              </td>
            </tr>
          ) : (
            policies.map((policy) => (
              <tr key={policy.id} className="policyPanel__row">
                <td>{payerName(policy.payerId ?? null)}</td>
                <td>{policy.memberNumber || "—"}</td>
                <td>{policy.motherMemberNumber || "—"}</td>
                <td>{policy.effectiveFrom || "—"}</td>
                <td>{policy.effectiveTo || "—"}</td>
                <td>
                  <Tag
                    type={isActive(policy) ? "green" : "gray"}
                    size="sm"
                  >
                    {isActive(policy)
                      ? intl.formatMessage({
                          id: "billing.config.policy.active",
                          defaultMessage: "Yes",
                        })
                      : intl.formatMessage({
                          id: "billing.config.policy.inactive",
                          defaultMessage: "No",
                        })}
                  </Tag>
                </td>
                <td>
                  <Button
                    kind="tertiary"
                    size="sm"
                    onClick={() => handleDeactivate(policy.id ?? "")}
                    disabled={policiesLoading || creating || !policy.id}
                  >
                    {intl.formatMessage({
                      id: "billing.config.policy.deactivate",
                      defaultMessage: "Deactivate",
                    })}
                  </Button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      <div className="policyPanel__form">
        <h4 className="policyPanel__formTitle">
          {intl.formatMessage({
            id: "billing.config.policy.create.title",
            defaultMessage: "Link Policy",
          })}
        </h4>
        <label className="policyPanel__label">
          {intl.formatMessage({
            id: "billing.config.policy.label.payer",
            defaultMessage: "Payer",
          })}
        </label>
        <select
          className="policyPanel__select"
          value={String(payerId ?? "")}
          onChange={(event) =>
            setPayerId(event.target.value ? Number(event.target.value) : null)
          }
        >
          <option value="">
            {intl.formatMessage({
              id: "billing.config.policy.option.selectPayer",
              defaultMessage: "Select payer",
            })}
          </option>
          {payers.map((payer) => (
            <option key={payer.id} value={String(payer.id ?? "")}>
              {payer.name || "—"}
            </option>
          ))}
        </select>
        <div className="policyPanel__fieldRow">
          <TextInput
            id="policy-member-number"
            labelText={intl.formatMessage({
              id: "billing.config.policy.label.memberNumber",
              defaultMessage: "Member Number",
            })}
            value={memberNumber}
            onChange={(event) => setMemberNumber(event.target.value)}
          />
          <TextInput
            id="policy-mother-member-number"
            labelText={intl.formatMessage({
              id: "billing.config.policy.label.motherMemberNumber",
              defaultMessage: "Mother Member Number",
            })}
            value={motherMemberNumber}
            onChange={(event) => setMotherMemberNumber(event.target.value)}
          />
        </div>
        <div className="policyPanel__fieldRow">
          <TextInput
            id="policy-effective-from"
            type="date"
            labelText={intl.formatMessage({
              id: "billing.config.policy.label.effectiveFrom",
              defaultMessage: "Effective From",
            })}
            value={effectiveFrom}
            onChange={(event) => setEffectiveFrom(event.target.value)}
          />
          <TextInput
            id="policy-effective-to"
            type="date"
            labelText={intl.formatMessage({
              id: "billing.config.policy.label.effectiveTo",
              defaultMessage: "Effective To",
            })}
            value={effectiveTo}
            onChange={(event) => setEffectiveTo(event.target.value)}
          />
        </div>
        <div className="policyPanel__actions">
          <Button
            kind="primary"
            size="md"
            onClick={handleSubmit}
            disabled={creating || !patientId.trim() || !payerId}
            data-testid="policy-submit"
          >
            {intl.formatMessage({
              id: "billing.config.policy.submit",
              defaultMessage: "Link Policy",
            })}
          </Button>
        </div>
      </div>
    </section>
  );
};

export default PatientPolicyPanel;
