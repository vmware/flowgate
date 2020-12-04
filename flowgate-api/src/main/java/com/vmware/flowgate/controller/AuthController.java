/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.AuthToken;
import com.vmware.flowgate.common.model.WormholeRole;
import com.vmware.flowgate.common.model.WormholeUser;
import com.vmware.flowgate.common.security.DesensitizationUserData;
import com.vmware.flowgate.config.InitializeConfigureData;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.RoleRepository;
import com.vmware.flowgate.repository.UserRepository;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.util.AuthorityUtil;
import com.vmware.flowgate.util.BaseDocumentUtil;
import com.vmware.flowgate.util.JwtTokenUtil;
import com.vmware.flowgate.util.WormholeUserDetails;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

   @Autowired
   private UserRepository userRepository;
   @Autowired
   private RoleRepository roleRepository;
   @Autowired
   private AccessTokenService accessTokenService;
   @Autowired
   private JwtTokenUtil jwtTokenUtil;
   @Value("${jwt.expiration:7200}")
   private int expiration;

   @RequestMapping(value = "/token", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public AuthToken getToken(@RequestBody(required = false) WormholeUser user,
         @RequestHeader(name = "serviceKey", required = false) String serviceKey,
         HttpServletRequest request, HttpServletResponse response) {
      AuthToken access_token = null;
      if (user == null && serviceKey == null) {
         throw new WormholeRequestException(HttpStatus.UNAUTHORIZED, "Invalid username or password",
               null);
      }
      if (user != null) {
         access_token = accessTokenService.createToken(user);
      } else {
         if(InitializeConfigureData.checkServiceKey(serviceKey) || accessTokenService.validateServiceKey(serviceKey)) {
            List<String> roleNames = new ArrayList<String>();
            roleNames.add(FlowgateConstant.Role_admin);
            AuthorityUtil util = new AuthorityUtil();
            WormholeUserDetails userDetails =
                  new WormholeUserDetails(FlowgateConstant.systemUser, FlowgateConstant.systemUser,
                        FlowgateConstant.systemUser, util.createGrantedAuthorities(roleNames));
            access_token = jwtTokenUtil.generate(userDetails);
         }else {
            throw new WormholeRequestException(HttpStatus.UNAUTHORIZED, "Invalid username or password",
                  null);
         }
      }
      Cookie cookie = new Cookie(JwtTokenUtil.Token_Name, access_token.getAccess_token());
      cookie.setHttpOnly(true);
      cookie.setPath("/");
      cookie.setDomain(request.getServerName());
      cookie.setMaxAge(expiration);
      response.addCookie(cookie);
      return access_token;
   }

   @RequestMapping(value = "/token/refresh", method = RequestMethod.GET)
   public AuthToken refreshToken(HttpServletRequest request, HttpServletResponse response) {
      String authToken = accessTokenService.getToken(request);
      if (authToken == null || "".equals(authToken)) {
         return null;
      }
      DecodedJWT jwt = jwtTokenUtil.getDecodedJwt(authToken);
      String currentuser = accessTokenService.getCurrentUser(request).getUsername();
      if (!jwt.getSubject().equals(currentuser)) {
         throw new WormholeRequestException(HttpStatus.FORBIDDEN, "Forbidden", null);
      }
      AuthToken access_token = accessTokenService.refreshToken(authToken);
      if (access_token != null) {
         response.addHeader(InitializeConfigureData.Authentication_Header,
               access_token.getAccess_token());
         Cookie cookie = new Cookie(JwtTokenUtil.Token_Name, access_token.getAccess_token());
         cookie.setHttpOnly(true);
         cookie.setPath("/");
         cookie.setDomain(request.getServerName());
         response.addCookie(cookie);
      }
      return access_token;
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/logout", method = RequestMethod.GET)
   public void logout(HttpServletRequest request, HttpServletResponse response) {
      String authToken = accessTokenService.getToken(request);
      if (authToken != null && !"".equals(authToken)) {
         accessTokenService.removeToken(authToken);
      }
   }

   // Create a new user
   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/user", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void createUser(@RequestBody WormholeUser user, HttpServletResponse response) {
      if (userRepository.findOneByUserName(user.getUserName()) != null) {
         String message = user.getUserName() + "is already exsit";
         throw new WormholeRequestException(message);
      }
      user.setCreateTime(new Date());
      BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
      user.setPassword(encoder.encode(user.getPassword().trim()));
      user.setLastPasswordResetDate(new Date().getTime());
      BaseDocumentUtil.generateID(user);
      userRepository.save(user);
   }

   // Delete a user
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE)
   public void deleteUser(@PathVariable String id) {
      if (!userRepository.findById(id).isPresent()) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND, "User not found", null);
      }
      userRepository.deleteById(id);
   }

   //Update a user
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/user", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateUser(@RequestBody WormholeUser user, HttpServletRequest request) {
      WormholeUserDetails userDetail = accessTokenService.getCurrentUser(request);
      Optional<WormholeUser> currentUserOptional = userRepository.findById(userDetail.getUserId());
      WormholeUser currentUser = currentUserOptional.get();
      WormholeUser old = null;
      if (currentUser.getUserName().equals(user.getUserName())) {
         old = currentUser;
      }else if (currentUser.getRoleNames().contains(FlowgateConstant.Role_admin)) {
         Optional<WormholeUser> oldUserOptional = userRepository.findById(user.getId());
         old = oldUserOptional.get();
      } else {
         throw new WormholeRequestException(HttpStatus.FORBIDDEN, "Forbidden", null);
      }
      if (old == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND, "User not found", null);
      }
      if (user.getPassword() != null && !"".equals(user.getPassword().trim())) {
         BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
         old.setPassword(encoder.encode(user.getPassword()));
         old.setLastPasswordResetDate(new Date().getTime());
      }
      if (user.getEmailAddress() != null && !"".equals(user.getEmailAddress().trim())) {
         old.setEmailAddress(user.getEmailAddress());
      }
      if (user.getRoleNames() != null) {
         old.setRoleNames(user.getRoleNames());
      }
      userRepository.save(old);
   }

   // Read a user
   @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
   public WormholeUser readUser(@PathVariable(required = false) String id,
         HttpServletRequest request) {
      WormholeUserDetails userDetail = accessTokenService.getCurrentUser(request);
      Optional<WormholeUser> currentUserOptional = userRepository.findById(userDetail.getUserId());
      WormholeUser user = null;
      WormholeUser currentUser = currentUserOptional.get();
      if (currentUser.getId().equals(id)) {
         user = currentUser;
      }else if (currentUser.getRoleNames().contains(FlowgateConstant.Role_admin)) {
         Optional<WormholeUser> userOptional = userRepository.findById(id);
         user = userOptional.get();
      } else {
         throw new WormholeRequestException(HttpStatus.FORBIDDEN, "Forbidden", null);
      }
      if (user != null) {
         return DesensitizationUserData.desensitizationUser(user);
      }
      return user;
   }

   // Read a user by user name
   @RequestMapping(value = "/user/username/{name}", method = RequestMethod.GET)
   public WormholeUser readUserByName(@PathVariable(required = false) String name,
         HttpServletRequest request) {
      WormholeUserDetails userDetail = accessTokenService.getCurrentUser(request);
      Optional<WormholeUser> currentUserOptional = userRepository.findById(userDetail.getUserId());
      WormholeUser user = null;
      WormholeUser currentUser = currentUserOptional.get();
      if (currentUser.getRoleNames().contains(FlowgateConstant.Role_admin)) {
         user = userRepository.findOneByUserName(name);
      } else if (currentUser.getUserName().equals(name)) {
         user = currentUser;
      } else {
         throw new WormholeRequestException(HttpStatus.FORBIDDEN, "Forbidden", null);
      }
      if (user != null) {
         return DesensitizationUserData.desensitizationUser(user);
      }
      return user;
   }

   // Read users
   @RequestMapping(value = "/user", method = RequestMethod.GET)
   public Page<WormholeUser> queryUserByPageable(@RequestParam("currentPage") int currentPage,
         @RequestParam("pageSize") int pageSize) {
      if (currentPage < FlowgateConstant.defaultPageNumber) {
         currentPage = FlowgateConstant.defaultPageNumber;
      }
      if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);
      Page<WormholeUser> pageUsers = userRepository.findAll(pageRequest);
      DesensitizationUserData.desensitizationUser(pageUsers.getContent());
      return pageUsers;
   }

   // Create a new role
   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/role", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void createRole(@RequestBody WormholeRole role) {
      if (roleRepository.findOneByRoleName(role.getRoleName()) != null) {
         String message = "The role " + role.getRoleName() + " is already exsit.";
         throw new WormholeRequestException(message);
      }
      BaseDocumentUtil.generateID(role);
      roleRepository.save(role);
      InitializeConfigureData.setPrivileges(role.getRoleName(), role.getPrivilegeNames());
   }

   // Read a role
   @RequestMapping(value = "/role/{id}", method = RequestMethod.GET)
   public WormholeRole readRole(@PathVariable String id) {
      Optional<WormholeRole> roleOptional = roleRepository.findById(id);
      return roleOptional.get();
   }

   // Read roles
   @RequestMapping(value = "/role", method = RequestMethod.GET)
   public Page<WormholeRole> queryByPage(@RequestParam("currentPage") int currentPage,
         @RequestParam("pageSize") int pageSize) {
      if (currentPage < FlowgateConstant.defaultPageNumber) {
         currentPage = FlowgateConstant.defaultPageNumber;
      }
      if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);
      return roleRepository.findAll(pageRequest);
   }

   // Delete a role
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/role/{id}", method = RequestMethod.DELETE)
   public void deleteRole(@PathVariable String id) {
      roleRepository.deleteById(id);
   }

   //update a role
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/role", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateRole(@RequestBody WormholeRole role) {
      Optional<WormholeRole> oldRoleOptional = roleRepository.findById(role.getId());
      if(!oldRoleOptional.isPresent()) {
         throw WormholeRequestException.NotFound("Role", "id", role.getId());
      }
      WormholeRole  existingRole = roleRepository.findOneByRoleName(role.getRoleName());
      if (existingRole != null && !existingRole.getId().equals(role.getId())) {
         String message = "The role name: " + role.getRoleName() + " is already exsit.";
         throw new WormholeRequestException(message);
      }
      WormholeRole old = oldRoleOptional.get();
      if (role.getRoleName() != null && !"".equals(role.getRoleName().trim())) {
         old.setRoleName(role.getRoleName());
      }
      if (role.getPrivilegeNames() != null) {
         old.setPrivilegeNames(role.getPrivilegeNames());
      }
      roleRepository.save(old);
      InitializeConfigureData.setPrivileges(role.getRoleName(), role.getPrivilegeNames());
   }

   @RequestMapping(value = "/privileges", method = RequestMethod.GET)
   public Set<String> getPrivilegeName(HttpServletRequest request) {
      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      AuthorityUtil util = new AuthorityUtil();
      return util.getPrivilege(user);
   }
}
