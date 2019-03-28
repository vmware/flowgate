/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.vmware.flowgate.security.filter.AuthenticationTokenFilter;
import com.vmware.flowgate.security.service.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

   @Autowired
   private JwtAuthenticationEntryPoint unauthorizedHandler;


   @Bean
   protected UserDetailsService customUserService() {
      return new UserDetailsServiceImpl();
   }

   @Bean
   public AuthenticationTokenFilter authenticationTokenFilterBean() {
      return new AuthenticationTokenFilter();
   }

   @Override
   protected void configure(AuthenticationManagerBuilder auth) throws Exception {
      auth.userDetailsService(customUserService()).passwordEncoder(passwordEncoder());
   }

   @Bean
   public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
   }

   @Bean
   @Override
   public AuthenticationManager authenticationManagerBean() throws Exception {
      return super.authenticationManagerBean();
   }

   @Override
   public void configure(WebSecurity web) throws Exception {

   }

   @Override
   protected void configure(HttpSecurity http) throws Exception {
      http.cors().and().csrf().disable().exceptionHandling()
            .authenticationEntryPoint(unauthorizedHandler).and().sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS).and() // don't create session
            .addFilterBefore(authenticationTokenFilterBean(),
                  UsernamePasswordAuthenticationFilter.class) // Custom JWT based security filter
            .headers().cacheControl(); // disable page caching
   }

}
