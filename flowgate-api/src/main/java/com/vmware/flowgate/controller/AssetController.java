/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.couchbase.client.java.json.JsonArray;
import com.google.common.collect.Lists;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.MetricData;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.utils.IPAddressUtil;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.AssetIPMappingRepository;
import com.vmware.flowgate.repository.AssetRealtimeDataRepository;
import com.vmware.flowgate.repository.AssetRepository;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.repository.ServerMappingRepository;
import com.vmware.flowgate.service.AssetService;
import com.vmware.flowgate.util.BaseDocumentUtil;

@RestController
@RequestMapping("/v1/assets")
public class AssetController {

   @Autowired
   AssetRepository assetRepository;

   @Autowired
   FacilitySoftwareConfigRepository facilityRepository;

   @Autowired
   AssetRealtimeDataRepository realtimeDataRepository;

   @Autowired
   ServerMappingRepository serverMappingRepository;

   @Autowired
   AssetIPMappingRepository assetIPMappingRepository;

   @Autowired
   AssetService assetService;

   // @Value("${}")
   private int RealtimeQueryDurationLimitation;
   private static final int FIVE_MINUTES = 305000;//add extra 5 seconds;
   private static final String FLOWGATE_SOURCE = "flowgate";
   private static String TIME = "time";

   // Create a new Asset
   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public HttpHeaders  create(@RequestBody Asset asset) {
      HttpHeaders httpHeaders = new HttpHeaders();
      //when labsdb-worker uses this API to save asset ,the created is existed.Need refactor.
      if(asset.getCreated() == 0) {
         asset.setCreated(System.currentTimeMillis());
      }
      BaseDocumentUtil.generateID(asset);
      assetRepository.save(asset);
      httpHeaders.setLocation(linkTo(AssetController.class).slash(asset.getId()).toUri());
      return httpHeaders;
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/batchoperation", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void batchCreation(@RequestBody List<Asset> assets) {
      BaseDocumentUtil.generateID(assets);
      assetRepository.saveAll(assets);
   }

   // Read a Asset
   @RequestMapping(value = "/{id}", method = RequestMethod.GET)
   public Asset read(@PathVariable String id) {
      Optional<Asset> assetOptional = assetRepository.findById(id);
      return assetOptional.get();
   }

   @RequestMapping(value = "/name/{name}", method = RequestMethod.GET)
   public Asset getAssetByName(@PathVariable String name) {
      return assetRepository.findOneByAssetName(name);
   }

   @RequestMapping(value = "/assetnumber/{number}/assetname/{name}", method = RequestMethod.GET)
   public Asset getAssetByAssetNumber(@PathVariable Long number,@PathVariable String name) {
      return assetRepository.findOneByAssetNumberAndAssetName(number,name);
   }

   // Read Asset by source
   @RequestMapping(value = "/source/{assetsource}", method = RequestMethod.GET)
   public Page<Asset> getAssetBySource(@PathVariable("assetsource") String assetSource,
        @RequestParam("currentPage") int currentPage,@RequestParam("pageSize") int pageSize) {
      if (currentPage < FlowgateConstant.defaultPageNumber) {
         currentPage = FlowgateConstant.defaultPageNumber;
      }
      if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);
      return assetRepository.findByAssetSource(assetSource,pageRequest);
   }

   // Read Asset by source and type
   @RequestMapping(value = "/source/{assetsource}/type/{type}", method = RequestMethod.GET)
   public Page<Asset> getAssetBySourceAndType(@PathVariable("assetsource") String assetSource,
         @PathVariable("type") AssetCategory type,@RequestParam("currentPage") int currentPage,
         @RequestParam("pageSize") int pageSize) {
      if (currentPage < FlowgateConstant.defaultPageNumber) {
         currentPage = FlowgateConstant.defaultPageNumber;
      }
      if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);

      return assetRepository.findByAssetSourceContainingAndCategory(assetSource, type.name(),pageRequest);
   }

   // Read Asset by type
   @RequestMapping(value = "/type/{type}", method = RequestMethod.GET)
   public Page<Asset> getAssetByType(@PathVariable AssetCategory type,
         @RequestParam("currentPage") int currentPage,
         @RequestParam("pageSize") int pageSize) {
      if (currentPage < FlowgateConstant.defaultPageNumber) {
         currentPage = FlowgateConstant.defaultPageNumber;
      }
      if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);

      return assetRepository.findAssetByCategory(type.name(),pageRequest);
   }

   // Read mapped Asset
   @RequestMapping(value = "/mappedasset/category/{category}", method = RequestMethod.GET)
   public List<Asset> getMappedAsset(@PathVariable AssetCategory category) {
      List<ServerMapping> serverMappings = serverMappingRepository.findByAssetNotNull();
      List<String> assetIDs =
            serverMappings.stream().map(ServerMapping::getAsset).collect(Collectors.toList());
      JsonArray assetIdarray = JsonArray.from(assetIDs);
      Iterable<Asset> assets = assetRepository.findAll(assetIdarray);
      Set<String> assetids = new HashSet<String>();
      if (category.equals(AssetCategory.Server)) {
         return Lists.newArrayList(assets);
      } else {
         if (category.equals(AssetCategory.PDU)) {
            for (Asset asset : assets) {
               if (asset.getPdus() != null) {
                  assetids.addAll(asset.getPdus());
               }
            }
         } else if (category.equals(AssetCategory.Networks)) {
            for (Asset asset : assets) {
               if (asset.getSwitches() != null) {
                  assetids.addAll(asset.getSwitches());
               }
            }
         }
         if (!assetids.isEmpty()) {
            JsonArray array = JsonArray.from(new ArrayList<String>(assetids));
             assets = assetRepository.findAll(array);
         } else {
            assets = new ArrayList<Asset>();
         }
      }
      return Lists.newArrayList(assets);
   }


   //searchAssetsByAssetNameAndTagLike
   @RequestMapping(value = { "/page/{pageNumber}/pagesize/{pageSize}/keywords/{keyWords}",
         "/page/{pageNumber}/pagesize/{pageSize}" }, method = RequestMethod.GET)
   public Page<Asset> searchAssetsByAssetNameAndTagLike(@PathVariable("pageNumber") int pageNumber,
         @PathVariable("pageSize") int pageSize, @PathVariable(required = false) String keyWords,
         @RequestParam(required = false) AssetCategory category) {

      if (keyWords == null) {
         keyWords = "%%";
      }else {
         keyWords = "%"+keyWords+"%";
      }
      if(category == null) {
         category = AssetCategory.Server;
      }
      if (pageNumber < FlowgateConstant.defaultPageNumber) {
         pageNumber = FlowgateConstant.defaultPageNumber;
      } else if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize);
      List<Asset> assets = assetRepository.findByAssetNameLikeAndCategory(
            keyWords, category.name(), pageSize, pageSize*(pageNumber - 1));
      PageImpl<Asset> assetPage = new PageImpl<Asset>(assets,pageable,0);
      HashMap<String, String> assetSourceIDAndAssetSourceNameMap = new HashMap<String, String>();
      assetSourceIDAndAssetSourceNameMap.put(FLOWGATE_SOURCE, FLOWGATE_SOURCE);
      for (Asset asset : assetPage.getContent()) {
         String assetSourceID = asset.getAssetSource();
         if(assetSourceID == null) {
            continue;
         }
         String[] assetSource = assetSourceID.split(FlowgateConstant.SPILIT_FLAG);
         List<String> assetSourceNames = new ArrayList<String>();
         for(String sourceId : assetSource) {
            String assetSourceName = assetSourceIDAndAssetSourceNameMap.get(sourceId);
            if (assetSourceName == null) {
               Optional<FacilitySoftwareConfig> facilityOptional = facilityRepository.findById(sourceId);
               if(facilityOptional.isPresent()) {
                  assetSourceName = facilityOptional.get().getName();
                  assetSourceIDAndAssetSourceNameMap.put(sourceId, assetSourceName);
               }
            }
            assetSourceNames.add(assetSourceName);
         }
         String sourceNames = String.join(FlowgateConstant.SPILIT_FLAG, assetSourceNames);
         asset.setAssetSource(sourceNames);
      }
      return assetPage;
   }

   /**
    *
    * @return Server list that don't have PDU information. It only Include the servers that create
    *         mapping between SDDC system and Wormhole.
    */
   @RequestMapping(value = "/pdusisnull", method = RequestMethod.GET)
   public List<Asset> findServersWithoutPDUInfo() {
      List<ServerMapping> serverMappings = serverMappingRepository.findByAssetNotNull();
      List<String> assetIDs =
            serverMappings.stream().map(ServerMapping::getAsset).collect(Collectors.toList());
      JsonArray array = JsonArray.from(assetIDs);
      Iterable<Asset> assets = assetRepository.findAll(array);
      List<Asset> result = new ArrayList<Asset>();
      for (Asset asset : assets) {
         if (asset.getPdus() == null || asset.getPdus().isEmpty()) {
            result.add(asset);
         }
      }
      return result;
   }

   /**
    *
    * @return Server list that have PDU information. It only Include the servers that create mapping
    *         between SDDC system and Wormhole.
    */
   @RequestMapping(value = "/pdusisnotnull", method = RequestMethod.GET)
   public List<Asset> findServersWithPDUInfo() {
      List<ServerMapping> serverMappings = serverMappingRepository.findByAssetNotNull();
      List<String> assetIDs =
            serverMappings.stream().map(ServerMapping::getAsset).collect(Collectors.toList());
      JsonArray array = JsonArray.from(assetIDs);
      Iterable<Asset> assets = assetRepository.findAll(array);
      List<Asset> result = new ArrayList<Asset>();
      for (Asset asset : assets) {
         if (asset.getPdus() != null && !asset.getPdus().isEmpty()) {
            result.add(asset);
         }
      }
      return result;
   }

   //Update
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void update(@RequestBody Asset asset) {

      Optional<Asset> oldAssetOptional = assetRepository.findById(asset.getId());
      if (!oldAssetOptional.isPresent()) {
         throw WormholeRequestException.NotFound("Asset", "id", asset.getId());
      }
      Asset oldAsset = oldAssetOptional.get();
      try {
         BaseDocumentUtil.applyChanges(oldAsset, asset);
      } catch (Exception e) {
         throw new WormholeRequestException("Failed to update the Asset", e);
      }
      oldAsset.setLastupdate(System.currentTimeMillis());
      assetRepository.save(oldAsset);
   }

   //Update
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mappingfacility", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateMetricsFormular(@RequestBody Asset asset) {
      assetService.mappingFacilityForServerAsset(asset);
   }

   // Delete a asset
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
   public void delete(@PathVariable String id) {
      assetRepository.deleteById(id);
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/sensordata/batchoperation", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void realTimeDatabatchCreation(@RequestBody List<RealTimeData> realtimedatas) {
      BaseDocumentUtil.generateID(realtimedatas);
      realtimeDataRepository.saveAll(realtimedatas);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}/sensordata", method = RequestMethod.POST)
   public void insertRealtimeData(@PathVariable("id") String assetID,
         @RequestBody RealTimeData data) {
      if (!data.getAssetID().equals(assetID)) {
         throw WormholeRequestException.InvalidFiled("assetID", assetID);
      }
      BaseDocumentUtil.generateID(data);
      realtimeDataRepository.save(data);
   }

   @Deprecated
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/pdu/{id}/realtimedata", method = RequestMethod.GET)
   public List<MetricData> getPduMetricsData(@PathVariable("id") String assetID,
         @RequestParam(value = "starttime", required = false) Long starttime,
         @RequestParam(value = "duration", required = false) Integer duration) {
      if (starttime == null || starttime <= 0) {
         starttime = System.currentTimeMillis() - FIVE_MINUTES;
      }
      if (duration == null || duration <= 0 || duration > FIVE_MINUTES * 24) {
         duration = FIVE_MINUTES;
      }
      return assetService.getPduMetricsDataById(assetID, starttime, duration);
   }

   //starttime miliseconds.
   @Deprecated
   @RequestMapping(value = "/server/{id}/realtimedata", method = RequestMethod.GET)
   public List<MetricData> getServerMetricsData(@PathVariable("id") String assetID,
         @RequestParam(value = "starttime", required = false) Long starttime,
         @RequestParam(value = "duration", required = false) Integer duration) {
      if (starttime == null || starttime <= 0) {
         starttime = System.currentTimeMillis() - FIVE_MINUTES;
      }
      if (duration == null || duration <= 0 || duration > FIVE_MINUTES * 24) {
         duration = FIVE_MINUTES;
      }
      return assetService.getServerMetricsDataById(assetID, starttime, duration);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}/realtimedata", method = RequestMethod.GET)
   public List<MetricData> getLatestAssetMetricsData(@PathVariable("id") String assetID,
                                          @RequestParam(value = "starttime", required = false) Long starttime,
                                          @RequestParam(value = "duration", required = false) Integer duration) {
      if (starttime == null || starttime <= 0) {
         starttime = System.currentTimeMillis() - FIVE_MINUTES;
      }
      if (duration == null || duration <= 0 || duration > FIVE_MINUTES * 24) {
         duration = FIVE_MINUTES;
      }
      return assetService.getLatestAssetMetricsDataById(assetID, starttime, duration);
   }

   /**
    * Return all metric data of the duration
    * @param assetID
    * @param starttime
    * @param duration
    * @return
    */
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}/metrics", method = RequestMethod.GET)
   public List<MetricData> getMetrics(@PathVariable("id") String assetID,
         @RequestParam(value = "starttime", required = false) Long starttime,
         @RequestParam(value = "duration", required = false) Integer duration) {
      if (starttime == null || starttime <= 0) {
         starttime = System.currentTimeMillis() - FIVE_MINUTES;
      }
      if (duration == null || duration <= 0) {
         duration = FIVE_MINUTES;
      }
      if (duration > FIVE_MINUTES * 12) {
         duration = FIVE_MINUTES * 12;
      }
      return assetService.getMetricsByID(assetID, starttime, duration);
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/mapping", method = RequestMethod.POST)
   public void saveServerMapping(@RequestBody ServerMapping serverMapping) {
      BaseDocumentUtil.generateID(serverMapping);
      serverMappingRepository.save(serverMapping);
   }

   @RequestMapping(value = "/vrops/{id}")
   public List<Asset> getAssetsByVROPSId(@PathVariable("id") String vropsID) {
      List<ServerMapping> mappings = new ArrayList<ServerMapping>();
      int currentPage = FlowgateConstant.defaultPageNumber;
      PageRequest pageRequest = PageRequest.of(currentPage-1, FlowgateConstant.maxPageSize);
      Page<ServerMapping> mappingPage =
            serverMappingRepository.findAllByVroID(vropsID, pageRequest);
      mappings.addAll(mappingPage.getContent());
      while(!mappingPage.isLast()) {
    	  currentPage++;
    	  pageRequest = PageRequest.of(currentPage-1, FlowgateConstant.maxPageSize);
    	  mappingPage = serverMappingRepository.findAllByVroID(vropsID, pageRequest);
    	  mappings.addAll(mappingPage.getContent());
      }
      List<String> assetIDs = new ArrayList<String>();
      for(ServerMapping mapping:mappings) {
    	  String assetId = mapping.getAsset();
    	  if(assetId != null) {
    		  assetIDs.add(assetId);
    	  }
      }
      JsonArray array = JsonArray.from(assetIDs);
      return assetRepository.findAll(array);
   }

   // Update serverMapping
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void updateServerMapping(@RequestBody ServerMapping serverMaping) {
      Optional<ServerMapping> mappingOptional = serverMappingRepository.findById(serverMaping.getId());
      if (!mappingOptional.isPresent()) {
         throw WormholeRequestException.NotFound("ServerMapping", "id", serverMaping.getId());
      }
      ServerMapping mapping = mappingOptional.get();
      try {
         mapping.setAsset(serverMaping.getAsset());
         serverMappingRepository.save(mapping);
      } catch (Exception e) {
         throw new WormholeRequestException("Failed to update the ServerMapping", e);
      }
   }

   // Delete a serverMapping
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/{id}", method = RequestMethod.DELETE)
   public void deleteServerMapping(@PathVariable String id) {
      serverMappingRepository.deleteById(id);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/merge/{firstid}/{secondid}", method = RequestMethod.PUT)
   public void mergeServerMapping(@PathVariable("firstid") String id1,
         @PathVariable("secondid") String id2) {
      if (id1 == id2) {
         WormholeRequestException.InvalidFiled("ids", id1+":"+id2);
      }
      Optional<ServerMapping> firstMappingOptional = serverMappingRepository.findById(id1);
      if (!firstMappingOptional.isPresent()) {
         throw WormholeRequestException.NotFound("ServerMapping", "id", id1);
      }
      Optional<ServerMapping> secondMappingOptional = serverMappingRepository.findById(id2);
      if (!secondMappingOptional.isPresent()) {
         throw WormholeRequestException.NotFound("ServerMapping", "id", id2);
      }
      ServerMapping firstMapping = firstMappingOptional.get();
      ServerMapping secondMapping = secondMappingOptional.get();
      mergeMapping(firstMapping, secondMapping);
      serverMappingRepository.save(firstMapping);
      serverMappingRepository.delete(secondMapping);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/vrops/{vropsID}", method = RequestMethod.GET)
   public List<ServerMapping> getMappingsByVROPSId(@PathVariable("vropsID") String vropsID) {
      List<ServerMapping> mappings =
            serverMappingRepository.findAllByVroID(vropsID);
      return mappings;
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/vc/{vcID}", method = RequestMethod.GET)
   public List<ServerMapping> getMappingsByVCId(@PathVariable("vcID") String vcID) {
      List<ServerMapping> mappings =
            serverMappingRepository.findAllByVCID(vcID);
      return mappings;
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/{id}", method = RequestMethod.GET)
   public ServerMapping getMappingById(@PathVariable("id") String id) {
      Optional<ServerMapping> serverMappingOptional = serverMappingRepository.findById(id);
      return serverMappingOptional.get();
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/vrops/{vropsID}/page/{pageNumber}/pagesize/{pageSize}", method = RequestMethod.GET)
   public Page<ServerMapping> getPageMappingsByVROPSId(@PathVariable("vropsID") String vropsID,
         @PathVariable("pageNumber") int pageNumber, @PathVariable("pageSize") int pageSize) {
      if (pageNumber < FlowgateConstant.defaultPageNumber) {
         pageNumber = FlowgateConstant.defaultPageNumber;
      } else if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }

      PageRequest pageRequest = PageRequest.of(pageNumber - 1, pageSize);
      Page<ServerMapping> mappings =
            serverMappingRepository.findAllByVroID(vropsID, pageRequest);
      return replaceAssetIDwithAssetName(mappings);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/vc/{vcID}/page/{pageNumber}/pagesize/{pageSize}", method = RequestMethod.GET)
   public Page<ServerMapping> getPageMappingsByVCId(@PathVariable("vcID") String vcID,
         @PathVariable("pageNumber") int pageNumber, @PathVariable("pageSize") int pageSize) {
      if (pageNumber < FlowgateConstant.defaultPageNumber) {
         pageNumber = FlowgateConstant.defaultPageNumber;
      } else if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = PageRequest.of(pageNumber - 1, pageSize);
      Page<ServerMapping> mappings =
            serverMappingRepository.findAllByVcID(vcID, pageRequest);
      return replaceAssetIDwithAssetName(mappings);
   }

   private Page<ServerMapping> replaceAssetIDwithAssetName(Page<ServerMapping> mappings) {
      HashMap<String,String> assetIdAndName = new HashMap<String,String>();
      for (ServerMapping mapping : mappings.getContent()) {
         String assetID = mapping.getAsset();
         if(assetID == null) {
            continue;
         }
         if(assetIdAndName.containsKey(assetID)) {
            String assetName = assetIdAndName.get(assetID);
            mapping.setAsset(assetName);
         }else {
            Optional<Asset> assetOptional = assetRepository.findById(assetID);
            if(assetOptional.isPresent()) {
               String assetName = assetOptional.get().getAssetName();
               mapping.setAsset(assetName);
               assetIdAndName.put(assetID, assetName);
            }
         }
      }
      return mappings;
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/hostnameip/ip/{ip:.+}", method = RequestMethod.GET)
   public List<AssetIPMapping> getHostNameByIP(@PathVariable("ip") String ip) {
      return assetIPMappingRepository.findAllByIp(ip);
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/mapping/hostnameip", method = RequestMethod.POST)
   public void createHostNameIPMapping(@RequestBody AssetIPMapping mapping) {
      BaseDocumentUtil.generateID(mapping);
      String ip = mapping.getIp();
      if(!IPAddressUtil.isValidIp(ip)) {
         throw WormholeRequestException.InvalidFiled("ip", ip);
      }
      if (!assetService.isAssetNameValidate(mapping.getAssetname())) {
         throw new WormholeRequestException(HttpStatus.INTERNAL_SERVER_ERROR, "Can't find any asset with the name : " + mapping.getAssetname(),
               null);
      }
      assetIPMappingRepository.save(mapping);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/hostnameip", method = RequestMethod.PUT)
   public void updateHostNameIPMapping(@RequestBody AssetIPMapping mapping) {
      Optional<AssetIPMapping> oldMappingOptional = assetIPMappingRepository.findById(mapping.getId());
      if(!oldMappingOptional.isPresent()) {
         throw new WormholeRequestException(HttpStatus.INTERNAL_SERVER_ERROR,"Can't find any mapping with the id: " + mapping.getId(),null);
      }
      AssetIPMapping oldMapping = oldMappingOptional.get();
      String assetName = mapping.getAssetname();
      String macAddress = mapping.getMacAddress();
      if(StringUtils.equals(oldMapping.getAssetname(), assetName)) {
         if (StringUtils.equals(macAddress, oldMapping.getMacAddress())) {
            return;
         }
      }
      if(!assetService.isAssetNameValidate(assetName)) {
         throw new WormholeRequestException(HttpStatus.INTERNAL_SERVER_ERROR,"Can't find any asset with the name : " + mapping.getAssetname(),null);
      }
      oldMapping.setAssetname(mapping.getAssetname());
      oldMapping.setMacAddress(mapping.getMacAddress());
      assetIPMappingRepository.save(oldMapping);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/hostnameip", method = RequestMethod.GET)
   public Page<AssetIPMapping> getAssetIPMappingByPage(@RequestParam(value = "pagesize",required = false, defaultValue = "20") Integer pageSize,
         @RequestParam(value = "pagenumber",required = false, defaultValue = "1") Integer pageNumber,
         @RequestParam(value = "ip",required = false) String ip) {
      if (pageNumber < FlowgateConstant.defaultPageNumber) {
         pageNumber = FlowgateConstant.defaultPageNumber;
      } else if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageRequest = PageRequest.of(pageNumber - 1, pageSize);
      if(ip == null) {
         return assetIPMappingRepository.findAll(pageRequest);
      }else if(IPAddressUtil.isValidIp(ip)) {
         return assetIPMappingRepository.findByIp(ip, pageRequest);
      }
      throw WormholeRequestException.InvalidFiled("ip", ip);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/hostnameip/{id}", method = RequestMethod.DELETE)
   public void deleteHostNameByID(@PathVariable("id") String id) {
      assetIPMappingRepository.deleteById(id);
   }

   @RequestMapping(value = "/mapping/hostnameip/file",method = RequestMethod.POST)
   public List<AssetIPMapping> batchCreateMapping(@RequestParam("file") MultipartFile file) {
       try {
         return assetService.batchCreateMappingFromFile(file);
      } catch (IOException e) {
         throw new WormholeRequestException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(),e);
      }
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/names", method = RequestMethod.GET)
   public List<String> searchAssetName(@RequestParam("queryParam") String content){
      return assetService.searchServerAssetName(content);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/unmappedservers", method = RequestMethod.GET)
   public List<String> getUnmappedServers() {
      List<ServerMapping> mappings = serverMappingRepository.findByAssetIsNull();
      Set<String> result = new HashSet<String>();
      if (mappings != null && !mappings.isEmpty()) {
         for (ServerMapping mapping : mappings) {
            if (null != mapping.getVcHostName()) {
               result.add(mapping.getVcHostName());
            } else if (null != mapping.getVroVMEntityName()) {
               result.add(mapping.getVroVMEntityName());
            }
         }
      }
      List<String> mergedList = new ArrayList<String>();
      mergedList.addAll(result);
      return mergedList;
   }

   @RequestMapping(value = "/vc/{id}", method = RequestMethod.GET)
   public List<Asset> getAssetsByVCId(@PathVariable("id") String vcID) {
      List<ServerMapping> mappings = new ArrayList<ServerMapping>();
      int currentPage = FlowgateConstant.defaultPageNumber;
      PageRequest pageRequest = PageRequest.of(currentPage-1, FlowgateConstant.maxPageSize);
      Page<ServerMapping> mappingPage =
            serverMappingRepository.findAllByVcID(vcID, pageRequest);
      mappings.addAll(mappingPage.getContent());
      while(!mappingPage.isLast()) {
    	  currentPage++;
    	  pageRequest = PageRequest.of(currentPage - 1, FlowgateConstant.maxPageSize);
    	  mappingPage = serverMappingRepository.findAllByVcID(vcID, pageRequest);
    	  mappings.addAll(mappingPage.getContent());
      }
      List<String> assetIDs = new ArrayList<String>();
      for(ServerMapping mapping:mappings) {
    	  String assetId = mapping.getAsset();
    	  if(assetId != null) {
    		  assetIDs.add(assetId);
    	  }
      }
      JsonArray array = JsonArray.from(assetIDs);
      return assetRepository.findAll(array);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/realtimedata/{expiredtimerange}", method = RequestMethod.DELETE)
   public void removeRealTimeData(@PathVariable("expiredtimerange") Long expiredtimerange) {
      long currentTime = System.currentTimeMillis();
      if(expiredtimerange < FlowgateConstant.DEFAULTEXPIREDTIMERANGE) {
         expiredtimerange = FlowgateConstant.DEFAULTEXPIREDTIMERANGE;
      }
      List<RealTimeData> dataToBeDeleted = realtimeDataRepository.getRealTimeDatabtTimeRange(currentTime - expiredtimerange);
      while(!dataToBeDeleted.isEmpty()) {
         for(RealTimeData realtimedata : dataToBeDeleted) {
            realtimeDataRepository.deleteById(realtimedata.getId());
         }
         dataToBeDeleted = realtimeDataRepository.getRealTimeDatabtTimeRange(currentTime - expiredtimerange);
      }
   }

   private void mergeMapping(ServerMapping firstMapping, ServerMapping secondMapping) {
      String firstMappingKey = null;
      if (firstMapping.getVroVMEntityVCID() != null) {
         firstMappingKey =
               firstMapping.getVroVMEntityVCID() + ":" + firstMapping.getVroVMEntityObjectID();
      } else if (firstMapping.getVcInstanceUUID() != null) {
         firstMappingKey = firstMapping.getVcInstanceUUID() + ":" + firstMapping.getVcMobID();
      }
      if (firstMappingKey == null) {
         throw new WormholeRequestException("Invalid mapping");
      }
      String secondMappingKey = null;
      if (secondMapping.getVroVMEntityVCID() != null) {
         secondMappingKey =
               secondMapping.getVroVMEntityVCID() + ":" + secondMapping.getVroVMEntityObjectID();
      } else if (secondMapping.getVcInstanceUUID() != null) {
         secondMappingKey = secondMapping.getVcInstanceUUID() + ":" + secondMapping.getVcMobID();
      }
      if (!firstMappingKey.equals(secondMappingKey)) {
         throw new WormholeRequestException(String.format("Cannot merge different asset %s, %s",
               firstMappingKey, secondMappingKey));
      }

      if (firstMapping.getVroVMEntityVCID() != null) {
         //merge vc
         if (secondMapping.getVcClusterMobID() != null) {
            firstMapping.setVcClusterMobID(secondMapping.getVcClusterMobID());
         }
         if (secondMapping.getVcHostName() != null) {
            firstMapping.setVcHostName(secondMapping.getVcHostName());
         }
         if (secondMapping.getVcID() != null) {
            firstMapping.setVcID(secondMapping.getVcID());
         }
         if (secondMapping.getVcInstanceUUID() != null) {
            firstMapping.setVcInstanceUUID(secondMapping.getVcInstanceUUID());
         }
         if (secondMapping.getVcMobID() != null) {
            firstMapping.setVcMobID(secondMapping.getVcMobID());
         }
         if (secondMapping.getAsset() != null) {
            firstMapping.setAsset(secondMapping.getAsset());
         }
      } else {
         if (secondMapping.getVroID() != null) {
            firstMapping.setVroID(secondMapping.getVroID());
         }
         if (secondMapping.getVroResourceID() != null) {
            firstMapping.setVroResourceID(secondMapping.getVroResourceID());
         }
         if (secondMapping.getVroResourceName() != null) {
            firstMapping.setVroResourceName(secondMapping.getVroResourceName());
         }
         if (secondMapping.getVroVMEntityName() != null) {
            firstMapping.setVroVMEntityName(secondMapping.getVroVMEntityName());
         }
         if (secondMapping.getVroVMEntityObjectID() != null) {
            firstMapping.setVroVMEntityObjectID(secondMapping.getVroVMEntityObjectID());
         }
         if (secondMapping.getVroVMEntityVCID() != null) {
            firstMapping.setVroVMEntityVCID(secondMapping.getVroVMEntityVCID());
         }
         if (secondMapping.getAsset() != null) {
            firstMapping.setAsset(secondMapping.getAsset());
         }
      }
   }

}
