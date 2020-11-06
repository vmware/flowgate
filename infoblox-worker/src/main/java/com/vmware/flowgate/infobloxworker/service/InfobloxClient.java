/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.infobloxworker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.client.RestClientBase;
import com.vmware.flowgate.client.RestTemplateBuilder;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.infobloxworker.model.InfoBloxIPInfoResult;
import com.vmware.flowgate.infobloxworker.model.JsonResultForQueryHostNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.HttpClientErrorException;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


public class InfobloxClient extends RestClientBase {

   private static Logger logger = LoggerFactory.getLogger(InfobloxClient.class);

   @Value("${infoblox.socket.timeout:60000}")
   private int socketTimeout;

   // we should make proxy_search=GM as optional
   private String queryHostAndMacURL = "%s/wapi/v2.7/record:host?_return_as_object=1&_return_type=json&_return_fields=zone,name,ipv4addrs.discovered_data,ipv4addrs.ipv4addr,ipv4addrs.mac,ipv4addrs.host&ipv4addr=%s";

   private String hostName;
   private String userName;
   private String password;

   public InfobloxClient() {
   }

   public InfobloxClient(FacilitySoftwareConfig config) {
      try {
         this.restTemplate =
               RestTemplateBuilder.buildTemplate(config.isVerifyCert(), socketTimeout);
      } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
         logger.error("Failed to initalize", e);
      }
      this.hostName = config.getServerURL();
      this.userName = config.getUserName();
      this.password = config.getPassword();
      HashMap<AdvanceSettingType, String> advanceSetting = config.getAdvanceSetting();
      if(advanceSetting != null) {
         String proxy_search = advanceSetting.get(AdvanceSettingType.INFOBLOX_PROXY_SEARCH);
         if(proxy_search != null && !proxy_search.isEmpty()) {
            this.queryHostAndMacURL = this.queryHostAndMacURL + "&_proxy_search=" + proxy_search;
         }
      }
   }

   public JsonResultForQueryHostNames getHostNameList(String ip) {
      try {
         JsonResultForQueryHostNames hostName = this.restTemplate.exchange(String.format(queryHostAndMacURL, this.hostName, ip), HttpMethod.GET,
                RestTemplateBuilder.getDefaultEntity(), JsonResultForQueryHostNames.class).getBody();
    	   logger.debug("Before transform:{}", new ObjectMapper().writeValueAsString(hostName));
    	   return hostName;
      } catch (HttpClientErrorException e) {
    	   if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
    	      throw e;
    	   }
         logger.info("Failed to query data from Infoblox : "+e.getMessage() );
      } catch (Exception e) {
         logger.info("Failed to query the ipaddress."+e.getMessage() );
      }
      return null;
   }

   public List<InfoBloxIPInfoResult> queryHostNamesByIP(String ip) {
      this.restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(this.userName, this.password));
      JsonResultForQueryHostNames jsonResultForQueryHostNames = this.getHostNameList(ip);
      if(jsonResultForQueryHostNames == null || jsonResultForQueryHostNames.getResult() == null || jsonResultForQueryHostNames.getResult().size() == 0) {
         return null;
      }
      List<InfoBloxIPInfoResult> results = jsonResultForQueryHostNames.getResult().stream().map(InfoBloxIPInfoResult::build).collect(Collectors.toList());
      logger.debug("After transform:{}", results);
      return results;
   }
}
