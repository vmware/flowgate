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
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.Cookie;

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.AuthToken;
import com.vmware.flowgate.common.model.WormholeRole;
import com.vmware.flowgate.common.model.WormholeUser;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.RoleRepository;
import com.vmware.flowgate.repository.UserRepository;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.security.service.UserDetailsServiceImpl;
import com.vmware.flowgate.util.JwtTokenUtil;
import com.vmware.flowgate.util.WormholeUserDetails;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class AuthControllerTest {

   @Rule
   public final JUnitRestDocumentation restDocumentation =
         new JUnitRestDocumentation("target/generated-snippets");

   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private WebApplicationContext context;

   @Autowired
   private RoleRepository roleRepository;

   @SpyBean
   private AccessTokenService tokenService;

   @SpyBean
   private UserDetailsServiceImpl userDetailservice;

   @MockBean
   private StringRedisTemplate template;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private JwtTokenUtil jwtUtil;

   @Rule
   public ExpectedException expectedEx = ExpectedException.none();

   @Before
   public void setUp() {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation)).build();
   }

   @Test
   public void testCreateToken() throws Exception {
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      WormholeUser wormholeuser = createUser();
      wormholeuser.setUserName("tom");
      wormholeuser.setPassword("$2a$10$Vm8MLIkGwinuICfcqW5RDOoE.aJqnvsaPhnxl7.N4H7oLKVIu3o0.");
      wormholeuser.setRoleNames(Arrays.asList("admin"));
      userRepository.save(wormholeuser);
      this.mockMvc
      .perform(post("/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
      .content("{\"userName\":\"tom\",\"password\":\"123456\"}"))
      .andExpect(status().isOk())
      .andDo(document("AuthController-CreateAccessToken-example", relaxedRequestFields(
      fieldWithPath("userName").description("A user name for flowgate Project"),
      fieldWithPath("password").description("A password for flowgate Project."))))
      .andReturn();
      userRepository.deleteById(wormholeuser.getId());
   }

   @Test
   public void testCreateToken1() throws Exception {
      WormholeUser user = null;
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Invalid username or password");
      MvcResult result =  this.mockMvc
      .perform(post("/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(user)))
      .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      }
   }

   @Test
   public void testCreateTokenUseAdapterServiceKey() throws Exception {
      WormholeUser user = null;
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Invalid username or password");
      MvcResult result =  this.mockMvc
      .perform(post("/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(user)))
      .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      }
   }

   @Test
   public void testRefreshToken() throws Exception {
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      WormholeUser wormholeuser = createUser();
      String userId = UUID.randomUUID().toString();
      wormholeuser.setId(userId);
      wormholeuser.setUserName("tomRefresh");
      wormholeuser.setPassword("$2a$10$Vm8MLIkGwinuICfcqW5RDOoE.aJqnvsaPhnxl7.N4H7oLKVIu3o0.");
      wormholeuser.setRoleNames(Arrays.asList("admin"));
      userRepository.save(wormholeuser);
      MvcResult result = this.mockMvc
      .perform(post("/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
      .content("{\"userName\":\"tomRefresh\",\"password\":\"123456\"}"))
      .andExpect(status().isOk())
      .andReturn();
      String access_token = "";
      Cookie[] cookies =  result.getResponse().getCookies();
      if(cookies != null && cookies.length!=0) {
         for(Cookie currentcookie:cookies) {
            if(JwtTokenUtil.Token_Name.equals(currentcookie.getName())) {
               access_token = currentcookie.getValue();
               break;
            }
         }
      }
      DecodedJWT jwtcre = jwtUtil.getDecodedJwt(access_token);
      ObjectMapper mapper = new ObjectMapper();
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId(userId);
      userdetail.setUsername("tomRefresh");
      userdetail.setPassword("$2a$10$Vm8MLIkGwinuICfcqW5RDOoE.aJqnvsaPhnxl7.N4H7oLKVIu3o0.");

      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      Mockito.doReturn(mapper.writeValueAsString(userdetail)).when(tokenService).getUserJsonString(any());
      MvcResult result1 = this.mockMvc.perform(get("/v1/auth/token/refresh").header("Authorization", "Bearer "+access_token))
      .andDo(document("AuthController-RefreshAccessToken-example"))
      .andReturn();

      //JwtTokenUtil jwtUtil = new JwtTokenUtil();
      DecodedJWT jwt = jwtUtil.getDecodedJwt(result1.getResponse().getHeader("Authorization"));
      TestCase.assertEquals("tomRefresh", jwt.getSubject());
      userRepository.deleteById(wormholeuser.getId());
   }

   @Test
   public void testRefreshToken1() throws Exception {
      AuthToken token = new AuthToken();
      Mockito.doReturn(token).when(tokenService).refreshToken(any());
      this.mockMvc.perform(get("/v1/auth/token/refresh"))
      .andExpect(status().isOk())
      .andReturn();
   }

   @Test
   public void testLogout() throws Exception {
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      this.mockMvc.perform(get("/v1/auth/logout").header("Authorization", "Bearer "+"R$TYUIMJ"))
            .andDo(document("AuthController-UserLogout-example"))
            .andExpect(status().isOk())
            .andReturn();
   }
   @Test
   public void createUserExample() throws Exception {
       WormholeUser user = createUser();
       this.mockMvc.perform(post("/v1/auth/user").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(user)))
              .andExpect(status().isCreated())
              .andDo(document("AuthController-createUser-example", requestFields(
                      fieldWithPath("id").description("ID of User, created by flowgate"),
                      fieldWithPath("userName").description("userName.").type(JsonFieldType.STRING),
                      fieldWithPath("gender").description("gender").type(JsonFieldType.NUMBER),
                      fieldWithPath("password").description("password").type(JsonFieldType.STRING),
                      fieldWithPath("mobile").description("mobile").type(JsonFieldType.STRING),
                      fieldWithPath("status").description("status").type(JsonFieldType.NUMBER),
                      fieldWithPath("createTime").description("createTime").type(Date.class),
                      fieldWithPath("emailAddress").description("emailAddress").type(JsonFieldType.STRING),
                      fieldWithPath("roleNames").description("roleNames").type(List.class),
                      fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
                      fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate").type(JsonFieldType.NUMBER))))
              .andReturn();
       userRepository.deleteById(user.getId());
   }
   @Test
   public void deleteUserExample() throws Exception {
       WormholeUser user = createUser();
       userRepository.save(user);

       this.mockMvc.perform(delete("/v1/auth/user/{id}",user.getId()))
              .andExpect(status().isOk())
              .andDo(document("AuthController-deleteUser-example",pathParameters(
                    parameterWithName("id").description("The id of user, generated by flowgate."))))
              .andReturn();
   }

   @Test
   public void updateUserExample() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      String userId = UUID.randomUUID().toString();
      userdetail.setUserId(userId);
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("admin");
      WormholeUser adminUser = new WormholeUser();
      adminUser.setUserName("admin");
      adminUser.setRoleNames(rolenames);
      adminUser.setId(userId);
      userRepository.save(adminUser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());

      WormholeUser user = createUser();
      userRepository.save(user);

      this.mockMvc
            .perform(put("/v1/auth/user").contentType(MediaType.APPLICATION_JSON_VALUE)
                  .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk())
            .andDo(document("AuthController-updateUser-example",
                  requestFields(fieldWithPath("id").description("ID of User, created by flowgate"),
                        fieldWithPath("userName").description("userName.")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("gender").description("gender").type(JsonFieldType.NUMBER),
                        fieldWithPath("password").description("password")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("mobile").description("mobile").type(JsonFieldType.STRING),
                        fieldWithPath("status").description("status").type(JsonFieldType.NUMBER),
                        fieldWithPath("createTime").description("createTime").type(Date.class),
                        fieldWithPath("emailAddress").description("emailAddress")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("roleNames").description("roleNames").type(List.class),
                        fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
                        fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate")
                              .type(JsonFieldType.NUMBER))))
            .andReturn();

      userRepository.deleteById(user.getId());
      userRepository.deleteById(adminUser.getId());
   }

   @Test
   public void updateUserExample1() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      String userId = UUID.randomUUID().toString();
      userdetail.setUserId(userId);
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("admin");
      WormholeUser adminUser = new WormholeUser();
      adminUser.setUserName("admin");
      adminUser.setRoleNames(rolenames);
      adminUser.setId(userId);
      userRepository.save(adminUser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());

      WormholeUser user = createUser();
      userRepository.save(user);

      this.mockMvc
            .perform(put("/v1/auth/user").contentType(MediaType.APPLICATION_JSON_VALUE)
                  .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk())
            .andDo(document("AuthController-updateUser-example",
                  requestFields(fieldWithPath("id").description("ID of User, created by flowgate"),
                        fieldWithPath("userName").description("userName.")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("gender").description("gender").type(JsonFieldType.NUMBER),
                        fieldWithPath("password").description("password")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("mobile").description("mobile").type(JsonFieldType.STRING),
                        fieldWithPath("status").description("status").type(JsonFieldType.NUMBER),
                        fieldWithPath("createTime").description("createTime").type(Date.class),
                        fieldWithPath("emailAddress").description("emailAddress")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("roleNames").description("roleNames").type(List.class),
                        fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
                        fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate")
                              .type(JsonFieldType.NUMBER))))
            .andReturn();

      userRepository.deleteById(user.getId());
      userRepository.deleteById(adminUser.getId());
   }

   @Test
   public void readOneUserByIdExample() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      String userId = UUID.randomUUID().toString();
      userdetail.setUserId(userId);
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("admin");
      WormholeUser adminUser = new WormholeUser();
      adminUser.setRoleNames(rolenames);
      adminUser.setId(userId);
      userRepository.save(adminUser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      WormholeUser user = createUser();
      userRepository.save(user);

      this.mockMvc.perform(get("/v1/auth/user/{userId}",user.getId()))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readOneUserById-example",pathParameters(
                    parameterWithName("userId").description("The id of user, generated by flowgate.")),
                    responseFields(
                      fieldWithPath("id").description("ID of User, created by flowgate"),
                      fieldWithPath("userName").description("userName.").type(JsonFieldType.STRING),
                      fieldWithPath("gender").description("gender").type(JsonFieldType.NUMBER),
                      fieldWithPath("password").description("password"),
                      fieldWithPath("mobile").description("mobile").type(JsonFieldType.STRING),
                      fieldWithPath("status").description("status").type(JsonFieldType.NUMBER),
                      fieldWithPath("createTime").description("createTime").type(Date.class),
                      fieldWithPath("emailAddress").description("emailAddress").type(JsonFieldType.STRING),
                      fieldWithPath("roleNames").description("roleNames").type(List.class),
                      fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
                      fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate").type(JsonFieldType.NUMBER))))
              .andReturn();

       userRepository.deleteById(user.getId());
       userRepository.deleteById(adminUser.getId());
   }

   @Test
   public void readOneUserByIdExample1() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      String userId = UUID.randomUUID().toString();
      userdetail.setUserId(userId);
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("sysuser");
      WormholeUser sysuser = new WormholeUser();
      sysuser.setRoleNames(rolenames);
      sysuser.setId(userId);
      userRepository.save(sysuser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      this.mockMvc.perform(get("/v1/auth/user/"+sysuser.getId()+""))
              .andExpect(status().isOk())
              .andReturn();
      userRepository.deleteById(sysuser.getId());
   }

   @Test
   public void readOneUserByIdExample2() throws Exception {
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Forbidden");
      String userId = UUID.randomUUID().toString();
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId(userId);
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("sysuser");
      WormholeUser sysuser = new WormholeUser();
      sysuser.setRoleNames(rolenames);
      sysuser.setId(userId);
      userRepository.save(sysuser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      MvcResult result = this.mockMvc.perform(get("/v1/auth/user/123"))
              .andExpect(status().isForbidden())
              .andReturn();
      if (result.getResolvedException() != null) {
         userRepository.deleteById(userId);
         throw result.getResolvedException();
      }
   }

   @Test
   public void readOneUserByNameExample() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      String userId = UUID.randomUUID().toString();
      userdetail.setUserId(userId);
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("admin");
      WormholeUser adminUser = new WormholeUser();
      adminUser.setRoleNames(rolenames);
      adminUser.setId(userId);
      userRepository.save(adminUser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      WormholeUser user = createUser();
      userRepository.save(user);
      this.mockMvc.perform(get("/v1/auth/user/username/{userName}",user.getUserName()))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readOneUserByUserName-example",pathParameters(
                    parameterWithName("userName").description("The name of user")),
                    responseFields(
                    fieldWithPath("id").description("ID of User, created by flowgate"),
                    fieldWithPath("userName").description("userName.").type(JsonFieldType.STRING),
                    fieldWithPath("gender").description("gender").type(JsonFieldType.NUMBER),
                    fieldWithPath("password").description("password"),
                    fieldWithPath("mobile").description("mobile").type(JsonFieldType.STRING),
                    fieldWithPath("status").description("status").type(JsonFieldType.NUMBER),
                    fieldWithPath("createTime").description("createTime").type(Date.class),
                    fieldWithPath("emailAddress").description("emailAddress").type(JsonFieldType.STRING),
                    fieldWithPath("roleNames").description("roleNames").type(List.class),
                    fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
                    fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate").type(JsonFieldType.NUMBER))))
              .andReturn();
      userRepository.deleteById(userId);
      userRepository.deleteById(user.getId());
   }

   @Test
   public void readOneUserByUserNameExample1() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      String userId = UUID.randomUUID().toString();
      userdetail.setUserId(userId);
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("sysuser");
      WormholeUser sysuser = new WormholeUser();
      sysuser.setRoleNames(rolenames);
      sysuser.setId(userId);
      sysuser.setUserName("lucy");
      userRepository.save(sysuser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      this.mockMvc.perform(get("/v1/auth/user/username/"+sysuser.getUserName()+""))
              .andExpect(status().isOk())
              .andReturn();
      userRepository.deleteById(sysuser.getId());
   }

   @Test
   public void readOneUserByUserNameExample2() throws Exception {
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Forbidden");
      String userId = UUID.randomUUID().toString();
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId(userId);
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("sysuser");
      WormholeUser sysuser = createUser();
      sysuser.setId(userId);
      sysuser.setRoleNames(rolenames);
      sysuser.setUserName("lucy");
      userRepository.save(sysuser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      MvcResult result = this.mockMvc.perform(get("/v1/auth/user/username/tom"))
              .andExpect(status().isForbidden())
              .andReturn();
      if (result.getResolvedException() != null) {
         userRepository.deleteById(sysuser.getId());
         throw result.getResolvedException();
      }
   }

   @Test
   public void readUsersByPageExample() throws Exception {
       WormholeUser user1 = createUser();
       userRepository.save(user1);
       this.mockMvc.perform(get("/v1/auth/user")
              .param("currentPage", "1").param("pageSize", "5"))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readUsersByPage-example", requestParameters(
                    parameterWithName("currentPage").description("The page you want to get"),
                    parameterWithName("pageSize").description("The number of users you want to get by every request.Default value: 20"))));
       userRepository.deleteById(user1.getId());
   }

   @Test
   public void createRoleExample() throws Exception {
       WormholeRole role = createRole();
       this.mockMvc.perform(post("/v1/auth/role").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(role)))
              .andExpect(status().isCreated())
              .andDo(document("AuthController-createRole-example", requestFields(
                      fieldWithPath("id").description("ID of role, created by flowgate"),
                      fieldWithPath("roleName").description("roleName."),
                      fieldWithPath("privilegeNames").description("list of privilegeNames").type(List.class))));
       roleRepository.deleteById(role.getId());
   }

   @Test
   public void readOneRoleByIdExample() throws Exception {
       WormholeRole role = createRole();
       roleRepository.save(role);
       this.mockMvc.perform(get("/v1/auth/role/{roleId}", role.getId()))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readOneRoleById-example", pathParameters(
                    parameterWithName("roleId").description("The id of role,generated by flowgate.")),
                    responseFields(
                      fieldWithPath("id").description("ID of role, created by flowgate"),
                      fieldWithPath("roleName").description("roleName."),
                      fieldWithPath("privilegeNames").description("list of privilegeNames").type(List.class))));

       roleRepository.deleteById(role.getId());
   }
   @Test
   public void readRoleByPageExample() throws Exception {
       WormholeRole role = createRole();
       roleRepository.save(role);
       this.mockMvc.perform(get("/v1/auth/role")
               .param("currentPage", "1").param("pageSize", "5"))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readRoleByPage-example", requestParameters(
                    parameterWithName("currentPage").description("The page you want to get"),
                    parameterWithName("pageSize").description("The number of users you want to get by every request.Default value: 20"))));


       roleRepository.deleteById(role.getId());
   }

   @Test
   public void deleteRoleExample() throws Exception {
       WormholeRole role = createRole();
       roleRepository.save(role);
       this.mockMvc.perform(delete("/v1/auth/role/{Id}", role.getId()))
              .andExpect(status().isOk())
              .andDo(document("AuthController-deleteRole-example", pathParameters(
                    parameterWithName("Id").description("The id of role, generated by flowgate."))))
              .andReturn();
   }

   @Test
   public void updateRoleExample() throws Exception {

       WormholeRole role = createRole();
       roleRepository.save(role);
       role.setRoleName("sddcuser");

       this.mockMvc.perform(put("/v1/auth/role").contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(objectMapper.writeValueAsString(role)))
              .andExpect(status().isOk())
              .andDo(document("AuthController-updateRole-example", requestFields(
                      fieldWithPath("id")
                      .description("ID of the Role, created by flowgate"),
                fieldWithPath("roleName").description("roleName."),
                fieldWithPath("privilegeNames").description("privilegeNames").type(String.class))))
              .andReturn();
       WormholeRole role1 =  roleRepository.findById(role.getId()).get();
       TestCase.assertEquals("sddcuser", role1.getRoleName());
       roleRepository.deleteById(role.getId());
   }

   @Test
   public void updateRoleExampleException() throws Exception {
       WormholeRole role = createRole();
       role.setRoleName("sddcuser");
       roleRepository.save(role);
       WormholeRole role1 = createRole();
       roleRepository.save(role1);
       role1.setRoleName("sddcuser");
       expectedEx.expect(WormholeRequestException.class);
       expectedEx.expectMessage("The role name: "+role1.getRoleName()+" is already exsit.");
       MvcResult result = this.mockMvc.perform(put("/v1/auth/role").contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(objectMapper.writeValueAsString(role1))).andReturn();
       if (result.getResolvedException() != null) {
          roleRepository.deleteById(role.getId());
          roleRepository.deleteById(role1.getId());
          throw result.getResolvedException();
       }
   }


   AuthToken createToken(){
      AuthToken token = new AuthToken();
      token.setAccess_token("R$TYUIMJ");
      return token;
   }
   WormholeRole createRole() {
       List<String> privilegeNames = new ArrayList<String>();
       privilegeNames.add("privilegeName1");
       privilegeNames.add("privilegeName2");
       WormholeRole role = new WormholeRole();
       role.setId(UUID.randomUUID().toString());
       role.setPrivilegeNames(privilegeNames);
       role.setRoleName("roleName");
       return role;
   }
   WormholeUser createUser(){
       List<String> rolenames = new ArrayList<String>();
       rolenames.add("role1");
       rolenames.add("role2");
       List<String> userGroupIDs = new ArrayList<String>();
       userGroupIDs.add("userGroupIDs1");
       userGroupIDs.add("userGroupIDs2");

       WormholeUser user = new WormholeUser();
       long time = System.currentTimeMillis();
       user.setId(UUID.randomUUID().toString());
       user.setCreateTime(new Date(time));
       user.setEmailAddress("emailAddress");
       user.setGender(1);
       user.setMobile("mobile");
       user.setPassword("password");
       user.setRoleNames(rolenames);
       user.setStatus(0);
       user.setUserGroupIDs(userGroupIDs);
       user.setUserName("userName");

       return user;
   }

}
