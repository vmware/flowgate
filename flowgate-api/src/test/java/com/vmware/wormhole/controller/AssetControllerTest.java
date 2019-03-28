/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.controller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.MountingSide;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetAddress;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.AssetRealtimeDataSpec;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.ServerSensorData;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ServerSensorData.ServerSensorType;
import com.vmware.flowgate.common.model.ValueUnit.ValueType;
import com.vmware.flowgate.repository.AssetIPMappingRepository;
import com.vmware.flowgate.repository.AssetRealtimeDataRepository;
import com.vmware.flowgate.repository.AssetRepository;
import com.vmware.flowgate.repository.ServerMappingRepository;

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
   private WebApplicationContext context;

   @Autowired
   ServerMappingRepository serverMappingRepository;

   @Autowired
   AssetIPMappingRepository assetIPMappingRepository;

   @Before
   public void setUp() {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation)).build();
   }

   @Test
   public void createAnAssetExample() throws JsonProcessingException, Exception {
      Asset asset = createAsset();
      asset.setId("temporary_id");
      this.mockMvc
            .perform(post("/v1/assets").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(asset)))
            .andExpect(status().isCreated()).andExpect(header().string("Location", notNullValue()))
            .andDo(document("assets-create-example", requestFields(
                  fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
                  fieldWithPath("room").description(""),
                  fieldWithPath("row").description("").optional(),
                  fieldWithPath("col").description("").optional(),
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
                  fieldWithPath("cabinetsize")
                        .description("The cabinet size(only for cabinet type)").type(int.class)
                        .optional(),
                  fieldWithPath("cabinetAssetNumber").description(
                        "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                        .type(long.class).optional(),
                  fieldWithPath("assetRealtimeDataSpec")
                        .description("Only valid for sensor type of asset.")
                        .type(AssetRealtimeDataSpec.class).optional(),
                  fieldWithPath("justificationfields").ignored(),
                  fieldWithPath("sensorsformulars").ignored(),
                  fieldWithPath("lastupdate").ignored(), fieldWithPath("created").ignored(),
                  fieldWithPath("pdus")
                        .description("Possible PDUs that this server connected with"),
                  fieldWithPath("switches")
                        .description("Physical switchs that this host connected with"),
                  fieldWithPath("status")
                        .description("This is a collection of states, including the state of the asset, "
                              + "the state of the pdu mapping, and the state of the switch mapping."))))
            .andReturn().getResponse().getHeader("Location");
      assetRepository.delete("temporary_id");
   }

   @Test
   public void saveServerMappingExample() throws JsonProcessingException, Exception {
      ServerMapping mapping = createServerMapping();
      mapping.setId("temporary_id");
      this.mockMvc
            .perform(post("/v1/assets/mapping").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(mapping)))
            .andExpect(status().isCreated())
            .andDo(document("assets-saveServerMapping-example",
                  requestFields(
                        fieldWithPath("id").description("ID of the mapping, created by wormhole"),
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
                        fieldWithPath("vroResourceID").description("VROps Resource ID."))))
            .andReturn().getResponse().getHeader("Location");

      serverMappingRepository.delete("temporary_id");
   }

   @Test
   public void insertRealtimeDataExample() throws JsonProcessingException, Exception {

      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      RealTimeData realtime = new RealTimeData();
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
                  fieldWithPath("id").description("ID of the realtime, created by wormhole"),
                  fieldWithPath("assetID").description("ID of the asset, created by wormhole"),
                  fieldWithPath("values")
                        .description("A list of sensor data. eg. Humidity , Electric... ")
                        .type(ValueUnit[].class),
                  fieldWithPath("time").description("The time of generate sensor data."))))
            .andReturn().getResponse().getHeader("Location");

      assetRepository.delete(asset.getId());
      realtimeDataRepository.delete(realtime.getId());

   }

   @Test
   public void createAssetBatchExample() throws JsonProcessingException, Exception {
      List<Asset> assets = new ArrayList<Asset>();
      Asset asset1 = createAsset();
      asset1.setAssetName("lhy");
      asset1.setAssetNumber(18);
      asset1.setId("temporary_id1");
      assets.add(asset1);
      Asset asset2 = createAsset();
      asset2.setAssetName("lwy");
      asset2.setAssetNumber(17);
      asset2.setId("temporary_id2");
      assets.add(asset2);

      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
            fieldWithPath("room").description(""), fieldWithPath("row").description("").optional(),
            fieldWithPath("col").description("").optional(),
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
            fieldWithPath("cabinetsize").description("The cabinet size(only for cabinet type)")
                  .type(int.class).optional(),
            fieldWithPath("cabinetAssetNumber").description(
                  "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                  .type(long.class).optional(),
            fieldWithPath("assetRealtimeDataSpec")
                  .description("Only valid for sensor type of asset.")
                  .type(AssetRealtimeDataSpec.class).optional(),
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("sensorsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };
      this.mockMvc
            .perform(post("/v1/assets/batchoperation").contentType(MediaType.APPLICATION_JSON_VALUE)
                  .content(objectMapper.writeValueAsString(assets)))
            .andExpect(status().isCreated())
            .andDo(document("assets-createBatch-example",
                  requestFields(fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      assetRepository.delete("temporary_id1");
      assetRepository.delete("temporary_id2");
   }

   @Test
   public void realTimeDatabatchCreationExample() throws JsonProcessingException, Exception {
      List<RealTimeData> realtimedatas = new ArrayList<RealTimeData>();
      RealTimeData realtimedata1 = createRealTimeData();
      realtimedata1.setAssetID("assetid1");
      realtimedata1.setId("temporary_id1");
      realtimedatas.add(realtimedata1);
      RealTimeData realtimedata2 = createRealTimeData();
      realtimedata2.setAssetID("assetid2");
      realtimedata2.setId("temporary_id2");
      realtimedatas.add(realtimedata2);

      FieldDescriptor[] fieldpath =
            new FieldDescriptor[] { fieldWithPath("id").description("ID of the RealTimeData"),
                  fieldWithPath("assetID").description("ID of the asset, created by wormhole"),
                  fieldWithPath("values").description("List of ValueUnit") };
      this.mockMvc
            .perform(post("/v1/assets/sensordata/batchoperation")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .content(objectMapper.writeValueAsString(realtimedatas)))
            .andExpect(status().isCreated())
            .andDo(document("assets-realTimeDatabatchCreation-example",
                  requestFields(fieldWithPath("[]").description("An array of RealTimeData"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      realtimeDataRepository.delete("temporary_id1");
      realtimeDataRepository.delete("temporary_id2");
   }

   @Test
   public void readAssetBySourceAndTypeExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
            fieldWithPath("room").description(""), fieldWithPath("row").description("").optional(),
            fieldWithPath("col").description("").optional(),
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
            fieldWithPath("cabinetsize").description("The cabinet size(only for cabinet type)")
                  .type(int.class).optional(),
            fieldWithPath("cabinetAssetNumber").description(
                  "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                  .type(long.class).optional(),
            fieldWithPath("assetRealtimeDataSpec")
                  .description("Only valid for sensor type of asset.")
                  .type(AssetRealtimeDataSpec.class).optional(),
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("sensorsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };

      this.mockMvc
            .perform(get(
                  "/v1/assets/source/" + asset.getAssetSource() + "/type/" + asset.getCategory()))
            .andExpect(status().isOk())
            .andDo(document("assets-getBySourceAndType-example",
                  responseFields(fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      assetRepository.delete(asset.getId());
   }

   @Test
   public void readAssetByTypeExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
            fieldWithPath("room").description(""), fieldWithPath("row").description("").optional(),
            fieldWithPath("col").description("").optional(),
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
            fieldWithPath("cabinetsize").description("The cabinet size(only for cabinet type)")
                  .type(int.class).optional(),
            fieldWithPath("cabinetAssetNumber").description(
                  "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                  .type(long.class).optional(),
            fieldWithPath("assetRealtimeDataSpec")
                  .description("Only valid for sensor type of asset.")
                  .type(AssetRealtimeDataSpec.class).optional(),
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("sensorsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };

      this.mockMvc.perform(get("/v1/assets/type/" + asset.getCategory())).andExpect(status().isOk())
            .andDo(document("assets-getByType-example",
                  responseFields(fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      assetRepository.delete(asset.getId());
   }

   @Test
   public void readAssetByVROPSIdExample() throws Exception {
      Asset asset = createAsset();
      ServerMapping mapping = createServerMapping();
      asset = assetRepository.save(asset);
      mapping.setAsset(asset.getId());
      mapping = serverMappingRepository.save(mapping);

      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
            fieldWithPath("room").description(""), fieldWithPath("row").description("").optional(),
            fieldWithPath("col").description("").optional(),
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
            fieldWithPath("cabinetsize").description("The cabinet size(only for cabinet type)")
                  .type(int.class).optional(),
            fieldWithPath("cabinetAssetNumber").description(
                  "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                  .type(long.class).optional(),
            fieldWithPath("assetRealtimeDataSpec")
                  .description("Only valid for sensor type of asset.")
                  .type(AssetRealtimeDataSpec.class).optional(),
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("sensorsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };

      this.mockMvc.perform(get("/v1/assets/vrops/" + mapping.getVroID())).andExpect(status().isOk())
            .andDo(document("assets-getAssetsByVROPSId-example",
                  responseFields(fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      assetRepository.delete(asset.getId());
      serverMappingRepository.delete(mapping.getId());
   }

   @Test
   public void getMappingsByVropsIdExample() throws Exception {

      ServerMapping mapping = createServerMapping();
      mapping.setVroID("1");
      serverMappingRepository.save(mapping);

      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the mapping, created by wormhole"),
            fieldWithPath("asset").description("An asset for serverMapping."),
            fieldWithPath("vcID").description("ID of Vcenter."),
            fieldWithPath("vcHostName").description("Server's hostname display in Vcenter."),
            fieldWithPath("vcMobID").description("EXSI server's management object ID."),
            fieldWithPath("vcClusterMobID").description("MobID of Vcenter Cluster."),
            fieldWithPath("vcInstanceUUID").description("Vcenter's UUID."),
            fieldWithPath("vroID").description("ID of VROps."),
            fieldWithPath("vroResourceName").description("Resource Name in VROps for this server."),
            fieldWithPath("vroVMEntityName").description("EntityName of Resource."),
            fieldWithPath("vroVMEntityObjectID").description("VROps Entity Object ID."),
            fieldWithPath("vroVMEntityVCID").description("VROps Entity's Vcenter ID."),
            fieldWithPath("vroResourceID").description("VROps Resource ID.") };

      this.mockMvc.perform(get("/v1/assets/mapping/vrops/" + mapping.getVroID()))
            .andExpect(status().isOk())
            .andDo(document("assets-getMappingsByVropsId-example",
                  responseFields(fieldWithPath("[]").description("An array of ServerMappings"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      serverMappingRepository.delete(mapping.getId());
   }

   @Test
   public void getMappingsByVCIdExample() throws Exception {

      ServerMapping mapping = createServerMapping();
      mapping.setVroID("1");
      mapping.setVcID("2");
      serverMappingRepository.save(mapping);

      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the mapping, created by wormhole"),
            fieldWithPath("asset").description("An asset for serverMapping."),
            fieldWithPath("vcID").description("ID of Vcenter."),
            fieldWithPath("vcHostName").description("Server's hostname display in Vcenter."),
            fieldWithPath("vcMobID").description("EXSI server's management object ID."),
            fieldWithPath("vcClusterMobID").description("MobID of Vcenter Cluster."),
            fieldWithPath("vcInstanceUUID").description("Vcenter's UUID."),
            fieldWithPath("vroID").description("ID of VROps."),
            fieldWithPath("vroResourceName").description("Resource Name in VROps for this server."),
            fieldWithPath("vroVMEntityName").description("EntityName of Resource."),
            fieldWithPath("vroVMEntityObjectID").description("VROps Entity Object ID."),
            fieldWithPath("vroVMEntityVCID").description("VROps Entity's Vcenter ID."),
            fieldWithPath("vroResourceID").description("VROps Resource ID.") };

      this.mockMvc.perform(get("/v1/assets/mapping/vc/" + mapping.getVcID()))
            .andExpect(status().isOk())
            .andDo(document("assets-getMappingsByVCId-example",
                  responseFields(fieldWithPath("[]").description("An array of ServerMappings"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      serverMappingRepository.delete(mapping.getId());
   }

   @Test
   public void getHostNameByIPExample() throws Exception {

      AssetIPMapping mapping1 = createAssetIPMapping();
      mapping1.setId("temporary_id1");
      assetIPMappingRepository.save(mapping1);
      AssetIPMapping mapping2 = createAssetIPMapping();
      mapping2.setId("temporary_id2");
      assetIPMappingRepository.save(mapping2);

      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the AssetIPMapping, created by wormhole"),
            fieldWithPath("ip").description("IP of AssetIPMapping."),
            fieldWithPath("assetname").description("name of asset.") };
      this.mockMvc.perform(get("/v1/assets/mapping/hostnameip/ip/" + mapping1.getIp()))
            .andExpect(status().isOk())
            .andDo(document("assets-getHostNameByIP-example",
                  responseFields(fieldWithPath("[]").description("An array of ServerMappings"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      assetIPMappingRepository.delete(mapping1.getId());
      assetIPMappingRepository.delete(mapping2.getId());
   }

   @Test
   public void getUnmappedServersExample() throws Exception {

      ServerMapping mapping1 = createServerMapping();
      mapping1.setId("temporary_id1");
      serverMappingRepository.save(mapping1);
      ServerMapping mapping2 = createServerMapping();
      mapping2.setId("temporary_id2");
      serverMappingRepository.save(mapping2);

      FieldDescriptor[] fieldpath =
            new FieldDescriptor[] { fieldWithPath("").description("hostname") };

      this.mockMvc.perform(get("/v1/assets/mapping/unmappedservers")).andExpect(status().isOk())
            .andDo(document("assets-getUnmappedServers-example",
                  responseFields(fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      serverMappingRepository.delete(mapping1.getId());
      serverMappingRepository.delete(mapping2.getId());
   }

   @Test
   public void readMappedAssetExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      ServerMapping mapping = new ServerMapping();
      mapping.setAsset(asset.getId());
      mapping.setVcID("5b7cfd5655368548d42e0fd5");
      mapping.setVcHostName("10.192.74.203");
      mapping.setVcMobID("host-11");
      serverMappingRepository.save(mapping);
      Asset asset2 = createAsset();
      asset2 = assetRepository.save(asset2);
      ServerMapping mapping2 = new ServerMapping();
      mapping2.setAsset(asset2.getId());
      mapping2.setVcID("5b7cfd5655368548d42e0fd6");
      mapping2.setVcHostName("10.192.74.203");
      mapping2.setVcMobID("host-11");
      serverMappingRepository.save(mapping2);

      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
            fieldWithPath("room").description(""), fieldWithPath("row").description("").optional(),
            fieldWithPath("col").description("").optional(),
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
            fieldWithPath("cabinetsize").description("The cabinet size(only for cabinet type)")
                  .type(int.class).optional(),
            fieldWithPath("cabinetAssetNumber").description(
                  "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                  .type(long.class).optional(),
            fieldWithPath("assetRealtimeDataSpec")
                  .description("Only valid for sensor type of asset.")
                  .type(AssetRealtimeDataSpec.class).optional(),
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("sensorsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };
      this.mockMvc.perform(get("/v1/assets/mappedasset/category/" + asset.getCategory()))
            .andDo(document("assets-getMapped-example",
                  responseFields(fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      assetRepository.delete(asset.getId());
      serverMappingRepository.delete(mapping.getId());
      assetRepository.delete(asset2.getId());
      serverMappingRepository.delete(mapping2.getId());
   }

   @Test
   public void getAssetsByVCIdExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      ServerMapping mapping = new ServerMapping();
      mapping.setAsset(asset.getId());
      mapping.setVcID("5b7cfd5655368548d42e0fd5");
      mapping.setVcHostName("10.192.74.203");
      mapping.setVcMobID("host-11");
      serverMappingRepository.save(mapping);
      Asset asset2 = createAsset();
      asset2 = assetRepository.save(asset2);
      ServerMapping mapping2 = new ServerMapping();
      mapping2.setAsset(asset2.getId());
      mapping2.setVcID("5b7cfd5655368548d42e0fd6");
      mapping2.setVcHostName("10.192.74.203");
      mapping2.setVcMobID("host-11");
      serverMappingRepository.save(mapping2);

      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
            fieldWithPath("room").description(""), fieldWithPath("row").description("").optional(),
            fieldWithPath("col").description("").optional(),
            fieldWithPath("extraLocation")
                  .description("Extra location information. Only valid for some system.")
                  .optional(),
            fieldWithPath("cabinetName").description(
                  "The cabinet name where this asset is located. If the asset is cabinet then this filed is empty.")
                  .optional(),
            fieldWithPath("cabinetUnitPosition").description("The cabinet unit number")
                  .type(int.class).optional(),
            fieldWithPath("cabinetsize").description("The cabinet size(only for cabinet type)")
                  .type(int.class).optional(),
            fieldWithPath("cabinetAssetNumber").description(
                  "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                  .type(long.class).optional(),
            fieldWithPath("assetRealtimeDataSpec")
                  .description("Only valid for sensor type of asset.")
                  .type(AssetRealtimeDataSpec.class).optional(),
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("sensorsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };
      this.mockMvc.perform(get("/v1/assets/vc/" + mapping.getVcID()))
            .andDo(document("assets-getAssetsByVCId-example",
                  responseFields(fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      assetRepository.delete(asset.getId());
      serverMappingRepository.delete(mapping.getId());
      assetRepository.delete(asset2.getId());
      serverMappingRepository.delete(mapping2.getId());
   }

   @Test
   public void readAssetsByAssetNameAndTagLikExample() throws Exception {
      Asset asset1 = createAsset();
      asset1.setAssetName("lhy");
      asset1.setAssetNumber(18);
      asset1.setAssetSource(null);
      assetRepository.save(asset1);
      Asset asset2 = createAsset();
      asset2.setAssetName("lwy");
      asset2.setAssetNumber(17);
      asset2.setAssetSource(null);
      assetRepository.save(asset2);
      int pageNumber = 1;
      int pageSize = 1;

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

      assetRepository.delete(asset1.getId());
      assetRepository.delete(asset2.getId());

   }

   @Test
   public void readAssetsByAssetNameAndTagLikAndKeywordsExample() throws Exception {
      Asset asset1 = createAsset();
      asset1.setAssetName("lhy");
      asset1.setAssetNumber(18);
      assetRepository.save(asset1);
      Asset asset2 = createAsset();
      asset2.setAssetName("lwy");
      asset2.setAssetNumber(17);
      assetRepository.save(asset2);
      int pageNumber = 1;
      int pageSize = 1;
      String keywords = "1";

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

      assetRepository.delete(asset1.getId());
      assetRepository.delete(asset2.getId());

   }

   @Test
   public void findServersWithoutPDUInfoExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      ServerMapping mapping = new ServerMapping();
      mapping.setAsset(asset.getId());
      mapping.setVcID("5b7cfd5655368548d42e0fd5");
      mapping.setVcHostName("10.192.74.203");
      mapping.setVcMobID("host-11");
      serverMappingRepository.save(mapping);
      Asset asset2 = createAsset();

      asset2 = assetRepository.save(asset2);
      ServerMapping mapping2 = new ServerMapping();
      mapping2.setAsset(asset2.getId());
      mapping2.setVcID("5b7cfd5655368548d42e0fd6");
      mapping2.setVcHostName("10.192.74.203");
      mapping2.setVcMobID("host-11");
      serverMappingRepository.save(mapping2);

      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
            fieldWithPath("room").description(""), fieldWithPath("row").description("").optional(),
            fieldWithPath("col").description("").optional(),
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
            fieldWithPath("cabinetsize").description("The cabinet size(only for cabinet type)")
                  .type(int.class).optional(),
            fieldWithPath("cabinetAssetNumber").description(
                  "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                  .type(long.class).optional(),
            fieldWithPath("assetRealtimeDataSpec")
                  .description("Only valid for sensor type of asset.")
                  .type(AssetRealtimeDataSpec.class).optional(),
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("sensorsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };
      this.mockMvc.perform(get("/v1/assets/pdusisnull"))
            .andDo(document("assets-findServersWithoutPDUInfo-example",
                  responseFields(fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse().getHeader("Location");

      assetRepository.delete(asset.getId());
      serverMappingRepository.delete(mapping.getId());
      assetRepository.delete(asset2.getId());
      serverMappingRepository.delete(mapping2.getId());

   }

   @Test
   public void findServersWithPDUInfoExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      ServerMapping mapping = new ServerMapping();
      mapping.setAsset(asset.getId());
      mapping.setVcID("5b7cfd5655368548d42e0fd5");
      mapping.setVcHostName("10.192.74.203");
      mapping.setVcMobID("host-11");
      serverMappingRepository.save(mapping);
      Asset asset2 = createAsset();
      asset2.setPdus(Arrays.asList("pdu1", "pdu2"));
      asset2 = assetRepository.save(asset2);
      ServerMapping mapping2 = new ServerMapping();
      mapping2.setAsset(asset2.getId());
      mapping2.setVcID("5b7cfd5655368548d42e0fd6");
      mapping2.setVcHostName("10.192.74.203");
      mapping2.setVcMobID("host-11");
      serverMappingRepository.save(mapping2);
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
            fieldWithPath("room").description(""), fieldWithPath("row").description("").optional(),
            fieldWithPath("col").description("").optional(),
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
            fieldWithPath("cabinetsize").description("The cabinet size(only for cabinet type)")
                  .type(int.class).optional(),
            fieldWithPath("cabinetAssetNumber").description(
                  "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                  .type(long.class).optional(),
            fieldWithPath("assetRealtimeDataSpec")
                  .description("Only valid for sensor type of asset.")
                  .type(AssetRealtimeDataSpec.class).optional(),
            fieldWithPath("justificationfields")
                  .description("Justification fields that input by user."),
            fieldWithPath("sensorsformulars")
                  .description("The sensor data generator logic for this asset."),
            fieldWithPath("lastupdate").description("When this asset was last upated"),
            fieldWithPath("created").description("When this asset was created"),
            fieldWithPath("pdus").description("Possible PDUs that this server connected with"),
            fieldWithPath("switches")
                  .description("Physical switchs that this host connected with") };
      this.mockMvc.perform(get("/v1/assets/pdusisnotnull"))
            .andExpect(jsonPath("$[0].assetNumber", is(12345)))
            .andExpect(jsonPath("$[0].assetName", is("pek-wor-server-02")))
            .andExpect(jsonPath("$[0].pdus", hasSize(2)))
            .andExpect(jsonPath("$[0].pdus[0]", is("pdu1")))
            .andDo(document("assets-findServersWithPDUInfo-example",
                  responseFields(fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)))
            .andReturn().getResponse();

      assetRepository.delete(asset.getId());
      serverMappingRepository.delete(mapping.getId());
      assetRepository.delete(asset2.getId());
      serverMappingRepository.delete(mapping2.getId());

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


      serverMappingRepository.delete(mapping1.getId());
      serverMappingRepository.delete(mapping2.getId());
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
      int vropsID = 1;
      int vcID = 1;

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


      serverMappingRepository.delete(mapping1.getId());
      serverMappingRepository.delete(mapping2.getId());
   }

   @Test
   public void createHostNameIPMappingExample() throws Exception {
      AssetIPMapping assetipmapping = createAssetIPMapping();
      assetipmapping.setId("temporary_id");
      this.mockMvc
            .perform(post("/v1/assets/mapping/hostnameip").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(assetipmapping)))
            .andExpect(status().isCreated())
            .andDo(document("assets-createHostNameIPMapping-example", requestFields(
                  fieldWithPath("id").description("ID of the asset, created by wormhole"),
                  fieldWithPath("ip").description("ip of hostname"),
                  fieldWithPath("assetname").description(
                        "The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique identifier of an asset"))))
            .andReturn().getResponse().getHeader("Location");

      assetRepository.delete("temporary_id");
   }

   @Test
   public void readAssetExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      this.mockMvc.perform(get("/v1/assets/" + asset.getId() + "")).andExpect(status().isOk())
            .andExpect(jsonPath("assetName", is(asset.getAssetName())))
            .andExpect(jsonPath("assetNumber", is((int) asset.getAssetNumber())))
            .andExpect(jsonPath("category", is(asset.getCategory().toString())))
            .andExpect(jsonPath("model", is(asset.getModel())))
            .andExpect(jsonPath("manufacturer", is(asset.getManufacturer())))
            .andDo(document("assets-get-example", responseFields(
                  fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
                  fieldWithPath("room").description(""),
                  fieldWithPath("row").description("").optional(),
                  fieldWithPath("col").description("").optional(),
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
                  fieldWithPath("cabinetsize")
                        .description("The cabinet size(only for cabinet type)").type(int.class)
                        .optional(),
                  fieldWithPath("cabinetAssetNumber").description(
                        "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                        .type(long.class).optional(),
                  fieldWithPath("assetRealtimeDataSpec")
                        .description("Only valid for sensor type of asset.")
                        .type(AssetRealtimeDataSpec.class).optional(),
                  fieldWithPath("justificationfields")
                        .description("Justification fields that input by user."),
                  fieldWithPath("sensorsformulars")
                        .description("The sensor data generator logic for this asset."),
                  fieldWithPath("lastupdate").description("When this asset was last upated"),
                  fieldWithPath("created").description("When this asset was created"),
                  fieldWithPath("pdus")
                        .description("Possible PDUs that this server connected with"),
                  fieldWithPath("switches")
                        .description("Physical switchs that this host connected with"),
                  fieldWithPath("status")
                        .description("This is a collection of states, including the state of the asset, "
                              + "the state of the pdu mapping, and the state of the switch mapping."))));

      assetRepository.delete(asset.getId());
   }

   @Test
   public void getAssetByNameExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      this.mockMvc.perform(get("/v1/assets/name/" + asset.getAssetName() + ""))
            .andExpect(status().isOk()).andExpect(jsonPath("assetName", is(asset.getAssetName())))
            .andExpect(jsonPath("assetNumber", is((int) asset.getAssetNumber())))
            .andExpect(jsonPath("category", is(asset.getCategory().toString())))
            .andExpect(jsonPath("model", is(asset.getModel())))
            .andExpect(jsonPath("manufacturer", is(asset.getManufacturer())))
            .andDo(document("assets-getAssetByName-example", responseFields(
                  fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
                  fieldWithPath("room").description(""),
                  fieldWithPath("row").description("").optional(),
                  fieldWithPath("col").description("").optional(),
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
                  fieldWithPath("cabinetsize")
                        .description("The cabinet size(only for cabinet type)").type(int.class)
                        .optional(),
                  fieldWithPath("cabinetAssetNumber").description(
                        "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                        .type(long.class).optional(),
                  fieldWithPath("assetRealtimeDataSpec")
                        .description("Only valid for sensor type of asset.")
                        .type(AssetRealtimeDataSpec.class).optional(),
                  fieldWithPath("justificationfields")
                        .description("Justification fields that input by user."),
                  fieldWithPath("sensorsformulars")
                        .description("The sensor data generator logic for this asset."),
                  fieldWithPath("lastupdate").description("When this asset was last upated"),
                  fieldWithPath("created").description("When this asset was created"),
                  fieldWithPath("pdus")
                        .description("Possible PDUs that this server connected with"),
                  fieldWithPath("switches")
                        .description("Physical switchs that this host connected with"),
                  fieldWithPath("status")
                        .description("This is a collection of states, including the state of the asset, "
                              + "the state of the pdu mapping, and the state of the switch mapping."))));

      assetRepository.delete(asset.getId());
   }

   @Test
   public void updateAssetExample() throws Exception {
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      asset.setAssetName("pek-wor-server-04");
      asset.setManufacturer("VMware");
      asset.setAssetSource("4");
      this.mockMvc
            .perform(put("/v1/assets").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(asset)))
            .andExpect(status().isOk())
            .andDo(document("assets-update-example", requestFields(
                  fieldWithPath("id").description("ID of the asset, created by wormhole"),
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
                  fieldWithPath("room").description(""),
                  fieldWithPath("row").description("").optional(),
                  fieldWithPath("col").description("").optional(),
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
                  fieldWithPath("cabinetsize")
                        .description("The cabinet size(only for cabinet type)").type(int.class)
                        .optional(),
                  fieldWithPath("cabinetAssetNumber").description(
                        "The asset number of the cabinet. Will be used to search more detail information about the cabinet.")
                        .type(long.class).optional(),
                  fieldWithPath("assetRealtimeDataSpec")
                        .description("Only valid for sensor type of asset.")
                        .type(AssetRealtimeDataSpec.class).optional(),
                  fieldWithPath("justificationfields").ignored(),
                  fieldWithPath("sensorsformulars").ignored(),
                  fieldWithPath("lastupdate").ignored(), fieldWithPath("created").ignored(),
                  fieldWithPath("pdus")
                        .description("Possible PDUs that this server connected with"),
                  fieldWithPath("switches")
                        .description("Physical switchs that this host connected with"),
                  fieldWithPath("status")
                        .description("This is a collection of states, including the state of the asset, "
                              + "the state of the pdu mapping, and the state of the switch mapping."))));

      assetRepository.delete(asset.getId());
   }

   @Test
   public void updateServerMappingExample() throws Exception {

      ServerMapping mapping = createServerMapping();
      serverMappingRepository.save(mapping);

      mapping.setVcClusterMobID("1");
      mapping.setVcHostName("1");

      this.mockMvc
            .perform(put("/v1/assets/mapping").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(mapping)))
            .andExpect(status().isOk())
            .andDo(document("assets-updateServerMapping-example",
                  requestFields(
                        fieldWithPath("id").description("ID of the mapping, created by wormhole"),
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

      this.mockMvc
            .perform(put("/v1/assets/mapping/merge/" + mapping1.getId() + "/" + mapping2.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(mappingId)))
            .andExpect(status().isOk())
            .andDo(document("assets-mergeServerMapping-example",
                  requestFields(
                        fieldWithPath("FirstId")
                              .description("ID of the mapping's firstid created by wormhole."),
                        fieldWithPath("SecondId")
                              .description("ID of the mapping's secondid created by wormhole."))));


      serverMappingRepository.delete(mapping1.getId());
      serverMappingRepository.delete(mapping2.getId());
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

      assetRepository.delete(asset.getId());
   }

   ServerMapping createServerMapping() throws Exception {
      ServerMapping mapping = new ServerMapping();
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
   public void getServerSensorData() throws Exception {
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("type").description("type").type(ServerSensorData.ServerSensorType.class),
            fieldWithPath("valueNum").description("valueNum.").type(JsonFieldType.NUMBER),
            fieldWithPath("value").description("value").type(JsonFieldType.NULL),
            fieldWithPath("timeStamp").description("timeStamp").type(JsonFieldType.NUMBER) };
      RealTimeData realTimeData = createRealTimeData();
      List<RealTimeData> realTimeDatas = new ArrayList<RealTimeData>();
      realTimeDatas.add(realTimeData);
      Asset asset = createAsset();
      asset = assetRepository.save(asset);
      realTimeDatas = realtimeDataRepository.save(realTimeDatas);
      this.mockMvc
            .perform(get("/v1/assets/" + asset.getId() + "/serversensordata").param("starttime",
                  "1501981711206"))
            .andExpect(content().string(equalTo(
                  "[{\"type\":\"PDU_RealtimeVoltage\",\"valueNum\":208.0,\"value\":null,\"timeStamp\":1501981711206},{\"type\":\"PDU_RealtimePower\",\"valueNum\":2.38,\"value\":null,\"timeStamp\":1501981711206},{\"type\":\"PDU_RealtimeLoad\",\"valueNum\":20.0,\"value\":null,\"timeStamp\":1501981711206}]")))
            .andDo(document("assets-getServerSensorData-example",
                  responseFields(fieldWithPath("[]").description("An array of realTimeDatas"))
                        .andWithPrefix("[].", fieldpath)));
      assetRepository.delete(asset);
      realtimeDataRepository.delete(realTimeDatas);
   }

   RealTimeData createRealTimeData() {
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();
      ValueUnit valueunit = new ValueUnit();
      valueunit.setKey(ValueType.PDU_RealtimeLoad);
      valueunit.setUnit("Amps");
      valueunit.setValueNum(20);
      valueunit.setTime(1501981711206L);
      valueunits.add(valueunit);
      ValueUnit valueunitpower = new ValueUnit();
      valueunitpower.setKey(ValueType.PDU_RealtimePower);
      valueunitpower.setUnit("KW");
      valueunitpower.setValueNum(2.38);
      valueunitpower.setTime(1501981711206L);
      valueunits.add(valueunitpower);
      ValueUnit valueunitvoltage = new ValueUnit();
      valueunitvoltage.setKey(ValueType.PDU_RealtimeVoltage);
      valueunitvoltage.setUnit("Volts");
      valueunitvoltage.setValueNum(208);
      valueunitvoltage.setTime(1501981711206L);
      valueunits.add(valueunitvoltage);

      RealTimeData realTimeData = new RealTimeData();
      realTimeData.setAssetID("5x4ff46982db22e1b040e0f2");
      realTimeData.setValues(valueunits);
      realTimeData.setTime(valueunits.get(0).getTime());
      return realTimeData;
   }

   AssetIPMapping createAssetIPMapping() throws Exception {
      AssetIPMapping assetipmapping = new AssetIPMapping();
      assetipmapping.setAssetname("assetname");
      assetipmapping.setIp("127.0.0.1");
      return assetipmapping;
   }

   Asset createAsset() {
      Asset asset = new Asset();
      asset.setAssetName("pek-wor-server-02");
      asset.setAssetNumber(12345);
      asset.setAssetSource("5b7d208d55368540fcba1692");
      asset.setCategory(AssetCategory.Server);
      asset.setModel("Dell 750");
      asset.setManufacturer("Dell");
      EnumMap<ServerSensorType, String> sensorsformulars =
            new EnumMap<ServerSensorType, String>(ServerSensorType.class);
      sensorsformulars.put(ServerSensorType.PDU_RealtimeLoad, "5x4ff46982db22e1b040e0f2");
      sensorsformulars.put(ServerSensorType.PDU_RealtimePower, "5x4ff46982db22e1b040e0f2");
      sensorsformulars.put(ServerSensorType.PDU_RealtimeVoltage, "5x4ff46982db22e1b040e0f2");
      asset.setSensorsformulars(sensorsformulars);
      return asset;
   }
}
