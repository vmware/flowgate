/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vmware.flowgate.config.InitializeConfigureData;
import com.vmware.flowgate.security.config.JwtAuthenticationEntryPoint;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.util.WormholeUserDetails;

public class AuthenticationTokenFilter extends OncePerRequestFilter {

   @Autowired
   private AccessTokenService accessTokenService;
   @Autowired
   private JwtAuthenticationEntryPoint unauthorizedHandler;
   
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
         FilterChain filterChain) throws ServletException, IOException {
      String resUrl = request.getRequestURI();
      WormholeUserDetails user = accessTokenService.getCurrentUser(request);
      if(user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        logger.debug("checking authentication for user " + user.getUsername());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), "N/A", user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
     }else if (InitializeConfigureData.unauthirzedURLs.containsKey(resUrl)
           && request.getMethod().equals(InitializeConfigureData.unauthirzedURLs.get(resUrl))) {
        filterChain.doFilter(request, response);
     }else {
        unauthorizedHandler.commence(request, response, null);
     }
  }

}
