/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.exception.WormholeException;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.AuthToken;
import com.vmware.flowgate.common.model.FacilityAdapter;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.JobConfig;
import com.vmware.flowgate.common.model.JobConfig.JobType;
import com.vmware.flowgate.common.model.MetricData;
import com.vmware.flowgate.common.model.PageModelImp;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.SensorSetting;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.SystemSummary;
import com.vmware.flowgate.common.model.WormholeUser;

@Service
public class WormholeAPIClient extends RestClientBase {

   private static final String fetchSensorDataURL =
         "/v1/assets/server/%s/realtimedata?starttime=%d&duration=%d";

   private static final String GetJobsURL = "/v1/jobs/type/%s";
   private static final String GetSummaryDataURL = "/v1/summary/systemsummary?usecache=%s";

   private static final String SensorSettingURL = "/v1/sensors/setting/page/%s/pagesize/%s";
   private static final String ServerMappingURL = "/v1/assets/mapping";
   private static final String ServerMappingByVCURL = "/v1/assets/mapping/vc/%s";
   private static final String GetVCServersURL = "/v1/sddc/vc";
   private static final String GetAssetByVCURL = "/v1/assets/vc/%s";
   private static final String GetInternalSDDCSoftwareConfigByType = "/v1/sddc/internal/type/%s";

   private static final String ServerMappingByVROURL = "/v1/assets/mapping/vrops/%s";
   private static final String GetVROServersURL = "/v1/sddc/vrops";
   private static final String GetAssetByVROURL = "/v1/assets/vrops/%s";
   private static final String AssetURL = "/v1/assets/batchoperation";
   private static final String SaveAssetURL = "/v1/assets";
   private static final String ServerMappingMergURL = "/v1/assets/mapping/merge/%s/%s";

   private static final String GetAssetBySourceAndTypeURL = "/v1/assets/source/%s/type/%s?currentPage=%s&pageSize=%s";
   private static final String GetFacilitySoftwareByTypeURL = "/v1/facilitysoftware/type/%s";
   private static final String GetFacilitySoftwareInternalByTypeURL = "/v1/facilitysoftware/internal/type/%s";
   private static final String GetMappedAssetURL = "/v1/assets/mappedasset/category/%s";
   private static final String AssetByIdURL = "/v1/assets/%s";
   private static final String GetAssetBySource = "/v1/assets/source/%s?currentPage=%s&pageSize=%s";

   private static final String DeleteRealTimeDatasURL = "/v1/assets/realtimedata/%s";
   private static final String RealTimeDatasURL = "/v1/assets/sensordata/batchoperation";
   private static final String GetFacilitySoftwareById = "/v1/facilitysoftware/%s";
   private static final String GetFacilitySoftwareInternalById = "/v1/facilitysoftware/internal/%s";
   private static final String UpdateFacilitySoftwareStatus = "/v1/facilitysoftware/status";
   private static final String UpdateSDDCSoftwareStatus = "/v1/sddc/status";

   private static final String GetHostNameByIP = "/v1/assets/mapping/hostnameip/ip/%s";
   private static final String HostNameIPMapping = "/v1/assets/mapping/hostnameip";

   private static final String GetServersWithnoPDUInfo = "/v1/assets/pdusisnull";
   private static final String GetServersWithPDUInfo = "/v1/assets/pdusisnotnull";
   private static final String GetAssetsByType = "/v1/assets/type/%s?currentPage=%s&pageSize=%s";
   private static final String GetAssetByName = "/v1/assets/name/%s";
   private static final String GetAllCustomerFacilityAdapters = "/v1/facilityadapter";

   private static final String GetToken = "/v1/auth/token";
   private static final String GetRefreshToken = "/v1/auth/token/refresh";

   @Value("${apiserver.url}")
   protected String apiServiceEndpoint;
   private String serviceKey;
   private String userName;
   private String password;
   protected List<String> cookies = null;
   protected long expired_time = 0;


   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getServiceKey() {
      return serviceKey;
   }

   public void setServiceKey(String serviceKey) {
      this.serviceKey = serviceKey;
   }

   private WormholeUser getUser() {
      WormholeUser user = new WormholeUser();
      user.setUserName(this.userName);
      user.setPassword(this.password);
      return user;
   }

   public String getAPIServiceEndpoint() {
      return apiServiceEndpoint;
   }

   public void setAPIServiceEndpoint(String url) {
      this.apiServiceEndpoint = url;
   }

   public void initAuthenticationWebToken() {
      long time = System.currentTimeMillis();
      if (time > expired_time) {
         String max_age = "";
         cookies = getToken().getHeaders().get("Set-Cookie");
         String[] cookiesValue = cookies.get(0).split(";");
         for (int i = 0; i < cookiesValue.length; i++) {
            if (cookiesValue[i].trim().startsWith("Max-Age")) {
               max_age =
                     cookiesValue[i].trim().substring(cookiesValue[i].trim().lastIndexOf("=") + 1);
               break;
            }
         }
         expired_time = (Long.parseLong(max_age)-5*60) * 1000 + System.currentTimeMillis();
      }
   }

   protected HttpHeaders buildHeaders() {
      HttpHeaders headers = RestTemplateBuilder.getDefaultHeader();
      initAuthenticationWebToken();
      if(cookies != null) {
         headers.put(HttpHeaders.COOKIE, cookies);
      }
      return headers;
   }

   protected HttpEntity<Object> getDefaultEntity() {
      return new HttpEntity<Object>(buildHeaders());
   }

   public ResponseEntity<AuthToken> getToken() {
      HttpEntity<Object> postEntity = null;
      if(this.userName != null && this.password !=null) {
         postEntity = new HttpEntity<Object>(getUser(), RestTemplateBuilder.getDefaultHeader());
         return this.restTemplate.exchange(getAPIServiceEndpoint() + GetToken,
               HttpMethod.POST, postEntity , AuthToken.class);
      }else if(this.serviceKey != null) {
         HttpHeaders headers = RestTemplateBuilder.getDefaultHeader();
         headers.add("serviceKey", this.serviceKey);
         postEntity = new HttpEntity<Object>(headers);
         return this.restTemplate.exchange(getAPIServiceEndpoint() + GetToken,
               HttpMethod.POST, postEntity , AuthToken.class);
      }else {
         throw new WormholeException("Requires authentication information", null);
      }
   }

   public ResponseEntity<JobConfig[]> getJobs(JobType type) {
      return this.restTemplate.exchange(getAPIServiceEndpoint() + String.format(GetJobsURL, type),
            HttpMethod.GET, getDefaultEntity(), JobConfig[].class);
   }

   public ResponseEntity<SDDCSoftwareConfig[]> getVCServers() {
      return this.restTemplate.exchange(getAPIServiceEndpoint() + GetVCServersURL, HttpMethod.GET,
            getDefaultEntity(), SDDCSoftwareConfig[].class);
   }

   public ResponseEntity<SDDCSoftwareConfig[]> getInternalSDDCSoftwareConfigByType(SDDCSoftwareConfig.SoftwareType softwareType) {
      return this.restTemplate.exchange(getAPIServiceEndpoint() + String.format(GetInternalSDDCSoftwareConfigByType, softwareType), HttpMethod.GET,
               getDefaultEntity(), SDDCSoftwareConfig[].class);
   }

   public ResponseEntity<SystemSummary> getSystemSummary(boolean usecache) {
      return this.restTemplate.exchange(getAPIServiceEndpoint() + String.format(GetSummaryDataURL, usecache), HttpMethod.GET,
            getDefaultEntity(), SystemSummary.class);
   }

   public ResponseEntity<Asset[]> getAssetsByVCID(String VCServerID) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(GetAssetByVCURL, VCServerID), HttpMethod.GET,
            getDefaultEntity(), Asset[].class);
   }

   public ResponseEntity<Asset> getAssetByID(String assetID) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(AssetByIdURL, assetID), HttpMethod.GET,
            getDefaultEntity(), Asset.class);
   }

   public ResponseEntity<PageModelImp<Asset>> getAssetsBySourceAndType(String source, AssetCategory type,int currentPage,int pageSize) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(GetAssetBySourceAndTypeURL, source, type, currentPage, pageSize),
            HttpMethod.GET, getDefaultEntity(), new ParameterizedTypeReference<PageModelImp<Asset>>() {});
   }

   public List<Asset> getAllAssetsBySourceAndType(String source, AssetCategory category){
      List<Asset> assets = new ArrayList<Asset>();
      int currentPage = FlowgateConstant.defaultPageNumber;
      PageModelImp<Asset> assetsPage = getAssetsBySourceAndType(source,category,currentPage,FlowgateConstant.maxPageSize).getBody();
      if(assetsPage != null) {
         assets.addAll(assetsPage.getContent());
         while(!assetsPage.isLast()) {
            currentPage++;
            assetsPage = getAssetsBySourceAndType(source,category,currentPage,FlowgateConstant.maxPageSize).getBody();
            assets.addAll(assetsPage.getContent());
         }
      }
      return assets;
   }

   public ResponseEntity<PageModelImp<Asset>> getAssetsByType(AssetCategory type,int currentPage,int pageSize) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(GetAssetsByType, type,currentPage,pageSize), HttpMethod.GET,
            getDefaultEntity(), new ParameterizedTypeReference<PageModelImp<Asset>>() {});
   }

   public List<Asset> getAllAssetsByType(AssetCategory category){
      List<Asset> assets = new ArrayList<Asset>();
      int currentPage = FlowgateConstant.defaultPageNumber;
      PageModelImp<Asset> assetsPage = getAssetsByType(category,currentPage,FlowgateConstant.maxPageSize).getBody();
      if(assetsPage != null) {
         assets.addAll(assetsPage.getContent());
         while(!assetsPage.isLast()) {
            currentPage++;
            assetsPage = getAssetsByType(category,currentPage,FlowgateConstant.maxPageSize).getBody();
            assets.addAll(assetsPage.getContent());
         }
      }
      return assets;
   }

   public ResponseEntity<PageModelImp<Asset>> getAssetsBySource(String source,int currentPage,int pageSize) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(GetAssetBySource, source,currentPage,pageSize), HttpMethod.GET,
            getDefaultEntity(), new ParameterizedTypeReference<PageModelImp<Asset>>() {});
   }

   public List<Asset> getAllAssetsBySource(String source){
      List<Asset> assets = new ArrayList<Asset>();
      int currentPage = FlowgateConstant.defaultPageNumber;
      PageModelImp<Asset> assetsPage = getAssetsBySource(source,currentPage,FlowgateConstant.maxPageSize).getBody();
      if(assetsPage != null) {
         assets.addAll(assetsPage.getContent());
         while(!assetsPage.isLast()) {
            currentPage++;
            assetsPage = getAssetsBySource(source,currentPage,FlowgateConstant.maxPageSize).getBody();
            assets.addAll(assetsPage.getContent());
         }
      }
      return assets;
   }

   public ResponseEntity<Void> deleteRealTimeData(long timeRange) {
      return this.restTemplate.exchange(getAPIServiceEndpoint() + String.format(DeleteRealTimeDatasURL, String.valueOf(timeRange)), HttpMethod.DELETE,
            getDefaultEntity(), Void.class);
   }

   public ResponseEntity<Void> saveAssets(List<Asset> assets) {
      HttpEntity<Object> postEntity =
            new HttpEntity<Object>(assets, buildHeaders());
      return this.restTemplate.exchange(getAPIServiceEndpoint() + AssetURL, HttpMethod.POST,
            postEntity, Void.class);
   }

   public ResponseEntity<Void> removeAssetByID(String assetId) {
      return this.restTemplate.exchange(getAPIServiceEndpoint() + String.format(AssetByIdURL, assetId), HttpMethod.DELETE,
            getDefaultEntity(), Void.class);
   }

   public ResponseEntity<Void> saveAssets(Asset asset) {
      HttpEntity<Object> postEntity =
            new HttpEntity<Object>(asset, buildHeaders());
      return this.restTemplate.exchange(getAPIServiceEndpoint() + SaveAssetURL, HttpMethod.POST,
            postEntity, Void.class);
   }

   public ResponseEntity<Void> saveRealTimeData(List<RealTimeData> realTimeDatas) {
      HttpEntity<Object> postEntity =
            new HttpEntity<Object>(realTimeDatas, buildHeaders());
      return this.restTemplate.exchange(getAPIServiceEndpoint() + RealTimeDatasURL, HttpMethod.POST,
            postEntity, Void.class);
   }

   public ResponseEntity<ServerMapping[]> getServerMappingsByVC(String VCServerID) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(ServerMappingByVCURL, VCServerID),
            HttpMethod.GET, getDefaultEntity(), ServerMapping[].class);
   }

   public ResponseEntity<Void> saveServerMapping(ServerMapping mapping) {
      HttpEntity<Object> postEntity =
            new HttpEntity<Object>(mapping, buildHeaders());
      return this.restTemplate.exchange(getAPIServiceEndpoint() + ServerMappingURL, HttpMethod.POST,
            postEntity, Void.class);
   }

   public ResponseEntity<PageModelImp<SensorSetting>> getSensorThreshold(int currentPage,int pageSize) {
      return this.restTemplate.exchange(getAPIServiceEndpoint() + String.format(SensorSettingURL,currentPage,pageSize), HttpMethod.GET,
            getDefaultEntity(), new ParameterizedTypeReference<PageModelImp<SensorSetting>>() {});
   }

   public List<SensorSetting> getAllSensorThreshold(){
	   List<SensorSetting> sensorSettings = new ArrayList<SensorSetting>();
       int currentPage = FlowgateConstant.defaultPageNumber;
       PageModelImp<SensorSetting> sensorSettingsPage = getSensorThreshold(currentPage,FlowgateConstant.maxPageSize).getBody();
       if(sensorSettingsPage != null) {
    	  sensorSettings.addAll(sensorSettingsPage.getContent());
          while(!sensorSettingsPage.isLast()) {
             currentPage++;
             sensorSettingsPage = getSensorThreshold(currentPage,FlowgateConstant.maxPageSize).getBody();
             sensorSettings.addAll(sensorSettingsPage.getContent());
         }
      }
      return sensorSettings;
   }

   public ResponseEntity<SDDCSoftwareConfig[]> getVROServers() {
      return this.restTemplate.exchange(getAPIServiceEndpoint() + GetVROServersURL, HttpMethod.GET,
            getDefaultEntity(), SDDCSoftwareConfig[].class);
   }

   public ResponseEntity<Asset[]> getAssetsByVRO(String VROpsServerID) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(GetAssetByVROURL, VROpsServerID),
            HttpMethod.GET, getDefaultEntity(), Asset[].class);
   }

   public ResponseEntity<ServerMapping[]> getServerMappingsByVRO(String VROpsServerID) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(ServerMappingByVROURL, VROpsServerID),
            HttpMethod.GET, getDefaultEntity(), ServerMapping[].class);
   }

   public ResponseEntity<MetricData[]> getServerRelatedSensorDataByServerID(String assetID,
         long startTime, long duration) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint()
                  + String.format(fetchSensorDataURL, assetID, startTime, duration),
            HttpMethod.GET, getDefaultEntity(), MetricData[].class);
   }

   public ResponseEntity<Void> mergMapping(String id1, String id2) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(ServerMappingMergURL, id1, id2), HttpMethod.PUT,
            getDefaultEntity(), Void.class);
   }

   public ResponseEntity<FacilitySoftwareConfig[]> getFacilitySoftwareByType(SoftwareType type) {
	   return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(GetFacilitySoftwareByTypeURL, type),
            HttpMethod.GET, getDefaultEntity(), FacilitySoftwareConfig[].class);
   }

   public ResponseEntity<FacilitySoftwareConfig[]> getFacilitySoftwareInternalByType(SoftwareType type) {
      return this.restTemplate.exchange(
               getAPIServiceEndpoint() + String.format(GetFacilitySoftwareInternalByTypeURL, type),
               HttpMethod.GET, getDefaultEntity(), FacilitySoftwareConfig[].class);
   }

   public ResponseEntity<FacilitySoftwareConfig> getFacilitySoftwareById(String id) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(GetFacilitySoftwareById, id), HttpMethod.GET,
            getDefaultEntity(), FacilitySoftwareConfig.class);
   }

   public ResponseEntity<FacilitySoftwareConfig> getFacilitySoftwareInternalById(String id) {
      return this.restTemplate.exchange(
               getAPIServiceEndpoint() + String.format(GetFacilitySoftwareInternalById, id), HttpMethod.GET,
               getDefaultEntity(), FacilitySoftwareConfig.class);
   }

   public ResponseEntity<Void> updateFacility(FacilitySoftwareConfig config) {
      HttpEntity<Object> postEntity =
            new HttpEntity<Object>(config, buildHeaders());
      return this.restTemplate.exchange(getAPIServiceEndpoint() + UpdateFacilitySoftwareStatus, HttpMethod.PUT,
            postEntity, Void.class);
   }

   public ResponseEntity<Void> updateSDDC(SDDCSoftwareConfig config) {
      HttpEntity<Object> postEntity =
            new HttpEntity<Object>(config, buildHeaders());
      return this.restTemplate.exchange(getAPIServiceEndpoint() + UpdateSDDCSoftwareStatus, HttpMethod.PUT,
            postEntity, Void.class);
   }

   public ResponseEntity<Asset[]> getMappedAsset(AssetCategory category) {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(GetMappedAssetURL, category), HttpMethod.GET,
            getDefaultEntity(), Asset[].class);
   }

   public ResponseEntity<Asset[]> getServersWithnoPDUInfo() {
      return this.restTemplate.exchange(getAPIServiceEndpoint() + GetServersWithnoPDUInfo,
            HttpMethod.GET, getDefaultEntity(), Asset[].class);
   }

   public ResponseEntity<Asset[]> getServersWithPDUInfo() {
      return this.restTemplate.exchange(getAPIServiceEndpoint() + GetServersWithPDUInfo,
            HttpMethod.GET, getDefaultEntity(), Asset[].class);
   }

   public ResponseEntity<AssetIPMapping[]> getHostnameIPMappingByIP(String ip) {
	   return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(GetHostNameByIP, ip), HttpMethod.GET,
            getDefaultEntity(), AssetIPMapping[].class);
   }

   public ResponseEntity<Void> createHostnameIPMapping(AssetIPMapping mapping) {
      HttpEntity<Object> postEntity =
            new HttpEntity<Object>(mapping, buildHeaders());
      return this.restTemplate.exchange(getAPIServiceEndpoint() + HostNameIPMapping,
            HttpMethod.POST, postEntity, Void.class);
   }

   public ResponseEntity<Void> updateHostnameIPMapping(AssetIPMapping mapping) {
      HttpEntity<Object> postEntity =
               new HttpEntity<Object>(mapping, buildHeaders());
      return this.restTemplate.exchange(getAPIServiceEndpoint() + HostNameIPMapping,
               HttpMethod.PUT, postEntity, Void.class);
   }

   public ResponseEntity<Asset> getAssetByName(String name){
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + String.format(GetAssetByName, name), HttpMethod.GET,
            getDefaultEntity(), Asset.class);
   }

   public ResponseEntity<FacilityAdapter[]> getAllCustomerFacilityAdapters() {
      return this.restTemplate.exchange(
            getAPIServiceEndpoint() + GetAllCustomerFacilityAdapters, HttpMethod.GET,
            getDefaultEntity(), FacilityAdapter[].class);
   }
}
