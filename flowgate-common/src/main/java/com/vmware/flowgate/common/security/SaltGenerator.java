/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.flowgate.common.security;

import java.security.SecureRandom;

public class SaltGenerator {
   private static final int SALT_SIZE = 16;
   private static final String SHA512_SALT_ROUNDS_PREFIX = "rounds=100000$";
   private static SecureRandom rand = new SecureRandom();

   /**
    * Don't let anyone instantiate this class.
    */
   private SaltGenerator() { }

   /**
    * Generate a salt string. <br>
    *
    * @return The salt string.
    */
   public static String genSalt() {
      return SHA512_SALT_ROUNDS_PREFIX + genRandomString(SALT_SIZE);
   }

   /**
    * Generate a random string. <br>
    *
    * @return The salt string.
    */
   public static String genRandomString(int length) {
      final char[] charset = new char[] {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z', '~', '!', '@', '#', '$', '%', '^', '&',
            '*', '(', ')', '-', '+', '`', '=', '<', '>', '?', ',', '.',
            '/', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
      StringBuffer sbuf = new StringBuffer();
      for (int i = 0; i < length; i++) {
         char c = charset[rand.nextInt(charset.length)];
         sbuf.append(c);
      }
      return sbuf.toString();
   }
}

