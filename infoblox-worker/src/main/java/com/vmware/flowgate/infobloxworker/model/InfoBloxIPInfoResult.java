package com.vmware.flowgate.infobloxworker.model;

import org.apache.logging.log4j.util.Strings;

import java.io.Serializable;

public class InfoBloxIPInfoResult implements Serializable {

   private String hostName;
   private String ipAddress;
   private String macAddress;

   public InfoBloxIPInfoResult() {
   }

   public static InfoBloxIPInfoResult build(Infoblox infoblox) {
      InfoBloxIPInfoResult infoBloxIPInfoResult = new InfoBloxIPInfoResult();
      final Infoblox.ipv4addr ipv4addr = infoblox.getIpv4addrs()[0];
      final String zone = infoblox.getZone();
      final String host = ipv4addr.getHost();
      final String mac = ipv4addr.getMac();
      final String discoveredMac = ipv4addr.getDiscoveredData() != null ? ipv4addr.getDiscoveredData().getMacAddress() : null;

      infoBloxIPInfoResult.setIpAddress(ipv4addr.getIpv4addr());
      infoBloxIPInfoResult.setMacAddress(Strings.isBlank(mac) ? discoveredMac : mac);
      infoBloxIPInfoResult.setHostName(Strings.isNotBlank(zone) && host.endsWith(zone) ? host.substring(0, host.length() - zone.length() - 1) : host);
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
