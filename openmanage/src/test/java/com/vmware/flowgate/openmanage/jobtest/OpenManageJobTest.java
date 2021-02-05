package com.vmware.flowgate.openmanage.jobtest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.FlowgatePowerState;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.openmanage.client.OpenManageAPIClient;
import com.vmware.flowgate.openmanage.config.ServiceKeyConfig;
import com.vmware.flowgate.openmanage.datamodel.Chassis;
import com.vmware.flowgate.openmanage.datamodel.CommonResult;
import com.vmware.flowgate.openmanage.datamodel.DeviceMetric;
import com.vmware.flowgate.openmanage.datamodel.DeviceMetricsResult;
import com.vmware.flowgate.openmanage.datamodel.DevicePower;
import com.vmware.flowgate.openmanage.datamodel.DeviceStatus;
import com.vmware.flowgate.openmanage.datamodel.DeviceTemperature;
import com.vmware.flowgate.openmanage.datamodel.MetricType;
import com.vmware.flowgate.openmanage.datamodel.Plugin;
import com.vmware.flowgate.openmanage.datamodel.PowerDisplayUnit;
import com.vmware.flowgate.openmanage.datamodel.PowerManageMetricsRequestBody;
import com.vmware.flowgate.openmanage.datamodel.PowerSetting;
import com.vmware.flowgate.openmanage.datamodel.PowerSettingType;
import com.vmware.flowgate.openmanage.datamodel.PowerState;
import com.vmware.flowgate.openmanage.datamodel.Server;
import com.vmware.flowgate.openmanage.datamodel.ServerSpecificData;
import com.vmware.flowgate.openmanage.datamodel.TemperatureDisplayUnit;
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
   @Mock
   private OpenManageAPIClient openManageAPIClient;
   @Mock
   private WormholeAPIClient wormholeAPIClient;
   @Mock
   private ServiceKeyConfig serviceKeyConfig;
   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
      Mockito.when(this.wormholeAPIClient.getMappedAsset(AssetCategory.Server)).thenReturn(getMappedAsset());
   }

   @Test
   public void executeJobTest() {
      FacilitySoftwareConfig config = new FacilitySoftwareConfig();
      config.setId("executeJob");
      IntegrationStatus integrationStatus = new IntegrationStatus();
      integrationStatus.setStatus(IntegrationStatus.Status.ACTIVE);
      doNothing().when(this.openManageAPIClient).getToken();
      doNothing().when(this.wormholeAPIClient).setServiceKey(any(String.class));
      Mockito.when(this.openmanageJobService.createClient(config)).thenReturn(this.openManageAPIClient);
      Mockito.when(this.wormholeAPIClient.saveAssets(any(Asset.class))).thenReturn(saveAsset());
      Mockito.when(this.wormholeAPIClient.saveAssets(ArgumentMatchers.anyList())).thenReturn(null);
      Mockito.when(this.wormholeAPIClient.getAllAssetsBySourceAndType(config.getId(),
            AssetCategory.Server)).thenReturn(getAllAssetsBySourceAndType());
      Mockito.when(this.wormholeAPIClient.getAllAssetsBySourceAndType(config.getId(),
            AssetCategory.Chassis)).thenReturn(new ArrayList<Asset>());
      Mockito.when(this.openManageAPIClient.getDevices(any(Integer.class),any(Integer.class),
            ArgumentMatchers.eq(Server.class))).thenReturn(getCommonResultServer());
      Mockito.when(this.openManageAPIClient.getDevices(any(Integer.class),any(Integer.class),
            ArgumentMatchers.eq(Chassis.class))).thenReturn(new CommonResult<Chassis>());
      openmanageJobService.executeJob(EventMessageUtil.OpenManage_SyncAssetsMetaData, config);
   }

   @Test
   public void handleServerAssetsTest() {
      CommonResult<Server> serversResult = new CommonResult<Server>();
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
      server2.setPowerState(PowerState.OFF.getValue());
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
      String source = "integrationId";
      List<Asset> assets = openmanageJobService.handleServerAssets(serversResult, assetNumberMap, source);
      TestCase.assertEquals(servers.size(), assets.size());
      for(Asset serverAsset : assets) {
         HashMap<String, String> valueMap = serverAsset.getJustificationfields();
         if(server1.getDeviceName().equals(serverAsset.getAssetName())) {
            TestCase.assertEquals(server1.getAssetTag(), serverAsset.getTag());
            TestCase.assertEquals(server1.getModel(), serverAsset.getModel());
            TestCase.assertEquals(server1.getDeviceSpecificData().getManufacturer(), serverAsset.getManufacturer());
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
            TestCase.assertEquals(FlowgatePowerState.OFFHARD.name(), valueMap.get(FlowgateConstant.POWERSTATE));
         }else {
            TestCase.fail("Invalid asset");
         }
      }
   }

   @Test
   public void handleChassiAssetsTest() {
      CommonResult<Chassis> serversResult = new CommonResult<Chassis>();
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
      String source = "integrationId";
      List<Asset> assets = openmanageJobService.handleChassisAssets(serversResult, assetNumberMap, source);
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

   @Test
   public void getMetricDatasSourceIsDifferent() {
      FacilitySoftwareConfig config = new FacilitySoftwareConfig();
      config.setId(createAsset().getAssetSource()+"SourceIsDifferent");
      List<RealTimeData> metricDatas = openmanageJobService.getMetricDatas(config, openManageAPIClient);
      TestCase.assertEquals(true, metricDatas.isEmpty());
   }

   @Test
   public void getMetricDatas() {
      Mockito.when(this.openManageAPIClient.getCommonResult(Plugin.class)).thenReturn(getCommonResult());
      Mockito.when(this.openManageAPIClient.getDevicePowerMetrics("10074")).thenReturn(getPowerMetricsData());
      Mockito.when(this.openManageAPIClient.getDeviceTemperatureMetrics("10074")).thenReturn(getDeviceTemperature());
      Mockito.when(this.openManageAPIClient.getMetricsFromPowerManage(any(PowerManageMetricsRequestBody.class))).thenReturn(getDeviceMetricsResult());
      Mockito.when(this.openManageAPIClient.getCommonResult(PowerSetting.class)).thenReturn(getPowerSettings());
      FacilitySoftwareConfig config = new FacilitySoftwareConfig();
      config.setId(createAsset().getAssetSource());
      List<RealTimeData> metricDatas = openmanageJobService.getMetricDatas(config, openManageAPIClient);
      for(RealTimeData data : metricDatas) {
         if("assetid1611766210404".equals(data.getId())) {
            //power metrics
            List<ValueUnit> values = data.getValues();
            for(ValueUnit value : values) {
               switch (value.getKey()) {
               case MetricName.SERVER_AVERAGE_USED_POWER:
                  TestCase.assertEquals(0.074, value.getValueNum());
                  break;
               //use powerManager value to override it
               case MetricName.SERVER_POWER:
                  TestCase.assertEquals(0.072, value.getValueNum());
                  break;
               case MetricName.SERVER_MINIMUM_USED_POWER:
                  TestCase.assertEquals(0.07, value.getValueNum());
                  break;
               case MetricName.SERVER_PEAK_USED_POWER:
                  TestCase.assertEquals(0.08, value.getValueNum());
                  break;
               case MetricName.SERVER_ENERGY_CONSUMPTION:
                  TestCase.assertEquals(800.0, value.getValueNum());
                  break;
               case MetricName.SERVER_AVERAGE_TEMPERATURE:
                  TestCase.assertEquals(22.0, value.getValueNum());
                  break;
               case MetricName.SERVER_TEMPERATURE:
                  TestCase.assertEquals(24.0, value.getValueNum());
                  break;
               case MetricName.SERVER_PEAK_TEMPERATURE:
                  TestCase.assertEquals(30.0, value.getValueNum());
                  break;
               case MetricName.SERVER_FRONT_TEMPERATURE:
                  TestCase.assertEquals(24.0, value.getValueNum());
                  break;
               default:
                  TestCase.fail();
                  break;
               }
            }
         }else {
            TestCase.fail();
         }
      }
   }

   public ResponseEntity<Asset[]> getMappedAsset(){
      Asset[] assets = new Asset[2];
      assets[0] = createAsset();

      Map<String, String> openManageMap = new HashMap<String, String>();
      openManageMap.put(FlowgateConstant.ASSETNUMBER, "10074");
      HashMap<String,String> justficationfileds = new HashMap<String,String>();
      try {
         String openManageInfo = mapper.writeValueAsString(openManageMap);
         justficationfileds.put(FlowgateConstant.OPENMANAGE, openManageInfo);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      assets[0].setId("assetid");
      assets[0].setJustificationfields(justficationfileds);
      assets[1] = createAsset();
      return new ResponseEntity<Asset[]>(assets,HttpStatus.OK);
   }

   ResponseEntity<Void> saveAsset(){
      HttpHeaders httpHeaders = new HttpHeaders();
      try {
         httpHeaders.setLocation(new URI("/v1/assets/123"));
      } catch (URISyntaxException e) {
        TestCase.fail(e.getMessage());
      }
      ResponseEntity<Void> res = new ResponseEntity<Void>(httpHeaders,HttpStatus.OK);
      return res;
   }

   List<Asset> getAllAssetsBySourceAndType(){
      List<Asset> assets = new ArrayList<Asset>();
      Asset asset1 = createAsset();
      Map<String, String> openManageMap = new HashMap<String, String>();
      openManageMap.put(FlowgateConstant.ASSETNUMBER, "10084");
      HashMap<String,String> justficationfileds = new HashMap<String,String>();
      try {
         String openManageInfo = mapper.writeValueAsString(openManageMap);
         justficationfileds.put(FlowgateConstant.OPENMANAGE, openManageInfo);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      asset1.setId("assetid");
      asset1.setJustificationfields(justficationfileds);
      assets.add(asset1);
      return assets;
   }

   Asset createAsset(){
      Asset asset = new Asset();
      asset.setAssetName("pek-wor-server-02");
      asset.setAssetNumber(12345);
      asset.setAssetSource("l9i8728d55368540fcba1692");
      asset.setCategory(AssetCategory.Server);
      asset.setModel("Dell 750");
      asset.setManufacturer("Dell");
      return asset;
   }

   private CommonResult<Plugin> getCommonResult(){
      Plugin plugin1 = new Plugin();
      plugin1.setEnabled(true);
      plugin1.setInstalled(true);
      plugin1.setName(OpenManageJobService.PowerManager);
      plugin1.setId("powerManagerplugin");
      List<Plugin> plugins = new ArrayList<Plugin>();
      plugins.add(plugin1);
      CommonResult<Plugin> commonResult = new CommonResult<Plugin>();
      commonResult.setValue(plugins);
      return commonResult;
   }

   private DevicePower getPowerMetricsData() {
      DevicePower devicePower = new DevicePower();
      devicePower.setDateFormat("CIM");
      devicePower.setAvgPower(74);
      devicePower.setPower(72);
      devicePower.setPeakPower(80);
      devicePower.setMinimumPower(70);
      devicePower.setSystemEnergyConsumption(800);
      devicePower.setAvgPowerUnit(OpenManageJobService.OpenmanagePowerUnit);
      devicePower.setPeakPowerUnit(OpenManageJobService.OpenmanagePowerUnit);
      devicePower.setPowerUnit(OpenManageJobService.OpenmanagePowerUnit);
      devicePower.setMinimumPowerUnit(OpenManageJobService.OpenmanagePowerUnit);
      devicePower.setSystemEnergyConsumptionUnit(OpenManageJobService.systemEnergyConsumptionUnit);
      devicePower.setPeakPowerTimeStamp("20210125105010.404804-360");
      devicePower.setMinimumPowerTimeStamp("20210126105010.404804-360");
      devicePower.setInstantaneousHeadroomTimeStamp("20210127105010.404804-360");
      devicePower.setSince("20210117105010.404804-360");
      devicePower.setSystemEnergyConsumptionTimeStamp("20210127105010.404804-360");
      return devicePower;
   }

   private DeviceTemperature getDeviceTemperature() {
      DeviceTemperature temperature = new DeviceTemperature();
      temperature.setAvgTemperature(22);
      temperature.setAvgTemperatureUnit(OpenManageJobService.temperatureUnit);
      temperature.setAvgTemperatureTimeStamp("20210122105010.404804-360");
      temperature.setInstantaneousTemperature(24);
      temperature.setInstantaneousTemperatureUnit(OpenManageJobService.temperatureUnit);
      temperature.setPeakTemperature(30);
      temperature.setPeakTemperatureTimeStamp("20210107105010.404804-360");
      temperature.setPeakTemperatureUnit(OpenManageJobService.temperatureUnit);
      temperature.setStartTime("20210101105010.404804-360");
      temperature.setDateFormat(OpenManageJobService.CIM );
      return temperature;
   }

   private DeviceMetricsResult getDeviceMetricsResult() {
      DeviceMetricsResult result = new DeviceMetricsResult();
      List<DeviceMetric> values = new ArrayList<DeviceMetric>();
      DeviceMetric power = new DeviceMetric();
      power.setTimestamp("2021-01-02 13:45:05.147666");
      power.setType(MetricType.INSTANT_POWER.getValue());
      power.setValue(70);
      values.add(power);

      DeviceMetric temperature = new DeviceMetric();
      temperature.setType(MetricType.INSTANT_TEMP.getValue());
      temperature.setTimestamp("2021-01-02 13:45:05.147666");
      temperature.setValue(24);
      values.add(temperature);
      result.setValue(values);
      return result;
   }

   private CommonResult<PowerSetting> getPowerSettings(){
      CommonResult<PowerSetting> powerSettings = new CommonResult<PowerSetting>();
      List<PowerSetting> values = new ArrayList<PowerSetting>();
      PowerSetting power = new PowerSetting();
      power.setId(PowerSettingType.PowerUnit.getValue());
      power.setValue(PowerDisplayUnit.Watt.getValue());
      values.add(power);
      PowerSetting temperature = new PowerSetting();
      temperature.setId(PowerSettingType.TemperatureUnit.getValue());
      temperature.setValue(TemperatureDisplayUnit.Celsius.getValue());
      values.add(temperature);
      powerSettings.setValue(values);
      return powerSettings;
   }

   private CommonResult<Server> getCommonResultServer(){
      CommonResult<Server> serversResult = new CommonResult<Server>();
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
      server2.setPowerState(PowerState.OFF.getValue());
      server2.setStatus(DeviceStatus.NORMAL.getValue());
      ServerSpecificData specificData2 = new ServerSpecificData();
      specificData2.setManufacturer("Dell");
      server2.setDeviceSpecificData(specificData2);
      servers.add(server2);
      serversResult.setValue(servers);

      return serversResult;
   }
}
