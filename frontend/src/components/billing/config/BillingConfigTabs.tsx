import React, { useState } from "react";
import { Tab, TabList, TabPanels, TabPanel, Tabs, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import PayerPanel from "./PayerPanel";
import PatientPolicyPanel from "./PatientPolicyPanel";
import TariffPanel from "./TariffPanel";
import FacilityCredentialPanel from "./FacilityCredentialPanel";
import GatewayConfigPanel from "./GatewayConfigPanel";
import "./BillingConfigTabs.css";const BILLING_CONFIG_TABS_ID = "billing.config.tabs";

const tabs = [
  { id: "payers", label: "billing.config.tab.payers" },
  { id: "patient-policies", label: "billing.config.tab.patientPolicies" },
  { id: "tariffs", label: "billing.config.tab.tariffs" },
  { id: "facility-credentials", label: "billing.config.tab.facilityCredentials" },
  { id: "gateway-config", label: "billing.config.tab.gatewayConfig" },
];

const BillingConfigTabs = () => {
  const intl = useIntl();
  const [selectedIndex, setSelectedIndex] = useState<number>(0);

  const handleChange = (state: { selectedIndex: number }) => {
    setSelectedIndex(state.selectedIndex);
  };

  return (
    <section className="billingConfigTabs" data-testid="billing-config-tabs">
      <PageBreadCrumb
        breadcrumbs={[
          {
            label: "billing.config.title",
            isCurrentPage: true,
          },
        ]}
      />
      <h2 className="billingConfigTabs__heading">
        {intl.formatMessage({
          id: "billing.config.title",
          defaultMessage: "Billing Configuration",
        })}
      </h2>
      <Tabs
        selectedIndex={selectedIndex}
        onChange={handleChange}
      >
        <TabList className="billingConfigTabs__tabList">
          {tabs.map((tab, index) => (
            <Tab key={tab.id}>
              {intl.formatMessage({
                id: tab.label,
                defaultMessage: tab.label,
              })}
            </Tab>
          ))}
        </TabList>
        <TabPanels>
          <TabPanel className="billingConfigTabs__tabPanel">
            <PayerPanel />
          </TabPanel>
          <TabPanel className="billingConfigTabs__tabPanel">
            <PatientPolicyPanel />
          </TabPanel>
          <TabPanel className="billingConfigTabs__tabPanel">
            <TariffPanel />
          </TabPanel>
          <TabPanel className="billingConfigTabs__tabPanel">
            <FacilityCredentialPanel />
          </TabPanel>
          <TabPanel className="billingConfigTabs__tabPanel">
            <GatewayConfigPanel />
          </TabPanel>
        </TabPanels>
      </Tabs>
      <p className="billingConfigTabs__note">
        {intl.formatMessage({
          id: "billing.config.secret.note",
          defaultMessage:
            "Gateway secretKey and webhookSecret are stored encrypted. Values are never revealed here.",
        })}
      </p>
      <Tag
        type="blue"
        size="sm"
        data-testid="billing-config-active-tab"
      >
        {intl.formatMessage({
          id: tabs[selectedIndex]?.label || "billing.config.tab.payers",
          defaultMessage: tabs[selectedIndex]?.label || "Payers",
        })}
      </Tag>
    </section>
  );
};

export default BillingConfigTabs;
