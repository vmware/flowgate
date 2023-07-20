/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import javax.net.ssl.SSLException;

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
import com.vmware.flowgate.auth.AuthVcUser;
import com.vmware.flowgate.common.model.IntegrationStatus;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.SDDCSoftwareRepository;
import com.vmware.flowgate.repository.ServerMappingRepository;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.service.ServerValidationService;
import com.vmware.flowgate.util.EncryptionGuard;
import com.vmware.flowgate.util.WormholeUserDetails;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class SDDCSoftwareControllerTest {

   @Rule
   public final JUnitRestDocumentation restDocumentation =
         new JUnitRestDocumentation("target/generated-snippets");

   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private SDDCSoftwareRepository sddcRepository;

   @Autowired
   private WebApplicationContext context;

   @Autowired
   ServerMappingRepository serverMappingRepository;

   @SpyBean
   private ServerValidationService serverValidationService;

   @SpyBean
   private AccessTokenService tokenService;

   @MockBean
   private MessagePublisher publisher;
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
   public void createVCenterSDDCSoftwareConfig() throws JsonProcessingException, Exception {
      SDDCSoftwareConfig sddc = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      AuthVcUser vcAuth = Mockito.mock(AuthVcUser.class);
      ListOperations<String, String> listOperations = Mockito.mock(ListOperations.class);
      Mockito.doReturn(0L).when(listOperations).leftPush(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(listOperations).when(template).opsForList();
      Mockito.doNothing().when(vcAuth).authenticateUser(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(vcAuth).when(serverValidationService)
            .getVcAuth(Mockito.any(SDDCSoftwareConfig.class));
      Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());
      try {
          this.mockMvc
               .perform(post("/v1/sddc/").contentType(MediaType.APPLICATION_JSON)
                     .content(objectMapper.writeValueAsString(sddc)))
               .andExpect(status().isCreated())
               .andDo(document("SDDCSoftware-create-example", requestFields(
                     fieldWithPath("id")
                           .description("ID of the SDDCSoftwareConfig, created by flowgate"),
                     fieldWithPath("name").description("The SDDCSoftwareConfig name."),
                     fieldWithPath("description").description("The SDDCSoftwareConfig description."),
                     fieldWithPath("serverURL")
                           .description("An ip address for a SDDCSoftwareConfig"),
                     fieldWithPath("userName")
                           .description("An username used to obtain authorization"),
                     fieldWithPath("password")
                           .description(" A password used to obtain authorization."),
                     fieldWithPath("type")
                           .description(
                                 "A type for SDDCSoftwareConfig,forExample VRO, VCENTER, OTHERS")
                           .type(SoftwareType.class).optional(),
                     fieldWithPath("userId").description("userId"),
                     fieldWithPath("subCategory").description(
                           "subCategory"),
                     fieldWithPath("verifyCert").description(
                           "Whether to verify the certificate when accessing the serverURL."),
                     subsectionWithPath("integrationStatus").description("The status of integration."))))
               .andReturn();
      } catch (Exception e) {
         TestCase.fail(e.getMessage());
      }
      sddcRepository.deleteById(sddc.getId());
   }
   @Test
   public void syncSDDCServerDataExample() throws JsonProcessingException, Exception {

      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      SDDCSoftwareConfig sddcCreate = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddcCreate.setPassword(EncryptionGuard.encode(sddcCreate.getPassword()));
      SDDCSoftwareConfig sddc = sddcRepository.save(sddcCreate);
      AuthVcUser vcAuth = Mockito.mock(AuthVcUser.class);
      ListOperations<String, String> listOperations = Mockito.mock(ListOperations.class);
      Mockito.doReturn(0L).when(listOperations).leftPush(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(listOperations).when(template).opsForList();
      Mockito.doNothing().when(vcAuth).authenticateUser(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(vcAuth).when(serverValidationService)
            .getVcAuth(Mockito.any(SDDCSoftwareConfig.class));
      Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());
      try {
         MvcResult result = this.mockMvc
               .perform(post("/v1/sddc/syncdatabyserverid/"+ sddc.getId() + ""))
               .andDo(document("SDDCSoftware-syncSDDCServerData-example"))
               .andReturn();
         TestCase.assertEquals(201, result.getResponse().getStatus());
      } catch (Exception e) {
         TestCase.fail();
      }
      sddcRepository.deleteById(sddc.getId());
   }

   /**
    * Update SDDCSoftwareConfig Test case one:To update the IsVerifyCert.Whether can be saved
    * successfully,when the VerifyCert is true. The message 'Certificate verification error' will be
    * throwed,when the VerifyCert is true.
    */
   @Test
   public void updateSDDCSoftwareConfigIsVerifyCert() throws Exception {
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Certificate verification error");
      SDDCSoftwareConfig sddcCreate = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      SDDCSoftwareConfig sddc = sddcRepository.save(sddcCreate);
      sddc.setVerifyCert(true);
      AuthVcUser vcAuth = Mockito.mock(AuthVcUser.class);
      Mockito.doThrow(new SSLException("I will failed")).when(vcAuth)
            .authenticateUser(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(vcAuth).when(serverValidationService)
            .getVcAuth(Mockito.any(SDDCSoftwareConfig.class));
      try {
         MvcResult result = this.mockMvc
               .perform(put("/v1/sddc/").contentType(MediaType.APPLICATION_JSON)
                     .content(objectMapper.writeValueAsString(sddc)))
               .andDo(document("SDDCSoftware-update-example", requestFields(
                     fieldWithPath("id")
                           .description("ID of the SDDCSoftwareConfig, created by flowgate"),
                     fieldWithPath("name").description("The SDDCSoftwareConfig name."),
                     fieldWithPath("description").description("The SDDCSoftwareConfig description."),
                     fieldWithPath("serverURL")
                           .description("An ip address for a SDDCSoftwareConfig"),
                     fieldWithPath("userName")
                           .description("An username used to obtain authorization"),
                     fieldWithPath("password")
                           .description(" A password used to obtain authorization."),
                     fieldWithPath("type")
                           .description(
                                 "A type for SDDCSoftwareConfig,forExample VRO, VCENTER, OTHERS")
                           .type(SoftwareType.class).optional(),
                     fieldWithPath("userId").description("userId"),
                     fieldWithPath("subCategory").description(
                           "subCategory"),
                     fieldWithPath("verifyCert").description(
                           "Whether to verify the certificate when accessing the serverURL."),
                     subsectionWithPath("integrationStatus").description("The status of integration."))))
               .andReturn();
         if (result.getResolvedException() != null) {
            throw result.getResolvedException();
         }
      } catch (Exception e) {
         throw e;
      } finally {
         sddcRepository.deleteById(sddc.getId());
      }
   }

   /**
    * Update SDDCSoftwareConfig Test case two:To update the username and password.Whether the wrong
    * username and password can be saved successfully The message 'Invalid user name or password'
    * will be throwed,when you given a password and an userName what is wrong;
    */
   @Test
   public void updateAnSDDCSoftwareConfigUserNameAndPassword()
         throws JsonProcessingException, Exception {

      AuthVcUser vcAuth = Mockito.mock(AuthVcUser.class);
      Mockito.doNothing().when(vcAuth).authenticateUser(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(vcAuth).when(serverValidationService)
            .getVcAuth(Mockito.any(SDDCSoftwareConfig.class));

      SDDCSoftwareConfig sddcCreate = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      SDDCSoftwareConfig sddc = sddcRepository.save(sddcCreate);
      sddc.setName("test update name");
      sddc.setUserName("testUserName");
      sddc.setPassword("testPassword");

      try {
         MvcResult result = this.mockMvc
               .perform(put("/v1/sddc/").contentType(MediaType.APPLICATION_JSON)
                     .content(objectMapper.writeValueAsString(sddc)))
               .andReturn();
         TestCase.assertEquals(200, result.getResponse().getStatus());
      } catch (Exception e) {
         TestCase.fail();
      }
      sddcRepository.deleteById(sddc.getId());
   }

   /**
    * Update SDDCSoftwareConfig Test case three:Whether can be saved successfully,when modify only
    * the server name or server description save success and test pass
    */
   @Test
   public void updateAnSDDCSoftwareConfig() throws JsonProcessingException, Exception {

      AuthVcUser vcAuth = Mockito.mock(AuthVcUser.class);
      Mockito.doNothing().when(vcAuth).authenticateUser(Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(vcAuth).when(serverValidationService)
            .getVcAuth(Mockito.any(SDDCSoftwareConfig.class));

      SDDCSoftwareConfig sddcCreate = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      SDDCSoftwareConfig sddc = sddcRepository.save(sddcCreate);
      sddc.setName("test update name");
      this.mockMvc
            .perform(put("/v1/sddc/").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(sddc)))
            .andExpect(status().isOk())
            .andReturn();
      sddcRepository.deleteById(sddc.getId());
   }

   @Test
   public void SDDCSoftwareQueryByPageExample() throws Exception {
      SDDCSoftwareConfig sddcCreate = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddcCreate.setPassword(EncryptionGuard.encode(sddcCreate.getPassword()));
      SDDCSoftwareConfig sddc = sddcRepository.save(sddcCreate);
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      int pageNumber = 1;
      int pageSize = 5;
      this.mockMvc.perform(get("/v1/sddc/page/{pageNumber}/pagesize/{pageSize}", pageNumber, pageSize))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$..content[0].name").value(sddc.getName()))
            .andExpect(jsonPath("$..content[0].userId").value(sddc.getUserId()))
            .andDo(document("SDDCSoftware-query-example", pathParameters(
                  parameterWithName("pageNumber").description("The page you want to get"),
                  parameterWithName("pageSize").description("The number of SDDCSoftwares you want to get by every request.Default value: 20")),
                  responseFields(
                  subsectionWithPath("content").type(JsonFieldType.ARRAY)
                        .description("SDDCSoftwareConfig's array."),
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
      sddcRepository.deleteById(sddc.getId());
   }

   @Test
   public void SDDCSoftwareQueryByTypeAndUserId() throws Exception {
      SDDCSoftwareConfig sddcCreate = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddcCreate.setType(SoftwareType.VCENTER);
      sddcCreate.setName("flowgate");
      sddcCreate.setDescription("flowgate cluster");
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      sddcCreate.setPassword(EncryptionGuard.encode(sddcCreate.getPassword()));
      sddcRepository.save(sddcCreate);
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
              fieldWithPath("id").description("ID of FacilitySoftwareConfig, created by flowgate"),
              fieldWithPath("name").description("The facilitySoftware name."),
              fieldWithPath("description").description("The facilitySoftware description."),
              fieldWithPath("userName").description(
                    "An username used to obtain authorization"),
              fieldWithPath("password").description(
                    " A password used to obtain authorization."),
              fieldWithPath("serverURL").description(
                      "The server's address, it can be an IP or FQDN."),
              fieldWithPath("type").description(
                      "A type for facilitySoftware,forExample Nlyte,PowerIQ,Device42,OtherDCIM or OtherCMDB").type(SoftwareType.class).optional(),
              fieldWithPath("userId").description(
                      "userId"),
              fieldWithPath("subCategory").description(
                    "subCategory"),
              fieldWithPath("verifyCert").description(
                      "Whether to verify the certificate when accessing the serverURL.").type(JsonFieldType.BOOLEAN),
              subsectionWithPath("integrationStatus").description("The status of integration.").optional(),
      };
      MvcResult result = this.mockMvc.perform(get("/v1/sddc/type/{type}", SoftwareType.VCENTER))
            .andExpect(status().isOk())
            .andDo(document("SDDCSoftware-queryByType-example",pathParameters(
                  parameterWithName("type").description("The type of SDDCSoftwareConfig, Sample value: VRO/VCENTER")),
                    responseFields(
                    fieldWithPath("[]").description("An array of SDDCSoftwareConfig.")
                    ).andWithPrefix("[].", fieldpath)))
            .andReturn();
      ObjectMapper mapper = new ObjectMapper();
      String res = result.getResponse().getContentAsString();
      SDDCSoftwareConfig [] sddcs = mapper.readValue(res, SDDCSoftwareConfig[].class);
      for(SDDCSoftwareConfig sddc:sddcs) {
         if(sddc.getName().equals("flowgate")) {
            TestCase.assertEquals("flowgate cluster", sddc.getDescription());
         }
      }
      sddcRepository.deleteById(sddcCreate.getId());
   }

   @Test
   public void getVROServerConfigsExample() throws Exception {
      SDDCSoftwareConfig sddc1Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddc1Create.setType(SoftwareType.VRO);
      sddc1Create.setName("flowgate");
      sddc1Create.setDescription("flowgate cluster");
      SDDCSoftwareConfig sddc2Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddc2Create.setType(SoftwareType.VRO);
      sddc1Create.setPassword(EncryptionGuard.encode(sddc1Create.getPassword()));
      sddc2Create.setPassword(EncryptionGuard.encode(sddc2Create.getPassword()));
      sddcRepository.save(sddc1Create);
      sddcRepository.save(sddc2Create);
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
              fieldWithPath("id").description("ID of SDDCSoftwareConfig, created by flowgate"),
              fieldWithPath("name").description("The SDDCSoftwareConfig name."),
              fieldWithPath("description").description("The SDDCSoftwareConfig description."),
              fieldWithPath("userName").description(
                    "An username used to obtain authorization"),
              fieldWithPath("password").description(
                    " A password used to obtain authorization."),
              fieldWithPath("serverURL").description(
                      "The server's address, it can be an IP or FQDN."),
              fieldWithPath("type").description(
                      "A type for SDDCSoftwareConfig,forExample VRO, VCENTER, OTHERS, VROPSMP").type(SoftwareType.class).optional(),
              fieldWithPath("userId").description(
                      "userId"),
              fieldWithPath("subCategory").description(
                    "subCategory"),
              fieldWithPath("verifyCert").description(
                      "Whether to verify the certificate when accessing the serverURL.").type(JsonFieldType.BOOLEAN),
              subsectionWithPath("integrationStatus").description("The status of integration.").optional()
              };
      MvcResult result = this.mockMvc.perform(get("/v1/sddc/vrops"))
            .andExpect(status().isOk())
            .andDo(document("SDDCSoftware-getVROServerConfigs-example", responseFields(
                    fieldWithPath("[]").description("An array of Aria Operations server configs"))
                    .andWithPrefix("[].", fieldpath)))
            .andReturn();
      ObjectMapper mapper = new ObjectMapper();
      String res = result.getResponse().getContentAsString();
      SDDCSoftwareConfig [] sddcs = mapper.readValue(res, SDDCSoftwareConfig[].class);
      for(SDDCSoftwareConfig sddc:sddcs) {
         if(sddc.getName().equals("flowgate")) {
            TestCase.assertEquals("flowgate cluster", sddc.getDescription());
         }
      }
      sddcRepository.deleteById(sddc1Create.getId());
      sddcRepository.deleteById(sddc2Create.getId());
   }

   @Test
   public void getVROServerConfigsByUserExample() throws Exception {
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
       SDDCSoftwareConfig sddc1Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
       sddc1Create.setType(SoftwareType.VRO);
       sddc1Create.setName("flowgate");
       sddc1Create.setDescription("flowgate cluster");
       SDDCSoftwareConfig sddc2Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
       sddc2Create.setType(SoftwareType.VRO);
       sddc1Create.setPassword(EncryptionGuard.encode(sddc1Create.getPassword()));
       sddc2Create.setPassword(EncryptionGuard.encode(sddc2Create.getPassword()));
       sddcRepository.save(sddc1Create);
       sddcRepository.save(sddc2Create);
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
              fieldWithPath("id").description("ID of SDDCSoftwareConfig, created by flowgate"),
              fieldWithPath("name").description("The SDDCSoftwareConfig name."),
              fieldWithPath("description").description("The SDDCSoftwareConfig description."),
              fieldWithPath("userName").description(
                    "An username used to obtain authorization"),
              fieldWithPath("password").description(
                    " A password used to obtain authorization."),
              fieldWithPath("serverURL").description(
                      "The server's address, it can be an IP or FQDN."),
              fieldWithPath("type").description(
                      "A type for SDDCSoftwareConfig,forExample VRO, VCENTER, OTHERS, VROPSMP").type(SoftwareType.class).optional(),
              fieldWithPath("userId").description(
                      "userId"),
              fieldWithPath("subCategory").description(
                    "subCategory"),
              fieldWithPath("verifyCert").description(
                      "Whether to verify the certificate when accessing the serverURL.").type(JsonFieldType.BOOLEAN),
              subsectionWithPath("integrationStatus").description("The status of integration.").optional()
              };
      MvcResult result = this.mockMvc.perform(get("/v1/sddc/user/vrops"))
            .andExpect(status().isOk())
            .andDo(document("SDDCSoftware-getVROServerConfigsByUser-example", responseFields(
                    fieldWithPath("[]").description("An array of Aria Operations server configs"))
                    .andWithPrefix("[].", fieldpath)))
            .andReturn();
      String res = result.getResponse().getContentAsString();
      ObjectMapper mapper = new ObjectMapper();
      SDDCSoftwareConfig [] sddcs = mapper.readValue(res, SDDCSoftwareConfig[].class);
      for(SDDCSoftwareConfig sddc:sddcs) {
         if(sddc.getName().equals("flowgate")) {
            TestCase.assertEquals("flowgate cluster", sddc.getDescription());
         }
      }
      sddcRepository.deleteById(sddc1Create.getId());
      sddcRepository.deleteById(sddc2Create.getId());
   }
   @Test
   public void getVCServerConfigsExample() throws Exception {
      SDDCSoftwareConfig sddc1Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddc1Create.setType(SoftwareType.VCENTER);
      sddc1Create.setName("flowgate");
      sddc1Create.setDescription("flowgate cluster");
      SDDCSoftwareConfig sddc2Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddc2Create.setType(SoftwareType.VCENTER);
      sddc1Create.setPassword(EncryptionGuard.encode(sddc1Create.getPassword()));
      sddc2Create.setPassword(EncryptionGuard.encode(sddc2Create.getPassword()));
      sddcRepository.save(sddc1Create);
      sddcRepository.save(sddc2Create);
      FieldDescriptor[] fieldpath = new FieldDescriptor[] {
              fieldWithPath("id").description("ID of SDDCSoftwareConfig, created by flowgate"),
              fieldWithPath("name").description("The SDDCSoftwareConfig name."),
              fieldWithPath("description").description("The SDDCSoftwareConfig description."),
              fieldWithPath("userName").description(
                    "An username used to obtain authorization"),
              fieldWithPath("password").description(
                    " A password used to obtain authorization."),
              fieldWithPath("serverURL").description(
                      "The server's address, it can be an IP or FQDN."),
              fieldWithPath("type").description(
                      "A type for SDDCSoftwareConfig,forExample VRO, VCENTER, OTHERS, VROPSMP").type(SoftwareType.class).optional(),
              fieldWithPath("userId").description(
                      "userId"),
              fieldWithPath("subCategory").description(
                    "subCategory"),
              fieldWithPath("verifyCert").description(
                      "Whether to verify the certificate when accessing the serverURL.").type(JsonFieldType.BOOLEAN),
              subsectionWithPath("integrationStatus").description("The status of integration.").optional()
              };

      MvcResult result = this.mockMvc.perform(get("/v1/sddc/vc"))
            .andExpect(status().isOk())
            .andDo(document("SDDCSoftware-getVCServerConfigs-example", responseFields(
                    fieldWithPath("[]").description("An array of vCenter server configs"))
                    .andWithPrefix("[].", fieldpath))).andReturn();
      String res = result.getResponse().getContentAsString();

      ObjectMapper mapper = new ObjectMapper();
      SDDCSoftwareConfig [] sddcs = mapper.readValue(res, SDDCSoftwareConfig[].class);
      for(SDDCSoftwareConfig sddc:sddcs) {
         if(sddc.getName().equals("flowgate")) {
            TestCase.assertEquals("flowgate cluster", sddc.getDescription());
         }
      }
      sddcRepository.deleteById(sddc1Create.getId());
      sddcRepository.deleteById(sddc2Create.getId());
   }

   @Test
   public void testGetInternalServerConfigsByType() throws Exception {
      SDDCSoftwareConfig sddc1Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddc1Create.setType(SoftwareType.VCENTER);
      sddc1Create.setName("flowgate");
      sddc1Create.setDescription("flowgate cluster");
      SDDCSoftwareConfig sddc2Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddc2Create.setType(SoftwareType.VCENTER);
      sddc1Create.setPassword(EncryptionGuard.encode(sddc1Create.getPassword()));
      sddc2Create.setPassword(EncryptionGuard.encode(sddc2Create.getPassword()));
      sddcRepository.save(sddc1Create);
      sddcRepository.save(sddc2Create);

      MvcResult result = this.mockMvc.perform(get("/v1/sddc/internal/type/" + SoftwareType.VCENTER))
               .andExpect(status().isOk()).andReturn();
      String res = result.getResponse().getContentAsString();

      ObjectMapper mapper = new ObjectMapper();
      SDDCSoftwareConfig [] sddcs = mapper.readValue(res, SDDCSoftwareConfig[].class);
      for(SDDCSoftwareConfig sddc:sddcs) {
         if(sddc.getName().equals("flowgate")) {
            TestCase.assertEquals("flowgate cluster", sddc.getDescription());
         }
      }
      sddcRepository.deleteById(sddc1Create.getId());
      sddcRepository.deleteById(sddc2Create.getId());
   }

   @Test
   public void sDDCSoftwareDeleteVcenterExample() throws Exception {
      SDDCSoftwareConfig sddc = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddcRepository.save(sddc);
      ServerMapping serverMapping1 = createServerMapping(SoftwareType.VCENTER, sddc.getId());
      serverMappingRepository.save(serverMapping1);
      ServerMapping serverMapping2 = createServerMapping(SoftwareType.VCENTER, sddc.getId());
      serverMappingRepository.save(serverMapping2);
      this.mockMvc
            .perform(
                  delete("/v1/sddc/{Id}", sddc.getId()))
            .andExpect(status().isOk()).andDo(document("SDDCSoftware-delete-example", pathParameters(
                  parameterWithName("Id").description("The id of SDDCSoftwareConfig, generated by flowgate."))));

      Optional<ServerMapping> serverMappingOptional1 = serverMappingRepository.findById(serverMapping1.getId());
      TestCase.assertEquals(false, serverMappingOptional1.isPresent());
      Optional<ServerMapping> serverMappingOptional2 = serverMappingRepository.findById(serverMapping2.getId());
      TestCase.assertEquals(false, serverMappingOptional2.isPresent());
      Optional<SDDCSoftwareConfig> sddcOptional = sddcRepository.findById(sddc.getId());
      TestCase.assertEquals(false, sddcOptional.isPresent());

   }

   @Test
   public void sDDCSoftwareDeleteVroAndVropsMpExample() throws Exception {
      SDDCSoftwareConfig sddc = createSDDCSoftwareConfig(SoftwareType.VRO);
      sddcRepository.save(sddc);
      ServerMapping serverMapping1 = createServerMapping(SoftwareType.VRO, sddc.getId());
      serverMappingRepository.save(serverMapping1);
      ServerMapping serverMapping2 = createServerMapping(SoftwareType.VRO, sddc.getId());
      serverMappingRepository.save(serverMapping2);
      this.mockMvc
            .perform(
                  delete("/v1/sddc/" + sddc.getId()))
            .andExpect(status().isOk());

      Optional<ServerMapping> serverMappingOptional1 = serverMappingRepository.findById(serverMapping1.getId());
      TestCase.assertEquals(false, serverMappingOptional1.isPresent());
      Optional<ServerMapping> serverMappingOptional2 = serverMappingRepository.findById(serverMapping2.getId());
      TestCase.assertEquals(false, serverMappingOptional2.isPresent());
      Optional<SDDCSoftwareConfig> sddcOptional = sddcRepository.findById(sddc.getId());
      TestCase.assertEquals(false, sddcOptional.isPresent());
   }

   @Test
   public void sDDCSoftwareDeleteOtherExample() throws Exception {
      SDDCSoftwareConfig sddc = createSDDCSoftwareConfig(SoftwareType.OTHERS);
      sddcRepository.save(sddc);
      this.mockMvc
            .perform(
                  delete("/v1/sddc/" + sddc.getId()))
            .andExpect(status().isOk());

      Optional<SDDCSoftwareConfig> sddcOptional = sddcRepository.findById(sddc.getId());
      TestCase.assertEquals(false, sddcOptional.isPresent());
   }

   @Test
   public void sDDCSoftwareDeleteExceptionExample() throws Exception {

	   MvcResult result = this.mockMvc
            .perform(
                  delete("/v1/sddc/" + "10"))
            .andExpect(status().isNotFound()).andReturn();
	   TestCase.assertEquals("Failed to find sddc with field: id  and value: 10", result.getResolvedException().getMessage());
   }

   ServerMapping createServerMapping(SoftwareType type, String sddcId){
	   ServerMapping serverMapping = new ServerMapping();
	   switch(type) {
		  case VCENTER:
			  serverMapping.setVcID(sddcId);
			  serverMapping.setId(UUID.randomUUID().toString());
			  break;
		  case VRO:
		  case VROPSMP:
			  serverMapping.setVroID(sddcId);
			  serverMapping.setId(UUID.randomUUID().toString());
			  break;
		default:
			break;
	   }
	   return serverMapping;
   }

   SDDCSoftwareConfig createSDDCSoftwareConfig(SoftwareType type) {
      SDDCSoftwareConfig example = new SDDCSoftwareConfig();
      example.setId(UUID.randomUUID().toString());
      example.setName("test server");
      example.setServerURL("10.160.30.134");
      example.setType(type);
      example.setUserName("administrator@vsphere.local");
      example.setPassword("Admin!23");
      example.setUserId("1001");
      example.setVerifyCert(false);
      example.setDescription("description");
      IntegrationStatus integrationStatus = new IntegrationStatus();
      example.setIntegrationStatus(integrationStatus);
      return example;
   }

   WormholeUserDetails createuser() {
      WormholeUserDetails user = new WormholeUserDetails();
      user.setUserId("1001");
      return user;
   }
}
