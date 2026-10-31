import React, { useCallback, useState } from "react";
import {
  Button,
  Column,
  Grid,
  InlineNotification,
  Select,
  SelectItem,
  TextInput,
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  csvCell,
  downloadBlob,
  toLocalIsoDate,
} from "../../../components/utils/Utils";
import {
  getDailySummary,
  getMonthlySummary,
  getOutstanding,
  getRevenueByTest,
  recomputeDailySummary,
  type BillingInvoice,
  type DailyRevenueSummary,
  type RevenueByTestRow,
} from "../../../services/billingReportService";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import "./RevenueReports.css";

const TODAY = toLocalIsoDate(new Date());
const CURRENT_YEAR = new Date().getFullYear();
const CURRENT_MONTH = new Date().getMonth() + 1;
const YEARS = [CURRENT_YEAR - 2, CURRENT_YEAR - 1, CURRENT_YEAR, CURRENT_YEAR + 1, CURRENT_YEAR + 2];
const MONTHS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

const money = (value: number | string | null | undefined): string =>
  value == null || value === "" ? "—" : String(value);

const balanceOf = (invoice: BillingInvoice): string =>
  money(Number(invoice.total ?? 0) - Number(invoice.amountPaid ?? 0));

const RevenueReports = () => {
  const intl = useIntl();

  const [orgId, setOrgId] = useState<string>("");
  const [dailyDate, setDailyDate] = useState<string>(TODAY);
  const [year, setYear] = useState<number>(CURRENT_YEAR);
  const [month, setMonth] = useState<number>(CURRENT_MONTH);
  const [fromDate, setFromDate] = useState<string>("");
  const [toDate, setToDate] = useState<string>("");

  const [dailySummary, setDailySummary] = useState<DailyRevenueSummary | null>(
    null,
  );
  const [dailyLoading, setDailyLoading] = useState<boolean>(false);
  const [dailyMessage, setDailyMessage] = useState<string | null>(null);

  const [monthly, setMonthly] = useState<DailyRevenueSummary[]>([]);
  const [monthlyLoading, setMonthlyLoading] = useState<boolean>(false);
  const [monthlyMessage, setMonthlyMessage] = useState<string | null>(null);

  const [byTest, setByTest] = useState<RevenueByTestRow[]>([]);
  const [byTestLoading, setByTestLoading] = useState<boolean>(false);
  const [byTestMessage, setByTestMessage] = useState<string | null>(null);

  const [outstanding, setOutstanding] = useState<BillingInvoice[]>([]);
  const [outstandingLoading, setOutstandingLoading] = useState<boolean>(false);
  const [outstandingMessage, setOutstandingMessage] = useState<string | null>(null);

  const parseOrg = useCallback((): number | null => {
    const parsed = Number(orgId);
    return Number.isFinite(parsed) ? parsed : null;
  }, [orgId]);

  const handleLoadDaily = useCallback(() => {
    const org = parseOrg();
    if (!org || !dailyDate) {
      setDailyMessage("Enter an organization id and a date.");
      return;
    }
    setDailyLoading(true);
    setDailySummary(null);
    setDailyMessage(null);
    getDailySummary(org, dailyDate, (summary) => {
      setDailyLoading(false);
      if (summary) {
        setDailySummary(summary);
      } else {
        setDailyMessage("No daily summary found for this date.");
      }
    });
  }, [dailyDate, parseOrg]);

  const handleRecomputeDaily = useCallback(() => {
    const org = parseOrg();
    if (!org || !dailyDate) {
      setDailyMessage("Enter an organization id and a date.");
      return;
    }
    setDailyLoading(true);
    setDailyMessage(null);
    recomputeDailySummary(org, dailyDate, (summary) => {
      setDailyLoading(false);
      if (summary) {
        setDailySummary(summary);
        setDailyMessage("Daily summary recomputed.");
      } else {
        setDailyMessage("Recompute failed.");
      }
    });
  }, [dailyDate, parseOrg]);

  const handleLoadMonthly = useCallback(() => {
    const org = parseOrg();
    if (!org) {
      setMonthlyMessage("Enter an organization id.");
      return;
    }
    setMonthlyLoading(true);
    setMonthly([]);
    setMonthlyMessage(null);
    getMonthlySummary(org, year, month, (list) => {
      setMonthlyLoading(false);
      setMonthly(list || []);
      if (!(list || []).length) {
        setMonthlyMessage("No monthly summaries found.");
      }
    });
  }, [orgId, year, month, parseOrg]);

  const handleLoadByTest = useCallback(() => {
    const org = parseOrg();
    if (!org || !fromDate || !toDate) {
      setByTestMessage("Enter an organization id and a date range.");
      return;
    }
    setByTestLoading(true);
    setByTest([]);
    setByTestMessage(null);
    getRevenueByTest(org, fromDate, toDate, (list) => {
      setByTestLoading(false);
      setByTest(list || []);
      if (!(list || []).length) {
        setByTestMessage("No revenue by test found for this range.");
      }
    });
  }, [fromDate, toDate, parseOrg]);

  const handleLoadOutstanding = useCallback(() => {
    const org = parseOrg();
    if (!org) {
      setOutstandingMessage("Enter an organization id.");
      return;
    }
    setOutstandingLoading(true);
    setOutstanding([]);
    setOutstandingMessage(null);
    getOutstanding(org, (list) => {
      setOutstandingLoading(false);
      setOutstanding(list || []);
      if (!(list || []).length) {
        setOutstandingMessage("No outstanding invoices.");
      }
    });
  }, [parseOrg]);

  const handleExportByTest = useCallback(() => {
    const headers = [
      intl.formatMessage({ id: "reports.byTest.testId", defaultMessage: "Test ID" }),
      intl.formatMessage({ id: "reports.byTest.testName", defaultMessage: "Test Name" }),
      intl.formatMessage({ id: "reports.byTest.localCode", defaultMessage: "Local Code" }),
      intl.formatMessage({ id: "reports.byTest.revenue", defaultMessage: "Revenue" }),
    ];
    const rows = byTest.map((row) => [
      row.testId,
      row.testName,
      row.localCode,
      row.revenue,
    ]);
    const csv = [headers, ...rows]
      .map((r) => r.map((c) => csvCell(c)).join(","))
      .join("\n");
    downloadBlob(
      new Blob([csv], { type: "text/csv;charset=utf-8;" }),
      "revenue-by-test.csv",
    );
  }, [byTest, intl]);

  const handleExportOutstanding = useCallback(() => {
    const headers = [
      intl.formatMessage({ id: "reports.outstanding.invoice", defaultMessage: "Invoice" }),
      intl.formatMessage({ id: "reports.outstanding.patient", defaultMessage: "Patient" }),
      intl.formatMessage({ id: "reports.outstanding.status", defaultMessage: "Status" }),
      intl.formatMessage({ id: "reports.outstanding.payer", defaultMessage: "Payer" }),
      intl.formatMessage({ id: "reports.outstanding.currency", defaultMessage: "Currency" }),
      intl.formatMessage({ id: "reports.outstanding.issued", defaultMessage: "Issued" }),
      intl.formatMessage({ id: "reports.outstanding.total", defaultMessage: "Total" }),
      intl.formatMessage({ id: "reports.outstanding.paid", defaultMessage: "Paid" }),
      intl.formatMessage({ id: "reports.outstanding.balance", defaultMessage: "Balance" }),
    ];
    const rows = outstanding.map((invoice) => [
      invoice.invoiceNumber,
      invoice.patientId,
      invoice.status,
      invoice.payerType,
      invoice.currency,
      invoice.issuedAt,
      invoice.total,
      invoice.amountPaid,
      balanceOf(invoice),
    ]);
    const csv = [headers, ...rows]
      .map((r) => r.map((c) => csvCell(c)).join(","))
      .join("\n");
    downloadBlob(
      new Blob([csv], { type: "text/csv;charset=utf-8;" }),
      "outstanding-invoices.csv",
    );
  }, [outstanding, intl]);

  const dailyTiles = [
    {
      id: "gross",
      label: intl.formatMessage({ id: "reports.daily.gross", defaultMessage: "Gross revenue" }),
      value: money(dailySummary?.grossRevenue),
      testId: "daily-tile-gross",
    },
    {
      id: "cash",
      label: intl.formatMessage({ id: "reports.daily.cash", defaultMessage: "Cash" }),
      value: money(dailySummary?.cashRevenue),
      testId: "daily-tile-cash",
    },
    {
      id: "mobileMoney",
      label: intl.formatMessage({ id: "reports.daily.mobileMoney", defaultMessage: "Mobile money" }),
      value: money(dailySummary?.mobileMoneyRevenue),
      testId: "daily-tile-mobile-money",
    },
    {
      id: "gateway",
      label: intl.formatMessage({ id: "reports.daily.gateway", defaultMessage: "Gateway" }),
      value: money(dailySummary?.gatewayRevenue),
      testId: "daily-tile-gateway",
    },
    {
      id: "insurance",
      label: intl.formatMessage({ id: "reports.daily.insurance", defaultMessage: "Insurance" }),
      value: money(dailySummary?.insuranceRevenue),
      testId: "daily-tile-insurance",
    },
    {
      id: "nhis",
      label: intl.formatMessage({ id: "reports.daily.nhis", defaultMessage: "NHIS" }),
      value: money(dailySummary?.nhisRevenue),
      testId: "daily-tile-nhis",
    },
    {
      id: "private",
      label: intl.formatMessage({ id: "reports.daily.private", defaultMessage: "Private" }),
      value: money(dailySummary?.privateRevenue),
      testId: "daily-tile-private",
    },
    {
      id: "refunds",
      label: intl.formatMessage({ id: "reports.daily.refunds", defaultMessage: "Refunds" }),
      value: money(dailySummary?.refunds),
      testId: "daily-tile-refunds",
    },
    {
      id: "invoiceCount",
      label: intl.formatMessage({ id: "reports.daily.invoiceCount", defaultMessage: "Invoices" }),
      value: money(dailySummary?.invoiceCount),
      testId: "daily-tile-invoice-count",
    },
    {
      id: "patientCount",
      label: intl.formatMessage({ id: "reports.daily.patientCount", defaultMessage: "Patients" }),
      value: money(dailySummary?.patientCount),
      testId: "daily-tile-patient-count",
    },
  ];

  return (
    <div className="revenueReports">
      <PageBreadCrumb
        breadcrumbs={[
          {
            label: "reports.title",
            isCurrentPage: true,
          },
        ]}
      />
      <h1 className="revenueReports__heading">
        {intl.formatMessage({ id: "reports.title", defaultMessage: "Revenue Reports" })}
      </h1>

      <Grid>
        <Column sm={4} md={8} lg={16}>
          <TextInput
            id="report-org"
            labelText={
              intl.formatMessage({ id: "reports.orgId", defaultMessage: "Organization id" })
            }
            placeholder="1"
            type="number"
            value={orgId}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
              setOrgId(event.target.value)
            }
            data-testid="report-org-input"
          />
        </Column>
      </Grid>

      {/* Daily summary */}
      <section className="revenueReports__section">
        <h2 className="revenueReports__sectionTitle">
          {intl.formatMessage({ id: "reports.daily.title", defaultMessage: "Daily summary" })}
        </h2>
        <div className="revenueReports__controls">
          <div className="revenueReports__control">
            <label className="revenueReports__label">
              {intl.formatMessage({ id: "reports.daily.date", defaultMessage: "Date" })}
            </label>
            <TextInput
              id="report-date"
              type="date"
              labelText={intl.formatMessage({
                id: "reports.daily.date",
                defaultMessage: "Date",
              })}
              hideLabel
              value={dailyDate}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                setDailyDate(event.target.value)
              }
              data-testid="report-date"
            />
          </div>
          <div className="revenueReports__control">
            <Button
              kind="primary"
              size="md"
              onClick={handleLoadDaily}
              disabled={dailyLoading}
              data-testid="report-load-daily"
            >
              {intl.formatMessage({ id: "reports.daily.load", defaultMessage: "Load daily" })}
            </Button>
          </div>
          <div className="revenueReports__control">
            <Button
              kind="primary"
              size="md"
              onClick={handleRecomputeDaily}
              disabled={dailyLoading}
              data-testid="recompute-daily"
            >
              {intl.formatMessage({
                id: "reports.daily.recompute",
                defaultMessage: "Recompute daily summary",
              })}
            </Button>
          </div>
        </div>
        {dailyLoading && (
          <p>{intl.formatMessage({ id: "reports.loading", defaultMessage: "Loading..." })}</p>
        )}
        {dailyMessage && (
          <InlineNotification
            kind="error"
            title={dailyMessage}
            lowContrast
            hideCloseButton
          />
        )}
        {dailySummary && (
          <div data-testid="daily-summary-tiles" className="revenueReports__tiles">
            {dailyTiles.map((tile) => (
              <Tile className="revenueReports__tile" key={tile.testId}>
                <h3 className="revenueReports__tileTitle">{tile.label}</h3>
                <p className="revenueReports__tileValue" data-testid={tile.testId}>
                  {tile.value}
                </p>
              </Tile>
            ))}
          </div>
        )}
      </section>

      {/* Monthly rollup */}
      <section className="revenueReports__section">
        <h2 className="revenueReports__sectionTitle">
          {intl.formatMessage({ id: "reports.monthly.title", defaultMessage: "Monthly rollup" })}
        </h2>
        <div className="revenueReports__controls">
          <div className="revenueReports__control">
            <label className="revenueReports__label">
              {intl.formatMessage({ id: "reports.monthly.year", defaultMessage: "Year" })}
            </label>
            <Select
              id="report-year"
              value={String(year)}
              onChange={({ selectedItem }: any) =>
                setYear(Number(selectedItem?.value ?? year))
              }
              data-testid="report-year"
            >
              {YEARS.map((y) => (
                <SelectItem key={y} value={String(y)} text={String(y)}>
                  {y}
                </SelectItem>
              ))}
            </Select>
          </div>
          <div className="revenueReports__control">
            <label className="revenueReports__label">
              {intl.formatMessage({ id: "reports.monthly.month", defaultMessage: "Month" })}
            </label>
            <Select
              id="report-month"
              value={String(month)}
              onChange={({ selectedItem }: any) =>
                setMonth(Number(selectedItem?.value ?? month))
              }
              data-testid="report-month"
            >
              {MONTHS.map((m) => (
                <SelectItem key={m} value={String(m)} text={String(m)}>
                  {m}
                </SelectItem>
              ))}
            </Select>
          </div>
          <div className="revenueReports__control">
            <Button
              kind="primary"
              size="md"
              onClick={handleLoadMonthly}
              disabled={monthlyLoading}
              data-testid="report-load-monthly"
            >
              {intl.formatMessage({ id: "reports.monthly.load", defaultMessage: "Load monthly" })}
            </Button>
          </div>
        </div>
        {monthlyLoading && (
          <p>{intl.formatMessage({ id: "reports.loading", defaultMessage: "Loading..." })}</p>
        )}
        {monthlyMessage && (
          <InlineNotification
            kind="error"
            title={monthlyMessage}
            lowContrast
            hideCloseButton
          />
        )}
        <table data-testid="monthly-table" className="revenueReports__table">
          <thead>
            <tr>
              <th>
                {intl.formatMessage({ id: "reports.monthly.date", defaultMessage: "Date" })}
              </th>
              <th>
                {intl.formatMessage({ id: "reports.monthly.gross", defaultMessage: "Gross" })}
              </th>
              <th>
                {intl.formatMessage({ id: "reports.monthly.invoices", defaultMessage: "Invoices" })}
              </th>
              <th>
                {intl.formatMessage({ id: "reports.monthly.patients", defaultMessage: "Patients" })}
              </th>
            </tr>
          </thead>
          <tbody>
            {monthly.map((summary) => (
              <tr key={summary.id ?? summary.businessDate}>
                <td>{summary.businessDate || "—"}</td>
                <td className="numeric">{money(summary.grossRevenue)}</td>
                <td className="numeric">{money(summary.invoiceCount)}</td>
                <td className="numeric">{money(summary.patientCount)}</td>
              </tr>
            ))}
            {!monthlyLoading && monthly.length === 0 && (
              <tr>
                <td colSpan={4}>
                  {intl.formatMessage({
                    id: "reports.monthly.empty",
                    defaultMessage: "No data",
                  })}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      {/* Revenue by test */}
      <section className="revenueReports__section">
        <h2 className="revenueReports__sectionTitle">
          {intl.formatMessage({ id: "reports.byTest.title", defaultMessage: "Revenue by test" })}
        </h2>
        <div className="revenueReports__controls">
          <div className="revenueReports__control">
            <label className="revenueReports__label">
              {intl.formatMessage({ id: "reports.byTest.from", defaultMessage: "From" })}
            </label>
            <TextInput
              id="report-from"
              type="date"
              labelText={intl.formatMessage({
                id: "reports.byTest.from",
                defaultMessage: "From",
              })}
              hideLabel
              value={fromDate}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                setFromDate(event.target.value)
              }
              data-testid="report-from"
            />
          </div>
          <div className="revenueReports__control">
            <label className="revenueReports__label">
              {intl.formatMessage({ id: "reports.byTest.to", defaultMessage: "To" })}
            </label>
            <TextInput
              id="report-to"
              type="date"
              labelText={intl.formatMessage({
                id: "reports.byTest.to",
                defaultMessage: "To",
              })}
              hideLabel
              value={toDate}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                setToDate(event.target.value)
              }
              data-testid="report-to"
            />
          </div>
          <div className="revenueReports__control">
            <Button
              kind="primary"
              size="md"
              onClick={handleLoadByTest}
              disabled={byTestLoading}
              data-testid="report-load-by-test"
            >
              {intl.formatMessage({ id: "reports.byTest.load", defaultMessage: "Load by test" })}
            </Button>
          </div>
          <div className="revenueReports__control revenueReports__export">
            <Button
              kind="tertiary"
              size="md"
              onClick={handleExportByTest}
              disabled={byTestLoading || byTest.length === 0}
              data-testid="report-export-by-test"
            >
              {intl.formatMessage({ id: "reports.byTest.export", defaultMessage: "Export CSV" })}
            </Button>
          </div>
        </div>
        {byTestLoading && (
          <p>{intl.formatMessage({ id: "reports.loading", defaultMessage: "Loading..." })}</p>
        )}
        {byTestMessage && (
          <InlineNotification
            kind="error"
            title={byTestMessage}
            lowContrast
            hideCloseButton
          />
        )}
        <table data-testid="by-test-table" className="revenueReports__table">
          <thead>
            <tr>
              <th>
                {intl.formatMessage({ id: "reports.byTest.test", defaultMessage: "Test" })}
              </th>
              <th>
                {intl.formatMessage({ id: "reports.byTest.localCode", defaultMessage: "Local code" })}
              </th>
              <th>
                {intl.formatMessage({ id: "reports.byTest.revenue", defaultMessage: "Revenue" })}
              </th>
            </tr>
          </thead>
          <tbody>
            {byTest.map((row) => (
              <tr key={row.testId ?? row.localCode ?? String(Math.random())}>
                <td>{row.testName || "—"}</td>
                <td>{row.localCode || "—"}</td>
                <td className="numeric">{money(row.revenue)}</td>
              </tr>
            ))}
            {!byTestLoading && byTest.length === 0 && (
              <tr>
                <td colSpan={3}>
                  {intl.formatMessage({
                    id: "reports.byTest.empty",
                    defaultMessage: "No data",
                  })}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      {/* Outstanding balances */}
      <section className="revenueReports__section">
        <h2 className="revenueReports__sectionTitle">
          {intl.formatMessage({
            id: "reports.outstanding.title",
            defaultMessage: "Outstanding balances",
          })}
        </h2>
        <div className="revenueReports__controls">
          <div className="revenueReports__control">
            <Button
              kind="primary"
              size="md"
              onClick={handleLoadOutstanding}
              disabled={outstandingLoading}
              data-testid="report-load-outstanding"
            >
              {intl.formatMessage({
                id: "reports.outstanding.load",
                defaultMessage: "Load outstanding",
              })}
            </Button>
          </div>
          <div className="revenueReports__control revenueReports__export">
            <Button
              kind="tertiary"
              size="md"
              onClick={handleExportOutstanding}
              disabled={outstandingLoading || outstanding.length === 0}
              data-testid="report-export-outstanding"
            >
              {intl.formatMessage({
                id: "reports.outstanding.export",
                defaultMessage: "Export CSV",
              })}
            </Button>
          </div>
        </div>
        {outstandingLoading && (
          <p>{intl.formatMessage({ id: "reports.loading", defaultMessage: "Loading..." })}</p>
        )}
        {outstandingMessage && (
          <InlineNotification
            kind="error"
            title={outstandingMessage}
            lowContrast
            hideCloseButton
          />
        )}
        <table data-testid="outstanding-table" className="revenueReports__table">
          <thead>
            <tr>
              <th>
                {intl.formatMessage({ id: "reports.outstanding.invoice", defaultMessage: "Invoice" })}
              </th>
              <th>
                {intl.formatMessage({ id: "reports.outstanding.patient", defaultMessage: "Patient" })}
              </th>
              <th>
                {intl.formatMessage({ id: "reports.outstanding.status", defaultMessage: "Status" })}
              </th>
              <th>
                {intl.formatMessage({ id: "reports.outstanding.payer", defaultMessage: "Payer" })}
              </th>
              <th>
                {intl.formatMessage({ id: "reports.outstanding.currency", defaultMessage: "Currency" })}
              </th>
              <th>
                {intl.formatMessage({ id: "reports.outstanding.issued", defaultMessage: "Issued" })}
              </th>
              <th className="numeric">
                {intl.formatMessage({ id: "reports.outstanding.total", defaultMessage: "Total" })}
              </th>
              <th className="numeric">
                {intl.formatMessage({ id: "reports.outstanding.paid", defaultMessage: "Paid" })}
              </th>
              <th className="numeric">
                {intl.formatMessage({ id: "reports.outstanding.balance", defaultMessage: "Balance" })}
              </th>
            </tr>
          </thead>
          <tbody>
            {outstanding.map((invoice) => (
              <tr key={invoice.id ?? invoice.invoiceNumber}>
                <td>{invoice.invoiceNumber || "—"}</td>
                <td>{invoice.patientId || "—"}</td>
                <td>{invoice.status || "—"}</td>
                <td>{invoice.payerType || "—"}</td>
                <td>{invoice.currency || "—"}</td>
                <td>{invoice.issuedAt || "—"}</td>
                <td className="numeric">{money(invoice.total)}</td>
                <td className="numeric">{money(invoice.amountPaid)}</td>
                <td className="numeric">{balanceOf(invoice)}</td>
              </tr>
            ))}
            {!outstandingLoading && outstanding.length === 0 && (
              <tr>
                <td colSpan={9}>
                  {intl.formatMessage({
                    id: "reports.outstanding.empty",
                    defaultMessage: "No data",
                  })}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  );
};

export default RevenueReports;
