/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.vmware.wormhole.common.WormholeConstant;
import com.vmware.wormhole.common.model.AuthToken;
import com.vmware.wormhole.common.model.AuthenticationResult;
import com.vmware.wormhole.exception.WormholeRequestException;
import com.vmware.wormhole.repository.UserRepository;
import com.vmware.wormhole.common.model.WormholeUser;
import com.vmware.wormhole.common.security.DesensitizationUserData;
import com.vmware.wormhole.config.InitializeConfigureData;
import com.vmware.wormhole.common.model.WormholePrivilege;
import com.vmware.wormhole.common.model.WormholeRole;
import com.vmware.wormhole.repository.WormholePrivilegeRepository;
import com.vmware.wormhole.repository.RoleRepository;
import com.vmware.wormhole.security.service.AccessTokenService;
import com.vmware.wormhole.security.service.UserDetailsServiceImpl;
import com.vmware.wormhole.util.AuthorityUtil;
import com.vmware.wormhole.util.JwtTokenUtil;
import com.vmware.wormhole.util.WormholeUserDetails;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

   private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
   @Autowired
   private UserRepository userRepository;
   @Autowired
   private RoleRepository roleRepository;
   @Autowired
   private WormholePrivilegeRepository privilegeRepository;
   @Autowired
   private AccessTokenService accessTokenService;
   @Autowired
   private UserDetailsServiceImpl userdetailservice;
   @Autowired
   private JwtTokenUtil jwtTokenUtil;
   @Value("${jwt.expiration:7200}")
   private int expiration;
   private static String Role_admin = "admin";
   
   @RequestMapping(value="/token", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public AuthToken getToken(@RequestBody(required = false) WormholeUser user,
         @RequestHeader(name = "serviceKey",required = false) String serviceKey, HttpServletRequest request,
         HttpServletResponse response) {
      AuthToken access_token = null;
      if(user == null && !InitializeConfigureData.checkServiceKey(serviceKey)) {
         throw new WormholeRequestException(HttpStatus.UNAUTHORIZED, "Invalid username or password", null);
      }
      if(user != null) {
         access_token = accessTokenService.createToken(user);
      }else if(InitializeConfigureData.checkServiceKey(serviceKey)) {
         List<String> roleNames = new ArrayList<String>();
         roleNames.add(Role_admin);
         AuthorityUtil util = new AuthorityUtil();
         WormholeUserDetails userDetails = 
               new WormholeUserDetails(WormholeConstant.systemUser,WormholeConstant.systemUser, 
                     WormholeConstant.systemUser, util.createGrantedAuthorities(roleNames));
         access_token = jwtTokenUtil.generate(userDetails);
      }
      Cookie cookie = new Cookie(JwtTokenUtil.Token_Name, access_token.getAccess_token());
      cookie.setHttpOnly(true);
      cookie.setPath("/");
      cookie.setDomain(request.getServerName());
      cookie.setMaxAge(expiration);
      response.addCookie(cookie);
      return access_token;
   }
   
   @RequestMapping(value="/token/refresh", method = RequestMethod.GET)
   public AuthToken refreshToken(HttpServletRequest request,HttpServletResponse response) {
      String authToken = accessTokenService.getToken(request);
      if(authToken == null || "".equals(authToken)) {
         return  null;
      }
      DecodedJWT jwt = jwtTokenUtil.getDecodedJwt(authToken);
      String currentuser = accessTokenService.getCurrentUser(request).getUsername();
      if(!jwt.getSubject().equals(currentuser)) {
         throw new WormholeRequestException(HttpStatus.FORBIDDEN, "Forbidden", null);
      }
      AuthToken access_token = accessTokenService.refreshToken(authToken);
      if(access_token != null) {
         response.addHeader(InitializeConfigureData.Authentication_Header, authToken);
         Cookie cookie = new Cookie(JwtTokenUtil.Token_Name, access_token.getAccess_token());
         cookie.setHttpOnly(true);
         cookie.setPath("/");
         cookie.setDomain(request.getServerName());
         response.addCookie(cookie);
      }
      return access_token;
   }

   @RequestMapping(value = "/login", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public AuthenticationResult login(@RequestBody WormholeUser user, HttpServletRequest request,
         HttpServletResponse response) {
      UserDetails userDetails = userdetailservice.loadUserByUsername(user.getUserName());
      AuthorityUtil util = new AuthorityUtil();
      AuthenticationResult result = new AuthenticationResult();
      AuthToken access_token = accessTokenService.createToken(user);
      Cookie cookie = new Cookie(JwtTokenUtil.Token_Name, access_token.getAccess_token());
      cookie.setHttpOnly(true);
      cookie.setPath("/");
      cookie.setDomain(request.getServerName());
      response.addCookie(cookie);
      result.setPrivileges(util.getPrivilege(userDetails));
      result.setToken(access_token);
      result.setUserName(user.getUserName());
      return result;
   }
   
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/logout", method = RequestMethod.GET)
   public void logout( HttpServletRequest request,
         HttpServletResponse response) {
      String authToken = accessTokenService.getToken(request);
      if(authToken != null && !"".equals(authToken)) {
         accessTokenService.removeToken(authToken);
      }
   }
   
   // Create a new user
   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value="/user", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void createUser(@RequestBody WormholeUser user,HttpServletResponse response) {
      WormholeUser example = new WormholeUser();
      example.setUserName(user.getUserName());
      ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("lastPasswordResetDate");
      Example<WormholeUser> userexample = Example.of(example, matcher);
      if(userRepository.findOne(userexample) != null) {
         logger.info(user.getUserName()+" is already exsit");
         String message = example.getUserName()+"is already exsit";
         throw new WormholeRequestException(message);
      }
      user.setCreateTime(new Date());
      BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
      user.setPassword(encoder.encode(user.getPassword().trim()));
      user.setLastPasswordResetDate(new Date().getTime());
      userRepository.save(user);
   }

   // Delete a user
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE)
   public void deleteUser(@PathVariable String id) {
      if(userRepository.findOne(id) == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND, "User not found", null);
      }
      userRepository.delete(id);
   }

   //Update a user
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value="/user",method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateUser(@RequestBody WormholeUser user,HttpServletRequest request) {
      WormholeUserDetails userDetail = accessTokenService.getCurrentUser(request);
      WormholeUser currentUser = userRepository.findOne(userDetail.getUserId());
      WormholeUser old = null;
      if(currentUser.getRoleNames().contains(Role_admin)) {
         old = userRepository.findOne(user.getId());
      }else if(currentUser.getUserName().equals(user.getUserName())){
         old = currentUser;
      }else {
         throw new WormholeRequestException(HttpStatus.FORBIDDEN, "Forbidden", null);
      }
      if (old == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND, "User not found", null);
      }
      if(user.getPassword() != null && !"".equals(user.getPassword().trim())) {
         BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
         old.setPassword(encoder.encode(user.getPassword()));
         old.setLastPasswordResetDate(new Date().getTime());
      }
     if(user.getEmailAddress() != null && !"".equals(user.getEmailAddress().trim())) {
         old.setEmailAddress(user.getEmailAddress());
      }
     if(user.getRoleNames() != null) {
         old.setRoleNames(user.getRoleNames());
      }
      userRepository.save(old);
   }

   // Read a user
   @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
   public WormholeUser readUser(@PathVariable(required = false) String id,HttpServletRequest request) {
      WormholeUserDetails userDetail = accessTokenService.getCurrentUser(request);
      WormholeUser currentUser = userRepository.findOne(userDetail.getUserId());
      WormholeUser user = null;
      if(currentUser.getRoleNames().contains(Role_admin)) {
         user = userRepository.findOne(id);
      }else if(currentUser.getId().equals(id)){
         user = currentUser;
      }else {
         throw new WormholeRequestException(HttpStatus.FORBIDDEN, "Forbidden", null);
      }
      if(user != null) {
         return DesensitizationUserData.desensitizationUser(user);
      }
      return user;
   }
   
   // Read a user by user name
   @RequestMapping(value = "/user/username/{name}", method = RequestMethod.GET)
   public WormholeUser readUserByName(@PathVariable(required = false) String name,HttpServletRequest request) {
      WormholeUserDetails userDetail = accessTokenService.getCurrentUser(request);
      WormholeUser currentUser = userRepository.findOne(userDetail.getUserId());
      WormholeUser user = null;
      if(currentUser.getRoleNames().contains(Role_admin)) {
         WormholeUser example = new WormholeUser();
         example.setUserName(name);
         ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("lastPasswordResetDate");
         Example<WormholeUser> userexample = Example.of(example, matcher);
         user = userRepository.findOne(userexample);
      }else if(currentUser.getUserName().equals(name)){
         user = currentUser;
      }else {
         throw new WormholeRequestException(HttpStatus.FORBIDDEN, "Forbidden", null);
      }
      if(user != null) {
         return DesensitizationUserData.desensitizationUser(user);
      }
      return user;
   }
   
   // Read users
   @RequestMapping(value = "/user/page",method = RequestMethod.GET)
   public Page<WormholeUser> queryUserByPageable (@RequestParam("currentPage") Integer currentPage,
         @RequestParam("pageSize") Integer pageSize) {
      if(currentPage < 1) {
         currentPage = 1;
      }
      if(pageSize == null) {
         pageSize = WormholeConstant.defaultPageSize;
      }else if(pageSize > WormholeConstant.maxPageSize){
         pageSize = WormholeConstant.maxPageSize;
      }
      PageRequest pageRequest = new PageRequest(currentPage-1,pageSize);
      Page<WormholeUser> pageUsers = userRepository.findAll(pageRequest);
      DesensitizationUserData.desensitizationUser(pageUsers.getContent());
      return pageUsers;
   }
   // Read users
   @RequestMapping(value="/user/users", method = RequestMethod.GET)
   public List<WormholeUser> queryUsers() {
      return DesensitizationUserData.desensitizationUser(userRepository.findAll());
   }

// Create a new role
   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value="/role",method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void createRole(@RequestBody WormholeRole role) {
      WormholeRole example = new WormholeRole();
      example.setRoleName(role.getRoleName());
      if(roleRepository.findOne(Example.of(example)) != null) {
         logger.info("The role "+example.getRoleName()+" is already exsit.");
         String message = "The role "+example.getRoleName()+" is already exsit.";
         throw new WormholeRequestException(message);
      }
      role.setId(null);
      roleRepository.save(role);
      InitializeConfigureData.setPrivileges(role.getRoleName(), role.getPrivilegeNames());
   }
   // Read a role
   @RequestMapping(value = "/role/{id}", method = RequestMethod.GET)
   public WormholeRole readRole(@PathVariable String id) {
      return roleRepository.findOne(id);
   }
   // Read roles
   @RequestMapping(value = "/role/page",method = RequestMethod.GET)
   public Page<WormholeRole> queryByPage(@RequestParam ("currentPage") Integer currentPage,
         @RequestParam("pageSize") Integer pageSize) {
      if(currentPage < 1) {
         currentPage = 1;
      }
      if(pageSize > WormholeConstant.maxPageSize) {
          pageSize = WormholeConstant.maxPageSize;
      }
      PageRequest pageRequest = new PageRequest(currentPage-1,pageSize);
      return roleRepository.findAll(pageRequest);
   }
   // Read roles
   @RequestMapping(value = "/roles",method = RequestMethod.GET)
   public List<WormholeRole> readRoleNames() {
      return  roleRepository.findAll();
   }
   // Delete a role
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/role/{id}", method = RequestMethod.DELETE)
   public void deleteRole(@PathVariable String id) {
      roleRepository.delete(id);
   }
   //update a role
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value="/role",method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateRole(@RequestBody WormholeRole role) {
      WormholeRole old = roleRepository.findOne(role.getId());
      if (old == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND, "Role not found", null);
      }
      if(role.getRoleName() != null && !"".equals(role.getRoleName().trim())) {
         old.setRoleName(role.getRoleName());
      }
      if(role.getPrivilegeNames() != null) {
         old.setPrivilegeNames(role.getPrivilegeNames());
      }
      roleRepository.save(old);
      InitializeConfigureData.setPrivileges(role.getRoleName(), role.getPrivilegeNames());
   }

   @RequestMapping(value="/privileges",method = RequestMethod.GET)
   public List<String> getPrivilegeName(){
      return InitializeConfigureData.privilegeNames;
   }
   public List<WormholePrivilege> readPrivilege(){
      return privilegeRepository.findAll();
   }
}
