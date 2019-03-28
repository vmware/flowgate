/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.management.util;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataUtil {

   public static String getUUID() {
      return UUID.randomUUID().toString().replace("-", "");
   }

   /**
    *
    * @param str
    * @return
    */
   public static boolean checkMobile(String str) {
      if (str == null) {
         return false;
      }
      Pattern regex = Pattern.compile("^[1][3,4,5,7,8][0-9]{9}$");
      Matcher m = regex.matcher(str);
      return m.matches();
   }

   /**
    *
    * @param str
    * @return
    */
   public static boolean checkMail(String str) {
      if (str == null) {
         return false;
      }
      Pattern regex = Pattern.compile(
            "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$");
      Matcher m = regex.matcher(str);
      return m.matches();
   }

}
