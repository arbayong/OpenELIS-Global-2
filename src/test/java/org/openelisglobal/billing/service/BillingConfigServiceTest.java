package org.openelisglobal.billing.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.dao.BillingConfigDAO;
import org.openelisglobal.billing.valueholder.BillingConfig;

/**
 * BillingConfigService reads the plan §9 flags from billing_config. Every read
 * falls back to its documented default so an unconfigured install bills
 * normally, and these tests pin those fallbacks.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BillingConfigServiceTest {

    @Mock
    private BillingConfigDAO billingConfigDAO;

    @InjectMocks
    private BillingConfigService service;

    private BillingConfig config(String value) {
        BillingConfig config = new BillingConfig();
        config.setConfigValue(value);
        return config;
    }

    @Test
    public void isEnabled_returnsConfiguredFalse() {
        when(billingConfigDAO.getByConfigKey("billing.module.enabled"))
                .thenReturn(List.of(config("false")));

        assertFalse(service.isModuleEnabled());
    }

    @Test
    public void isEnabled_acceptsYAndOne() {
        when(billingConfigDAO.getByConfigKey("nhis.enabled")).thenReturn(List.of(config("Y")));
        assertTrue(service.isNhisEnabled());

        when(billingConfigDAO.getByConfigKey("nhis.enabled")).thenReturn(List.of(config("1")));
        assertTrue(service.isNhisEnabled());
    }

    @Test
    public void isEnabled_fallsBackToDefault_whenKeyUnset() {
        when(billingConfigDAO.getByConfigKey("billing.module.enabled")).thenReturn(Collections.emptyList());

        assertEquals(true, service.isModuleEnabled());
        assertEquals(true, service.isNhisEnabled());
        assertEquals(true, service.isTariffLookupEnabled());
    }

    @Test
    public void isEnabled_fallsBackToDefault_whenValueBlank() {
        when(billingConfigDAO.getByConfigKey("nhis.enabled")).thenReturn(List.of(config("   ")));

        assertTrue(service.isNhisEnabled());
    }

    @Test
    public void isEnabled_fallsBackToDefault_whenDaoThrows() {
        when(billingConfigDAO.getByConfigKey("billing.module.enabled"))
                .thenThrow(new RuntimeException("database unavailable"));

        assertTrue(service.isModuleEnabled());
    }

    @Test
    public void getNhisXmlVersion_returnsConfiguredValue() {
        when(billingConfigDAO.getByConfigKey("nhis.xml.version")).thenReturn(List.of(config(" 9.0 ")));

        assertEquals("9.0", service.getNhisXmlVersion());
    }

    @Test
    public void getNhisXmlVersion_fallsBackToNhai86() {
        when(billingConfigDAO.getByConfigKey("nhis.xml.version")).thenReturn(Collections.emptyList());

        assertEquals("8.6", service.getNhisXmlVersion());
    }

    @Test
    public void isGatewayEnabled_readsPerDriverFlag() {
        when(billingConfigDAO.getByConfigKey("gateway.paystack.enabled")).thenReturn(List.of(config("false")));

        assertFalse(service.isGatewayEnabled("PAYSTACK"));
        assertTrue(service.isGatewayEnabled("HUBTEL"));
    }

    @Test
    public void isGatewayEnabled_falseForMissingDriver() {
        assertFalse(service.isGatewayEnabled(null));
        assertFalse(service.isGatewayEnabled("  "));
    }
}
