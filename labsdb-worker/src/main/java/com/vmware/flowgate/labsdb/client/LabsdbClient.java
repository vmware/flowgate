/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.labsdb.client;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.vmware.flowgate.client.RestTemplateBuilder;
import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;

@Service
public class LabsdbClient {

   private static final String GetwiremapURL = "/wiremap/index.php?action=search&page=0&dev_type=is&dev_name=%s&output=xml";
   private String username;
   private String password;
   private RestTemplate restTemplate;
   private String serviceEndPoint;
   
   public LabsdbClient() {   
   }
   
   public LabsdbClient(FacilitySoftwareConfig config) {
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
   
   public ResponseEntity<String> getWireMap(String deviceName){
      return this.restTemplate.
            exchange(getServiceEndPoint()+String.format(GetwiremapURL, deviceName), 
                  HttpMethod.GET, RestTemplateBuilder.getDefaultEntity(), String.class);
   }
   
   public boolean checkConnection() {
      ResponseEntity<String> result = this.restTemplate.getForEntity(getServiceEndPoint(), String.class);
      return result.getStatusCodeValue() == HttpStatus.OK.value();
   }
   
}
