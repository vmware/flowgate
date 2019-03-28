/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vcworker.client;

import java.io.Closeable;
import java.security.KeyStore;

import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.protocol.HttpConfiguration;
import com.vmware.vapi.protocol.HttpConfiguration.KeyStoreConfig;
import com.vmware.vapi.protocol.HttpConfiguration.SslConfiguration;

import vmware.samples.common.SslUtil;
import vmware.samples.common.authentication.VapiAuthenticationHelper;
import vmware.samples.common.authentication.VimAuthenticationHelper;

public abstract class VSphereAutomationClientBase implements AutoCloseable, Closeable {

   protected String server;
   protected String username;
   protected String password;
   protected boolean clearData;
   protected boolean skipServerVerification;
   protected String truststorePath;
   protected String truststorePassword;
   protected String configFile;
   protected VimAuthenticationHelper vimAuthHelper;
   protected VapiAuthenticationHelper vapiAuthHelper;
   protected StubConfiguration sessionStubConfig;

   //   private final Logger logger = LoggerFactory.getLogger(VSphereClientBase.class);
   protected VSphereAutomationClientBase(String server, String userName, String password,
         boolean skipCertVerification) {
      this.server = server;
      this.username = userName;
      this.password = password;
      this.skipServerVerification = skipCertVerification;
   }

   protected void login() throws Exception {
      this.vapiAuthHelper = new VapiAuthenticationHelper();
      this.vimAuthHelper = new VimAuthenticationHelper();
      HttpConfiguration httpConfig = buildHttpConfiguration();
      this.sessionStubConfig = vapiAuthHelper.loginByUsernameAndPassword(this.server, this.username,
            this.password, httpConfig);
      this.vimAuthHelper.loginByUsernameAndPassword(this.server, this.username, this.password);
   }

   protected HttpConfiguration buildHttpConfiguration() throws Exception {
      HttpConfiguration httpConfig = new HttpConfiguration.Builder()
            .setSslConfiguration(buildSslConfiguration()).getConfig();

      return httpConfig;
   }

   protected SslConfiguration buildSslConfiguration() throws Exception {
      SslConfiguration sslConfig;

      if (this.skipServerVerification) {
         /*
          * Below method enables all VIM API connections to the server
          * without validating the server certificates.
          *
          * Note: Below code is to be used ONLY IN DEVELOPMENT ENVIRONMENTS.
          * Circumventing SSL trust is unsafe and should not be used in
          * production software.
          */
         SslUtil.trustAllHttpsCertificates();

         /*
          * Below code enables all vAPI connections to the server
          * without validating the server certificates..
          *
          * Note: Below code is to be used ONLY IN DEVELOPMENT ENVIRONMENTS.
          * Circumventing SSL trust is unsafe and should not be used in
          * production software.
          */
         sslConfig = new SslConfiguration.Builder().disableCertificateValidation()
               .disableHostnameVerification().getConfig();
      } else {
         /*
          * Set the system property "javax.net.ssl.trustStore" to
          * the truststorePath
          */
         System.setProperty("javax.net.ssl.trustStore", this.truststorePath);
         KeyStore trustStore = SslUtil.loadTrustStore(this.truststorePath, this.truststorePassword);
         KeyStoreConfig keyStoreConfig = new KeyStoreConfig("", this.truststorePassword);
         sslConfig = new SslConfiguration.Builder().setKeyStore(trustStore)
               .setKeyStoreConfig(keyStoreConfig).getConfig();
      }

      return sslConfig;
   }

   protected void logout() throws Exception {
      this.vapiAuthHelper.logout();
      this.vimAuthHelper.logout();
      this.vapiAuthHelper = null;
      this.vimAuthHelper = null;
      this.sessionStubConfig = null;
   }

   @Override
   public void close() {
      try {
         logout();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
