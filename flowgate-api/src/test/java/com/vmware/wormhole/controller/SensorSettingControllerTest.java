/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.SensorSetting;
import com.vmware.flowgate.common.model.ServerSensorData.ServerSensorType;
import com.vmware.flowgate.repository.SensorSettingRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class SensorSettingControllerTest {

   @Rule
   public final JUnitRestDocumentation restDocumentation =
         new JUnitRestDocumentation("target/generated-snippets");

   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private SensorSettingRepository sensorSettingRepository;

   @Autowired
   private WebApplicationContext context;

   @Before
   public void setUp() {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation)).build();
   }

   @Test
   public void createAnSensorSetting() throws JsonProcessingException, Exception {
      SensorSetting  sensorsetting = createSensorSetting();
      sensorsetting.setId("temporary_id");
      this.mockMvc
            .perform(post("/v1/sensors/setting").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(sensorsetting)))
            .andExpect(status().isCreated())
            .andDo(document("sensorSetting-create-example", requestFields(
                  fieldWithPath("id").description("ID of the sensorSetting, created by wormhole"),
                  fieldWithPath("type").description(
                        "The sensor type."),
                  fieldWithPath("minNum").description("Value type is double"),
                  fieldWithPath("maxNum").description("Value type is double"),
                  fieldWithPath("minValue").description("Value type is string"),
                  fieldWithPath("maxValue").description("Value type is string")
                 )));

      sensorSettingRepository.delete(sensorsetting.getId());
   }
   @Test
   public void udapteAnSensorSetting() throws JsonProcessingException, Exception {
      SensorSetting  sensorsetting = createSensorSetting();
      sensorSettingRepository.save(sensorsetting);
      sensorsetting.setMaxNum(25);
      this.mockMvc
            .perform(put("/v1/sensors/setting").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(sensorsetting)))
            .andExpect(status().isOk())
            .andDo(document("sensorSetting-update-example", requestFields(
                  fieldWithPath("id").description("ID of the sensorSetting, created by wormhole"),
                  fieldWithPath("type").description(
                        "The sensor type."),
                  fieldWithPath("minNum").description("Value type is double"),
                  fieldWithPath("maxNum").description("Value type is double"),
                  fieldWithPath("minValue").description("Value type is string"),
                  fieldWithPath("maxValue").description("Value type is string")
                 )));

      sensorSettingRepository.delete(sensorsetting.getId());
   }
   @Test
   public void sensorSettingQueryByPageExample() throws Exception {
      SensorSetting  sensorsetting = createSensorSetting();
      sensorSettingRepository.save(sensorsetting);
      int pageNumber = 1;
      int pageSize = 5;
      this.mockMvc
            .perform(get("/v1/sensors/setting/page/" + pageNumber
                  + "/pagesize/" + pageSize + "").content("{\"pageNumber\":1,\"pageSize\":5}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$..content[0].maxNum").value(sensorsetting.getMaxNum()))
            .andExpect(jsonPath("last", is(true)))
            .andExpect(jsonPath("number", is(0)))
            .andExpect(jsonPath("size", is(5)))
            .andExpect(jsonPath("first", is(true)))
            .andDo(document("sensorSetting-queryByPage-example", requestFields(
                  fieldWithPath("pageNumber")
                  .description("get datas for this page number."),
                  fieldWithPath("pageSize")
                  .description("The number of data displayed per page."))));

      sensorSettingRepository.delete(sensorsetting.getId());
   }
   
   @Test
   public void sensorQuerySettingExample() throws Exception {
      SensorSetting  sensorsetting1 = createSensorSetting();
      sensorsetting1.setId("1");
      sensorSettingRepository.save(sensorsetting1);
      SensorSetting  sensorsetting2 = createSensorSetting();
      sensorsetting2.setId("2");
      sensorSettingRepository.save(sensorsetting2);
      
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
              fieldWithPath("id").description("ID of the sensorSetting, created by wormhole"),
              fieldWithPath("type").description(
                    "The sensor type."),
              fieldWithPath("minNum").description("Value type is double"),
              fieldWithPath("maxNum").description("Value type is double"),
              fieldWithPath("minValue").description("Value type is string"),
              fieldWithPath("maxValue").description("Value type is string")
               };
      this.mockMvc
            .perform(get("/v1/sensors/setting"))
            .andExpect(status().isOk())
            .andDo(document("sensorSetting-querySetting-example", responseFields(
                    fieldWithPath("[]").description("An array of RealTimeData"))
                    .andWithPrefix("[].", fieldpath)))
                    .andReturn().getResponse().getHeader("Location");

      sensorSettingRepository.delete(sensorsetting1.getId());
      sensorSettingRepository.delete(sensorsetting2.getId());
   }

   @Test
   public void sensorSettingDeleteExample() throws Exception {
      SensorSetting  sensorsetting = createSensorSetting();
      sensorSettingRepository.save(sensorsetting);
      this.mockMvc.perform(delete("/v1/sensors/setting/" + sensorsetting.getId())
            .content("{\"id\":\""+sensorsetting.getId()+"\"}"))
            .andExpect(status().isOk())
            .andDo(document("sensorSetting-delete-example", requestFields(
            fieldWithPath("id").description("The primary key for sensorsetting."))));

      sensorSettingRepository.delete(sensorsetting.getId());
   }

   SensorSetting createSensorSetting() {
      SensorSetting example = new SensorSetting();
      example.setType(ServerSensorType.BACKPANELTEMP);
      example.setMaxNum(35);
      example.setMinNum(5);
      return example;
   }
}
