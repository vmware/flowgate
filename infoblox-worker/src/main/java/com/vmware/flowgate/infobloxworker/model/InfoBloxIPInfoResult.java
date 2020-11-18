package com.vmware.flowgate.infobloxworker.model;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class InfoBloxIPInfoResult implements Serializable {

   private String hostName;
   private String ipAddress;
   private String macAddress;

   public InfoBloxIPInfoResult() {
   }

   public static InfoBloxIPInfoResult build(String hostName, InfobloxIpv4addressItem infobloxIpv4addressItem) {
      InfoBloxIPInfoResult infoBloxIPInfoResult = new InfoBloxIPInfoResult();
      final String ipAddress = infobloxIpv4addressItem.getIpAddress();
      final String macAddress;
      final String hostNameWithoutZone;
      if (StringUtils.isNotBlank(infobloxIpv4addressItem.getMacAddress())) {
         macAddress = infobloxIpv4addressItem.getMacAddress();
      } else if (infobloxIpv4addressItem.getDiscoveredData() != null && StringUtils.isNotBlank(infobloxIpv4addressItem.getDiscoveredData().getMacAddress())) {
         macAddress = infobloxIpv4addressItem.getDiscoveredData().getMacAddress();
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

   public static InfoBloxIPInfoResult build(InfobloxHostRecordItem infobloxHostRecordItem) {
      InfoBloxIPInfoResult infoBloxIPInfoResult = new InfoBloxIPInfoResult();
      ipv4addr ipv4addr  = infobloxHostRecordItem.getIpv4addrs()[0];
      final String zone = infobloxHostRecordItem.getZone();
      final String hostname = ipv4addr.getHost();
      final String macAddress;
      final String hostNameWithoutZone = StringUtils.isNotBlank(zone) && hostname.endsWith(zone) ? hostname.substring(0, hostname.length() - zone.length() - 1) : hostname;
      if (StringUtils.isNotBlank(ipv4addr.getMac())) {
         macAddress = ipv4addr.getMac();
      } else if (ipv4addr.getDiscoveredData() != null && StringUtils.isNotBlank(ipv4addr.getDiscoveredData().getMacAddress())) {
         macAddress = ipv4addr.getDiscoveredData().getMacAddress();
      } else {
         macAddress = null;
      }

      infoBloxIPInfoResult.setIpAddress(ipv4addr.getIpv4addr());
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
