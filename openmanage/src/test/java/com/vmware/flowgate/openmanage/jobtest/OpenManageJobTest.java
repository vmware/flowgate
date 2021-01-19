package com.vmware.flowgate.openmanage.jobtest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.openmanage.datamodel.Chassis;
import com.vmware.flowgate.openmanage.datamodel.DeviceStatus;
import com.vmware.flowgate.openmanage.datamodel.DevicesResult;
import com.vmware.flowgate.openmanage.datamodel.PowerState;
import com.vmware.flowgate.openmanage.datamodel.Server;
import com.vmware.flowgate.openmanage.datamodel.ServerSpecificData;
import com.vmware.flowgate.openmanage.job.OpenManageJobService;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class OpenManageJobTest {

   @Spy
   @InjectMocks
   private OpenManageJobService openmanageJobService;
   private ObjectMapper mapper = new ObjectMapper();

   @Test
   public void handleServerAssetsTest() {
      DevicesResult<Server> serversResult = new DevicesResult<Server>();
      List<Server> servers = new ArrayList<Server>();
      Server server1 = new Server();
      server1.setAssetTag("lkw456");
      server1.setModel("los23");
      server1.setId(258l);
      server1.setDeviceName("myserver1");
      server1.setPowerState(PowerState.ON.getValue());
      server1.setStatus(DeviceStatus.NORMAL.getValue());
      ServerSpecificData specificData = new ServerSpecificData();
      specificData.setManufacturer("Dell");
      server1.setDeviceSpecificData(specificData);
      servers.add(server1);

      Server server2 = new Server();
      server2.setAssetTag("asdqw2");
      server2.setModel("sr4d");
      server2.setId(252l);
      server2.setDeviceName("myserver2");
      server2.setPowerState(PowerState.ON.getValue());
      server2.setStatus(DeviceStatus.NORMAL.getValue());
      ServerSpecificData specificData2 = new ServerSpecificData();
      specificData2.setManufacturer("Dell");
      server2.setDeviceSpecificData(specificData2);
      servers.add(server2);
      serversResult.setValue(servers);

      Asset asset = new Asset();
      asset.setCategory(AssetCategory.Server);
      asset.setAssetName("myserver2");
      asset.setModel("sr4d");
      asset.setManufacturer("Dell");
      asset.setTag("0121");

      Map<String, String> openManageMap = new HashMap<String, String>();
      openManageMap.put(FlowgateConstant.ASSETNUMBER, String.valueOf(server2.getId()));
      HashMap<String,String> justficationfileds = new HashMap<String,String>();
      try {
         String openManageInfo = mapper.writeValueAsString(openManageMap);
         justficationfileds.put(FlowgateConstant.OPENMANAGE, openManageInfo);
         asset.setJustificationfields(justficationfileds);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      Map<Long, Asset> assetNumberMap = new HashMap<Long, Asset>();
      assetNumberMap.put(252l, asset);
      List<Asset> assets = openmanageJobService.handleServerAssets(serversResult, assetNumberMap);
      TestCase.assertEquals(servers.size(), assets.size());
      for(Asset serverAsset : assets) {
         if(server1.getDeviceName().equals(serverAsset.getAssetName())) {
            TestCase.assertEquals(server1.getAssetTag(), serverAsset.getTag());
            TestCase.assertEquals(server1.getModel(), serverAsset.getModel());
            TestCase.assertEquals(server1.getDeviceSpecificData().getManufacturer(), serverAsset.getManufacturer());
            HashMap<String, String> valueMap = serverAsset.getJustificationfields();
            if(valueMap.isEmpty() || !valueMap.containsKey(FlowgateConstant.OPENMANAGE)) {
               TestCase.fail("Asset number is required");
            }
            String openManageInfo = valueMap.get(FlowgateConstant.OPENMANAGE);
            try {
               Map<String, String> infoMap = mapper.readValue(openManageInfo, new TypeReference<Map<String, String>>() {});;
               String openManageDeviceId = infoMap.get(FlowgateConstant.ASSETNUMBER);
               TestCase.assertEquals(server1.getId(), Long.valueOf(openManageDeviceId).longValue());
            } catch (IOException ioException) {
               TestCase.fail(ioException.getMessage());
            }
         }else if(server2.getDeviceName().equals(serverAsset.getAssetName())) {
            TestCase.assertEquals(server2.getAssetTag(), serverAsset.getTag());
         }else {
            TestCase.fail("Invalid asset");
         }
      }
   }

   @Test
   public void handleChassiAssetsTest() {
      DevicesResult<Chassis> serversResult = new DevicesResult<Chassis>();
      List<Chassis> chassisList = new ArrayList<Chassis>();
      Chassis chassis1 = new Chassis();
      chassis1.setAssetTag("chassis1");
      chassis1.setModel("los23");
      chassis1.setId(2583l);
      chassis1.setDeviceName("mychassis1");
      chassis1.setPowerState(PowerState.ON.getValue());
      chassis1.setStatus(DeviceStatus.NORMAL.getValue());
      chassisList.add(chassis1);

      Chassis chassis2 = new Chassis();
      chassis2.setAssetTag("chassis2");
      chassis2.setModel("sr4d");
      chassis2.setId(2523l);
      chassis2.setDeviceName("mychassis2");
      chassis2.setPowerState(PowerState.ON.getValue());
      chassis2.setStatus(DeviceStatus.NORMAL.getValue());
      chassisList.add(chassis2);

      Chassis chassis3 = new Chassis();
      chassis3.setAssetTag("chassis3");
      chassis3.setModel("chassis");
      chassis3.setId(2522l);
      chassis3.setDeviceName("mychassis3");
      chassis3.setPowerState(PowerState.ON.getValue());
      chassis3.setStatus(DeviceStatus.UNKNOWN.getValue());
      chassisList.add(chassis3);
      serversResult.setValue(chassisList);

      Asset asset = new Asset();
      asset.setCategory(AssetCategory.Chassis);
      asset.setAssetName("mychassis2");
      asset.setModel("sr4d");
      asset.setManufacturer("Dell");
      asset.setTag("0121");

      Map<String, String> openManageMap = new HashMap<String, String>();
      openManageMap.put(FlowgateConstant.ASSETNUMBER, String.valueOf(chassis2.getId()));
      HashMap<String,String> justficationfileds = new HashMap<String,String>();
      try {
         String openManageInfo = mapper.writeValueAsString(openManageMap);
         justficationfileds.put(FlowgateConstant.OPENMANAGE, openManageInfo);
         asset.setJustificationfields(justficationfileds);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      Map<Long, Asset> assetNumberMap = new HashMap<Long, Asset>();
      assetNumberMap.put(252l, asset);
      List<Asset> assets = openmanageJobService.handleChassisAssets(serversResult, assetNumberMap);
      TestCase.assertEquals(chassisList.size() - 1, assets.size());
      for(Asset chassisAsset : assets) {
         if(chassis1.getDeviceName().equals(chassisAsset.getAssetName())) {
            TestCase.assertEquals(chassis1.getAssetTag(), chassisAsset.getTag());
            TestCase.assertEquals(chassis1.getModel(), chassisAsset.getModel());
            HashMap<String, String> valueMap = chassisAsset.getJustificationfields();
            if(valueMap.isEmpty() || !valueMap.containsKey(FlowgateConstant.OPENMANAGE)) {
               TestCase.fail("Asset number is required");
            }
            String openManageInfo = valueMap.get(FlowgateConstant.OPENMANAGE);
            try {
               Map<String, String> infoMap = mapper.readValue(openManageInfo, new TypeReference<Map<String, String>>() {});;
               String openManageDeviceId = infoMap.get(FlowgateConstant.ASSETNUMBER);
               TestCase.assertEquals(chassis1.getId(), Long.valueOf(openManageDeviceId).longValue());
            } catch (IOException ioException) {
               TestCase.fail(ioException.getMessage());
            }
         }else if(chassis2.getDeviceName().equals(chassisAsset.getAssetName())) {
            TestCase.assertEquals(chassis2.getAssetTag(), chassisAsset.getTag());
         }else {
            TestCase.fail("Invalid asset");
         }
      }
   }
}
