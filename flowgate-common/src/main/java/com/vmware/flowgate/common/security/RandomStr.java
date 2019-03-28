/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.security;

import java.util.Random;

public class RandomStr {

   public static String getRandomStr(int length) {
      String base = "abcdefghijklmnopqrstuvwxyz0123456789";
      int randomNum;
      char randomChar;
      Random random = new Random();
      StringBuffer str = new StringBuffer();

      for (int i = 0; i < length; i++) {
          randomNum = random.nextInt(base.length());
          randomChar = base.charAt(randomNum);
          str.append(randomChar);
      }
      return str.toString();
  }

}
