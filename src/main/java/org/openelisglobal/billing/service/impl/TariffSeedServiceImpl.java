package org.openelisglobal.billing.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.billing.dao.GdrgMappingDAO;
import org.openelisglobal.billing.dao.TariffVersionDAO;
import org.openelisglobal.billing.service.TariffSeedService;
import org.openelisglobal.billing.service.TariffSeedService.ImportSummary;
import org.openelisglobal.billing.service.TariffSeedService.ParsedTariffRow;
import org.openelisglobal.billing.valueholder.GdrgMapping;
import org.openelisglobal.billing.valueholder.TariffVersion;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TariffSeedServiceImpl implements TariffSeedService {

    private static final String SOURCE = "nhis-inve-tariffs.csv";
    private static final String HEADER_CODE = "code";
    private static final String HEADER_DESCRIPTION = "description";
    private static final String HEADER_TARIFF = "tariff_ghs";

    @Autowired
    private TariffVersionDAO tariffVersionDAO;

    @Autowired
    private GdrgMappingDAO gdrgMappingDAO;

    @Override
    @Transactional
    public ImportSummary importInveTariffs(String csvContents, String version, String providerTier,
            Date effectiveFrom, Date effectiveTo, String recordedBy) {
        if (csvContents == null || csvContents.trim().isEmpty()) {
            throw new LIMSRuntimeException("CSV contents are required");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new LIMSRuntimeException("Tariff version is required");
        }
        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.after(effectiveTo)) {
            throw new LIMSRuntimeException("Effective from date cannot be after effective to date");
        }

        List<TariffVersion> existing = tariffVersionDAO.getAll();
        if (existing != null) {
            for (TariffVersion current : existing) {
                if (version.equals(current.getVersion())) {
                    throw new LIMSRuntimeException("Tariff version " + version + " already exists");
                }
            }
        }

        String[] lines = splitLines(csvContents);

        List<ParsedTariffRow> valid = new ArrayList<ParsedTariffRow>();
        List<String> warnings = new ArrayList<String>();
        int skipped = 0;

        int physicalLine = 1;
        boolean headerPassed = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isEmpty()) {
                physicalLine++;
                continue;
            }
            if (!headerPassed) {
                headerPassed = true;
                if (isHeader(line)) {
                    physicalLine++;
                    continue;
                }
            }

            String[] fields = parseLine(line);
            String reason = malformedReason(fields);
            if (reason != null) {
                skipped++;
                warnings.add("Line " + physicalLine + ": " + reason);
                physicalLine++;
                continue;
            }

            ParsedTariffRow row = new ParsedTariffRow();
            row.gdrgCode = fields[0].trim();
            row.description = fields[1].trim();
            row.tariff = new BigDecimal(fields[2].trim());
            valid.add(row);
            physicalLine++;
        }

        if (valid.isEmpty()) {
            throw new LIMSRuntimeException("CSV contains no valid tariff rows to import");
        }

        TariffVersion tariffVersion = new TariffVersion();
        tariffVersion.setVersion(version.trim());
        tariffVersion.setName("NHIA investigations " + version.trim());
        tariffVersion.setProviderTier(providerTier);
        tariffVersion.setEffectiveFrom(effectiveFrom);
        tariffVersion.setEffectiveTo(effectiveTo);
        tariffVersion.setIsActive("Y");
        tariffVersion.setSource(SOURCE);
        tariffVersion.setImportedAt(new Timestamp(System.currentTimeMillis()));

        Integer versionId = tariffVersionDAO.insert(tariffVersion);
        tariffVersion.setId(versionId);

        for (ParsedTariffRow row : valid) {
            GdrgMapping mapping = new GdrgMapping();
            mapping.setLocalCode(row.gdrgCode);
            mapping.setVersionId(versionId);
            mapping.setGdrgCode(row.gdrgCode);
            mapping.setDescription(row.description);
            mapping.setTariff(row.tariff);
            mapping.setEffectiveFrom(effectiveFrom);
            mapping.setEffectiveTo(effectiveTo);
            mapping.setIsActive("Y");
            gdrgMappingDAO.insert(mapping);
        }

        ImportSummary summary = new ImportSummary();
        summary.versionId = versionId;
        summary.version = version.trim();
        summary.importedRows = valid.size();
        summary.skippedRows = skipped;
        summary.warnings = warnings;

        LogEvent.logInfo("TariffSeedServiceImpl", "importInveTariffs",
                "Imported " + valid.size() + " tariff rows into version " + version
                        + " (" + skipped + " skipped, source " + SOURCE + ")");

        return summary;
    }

    private static String[] splitLines(String contents) {
        return contents.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
    }

    private static boolean isHeader(String line) {
        String[] fields = line.split(",", -1);
        if (fields.length != 3) {
            return false;
        }
        return HEADER_CODE.equalsIgnoreCase(fields[0].trim())
                && HEADER_DESCRIPTION.equalsIgnoreCase(fields[1].trim())
                && HEADER_TARIFF.equalsIgnoreCase(fields[2].trim());
    }

    /**
     * Parse one CSV line into fields, honouring quoted fields with embedded
     * commas and escaped double quotes ("" -> "). No library is used.
     */
    private static String[] parseLine(String line) {
        List<String> fields = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    /**
     * Returns a human-readable reason why a row is malformed, or null when the
     * row is valid. A row is malformed when it does not have exactly 3 fields,
     * the code is blank, or the tariff does not parse as a non-negative
     * BigDecimal.
     */
    private static String malformedReason(String[] fields) {
        if (fields == null || fields.length != 3) {
            return "malformed row (expected 3 fields, got "
                    + (fields == null ? 0 : fields.length) + ")";
        }
        String code = fields[0] == null ? null : fields[0].trim();
        if (code == null || code.isEmpty()) {
            return "malformed row (code is blank)";
        }
        String tariffText = fields[2] == null ? null : fields[2].trim();
        if (tariffText == null || tariffText.isEmpty()) {
            return "malformed row (tariff is blank)";
        }
        try {
            BigDecimal tariff = new BigDecimal(tariffText);
            if (tariff.compareTo(BigDecimal.ZERO) < 0) {
                return "malformed row (tariff is negative: '" + tariffText + "')";
            }
        } catch (NumberFormatException e) {
            return "malformed row (tariff is not a number: '" + tariffText + "')";
        }
        return null;
    }
}
