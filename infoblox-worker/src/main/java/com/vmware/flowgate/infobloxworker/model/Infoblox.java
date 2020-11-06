/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.flowgate.infobloxworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Infoblox {

   @JsonProperty(value = "_ref")
   private String ref;
   @JsonProperty(value = "ipv4addrs")
   private ipv4addr[] ipv4addrs;
   @JsonProperty(value = "name")
   private String name;
   @JsonProperty(value = "zone")
   private String zone;

   static class ipv4addr {

      @JsonProperty(value = "_ref")
      private String ref;
      @JsonProperty(value = "discovered_data")
      private DiscoveredData discoveredData;
      @JsonProperty(value = "host")
      private String host;
      @JsonProperty(value = "ipv4addr")
      private String ipv4addr;
      @JsonProperty(value = "mac")
      private String mac;

      public String getRef() {
         return ref;
      }
      public void setRef(String ref) {
         this.ref = ref;
      }
      public DiscoveredData getDiscoveredData() {
         return discoveredData;
      }
      public void setDiscoveredData(DiscoveredData discoveredData) {
         this.discoveredData = discoveredData;
      }
      public String getHost() {
         return host;
      }

      public void setHost(String host) {
         this.host = host;
      }
      public String getIpv4addr() {
         return ipv4addr;
      }
      public void setIpv4addr(String ipv4addr) {
         this.ipv4addr = ipv4addr;
      }
      public String getMac() {
         return mac;
      }
      public void setMac(String mac) {
         this.mac = mac;
      }

      static class DiscoveredData {

         @JsonProperty(value = "first_discovered")
         private long firstDiscovered;
         @JsonProperty(value = "last_discovered")
         private long lastDiscovered;
         @JsonProperty(value = "mac_address")
         private String macAddress;
         @JsonProperty(value = "os")
         private String os;

         public long getFirstDiscovered() {
            return firstDiscovered;
         }
         public void setFirstDiscovered(long firstDiscovered) {
            this.firstDiscovered = firstDiscovered;
         }
         public long getLastDiscovered() {
            return lastDiscovered;
         }
         public void setLastDiscovered(long lastDiscovered) {
            this.lastDiscovered = lastDiscovered;
         }
         public String getMacAddress() {
            return macAddress;
         }
         public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
         }
         public String getOs() {
            return os;
         }

         public void setOs(String os) {
            this.os = os;
         }
      }

   }

   public String getRef() {
      return ref;
   }
   public void setRef(String ref) {
      this.ref = ref;
   }
   public ipv4addr[] getIpv4addrs() {
      return ipv4addrs;
   }
   public void setIpv4addrs(ipv4addr[] ipv4addrs) {
      this.ipv4addrs = ipv4addrs;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getZone() {
      return zone;
   }
   public void setZone(String zone) {
      this.zone = zone;
   }

}
