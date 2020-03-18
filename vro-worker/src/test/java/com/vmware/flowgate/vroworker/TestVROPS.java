/**
 *
 */
package com.vmware.flowgate.vroworker;

import java.util.List;

import com.vmware.ops.api.client.Client;
import com.vmware.ops.api.client.Client.ClientConfig;
import com.vmware.ops.api.client.controllers.ResourcesClient;
import com.vmware.ops.api.model.auth.AuthToken;
import com.vmware.ops.api.model.auth.UsernamePassword;
import com.vmware.ops.api.model.resource.ResourceDto;
import com.vmware.ops.api.model.resource.ResourceQuery;

/**
 * @author admin
 *
 */
public class TestVROPS {

   private final Client client;

   public TestVROPS(String apiUrl, String userName, String password) {
      try {
         Client tokentClient = ClientConfig.builder().useJson()
               .serverUrl(apiUrl).verify("false").ignoreHostName(true)
               .useInternalApis(false).build().newClient();
         UsernamePassword up = new UsernamePassword(userName, password);
         AuthToken token = tokentClient.userAndAuthManagementClient().acquireToken(up);
         client = ClientConfig.builder().tokenAuth(token.getToken()).useJson()
               .serverUrl(apiUrl).verify("false").ignoreHostName(true)
               .useInternalApis(false).build().newClient();

      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   public static void main(String[] args) {
      // TODO Auto-generated method stub
      TestVROPS vrops = new TestVROPS("https://localhost/suite-api", "admin", "Admin!23");

      List<ResourceDto> hosts = vrops.getHostSystemResources();

      System.out.println(hosts.size());
   }

   public List<ResourceDto> getHostSystemResources() {
      ResourcesClient rClient = client.resourcesClient();
      ResourceQuery rq = new ResourceQuery();
      rq.setAdapterKind(new String[] { "VMWARE" });
      rq.setResourceKind(new String[] { "HostSystem" });
      return rClient.getResources(rq, null).getResourceList();
   }
}
