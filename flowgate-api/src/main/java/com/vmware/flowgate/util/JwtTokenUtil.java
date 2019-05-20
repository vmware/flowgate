/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.util;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.AuthToken;

@Component
public class JwtTokenUtil {

   private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
   @Value("${jwt.issuer:Flowgate}")
   private String issuer;
   @Value("${jwt.expiration:7200}")
   private int expiration;
   @Autowired
   private StringRedisTemplate redisTemplate;
   public static final String Prefix_token = "token_";
   public static final String Token_Name = "token";
   public static final String Token_type = "Bearer";
   private static final String CLAIM_AUTHORITIES = "authorities";
   /**
    * generate token with roles
    * @param user
    * @return
    */
   public AuthToken generate(WormholeUserDetails user) {
      String secret = FlowgateKeystore.getEncryptKey();
      Algorithm algorithm = null;
      try {
         algorithm = Algorithm.HMAC256(secret);
      } catch (IllegalArgumentException | UnsupportedEncodingException e) {
         logger.error("Error when generating token",e.getMessage());
         return null;
      }
      ObjectMapper mapper = new ObjectMapper();
      AuthToken access_token = new AuthToken();
      Date issure_date = new Date();
      Date expires_date = new Date(System.currentTimeMillis()+expiration*1000);
      long timeMillis = expires_date.getTime();
      String token = JWT.create().withIssuer(issuer).withIssuedAt(issure_date)
            .withExpiresAt(expires_date).withSubject(user.getUsername())
            .withClaim("userId", user.getUserId())
            .sign(algorithm);
      access_token.setAccess_token(token);
      access_token.setExpires_in(timeMillis);
      try {
         mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
         redisTemplate.opsForValue().set(Prefix_token + token,
               mapper.writeValueAsString(user),expiration,TimeUnit.SECONDS);
      } catch (JsonProcessingException e) {
         logger.error(e.getMessage());
         return null;
      }
      logger.debug(user.getUsername()+"'s token has been generated.");
      return access_token;
      
   }
   
   public DecodedJWT getDecodedJwt(String token) {
      String secret = FlowgateKeystore.getEncryptKey();
      if (token == null) {
         return null;
     }
      Algorithm algorithm = null;
      try {
         algorithm = Algorithm.HMAC256(secret);
      } catch (IllegalArgumentException e) {
         logger.error("Error when generating DecodedJWT",e.getMessage());
      } catch (UnsupportedEncodingException e) {
         logger.error("Error when generating DecodedJWT",e.getMessage());
      }
      JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer).build();
      return verifier.verify(token);
   }
   
   public boolean isCreatedAfterLastPasswordReset(Date issuedAt , long lastPasswordReset) {
      return issuedAt.getTime()>lastPasswordReset;
   }
 
}
