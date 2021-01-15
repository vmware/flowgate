/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

public class ChassisSpecificData {

   private String powerRedundancy;
   private String  expressservicecode;
   private String servicetag;
   private int faceplatePower;
   private String dnsname;
   private String location;
   private String hardwareVersion;
   private String powercapacity;
   public String getPowerRedundancy() {
      return powerRedundancy;
   }
   public void setPowerRedundancy(String powerRedundancy) {
      this.powerRedundancy = powerRedundancy;
   }
   public String getExpressservicecode() {
      return expressservicecode;
   }
   public void setExpressservicecode(String expressservicecode) {
      this.expressservicecode = expressservicecode;
   }
   public String getServicetag() {
      return servicetag;
   }
   public void setServicetag(String servicetag) {
      this.servicetag = servicetag;
   }
   public int getFaceplatePower() {
      return faceplatePower;
   }
   public void setFaceplatePower(int faceplatePower) {
      this.faceplatePower = faceplatePower;
   }
   public String getDnsname() {
      return dnsname;
   }
   public void setDnsname(String dnsname) {
      this.dnsname = dnsname;
   }
   public String getLocation() {
      return location;
   }
   public void setLocation(String location) {
      this.location = location;
   }
   public String getHardwareVersion() {
      return hardwareVersion;
   }
   public void setHardwareVersion(String hardwareVersion) {
      this.hardwareVersion = hardwareVersion;
   }
   public String getPowercapacity() {
      return powercapacity;
   }
   public void setPowercapacity(String powercapacity) {
      this.powercapacity = powercapacity;
   }

}
