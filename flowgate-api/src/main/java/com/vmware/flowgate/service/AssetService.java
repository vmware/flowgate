package com.vmware.flowgate.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricKeyName;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.MetricData;
import com.vmware.flowgate.common.model.RealTimeData;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.repository.AssetRealtimeDataRepository;
import com.vmware.flowgate.repository.AssetRepository;

@Component
public class AssetService {

   @Autowired
   AssetRepository assetRepository;
   @Autowired
   AssetRealtimeDataRepository realtimeDataRepository;
   private static final int TEN_MINUTES = 605000;//add extra 5 seconds;
   private static Map<String,String> metricNameMap = new HashMap<String,String>();
   static {
      metricNameMap.put(MetricName.PDU_HUMIDITY, MetricName.HUMIDITY);
      metricNameMap.put(MetricName.PDU_TEMPERATURE, MetricName.TEMPERATURE);
      metricNameMap.put(MetricName.SERVER_BACK_HUMIDITY, MetricName.HUMIDITY);
      metricNameMap.put(MetricName.SERVER_FRONT_HUMIDITY, MetricName.HUMIDITY);
      metricNameMap.put(MetricName.SERVER_BACK_TEMPREATURE, MetricName.TEMPERATURE);
      metricNameMap.put(MetricName.SERVER_FRONT_TEMPERATURE, MetricName.TEMPERATURE);
      metricNameMap = Collections.unmodifiableMap(metricNameMap);
   }

   public List<MetricData> getPduMetricsDataById(String assetID){
      long starttime = System.currentTimeMillis() - TEN_MINUTES;
      List<RealTimeData> pduMetricsRealtimeDatas =
            realtimeDataRepository.getDataByIDAndTimeRange(assetID, starttime, TEN_MINUTES);
      List<ValueUnit> valueunits = new ArrayList<>();

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

      //pdu metrics data,such as power/current/voltage
      valueunits.addAll(getValueUnits(pduMetricsRealtimeDatas, metricNames));

      Asset pdu = assetRepository.findOne(assetID);
      //sensor metrics data, such as temperature or humidity
      Map<String, Map<String, Map<String, String>>> formulars = pdu.getMetricsformulars();
      Map<String, Map<String, String>> sensorFormulars = null;
      if(formulars != null && !formulars.isEmpty()) {
         sensorFormulars = formulars.get(FlowgateConstant.SENSOR);
      }

      if(sensorFormulars != null) {
         Map<String,List<RealTimeData>> assetIdAndRealtimeDataMap = new HashMap<String,List<RealTimeData>>();
         Map<String,String> humidityLocationAndIdMap = sensorFormulars.get(MetricName.PDU_HUMIDITY);
         if (humidityLocationAndIdMap != null && !humidityLocationAndIdMap.isEmpty()) {
            valueunits.addAll(generateSensorValueUnit(assetIdAndRealtimeDataMap, starttime,
                  humidityLocationAndIdMap, MetricName.PDU_HUMIDITY));
         }
         Map<String,String> temperatureLocationAndIdMap = sensorFormulars.get(MetricName.PDU_TEMPERATURE);
         if(temperatureLocationAndIdMap != null && !temperatureLocationAndIdMap.isEmpty()) {
            valueunits.addAll(generateSensorValueUnit(assetIdAndRealtimeDataMap, starttime,
                  temperatureLocationAndIdMap, MetricName.PDU_TEMPERATURE));
         }
      }
      return generateMetricsDataForPDU(valueunits);
   }

   public List<MetricData> getServerMetricsDataById(String assetID, long starttime, int duration){
      Asset server = assetRepository.findOne(assetID);
      List<MetricData> result = new ArrayList<MetricData>();
      Map<String, Map<String, Map<String, String>>> metricFormula = server.getMetricsformulars();
      if(metricFormula == null || metricFormula.isEmpty()) {
         return result;
      }
      Map<String, Map<String, String>> pduMetrics = metricFormula.get(FlowgateConstant.PDU);
      if(pduMetrics != null && !pduMetrics.isEmpty()) {
         List<String> metricNames = new ArrayList<String>();
         metricNames.add(MetricName.PDU_TOTAL_POWER);
         metricNames.add(MetricName.PDU_TOTAL_CURRENT);
         metricNames.add(MetricName.PDU_APPARENT_POWER);
         metricNames.add(MetricName.PDU_CURRENT);
         metricNames.add(MetricName.PDU_VOLTAGE);
         for(String pduId : pduMetrics.keySet()) {
            List<RealTimeData> realtimedatas =
                  realtimeDataRepository.getDataByIDAndTimeRange(pduId, starttime, duration);
            List<ValueUnit> valueUnits = getValueUnits(realtimedatas, metricNames);
            result.addAll(generateServerPduMetricData(valueUnits, pduId));
         }
      }

      Map<String, Map<String, String>> sensorFormulars = metricFormula.get(FlowgateConstant.SENSOR);
      if (sensorFormulars != null) {
         Map<String, List<RealTimeData>> assetIdAndRealtimeDataMap =
               new HashMap<String, List<RealTimeData>>();
         for (Map.Entry<String, Map<String, String>> sensorFormula : sensorFormulars.entrySet()) {
            Map<String, String> locationAndIdMap = sensorFormula.getValue();
            String metricName = sensorFormula.getKey();
            List<ValueUnit> valueUnits = generateSensorValueUnit(assetIdAndRealtimeDataMap,
                  starttime, locationAndIdMap, metricName);
            result.addAll(generateServerSensorMetricData(valueUnits, metricName));
         }
      }
      return result;
   }

   private List<MetricData> generateServerPduMetricData(List<ValueUnit> valueUnits, String pduAssetId){
      List<MetricData> result = new ArrayList<MetricData>();
      for(ValueUnit value : valueUnits) {
         MetricData data = new MetricData();
         data.setTimeStamp(value.getTime());
         data.setValueNum(value.getValueNum());
         switch (value.getKey()) {
         case MetricName.PDU_TOTAL_POWER:
            data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_POWER, pduAssetId));
            result.add(data);
            break;
         case MetricName.PDU_TOTAL_CURRENT:
            data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_TOTAL_CURRENT, pduAssetId));
            result.add(data);
            break;
         case MetricName.PDU_APPARENT_POWER:
            data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_POWER, pduAssetId, value.getExtraidentifier()));
            result.add(data);
            break;
         case MetricName.PDU_CURRENT:
            data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_CURRENT, pduAssetId, value.getExtraidentifier()));
            result.add(data);
            break;
         case MetricName.PDU_VOLTAGE:
            data.setMetricName(String.format(MetricKeyName.SERVER_CONNECTED_PDUX_OUTLETX_VOLTAGE, pduAssetId, value.getExtraidentifier()));
            result.add(data);
            break;
         default:
            break;
         }
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
      List<ValueUnit> valueunits = null;
      if(realtimeDatas == null || realtimeDatas.isEmpty()) {
         return valueunits;
      }
      valueunits = new ArrayList<>();
      RealTimeData realTimeData = findLatestData(realtimeDatas);
      for(ValueUnit value : realTimeData.getValues()) {
         if(metricsName.contains(value.getKey())) {
            valueunits.add(value);
         }
      }
      return valueunits;
   }

   private List<ValueUnit> generateSensorValueUnit(Map<String,List<RealTimeData>> assetIdAndRealtimeDataMap,
         long starttime, Map<String,String> locationAndIdMap, String metricName){
      List<ValueUnit> valueunits = null;
      for(Map.Entry<String, String> locationInfoAndId : locationAndIdMap.entrySet()) {
         String formula = locationInfoAndId.getValue();
         String location = locationInfoAndId.getKey();
         String ids[] = formula.split("\\+|-|\\*|/|\\(|\\)");
         for(String assetId : ids) {
            List<RealTimeData> realtimeDatas = null;
            if(!assetIdAndRealtimeDataMap.containsKey(assetId)) {
               realtimeDatas =
                     realtimeDataRepository.getDataByIDAndTimeRange(assetId, starttime, TEN_MINUTES);
               assetIdAndRealtimeDataMap.put(assetId, realtimeDatas);
            }
            realtimeDatas = assetIdAndRealtimeDataMap.get(assetId);
            if(realtimeDatas == null || realtimeDatas.isEmpty()) {
               continue;
            }
            valueunits = new ArrayList<>();
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
         switch (valueunit.getKey()) {
         case MetricName.PDU_ACTIVE_POWER:
            //PDU|INLET:1|ActivePower
            metricData.setMetricName(String.format(MetricKeyName.PDU_XLET_ACTIVE_POWER,
                  valueunit.getExtraidentifier()));
            result.add(metricData);
            break;
         case MetricName.PDU_APPARENT_POWER:
            metricData.setMetricName(String.format(MetricKeyName.PDU_XLET_APPARENT_POWER,
                  valueunit.getExtraidentifier()));
            result.add(metricData);
            break;
         case MetricName.PDU_CURRENT:
            metricData.setMetricName(
                  String.format(MetricKeyName.PDU_XLET_CURRENT, valueunit.getExtraidentifier()));
            result.add(metricData);
            break;
         case MetricName.PDU_VOLTAGE:
            metricData.setMetricName(
                  String.format(MetricKeyName.PDU_XLET_VOLTAGE, valueunit.getExtraidentifier()));
            result.add(metricData);
            break;
         case MetricName.PDU_FREE_CAPACITY:
            metricData.setMetricName(String.format(MetricKeyName.PDU_XLET_FREE_CAPACITY,
                  valueunit.getExtraidentifier()));
            result.add(metricData);
            break;
         case MetricName.PDU_CURRENT_LOAD:
            metricData.setMetricName(MetricName.PDU_CURRENT_LOAD);
            result.add(metricData);
            break;
         case MetricName.PDU_POWER_LOAD:
            metricData.setMetricName(MetricName.PDU_POWER_LOAD);
            result.add(metricData);
            break;
         case MetricName.PDU_TOTAL_CURRENT:
            metricData.setMetricName(MetricName.PDU_TOTAL_CURRENT);
            result.add(metricData);
            break;
         case MetricName.PDU_TOTAL_POWER:
            metricData.setMetricName(MetricName.PDU_TOTAL_POWER);
            result.add(metricData);
            break;
         case MetricName.PDU_HUMIDITY:
            metricData.setMetricName(String.format(MetricKeyName.PDU_HUMIDITY_LOCATIONX,
                  valueunit.getExtraidentifier()));
            result.add(metricData);
            break;
         case MetricName.PDU_TEMPERATURE:
            metricData.setMetricName(String.format(MetricKeyName.PDU_TEMPERATURE_LOCATIONX,
                  valueunit.getExtraidentifier()));
            result.add(metricData);
            break;
         default:
            break;
         }
      }
      return result;
   }
}
