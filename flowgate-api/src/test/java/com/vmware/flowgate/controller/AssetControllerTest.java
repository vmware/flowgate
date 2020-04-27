/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.AssetIPMappingRepository;
import com.vmware.flowgate.repository.AssetRealtimeDataRepository;
import com.vmware.flowgate.repository.AssetRepository;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.repository.ServerMappingRepository;
import com.vmware.flowgate.util.BaseDocumentUtil;

import junit.framework.TestCase;

class MappingIdForDoc {
   public String FirstId;
   public String SecondId;
}

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

   @MockBean
   private StringRedisTemplate template;

   @Rule
   public ExpectedException expectedEx = ExpectedException.none();

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
                  fieldWithPath("justificationfields").ignored(),
                  fieldWithPath("metricsformulars").ignored(),
                  fieldWithPath("lastupdate").ignored(), fieldWithPath("created").ignored(),
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
                  fieldWithPath("status").description(
                        "This is a collection of states, including the state of the asset, "
                              + "the state of the pdu mapping, and the state of the switch mapping."))))
            .andReturn().getResponse().getHeader("Location");
      assetRepository.delete(asset.getId());
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
                        fieldWithPath("vroID").description("ID of VROps."),
                        fieldWithPath("vroResourceName")
                              .description("Resource Name in VROps for this server."),
                        fieldWithPath("vroVMEntityName").description("EntityName of Resource."),
                        fieldWithPath("vroVMEntityObjectID").description("VROps Entity Object ID."),
                        fieldWithPath("vroVMEntityVCID").description("VROps Entity's Vcenter ID."),
                        fieldWithPath("vroResourceID").description("VROps Resource ID."))));
      serverMappingRepository.delete(mapping.getId());
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
      Map<String, Map<String, Map<String, String>>> metricsformulars =
            new HashMap<String, Map<String, Map<String, String>>>();
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
      ObjectMapper mapper = new ObjectMapper();
      try {
         sensorAssetJustfication.put(FlowgateConstant.SENSOR, mapper.writeValueAsString(sensorInfo));
         humiditySensorAsset.setJustificationfields(sensorAssetJustfication);
      } catch (JsonProcessingException e) {
         TestCase.fail();
      }
      humiditySensorAsset = assetRepository.save(humiditySensorAsset);
      positionInfo.put(humiditySensorAsset.getId(), humiditySensorAsset.getId());
      sensorMap.put(MetricName.SERVER_FRONT_HUMIDITY, positionInfo);
      metricsformulars.put(FlowgateConstant.SENSOR, sensorMap);
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
            fieldWithPath("metricsformulars").description("Possible PDUs And sensors that this server connected with"),
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
            fieldWithPath("status").description(
                        "This is a collection of states, including the state of the asset, "
                              + "the state of the pdu mapping, and the state of the switch mapping."))));
      Asset testAsset = assetRepository.findOne(asset.getId());
      TestCase.assertEquals(1, testAsset.getPdus().size());
      TestCase.assertEquals("oqwen812321093asdmgtqawee1", testAsset.getPdus().get(0));

      TestCase.assertEquals(1, testAsset.getSwitches().size());
      TestCase.assertEquals("ow23aw312e3nr3d2a57788i", testAsset.getSwitches().get(0));

      TestCase.assertEquals(FlowgateConstant.RACK_UNIT_PREFIX + humiditySensorAsset.getCabinetUnitPosition()+FlowgateConstant.SEPARATOR+"INLET",
            testAsset.getMetricsformulars().get(FlowgateConstant.SENSOR).get(MetricName.SERVER_FRONT_HUMIDITY).keySet().iterator().next());
      assetRepository.delete(testAsset.getId());
      assetRepository.delete(humiditySensorAsset.getId());
   }

   @Test
   public void insertRealtimeDataExample() throws JsonProcessingException, Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      RealTimeData realtime = new RealTimeData();
      realtime.setId(UUID.randomUUID().toString());
      List<ValueUnit> values = new ArrayList<ValueUnit>();
      ValueUnit v = new ValueUnit();
      v.setValue("123");
      values.add(0, v);
      realtime.setAssetID(asset.getId());
      realtime.setTime(1234456);
      realtime.setValues(values);
      realtimeDataRepository.save(realtime);

      this.mockMvc
            .perform(post("/v1/assets/" + asset.getId() + "/sensordata")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(realtime)))
            .andExpect(status().isOk())
            .andDo(document("assets-insertRealtimeData-example", requestFields(
                  fieldWithPath("id").description("ID of the realtime, created by flowgate"),
                  fieldWithPath("assetID").description("ID of the asset, created by flowgate"),
                  fieldWithPath("values")
                        .description("A list of sensor data. eg. Humidity , Electric... ")
                        .type(ValueUnit[].class),
                  fieldWithPath("time").description("The time of generate sensor data."))));
      assetRepository.delete(asset.getId());
      realtimeDataRepository.delete(realtime.getId());
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
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("metricsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("capacity").description("The capacity of asset.").type(int.class).optional(),
            fieldWithPath("freeCapacity").description("The free capacity of asset.").type(int.class).optional(),
            fieldWithPath("parent").description("The parent of asset,it will be null unless the asset's category is Sensors")
            .type(Parent.class).optional(),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };
      this.mockMvc
            .perform(post("/v1/assets/batchoperation").contentType(MediaType.APPLICATION_JSON_VALUE)
                  .content(objectMapper.writeValueAsString(assets)))
            .andExpect(status().isCreated())
            .andDo(document("assets-createBatch-example",
                  requestFields(fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)));

      assetRepository.delete(asset1.getId());
      assetRepository.delete(asset2.getId());
   }

   @Test
   public void realTimeDatabatchCreationExample() throws JsonProcessingException, Exception {
      List<RealTimeData> realtimedatas = new ArrayList<RealTimeData>();
      RealTimeData realtimedata1 = createServerPDURealTimeData();
      realtimedata1.setAssetID("assetid1");
      realtimedatas.add(realtimedata1);
      RealTimeData realtimedata2 = createServerPDURealTimeData();
      realtimedata2.setAssetID("assetid2");
      realtimedatas.add(realtimedata2);

      FieldDescriptor[] fieldpath =
            new FieldDescriptor[] { fieldWithPath("id").description("ID of the RealTimeData"),
                  fieldWithPath("assetID").description("ID of the asset, created by flowgate"),
                  fieldWithPath("values").description("List of ValueUnit") };
     this.mockMvc
     .perform(post("/v1/assets/sensordata/batchoperation")
           .contentType(MediaType.APPLICATION_JSON_VALUE)
           .content(objectMapper.writeValueAsString(realtimedatas)))
     .andExpect(status().isCreated())
     .andDo(document("assets-realTimeDatabatchCreation-example",
           requestFields(fieldWithPath("[]").description("An array of RealTimeData"))
                 .andWithPrefix("[].", fieldpath)));
     realtimeDataRepository.delete(realtimedata1.getId());
     realtimeDataRepository.delete(realtimedata2.getId());

   }

   @Test
   public void readAssetBySourceAndTypeExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      try {
         MvcResult result = this.mockMvc
               .perform(get(
                     "/v1/assets/source/" + asset.getAssetSource() + "/type/" + asset.getCategory()+"/?currentPage=1&pageSize=5"))
               .andExpect(status().isOk())
               .andDo(document("assets-getBySourceAndType-example",
                       responseFields(fieldWithPath("content").description("An assets array."),
                               fieldWithPath("totalPages").description("content's total pages."),
                               fieldWithPath("totalElements").description("content's total elements."),
                               fieldWithPath("last").description("Is the last."),
                               fieldWithPath("number").description("The page number."),
                               fieldWithPath("size").description("The page size."),
                               fieldWithPath("sort").description("The sort."),
                               fieldWithPath("numberOfElements").description("The number of Elements."),
                               fieldWithPath("first").description("Is the first."))))
               .andReturn();
      }finally {
         assetRepository.delete(asset.getId());
      }
   }

   @Test
   public void readAssetByTypeExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      try {
         this.mockMvc.perform(get("/v1/assets/type/" + asset.getCategory()+"/?currentPage=1&pageSize=5")).andExpect(status().isOk())
         .andDo(document("assets-getByType-example",
                 responseFields(fieldWithPath("content").description("An assets array."),
                         fieldWithPath("totalPages").description("content's total pages."),
                         fieldWithPath("totalElements").description("content's total elements."),
                         fieldWithPath("last").description("Is the last."),
                         fieldWithPath("number").description("The page number."),
                         fieldWithPath("size").description("The page size."),
                         fieldWithPath("sort").description("The sort."),
                         fieldWithPath("numberOfElements").description("The number of Elements."),
                         fieldWithPath("first").description("Is the first."))))
         .andReturn();
      }finally {
         assetRepository.delete(asset.getId());
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
               fieldWithPath("assetname").description("name of asset.") };
         this.mockMvc.perform(get("/v1/assets/mapping/hostnameip/ip/" + mapping1.getIp()))
               .andExpect(status().isOk())
               .andDo(document("assets-getHostNameByIP-example",
                     responseFields(fieldWithPath("[]").description("An array of ServerMappings"))
                           .andWithPrefix("[].", fieldpath)));
      }finally {
         assetIPMappingRepository.delete(mapping1.getId());
         assetIPMappingRepository.delete(mapping2.getId());
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
                     responseFields(fieldWithPath("[]").description("An array of asserts"))
                           .andWithPrefix("[].", fieldpath)));
      } finally {
         serverMappingRepository.delete(mapping1.getId());
         serverMappingRepository.delete(mapping2.getId());
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
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("metricsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("capacity").description("The capacity of asset.").type(int.class).optional(),
            fieldWithPath("freeCapacity").description("The free capacity of asset.").type(int.class).optional(),
            fieldWithPath("parent").description("The parent of asset,it will be null unless the asset's category is Sensors")
            .type(Parent.class).optional(),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };

      try {
         this.mockMvc.perform(get("/v1/assets/mappedasset/category/" + asset.getCategory()))
         .andDo(document("assets-getMapped-example",
               responseFields(fieldWithPath("[]").description("An array of asserts"))
                     .andWithPrefix("[].", fieldpath)));
      }finally {
         assetRepository.delete(asset.getId());
         serverMappingRepository.delete(mapping.getId());
         assetRepository.delete(asset2.getId());
         serverMappingRepository.delete(mapping2.getId());
      }
   }

   @Test
   public void readAssetsByAssetNameAndTagLikExample() throws Exception {
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
         this.mockMvc.perform(get("/v1/assets/page/" + pageNumber + "/pagesize/" + pageSize))
         .andExpect(status().isOk())
         .andDo(document("assets-getByAssetNameAndTagLik-example",
               responseFields(fieldWithPath("content").description("ServerMapping's array."),
                     fieldWithPath("totalPages").description("content's total pages."),
                     fieldWithPath("totalElements").description("content's total elements."),
                     fieldWithPath("last").description("Is the last."),
                     fieldWithPath("number").description("The page number."),
                     fieldWithPath("size").description("The page size."),
                     fieldWithPath("sort").description("The sort."),
                     fieldWithPath("numberOfElements").description("The number of Elements."),
                     fieldWithPath("first").description("Is the first."))));

      }finally {
         assetRepository.delete(asset1.getId());
         assetRepository.delete(asset2.getId());
         facilitySoftwareRepository.delete(facility.getId());
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
         .perform(get("/v1/assets/page/"
               + pageNumber + "/pagesize/" + pageSize + "/keywords/" + keywords))
         .andExpect(status().isOk())
         .andDo(document("assets-getByAssetNameAndTagLikAndKeywords-example",
               responseFields(fieldWithPath("content").description("ServerMapping's array."),
                     fieldWithPath("totalPages").description("content's total pages."),
                     fieldWithPath("totalElements").description("content's total elements."),
                     fieldWithPath("last").description("Is the last."),
                     fieldWithPath("number").description("The page number."),
                     fieldWithPath("size").description("The page size."),
                     fieldWithPath("sort").description("The sort."),
                     fieldWithPath("numberOfElements").description("The number of Elements."),
                     fieldWithPath("first").description("Is the first."))));
      }finally {
         assetRepository.delete(asset1.getId());
         assetRepository.delete(asset2.getId());
         facilitySoftwareRepository.delete(facility.getId());
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
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("metricsformulars")
                  .description("The formula of metrics data for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("capacity").description("The capacity of asset.").type(int.class).optional(),
            fieldWithPath("freeCapacity").description("The free capacity of asset.").type(int.class).optional(),
            fieldWithPath("parent").description("The parent of asset,it will be null unless the asset's category is Sensors")
            .type(Parent.class).optional(),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };
      try {
         this.mockMvc.perform(get("/v1/assets/pdusisnull"))
         .andDo(document("assets-findServersWithoutPDUInfo-example",
               responseFields(fieldWithPath("[]").description("An array of asserts"))
                     .andWithPrefix("[].", fieldpath)));
      }finally {
         assetRepository.delete(asset.getId());
         serverMappingRepository.delete(mapping.getId());
         assetRepository.delete(asset2.getId());
         serverMappingRepository.delete(mapping2.getId());
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
            fieldWithPath("room").description("The location floor of the asset"),
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
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("metricsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("capacity").description("The capacity of asset.").type(int.class).optional(),
            fieldWithPath("freeCapacity").description("The free capacity of asset.").type(int.class).optional(),
            fieldWithPath("parent").description("The parent of asset,it will be null unless the asset's category is Sensors")
            .type(Parent.class).optional(),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };
      try {
         this.mockMvc.perform(get("/v1/assets/pdusisnotnull"))
         .andExpect(jsonPath("$[0].assetNumber", is(12345)))
         .andExpect(jsonPath("$[0].assetName", is("pek-wor-server-02")))
         .andExpect(jsonPath("$[0].pdus", hasSize(2)))
         .andExpect(jsonPath("$[0].pdus[0]", is("pdu1")))
         .andDo(document("assets-findServersWithPDUInfo-example",
               responseFields(fieldWithPath("[]").description("An array of asserts"))
                     .andWithPrefix("[].", fieldpath)))
         .andReturn().getResponse();
      }finally {
         assetRepository.delete(asset.getId());
         serverMappingRepository.delete(mapping.getId());
         assetRepository.delete(asset2.getId());
         serverMappingRepository.delete(mapping2.getId());
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
               .perform(get("/v1/assets/vrops/90o76d5655368548d42e0fd5").content("{\"vroID\":\"90o76d5655368548d42e0fd5\"}"))
               .andExpect(status().isOk())
               .andDo(document("assets-getAssetsByVroId-example", requestFields(
                    fieldWithPath("vroID").description("ID of VROps"))))
               .andReturn();
               ObjectMapper mapper = new ObjectMapper();
               String res = result.getResponse().getContentAsString();
               Asset [] assets = mapper.readValue(res, Asset[].class);
               TestCase.assertEquals(asset.getId(), assets[0].getId());
      }finally {
         serverMappingRepository.delete(mapping1.getId());
         serverMappingRepository.delete(mapping2.getId());
         assetRepository.delete(asset.getId());
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
                     .perform(get("/v1/assets/vc/5b7cfd5655368548d42e0fd5")
                           .content("{\"vcID\":\"5b7cfd5655368548d42e0fd5\"}"))
                     .andExpect(status().isOk())
                     .andDo(document("assets-getAssetsByVcId-example",
                           requestFields(fieldWithPath("vcID").description("ID of VCENER"))))
                     .andReturn();
         ObjectMapper mapper = new ObjectMapper();
         String res = result.getResponse().getContentAsString();
         Asset[] assets = mapper.readValue(res, Asset[].class);
         TestCase.assertEquals(asset.getId(), assets[0].getId());
      } finally {
         serverMappingRepository.delete(mapping1.getId());
         serverMappingRepository.delete(mapping2.getId());
         assetRepository.delete(asset.getId());
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
         .perform(get("/v1/assets/mapping/vrops/"
               + vropsID + "/page/" + pageNumber + "/pagesize/" + pageSize))
         .andExpect(status().isOk())
         .andDo(document("assets-getPageMappingsByVROPSId-example",
               responseFields(fieldWithPath("content").description("ServerMapping's array."),
                     fieldWithPath("totalPages").description("content's total pages."),
                     fieldWithPath("totalElements").description("content's total elements."),
                     fieldWithPath("last").description("Is the last."),
                     fieldWithPath("number").description("The page number."),
                     fieldWithPath("size").description("The page size."),
                     fieldWithPath("sort").description("The sort."),
                     fieldWithPath("numberOfElements").description("The number of Elements."),
                     fieldWithPath("first").description("Is the first."))));

      }finally {
         serverMappingRepository.delete(mapping1.getId());
         serverMappingRepository.delete(mapping2.getId());
      }
   }

   @Test
   public void getPageMappingsByVCIdExample() throws Exception {
      ServerMapping mapping1 = createServerMapping();
      mapping1.setVcClusterMobID("1");
      mapping1.setVcHostName("1");
      mapping1.setVroID("1");
      mapping1.setVcID("1");
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
               get("/v1/assets/mapping/vc/" + vcID + "/page/" + pageNumber + "/pagesize/" + pageSize))
               .andExpect(status().isOk())
               .andDo(document("assets-getPageMappingsByVCId-example",
                     responseFields(fieldWithPath("content").description("ServerMapping's array."),
                           fieldWithPath("totalPages").description("content's total pages."),
                           fieldWithPath("totalElements").description("content's total elements."),
                           fieldWithPath("last").description("Is the last."),
                           fieldWithPath("number").description("The page number."),
                           fieldWithPath("size").description("The page size."),
                           fieldWithPath("sort").description("The sort."),
                           fieldWithPath("numberOfElements").description("The number of Elements."),
                           fieldWithPath("first").description("Is the first."))));

      }finally {
         serverMappingRepository.delete(mapping1.getId());
         serverMappingRepository.delete(mapping2.getId());
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

      this.mockMvc
            .perform(post("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(assetipmapping)))
            .andExpect(status().isCreated())
            .andDo(document("assets-createHostNameIPMapping-example", requestFields(
                  fieldWithPath("id").description("ID of the asset, created by flowgate"),
                  fieldWithPath("ip").description("ip of hostname"),
                  fieldWithPath("assetname").description(
                        "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"))));
      assetipmapping = assetIPMappingRepository.findOne(assetipmapping.getId());
      assetRepository.delete(server.getId());
      assetIPMappingRepository.delete(assetipmapping.getId());
      TestCase.assertEquals(server.getAssetName(), assetipmapping.getAssetname());
   }

   @Test
   public void createHostNameAndIPMappingFailureExample() throws Exception {
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Invalid ip address");
      AssetIPMapping mapping = new AssetIPMapping();
      mapping.setAssetname("cloud-sha1-esx2");
      mapping.setIp("10.15");
      MvcResult result = this.mockMvc
      .perform(post("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapping)))
      .andExpect(status().is5xxServerError())
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
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("The Asset name is not exist : " + mapping.getAssetname());
      MvcResult result = this.mockMvc
      .perform(post("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapping)))
      .andExpect(status().is5xxServerError())
      .andReturn();
      if (result.getResolvedException() != null) {
         assetRepository.delete(server.getId());
         throw result.getResolvedException();
      }
   }

   @Test
   public void deleteAssetIPAndNameMappingExample() throws Exception {
      AssetIPMapping assetipmapping = createAssetIPMapping();
      assetipmapping.setAssetname("cloud-server");
      assetipmapping = assetIPMappingRepository.save(assetipmapping);
      this.mockMvc.perform(delete("/v1/assets/mapping/hostnameip/" + assetipmapping.getId()))
            .andExpect(status().isOk()).andDo(document("assets-deleteAssetIPAndNameMapping-example"));
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
      this.mockMvc
            .perform(post("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(newAssetIPMapping)))
            .andExpect(status().isCreated())
            .andDo(document("assets-updateHostNameIPMapping-example", requestFields(
                  fieldWithPath("id").description("ID of the asset, created by flowgate"),
                  fieldWithPath("ip").description("ip of hostname"),
                  fieldWithPath("assetname").description(
                        "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"))));
      assetipmapping = assetIPMappingRepository.findOne(assetipmapping.getId());
      assetRepository.delete(server.getId());
      assetRepository.delete(server1.getId());
      assetIPMappingRepository.delete(assetipmapping.getId());
      TestCase.assertEquals(server1.getAssetName(), assetipmapping.getAssetname());
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
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("The Asset name is not exist : " + newAssetIPMapping.getAssetname());
      MvcResult result = this.mockMvc
            .perform(post("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(newAssetIPMapping)))
            .andExpect(status().is5xxServerError())
            .andReturn();
      if (result.getResolvedException() != null) {
         assetRepository.delete(server.getId());
         assetRepository.delete(server1.getId());
         assetIPMappingRepository.delete(assetipmapping.getId());
         throw result.getResolvedException();
      }
   }

   @Test
   public void getHostNameIPMappingByPage() throws Exception {
      AssetIPMapping assetipmapping = createAssetIPMapping();
      assetipmapping.setAssetname("cloud-sha2-esx2");
      assetIPMappingRepository.deleteAll();
      assetipmapping = assetIPMappingRepository.save(assetipmapping);
      this.mockMvc
      .perform(get("/v1/assets/mapping/hostnameip?pagesize=10&pagenumber=1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$..totalPages").value(1))
      .andExpect(jsonPath("$..content[0].ip").value(assetipmapping.getIp()))
      .andExpect(jsonPath("$..content[0].assetname").value(assetipmapping.getAssetname()))
      .andDo(document("assets-getHostNameIPMappingByPage-example",
            responseFields(fieldWithPath("content").description("AssetIPMapping's array."),
                  fieldWithPath("totalPages").description("content's total pages."),
                  fieldWithPath("totalElements").description("content's total elements."),
                  fieldWithPath("last").description("Is the last."),
                  fieldWithPath("number").description("The page number."),
                  fieldWithPath("size").description("The page size."),
                  fieldWithPath("sort").description("The sort."),
                  fieldWithPath("numberOfElements").description("The number of Elements."),
                  fieldWithPath("first").description("Is the first."))));
      assetIPMappingRepository.delete(assetipmapping.getId());
   }

   @Test
   public void getServerMappingByID() throws Exception {
      ServerMapping mapping = createServerMapping();
      mapping.setVcClusterMobID("1001");
      mapping.setVcHostName("host1");
      serverMappingRepository.save(mapping);
      this.mockMvc.perform(get("/v1/assets/mapping/" + mapping.getId() + "")).andExpect(status().isOk())
                  .andExpect(jsonPath("vcClusterMobID", is(mapping.getVcClusterMobID())))
                  .andExpect(jsonPath("vcHostName", is(mapping.getVcHostName())))
                  .andDo(document("assets-getServerMappingByID-example",
                        responseFields(
                              fieldWithPath("id").description("ID of the mapping, created by flowgate"),
                              fieldWithPath("asset").description("An asset for serverMapping."),
                              fieldWithPath("vcID").description("ID of Vcenter."),
                              fieldWithPath("vcHostName")
                                    .description("Server's hostname display in Vcenter."),
                              fieldWithPath("vcMobID").description("EXSI server's management object ID."),
                              fieldWithPath("vcClusterMobID").description("MobID of Vcenter Cluster."),
                              fieldWithPath("vcInstanceUUID").description("Vcenter's UUID."),
                              fieldWithPath("vroID").description("ID of VROps."),
                              fieldWithPath("vroResourceName")
                                    .description("Resource Name in VROps for this server."),
                              fieldWithPath("vroVMEntityName").description("EntityName of Resource."),
                              fieldWithPath("vroVMEntityObjectID").description("VROps Entity Object ID."),
                              fieldWithPath("vroVMEntityVCID").description("VROps Entity's Vcenter ID."),
                              fieldWithPath("vroResourceID").description("VROps Resource ID."))));
      serverMappingRepository.delete(mapping.getId());
   }

   @Test
   public void readAssetExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      try {
         this.mockMvc.perform(get("/v1/assets/" + asset.getId() + "")).andExpect(status().isOk())
               .andExpect(jsonPath("assetName", is(asset.getAssetName())))
               .andExpect(jsonPath("assetNumber", is((int) asset.getAssetNumber())))
               .andExpect(jsonPath("category", is(asset.getCategory().toString())))
               .andExpect(jsonPath("model", is(asset.getModel())))
               .andExpect(jsonPath("manufacturer", is(asset.getManufacturer())))
               .andDo(document("assets-get-example", responseFields(
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
                     fieldWithPath("justificationfields")
                           .description("Justification fields that input by user."),
                     fieldWithPath("metricsformulars")
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
                     fieldWithPath("status").description(
                           "This is a collection of states, including the state of the asset, "
                                 + "the state of the pdu mapping, and the state of the switch mapping."))));
      } finally {
         assetRepository.delete(asset.getId());
      }
   }

   @Test
   public void getAssetByNameExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      try {
         this.mockMvc.perform(get("/v1/assets/name/" + asset.getAssetName() + ""))
               .andExpect(status().isOk())
               .andExpect(jsonPath("assetName", is(asset.getAssetName())))
               .andExpect(jsonPath("assetNumber", is((int) asset.getAssetNumber())))
               .andExpect(jsonPath("category", is(asset.getCategory().toString())))
               .andExpect(jsonPath("model", is(asset.getModel())))
               .andExpect(jsonPath("manufacturer", is(asset.getManufacturer())))
               .andDo(document("assets-getAssetByName-example", responseFields(
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
                     fieldWithPath("justificationfields")
                           .description("Justification fields that input by user."),
                     fieldWithPath("metricsformulars")
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
                     fieldWithPath("status").description(
                           "This is a collection of states, including the state of the asset, "
                                 + "the state of the pdu mapping, and the state of the switch mapping."))));

      } finally {
         assetRepository.delete(asset.getId());
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
                     fieldWithPath("justificationfields").ignored(),
                     fieldWithPath("metricsformulars").ignored(),
                     fieldWithPath("lastupdate").ignored(), fieldWithPath("created").ignored(),
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
                     fieldWithPath("status").description(
                           "This is a collection of states, including the state of the asset, "
                                 + "the state of the pdu mapping, and the state of the switch mapping."))));

      } finally {
         assetRepository.delete(asset.getId());
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
                     fieldWithPath("vroID").description("ID of VROps."),
                     fieldWithPath("vroResourceName")
                           .description("Resource Name in VROps for this server."),
                     fieldWithPath("vroVMEntityName").description("EntityName of Resource."),
                     fieldWithPath("vroVMEntityObjectID").description("VROps Entity Object ID."),
                     fieldWithPath("vroVMEntityVCID").description("VROps Entity's Vcenter ID."),
                     fieldWithPath("vroResourceID").description("VROps Resource ID."))));

      }finally {
         serverMappingRepository.delete(mapping.getId());
      }
   }

   @Test
   public void deleteServerMappingExample() throws Exception {
      ServerMapping mapping = createServerMapping();
      ServerMapping returnMapping = serverMappingRepository.save(mapping);

      this.mockMvc.perform(delete("/v1/assets/mapping/" + returnMapping.getId()))
            .andExpect(status().isOk()).andDo(document("assets-deleteServerMapping-example"));
   }

   @Test
   public void mergeServerMappingExample() throws Exception {

      ServerMapping mapping1 = createServerMapping();
      serverMappingRepository.save(mapping1);
      ServerMapping mapping2 = createServerMapping();
      serverMappingRepository.save(mapping2);
      MappingIdForDoc mappingId = new MappingIdForDoc();
      mappingId.FirstId = mapping1.getId();
      mappingId.SecondId = mapping2.getId();

      try {
         this.mockMvc
         .perform(put("/v1/assets/mapping/merge/" + mapping1.getId() + "/" + mapping2.getId())
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(mappingId)))
         .andExpect(status().isOk())
         .andDo(document("assets-mergeServerMapping-example",
               requestFields(
                     fieldWithPath("FirstId")
                           .description("ID of the mapping's firstid created by flowgate."),
                     fieldWithPath("SecondId")
                           .description("ID of the mapping's secondid created by flowgate."))));
      }finally {
         serverMappingRepository.delete(mapping1.getId());
      }
   }

   @Test
   public void assetDeleteExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      this.mockMvc
            .perform(delete("/v1/assets/" + asset.getId())
                  .content("{\"id\":\"" + asset.getId() + "\"}"))
            .andExpect(status().isOk()).andDo(document("assets-delete-example",
                  requestFields(fieldWithPath("id").description("The primary key for asset."))));
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
         this.mockMvc.perform(get("/v1/assets/assetnumber/53968/assetname/SHA-pdu1"))
               .andExpect(status().isOk()).andExpect(jsonPath("assetName", is("SHA-pdu1")))
               .andExpect(jsonPath("assetNumber", is(53968)))
               .andDo(document("assets-getAssetByAssetNumberAndName-example", responseFields(
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
                     fieldWithPath("justificationfields")
                           .description("Justification fields that input by user."),
                     fieldWithPath("metricsformulars")
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
                     fieldWithPath("status").description(
                           "This is a collection of states, including the state of the asset, "
                                 + "the state of the pdu mapping, and the state of the switch mapping."))));

      } finally {
         assetRepository.delete(asset.getId());
      }
   }

   @Test
   public void getServerMetricsData() throws Exception {
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("metricName").description("metric name").type(JsonFieldType.STRING),
            fieldWithPath("valueNum").description("valueNum.").type(JsonFieldType.NUMBER),
            fieldWithPath("value").description("value").type(JsonFieldType.NULL),
            fieldWithPath("timeStamp").description("timeStamp").type(JsonFieldType.NUMBER) };
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      RealTimeData pduRealTimeData = createServerPDURealTimeData();
      pduRealTimeData.setAssetID("0001bdc8b25d4c2badfd045ab61aabfa");
      RealTimeData sensorRealTimeData = createSensorRealtimeData();
      sensorRealTimeData.setAssetID("00027ca37b004a9890d1bf20349d5ac1");
      realTimeDatas.add(pduRealTimeData);
      realTimeDatas.add(sensorRealTimeData);
      Iterable<RealTimeData> result = realtimeDataRepository.save(realTimeDatas);

      Asset asset = createAsset();
      Map<String, Map<String, Map<String, String>>> formulars = new HashMap<String, Map<String, Map<String, String>>>();
      Map<String, Map<String, String>> pduMetricFormulars = new HashMap<String, Map<String, String>>();
      Map<String, String> pduMetricAndIdMap = new HashMap<String,String>();
      pduMetricAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_POWER, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_USED_PDU_OUTLET_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricAndIdMap.put(MetricName.SERVER_USED_PDU_OUTLET_POWER, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricFormulars.put("0001bdc8b25d4c2badfd045ab61aabfa", pduMetricAndIdMap);
      formulars.put(FlowgateConstant.PDU, pduMetricFormulars);

      Map<String, Map<String, String>> sensorMetricFormulars = new HashMap<String, Map<String, String>>();
      Map<String, String> frontTempSensor = new HashMap<String,String>();
      frontTempSensor.put("INLET", "00027ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulars.put(MetricName.SERVER_FRONT_TEMPERATURE, frontTempSensor);
      Map<String, String> backTempSensor = new HashMap<String,String>();
      backTempSensor.put("OUTLET", "00027ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulars.put(MetricName.SERVER_BACK_TEMPREATURE, backTempSensor);

      Map<String, String> frontHumiditySensor = new HashMap<String,String>();
      frontHumiditySensor.put("INLET", "00027ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulars.put(MetricName.SERVER_FRONT_HUMIDITY, frontHumiditySensor);

      Map<String, String> backHumiditySensor = new HashMap<String,String>();
      backHumiditySensor.put("OUTLET", "00027ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulars.put(MetricName.SERVER_BACK_HUMIDITY, backHumiditySensor);

      formulars.put(FlowgateConstant.SENSOR, sensorMetricFormulars);
      asset.setMetricsformulars(formulars);
      asset = assetRepository.save(asset);

      MvcResult result1 = this.mockMvc
            .perform(get("/v1/assets/server/" + asset.getId() + "/realtimedata").param("starttime",
                  "1501981711206").param("duration", "300000"))
            .andDo(document("assets-getServerMetricsData-example",
                  responseFields(fieldWithPath("[]").description("An array of realTimeDatas"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn();
      ObjectMapper mapper = new ObjectMapper();
      String res = result1.getResponse().getContentAsString();
      MetricData [] datas = mapper.readValue(res, MetricData[].class);
      try {
         for(MetricData serverdata:datas) {
            String metricName = serverdata.getMetricName();
            if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 20.0);
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_POWER, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 2.38);
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa","OUTLET:1").
                  equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 208.0);
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 196.0);
            }else if(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_POWER, "0001bdc8b25d4c2badfd045ab61aabfa").
                  equals(metricName)) {
               TestCase.assertEquals(serverdata.getValueNum(), 200.0);
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
               TestCase.assertEquals(serverdata.getValueNum(), 208.0);
            }else {
               TestCase.fail();
            }
          }
      }finally {
         assetRepository.delete(asset);
         realtimeDataRepository.delete(result);
      }
   }

   @Test
   public void getPduMetricsDataById() throws Exception {
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("metricName").description("metric name").type(JsonFieldType.STRING),
            fieldWithPath("valueNum").description("valueNum.").type(JsonFieldType.NUMBER),
            fieldWithPath("value").description("value").type(JsonFieldType.NULL),
            fieldWithPath("timeStamp").description("timeStamp").type(JsonFieldType.NUMBER) };
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      RealTimeData pduRealTimeData = createPduRealTimeData();
      pduRealTimeData.setAssetID("00040717c4154b5b924ced78eafcea7a");

      RealTimeData sensorRealTimeData = createSensorRealtimeData();
      sensorRealTimeData.setAssetID("00027ca37b004a9890d1bf20349d5ac1");
      realTimeDatas.add(pduRealTimeData);
      realTimeDatas.add(sensorRealTimeData);
      Iterable<RealTimeData> result = realtimeDataRepository.save(realTimeDatas);

      Asset asset = createAsset();
      Map<String, Map<String, Map<String, String>>> formulars = new HashMap<String, Map<String, Map<String, String>>>();
      Map<String, Map<String, String>> pduMetricFormulars = new HashMap<String, Map<String, String>>();
      Map<String, String> pduMetricAndIdMap = new HashMap<String,String>();
      pduMetricAndIdMap.put(MetricName.PDU_ACTIVE_POWER, "00040717c4154b5b924ced78eafcea7a");
      pduMetricAndIdMap.put(MetricName.PDU_APPARENT_POWER, "00040717c4154b5b924ced78eafcea7a");
      pduMetricAndIdMap.put(MetricName.PDU_CURRENT, "00040717c4154b5b924ced78eafcea7a");
      pduMetricAndIdMap.put(MetricName.PDU_CURRENT_LOAD, "00040717c4154b5b924ced78eafcea7a");
      pduMetricAndIdMap.put(MetricName.PDU_FREE_CAPACITY, "00040717c4154b5b924ced78eafcea7a");
      pduMetricAndIdMap.put(MetricName.PDU_POWER_LOAD, "00040717c4154b5b924ced78eafcea7a");
      pduMetricAndIdMap.put(MetricName.PDU_TOTAL_CURRENT, "00040717c4154b5b924ced78eafcea7a");
      pduMetricAndIdMap.put(MetricName.PDU_TOTAL_POWER, "00040717c4154b5b924ced78eafcea7a");
      pduMetricAndIdMap.put(MetricName.PDU_VOLTAGE, "00040717c4154b5b924ced78eafcea7a");
      pduMetricFormulars.put("00040717c4154b5b924ced78eafcea7a", pduMetricAndIdMap);
      formulars.put(FlowgateConstant.PDU, pduMetricFormulars);

      Map<String, Map<String, String>> sensorMetricFormulars = new HashMap<String, Map<String, String>>();
      Map<String, String> tempSensor = new HashMap<String,String>();
      tempSensor.put("INLET", "00027ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulars.put(MetricName.PDU_TEMPERATURE, tempSensor);

      Map<String, String> humiditySensor = new HashMap<String,String>();
      humiditySensor.put("OUTLET", "00027ca37b004a9890d1bf20349d5ac1");
      sensorMetricFormulars.put(MetricName.PDU_HUMIDITY, humiditySensor);

      formulars.put(FlowgateConstant.SENSOR, sensorMetricFormulars);
      asset.setMetricsformulars(formulars);
      asset.setId("00040717c4154b5b924ced78eafcea7a");
      asset = assetRepository.save(asset);

      MvcResult result1 = this.mockMvc
            .perform(get("/v1/assets/pdu/" + asset.getId() + "/realtimedata").param("starttime",
                  "1501981711206").param("duration", "300000"))
            .andDo(document("assets-getPduMetricsData-example",
                  responseFields(fieldWithPath("[]").description("An array of realTimeDatas"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn();
      ObjectMapper mapper = new ObjectMapper();
      String res = result1.getResponse().getContentAsString();
      MetricData [] datas = mapper.readValue(res, MetricData[].class);
      try {
         for(MetricData pduMetricdata:datas) {
            String metricName = pduMetricdata.getMetricName();
            if(String.format(MetricKeyName.PDU_XLET_ACTIVE_POWER,"OUTLET:1").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 2.0);
            }else if(String.format(MetricKeyName.PDU_XLET_APPARENT_POWER,"OUTLET:1").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 2.38);
            }else if(String.format(MetricKeyName.PDU_XLET_CURRENT,"OUTLET:1").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 20.0);
            }else if(String.format(MetricKeyName.PDU_XLET_FREE_CAPACITY, "OUTLET:1").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 20.0);
            }else if(String.format(MetricKeyName.PDU_XLET_VOLTAGE, "OUTLET:1").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 208.0);
            }else if(String.format(MetricKeyName.PDU_HUMIDITY_LOCATIONX, "OUTLET").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 20.0);
            }else if(String.format(MetricKeyName.PDU_TEMPERATURE_LOCATIONX, "INLET").
                  equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 32.0);
            }else if(MetricName.PDU_CURRENT_LOAD.equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 20.0);
            }else if(MetricName.PDU_POWER_LOAD.equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 20.0);
            }else if(MetricName.PDU_TOTAL_CURRENT.equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 196.0);
            }else if(MetricName.PDU_TOTAL_POWER.equals(metricName)) {
               TestCase.assertEquals(pduMetricdata.getValueNum(), 200.0);
            }else {
               TestCase.fail();
            }
          }
      }finally {
         assetRepository.delete(asset);
         realtimeDataRepository.delete(result);
      }
   }

   RealTimeData createPduRealTimeData() {
      RealTimeData realTimeData = createServerPDURealTimeData();
      List<ValueUnit> valueunits = realTimeData.getValues();

      ValueUnit valueunitActivePower = new ValueUnit();
      valueunitActivePower.setKey(MetricName.PDU_ACTIVE_POWER);
      valueunitActivePower.setUnit("W");
      valueunitActivePower.setExtraidentifier("OUTLET:1");
      valueunitActivePower.setValueNum(2);
      valueunitActivePower.setTime(1501981711206L);
      valueunits.add(valueunitActivePower);

      ValueUnit valueunitFreeCapacity = new ValueUnit();
      valueunitFreeCapacity.setKey(MetricName.PDU_FREE_CAPACITY);
      valueunitFreeCapacity.setUnit("Amps");
      valueunitFreeCapacity.setExtraidentifier("OUTLET:1");
      valueunitFreeCapacity.setValueNum(20);
      valueunitFreeCapacity.setTime(1501981711206L);
      valueunits.add(valueunitFreeCapacity);

      ValueUnit valueunitCurrentLoad = new ValueUnit();
      valueunitCurrentLoad.setKey(MetricName.PDU_CURRENT_LOAD);
      valueunitCurrentLoad.setUnit("%");
      valueunitCurrentLoad.setExtraidentifier("OUTLET:1");
      valueunitCurrentLoad.setValueNum(20);
      valueunitCurrentLoad.setTime(1501981711206L);
      valueunits.add(valueunitCurrentLoad);

      ValueUnit valueunitPowerLoad = new ValueUnit();
      valueunitPowerLoad.setKey(MetricName.PDU_POWER_LOAD);
      valueunitPowerLoad.setUnit("%");
      valueunitPowerLoad.setExtraidentifier("OUTLET:1");
      valueunitPowerLoad.setValueNum(20);
      valueunitPowerLoad.setTime(1501981711206L);
      valueunits.add(valueunitPowerLoad);

      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setValues(valueunits);
      realTimeData.setTime(System.currentTimeMillis() - 1000);
      return realTimeData;
   }

   RealTimeData createServerPDURealTimeData() {
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();
      ValueUnit valueunitvoltage = new ValueUnit();
      valueunitvoltage.setKey(MetricName.PDU_VOLTAGE);
      valueunitvoltage.setUnit("Volts");
      valueunitvoltage.setExtraidentifier("OUTLET:1");
      valueunitvoltage.setValueNum(208);
      valueunitvoltage.setTime(1501981711206L);
      valueunits.add(valueunitvoltage);
      ValueUnit valueunitpower = new ValueUnit();
      valueunitpower.setKey(MetricName.PDU_APPARENT_POWER);
      valueunitpower.setUnit("W");
      valueunitpower.setExtraidentifier("OUTLET:1");
      valueunitpower.setValueNum(2.38);
      valueunitpower.setTime(1501981711206L);
      valueunits.add(valueunitpower);
      ValueUnit valueunitCurrent = new ValueUnit();
      valueunitCurrent.setExtraidentifier("OUTLET:1");
      valueunitCurrent.setKey(MetricName.PDU_CURRENT);
      valueunitCurrent.setUnit("Amps");
      valueunitCurrent.setValueNum(20);
      valueunitCurrent.setTime(1501981711206L);
      valueunits.add(valueunitCurrent);

      ValueUnit valueunitTotalCurrent = new ValueUnit();
      valueunitTotalCurrent.setKey(MetricName.PDU_TOTAL_CURRENT);
      valueunitTotalCurrent.setUnit("Amps");
      valueunitTotalCurrent.setValueNum(196);
      valueunitTotalCurrent.setTime(1501981711206L);
      valueunits.add(valueunitTotalCurrent);

      ValueUnit valueunitTotalPower = new ValueUnit();
      valueunitTotalPower.setExtraidentifier("OUTLET:1");
      valueunitTotalPower.setKey(MetricName.PDU_TOTAL_POWER);
      valueunitTotalPower.setUnit("W");
      valueunitTotalPower.setValueNum(200);
      valueunitTotalPower.setTime(1501981711206L);
      valueunits.add(valueunitTotalPower);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID("0001bdc8b25d4c2badfd045ab61aabfa");
      realTimeData.setValues(valueunits);
      realTimeData.setTime(valueunits.get(0).getTime());
      return realTimeData;
   }

   RealTimeData createSensorRealtimeData() {
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();

      ValueUnit tempValue = new ValueUnit();
      tempValue.setValueNum(32);
      tempValue.setTime(1501981711206L);
      tempValue.setKey(MetricName.TEMPERATURE);
      valueunits.add(tempValue);

      ValueUnit humidityValue = new ValueUnit();
      humidityValue.setValueNum(20);
      humidityValue.setTime(1501981711206L);
      humidityValue.setKey(MetricName.HUMIDITY);
      valueunits.add(humidityValue);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setId(UUID.randomUUID().toString());
      realTimeData.setAssetID("00027ca37b004a9890d1bf20349d5ac1");
      realTimeData.setValues(valueunits);
      realTimeData.setTime(valueunits.get(0).getTime());
      return realTimeData;
   }

   AssetIPMapping createAssetIPMapping() {
      AssetIPMapping assetipmapping = new AssetIPMapping();
      assetipmapping.setId(UUID.randomUUID().toString());
      assetipmapping.setAssetname("assetname");
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
      Map<String, Map<String, Map<String, String>>> formulars = new HashMap<String, Map<String, Map<String, String>>>();
      Map<String, Map<String, String>> pduMetricFormulars = new HashMap<String, Map<String, String>>();
      Map<String, String> MetricAndIdMap = new HashMap<String,String>();
      MetricAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_CURRENT, "0001bdc8b25d4c2badfd045ab61aabfa");
      MetricAndIdMap.put(MetricName.SERVER_CONNECTED_PDU_POWER, "0001bdc8b25d4c2badfd045ab61aabfa");
      MetricAndIdMap.put(MetricName.SERVER_VOLTAGE, "0001bdc8b25d4c2badfd045ab61aabfa");
      pduMetricFormulars.put("0001bdc8b25d4c2badfd045ab61aabfa", MetricAndIdMap);
      formulars.put(FlowgateConstant.PDU, pduMetricFormulars);
      asset.setMetricsformulars(formulars);
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
}
