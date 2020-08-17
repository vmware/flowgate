/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.AdapterJobCommand;
import com.vmware.flowgate.common.model.FacilityAdapter;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.FacilityAdapterRepository;

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

   @Rule
   public ExpectedException expectedEx = ExpectedException.none();

   @Before
   public void setUp() {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation)).build();
   }

   @Test
   public void createFacilityAdapterExample() throws JsonProcessingException, Exception {
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
      facilityAdapterRepo.deleteById(adapter1.getId());
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
            .perform(get("/v1/facilityadapter/"+adapter.getId()+"")
                  .content("{\"id\":\"" + adapter.getId() + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$..displayName").value("displayNameForTestfindById"))
            .andDo(document("facilityadapter-findOneById-example", requestFields(
                  fieldWithPath("id").description("ID of the facility adapter, created by flowgate"))))
            .andReturn();
      facilityAdapterRepo.deleteById(adapter.getId());
   }

   @Test
   public void deleteFacilityAdapterExample() throws Exception {
      FacilityAdapter adapter = createAdapter();
      facilityAdapterRepo.save(adapter);
      this.mockMvc
            .perform(delete("/v1/facilityadapter/" + adapter.getId() + "")
                  .content("{\"id\":\"" + adapter.getId() + "\"}"))
            .andExpect(status().isOk())
            .andDo(document("facilityadapter-deleteById-example",
                  requestFields(fieldWithPath("id")
                        .description("ID of the facility adapter, created by flowgate"))))
            .andReturn();
   }

   @Test
   public void findAllFacilityAdapterExample() throws JsonProcessingException, Exception {
      FacilityAdapter adapter = createAdapter();
      adapter.setDisplayName("displayNameForTestfindall");
      facilityAdapterRepo.save(adapter);
      this.mockMvc
            .perform(get("/v1/facilityadapter/pagenumber/1/pagesize/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$..content[0].displayName").value("displayNameForTestfindall"))
            .andDo(document("facilityadapter-findByPage-example", responseFields(
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
