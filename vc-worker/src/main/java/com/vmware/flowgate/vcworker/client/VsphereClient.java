/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vcworker.client;

import java.io.Closeable;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.flowgate.vcworker.model.VCConstants;
import com.vmware.vim.binding.impl.vmodl.TypeNameImpl;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.CustomFieldsManager;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.PerformanceManager;
import com.vmware.vim.binding.vim.ServiceInstance;
import com.vmware.vim.binding.vim.ServiceInstanceContent;
import com.vmware.vim.binding.vim.SessionManager;
import com.vmware.vim.binding.vim.UserSession;
import com.vmware.vim.binding.vim.fault.DuplicateName;
import com.vmware.vim.binding.vim.fault.InvalidPrivilege;
import com.vmware.vim.binding.vim.version.version10;
import com.vmware.vim.binding.vim.view.ViewManager;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.TypeName;
import com.vmware.vim.binding.vmodl.query.PropertyCollector;
import com.vmware.vim.vmomi.client.Client;
import com.vmware.vim.vmomi.client.common.ProtocolBinding;
import com.vmware.vim.vmomi.client.http.HttpClientConfiguration;
import com.vmware.vim.vmomi.client.http.HttpConfiguration;
import com.vmware.vim.vmomi.client.http.ThumbprintVerifier;
import com.vmware.vim.vmomi.client.http.impl.HttpConfigurationImpl;
import com.vmware.vim.vmomi.core.impl.BlockingFuture;
import com.vmware.vim.vmomi.core.types.VmodlContext;

public class VsphereClient implements AutoCloseable, Closeable {


   private static final String VIEW_MANAGER_IS_NULL_FOR = "ViewManager is null for %s.";
   private static final String PROPERTY_COLLECTOR_IS_NULL_FOR = "PropertyCollector is null for %s.";
   private static final String NO_CONNECTION_TO_V_SPHERE = "No connection to vSphere.";
   private static final String EN = "en";
   private static final String SERVICE_INSTANCE = "ServiceInstance";
   private static final String HTTP = "http";
   private static final String COM_VMWARE_VIM_BINDING_VMODL_REFLECT =
         "com.vmware.vim.binding.vmodl.reflect";
   private static final String COM_VMWARE_VIM_BINDING_VIM = "com.vmware.vim.binding.vim";
   private static final String COMPUTE_RESOURCE = "ComputeResource";
   private static final Logger logger = LoggerFactory.getLogger(VsphereClient.class);
   private ThreadPoolExecutor executor;
   private HttpConfiguration httpConfig;
   private Client client;
   private String uri;
   private ServiceInstanceContent sic;
   private SessionManager sessionMgr;
   private CustomFieldsManager cfm;
   private UserSession userSession;
   private String vcUUID;
   private PropertyCollector propertyCollector;
   private ViewManager viewManager;
   private PerformanceManager performanceManager;

   private static final String https_sdk_tunnel_8089 = "https://sdkTunnel:8089";

   // This is indicate if the client is connected as an extension.
   private boolean extension = false;

   // If connected as an extension then the extension key being used.
   private String extensionKey = null;

   static {
      VmodlContext context = VmodlContext.initContext(
            new String[] { COM_VMWARE_VIM_BINDING_VIM, COM_VMWARE_VIM_BINDING_VMODL_REFLECT });
   }

   private VsphereClient(String uri, String name, String password, boolean skipCertVerify)
         throws Exception {
      this.uri = uri;
      URI serviceUri = new URI(uri);
      executor = new ThreadPoolExecutor(1, // core pool size
            1, // max pool size
            10, TimeUnit.SECONDS, // max thread idle time
            new LinkedBlockingQueue<Runnable>()); // work queue

      HttpClientConfiguration clientConfig = HttpClientConfiguration.Factory.newInstance();

      httpConfig = new HttpConfigurationImpl();
      httpConfig.setThumbprintVerifier(new ThumbprintVerifier() {
         @Override
         public Result verify(String thumbprint) {
            return Result.MATCH; // ignore verify
         }

         @Override
         public void onSuccess(X509Certificate[] chain, String thumbprint, Result verifyResult,
               boolean trustedChain, boolean verifiedAssertions) throws SSLException {

         }
      });

      clientConfig.setExecutor(executor);
      clientConfig.setHttpConfiguration(httpConfig);
      try {
         VmodlContext.getContext();
      } catch (IllegalStateException ex) {
         VmodlContext.initContext(new String[] { COM_VMWARE_VIM_BINDING_VIM });
      }
      client = Client.Factory.createClient(serviceUri, version10.class, clientConfig);
      ManagedObjectReference svcRef = new ManagedObjectReference();
      svcRef.setType(SERVICE_INSTANCE);
      svcRef.setValue(SERVICE_INSTANCE);

      ServiceInstance si = client.createStub(ServiceInstance.class, svcRef);

      sic = si.retrieveContent();
      vcUUID = sic.getAbout().getInstanceUuid();
      sessionMgr = client.createStub(SessionManager.class, sic.getSessionManager());
      cfm = client.createStub(CustomFieldsManager.class, sic.getCustomFieldsManager());

      BlockingFuture<UserSession> userSessionFuture = new BlockingFuture<UserSession>();
      sessionMgr.login(name, password, null, userSessionFuture);
      userSession = userSessionFuture.get();

      logger.info("Successfully login to {}", uri);
   }

   public static VsphereClient connect(String uri, String username, String password)
         throws Exception {
      return new VsphereClient(uri, username, password, false);
   }

   public static VsphereClient connect(String uri, String username, String password,
         boolean skipCertVerify) throws Exception {
      return new VsphereClient(uri, username, password, skipCertVerify);
   }

   //   public static VsphereClient connect(String uri, LocalCertificatesService service)
   //         throws Exception {
   //      return new VsphereClient(uri, null, null, service);
   //   }

   public Client getClient() {
      return client;
   }

   public UserSession getUserSession() {
      return userSession;
   }

   public boolean isExtension() {
      return extension;
   }

   public String getExtensionKey() {
      if (extension)
         return extensionKey;
      else
         return null;
   }

   public <T extends ManagedObject> T createStub(Class<T> clazz, ManagedObjectReference moRef) {
      return client.createStub(clazz, moRef);
   }

   public ProtocolBinding getBinding() {
      return client.getBinding();
   }

   public void shutdown() {
      sessionMgr.logout();
      sessionMgr = null;
      client.shutdown();
      client = null;
      if (executor != null) {
         int count = 0;
         executor.shutdown();
         try {
            while (!executor.isTerminated()) {
               executor.awaitTermination(2, TimeUnit.SECONDS);
               if (!executor.isTerminated()) {
                  ++count;
                  logger.warn("started terminating " + " for executor " + executor + " for "
                        + count * 2 + " seconds");
               }
            }
         } catch (InterruptedException e) {
            logger.error("", e);
         }
      }
      logger.info("shutdown vc connection");
   }

   public ServiceInstanceContent getServiceInstanceContent() {
      // TODO: define more specific exception.
      if (null == client)
         throw new NullPointerException(NO_CONNECTION_TO_V_SPHERE);
      return sic;
   }

   public PerformanceManager getPerformanceManager() {
      if(performanceManager == null) {
         performanceManager = client.createStub(PerformanceManager.class, sic.getPerfManager());
      }
      return performanceManager;
   }

   public PropertyCollector getPropertyCollector() {
      if (null == getServiceInstanceContent().getPropertyCollector())
         throw new NullPointerException(String.format(PROPERTY_COLLECTOR_IS_NULL_FOR, uri));
      if (null == propertyCollector)
         propertyCollector = createStub(PropertyCollector.class, sic.getPropertyCollector());
      return propertyCollector;
   }

   public ViewManager getViewManager() {
      if (null == getServiceInstanceContent().getViewManager())
         throw new NullPointerException(String.format(VIEW_MANAGER_IS_NULL_FOR, uri));
      if (null == viewManager)
         viewManager = createStub(ViewManager.class, sic.getViewManager());
      return viewManager;
   }

   public ManagedObjectReference getRootRef() {
      return getServiceInstanceContent().getRootFolder();
   }

   public ManagedObjectReference getContainerView(String typeName) {
      return getViewManager().createContainerView(getRootRef(),
            new TypeName[] { new TypeNameImpl(typeName) }, true);
   }

   private <T extends ManagedObject> Collection<T> getResourcesByType(Class<T> clazz, String type) {
      ManagedObjectReference[] crRef = InventoryService.getInstance()
            .findAll(getPropertyCollector(), getContainerView(type), type);
      ArrayList<T> res = new ArrayList<T>();
      for (ManagedObjectReference ref : crRef) {

         res.add(client.createStub(clazz, ref));
      }
      return res;
   }

   public Collection<HostSystem> getAllHost() {
      return getResourcesByType(HostSystem.class, VCConstants.HOSTSYSTEM);
   }
   
   public Collection<ClusterComputeResource> getAllClusterComputeResource() {
      return getResourcesByType(ClusterComputeResource.class, VCConstants.CLUSTERCOMPUTERESOURCE);
   }
   
   public void createCustomAttribute(String name, String type)
         throws InvalidPrivilege, DuplicateName {

      TypeName ty = new TypeNameImpl(type);
      try {
         cfm.addFieldDefinition(name, ty, null, null);
      } catch (Exception e) {
         if (e instanceof DuplicateName) {
            logger.debug("Customer attribute already exsit.", e);
         } else {
            throw e;
         }
      }
   }

   public String getVCUUID() {
      return vcUUID;
   }

   @Override
   public void close() {
      this.shutdown();
   }
}