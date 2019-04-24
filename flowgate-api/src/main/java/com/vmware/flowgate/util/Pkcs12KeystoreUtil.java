/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

public class Pkcs12KeystoreUtil {
   private static final String KEYSTORE_TYPE = "PKCS12";
   private static Logger logger = LoggerFactory.getLogger(Pkcs12KeystoreUtil.class);

   public static KeyStore loadKeyStore(String storeFilePath, String storePasswd)
         throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
      return loadKeyStore(storeFilePath, storePasswd, null);
   }


   public static KeyStore loadKeyStore(String storeFilePath, String storePasswd,
         String keystoreType)
         throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
      InputStream in = null;
      KeyStore store = null;
      if (null == keystoreType) {
         keystoreType = KEYSTORE_TYPE;
      }
      try {
         store = KeyStore.getInstance(keystoreType);
         File ks = ResourceUtils.getFile(storeFilePath);
         if (ks.exists()) {
            in = new FileInputStream(ks);
         } else {
            logger.error("Keystore " + storeFilePath + " doesn't exist");
         }
         store.load(in, storePasswd.toCharArray());
      } finally {
         if (in != null) {
            try {
               in.close();
            } catch (IOException ioe) {
               logger.error("Failed to close iput stream." + ioe);
            }
         }
      }
      return store;
   }

   public static void serializeKeyStore(String storeFilePath, KeyStore store, String storePasswd)
         throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
      OutputStream outStream = null;
      try {
         outStream = new FileOutputStream(new File(storeFilePath));
         store.store(outStream, storePasswd.toCharArray());
      } finally {
         if (outStream != null) {
            try {
               outStream.close();
            } catch (IOException ioe) {
               logger.error("Failed to close output stream." + ioe);
            }
         }
      }
   }
}
