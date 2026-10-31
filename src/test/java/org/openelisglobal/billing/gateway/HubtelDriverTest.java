package org.openelisglobal.billing.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.dao.BillingInvoiceDAO;
import org.openelisglobal.billing.service.BillingConfigService;
import org.openelisglobal.billing.valueholder.BillingInvoice;
import org.openelisglobal.billing.valueholder.BranchGatewayConfig;
import org.openelisglobal.billing.valueholder.GatewayDriver;

@RunWith(MockitoJUnitRunner.Silent.class)
public class HubtelDriverTest {

    private GatewayClient client;
    private GatewayConfigService configService;
    private BillingConfigService billingConfigService;
    private BillingInvoiceDAO invoiceDAO;
    private HubtelDriver driver;

    private static final String ENCRYPTED_SECRET = "enc-key";
    private static final String SECRET_KEY = "sk_test_x";

    @Before
    public void setUp() {
        client = mock(GatewayClient.class);
        configService = mock(GatewayConfigService.class);
        billingConfigService = mock(BillingConfigService.class);
        invoiceDAO = mock(BillingInvoiceDAO.class);
        driver = new HubtelDriver(client, configService, billingConfigService, invoiceDAO);

        BillingInvoice invoice = new BillingInvoice();
        invoice.setId(101);
        invoice.setOrganizationId(1);

        BranchGatewayConfig config = new BranchGatewayConfig();
        config.setOrganizationId(1);
        config.setDriver(GatewayDriver.HUBTEL.name());
        config.setSecretKey(ENCRYPTED_SECRET);
        config.setIsEnabled(true);
        config.setTestMode(false);

        when(billingConfigService.isGatewayEnabled("hubtel")).thenReturn(true);
        when(invoiceDAO.get(101)).thenReturn(Optional.of(invoice));
        when(configService.getConfig(1, GatewayDriver.HUBTEL)).thenReturn(config);
        when(configService.getConfig(null, GatewayDriver.HUBTEL)).thenReturn(config);
        when(configService.decryptSecret(ENCRYPTED_SECRET)).thenReturn(SECRET_KEY);
    }

    @Test
    public void initialize_returnsCheckoutUrl_whenGatewayRespondsSuccessfully() {
        when(client.post(anyInt(), any(), anyString(), anyString(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(200,
                        "{\"data\":{\"checkout_url\":\"https://pay.hubtel.com/x\",\"reference\":\"ref_xyz\"}}"));

        String url = driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback");

        assertEquals("https://pay.hubtel.com/x", url);
        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(client).post(eq(1), eq(GatewayDriver.HUBTEL), eq("https://api.hubtel.com"),
                eq("/api/v1/payments/request"), bodyCaptor.capture());
        assertEquals(Long.valueOf(1250), bodyCaptor.getValue().get("amount"));
        assertEquals("https://example.com/callback", bodyCaptor.getValue().get("callback_url"));
    }

    @Test
    public void initialize_routesToSandbox_whenTestMode() {
        BranchGatewayConfig sandboxConfig = new BranchGatewayConfig();
        sandboxConfig.setOrganizationId(1);
        sandboxConfig.setDriver(GatewayDriver.HUBTEL.name());
        sandboxConfig.setSecretKey(ENCRYPTED_SECRET);
        sandboxConfig.setIsEnabled(true);
        sandboxConfig.setTestMode(true);
        when(configService.getConfig(1, GatewayDriver.HUBTEL)).thenReturn(sandboxConfig);
        when(client.post(anyInt(), any(), anyString(), anyString(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"data\":{\"checkout_url\":\"https://pay.hubtel.com/x\"}}"));

        driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback");

        ArgumentCaptor<String> baseUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).post(anyInt(), any(), baseUrlCaptor.capture(), anyString(), any());
        assertEquals("https://sandbox.api.hubtel.com", baseUrlCaptor.getValue());
    }

    @Test
    public void initialize_returnsNull_whenInvoiceHasNoOrganization() {
        when(invoiceDAO.get(101)).thenReturn(Optional.empty());

        assertNull(driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback"));
        verify(client, never()).post(anyInt(), any(), anyString(), anyString(), any());
    }

    @Test
    public void verify_returnsTrue_whenCompleted() {
        when(client.get(anyInt(), any(), anyString(), anyString()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"data\":{\"status\":\"completed\"}}"));

        assertTrue(driver.verify("ref_xyz"));
    }

    @Test
    public void verify_returnsFalse_whenFailed() {
        when(client.get(anyInt(), any(), anyString(), anyString()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"data\":{\"status\":\"failed\"}}"));

        assertFalse(driver.verify("ref_xyz"));
    }

    @Test
    public void refund_returnsTrue_whenRefunded() {
        when(client.post(anyInt(), any(), anyString(), anyString(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"data\":{\"status\":\"refunded\"}}"));

        assertTrue(driver.refund("ref_xyz", new BigDecimal("12.50")));
        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(client).post(eq(null), eq(GatewayDriver.HUBTEL), eq("https://api.hubtel.com"),
                eq("/api/v1/payments/ref_xyz/refund"), bodyCaptor.capture());
        assertEquals(Long.valueOf(1250), bodyCaptor.getValue().get("amount"));
    }

    @Test
    public void refund_returnsFalse_whenNotRefunded() {
        when(client.post(anyInt(), any(), anyString(), anyString(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"data\":{\"status\":\"declined\"}}"));

        assertFalse(driver.refund("ref_xyz", new BigDecimal("1")));
    }

    @Test
    public void webhook_parsesSuccessfulCallback() {
        String payload = "{\"responseCode\":\"00\",\"reference\":\"ref_xyz\",\"data\":{\"amount\":\"20000\",\"status\":\"success\"}}";

        GatewayCallbackResult result = driver.webhook(payload);

        assertTrue(result.isValid());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("ref_xyz", result.getReference());
        assertEquals(new BigDecimal("200.00"), result.getAmount());
    }

    @Test
    public void webhook_parsesFailedCallback() {
        String payload = "{\"responseCode\":\"42\",\"reference\":\"ref_xyz\",\"data\":{\"status\":\"failed\"}}";

        GatewayCallbackResult result = driver.webhook(payload);

        assertTrue(result.isValid());
        assertEquals("FAILED", result.getStatus());
    }

    @Test
    public void disabledDriver_shortCircuitsWithoutHttpCall() {
        when(billingConfigService.isGatewayEnabled("hubtel")).thenReturn(false);

        assertNull(driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback"));
        assertFalse(driver.verify("ref_xyz"));
        assertFalse(driver.refund("ref_xyz", new BigDecimal("1")));
        assertFalse(driver.webhook("{}", "sig").isValid());

        verify(client, never()).post(anyInt(), any(), anyString(), anyString(), any());
        verify(client, never()).get(anyInt(), any(), anyString(), anyString());
    }
}
