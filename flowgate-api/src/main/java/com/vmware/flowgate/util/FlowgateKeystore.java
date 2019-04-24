/**
 * 
 */
package com.vmware.flowgate.util;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowgateKeystore {
   private static Logger logger = LoggerFactory.getLogger(FlowgateKeystore.class);
   private static ReadWriteLock lock = new ReentrantReadWriteLock();

   private static KeyStore keyStore = null;
   private static String guardKeyStorePath;
   private static String guardKeyAlias;
   private static String guardStorePass;

   private static final String JCEKS = "jceks";


   public static void init(String keyPath, String alias, String guardPass) {
      guardKeyStorePath = keyPath;
      guardKeyAlias = alias;
      guardStorePass = guardPass;
      try {
         keyStore = loadKeyStore(guardKeyStorePath, guardStorePass, JCEKS);
      } catch (Exception e) {
         logger.info("Cannot load guardKey store", e);
      }
   }

   public static Lock getWriteLock() {
      logger.debug("Get CMS keystore write lock");
      return lock.writeLock();
   }

   public static Lock getReadLock() {
      logger.debug("Get guard keystore read lock");
      return lock.readLock();
   }

   private static KeyStore loadKeyStore(String path, String password, String keystoreType)
         throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException,
         InterruptedException {
      KeyStore store = null;
      Lock lock = getReadLock();
      if (!lock.tryLock(10, TimeUnit.MINUTES)) {
         throw new CertificateException("ACQUIRE_TRUST_LOCK_TIMEOUT");
      }
      try {
         store = Pkcs12KeystoreUtil.loadKeyStore(path, password, keystoreType);
      } finally {
         lock.unlock();
      }
      return store;
   }

   public static KeyStore loadLicenseKeyStore() throws NoSuchAlgorithmException,
         CertificateException, IOException, KeyStoreException, InterruptedException {
      return loadKeyStore(guardKeyStorePath, guardStorePass, JCEKS);
   }

   public static String getKeyStoreFilePath() {
      return guardKeyStorePath;
   }

   public static String getGuardKeyAlias() {
      return guardKeyAlias;
   }

   public static KeyStore setEncryptKey(String encryptKey) {
      Lock lock = getWriteLock();
      try {
         if (!lock.tryLock(10, TimeUnit.MINUTES)) {
            throw new CertificateException("ACQUIRE_TRUST_LOCK_TIMEOUT");
         }
         KeyStore.PasswordProtection keyStorePP =
               new KeyStore.PasswordProtection(guardStorePass.toCharArray());
         SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
         factory.generateSecret(new PBEKeySpec(encryptKey.toCharArray()));
         SecretKey key = factory.generateSecret(new PBEKeySpec(encryptKey.toCharArray()));
         keyStore.setEntry(guardKeyAlias, new SecretKeyEntry(key), keyStorePP);
         Pkcs12KeystoreUtil.serializeKeyStore(guardKeyStorePath, keyStore, guardStorePass);
      } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
            | InvalidKeySpecException | InterruptedException e) {
         logger.error("Failed to set the key", e);
      } finally {
         lock.unlock();
      }
      return keyStore;
   }

   public static String getEncryptKey() {
      try {
         KeyStore.PasswordProtection keyStorePP =
               new KeyStore.PasswordProtection(guardStorePass.toCharArray());
         KeyStore.SecretKeyEntry ske =
               (KeyStore.SecretKeyEntry) keyStore.getEntry(guardKeyAlias, keyStorePP);
         if (null == ske) {
            return null;
         }
         SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
         PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
         return new String(keySpec.getPassword());
      } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException
            | InvalidKeySpecException e) {
         logger.error("", e);
         return null;
      }
   }

   public static KeyStore getKeyStore() {
      return keyStore;
   }

}
