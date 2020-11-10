/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.infobloxworker.service;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.vmware.flowgate.infobloxworker.model.InfoBloxIPInfoResult;
import com.vmware.flowgate.infobloxworker.model.Infoblox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.HttpClientErrorException;

import com.vmware.flowgate.client.RestClientBase;
import com.vmware.flowgate.client.RestTemplateBuilder;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.AdvanceSettingType;
import com.vmware.flowgate.infobloxworker.model.JsonResultForQueryHostNames;


public class InfobloxClient extends RestClientBase {

   private static Logger logger = LoggerFactory.getLogger(InfobloxClient.class);

   @Value("${infoblox.socket.timeout:60000}")
   private int socketTimeout;

   //we should make proxy_search=GM as optional
   private String queryHostAndMacURL = "%s/wapi/v2.5/ipv4address?ip_address=%s&_return_as_object=1&_return_type=json&_return_fields=ip_address,mac_address,names,discovered_data";


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
    	 JsonResultForQueryHostNames hostName = this.restTemplate.exchange(
               String.format(queryHostAndMacURL, this.hostName, ip), HttpMethod.GET, RestTemplateBuilder.getDefaultEntity(), JsonResultForQueryHostNames.class).getBody();
    	 return hostName;
      }catch(HttpClientErrorException e) {
    	  if(HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
    		  throw e;
    	  }
    	  logger.info("Failed to query data from Infoblox : "+e.getMessage() );
      }catch(Exception e) {
         logger.info("Failed to query the ipaddress."+e.getMessage() );
      }
      return null;
   }

   public List<InfoBloxIPInfoResult> queryHostNamesByIP(String ip) {
      this.restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(this.userName, this.password));
      JsonResultForQueryHostNames hostNames = this.getHostNameList(ip);
      if(hostNames == null || hostNames.getResult().isEmpty() || hostNames.getResult().get(0).getHostNames().length == 0) {
         return null;
      }
      List<InfoBloxIPInfoResult> infoBloxIPInfoResults = new ArrayList<>();
      Infoblox infoblox = hostNames.getResult().get(0);
      //TODO: we need real InfoBlox API to get HostName without zone from InfoBlox
      for (String hostname : infoblox.getHostNames()) {
         infoBloxIPInfoResults.add(InfoBloxIPInfoResult.build(hostname, infoblox));
      }
      logger.debug(String.format("%s", infoBloxIPInfoResults));
      return infoBloxIPInfoResults;
   }
}
