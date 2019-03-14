/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.wormhole.nlyteworker.restclient;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.vmware.wormhole.client.RestTemplateBuilder;
import com.vmware.wormhole.common.AssetCategory;
import com.vmware.wormhole.common.model.FacilitySoftwareConfig;
import com.vmware.wormhole.nlyteworker.exception.NlyteWorkerException;
import com.vmware.wormhole.nlyteworker.model.JsonResultForAsset;
import com.vmware.wormhole.nlyteworker.model.JsonResultForLocationGroup;
import com.vmware.wormhole.nlyteworker.model.JsonResultForManufacturer;
import com.vmware.wormhole.nlyteworker.model.JsonResultForMaterial;
import com.vmware.wormhole.nlyteworker.model.JsonResultForPDURealtimeValue;
import com.vmware.wormhole.nlyteworker.model.LocationGroup;
import com.vmware.wormhole.nlyteworker.model.Manufacturer;
import com.vmware.wormhole.nlyteworker.model.Material;
import com.vmware.wormhole.nlyteworker.model.NlyteAsset;
import com.vmware.wormhole.nlyteworker.model.SessionExpriesData;
import com.vmware.wormhole.nlyteworker.scheduler.job.common.HandleAssetUtil;

@Service
public class NlyteAPIClient {
   private static final Logger logger = LoggerFactory.getLogger(NlyteAPIClient.class);
   private static final String GetManufacturersURL = "/nlyte/integration/api/odata/manufacturers";
   private static final String GetServerAssetsURL = "/nlyte/integration/api/odata/Servers?$expand=UMounting";
   private static final String GetCabinetsURL = "/nlyte/integration/api/odata/Cabinets";
   private static final String GetPowerStripAssetsURL = "/nlyte/integration/api/odata/PowerStrips";
   private static final String GetLocationGroupsURL = "/nlyte/integration/api/odata/LocationGroups";
   private static final String GetBladeServerMaterialsURL = "/nlyte/integration/api/odata/BladeServerMaterials";
   private static final String GetCabinetMaterialsURL = "/nlyte/integration/api/odata/CabinetMaterials";
   private static final String GetStandardServerMaterialsURL = "/nlyte/integration/api/odata/StandardServerMaterials";
   private static final String GetPowerStripMaterialsURL = "/nlyte/integration/api/odata/PowerStripMaterials";
   private static final String AuthenticateBasicURL = "/nlyte/integration/api/odata/auth/AuthenticateBasic";
   private static final String GetPowerStripRealtimeValue = "/nlyte/integration/api/odata/PowerStrips(%s)/GetRealtimeValues";
   private static final String GetNetworksURL = "/nlyte/integration/api/odata/Networks";
   private static final String GetNetworkMaterialsURL = "/nlyte/integration/api/odata/NetworkMaterials";
   protected String nlyteServiceEndpoint;

   protected String username;

   protected String password;

   protected List<String> cookies = null;

   protected long expired_time = 0;

   protected RestTemplate restTemplate;

   public NlyteAPIClient() {
   }

   public NlyteAPIClient(FacilitySoftwareConfig facilitySoftwareConfig) {
      this.username = facilitySoftwareConfig.getUserName();
      this.password = facilitySoftwareConfig.getPassword();
      this.nlyteServiceEndpoint = facilitySoftwareConfig.getServerURL();
      try {
         this.restTemplate =
               RestTemplateBuilder.buildTemplate(facilitySoftwareConfig.isVerifyCert(), 60000);
      } catch (Exception e) {
         throw new NlyteWorkerException(e.getMessage(), e.getCause());
      }
   }

   public void setRestTemplate(RestTemplate restTemplate) {
      this.restTemplate = restTemplate;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public void setNlyteServiceEndpoint(String nlyteServiceEndpoint) {
      this.nlyteServiceEndpoint = nlyteServiceEndpoint;
      ;
   }

   public String getNlyteServiceEndpoint() {
      return this.nlyteServiceEndpoint;
   }

   protected HttpHeaders buildHeaders() {
      HttpHeaders headers = new HttpHeaders();
      headers.put(HttpHeaders.COOKIE, cookies);
      headers.setContentType(MediaType.APPLICATION_JSON);
      List<MediaType> acceptedTypes = new ArrayList<MediaType>();
      acceptedTypes.add(MediaType.APPLICATION_JSON);
      acceptedTypes.add(MediaType.TEXT_HTML);
      headers.setAccept(acceptedTypes);
      return headers;
   }

   protected HttpEntity<String> getDefaultEntity() {
      return new HttpEntity<String>(buildHeaders());
   }

   public void initAuthenticationWebToken() {
      long time = System.currentTimeMillis();
      if (time > expired_time) {
         String max_age = "";
         cookies = getAuthorizationToken().getHeaders().get("Set-Cookie");
         String[] cookiesValue = cookies.get(0).split(";");
         for (int i = 0; i < cookiesValue.length; i++) {
            if (cookiesValue[i].trim().startsWith("max-age")) {
               max_age =
                     cookiesValue[i].trim().substring(cookiesValue[i].trim().lastIndexOf("=") + 1);
               break;
            }
         }
         expired_time = Long.parseLong(max_age) * 1000 + System.currentTimeMillis();
      }
   }

   public ResponseEntity<SessionExpriesData> getAuthorizationToken() {
      this.restTemplate.getInterceptors()
            .add(new BasicAuthorizationInterceptor(this.username, this.password));
      return this.restTemplate.exchange(getNlyteServiceEndpoint() + AuthenticateBasicURL,
            HttpMethod.GET, RestTemplateBuilder.getDefaultEntity() , SessionExpriesData.class);
   }

   public List<Manufacturer> getManufacturers(boolean isAllData) {
      initAuthenticationWebToken();
      List<Manufacturer> manufacturers = new LinkedList<Manufacturer>();
      JsonResultForManufacturer manufacturerResult =
            this.restTemplate.exchange(getNlyteServiceEndpoint() + GetManufacturersURL,
                  HttpMethod.GET, getDefaultEntity(), JsonResultForManufacturer.class).getBody();
      manufacturers = manufacturerResult.getValue();
      if (!isAllData) {
         return manufacturers;
      }
      List<Manufacturer> nextPageManufacturers = null;
      while (manufacturerResult.getOdatanextLink() != null
            && !manufacturerResult.getOdatanextLink().equals("")) {
         nextPageManufacturers = new LinkedList<Manufacturer>();
         manufacturerResult = this.restTemplate.exchange(manufacturerResult.getOdatanextLink(),
               HttpMethod.GET, getDefaultEntity(), JsonResultForManufacturer.class).getBody();
         nextPageManufacturers = manufacturerResult.getValue();
         manufacturers.addAll(nextPageManufacturers);
      }
      return manufacturers;
   }

   public List<LocationGroup> getLocationGroups(boolean isAllData) {
      initAuthenticationWebToken();
      JsonResultForLocationGroup locationGroupresult =
            this.restTemplate.exchange(getNlyteServiceEndpoint() + GetLocationGroupsURL,
                  HttpMethod.GET, getDefaultEntity(), JsonResultForLocationGroup.class).getBody();
      List<LocationGroup> locationGroups = new LinkedList<LocationGroup>();
      locationGroups = locationGroupresult.getValue();
      if (!isAllData) {
         return locationGroups;
      }
      List<LocationGroup> nextPageLocationGroup = null;
      while (locationGroupresult.getOdatanextLink() != null
            && !locationGroupresult.getOdatanextLink().equals("")) {
         nextPageLocationGroup = new LinkedList<LocationGroup>();
         locationGroupresult = this.restTemplate.exchange(locationGroupresult.getOdatanextLink(),
               HttpMethod.GET, getDefaultEntity(), JsonResultForLocationGroup.class).getBody();
         nextPageLocationGroup = locationGroupresult.getValue();
         locationGroups.addAll(nextPageLocationGroup);
      }
      return locationGroups;
   }

   public List<Material> getMaterials(boolean isAllData,int materialType){

      initAuthenticationWebToken();
      String getMaterialsURL = null;
      switch (materialType) {
      case HandleAssetUtil.bladeServerMaterial:
         getMaterialsURL = getNlyteServiceEndpoint() + GetBladeServerMaterialsURL;
         break;
      case HandleAssetUtil.standardServerMaterial:
         getMaterialsURL = getNlyteServiceEndpoint() + GetStandardServerMaterialsURL;
         break;
      case HandleAssetUtil.powerStripMaterial:
         getMaterialsURL = getNlyteServiceEndpoint() + GetPowerStripMaterialsURL;
         break;
      case HandleAssetUtil.cabinetMaterials:
         getMaterialsURL = getNlyteServiceEndpoint() + GetCabinetMaterialsURL;
         break;
      case HandleAssetUtil.networkMaterials:
         getMaterialsURL = getNlyteServiceEndpoint() + GetNetworkMaterialsURL;
         break;
      default:
         throw new NlyteWorkerException(
               "category should be 'Blade' or 'Standard' for server matreial");
      }
      List<Material> materials = new LinkedList<Material>();
      JsonResultForMaterial materialResult = this.restTemplate.exchange(getMaterialsURL,
            HttpMethod.GET, getDefaultEntity(), JsonResultForMaterial.class).getBody();
      materials.addAll(materialResult.getValue());
      if (!isAllData) {
         return materials;
      }
      List<Material> nextPageMaterials = null;
      while (materialResult.getOdatanextLink() != null
            && !materialResult.getOdatanextLink().equals("")) {
         nextPageMaterials = new LinkedList<Material>();
         try {
            materialResult = this.restTemplate.exchange(materialResult.getOdatanextLink(),
                  HttpMethod.GET, getDefaultEntity(), JsonResultForMaterial.class).getBody();
         }catch(HttpServerErrorException e) {
            logger.error("Internal Server Error.The url is "+materialResult.getOdatanextLink());
            break;
         }
         nextPageMaterials = materialResult.getValue();
         materials.addAll(nextPageMaterials);
      }
      return materials;
   }

   public List<NlyteAsset> getAssets(boolean isAllData, AssetCategory category) {
      initAuthenticationWebToken();
      String getAssetsUrl = null;
      switch (category) {
      case Server:
         getAssetsUrl = getNlyteServiceEndpoint() + GetServerAssetsURL;
         break;
      case PDU:
         getAssetsUrl = getNlyteServiceEndpoint() + GetPowerStripAssetsURL;
         break;
      case Cabinet:
         getAssetsUrl = getNlyteServiceEndpoint() + GetCabinetsURL;
         break;
      case Networks:
         getAssetsUrl = getNlyteServiceEndpoint() + GetNetworksURL;
         break;
      default:
         throw new NlyteWorkerException("no such assets of the category ");
      }
      JsonResultForAsset nlyteAssetResult = this.restTemplate
            .exchange(getAssetsUrl, HttpMethod.GET, getDefaultEntity(), JsonResultForAsset.class)
            .getBody();
      List<NlyteAsset> nlyteAssets = new LinkedList<NlyteAsset>();
      nlyteAssets = nlyteAssetResult.getValue();
      if (!isAllData) {
         return nlyteAssets;
      }
      List<NlyteAsset> nextPageNlyteAssets = null;
      while (nlyteAssetResult.getOdatanextLink() != null
            && !nlyteAssetResult.getOdatanextLink().equals("")) {
         nextPageNlyteAssets = new ArrayList<NlyteAsset>();
         nlyteAssetResult = this.restTemplate.exchange(nlyteAssetResult.getOdatanextLink(),
               HttpMethod.GET, getDefaultEntity(), JsonResultForAsset.class).getBody();
         nextPageNlyteAssets = nlyteAssetResult.getValue();
         nlyteAssets.addAll(nextPageNlyteAssets);
      }
      return nlyteAssets;
   }
   public ResponseEntity<JsonResultForPDURealtimeValue> getPowerStripsRealtimeValue(long assetNumber) {
      initAuthenticationWebToken();
      return this.restTemplate.exchange( getNlyteServiceEndpoint()+String.format(GetPowerStripRealtimeValue, assetNumber), HttpMethod.GET,
            getDefaultEntity(), JsonResultForPDURealtimeValue.class);
   }
   public ResponseEntity<JsonResultForAsset> getNextPageAssets(String nextLink) {
      initAuthenticationWebToken();
      return this.restTemplate.exchange(nextLink, HttpMethod.GET, getDefaultEntity(),
            JsonResultForAsset.class);
   }

}
