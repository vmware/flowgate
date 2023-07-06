package com.vmware.flowgate.vroworker.scheduler.job;

import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.vroworker.config.ServiceKeyConfig;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
class VROAsyncJobTest {

    @Spy
    @InjectMocks
    VROAsyncJob service;

    @Mock
    private WormholeAPIClient restClient;

    @Mock
    private ServiceKeyConfig serviceKeyConfig;

    @Test
    void testCheckAndUpdateIntegrationStatus() {
        SDDCSoftwareConfig sddcSoftwareConfig = new SDDCSoftwareConfig();
        service.checkAndUpdateIntegrationStatus(sddcSoftwareConfig,"message");
        TestCase.assertNotNull(sddcSoftwareConfig.getIntegrationStatus());
    }
}