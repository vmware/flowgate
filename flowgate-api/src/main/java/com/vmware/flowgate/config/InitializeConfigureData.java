/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.vmware.flowgate.common.FlowgateConstant;

public class InitializeConfigureData {

   
   private static HashMap<String, List<String>> roleNameAndPrivilegeMap =
         new HashMap<String, List<String>>();
   public static HashMap<String, List<Map<AntPathRequestMatcher, Collection<ConfigAttribute>>>> 
         privilegeResourceMap = null;
   public static Map<String,String> unauthirzedURLs = new HashMap<String,String>();
   public static final String Authentication_Header = "Authorization";
   public static final String GET = "GET";
   public static final String POST = "POST";
   public static final String PUT = "PUT";
   public static final String DELETE = "DELETE";
   public static final String Register_Path = "/v1/auth/user";
   public static final String Generate_Token_Path = "/v1/auth/token";
   public static final String Refresh_Token_Path = "/v1/auth/token/refresh";
   public static final String Login_Path = "/v1/auth/login";
   public static final String Logout_Path = "/v1/auth/logout";
   public static final String Default_Access_Privilege = "Default_Access_Privilege";
   private static boolean isInitialized;
   private static String serviceKey = null;
   //private static 
   public static List<String> privilegeNames = new ArrayList<String>();
   static{
      
      unauthirzedURLs.put(Register_Path, POST);
      unauthirzedURLs.put(Generate_Token_Path, POST);
      unauthirzedURLs.put(Login_Path, POST);
      unauthirzedURLs.put(Logout_Path, GET);
      unauthirzedURLs.put(Refresh_Token_Path, GET);
      unauthirzedURLs = Collections.unmodifiableMap(unauthirzedURLs);
   }
   
   public static boolean isInitialized() {
      return isInitialized;
   }

   public static void init(
         HashMap<String, List<String>> roleNameAndPrivilegeMap,
         HashMap<String, List<Map<AntPathRequestMatcher, Collection<ConfigAttribute>>>> privilegeResourceMap,
         List<String> privileges) {
      if(!isInitialized) {
         InitializeConfigureData.roleNameAndPrivilegeMap = roleNameAndPrivilegeMap;
         InitializeConfigureData.privilegeResourceMap = privilegeResourceMap;
         serviceKey = System.getenv(FlowgateConstant.serviceKey);
         privilegeNames = Collections.unmodifiableList(privileges);
         isInitialized = true;
      }
   }

   public static boolean checkServiceKey(String key) {
      if(serviceKey == null) {
         return false;
      }
      return serviceKey.equals(key);
   }
   
   public static List<String> getPrivileges(String roleName) {
      return roleNameAndPrivilegeMap.get(roleName);
   }
   
   public static void setPrivileges(String roleName,List<String> privileges) {
      roleNameAndPrivilegeMap.put(roleName, privileges);
   }
   
}
