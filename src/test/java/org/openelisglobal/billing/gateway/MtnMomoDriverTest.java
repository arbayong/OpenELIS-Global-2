package org.openelisglobal.billing.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
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
public class MtnMomoDriverTest {

    private GatewayClient client;
    private GatewayConfigService configService;
    private BillingConfigService billingConfigService;
    private BillingInvoiceDAO invoiceDAO;
    private MtnMomoDriver driver;

    private static final String ENCRYPTED_SECRET = "enc-key";
    private static final String SECRET_KEY = "sk_test_x";

    @Before
    public void setUp() {
        client = mock(GatewayClient.class);
        configService = mock(GatewayConfigService.class);
        billingConfigService = mock(BillingConfigService.class);
        invoiceDAO = mock(BillingInvoiceDAO.class);
        driver = new MtnMomoDriver(client, configService, billingConfigService, invoiceDAO);

        BillingInvoice invoice = new BillingInvoice();
        invoice.setId(101);
        invoice.setOrganizationId(1);

        BranchGatewayConfig config = new BranchGatewayConfig();
        config.setOrganizationId(1);
        config.setDriver(GatewayDriver.MTN_MOMO.name());
        config.setSecretKey(ENCRYPTED_SECRET);
        config.setIsEnabled(true);
        config.setTestMode(false);

        when(billingConfigService.isGatewayEnabled("mtn_momo")).thenReturn(true);
        when(invoiceDAO.get(101)).thenReturn(Optional.of(invoice));
        when(configService.getConfig(1, GatewayDriver.MTN_MOMO)).thenReturn(config);
        when(configService.getConfig(null, GatewayDriver.MTN_MOMO)).thenReturn(config);
        when(configService.decryptSecret(ENCRYPTED_SECRET)).thenReturn(SECRET_KEY);
    }

    @Test
    public void initialize_returnsReference_whenRequestToPayAccepted() {
        // MTN MoMo returns 202 with no body; the X-Reference sent with the
        // request is the checkout reference.
        when(client.post(anyInt(), any(), anyString(), anyString(), any(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(202, ""));

        String reference = driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback");

        assertNotNull(reference);
        assertTrue(reference.startsWith("INV-101-"));
        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(client).post(eq(1), eq(GatewayDriver.MTN_MOMO), eq("https://momodeveloper.mtn.com"),
                eq("/collection/v1_0/requesttopay"), bodyCaptor.capture(), headersCaptor.capture());
        assertEquals("1250", bodyCaptor.getValue().get("amount"));
        assertEquals("GHS", bodyCaptor.getValue().get("currency"));
        assertTrue((String) headersCaptor.getValue().get("X-Reference"),
                ((String) headersCaptor.getValue().get("X-Reference")).startsWith("INV-101-"));
        assertEquals("production", headersCaptor.getValue().get("X-Target-Environment"));
    }

    @Test
    public void initialize_routesToSandbox_whenTestMode() {
        BranchGatewayConfig sandboxConfig = new BranchGatewayConfig();
        sandboxConfig.setOrganizationId(1);
        sandboxConfig.setDriver(GatewayDriver.MTN_MOMO.name());
        sandboxConfig.setSecretKey(ENCRYPTED_SECRET);
        sandboxConfig.setIsEnabled(true);
        sandboxConfig.setTestMode(true);
        when(configService.getConfig(1, GatewayDriver.MTN_MOMO)).thenReturn(sandboxConfig);
        when(client.post(anyInt(), any(), anyString(), anyString(), any(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(202, ""));

        driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback");

        ArgumentCaptor<String> baseUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).post(anyInt(), any(), baseUrlCaptor.capture(), anyString(), any(), any());
        assertEquals("https://sandbox.momodeveloper.mtn.com", baseUrlCaptor.getValue());
    }

    @Test
    public void initialize_returnsNull_whenInvoiceHasNoOrganization() {
        when(invoiceDAO.get(101)).thenReturn(Optional.empty());

        assertNull(driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback"));
        verify(client, never()).post(anyInt(), any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void verify_returnsTrue_whenSuccess() {
        when(client.get(anyInt(), any(), anyString(), anyString()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"status\":\"SUCCESS\"}"));

        assertTrue(driver.verify("ref_xyz"));
    }

    @Test
    public void verify_returnsFalse_whenFailed() {
        when(client.get(anyInt(), any(), anyString(), anyString()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"status\":\"FAILED\"}"));

        assertFalse(driver.verify("ref_xyz"));
    }

    @Test
    public void verify_returnsFalse_whenPending() {
        when(client.get(anyInt(), any(), anyString(), anyString()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"status\":\"PENDING\"}"));

        assertFalse(driver.verify("ref_xyz"));
    }

    @Test
    public void refund_returnsTrue_whenReversed() {
        when(client.post(anyInt(), any(), anyString(), anyString(), any(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(200, ""));

        assertTrue(driver.refund("ref_xyz", new BigDecimal("12.50")));
    }

    @Test
    public void refund_returnsFalse_whenReversalFails() {
        when(client.post(anyInt(), any(), anyString(), anyString(), any(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(400, ""));

        assertFalse(driver.refund("ref_xyz", new BigDecimal("1")));
    }

    @Test
    public void webhook_parsesSuccessfulCallback() {
        String payload = "{\"status\":\"SUCCESS\",\"externalReference\":\"ref_xyz\",\"amount\":\"20000\",\"currency\":\"GHS\"}";

        GatewayCallbackResult result = driver.webhook(payload);

        assertTrue(result.isValid());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("ref_xyz", result.getReference());
        assertEquals(new BigDecimal("200.00"), result.getAmount());
    }

    @Test
    public void disabledDriver_shortCircuitsWithoutHttpCall() {
        when(billingConfigService.isGatewayEnabled("mtn_momo")).thenReturn(false);

        assertNull(driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback"));
        assertFalse(driver.verify("ref_xyz"));
        assertFalse(driver.refund("ref_xyz", new BigDecimal("1")));
        assertFalse(driver.webhook("{}", "sig").isValid());

        verify(client, never()).post(anyInt(), any(), anyString(), anyString(), any(), any());
        verify(client, never()).get(anyInt(), any(), anyString(), anyString());
    }
}
