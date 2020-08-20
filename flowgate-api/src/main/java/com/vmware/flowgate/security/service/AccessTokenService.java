/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.security.service;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.AuthToken;
import com.vmware.flowgate.common.model.WormholeUser;
import com.vmware.flowgate.config.InitializeConfigureData;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.util.JwtTokenUtil;
import com.vmware.flowgate.util.WormholeUserDetails;

@Service
public class AccessTokenService {

   private static final Logger logger = LoggerFactory.getLogger(AccessTokenService.class);

   @Autowired
   private UserDetailsServiceImpl userDetailsService;
   @Autowired
   private AuthenticationManager authenticationManager;
   @Autowired
   private JwtTokenUtil jwtTokenUtil;
   @Autowired
   private UserDetailsServiceImpl userservice;
   @Autowired
   private StringRedisTemplate redisTemplate;

   public AuthToken createToken(WormholeUser user) {
      // Perform the security
      AuthToken access_token = null;
      try {
         String username = user.getUserName();
         final Authentication authentication = authenticationManager
               .authenticate(new UsernamePasswordAuthenticationToken(username, user.getPassword()));
         SecurityContextHolder.getContext().setAuthentication(authentication);
         // Reload password post-security so we can generate token
         WormholeUserDetails userDetails = userDetailsService.loadUserByUsername(user.getUserName());
         access_token = jwtTokenUtil.generate(userDetails);
      }catch (BadCredentialsException e) {
         throw new WormholeRequestException(HttpStatus.UNAUTHORIZED, "Invalid username or password", e.getCause());
      }
      return access_token;
   }

   public AuthToken refreshToken(String token) {
      DecodedJWT jwt = jwtTokenUtil.getDecodedJwt(token);
      WormholeUser user =  userservice.getUserByName(jwt.getSubject());
      ObjectMapper mapper = new ObjectMapper();
      if(jwtTokenUtil.isCreatedAfterLastPasswordReset(jwt.getIssuedAt(),user.getLastPasswordResetDate())) {
         String userdetailString = getUserJsonString(token);
         WormholeUserDetails userDetails = null;
         try {
            userDetails = mapper.readValue(userdetailString, WormholeUserDetails.class);
         }catch (IOException e) {
            logger.error(e.getMessage());
            return null;
         }
         AuthToken refreshToken = jwtTokenUtil.generate(userDetails);
         redisTemplate.delete(JwtTokenUtil.Prefix_token + token);
         return refreshToken;
      }
      return null;
   }

   public String getUserJsonString(String token) {
      String userJson = redisTemplate.opsForValue().get(JwtTokenUtil.Prefix_token + token);
      if(userJson == null) {
         throw new WormholeRequestException(HttpStatus.UNAUTHORIZED, "Invalid token",null);
      }
      return userJson;
   }

   public void removeToken(String token) {
      redisTemplate.delete(JwtTokenUtil.Prefix_token + token);
   }

   public String getToken(HttpServletRequest request) {
      String authToken = request.getHeader(InitializeConfigureData.Authentication_Header);
      if (authToken != null && authToken.startsWith(JwtTokenUtil.Token_type+" ")) {
          authToken = authToken.replace(JwtTokenUtil.Token_type, "").trim();
      }
      if(authToken == null || "".equals(authToken)) {
         Cookie[] cookies = request.getCookies();
         if(cookies != null && cookies.length!=0) {
            for(Cookie currentcookie:cookies) {
               if(JwtTokenUtil.Token_Name.equals(currentcookie.getName())) {
                  authToken = currentcookie.getValue();
                  break;
               }
            }
         }
      }
      return authToken;
   }

   public WormholeUserDetails getCurrentUser(HttpServletRequest request) {
      String token = getToken(request);
      if(token == null && !InitializeConfigureData.unauthirzedURLs.containsKey(request.getRequestURI())) {
         logger.error("Get current user failed,please check the token."+request.getRequestURI());
         return null;
      }
      String userJson = null;
      try {
         userJson = getUserJsonString(token);
      }catch (WormholeRequestException e) {
         return null;
      }
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
      WormholeUserDetails user = null;
      try {
         user = mapper.readValue(userJson,WormholeUserDetails.class);
      } catch (IOException e) {
         logger.error("Get current user failed,"+e.getMessage());
      }
      return user;
   }

   public boolean validateServiceKey(String value) {
      if(redisTemplate.hasKey(FlowgateConstant.SERVICE_KEY_SET)) {
         return redisTemplate.opsForSet().isMember(FlowgateConstant.SERVICE_KEY_SET, value);
      }
      return false;
   }
}
