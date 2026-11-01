package org.openelisglobal.billing.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.dao.GdrgMappingDAO;
import org.openelisglobal.billing.dao.TariffVersionDAO;
import org.openelisglobal.billing.service.TariffSeedService.ImportSummary;
import org.openelisglobal.billing.service.impl.TariffSeedServiceImpl;
import org.openelisglobal.billing.valueholder.GdrgMapping;
import org.openelisglobal.billing.valueholder.TariffVersion;
import org.openelisglobal.common.exception.LIMSRuntimeException;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TariffSeedServiceImplTest {

    private static final String VERSION = "2024-A";
    private static final String PROVIDER_TIER = "Tertiary";
    private static final Date EFFECTIVE_FROM = Date.valueOf("2024-01-01");
    private static final Date EFFECTIVE_TO = Date.valueOf("2024-12-31");

    @Mock
    private TariffVersionDAO tariffVersionDAO;
    @Mock
    private GdrgMappingDAO gdrgMappingDAO;
    @InjectMocks
    private TariffSeedServiceImpl service;

    @Before
    public void setUp() {
        List<TariffVersion> empty = new ArrayList<TariffVersion>();
        when(tariffVersionDAO.getAll()).thenReturn(empty);
        when(tariffVersionDAO.insert(any(TariffVersion.class))).thenReturn(42);
    }

    @Test
    public void testHappyPathImportsThreeRowsAndLinksMappingsToNewVersion() {
        String csv = "code,description,tariff_ghs\n"
                + "INVE01D,\"2 Hour Post Prandial Blood Glucose\",21.28\n"
                + "INVE02D,24hr Urine for Protein,15.76\n"
                + "INVE100,\"Acid, Phosphate\",12.43";

        ImportSummary summary = service.importInveTariffs(csv, VERSION, PROVIDER_TIER,
                EFFECTIVE_FROM, EFFECTIVE_TO, "operator");

        assertNotNull(summary.versionId);
        assertEquals(Integer.valueOf(42), summary.versionId);
        assertEquals(VERSION, summary.version);
        assertEquals(3, summary.importedRows);
        assertEquals(0, summary.skippedRows);
        assertNotNull(summary.warnings);
        assertTrue(summary.warnings.isEmpty());

        InOrder order = inOrder(tariffVersionDAO, gdrgMappingDAO);
        ArgumentCaptor<TariffVersion> versionCap = ArgumentCaptor.forClass(TariffVersion.class);
        order.verify(tariffVersionDAO).insert(versionCap.capture());
        order.verify(gdrgMappingDAO, times(3)).insert(any(GdrgMapping.class));

        TariffVersion inserted = versionCap.getValue();
        assertEquals(VERSION, inserted.getVersion());
        assertEquals("NHIA investigations " + VERSION, inserted.getName());
        assertEquals(PROVIDER_TIER, inserted.getProviderTier());
        assertEquals(EFFECTIVE_FROM, inserted.getEffectiveFrom());
        assertEquals(EFFECTIVE_TO, inserted.getEffectiveTo());
        assertEquals("Y", inserted.getIsActive());
        assertEquals("nhis-inve-tariffs.csv", inserted.getSource());
        assertNotNull(inserted.getImportedAt());
        assertEquals(Integer.valueOf(42), inserted.getId());

        ArgumentCaptor<GdrgMapping> mappingCap = ArgumentCaptor.forClass(GdrgMapping.class);
        verify(gdrgMappingDAO, times(3)).insert(mappingCap.capture());
        List<GdrgMapping> mappings = mappingCap.getAllValues();
        assertEquals(Integer.valueOf(42), mappings.get(0).getVersionId());
        assertEquals("INVE01D", mappings.get(0).getLocalCode());
        assertEquals("INVE01D", mappings.get(0).getGdrgCode());
        assertEquals("2 Hour Post Prandial Blood Glucose", mappings.get(0).getDescription());
        assertEquals(new BigDecimal("21.28"), mappings.get(0).getTariff());
        assertEquals(EFFECTIVE_FROM, mappings.get(0).getEffectiveFrom());
        assertEquals(EFFECTIVE_TO, mappings.get(0).getEffectiveTo());
        assertEquals("Y", mappings.get(0).getIsActive());

        assertEquals("INVE02D", mappings.get(1).getLocalCode());
        assertEquals("24hr Urine for Protein", mappings.get(1).getDescription());
        assertEquals(new BigDecimal("15.76"), mappings.get(1).getTariff());

        assertEquals("INVE100", mappings.get(2).getLocalCode());
        assertEquals("INVE100", mappings.get(2).getGdrgCode());
        assertEquals("Acid, Phosphate", mappings.get(2).getDescription());
        assertEquals(new BigDecimal("12.43"), mappings.get(2).getTariff());
    }

    @Test
    public void testMalformedRowIsSkippedWithLineWarning() {
        String csv = "code,description,tariff_ghs\n"
                + "INVE01D,Glucose,21.28\n"
                + "this is just one field\n"
                + "INVE02D,Protein,15.76";

        ImportSummary summary = service.importInveTariffs(csv, VERSION, null, EFFECTIVE_FROM, EFFECTIVE_TO, null);

        assertEquals(2, summary.importedRows);
        assertEquals(1, summary.skippedRows);
        assertEquals(1, summary.warnings.size());
        assertTrue(summary.warnings.get(0), summary.warnings.get(0).contains("Line 3"));
        assertTrue(summary.warnings.get(0), summary.warnings.get(0).contains("expected 3 fields, got 1"));

        verify(gdrgMappingDAO, times(2)).insert(any(GdrgMapping.class));
    }

    @Test
    public void testNonNumericAndNegativeTariffsAreMalformed() {
        String csv = "code,description,tariff_ghs\n"
                + "INVE01D,Glucose,21.28\n"
                + "INVE02D,Protein,notanumber\n"
                + "INVE03D,ALT,-5.00\n";

        ImportSummary summary = service.importInveTariffs(csv, VERSION, null, EFFECTIVE_FROM, EFFECTIVE_TO, null);

        assertEquals(1, summary.importedRows);
        assertEquals(2, summary.skippedRows);
        assertEquals(2, summary.warnings.size());
        assertTrue(summary.warnings.get(0), summary.warnings.get(0).contains("Line 3"));
        assertTrue(summary.warnings.get(0), summary.warnings.get(0).contains("not a number"));
        assertTrue(summary.warnings.get(0), summary.warnings.get(0).contains("notanumber"));
        assertTrue(summary.warnings.get(1), summary.warnings.get(1).contains("Line 4"));
        assertTrue(summary.warnings.get(1), summary.warnings.get(1).contains("negative"));
        assertTrue(summary.warnings.get(1), summary.warnings.get(1).contains("-5.00"));

        verify(gdrgMappingDAO, times(1)).insert(any(GdrgMapping.class));
    }

    @Test
    public void testAllMalformedCsvImportsNothingAndThrows() {
        String csv = "code,description,tariff_ghs\nbad\nworse";
        try {
            service.importInveTariffs(csv, VERSION, null, EFFECTIVE_FROM, EFFECTIVE_TO, null);
            fail("expected LIMSRuntimeException");
        } catch (LIMSRuntimeException expected) {
            // expected: nothing is inserted when no row parses
            verify(tariffVersionDAO, never()).insert(any(TariffVersion.class));
            verify(gdrgMappingDAO, never()).insert(any(GdrgMapping.class));
        }
    }

    @Test
    public void testDuplicateVersionThrowsAndInsertsNothing() {
        TariffVersion existing = new TariffVersion();
        existing.setVersion(VERSION);
        when(tariffVersionDAO.getAll()).thenReturn(List.of(existing));

        String csv = "code,description,tariff_ghs\nINVE01D,Glucose,21.28";
        try {
            service.importInveTariffs(csv, VERSION, null, EFFECTIVE_FROM, EFFECTIVE_TO, null);
            fail("expected LIMSRuntimeException");
        } catch (LIMSRuntimeException expected) {
            assertTrue(expected.getMessage(),
                    expected.getMessage().contains("Tariff version " + VERSION + " already exists"));
            verify(tariffVersionDAO, never()).insert(any(TariffVersion.class));
            verify(gdrgMappingDAO, never()).insert(any(GdrgMapping.class));
        }
    }

    @Test
    public void testEffectiveFromAfterEffectiveToThrows() {
        String csv = "code,description,tariff_ghs\nINVE01D,Glucose,21.28";
        try {
            service.importInveTariffs(csv, VERSION, null, EFFECTIVE_TO, EFFECTIVE_FROM, null);
            fail("expected LIMSRuntimeException");
        } catch (LIMSRuntimeException expected) {
            verify(tariffVersionDAO, never()).insert(any(TariffVersion.class));
        }
    }

    @Test
    public void testBlankCsvAndBlankVersionThrow() {
        String csv = "code,description,tariff_ghs\nINVE01D,Glucose,21.28";
        try {
            service.importInveTariffs("", VERSION, null, EFFECTIVE_FROM, EFFECTIVE_TO, null);
            fail("expected LIMSRuntimeException");
        } catch (LIMSRuntimeException expected) {
            // expected
        }
        try {
            service.importInveTariffs(null, VERSION, null, EFFECTIVE_FROM, EFFECTIVE_TO, null);
            fail("expected LIMSRuntimeException");
        } catch (LIMSRuntimeException expected) {
            // expected
        }
        try {
            service.importInveTariffs(csv, "   ", null, EFFECTIVE_FROM, EFFECTIVE_TO, null);
            fail("expected LIMSRuntimeException");
        } catch (LIMSRuntimeException expected) {
            // expected
        }
        verify(tariffVersionDAO, never()).insert(any(TariffVersion.class));
    }

    @Test
    public void testCrlfAndMissingTrailingNewlineParse() {
        String csv = "code,description,tariff_ghs\r\nINVE01D,Glucose,21.28\r\nINVE02D,Protein,15.76\r\n\n";
        ImportSummary summary = service.importInveTariffs(csv, VERSION, null, EFFECTIVE_FROM, EFFECTIVE_TO, null);

        assertEquals(2, summary.importedRows);
        assertEquals(0, summary.skippedRows);
        assertTrue(summary.warnings.isEmpty());
        verify(gdrgMappingDAO, times(2)).insert(any(GdrgMapping.class));
    }
}
