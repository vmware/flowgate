package com.vmware.flowgate.infobloxworker.model;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class InfoBloxIPInfoResult implements Serializable {

   private String hostName;
   private String ipAddress;
   private String macAddress;

   public InfoBloxIPInfoResult() {
   }

   public static InfoBloxIPInfoResult build(String hostName, Infoblox infoblox) {
      InfoBloxIPInfoResult infoBloxIPInfoResult = new InfoBloxIPInfoResult();
      final String ipAddress = infoblox.getIpAddress();
      final String macAddress;
      final String hostNameWithoutZone;
      if (StringUtils.isNotBlank(infoblox.getMacAddress())) {
         macAddress = infoblox.getMacAddress();
      } else if (infoblox.getDiscoveredData() != null && StringUtils.isNotBlank(infoblox.getDiscoveredData().getMacAddress())) {
         macAddress = infoblox.getDiscoveredData().getMacAddress();
      } else {
         macAddress = null;
      }
      int index = hostName.indexOf(".");
      if (index > 0) {
         hostNameWithoutZone = hostName.substring(0, index);
      } else {
         hostNameWithoutZone = hostName;
      }

      infoBloxIPInfoResult.setIpAddress(ipAddress);
      infoBloxIPInfoResult.setMacAddress(macAddress);
      infoBloxIPInfoResult.setHostName(hostNameWithoutZone);
      return infoBloxIPInfoResult;
   }

   public String getHostName() {
      return hostName;
   }

   public void setHostName(String hostName) {
      this.hostName = hostName;
   }

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public String getMacAddress() {
      return macAddress;
   }

   public void setMacAddress(String macAddress) {
      this.macAddress = macAddress;
   }

   @Override
   public String toString() {
      return "InfoBloxResult{" +
               "hostName='" + hostName + '\'' +
               ", ipAddress='" + ipAddress + '\'' +
               ", macAddress='" + macAddress + '\'' +
               '}';
   }
}
