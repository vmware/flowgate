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
package com.vmware.flowgate.util;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.security.SaltGenerator;

public class EncryptionGuard {

   private static final String UTF8_ENCODING = "UTF8";

   // explicitly declare algorithm, block mode, padding mode.
   // once need to change them, change the internal methods if necessary.
   private static final String ALGORITHM = "AES";
   private static final String BLOCK_MODE = "CBC";
   private static final String PADDING = "PKCS5Padding";
   private static final String TRANSFORMATION = ALGORITHM + "/" + BLOCK_MODE
         + "/" + PADDING;
   private static final int DEFAULT_IV_BYTES = 16;

   // initialization vector required for CBC
   private static final byte[] IV_PARAMETER = {
         (byte) 0x51, (byte) 0x2c, (byte) 0x3a, (byte) 0xb4, (byte) 0x87,
         (byte) 0xa0, (byte) 0xa1, (byte) 0x79, (byte) 0x56, (byte) 0x73,
         (byte) 0x56, (byte) 0x7d, (byte) 0xc2, (byte) 0x1f, (byte) 0xeb,
         (byte) 0x73
   };
   // fix salt size
   private static final int SALT_SIZE = 16;

   private EncryptionGuard() { }

   /**
    * Encrypt the clear text against given secret key.
    *
    * @param clearText
    *           the clear string
    * @return the encrypted string, or null if the clear string is null
    * @throws WormholeException
    *            if input arguments is null
    */
   public static String encode(String clearText)
         throws GeneralSecurityException, UnsupportedEncodingException {
      if (clearText == null) {
         return null;
      }
      String initKey = FlowgateKeystore.getEncryptKey();
      Key key = new SecretKeySpec(initKey.getBytes(), "AES");
      String salt = SaltGenerator.genRandomString(SALT_SIZE);

      String inputText = salt + clearText; // add salt
      byte[] clearBytes = inputText.getBytes(UTF8_ENCODING);

      Cipher cipher = getCiperInternal(Cipher.ENCRYPT_MODE, key);
      byte[] encryptedBytes = cipher.doFinal(clearBytes);
      return salt + Base64.getEncoder().encodeToString(encryptedBytes);
   }

   /**
    * Decrypt the encrypted text against given secret key.
    *
    * @param encodedText
    *           the encrypted string
    * @return the clear string, or null if encrypted string is null
    * @throws WormholeException
    *            if input arguments is null
    */
   public static String decode(String encodedText)
         throws GeneralSecurityException, UnsupportedEncodingException {
      if (encodedText == null) {
         return null;
      }

      if (encodedText.length() < SALT_SIZE) {
         throw new WormholeException("This encodedText is invalid");
         //throw EncryptionException.SHORT_ENCRYPTED_STRING(encodedText);
      }
      String initKey = FlowgateKeystore.getEncryptKey();
      Key key = new SecretKeySpec(initKey.getBytes(), "AES");
      encodedText.substring(0, SALT_SIZE);
      String encryptedText = encodedText.substring(SALT_SIZE);

      byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
      Cipher cipher = getCiperInternal(Cipher.DECRYPT_MODE, key);
      byte[] outputBytes = cipher.doFinal(encryptedBytes);

      String outputText = new String(outputBytes, UTF8_ENCODING);
      // Assert salt
      //AuAssert.check(salt.equals(outputText.substring(0, SALT_SIZE)));

      return outputText.substring(SALT_SIZE);
   }

   /**
    * Get a cipher instance when need it, not share between multiple threads
    * because cipher is not thread safe.
    *
    * @param opmode
    *           the operation mode
    * @param key
    *           the key
    * @return initialized cipher instance
    */
   private static Cipher getCiperInternal(int opmode, Key key)
         throws GeneralSecurityException {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      IvParameterSpec ips = new IvParameterSpec(
            IV_PARAMETER, 0, DEFAULT_IV_BYTES);
      cipher.init(opmode, key, ips);
      return cipher;
   }
}
