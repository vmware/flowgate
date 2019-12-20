/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.infobloxworker.service;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;

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
   private String queryHostURL = "%s/wapi/v2.5/ipv4address?ip_address=%s&_return_as_object=1&_return_type=json";


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
            this.queryHostURL = this.queryHostURL + "&_proxy_search=" + proxy_search;
         }
      }
   }


   public JsonResultForQueryHostNames getHostNameList(String ip) {
      try {
       JsonResultForQueryHostNames hostName = this.restTemplate.exchange(
               String.format(queryHostURL, this.hostName, ip), HttpMethod.GET, RestTemplateBuilder.getDefaultEntity(), JsonResultForQueryHostNames.class).getBody();
       return hostName;
      }catch(Exception e) {
         logger.info("Failed to query the ipaddress."+e.getMessage() );
      }
      return null;
   }

   public List<String> queryHostNamesByIP(String ip) {

      List<String> hostNameList = new ArrayList<String>();
      List<String> retHostNameList = new ArrayList<String>();
      this.restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(this.userName, this.password));

      JsonResultForQueryHostNames hostNames = this.getHostNameList(ip);
      if(hostNames == null) {
         return retHostNameList;
      }
      Collections.addAll(hostNameList, hostNames.getResult().get(0).getHostNames());
      //TODO: we need real InfoBlox API to get HostName without zone from InfoBlox
      String hostNameWithoutZone;
      for(String hostName : hostNameList) {

          int index = hostName.indexOf(".");
          if(index > 0) {
              hostNameWithoutZone = hostName.substring(0,index);
          }else{
              hostNameWithoutZone = hostName;
          }
          retHostNameList.add(hostNameWithoutZone);
      }
      logger.debug(String.format("%s", retHostNameList));
      return retHostNameList;
   }
}
