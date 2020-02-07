/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
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

import com.couchbase.client.java.document.json.JsonArray;
import com.google.common.collect.Lists;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ServerMapping;
import com.vmware.flowgate.common.model.ServerSensorData;
import com.vmware.flowgate.common.model.ServerSensorData.ServerSensorType;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ValueUnit.ValueType;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.AssetIPMappingRepository;
import com.vmware.flowgate.repository.AssetRealtimeDataRepository;
import com.vmware.flowgate.repository.AssetRepository;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.repository.ServerMappingRepository;
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

   // @Value("${}")
   private int RealtimeQueryDurationLimitation;
   private static final int TEN_MINUTES = 605000;//add extra 5 seconds;
   private static String TIME = "time";

   // Create a new Asset
   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public HttpHeaders create(@RequestBody Asset asset) {
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
      assetRepository.save(assets);
   }

   // Read a Asset
   @RequestMapping(value = "/{id}", method = RequestMethod.GET)
   public Asset read(@PathVariable String id) {
      return assetRepository.findOne(id);
   }

   @RequestMapping(value = "/name/{name}", method = RequestMethod.GET)
   public Asset getAssetByName(@PathVariable String name) {
      return assetRepository.findOneByAssetName(name);
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
      PageRequest pageRequest = new PageRequest(currentPage - 1, pageSize);
      return assetRepository.findByAssetSourceContaining(assetSource,pageRequest);
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
      PageRequest pageRequest = new PageRequest(currentPage - 1, pageSize);

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
      PageRequest pageRequest = new PageRequest(currentPage - 1, pageSize);

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
         @PathVariable("pageSize") int pageSize, @PathVariable(required = false) String keyWords) {
      if (keyWords == null) {
         keyWords = "";
      }
      if (pageNumber < FlowgateConstant.defaultPageNumber) {
         pageNumber = FlowgateConstant.defaultPageNumber;
      } else if (pageSize <= 0) {
         pageSize = FlowgateConstant.defaultPageSize;
      } else if (pageSize > FlowgateConstant.maxPageSize) {
         pageSize = FlowgateConstant.maxPageSize;
      }
      PageRequest pageable = new PageRequest(pageNumber - 1, pageSize);
      List<Asset> assets = assetRepository.findByAssetNameLikeAndCategoryOrTagLikeAndCategory(
            keyWords+"%", AssetCategory.Server.name(), pageSize, pageSize*(pageNumber - 1));
      long total = assetRepository.getNumber(keyWords+"%", AssetCategory.Server.name());
      PageImpl<Asset> assetPage = new PageImpl<Asset>(assets,pageable,total);
      HashMap<String, String> assetSourceIDAndAssetSourceNameMap = new HashMap<String, String>();

      for (Asset asset : assetPage.getContent()) {
         String assetSourceID = asset.getAssetSource();
         if(assetSourceID == null) {
            continue;
         }
         String assetSource [] = assetSourceID.split(FlowgateConstant.SPILIT_FLAG);
         List<String> assetSourceNames = new ArrayList<String>();
         for(String sourceId : assetSource) {
            String assetSourceName = assetSourceIDAndAssetSourceNameMap.get(sourceId);
            if (assetSourceName == null) {
               assetSourceName = facilityRepository.findOne(assetSourceID).getName();
               assetSourceIDAndAssetSourceNameMap.put(assetSourceID, assetSourceName);
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

      Asset old = assetRepository.findOne(asset.getId());
      if (old == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND, "Asset not found", null);
      }
      try {
         BaseDocumentUtil.applyChanges(old, asset);
      } catch (Exception e) {
         throw new WormholeRequestException("Failed to update the Asset", e);
      }
      old.setLastupdate(System.currentTimeMillis());
      assetRepository.save(old);
   }

   // Delete a asset
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
   public void delete(@PathVariable String id) {
      assetRepository.delete(id);
   }

   @ResponseStatus(HttpStatus.CREATED)
   @RequestMapping(value = "/sensordata/batchoperation", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void realTimeDatabatchCreation(@RequestBody List<RealTimeData> realtimedatas) {
      BaseDocumentUtil.generateID(realtimedatas);
      realtimeDataRepository.save(realtimedatas);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}/sensordata", method = RequestMethod.POST)
   public void insertRealtimeData(@PathVariable("id") String assetID,
         @RequestBody RealTimeData data) {
      if (!data.getAssetID().equals(assetID)) {
         throw new WormholeRequestException("Invalid AssetID.");
      }
      BaseDocumentUtil.generateID(data);
      realtimeDataRepository.save(data);
   }

   //starttime miliseconds.
   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/{id}/serversensordata", method = RequestMethod.GET)
   public List<ServerSensorData> getServerSensorData(@PathVariable("id") String assetID,
         @RequestParam(value = "starttime", required = false) Long starttime,
         @RequestParam(value = "duration", required = false) Integer duration) {
      if (starttime == null || starttime <= 0) {
         starttime = System.currentTimeMillis() - TEN_MINUTES;
      }
      if (duration == null || duration <= 0 || duration > TEN_MINUTES * 12) {
         duration = TEN_MINUTES;
      }
      Asset server = assetRepository.findOne(assetID);
      List<ServerSensorData> result = new ArrayList<ServerSensorData>();
      Map<ServerSensorType, String> formulars = server.getSensorsformulars();

      for (ServerSensorType category : formulars.keySet()) {
         String formular = formulars.get(category);
         ExpressionParser parser = new SpelExpressionParser();
         List<Map<String, String>> validDataComposeList = new ArrayList<Map<String, String>>();
         String[] assetIDs = formular.split("\\+|-|\\*|/|\\(|\\)");
         Map<String, List<RealTimeData>> dataSlice = new HashMap<String, List<RealTimeData>>();
         for (String id : assetIDs) {
            if (id.length() == FlowgateConstant.COUCHBASEIDLENGTH) {//the default uuid length is 32

               List<RealTimeData> currentData =
                     realtimeDataRepository.getDataByIDAndTimeRange(id, starttime, duration);
               Collections.sort(currentData, new Comparator<RealTimeData>() {
                  @Override
                  public int compare(RealTimeData data1, RealTimeData data2) {
                     return (int) (data1.getTime() - data2.getTime());
                  }

               });
               dataSlice.put(id, currentData);
            }
         }
         switch (category) {
         case FRONTPANELTEMP:
         case BACKPANELTEMP:
            computeTemp(result, dataSlice, validDataComposeList, category, formular, parser);
            break;
         default:
            //it should only has 1 item in the dataslice for other case.
            //            if (dataSlice.size() > 1) {
            //               throw new WormholeException(
            //                     "The Server should only attached to 1 sensor but find multiple");
            //            }
            for (List<RealTimeData> value : dataSlice.values()) {
               for (RealTimeData data : value) {

                  for (ValueUnit unitData : data.getValues()) {
                     if (compareType(unitData.getKey(), category)) {
                        ServerSensorData sData = new ServerSensorData();
                        sData.setType(category);
                        sData.setTimeStamp(unitData.getTime());
                        sData.setValue(unitData.getValue());
                        sData.setValueNum(unitData.getValueNum());
                        result.add(sData);
                     }
                  }
               }
            }
         }
      }
      return result;
   }

   private void computeTemp(List<ServerSensorData> result,
         Map<String, List<RealTimeData>> dataSlice, List<Map<String, String>> validDataComposeList,
         ServerSensorType category, String formular, ExpressionParser parser) {
      //aggregate the data.
      for (String tempID : dataSlice.keySet()) {
         List<RealTimeData> tempDatas = dataSlice.get(tempID);
         for (RealTimeData rData : tempDatas) {
            long time = rData.getTime();
            boolean goodData = true;
            Map<String, String> dataset = new HashMap<String, String>();
            dataset.put(TIME, String.valueOf(time));
            //get the tempdata.
            for (ValueUnit data : rData.getValues()) {
               if (data.getKey().equals(ValueType.TEMP)) {
                  if (data.getValue() != null && !"".equals(data.getValue())) {
                     dataset.put(tempID, data.getValue());
                  } else {
                     dataset.put(tempID, String.valueOf(data.getValueNum()));
                  }
               }
            }
            for (String otherID : dataSlice.keySet()) {
               if (tempID.equals(otherID)) {
                  continue;
               }
               RealTimeData clostData = getMostClostData(dataSlice.get(otherID), time);
               if (Math.abs(clostData.getTime() - time) > 10000) {
                  goodData = false;
                  break;
               }
               for (ValueUnit cData : clostData.getValues()) {
                  if (cData.getKey().equals(ValueType.TEMP)) {
                     if (cData.getValue() != null && !"".equals(cData.getValue())) {
                        dataset.put(otherID, cData.getValue());
                     } else {
                        dataset.put(otherID, String.valueOf(cData.getValueNum()));
                     }
                  }
               }
            }
            if (goodData) {
               validDataComposeList.add(dataset);
            }
         }
      }
      //now we have data and formula
      for (Map<String, String> composeData : validDataComposeList) {
         ServerSensorData sData = new ServerSensorData();
         sData.setType(category);
         sData.setTimeStamp(Long.parseLong(composeData.get(TIME)));
         String dataFormular = formular;
         for (String aID : composeData.keySet()) {
            dataFormular = dataFormular.replaceAll(aID, composeData.get(aID));
         }
         Expression exp = parser.parseExpression(dataFormular);
         double rValue = exp.getValue(Double.class);
         sData.setValueNum(rValue);
         sData.setValue(String.valueOf(rValue));
         result.add(sData);
      }
   }

   private RealTimeData getMostClostData(List<RealTimeData> datas, long time) {
      RealTimeData lastData = datas.get(0);
      for (RealTimeData data : datas) {
         if (data.getTime() < time) {
            lastData = data;
            continue;
         } else if (data.getTime() > time) {
            if ((data.getTime() - time) < (time - lastData.getTime())) {
               return data;
            }
            return lastData;
         }
         return data;
      }
      return lastData;
   }

   private boolean compareType(ValueUnit.ValueType valueType, ServerSensorType sType) {
      return valueType.toString().equals(sType.toString());
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
      PageRequest pageRequest = new PageRequest(currentPage-1, FlowgateConstant.maxPageSize);
      Page<ServerMapping> mappingPage =
            serverMappingRepository.findAllByVroID(vropsID, pageRequest);
      mappings.addAll(mappingPage.getContent());
      while(!mappingPage.isLast()) {
    	  currentPage++;
    	  pageRequest = new PageRequest(currentPage-1, FlowgateConstant.maxPageSize);
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
      ServerMapping mapping = serverMappingRepository.findOne(serverMaping.getId());
      if (mapping == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND, "ServerMapping not found", null);
      }
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
      serverMappingRepository.delete(id);
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/mapping/merge/{firstid}/{secondid}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
   public void mergeServerMapping(@PathVariable("firstid") String id1,
         @PathVariable("secondid") String id2) {
      if (id1 == id2) {
         throw new WormholeRequestException("Invalid mapping ids");
      }
      ServerMapping firstMapping = serverMappingRepository.findOne(id1);
      if (firstMapping == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND,
               String.format("ServerMapping for %s not found", id1), null);
      }
      ServerMapping secondMapping = serverMappingRepository.findOne(id2);
      if (secondMapping == null) {
         throw new WormholeRequestException(HttpStatus.NOT_FOUND,
               String.format("ServerMapping for %s not found", id2), null);
      }
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

      PageRequest pageRequest = new PageRequest(pageNumber - 1, pageSize);
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
      PageRequest pageRequest = new PageRequest(pageNumber - 1, pageSize);
      Page<ServerMapping> mappings =
            serverMappingRepository.findAllByVcID(vcID, pageRequest);
      return replaceAssetIDwithAssetName(mappings);
   }

   private Page<ServerMapping> replaceAssetIDwithAssetName(Page<ServerMapping> mappings) {
      Map<String, ServerMapping> serverMappings = new HashMap<String, ServerMapping>();
      for (ServerMapping mapping : mappings.getContent()) {
         String asset = mapping.getAsset();
         if (asset != null) {
            serverMappings.put(mapping.getAsset(), mapping);
         }
      }
      List<String> assetIds = new ArrayList<String>(serverMappings.keySet());
      JsonArray array = JsonArray.from(assetIds);
      Iterable<Asset> assets = assetRepository.findAll(array);
      for (Asset asset : assets) {
         serverMappings.get(asset.getId()).setAsset(asset.getAssetName());
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
      assetIPMappingRepository.save(mapping);
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
      PageRequest pageRequest = new PageRequest(currentPage-1, FlowgateConstant.maxPageSize);
      Page<ServerMapping> mappingPage =
            serverMappingRepository.findAllByVcID(vcID, pageRequest);
      mappings.addAll(mappingPage.getContent());
      while(!mappingPage.isLast()) {
    	  currentPage++;
    	  pageRequest = new PageRequest(currentPage - 1, FlowgateConstant.maxPageSize);
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
            realtimeDataRepository.delete(realtimedata.getId());
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
