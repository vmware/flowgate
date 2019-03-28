/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPAddressUtil {
   public static boolean isValidIp(long addr) {
      InetAddress ip = getAddressFromLong(addr);

      return !ip.isLoopbackAddress() && !ip.isMulticastAddress();
   }

   public static boolean isValidIp(String ipAddr) {
      Long ip = getAddressAsLong(ipAddr);
      if (ipAddr == null || null == ip) {
         return false;
      }

      return isValidIp(ip);
   }

   public static boolean isValidIp(long netmask, long addr) {
      long hostPart = addr & ~netmask;
      int bits = 32 - getNetworkPrefixBits(netmask);

      // should not be network and broadcast addresses (all 0 and all 1)
      if (hostPart == 0 || hostPart == (1L << bits) - 1) {
         return false;
      }

      return isValidIp(addr);
   }

   public static boolean isValidIp(String netmask, String ipAddr) {
      Long mask = getAddressAsLong(netmask);
      Long ip = getAddressAsLong(ipAddr);
      if (mask == null || ipAddr == null) {
         return false;
      }

      return isValidIp(mask, ip);
   }

   public static InetAddress getAddressFromLong(Long addr) {
      if (addr == null) {
         return null;
      }

      if (addr < 0 || addr >= (1L << 32)) {
         return null;
      }
      ;

      byte[] bytes = new byte[] { (byte) ((addr >> 24) & 0xff), (byte) ((addr >> 16) & 0xff),
            (byte) ((addr >> 8) & 0xff), (byte) (addr & 0xff) };

      try {
         return InetAddress.getByAddress(bytes);
      } catch (UnknownHostException e) {
         return null;
      }
   }

   public static Long getAddressAsLong(InetAddress addr) {
      if (addr == null) {
         return null;
      }

      byte[] bytes = addr.getAddress();
      if (bytes.length != 4) {
         return null;
      }

      // byte & 0xff is a hack to use java unsigned byte
      return ((bytes[0] & 0xffL) << 24) + ((bytes[1] & 0xffL) << 16) + ((bytes[2] & 0xffL) << 8)
            + (bytes[3] & 0xffL);
   }

   public static Long getAddressAsLong(String addr) {
      if (addr == null) {
         return null;
      }

      String[] parts = addr.split("\\.");
      if (parts.length != 4) {
         return null;
      }

      long ip = 0;
      try {
         for (int i = 0; i < 4; ++i) {
            long part = Integer.parseInt(parts[i]);
            if (part < 0 || part > 255) {
               return null;
            }

            ip += part << ((3 - i) * 8);
         }
      } catch (NumberFormatException ex) {
         return null;
      }

      return ip;
   }

   public static int getNetworkPrefixBits(long netmask) {
      if (netmask < 0 || netmask >= (1L << 32)) {
         return -1;
      }
      ;

      int i = 0;
      long tmp = netmask;
      while (i <= 32) {
         if ((tmp & 1L) == 1L) {
            long expected = ((1L << 32) - 1L) >> i;
            if ((expected & tmp) == expected) {
               return 32 - i;
            } else {
               return -1;
            }
         }
         ++i;
         tmp = tmp >> 1;
      }

      return -1;
   }

   public static String getIpAddressByHost(String hostname) throws UnknownHostException {
      InetAddress address = InetAddress.getByName(hostname);
      return address.getHostAddress();
   }

   public static String getIPAddress(String address) {
      if (IPAddressUtil.isValidIp(address)) {
        return address;
      }else {
         try {
            return IPAddressUtil.getIpAddressByHost(address);
         } catch (UnknownHostException e) {
            return null;
         }
      }
   }

}
