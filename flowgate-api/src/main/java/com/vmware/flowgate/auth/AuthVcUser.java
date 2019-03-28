/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.vim.binding.vim.ServiceInstance;
import com.vmware.vim.binding.vim.ServiceInstanceContent;
import com.vmware.vim.binding.vim.SessionManager;
import com.vmware.vim.binding.vim.UserSession;
import com.vmware.vim.binding.vim.fault.InvalidLocale;
import com.vmware.vim.binding.vim.fault.InvalidLogin;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.version.version2;
import com.vmware.vim.vmomi.client.Client;
import com.vmware.vim.vmomi.client.http.HttpClientConfiguration;
import com.vmware.vim.vmomi.client.http.HttpConfiguration;
import com.vmware.vim.vmomi.client.http.ThumbprintVerifier;
import com.vmware.vim.vmomi.client.http.impl.HttpConfigurationImpl;
import com.vmware.vim.vmomi.core.types.VmodlContext;

public class AuthVcUser {
   private final static Log logger = LogFactory.getLog(AuthVcUser.class);
   private static final Class<?> version = version2.class;
   private String vcThumbprint;
   private String serviceUrl;
   private boolean skipCertVerify = false;
   static {
      try {
         VmodlContext.getContext();
      } catch (IllegalStateException ex) {
         VmodlContext.initContext(new String[] { "com.vmware.vim.binding.vim" });
      }
   }

   public AuthVcUser(String vcHost, int vcPort) {
      this(vcHost, vcPort, null);
   }

   public AuthVcUser(String vcHost, int vcPort, boolean skipCertVerify) {
      this(vcHost, vcPort);
      this.skipCertVerify=skipCertVerify;
   }

   public AuthVcUser(String vcHost, int vcPort, String vcThumbprint) {
      serviceUrl = "https://" + vcHost + ":" + vcPort + "/sdk";
      this.vcThumbprint = vcThumbprint;
   }

   private ThumbprintVerifier getThumbprintVerifier() {
      return new ThumbprintVerifier() {
         @Override
         public Result verify(String thumbprint) {
            //tempo solution, when connect to another VC, disable certificate verification.
            if (skipCertVerify) {
               logger.info("Skip verify cert");
               return Result.MATCH;
            }

            //default and good behavior.
            if (vcThumbprint != null || vcThumbprint.equalsIgnoreCase(thumbprint)) {
               return Result.MATCH;
            } else {
               return Result.MISMATCH;
            }
         }

         @Override
         public void onSuccess(X509Certificate[] chain, String thumbprint, Result verifyResult,
               boolean trustedChain, boolean verifiedAssertions) throws SSLException {
         }
      };
   }

   public void authenticateUser(String name, String password) throws URISyntaxException, InvalidLogin, InvalidLocale,SSLException  {
      Client vmomiClient = null;
      SessionManager sessionManager = null;
      try {

         URI uri = new URI(serviceUrl);
         HttpConfiguration httpConfig = new HttpConfigurationImpl();
         httpConfig.setThumbprintVerifier(getThumbprintVerifier());
         HttpClientConfiguration clientConfig = HttpClientConfiguration.Factory.newInstance();
         //set customized SSL protocols
         //TlsClientConfiguration tlsClientConfiguration = new TlsClientConfiguration();
         httpConfig.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
         clientConfig.setHttpConfiguration(httpConfig);
         vmomiClient = Client.Factory.createClient(uri, version, clientConfig);

         ManagedObjectReference svcRef = new ManagedObjectReference();
         svcRef.setType("ServiceInstance");
         svcRef.setValue("ServiceInstance");

         if (name == null || name.isEmpty()) { // VC session token auth
            vmomiClient.getBinding().setSession(vmomiClient.getBinding().createSession(password));
         }

         ServiceInstance instance = vmomiClient.createStub(ServiceInstance.class, svcRef);
         ServiceInstanceContent instanceContent = instance.retrieveContent();
         sessionManager =
               vmomiClient.createStub(SessionManager.class, instanceContent.getSessionManager());

         if (name != null && !name.isEmpty()) { // username/passowrd auth
            sessionManager.login(name, password, sessionManager.getDefaultLocale());
            sessionManager.logout();
         } else { // VC session token auth
            UserSession session = sessionManager.getCurrentSession();
            if (session == null) {
               throw new WormholeException("invalid vc session.");
            } else {
               logger.info(session.getUserName() + " is authenticated");
            }

         }
      } finally {
         if (vmomiClient != null) {
            vmomiClient.shutdown();
         }
      }
   }

   public boolean isSkipCertVerify() {
      return skipCertVerify;
   }

   public void setSkipCertVerify(boolean skipCertVerify) {
      this.skipCertVerify = skipCertVerify;
   }

}
