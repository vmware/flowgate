/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.flowgate.infobloxworker.service;

import java.net.ConnectException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.vmware.flowgate.infobloxworker.model.*;
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
import org.springframework.web.client.ResourceAccessException;


public class InfobloxClient extends RestClientBase {

   private static Logger logger = LoggerFactory.getLogger(InfobloxClient.class);

   @Value("${infoblox.socket.timeout:60000}")
   private int socketTimeout;

   //we should make proxy_search=GM as optional
   private String queryHostAndMacURL = "%s/wapi/v2.5/ipv4address?ip_address=%s&_return_as_object=1&_return_type=json&_return_fields=ip_address,mac_address,names,discovered_data";
   private String queryHostRecordURL = "%s/wapi/v2.5/record:host?_return_as_object=1&_return_type=json&_return_fields=zone,name,ipv4addrs.discovered_data,ipv4addrs.ipv4addr,ipv4addrs.mac,ipv4addrs.host&ipv4addr=%s";


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
      this.restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(this.userName, this.password));
      HashMap<AdvanceSettingType, String> advanceSetting = config.getAdvanceSetting();
      if(advanceSetting != null) {
         String proxy_search = advanceSetting.get(AdvanceSettingType.INFOBLOX_PROXY_SEARCH);
         if(proxy_search != null && !proxy_search.isEmpty()) {
            this.queryHostAndMacURL = this.queryHostAndMacURL + "&_proxy_search=" + proxy_search;
            this.queryHostRecordURL = this.queryHostRecordURL + "&_proxy_search=" + proxy_search;
         }
      }
   }

   public JsonResultForQueryHostRecord getHostRecord(String ip) {
      try {
         JsonResultForQueryHostRecord hostRecord = this.restTemplate.exchange(
                  String.format(queryHostRecordURL, this.hostName, ip), HttpMethod.GET, RestTemplateBuilder.getDefaultEntity(), JsonResultForQueryHostRecord.class).getBody();
         return hostRecord;
      } catch (ResourceAccessException e) {
         logger.info("Failed to query data from Infoblox : {}", e.getMessage());
         if(e.getCause().getCause() instanceof ConnectException) {
            throw e;
         }
      } catch (HttpClientErrorException e) {
         logger.error("Failed to query data from Infoblox : {}", e.getMessage());
         if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
            throw e;
         }
      } catch (Exception e) {
         logger.info("Failed to query the ipaddress.{}", e.getMessage());
      }
      return null;
   }

   public JsonResultForQueryHostNames getIpv4address(String ip) {
      try {
         JsonResultForQueryHostNames hostName = this.restTemplate.exchange(
                  String.format(queryHostAndMacURL, this.hostName, ip), HttpMethod.GET, RestTemplateBuilder.getDefaultEntity(), JsonResultForQueryHostNames.class).getBody();
         return hostName;
      } catch (ResourceAccessException e) {
         logger.info("Failed to query data from Infoblox : {}", e.getMessage());
         if(e.getCause().getCause() instanceof ConnectException) {
            throw e;
         }
      } catch (HttpClientErrorException e) {
         logger.error("Failed to query data from Infoblox : {}", e.getMessage());
         if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
            throw e;
         }
      } catch (Exception e) {
         logger.info("Failed to query the ipaddress.{}", e.getMessage());
      }
      return null;
   }

   public List<InfoBloxIPInfoResult> queryIpv4addressByIP(String ip) {
      List<InfoBloxIPInfoResult> infoBloxIPInfoResults = new ArrayList<>();
      JsonResultForQueryHostNames hostNames = this.getIpv4address(ip);
      if(hostNames == null) {
         return null;
      }
      InfobloxIpv4addressItem ipv4address = hostNames.getResult().get(0);
      //TODO: we need real InfoBlox API to get HostName without zone from InfoBlox
      for (String hostname : ipv4address.getHostNames()) {
         infoBloxIPInfoResults.add(InfoBloxIPInfoResult.build(hostname, ipv4address));
      }
      return infoBloxIPInfoResults;
   }

   public List<InfoBloxIPInfoResult> queryHostRecordByIP(String ip) {
      List<InfoBloxIPInfoResult> infoBloxIPInfoResults = new ArrayList<>();
      JsonResultForQueryHostRecord hostRecord = this.getHostRecord(ip);
      if(hostRecord == null) {
         return null;
      }
      for (InfobloxHostRecordItem infobloxHostRecordItem : hostRecord.getResult()) {
         infoBloxIPInfoResults.add(InfoBloxIPInfoResult.build(infobloxHostRecordItem));
      }
      return infoBloxIPInfoResults;
   }

}
