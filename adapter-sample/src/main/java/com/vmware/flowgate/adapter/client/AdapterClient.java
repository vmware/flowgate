/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.adapter.client;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.vmware.flowgate.client.RestTemplateBuilder;
import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;

@Service
public class AdapterClient {

   private static final String CHECK_CONNECTION_API = "";
   private static final String GET_DATA_API = "";
   private String username;
   private String password;
   private RestTemplate restTemplate;
   private String serviceEndPoint;

   public AdapterClient() {
   }

   public AdapterClient(FacilitySoftwareConfig config) {
      this.password = config.getPassword();
      this.username = config.getUserName();
      this.serviceEndPoint = config.getServerURL();
      try {
         this.restTemplate =
               RestTemplateBuilder.buildTemplate(config.isVerifyCert(), 60000);
      } catch (Exception e) {
         throw new WormholeException(e.getMessage(), e.getCause());
      }
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getServiceEndPoint() {
      return serviceEndPoint;
   }

//   public ResponseEntity<MyDataModel1> getDataFromCustomerApi(){
//      return this.restTemplate.
//            exchange(getServiceEndPoint() + GET_DATA_API,
//                  HttpMethod.GET, RestTemplateBuilder.getDefaultEntity(), MyDataModel1.class);
//   }

   public boolean checkConnection() {
      //TODO
      return true;
   }

}
