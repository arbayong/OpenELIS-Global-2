package org.openelisglobal.billing.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.billing.dao.BranchGatewayConfigDAO;
import org.openelisglobal.billing.valueholder.BranchGatewayConfig;
import org.openelisglobal.billing.valueholder.GatewayDriver;

/**
 * getConfig must resolve a config even when no branch is supplied, because
 * verify and webhook signatures carry no organization. Regression: a null
 * organization used to make getConfig return null, which made every gateway
 * verification and webhook fail in production while the mocked tests passed.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GatewayConfigServiceImplTest {

    @Mock
    private BranchGatewayConfigDAO gatewayConfigDAO;

    @Mock
    private GatewayConfigRepository gatewayConfigRepository;

    @InjectMocks
    private GatewayConfigServiceImpl service;

    private BranchGatewayConfig config(Integer orgId) {
        BranchGatewayConfig config = new BranchGatewayConfig();
        config.setOrganizationId(orgId);
        config.setDriver(GatewayDriver.PAYSTACK.name());
        config.setIsEnabled(Boolean.TRUE);
        return config;
    }

    @Test
    public void getConfig_usesBranchConfig_whenOrganizationGiven() {
        when(gatewayConfigDAO.getEnabledByOrganizationId(7)).thenReturn(List.of(config(7)));

        assertEquals(Integer.valueOf(7),
                service.getConfig(7, GatewayDriver.PAYSTACK).getOrganizationId());
    }

    @Test
    public void getConfig_fallsBackToDriverWideConfig_whenOrganizationIsNull() {
        when(gatewayConfigRepository.findEnabledByDriver(GatewayDriver.PAYSTACK))
                .thenReturn(List.of(config(7)));

        BranchGatewayConfig resolved = service.getConfig(null, GatewayDriver.PAYSTACK);

        assertEquals(Integer.valueOf(7), resolved.getOrganizationId());
    }

    @Test
    public void getConfig_returnsNull_whenNoConfigAnywhere() {
        when(gatewayConfigRepository.findEnabledByDriver(GatewayDriver.PAYSTACK)).thenReturn(List.of());

        assertNull(service.getConfig(null, GatewayDriver.PAYSTACK));
    }

    @Test
    public void getConfig_returnsNull_forNullDriver() {
        assertNull(service.getConfig(7, null));
    }
}
