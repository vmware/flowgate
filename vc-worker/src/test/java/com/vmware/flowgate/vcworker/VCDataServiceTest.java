/**
 *
 */
package com.vmware.flowgate.vcworker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.flowgate.common.model.redis.message.EventMessage;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.vcworker.model.VCServerInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.vcworker.client.VsphereClient;
import com.vmware.flowgate.vcworker.config.ServiceKeyConfig;
import com.vmware.flowgate.vcworker.model.EsxiMetadata;
import com.vmware.flowgate.vcworker.model.HostInfo;
import com.vmware.flowgate.vcworker.model.HostNic;
import com.vmware.flowgate.vcworker.model.VCConstants;
import com.vmware.flowgate.vcworker.scheduler.job.VCDataService;
import com.vmware.vim.binding.vim.AboutInfo;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.ElementDescription;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.HostSystem.PowerState;
import com.vmware.vim.binding.vim.PerformanceManager;
import com.vmware.vim.binding.vim.PerformanceManager.CounterInfo;
import com.vmware.vim.binding.vim.PerformanceManager.EntityMetric;
import com.vmware.vim.binding.vim.PerformanceManager.IntSeries;
import com.vmware.vim.binding.vim.PerformanceManager.MetricId;
import com.vmware.vim.binding.vim.PerformanceManager.ProviderSummary;
import com.vmware.vim.binding.vim.PerformanceManager.SampleInfo;
import com.vmware.vim.binding.vim.ServiceInstanceContent;
import com.vmware.vim.binding.vim.cluster.ConfigInfoEx;
import com.vmware.vim.binding.vim.cluster.DpmConfigInfo;
import com.vmware.vim.binding.vim.cluster.DpmHostConfigInfo;
import com.vmware.vim.binding.vim.cluster.DrsConfigInfo;
import com.vmware.vim.binding.vim.cluster.DrsConfigInfo.DrsBehavior;
import com.vmware.vim.binding.vim.host.Capability;
import com.vmware.vim.binding.vim.host.ConfigInfo;
import com.vmware.vim.binding.vim.host.ConnectInfo;
import com.vmware.vim.binding.vim.host.ConnectInfo.DatastoreInfo;
import com.vmware.vim.binding.vim.host.NetworkInfo;
import com.vmware.vim.binding.vim.host.PhysicalNic;
import com.vmware.vim.binding.vim.host.PhysicalNic.LinkSpeedDuplex;
import com.vmware.vim.binding.vim.host.RuntimeInfo;
import com.vmware.vim.binding.vim.host.Summary;
import com.vmware.vim.binding.vim.host.Summary.ConfigSummary;
import com.vmware.vim.binding.vim.host.Summary.HardwareSummary;
import com.vmware.vim.binding.vim.host.Summary.QuickStats;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.client.Client;

import junit.framework.TestCase;

public class VCDataServiceTest {

   @Spy
   @InjectMocks
   VCDataService service;

   @Mock
   private WormholeAPIClient restClient;

   @Mock
   private ServiceKeyConfig serviceKeyConfig;

   @Mock
   private PerformanceManager performanceManager;

   @Mock
   private Client client;

   @Mock
   private ServiceInstanceContent sic;

   @Mock
   private VsphereClient vsphereClient;

   @Mock
   private StringRedisTemplate template;

   private ObjectMapper mapper = new ObjectMapper();

   @Before
   public void before() {
      MockitoAnnotations.initMocks(this);
   }

   @Test
   public void testGetPDUSwitchIDNamePortMapping() {
      Asset asset = new Asset();
      HashMap<String, String> justifications = new HashMap<String, String>();
      justifications.put("NETWORK_PORT_FOR_SERVER",
            "pci-2:hba:1_FIELDSPLIT_cloud-fc02-sha1_FIELDSPLIT_05_FIELDSPLIT_4b029b8337c64630b68d0f6c20a18e40,onboard:1gb-nic:4_FIELDSPLIT_cloud-sw02-sha1_FIELDSPLIT_08_FIELDSPLIT_3fc319e50d21476684d841aa0842bd52,pci-1:hba:1_FIELDSPLIT_cloud-fc01-sha1_FIELDSPLIT_05_FIELDSPLIT_5008de702d7f4a96af939609c5453ec5,ilo_FIELDSPLIT_cloud-sw03-sha1_FIELDSPLIT_02_FIELDSPLIT_e53c01312682455ab8c039780c88db6f,onboard:1gb-nic:1_FIELDSPLIT_cloud-sw01-sha1_FIELDSPLIT_07_FIELDSPLIT_3590c57182fe481d98d9ff647abaebc6,onboard:1gb-nic:3_FIELDSPLIT_cloud-sw01-sha1_FIELDSPLIT_08_FIELDSPLIT_3590c57182fe481d98d9ff647abaebc6,onboard:1gb-nic:2_FIELDSPLIT_cloud-sw02-sha1_FIELDSPLIT_07_FIELDSPLIT_3fc319e50d21476684d841aa0842bd52");
      asset.setJustificationfields(justifications);
      VCDataService service = new VCDataService();
      Map<String, String> nameMap = service.getPDUSwitchIDNamePortMapping(asset);
      Assert.assertTrue(nameMap.containsKey("e53c01312682455ab8c039780c88db6f"));
      Assert.assertTrue(
            nameMap.get("4b029b8337c64630b68d0f6c20a18e40").equals("cloud-fc02-sha1:05"));
      Assert.assertEquals(5, nameMap.size());
   }

   @Test
   public void testCheckAndUpdateIntegrationStatus() {

      SDDCSoftwareConfig vc = Mockito.spy(new SDDCSoftwareConfig());
      IntegrationStatus integrationStatus = Mockito.spy(new IntegrationStatus());
      String message = "message";

      vc.setIntegrationStatus(null);
      Mockito.doNothing().when(service).updateIntegrationStatus(any(SDDCSoftwareConfig.class));
      service.checkAndUpdateIntegrationStatus(vc, message);
      TestCase.assertEquals(1, vc.getIntegrationStatus().getRetryCounter());

      Mockito.when(vc.getIntegrationStatus()).thenReturn(integrationStatus);
      Mockito.when(integrationStatus.getRetryCounter())
            .thenReturn(FlowgateConstant.MAXNUMBEROFRETRIES);

      service.checkAndUpdateIntegrationStatus(vc, message);
      TestCase.assertEquals(IntegrationStatus.Status.ERROR, integrationStatus.getStatus());
      TestCase.assertEquals(message, integrationStatus.getDetail());

   }

   @Test
   public void testGetVaildServerMapping() {

      SDDCSoftwareConfig vc = Mockito.mock(SDDCSoftwareConfig.class);
      String serviceKey = "serviceKey";
      when(serviceKeyConfig.getServiceKey()).thenReturn(serviceKey);
      doNothing().when(restClient).setServiceKey(serviceKey);
      when(vc.getId()).thenReturn("id");
      ServerMapping mapping1 = new ServerMapping();
      mapping1.setVcMobID("vc1");
      mapping1.setAsset("asset1");
      ServerMapping mapping2 = new ServerMapping();
      mapping2.setVcMobID("vc2");
      mapping2.setAsset("asset2");
      ServerMapping[] mappings = { mapping1, mapping2 };
      ResponseEntity<ServerMapping[]> serverMappings =
            new ResponseEntity<ServerMapping[]>(mappings, HttpStatus.OK);
      when(restClient.getServerMappingsByVC(anyString())).thenReturn(serverMappings);

      Map<String, ServerMapping> mobIdDictionary = service.getVaildServerMapping(vc);
      TestCase.assertEquals(mapping1, mobIdDictionary.get(mapping1.getVcMobID()));
      TestCase.assertEquals(mapping2, mobIdDictionary.get(mapping2.getVcMobID()));
   }

   @Test
   public void testQueryHostMetaData() throws Exception {

      SDDCSoftwareConfig vc = Mockito.mock(SDDCSoftwareConfig.class);
      HashMap<String, ServerMapping> serverMappingMap = new HashMap<String, ServerMapping>();
      ServerMapping mapping1 = new ServerMapping();
      mapping1.setVcMobID("vc1");
      mapping1.setAsset("asset1");
      serverMappingMap.put(mapping1.getVcMobID(), mapping1);
      ServerMapping mapping2 = new ServerMapping();
      mapping2.setVcMobID("vc2");
      mapping2.setAsset("asset2");
      serverMappingMap.put(mapping2.getVcMobID(), mapping2);
      doReturn(serverMappingMap).when(service).getVaildServerMapping(any());
      doReturn(vsphereClient).when(service).connectVsphere(any());

      HostSystem host1 = Mockito.mock(HostSystem.class);
      HostSystem host2 = Mockito.mock(HostSystem.class);
      Collection<HostSystem> hosts = new ArrayList<>();
      hosts.add(host1);
      hosts.add(host2);
      when(vsphereClient.getAllHost()).thenReturn(hosts);

      Collection<ClusterComputeResource> clusterComputeResources = new ArrayList<>();
      ClusterComputeResource cluster = Mockito.mock(ClusterComputeResource.class);
      clusterComputeResources.add(cluster);
      clusterComputeResources.add(cluster);
      when(vsphereClient.getAllClusterComputeResource()).thenReturn(clusterComputeResources);

      ManagedObjectReference mor1 = Mockito.mock(ManagedObjectReference.class);
      ManagedObjectReference mor2 = Mockito.mock(ManagedObjectReference.class);
      when(host1._getRef()).thenReturn(mor1);
      when(mor1.getValue()).thenReturn("vc1");
      when(host2._getRef()).thenReturn(mor2);
      when(mor2.getValue()).thenReturn("vc2");

      Asset asset = new Asset();
      asset.setId("asset1");
      HashMap<String, String> hostJustification = new HashMap<String, String>();
      HostInfo hostInfo = new HostInfo();
      String vcHostObjStr = mapper.writeValueAsString(hostInfo);
      hostJustification.put(FlowgateConstant.HOST_METADATA, vcHostObjStr);
      asset.setJustificationfields(hostJustification);

      ResponseEntity<Asset> assets = Mockito.mock(ResponseEntity.class);
      when(restClient.getAssetByID(anyString())).thenReturn(assets);
      when(assets.getBody()).thenReturn(asset);

      doReturn(true).when(service).feedHostMetaData(any(), any());
      doReturn(true).when(service).feedClusterMetaData(any(), any(), any(), anyString());

      service.queryHostMetaData(vc);
   }

   @Test
   public void testQueryHostMetaDataForclustersIsNull() throws Exception {

      SDDCSoftwareConfig vc = Mockito.mock(SDDCSoftwareConfig.class);
      HashMap<String, ServerMapping> serverMappingMap = new HashMap<String, ServerMapping>();
      ServerMapping mapping1 = new ServerMapping();
      mapping1.setVcMobID("vc1");
      mapping1.setAsset("asset1");
      serverMappingMap.put(mapping1.getVcMobID(), mapping1);
      ServerMapping mapping2 = new ServerMapping();
      mapping2.setVcMobID("vc2");
      mapping2.setAsset("asset2");
      serverMappingMap.put(mapping2.getVcMobID(), mapping2);
      doReturn(serverMappingMap).when(service).getVaildServerMapping(any());
      doReturn(vsphereClient).when(service).connectVsphere(any());

      HostSystem host1 = Mockito.mock(HostSystem.class);
      HostSystem host2 = Mockito.mock(HostSystem.class);
      Collection<HostSystem> hosts = new ArrayList<>();
      hosts.add(host1);
      hosts.add(host2);
      when(vsphereClient.getAllHost()).thenReturn(hosts);

      when(vsphereClient.getAllClusterComputeResource()).thenReturn(null);

      ManagedObjectReference mor1 = Mockito.mock(ManagedObjectReference.class);
      ManagedObjectReference mor2 = Mockito.mock(ManagedObjectReference.class);
      when(host1._getRef()).thenReturn(mor1);
      when(mor1.getValue()).thenReturn("vc1");
      when(host2._getRef()).thenReturn(mor2);
      when(mor2.getValue()).thenReturn("vc2");

      Asset asset = new Asset();
      asset.setId("asset1");
      HashMap<String, String> hostJustification = new HashMap<String, String>();
      HostInfo hostInfo = new HostInfo();
      String vcHostObjStr = mapper.writeValueAsString(hostInfo);
      hostJustification.put(FlowgateConstant.HOST_METADATA, vcHostObjStr);
      asset.setJustificationfields(hostJustification);

      ResponseEntity<Asset> assets = Mockito.mock(ResponseEntity.class);
      when(restClient.getAssetByID(anyString())).thenReturn(assets);
      when(assets.getBody()).thenReturn(asset);

      doReturn(true).when(service).feedHostMetaData(any(), any());
      doReturn(true).when(service).feedClusterMetaData(any(), any(), any(), anyString());

      service.queryHostMetaData(vc);
   }

   @Test
   public void testFeedHostMetaData() {

      HostSystem host = Mockito.mock(HostSystem.class);

      Capability capability = Mockito.mock(Capability.class);
      when(host.getCapability()).thenReturn(capability);
      when(capability.isMaintenanceModeSupported()).thenReturn(true);
      when(capability.isRebootSupported()).thenReturn(true);
      when(capability.getMaxRunningVMs()).thenReturn(10);
      when(capability.getMaxSupportedVcpus()).thenReturn(10);
      when(capability.getMaxRegisteredVMs()).thenReturn(10);

      RuntimeInfo runtimeInfo = Mockito.mock(RuntimeInfo.class);
      when(host.getRuntime()).thenReturn(runtimeInfo);
      when(runtimeInfo.getConnectionState()).thenReturn(ConnectionState.connected);
      when(runtimeInfo.getPowerState()).thenReturn(PowerState.poweredOff);
      Calendar time = Mockito.mock(Calendar.class);
      when(runtimeInfo.getBootTime()).thenReturn(time);

      Summary summary = Mockito.mock(Summary.class);
      when(host.getSummary()).thenReturn(summary);
      when(summary.isRebootRequired()).thenReturn(true);

      QuickStats quickStats = Mockito.mock(QuickStats.class);
      when(summary.getQuickStats()).thenReturn(quickStats);
      when(quickStats.getUptime()).thenReturn(10);

      AboutInfo aboutInfo = Mockito.mock(AboutInfo.class);
      ConfigSummary configSummary = Mockito.mock(ConfigSummary.class);
      when(summary.getConfig()).thenReturn(configSummary);
      when(configSummary.getProduct()).thenReturn(aboutInfo);
      when(aboutInfo.getBuild()).thenReturn("build");
      when(aboutInfo.getFullName()).thenReturn("fullname");
      when(aboutInfo.getLicenseProductName()).thenReturn("productname");
      when(aboutInfo.getVersion()).thenReturn("version");
      when(aboutInfo.getLicenseProductVersion()).thenReturn("productversion");

      HardwareSummary hardwareSummary = Mockito.mock(HardwareSummary.class);
      when(summary.getHardware()).thenReturn(hardwareSummary);
      when(hardwareSummary.getModel()).thenReturn("model");
      when(hardwareSummary.getVendor()).thenReturn("model");
      when(hardwareSummary.getNumCpuCores()).thenReturn((short) 2);
      when(hardwareSummary.getNumCpuPkgs()).thenReturn((short) 2);
      when(hardwareSummary.getNumCpuThreads()).thenReturn((short) 2);
      when(hardwareSummary.getCpuMhz()).thenReturn(2);
      when(hardwareSummary.getMemorySize()).thenReturn(2L);

      NetworkInfo networkInfo = Mockito.mock(NetworkInfo.class);
      ConfigInfo configInfo = Mockito.mock(ConfigInfo.class);
      when(host.getConfig()).thenReturn(configInfo);
      when(configInfo.getNetwork()).thenReturn(networkInfo);

      LinkSpeedDuplex linkSpeedDuplex = Mockito.mock(LinkSpeedDuplex.class);
      when(linkSpeedDuplex.isDuplex()).thenReturn(true);
      when(linkSpeedDuplex.getSpeedMb()).thenReturn(2);


      PhysicalNic[] physicalNics = new PhysicalNic[2];
      physicalNics[0] = Mockito.mock(PhysicalNic.class);
      when(physicalNics[0].getMac()).thenReturn("mac");
      when(physicalNics[0].getDriver()).thenReturn("driver");
      when(physicalNics[0].getLinkSpeed()).thenReturn(linkSpeedDuplex);
      when(physicalNics[0].getDevice()).thenReturn("device");

      physicalNics[1] = Mockito.mock(PhysicalNic.class);
      when(physicalNics[1].getMac()).thenReturn("mac1");
      when(physicalNics[1].getDriver()).thenReturn("driver1");
      when(physicalNics[1].getLinkSpeed()).thenReturn(linkSpeedDuplex);
      when(physicalNics[1].getDevice()).thenReturn("device1");
      when(networkInfo.getPnic()).thenReturn(physicalNics);

      ConnectInfo connectInfo = Mockito.mock(ConnectInfo.class);
      DatastoreInfo[] datastores =
            { Mockito.mock(DatastoreInfo.class), Mockito.mock(DatastoreInfo.class) };

      com.vmware.vim.binding.vim.Datastore.Summary connectInfoSummary =
            Mockito.mock(com.vmware.vim.binding.vim.Datastore.Summary.class);
      when(connectInfoSummary.getCapacity()).thenReturn(2L);
      when(datastores[0].getSummary()).thenReturn(connectInfoSummary);
      ManagedObjectReference mor1 = Mockito.mock(ManagedObjectReference.class);
      when(datastores[1].getSummary()).thenReturn(connectInfoSummary);
      when(connectInfoSummary.getDatastore()).thenReturn(mor1);
      when(mor1.getValue()).thenReturn("datastoreid");

      when(host.queryConnectionInfo()).thenReturn(connectInfo);
      when(connectInfo.getDatastore()).thenReturn(datastores);

      com.vmware.vim.binding.vim.vsan.host.ConfigInfo vsanConfigInfo =
            Mockito.mock(com.vmware.vim.binding.vim.vsan.host.ConfigInfo.class);
      when(configInfo.getVsanHostConfig()).thenReturn(vsanConfigInfo);
      when(vsanConfigInfo.getEnabled()).thenReturn(true);

      when(capability.getVsanSupported()).thenReturn(true);

      Collection<ClusterComputeResource> clusterComputeResources = new ArrayList<>();
      ClusterComputeResource cluster = Mockito.mock(ClusterComputeResource.class);
      clusterComputeResources.add(cluster);
      clusterComputeResources.add(cluster);
      when(vsphereClient.getAllClusterComputeResource()).thenReturn(clusterComputeResources);

      ManagedObjectReference[] mors = new ManagedObjectReference[2];
      mors[0] = Mockito.mock(ManagedObjectReference.class);
      mors[1] = Mockito.mock(ManagedObjectReference.class);
      when(mors[0].getValue()).thenReturn("hostMobId");
      when(mors[1].getValue()).thenReturn("hostMobId");

      when(cluster.getHost()).thenReturn(mors);

      ConfigInfoEx ci = Mockito.mock(ConfigInfoEx.class);
      when(cluster.getConfigurationEx()).thenReturn(ci);
      when(cluster.getName()).thenReturn("cluster");
      DpmConfigInfo dpmConfigInfo = Mockito.mock(DpmConfigInfo.class);
      when(dpmConfigInfo.getEnabled()).thenReturn(true);
      when(ci.getDpmConfigInfo()).thenReturn(dpmConfigInfo);

      DrsConfigInfo drsConfigInfo = Mockito.mock(DrsConfigInfo.class);
      when(drsConfigInfo.getDefaultVmBehavior()).thenReturn(DrsBehavior.fullyAutomated);
      when(ci.getDrsConfig()).thenReturn(drsConfigInfo);
      com.vmware.vim.binding.vim.ComputeResource.Summary computeResourceSummary =
            Mockito.mock(com.vmware.vim.binding.vim.ComputeResource.Summary.class);
      when(computeResourceSummary.getNumEffectiveHosts()).thenReturn(2);
      when(computeResourceSummary.getNumHosts()).thenReturn(2);
      when(computeResourceSummary.getTotalCpu()).thenReturn(2);
      when(computeResourceSummary.getNumCpuCores()).thenReturn((short) 2);
      when(computeResourceSummary.getNumCpuThreads()).thenReturn((short) 2);
      when(computeResourceSummary.getTotalMemory()).thenReturn(2L);
      when(cluster.getSummary()).thenReturn(computeResourceSummary);
      com.vmware.vim.binding.vim.vsan.cluster.ConfigInfo clusterConfigInfo =
            Mockito.mock(com.vmware.vim.binding.vim.vsan.cluster.ConfigInfo.class);
      when(ci.getVsanConfigInfo()).thenReturn(clusterConfigInfo);
      when(clusterConfigInfo.getEnabled()).thenReturn(true);

      DpmHostConfigInfo[] dpmHostConfigInfos = new DpmHostConfigInfo[2];
      dpmHostConfigInfos[0] = Mockito.mock(DpmHostConfigInfo.class);
      dpmHostConfigInfos[1] = Mockito.mock(DpmHostConfigInfo.class);
      ManagedObjectReference mor2 = Mockito.mock(ManagedObjectReference.class);
      when(dpmHostConfigInfos[0].getKey()).thenReturn(mor2);
      when(mor2.getValue()).thenReturn("hostMobId");
      when(dpmHostConfigInfos[1].getKey()).thenReturn(mor2);
      when(mor2.getValue()).thenReturn("hostMobId");

      when(ci.getDpmHostConfig()).thenReturn(dpmHostConfigInfos);
      ManagedObjectReference mor3 = Mockito.mock(ManagedObjectReference.class);
      when(cluster._getRef()).thenReturn(mor3);
      when(mor3.getValue()).thenReturn("clusterMobid");
      ManagedObjectReference mor4 = Mockito.mock(ManagedObjectReference.class);
      when(cluster.getParent()).thenReturn(mor4);
      when(mor4.getValue()).thenReturn("InstanceId");

      when(host.getName()).thenReturn("hostname");

      HostInfo hostInfo = new HostInfo();
      List<HostNic> hostNics = new ArrayList<HostNic>();
      HostNic hostNic1 = new HostNic();
      hostNic1.setDriver("driver1");
      hostNic1.setMacAddress("mac1");
      hostNic1.setDuplex(true);
      hostNic1.setLinkSpeedMb(2);
      hostNic1.setName("nic1");
      HostNic hostNic2 = new HostNic();
      hostNic2.setDriver("driver2");
      hostNic2.setMacAddress("mac2");
      hostNic2.setDuplex(true);
      hostNic2.setLinkSpeedMb(2);
      hostNic2.setName("nic2");
      hostNics.add(hostNic1);
      hostNics.add(hostNic2);
      hostInfo.setHostNics(hostNics);

      boolean needUpdate = service.feedHostMetaData(host, hostInfo);
      TestCase.assertEquals(needUpdate, true);
   }

   @Test
   public void testFeedClusterMetaData() {

      HostSystem host = Mockito.mock(HostSystem.class);

      HostInfo hostInfo = new HostInfo();
      EsxiMetadata esxiMetadata = new EsxiMetadata();
      esxiMetadata.setClusterDPMenabled(true);
      esxiMetadata.setClusterDRSBehavior("clusterDRSBehavior");
      esxiMetadata.setClusterEffectiveHostsNum(2);
      esxiMetadata.setClusterHostsNum(2);
      esxiMetadata.setClusterMobid("clusterMobid");
      esxiMetadata.setClusterName("cluster");
      esxiMetadata.setClusterTotalCpu(2);
      esxiMetadata.setClusterTotalCpuCores(2);
      esxiMetadata.setClusterTotalCpuThreads(2);
      esxiMetadata.setClusterTotalMemory(2);
      esxiMetadata.setClusterVSANenabled(true);
      esxiMetadata.setHostDPMenabled(true);
      esxiMetadata.setHostMobid("hostMobid");
      esxiMetadata.setHostName("hostName");
      esxiMetadata.setHostVSANenabled(true);
      esxiMetadata.setHostVsanSupported(true);
      esxiMetadata.setInstanceId("instanceId");
      hostInfo.setEsxiMetadata(esxiMetadata);

      ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
      when(host.getParent()).thenReturn(mor);
      when(mor.getValue()).thenReturn("cluster");
      when(mor.getType()).thenReturn("ClusterComputeResource");

      when(host._getRef()).thenReturn(mor);
      when(mor.getValue()).thenReturn("hostMobId");

      HashMap<String, ClusterComputeResource> clusterMap = Mockito.mock(HashMap.class);
      ClusterComputeResource cluster = Mockito.mock(ClusterComputeResource.class);
      when(clusterMap.get(anyString())).thenReturn(cluster);

      clusterMap.put("cluster", cluster);
      ConfigInfoEx ci = Mockito.mock(ConfigInfoEx.class);
      when(cluster.getConfigurationEx()).thenReturn(ci);

      when(cluster.getName()).thenReturn("cluster");
      DpmConfigInfo dci = Mockito.mock(DpmConfigInfo.class);
      when(ci.getDpmConfigInfo()).thenReturn(dci);
      when(dci.getEnabled()).thenReturn(true);

      DrsConfigInfo drsCI = Mockito.mock(DrsConfigInfo.class);
      when(ci.getDrsConfig()).thenReturn(drsCI);
      when(drsCI.getDefaultVmBehavior()).thenReturn(DrsBehavior.fullyAutomated);

      com.vmware.vim.binding.vim.ComputeResource.Summary summary =
            Mockito.mock(com.vmware.vim.binding.vim.ComputeResource.Summary.class);
      when(cluster.getSummary()).thenReturn(summary);
      when(summary.getNumEffectiveHosts()).thenReturn(2);
      when(summary.getNumHosts()).thenReturn(2);
      when(summary.getNumCpuCores()).thenReturn((short) 2);
      when(summary.getTotalMemory()).thenReturn(2L);
      when(summary.getNumCpuThreads()).thenReturn((short) 2);
      when(summary.getTotalCpu()).thenReturn(2);

      com.vmware.vim.binding.vim.vsan.cluster.ConfigInfo configInfo =
            Mockito.mock(com.vmware.vim.binding.vim.vsan.cluster.ConfigInfo.class);
      when(ci.getVsanConfigInfo()).thenReturn(configInfo);
      when(configInfo.getEnabled()).thenReturn(true);

      DpmHostConfigInfo[] dpmHostConfigInfos = new DpmHostConfigInfo[2];
      dpmHostConfigInfos[0] = Mockito.mock(DpmHostConfigInfo.class);
      dpmHostConfigInfos[1] = Mockito.mock(DpmHostConfigInfo.class);
      ManagedObjectReference mor2 = Mockito.mock(ManagedObjectReference.class);
      when(dpmHostConfigInfos[0].getKey()).thenReturn(mor2);
      when(mor2.getValue()).thenReturn("hostMobId");
      when(dpmHostConfigInfos[0].getEnabled()).thenReturn(true);
      when(dpmHostConfigInfos[1].getKey()).thenReturn(mor2);
      when(mor2.getValue()).thenReturn("hostMobId");
      when(dpmHostConfigInfos[1].getEnabled()).thenReturn(true);

      when(ci.getDpmHostConfig()).thenReturn(dpmHostConfigInfos);

      ManagedObjectReference mor3 = Mockito.mock(ManagedObjectReference.class);
      when(cluster._getRef()).thenReturn(mor3);
      when(mor3.getValue()).thenReturn("cluster");
      ManagedObjectReference mor4 = Mockito.mock(ManagedObjectReference.class);
      when(cluster.getParent()).thenReturn(mor4);
      when(mor4.getValue()).thenReturn("clusterInstance");
      when(host.getName()).thenReturn("host");

      ConfigInfo configInfo1 = Mockito.mock(ConfigInfo.class);
      when(host.getConfig()).thenReturn(configInfo1);
      com.vmware.vim.binding.vim.vsan.host.ConfigInfo hostConfig =
            Mockito.mock(com.vmware.vim.binding.vim.vsan.host.ConfigInfo.class);
      when(configInfo1.getVsanHostConfig()).thenReturn(hostConfig);
      when(hostConfig.getEnabled()).thenReturn(true);
      Capability hostCapability = Mockito.mock(Capability.class);
      when(host.getCapability()).thenReturn(hostCapability);
      when(hostCapability.getVsanSupported()).thenReturn(true);

      boolean needUpdate =
            service.feedClusterMetaData(clusterMap, host, hostInfo, "vcInstanceUUID");
      TestCase.assertEquals(needUpdate, true);
   }

   @Test
   public void testFeedHostUsageDataNotAvailableMetricId() {
      PerformanceManager performanceManager = Mockito.mock(PerformanceManager.class);
      when(vsphereClient.getPerformanceManager()).thenReturn(performanceManager);

      CounterInfo[] counterInfos = new CounterInfo[4];
      counterInfos[0] = Mockito.mock(CounterInfo.class);
      when(counterInfos[0].getKey()).thenReturn(1);
      ElementDescription edGroup = Mockito.mock(ElementDescription.class);
      when(counterInfos[0].getGroupInfo()).thenReturn(edGroup);
      when(edGroup.getKey()).thenReturn(VCConstants.HOST_CPU_GROUP);
      ElementDescription edName = Mockito.mock(ElementDescription.class);
      when(counterInfos[0].getNameInfo()).thenReturn(edName);
      when(edName.getKey()).thenReturn(VCConstants.HOST_METRIC_USAGE);
      counterInfos[1] = Mockito.mock(CounterInfo.class);
      when(counterInfos[1].getKey()).thenReturn(2);
      ElementDescription edGroup1 = Mockito.mock(ElementDescription.class);
      when(counterInfos[1].getGroupInfo()).thenReturn(edGroup1);
      when(edGroup1.getKey()).thenReturn(VCConstants.HOST_MEMORY_GROUP);
      ElementDescription edName1 = Mockito.mock(ElementDescription.class);
      when(counterInfos[1].getNameInfo()).thenReturn(edName1);
      when(edName1.getKey()).thenReturn(VCConstants.HOST_METRIC_USAGE);
      counterInfos[2] = Mockito.mock(CounterInfo.class);
      when(counterInfos[2].getKey()).thenReturn(3);
      ElementDescription edGroup2 = Mockito.mock(ElementDescription.class);
      when(counterInfos[2].getGroupInfo()).thenReturn(edGroup2);
      when(edGroup2.getKey()).thenReturn(VCConstants.HOST_POWER_GROUP);
      ElementDescription edName2 = Mockito.mock(ElementDescription.class);
      when(counterInfos[2].getNameInfo()).thenReturn(edName2);
      when(edName2.getKey()).thenReturn(VCConstants.HOST_METRIC_POWER_ENERGY);
      counterInfos[3] = Mockito.mock(CounterInfo.class);
      when(counterInfos[3].getKey()).thenReturn(4);
      ElementDescription edGroup3 = Mockito.mock(ElementDescription.class);
      when(counterInfos[3].getGroupInfo()).thenReturn(edGroup3);
      when(edGroup3.getKey()).thenReturn(VCConstants.HOST_POWER_GROUP);
      ElementDescription edName3 = Mockito.mock(ElementDescription.class);
      when(counterInfos[3].getNameInfo()).thenReturn(edName3);
      when(edName3.getKey()).thenReturn(VCConstants.HOST_METRIC_POWER_POWER);

      when(performanceManager.getPerfCounter()).thenReturn(counterInfos);
      HostSystem host = Mockito.mock(HostSystem.class);
      ManagedObjectReference managedObjectReference = host._getRef();
      when(performanceManager.queryAvailableMetric(managedObjectReference, null, null, new Integer(20))).thenReturn(null);
      service.feedHostUsageData(vsphereClient, "avbaebhqw9", managedObjectReference);
   }

   @Test
   public void testQueryHostMetrics() throws Exception {

      SDDCSoftwareConfig vc = Mockito.mock(SDDCSoftwareConfig.class);
      HashMap<String, ServerMapping> serverMappingMap = new HashMap<String, ServerMapping>();
      ServerMapping mapping1 = new ServerMapping();
      mapping1.setVcMobID("vc1");
      mapping1.setAsset("asset1");
      serverMappingMap.put(mapping1.getVcMobID(), mapping1);
      ServerMapping mapping2 = new ServerMapping();
      mapping2.setVcMobID("vc2");
      mapping2.setAsset("asset2");
      serverMappingMap.put(mapping2.getVcMobID(), mapping2);
      doReturn(serverMappingMap).when(service).getVaildServerMapping(any());
      doReturn(vsphereClient).when(service).connectVsphere(any());

      HostSystem host1 = Mockito.mock(HostSystem.class);
      HostSystem host2 = Mockito.mock(HostSystem.class);
      Collection<HostSystem> hosts = new ArrayList<>();
      hosts.add(host1);
      hosts.add(host2);
      when(vsphereClient.getAllHost()).thenReturn(hosts);

      ManagedObjectReference mor1 = Mockito.mock(ManagedObjectReference.class);
      ManagedObjectReference mor2 = Mockito.mock(ManagedObjectReference.class);
      when(host1._getRef()).thenReturn(mor1);
      when(mor1.getValue()).thenReturn("vc1");
      when(host2._getRef()).thenReturn(mor2);
      when(mor2.getValue()).thenReturn("vc2");

      Asset asset = new Asset();
      asset.setId("asset1");
      Map<String, String>  metricsFormulars = new HashMap<>();
      Map<String, String> metrics = new HashMap<>();
      metricsFormulars.put(FlowgateConstant.HOST_METRICS, mapper.writeValueAsString(metrics));
      asset.setMetricsformulars(metricsFormulars);

      ResponseEntity<Asset> assets = Mockito.mock(ResponseEntity.class);
      when(restClient.getAssetByID(anyString())).thenReturn(assets);
      when(assets.getBody()).thenReturn(asset);

      doNothing().when(service).feedHostUsageData(any(), any(), any());

      service.queryHostMetrics(vc);
   }

   @Test
   public void testFeedHostUsageData() {

      Asset hostMappingAsset = Mockito.mock(Asset.class);
      HostSystem host = Mockito.mock(HostSystem.class);

      when(hostMappingAsset.getId()).thenReturn("hostid");
      PerformanceManager performanceManager = Mockito.mock(PerformanceManager.class);
      when(vsphereClient.getPerformanceManager()).thenReturn(performanceManager);

      CounterInfo[] counterInfos = new CounterInfo[4];
      counterInfos[0] = Mockito.mock(CounterInfo.class);
      when(counterInfos[0].getKey()).thenReturn(1);
      ElementDescription edGroup = Mockito.mock(ElementDescription.class);
      when(counterInfos[0].getGroupInfo()).thenReturn(edGroup);
      when(edGroup.getKey()).thenReturn(VCConstants.HOST_CPU_GROUP);
      ElementDescription edName = Mockito.mock(ElementDescription.class);
      when(counterInfos[0].getNameInfo()).thenReturn(edName);
      when(edName.getKey()).thenReturn(VCConstants.HOST_METRIC_USAGE);

      counterInfos[1] = Mockito.mock(CounterInfo.class);
      when(counterInfos[1].getKey()).thenReturn(2);
      ElementDescription edGroup1 = Mockito.mock(ElementDescription.class);
      when(counterInfos[1].getGroupInfo()).thenReturn(edGroup1);
      when(edGroup1.getKey()).thenReturn(VCConstants.HOST_MEMORY_GROUP);
      ElementDescription edName1 = Mockito.mock(ElementDescription.class);
      when(counterInfos[1].getNameInfo()).thenReturn(edName1);
      when(edName1.getKey()).thenReturn(VCConstants.HOST_METRIC_USAGE);

      counterInfos[2] = Mockito.mock(CounterInfo.class);
      when(counterInfos[2].getKey()).thenReturn(3);
      ElementDescription edGroup2 = Mockito.mock(ElementDescription.class);
      when(counterInfos[2].getGroupInfo()).thenReturn(edGroup2);
      when(edGroup2.getKey()).thenReturn(VCConstants.HOST_POWER_GROUP);
      ElementDescription edName2 = Mockito.mock(ElementDescription.class);
      when(counterInfos[2].getNameInfo()).thenReturn(edName2);
      when(edName2.getKey()).thenReturn(VCConstants.HOST_METRIC_POWER_ENERGY);

      counterInfos[3] = Mockito.mock(CounterInfo.class);
      when(counterInfos[3].getKey()).thenReturn(4);
      ElementDescription edGroup3 = Mockito.mock(ElementDescription.class);
      when(counterInfos[3].getGroupInfo()).thenReturn(edGroup3);
      when(edGroup3.getKey()).thenReturn(VCConstants.HOST_POWER_GROUP);
      ElementDescription edName3 = Mockito.mock(ElementDescription.class);
      when(counterInfos[3].getNameInfo()).thenReturn(edName3);
      when(edName3.getKey()).thenReturn(VCConstants.HOST_METRIC_POWER_POWER);

      when(performanceManager.getPerfCounter()).thenReturn(counterInfos);

      MetricId[] metrics = new MetricId[4];
      metrics[0] = Mockito.mock(MetricId.class);
      metrics[1] = Mockito.mock(MetricId.class);
      metrics[2] = Mockito.mock(MetricId.class);
      metrics[3] = Mockito.mock(MetricId.class);
      when(metrics[0].getCounterId()).thenReturn(1);
      when(metrics[1].getCounterId()).thenReturn(2);
      when(metrics[2].getCounterId()).thenReturn(3);
      when(metrics[3].getCounterId()).thenReturn(4);
      when(metrics[0].getInstance()).thenReturn(null);
      when(metrics[1].getInstance()).thenReturn(null);
      when(metrics[2].getInstance()).thenReturn(null);
      when(metrics[3].getInstance()).thenReturn(null);

      when(performanceManager.queryAvailableMetric(host._getRef(), null, null, new Integer(20))).thenReturn(metrics);

      ProviderSummary summary = Mockito.mock(ProviderSummary.class);
      when(summary.getRefreshRate()).thenReturn(1);
      when(performanceManager.queryProviderSummary(host._getRef())).thenReturn(summary);

      EntityMetric[] metricBase = new EntityMetric[4];
      metricBase[0] = Mockito.mock(EntityMetric.class);
      long[] values = {1L, 2L, 3L, 4L};

      IntSeries[] metricSerieses1 = new IntSeries[4];
      metricSerieses1[0] = Mockito.mock(IntSeries.class);
      MetricId metricID = Mockito.mock(MetricId.class);
      when(metricSerieses1[0].getId()).thenReturn(metricID);
      when(metricID.getCounterId()).thenReturn(1);

      IntSeries intSeries1 = Mockito.mock(IntSeries.class);
      intSeries1 = metricSerieses1[0];
      when((intSeries1.getValue())).thenReturn(values);
      when(metricID.getCounterId()).thenReturn(1);

      metricSerieses1[1] = Mockito.mock(IntSeries.class);
      when(metricSerieses1[1].getId()).thenReturn(metricID);
      when(metricID.getCounterId()).thenReturn(1);

      IntSeries intSeries2 = Mockito.mock(IntSeries.class);
      intSeries2 = metricSerieses1[1];
      when(intSeries2.getValue()).thenReturn(values);

      MetricId metricID2 = Mockito.mock(MetricId.class);
      metricSerieses1[2] = Mockito.mock(IntSeries.class);
      when(metricSerieses1[2].getId()).thenReturn(metricID2);
      when(metricID2.getCounterId()).thenReturn(3);

      IntSeries intSeries3 = Mockito.mock(IntSeries.class);
      intSeries3 = metricSerieses1[2];
      when(intSeries3.getValue()).thenReturn(values);


      MetricId metricID3 = Mockito.mock(MetricId.class);
      metricSerieses1[3] = Mockito.mock(IntSeries.class);
      when(metricSerieses1[3].getId()).thenReturn(metricID3);
      when(metricID3.getCounterId()).thenReturn(4);
      IntSeries intSeries4 = Mockito.mock(IntSeries.class);
      intSeries4 = metricSerieses1[3];
      when(intSeries4.getValue()).thenReturn(values);
      when(metricBase[0].getValue()).thenReturn(metricSerieses1);

      SampleInfo[] sampleInfos1 = new SampleInfo[4];
      sampleInfos1[0] = Mockito.mock(SampleInfo.class);
      Calendar time = Calendar.getInstance();
      when(sampleInfos1[0].getTimestamp()).thenReturn(time);

      sampleInfos1[1] = Mockito.mock(SampleInfo.class);
      when(sampleInfos1[1].getTimestamp()).thenReturn(time);

      sampleInfos1[2] = Mockito.mock(SampleInfo.class);
      when(sampleInfos1[2].getTimestamp()).thenReturn(time);

      sampleInfos1[3] = Mockito.mock(SampleInfo.class);
      when(sampleInfos1[3].getTimestamp()).thenReturn(time);

      when(metricBase[0].getSampleInfo()).thenReturn(sampleInfos1);

      metricBase[1] = Mockito.mock(EntityMetric.class);
      when(metricBase[1].getValue()).thenReturn(metricSerieses1);
      when(metricBase[1].getSampleInfo()).thenReturn(sampleInfos1);

      metricBase[2] = Mockito.mock(EntityMetric.class);
      when(metricBase[2].getValue()).thenReturn(metricSerieses1);
      when(metricBase[2].getSampleInfo()).thenReturn(sampleInfos1);

      metricBase[3] = Mockito.mock(EntityMetric.class);
      when(metricBase[3].getValue()).thenReturn(metricSerieses1);
      when(metricBase[3].getSampleInfo()).thenReturn(sampleInfos1);

      when(performanceManager.queryStats(any())).thenReturn(metricBase);

      service.feedHostUsageData(vsphereClient, hostMappingAsset.getId(), host._getRef());
   }

   @Test
   public void testGetPerformanceManager() {
      ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
      when(sic.getPerfManager()).thenReturn(mor);
      when(client.createStub(PerformanceManager.class, mor)).thenReturn(performanceManager);
      vsphereClient.getPerformanceManager();
   }

   @Test
   public void testGetStatisticsValueUnit() {
      List<ValueUnit> valueUnits = new ArrayList<>(15);
      ValueUnit valueUnit1 = new ValueUnit();
      valueUnit1.setKey(MetricName.SERVER_POWER);
      valueUnit1.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit1.setTime(1615563920000L);
      valueUnit1.setValueNum(0.069);
      ValueUnit valueUnit2 = new ValueUnit();
      valueUnit2.setKey(MetricName.SERVER_POWER);
      valueUnit2.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit2.setTime(1615563940000L);
      valueUnit2.setValueNum(0.068);
      ValueUnit valueUnit3 = new ValueUnit();
      valueUnit3.setKey(MetricName.SERVER_POWER);
      valueUnit3.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit3.setTime(1615563960000L);
      valueUnit3.setValueNum(0.070);
      ValueUnit valueUnit4 = new ValueUnit();
      valueUnit4.setKey(MetricName.SERVER_POWER);
      valueUnit4.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit4.setTime(1615563980000L);
      valueUnit4.setValueNum(0.071);
      ValueUnit valueUnit5 = new ValueUnit();
      valueUnit5.setKey(MetricName.SERVER_POWER);
      valueUnit5.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit5.setTime(1615564000000L);
      valueUnit5.setValueNum(0.074);
      ValueUnit valueUnit6 = new ValueUnit();
      valueUnit6.setKey(MetricName.SERVER_POWER);
      valueUnit6.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit6.setTime(1615564020000L);
      valueUnit6.setValueNum(0.069);
      ValueUnit valueUnit7 = new ValueUnit();
      valueUnit7.setKey(MetricName.SERVER_POWER);
      valueUnit7.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit7.setTime(1615564040000L);
      valueUnit7.setValueNum(0.069);
      ValueUnit valueUnit8 = new ValueUnit();
      valueUnit8.setKey(MetricName.SERVER_POWER);
      valueUnit8.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit8.setTime(1615564060000L);
      valueUnit8.setValueNum(0.075);
      ValueUnit valueUnit9 = new ValueUnit();
      valueUnit9.setKey(MetricName.SERVER_POWER);
      valueUnit9.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit9.setTime(1615564080000L);
      valueUnit9.setValueNum(0.069);
      ValueUnit valueUnit10 = new ValueUnit();
      valueUnit10.setKey(MetricName.SERVER_POWER);
      valueUnit10.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit10.setTime(1615564100000L);
      valueUnit10.setValueNum(0.069);
      ValueUnit valueUnit11 = new ValueUnit();
      valueUnit11.setKey(MetricName.SERVER_POWER);
      valueUnit11.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit11.setTime(1615564120000L);
      valueUnit11.setValueNum(0.070);
      ValueUnit valueUnit12 = new ValueUnit();
      valueUnit12.setKey(MetricName.SERVER_POWER);
      valueUnit12.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit12.setTime(1615564140000L);
      valueUnit12.setValueNum(0.069);
      ValueUnit valueUnit13 = new ValueUnit();
      valueUnit13.setKey(MetricName.SERVER_POWER);
      valueUnit13.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit13.setTime(1615564160000L);
      valueUnit13.setValueNum(0.069);
      ValueUnit valueUnit14 = new ValueUnit();
      valueUnit14.setKey(MetricName.SERVER_POWER);
      valueUnit14.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit14.setTime(1615564180000L);
      valueUnit14.setValueNum(0.069);
      ValueUnit valueUnit15 = new ValueUnit();
      valueUnit15.setKey(MetricName.SERVER_POWER);
      valueUnit15.setUnit(ValueUnit.MetricUnit.kW.name());
      valueUnit15.setTime(1615564200000L);
      valueUnit15.setValueNum(0.065);
      valueUnits.add(valueUnit1);
      valueUnits.add(valueUnit2);
      valueUnits.add(valueUnit3);
      valueUnits.add(valueUnit4);
      valueUnits.add(valueUnit5);
      valueUnits.add(valueUnit6);
      valueUnits.add(valueUnit7);
      valueUnits.add(valueUnit8);
      valueUnits.add(valueUnit9);
      valueUnits.add(valueUnit10);
      valueUnits.add(valueUnit11);
      valueUnits.add(valueUnit12);
      valueUnits.add(valueUnit13);
      valueUnits.add(valueUnit14);
      valueUnits.add(valueUnit15);
      List<ValueUnit> statisticsValueUnit = service.getMinMaxAvgValueUnit(valueUnits);
      for (ValueUnit valueUnit : statisticsValueUnit) {
         if (MetricName.SERVER_PEAK_USED_POWER.equals(valueUnit.getKey())) {
            TestCase.assertEquals(0.075, valueUnit.getValueNum());
            TestCase.assertEquals(1615564200000L, valueUnit.getTime());
            TestCase.assertEquals("1615563920000_FIELDSPLIT_1615564060000", valueUnit.getExtraidentifier());
         } else if (MetricName.SERVER_MINIMUM_USED_POWER.equals(valueUnit.getKey())) {
            TestCase.assertEquals(0.065, valueUnit.getValueNum());
            TestCase.assertEquals(1615564200000L, valueUnit.getTime());
            TestCase.assertEquals("1615563920000_FIELDSPLIT_1615564200000", valueUnit.getExtraidentifier());
         } else if (MetricName.SERVER_AVERAGE_USED_POWER.equals(valueUnit.getKey())) {
            TestCase.assertEquals(0.06966666666666667, valueUnit.getValueNum());
            TestCase.assertEquals(1615564200000L, valueUnit.getTime());
            TestCase.assertEquals("1615563920000", valueUnit.getExtraidentifier());
         }
      }
   }

   @Test
   public void testSyncCustomerAttrsData() throws Exception {
      String assetId = "QONVN1098G1NVN01NG01";
      String vcMobID = "host-11";

      SDDCSoftwareConfig sddcSoftwareConfig = new SDDCSoftwareConfig();
      sddcSoftwareConfig.setType(SDDCSoftwareConfig.SoftwareType.VCENTER);
      sddcSoftwareConfig.setServerURL("https://1.1.1.1");
      sddcSoftwareConfig.setPassword("ASDFGAGAHAHwegqhwrjw");
      sddcSoftwareConfig.setVerifyCert(false);
      EventMessage eventMessage = EventMessageUtil.createEventMessage(EventType.VCenter, EventMessageUtil.VCENTER_SyncCustomerAttrsData, mapper.writeValueAsString(sddcSoftwareConfig));

      ListOperations<String, String> listOperations = Mockito.mock(ListOperations.class);
      doReturn(listOperations).when(template).opsForList();
      doReturn(mapper.writeValueAsString(eventMessage), null).when(listOperations).rightPop(EventMessageUtil.vcJobList);
      doReturn(vsphereClient).when(service).connectVsphere(any());

      ServerMapping serverMapping = new ServerMapping();
      serverMapping.setVcMobID(vcMobID);
      serverMapping.setVcHostName("server-1");
      serverMapping.setAsset(assetId);
      ServerMapping[] serverMappings = { serverMapping };
      doReturn(new ResponseEntity<>(serverMappings, HttpStatus.OK)).when(restClient).getServerMappingsByVC(any());

      Collection<HostSystem> hostSystems = new ArrayList<>();
      HostSystem hostSystem = mock(HostSystem.class);
      ManagedObjectReference managedObjectReference = mock(ManagedObjectReference.class);
      hostSystems.add(hostSystem);
      doReturn(managedObjectReference).when(hostSystem)._getRef();
      doReturn("server-1").when(hostSystem).getName();
      doReturn(vcMobID).when(managedObjectReference).getValue();
      doReturn(hostSystems).when(vsphereClient).getAllHost();
      Asset asset = new Asset();
      asset.setId(assetId);
      Map<String, String> metricsFormulas = new HashMap<>();
      Map<String, String> hostMetricsFormula = new HashMap<>();
      hostMetricsFormula.put(MetricName.SERVER_TEMPERATURE, asset.getId());
      hostMetricsFormula.put(MetricName.SERVER_PEAK_TEMPERATURE, asset.getId());
      hostMetricsFormula.put(MetricName.SERVER_AVERAGE_TEMPERATURE, asset.getId());
      hostMetricsFormula.put(MetricName.SERVER_ENERGY_CONSUMPTION, asset.getId());
      hostMetricsFormula.put(MetricName.SERVER_POWER, asset.getId());
      hostMetricsFormula.put(MetricName.SERVER_AVERAGE_USED_POWER, asset.getId());
      hostMetricsFormula.put(MetricName.SERVER_PEAK_USED_POWER, asset.getId());
      hostMetricsFormula.put(MetricName.SERVER_MINIMUM_USED_POWER, asset.getId());
      metricsFormulas.put(FlowgateConstant.HOST_METRICS, asset.metricsFormulaToString(hostMetricsFormula));
      asset.setMetricsformulars(metricsFormulas);
      Asset[] assets = { asset };
      doReturn(new ResponseEntity<>(assets, HttpStatus.OK)).when(restClient).getAssetsByVCID(any());
      doReturn(new ResponseEntity<Void>(HttpStatus.OK)).when(restClient).saveAssets(any(Asset.class));
      service.executeAsync(EventMessageUtil.createEventMessage(EventType.VCenter, EventMessageUtil.VCENTER_SyncData, null));
      service.executeAsync(EventMessageUtil.createEventMessage(EventType.VCenter, EventMessageUtil.VCENTER_SyncCustomerAttrsData, null));
      service.executeAsync(EventMessageUtil.createEventMessage(EventType.VCenter, EventMessageUtil.VCENTER_SyncCustomerAttrs, null));
   }

}
