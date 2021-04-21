/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ScanOptions.ScanOptionsBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricKeyName;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.RealtimeDataUnit;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.AssetIPMapping;
import com.vmware.flowgate.common.model.MetricData;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.TranslateContext;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ValueUnit.MetricUnit;
import com.vmware.flowgate.common.utils.IPAddressUtil;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.AssetIPMappingRepository;
import com.vmware.flowgate.repository.AssetRealtimeDataRepository;
import com.vmware.flowgate.repository.AssetRepository;
import com.vmware.flowgate.util.BaseDocumentUtil;

@Component
public class AssetService {
   private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

   public static final String SERVER_ASSET_NAME_LIST = "asset:servernamelist";
   private static final int SERVER_ASSET_NAME_TIME_OUT = 7200;
   private static final int LIMIT_RESULT = 100;
   private static final String UnknownOulet = "unknownoutlet";
   private static final int TIMESTAMPARRAYLENGTH = 2;//The extraInfo of peakPower and minimumPower metric include since time and event time.
   @Autowired
   private AssetIPMappingRepository assetIPMappingRepository;
   @Autowired
   private AssetRepository assetRepository;
   @Autowired
   private AssetRealtimeDataRepository realtimeDataRepository;
   @Autowired
   private StringRedisTemplate redisTemplate;
   ObjectMapper mapper = new ObjectMapper();
   //This map databaseUnitAndOutputUnitMap will be deprecated in Flowgate-1.3
   public static Map<String, String> databaseUnitAndOutputUnitMap = null;
   private static Map<String,String> metricNameMap = new HashMap<String,String>();
   private static Map<String, String> formulaKeyAndValueUnitNameMapForServer = new HashMap<>();
   private static Map<String, String> formulaKeyAndMetricFormatNameMapForServer = new HashMap<>();
   private static String DISPLAYNAMEANDFORMULANAMEMAPKEY = "DISPLAYNAMEANDFORMULANAMEMAPKEY";
   static {
      metricNameMap.put(MetricName.PDU_HUMIDITY, MetricName.HUMIDITY);
      metricNameMap.put(MetricName.PDU_TEMPERATURE, MetricName.TEMPERATURE);
      metricNameMap.put(MetricName.SERVER_BACK_HUMIDITY, MetricName.HUMIDITY);
      metricNameMap.put(MetricName.SERVER_FRONT_HUMIDITY, MetricName.HUMIDITY);
      metricNameMap.put(MetricName.SERVER_BACK_TEMPREATURE, MetricName.TEMPERATURE);
      metricNameMap.put(MetricName.SERVER_FRONT_TEMPERATURE, MetricName.TEMPERATURE);
      metricNameMap = Collections.unmodifiableMap(metricNameMap);
      databaseUnitAndOutputUnitMap = new HashMap<String, String>();
      databaseUnitAndOutputUnitMap.put(RealtimeDataUnit.Amps.toString(), MetricUnit.A.toString());
      databaseUnitAndOutputUnitMap.put(RealtimeDataUnit.Volts.toString(), MetricUnit.V.toString());
      databaseUnitAndOutputUnitMap.put(RealtimeDataUnit.Celsius.toString(), MetricUnit.C.toString());
      databaseUnitAndOutputUnitMap.put(RealtimeDataUnit.Fahrenheit.toString(), MetricUnit.F.toString());
      databaseUnitAndOutputUnitMap.put(RealtimeDataUnit.KW.toString(), MetricUnit.kW.toString());
      databaseUnitAndOutputUnitMap.put(RealtimeDataUnit.Percent.toString(), MetricUnit.percent.toString());
      databaseUnitAndOutputUnitMap.put(RealtimeDataUnit.KBps.toString(), MetricUnit.kBps.toString());
      databaseUnitAndOutputUnitMap.put(RealtimeDataUnit.Mhz.toString(), MetricUnit.Mhz.toString());
      databaseUnitAndOutputUnitMap.put(RealtimeDataUnit.KB.toString(), MetricUnit.kB.toString());
      databaseUnitAndOutputUnitMap.put(MetricUnit.KW.toString(), MetricUnit.kW.toString());
      databaseUnitAndOutputUnitMap.put(MetricUnit.PERCENT.toString(), MetricUnit.percent.toString());
      databaseUnitAndOutputUnitMap.put(MetricUnit.KWH.toString(), MetricUnit.kWh.toString());
      databaseUnitAndOutputUnitMap = Collections.unmodifiableMap(databaseUnitAndOutputUnitMap);

      formulaKeyAndValueUnitNameMapForServer.put(MetricName.SERVER_USED_PDU_OUTLET_CURRENT, MetricName.PDU_CURRENT);
      formulaKeyAndValueUnitNameMapForServer.put(MetricName.SERVER_USED_PDU_OUTLET_POWER, MetricName.PDU_APPARENT_POWER);
      formulaKeyAndValueUnitNameMapForServer.put(MetricName.SERVER_USED_PDU_OUTLET_VOLTAGE, MetricName.PDU_VOLTAGE);
      formulaKeyAndValueUnitNameMapForServer.put(MetricName.SERVER_CONNECTED_PDU_POWER_LOAD, MetricName.PDU_POWER_LOAD);
      formulaKeyAndValueUnitNameMapForServer.put(MetricName.SERVER_CONNECTED_PDU_CURRENT_LOAD, MetricName.PDU_CURRENT_LOAD);
      formulaKeyAndValueUnitNameMapForServer.put(MetricName.SERVER_FRONT_TEMPERATURE, MetricName.TEMPERATURE);
      formulaKeyAndValueUnitNameMapForServer.put(MetricName.SERVER_BACK_TEMPREATURE, MetricName.TEMPERATURE);
      formulaKeyAndValueUnitNameMapForServer.put(MetricName.SERVER_FRONT_HUMIDITY, MetricName.HUMIDITY);
      formulaKeyAndValueUnitNameMapForServer.put(MetricName.SERVER_BACK_HUMIDITY, MetricName.HUMIDITY);
      formulaKeyAndValueUnitNameMapForServer = Collections.unmodifiableMap(formulaKeyAndValueUnitNameMapForServer);

      formulaKeyAndMetricFormatNameMapForServer.put(MetricName.SERVER_CONNECTED_PDU_CURRENT_LOAD, MetricKeyName.SERVER_CONNECTED_PDUX_CURRENT_LOAD);
      formulaKeyAndMetricFormatNameMapForServer.put(MetricName.SERVER_CONNECTED_PDU_POWER_LOAD, MetricKeyName.SERVER_CONNECTED_PDUX_POWER_LOAD);
      formulaKeyAndMetricFormatNameMapForServer.put(MetricName.SERVER_USED_PDU_OUTLET_CURRENT, MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_CURRENT);
      formulaKeyAndMetricFormatNameMapForServer.put(MetricName.SERVER_USED_PDU_OUTLET_POWER, MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_POWER);
      formulaKeyAndMetricFormatNameMapForServer.put(MetricName.SERVER_USED_PDU_OUTLET_VOLTAGE, MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE);
      formulaKeyAndMetricFormatNameMapForServer.put(MetricName.SERVER_FRONT_TEMPERATURE, MetricKeyName.SERVER_FRONT_TEMPERATURE_LOCATIONX);
      formulaKeyAndMetricFormatNameMapForServer.put(MetricName.SERVER_BACK_TEMPREATURE, MetricKeyName.SERVER_BACK_TEMPREATURE_LOCATIONX);
      formulaKeyAndMetricFormatNameMapForServer.put(MetricName.SERVER_FRONT_HUMIDITY, MetricKeyName.SERVER_FRONT_HUMIDITY_LOCATIONX);
      formulaKeyAndMetricFormatNameMapForServer.put(MetricName.SERVER_BACK_HUMIDITY, MetricKeyName.SERVER_BACK_HUMIDITY_LOCATIONX);
      formulaKeyAndMetricFormatNameMapForServer = Collections.unmodifiableMap(formulaKeyAndMetricFormatNameMapForServer);
   }

   public List<MetricData> getPduMetricsDataById(String assetID, long starttime, int duration){
      Optional<Asset> pduAssetOptional = assetRepository.findById(assetID);
      if(!pduAssetOptional.isPresent()) {
         return null;
      }
      List<RealTimeData> pduMetricsRealtimeDatas =
            realtimeDataRepository.getDataByIDAndTimeRange(assetID, starttime, duration);
      List<ValueUnit> valueunits = new ArrayList<>();

      List<String> metricNames = new ArrayList<String>();
      metricNames.add(MetricName.PDU_TOTAL_POWER);
      metricNames.add(MetricName.PDU_APPARENT_POWER);
      metricNames.add(MetricName.PDU_ACTIVE_POWER);
      metricNames.add(MetricName.PDU_CURRENT);
      metricNames.add(MetricName.PDU_VOLTAGE);
      metricNames.add(MetricName.PDU_FREE_CAPACITY);
      metricNames.add(MetricName.PDU_POWER_LOAD);
      metricNames.add(MetricName.PDU_CURRENT_LOAD);

      //pdu metrics data,such as power/current/voltage
      valueunits.addAll(getValueUnits(pduMetricsRealtimeDatas, metricNames));

      Asset pdu = pduAssetOptional.get();
      //sensor metrics data, such as temperature or humidity
      Map<String, String> formulars = pdu.getMetricsformulars();
      String sensorFormulasInfo = formulars.get(FlowgateConstant.SENSOR);
      Map<String, Map<String, String>> sensorFormulasMap = null;
      if(sensorFormulasInfo != null) {
         sensorFormulasMap =
               pdu.metricsFormulaToMap(sensorFormulasInfo, new TypeReference<Map<String, Map<String, String>>>() {});
      }

      if(sensorFormulasMap != null) {
         Map<String,List<RealTimeData>> assetIdAndRealtimeDataMap = new HashMap<String,List<RealTimeData>>();
         Map<String,String> humidityLocationAndIdMap = sensorFormulasMap.get(MetricName.PDU_HUMIDITY);
         if (humidityLocationAndIdMap != null && !humidityLocationAndIdMap.isEmpty()) {
            valueunits.addAll(generateSensorValueUnit(assetIdAndRealtimeDataMap, starttime,duration,
                  humidityLocationAndIdMap, MetricName.PDU_HUMIDITY));
         }
         Map<String,String> temperatureLocationAndIdMap = sensorFormulasMap.get(MetricName.PDU_TEMPERATURE);
         if(temperatureLocationAndIdMap != null && !temperatureLocationAndIdMap.isEmpty()) {
            valueunits.addAll(generateSensorValueUnit(assetIdAndRealtimeDataMap, starttime,duration,
                  temperatureLocationAndIdMap, MetricName.PDU_TEMPERATURE));
         }
      }
      return generateMetricsDataForPDU(valueunits);
   }

   public List<MetricData> getServerMetricsDataById(String assetID, long starttime, int duration){
      Optional<Asset> serverAssetOptional = assetRepository.findById(assetID);
      if(!serverAssetOptional.isPresent()) {
         throw WormholeRequestException.NotFound("asset", "id", assetID);
      }
      Asset server = serverAssetOptional.get();
      List<MetricData> result = new ArrayList<MetricData>();
      Map<String, String> metricFormula = server.getMetricsformulars();
      if(metricFormula == null || metricFormula.isEmpty()) {
         return result;
      }

      result.addAll(getServerHostMetric(server, starttime, duration));

      Map<String,String> justficationfileds = server.getJustificationfields();
      String allPduPortInfo = justficationfileds.get(FlowgateConstant.PDU_PORT_FOR_SERVER);
      List<String> pduPorts = null;
      Map<String, String> pduAssetIdAndUsedOutletMap = null;
      if (!StringUtils.isEmpty(allPduPortInfo)) {
         pduPorts = Arrays.asList(allPduPortInfo.split(FlowgateConstant.SPILIT_FLAG));
         pduAssetIdAndUsedOutletMap = new HashMap<String, String>();
         for (String pduPortInfo : pduPorts) {
            // startport_FIELDSPLIT_endDeviceName_FIELDSPLIT_endport_FIELDSPLIT_endDeviceAssetID
            // item[0] start port
            // item[1] device name
            // item[2] end port
            // itme[3] assetid
            String items[] = pduPortInfo.split(FlowgateConstant.SEPARATOR);
            pduAssetIdAndUsedOutletMap.put(items[3], items[2]);
         }
      }
      String pduFormulasInfo = metricFormula.get(FlowgateConstant.PDU);
      if(pduFormulasInfo != null) {
         Map<String, Map<String, String>> pduMetrics =
               server.metricsFormulaToMap(pduFormulasInfo, new TypeReference<Map<String, Map<String, String>>>() {});
         List<String> metricNames = new ArrayList<String>();
         metricNames.add(MetricName.PDU_TOTAL_POWER);
         metricNames.add(MetricName.PDU_APPARENT_POWER);
         metricNames.add(MetricName.PDU_CURRENT);
         metricNames.add(MetricName.PDU_VOLTAGE);
         metricNames.add(MetricName.PDU_POWER_LOAD);
         metricNames.add(MetricName.PDU_CURRENT_LOAD);
         for(String pduId : pduMetrics.keySet()) {
            List<RealTimeData> realtimedatas =
                  realtimeDataRepository.getDataByIDAndTimeRange(pduId, starttime, duration);
            List<ValueUnit> valueUnits = getValueUnits(realtimedatas, metricNames);
            String outlet = UnknownOulet;
            if(pduAssetIdAndUsedOutletMap != null) {
               outlet = pduAssetIdAndUsedOutletMap.get(pduId);
            }
            result.addAll(generateServerPduMetricData(valueUnits, pduId, outlet));
         }
      }

      String sensorFormulasInfo = metricFormula.get(FlowgateConstant.SENSOR);
      if (sensorFormulasInfo != null) {
         Map<String, Map<String, String>> sensorFormulars =
               server.metricsFormulaToMap(sensorFormulasInfo, new TypeReference<Map<String, Map<String, String>>>() {});
         Map<String, List<RealTimeData>> assetIdAndRealtimeDataMap =
               new HashMap<String, List<RealTimeData>>();
         for (Map.Entry<String, Map<String, String>> sensorFormula : sensorFormulars.entrySet()) {
            Map<String, String> locationAndIdMap = sensorFormula.getValue();
            String metricName = sensorFormula.getKey();
            List<ValueUnit> valueUnits = generateSensorValueUnit(assetIdAndRealtimeDataMap,
                  starttime, duration, locationAndIdMap, metricName);
            result.addAll(generateServerSensorMetricData(valueUnits, metricName));
         }
      }
      return result;
   }

   private List<MetricData> getServerHostMetric(Asset server, long starttime, int duration) {
      List<MetricData> metricDataList = Lists.newArrayList();
      String hostMetricsFormulaString = server.getMetricsformulars().get(FlowgateConstant.HOST_METRICS);
      if (StringUtils.isBlank(hostMetricsFormulaString)) {
         return metricDataList;
      }
      Map<String, String> hostMetricsFormula = server.metricsFormulaToMap(hostMetricsFormulaString, new TypeReference<Map<String, String>>() {});
      if (hostMetricsFormula == null || hostMetricsFormula.isEmpty()) {
         return metricDataList;
      }
      Map<String, List<String>> assetIdAndMetricsNameList = Maps.newHashMap();
      for (Map.Entry<String, String> itemEntry : hostMetricsFormula.entrySet()) {
         List<String> metricsNameList = assetIdAndMetricsNameList.computeIfAbsent(itemEntry.getValue(), k -> Lists.newArrayList());
         metricsNameList.add(itemEntry.getKey());
      }

      Map<String, List<RealTimeData>> realtimeDataMap = Maps.newHashMap();
      for (Map.Entry<String, List<String>> entry : assetIdAndMetricsNameList.entrySet()) {
         realtimeDataMap.put(entry.getKey(), realtimeDataRepository.getDataByIDAndTimeRange(entry.getKey(), starttime, duration));
      }
      Set<String> specialMetricNames = new HashSet<String>();
      specialMetricNames.add(MetricName.SERVER_AVERAGE_USED_POWER);
      specialMetricNames.add(MetricName.SERVER_PEAK_USED_POWER);
      specialMetricNames.add(MetricName.SERVER_MINIMUM_USED_POWER);
      specialMetricNames.add(MetricName.SERVER_ENERGY_CONSUMPTION);
      specialMetricNames.add(MetricName.SERVER_AVERAGE_TEMPERATURE);
      specialMetricNames.add(MetricName.SERVER_PEAK_TEMPERATURE);
      MetricData metricData;
      for (Map.Entry<String, List<RealTimeData>> realtimeDataEntry : realtimeDataMap.entrySet()) {
         List<String> metricsNameList = assetIdAndMetricsNameList.get(realtimeDataEntry.getKey());
         for (RealTimeData realTimeData : realtimeDataEntry.getValue()) {
            for (ValueUnit valueUnit : realTimeData.getValues()) {
               if (metricsNameList.contains(valueUnit.getKey())) {
                  metricData = new MetricData();
                  long timestamp = 0l;
                  String metricName = valueUnit.getKey();
                  String extraInfo = valueUnit.getExtraidentifier();
                  if(extraInfo == null && specialMetricNames.contains(metricName)) {
                     logger.error("The value of {} is invalid,RealtimeData Id: {}", metricName, realTimeData.getId());
                     continue;
                  }
                  switch (metricName) {
                  case MetricName.SERVER_AVERAGE_USED_POWER:
                     //Time of AvgPower is since time
                     timestamp = Long.valueOf(extraInfo);
                     break;
                  case MetricName.SERVER_PEAK_USED_POWER:
                     String[] sinceTimeAndPeakTime = extraInfo.split(FlowgateConstant.SEPARATOR);
                     if(sinceTimeAndPeakTime.length < TIMESTAMPARRAYLENGTH) {
                        logger.error("The extraInfo of {} is invalid,RealtimeData Id: {}", metricName, realTimeData.getId());
                        continue;
                     }
                     //Time of PeakPower is peakPower time
                     timestamp = Long.valueOf(sinceTimeAndPeakTime[1]);
                     break;
                  case MetricName.SERVER_MINIMUM_USED_POWER:
                     String[] sinceTimeAndMinimumTime = extraInfo.split(FlowgateConstant.SEPARATOR);
                     if(sinceTimeAndMinimumTime.length < TIMESTAMPARRAYLENGTH) {
                        logger.error("The extraInfo of {} is invalid,RealtimeData Id: {}", metricName, realTimeData.getId());
                        continue;
                     }
                     //Time of MinimumPower is minimumPower time
                     timestamp = Long.valueOf(sinceTimeAndMinimumTime[1]);
                     break;
                  case MetricName.SERVER_ENERGY_CONSUMPTION:
                     //Time of energy consumption is since time
                     timestamp = Long.valueOf(extraInfo);
                     break;
                  case MetricName.SERVER_AVERAGE_TEMPERATURE:
                     //Time of average temperature is since time
                     timestamp = Long.valueOf(extraInfo);
                     break;
                  case MetricName.SERVER_PEAK_TEMPERATURE:
                     //Time of energy consumption is since time
                     String[] temperatureSinceTimeAndPeakTime = extraInfo.split(FlowgateConstant.SEPARATOR);
                     if(temperatureSinceTimeAndPeakTime.length < TIMESTAMPARRAYLENGTH) {
                        logger.error("The extraInfo of {} is invalid,RealtimeData Id: {}", metricName, realTimeData.getId());
                        continue;
                     }
                     //Time of PeakTemperature is peak time
                     timestamp = Long.valueOf(temperatureSinceTimeAndPeakTime[1]);
                     break;
                  default:
                     //Time of other host metric is valueUnit current time
                     timestamp = valueUnit.getTime();
                     break;
                  }
                  metricData.setMetricName(valueUnit.getKey());
                  metricData.setTimeStamp(timestamp);
                  metricData.setValue(valueUnit.getValue());
                  metricData.setValueNum(valueUnit.getValueNum());
                  //This kind of logic will be deprecated in next version
                  String unit = databaseUnitAndOutputUnitMap.get(valueUnit.getUnit());
                  if(unit == null) {
                     unit = valueUnit.getUnit();
                  }
                  metricData.setUnit(unit);
                  metricDataList.add(metricData);
               }
            }
         }
      }
      return metricDataList;
   }

   private List<MetricData> getOtherMetricsDataById(String assetID, Long starttime, Integer duration) {
      List<MetricData> metricDataList = new ArrayList<>();
      List<RealTimeData> realTimeData = realtimeDataRepository.getDataByIDAndTimeRange(assetID, starttime, duration);
      if (realTimeData == null || realTimeData.isEmpty()) {
         return metricDataList;
      }
      RealTimeData latestData = findLatestData(realTimeData);
      List<ValueUnit> latestDataValues = latestData.getValues();
      if (latestDataValues == null || latestDataValues.isEmpty()) {
         return metricDataList;
      }
      metricDataList.addAll(generateOtherMetricData(latestDataValues));
      return metricDataList;
   }

   public List<MetricData> getAssetMetricsDataById(String assetID, Long starttime, Integer duration) {
      Optional<Asset> assetOptional = assetRepository.findById(assetID);
      if (!assetOptional.isPresent()) {
         throw WormholeRequestException.NotFound("asset", "id", assetID);
      }
      Asset asset = assetOptional.get();
      if (asset.getCategory() == AssetCategory.PDU) {
         return getPduMetricsDataById(assetID, starttime, duration);
      } else if (asset.getCategory() == AssetCategory.Server) {
         return getServerMetricsDataById(assetID, starttime, duration);
      }
      return getOtherMetricsDataById(assetID, starttime, duration);
   }

   public boolean isAssetNameValidate(String assetName) {
      if(assetName == null) {
         return false;
      }
      if(redisTemplate.hasKey(SERVER_ASSET_NAME_LIST)) {
         if(!redisTemplate.opsForSet().isMember(SERVER_ASSET_NAME_LIST, assetName)) {
            logger.info("Not found this item in redis : " + assetName);
            return false;
         }
      }else {
         Set<String> assetNames = getAssetNamesAndUpdateCache();
         if(!assetNames.contains(assetName)) {
            logger.info("Not found this item : " + assetName);
            return false;
         }
      }
      return true;
   }

   public List<String> searchServerAssetName(String content){
      if(redisTemplate.hasKey(SERVER_ASSET_NAME_LIST)) {
         ScanOptionsBuilder builder = ScanOptions.scanOptions();
         builder.count(redisTemplate.opsForSet().size(SERVER_ASSET_NAME_LIST));
         builder.match("*"+content+"*");
         List<String> matchResult = new ArrayList<String>();
         Cursor<String> curosr = redisTemplate.opsForSet().scan(SERVER_ASSET_NAME_LIST, builder.build());
         while (curosr.hasNext()) {
            if(matchResult.size() > LIMIT_RESULT) {
               break;
            }
            matchResult.add(curosr.next());
         }
         return matchResult;
      }
      Set<String> assetNames = getAssetNamesAndUpdateCache();
      List<String> serverNames = new ArrayList<String>(LIMIT_RESULT+1);
      for(String assetName : assetNames) {
         if(!assetName.contains(content)) {
            continue;
         }
         if(serverNames.size() < LIMIT_RESULT + 1) {
            serverNames.add(assetName);
         }
      }
      return serverNames;
   }

   private Set<String> getAssetNamesAndUpdateCache() {
      List<Asset> assets = assetRepository.findAssetNameByCategory(AssetCategory.Server.name());
      Set<String> assetNames = new HashSet<String>();
      for(Asset asset : assets) {
         assetNames.add(asset.getAssetName());
      }
      if(assetNames.isEmpty()) {
         return assetNames;
      }
      redisTemplate.opsForSet().add(SERVER_ASSET_NAME_LIST, assetNames.toArray(new String[assetNames.size()]));
      if(redisTemplate.hasKey(SERVER_ASSET_NAME_LIST)) {
         redisTemplate.expire(SERVER_ASSET_NAME_LIST, SERVER_ASSET_NAME_TIME_OUT, TimeUnit.SECONDS);
      }
      return assetNames;
   }

   public List<AssetIPMapping> batchCreateMappingFromFile(MultipartFile multipartFile)
         throws IOException {
      List<AssetIPMapping> failureMappings = new ArrayList<AssetIPMapping>();
      InputStream inputStream = multipartFile.getInputStream();
      try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader br = new BufferedReader(inputStreamReader);) {
         String assetIPMappingString = null;
         while ((assetIPMappingString = br.readLine()) != null) {
            AssetIPMapping mapping = parseAssetIPMapingByString(assetIPMappingString);
            if(isAssetNameValidate(mapping.getAssetname()) && IPAddressUtil.isValidIp(mapping.getIp())) {
               BaseDocumentUtil.generateID(mapping);
               assetIPMappingRepository.save(mapping);
            }else {
               failureMappings.add(mapping);
            }
         }
      }
      return failureMappings;
   }

   public static AssetIPMapping parseAssetIPMapingByString(String contentString) {
      String contentsArray[] = contentString.trim().split("\\s+");
      AssetIPMapping mapping = new AssetIPMapping();
      for(String content : contentsArray) {
         if(!content.isEmpty() && mapping.getIp() == null) {
            mapping.setIp(content);
            continue;
         }
         if(!content.isEmpty() && mapping.getAssetname() == null) {
            mapping.setAssetname(content);
            break;
         }
      }
      return mapping;
   }

   private List<MetricData> generateServerPduMetricData(List<ValueUnit> valueUnits, String pduAssetId, String outLet){
      List<MetricData> result = new ArrayList<MetricData>();
      Double serverVoltage = null;
      long serverVoltageReadTime = 0;

      for(ValueUnit value : valueUnits) {
         MetricData data = new MetricData();
         data.setTimeStamp(value.getTime());
         data.setValueNum(value.getValueNum());
         String unit = databaseUnitAndOutputUnitMap.get(value.getUnit());
         if(unit == null) {
            unit = value.getUnit();
         }
         data.setUnit(unit);
         switch (value.getKey()) {
         case MetricName.PDU_TOTAL_POWER:
            data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_POWER, pduAssetId));
            result.add(data);
            break;
         case MetricName.PDU_APPARENT_POWER:
            String outlet_pdu_power_extraidentifier = value.getExtraidentifier();
            if(outLet.equals(outlet_pdu_power_extraidentifier)) {
               data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_POWER, pduAssetId, value.getExtraidentifier()));
               result.add(data);
            }
            break;
         case MetricName.PDU_CURRENT:
            String outlet_pdu_current_extraidentifier = value.getExtraidentifier();
            if(outlet_pdu_current_extraidentifier == null) {
               data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_CURRENT, pduAssetId));
               result.add(data);
            } else if(outLet.equals(outlet_pdu_current_extraidentifier)) {
               data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_CURRENT, pduAssetId, value.getExtraidentifier()));
               result.add(data);
            }
            break;
         case MetricName.PDU_VOLTAGE:
            String extraidentifier = value.getExtraidentifier();
            serverVoltageReadTime = data.getTimeStamp();
            //some pdus without outlet metrics,but have inlet metrics
            if(serverVoltage == null) {
               serverVoltage = data.getValueNum();
            }
            if(outLet.equals(extraidentifier)) {
               data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE, pduAssetId, value.getExtraidentifier()));
               result.add(data);
               serverVoltage = data.getValueNum();
            }
            break;
         case MetricName.PDU_POWER_LOAD:
            data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_POWER_LOAD, pduAssetId));
            result.add(data);
            break;
         case MetricName.PDU_CURRENT_LOAD:
            data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_CURRENT_LOAD, pduAssetId));
            result.add(data);
            break;
         default:
            break;
         }
      }
      if(serverVoltage != null) {
         MetricData data = new MetricData();
         data.setMetricName(MetricName.SERVER_VOLTAGE);
         data.setTimeStamp(serverVoltageReadTime);
         data.setValueNum(serverVoltage);
         data.setUnit(MetricUnit.V.toString());
         result.add(data);
      }
      return result;
   }

   private List<MetricData> generateServerSensorMetricData(List<ValueUnit> valueUnits, String metricName){
      List<MetricData> result = new ArrayList<MetricData>();
      if(valueUnits == null || valueUnits.isEmpty()) {
         return result;
      }
      for(ValueUnit value : valueUnits) {
         MetricData data = new MetricData();
         data.setTimeStamp(value.getTime());
         data.setValueNum(value.getValueNum());
         String unit = databaseUnitAndOutputUnitMap.get(value.getUnit());
         if(unit == null) {
            unit = value.getUnit();
         }
         data.setUnit(unit);
         switch (value.getKey()) {
         case MetricName.HUMIDITY:
            switch (metricName) {
            case MetricName.SERVER_BACK_HUMIDITY:
               data.setMetricName(String.format(MetricKeyName.SERVER_BACK_HUMIDITY_LOCATIONX, value.getExtraidentifier()));
               result.add(data);
               break;
            case MetricName.SERVER_FRONT_HUMIDITY:
               data.setMetricName(String.format(MetricKeyName.SERVER_FRONT_HUMIDITY_LOCATIONX, value.getExtraidentifier()));
               result.add(data);
               break;
            default:
               break;
            }
            break;
         case MetricName.TEMPERATURE:
            switch (metricName) {
            case MetricName.SERVER_BACK_TEMPREATURE:
               data.setMetricName(String.format(MetricKeyName.SERVER_BACK_TEMPREATURE_LOCATIONX, value.getExtraidentifier()));
               result.add(data);
               break;
            case MetricName.SERVER_FRONT_TEMPERATURE:
               data.setMetricName(String.format(MetricKeyName.SERVER_FRONT_TEMPERATURE_LOCATIONX, value.getExtraidentifier()));
               result.add(data);
               break;
            default:
               break;
            }
            break;
         default:
            break;
         }
      }
      return result;
   }

   private List<MetricData> generateOtherMetricData(List<ValueUnit> valueUnits) {
      List<MetricData> result = new ArrayList<>();
      if(valueUnits == null || valueUnits.isEmpty()) {
         return result;
      }
      MetricData data;
      for (ValueUnit value : valueUnits) {
         data = new MetricData();
         data.setTimeStamp(value.getTime());
         data.setValueNum(value.getValueNum());
         data.setValue(value.getValue());
         data.setMetricName(value.getKey());
         String unit = databaseUnitAndOutputUnitMap.get(value.getUnit());
         if(unit == null) {
            unit = value.getUnit();
         }
         data.setUnit(unit);
         result.add(data);
      }
      return result;
   }

   private RealTimeData findLatestData(List<RealTimeData> realtimeDatas) {
      RealTimeData latestResult = realtimeDatas.get(0);
      for(int i=0;i<realtimeDatas.size()-1;i++) {
         if(latestResult.getTime() < realtimeDatas.get(i+1).getTime()) {
            latestResult = realtimeDatas.get(i+1);
         }
      }
      return latestResult;
   }

   private List<ValueUnit> getValueUnits(List<RealTimeData> realtimeDatas,
         List<String> metricsName){
      List<ValueUnit> valueunits = new ArrayList<>();
      if(realtimeDatas == null || realtimeDatas.isEmpty()) {
         return valueunits;
      }
      RealTimeData realTimeData = findLatestData(realtimeDatas);
      for(ValueUnit value : realTimeData.getValues()) {
         if(metricsName.contains(value.getKey())) {
            valueunits.add(value);
         }
      }
      return valueunits;
   }

   private List<ValueUnit> generateSensorValueUnit(Map<String,List<RealTimeData>> assetIdAndRealtimeDataMap,
         long starttime, int duration, Map<String,String> locationAndIdMap, String metricName){
      List<ValueUnit> valueunits = new ArrayList<>();
      for(Map.Entry<String, String> locationInfoAndId : locationAndIdMap.entrySet()) {
         String formula = locationInfoAndId.getValue();
         String location = locationInfoAndId.getKey();
         String ids[] = formula.split("\\+|-|\\*|/|\\(|\\)");
         for(String assetId : ids) {
            List<RealTimeData> realtimeDatas = null;
            if(!assetIdAndRealtimeDataMap.containsKey(assetId)) {
               realtimeDatas =
                     realtimeDataRepository.getDataByIDAndTimeRange(assetId, starttime, duration);
               assetIdAndRealtimeDataMap.put(assetId, realtimeDatas);
            }
            realtimeDatas = assetIdAndRealtimeDataMap.get(assetId);
            if(realtimeDatas == null || realtimeDatas.isEmpty()) {
               continue;
            }

            RealTimeData realTimeData = findLatestData(realtimeDatas);
            for(ValueUnit value : realTimeData.getValues()) {
               if(value.getKey().equals(metricNameMap.get(metricName))) {
                  if(location.indexOf(FlowgateConstant.SEPARATOR) > -1) {
                     location = location.replace(FlowgateConstant.SEPARATOR, FlowgateConstant.UNDERLINE);
                  }
                  value.setExtraidentifier(location);
                  valueunits.add(value);
               }
            }
         }
      }
      return valueunits;
   }

   private List<MetricData> generateMetricsDataForPDU(List<ValueUnit> valueunits){
      List<MetricData> result = new ArrayList<MetricData>();
      for (ValueUnit valueunit : valueunits) {
         MetricData metricData = new MetricData();
         metricData.setTimeStamp(valueunit.getTime());
         metricData.setValue(valueunit.getValue());
         metricData.setValueNum(valueunit.getValueNum());
         String unit = databaseUnitAndOutputUnitMap.get(valueunit.getUnit());
         if(unit == null) {
            unit = valueunit.getUnit();
         }
         metricData.setUnit(unit);
         String extraidentifier = valueunit.getExtraidentifier();

         switch (valueunit.getKey()) {
         case MetricName.PDU_ACTIVE_POWER:
            //PDU|INLET:1|ActivePower
            metricData.setMetricName(String.format(MetricKeyName.PDU_XLET_ACTIVE_POWER, extraidentifier));
            result.add(metricData);
            break;
         case MetricName.PDU_APPARENT_POWER:
            metricData.setMetricName(String.format(MetricKeyName.PDU_XLET_APPARENT_POWER,
                  extraidentifier));
            result.add(metricData);
            break;
         case MetricName.PDU_CURRENT:
            if(extraidentifier != null) {
               String extraidentifierList[] = extraidentifier.split("\\|");
               if(extraidentifierList.length == 1) {
                  metricData.setMetricName(
                        String.format(MetricKeyName.PDU_XLET_CURRENT, extraidentifier));
               }else if(extraidentifierList.length == 2) {
                  String inlet = extraidentifierList[0];
                  String pole = extraidentifierList[1];
                  metricData.setMetricName(
                        String.format(MetricKeyName.PDU_INLET_POLE_CURRENT, inlet, pole));
               }
               result.add(metricData);
            }else {
               metricData.setMetricName(MetricName.PDU_TOTAL_CURRENT);
               result.add(metricData);
            }
            break;
         case MetricName.PDU_VOLTAGE:
            if(extraidentifier != null) {
               String extraidentifierList[] = extraidentifier.split("\\|");
               if(extraidentifierList.length == 1) {
                  metricData.setMetricName(
                        String.format(MetricKeyName.PDU_XLET_VOLTAGE, extraidentifier));
               }else if(extraidentifierList.length == 2) {
                  String inlet = extraidentifierList[0];
                  String pole = extraidentifierList[1];
                  metricData.setMetricName(
                        String.format(MetricKeyName.PDU_INLET_POLE_VOLTAGE, inlet, pole));
               }
               result.add(metricData);
            }
            break;
         case MetricName.PDU_FREE_CAPACITY:
            if(extraidentifier != null) {
               String extraidentifierList[] = extraidentifier.split("\\|");
               if(extraidentifierList.length == 1) {
                  metricData.setMetricName(String.format(MetricKeyName.PDU_XLET_FREE_CAPACITY,
                        extraidentifier));
               }else if(extraidentifierList.length == 2) {
                  String inlet = extraidentifierList[0];
                  String pole = extraidentifierList[1];
                  metricData.setMetricName(
                        String.format(MetricKeyName.PDU_INLET_POLE_FREE_CAPACITY, inlet, pole));
               }
               result.add(metricData);
            }
            break;
         case MetricName.PDU_CURRENT_LOAD:
            metricData.setMetricName(MetricName.PDU_CURRENT_LOAD);
            result.add(metricData);
            break;
         case MetricName.PDU_POWER_LOAD:
            metricData.setMetricName(MetricName.PDU_POWER_LOAD);
            result.add(metricData);
            break;
         case MetricName.PDU_TOTAL_POWER:
            metricData.setMetricName(MetricName.PDU_TOTAL_POWER);
            result.add(metricData);
            break;
         case MetricName.PDU_HUMIDITY:
            metricData.setMetricName(String.format(MetricKeyName.PDU_HUMIDITY_LOCATIONX,
                  extraidentifier));
            result.add(metricData);
            break;
         case MetricName.PDU_TEMPERATURE:
            metricData.setMetricName(String.format(MetricKeyName.PDU_TEMPERATURE_LOCATIONX,
                  extraidentifier));
            result.add(metricData);
            break;
         default:
            break;
         }
      }
      return result;
   }

   public void mappingFacilityForServerAsset(Asset asset) {
      Optional<Asset> oldAssetOptional = assetRepository.findById(asset.getId());
      if(!oldAssetOptional.isPresent()) {
         throw new WormholeRequestException(HttpStatus.INTERNAL_SERVER_ERROR, "Asset not found", null);
      }
      Asset oldAsset = oldAssetOptional.get();
      List<String> pdus = asset.getPdus();
      if(pdus != null) {
         oldAsset.setPdus(pdus);
      }
      List<String> switchs = asset.getSwitches();
      if(switchs != null) {
         oldAsset.setSwitches(switchs);
      }
      Map<String, String> newMetricsformulas = asset.getMetricsformulars();
      if(newMetricsformulas != null && newMetricsformulas.containsKey(FlowgateConstant.SENSOR)) {
         Map<String, String> oldMetricsformulas = oldAsset.getMetricsformulars();
         Map<String, Map<String, String>> oldSensorformulasMap = null;
         String oldSensorFormulasInfo  = null;
         if(oldMetricsformulas.containsKey(FlowgateConstant.SENSOR)) {
            oldSensorFormulasInfo = oldMetricsformulas.get(FlowgateConstant.SENSOR);
            oldSensorformulasMap =
                  asset.metricsFormulaToMap(oldSensorFormulasInfo, new TypeReference<Map<String, Map<String, String>>>() {});
         }else {
            oldSensorformulasMap = new HashMap<String,Map<String, String>>();
         }
         String newSensorFormulaInfo = newMetricsformulas.get(FlowgateConstant.SENSOR);
         Map<String, Map<String, String>> newSensorformulasMap =
               asset.metricsFormulaToMap(newSensorFormulaInfo, new TypeReference<Map<String, Map<String, String>>>() {});
         generateSensorFormula(oldSensorformulasMap, newSensorformulasMap);
         oldSensorFormulasInfo = asset.metricsFormulaToString(oldSensorformulasMap);
         oldMetricsformulas.put(FlowgateConstant.SENSOR, oldSensorFormulasInfo);
         oldAsset.setMetricsformulars(oldMetricsformulas);
      }
      oldAsset.setLastupdate(System.currentTimeMillis());
      assetRepository.save(oldAsset);
   }


   private void generateSensorFormula(Map<String, Map<String, String>> oldMetricsformulas,Map<String, Map<String, String>> newMetricsformulas){
       for(Map.Entry<String,Map<String, String>> metricNameMap : newMetricsformulas.entrySet()) {
          switch (metricNameMap.getKey()) {
          case MetricName.SERVER_FRONT_TEMPERATURE:
             oldMetricsformulas.put(MetricName.SERVER_FRONT_TEMPERATURE, generatePositionAndIdMap(metricNameMap.getValue()));
             break;
          case MetricName.SERVER_BACK_TEMPREATURE:
             oldMetricsformulas.put(MetricName.SERVER_BACK_TEMPREATURE, generatePositionAndIdMap(metricNameMap.getValue()));
             break;
          case MetricName.SERVER_FRONT_HUMIDITY:
             oldMetricsformulas.put(MetricName.SERVER_FRONT_HUMIDITY, generatePositionAndIdMap(metricNameMap.getValue()));
             break;
          case MetricName.SERVER_BACK_HUMIDITY:
             oldMetricsformulas.put(MetricName.SERVER_BACK_HUMIDITY, generatePositionAndIdMap(metricNameMap.getValue()));
             break;
          default:
             break;
          }
       }
   }

   private Map<String,String> generatePositionAndIdMap(Map<String,String> sensorIdMap){
      Map<String,String> positionAndSensorIdMap = new HashMap<String,String>();
      for(String sensorId : sensorIdMap.keySet()) {
         Optional<Asset> sensorOptional = assetRepository.findById(sensorId);
         if(sensorOptional.isPresent()) {
            String position = getSensorPositionInfo(sensorOptional.get());
            positionAndSensorIdMap.put(position, sensorId);
         }
      }
      return positionAndSensorIdMap;
   }

   private String getSensorPositionInfo(Asset asset) {
      StringBuilder positionInfo = new StringBuilder();
      Map<String,String> sensorAssetJustfication = asset.getJustificationfields();
      int rackUnitNumber = asset.getCabinetUnitPosition();
      String rackUnitInfo = null;
      String positionFromAsset = null;

      if(rackUnitNumber != 0) {
         rackUnitInfo = FlowgateConstant.RACK_UNIT_PREFIX  + rackUnitNumber;
         positionInfo.append(rackUnitInfo);
         if(sensorAssetJustfication == null || sensorAssetJustfication.isEmpty() ||
               sensorAssetJustfication.get(FlowgateConstant.SENSOR) == null) {
            return positionInfo.toString();
         }
         String sensorInfo = sensorAssetJustfication.get(FlowgateConstant.SENSOR);
         try {
            Map<String,String> sensorInfoMap = mapper.readValue(sensorInfo, new TypeReference<Map<String,String>>() {});
            positionFromAsset = sensorInfoMap.get(FlowgateConstant.POSITION);
            if(positionFromAsset != null) {
               positionInfo.append(FlowgateConstant.SEPARATOR + positionFromAsset);
            }
         } catch (IOException e) {
            return positionInfo.toString();
         }
      }else {
         if(sensorAssetJustfication == null || sensorAssetJustfication.isEmpty() ||
               sensorAssetJustfication.get(FlowgateConstant.SENSOR) == null) {
            positionInfo.append(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION);
            return positionInfo.toString();
         }
         String sensorInfo = sensorAssetJustfication.get(FlowgateConstant.SENSOR);
         try {
            Map<String,String> sensorInfoMap = mapper.readValue(sensorInfo, new TypeReference<Map<String,String>>() {});
            positionFromAsset = sensorInfoMap.get(FlowgateConstant.POSITION);
            if(positionFromAsset != null) {
               positionInfo.append(positionFromAsset);
            }else {
               positionInfo.append(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION);
            }
         } catch (IOException e) {
            positionInfo.append(FlowgateConstant.DEFAULT_CABINET_UNIT_POSITION);
            return positionInfo.toString();
         }
      }
      return positionInfo.toString();
   }

   private Asset getAssetById(String assetID) {
      Optional<Asset> assetOptional = assetRepository.findById(assetID);
      if (!assetOptional.isPresent()) {
         throw WormholeRequestException.NotFound("asset", "id", assetID);
      }
      return assetOptional.get();
   }

   private void filterValueUnitsByMetricNames(List<ValueUnit> valueunits, List<String> metricsName) {
      Iterator<ValueUnit> ite = valueunits.iterator();
      while (ite.hasNext()) {
         ValueUnit valueUnit = ite.next();
         if(!metricsName.contains(valueUnit.getKey())) {
            ite.remove();
         }
      }
   }

   private List<ValueUnit> getValueUnitsByAssetID(String assetID, long starttime, int duration) {
      List<RealTimeData> realtimeDatas =
            realtimeDataRepository.getDataByIDAndTimeRange(assetID, starttime, duration);
      List<ValueUnit> valueunits = new ArrayList<ValueUnit>();
      if(realtimeDatas.isEmpty()) {
         return valueunits;
      }
      for(RealTimeData realTimeData : realtimeDatas) {
         for(ValueUnit value : realTimeData.getValues()) {
            valueunits.add(value);
         }
      }
      return valueunits;
   }

   /**
    * PDU'sensor formula
    * Sensor:{
    *    Temperature:{"rackUnit01_FIELDSPLIT_INLET", "FLowgateAssetIDs"},
    *    Humidity:{"rackUnit01_FIELDSPLIT_INLET", "FLowgateAssetIDs"}
    * }
    * Note: Now the FLowgateAssetIDs only include one assetID,
    * in the future the FLowgateAssetIDs may be a formula with multiple IDs.
    * The sample values of sensor's position on the rack are INLET, OUTLET, EXTERNAL
    *
    * Server'sensor formula
    * Sensor:{
    *    FrontTemperature:{"INLET","FLowgateAssetIDs"},
    *    BackTemperature:{},
    *    FrontHumidity:{},
    *    BackHumidity:{},
    * }
    *
    * @param asset
    * @return
    */
   private Set<String> extractSensorIDfromFormula(Asset asset){
      Map<String, String> formulas = asset.getMetricsformulars();
      Set<String> assetIds = new HashSet<String>();
      if(formulas == null || formulas.get(FlowgateConstant.SENSOR) == null) {
         return assetIds;
      }
      TypeReference<Map<String, Map<String, String>>> typeReference = new TypeReference<Map<String, Map<String, String>>>() {};
      Map<String, Map<String, String>> sensorFormulasMap =
            getFormula(asset, FlowgateConstant.SENSOR, formulas, typeReference);
      for(Map.Entry<String, Map<String, String>> metricAndFormulaEntry : sensorFormulasMap.entrySet()) {
         Map<String, String> locationAndIdsMap = metricAndFormulaEntry.getValue();
         for(Map.Entry<String, String> locationInfoAndId : locationAndIdsMap.entrySet()) {
            String formula = locationInfoAndId.getValue();
            String ids[] = formula.split("\\+|-|\\*|/|\\(|\\)");
            for(String id : ids) {
               if(id.length() == FlowgateConstant.COUCHBASEIDLENGTH) {
                  assetIds.add(id);
               }
            }
         }
      }
      return assetIds;
   }

   public List<MetricData> getMetricsByID(String assetID, Long starttime, Integer duration){
      //Check and get asset by ID
      Asset asset = getAssetById(assetID);
      //Get ValueUnits
      Map<String, List<ValueUnit>> assetAndValueUnitsMap = new HashMap<String, List<ValueUnit>>();
      List<MetricData> metricDatas = null;
      switch (asset.getCategory()) {
      case PDU:
         assetAndValueUnitsMap = getPDURawMetrics(asset, starttime, duration);
         //return translateToMetricsDataForPDU(pduValueUnits);
      case Server:
         //1. Get all metric Data
         assetAndValueUnitsMap = getServerRawMetrics(asset, starttime, duration);
         //2. Remove or filter
         //3. Translate
         metricDatas = translateToMetricDataForServer(assetAndValueUnitsMap, asset);
      default:
         assetAndValueUnitsMap = getRawMetrics(asset, starttime, duration);
         //return translateToMetricDataForOtherAsset(otherValueUnits);
      }
      return metricDatas;
   }

   private List<MetricData> translateToMetricDataForServer(Map<String, List<ValueUnit>> assetAndValueUnitsMap,
         Asset asset){
      List<MetricData> metricDatas = new ArrayList<MetricData>();
      Map<String, Map<String, String>> displayNameAndFormulasMap = getMetricDispalyNameAndFormularMapForServer(asset);
      if(displayNameAndFormulasMap.isEmpty() ||
            !displayNameAndFormulasMap.containsKey(DISPLAYNAMEANDFORMULANAMEMAPKEY)) {
         return metricDatas;
      }
      //The displayNameAndFormulaKeyMap use to find a method to translate the valueUnit to MetricData
      Map<String, String> displayNameAndFormulaKeyMap =
            displayNameAndFormulasMap.get(DISPLAYNAMEANDFORMULANAMEMAPKEY);
      displayNameAndFormulasMap.remove(DISPLAYNAMEANDFORMULANAMEMAPKEY);
      CompareValueUnitByTime comparator = new CompareValueUnitByTime();
      for(Map.Entry<String, Map<String, String>> displayNameAndFormulaEntry : displayNameAndFormulasMap.entrySet()) {
         String displayName = displayNameAndFormulaEntry.getKey();
         //Key:MetricName Value:Formula
         Map<String, String> valueUnitNameAndFormulaMap = displayNameAndFormulaEntry.getValue();
         for(Map.Entry<String, String> valueUnitNameAndFormula : valueUnitNameAndFormulaMap.entrySet()) {
            String valueUnitName = valueUnitNameAndFormula.getKey();
            String formula = valueUnitNameAndFormula.getValue();
            String ids[] = formula.split("\\+|-|\\*|/|\\(|\\)");
            Map<String, List<ValueUnit>> idAndValues = new HashMap<>();
            for(String id : ids) {
               List<ValueUnit> inletVoltages = new ArrayList<>();
               List<ValueUnit> outletVoltages = new ArrayList<>();
               List<ValueUnit> valueUnits = new ArrayList<>();
               for(ValueUnit valueUnit : assetAndValueUnitsMap.get(id)) {
                  if(!valueUnitName.equals(valueUnit.getKey())) {
                     continue;
                  }
                  String extraidentifier = valueUnit.getExtraidentifier();
                  String outlet = null;
                  switch (valueUnitName) {
                  case MetricName.PDU_CURRENT:
                     //PDU:23551d6dacf2432c8a3edbc6bbc922cd|OUTLET:1|Current
                     if(displayName.contains(FlowgateConstant.OUTLET_NAME_PREFIX)
                           && extraidentifier != null) {
                        outlet = displayName.split("|")[1];
                        if(outlet.equals(extraidentifier)) {
                           valueUnits.add(valueUnit);
                        }
                     }
                     //PDU:23551d6dacf2432c8a3edbc6bbc922cd|Current
                     if(!displayName.contains(FlowgateConstant.OUTLET_NAME_PREFIX)
                           && !displayName.contains(FlowgateConstant.INLET_NAME_PREFIX) ) {
                        valueUnits.add(valueUnit);
                     }
                     break;
                  case MetricName.PDU_APPARENT_POWER:
                     //Sample value of toName: PDU:pduID|OUTLET:1|Power
                     outlet = displayName.split("|")[1];
                     if(outlet.equals(extraidentifier)) {
                        valueUnits.add(valueUnit);
                     }
                     break;
                  case MetricName.PDU_VOLTAGE:
                     //DisplayName: PDU:23551d6dacf2432c8a3edbc6bbc922cd|OUTLET:1|Voltage
                     outlet = displayName.split("|")[1];
                     if(displayName.contains(FlowgateConstant.OUTLET_NAME_PREFIX)) {
                        if(outlet.equals(extraidentifier)) {
                           valueUnits.add(valueUnit);
                        }
                     }
                     //DisplayName:Voltage
                     if(MetricName.SERVER_VOLTAGE.equals(displayName)) {
                        if (extraidentifier
                              .indexOf(FlowgateConstant.INLET_NAME_PREFIX) > 0) {
                           inletVoltages.add(valueUnit);
                        } else if (outlet.equals(extraidentifier)) {
                           outletVoltages.add(valueUnit);
                        }
                     }
                     break;
                  default:
                     valueUnits.add(valueUnit);
                     break;
                  }
               }
               if(MetricName.SERVER_VOLTAGE.equals(displayName)) {
                  if(outletVoltages.isEmpty()) {
                     if(inletVoltages.isEmpty()) {
                        continue;
                     }
                     valueUnits = inletVoltages;
                  }
                  valueUnits = outletVoltages;
               }
               if(valueUnits.isEmpty()) {
                  continue;
               }
               Collections.sort(valueUnits, comparator);
               idAndValues.put(id, valueUnits);
            }
            if(!idAndValues.isEmpty()) {
               metricDatas.addAll(convertValueUnitToMetricData(displayName,displayNameAndFormulaKeyMap.get(displayName), formula, idAndValues));
            }
         }
      }
      return metricDatas;
   }

   /**
    * Sample of TranslateContext
    * {
    *    DisplayName:"Temperature|%S",
    *    Formulas:"23551d6dacf2432c8a3edbc6bbc922cd + 123456",
    *    valueData:[{id,valueUnit}]
    * }
    * One displayName may correspond to one or multiple values
    * When the API of get-latest-data use the method, One MetricName correspond one value
    * When the API of get-duration-data use the method, One MetricName correspond multiple values
    * @param displayName
    * @param formula
    * @param idAndValues
    * @return
    */
   private List<MetricData> convertValueUnitToMetricData(String displayName, String formulaKey,
         String formula, Map<String, List<ValueUnit>> idAndValues){
      List<ValueUnit> valueUnits = idAndValues.get(idAndValues.keySet().iterator().next());
      Map<String, ValueUnit> idAndValueUnitMap = null;
      //Assuming the collection length is the same
      List<MetricData> metricDatas = new ArrayList<MetricData>();
      for(int i = 0; i < valueUnits.size(); i++) {
         //Sample data: <id1, valueUnit1>,<id2, valueUnit1>
         idAndValueUnitMap = new HashMap<String, ValueUnit>();
         TranslateContext translateContext = new TranslateContext();
         for(Map.Entry<String, List<ValueUnit>> valuesEntry : idAndValues.entrySet()) {
            String id = valuesEntry.getKey();
            ValueUnit value = valuesEntry.getValue().get(i);
            idAndValueUnitMap.put(id, value);
         }
         translateContext.setDisplayName(displayName);
         translateContext.setFormula(formula);
         translateContext.setValueUnits(idAndValueUnitMap);
         MetricData metricData = new MetricData();
         //Add translate logic
         metricDatas.add(metricData);
      }
      return metricDatas;
   }

   private Map<String, List<ValueUnit>> getPDURawMetrics(Asset pduAsset, long starttime, int duration){
      List<ValueUnit> pduValueunits = getValueUnitsByAssetID(pduAsset.getId(), starttime, duration);
      Map<String, List<ValueUnit>> assetAndValueUnitsMap = new HashMap<String, List<ValueUnit>>();
      if(!pduValueunits.isEmpty()) {
         //Filter pdu metric by metric name
         List<String> metricNames = new ArrayList<String>();
         metricNames.add(MetricName.PDU_TOTAL_POWER);
         metricNames.add(MetricName.PDU_TOTAL_CURRENT);
         metricNames.add(MetricName.PDU_APPARENT_POWER);
         metricNames.add(MetricName.PDU_ACTIVE_POWER);
         metricNames.add(MetricName.PDU_CURRENT);
         metricNames.add(MetricName.PDU_VOLTAGE);
         metricNames.add(MetricName.PDU_FREE_CAPACITY);
         metricNames.add(MetricName.PDU_POWER_LOAD);
         metricNames.add(MetricName.PDU_CURRENT_LOAD);
         filterValueUnitsByMetricNames(pduValueunits, metricNames);
         assetAndValueUnitsMap.put(pduAsset.getId(), pduValueunits);
      }
      //sensor valueUnits, such as temperature or humidity
      Set<String> assetIds = extractSensorIDfromFormula(pduAsset);
      if(!assetIds.isEmpty()) {
         List<String> sensorMetricNames = new ArrayList<String>();
         sensorMetricNames.add(MetricName.PDU_TEMPERATURE);
         sensorMetricNames.add(MetricName.PDU_HUMIDITY);
         for(String sensorId : assetIds) {
            List<ValueUnit> valueUnits = getValueUnitsByAssetID(sensorId, starttime, duration);
            if(!valueUnits.isEmpty()) {
               filterValueUnitsByMetricNames(valueUnits, sensorMetricNames);
               assetAndValueUnitsMap.put(sensorId, valueUnits);
            }
         }
      }
      return assetAndValueUnitsMap;
   }

   private Map<String, List<ValueUnit>> getServerRawMetrics(Asset server, long starttime, int duration){
      Map<String, String> metricFormula = server.getMetricsformulars();
      Map<String, List<ValueUnit>> assetAndValueUnitsMap = new HashMap<String, List<ValueUnit>>();
      if(metricFormula == null || metricFormula.isEmpty()) {
         return assetAndValueUnitsMap;
      }
      //Host ValueUnits of Server
      assetAndValueUnitsMap =
            getServerUsageRawMetrics(server, starttime, duration);
      //PDU ValueUnits of Server
      TypeReference<Map<String, Map<String, String>>> typeReference = new TypeReference<Map<String, Map<String, String>>>() {};
      Map<String, Map<String, String>> pduMetricsFormula =
            getFormula(server, FlowgateConstant.PDU, metricFormula, typeReference);
      if(pduMetricsFormula != null) {
         List<String> metricNames = new ArrayList<String>();
         metricNames.add(MetricName.PDU_TOTAL_POWER);
         metricNames.add(MetricName.PDU_TOTAL_CURRENT);
         metricNames.add(MetricName.PDU_APPARENT_POWER);
         metricNames.add(MetricName.PDU_CURRENT);
         metricNames.add(MetricName.PDU_VOLTAGE);
         metricNames.add(MetricName.PDU_POWER_LOAD);
         metricNames.add(MetricName.PDU_CURRENT_LOAD);
         for(String pduId : pduMetricsFormula.keySet()) {
            List<ValueUnit> valueUnits = getValueUnitsByAssetID(pduId, starttime, duration);
            if(!valueUnits.isEmpty()) {
               filterValueUnitsByMetricNames(valueUnits, metricNames);
               assetAndValueUnitsMap.put(pduId, valueUnits);
            }
         }
      }
      //Sensor ValueUnit of Server
      Set<String> assetIds = extractSensorIDfromFormula(server);
      if(!assetIds.isEmpty()) {
         List<String> sensorMetricNames = new ArrayList<String>();
         sensorMetricNames.add(MetricName.TEMPERATURE);
         sensorMetricNames.add(MetricName.HUMIDITY);
         for(String sensorId : assetIds) {
            List<ValueUnit> valueUnits = getValueUnitsByAssetID(sensorId, starttime, duration);
            if(!valueUnits.isEmpty()) {
               filterValueUnitsByMetricNames(valueUnits, sensorMetricNames);
               assetAndValueUnitsMap.put(sensorId, valueUnits);
            }
         }
      }
      return assetAndValueUnitsMap;
   }

   private Map<String, List<ValueUnit>> getRawMetrics(Asset asset, Long starttime, Integer duration){
      List<ValueUnit> valueUnits = getValueUnitsByAssetID(asset.getId(), starttime, duration);
      Map<String, List<ValueUnit>> assetAndValueUnitsMap = new HashMap<String, List<ValueUnit>>();
      if(valueUnits.isEmpty()) {
         return assetAndValueUnitsMap;
      }
      assetAndValueUnitsMap.put(asset.getId(), valueUnits);
      return assetAndValueUnitsMap;
   }

   private Map<String, List<ValueUnit>> getServerUsageRawMetrics(Asset server, long starttime, int duration) {
      String hostMetricsFormulaString = server.getMetricsformulars().get(FlowgateConstant.HOST_METRICS);
      Map<String, List<ValueUnit>> assetAndValueUnitsMap = new HashMap<String, List<ValueUnit>>();
      if (StringUtils.isBlank(hostMetricsFormulaString)) {
         return assetAndValueUnitsMap;
      }
      Map<String, String> hostMetricsFormula =
            server.metricsFormulaToMap(hostMetricsFormulaString, new TypeReference<Map<String, String>>() {});
      Map<String, List<String>> assetIdAndMetricsNameList = Maps.newHashMap();
      for (Map.Entry<String, String> itemEntry : hostMetricsFormula.entrySet()) {
         List<String> metricsNameList = assetIdAndMetricsNameList.computeIfAbsent(itemEntry.getValue(), k -> Lists.newArrayList());
         metricsNameList.add(itemEntry.getKey());
      }
      for (Map.Entry<String, List<String>> entry : assetIdAndMetricsNameList.entrySet()) {
         String assetID = entry.getKey();//Usually this assetID is the serverAssetID
         List<ValueUnit> valueUnits = getValueUnitsByAssetID(assetID, starttime, duration);
         if(valueUnits.isEmpty()) {
            continue;
         }
         List<String> metricsNameList = assetIdAndMetricsNameList.get(assetID);
         filterValueUnitsByMetricNames(valueUnits, metricsNameList);
         assetAndValueUnitsMap.put(assetID, valueUnits);
      }
      return assetAndValueUnitsMap;
   }

   /**
    * @param server
    * @return
    * Map<String, Map<String, String>>
    * {
    *    DisplayName:{metricName,formula}
    * }
    *
    * Sample value : SERVER_CONNECTED_PDU_CURRENT
    * {
    *    PDU:23551d6dacf2432c8a3edbc6bbc922cd|Current:{Current,23551d6dacf2432c8a3edbc6bbc922cd}
    * }
    */
   private Map<String, Map<String, String>> getMetricDispalyNameAndFormularMapForServer(Asset server){
      Map<String, Map<String, String>> displayNameAndFormulasMap = new HashMap<String, Map<String, String>>();
      Map<String, String> formulas = server.getMetricsformulars();
      if(formulas == null || formulas.isEmpty()) {
         return displayNameAndFormulasMap;
      }
      Map<String, String> displayNameAndFormulaKey = new HashMap<String, String>();
      //Server Host Usage Formula
      Map<String, String> hostUsageformulas =
            getFormula(server, FlowgateConstant.HOST_METRICS, formulas, new TypeReference<Map<String, String>>(){});
      if (hostUsageformulas != null) {
         for(Map.Entry<String, String> formulaKeyAndFormula : hostUsageformulas.entrySet()) {
            Map<String, String> metricNameAndFormulaMap = new HashMap<>();
            String valueUnitName = getValueUnitName(formulaKeyAndValueUnitNameMapForServer, formulaKeyAndFormula.getKey());
            String displayName = getDisplayName(formulaKeyAndMetricFormatNameMapForServer, formulaKeyAndFormula.getKey());
            metricNameAndFormulaMap.put(valueUnitName, formulaKeyAndFormula.getValue());
            displayNameAndFormulasMap.put(displayName, metricNameAndFormulaMap);
            displayNameAndFormulaKey.put(displayName, formulaKeyAndFormula.getKey());
         }
      }
      //Server PDU formula
      TypeReference<Map<String, Map<String, String>>> typeReference =
            new TypeReference<Map<String, Map<String, String>>>() {};
      Map<String, Map<String, String>> pduFormulas =
            getFormula(server,FlowgateConstant.PDU, formulas, typeReference);
      if(pduFormulas != null) {
         Map<String,String> justficationfileds = server.getJustificationfields();
         String allPduPortInfo = justficationfileds.get(FlowgateConstant.PDU_PORT_FOR_SERVER);
         List<String> pduPorts = null;
         Map<String, List<String>> pduAssetIdAndUsedOutletMap = null;
         if (!StringUtils.isEmpty(allPduPortInfo)) {
            pduPorts = Arrays.asList(allPduPortInfo.split(FlowgateConstant.SPILIT_FLAG));
            pduAssetIdAndUsedOutletMap = new HashMap<String, List<String>>();
            for (String pduPortInfo : pduPorts) {
               // startport_FIELDSPLIT_endDeviceName_FIELDSPLIT_endport_FIELDSPLIT_endDeviceAssetID
               // item[0] start port
               // item[1] device name
               // item[2] end port
               // itme[3] assetid
               String items[] = pduPortInfo.split(FlowgateConstant.SEPARATOR);
               List<String> outlets = pduAssetIdAndUsedOutletMap.get(items[3]);
               if(outlets == null) {
                  outlets = new ArrayList<String>();
               }
               outlets.add(items[2]);
               pduAssetIdAndUsedOutletMap.put(items[3], outlets);
            }
         }
         for(Map.Entry<String, Map<String, String>> pduIdAndFormula : pduFormulas.entrySet()) {
            String pduId = pduIdAndFormula.getKey();
            Map<String, String> formulaKeyAndFormulaMap = pduIdAndFormula.getValue();
            List<String> outLets = pduAssetIdAndUsedOutletMap.get(pduId);
            for(Map.Entry<String, String> formulaEntry : formulaKeyAndFormulaMap.entrySet()) {
               Map<String, String> metricNameAndFormulaMap = new HashMap<>();
               String valueUnitName = getValueUnitName(formulaKeyAndValueUnitNameMapForServer, formulaEntry.getKey());
               String displayName = getDisplayName(formulaKeyAndMetricFormatNameMapForServer, formulaEntry.getKey());
               metricNameAndFormulaMap.put(valueUnitName, formulaEntry.getValue());
               switch (displayName) {
               case MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_CURRENT:
               case MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE:
               case MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_POWER:
                  if(outLets == null) {
                     continue;
                  }
                  for(String outLet : outLets) {
                     displayName = String.format(displayName, pduId, outLet);
                     displayNameAndFormulasMap.put(displayName, metricNameAndFormulaMap);
                  }
                  break;
               case MetricName.SERVER_VOLTAGE://displayName: Voltage
                  displayNameAndFormulasMap.put(displayName, metricNameAndFormulaMap);
                  break;
               default:
                  displayName = String.format(displayName, pduId);
                  displayNameAndFormulasMap.put(displayName, metricNameAndFormulaMap);
                  break;
               }
               displayNameAndFormulaKey.put(displayName, formulaEntry.getKey());
            }
         }
      }

      //Server Sensor Formula
      Map<String, Map<String, String>> sensorFormulaMap =
            getFormula(server, FlowgateConstant.SENSOR, formulas, typeReference);
      if (sensorFormulaMap != null) {
         for(Map.Entry<String, Map<String, String>> entry : sensorFormulaMap.entrySet()) {
            String valueUnitName = getValueUnitName(formulaKeyAndValueUnitNameMapForServer, entry.getKey());
            String displayName = getDisplayName(formulaKeyAndMetricFormatNameMapForServer, entry.getKey());
            Map<String, String> metricNameAndFormula = new HashMap<>();
            Map<String, String> locationAndFormulaMap = entry.getValue();
            for(Map.Entry<String, String> locationAndFormula : locationAndFormulaMap.entrySet()) {
               String location = locationAndFormula.getKey();
               String formula = locationAndFormula.getValue();
               location = location.replace(FlowgateConstant.SEPARATOR, FlowgateConstant.UNDERLINE);
               displayName = String.format(displayName, location);
               metricNameAndFormula.put(valueUnitName, formula);
            }
            displayNameAndFormulasMap.put(displayName, metricNameAndFormula);
            displayNameAndFormulaKey.put(displayName, entry.getKey());
         }
      }
      if(!displayNameAndFormulaKey.isEmpty()) {
         displayNameAndFormulasMap.put(DISPLAYNAMEANDFORMULANAMEMAPKEY, displayNameAndFormulaKey);
      }
      return displayNameAndFormulasMap;
   }

   private <T> T getFormula(Asset asset, String formulaType,
         Map<String, String> formulas, TypeReference<T> type){
      String formulasInfo = formulas.get(formulaType);
      if (StringUtils.isBlank(formulasInfo)) {
         return null;
      }
      return asset.metricsFormulaToMap(formulasInfo, type);
   }

   private String getDisplayName(Map<String, String> formulaKeyAndMetricFormatNameMap,String formulaKey) {
      String displayName = formulaKeyAndMetricFormatNameMapForServer.get(formulaKey);
      if(StringUtils.isEmpty(displayName)) {
         displayName = formulaKey;
      }
      return displayName;
   }

   private String getValueUnitName(Map<String, String> formulaKeyAndValueUnitNameMap, String formulaKey) {
      String valueUnitName = formulaKeyAndValueUnitNameMapForServer.get(formulaKey);
      if(StringUtils.isEmpty(valueUnitName)) {
         valueUnitName = formulaKey;
      }
      return valueUnitName;
   }
}
