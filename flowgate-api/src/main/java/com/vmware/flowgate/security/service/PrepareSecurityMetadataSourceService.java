/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.security.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Service;

import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.PrivilegeResourceMapping;
import com.vmware.flowgate.common.model.WormholeResources;
import com.vmware.flowgate.common.model.WormholeRole;
import com.vmware.flowgate.config.InitializeConfigureData;
import com.vmware.flowgate.repository.PrivilegeResourcesMappingReposity;
import com.vmware.flowgate.repository.RoleRepository;
import com.vmware.flowgate.util.FlowgateKeystore;
import com.vmware.flowgate.util.WormholeResourceWeightComparator;

@Service
public class PrepareSecurityMetadataSourceService
      implements FilterInvocationSecurityMetadataSource {
   private static final Logger logger =
         LoggerFactory.getLogger(PrepareSecurityMetadataSourceService.class);
   private HashMap<String, TreeMap<WormholeResources, String>> resourcesMap =
         new  HashMap<String, TreeMap<WormholeResources, String>>();
   private List<String> privilegeNames = new ArrayList<String>();
   @Autowired
   private PrivilegeResourcesMappingReposity mappingReposity;
   @Autowired
   private RoleRepository roleRepository;

   @Value("${api.guardstore.alias:flowgateEncrypt}")
   private String guardStoreAlias;

   @Value("${api.guardstore.path:guard.jceks}")
   private String guardStoreFilePath;
   @Value("${api.guardstore.pass}")
   private String guardStorePass;
   @PostConstruct
   public void loadResourceDefine(){
      logger.info("Initialization security resource.");
      PageRequest pageRequest = PageRequest.of(FlowgateConstant.defaultPageNumber-1, FlowgateConstant.maxPageSize);
      Page<PrivilegeResourceMapping> mappings = mappingReposity.findAll(pageRequest);
      List<PrivilegeResourceMapping> privilegeResourceMappings = mappings.getContent();
      HashMap<String,List<Map<AntPathRequestMatcher,Collection<ConfigAttribute>>>> resourceMappings = initResourceMap(privilegeResourceMappings);
      InitializeConfigureData.init(prepareRolePrivilegeMap(),resourceMappings,privilegeNames);
      FlowgateKeystore.init(guardStoreFilePath, guardStoreAlias, guardStorePass);
  }

   public HashMap<String, List<Map<AntPathRequestMatcher, Collection<ConfigAttribute>>>> initResourceMap(
         Iterable<PrivilegeResourceMapping> privilegeResourceMappings) {
      HashMap<String,List<Map<AntPathRequestMatcher,Collection<ConfigAttribute>>>> resourcesPrivilegeMap = new HashMap<>();
      WormholeResourceWeightComparator comparator = new WormholeResourceWeightComparator();
      TreeMap<WormholeResources, String> sortMap =
            new TreeMap<WormholeResources, String>(comparator);
      for (PrivilegeResourceMapping privilegeResourceMapping : privilegeResourceMappings) {
         privilegeNames.add(privilegeResourceMapping.getPrivilegeName());
         List<WormholeResources> resources = privilegeResourceMapping.getResource();
         if(resources == null) {
            continue;
         }
         for (WormholeResources resource : resources) {
            if(!resourcesMap.containsKey(resource.getHttpMethod())) {
               sortMap = new TreeMap<WormholeResources, String>(comparator);
            }else {
               sortMap = resourcesMap.get(resource.getHttpMethod());
            }
            sortMap.put(resource, privilegeResourceMapping.getPrivilegeName());
            resourcesMap.put(resource.getHttpMethod(), sortMap);
         }
      }
      for(String method:resourcesMap.keySet()) {
         resourcesPrivilegeMap.put(method, getMappingList(resourcesMap.get(method)));
      }
      return resourcesPrivilegeMap;
   }

   public static List<Map<AntPathRequestMatcher, Collection<ConfigAttribute>>> getMappingList(
         TreeMap<WormholeResources, String> resourcemap) {
      List<Map<AntPathRequestMatcher, Collection<ConfigAttribute>>> mappingList =
            new ArrayList<Map<AntPathRequestMatcher, Collection<ConfigAttribute>>>();
      Map<AntPathRequestMatcher, Collection<ConfigAttribute>> map = null;
      Collection<ConfigAttribute> configAttributes = null;
      ConfigAttribute configAttribute = null;
      Iterator<Map.Entry<WormholeResources, String>> iter = resourcemap.entrySet().iterator();
      while (iter.hasNext()) {
         map = new HashMap<AntPathRequestMatcher, Collection<ConfigAttribute>>();
         Map.Entry<WormholeResources, String> entry = iter.next();
         configAttributes = new ArrayList<>();
         configAttribute = new SecurityConfig(entry.getValue());
         configAttributes.add(configAttribute);
         map.put(new AntPathRequestMatcher(entry.getKey().getPattern()), configAttributes);
         mappingList.add(map);
      }
      return mappingList;
   }

   @Override
   public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
      if(InitializeConfigureData.privilegeResourceMap == null) loadResourceDefine();
      HttpServletRequest request = ((FilterInvocation) object).getHttpRequest();
      List<Map<AntPathRequestMatcher, Collection<ConfigAttribute>>> matcherAttributeMappings =
            InitializeConfigureData.privilegeResourceMap.get(request.getMethod());
      for (Map<AntPathRequestMatcher, Collection<ConfigAttribute>> matcherAttributeMapping : matcherAttributeMappings) {
         Iterator<Map.Entry<AntPathRequestMatcher, Collection<ConfigAttribute>>> iterator =
               matcherAttributeMapping.entrySet().iterator();
         while (iterator.hasNext()) {
            Map.Entry<AntPathRequestMatcher, Collection<ConfigAttribute>> entry = iterator.next();
            AntPathRequestMatcher matcher = entry.getKey();
            if (matcher.matches(request)) {
               return entry.getValue();
            }
         }
      }
      Collection<ConfigAttribute> collection = new LinkedList<>();
      if(collection.isEmpty()){
         ConfigAttribute configAttribute = new SecurityConfig("NO_RESOURCE");
         collection.add(configAttribute);
      }
      return collection;

   }

   @Override
   public Collection<ConfigAttribute> getAllConfigAttributes() {
      return null;
   }

   @Override
   public boolean supports(Class<?> clazz) {
      return true;
   }

   public HashMap<String, List<String>> prepareRolePrivilegeMap() {
      List<WormholeRole> roles = new ArrayList<WormholeRole>();
      PageRequest pageRequest = PageRequest.of(FlowgateConstant.defaultPageNumber-1, FlowgateConstant.maxPageSize);
      Page<WormholeRole> flwogateRoles = roleRepository.findAll(pageRequest);
      roles.addAll(flwogateRoles.getContent());
      if(flwogateRoles.getTotalPages()>1) {
         for(int i = 1; i<flwogateRoles.getTotalPages(); i++) {
            PageRequest page = PageRequest.of(i, FlowgateConstant.maxPageSize);
            Page<WormholeRole> rolePage = roleRepository.findAll(page);
            roles.addAll(rolePage.getContent());
         }
      }
      HashMap<String, List<String>> rolePrivilegeMap = new HashMap<String, List<String>>();
      for (WormholeRole role : roles) {
         rolePrivilegeMap.put(role.getRoleName(), role.getPrivilegeNames());
      }
      return rolePrivilegeMap;
   }
}
