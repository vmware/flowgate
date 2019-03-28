/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WormholeDateFormat {
   
   public static long getLongTime(String dateTime,String dateformat) {
      SimpleDateFormat format = new SimpleDateFormat(dateformat);
      Date utc = null;
      long time = -1;
      try {
         utc = format.parse(dateTime);
         time = utc.getTime();
      } catch (ParseException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      return time;
   }
   
   public static long getLongTime(String dateTime,String dateformat,String timezone) {
      SimpleDateFormat format = new SimpleDateFormat(dateformat);
      if(timezone != null && !timezone.trim().isEmpty()) {
         format.setTimeZone(TimeZone.getTimeZone(timezone));
      }
      Date utc = null;
      long time = -1;
      try {
         utc = format.parse(dateTime);
         time = utc.getTime();
      } catch (ParseException e) {
         e.printStackTrace();
      }
      return time;
   }
}
