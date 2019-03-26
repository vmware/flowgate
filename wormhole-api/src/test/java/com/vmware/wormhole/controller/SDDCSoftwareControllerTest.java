/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.controller;

import static org.hamcrest.CoreMatchers.equalTo;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.vmware.wormhole.auth.AuthVcUser;
import com.vmware.wormhole.common.model.SDDCSoftwareConfig;
import com.vmware.wormhole.common.model.SDDCSoftwareConfig.SoftwareType;
import com.vmware.wormhole.common.model.redis.message.MessagePublisher;
import com.vmware.wormhole.exception.WormholeRequestException;
import com.vmware.wormhole.repository.SDDCSoftwareRepository;
import com.vmware.wormhole.security.service.AccessTokenService;
import com.vmware.wormhole.service.ServerValidationService;
import com.vmware.wormhole.util.WormholeUserDetails;

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
      sddc.setId("temporary_id");
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
               .perform(post("/v1/sddc/").contentType(MediaType.APPLICATION_JSON)
                     .content(objectMapper.writeValueAsString(sddc)))
               .andDo(document("SDDCSoftware-create-example", requestFields(
                     fieldWithPath("id")
                           .description("ID of the SDDCSoftwareConfig, created by wormhole"),
                     fieldWithPath("name").description("The SDDCSoftwareConfig name."),
                     fieldWithPath("description").description(""),
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
                     fieldWithPath("userId").description(""),
                     fieldWithPath("verifyCert").description(
                           "Whether to verify the certificate when accessing the serverURL."),
                     fieldWithPath("integrationStatus").description("The status of integration."))))
               .andReturn();
         TestCase.assertEquals(201, result.getResponse().getStatus());
      } catch (Exception e) {
         TestCase.fail();
      }
      sddcRepository.delete(sddc.getId());
   }
   @Test
   public void syncSDDCServerDataExample() throws JsonProcessingException, Exception {
      
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      SDDCSoftwareConfig sddcCreate = createSDDCSoftwareConfig(SoftwareType.VCENTER);
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
      sddcRepository.delete(sddc.getId());
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
                           .description("ID of the SDDCSoftwareConfig, created by wormhole"),
                     fieldWithPath("name").description("The SDDCSoftwareConfig name."),
                     fieldWithPath("description").description(""),
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
                     fieldWithPath("userId").description(""),
                     fieldWithPath("verifyCert").description(
                           "Whether to verify the certificate when accessing the serverURL."),
                     fieldWithPath("integrationStatus").description("The status of integration."))))
               .andReturn();
         if (result.getResolvedException() != null) {
            throw result.getResolvedException();
         }
      } catch (Exception e) {
         throw e;
      } finally {
         sddcRepository.delete(sddc.getId());
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
      sddcRepository.delete(sddc.getId());
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
      MvcResult result = this.mockMvc
            .perform(put("/v1/sddc/").contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(sddc)))
            .andExpect(status().isOk())
            .andReturn();
      TestCase.assertEquals(200, result.getResponse().getStatus());
      sddcRepository.delete(sddc.getId());
   }

   @Test
   public void SDDCSoftwareQueryByPageExample() throws Exception {
      SDDCSoftwareConfig sddcCreate = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      SDDCSoftwareConfig sddc = sddcRepository.save(sddcCreate);
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      int pageNumber = 1;
      int pageSize = 5;
      this.mockMvc
            .perform(get("/v1/sddc/page/" + pageNumber + "/pagesize/" + pageSize + ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$..content[0].name").value(sddc.getName()))
            .andExpect(jsonPath("$..content[0].userId").value(sddc.getUserId()))
            .andDo(document("SDDCSoftware-query-example",responseFields(
                    fieldWithPath("content").description("SDDCSoftwareConfig's array."),
                    fieldWithPath("totalPages").description("content's total pages."),
                      fieldWithPath("totalElements").description("content's total elements."),
                      fieldWithPath("last").description("Is the last."),
                      fieldWithPath("number").description("The page number."),
                      fieldWithPath("size").description("The page size."),
                      fieldWithPath("sort").description("The sort."),
                      fieldWithPath("numberOfElements").description("The number of Elements."),
                      fieldWithPath("first").description("Is the first.")
                     )));
      sddcRepository.delete(sddc.getId());
   }

   @Test
   public void SDDCSoftwareQueryByTypeAndUserId() throws Exception {
      SDDCSoftwareConfig sddcCreate = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddcCreate.setType(SoftwareType.VCENTER);
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      sddcCreate.setId("1");
      SDDCSoftwareConfig sddc = sddcRepository.save(sddcCreate);
      String type = "VCENTER";
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
      this.mockMvc.perform(get("/v1/sddc/type/" + type + ""))
            .andExpect(status().isOk())
            .andExpect(content()
                  .string(equalTo("[{\"id\":\"1\",\"name\":\"test server\",\"description\":null,"
                        + "\"userName\":\"administrator@vsphere.local\","
                        + "\"password\":\"Admin!23\",\"serverURL\":\"10.160.30.134\",\"type\":\"VCENTER\","
                        + "\"userId\":\"1001\",\"verifyCert\":false,\"integrationStatus\":null}]")))
            .andDo(document("SDDCSoftware-queryByType-example", 
                    responseFields(
                    fieldWithPath("[]").description("An array of SDDCSoftwareConfig.")
                    ).andWithPrefix("[].", fieldpath)
                    ));

      sddcRepository.delete(sddc.getId());
   }
   
   @Test
   public void getVROServerConfigsExample() throws Exception {
      SDDCSoftwareConfig sddc1Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddc1Create.setId("1");
      sddc1Create.setType(SoftwareType.VRO);
      SDDCSoftwareConfig sddc2Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddc2Create.setType(SoftwareType.VRO);
      sddc2Create.setId("2");
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      
      SDDCSoftwareConfig sddc1 = sddcRepository.save(sddc1Create);
      SDDCSoftwareConfig sddc2 = sddcRepository.save(sddc2Create);
      
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
      
      this.mockMvc.perform(get("/v1/sddc/vrops"))
            .andExpect(status().isOk())
            .andExpect(content().string(equalTo("[{\"id\":\"1\",\"name\":\"test server\",\"description\":null,"
                        + "\"userName\":\"administrator@vsphere.local\","
                        + "\"password\":\"Admin!23\",\"serverURL\":\"10.160.30.134\",\"type\":\"VRO\","
                        + "\"userId\":\"1001\",\"verifyCert\":false,\"integrationStatus\":null},{\"id\":\"2\",\"name\":\"test server\",\"description\":null,"
                        + "\"userName\":\"administrator@vsphere.local\","
                        + "\"password\":\"Admin!23\",\"serverURL\":\"10.160.30.134\",\"type\":\"VRO\","
                        + "\"userId\":\"1001\",\"verifyCert\":false,\"integrationStatus\":null}]")))
            .andDo(document("SDDCSoftware-getVROServerConfigs-example", responseFields(
                    fieldWithPath("[]").description("An array of asserts"))
                    .andWithPrefix("[].", fieldpath)));

      sddcRepository.delete(sddc1.getId());
      sddcRepository.delete(sddc2.getId());
   }
   
   @Test
   public void getVROServerConfigsByUserExample() throws Exception {
       SDDCSoftwareConfig sddc1Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
       sddc1Create.setId("1");
       sddc1Create.setType(SoftwareType.VRO);
       SDDCSoftwareConfig sddc2Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
       sddc2Create.setType(SoftwareType.VRO);
       sddc2Create.setId("2");
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      
      SDDCSoftwareConfig sddc1 = sddcRepository.save(sddc1Create);
      SDDCSoftwareConfig sddc2 = sddcRepository.save(sddc2Create);
      
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
      
      this.mockMvc.perform(get("/v1/sddc/user/vrops"))
            .andExpect(status().isOk())
            .andExpect(content().string(equalTo("[{\"id\":\"1\",\"name\":\"test server\",\"description\":null,"
                        + "\"userName\":\"administrator@vsphere.local\","
                        + "\"password\":\"Admin!23\",\"serverURL\":\"10.160.30.134\",\"type\":\"VRO\","
                        + "\"userId\":\"1001\",\"verifyCert\":false,\"integrationStatus\":null},{\"id\":\"2\",\"name\":\"test server\",\"description\":null,"
                        + "\"userName\":\"administrator@vsphere.local\","
                        + "\"password\":\"Admin!23\",\"serverURL\":\"10.160.30.134\",\"type\":\"VRO\","
                        + "\"userId\":\"1001\",\"verifyCert\":false,\"integrationStatus\":null}]")))
            .andDo(document("SDDCSoftware-getVROServerConfigsByUser-example", responseFields(
                    fieldWithPath("[]").description("An array of asserts"))
                    .andWithPrefix("[].", fieldpath)));

      sddcRepository.delete(sddc1.getId());
      sddcRepository.delete(sddc2.getId());
   }
   @Test
   public void getVCServerConfigsExample() throws Exception {
      SDDCSoftwareConfig sddc1Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddc1Create.setId("1");
      sddc1Create.setType(SoftwareType.VCENTER);
      SDDCSoftwareConfig sddc2Create = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddc2Create.setType(SoftwareType.VCENTER);
      sddc2Create.setId("2");
      Mockito.doReturn(createuser()).when(tokenService).getCurrentUser(any());
      
      SDDCSoftwareConfig sddc1 = sddcRepository.save(sddc1Create);
      SDDCSoftwareConfig sddc2 = sddcRepository.save(sddc2Create);
      
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
      
      this.mockMvc.perform(get("/v1/sddc/vc"))
            .andExpect(status().isOk())
            .andExpect(content().string(equalTo("[{\"id\":\"1\",\"name\":\"test server\",\"description\":null,"
                        + "\"userName\":\"administrator@vsphere.local\","
                        + "\"password\":\"Admin!23\",\"serverURL\":\"10.160.30.134\",\"type\":\"VCENTER\","
                        + "\"userId\":\"1001\",\"verifyCert\":false,\"integrationStatus\":null},{\"id\":\"2\",\"name\":\"test server\",\"description\":null,"
                        + "\"userName\":\"administrator@vsphere.local\","
                        + "\"password\":\"Admin!23\",\"serverURL\":\"10.160.30.134\",\"type\":\"VCENTER\","
                        + "\"userId\":\"1001\",\"verifyCert\":false,\"integrationStatus\":null}]")))
            .andDo(document("SDDCSoftware-getVCServerConfigs-example", responseFields(
                    fieldWithPath("[]").description("An array of asserts"))
                    .andWithPrefix("[].", fieldpath)));

      sddcRepository.delete(sddc1.getId());
      sddcRepository.delete(sddc2.getId());
   }

   @Test
   public void sDDCSoftwareDeleteExample() throws Exception {
      SDDCSoftwareConfig sddc = createSDDCSoftwareConfig(SoftwareType.VCENTER);
      sddcRepository.save(sddc);
      this.mockMvc
            .perform(
                  delete("/v1/sddc/" + sddc.getId()))
            .andExpect(status().isOk()).andDo(document("SDDCSoftware-delete-example"));

      sddcRepository.delete(sddc.getId());
   }

   SDDCSoftwareConfig createSDDCSoftwareConfig(SoftwareType type) {
      SDDCSoftwareConfig example = new SDDCSoftwareConfig();
      example.setName("test server");
      example.setServerURL("10.160.30.134");
      example.setType(type);
      example.setUserName("administrator@vsphere.local");
      example.setPassword("Admin!23");
      example.setUserId("1001");
      example.setVerifyCert(false);
      return example;
   }

   WormholeUserDetails createuser() {
      WormholeUserDetails user = new WormholeUserDetails();
      user.setUserId("1001");
      return user;
   }
}
