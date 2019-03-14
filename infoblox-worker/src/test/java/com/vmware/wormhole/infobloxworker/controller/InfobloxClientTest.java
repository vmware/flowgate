/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.infobloxworker.controller;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.ClientProtocolException;
import com.vmware.wormhole.client.WormholeAPIClient;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.wormhole.infobloxworker.model.Infoblox;
import com.vmware.wormhole.infobloxworker.model.JsonResultForQueryHostNames;
import com.vmware.wormhole.infobloxworker.service.InfobloxClient;

import ch.qos.logback.classic.Logger;
import junit.framework.TestCase;

public class InfobloxClientTest {

    @Mock
    private JsonResultForQueryHostNames jsonHostNameResult = new JsonResultForQueryHostNames();

    @Test
    public void queryHostNamesByIPTest() {
        String ip = "192.168.1.6";
        List<String> successResult = new ArrayList<String>();
        successResult.add("abc");
        successResult.add("bcd");
        successResult.add("def");
        initJsonHostNameResult();
        InfobloxClient client = new InfobloxClient(initInfobloxData());
        client = Mockito.spy(client);
        Mockito.doReturn(jsonHostNameResult).when(client).getHostNameList(ip);
        List<String> ret = client.queryHostNamesByIP(ip);
        TestCase.assertEquals(successResult, ret);
    }

    private FacilitySoftwareConfig initInfobloxData() {
        FacilitySoftwareConfig infoBlox = new FacilitySoftwareConfig();
        infoBlox.setName("Nlyte");
        infoBlox.setUserName("admin");
        infoBlox.setPassword("Admin!23");
        infoBlox.setServerURL("10.160.46.136");
        infoBlox.setUserId("1");
        infoBlox.setVerifyCert(false);
        infoBlox.setType(SoftwareType.InfoBlox);
    return infoBlox;
    }
    private void initJsonHostNameResult() {
        List<Infoblox> result = new ArrayList<Infoblox>();
        Infoblox infoblox = new Infoblox();
        String[] hostnames = {"abc", "bcd.com", "def.com"};
        infoblox.set_ref("123");
        infoblox.setHostNames(hostnames);
        infoblox.setIpAddress("192.168.1.6");
        infoblox.setIsConflict(true);
        infoblox.setMacAddress("123");
        infoblox.setNetwork("123");
        infoblox.setNetworkView("123");
        infoblox.setObjects(hostnames);
        infoblox.setStatus("123");
        infoblox.setTypes(hostnames);
        infoblox.setUsage(hostnames);
        result.add(infoblox);
        jsonHostNameResult.setResult(result);
    }
}
