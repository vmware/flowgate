/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class WormholeDateFormat {
   private static final String CIMDateFormat = "yyyyMMddHHmmss.SSSSSSZZZZZ";
   private static final String PowerManageDateFormat = "yyyy-MM-dd HH:mm:ss.SSSSSS";
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

   /**
    * Reference the DMTF-DSP0004 document, the part before the point is local time.
    * For example, Monday, May 25, 1998, at 1:30:15 PM EST
    * is represented in datetime timestamp format 19980525133015.0000000-300
    * @param cimDate
    * @return Returns the number of milliseconds since January 1, 1970, 00:00:00 GMTrepresented by this Date object.
    */
   public static long cimDateToMilliseconds(String cimDate) {
      int offsetInMinutes = Integer.parseInt(cimDate.substring(22));
      LocalTime offsetAsLocalTime = LocalTime.MIN.plusMinutes(offsetInMinutes);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(CIMDateFormat);
      String inputModified = cimDate.substring(0, 22) + offsetAsLocalTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
      OffsetDateTime dateTime = OffsetDateTime.parse(inputModified,formatter);
      return dateTime.toInstant().toEpochMilli();
   }

   /**
    * For example 2019-07-02 13:45:05.147666
    * Format "yyyy-MM-dd HH:mm:ss.SSSSSS";
    * @param dateTime
    * @return
    */
   public static long getTimeMilliseconds(String dateTime) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PowerManageDateFormat);
      LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
      return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
   }
}
