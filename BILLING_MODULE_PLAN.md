# OpenELIS Global — Billing & Insurance Module Plan

**Target system:** OpenELIS Global (`C:\Users\arbay\develop\openelisglobal`)
**Authoring product:** AegisDiagnostics
**Scope:** Revenue tracking + daily summaries + NHIS/private insurance claims + payment gateway. Not a general ledger.

---

## 1. Objectives

- Capture billing at order creation (sample/order save) — event-driven, does not replace order entry.
- Track revenue per branch per day (cash vs mobile money vs gateway vs insurance).
- Support Ghana payer mix: NHIS + private insurers (Glico, Nationwide Mutual, corporates) + cash.
- Facility-level NHIS accreditation via the technical head's license (one record per branch).
- Per-branch payment gateway configuration (Paystack, Hubtel, MTN MoMo).
- Void/refund for cancelled tests (no stock, analyzer breakdown).
- Daily revenue summary + few reports (by branch, by test, by payer).
- Delivered as a separate OpenELIS module so core upgrades remain safe.

---

## 2. Branch model in OpenELIS

OpenELIS has **no `Branch` entity**. Map "branch" to:

- **Organization** (child org under HQ via self-referential `ORG_ID` in `ORGANIZATION` table), or
- **LabUnit** (existing user-role scoping used in workplans/sample visibility).

Every invoice, payment, claim, and daily summary carries `organization_id` (branch).

---

## 3. Module layout (Java)

```
org.openelisglobal.billing
  valueholder/
    BillingInvoice.java
    BillingInvoiceLine.java
    BillingPayment.java
    BillingPaymentAllocation.java
    DailyRevenueSummary.java
    BillingPayer.java
    PatientPolicy.java
    ClaimBatch.java
    ClaimLine.java
    FacilityCredentialing.java
    BranchGatewayConfig.java
  service/
    InvoiceService.java
    PaymentService.java
    RevenueReportService.java
    PayerService.java
    PolicyService.java
    ClaimBatchService.java
    NhisXmlService.java
    ClaimFeedbackService.java
    GatewayService.java        (interface + drivers)
      drivers/PaystackDriver.java
      drivers/HubtelDriver.java
      drivers/MtnMomoDriver.java
  dao/
    BillingInvoiceDAO.java + Impl
    ClaimBatchDAO.java + Impl
    ...
  controller/rest/
    BillingRestController.java
    ClaimsRestController.java
    ReportsRestController.java
  fhir/        (optional later)
    InvoiceTransformServiceImpl.java

org.openelisglobal.license  (separate package — shipped with the app)
  service/
    LicenseService.java
    FingerprintService.java
    TrialService.java
  model/
    License.java
    TrialRecord.java
  controller/rest/
    LicenseRestController.java
  config/
    LicenseConfiguration.java
```

---

## 4. Data model

### 4.1 BillingInvoice
| Field | Type | Notes |
|---|---|---|
| id | String | PK |
| organization_id | String | Branch org |
| patient_id | String | FK to Patient |
| external_order_id | String | Sample accession / ElectronicOrder externalId |
| invoice_number | String | Per-branch per-day sequence |
| status | enum | draft / issued / partially_paid / paid / void |
| currency | String | GHS default |
| issued_at | Timestamp | |
| subtotal / tax_total / discount_total / total | decimal | |
| amount_paid | decimal | |
| created_by | String | |
| payer_type | enum | cash / nhis / private |
| insurance_number | String | From Patient.insuranceNumber |

### 4.2 BillingInvoiceLine (one per test)
| Field | Type | Notes |
|---|---|---|
| id | String | PK |
| invoice_id | String | FK |
| test_id / test_name | String | FK to Test + display name |
| loinc_code / local_code | String | From Test |
| quantity | int | |
| unit_price / line_total | decimal | From tariff config |
| patient_responsibility_amount | decimal | Patient pays |
| insurance_expected_amount | decimal | Payer expected to pay |
| line_status | enum | active / void |
| claim_line_id | String | FK if on NHIS claim |
| payer_snapshot | JSON | Payer/plan in force at order time |

### 4.3 BillingPayment
| Field | Type | Notes |
|---|---|---|
| id | String | PK |
| organization_id | String | Branch |
| patient_id | String | FK |
| method | enum | cash / card / bank_transfer / mobile_money / gateway |
| gateway | enum | paystack / hubtel / mtn_momo |
| type | enum | payment / refund |
| amount | decimal | |
| provider_transaction_id | String | Gateway reference |
| received_at | Timestamp | |
| recorded_by | String | |
| metadata | JSON | |

### 4.4 BillingPaymentAllocation
| Field | Type | Notes |
|---|---|---|
| id | String | PK |
| payment_id | String | FK |
| invoice_line_id | String | FK |
| amount | decimal | |

### 4.5 DailyRevenueSummary
| Field | Type | Notes |
|---|---|---|
| organization_id | String | Branch |
| business_date | Date | |
| gross_revenue | decimal | |
| cash_revenue / mobile_money_revenue / gateway_revenue | decimal | |
| insurance_revenue / nhis_revenue / private_revenue | decimal | |
| refunds | decimal | |
| invoice_count | int | |
| patient_count | int | |

### 4.6 BillingPayer
| Field | Type | Notes |
|---|---|---|
| id | String | PK |
| name | String | NHIS / Glico / Nationwide Mutual / corporate |
| type | enum | nhis / private |
| config | encrypted JSON | NHIA number, eClaim number, endpoints per payer |
| is_active | boolean | |

### 4.7 PatientPolicy
| Field | Type | Notes |
|---|---|---|
| id | String | PK |
| patient_id | String | FK to Patient |
| payer_id | String | FK to BillingPayer |
| member_number | String | NHIS member no |
| mother_member_number | String | NHIS |
| effective_from / effective_to | Date | |
| active | boolean | |

### 4.8 FacilityCredentialing (per branch, technical head)
| Field | Type | Notes |
|---|---|---|
| id | String | PK |
| organization_id | String | Branch |
| technical_head_name | String | Legally responsible person |
| technical_head_license_number | String | |
| prescribing_level | String | |
| specialities | JSON | |
| accreditation_number | String | NHIA facility accreditation |
| level_of_care | String | |
| valid_from / valid_to | Date | |
| is_active | boolean | |
| source_file / imported_at | Timestamp | Audit of credential source |

### 4.9 BranchGatewayConfig
| Field | Type | Notes |
|---|---|---|
| id | String | PK |
| organization_id | String | Branch |
| driver | enum | paystack / hubtel / mtn_momo |
| public_key | String | |
| secret_key | encrypted | |
| webhook_secret | encrypted | |
| is_enabled | boolean | |
| test_mode | boolean | |
| metadata | JSON | |

Multiple drivers per branch allowed.

---

## 5. Gateway integration (to be added)

Interface `PaymentGatewayDriver`:
- `initialize(invoice)` → checkout URL / reference
- `verify(reference)` → payment status
- `refund(transactionId, amount)` → refund result
- `webhook(payload)` → parse gateway callback

Drivers: `PaystackDriver`, `HubtelDriver`, `MtnMomoDriver`.

Config stored per branch in `BranchGatewayConfig` (encrypted keys). No payment plans, no deposit system — keep minimal per scope.

---

## 6. Hook into order creation

Integration point: `SamplePatientUpdateData.createPopulatedSample` / `initSampleData`
(`src/main/java/org/openelisglobal/sample/action/util/SamplePatientUpdateData.java`).

Flow after `SampleTestCollection` is built:
1. Resolve `organization_id` (branch) from the current user's lab unit / session org.
2. Read `paymentOptionSelection` → `payer_type`; read `Patient.insuranceNumber` → resolve `PatientPolicy` / `BillingPayer`.
3. If an invoice already exists for `externalOrderNumber`/`sampleId`, skip (idempotent).
4. Create `BillingInvoice` (status `draft`).
5. For each `SampleTestCollection` item + test, create `BillingInvoiceLine`:
   - `test_id` / `test_name` / `loinc_code` / `local_code` from `Test`
   - `unit_price` from configurable tariff (`Test.localCode` → price)
   - Split `patient_responsibility_amount` vs `insurance_expected_amount` based on payer type and policy.
6. On issuance: set status `issued`, compute totals.
7. On payment: create `BillingPayment` + `BillingPaymentAllocation`s; update invoice `amount_paid` / `status`.
8. On cancellation (no stock / analyzer down): void invoice lines (`line_status = void`, invoice status `void`), optionally create `BillingPayment` of type `refund`.

---

## 7. NHIS / insurance flow (Ghana-specific)

1. **Facility accreditation**: NHIS credentials the facility via `FacilityCredentialing` (technical head license). One record per branch, valid date range.
2. **Payer registration**: NHIS, Glico, Nationwide Mutual, corporates are rows in `BillingPayer`. NHIS config holds NHIA number/eClaim details.
3. **Patient coverage**: `PatientPolicy` links patient to payer with member number and dates.
4. **Claim batch**: `ClaimBatchService` filters eligible invoice lines (payer_type = nhis, active policy, branch, date range) → builds `ClaimLine`s → pre-flight check → export NHIA v8.6 XML (`NhisXmlService`) → file-based Claim-It upload (no real-time API, per Flowrise pattern).
5. **Feedback import**: `ClaimFeedbackService` parses NHIA response XML → updates `ClaimLine.approved_amount` / `status` → reconciles against invoice balances.

---

## 8. Reports (core deliverable)

- **Daily revenue summary** per branch per day: gross, cash, mobile money, gateway, insurance, NHIS, private, refunds, invoice count, patient count.
- **Monthly rollup** by branch.
- **Revenue by test / LOINC**.
- **Outstanding balance by patient** (issued minus paid).
- **NHIS batch reconciliation** (submitted vs approved vs paid).

No general ledger, no double-entry.

---

## 9. Configuration flags

- `billing.module.enabled`
- `billing.tariff.lookup` (test localCode → price)
- `gateway.paystack.enabled`, `gateway.hubtel.enabled`, `gateway.mtn_momo.enabled` (per branch via `BranchGatewayConfig`)
- `nhis.enabled`, `nhis.xml.version` (8.6)
- `nhis.accreditation.number` (held on FacilityCredentialing, not a global flag)

---

## 10. What is explicitly NOT in scope

- General ledger / double-entry accounting
- Payment plans / installments
- Patient deposit / prepaid credit
- Real-time NHIS API submission (file-based XML only)
- Refunds register beyond void + refund payment record

---

## 11. Implementation phases

| Phase | Work |
|---|---|
| 1 | Module scaffold + Hibernate entities + DAOs |
| 2 | Invoice creation hook on order/sample save |
| 3 | Payment recording + allocation + void/refund |
| 4 | Gateway drivers (Paystack, Hubtel, MTN MoMo) + per-branch config |
| 5 | Payer + policy + NHIS claim batch + XML export/import |
| 6 | Daily revenue summary + reports UI |
| 7 | License + fingerprint + trial + .exe wrapper + offline deployment |
| 8 | Testing against Ghana NHIS flow + UAT |

---

## 12. Files to modify in OpenELIS core

**None.** Zero edits to any OpenELIS core file. This keeps upgrades safe — you can pull new OpenELIS releases without merge conflicts.

### How billing hooks into order creation (no core edits)

Instead of editing `SamplePatientUpdateData.java` or `Sample.java`, use **Spring AOP**:

```
org.openelisglobal.billing.aspect
  SampleOrderAspect.java   (@AfterReturning on SamplePatientUpdateData.createPopulatedSample()
                           and/or SamplePatientUpdateData.initSampleData())
```

The aspect fires after OpenELIS saves the sample/order, reads `externalOrderNumber` / `paymentOptionSelection` / patient insurance info from the form, and creates `BillingInvoice` + `BillingInvoiceLine` rows in your own tables. OpenELIS core code is untouched.

### Where billing data lives

- All billing entities: `org.openelisglobal.billing.valueholder` (new classes only)
- All billing tables: new `*.hbm.xml` files under `src/main/resources/hibernate/hbm/` (billing-only tables, no core table changes)
- All billing logic: `org.openelisglobal.billing.service` / `dao` / `controller/rest`
- All license logic: `org.openelisglobal.license` (separate package)

### Linking invoices to orders without touching Sample.java

`BillingInvoice.external_order_id` stores the sample accession number or ElectronicOrder externalId. Join on that field — no foreign key to core `Sample` table, no core column additions.

### Configuration flags

Store billing/NHIS/gateway flags in your own `BillingConfig` table (new table), not in OpenELIS core config properties.

### Tables you create (billing-only)

```
billing_invoice
billing_invoice_line
billing_payment
billing_payment_allocation
daily_revenue_summary
billing_payer
patient_policy
claim_batch
claim_line
facility_credentialing
branch_gateway_config
billing_config
license_trial_record   (trial start date keyed to machine fingerprint)
```

No `ALTER TABLE` on any OpenELIS core table. No new columns on `PATIENT`, `SAMPLE`, `TEST`, etc.

---

## 13. Windows deployment without Docker

OpenELIS Global is a Java/Spring Boot WAR running on Tomcat with a PostgreSQL database. For offline Windows deployment, bundle everything into a single installable package.

### 13.1 Java version compatibility — critical

| Item | Required | System currently has |
|---|---|---|
| OpenELIS `maven.java.release` | 21 | Java 25.0.3 (Temurin LTS) |
| Hibernate | 5.6.15.Final | — |
| Spring Framework | 6.2.19 | — |

**Do not compile or run OpenELIS with Java 25.** Java 25 is newer than what Hibernate 5.6 / Spring 6.2 / the OpenELIS codebase was tested against. Class-file version, reflection, and module-system changes in Java 25 can cause runtime failures.

**Build:** Use **Java 21** JDK for `mvn clean package`. Install a side-by-side Java 21 JDK (e.g. Temurin 21) and point `JAVA_HOME` to it during the build. Do not use the system Java 25 for Maven.

**Runtime (Windows):** Bundle a **Java 21 JRE** inside the installer (`C:\Program Files\AegisDiagnostics\OpenELIS\jre\`). Point the `.exe` wrapper and Tomcat at this private JRE, not the system `java`. This guarantees the same Java version that was used in QA, regardless of what the customer's machine has installed.

### 13.2 Stack to bundle

| Component | Windows delivery |
|---|---|
| Application | OpenELIS WAR + `org.openelisglobal.billing` classes |
| Servlet container | Tomcat (embedded or standalone) |
| JDK/JRE | OpenJDK JRE 21 (bundled, private to the app) |
| Database | PostgreSQL (Windows installer, run as local service) |
| Config | `application.properties` + OpenELIS config DB tables |
| Launcher | `.exe` wrapper (Launch4j / exe4j / custom C# launcher) |

### 13.3 Directory layout (installed on C:\ or D:\)

```
C:\Program Files\AegisDiagnostics\OpenELIS\
  bin/               → start.bat, stop.bat, wrapper .exe
  jre/               → bundled OpenJRE 21
  tomcat/            → Tomcat (webapps/openelis.war)
  postgresql/        → PostgreSQL data directory (or separate service)
  config/            → application.properties, logging, license file
  logs/              → app logs
  data/              → NHIS XML exports, uploaded files
```

### 13.4 Offline installation flow

1. Run installer (or extract zip) → installs JRE 21, Tomcat, PostgreSQL, deploys WAR.
2. PostgreSQL initialized with OpenELIS schema (run `ddl` / Liquibase equivalent once).
3. First launch: app detects no valid license → shows "Enter license key" screen (or exits to license UI).
4. Operator copies the machine fingerprint to a USB stick, takes it to an internet-connected machine, sends it to you, you generate a license key, they bring it back on USB and paste it in.
5. App validates fingerprint + expiry + features → unlocks.

---

## 14. Hardware fingerprint + license key (payment protection)

Since you work before you are paid and deployments are offline, the license is the enforcement mechanism. No phone-home required.

### 14.1 License workflow (trial → paid)

1. **First install:** App installs with no license key. A **2-week trial clock starts** from the first launch date (stored locally as `trial_start_date`).
2. **During trial:** Full functionality is unlocked. App shows "Trial expires in X days" on the license screen.
3. **After 14 days:** App locks — all features blocked except the license activation screen.
4. **Operator obtains fingerprint:** On the lock screen, the app displays a machine fingerprint. Operator copies it (USB/email/phone) and sends it to you (the vendor).
5. **You generate a key:** Using your offline keygen tool, you enter the fingerprint + customer name + expiry + features and generate a signed license file.
6. **Operator activates:** Brings the license file back on USB, pastes it into the app. App validates signature, binds key to this machine's fingerprint, and unlocks permanently (or until `expires_at`).

### 14.2 Hardware fingerprint (HDD serial)

On Windows, collect a hardware-bound identifier from the target machine:

- **Primary:** Physical hard drive serial number via WMI (`Win32_DiskDrive.SerialNumber`).
- **Fallback / mixer:** Motherboard UUID (`Win32_ComputerSystemProduct.UUID`) + BIOS serial (`Win32_BIOS.SerialNumber`) + MAC address of first active adapter.
- Combine into a single fingerprint string, then SHA-256 hash it. Display this fingerprint to the operator in the license screen.

Example Java capture (JNA or WMI via COM, or shell out to `wmic`):

```text
wmic diskdrive get SerialNumber
wmic csproduct get UUID
wmic bios get SerialNumber
```

### 14.3 License key format

The license key is an encrypted/signed blob (AES-256 or RSA-signed JSON) containing:

| Field | Purpose |
|---|---|
| `fingerprint_hash` | SHA-256 of the target machine's HDD fingerprint — key only works on that machine |
| `issued_to` | Customer / clinic name |
| `expires_at` | Expiry date (e.g. 1 year, or "perpetual" / null) |
| `trial_granted` | boolean — whether a trial was already granted on this machine |
| `features` | Enabled modules: `billing`, `insurance`, `gateway_paystack`, `gateway_hubtel`, `gateway_mtn_momo` |
| `max_branches` | Branch limit (optional) |
| `issued_at` | Issue date |
| `signature` | Your private-key signature so keys cannot be forged |

### 14.4 Trial enforcement logic

On every startup, the app checks in this order:

1. **Valid license present?** → Verify signature + fingerprint match + expiry not passed → unlock.
2. **No license, but trial not yet started?** → Record `trial_start_date = today`, mark trial as started, unlock (trial mode).
3. **No license, trial started:** → Check `today - trial_start_date <= 14 days` → if yes, unlock (trial mode, show days remaining). If no → lock app, show license activation screen only.
4. **Invalid license (tampered, wrong machine, expired):** → Lock app, show license activation screen.

The trial start date is stored locally (e.g. in the license config file or a small preferences table). Do not rely solely on system clock — store it persistently so the user cannot reset the trial by changing the PC date.

### 14.5 Enforcement in the app

Add a Spring bean / `ServletContextListener` that runs at startup:

1. Recompute the local machine fingerprint.
2. Read `license.key` (or `license.dat`) from `config/`.
3. If valid license exists:
   - Decrypt / verify signature with your embedded public key.
   - Compare `fingerprint_hash` to local fingerprint → if mismatch, lock app.
   - Check `expires_at` → if past, lock app.
   - Check `features` → gate billing/insurance/gateway endpoints accordingly.
   - Unlock.
4. If no valid license:
   - Check trial state (started? within 14 days?). If trial valid → unlock in trial mode.
   - Else → redirect all requests to a license UI (`/license`) where the operator enters a new key. Block all other functionality.

### 14.6 Key generation (offline, your side — NOT shipped)

You (the vendor) run a small offline keygen tool on your own machine:

1. Receive customer's fingerprint hash (via USB/email/phone).
2. Enter customer name, expiry date, features, trial flag.
3. Generate signed, encrypted license file using your **private signing key**.
4. Send the license file back to the customer (USB/email).
5. Customer imports the file into the app.

Because the key is bound to the HDD fingerprint, the customer cannot copy the install + license to another machine without getting a new key from you.

### 14.7 .exe wrapper

Use **Launch4j** (free) or **exe4j** (commercial) to wrap the Tomcat startup script into `OpenELIS.exe`:

- Icon + version info (AegisDiagnostics branding)
- Points to bundled `jre/bin/javaw.exe` (Java 21)
- Launches `startup.bat` / embedded Tomcat on localhost:8080
- Optionally single-instance enforcement

Alternative: a lightweight C# (.NET) launcher that:
- Shows a splash screen
- Validates the license file before starting Tomcat
- Starts Tomcat as a child process
- Keeps a tray icon for start/stop

### 14.8 License UI (minimal, offline)

A single screen in OpenELIS:

```
Machine Fingerprint: ABCD-1234-EFGH-5678   [Copy]
Trial started: 2026-10-29 | Days remaining: 12
[Enter License Key: ________________________]  [Activate]
Status: TRIAL — expires in 12 days | Features: Billing, Insurance
```

After activation:

```
Machine Fingerprint: ABCD-1234-EFGH-5678   [Copy]
[License Key: ACTIVATED]                   [Change]
Status: Valid until 2027-10-29 | Features: Billing, Insurance, NHIS, Paystack
```

### 14.9 Files to add for licensing

```
org.openelisglobal.license
  service/
    LicenseService.java          (verify, validate, lock, trial tracking)
    FingerprintService.java      (WMI/HDD capture)
    TrialService.java            (trial start date, days remaining)
    KeyGenerator.java            (vendor-side, not shipped)
  model/
    License.java
    TrialRecord.java
  controller/rest/
    LicenseRestController.java   (/license endpoint — activate, status, fingerprint)
  config/
    LicenseConfiguration.java    (startup validator, trial check)
```

Resources:
- `src/main/resources/license/public-key.pem` (embedded public key for verification — private key stays with you)
- `config/license.key` or `config/license.dat` (customer-placed)
- `config/trial.dat` (persistent trial start record)

### 14.10 Security caveats

- This stops casual copying, not a determined reverse-engineer. Obfuscate the fingerprint check and license validation code (ProGuard / Allatori) if you want stronger protection.
- The fingerprint is hardware-bound, so HDD replacement invalidates the license — plan for a re-key support process.
- Do not ship your private signing key inside the app; only the public key is needed for verification.
- Store the trial start date persistently and validate it against the fingerprint so a user cannot reset the trial by reinstalling or changing the system clock.

---

