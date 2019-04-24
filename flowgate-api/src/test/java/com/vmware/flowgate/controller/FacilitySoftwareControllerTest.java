/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.mockito.Matchers.any;
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
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.ListOperations;
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
import com.vmware.flowgate.auth.NlyteAuth;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.service.ServerValidationService;
import com.vmware.flowgate.util.EncryptionGuard;
import com.vmware.flowgate.util.WormholeUserDetails;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class FacilitySoftwareControllerTest {
   @Rule
   public final JUnitRestDocumentation restDocumentation =
         new JUnitRestDocumentation("target/generated-snippets");

   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private FacilitySoftwareConfigRepository facilitySoftwareRepository;

   @Autowired
   private WebApplicationContext context;

   @SpyBean
   private ServerValidationService serverValidationService;

   @SpyBean
   private AccessTokenService tokenService;

   @MockBean
   private StringRedisTemplate template;
   @MockBean
   private MessagePublisher publisher;
   @Rule
   public ExpectedException expectedEx = ExpectedException.none();

   @Before
   public void setUp() {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation)).build();
   }

   @Test
   public void createAnFacilitySoftwareThrowException() throws JsonProcessingException, Exception {
      NlyteAuth nlyteAuth = Mockito.mock(NlyteAuth.class);
      Mockito.doReturn(false).when(nlyteAuth).auth(Mockito.any(FacilitySoftwareConfig.class));
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      Mockito.doReturn(nlyteAuth).when(serverValidationService).createNlyteAuth();
      ListOperations<String, String> listOperations = Mockito.mock(ListOperations.class);
      Mockito.doReturn(0L).when(listOperations).leftPush(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(listOperations).when(template).opsForList();
      Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Invalid user name or password");
      FacilitySoftwareConfig facilitySoftware = createFacilitySoftware();
      MvcResult result = this.mockMvc
            .perform(post("/v1/facilitysoftware").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(facilitySoftware)))
            .andExpect(status().is4xxClientError())
            .andDo(document("facilitySoftware-create-example", requestFields(
                  fieldWithPath("id")
                        .description("ID of the facilitySoftware, created by wormhole"),
                  fieldWithPath("name").description("The facilitySoftware name."),
                  fieldWithPath("description").description(""),
                  fieldWithPath("serverURL")
                        .description("The server's address, it can be an IP or FQDN."),
                  fieldWithPath("userName").description("An username used to obtain authorization"),
                  fieldWithPath("password")
                        .description(" A password used to obtain authorization."),
                  fieldWithPath("type").description(
                        "A type for facilitySoftware,forExample Nlyte,PowerIQ,Device42,OtherDCIM or OtherCMDB")
                        .type(AssetSubCategory.class).optional(),
                  fieldWithPath("userId").description(""),
                  fieldWithPath("verifyCert").description(
                        "Whether to verify the certificate when accessing the serverURL."),
                  fieldWithPath("advanceSetting").description(""),
                  fieldWithPath("integrationStatus").description("The status of integration."))))
            .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      } else {
         TestCase.fail();
      }
      facilitySoftwareRepository.delete(facilitySoftware.getId());
   }
   
   @Test
   public void syncFacilityServerDataExample() throws JsonProcessingException, Exception {
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Invalid ID");
      MvcResult result = this.mockMvc
            .perform(post("/v1/facilitysoftware/syncdatabyserverid/5c3704c69662e37c30a8db2f"))
            .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      } else {
         TestCase.fail();
      }
   }
   
   @Test
   public void syncFacilityServerDataExample1() throws JsonProcessingException, Exception {
      ListOperations<String, String> listOperations = Mockito.mock(ListOperations.class);
      Mockito.doReturn(0L).when(listOperations).leftPush(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(listOperations).when(template).opsForList();
      Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());
      FacilitySoftwareConfig facilitySoftware = createFacilitySoftware();
      facilitySoftware.setPassword(EncryptionGuard.encode(facilitySoftware.getPassword()));
      facilitySoftware = facilitySoftwareRepository.save(facilitySoftware);
      MvcResult result = this.mockMvc
            .perform(post("/v1/facilitysoftware/syncdatabyserverid/"+facilitySoftware.getId()+""))
            .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      } 
      facilitySoftwareRepository.delete(facilitySoftware.getId());
   }
   
   @Test
   public void syncFacilityServerDataExample2() throws JsonProcessingException, Exception {
      ListOperations<String, String> listOperations = Mockito.mock(ListOperations.class);
      Mockito.doReturn(0L).when(listOperations).leftPush(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(listOperations).when(template).opsForList();
      Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());
      FacilitySoftwareConfig facilitySoftware = createFacilitySoftware();
      facilitySoftware.setType(FacilitySoftwareConfig.SoftwareType.PowerIQ);
      facilitySoftware.setPassword(EncryptionGuard.encode(facilitySoftware.getPassword()));
      facilitySoftware = facilitySoftwareRepository.save(facilitySoftware);
      MvcResult result = this.mockMvc
            .perform(post("/v1/facilitysoftware/syncdatabyserverid/"+facilitySoftware.getId()+""))
            .andDo(document("facilitySoftware-syncFacilityServerData-example"))
            .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      } 
      facilitySoftwareRepository.delete(facilitySoftware.getId());
   }
   
   @Test
   public void createAnFacilitySoftware() throws JsonProcessingException, Exception {

      NlyteAuth nlyteAuth = Mockito.mock(NlyteAuth.class);
      Mockito.doReturn(true).when(nlyteAuth).auth(Mockito.any(FacilitySoftwareConfig.class));
      Mockito.doReturn(nlyteAuth).when(serverValidationService).createNlyteAuth();
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      ListOperations<String, String> listOperations = Mockito.mock(ListOperations.class);
      Mockito.doReturn(0L).when(listOperations).leftPush(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(listOperations).when(template).opsForList();
      Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());
      FacilitySoftwareConfig facilitySoftware = createFacilitySoftware();
      facilitySoftware.setServerURL("some.not.exit.server");
      facilitySoftware.setUserName("atestuser1");
      facilitySoftware.setPassword("atestuser1");
      try {
         this.mockMvc
               .perform(post("/v1/facilitysoftware").contentType(MediaType.APPLICATION_JSON)
                     .content(objectMapper.writeValueAsString(facilitySoftware)))
               .andExpect(status().isCreated())
               .andDo(document("facilitySoftware-create-example", requestFields(
                     fieldWithPath("id")
                           .description("ID of the facilitySoftware, created by wormhole"),
                     fieldWithPath("name").description("The facilitySoftware name."),
                     fieldWithPath("description").description(""),
                     fieldWithPath("serverURL")
                           .description("The server's address, it can be an IP or FQDN."),
                     fieldWithPath("userName")
                           .description("An username used to obtain authorization"),
                     fieldWithPath("password")
                           .description(" A password used to obtain authorization."),
                     fieldWithPath("type").description(
                           "A type for facilitySoftware,forExample Nlyte,PowerIQ,Device42,OtherDCIM or OtherCMDB")
                           .type(AssetSubCategory.class).optional(),
                     fieldWithPath("userId").description(""),
                     fieldWithPath("verifyCert").description("Whether to verify the certificate when accessing the serverURL."),
                     fieldWithPath("advanceSetting").description(""),
                     fieldWithPath("integrationStatus").description("The status of integration."))));
      } catch (Exception e) {
         TestCase.fail();
      }
   }

   @Test
   public void updateAnFacilitySoftware() throws JsonProcessingException, Exception {
      NlyteAuth nlyteAuth = Mockito.mock(NlyteAuth.class);
      Mockito.doReturn(false).when(nlyteAuth).auth(Mockito.any(FacilitySoftwareConfig.class));
      Mockito.doReturn(nlyteAuth).when(serverValidationService).createNlyteAuth();

      FacilitySoftwareConfig facilitySoftware = createFacilitySoftware();
      facilitySoftwareRepository.save(facilitySoftware);
      facilitySoftware.setDescription("for generalUser");
      facilitySoftware.setUserName("generalUser");
      facilitySoftware.setPassword("generalUserPassword");
      try {
         this.mockMvc
               .perform(put("/v1/facilitysoftware").contentType(MediaType.APPLICATION_JSON)
                     .content(objectMapper.writeValueAsString(facilitySoftware)))
               .andDo(document("facilitySoftware-update-example", requestFields(
                     fieldWithPath("id")
                           .description("ID of the facilitySoftware, created by wormhole"),
                     fieldWithPath("name").description("The facilitySoftware name."),
                     fieldWithPath("description").description(""),
                     fieldWithPath("serverURL")
                           .description("The server's address, it can be an IP or FQDN."),
                     fieldWithPath("userName")
                           .description("An username used to obtain authorization"),
                     fieldWithPath("password")
                           .description(" A password used to obtain authorization."),
                     fieldWithPath("type").description(
                           "A type for facilitySoftware,forExample Nlyte,PowerIQ,Device42,OtherDCIM or OtherCMDB")
                           .type(AssetSubCategory.class).optional(),
                     fieldWithPath("userId").description(""),
                     fieldWithPath("verifyCert").description(
                           "Whether to verify the certificate when accessing the serverURL."),
                     fieldWithPath("advanceSetting").description(""),
                     fieldWithPath("integrationStatus").description("The status of integration."))));
      } catch (Exception e) {
         TestCase.fail();
      }

      facilitySoftwareRepository.delete(facilitySoftware.getId());
   }

   @Test
   public void facilitySoftwareQueryByPageExample() throws Exception {
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      FacilitySoftwareConfig facilitySoftware = createFacilitySoftware();
      facilitySoftware.setPassword(EncryptionGuard.encode(facilitySoftware.getPassword()));
      facilitySoftwareRepository.save(facilitySoftware);
      int pageNumber = 1;
      int pageSize = 5;
      this.mockMvc
            .perform(
                  get("/v1/facilitysoftware/page/" + pageNumber + "/pagesize/"
                        + pageSize + "").content("{\"pageNumber\":1,\"pageSize\":5}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$..content[0].name").value(facilitySoftware.getName()))
            .andExpect(jsonPath("$..content[0].userId").value(facilitySoftware.getUserId()))
            .andDo(document("facilitySoftware-query-example",
                  requestFields(
                        fieldWithPath("pageNumber").description("get datas for this page number."),
                        fieldWithPath("pageSize")
                              .description("The number of data displayed per page."))));

      facilitySoftwareRepository.delete(facilitySoftware.getId());
   }
   
   @Test
   public void getFacilitySoftwareConfigByTypeExample() throws Exception {
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      FacilitySoftwareConfig facilitySoftware1 = createFacilitySoftware();
      facilitySoftware1.setName("1");
      facilitySoftware1.setPassword(EncryptionGuard.encode(facilitySoftware1.getPassword()));
      facilitySoftwareRepository.save(facilitySoftware1);
      FacilitySoftwareConfig facilitySoftware2 = createFacilitySoftware();
      facilitySoftware1.setName("2");
      facilitySoftware2.setPassword(EncryptionGuard.encode(facilitySoftware2.getPassword()));
      facilitySoftwareRepository.save(facilitySoftware2);
      
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
              fieldWithPath("id").description("ID of FacilitySoftwareConfig, created by wormhole"),
              fieldWithPath("name").description("The facilitySoftware name."),
              fieldWithPath("description").description(""),
              fieldWithPath("userName").description(
                    "An username used to obtain authorization"),
              fieldWithPath("password").description(
                    " A password used to obtain authorization."),
              fieldWithPath("serverURL").description(
                      "The server's address, it can be an IP or FQDN."),
              fieldWithPath("type").description(
                      "A type for facilitySoftware,forExample Nlyte,PowerIQ,Device42,OtherDCIM or OtherCMDB").type(SoftwareType.class).optional(),
              fieldWithPath("userId").description(
                      ""),
              fieldWithPath("verifyCert").description(
                      "Whether to verify the certificate when accessing the serverURL.").type(JsonFieldType.BOOLEAN)
              };
      this.mockMvc
         .perform(get("/v1/facilitysoftware/type/" + facilitySoftware1.getType() + ""))
                .andExpect(status().isOk())
                .andDo(document("facilitySoftware-getFacilitySoftwareConfigByType-example",responseFields(
                        fieldWithPath("[]").description("An array of asserts"))
                        .andWithPrefix("[].", fieldpath)));

      facilitySoftwareRepository.delete(facilitySoftware1.getId());
      facilitySoftwareRepository.delete(facilitySoftware2.getId());
   }

   @Test
   public void facilitySoftwareDeleteExample() throws Exception {
      FacilitySoftwareConfig facilitySoftware = createFacilitySoftware();
      facilitySoftware = facilitySoftwareRepository.save(facilitySoftware);
      this.mockMvc
            .perform(delete("/v1/facilitysoftware/" + facilitySoftware.getId())
                  .content("{\"id\":\"" + facilitySoftware.getId() + "\"}"))
            .andExpect(status().isOk())
            .andDo(document("facilitySoftware-delete-example", requestFields(
                  fieldWithPath("id").description("The primary key for facility software."))));
   }


   FacilitySoftwareConfig createFacilitySoftware() throws Exception {
      FacilitySoftwareConfig example = new FacilitySoftwareConfig();
      example.setName("Nlyte");
      example.setUserName("administrator@vsphere.local");
      example.setPassword("Admin!23");
      example.setServerURL("https://10.160.30.134");
      example.setType(FacilitySoftwareConfig.SoftwareType.Nlyte);
      example.setUserId("1");
      example.setVerifyCert(false);
      return example;
   }

   WormholeUserDetails createuser() {
      WormholeUserDetails user = new WormholeUserDetails();
      user.setUserId("1");
      return user;
   }
}
