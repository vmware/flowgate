/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.AdapterJobCommand;
import com.vmware.flowgate.common.model.FacilityAdapter;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.FacilityAdapterRepository;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class FacilityAdapterControllerTest {
   @Rule
   public final JUnitRestDocumentation restDocumentation =
         new JUnitRestDocumentation("target/generated-snippets");

   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private WebApplicationContext context;

   @Autowired
   private FacilityAdapterRepository facilityAdapterRepo;

   @Autowired
   private FacilitySoftwareConfigRepository facilitySoftwareRepo;

   @MockBean
   private StringRedisTemplate redis;

   @Rule
   public ExpectedException expectedEx = ExpectedException.none();

   @Before
   public void setUp() {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation)).build();
   }

   @Test
   public void createFacilityAdapterExample() throws JsonProcessingException, Exception {
      SetOperations<String, String> setop = Mockito.mock(SetOperations.class);
      when(redis.opsForSet()).thenReturn(setop);
      when(setop.add(anyString(), anyString())).thenReturn(1l);
      FacilityAdapter adapter = createAdapter();
      this.mockMvc
            .perform(post("/v1/facilityadapter").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(adapter)))
            .andExpect(status().isCreated()).andExpect(header().string("Location", notNullValue()))
            .andDo(document("facilityadapter-create-example", requestFields(
                  fieldWithPath("id").description("ID of the facility adapter, created by flowgate"),
                  fieldWithPath("displayName").description("Display name of the facility adapter, created by user"),
                  fieldWithPath("type").description("Type of the facility adapter"),
                  fieldWithPath("description").description("Description of the facility adapter"),
                  fieldWithPath("topic").description("Topic of the facility adapter,created by flowgate"),
                  fieldWithPath("subCategory").description("Subcategory of the facility adapter,created by flowgate"),
                  fieldWithPath("queueName").description("Queue name of the facility adapter,created by flowgate"),
                  fieldWithPath("serviceKey").description("Value for auth,created by flowgate"),
                  fieldWithPath("createTime").description("Create time of the facility adapter,created by flowgate"),
                  subsectionWithPath("commands").description("Job commands of the facility adapter,it should be not null"))))
            .andReturn().getResponse().getHeader("Location");
      facilityAdapterRepo.deleteById(adapter.getId());
   }

   @Test
   public void createFacilityAdapterValidDisplayName() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      adapter.setDisplayName("Nlyte");
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Invalid value: 'Nlyte' for field: 'DisplayName' ");
      MvcResult result = this.mockMvc
            .perform(post("/v1/facilityadapter").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(adapter)))
            .andExpect(status().is4xxClientError())
            .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      } else {
         TestCase.fail();
      }
   }

   @Test
   public void createFacilityAdapterValidDisplayName1() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      adapter.setDisplayName("testdisplayName");
      facilityAdapterRepo.save(adapter);
      FacilityAdapter adapter1 = createAdapter();
      adapter1.setDisplayName(adapter.getDisplayName());
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Adapter with dispalyName : testdisplayName is existed");
      MvcResult result = this.mockMvc
            .perform(post("/v1/facilityadapter").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(adapter1)))
            .andExpect(status().is4xxClientError())
            .andReturn();
      facilityAdapterRepo.deleteById(adapter.getId());
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      } else {
         TestCase.fail();
      }
   }

   @Test
   public void createFacilityAdapterValidCommands() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      adapter.setCommands(null);
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("The Commands field is required.");
      MvcResult result = this.mockMvc
            .perform(post("/v1/facilityadapter").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(adapter)))
            .andExpect(status().is4xxClientError())
            .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      } else {
         TestCase.fail();
      }
   }

   @Test
   public void updateFacilityAdapterExample() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      facilityAdapterRepo.save(adapter);
      adapter.setDisplayName("displayNameForTestUpdate");
      this.mockMvc
            .perform(put("/v1/facilityadapter").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(adapter)))
            .andExpect(status().isOk())
            .andDo(document("facilityadapter-update-example", requestFields(
                  fieldWithPath("id").description("ID of the facility adapter, created by flowgate"),
                  fieldWithPath("displayName").description("Display name of the facility adapter, created by user"),
                  fieldWithPath("type").description("Type of the facility adapter"),
                  fieldWithPath("description").description("Description of the facility adapter"),
                  fieldWithPath("topic").description("Topic of the facility adapter,created by flowgate"),
                  fieldWithPath("subCategory").description("Subcategory of the facility adapter,created by flowgate"),
                  fieldWithPath("queueName").description("Queue name of the facility adapter,created by flowgate"),
                  fieldWithPath("serviceKey").description("Value for auth,created by flowgate"),
                  fieldWithPath("createTime").description("Create time of the facility adapter,created by flowgate"),
                  subsectionWithPath("commands").description("Job commands of the facility adapter,it should be not null"))))
            .andReturn();
      facilityAdapterRepo.deleteById(adapter.getId());
   }

   @Test
   public void updateFacilityAdapterValidDisplayNameExample() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      adapter.setDisplayName("displayNameForTestUpdate");
      facilityAdapterRepo.save(adapter);
      FacilityAdapter adapter1 = createAdapter();
      adapter1.setDisplayName("displayNameForTestUpdate");
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Adapter with dispalyName : displayNameForTestUpdate is existed");
      MvcResult result = this.mockMvc
            .perform(put("/v1/facilityadapter").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(adapter1)))
            .andExpect(status().is4xxClientError())
            .andReturn();
      facilityAdapterRepo.deleteById(adapter.getId());
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      } else {
         TestCase.fail();
      }
   }

   @Test
   public void updateFacilityAdapterValidCommandsExample() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      facilityAdapterRepo.save(adapter);
      adapter.setCommands(null);
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("The Commands field is required.");
      MvcResult result = this.mockMvc
            .perform(put("/v1/facilityadapter").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(adapter)))
            .andExpect(status().is4xxClientError())
            .andReturn();
      facilityAdapterRepo.deleteById(adapter.getId());
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      } else {
         TestCase.fail();
      }
   }

   @Test
   public void updateFacilityAdapterValidExistedExample() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Failed to find FacilityAdapter with field: id  and value: "+adapter.getId()+"");
      MvcResult result = this.mockMvc
            .perform(put("/v1/facilityadapter").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(adapter)))
            .andExpect(status().is4xxClientError())
            .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      } else {
         TestCase.fail();
      }
   }

   @Test
   public void findFacilityAdapterByIdExample() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      adapter.setDisplayName("displayNameForTestfindById");
      facilityAdapterRepo.save(adapter);

      this.mockMvc
            .perform(get("/v1/facilityadapter/{Id}", adapter.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$..displayName").value("displayNameForTestfindById"))
            .andDo(document("facilityadapter-findOneById-example", pathParameters(
                  parameterWithName("Id").description("The id of FacilityAdapter, generated by flowgate."))))
            .andReturn();
      facilityAdapterRepo.deleteById(adapter.getId());
   }

   @Test
   public void deleteFacilityAdapterExample() throws Exception {
      SetOperations<String, String> setop = Mockito.mock(SetOperations.class);
      when(redis.opsForSet()).thenReturn(setop);
      when(setop.isMember(anyString(), anyString())).thenReturn(true);
      when(setop.remove(anyString(), anyString())).thenReturn(1l);
      FacilityAdapter adapter = createAdapter();
      facilityAdapterRepo.save(adapter);
      this.mockMvc
            .perform(delete("/v1/facilityadapter/{Id}", adapter.getId()))
            .andExpect(status().isOk())
            .andDo(document("facilityadapter-deleteById-example",
                  pathParameters(
                        parameterWithName("Id").description("The id of facility adapter,generated by flowgate."))))
            .andReturn();
   }

   @Test
   public void deleteFacilityAdapterThrowExceptionExample() throws Exception {
      FacilityAdapter adapter = createAdapter();
      String subcategory = "OtherDCIM_"+ UUID.randomUUID().toString();
      adapter.setSubCategory(subcategory);
      facilityAdapterRepo.save(adapter);
      FacilitySoftwareConfig example = new FacilitySoftwareConfig();
      example.setId(UUID.randomUUID().toString());
      example.setName("Nlyte");
      example.setUserName("administrator@vsphere.local");
      example.setPassword("Admin!23");
      example.setServerURL("https://10.160.30.134");
      example.setType(FacilitySoftwareConfig.SoftwareType.Nlyte);
      example.setUserId("1");
      example.setVerifyCert(false);
      example.setDescription("description");
      example.setSubCategory(subcategory);
      facilitySoftwareRepo.save(example);
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Adapter deletion failed, there are some integration instances are using it");
      MvcResult result = this.mockMvc
            .perform(delete("/v1/facilityadapter/" + adapter.getId() + "")
                  .content("{\"id\":\"" + adapter.getId() + "\"}"))
            .andReturn();
      if (result.getResolvedException() != null) {
         facilityAdapterRepo.deleteById(adapter.getId());
         facilitySoftwareRepo.deleteById(example.getId());
         throw result.getResolvedException();
      } else {
         TestCase.fail();
      }
   }

   @Test
   public void findAllFacilityAdapterByPageExample() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      adapter.setDisplayName("displayNameForTestfindallByPage");
      facilityAdapterRepo.save(adapter);
      this.mockMvc
            .perform(get("/v1/facilityadapter/pagenumber/{pageNumber}/pagesize/{pageSize}", 1, 5))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$..content[0].displayName").value("displayNameForTestfindallByPage"))
            .andDo(document("facilityadapter-findByPage-example", pathParameters(
                  parameterWithName("pageNumber").description("The page you want to get"),
                  parameterWithName("pageSize").description("The number of facilityadapters you want to get by every request.Default value: 20")),
                  responseFields(
                  subsectionWithPath("content").description("FacilityAdapter's array."),
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
      facilityAdapterRepo.deleteById(adapter.getId());
   }

   @Test
   public void findAllFacilityAdapterExample() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      adapter.setDisplayName("displayNameForTestfindall");
      facilityAdapterRepo.save(adapter);
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
            fieldWithPath("id").description("ID of the facility adapter, created by flowgate"),
            fieldWithPath("displayName").description("Display name of the facility adapter, created by user"),
            fieldWithPath("type").description("Type of the facility adapter"),
            fieldWithPath("description").description("Description of the facility adapter"),
            fieldWithPath("topic").description("Topic of the facility adapter,created by flowgate"),
            fieldWithPath("subCategory").description("Subcategory of the facility adapter,created by flowgate"),
            fieldWithPath("queueName").description("Queue name of the facility adapter,created by flowgate"),
            fieldWithPath("serviceKey").description("Value for auth,created by flowgate"),
            fieldWithPath("createTime").description("Create time of the facility adapter,created by flowgate"),
            subsectionWithPath("commands").description("Job commands of the facility adapter,it should be not null") };
      this.mockMvc
            .perform(get("/v1/facilityadapter"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].displayName").value("displayNameForTestfindall"))
            .andDo(document("facilityadapter-findAll-example", responseFields(fieldWithPath("[]").description("An array of facility adapters"))
                  .andWithPrefix("[].", fieldpath)));
      facilityAdapterRepo.deleteById(adapter.getId());
   }


   FacilityAdapter createAdapter() {
      FacilityAdapter adapter = new FacilityAdapter();
      adapter.setId(UUID.randomUUID().toString());
      adapter.setDisplayName("defaultAdapter");
      adapter.setType(SoftwareType.OtherCMDB);
      adapter.setDescription("Default adapter for test");
      List<AdapterJobCommand> commands = new ArrayList<AdapterJobCommand>();
      AdapterJobCommand command = new AdapterJobCommand();
      command.setCommand("syncmetadata");
      command.setTriggerCycle(1440);
      command.setDescription("sync metadata job");
      commands.add(command);
      AdapterJobCommand metricsCommand = new AdapterJobCommand();
      metricsCommand.setCommand("syncmetricsdata");
      metricsCommand.setTriggerCycle(5);
      metricsCommand.setDescription("sync metrics data job");
      commands.add(metricsCommand);
      adapter.setCommands(commands);
      return adapter;
   }
}
