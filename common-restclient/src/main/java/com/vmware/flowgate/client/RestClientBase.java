/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.client;

import javax.annotation.Resource;

import org.springframework.web.client.RestTemplate;


public class RestClientBase {

   @Resource(name = "restTemplate")
   protected RestTemplate restTemplate;

   public RestClientBase() {
   }

//   protected HttpHeaders buildHeaders(boolean withCookie) {
//      HttpHeaders headers = new HttpHeaders();
//      headers.setContentType(MediaType.APPLICATION_JSON);
//      List<MediaType> acceptedTypes = new ArrayList<MediaType>();
//      acceptedTypes.add(MediaType.APPLICATION_JSON);
//      acceptedTypes.add(MediaType.TEXT_HTML);
//      headers.setAccept(acceptedTypes);
//      return headers;
//   }
//
//   protected HttpHeaders buildHeaders() {
//      return buildHeaders(true);
//   }
//
//   protected HttpEntity<String> getDefaultEntity() {
//      return new HttpEntity<String>(buildHeaders());
//   }


}
