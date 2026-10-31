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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
public class PaystackDriverTest {

    private GatewayClient client;
    private GatewayConfigService configService;
    private BillingConfigService billingConfigService;
    private BillingInvoiceDAO invoiceDAO;
    private PaystackDriver driver;

    private static final String ENCRYPTED_SECRET = "enc-key";
    private static final String ENCRYPTED_WEBHOOK_SECRET = "enc-wh";
    private static final String SECRET_KEY = "sk_test_x";
    private static final String WEBHOOK_SECRET = "whsec_test";

    @Before
    public void setUp() {
        client = mock(GatewayClient.class);
        configService = mock(GatewayConfigService.class);
        billingConfigService = mock(BillingConfigService.class);
        invoiceDAO = mock(BillingInvoiceDAO.class);
        driver = new PaystackDriver(client, configService, billingConfigService, invoiceDAO);

        BillingInvoice invoice = new BillingInvoice();
        invoice.setId(101);
        invoice.setOrganizationId(1);

        BranchGatewayConfig config = new BranchGatewayConfig();
        config.setOrganizationId(1);
        config.setDriver(GatewayDriver.PAYSTACK.name());
        config.setSecretKey(ENCRYPTED_SECRET);
        config.setWebhookSecret(ENCRYPTED_WEBHOOK_SECRET);
        config.setIsEnabled(true);
        config.setTestMode(false);

        when(billingConfigService.isGatewayEnabled("paystack")).thenReturn(true);
        when(invoiceDAO.get(101)).thenReturn(Optional.of(invoice));
        when(configService.getConfig(1, GatewayDriver.PAYSTACK)).thenReturn(config);
        when(configService.getConfig(null, GatewayDriver.PAYSTACK)).thenReturn(config);
        when(configService.decryptSecret(ENCRYPTED_SECRET)).thenReturn(SECRET_KEY);
        when(configService.decryptSecret(ENCRYPTED_WEBHOOK_SECRET)).thenReturn(WEBHOOK_SECRET);
    }

    @Test
    public void initialize_returnsCheckoutUrl_whenGatewayRespondsSuccessfully() {
        when(client.post(anyInt(), any(), anyString(), anyString(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(200,
                        "{\"status\":true,\"data\":{\"authorization_url\":\"https://checkout.paystack.com/x\",\"reference\":\"ref_xyz\"}}"));

        String url = driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback");

        assertEquals("https://checkout.paystack.com/x", url);
        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(client).post(eq(1), eq(GatewayDriver.PAYSTACK), eq("https://api.paystack.co"),
                eq("/transaction/initialize"), bodyCaptor.capture());
        assertEquals(Long.valueOf(1250), bodyCaptor.getValue().get("amount"));
        assertEquals("https://example.com/callback", bodyCaptor.getValue().get("callback_url"));
    }

    @Test
    public void initialize_returnsNull_whenInvoiceHasNoOrganization() {
        when(invoiceDAO.get(101)).thenReturn(Optional.empty());

        assertNull(driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback"));
        verify(client, never()).post(anyInt(), any(), anyString(), anyString(), any());
    }

    @Test
    public void initialize_returnsNull_whenNoEnabledConfig() {
        when(configService.getConfig(1, GatewayDriver.PAYSTACK)).thenReturn(null);

        assertNull(driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback"));
        verify(client, never()).post(anyInt(), any(), anyString(), anyString(), any());
    }

    @Test
    public void verify_returnsTrue_whenStatusIsSuccess() {
        when(client.get(anyInt(), any(), anyString(), anyString()))
                .thenReturn(new GatewayClient.GatewayResponse(200,
                        "{\"status\":true,\"data\":{\"status\":\"success\"}}"));

        assertTrue(driver.verify("ref_xyz"));
    }

    @Test
    public void verify_returnsFalse_whenStatusIsFailed() {
        when(client.get(anyInt(), any(), anyString(), anyString()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"data\":{\"status\":\"failed\"}}"));

        assertFalse(driver.verify("ref_xyz"));
    }

    @Test
    public void verify_returnsFalse_whenHttpFails() {
        when(client.get(anyInt(), any(), anyString(), anyString()))
                .thenReturn(new GatewayClient.GatewayResponse(402,
                        "{\"status\":true,\"data\":{\"status\":\"success\"}}"));

        assertFalse(driver.verify("ref_xyz"));
    }

    @Test
    public void refund_returnsTrue_whenRefunded() {
        when(client.post(anyInt(), any(), anyString(), anyString(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"data\":{\"status\":\"success\"}}"));

        assertTrue(driver.refund("ref_xyz", new BigDecimal("12.50")));
        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(client).post(eq(null), eq(GatewayDriver.PAYSTACK), eq("https://api.paystack.co"), eq("/refund"),
                bodyCaptor.capture());
        assertEquals("ref_xyz", bodyCaptor.getValue().get("transaction"));
        assertEquals(Long.valueOf(1250), bodyCaptor.getValue().get("amount"));
    }

    @Test
    public void refund_returnsFalse_whenNotRefunded() {
        when(client.post(anyInt(), any(), anyString(), anyString(), any()))
                .thenReturn(new GatewayClient.GatewayResponse(200, "{\"data\":{\"status\":\"failed\"}}"));

        assertFalse(driver.refund("ref_xyz", new BigDecimal("1")));
    }

    @Test
    public void webhook_acceptsValidSignature() throws Exception {
        String payload = "{\"event\":\"charge.success\",\"data\":{\"status\":\"success\",\"reference\":\"ref_xyz\",\"amount\":200000}}";
        String signature = hmacSha512Hex(WEBHOOK_SECRET, payload);

        GatewayCallbackResult result = driver.webhook(payload, signature);

        assertTrue(result.isValid());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("ref_xyz", result.getReference());
        assertEquals(new BigDecimal("2000.00"), result.getAmount());
        assertEquals("charge.success", result.getEvent());
    }

    @Test
    public void webhook_rejectsInvalidSignature() throws Exception {
        String payload = "{\"event\":\"charge.success\",\"data\":{\"status\":\"success\",\"reference\":\"ref_xyz\",\"amount\":200000}}";

        GatewayCallbackResult result = driver.webhook(payload, "tampered-signature");

        assertFalse(result.isValid());
        assertEquals("signature mismatch", result.getMessage());
    }

    @Test
    public void webhook_rejectsMissingSignature() throws Exception {
        String payload = "{\"event\":\"charge.success\",\"data\":{\"status\":\"success\"}}";

        GatewayCallbackResult result = driver.webhook(payload);

        assertFalse(result.isValid());
        assertEquals("signature header not provided", result.getMessage());
    }

    @Test
    public void disabledDriver_shortCircuitsWithoutHttpCall() {
        when(billingConfigService.isGatewayEnabled("paystack")).thenReturn(false);

        assertNull(driver.initialize(101, new BigDecimal("12.50"), "https://example.com/callback"));
        assertFalse(driver.verify("ref_xyz"));
        assertFalse(driver.refund("ref_xyz", new BigDecimal("1")));
        assertFalse(driver.webhook("{}", "sig").isValid());

        verify(client, never()).post(anyInt(), any(), anyString(), anyString(), any());
        verify(client, never()).get(anyInt(), any(), anyString(), anyString());
    }

    private static String hmacSha512Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return hex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
