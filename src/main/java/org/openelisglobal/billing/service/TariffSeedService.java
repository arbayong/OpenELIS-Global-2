package org.openelisglobal.billing.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

/**
 * Seeds a new NHIA G-DRG tariff version from a CLAIM-it/NHIA investigation
 * tariff CSV export. Each import creates a brand-new tariff version (never
 * overwriting an existing one), so claims stay reproducible against the
 * schedule in force on their service date.
 */
public interface TariffSeedService {

    /** One row as parsed from the CSV. */
    class ParsedTariffRow {
        public String gdrgCode;
        public String description;
        public BigDecimal tariff;
    }

    /** Result of a tariff CSV import. */
    class ImportSummary {
        public Integer versionId;
        public String version;
        public int importedRows;
        public int skippedRows;
        public List<String> warnings;
    }

    /**
     * Import NHIA investigation tariffs from CSV as a new tariff version.
     *
     * @param csvContents    the whole CSV file contents
     * @param version        the new tariff version string
     * @param providerTier   the provider tier the schedule applies to
     * @param effectiveFrom  start of the version's effective window
     * @param effectiveTo    end of the version's effective window
     * @param recordedBy     the operator who performed the import
     * @return summary of the import: version id, version, imported/skipped row
     *         counts and warnings for malformed rows
     */
    ImportSummary importInveTariffs(String csvContents, String version, String providerTier,
            Date effectiveFrom, Date effectiveTo, String recordedBy);
}
