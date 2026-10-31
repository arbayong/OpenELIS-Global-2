import React, { useCallback, useState } from "react";
import {
  Button,
  InlineNotification,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  recordPayment,
  type GatewayDriver,
  type PaymentMethod,
} from "../../../services/billingInvoiceService";
import "./PaymentForm.css";

const METHODS: { value: PaymentMethod; label: string }[] = [
  { value: "CASH", label: "Cash" },
  { value: "CARD", label: "Card" },
  { value: "BANK_TRANSFER", label: "Bank transfer" },
  { value: "MOBILE_MONEY", label: "Mobile money" },
];

const GATEWAYS: { value: GatewayDriver; label: string }[] = [
  { value: "PAYSTACK", label: "Paystack" },
  { value: "HUBTEL", label: "Hubtel" },
  { value: "MTN_MOMO", label: "MTN Mobile Money" },
];

interface PaymentFormProps {
  open: boolean;
  invoiceId: string | number;
  onClose: () => void;
  onSuccess?: () => void;
  recordedBy?: string;
}

const PaymentForm = ({
  open,
  invoiceId,
  onClose,
  onSuccess,
  recordedBy = "operator",
}: PaymentFormProps) => {
  const intl = useIntl();
  const [amount, setAmount] = useState<string>("");
  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [gateway, setGateway] = useState<GatewayDriver | "">("");
  const [providerTransactionId, setProviderTransactionId] = useState<string>(
    "",
  );
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const showGateway = method !== "CASH";

  const handleSubmit = () => {
    setError(null);
    const parsed = Number(amount);
    if (!method || !Number.isFinite(parsed) || parsed <= 0) {
      setError(
        intl.formatMessage({
          id: "billing.payment.invalid",
          defaultMessage: "Enter a valid amount and payment method",
        }),
      );
      return;
    }
    setSubmitting(true);
    recordPayment(
      {
        invoiceId,
        amount: parsed,
        method,
        gateway: showGateway ? (gateway || undefined) : undefined,
        providerTransactionId: providerTransactionId || undefined,
        recordedBy,
      },
      (response) => {
        setSubmitting(false);
        if (response?.id) {
          onClose();
          onSuccess?.();
        } else {
          setError(
            intl.formatMessage({
              id: "billing.payment.record.failed",
              defaultMessage: "Recording payment failed",
            }),
          );
        }
      },
    );
  };

  return (
    <div className="paymentForm">
      <h2 className="paymentForm__heading">
        {intl.formatMessage({
          id: "billing.payment.title",
          defaultMessage: "Record payment",
        })}
      </h2>
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}
      <div className="paymentForm__fieldRow">
        <div className="paymentForm__field">
          <TextInput
            id="payment-amount"
            labelText={intl.formatMessage({
              id: "billing.payment.amount.label",
              defaultMessage: "Amount",
            })}
            placeholder={intl.formatMessage({
              id: "billing.payment.amount.placeholder",
              defaultMessage: "0.00",
            })}
            type="number"
            value={amount}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
              setAmount(event.target.value)
            }
            data-testid="payment-amount-input"
          />
        </div>
        <div className="paymentForm__field">
          <label style={{ display: "block", marginBottom: "0.25rem" }}>
            {intl.formatMessage({
              id: "billing.payment.method.label",
              defaultMessage: "Method",
            })}
          </label>
          <Select
            data-testid="payment-method-dropdown"
            id="payment-method"
            value={method}
            onChange={(event: React.ChangeEvent<HTMLSelectElement>) =>
              setMethod(event.target.value as PaymentMethod)
            }
          >
            {METHODS.map((option) => (
              <SelectItem
                key={option.value}
                value={option.value}
                text={option.label}
              />
            ))}
          </Select>
        </div>
      </div>
      {showGateway && (
        <div className="paymentForm__fieldRow">
          <div className="paymentForm__field">
            <label style={{ display: "block", marginBottom: "0.25rem" }}>
              {intl.formatMessage({
                id: "billing.payment.gateway.label",
                defaultMessage: "Gateway / driver",
              })}
            </label>
            <Select
            id="payment-gateway"
            value={gateway || ""}
            onChange={(event: React.ChangeEvent<HTMLSelectElement>) =>
              setGateway(event.target.value as GatewayDriver)
            }
            >
              <SelectItem value="" text={intl.formatMessage({
                id: "billing.payment.gateway.none",
                defaultMessage: "None",
              })} />
              {GATEWAYS.map((option) => (
                <SelectItem
                  key={option.value}
                  value={option.value}
                  text={option.label}
                />
              ))}
            </Select>
          </div>
        </div>
      )}
      <div className="paymentForm__fieldRow">
        <div className="paymentForm__field">
          <TextInput
            id="payment-provider-transaction-id"
            labelText={intl.formatMessage({
              id: "billing.payment.providerTransaction.label",
              defaultMessage: "Provider transaction id",
            })}
            placeholder={intl.formatMessage({
              id: "billing.payment.providerTransaction.placeholder",
              defaultMessage: "Optional",
            })}
            value={providerTransactionId}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
              setProviderTransactionId(event.target.value)
            }
          />
        </div>
      </div>
      <div className="paymentForm__submitBar">
        <Button
          kind="primary"
          data-testid="payment-submit"
          onClick={handleSubmit}
          disabled={submitting}
        >
          {intl.formatMessage({
            id: "billing.payment.submit",
            defaultMessage: "Record payment",
          })}
        </Button>
        <Button kind="tertiary" onClick={onClose}>
          {intl.formatMessage({
            id: "label.button.cancel",
            defaultMessage: "Cancel",
          })}
        </Button>
      </div>
    </div>
  );
};

export default PaymentForm;
