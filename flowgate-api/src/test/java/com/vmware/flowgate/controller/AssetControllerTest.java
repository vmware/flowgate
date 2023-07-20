/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetStatus;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricKeyName;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.MountingSide;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetAddress;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.AssetRealtimeDataSpec;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.MetricData;
import com.vmware.flowgate.common.model.Parent;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.Tenant;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ValueUnit.MetricUnit;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.AssetIPMappingRepository;
import com.vmware.flowgate.repository.AssetRealtimeDataRepository;
import com.vmware.flowgate.repository.AssetRepository;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.repository.ServerMappingRepository;
import com.vmware.flowgate.service.AssetService;
import com.vmware.flowgate.util.BaseDocumentUtil;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class AssetControllerTest {

   @Rule
   public final JUnitRestDocumentation restDocumentation =
         new JUnitRestDocumentation("target/generated-snippets");

   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private AssetRepository assetRepository;

   @Autowired
   AssetRealtimeDataRepository realtimeDataRepository;

   @Autowired
   private FacilitySoftwareConfigRepository facilitySoftwareRepository;

   @Autowired
   private WebApplicationContext context;

   @Autowired
   ServerMappingRepository serverMappingRepository;

   @Autowired
   AssetIPMappingRepository assetIPMappingRepository;

   @Autowired
   AssetService assetService;

   @MockBean
   private StringRedisTemplate template;

   @Rule
   public ExpectedException expectedEx = ExpectedException.none();

   ObjectMapper mapper = new ObjectMapper();

   FieldDescriptor[] fieldpath = new FieldDescriptor[] {
         fieldWithPath("id").description("ID of the asset, created by flowgate"),
         fieldWithPath("assetNumber").description(
               "A unique number that can identify an asset from third part DCIM/CMDB systems.")
               .type(long.class),
         fieldWithPath("assetName").description(
               "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"),
         fieldWithPath("assetSource").description(
               "From which third part systems does this asset comes from. It will refer to a source collection which contains all the thirdpart systems"),
         fieldWithPath("category").description(
               "The category of the asset. Can only be one of :Server, PDU, Cabinet, Networks, Sensors, UPS")
               .type(AssetCategory.class),
         fieldWithPath("subCategory")
               .description("The subcategory of the asset. Only apply to some systems.")
               .type(AssetSubCategory.class).optional(),
         fieldWithPath("manufacturer").description("The manufacture name"),
         fieldWithPath("model").description("The model of the asset"),
         fieldWithPath("serialnumber").description(
               "The SN number of the asset, this number can be used to identify an asset. But only some systems have this number.")
               .optional(),
         fieldWithPath("tag").description(
               "Some system will use tag to identify an asset. It can be either an number or a string.")
               .type(String.class).optional(),
         fieldWithPath("assetAddress").description("The access address of the asset")
               .type(AssetAddress.class).optional(),
         fieldWithPath("region").description("The location region of the asset").optional(),
         fieldWithPath("country").description("The location country of the asset").optional(),
         fieldWithPath("city").description("The location city of the asset").optional(),
         fieldWithPath("building").description("The location building of the asset").optional(),
         fieldWithPath("floor").description("The location floor of the asset").optional(),
         fieldWithPath("room").description("The location room of the asset"),
         fieldWithPath("row").description("The location row of the asset").optional(),
         fieldWithPath("col").description("The location col of the asset").optional(),
         fieldWithPath("extraLocation")
               .description("Extra location information. Only valid for some system.")
               .optional(),
         fieldWithPath("cabinetName").description(
               "The cabinet name where this asset is located. If the asset is cabinet then this filed is empty.")
               .optional(),
         fieldWithPath("cabinetUnitPosition").description("The cabinet unit number")
               .type(int.class).optional(),
         fieldWithPath("mountingSide").description("The cabinet unit number")
               .type(MountingSide.class).optional(),
         fieldWithPath("cabinetAssetNumber").description(
               "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
               .type(long.class).optional(),
         fieldWithPath("assetRealtimeDataSpec")
               .description("Only valid for sensor type of asset.")
               .type(AssetRealtimeDataSpec.class).optional(),
         subsectionWithPath("justificationfields")
               .description("Justification fields that input by user."),
         subsectionWithPath("metricsformulars")
               .description("The sensor data generator logic for this asset."),
         fieldWithPath("lastupdate").description("When this asset was last upated"),
         fieldWithPath("created").description("When this asset was created"),
         fieldWithPath("capacity").description("The capacity of asset.").type(int.class).optional(),
         fieldWithPath("freeCapacity").description("The free capacity of asset.").type(int.class).optional(),
         subsectionWithPath("parent").description("The parent of asset,it will be null unless the asset's category is Sensors")
         .type(Parent.class).optional(),
         fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
         fieldWithPath("switches")
               .description("Physical switchs that this host connected with"),
         fieldWithPath("tenant")
               .description("Tenant information for the asset"),
         subsectionWithPath("status").description(
                     "This is a collection of states, including the state of the asset, "
                           + "the state of the pdu mapping, and the state of the switch mapping.") };

   @Before
   public void setUp() {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation)).build();
   }

   @Test
   public void removeRealTimeData() throws Exception {
      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID("5b7d208d55368540fcba1692");
      realTimeData.setTime(14235666666l);
      realtimeDataRepository.save(realTimeData);
      long time = System.currentTimeMillis();
      List<RealTimeData> res = realtimeDataRepository.getRealTimeDatabtTimeRange(time-FlowgateConstant.DEFAULTEXPIREDTIMERANGE);
      TestCase.assertEquals(1, res.size());
      this.mockMvc.perform(delete("/v1/assets/realtimedata/7899999"))
      .andExpect(status().isOk()).
      andDo(document("realtimedata-delete-example"));

      List<RealTimeData> res1 = realtimeDataRepository.getRealTimeDatabtTimeRange(time-FlowgateConstant.DEFAULTEXPIREDTIMERANGE);
      TestCase.assertEquals(0, res1.size());

   }

   @Test
   public void createAnAssetExample() throws JsonProcessingException, Exception {
      Asset asset = createAsset();
      this.mockMvc
            .perform(post("/v1/assets").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(asset)))
            .andExpect(status().isCreated()).andExpect(header().string("Location", notNullValue()))
            .andDo(document("assets-create-example", requestFields(
                  fieldWithPath("id").description("ID of the asset, created by flowgate"),
                  fieldWithPath("assetNumber").description(
                        "A unique number that can identify an asset from third part DCIM/CMDB systems.")
                        .type(long.class),
                  fieldWithPath("assetName").description(
                        "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"),
                  fieldWithPath("assetSource").description(
                        "From which third part systems does this asset comes from. It will refer to a source collection which contains all the thirdpart systems"),
                  fieldWithPath("category").description(
                        "The category of the asset. Can only be one of :Server, PDU, Cabinet, Networks, Sensors, UPS")
                        .type(AssetCategory.class),
                  fieldWithPath("subCategory")
                        .description("The subcategory of the asset. Only apply to some systems.")
                        .type(AssetSubCategory.class).optional(),
                  fieldWithPath("manufacturer").description("The manufacture name"),
                  fieldWithPath("model").description("The model of the asset"),
                  fieldWithPath("serialnumber").description(
                        "The SN number of the asset, this number can be used to identify an asset. But only some systems have this number.")
                        .optional(),
                  fieldWithPath("tag").description(
                        "Some system will use tag to identify an asset. It can be either an number or a string.")
                        .type(String.class).optional(),
                  fieldWithPath("assetAddress").description("The access address of the asset")
                        .type(AssetAddress.class).optional(),
                  fieldWithPath("region").description("The location region of the asset")
                        .optional(),
                  fieldWithPath("country").description("The location country of the asset")
                        .optional(),
                  fieldWithPath("city").description("The location city of the asset").optional(),
                  fieldWithPath("building").description("The location building of the asset")
                        .optional(),
                  fieldWithPath("floor").description("The location floor of the asset").optional(),
                  fieldWithPath("room").description("The location room of the asset"),
                  fieldWithPath("row").description("The location row of the asset").optional(),
                  fieldWithPath("col").description("The location col of the asset").optional(),
                  fieldWithPath("extraLocation")
                        .description("Extra location information. Only valid for some system.")
                        .optional(),
                  fieldWithPath("cabinetName").description(
                        "The cabinet name where this asset is located. If the asset is cabinet then this filed is empty.")
                        .optional(),
                  fieldWithPath("cabinetUnitPosition").description("The cabinet unit number")
                        .type(int.class).optional(),
                  fieldWithPath("mountingSide").description("The cabinet unit number")
                        .type(MountingSide.class).optional(),
                  fieldWithPath("cabinetAssetNumber").description(
                        "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                        .type(long.class).optional(),
                  fieldWithPath("assetRealtimeDataSpec")
                        .description("Only valid for sensor type of asset.")
                        .type(AssetRealtimeDataSpec.class).optional(),
                  subsectionWithPath("justificationfields").ignored(),
                  subsectionWithPath("metricsformulars").ignored(),
                  fieldWithPath("lastupdate").ignored(), fieldWithPath("created").ignored(),
                  fieldWithPath("capacity").description("The capacity of asset.").type(int.class)
                        .optional(),
                  fieldWithPath("freeCapacity").description("The free capacity of asset.")
                        .type(int.class).optional(),
                  subsectionWithPath("parent").description(
                        "The parent of asset,it will be null unless the asset's category is Sensors")
                        .type(Parent.class).optional(),
                  fieldWithPath("pdus")
                        .description("Possible PDUs that this server connected with"),
                  fieldWithPath("switches")
                        .description("Physical switchs that this host connected with"),
                  fieldWithPath("tenant").description("Tenant information for the asset")
                        .type(Tenant.class).optional(),
                  subsectionWithPath("status").description(
                        "This is a collection of states, including the state of the asset, "
                              + "the state of the pdu mapping, and the state of the switch mapping."))))
            .andReturn().getResponse().getHeader("Location");
      assetRepository.deleteById(asset.getId());
   }

   @Test
   public void saveServerMappingExample() throws JsonProcessingException, Exception {
      ServerMapping mapping = createServerMapping();
      this.mockMvc
            .perform(post("/v1/assets/mapping").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(mapping)))
            .andExpect(status().isCreated())
            .andDo(document("assets-saveServerMapping-example",
                  requestFields(
                        fieldWithPath("id").description("ID of the mapping, created by flowgate"),
                        fieldWithPath("asset").description("An asset for serverMapping."),
                        fieldWithPath("vcID").description("ID of Vcenter."),
                        fieldWithPath("vcHostName")
                              .description("Server's hostname display in Vcenter."),
                        fieldWithPath("vcMobID").description("EXSI server's management object ID."),
                        fieldWithPath("vcClusterMobID").description("MobID of Vcenter Cluster."),
                        fieldWithPath("vcInstanceUUID").description("Vcenter's UUID."),
                        fieldWithPath("vroID").description("ID of Aria Operations."),
                        fieldWithPath("vroResourceName")
                              .description("Resource Name in Aria Operations for this server."),
                        fieldWithPath("vroVMEntityName").description("EntityName of Resource."),
                        fieldWithPath("vroVMEntityObjectID").description("Aria Operations Entity Object ID."),
                        fieldWithPath("vroVMEntityVCID").description("Aria Operations Entity's Vcenter ID."),
                        fieldWithPath("vroResourceID").description("Aria Operations Resource ID."))));
      serverMappingRepository.deleteById(mapping.getId());
   }

   @Test
   public void mappingFacility() throws JsonProcessingException, Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      Asset newAsset = new Asset();
      newAsset.setId(asset.getId());
      List<String> pdus = new ArrayList<String>();
      pdus.add("oqwen812321093asdmgtqawee1");
      newAsset.setPdus(pdus);
      List<String> switches = new ArrayList<String>();
      switches.add("ow23aw312e3nr3d2a57788i");
      newAsset.setSwitches(switches);
      Map<String, String> metricsformulars = new HashMap<String, String>();
      Map<String, Map<String, String>> sensorMap =
            new HashMap<String, Map<String, String>>();
      Map<String, String> positionInfo = new HashMap<String, String>();

      Asset humiditySensorAsset = createAsset();
      humiditySensorAsset.setCategory(AssetCategory.Sensors);
      humiditySensorAsset.setSubCategory(AssetSubCategory.Humidity);
      humiditySensorAsset.setCabinetUnitPosition(12);
      HashMap<String,String> sensorAssetJustfication = new HashMap<String, String>();
      Map<String,String> sensorInfo = new HashMap<String,String>();
      sensorInfo.put(FlowgateConstant.POSITION, "INLET");
      try {
         sensorAssetJustfication.put(FlowgateConstant.SENSOR, mapper.writeValueAsString(sensorInfo));
         humiditySensorAsset.setJustificationfields(sensorAssetJustfication);
      } catch (JsonProcessingException e) {
         TestCase.fail();
      }
      humiditySensorAsset = assetRepository.save(humiditySensorAsset);
      positionInfo.put(humiditySensorAsset.getId(), humiditySensorAsset.getId());
      sensorMap.put(MetricName.SERVER_FRONT_HUMIDITY, positionInfo);
      String sensorFormulaInfo = null;
      try {
         sensorFormulaInfo = mapper.writeValueAsString(sensorMap);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      metricsformulars.put(FlowgateConstant.SENSOR, sensorFormulaInfo);
      newAsset.setMetricsformulars(metricsformulars);

      this.mockMvc
      .perform(put("/v1/assets/mappingfacility").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(newAsset)))
      .andExpect(status().isOk())
      .andDo(document("assets-mappingFacility-example", requestFields(
            fieldWithPath("id").description("ID of the asset, created by flowgate"),
            fieldWithPath("assetNumber").description(
                  "A unique number that can identify an asset from third part DCIM/CMDB systems.")
                  .type(long.class),
            fieldWithPath("assetName").description(
                  "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"),
            fieldWithPath("assetSource").description(
                  "From which third part systems does this asset comes from. It will refer to a source collection which contains all the thirdpart systems"),
            fieldWithPath("category").description(
                  "The category of the asset. Can only be one of :Server, PDU, Cabinet, Networks, Sensors, UPS")
                  .type(AssetCategory.class),
            fieldWithPath("subCategory")
                  .description("The subcategory of the asset. Only apply to some systems.")
                  .type(AssetSubCategory.class).optional(),
            fieldWithPath("manufacturer").description("The manufacture name"),
            fieldWithPath("model").description("The model of the asset"),
            fieldWithPath("serialnumber").description(
                  "The SN number of the asset, this number can be used to identify an asset. But only some systems have this number.")
                  .optional(),
            fieldWithPath("tag").description(
                  "Some system will use tag to identify an asset. It can be either an number or a string.")
                  .type(String.class).optional(),
            fieldWithPath("assetAddress").description("The access address of the asset")
                  .type(AssetAddress.class).optional(),
            fieldWithPath("region").description("The location region of the asset")
                  .optional(),
            fieldWithPath("country").description("The location country of the asset")
                  .optional(),
            fieldWithPath("city").description("The location city of the asset").optional(),
            fieldWithPath("building").description("The location building of the asset")
                  .optional(),
            fieldWithPath("floor").description("The location floor of the asset").optional(),
            fieldWithPath("room").description("The location room of the asset"),
            fieldWithPath("row").description("The location row of the asset").optional(),
            fieldWithPath("col").description("The location col of the asset").optional(),
            fieldWithPath("extraLocation")
                  .description("Extra location information. Only valid for some system.")
                  .optional(),
            fieldWithPath("cabinetName").description(
                  "The cabinet name where this asset is located. If the asset is cabinet then this filed is empty.")
                  .optional(),
            fieldWithPath("cabinetUnitPosition").description("The cabinet unit number")
                  .type(int.class).optional(),
            fieldWithPath("mountingSide").description("The cabinet unit number")
                  .type(MountingSide.class).optional(),
            fieldWithPath("cabinetAssetNumber").description(
                  "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                  .type(long.class).optional(),
            fieldWithPath("assetRealtimeDataSpec")
                  .description("Only valid for sensor type of asset.")
                  .type(AssetRealtimeDataSpec.class).optional(),
            fieldWithPath("justificationfields").ignored(),
            subsectionWithPath("metricsformulars").description("Possible PDUs And sensors that this server connected with"),
            fieldWithPath("pdus")
                  .description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with"),
            fieldWithPath("lastupdate").ignored(), fieldWithPath("created").ignored(),
            fieldWithPath("capacity").description("The capacity of asset.").type(int.class)
                  .optional(),
            fieldWithPath("freeCapacity").description("The free capacity of asset.")
                  .type(int.class).optional(),
            fieldWithPath("parent").description(
                  "The parent of asset,it will be null unless the asset's category is Sensors")
                  .type(Parent.class).optional(),
            fieldWithPath("tenant").description("Tenant information for the asset")
                  .type(Tenant.class).optional(),
            subsectionWithPath("status").description(
                        "This is a collection of states, including the state of the asset, "
                              + "the state of the pdu mapping, and the state of the switch mapping."))));
      Asset testAsset = assetRepository.findById(asset.getId()).get();
      TestCase.assertEquals(1, testAsset.getPdus().size());
      TestCase.assertEquals("oqwen812321093asdmgtqawee1", testAsset.getPdus().get(0));

      TestCase.assertEquals(1, testAsset.getSwitches().size());
      TestCase.assertEquals("ow23aw312e3nr3d2a57788i", testAsset.getSwitches().get(0));

      Map<String, Map<String, String>> sensorFormulaMap = null;
      try {
         sensorFormulaMap = mapper.readValue(testAsset.getMetricsformulars().get(FlowgateConstant.SENSOR), new TypeReference<Map<String, Map<String, String>>>() {});
      } catch (IOException e) {
         TestCase.fail(e.getMessage());
      }
      TestCase.assertEquals(FlowgateConstant.RACK_UNIT_PREFIX + humiditySensorAsset.getCabinetUnitPosition()+FlowgateConstant.SEPARATOR+"INLET",
            sensorFormulaMap.get(MetricName.SERVER_FRONT_HUMIDITY).keySet().iterator().next());
      assetRepository.deleteById(testAsset.getId());
      assetRepository.deleteById(humiditySensorAsset.getId());
   }

   @Test
   public void insertRealtimeDataExample() throws JsonProcessingException, Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      long time = System.currentTimeMillis();
      RealTimeData realtimedata = createServerPDURealTimeData(time);
      List<ValueUnit> valueunits = realtimedata.getValues();
      valueunits.addAll(createServerHostRealTimeData(time).getValues());
      ValueUnit tempValue = new ValueUnit();
      tempValue.setValueNum(32);
      tempValue.setTime(time);
      tempValue.setUnit(MetricUnit.C.toString());
      tempValue.setKey(MetricName.TEMPERATURE);
      valueunits.add(tempValue);

      ValueUnit humidityValue = new ValueUnit();
      humidityValue.setValueNum(20);
      humidityValue.setTime(time);
      humidityValue.setUnit("%");
      humidityValue.setKey(MetricName.HUMIDITY);
      valueunits.add(humidityValue);
      realtimedata.setId(UUID.randomUUID().toString());
      realtimedata.setAssetID(asset.getId());
      realtimeDataRepository.save(realtimedata);

      this.mockMvc
            .perform(post("/v1/assets/" + asset.getId() + "/sensordata")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(realtimedata)))
            .andExpect(status().isOk())
            .andDo(document("assets-insertRealtimeData-example", requestFields(
                  fieldWithPath("id").description("ID of the realtime, created by flowgate"),
                  fieldWithPath("assetID").description("ID of the asset, created by flowgate"),
                  subsectionWithPath("values")
                        .description("A list of sensor data. eg. Humidity , Electric... ")
                        .type(ValueUnit[].class),
                  fieldWithPath("time").description("The time of generate sensor data."))));
      assetRepository.deleteById(asset.getId());
      realtimeDataRepository.deleteById(realtimedata.getId());
   }

   @Test
   public void createAssetBatchExample() throws JsonProcessingException, Exception {
      List<Asset> assets = new ArrayList<Asset>();
      Asset asset1 = createAsset();
      asset1.setAssetName("assetname");
      asset1.setAssetNumber(18);
      assets.add(asset1);
      Asset asset2 = createAsset();
      asset2.setAssetName("assetname2");
      asset2.setAssetNumber(17);
      assets.add(asset2);
      this.mockMvc
            .perform(post("/v1/assets/batchoperation").contentType(MediaType.APPLICATION_JSON_VALUE)
                  .content(objectMapper.writeValueAsString(assets)))
            .andExpect(status().isCreated())
            .andDo(document("assets-createBatch-example",
                  requestFields(fieldWithPath("[]").description("An array of assets"))
                        .andWithPrefix("[].", fieldpath)));

      assetRepository.deleteById(asset1.getId());
      assetRepository.deleteById(asset2.getId());
   }

   @Test
   public void realTimeDatabatchCreationExample() throws JsonProcessingException, Exception {
      List<RealTimeData> realtimedatas = new ArrayList<RealTimeData>();
      long currentTime = System.currentTimeMillis();
      RealTimeData realtimedata1 = createServerPDURealTimeData(currentTime);
      List<ValueUnit> valueunits = realtimedata1.getValues();
      ValueUnit tempValue = new ValueUnit();
      tempValue.setValueNum(32);
      tempValue.setTime(currentTime);
      tempValue.setUnit(MetricUnit.C.toString());
      tempValue.setKey(MetricName.TEMPERATURE);
      valueunits.add(tempValue);

      ValueUnit humidityValue = new ValueUnit();
      humidityValue.setValueNum(20);
      humidityValue.setTime(currentTime);
      humidityValue.setUnit("%");
      humidityValue.setKey(MetricName.HUMIDITY);
      valueunits.add(humidityValue);
      realtimedata1.setAssetID("assetid1");

      realtimedatas.add(realtimedata1);
      RealTimeData realtimedata2 = createServerPDURealTimeData(currentTime);
      realtimedata2.setAssetID("assetid2");
      realtimedatas.add(realtimedata2);

      FieldDescriptor[] fieldpath =
            new FieldDescriptor[] { fieldWithPath("id").description("ID of the RealTimeData"),
                  fieldWithPath("assetID").description("ID of the asset, created by flowgate"),
                  fieldWithPath("values").description("List of ValueUnit"),
                  fieldWithPath("time").description("Create time of ValueUnit"),
                  subsectionWithPath("values")
                  .description("A list of sensor data. eg. Humidity , Electric... ")
                  .type(ValueUnit[].class)};
     this.mockMvc
     .perform(post("/v1/assets/sensordata/batchoperation")
           .contentType(MediaType.APPLICATION_JSON_VALUE)
           .content(objectMapper.writeValueAsString(realtimedatas)))
     .andExpect(status().isCreated())
     .andDo(document("assets-realTimeDatabatchCreation-example",
           requestFields(fieldWithPath("[]").description("An array of RealTimeData"))
                 .andWithPrefix("[].", fieldpath)));
     realtimeDataRepository.deleteById(realtimedata1.getId());
     realtimeDataRepository.deleteById(realtimedata2.getId());
   }

   @Test
   public void readAssetBySourceExample() throws Exception {
      Asset asset = createAsset();
      asset.setAssetName("testReadAssetBySource");
      asset = assetRepository.save(asset);
      try {
         this.mockMvc.perform(get("/v1/assets/source/{source}", asset.getAssetSource())
               .param("currentPage", "1").param("pageSize", "5"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$..content[0].assetName").value("testReadAssetBySource"))
               .andDo(document("assets-getBySource-example",
               pathParameters(
                     parameterWithName("source").description("The source of asset")),
               requestParameters(
                     parameterWithName("currentPage").description("The page you want to get"),
                     parameterWithName("pageSize").description("The number of assets you want to get by every request.Default value: 20")),
                       responseFields(subsectionWithPath("content").description("An assets array."),
                             fieldWithPath("totalPages").description("content's total pages."),
                               fieldWithPath("totalElements").description("content's total elements."),
                               fieldWithPath("last").description("Is the last."),
                               fieldWithPath("number").description("The page number."),
                               fieldWithPath("size").description("The page size."),
                               fieldWithPath("sort").description("The sort."),
                               fieldWithPath("numberOfElements").description("The number of Elements."),
                               fieldWithPath("first").description("Is the first."),
                               subsectionWithPath("pageable").description("pageable.").ignored(),
                               subsectionWithPath("sort").description("sorted.").ignored(),
                               fieldWithPath("empty").description("Is empty.").ignored())));
      }finally {
         assetRepository.deleteById(asset.getId());
      }
   }

   @Test
   public void readAssetBySourceAndTypeExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      try {
         this.mockMvc
               .perform(get(
                     "/v1/assets/source/{source}/type/{category}",asset.getAssetSource(), asset.getCategory())
                     .param("currentPage", "1").param("pageSize", "5"))
               .andExpect(status().isOk())
               .andDo(document("assets-getBySourceAndType-example",
                     pathParameters(
                              parameterWithName("source").description("The source of asset"),
                              parameterWithName("category").description("The category of asset, generated by flowgate.Sample value : Server/Sensors/PDU/Cabinet/Networks/Chassis/UPS")
                              ),
                     requestParameters(
                              parameterWithName("currentPage").description("The page you want to get"),
                              parameterWithName("pageSize").description("The number of assets you want to get by every request.Default value: 20")),
                     responseFields(subsectionWithPath("content").description("An assets array."),
                             fieldWithPath("totalPages").description("content's total pages."),
                               fieldWithPath("totalElements").description("content's total elements."),
                               fieldWithPath("last").description("Is the last."),
                               fieldWithPath("number").description("The page number."),
                               fieldWithPath("size").description("The page size."),
                               fieldWithPath("sort").description("The sort."),
                               fieldWithPath("numberOfElements").description("The number of Elements."),
                               fieldWithPath("first").description("Is the first."),
                               subsectionWithPath("pageable").description("pageable.").ignored(),
                               subsectionWithPath("sort").description("sorted.").ignored(),
                               fieldWithPath("empty").description("Is empty.").ignored())))
               .andReturn();
      }finally {
         assetRepository.deleteById(asset.getId());
      }
   }

   @Test
   public void readAssetByTypeExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      try {
         this.mockMvc
               .perform(
                     get("/v1/assets/type/{category}", asset.getCategory())
                     .param("currentPage", "1").param("pageSize", "5"))
               .andExpect(status().isOk())
               .andDo(document("assets-getByType-example", pathParameters(
                     parameterWithName("category").description("The category of asset, generated by flowgate.Sample value : Server/Sensors/PDU/Cabinet/Networks/Chassis/UPS")),
               requestParameters(
                     parameterWithName("currentPage").description("The page you want to get"),
                     parameterWithName("pageSize").description("The number of assets you want to get by every request.Default value: 20")),
               responseFields(
                     subsectionWithPath("content").description("An assets array."),
                     fieldWithPath("totalPages").description("content's total pages."),
                       fieldWithPath("totalElements").description("content's total elements."),
                       fieldWithPath("last").description("Is the last."),
                       fieldWithPath("number").description("The page number."),
                       fieldWithPath("size").description("The page size."),
                       fieldWithPath("sort").description("The sort."),
                       fieldWithPath("numberOfElements").description("The number of Elements."),
                       fieldWithPath("first").description("Is the first."),
                       subsectionWithPath("pageable").description("pageable.").ignored(),
                       subsectionWithPath("sort").description("sorted.").ignored(),
                       fieldWithPath("empty").description("Is empty.").ignored())))
               .andReturn();
      } finally {
         assetRepository.deleteById(asset.getId());
      }
   }

   @Test
   public void getHostNameByIPExample() throws Exception {

      AssetIPMapping mapping1 = createAssetIPMapping();
      assetIPMappingRepository.save(mapping1);
      AssetIPMapping mapping2 = createAssetIPMapping();
      assetIPMappingRepository.save(mapping2);

      try {
         FieldDescriptor[] fieldpath = new FieldDescriptor[] {
               fieldWithPath("id").description("ID of the AssetIPMapping, created by flowgate"),
               fieldWithPath("ip").description("IP of AssetIPMapping."),
               fieldWithPath("macAddress").description("macAddress of IP."),
               fieldWithPath("assetname").description("name of asset."), };
         this.mockMvc.perform(get("/v1/assets/mapping/hostnameip/ip/{ip}", mapping1.getIp()))
               .andExpect(status().isOk())
               .andDo(document("assets-getHostNameByIP-example",pathParameters(
                     parameterWithName("ip").description("IP of AssetIPMapping.")),
                     responseFields(fieldWithPath("[]").description("An array of AssetIPMapping"))
                           .andWithPrefix("[].", fieldpath)));
      }finally {
         assetIPMappingRepository.deleteById(mapping1.getId());
         assetIPMappingRepository.deleteById(mapping2.getId());
      }
   }

   @Test
   public void getUnmappedServersExample() throws Exception {

      ServerMapping mapping1 = createServerMapping();
      serverMappingRepository.save(mapping1);
      ServerMapping mapping2 = createServerMapping();
      serverMappingRepository.save(mapping2);
      try {

         FieldDescriptor[] fieldpath =
               new FieldDescriptor[] { fieldWithPath("").description("hostname") };

         this.mockMvc.perform(get("/v1/assets/mapping/unmappedservers")).andExpect(status().isOk())
               .andDo(document("assets-getUnmappedServers-example",
                     responseFields(fieldWithPath("[]").description("An array of host name,the host name is from IT system for example : vCenter or Aria Operations"))
                           .andWithPrefix("[].", fieldpath)));
      } finally {
         serverMappingRepository.deleteById(mapping1.getId());
         serverMappingRepository.deleteById(mapping2.getId());
      }

   }

   @Test
   public void readMappedAssetExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      ServerMapping mapping = new ServerMapping();
      mapping.setId(UUID.randomUUID().toString());
      mapping.setAsset(asset.getId());
      mapping.setVcID("5b7cfd5655368548d42e0fd5");
      mapping.setVcHostName("10.192.74.203");
      mapping.setVcMobID("host-11");
      serverMappingRepository.save(mapping);
      Asset asset2 = createAsset();
      asset2 = assetRepository.save(asset2);
      ServerMapping mapping2 = new ServerMapping();
      mapping2.setId(UUID.randomUUID().toString());
      mapping2.setAsset(asset2.getId());
      mapping2.setVcID("5b7cfd5655368548d42e0fd6");
      mapping2.setVcHostName("10.192.74.203");
      mapping2.setVcMobID("host-11");
      serverMappingRepository.save(mapping2);
      try {
         this.mockMvc.perform(get("/v1/assets/mappedasset/category/{category}", asset.getCategory()))
         .andDo(document("assets-getMapped-example",pathParameters(
               parameterWithName("category").description("The category of asset, generated by flowgate.Sample value : Server/Sensors/PDU/Cabinet/Networks/Chassis/UPS")),
               responseFields(fieldWithPath("[]").description("An array of assets"))
                     .andWithPrefix("[].", fieldpath)));
      }finally {
         assetRepository.deleteById(asset.getId());
         serverMappingRepository.deleteById(mapping.getId());
         assetRepository.deleteById(asset2.getId());
         serverMappingRepository.deleteById(mapping2.getId());
      }
   }

   @Test
   public void readAssetsByPage() throws Exception {
      Asset asset1 = createAsset();
      asset1.setAssetName("assetname");
      asset1.setAssetNumber(18);
      asset1.setAssetSource(null);
      assetRepository.save(asset1);
      Asset asset2 = createAsset();
      asset2.setAssetName("assetname2");
      asset2.setAssetNumber(17);
      asset2.setAssetSource(null);
      assetRepository.save(asset2);
      FacilitySoftwareConfig facility = createFacilitySoftware();
      facilitySoftwareRepository.save(facility);
      int pageNumber = 1;
      int pageSize = 1;
      try {
         this.mockMvc.perform(get("/v1/assets/page/{pageNumber}/pagesize/{pageSize}", pageNumber, pageSize))
         .andExpect(status().isOk())
         .andDo(document("assets-getAssetsByPage-example",pathParameters(
               parameterWithName("pageNumber").description("The number of page you want to get."),
               parameterWithName("pageSize").description("The number of assets you want to get by every request.Default value:20")),
               responseFields(subsectionWithPath("content").description("An assets array."),
                     fieldWithPath("totalPages").description("content's total pages."),
                     fieldWithPath("totalElements").description("content's total elements."),
                     fieldWithPath("last").description("Is the last."),
                     fieldWithPath("number").description("The page number."),
                     fieldWithPath("size").description("The page size."),
                     fieldWithPath("sort").description("The sort."),
                     fieldWithPath("numberOfElements").description("The number of Elements."),
                     fieldWithPath("first").description("Is the first."),
                     subsectionWithPath("pageable").description("pageable.").ignored(),
                     subsectionWithPath("sort").description("sorted.").ignored(),
                     fieldWithPath("empty").description("Is empty.").ignored())));

      }finally {
         assetRepository.deleteById(asset1.getId());
         assetRepository.deleteById(asset2.getId());
         facilitySoftwareRepository.deleteById(facility.getId());
      }
   }

   @Test
   public void readAssetsByAssetNameAndTagLikAndKeywordsExample() throws Exception {
      Asset asset1 = createAsset();
      asset1.setAssetName("assetname");
      asset1.setAssetNumber(18);
      assetRepository.save(asset1);
      Asset asset2 = createAsset();
      asset2.setAssetName("assetname2");
      asset2.setAssetNumber(17);
      assetRepository.save(asset2);
      int pageNumber = 1;
      int pageSize = 1;
      String keywords = "keyword";
      FacilitySoftwareConfig facility = createFacilitySoftware();
      facilitySoftwareRepository.save(facility);
      try {
         this.mockMvc
               .perform(get("/v1/assets/page/{pageNumber}/pagesize/{pageSize}/keywords/{keywords}",pageNumber, pageSize, keywords))
               .andExpect(status().isOk())
               .andDo(document("assets-getByAssetNameAndTagLikAndKeywords-example",pathParameters(
                     parameterWithName("pageNumber").description("The number of page you want to get."),
                     parameterWithName("pageSize").description("The number of assets you want to get by every request.Default value:20"),
                     parameterWithName("keywords").description("A part of asset name of asset tag.")),
                     responseFields(subsectionWithPath("content").description("An assets array."),
                           fieldWithPath("totalPages").description("content's total pages."),
                           fieldWithPath("totalElements").description("content's total elements."),
                           fieldWithPath("last").description("Is the last."),
                           fieldWithPath("number").description("The page number."),
                           fieldWithPath("size").description("The page size."),
                           fieldWithPath("sort").description("The sort."),
                           fieldWithPath("numberOfElements").description("The number of Elements."),
                           fieldWithPath("first").description("Is the first."),
                           subsectionWithPath("pageable").description("pageable.").ignored(),
                           subsectionWithPath("sort").description("sorted.").ignored(),
                           fieldWithPath("empty").description("Is empty.").ignored())));
      } finally {
         assetRepository.deleteById(asset1.getId());
         assetRepository.deleteById(asset2.getId());
         facilitySoftwareRepository.deleteById(facility.getId());
      }
   }

   @Test
   public void findServersWithoutPDUInfoExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      ServerMapping mapping = new ServerMapping();
      mapping.setId(UUID.randomUUID().toString());
      mapping.setAsset(asset.getId());
      mapping.setVcID("5b7cfd5655368548d42e0fd5");
      mapping.setVcHostName("10.192.74.203");
      mapping.setVcMobID("host-11");
      serverMappingRepository.save(mapping);
      Asset asset2 = createAsset();

      asset2 = assetRepository.save(asset2);
      ServerMapping mapping2 = new ServerMapping();
      mapping2.setId(UUID.randomUUID().toString());
      mapping2.setAsset(asset2.getId());
      mapping2.setVcID("5b7cfd5655368548d42e0fd6");
      mapping2.setVcHostName("10.192.74.203");
      mapping2.setVcMobID("host-11");
      serverMappingRepository.save(mapping2);

      try {
         this.mockMvc.perform(get("/v1/assets/pdusisnull"))
         .andDo(document("assets-findServersWithoutPDUInfo-example",
               responseFields(fieldWithPath("[]").description("An array of assets"))
                     .andWithPrefix("[].", fieldpath)));
      }finally {
         assetRepository.deleteById(asset.getId());
         serverMappingRepository.deleteById(mapping.getId());
         assetRepository.deleteById(asset2.getId());
         serverMappingRepository.deleteById(mapping2.getId());
      }
   }

   @Test
   public void findServersWithPDUInfoExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      ServerMapping mapping = createServerMapping();
      mapping.setAsset(asset.getId());
      mapping.setVcID("5b7cfd5655368548d42e0fd5");
      mapping.setVcHostName("10.192.74.203");
      mapping.setVcMobID("host-11");
      serverMappingRepository.save(mapping);
      Asset asset2 = createAsset();
      asset2.setPdus(Arrays.asList("pdu1", "pdu2"));
      asset2 = assetRepository.save(asset2);
      ServerMapping mapping2 = createServerMapping();
      mapping2.setAsset(asset2.getId());
      mapping2.setVcID("5b7cfd5655368548d42e0fd6");
      mapping2.setVcHostName("10.192.74.203");
      mapping2.setVcMobID("host-11");
      serverMappingRepository.save(mapping2);
      try {
         this.mockMvc.perform(get("/v1/assets/pdusisnotnull"))
         .andExpect(jsonPath("$[0].assetNumber", is(12345)))
         .andExpect(jsonPath("$[0].assetName", is("pek-wor-server-02")))
         .andExpect(jsonPath("$[0].pdus", hasSize(2)))
         .andExpect(jsonPath("$[0].pdus[0]", is("pdu1")))
         .andDo(document("assets-findServersWithPDUInfo-example",
               responseFields(fieldWithPath("[]").description("An array of assets"))
                     .andWithPrefix("[].", fieldpath)))
         .andReturn().getResponse();
      }finally {
         assetRepository.deleteById(asset.getId());
         serverMappingRepository.deleteById(mapping.getId());
         assetRepository.deleteById(asset2.getId());
         serverMappingRepository.deleteById(mapping2.getId());
      }
   }

   @Test
   public void findAssetsByVroIdTest() throws Exception{
      ServerMapping mapping1 = createServerMapping();
      mapping1.setVroID("90o76d5655368548d42e0fd5");
      mapping1.setAsset("0001bdc8b25d4c2badfd045ab61aabfa");
      serverMappingRepository.save(mapping1);
      ServerMapping mapping2 = createServerMapping();
      mapping1.setVroID("90o76d5655368548d42e0fd5");
      serverMappingRepository.save(mapping2);
      Asset asset = new Asset();
      asset.setId("0001bdc8b25d4c2badfd045ab61aabfa");
      assetRepository.save(asset);
      try {
         MvcResult result = this.mockMvc
               .perform(get("/v1/assets/vrops/{vropsId}", "90o76d5655368548d42e0fd5"))
               .andExpect(status().isOk())
               .andDo(document("assets-getAssetsByVroId-example", pathParameters(
                     parameterWithName("vropsId").description("The id of SDDCSoftwareConfig, generated by flowgate."))))
               .andReturn();
               String res = result.getResponse().getContentAsString();
               Asset [] assets = mapper.readValue(res, Asset[].class);
               TestCase.assertEquals(asset.getId(), assets[0].getId());
      }finally {
         serverMappingRepository.deleteById(mapping1.getId());
         serverMappingRepository.deleteById(mapping2.getId());
         assetRepository.deleteById(asset.getId());
      }
   }

   @Test
   public void findAssetsByVCIdTest() throws Exception{
      ServerMapping mapping1 = createServerMapping();
      mapping1.setAsset("0001bdc8b25d4c2badfd045ab61aabfa");
      mapping1.setVcID("5b7cfd5655368548d42e0fd5");
      serverMappingRepository.save(mapping1);
      ServerMapping mapping2 = createServerMapping();
      mapping2.setVcID("5b7cfd5655368548d42e0fd5");
      serverMappingRepository.save(mapping2);
      Asset asset = new Asset();
      asset.setId("0001bdc8b25d4c2badfd045ab61aabfa");
      assetRepository.save(asset);

      try {
         MvcResult result =
               this.mockMvc
                     .perform(get("/v1/assets/vc/{vcId}","5b7cfd5655368548d42e0fd5"))
                     .andExpect(status().isOk())
                     .andDo(document("assets-getAssetsByVcId-example",pathParameters(
                           parameterWithName("vcId").description("The id of SDDCSoftwareConfig, generated by flowgate."))))
                     .andReturn();
         String res = result.getResponse().getContentAsString();
         Asset[] assets = mapper.readValue(res, Asset[].class);
         TestCase.assertEquals(asset.getId(), assets[0].getId());
      } finally {
         serverMappingRepository.deleteById(mapping1.getId());
         serverMappingRepository.deleteById(mapping2.getId());
         assetRepository.deleteById(asset.getId());
      }
   }

   @Test
   public void getPageMappingsByVROPSIdExample() throws Exception {
      ServerMapping mapping1 = createServerMapping();
      mapping1.setVcClusterMobID("1");
      mapping1.setVcHostName("1");
      mapping1.setVroID("1");
      serverMappingRepository.save(mapping1);
      ServerMapping mapping2 = createServerMapping();
      mapping2.setVcClusterMobID("2");
      mapping2.setVcHostName("2");
      mapping2.setVroID("2");
      serverMappingRepository.save(mapping2);
      int pageNumber = 1;
      int pageSize = 1;
      int vropsID = 1;

      try {
         this.mockMvc
         .perform(get("/v1/assets/mapping/vrops/{vropsId}/page/{pageNumber}/pagesize/{pageSize}",vropsID, pageNumber, pageSize))
         .andExpect(status().isOk())
         .andDo(document("assets-getPageMappingsByVROPSId-example",pathParameters(
               parameterWithName("vropsId").description("The id of SDDCSoftwareConfig,generated by flowgate."),
               parameterWithName("pageNumber").description("The number of page you want to get."),
               parameterWithName("pageSize").description("The number of serverMapping you want to get by every request.Default value:20")),
               responseFields(subsectionWithPath("content").description("ServerMapping's array."),
                     fieldWithPath("totalPages").description("content's total pages."),
                     fieldWithPath("totalElements").description("content's total elements."),
                     fieldWithPath("last").description("Is the last."),
                     fieldWithPath("number").description("The page number."),
                     fieldWithPath("size").description("The page size."),
                     fieldWithPath("sort").description("The sort."),
                     fieldWithPath("numberOfElements").description("The number of Elements."),
                     fieldWithPath("first").description("Is the first."),
                     subsectionWithPath("pageable").description("pageable.").ignored(),
                     subsectionWithPath("sort").description("sorted.").ignored(),
                     fieldWithPath("empty").description("Is empty.").ignored())));

      }finally {
         serverMappingRepository.deleteById(mapping1.getId());
         serverMappingRepository.deleteById(mapping2.getId());
      }
   }

   @Test
   public void getPageMappingsByVCIdExample() throws Exception {
      String id = UUID.randomUUID().toString();
      Asset server = createAsset();
      server.setId(id);
      server.setCategory(AssetCategory.Server);
      server.setAssetName("cloud-sha1-esx2");
      server = assetRepository.save(server);
      ServerMapping mapping1 = createServerMapping();
      mapping1.setVcClusterMobID("1");
      mapping1.setVcHostName("1");
      mapping1.setVroID("1");
      mapping1.setVcID("1");
      mapping1.setAsset(id);
      serverMappingRepository.save(mapping1);
      ServerMapping mapping2 = createServerMapping();
      mapping2.setVcClusterMobID("2");
      mapping2.setVcHostName("2");
      mapping2.setVroID("2");
      serverMappingRepository.save(mapping2);
      int pageNumber = 1;
      int pageSize = 1;
      int vcID = 1;
      try {
         this.mockMvc.perform(
               get("/v1/assets/mapping/vc/{vcId}/page/{pageNumber}/pagesize/{pageSize}",vcID, pageNumber, pageSize))
               .andExpect(status().isOk())
               .andDo(document("assets-getPageMappingsByVCId-example",pathParameters(
                     parameterWithName("vcId").description("The id of SDDCSoftwareConfig,generated by flowgate."),
                     parameterWithName("pageNumber").description("The number of page you want to get."),
                     parameterWithName("pageSize").description("The number of serverMapping you want to get by every request.Default value:20")),
                     responseFields(subsectionWithPath("content").description("ServerMapping's array."),
                           fieldWithPath("totalPages").description("content's total pages."),
                           fieldWithPath("totalElements").description("content's total elements."),
                           fieldWithPath("last").description("Is the last."),
                           fieldWithPath("number").description("The page number."),
                           fieldWithPath("size").description("The page size."),
                           fieldWithPath("sort").description("The sort."),
                           fieldWithPath("numberOfElements").description("The number of Elements."),
                           fieldWithPath("first").description("Is the first."),
                           subsectionWithPath("pageable").description("pageable.").ignored(),
                           subsectionWithPath("sort").description("sorted.").ignored(),
                           fieldWithPath("empty").description("Is empty.").ignored())));

      }finally {
         serverMappingRepository.deleteById(mapping1.getId());
         serverMappingRepository.deleteById(mapping2.getId());
         assetRepository.deleteById(id);
      }
   }

   @Test
   public void createHostNameIPMappingExample() throws Exception {
      SetOperations<String,String> setOperations = Mockito.mock(SetOperations.class);
      when(template.hasKey(anyString())).thenReturn(false);
      when(template.opsForSet()).thenReturn(setOperations);
      when(setOperations.add(anyString(), any())).thenReturn(1l);
      Asset server = createAsset();
      server.setCategory(AssetCategory.Server);
      server.setAssetName("cloud-sha1-esx2");
      server = assetRepository.save(server);
      AssetIPMapping assetipmapping = createAssetIPMapping();
      assetipmapping.setAssetname(server.getAssetName());
      assetipmapping.setIp("192.168.0.1");
      assetipmapping.setMacAddress("50:00:56:ge:64:62");
      this.mockMvc
            .perform(post("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(assetipmapping)))
            .andExpect(status().isCreated())
            .andDo(document("assets-createHostNameIPMapping-example", requestFields(
                  fieldWithPath("id").description("ID of the asset, created by flowgate"),
                  fieldWithPath("ip").description("ip of hostname"),
                  fieldWithPath("macAddress").description("macAddress of IP"),
                  fieldWithPath("assetname").description(
                        "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"))));
      assetipmapping = assetIPMappingRepository.findById(assetipmapping.getId()).get();
      assetRepository.deleteById(server.getId());
      assetIPMappingRepository.deleteById(assetipmapping.getId());
      TestCase.assertEquals(server.getAssetName(), assetipmapping.getAssetname());
   }

   @Test
   public void createHostNameAndIPMappingFailureExample() throws Exception {
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Invalid value: '10.15' for field: 'ip'");
      AssetIPMapping mapping = new AssetIPMapping();
      mapping.setAssetname("cloud-sha1-esx2");
      mapping.setIp("10.15");
      mapping.setMacAddress("00:50:56:be:60:62");
      MvcResult result = this.mockMvc
      .perform(post("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapping)))
      .andExpect(status().is4xxClientError())
      .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      }
   }

   @Test
   public void createHostNameAndIPMappingFailureExample2() throws Exception {
      SetOperations<String,String> setOperations = Mockito.mock(SetOperations.class);
      when(template.hasKey(anyString())).thenReturn(false);
      when(template.opsForSet()).thenReturn(setOperations);
      when(setOperations.add(anyString(), any())).thenReturn(1l);
      Asset server = createAsset();
      server.setCategory(AssetCategory.Server);
      server.setAssetName("cloud-sha1-esx2");
      assetRepository.save(server);
      AssetIPMapping mapping = createAssetIPMapping();
      mapping.setAssetname("cloud-sha1-esx8");
      mapping.setIp("192.168.0.1");
      mapping.setMacAddress("50:00:56:ge:64:62");
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Can't find any asset with the name : " + mapping.getAssetname());
      MvcResult result = this.mockMvc
      .perform(post("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapping)))
      .andReturn();
      if (result.getResolvedException() != null) {
         assetRepository.deleteById(server.getId());
         throw result.getResolvedException();
      }
   }

   @Test
   public void deleteAssetIPAndNameMappingExample() throws Exception {
      AssetIPMapping assetipmapping = createAssetIPMapping();
      assetipmapping.setAssetname("cloud-server");
      assetipmapping.setMacAddress("00:50:56:be:60:62");
      assetipmapping = assetIPMappingRepository.save(assetipmapping);
      this.mockMvc.perform(delete("/v1/assets/mapping/hostnameip/{Id}", assetipmapping.getId()))
            .andExpect(status().isOk()).andDo(document("assets-deleteAssetIPAndNameMapping-example",
                  pathParameters(
                        parameterWithName("Id").description("The id of AssetIPMapping, generated by flowgate。"))
));
   }

   @Test
   public void updateHostNameIPMappingExample() throws Exception {
      SetOperations<String,String> setOperations = Mockito.mock(SetOperations.class);
      when(template.hasKey(anyString())).thenReturn(false);
      when(template.opsForSet()).thenReturn(setOperations);
      when(setOperations.add(anyString(), any())).thenReturn(1l);
      Asset server = createAsset();
      server.setCategory(AssetCategory.Server);
      server.setAssetName("cloud-sha1-esx2");
      server = assetRepository.save(server);
      Asset server1 = createAsset();
      server1.setCategory(AssetCategory.Server);
      server1.setAssetName("cloud-sha1-esx8");
      server1 = assetRepository.save(server1);
      AssetIPMapping assetipmapping = createAssetIPMapping();
      assetipmapping.setAssetname(server.getAssetName());
      assetipmapping = assetIPMappingRepository.save(assetipmapping);

      AssetIPMapping newAssetIPMapping = createAssetIPMapping();
      newAssetIPMapping.setAssetname(server1.getAssetName());
      newAssetIPMapping.setId(assetipmapping.getId());
      newAssetIPMapping.setMacAddress("00:50:56:be:60:62");
      newAssetIPMapping.setIp("192.168.0.1");
      this.mockMvc
            .perform(put("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(newAssetIPMapping)))
            .andExpect(status().isOk())
            .andDo(document("assets-updateHostNameIPMapping-example", requestFields(
                  fieldWithPath("id").description("ID of the asset, created by flowgate"),
                  fieldWithPath("ip").description("ip of hostname"),
                  fieldWithPath("macAddress").description("macAddress of IP"),
                  fieldWithPath("assetname").description(
                        "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"))));
      assetipmapping = assetIPMappingRepository.findById(assetipmapping.getId()).get();
      assetRepository.deleteById(server.getId());
      assetRepository.deleteById(server1.getId());
      assetIPMappingRepository.deleteById(assetipmapping.getId());
      TestCase.assertEquals(server1.getAssetName(), assetipmapping.getAssetname());
      TestCase.assertEquals("00:50:56:be:60:62", assetipmapping.getMacAddress());
   }

   @Test
   public void updateHostNameIPMappingMacIsNullExample() throws Exception {
      SetOperations<String,String> setOperations = Mockito.mock(SetOperations.class);
      when(template.hasKey(anyString())).thenReturn(false);
      when(template.opsForSet()).thenReturn(setOperations);
      when(setOperations.add(anyString(), any())).thenReturn(1l);
      Asset server = createAsset();
      server.setCategory(AssetCategory.Server);
      server.setAssetName("cloud-sha1-esx2");
      server = assetRepository.save(server);
      Asset server1 = createAsset();
      server1.setCategory(AssetCategory.Server);
      server1.setAssetName("cloud-sha1-esx8");
      server1 = assetRepository.save(server1);
      AssetIPMapping assetipmapping = createAssetIPMapping();
      assetipmapping.setAssetname(server.getAssetName());
      assetipmapping = assetIPMappingRepository.save(assetipmapping);

      AssetIPMapping newAssetIPMapping = createAssetIPMapping();
      newAssetIPMapping.setAssetname(server1.getAssetName());
      newAssetIPMapping.setId(assetipmapping.getId());
      newAssetIPMapping.setMacAddress(null);
      this.mockMvc
               .perform(put("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAssetIPMapping)))
               .andExpect(status().isOk());
      assetipmapping = assetIPMappingRepository.findById(assetipmapping.getId()).get();
      assetRepository.deleteById(server.getId());
      assetRepository.deleteById(server1.getId());
      assetIPMappingRepository.deleteById(assetipmapping.getId());
      TestCase.assertEquals(server1.getAssetName(), assetipmapping.getAssetname());
      TestCase.assertNull(assetipmapping.getMacAddress());
   }

   @Test
   public void updateHostNameIPMappingFailureExample() throws Exception {
      SetOperations<String,String> setOperations = Mockito.mock(SetOperations.class);
      when(template.hasKey(anyString())).thenReturn(false);
      when(template.opsForSet()).thenReturn(setOperations);
      when(setOperations.add(anyString(), any())).thenReturn(1l);
      Asset server = createAsset();
      server.setCategory(AssetCategory.Server);
      server.setAssetName("cloud-sha1-esx2");
      server = assetRepository.save(server);
      Asset server1 = createAsset();
      server1.setCategory(AssetCategory.Server);
      server1.setAssetName("cloud-sha1-esx8");
      server1 = assetRepository.save(server1);
      AssetIPMapping assetipmapping = createAssetIPMapping();
      assetipmapping.setAssetname(server.getAssetName());
      assetipmapping = assetIPMappingRepository.save(assetipmapping);
      AssetIPMapping newAssetIPMapping = createAssetIPMapping();
      newAssetIPMapping.setId(assetipmapping.getId());
      newAssetIPMapping.setAssetname("cloud-server1");
      newAssetIPMapping.setIp("192.168.0.1");
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Can't find any asset with the name : " + newAssetIPMapping.getAssetname());
      MvcResult result = this.mockMvc
            .perform(put("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(newAssetIPMapping)))
            .andExpect(status().is5xxServerError())
            .andReturn();
      if (result.getResolvedException() != null) {
         assetRepository.deleteById(server.getId());
         assetRepository.deleteById(server1.getId());
         assetIPMappingRepository.deleteById(assetipmapping.getId());
         throw result.getResolvedException();
      }
   }

   @Test
   public void getHostNameIPMappingByPage() throws Exception {
      AssetIPMapping assetipmapping = createAssetIPMapping();
      assetipmapping.setAssetname("cloud-sha2-esx2");
      assetipmapping = assetIPMappingRepository.save(assetipmapping);
      this.mockMvc
      .perform(get("/v1/assets/mapping/hostnameip").param("pagesize", "10")
            .param("pagenumber", "1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$..totalPages").value(1))
      .andExpect(jsonPath("$..content[0].ip").value(assetipmapping.getIp()))
      .andExpect(jsonPath("$..content[0].macAddress").value(assetipmapping.getMacAddress()))
      .andExpect(jsonPath("$..content[0].assetname").value(assetipmapping.getAssetname()))
      .andDo(document("assets-getHostNameIPMappingByPage-example",requestParameters(
            parameterWithName("pagesize").description("The number of AssetIPMapping you want to get by every request.Default value:20").optional(),
            parameterWithName("pagenumber").description("The number of page you want to get").optional()),
            responseFields(subsectionWithPath("content").description("AssetIPMapping's array."),
                  fieldWithPath("totalPages").description("content's total pages."),
                  fieldWithPath("totalElements").description("content's total elements."),
                  fieldWithPath("last").description("Is the last."),
                  fieldWithPath("number").description("The page number."),
                  fieldWithPath("size").description("The page size."),
                  fieldWithPath("sort").description("The sort."),
                  fieldWithPath("numberOfElements").description("The number of Elements."),
                  fieldWithPath("first").description("Is the first."),
                  subsectionWithPath("pageable").description("pageable.").ignored(),
                  subsectionWithPath("sort").description("sorted.").ignored(),
                  fieldWithPath("empty").description("Is empty.").ignored())));
      assetIPMappingRepository.deleteById(assetipmapping.getId());
   }

   @Test
   public void getServerMappingByID() throws Exception {
      ServerMapping mapping = createServerMapping();
      mapping.setVcClusterMobID("1001");
      mapping.setVcHostName("host1");
      serverMappingRepository.save(mapping);
      this.mockMvc.perform(get("/v1/assets/mapping/{mappingId}", mapping.getId())).andExpect(status().isOk())
                  .andExpect(jsonPath("vcClusterMobID", is(mapping.getVcClusterMobID())))
                  .andExpect(jsonPath("vcHostName", is(mapping.getVcHostName())))
                  .andDo(document("assets-getServerMappingByID-example",pathParameters(
                        parameterWithName("mappingId").description("The id of serverMapping, generated by flowgate.")),
                        responseFields(
                              fieldWithPath("id").description("ID of the mapping, created by flowgate"),
                              fieldWithPath("asset").description("An asset for serverMapping."),
                              fieldWithPath("vcID").description("ID of Vcenter."),
                              fieldWithPath("vcHostName")
                                    .description("Server's hostname display in Vcenter."),
                              fieldWithPath("vcMobID").description("EXSI server's management object ID."),
                              fieldWithPath("vcClusterMobID").description("MobID of Vcenter Cluster."),
                              fieldWithPath("vcInstanceUUID").description("Vcenter's UUID."),
                              fieldWithPath("vroID").description("ID of Aria Operations."),
                              fieldWithPath("vroResourceName")
                                    .description("Resource Name in Aria Operations for this server."),
                              fieldWithPath("vroVMEntityName").description("EntityName of Resource."),
                              fieldWithPath("vroVMEntityObjectID").description("Aria Operations Entity Object ID."),
                              fieldWithPath("vroVMEntityVCID").description("Aria Operations Entity's Vcenter ID."),
                              fieldWithPath("vroResourceID").description("Aria Operations Resource ID."))));
      serverMappingRepository.deleteById(mapping.getId());
   }

   @Test
   public void readAssetExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      try {
         this.mockMvc.perform(get("/v1/assets/{assetId}", asset.getId())).andExpect(status().isOk())
               .andExpect(jsonPath("assetName", is(asset.getAssetName())))
               .andExpect(jsonPath("assetNumber", is((int) asset.getAssetNumber())))
               .andExpect(jsonPath("category", is(asset.getCategory().toString())))
               .andExpect(jsonPath("model", is(asset.getModel())))
               .andExpect(jsonPath("manufacturer", is(asset.getManufacturer())))
               .andDo(document("assets-getByID-example",
                     pathParameters(
                           parameterWithName("assetId").description("The id of asset,generated by flowgate.")),
                     responseFields(
                           fieldWithPath("id").description("ID of the asset, created by flowgate"),
                           fieldWithPath("assetNumber").description(
                                 "A unique number that can identify an asset from third part DCIM/CMDB systems.")
                                 .type(long.class),
                           fieldWithPath("assetName").description(
                                 "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"),
                           fieldWithPath("assetSource").description(
                                 "From which third part systems does this asset comes from. It will refer to a source collection which contains all the thirdpart systems"),
                           fieldWithPath("category").description(
                                 "The category of the asset. Can only be one of :Server, PDU, Cabinet, Networks, Sensors, UPS")
                                 .type(AssetCategory.class),
                           fieldWithPath("subCategory")
                                 .description("The subcategory of the asset. Only apply to some systems.")
                                 .type(AssetSubCategory.class).optional(),
                           fieldWithPath("manufacturer").description("The manufacture name"),
                           fieldWithPath("model").description("The model of the asset"),
                           fieldWithPath("serialnumber").description(
                                 "The SN number of the asset, this number can be used to identify an asset. But only some systems have this number.")
                                 .optional(),
                           fieldWithPath("tag").description(
                                 "Some system will use tag to identify an asset. It can be either an number or a string.")
                                 .type(String.class).optional(),
                           fieldWithPath("assetAddress").description("The access address of the asset")
                                 .type(AssetAddress.class).optional(),
                           fieldWithPath("region").description("The location region of the asset")
                                 .optional(),
                           fieldWithPath("country").description("The location country of the asset")
                                 .optional(),
                           fieldWithPath("city").description("The location city of the asset").optional(),
                           fieldWithPath("building").description("The location building of the asset")
                                 .optional(),
                           fieldWithPath("floor").description("The location floor of the asset")
                                 .optional(),
                           fieldWithPath("room").description("The location room of the asset"),
                           fieldWithPath("row").description("The location row of the asset").optional(),
                           fieldWithPath("col").description("The location col of the asset").optional(),
                           fieldWithPath("extraLocation")
                                 .description("Extra location information. Only valid for some system.")
                                 .optional(),
                           fieldWithPath("cabinetName").description(
                                 "The cabinet name where this asset is located. If the asset is cabinet then this filed is empty.")
                                 .optional(),
                           fieldWithPath("cabinetUnitPosition").description("The cabinet unit number")
                                 .type(int.class).optional(),
                           fieldWithPath("mountingSide").description("The cabinet unit number")
                                 .type(MountingSide.class).optional(),
                           fieldWithPath("cabinetAssetNumber").description(
                                 "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                                 .type(long.class).optional(),
                           fieldWithPath("assetRealtimeDataSpec")
                                 .description("Only valid for sensor type of asset.")
                                 .type(AssetRealtimeDataSpec.class).optional(),
                           subsectionWithPath("justificationfields")
                                 .description("Justification fields that input by user."),
                           subsectionWithPath("metricsformulars")
                                 .description("The sensor data generator logic for this asset."),
                           fieldWithPath("lastupdate").description("When this asset was last upated"),
                           fieldWithPath("created").description("When this asset was created"),
                           fieldWithPath("capacity").description("The capacity of asset.").type(int.class)
                                 .optional(),
                           fieldWithPath("freeCapacity").description("The free capacity of asset.")
                                 .type(int.class).optional(),
                           fieldWithPath("parent").description(
                                 "The parent of asset,it will be null unless the asset's category is Sensors")
                                 .type(Parent.class).optional(),
                           fieldWithPath("pdus")
                                 .description("Possible PDUs that this server connected with"),
                           fieldWithPath("switches")
                                 .description("Physical switchs that this host connected with"),
                           fieldWithPath("tenant").description("Tenant information for the asset")
                                 .type(Tenant.class).optional(),
                           subsectionWithPath("status").description(
                                 "This is a collection of states, including the state of the asset, "
                                       + "the state of the pdu mapping, and the state of the switch mapping."))));
      } finally {
         assetRepository.deleteById(asset.getId());
      }
   }

   @Test
   public void getAssetByNameExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      try {
         this.mockMvc.perform(get("/v1/assets/name/{assetName}", asset.getAssetName()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("assetName", is(asset.getAssetName())))
               .andExpect(jsonPath("assetNumber", is((int) asset.getAssetNumber())))
               .andExpect(jsonPath("category", is(asset.getCategory().toString())))
               .andExpect(jsonPath("model", is(asset.getModel())))
               .andExpect(jsonPath("manufacturer", is(asset.getManufacturer())))
               .andDo(document("assets-getAssetByName-example",
                     pathParameters(
                           parameterWithName("assetName").description("The name of asset.")),
                     responseFields(
                           fieldWithPath("id").description("ID of the asset, created by flowgate"),
                           fieldWithPath("assetNumber").description(
                                 "A unique number that can identify an asset from third part DCIM/CMDB systems.")
                                 .type(long.class),
                           fieldWithPath("assetName").description(
                                 "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"),
                           fieldWithPath("assetSource").description(
                                 "From which third part systems does this asset comes from. It will refer to a source collection which contains all the thirdpart systems"),
                           fieldWithPath("category").description(
                                 "The category of the asset. Can only be one of :Server, PDU, Cabinet, Networks, Sensors, UPS")
                                 .type(AssetCategory.class),
                           fieldWithPath("subCategory")
                                 .description("The subcategory of the asset. Only apply to some systems.")
                                 .type(AssetSubCategory.class).optional(),
                           fieldWithPath("manufacturer").description("The manufacture name"),
                           fieldWithPath("model").description("The model of the asset"),
                           fieldWithPath("serialnumber").description(
                                 "The SN number of the asset, this number can be used to identify an asset. But only some systems have this number.")
                                 .optional(),
                           fieldWithPath("tag").description(
                                 "Some system will use tag to identify an asset. It can be either an number or a string.")
                                 .type(String.class).optional(),
                           fieldWithPath("assetAddress").description("The access address of the asset")
                                 .type(AssetAddress.class).optional(),
                           fieldWithPath("region").description("The location region of the asset")
                                 .optional(),
                           fieldWithPath("country").description("The location country of the asset")
                                 .optional(),
                           fieldWithPath("city").description("The location city of the asset").optional(),
                           fieldWithPath("building").description("The location building of the asset")
                                 .optional(),
                           fieldWithPath("floor").description("The location floor of the asset")
                                 .optional(),
                           fieldWithPath("room").description("The location room of the asset"),
                           fieldWithPath("row").description("The location row of the asset").optional(),
                           fieldWithPath("col").description("The location col of the asset").optional(),
                           fieldWithPath("extraLocation")
                                 .description("Extra location information. Only valid for some system.")
                                 .optional(),
                           fieldWithPath("cabinetName").description(
                                 "The cabinet name where this asset is located. If the asset is cabinet then this filed is empty.")
                                 .optional(),
                           fieldWithPath("cabinetUnitPosition").description("The cabinet unit number")
                                 .type(int.class).optional(),
                           fieldWithPath("mountingSide").description("The cabinet unit number")
                                 .type(MountingSide.class).optional(),
                           fieldWithPath("cabinetAssetNumber").description(
                                 "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                                 .type(long.class).optional(),
                           fieldWithPath("assetRealtimeDataSpec")
                                 .description("Only valid for sensor type of asset.")
                                 .type(AssetRealtimeDataSpec.class).optional(),
                           subsectionWithPath("justificationfields")
                                 .description("Justification fields that input by user."),
                           subsectionWithPath("metricsformulars")
                                 .description("The sensor data generator logic for this asset."),
                           fieldWithPath("lastupdate").description("When this asset was last upated"),
                           fieldWithPath("created").description("When this asset was created"),
                           fieldWithPath("pdus")
                                 .description("Possible PDUs that this server connected with"),
                           fieldWithPath("capacity").description("The capacity of asset.").type(int.class)
                                 .optional(),
                           fieldWithPath("freeCapacity").description("The free capacity of asset.")
                                 .type(int.class).optional(),
                           subsectionWithPath("parent").description(
                                 "The parent of asset,it will be null unless the asset's category is Sensors")
                                 .type(Parent.class).optional(),
                           fieldWithPath("switches")
                                 .description("Physical switchs that this host connected with"),
                           fieldWithPath("tenant").description("Tenant information for the asset")
                                 .type(Tenant.class).optional(),
                           subsectionWithPath("status").description(
                                 "This is a collection of states, including the state of the asset, "
                                       + "the state of the pdu mapping, and the state of the switch mapping."))));

      } finally {
         assetRepository.deleteById(asset.getId());
      }
   }

   @Test
   public void updateAssetExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      asset.setAssetName("pek-wor-server-04");
      asset.setManufacturer("VMware");
      asset.setAssetSource("4");
      try {
         this.mockMvc
               .perform(put("/v1/assets").contentType(MediaType.APPLICATION_JSON)
                     .content(objectMapper.writeValueAsString(asset)))
               .andExpect(status().isOk())
               .andDo(document("assets-update-example", requestFields(
                     fieldWithPath("id").description("ID of the asset, created by flowgate"),
                     fieldWithPath("assetNumber").description(
                           "A unique number that can identify an asset from third part DCIM/CMDB systems.")
                           .type(long.class),
                     fieldWithPath("assetName").description(
                           "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"),
                     fieldWithPath("assetSource").description(
                           "From which third part systems does this asset comes from. It will refer to a source collection which contains all the thirdpart systems"),
                     fieldWithPath("category").description(
                           "The category of the asset. Can only be one of :Server, PDU, Cabinet, Networks, Sensors, UPS")
                           .type(AssetCategory.class),
                     fieldWithPath("subCategory")
                           .description("The subcategory of the asset. Only apply to some systems.")
                           .type(AssetSubCategory.class).optional(),
                     fieldWithPath("manufacturer").description("The manufacture name"),
                     fieldWithPath("model").description("The model of the asset"),
                     fieldWithPath("serialnumber").description(
                           "The SN number of the asset, this number can be used to identify an asset. But only some systems have this number.")
                           .optional(),
                     fieldWithPath("tag").description(
                           "Some system will use tag to identify an asset. It can be either an number or a string.")
                           .type(String.class).optional(),
                     fieldWithPath("assetAddress").description("The access address of the asset")
                           .type(AssetAddress.class).optional(),
                     fieldWithPath("region").description("The location region of the asset")
                           .optional(),
                     fieldWithPath("country").description("The location country of the asset")
                           .optional(),
                     fieldWithPath("city").description("The location city of the asset").optional(),
                     fieldWithPath("building").description("The location building of the asset")
                           .optional(),
                     fieldWithPath("floor").description("The location floor of the asset")
                           .optional(),
                     fieldWithPath("room").description("The location room of the asset"),
                     fieldWithPath("row").description("The location row of the asset").optional(),
                     fieldWithPath("col").description("The location col of the asset").optional(),
                     fieldWithPath("extraLocation")
                           .description("Extra location information. Only valid for some system.")
                           .optional(),
                     fieldWithPath("cabinetName").description(
                           "The cabinet name where this asset is located. If the asset is cabinet then this filed is empty.")
                           .optional(),
                     fieldWithPath("cabinetUnitPosition").description("The cabinet unit number")
                           .type(int.class).optional(),
                     fieldWithPath("mountingSide").description("The cabinet unit number")
                           .type(MountingSide.class).optional(),
                     fieldWithPath("cabinetAssetNumber").description(
                           "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                           .type(long.class).optional(),
                     fieldWithPath("assetRealtimeDataSpec")
                           .description("Only valid for sensor type of asset.")
                           .type(AssetRealtimeDataSpec.class).optional(),
                     subsectionWithPath("justificationfields").ignored(),
                     subsectionWithPath("metricsformulars").ignored(),
                     fieldWithPath("lastupdate").ignored(), fieldWithPath("created").ignored(),
                     fieldWithPath("capacity").description("The capacity of asset.").type(int.class)
                           .optional(),
                     fieldWithPath("freeCapacity").description("The free capacity of asset.")
                           .type(int.class).optional(),
                     subsectionWithPath("parent").description(
                           "The parent of asset,it will be null unless the asset's category is Sensors")
                           .type(Parent.class).optional(),
                     fieldWithPath("pdus")
                           .description("Possible PDUs that this server connected with"),
                     fieldWithPath("switches")
                           .description("Physical switchs that this host connected with"),
                     fieldWithPath("tenant").description("Tenant information for the asset")
                           .type(Tenant.class).optional(),
                     subsectionWithPath("status").description(
                           "This is a collection of states, including the state of the asset, "
                                 + "the state of the pdu mapping, and the state of the switch mapping."))));

      } finally {
         assetRepository.deleteById(asset.getId());
      }
   }

   @Test
   public void updateServerMappingExample() throws Exception {

      ServerMapping mapping = createServerMapping();
      serverMappingRepository.save(mapping);
      mapping.setVcClusterMobID("1");
      mapping.setVcHostName("1");
      try {
         this.mockMvc
         .perform(put("/v1/assets/mapping").contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(mapping)))
         .andExpect(status().isOk())
         .andDo(document("assets-updateServerMapping-example",
               requestFields(
                     fieldWithPath("id").description("ID of the mapping, created by flowgate"),
                     fieldWithPath("asset").description("An asset for serverMapping."),
                     fieldWithPath("vcID").description("ID of Vcenter."),
                     fieldWithPath("vcHostName")
                           .description("Server's hostname display in Vcenter."),
                     fieldWithPath("vcMobID").description("EXSI server's management object ID."),
                     fieldWithPath("vcClusterMobID").description("MobID of Vcenter Cluster."),
                     fieldWithPath("vcInstanceUUID").description("Vcenter's UUID."),
                     fieldWithPath("vroID").description("ID of Aria Operations."),
                     fieldWithPath("vroResourceName")
                           .description("Resource Name in Aria Operations for this server."),
                     fieldWithPath("vroVMEntityName").description("EntityName of Resource."),
                     fieldWithPath("vroVMEntityObjectID").description("Aria Operations Entity Object ID."),
                     fieldWithPath("vroVMEntityVCID").description("Aria Operations Entity's Vcenter ID."),
                     fieldWithPath("vroResourceID").description("Aria Operations Resource ID."))));

      }finally {
         serverMappingRepository.deleteById(mapping.getId());
      }
   }

   @Test
   public void deleteServerMappingExample() throws Exception {
      ServerMapping mapping = createServerMapping();
      ServerMapping returnMapping = serverMappingRepository.save(mapping);

      this.mockMvc.perform(delete("/v1/assets/mapping/{mappingId}", returnMapping.getId()))
            .andExpect(status().isOk()).andDo(document("assets-deleteServerMapping-example",
                  pathParameters(
                        parameterWithName("mappingId").description("The id of serverMapping, generated by flowgate.")
                        )));
   }

   @Test
   public void mergeServerMappingExample() throws Exception {

      ServerMapping mapping1 = createServerMapping();
      serverMappingRepository.save(mapping1);
      ServerMapping mapping2 = createServerMapping();
      serverMappingRepository.save(mapping2);

      try {
         this.mockMvc
         .perform(put("/v1/assets/mapping/merge/{mapping1Id}/{mapping2Id}", mapping1.getId(), mapping2.getId())
               )
         .andExpect(status().isOk())
         .andDo(document("assets-mergeServerMapping-example",
               pathParameters(
                     parameterWithName("mapping1Id").description("The id of serverMapping, generated by flowgate."),
                     parameterWithName("mapping2Id").description("The id of serverMapping, generated by flowgate.")
                     )));
      }finally {
         serverMappingRepository.deleteById(mapping1.getId());
      }
   }

   @Test
   public void assetDeleteExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      this.mockMvc
            .perform(delete("/v1/assets/{assetId}", asset.getId()))
            .andExpect(status().isOk()).andDo(document("assets-delete-example",
                  pathParameters(
                        parameterWithName("assetId").description("The id of asset,generated by flowgate.")
                        )));
   }

   ServerMapping createServerMapping() {
      ServerMapping mapping = new ServerMapping();
      mapping.setId(UUID.randomUUID().toString());
      mapping.setVcHostName("mappinghostname");
      mapping.setVroResourceName("mappingresourcename");
      mapping.setVroID("1");
      mapping.setVcID("10086");
      mapping.setVcMobID("10010");
      mapping.setVcClusterMobID("12345");
      mapping.setVroResourceID("110");
      mapping.setVroVMEntityVCID("123");
      mapping.setVcInstanceUUID("12345");
      mapping.setVroVMEntityObjectID("123");
      return mapping;
   }

   @Test
   public void getAssetByAssetNumberAndName() throws Exception {
      Asset asset = createAsset();
      asset.setAssetNumber(53968);
      asset.setAssetName("SHA-pdu1");
      asset = assetRepository.save(asset);
      try {
         this.mockMvc.perform(get("/v1/assets/assetnumber/{assetNumber}/assetname/{assetName}","53968","SHA-pdu1"))
               .andExpect(status().isOk()).andExpect(jsonPath("assetName", is("SHA-pdu1")))
               .andExpect(jsonPath("assetNumber", is(53968)))
               .andDo(document("assets-getAssetByAssetNumberAndName-example",
                     pathParameters(
                           parameterWithName("assetNumber").description("Asset number, generally generated by DCIM."),
                           parameterWithName("assetName").description("The name of asset, generally generated by DCIM.")),
                     responseFields(
                           fieldWithPath("id").description("ID of the asset, created by flowgate"),
                           fieldWithPath("assetNumber").description(
                                 "A unique number that can identify an asset from third part DCIM/CMDB systems.")
                                 .type(long.class),
                           fieldWithPath("assetName").description(
                                 "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"),
                           fieldWithPath("assetSource").description(
                                 "From which third part systems does this asset comes from. It will refer to a source collection which contains all the thirdpart systems"),
                           fieldWithPath("category").description(
                                 "The category of the asset. Can only be one of :Server, PDU, Cabinet, Networks, Sensors, UPS")
                                 .type(AssetCategory.class),
                           fieldWithPath("subCategory")
                                 .description("The subcategory of the asset. Only apply to some systems.")
                                 .type(AssetSubCategory.class).optional(),
                           fieldWithPath("manufacturer").description("The manufacture name"),
                           fieldWithPath("model").description("The model of the asset"),
                           fieldWithPath("serialnumber").description(
                                 "The SN number of the asset, this number can be used to identify an asset. But only some systems have this number.")
                                 .optional(),
                           fieldWithPath("tag").description(
                                 "Some system will use tag to identify an asset. It can be either an number or a string.")
                                 .type(String.class).optional(),
                           fieldWithPath("assetAddress").description("The access address of the asset")
                                 .type(AssetAddress.class).optional(),
                           fieldWithPath("region").description("The location region of the asset")
                                 .optional(),
                           fieldWithPath("country").description("The location country of the asset")
                                 .optional(),
                           fieldWithPath("city").description("The location city of the asset").optional(),
                           fieldWithPath("building").description("The location building of the asset")
                                 .optional(),
                           fieldWithPath("floor").description("The location floor of the asset")
                                 .optional(),
                           fieldWithPath("room").description("The location room of the asset"),
                           fieldWithPath("row").description("The location row of the asset").optional(),
                           fieldWithPath("col").description("The location col of the asset").optional(),
                           fieldWithPath("extraLocation")
                                 .description("Extra location information. Only valid for some system.")
                                 .optional(),
                           fieldWithPath("cabinetName").description(
                                 "The cabinet name where this asset is located. If the asset is cabinet then this filed is empty.")
                                 .optional(),
                           fieldWithPath("cabinetUnitPosition").description("The cabinet unit number")
                                 .type(int.class).optional(),
                           fieldWithPath("mountingSide").description("The cabinet unit number")
                                 .type(MountingSide.class).optional(),
                           fieldWithPath("cabinetAssetNumber").description(
                                 "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                                 .type(long.class).optional(),
                           fieldWithPath("assetRealtimeDataSpec")
                                 .description("Only valid for sensor type of asset.")
                                 .type(AssetRealtimeDataSpec.class).optional(),
                           subsectionWithPath("justificationfields")
                                 .description("Justification fields that input by user."),
                           subsectionWithPath("metricsformulars")
                                 .description("The sensor data generator logic for this asset."),
                           fieldWithPath("lastupdate").description("When this asset was last upated"),
                           fieldWithPath("created").description("When this asset was created"),
                           fieldWithPath("pdus")
                                 .description("Possible PDUs that this server connected with"),
                           fieldWithPath("capacity").description("The capacity of asset.").type(int.class)
                                 .optional(),
                           fieldWithPath("freeCapacity").description("The free capacity of asset.")
                                 .type(int.class).optional(),
                           fieldWithPath("parent").description(
                                 "The parent of asset,it will be null unless the asset's category is Sensors")
                                 .type(Parent.class).optional(),
                           fieldWithPath("switches")
                                 .description("Physical switchs that this host connected with"),
                           fieldWithPath("tenant").description("Tenant information for the asset")
                                 .type(Tenant.class).optional(),
                           subsectionWithPath("status").description(
                           "This is a collection of states, including the state of the asset, "
                                 + "the state of the pdu mapping, and the state of the switch mapping."))));

      } finally {
         assetRepository.deleteById(asset.getId());
      }
   }

   @Test
   public void getServerMetricsData() throws Exception {
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("metricName").description("metric name").type(JsonFieldType.STRING),
            fieldWithPath("valueNum").description("valueNum.").type(JsonFieldType.NUMBER),
            fieldWithPath("value").description("value").type(JsonFieldType.NULL),
            fieldWithPath("unit").description("metric unit").type(JsonFieldType.STRING),
            fieldWithPath("timeStamp").description("timeStamp").type(JsonFieldType.NUMBER) };
      Asset asset = createAsset();
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      Long currentTime = System.currentTimeMillis();
      RealTimeData pduRealTimeData = createServerPDURealTimeData(currentTime);
      pduRealTimeData.setAssetID("0001bdc8b25d4c2badfd045ab61aabfa");
      RealTimeData sensorRealTimeData = createSensorRealtimeData(currentTime);
      sensorRealTimeData.setAssetID("00027ca37b004a9890d1bf20349d5ac1");
      RealTimeData serverHostRealTimeData = createServerHostRealTimeData(currentTime);
      serverHostRealTimeData.setAssetID(asset.getId());
      realTimeDatas.add(pduRealTimeData);
      realTimeDatas.add(sensorRealTimeData);
      realTimeDatas.add(serverHostRealTimeData);
      Iterable<RealTimeData> result = realtimeDataRepository.saveAll(realTimeDatas);

      HashMap<String, String> justificationfields = new HashMap<>();
      justificationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, "power-2_FIELDSPLIT_CAN1-MDF-R01-PDU-BUILDING_FIELDSPLIT_OUTLET:7_FIELDSPLIT_0001bdc8b25d4c2badfd045ab61aabfa");
      asset.setJustificationfields(justificationfields);
      asset = fillingMetricsformula(asset);
      asset = assetRepository.save(asset);

      MvcResult result1 = this.mockMvc
            .perform(get("/v1/assets/server/{assetId}/realtimedata", asset.getId()).param("starttime",
                  String.valueOf(currentTime)).param("duration", "300000"))
            .andDo(document("assets-getServerMetricsData-example",
                  pathParameters(
                        parameterWithName("assetId").description("The id of asset,generated by flowgate.")),
                  requestParameters(
                        parameterWithName("starttime").description("Start time of you want to query.Default value: the system current time in Millis").optional(),
                        parameterWithName("duration").description("Duration of you want to query.Default value: 300000 ms").optional()),
                  responseFields(fieldWithPath("[]").description("An array of realTimeDatas"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn();
      String res = result1.getResponse().getContentAsString();
      MetricData [] datas = mapper.readValue(res, MetricData[].class);
      try {
         for(MetricData serverdata:datas) {
            String metricName = serverdata.getMetricName();
            if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:7").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 0.633);
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_POWER, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:7").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 1.033);
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:7").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 226.0);
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_POWER_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 0.134);
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_CURRENT_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 0.1427);
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 4.566);
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_POWER, "0001bdc8b25d4c2badfd045ab61aabfa").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 1.033);
            }else if(String.format(MetricKeyName.SERVER_BACK_HUMIDITY_LOCATIONX, "OUTLET").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 20.0);
            }else if(String.format(MetricKeyName.SERVER_BACK_TEMPREATURE_LOCATIONX, "OUTLET").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 32.0);
            }else if(String.format(MetricKeyName.SERVER_FRONT_HUMIDITY_LOCATIONX, "INLET").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 20.0);
            }else if(String.format(MetricKeyName.SERVER_FRONT_TEMPERATURE_LOCATIONX, "INLET").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 32.0);
            }else if(MetricName.SERVER_VOLTAGE.equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 226.0);
            }else if(MetricName.SERVER_STORAGEUSAGE.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.0);
               } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               } else if (serverdata.getTimeStamp() == currentTime + 300000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 65.0);
               }
            }else if(MetricName.SERVER_MEMORYUSAGE.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               } else if (serverdata.getTimeStamp() == currentTime + 300000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 87.22);
               }
            }else if(MetricName.SERVER_CPUUSEDINMHZ.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 746.00);
               } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 570.00);
               } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 552.00);
               } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 844.00);
               } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 651.00);
               } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 566.00);
               } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 552.00);
               } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 569.00);
               } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 538.00);
               } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 836.00);
               } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 655.00);
               } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 565.00);
               } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 571.00);
               } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 551.00);
               } else if (serverdata.getTimeStamp() == currentTime + 300000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 570.00);
               }
            }else if(MetricName.SERVER_CPUUSAGE.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 4.67);
               } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 3.57);
               } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 3.46);
               } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 5.28);
               } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 4.08);
               } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 3.55);
               } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 3.45);
               } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 3.56);
               } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 3.37);
               } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 5.23);
               } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 4.1);
               } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 3.53);
               } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 3.57);
               } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 3.45);
               } else if (serverdata.getTimeStamp() == currentTime + 300000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 3.57);
               }
            }else if(MetricName.SERVER_ACTIVEMEMORY.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 1561416.00);
               } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 1561416.00);
               } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 2065824.00);
               } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 2065824.00);
               } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 2065824.00);
               } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 2065428.00);
               } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 2065428.00);
               } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 2065428.00);
               } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 1729924.00);
               } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 1729924.00);
               } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 1729924.00);
               } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 1561072.00);
               } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 1561072.00);
               } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 1561072.00);
               } else if (serverdata.getTimeStamp() == currentTime + 300000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 2063852.00);
               }
            }else if(MetricName.SERVER_SHAREDMEMORY.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               } else if (serverdata.getTimeStamp() == currentTime + 300000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 8.00);
               }
            }else if(MetricName.SERVER_CONSUMEDMEMORY.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291220.00);
               } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291236.00);
               } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291236.00);
               } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291236.00);
               } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291252.00);
               } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291252.00);
               } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291252.00);
               } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291156.00);
               } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291156.00);
               } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291200.00);
               } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291060.00);
               } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291172.00);
               } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291172.00);
               } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291188.00);
               } else if (serverdata.getTimeStamp() == currentTime + 300000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18291188.00);
               }
            }else if(MetricName.SERVER_SWAPMEMORY.equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 0.00);
            }else if(MetricName.SERVER_BALLOONMEMORY.equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 0.0);
            }else if(MetricName.SERVER_NETWORKUTILIZATION.equals(metricName)){
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 146.00);
               } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 18.00);
               } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 15.00);
               } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 16.00);
               } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 16.00);
               } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 12.00);
               } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 9.00);
               } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 16.00);
               } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 10.00);
               } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 16.00);
               } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 17.00);
               } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 12.00);
               } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 12.00);
               } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 19.00);
               } else if (serverdata.getTimeStamp() == currentTime + 300000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 29.00);
               }
            }else if(MetricName.SERVER_STORAGEIORATEUSAGE.equals(metricName)){
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 330.00);
               } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 98.00);
               } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 57.00);
               } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 852.00);
               } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 224.00);
               } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 95.00);
               } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 209.00);
               } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 66.00);
               } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 56.00);
               } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 798.00);
               } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 236.00);
               } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 81.00);
               } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 213.00);
               } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 63.00);
               } else if (serverdata.getTimeStamp() == currentTime + 300000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 61.00);
               }
            } else if (MetricName.SERVER_POWER.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.56);
               } else {
                  if (serverdata.getTimeStamp() == currentTime + 20000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.070);
                  } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  } else if (serverdata.getTimeStamp() == currentTime + 300000) {
                     TestCase.assertEquals(serverdata.getValueNum(), 0.069);
                  }
               }
            } else if (MetricName.SERVER_PEAK_USED_POWER.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.070);
               } else {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.80);
               }
            } else if (MetricName.SERVER_MINIMUM_USED_POWER.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.069);
               } else {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.50);
               }
            } else if (MetricName.SERVER_AVERAGE_USED_POWER.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.06906666666666665);
               } else {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.60);
               }
            } else if (MetricName.SERVER_ENERGY_CONSUMPTION.equals(metricName)) {
               if (serverdata.getTimeStamp() == currentTime) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.00038805555555555555);
               } else if (serverdata.getTimeStamp() == currentTime + 20000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.0003833333333333333);
               } else if (serverdata.getTimeStamp() == currentTime + 40000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.0003833333333333333);
               } else if (serverdata.getTimeStamp() == currentTime + 60000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.0003869444444444444);
               } else if (serverdata.getTimeStamp() == currentTime + 80000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.00038888888888888887);
               } else if (serverdata.getTimeStamp() == currentTime + 100000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.00038805555555555555);
               } else if (serverdata.getTimeStamp() == currentTime + 120000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.0003833333333333333);
               } else if (serverdata.getTimeStamp() == currentTime + 140000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.0003833333333333333);
               } else if (serverdata.getTimeStamp() == currentTime + 160000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.0003833333333333333);
               } else if (serverdata.getTimeStamp() == currentTime + 180000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.00038555555555555554);
               } else if (serverdata.getTimeStamp() == currentTime + 200000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.00038805555555555555);
               } else if (serverdata.getTimeStamp() == currentTime + 220000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.0003833333333333333);
               } else if (serverdata.getTimeStamp() == currentTime + 240000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.0003833333333333333);
               } else if (serverdata.getTimeStamp() == currentTime + 260000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.0003833333333333333);
               } else if (serverdata.getTimeStamp() == currentTime + 280000) {
                  TestCase.assertEquals(serverdata.getValueNum(), 0.00038555555555555554);
               } else {
                  TestCase.assertEquals(serverdata.getValueNum(), 356.00);
               }
            } else if (MetricName.SERVER_AVERAGE_TEMPERATURE.equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 24.00);
            } else if (MetricName.SERVER_PEAK_TEMPERATURE.equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 30.00);
            } else {
               TestCase.fail("Unknown metric :" + metricName);
            }
          }
      }finally {
         assetRepository.deleteById(asset.getId());
         realtimeDataRepository.deleteById(pduRealTimeData.getId());
         realtimeDataRepository.deleteById(sensorRealTimeData.getId());
         realtimeDataRepository.deleteById(serverHostRealTimeData.getId());
      }
   }

   @Test
   public void getPduMetricsDataById() throws Exception {
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("metricName").description("metric name").type(JsonFieldType.STRING),
            fieldWithPath("valueNum").description("valueNum.").type(JsonFieldType.NUMBER),
            fieldWithPath("value").description("value").type(JsonFieldType.NULL),
            fieldWithPath("unit").description("metric unit").type(JsonFieldType.STRING),
            fieldWithPath("timeStamp").description("timeStamp").type(JsonFieldType.NUMBER) };
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      Long currentTime = System.currentTimeMillis();
      RealTimeData pduRealTimeData = createPduRealTimeData(currentTime);
      pduRealTimeData.setAssetID("00040717c4154b5b924ced78eafcea7a");

      RealTimeData sensorRealTimeData = createSensorRealtimeData(currentTime);
      sensorRealTimeData.setAssetID("00027ca37b004a9890d1bf20349d5ac1");
      realTimeDatas.add(pduRealTimeData);
      realTimeDatas.add(sensorRealTimeData);
      Iterable<RealTimeData> result = realtimeDataRepository.saveAll(realTimeDatas);

      Asset asset = createAsset();
      Map<String, String> formulas = new HashMap<String, String>();

      Map<String, Map<String, String>> sensorMetricFormulas = new HashMap<String, Map<String, String>>();
      Map<String, String> tempSensor = new HashMap<String,String>();
      tempSensor.put("INLET", "00027ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulas.put(MetricName.PDU_TEMPERATURE, tempSensor);

      Map<String, String> humiditySensor = new HashMap<String,String>();
      humiditySensor.put("OUTLET", "00027ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulas.put(MetricName.PDU_HUMIDITY, humiditySensor);

      String sensorFormulaInfo = null;
      try {
         sensorFormulaInfo = mapper.writeValueAsString(sensorMetricFormulas);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      formulas.put(FlowgateConstant.SENSOR, sensorFormulaInfo);
      asset.setCategory(AssetCategory.PDU);
      asset.setMetricsformulars(formulas);
      asset.setId("00040717c4154b5b924ced78eafcea7a");
      asset = assetRepository.save(asset);

      MvcResult result1 = this.mockMvc
            .perform(get("/v1/assets/pdu/{assetId}/realtimedata", asset.getId()).param("starttime",
                  String.valueOf(currentTime)).param("duration", "300000"))
            .andDo(document("assets-getPduMetricsData-example",
                  pathParameters(
                        parameterWithName("assetId").description("The id of asset,generated by flowgate。")),
                  requestParameters(
                        parameterWithName("starttime").description("Start time of you want to query.Default value: the system current time in Millis").optional(),
                        parameterWithName("duration").description("Duration of you want to query.Default value: 300000 ms").optional()),
                  responseFields(fieldWithPath("[]").description("An array of realTimeDatas"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn();
      String res = result1.getResponse().getContentAsString();
      MetricData [] datas = mapper.readValue(res, MetricData[].class);
      try {
         for(MetricData pduMetricdata:datas) {
            String metricName = pduMetricdata.getMetricName();
            if(String.format(MetricName.PDU_XLET_ACTIVE_POWER,"OUTLET:7").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 0.2);
            }else if(String.format(MetricName.PDU_XLET_APPARENT_POWER,"OUTLET:7").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 1.033);
            }else if(String.format(MetricName.PDU_XLET_CURRENT,"OUTLET:7").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 0.633);
            }else if(String.format(MetricName.PDU_XLET_FREE_CAPACITY, "OUTLET:7").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 20.0);
            }else if(String.format(MetricName.PDU_XLET_VOLTAGE, "OUTLET:7").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 226.0);
            }else if(String.format(MetricKeyName.PDU_HUMIDITY_LOCATIONX, "OUTLET").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 20.0);
            }else if(String.format(MetricKeyName.PDU_TEMPERATURE_LOCATIONX, "INLET").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 32.0);
            }else if(MetricName.PDU_CURRENT_LOAD.equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 0.1427);
            }else if(MetricName.PDU_POWER_LOAD.equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 0.134);
            }else if(MetricName.PDU_TOTAL_CURRENT.equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 4.566);
            }else if(MetricName.PDU_TOTAL_POWER.equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 1.033);
            }else if(String.format(MetricName.PDU_INLET_XPOLE_CURRENT, "INLET:1","L1").
                  equals(metricName)){
               TestCase.assertEquals(pduMetricdata.getValueNum(), 6.0);
            }else if(String.format(MetricName.PDU_INLET_XPOLE_FREE_CAPACITY, "INLET:1","L1").
                  equals(metricName)){
               TestCase.assertEquals(pduMetricdata.getValueNum(), 34.0);
            }else if(String.format(MetricName.PDU_INLET_XPOLE_VOLTAGE, "INLET:1","L1").
                  equals(metricName)){
               TestCase.assertEquals(pduMetricdata.getValueNum(), 220.0);
            }else if(String.format(MetricName.PDU_INLET_XPOLE_CURRENT, "INLET:2","L1").
                  equals(metricName)){
               TestCase.assertEquals(pduMetricdata.getValueNum(), 6.0);
            }else if(String.format(MetricName.PDU_INLET_XPOLE_FREE_CAPACITY, "INLET:2","L1").
                  equals(metricName)){
               TestCase.assertEquals(pduMetricdata.getValueNum(), 24.0);
            }else if(String.format(MetricName.PDU_INLET_XPOLE_VOLTAGE, "INLET:2","L1").
                  equals(metricName)){
               TestCase.assertEquals(pduMetricdata.getValueNum(), 240.0);
            }else {
               TestCase.fail("Unkown metric");
            }
          }
      }finally {
         assetRepository.deleteById(asset.getId());
         realtimeDataRepository.deleteById(pduRealTimeData.getId());
         realtimeDataRepository.deleteById(sensorRealTimeData.getId());
      }
   }

   @Test
   public void testRealtimedataPDUExample() throws Exception {
      Asset pduAsset = createPDU();
      pduAsset = assetRepository.save(pduAsset);
      List<RealTimeData> datas = new ArrayList<>();
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      long startTime = time - duration;
      RealTimeData pduUsageMetricData = createPduAllRealTimeData(startTime);
      pduUsageMetricData.setAssetID(pduAsset.getId());
      RealTimeData tempRealTimeData =
               createTemperatureSensorRealtimeData(startTime, "00027ca37b004a9890d1bf20349d5ac1");
      RealTimeData humdityRealTimeData =
               createHumiditySensorRealtimeData(startTime, "34527ca37b004a9890d1bf20349d5ac1");
      datas.add(humdityRealTimeData);
      datas.add(tempRealTimeData);
      datas.add(pduUsageMetricData);
      realtimeDataRepository.saveAll(datas);

      MvcResult result1 = this.mockMvc
               .perform(get("/v1/assets/{assetId}/realtimedata", pduAsset.getId()).param("starttime",
                        String.valueOf(startTime)).param("duration", String.valueOf(duration)))
               .andReturn();
      String res = result1.getResponse().getContentAsString();
      MetricData[] metricDatas = mapper.readValue(res, MetricData[].class);


      int metricResultSize = pduUsageMetricData.getValues().size() + tempRealTimeData.getValues().size() + humdityRealTimeData.getValues().size();
      TestCase.assertEquals(metricResultSize, metricDatas.length);

      try {
         for(MetricData pduMetricdata : metricDatas) {
            String metricName = pduMetricdata.getMetricName();
            if(String.format(MetricName.PDU_XLET_ACTIVE_POWER,"OUTLET:1").
                     equals(metricName)) {
               TestCase.assertEquals(0.054, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_XLET_APPARENT_POWER,"OUTLET:1").
                     equals(metricName)) {
               TestCase.assertEquals(0.081, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_XLET_CURRENT,"OUTLET:1").
                     equals(metricName)) {
               TestCase.assertEquals(0.365, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_XLET_FREE_CAPACITY, "OUTLET:1").
                     equals(metricName)) {
               TestCase.assertEquals(9.635, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_XLET_VOLTAGE, "OUTLET:1").
                     equals(metricName)) {
               TestCase.assertEquals(221.0, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_XLET_ACTIVE_POWER,"OUTLET:2").
                     equals(metricName)) {
               TestCase.assertEquals(0.2, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_XLET_APPARENT_POWER,"OUTLET:2").
                     equals(metricName)) {
               TestCase.assertEquals(0.241, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_XLET_CURRENT,"OUTLET:2").
                     equals(metricName)) {
               TestCase.assertEquals(1.09, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_XLET_FREE_CAPACITY, "OUTLET:2").
                     equals(metricName)) {
               TestCase.assertEquals(8.91, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_XLET_VOLTAGE, "OUTLET:2").
                     equals(metricName)) {
               TestCase.assertEquals(221.0, pduMetricdata.getValueNum());
            }
            else if(String.format(MetricKeyName.PDU_HUMIDITY_LOCATIONX, "OUTLET").
                     equals(metricName)) {
               TestCase.assertEquals(20.0, pduMetricdata.getValueNum());
            }else if(String.format(MetricKeyName.PDU_TEMPERATURE_LOCATIONX, "INLET").
                     equals(metricName)) {
               TestCase.assertEquals(32.0, pduMetricdata.getValueNum());
            }else if(MetricName.PDU_CURRENT_LOAD.equals(metricName)) {
               TestCase.assertEquals(0.05, pduMetricdata.getValueNum());
            }else if(MetricName.PDU_POWER_LOAD.equals(metricName)) {
               TestCase.assertEquals(0.05, pduMetricdata.getValueNum());
            }else if(MetricName.PDU_TOTAL_CURRENT.equals(metricName)) {
               TestCase.assertEquals(1.455, pduMetricdata.getValueNum());
            }else if(MetricName.PDU_TOTAL_POWER.equals(metricName)) {
               TestCase.assertEquals(0.322, pduMetricdata.getValueNum());
            }else if(MetricName.PDU_VOLTAGE.equals(metricName)) {
               TestCase.assertEquals(221.0, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_INLET_XPOLE_CURRENT, "INLET:1","L1").
                     equals(metricName)){
               TestCase.assertEquals(1.455, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_INLET_XPOLE_FREE_CAPACITY, "INLET:1","L1").
                     equals(metricName)){
               TestCase.assertEquals(30.545, pduMetricdata.getValueNum());
            }else if(String.format(MetricName.PDU_INLET_XPOLE_VOLTAGE, "INLET:1","L1").
                     equals(metricName)){
               TestCase.assertEquals(221.0, pduMetricdata.getValueNum());
            }else {
               TestCase.fail("Unkown metric");
            }
         }
      }finally {
         assetRepository.deleteById(pduAsset.getId());
         realtimeDataRepository.deleteById(pduUsageMetricData.getId());
         realtimeDataRepository.deleteById(tempRealTimeData.getId());
         realtimeDataRepository.deleteById(humdityRealTimeData.getId());
      }
   }

   @Test
   public void testGetHostSpecialMetricsExample() throws Exception {

      Asset asset = createAsset();
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      Long time = System.currentTimeMillis();

      List<ValueUnit> valueUnits = new ArrayList<ValueUnit>();
      ValueUnit valueUnit = new ValueUnit();

      String sinceTime = String.valueOf(time - 900000l);
      valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      valueUnit.setExtraidentifier(sinceTime + FlowgateConstant.SEPARATOR);
      valueUnit.setKey(MetricName.SERVER_MINIMUM_USED_POWER);
      valueUnit.setUnit(MetricUnit.kW.toString());
      valueUnit.setValueNum(0.5);
      valueUnits.add(valueUnit);

      valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      valueUnit.setExtraidentifier(sinceTime + FlowgateConstant.SEPARATOR);
      valueUnit.setKey(MetricName.SERVER_PEAK_USED_POWER);
      valueUnit.setUnit(MetricUnit.kW.toString());
      valueUnit.setValueNum(0.8);
      valueUnits.add(valueUnit);

      valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      valueUnit.setKey(MetricName.SERVER_AVERAGE_USED_POWER);
      valueUnit.setUnit(MetricUnit.kW.toString());
      valueUnit.setValueNum(0.6);
      valueUnits.add(valueUnit);

      valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      valueUnit.setExtraidentifier(sinceTime);
      valueUnit.setKey(MetricName.SERVER_ENERGY_CONSUMPTION);
      valueUnit.setUnit(MetricUnit.kWh.toString());
      valueUnit.setValueNum(356);
      valueUnits.add(valueUnit);

      RealTimeData hostRealTimeData = new RealTimeData();
      hostRealTimeData.setValues(valueUnits);
      hostRealTimeData.setTime(time);
      hostRealTimeData.setId("00027ca37b004a9890d1bf20349d5ac99");
      hostRealTimeData.setAssetID(asset.getId());
      realTimeDatas.add(hostRealTimeData);
      realtimeDataRepository.saveAll(realTimeDatas);

      asset = fillingMetricsformula(asset);
      HashMap<String, String> justificationfields = new HashMap<>();
      justificationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, "power-2_FIELDSPLIT_CAN1-MDF-R01-PDU-BUILDING_FIELDSPLIT_OUTLET:7_FIELDSPLIT_0001bdc8b25d4c2badfd045ab61aabfa");
      asset.setJustificationfields(justificationfields);

      asset = assetRepository.save(asset);

      MvcResult result1 = this.mockMvc
               .perform(get("/v1/assets/{assetId}/realtimedata",asset.getId()).param("starttime",
                        String.valueOf(time)).param("duration", "300000"))
               .andReturn();
      String res = result1.getResponse().getContentAsString();
      MetricData [] datas = mapper.readValue(res, MetricData[].class);
      //In mock metrics data, SERVER_MINIMUM_USED_POWER/SERVER_PEAK_USED_POWER/SERVER_AVERAGE_USED_POWER is invalid
      try {
         for(MetricData serverdata:datas) {
            String metricName = serverdata.getMetricName();
          if(MetricName.SERVER_ENERGY_CONSUMPTION.equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 356.00);
            }else {
               TestCase.fail("Unknown metric :"+ metricName);
            }
         }
      }finally {
         assetRepository.deleteById(asset.getId());
         realtimeDataRepository.deleteById(hostRealTimeData.getId());
      }
   }

   @Test
   public void testRealtimedataServerExample() throws Exception {
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
               fieldWithPath("metricName").description("metric name").type(JsonFieldType.STRING),
               fieldWithPath("valueNum").description("valueNum.").type(JsonFieldType.NUMBER),
               fieldWithPath("value").description("value").type(JsonFieldType.NULL),
               fieldWithPath("unit").description("metric unit").type(JsonFieldType.STRING),
               fieldWithPath("timeStamp").description("timeStamp").type(JsonFieldType.NUMBER) };

      Asset asset = createAsset();
      List<RealTimeData> realTimeDatas = new ArrayList<>();
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      long startTime = time - duration;
      long latestTime = startTime + 300000;
      RealTimeData pduUsageMetricData = createPduAllRealTimeData(startTime);
      pduUsageMetricData.setAssetID("0001bdc8b25d4c2badfd045ab61aabfa");
      RealTimeData tempRealTimeData = createTemperatureSensorRealtimeData(startTime, "00027ca37b004a9890d1bf20349d5ac1");
      RealTimeData humdityRealTimeData = createHumiditySensorRealtimeData(startTime, "34527ca37b004a9890d1bf20349d5ac1");
      RealTimeData backTemperature = createBackTemperatureSensorRealtimeData(startTime, "968765a37b004a9890d1bf20349d5ac1");
      RealTimeData backHumidity = createBackHumiditySensorRealtimeData(startTime, "486970a37b004a9890d1bf20349d5ac1");
      RealTimeData hostRealTimeData = createServerHostRealTimeData(startTime);
      hostRealTimeData.setAssetID(asset.getId());
      realTimeDatas.add(hostRealTimeData);
      realTimeDatas.add(humdityRealTimeData);
      realTimeDatas.add(tempRealTimeData);
      realTimeDatas.add(backHumidity);
      realTimeDatas.add(backTemperature);
      realTimeDatas.add(pduUsageMetricData);
      realtimeDataRepository.saveAll(realTimeDatas);

      asset = fillingMetricsformula(asset);
      HashMap<String, String> justificationfields = new HashMap<>();
      justificationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, "power-2_FIELDSPLIT_CAN1-MDF-R01-PDU-BUILDING_FIELDSPLIT_OUTLET:1_FIELDSPLIT_0001bdc8b25d4c2badfd045ab61aabfa");
      asset.setJustificationfields(justificationfields);
      asset = assetRepository.save(asset);

      MvcResult result1 = this.mockMvc
               .perform(get("/v1/assets/{assetId}/realtimedata", asset.getId()).param("starttime",
                        String.valueOf(startTime)).param("duration", String.valueOf(duration)))
               .andDo(document("assets-getLatestAssetMetricsData-Server-example",
                     pathParameters(
                           parameterWithName("assetId").description("The id of asset,generated by flowgate.")),
                     requestParameters(
                           parameterWithName("starttime").description("Start time of you want to query.Default value: the system current time in Millis").optional(),
                           parameterWithName("duration").description("Duration of you want to query.Default value: 300000 ms").optional()),
                     responseFields(
                           fieldWithPath("[]").description("An array of realTimeDatas"))
                                 .andWithPrefix("[].", fieldpath)
               ))
               .andReturn();
      String res = result1.getResponse().getContentAsString();
      MetricData [] datas = mapper.readValue(res, MetricData[].class);
      try {
         for(MetricData serverdata : datas) {
            String metricName = serverdata.getMetricName();
            if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                     equals(metricName)) {
               TestCase.assertEquals(0.365, serverdata.getValueNum());
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_POWER, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                     equals(metricName)) {
               TestCase.assertEquals(0.081,serverdata.getValueNum());
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                     equals(metricName)) {
               TestCase.assertEquals(221.0, serverdata.getValueNum());
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_POWER_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                     equals(metricName)) {
               TestCase.assertEquals(0.05, serverdata.getValueNum());
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_CURRENT_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                     equals(metricName)) {
               TestCase.assertEquals(0.05, serverdata.getValueNum());
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa").
                     equals(metricName)) {
               TestCase.assertEquals(1.455, serverdata.getValueNum());
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_POWER, "0001bdc8b25d4c2badfd045ab61aabfa").
                     equals(metricName)) {
               TestCase.assertEquals(0.322, serverdata.getValueNum());
            }else if(String.format(MetricKeyName.SERVER_BACK_HUMIDITY_LOCATIONX, "OUTLET").
                     equals(metricName)) {
               TestCase.assertEquals(19.0, serverdata.getValueNum());
            }else if(String.format(MetricKeyName.SERVER_BACK_TEMPREATURE_LOCATIONX, "OUTLET").
                     equals(metricName)) {
               TestCase.assertEquals(25.0, serverdata.getValueNum());
            }else if(String.format(MetricKeyName.SERVER_FRONT_HUMIDITY_LOCATIONX, "INLET").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 20.0);
            }else if(String.format(MetricKeyName.SERVER_FRONT_TEMPERATURE_LOCATIONX, "INLET").
                     equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 32.0);
            }else if(MetricName.SERVER_VOLTAGE.equals(metricName)) {
               TestCase.assertEquals(221.0, serverdata.getValueNum());
            }else if(MetricName.SERVER_STORAGEUSAGE.equals(metricName)) {
               TestCase.assertEquals(65.0, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            }else if(MetricName.SERVER_MEMORYUSAGE.equals(metricName)) {
               TestCase.assertEquals(87.22, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            }else if(MetricName.SERVER_CPUUSEDINMHZ.equals(metricName)) {
               TestCase.assertEquals(570.0, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            }else if(MetricName.SERVER_CPUUSAGE.equals(metricName)) {
               TestCase.assertEquals(3.57, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            }else if(MetricName.SERVER_ACTIVEMEMORY.equals(metricName)) {
               TestCase.assertEquals(2063852.0, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            }else if(MetricName.SERVER_SHAREDMEMORY.equals(metricName)) {
               TestCase.assertEquals(8.00, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            }else if(MetricName.SERVER_CONSUMEDMEMORY.equals(metricName)) {
               TestCase.assertEquals(18291188.0, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            }else if(MetricName.SERVER_SWAPMEMORY.equals(metricName)) {
               TestCase.assertEquals(0.00, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            }else if(MetricName.SERVER_BALLOONMEMORY.equals(metricName)) {
               TestCase.assertEquals(0.0, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            }else if(MetricName.SERVER_NETWORKUTILIZATION.equals(metricName)){
               TestCase.assertEquals(146.00, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            }else if(MetricName.SERVER_STORAGEIORATEUSAGE.equals(metricName)){
               TestCase.assertEquals(61.0, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            } else if (MetricName.SERVER_POWER.equals(metricName)) {
               TestCase.assertEquals(0.069, serverdata.getValueNum());
               TestCase.assertEquals(latestTime, serverdata.getTimeStamp());
            } else if (MetricName.SERVER_PEAK_USED_POWER.equals(metricName)) {
               TestCase.assertEquals(0.80, serverdata.getValueNum());
            } else if (MetricName.SERVER_MINIMUM_USED_POWER.equals(metricName)) {
               TestCase.assertEquals(0.069, serverdata.getValueNum());
            } else if (MetricName.SERVER_AVERAGE_USED_POWER.equals(metricName)) {
               TestCase.assertEquals(0.60, serverdata.getValueNum());
            } else if (MetricName.SERVER_ENERGY_CONSUMPTION.equals(metricName)) {
               TestCase.assertEquals(356.0, serverdata.getValueNum());
            } else if (MetricName.SERVER_AVERAGE_TEMPERATURE.equals(metricName)) {
               TestCase.assertEquals(24.00, serverdata.getValueNum());
            } else if (MetricName.SERVER_PEAK_TEMPERATURE.equals(metricName)) {
               TestCase.assertEquals(30.00, serverdata.getValueNum());
            }
         }
      } finally {
         assetRepository.deleteById(asset.getId());
         realtimeDataRepository.deleteById(pduUsageMetricData.getId());
         realtimeDataRepository.deleteById(tempRealTimeData.getId());
         realtimeDataRepository.deleteById(humdityRealTimeData.getId());
         realtimeDataRepository.deleteById(backHumidity.getId());
         realtimeDataRepository.deleteById(backTemperature.getId());
         realtimeDataRepository.deleteById(hostRealTimeData.getId());
      }
   }

   @Test
   public void testRealtimedataOtherExample() throws Exception {
      Asset sensor = createSensor();
      assetRepository.save(sensor);
      String sensorId = sensor.getId();
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      long startTime = time - duration;
      List<RealTimeData> realTimeDatas = new ArrayList<>();
      RealTimeData tempRealTimeData = createTemperatureSensorRealtimeData(startTime, sensorId);
      realTimeDatas.add(tempRealTimeData);

      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();
      ValueUnit tempValue = new ValueUnit();
      tempValue.setValueNum(25);
      tempValue.setTime(startTime + 5*60*1000);
      tempValue.setUnit(MetricUnit.C.toString());
      tempValue.setKey(MetricName.TEMPERATURE);
      valueunits.add(tempValue);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID(sensorId);
      realTimeData.setValues(valueunits);
      realTimeData.setTime(startTime + 5*60*1000);
      realTimeDatas.add(realTimeData);

      realtimeDataRepository.saveAll(realTimeDatas);

      MvcResult result1 = this.mockMvc
               .perform(get("/v1/assets/" + sensor.getId() + "/realtimedata").param("starttime",
                        String.valueOf(startTime)).param("duration", String.valueOf(duration)))
               .andReturn();
      String res = result1.getResponse().getContentAsString();
      MetricData [] metricDatas = mapper.readValue(res, MetricData[].class);
      TestCase.assertEquals(1, metricDatas.length);
      try {
         for(MetricData sensordata : metricDatas) {
            long metricTime = sensordata.getTimeStamp();
            if(metricTime == startTime + 5*60*1000) {
               TestCase.assertEquals(25.0, sensordata.getValueNum());
            } else {
               TestCase.fail();
            }
         }
      } finally {
         assetRepository.deleteById(sensorId);
         realtimeDataRepository.deleteById(tempRealTimeData.getId());
         realtimeDataRepository.deleteById(realTimeData.getId());
      }
   }

   @Test
   public void testRealtimedataNotDataExample() throws Exception {
      Asset asset = createAsset();
      fillingMetricsformula(asset);
      asset.setId("00027ca37b004a9890d1bf20349d5ac1");
      asset.setCategory(AssetCategory.Sensors);
      asset = assetRepository.save(asset);

      List<RealTimeData> realTimeDatas = new ArrayList<>();
      long currentTime = System.currentTimeMillis();
      RealTimeData sensorRealTimeData = createSensorRealtimeData(currentTime);
      sensorRealTimeData.setAssetID("00027ca37b004a9890d1bf20349d5ac1");
      sensorRealTimeData.setValues(new ArrayList<>());
      realTimeDatas.add(sensorRealTimeData);
      realtimeDataRepository.saveAll(realTimeDatas);

      try {
         MvcResult result = this.mockMvc
                  .perform(get("/v1/assets/" + asset.getId() + "/realtimedata"))
                  .andExpect(status().isOk())
                  .andReturn();
         MetricData [] datas = getMetricDataByJsonString(result.getResponse().getContentAsString());
         TestCase.assertEquals(0, datas.length);

         MvcResult result1 = this.mockMvc
                  .perform(get("/v1/assets/" + asset.getId() + "/realtimedata").param("starttime", String.valueOf(currentTime)).param("duration", "300000"))
                  .andExpect(status().isOk())
                  .andReturn();
         MetricData [] datas1 = getMetricDataByJsonString(result1.getResponse().getContentAsString());
         TestCase.assertEquals(0, datas1.length);

         MvcResult result2 = this.mockMvc
                  .perform(get("/v1/assets/" + asset.getId() + "/realtimedata").param("starttime", String.valueOf(currentTime + 30000000)).param("duration", "1"))
                  .andExpect(status().isOk())
                  .andReturn();
         MetricData [] data2 = getMetricDataByJsonString(result2.getResponse().getContentAsString());
         TestCase.assertEquals(0, data2.length);
         asset.setCategory(AssetCategory.Server);
         asset = assetRepository.save(asset);
         MvcResult result3 = this.mockMvc
                  .perform(get("/v1/assets/" + asset.getId() + "/realtimedata").param("starttime", String.valueOf(currentTime + 30000000)).param("duration", "1"))
                  .andExpect(status().isOk())
                  .andReturn();
         asset.setCategory(AssetCategory.PDU);
         asset = assetRepository.save(asset);
         MvcResult result4 = this.mockMvc
                  .perform(get("/v1/assets/" + asset.getId() + "/realtimedata").param("starttime", String.valueOf(currentTime + 30000000)).param("duration", "1"))
                  .andExpect(status().isOk())
                  .andReturn();
      } finally {
         assetRepository.deleteById(asset.getId());
         realtimeDataRepository.deleteById(sensorRealTimeData.getId());
      }
   }

   private MetricData[] getMetricDataByJsonString (String jsonString) throws JsonProcessingException {
      return new ObjectMapper().readValue(jsonString, MetricData[].class);

   }

   @Test
   public void testRealtimedataExceptionExample() throws Exception {
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Failed to find asset with field: id  and value: 00027ca37b004a9890d1bf20349d5ac1");
      long currentTime = System.currentTimeMillis();
      MvcResult result = this.mockMvc
               .perform(get("/v1/assets/00027ca37b004a9890d1bf20349d5ac1/realtimedata").param("starttime", String.valueOf(currentTime)).param("duration", "300000"))
               .andExpect(status().is4xxClientError())
               .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      }
   }

   @Test
   public void testPduRealtimedataExceptionExample() throws Exception {
      long currentTime = System.currentTimeMillis();
      MvcResult result1 = this.mockMvc
               .perform(get("/v1/assets/pdu/00027ca37b004a9890d1bf20349d5ac1/realtimedata").param("starttime", String.valueOf(currentTime)).param("duration", "300000"))
               .andExpect(status().isOk())
               .andReturn();
   }

   @Test
   public void testServerRealtimedataExceptionExample() throws Exception {
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Failed to find asset with field: id  and value: 00027ca37b004a9890d1bf20349d5ac1");
      long currentTime = System.currentTimeMillis();
      MvcResult result = this.mockMvc
               .perform(get("/v1/assets/server/00027ca37b004a9890d1bf20349d5ac1/realtimedata").param("starttime", String.valueOf(currentTime)).param("duration", "300000"))
               .andExpect(status().is4xxClientError())
               .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      }
   }

   @Test
   public void testParseAssetIPMapingByString() {
      String contentString = "\t"+"\t"+"192.168.1.1"+" "+"\t"+" "+"cloud_server1";
      AssetIPMapping mapping = AssetService.parseAssetIPMapingByString(contentString);
      TestCase.assertEquals("192.168.1.1", mapping.getIp());
      TestCase.assertEquals("cloud_server1", mapping.getAssetname());
   }

   @Test
   public void testSearchAssetNames() throws Exception {
      SetOperations<String,String> setOperations = Mockito.mock(SetOperations.class);
      when(template.hasKey(anyString())).thenReturn(false);
      when(template.opsForSet()).thenReturn(setOperations);
      when(template.opsForSet().add(anyString(), any())).thenReturn(0l);
      Asset asset = createAsset();
      asset.setAssetName("cloud_server_01");
      assetRepository.save(asset);
      this.mockMvc
            .perform(get("/v1/assets/names").param("queryParam",
                  String.valueOf("cloud")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("cloud_server_01"))
            .andDo(document("assets-fuzzyQueryServerAssetNames-example",
                  requestParameters(
                        parameterWithName("queryParam").description("A part of asset name")),
                  responseFields(fieldWithPath("[]").description("An array of server names"))))
            .andReturn();
      assetRepository.deleteById(asset.getId());
   }

   @Test
   public void testGetPDUMetricsByID() {
      Asset pduAsset = createPDU();
      pduAsset = assetRepository.save(pduAsset);
      List<RealTimeData> datas = new ArrayList<>();
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      long startTime = time - duration;
      RealTimeData pduUsageMetricData = createPduAllRealTimeData(startTime);
      pduUsageMetricData.setAssetID(pduAsset.getId());
      RealTimeData tempRealTimeData =
            createTemperatureSensorRealtimeData(startTime, "00027ca37b004a9890d1bf20349d5ac1");
      RealTimeData humdityRealTimeData =
            createHumiditySensorRealtimeData(startTime, "34527ca37b004a9890d1bf20349d5ac1");
      datas.add(humdityRealTimeData);
      datas.add(tempRealTimeData);
      datas.add(pduUsageMetricData);
      realtimeDataRepository.saveAll(datas);
      List<MetricData> metricDatas =
            assetService.getMetricsByID(pduAsset.getId(), startTime, duration);
      int metricResultSize = pduUsageMetricData.getValues().size() +
            tempRealTimeData.getValues().size() + humdityRealTimeData.getValues().size();
      TestCase.assertEquals(metricResultSize, metricDatas.size());
      for(MetricData pduMetricdata : metricDatas) {
         String metricName = pduMetricdata.getMetricName();
         if(String.format(MetricName.PDU_XLET_ACTIVE_POWER,"OUTLET:1").
               equals(metricName)) {
            TestCase.assertEquals(0.054, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_XLET_APPARENT_POWER,"OUTLET:1").
               equals(metricName)) {
            TestCase.assertEquals(0.081, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_XLET_CURRENT,"OUTLET:1").
               equals(metricName)) {
            TestCase.assertEquals(0.365, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_XLET_FREE_CAPACITY, "OUTLET:1").
               equals(metricName)) {
            TestCase.assertEquals(9.635, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_XLET_VOLTAGE, "OUTLET:1").
               equals(metricName)) {
            TestCase.assertEquals(221.0, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_XLET_ACTIVE_POWER,"OUTLET:2").
               equals(metricName)) {
            TestCase.assertEquals(0.2, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_XLET_APPARENT_POWER,"OUTLET:2").
               equals(metricName)) {
            TestCase.assertEquals(0.241, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_XLET_CURRENT,"OUTLET:2").
               equals(metricName)) {
            TestCase.assertEquals(1.09, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_XLET_FREE_CAPACITY, "OUTLET:2").
               equals(metricName)) {
            TestCase.assertEquals(8.91, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_XLET_VOLTAGE, "OUTLET:2").
               equals(metricName)) {
            TestCase.assertEquals(221.0, pduMetricdata.getValueNum());
         }
         else if(String.format(MetricKeyName.PDU_HUMIDITY_LOCATIONX, "OUTLET").
               equals(metricName)) {
            TestCase.assertEquals(20.0, pduMetricdata.getValueNum());
         }else if(String.format(MetricKeyName.PDU_TEMPERATURE_LOCATIONX, "INLET").
               equals(metricName)) {
            TestCase.assertEquals(32.0, pduMetricdata.getValueNum());
         }else if(MetricName.PDU_CURRENT_LOAD.equals(metricName)) {
            TestCase.assertEquals(0.05, pduMetricdata.getValueNum());
         }else if(MetricName.PDU_POWER_LOAD.equals(metricName)) {
            TestCase.assertEquals(0.05, pduMetricdata.getValueNum());
         }else if(MetricName.PDU_TOTAL_CURRENT.equals(metricName)) {
            TestCase.assertEquals(1.455, pduMetricdata.getValueNum());
         }else if(MetricName.PDU_TOTAL_POWER.equals(metricName)) {
            TestCase.assertEquals(0.322, pduMetricdata.getValueNum());
         }else if(MetricName.PDU_VOLTAGE.equals(metricName)) {
            TestCase.assertEquals(221.0, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_INLET_XPOLE_CURRENT, "INLET:1","L1").
               equals(metricName)){
            TestCase.assertEquals(1.455, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_INLET_XPOLE_FREE_CAPACITY, "INLET:1","L1").
               equals(metricName)){
            TestCase.assertEquals(30.545, pduMetricdata.getValueNum());
         }else if(String.format(MetricName.PDU_INLET_XPOLE_VOLTAGE, "INLET:1","L1").
               equals(metricName)){
            TestCase.assertEquals(221.0, pduMetricdata.getValueNum());
         }else {
            TestCase.fail("Unkown metric");
         }
      }
      assetRepository.deleteById(pduAsset.getId());
      realtimeDataRepository.deleteById(pduUsageMetricData.getId());
      realtimeDataRepository.deleteById(tempRealTimeData.getId());
      realtimeDataRepository.deleteById(humdityRealTimeData.getId());
   }

   @Test
   public void testGetServerMetricsByIDFormulaIsEmpty() {
      Asset asset = createAsset();
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      long startTime = time - duration;
      Map<String, String> formulars = new HashMap<String, String>();
      asset.setMetricsformulars(formulars);
      asset = assetRepository.save(asset);
      List<MetricData> metricDatas =
            assetService.getMetricsByID(asset.getId(), startTime, duration);
      TestCase.assertEquals(0, metricDatas.size());
      assetRepository.deleteById(asset.getId());
   }

   @Test
   public void testGetServerMetricsByIDHostUsageFormulaIsNull() {
      Asset asset = createAsset();
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      long startTime = time - duration;
      RealTimeData pduUsageMetricData = createPduAllRealTimeData(startTime);
      pduUsageMetricData.setAssetID("0001bdc8b25d4c2badfd045ab61aabfa");
      RealTimeData tempRealTimeData =
            createTemperatureSensorRealtimeData(startTime, "00027ca37b004a9890d1bf20349d5ac1");
      RealTimeData humdityRealTimeData =
            createHumiditySensorRealtimeData(startTime, "34527ca37b004a9890d1bf20349d5ac1");
      RealTimeData backTemperature =
            createBackTemperatureSensorRealtimeData(startTime, "968765a37b004a9890d1bf20349d5ac1");
      RealTimeData backHumidity =
            createBackHumiditySensorRealtimeData(startTime, "486970a37b004a9890d1bf20349d5ac1");
      realTimeDatas.add(humdityRealTimeData);
      realTimeDatas.add(tempRealTimeData);
      realTimeDatas.add(backHumidity);
      realTimeDatas.add(backTemperature);
      realTimeDatas.add(pduUsageMetricData);
      realtimeDataRepository.saveAll(realTimeDatas);

      asset = fillingMetricsformula(asset);
      Map<String, String> metricFormula = asset.getMetricsformulars();
      metricFormula.remove(FlowgateConstant.HOST_METRICS);
      asset.setMetricsformulars(metricFormula);
      HashMap<String, String> justificationfields = new HashMap<>();
      justificationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, "power-2_FIELDSPLIT_CAN1-MDF-R01-PDU-BUILDING_FIELDSPLIT_OUTLET:1_FIELDSPLIT_0001bdc8b25d4c2badfd045ab61aabfa");
      asset.setJustificationfields(justificationfields);
      asset = assetRepository.save(asset);
      List<MetricData> metricDatas =
            assetService.getMetricsByID(asset.getId(), startTime, duration);
      for(MetricData serverdata : metricDatas) {
         String metricName = serverdata.getMetricName();
         if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
            TestCase.assertEquals(0.365, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_POWER, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
            TestCase.assertEquals(0.081,serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
            TestCase.assertEquals(221.0, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_POWER_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.05, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_CURRENT_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.05, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(1.455, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_POWER, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.322, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_BACK_HUMIDITY_LOCATIONX, "OUTLET").
                  equals(metricName)) {
            TestCase.assertEquals(19.0, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_BACK_TEMPREATURE_LOCATIONX, "OUTLET").
                  equals(metricName)) {
            TestCase.assertEquals(25.0, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_FRONT_HUMIDITY_LOCATIONX, "INLET").
                  equals(metricName)) {
            TestCase.assertEquals(serverdata.getValueNum(), 20.0);
         }else if(String.format(MetricKeyName.SERVER_FRONT_TEMPERATURE_LOCATIONX, "INLET").
                  equals(metricName)) {
            TestCase.assertEquals(serverdata.getValueNum(), 32.0);
         }else if(MetricName.SERVER_VOLTAGE.equals(metricName)) {
            TestCase.assertEquals(221.0, serverdata.getValueNum());
         }
      }
      assetRepository.deleteById(asset.getId());
      realtimeDataRepository.deleteById(pduUsageMetricData.getId());
      realtimeDataRepository.deleteById(tempRealTimeData.getId());
      realtimeDataRepository.deleteById(humdityRealTimeData.getId());
      realtimeDataRepository.deleteById(backHumidity.getId());
      realtimeDataRepository.deleteById(backTemperature.getId());
   }

   @Test
   public void testGetServerMetricsByID() {
      Asset asset = createAsset();
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      long startTime = time - duration;
      RealTimeData pduUsageMetricData = createPduAllRealTimeData(startTime);
      pduUsageMetricData.setAssetID("0001bdc8b25d4c2badfd045ab61aabfa");
      RealTimeData tempRealTimeData =
            createTemperatureSensorRealtimeData(startTime, "00027ca37b004a9890d1bf20349d5ac1");
      RealTimeData humdityRealTimeData =
            createHumiditySensorRealtimeData(startTime, "34527ca37b004a9890d1bf20349d5ac1");
      RealTimeData backTemperature =
            createBackTemperatureSensorRealtimeData(startTime, "968765a37b004a9890d1bf20349d5ac1");
      RealTimeData backHumidity =
            createBackHumiditySensorRealtimeData(startTime, "486970a37b004a9890d1bf20349d5ac1");
      RealTimeData hostRealTimeData = createServerHostRealTimeData(startTime);
      hostRealTimeData.setAssetID(asset.getId());
      realTimeDatas.add(hostRealTimeData);
      realTimeDatas.add(humdityRealTimeData);
      realTimeDatas.add(tempRealTimeData);
      realTimeDatas.add(backHumidity);
      realTimeDatas.add(backTemperature);
      realTimeDatas.add(pduUsageMetricData);
      realtimeDataRepository.saveAll(realTimeDatas);

      asset = fillingMetricsformula(asset);
      HashMap<String, String> justificationfields = new HashMap<>();
      justificationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, "power-2_FIELDSPLIT_CAN1-MDF-R01-PDU-BUILDING_FIELDSPLIT_OUTLET:1_FIELDSPLIT_0001bdc8b25d4c2badfd045ab61aabfa");
      asset.setJustificationfields(justificationfields);
      asset = assetRepository.save(asset);

      List<MetricData> metricDatas =
            assetService.getMetricsByID(asset.getId(), startTime, duration);

      Set<String> specialMetricNames = new HashSet<String>();
      specialMetricNames.add(MetricName.SERVER_AVERAGE_USED_POWER);
      specialMetricNames.add(MetricName.SERVER_PEAK_USED_POWER);
      specialMetricNames.add(MetricName.SERVER_MINIMUM_USED_POWER);
      specialMetricNames.add(MetricName.SERVER_AVERAGE_TEMPERATURE);
      specialMetricNames.add(MetricName.SERVER_PEAK_TEMPERATURE);

      for(MetricData serverdata : metricDatas) {
         String metricName = serverdata.getMetricName();
         if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
            TestCase.assertEquals(0.365, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_POWER, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
            TestCase.assertEquals(0.081,serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
            TestCase.assertEquals(221.0, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_POWER_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.05, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_CURRENT_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.05, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(1.455, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_POWER, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.322, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_BACK_HUMIDITY_LOCATIONX, "OUTLET").
                  equals(metricName)) {
            TestCase.assertEquals(19.0, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_BACK_TEMPREATURE_LOCATIONX, "OUTLET").
                  equals(metricName)) {
            TestCase.assertEquals(25.0, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_FRONT_HUMIDITY_LOCATIONX, "INLET").
                  equals(metricName)) {
            TestCase.assertEquals(serverdata.getValueNum(), 20.0);
         }else if(String.format(MetricKeyName.SERVER_FRONT_TEMPERATURE_LOCATIONX, "INLET").
                  equals(metricName)) {
            TestCase.assertEquals(serverdata.getValueNum(), 32.0);
         }else if(MetricName.SERVER_VOLTAGE.equals(metricName)) {
            TestCase.assertEquals(221.0, serverdata.getValueNum());
         }else if(MetricName.SERVER_STORAGEUSAGE.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 240000) {
               TestCase.assertEquals(65.0, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_MEMORYUSAGE.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(87.22, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_CPUUSEDINMHZ.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(746.00, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_CPUUSAGE.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(4.67, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_ACTIVEMEMORY.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(1561416.00, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_SHAREDMEMORY.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(8.00, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_CONSUMEDMEMORY.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(18291220.00, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_SWAPMEMORY.equals(metricName)) {
            TestCase.assertEquals(0.00, serverdata.getValueNum());
         }else if(MetricName.SERVER_BALLOONMEMORY.equals(metricName)) {
            TestCase.assertEquals(0.0, serverdata.getValueNum());
         }else if(MetricName.SERVER_NETWORKUTILIZATION.equals(metricName)){
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(146.00, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_STORAGEIORATEUSAGE.equals(metricName)){
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(330.00, serverdata.getValueNum());
            }
         } else if (MetricName.SERVER_POWER.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(0.069, serverdata.getValueNum());
            }
         }else if (MetricName.SERVER_ENERGY_CONSUMPTION.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime) {
               TestCase.assertEquals(0.00038805555555555555, serverdata.getValueNum());
            }
         } else if (specialMetricNames.contains(metricName)) {
            TestCase.fail("Duration API not support this metric "+ metricName);
         }
      }
      assetRepository.deleteById(asset.getId());
      realtimeDataRepository.deleteById(pduUsageMetricData.getId());
      realtimeDataRepository.deleteById(tempRealTimeData.getId());
      realtimeDataRepository.deleteById(humdityRealTimeData.getId());
      realtimeDataRepository.deleteById(backHumidity.getId());
      realtimeDataRepository.deleteById(backTemperature.getId());
      realtimeDataRepository.deleteById(hostRealTimeData.getId());
   }

   @Test
   public void testGetServerMetricsOutLetisNull() {
      Asset asset = createAsset();
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      long startTime = time - duration;
      RealTimeData pduUsageMetricData = createPduAllRealTimeData(startTime);
      pduUsageMetricData.setAssetID("0001bdc8b25d4c2badfd045ab61aabfa");
      realTimeDatas.add(pduUsageMetricData);
      realtimeDataRepository.saveAll(realTimeDatas);

      asset = fillingMetricsformula(asset);
      asset = assetRepository.save(asset);

      List<MetricData> metricDatas =
            assetService.getMetricsByID(asset.getId(), startTime, duration);
      for(MetricData serverdata : metricDatas) {
         String metricName = serverdata.getMetricName();
         if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_POWER_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.05, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_CURRENT_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.05, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(1.455, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_POWER, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.322, serverdata.getValueNum());
         }else {
            TestCase.fail("Unkown metric: "+metricName);
         }
      }
      assetRepository.deleteById(asset.getId());
      realtimeDataRepository.deleteById(pduUsageMetricData.getId());
   }

   @Test
   public void testGetMetricsDurationAPI() throws Exception {
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("metricName").description("metric name").type(JsonFieldType.STRING),
            fieldWithPath("valueNum").description("valueNum.").type(JsonFieldType.NUMBER),
            fieldWithPath("value").description("value").type(JsonFieldType.NULL),
            fieldWithPath("unit").description("metric unit").type(JsonFieldType.STRING),
            fieldWithPath("timeStamp").description("timeStamp").type(JsonFieldType.NUMBER) };

      Asset asset = createAsset();
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      long startTime = time - duration;
      RealTimeData pduUsageMetricData = createPduAllRealTimeData(startTime);
      pduUsageMetricData.setAssetID("0001bdc8b25d4c2badfd045ab61aabfa");
      RealTimeData tempRealTimeData =
            createTemperatureSensorRealtimeData(startTime, "00027ca37b004a9890d1bf20349d5ac1");
      RealTimeData humdityRealTimeData =
            createHumiditySensorRealtimeData(startTime, "34527ca37b004a9890d1bf20349d5ac1");
      RealTimeData backTemperature =
            createBackTemperatureSensorRealtimeData(startTime, "968765a37b004a9890d1bf20349d5ac1");
      RealTimeData backHumidity =
            createBackHumiditySensorRealtimeData(startTime, "486970a37b004a9890d1bf20349d5ac1");
      RealTimeData hostRealTimeData = createServerHostRealTimeData(startTime);
      hostRealTimeData.setAssetID(asset.getId());
      realTimeDatas.add(hostRealTimeData);
      realTimeDatas.add(humdityRealTimeData);
      realTimeDatas.add(tempRealTimeData);
      realTimeDatas.add(backHumidity);
      realTimeDatas.add(backTemperature);
      realTimeDatas.add(pduUsageMetricData);
      realtimeDataRepository.saveAll(realTimeDatas);

      asset = fillingMetricsformula(asset);
      HashMap<String, String> justificationfields = new HashMap<>();
      justificationfields.put(FlowgateConstant.PDU_PORT_FOR_SERVER, "power-2_FIELDSPLIT_CAN1-MDF-R01-PDU-BUILDING_FIELDSPLIT_OUTLET:1_FIELDSPLIT_0001bdc8b25d4c2badfd045ab61aabfa");
      asset.setJustificationfields(justificationfields);
      asset = assetRepository.save(asset);
      MvcResult result = this.mockMvc
            .perform(get("/v1/assets/{assetId}/metrics", asset.getId()).param("starttime",
                     String.valueOf(startTime)).param("duration", String.valueOf(duration)))
            .andDo(document("assets-getAllMetricsDataInDuration-Server-example",
                  pathParameters(
                        parameterWithName("assetId").description("The id of asset,generated by flowgate.")),
                  requestParameters(
                        parameterWithName("starttime").description("Start time of you want to query.Default value: the system current time in Millis").optional(),
                        parameterWithName("duration").description("Duration of you want to query.Default value: 300000 ms").optional()),
                  responseFields(
                        fieldWithPath("[]").description("An array of realTimeDatas"))
                  .andWithPrefix("[].", fieldpath)))
            .andReturn();

      String res = result.getResponse().getContentAsString();
      MetricData [] metricDatas = mapper.readValue(res, MetricData[].class);

      Set<String> specialMetricNames = new HashSet<String>();
      specialMetricNames.add(MetricName.SERVER_AVERAGE_USED_POWER);
      specialMetricNames.add(MetricName.SERVER_PEAK_USED_POWER);
      specialMetricNames.add(MetricName.SERVER_MINIMUM_USED_POWER);
      specialMetricNames.add(MetricName.SERVER_AVERAGE_TEMPERATURE);
      specialMetricNames.add(MetricName.SERVER_PEAK_TEMPERATURE);

      for(MetricData serverdata : metricDatas) {
         String metricName = serverdata.getMetricName();
         if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
            TestCase.assertEquals(0.365, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_POWER, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
            TestCase.assertEquals(0.081,serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
            TestCase.assertEquals(221.0, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_POWER_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.05, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_CURRENT_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.05, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(1.455, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_POWER, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
            TestCase.assertEquals(0.322, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_BACK_HUMIDITY_LOCATIONX, "OUTLET").
                  equals(metricName)) {
            TestCase.assertEquals(19.0, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_BACK_TEMPREATURE_LOCATIONX, "OUTLET").
                  equals(metricName)) {
            TestCase.assertEquals(25.0, serverdata.getValueNum());
         }else if(String.format(MetricKeyName.SERVER_FRONT_HUMIDITY_LOCATIONX, "INLET").
                  equals(metricName)) {
            TestCase.assertEquals(serverdata.getValueNum(), 20.0);
         }else if(String.format(MetricKeyName.SERVER_FRONT_TEMPERATURE_LOCATIONX, "INLET").
                  equals(metricName)) {
            TestCase.assertEquals(serverdata.getValueNum(), 32.0);
         }else if(MetricName.SERVER_VOLTAGE.equals(metricName)) {
            TestCase.assertEquals(221.0, serverdata.getValueNum());
         }else if(MetricName.SERVER_STORAGEUSAGE.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 240000) {
               TestCase.assertEquals(65.0, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_MEMORYUSAGE.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(87.22, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_CPUUSEDINMHZ.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(746.00, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_CPUUSAGE.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(4.67, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_ACTIVEMEMORY.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(1561416.00, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_SHAREDMEMORY.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(8.00, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_CONSUMEDMEMORY.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(18291220.00, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_SWAPMEMORY.equals(metricName)) {
            TestCase.assertEquals(0.00, serverdata.getValueNum());
         }else if(MetricName.SERVER_BALLOONMEMORY.equals(metricName)) {
            TestCase.assertEquals(0.0, serverdata.getValueNum());
         }else if(MetricName.SERVER_NETWORKUTILIZATION.equals(metricName)){
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(146.00, serverdata.getValueNum());
            }
         }else if(MetricName.SERVER_STORAGEIORATEUSAGE.equals(metricName)){
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(330.00, serverdata.getValueNum());
            }
         } else if (MetricName.SERVER_POWER.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime + 20000) {
               TestCase.assertEquals(0.069, serverdata.getValueNum());
            }
         }else if (MetricName.SERVER_ENERGY_CONSUMPTION.equals(metricName)) {
            if (serverdata.getTimeStamp() == startTime) {
               TestCase.assertEquals(0.00038805555555555555, serverdata.getValueNum());
            }
         } else if (specialMetricNames.contains(metricName)) {
            TestCase.fail("Duration API not support this metric "+ metricName);
         }
      }
      assetRepository.deleteById(asset.getId());
      realtimeDataRepository.deleteById(pduUsageMetricData.getId());
      realtimeDataRepository.deleteById(tempRealTimeData.getId());
      realtimeDataRepository.deleteById(humdityRealTimeData.getId());
      realtimeDataRepository.deleteById(backHumidity.getId());
      realtimeDataRepository.deleteById(backTemperature.getId());
      realtimeDataRepository.deleteById(hostRealTimeData.getId());
   }

   @Test
   public void testGetMetricDataForOther() {
      Asset sensor = createSensor();
      assetRepository.save(sensor);
      String sensorId = sensor.getId();
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      long startTime = time - duration;
      List<RealTimeData> realTimeDatas = new ArrayList<>();
      RealTimeData tempRealTimeData =
            createTemperatureSensorRealtimeData(startTime, sensorId);
      realTimeDatas.add(tempRealTimeData);

      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();
      ValueUnit tempValue = new ValueUnit();
      tempValue.setValueNum(25);
      tempValue.setTime(startTime + 5*60*1000);
      tempValue.setUnit(MetricUnit.C.toString());
      tempValue.setKey(MetricName.TEMPERATURE);
      valueunits.add(tempValue);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID(sensorId);
      realTimeData.setValues(valueunits);
      realTimeData.setTime(startTime + 5*60*1000);
      realTimeDatas.add(realTimeData);

      realtimeDataRepository.saveAll(realTimeDatas);
      List<MetricData> metricDatas =
            assetService.getMetricsByID(sensorId, startTime, duration);
      TestCase.assertEquals(2, metricDatas.size());
      for(MetricData sensordata : metricDatas) {
         long metricTime = sensordata.getTimeStamp();
         if(metricTime == startTime) {
            TestCase.assertEquals(32.0, sensordata.getValueNum());
         }else if(metricTime == startTime + 5*60*1000) {
            TestCase.assertEquals(25.0, sensordata.getValueNum());
         }else {
            TestCase.fail();
         }
      }
      assetRepository.deleteById(sensorId);
      realtimeDataRepository.deleteById(tempRealTimeData.getId());
      realtimeDataRepository.deleteById(realTimeData.getId());
   }

   @Test
   public void testMetricFormulaStringIsNull() {
      Asset asset = createAsset();
      Map<String,String> formulaInfo = asset.metricsFormulaToMap(null, new TypeReference<Map<String, String>>(){});
      TestCase.assertEquals(null, formulaInfo);
   }

   RealTimeData createPduRealTimeData(Long time) {
      RealTimeData realTimeData = createServerPDURealTimeData(time);
      List<ValueUnit> valueunits = realTimeData.getValues();

      ValueUnit valueunitActivePower = new ValueUnit();
      valueunitActivePower.setKey(MetricName.PDU_ACTIVE_POWER);
      valueunitActivePower.setUnit(MetricUnit.kW.toString());
      valueunitActivePower.setExtraidentifier("OUTLET:7");
      valueunitActivePower.setValueNum(0.2);
      valueunitActivePower.setTime(time);
      valueunits.add(valueunitActivePower);

      ValueUnit valueunitFreeCapacity = new ValueUnit();
      valueunitFreeCapacity.setKey(MetricName.PDU_FREE_CAPACITY);
      valueunitFreeCapacity.setUnit(MetricUnit.A.toString());
      valueunitFreeCapacity.setExtraidentifier("OUTLET:7");
      valueunitFreeCapacity.setValueNum(20);
      valueunitFreeCapacity.setTime(time);
      valueunits.add(valueunitFreeCapacity);

      ValueUnit valueunitL1FreeCapacity = new ValueUnit();
      valueunitL1FreeCapacity.setKey(MetricName.PDU_FREE_CAPACITY);
      valueunitL1FreeCapacity.setUnit(MetricUnit.A.toString());
      valueunitL1FreeCapacity.setExtraidentifier("INLET:1"+FlowgateConstant.INLET_POLE_NAME_PREFIX+1);
      valueunitL1FreeCapacity.setValueNum(34);
      valueunitL1FreeCapacity.setTime(time);
      valueunits.add(valueunitL1FreeCapacity);

      ValueUnit valueunitL1Current = new ValueUnit();
      valueunitL1Current.setKey(MetricName.PDU_CURRENT);
      valueunitL1Current.setUnit(MetricUnit.A.toString());
      valueunitL1Current.setExtraidentifier("INLET:1"+FlowgateConstant.INLET_POLE_NAME_PREFIX+1);
      valueunitL1Current.setValueNum(6);
      valueunitL1Current.setTime(time);
      valueunits.add(valueunitL1Current);

      ValueUnit valueunitL1Voltage = new ValueUnit();
      valueunitL1Voltage.setKey(MetricName.PDU_VOLTAGE);
      valueunitL1Voltage.setUnit(MetricUnit.V.toString());
      valueunitL1Voltage.setExtraidentifier("INLET:1"+FlowgateConstant.INLET_POLE_NAME_PREFIX+1);
      valueunitL1Voltage.setValueNum(220);
      valueunitL1Voltage.setTime(time);
      valueunits.add(valueunitL1Voltage);

      ValueUnit valueunit1L1FreeCapacity = new ValueUnit();
      valueunit1L1FreeCapacity.setKey(MetricName.PDU_FREE_CAPACITY);
      valueunit1L1FreeCapacity.setUnit(MetricUnit.A.toString());
      valueunit1L1FreeCapacity.setExtraidentifier("INLET:2"+FlowgateConstant.INLET_POLE_NAME_PREFIX+1);
      valueunit1L1FreeCapacity.setValueNum(24);
      valueunit1L1FreeCapacity.setTime(time);
      valueunits.add(valueunit1L1FreeCapacity);

      ValueUnit valueunit1L1Current = new ValueUnit();
      valueunit1L1Current.setKey(MetricName.PDU_CURRENT);
      valueunit1L1Current.setUnit(MetricUnit.A.toString());
      valueunit1L1Current.setExtraidentifier("INLET:2"+FlowgateConstant.INLET_POLE_NAME_PREFIX+1);
      valueunit1L1Current.setValueNum(6);
      valueunit1L1Current.setTime(time);
      valueunits.add(valueunit1L1Current);

      ValueUnit valueunit1L1Voltage = new ValueUnit();
      valueunit1L1Voltage.setKey(MetricName.PDU_VOLTAGE);
      valueunit1L1Voltage.setUnit(MetricUnit.V.toString());
      valueunit1L1Voltage.setExtraidentifier("INLET:2"+FlowgateConstant.INLET_POLE_NAME_PREFIX+1);
      valueunit1L1Voltage.setValueNum(240);
      valueunit1L1Voltage.setTime(time);
      valueunits.add(valueunit1L1Voltage);

      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setValues(valueunits);
      realTimeData.setTime(time);
      return realTimeData;
   }

   RealTimeData createPduAllRealTimeData(Long time) {
      RealTimeData realTimeData = new RealTimeData();
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();

      //PDU current 1.455 = sum(OutLet-Current)
      ValueUnit pduCurrentValueUnit = new ValueUnit();
      pduCurrentValueUnit.setKey(MetricName.PDU_TOTAL_CURRENT);
      pduCurrentValueUnit.setValueNum(1.455);
      pduCurrentValueUnit.setTime(time);
      pduCurrentValueUnit.setUnit(MetricUnit.A.toString());
      valueunits.add(pduCurrentValueUnit);

      //PDU voltage 221
      ValueUnit pduVoltageValueUnit = new ValueUnit();
      pduVoltageValueUnit.setKey(MetricName.PDU_VOLTAGE);
      pduVoltageValueUnit.setValueNum(221);
      pduVoltageValueUnit.setTime(time);
      pduVoltageValueUnit.setUnit(MetricUnit.V.toString());
      valueunits.add(pduVoltageValueUnit);

      //PDU power 0.322 sum(OutLet-Power)
      ValueUnit pduPowerValueUnit = new ValueUnit();
      pduPowerValueUnit.setKey(MetricName.PDU_TOTAL_POWER);
      pduPowerValueUnit.setValueNum(0.322);
      pduPowerValueUnit.setTime(time);
      pduPowerValueUnit.setUnit(MetricUnit.kW.toString());
      valueunits.add(pduPowerValueUnit);

      //PDU powerLoad 0.05
      ValueUnit powerLoadValueUnit = new ValueUnit();
      powerLoadValueUnit.setKey(MetricName.PDU_POWER_LOAD);
      powerLoadValueUnit.setValueNum(0.05);
      powerLoadValueUnit.setTime(time);
      powerLoadValueUnit.setUnit(MetricUnit.percent.toString());
      valueunits.add(powerLoadValueUnit);

      //PDU currentLoad 0.05
      ValueUnit currentLoadValueUnit = new ValueUnit();
      currentLoadValueUnit.setKey(MetricName.PDU_CURRENT_LOAD);
      currentLoadValueUnit.setValueNum(0.05);
      currentLoadValueUnit.setTime(time);
      currentLoadValueUnit.setUnit(MetricUnit.percent.toString());
      valueunits.add(currentLoadValueUnit);

      //Outlet:1 apparent power 0.081
      ValueUnit outLetApparentPowerValueUnit = new ValueUnit();
      outLetApparentPowerValueUnit.setKey(MetricName.PDU_APPARENT_POWER);
      outLetApparentPowerValueUnit.setValueNum(0.081);
      outLetApparentPowerValueUnit.setExtraidentifier("OUTLET:1");
      outLetApparentPowerValueUnit.setTime(time);
      outLetApparentPowerValueUnit.setUnit(MetricUnit.kW.toString());
      valueunits.add(outLetApparentPowerValueUnit);

      //Outlet:1 active power 0.054
      ValueUnit valueunitActivePower = new ValueUnit();
      valueunitActivePower.setKey(MetricName.PDU_ACTIVE_POWER);
      valueunitActivePower.setUnit(MetricUnit.kW.toString());
      valueunitActivePower.setExtraidentifier("OUTLET:1");
      valueunitActivePower.setValueNum(0.054);
      valueunitActivePower.setTime(time);
      valueunits.add(valueunitActivePower);

      //Outlet:1 current 0.365
      ValueUnit currentValueUnit = new ValueUnit();
      currentValueUnit.setKey(MetricName.PDU_CURRENT);
      currentValueUnit.setValueNum(0.365);
      currentValueUnit.setExtraidentifier("OUTLET:1");
      currentValueUnit.setTime(time);
      currentValueUnit.setUnit(MetricUnit.A.toString());
      valueunits.add(currentValueUnit);

      //Outlet:1 voltage 221
      ValueUnit voltageValueUnit = new ValueUnit();
      voltageValueUnit.setKey(MetricName.PDU_VOLTAGE);
      voltageValueUnit.setValueNum(221);
      voltageValueUnit.setExtraidentifier("OUTLET:1");
      voltageValueUnit.setTime(time);
      voltageValueUnit.setUnit(MetricUnit.V.toString());
      valueunits.add(voltageValueUnit);

      //Outlet:1 freeCapacity 9.635
      ValueUnit valueunitFreeCapacity = new ValueUnit();
      valueunitFreeCapacity.setKey(MetricName.PDU_FREE_CAPACITY);
      valueunitFreeCapacity.setUnit(MetricUnit.A.toString());
      valueunitFreeCapacity.setExtraidentifier("OUTLET:1");
      valueunitFreeCapacity.setValueNum(9.635);
      valueunitFreeCapacity.setTime(time);
      valueunits.add(valueunitFreeCapacity);

      //Outlet:2 apparent power 0.241
      ValueUnit outLet2ApparentPowerValueUnit = new ValueUnit();
      outLet2ApparentPowerValueUnit.setKey(MetricName.PDU_APPARENT_POWER);
      outLet2ApparentPowerValueUnit.setValueNum(0.241);
      outLet2ApparentPowerValueUnit.setExtraidentifier("OUTLET:2");
      outLet2ApparentPowerValueUnit.setTime(time);
      outLet2ApparentPowerValueUnit.setUnit(MetricUnit.kW.toString());
      valueunits.add(outLet2ApparentPowerValueUnit);

      //Outlet:2 active power 0.2
      ValueUnit outLet2valueunitActivePower = new ValueUnit();
      outLet2valueunitActivePower.setKey(MetricName.PDU_ACTIVE_POWER);
      outLet2valueunitActivePower.setUnit(MetricUnit.kW.toString());
      outLet2valueunitActivePower.setExtraidentifier("OUTLET:2");
      outLet2valueunitActivePower.setValueNum(0.2);
      outLet2valueunitActivePower.setTime(time);
      valueunits.add(outLet2valueunitActivePower);

      //Outlet:2 current 1.09
      ValueUnit outLet2currentValueUnit = new ValueUnit();
      outLet2currentValueUnit.setKey(MetricName.PDU_CURRENT);
      outLet2currentValueUnit.setValueNum(1.09);
      outLet2currentValueUnit.setExtraidentifier("OUTLET:2");
      outLet2currentValueUnit.setTime(time);
      outLet2currentValueUnit.setUnit(MetricUnit.A.toString());
      valueunits.add(outLet2currentValueUnit);

      //Outlet:2 voltage 221
      ValueUnit outLet2voltageValueUnit = new ValueUnit();
      outLet2voltageValueUnit.setKey(MetricName.PDU_VOLTAGE);
      outLet2voltageValueUnit.setValueNum(221);
      outLet2voltageValueUnit.setExtraidentifier("OUTLET:2");
      outLet2voltageValueUnit.setTime(time);
      outLet2voltageValueUnit.setUnit(MetricUnit.V.toString());
      valueunits.add(outLet2voltageValueUnit);

      //Outlet:2 freeCapacity 8.91
      ValueUnit outLet2valueunitFreeCapacity = new ValueUnit();
      outLet2valueunitFreeCapacity.setKey(MetricName.PDU_FREE_CAPACITY);
      outLet2valueunitFreeCapacity.setUnit(MetricUnit.A.toString());
      outLet2valueunitFreeCapacity.setExtraidentifier("OUTLET:2");
      outLet2valueunitFreeCapacity.setValueNum(8.91);
      outLet2valueunitFreeCapacity.setTime(time);
      valueunits.add(outLet2valueunitFreeCapacity);

      //Inlet Pole metrics 30.545
      ValueUnit valueunitL1FreeCapacity = new ValueUnit();
      valueunitL1FreeCapacity.setKey(MetricName.PDU_FREE_CAPACITY);
      valueunitL1FreeCapacity.setUnit(MetricUnit.A.toString());
      valueunitL1FreeCapacity.setExtraidentifier("INLET:1"+FlowgateConstant.INLET_POLE_NAME_PREFIX+1);
      valueunitL1FreeCapacity.setValueNum(30.545);
      valueunitL1FreeCapacity.setTime(time);
      valueunits.add(valueunitL1FreeCapacity);

      //1.455
      ValueUnit valueunitL1Current = new ValueUnit();
      valueunitL1Current.setKey(MetricName.PDU_CURRENT);
      valueunitL1Current.setUnit(MetricUnit.A.toString());
      valueunitL1Current.setExtraidentifier("INLET:1"+FlowgateConstant.INLET_POLE_NAME_PREFIX+1);
      valueunitL1Current.setValueNum(1.455);
      valueunitL1Current.setTime(time);
      valueunits.add(valueunitL1Current);

      //221
      ValueUnit valueunitL1Voltage = new ValueUnit();
      valueunitL1Voltage.setKey(MetricName.PDU_VOLTAGE);
      valueunitL1Voltage.setUnit(MetricUnit.V.toString());
      valueunitL1Voltage.setExtraidentifier("INLET:1"+FlowgateConstant.INLET_POLE_NAME_PREFIX+1);
      valueunitL1Voltage.setValueNum(221);
      valueunitL1Voltage.setTime(time);
      valueunits.add(valueunitL1Voltage);

      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setValues(valueunits);
      realTimeData.setTime(time);
      return realTimeData;
   }

   RealTimeData createServerHostRealTimeData(long time) {
      double[] storageIORateUsageValues = {330, 98, 57, 852, 224, 95, 209, 66, 56, 798, 236, 81, 213, 63, 61 };
      double[] memoryUsageValues = { 87.22, 87.22, 87.22, 87.22, 87.22, 87.22, 87.22, 87.22, 87.22, 87.22, 87.22, 87.22, 87.22, 87.22, 87.22 };
      double[] cpuUsedInMhzValues = { 746, 570, 552, 844, 651, 566, 552, 569, 538, 836, 655, 565, 571, 551, 570 };
      double[] cpuUsageValues = { 4.67, 3.57, 3.46, 5.28, 4.08, 3.55, 3.45, 3.56, 3.37, 5.23, 4.1, 3.53, 3.57, 3.45, 3.57 };
      double[] activeMemoryValues = { 1561416, 1561416, 2065824, 2065824, 2065824, 2065428, 2065428, 2065428, 1729924, 1729924, 1729924, 1561072, 1561072, 1561072, 2063852 };
      double[] sharedMemoryValues = { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 };
      double[] consumedMemoryValues = { 18291220, 18291236, 18291236, 18291236, 18291252, 18291252, 18291252, 18291156, 18291156, 18291200, 18291060, 18291172, 18291172, 18291188, 18291188 };
      double[] swapMemoryValues = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
      double[] balloonMemoryValues = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
      double[] networkUtilizationValues = { 146, 18, 15, 16, 16, 12, 9, 16, 10, 16, 17, 12, 12, 19, 29 };
      double[] powerValues = { 0.069, 0.069, 0.069, 0.069, 0.07, 0.069, 0.069, 0.069, 0.069, 0.069, 0.069, 0.069, 0.069, 0.069, 0.069 };
      double[] energyValues = { 0.00038805555555555555, 0.0003833333333333333, 0.0003833333333333333, 0.0003869444444444444, 0.00038888888888888887, 0.00038805555555555555, 0.0003833333333333333, 0.0003833333333333333, 0.0003833333333333333, 0.00038555555555555554, 0.00038805555555555555, 0.0003833333333333333, 0.0003833333333333333, 0.0003833333333333333, 0.00038555555555555554 };

      List<ValueUnit> valueUnits = new ArrayList<>();
      ValueUnit valueUnit;
      long startTime = 0;
      long endTime = 0;
      long maxValueTime = 0;
      long minValueTime = 0;
      for (int i = 0; i < 15; i++) {
         long tempTime = time + ((i + 1) * 20000);
         if (i == 0) {
            startTime = tempTime;
            minValueTime = tempTime;
         }
         if (i == 14) {
            endTime = tempTime;
         }
         if (i == 4) {
            maxValueTime = tempTime;
         }

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_STORAGEIORATEUSAGE);
         valueUnit.setUnit(MetricUnit.kBps.name());
         valueUnit.setValueNum(storageIORateUsageValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_MEMORYUSAGE);
         valueUnit.setUnit(MetricUnit.percent.name());
         valueUnit.setValueNum(memoryUsageValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_CPUUSEDINMHZ);
         valueUnit.setUnit(MetricUnit.Mhz.name());
         valueUnit.setValueNum(cpuUsedInMhzValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_CPUUSAGE);
         valueUnit.setUnit(MetricUnit.percent.name());
         valueUnit.setValueNum(cpuUsageValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_ACTIVEMEMORY);
         valueUnit.setUnit(MetricUnit.kB.name());
         valueUnit.setValueNum(activeMemoryValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_SHAREDMEMORY);
         valueUnit.setUnit(MetricUnit.kB.name());
         valueUnit.setValueNum(sharedMemoryValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_CONSUMEDMEMORY);
         valueUnit.setUnit(MetricUnit.kB.name());
         valueUnit.setValueNum(consumedMemoryValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_SWAPMEMORY);
         valueUnit.setUnit(MetricUnit.kB.name());
         valueUnit.setValueNum(swapMemoryValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_BALLOONMEMORY);
         valueUnit.setUnit(MetricUnit.kB.name());
         valueUnit.setValueNum(balloonMemoryValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_NETWORKUTILIZATION);
         valueUnit.setUnit(MetricUnit.kBps.name());
         valueUnit.setValueNum(networkUtilizationValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_POWER);
         valueUnit.setUnit(MetricUnit.kW.name());
         valueUnit.setValueNum(powerValues[i]);
         valueUnits.add(valueUnit);

         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_ENERGY_CONSUMPTION);
         valueUnit.setExtraidentifier(String.valueOf(tempTime - 20000));
         valueUnit.setUnit(MetricUnit.kWh.name());
         valueUnit.setValueNum(energyValues[i]);
         valueUnits.add(valueUnit);
      }
      valueUnit = new ValueUnit();
      valueUnit.setTime(endTime);
      valueUnit.setExtraidentifier(startTime + FlowgateConstant.SEPARATOR + minValueTime);
      valueUnit.setKey(MetricName.SERVER_MINIMUM_USED_POWER);
      valueUnit.setUnit(MetricUnit.kW.toString());
      valueUnit.setValueNum(0.069);
      valueUnits.add(valueUnit);

      valueUnit = new ValueUnit();
      valueUnit.setTime(endTime);
      valueUnit.setExtraidentifier(startTime + FlowgateConstant.SEPARATOR + maxValueTime);
      valueUnit.setKey(MetricName.SERVER_PEAK_USED_POWER);
      valueUnit.setUnit(MetricUnit.kW.toString());
      valueUnit.setValueNum(0.07);
      valueUnits.add(valueUnit);

      valueUnit = new ValueUnit();
      valueUnit.setTime(endTime);
      valueUnit.setExtraidentifier(String.valueOf(startTime));
      valueUnit.setKey(MetricName.SERVER_AVERAGE_USED_POWER);
      valueUnit.setUnit(MetricUnit.kW.toString());
      valueUnit.setValueNum(0.06906666666666665);
      valueUnits.add(valueUnit);


      valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      valueUnit.setKey(MetricName.SERVER_POWER);
      valueUnit.setUnit(MetricUnit.kW.toString());
      valueUnit.setValueNum(0.56);
      valueUnits.add(valueUnit);

      String sinceTime = String.valueOf(time - 900000l);
      valueUnit = new ValueUnit();
      String minimumPowerTime = String.valueOf(time - 30000l);
      valueUnit.setTime(time);
      valueUnit.setExtraidentifier(sinceTime + FlowgateConstant.SEPARATOR + minimumPowerTime);
      valueUnit.setKey(MetricName.SERVER_MINIMUM_USED_POWER);
      valueUnit.setUnit(MetricUnit.kW.toString());
      valueUnit.setValueNum(0.5);
      valueUnits.add(valueUnit);

      valueUnit = new ValueUnit();
      String peakPowerTime = String.valueOf(time - 60000l);
      valueUnit.setTime(time);
      valueUnit.setExtraidentifier(sinceTime + FlowgateConstant.SEPARATOR + peakPowerTime);
      valueUnit.setKey(MetricName.SERVER_PEAK_USED_POWER);
      valueUnit.setUnit(MetricUnit.kW.toString());
      valueUnit.setValueNum(0.8);
      valueUnits.add(valueUnit);

      valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      valueUnit.setExtraidentifier(sinceTime);
      valueUnit.setKey(MetricName.SERVER_AVERAGE_USED_POWER);
      valueUnit.setUnit(MetricUnit.kW.toString());
      valueUnit.setValueNum(0.6);
      valueUnits.add(valueUnit);

      valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      valueUnit.setExtraidentifier(sinceTime);
      valueUnit.setKey(MetricName.SERVER_ENERGY_CONSUMPTION);
      valueUnit.setUnit(MetricUnit.kWh.toString());
      valueUnit.setValueNum(356);
      valueUnits.add(valueUnit);

      valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      valueUnit.setExtraidentifier(sinceTime);
      valueUnit.setKey(MetricName.SERVER_AVERAGE_TEMPERATURE);
      valueUnit.setUnit(MetricUnit.C.toString());
      valueUnit.setValueNum(24);
      valueUnits.add(valueUnit);

      valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      String peakTemperatureTime = String.valueOf(time - 60000l);
      valueUnit.setExtraidentifier(sinceTime + FlowgateConstant.SEPARATOR + peakTemperatureTime);
      valueUnit.setKey(MetricName.SERVER_PEAK_TEMPERATURE);
      valueUnit.setUnit(MetricUnit.C.toString());
      valueUnit.setValueNum(30);
      valueUnits.add(valueUnit);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID("0001bdc8b25d4c2badfd045ab61aabfa");
      realTimeData.setValues(valueUnits);
      realTimeData.setTime(time);
      return realTimeData;
   }

   RealTimeData createServerPDURealTimeData(long time) {
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();
      ValueUnit totalPowerValueUnit = new ValueUnit();
      totalPowerValueUnit.setKey(MetricName.PDU_TOTAL_POWER);
      totalPowerValueUnit.setValueNum(1.033);
      totalPowerValueUnit.setTime(time);
      totalPowerValueUnit.setUnit(MetricUnit.kW.toString());
      valueunits.add(totalPowerValueUnit);

      ValueUnit totalCurrentValueUnit = new ValueUnit();
      totalCurrentValueUnit.setKey(MetricName.PDU_TOTAL_CURRENT);
      totalCurrentValueUnit.setValueNum(4.566);
      totalCurrentValueUnit.setTime(time);
      totalCurrentValueUnit.setUnit(MetricUnit.A.toString());
      valueunits.add(totalCurrentValueUnit);

      ValueUnit apparentPowerValueUnit = new ValueUnit();
      apparentPowerValueUnit.setKey(MetricName.PDU_APPARENT_POWER);
      apparentPowerValueUnit.setValueNum(1.033);
      apparentPowerValueUnit.setExtraidentifier("OUTLET:7");
      apparentPowerValueUnit.setTime(time);
      apparentPowerValueUnit.setUnit(MetricUnit.kW.toString());
      valueunits.add(apparentPowerValueUnit);

      ValueUnit currentValueUnit = new ValueUnit();
      currentValueUnit.setKey(MetricName.PDU_CURRENT);
      currentValueUnit.setValueNum(0.633);
      currentValueUnit.setExtraidentifier("OUTLET:7");
      currentValueUnit.setTime(time);
      currentValueUnit.setUnit(MetricUnit.A.toString());
      valueunits.add(currentValueUnit);

      ValueUnit voltageValueUnit = new ValueUnit();
      voltageValueUnit.setKey(MetricName.PDU_VOLTAGE);
      voltageValueUnit.setValueNum(226);
      voltageValueUnit.setExtraidentifier("OUTLET:7");
      voltageValueUnit.setTime(time);
      voltageValueUnit.setUnit(MetricUnit.V.toString());
      valueunits.add(voltageValueUnit);

      ValueUnit powerLoadValueUnit = new ValueUnit();
      powerLoadValueUnit.setKey(MetricName.PDU_POWER_LOAD);
      powerLoadValueUnit.setValueNum(0.134);
      powerLoadValueUnit.setTime(time);
      powerLoadValueUnit.setUnit(MetricUnit.percent.toString());
      valueunits.add(powerLoadValueUnit);

      ValueUnit currentLoadValueUnit = new ValueUnit();
      currentLoadValueUnit.setKey(MetricName.PDU_CURRENT_LOAD);
      currentLoadValueUnit.setValueNum(0.1427);
      currentLoadValueUnit.setTime(time);
      currentLoadValueUnit.setUnit(MetricUnit.percent.toString());
      valueunits.add(currentLoadValueUnit);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID("0001bdc8b25d4c2badfd045ab61aabfa");
      realTimeData.setValues(valueunits);
      realTimeData.setTime(time);
      return realTimeData;
   }

   RealTimeData createSensorRealtimeData(long time) {
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();

      ValueUnit tempValue = new ValueUnit();
      tempValue.setValueNum(32);
      tempValue.setTime(time);
      tempValue.setUnit(MetricUnit.C.toString());
      tempValue.setKey(MetricName.TEMPERATURE);
      valueunits.add(tempValue);

      ValueUnit humidityValue = new ValueUnit();
      humidityValue.setValueNum(20);
      humidityValue.setTime(time);
      humidityValue.setUnit(MetricUnit.percent.toString());
      humidityValue.setKey(MetricName.HUMIDITY);
      valueunits.add(humidityValue);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID("00027ca37b004a9890d1bf20349d5ac1");
      realTimeData.setValues(valueunits);
      realTimeData.setTime(time);
      return realTimeData;
   }

   RealTimeData createTemperatureSensorRealtimeData(long time, String sensorAssetId) {
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();

      ValueUnit tempValue = new ValueUnit();
      tempValue.setValueNum(32);
      tempValue.setTime(time);
      tempValue.setUnit(MetricUnit.C.toString());
      tempValue.setKey(MetricName.TEMPERATURE);
      valueunits.add(tempValue);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID(sensorAssetId);
      realTimeData.setValues(valueunits);
      realTimeData.setTime(time);
      return realTimeData;
   }

   RealTimeData createHumiditySensorRealtimeData(long time, String sensorAssetId) {
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();
      ValueUnit humidityValue = new ValueUnit();
      humidityValue.setValueNum(20);
      humidityValue.setTime(time);
      humidityValue.setUnit(MetricUnit.percent.toString());
      humidityValue.setKey(MetricName.HUMIDITY);
      valueunits.add(humidityValue);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID(sensorAssetId);
      realTimeData.setValues(valueunits);
      realTimeData.setTime(time);
      return realTimeData;
   }
   RealTimeData createBackTemperatureSensorRealtimeData(long time, String sensorAssetId) {
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();

      ValueUnit tempValue = new ValueUnit();
      tempValue.setValueNum(25);
      tempValue.setTime(time);
      tempValue.setUnit(MetricUnit.C.toString());
      tempValue.setKey(MetricName.TEMPERATURE);
      valueunits.add(tempValue);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID(sensorAssetId);
      realTimeData.setValues(valueunits);
      realTimeData.setTime(time);
      return realTimeData;
   }

   RealTimeData createBackHumiditySensorRealtimeData(long time, String sensorAssetId) {
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();
      ValueUnit humidityValue = new ValueUnit();
      humidityValue.setValueNum(19);
      humidityValue.setTime(time);
      humidityValue.setUnit(MetricUnit.percent.toString());
      humidityValue.setKey(MetricName.HUMIDITY);
      valueunits.add(humidityValue);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID(sensorAssetId);
      realTimeData.setValues(valueunits);
      realTimeData.setTime(time);
      return realTimeData;
   }

   AssetIPMapping createAssetIPMapping() {
      AssetIPMapping assetipmapping = new AssetIPMapping();
      assetipmapping.setId(UUID.randomUUID().toString());
      assetipmapping.setAssetname("assetname");
      assetipmapping.setMacAddress("00:50:56:be:60:62");
      assetipmapping.setIp("127.0.0.1");
      return assetipmapping;
   }

   Asset createAsset() {
      Asset asset = new Asset();
      BaseDocumentUtil.generateID(asset);
      asset.setAssetName("pek-wor-server-02");
      asset.setAssetNumber(12345);
      asset.setAssetSource("5b7d208d55368540fcba1692");
      asset.setCategory(AssetCategory.Server);
      asset.setModel("Dell 750");
      asset.setManufacturer("Dell");
      asset.setSerialnumber("Serialnumber");
      asset.setRegion("Region");
      asset.setCountry("china");
      asset.setCity("beijing");
      asset.setBuilding("Raycom");
      asset.setFloor("9F");
      asset.setRoom("901");
      asset.setRow("9");
      asset.setCol("9");
      asset.setExtraLocation("");
      asset.setCabinetName("");
      List <String> pdus = new ArrayList<>();
      asset.setPdus(pdus);
      List <String> switches = new ArrayList<>();
      asset.setSwitches(switches);
      AssetStatus status = new AssetStatus();
      asset.setStatus(status);
      Map<String, String> formulas = new HashMap<String, String>();
      Map<String, Map<String, String>> pduMetricFormulas = new HashMap<String, Map<String, String>>();
      Map<String, String> MetricAndIdMap = new HashMap<String,String>();
      MetricAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa");
      MetricAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_POWER, "0001bdc8b25d4c2badfd045ab61aabfa");
      MetricAndIdMap.put(MetricName.SERVER_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricFormulas.put("0001bdc8b25d4c2badfd045ab61aabfa", MetricAndIdMap);
      String pduFormulaInfo = null;
      try {
         pduFormulaInfo = mapper.writeValueAsString(pduMetricFormulas);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      formulas.put(FlowgateConstant.PDU, pduFormulaInfo);
      asset.setMetricsformulars(formulas);
      return asset;
   }

   Asset createSensor() {
      Asset asset = new Asset();
      BaseDocumentUtil.generateID(asset);
      asset.setAssetName("Temperature-02");
      asset.setAssetNumber(12345);
      asset.setAssetSource("5b7d208d55368540fcba1692");
      asset.setCategory(AssetCategory.Sensors);
      Map<String, String> formulas = new HashMap<String, String>();
      Map<String, String> sensorFormulaMap = new HashMap<>();
      sensorFormulaMap.put(MetricName.TEMPERATURE, asset.getId());
      formulas.put(FlowgateConstant.SENSOR, asset.metricsFormulaToString(sensorFormulaMap));
      asset.setMetricsformulars(formulas);
      return asset;
   }

   Asset createPDU() {
      Asset asset = new Asset();
      BaseDocumentUtil.generateID(asset);
      asset.setAssetName("pdu-02");
      asset.setAssetNumber(12345);
      asset.setAssetSource("5b7d208d55368540fcba1692");
      asset.setCategory(AssetCategory.PDU);
      asset.setSerialnumber("Serialnumber");
      asset.setRegion("Region");
      asset.setCountry("china");
      asset.setCity("beijing");
      asset.setBuilding("Raycom");
      asset.setFloor("9F");
      asset.setRoom("901");
      asset.setRow("9");
      asset.setCol("9");
      String pduAssetId = asset.getId();
      Map<String, String> formulas = new HashMap<String, String>();
      Map<String, String> pduFormulaMap = new HashMap<>();
      pduFormulaMap.put(MetricName.PDU_VOLTAGE, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_CURRENT, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_TOTAL_POWER, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_CURRENT_LOAD, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_POWER_LOAD, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_XLET_ACTIVE_POWER, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_XLET_APPARENT_POWER, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_XLET_FREE_CAPACITY, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_XLET_CURRENT, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_XLET_VOLTAGE, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_INLET_XPOLE_FREE_CAPACITY, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_INLET_XPOLE_CURRENT, pduAssetId);
      pduFormulaMap.put(MetricName.PDU_INLET_XPOLE_VOLTAGE, pduAssetId);

      Map<String, Map<String, String>> sensorMetricFormulas =
            new HashMap<String, Map<String, String>>();
      Map<String, String> tempSensor = new HashMap<String,String>();
      tempSensor.put("INLET", "00027ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulas.put(MetricName.PDU_TEMPERATURE, tempSensor);
      Map<String, String> humiditySensor = new HashMap<String,String>();
      humiditySensor.put("OUTLET", "34527ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulas.put(MetricName.PDU_HUMIDITY, humiditySensor);

      formulas.put(FlowgateConstant.PDU, asset.metricsFormulaToString(pduFormulaMap));
      formulas.put(FlowgateConstant.SENSOR, asset.metricsFormulaToString(sensorMetricFormulas));
      asset.setMetricsformulars(formulas);
      return asset;
   }

   FacilitySoftwareConfig createFacilitySoftware() {
      FacilitySoftwareConfig example = new FacilitySoftwareConfig();
      example.setId("5b7d208d55368540fcba1692");
      example.setName("Nlyte");
      example.setType(FacilitySoftwareConfig.SoftwareType.Nlyte);
      example.setVerifyCert(false);
      return example;
   }

   Asset fillingMetricsformula(Asset asset){
      Map<String, String> formulars = new HashMap<String, String>();
      Map<String, Map<String, String>> pduMetricFormulars = new HashMap<String, Map<String, String>>();
      Map<String, String> pduMetricAndIdMap = new HashMap<String,String>();
      pduMetricAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_POWER, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_USED_PDU_OUTLET_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_USED_PDU_OUTLET_POWER, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_USED_PDU_OUTLET_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_POWER_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_CURRENT_LOAD, "0001bdc8b25d4c2badfd045ab61aabfa");

      pduMetricFormulars.put("0001bdc8b25d4c2badfd045ab61aabfa", pduMetricAndIdMap);

      Map<String, Map<String, String>> sensorMetricFormulars = new HashMap<String, Map<String, String>>();
      Map<String, String> frontTempSensor = new HashMap<String,String>();
      frontTempSensor.put("INLET", "00027ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulars.put(MetricName.SERVER_FRONT_TEMPERATURE, frontTempSensor);
      Map<String, String> backTempSensor = new HashMap<String,String>();
      backTempSensor.put("OUTLET", "968765a37b004a9890d1bf20349d5ac1");
      sensorMetricFormulars.put(MetricName.SERVER_BACK_TEMPREATURE, backTempSensor);
      Map<String, String> frontHumiditySensor = new HashMap<String,String>();
      frontHumiditySensor.put("INLET", "34527ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulars.put(MetricName.SERVER_FRONT_HUMIDITY, frontHumiditySensor);
      Map<String, String> backHumiditySensor = new HashMap<String,String>();
      backHumiditySensor.put("OUTLET", "486970a37b004a9890d1bf20349d5ac1");
      sensorMetricFormulars.put(MetricName.SERVER_BACK_HUMIDITY, backHumiditySensor);
      String pduFormulaInfo = null;
      String sensorFormulaInfo = null;
      try {
         pduFormulaInfo = mapper.writeValueAsString(pduMetricFormulars);
         sensorFormulaInfo = mapper.writeValueAsString(sensorMetricFormulars);
      } catch (JsonProcessingException e) {
         TestCase.fail(e.getMessage());
      }
      formulars.put(FlowgateConstant.PDU, pduFormulaInfo);
      formulars.put(FlowgateConstant.SENSOR, sensorFormulaInfo);
      asset.setMetricsformulars(formulars);
      Map<String, String> hostMetrics = new HashMap<>();
      hostMetrics.put(MetricName.SERVER_CPUUSAGE, asset.getId());
      hostMetrics.put(MetricName.SERVER_CPUUSEDINMHZ, asset.getId());
      hostMetrics.put(MetricName.SERVER_ACTIVEMEMORY, asset.getId());
      hostMetrics.put(MetricName.SERVER_BALLOONMEMORY, asset.getId());
      hostMetrics.put(MetricName.SERVER_CONSUMEDMEMORY, asset.getId());
      hostMetrics.put(MetricName.SERVER_SHAREDMEMORY, asset.getId());
      hostMetrics.put(MetricName.SERVER_SWAPMEMORY, asset.getId());
      hostMetrics.put(MetricName.SERVER_MEMORYUSAGE, asset.getId());
      hostMetrics.put(MetricName.SERVER_STORAGEIORATEUSAGE, asset.getId());
      hostMetrics.put(MetricName.SERVER_STORAGEUSAGE, asset.getId());
      hostMetrics.put(MetricName.SERVER_STORAGEUSED, asset.getId());
      hostMetrics.put(MetricName.SERVER_TEMPERATURE, asset.getId());
      hostMetrics.put(MetricName.SERVER_PEAK_TEMPERATURE, asset.getId());
      hostMetrics.put(MetricName.SERVER_AVERAGE_TEMPERATURE, asset.getId());
      hostMetrics.put(MetricName.SERVER_ENERGY_CONSUMPTION, asset.getId());
      hostMetrics.put(MetricName.SERVER_POWER, asset.getId());
      hostMetrics.put(MetricName.SERVER_AVERAGE_USED_POWER, asset.getId());
      hostMetrics.put(MetricName.SERVER_PEAK_USED_POWER, asset.getId());
      hostMetrics.put(MetricName.SERVER_MINIMUM_USED_POWER, asset.getId());
      formulars.put(FlowgateConstant.HOST_METRICS, asset.metricsFormulaToString(hostMetrics));
      asset.setMetricsformulars(formulars);
      return asset;
   }
}
