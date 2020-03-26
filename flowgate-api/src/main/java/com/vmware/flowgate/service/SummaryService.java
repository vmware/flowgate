package com.vmware.flowgate.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.NlyteSummary;
import com.vmware.flowgate.common.model.PowerIqSummary;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.SystemSummary;
import com.vmware.flowgate.common.model.VcSummary;
import com.vmware.flowgate.common.model.VroSummary;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.repository.SDDCSoftwareRepository;
import com.vmware.flowgate.repository.SystemSummaryRepository;

@Component
public class SummaryService {

   @Autowired
   CouchbaseTemplate couchbaseTemplate;

   @Autowired
   SDDCSoftwareRepository sddcrpeo;

   @Autowired
   FacilitySoftwareConfigRepository facilityRepo;

   @Autowired
   SystemSummaryRepository summaryRepository;

   @Autowired
   StringRedisTemplate redisTemplate;

   public static final String SUMMARY_DATA = "summarydata";

   public SystemSummary getSystemResult(boolean useCache) throws IOException {
      SystemSummary data = new SystemSummary();
      ObjectMapper mapper = new ObjectMapper();
      if(useCache) {
         if(!redisTemplate.hasKey(SUMMARY_DATA)) {
            data = generateSummaryData();
            redisTemplate.opsForValue().set(SUMMARY_DATA, mapper.writeValueAsString(data));
            return data;
         }
         String value = redisTemplate.opsForValue().get(SUMMARY_DATA);
         data = mapper.readValue(value, SystemSummary.class);
         return data;
      }
      data = generateSummaryData();
      redisTemplate.opsForValue().set(SUMMARY_DATA, mapper.writeValueAsString(data));
     return data;
   }

   public SystemSummary generateSummaryData() {
      SystemSummary data = new SystemSummary();
      data.setAssetsNum(summaryRepository.countByClass("com.vmware.flowgate.common.model.Asset"));
      data.setFacilitySystemNum(summaryRepository.countByClass("com.vmware.flowgate.common.model.FacilitySoftwareConfig"));
      data.setUserNum(summaryRepository.countByClass("com.vmware.flowgate.common.model.WormholeUser"));
      data.setSddcServerNum(summaryRepository.countByClass("com.vmware.flowgate.common.model.ServerMapping"));
      data.setSddcIntegrationNum(summaryRepository.countByClass("com.vmware.flowgate.common.model.SDDCSoftwareConfig"));
      data.setVcNum(summaryRepository.countByClassAndType("com.vmware.flowgate.common.model.SDDCSoftwareConfig", "VCENTER"));
      data.setVroNum(summaryRepository.countByClassAndType("com.vmware.flowgate.common.model.SDDCSoftwareConfig", "VRO"));

      data = getAssetNumGroupByCategory(data);
      data = getSensorNumGroupBySubCategory(data);
      data.setNlyteSummary(getNlyteSummaryList());
      data.setPowerIqSummary(getPowerIQSummaryList());
      data.setVcSummary(getVcSummaryList());
      data.setVroSummary(getVroSummaryList());
      return data;
   }
   public List<VcSummary> getVcSummaryList() {
      List<VcSummary> vcSummary = new ArrayList<>();
      List<SDDCSoftwareConfig> sddcSoftwareConfigs = sddcrpeo.findAllByType(SoftwareType.VCENTER.name());
      for (SDDCSoftwareConfig s : sddcSoftwareConfigs) {
         VcSummary vc = new VcSummary();
         vc.setName(s.getName());
         vc.setUrl(s.getServerURL());
         vc.setHostsNum(summaryRepository.countServerMappingByVC(s.getId()));
         vcSummary.add(vc);
      }
      return vcSummary;
   }

   public List<VroSummary> getVroSummaryList() {
      List<VroSummary> vroSummary = new ArrayList<>();
      List<SDDCSoftwareConfig> sddcSoftwareConfigs = sddcrpeo.findAllByType(SoftwareType.VRO.name());
      for (SDDCSoftwareConfig s : sddcSoftwareConfigs) {
         VroSummary vro = new VroSummary();
         vro.setName(s.getName());
         vro.setUrl(s.getServerURL());
         vro.setHostsNum(summaryRepository.countServerMappingByVRO(s.getId()));
         vroSummary.add(vro);
      }
      return vroSummary;
   }

   public List<PowerIqSummary> getPowerIQSummaryList() {
      List<PowerIqSummary> powerIqSummarys = new ArrayList<PowerIqSummary>();
      List<FacilitySoftwareConfig> facility = facilityRepo.findAllByType(com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType.PowerIQ.name());
      for (FacilitySoftwareConfig powerIq : facility) {
         PowerIqSummary powerIQSummary = getPowerIQSummary(powerIq);
         powerIqSummarys.add(powerIQSummary);
      }
      return powerIqSummarys;
   }

   public PowerIqSummary getPowerIQSummary(FacilitySoftwareConfig powerIQ) {
      PowerIqSummary powerIQSummary = new PowerIqSummary();
      powerIQSummary.setName(powerIQ.getName());
      powerIQSummary.setUrl(powerIQ.getServerURL());
      List<HashMap<String,Object>> results = summaryRepository.countAssetGroupByTypeAndSource("%"+powerIQ.getId()+"%");
      if(!results.isEmpty()) {
         for(HashMap<String,Object> map :results) {
            if((String)map.get("category") == null) {
               continue;
            }
            switch ((String)map.get("category")) {
            case "PDU":
               powerIQSummary.setPduNum((Integer)map.get("count"));
               break;
            case "Sensors":
               powerIQSummary.setSensorNum((Integer)map.get("count"));
               break;
            default:
               break;
            }
         }
      }
      return powerIQSummary;
   }

   public List<NlyteSummary> getNlyteSummaryList() {
      List<NlyteSummary> nlyteSummarys = new ArrayList<>();
      List<FacilitySoftwareConfig> facility = facilityRepo.findAllByType(com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType.Nlyte.name());
      for (FacilitySoftwareConfig nlyte : facility) {
         NlyteSummary nlyteSummary = getNlyteSummary(nlyte);
         nlyteSummarys.add(nlyteSummary);
      }
      return nlyteSummarys;
   }

   public NlyteSummary getNlyteSummary(FacilitySoftwareConfig nlyte) {
      NlyteSummary nlyteSummary = new NlyteSummary();
      nlyteSummary.setName(nlyte.getName());
      nlyteSummary.setUrl(nlyte.getServerURL());
      List<HashMap<String,Object>> results = summaryRepository.countAssetGroupByTypeAndSource("%"+nlyte.getId()+"%");
      if(!results.isEmpty()) {
         for(HashMap<String,Object> map :results) {
            if((String)map.get("category") == null) {
               continue;
            }
            switch ((String)map.get("category")) {
            case "Server":
               nlyteSummary.setServerNum((Integer)map.get("count"));
               break;
            case "PDU":
               nlyteSummary.setPduNum((Integer)map.get("count"));
               break;
            case "Sensors":
               nlyteSummary.setSensorNum((Integer)map.get("count"));
               break;
            case "Cabinet":
               nlyteSummary.setCabinetNum((Integer)map.get("count"));
               break;
            case "Networks":
               nlyteSummary.setSwitchNum((Integer)map.get("count"));
               break;
            default:
               break;
            }
         }
      }
      return nlyteSummary;
   }

   public SystemSummary getAssetNumGroupByCategory(SystemSummary data) {
      List<HashMap<String,Object>> results = summaryRepository.countAssetGroupByType();
      if(!results.isEmpty()) {
         for(HashMap<String,Object> map :results) {
            if((String)map.get("category") == null) {
               continue;
            }
            switch ((String)map.get("category")) {
            case "Server":
               data.setServerNum((Integer)map.get("count"));
               break;
            case "PDU":
               data.setPduNum((Integer)map.get("count"));
               break;
            case "Sensors":
               data.setSensorNum((Integer)map.get("count"));
               break;
            case "Cabinet":
               data.setCabinetNum((Integer)map.get("count"));
               break;
            case "Networks":
               data.setSwitchNum((Integer)map.get("count"));
               break;
            default:
               break;
            }
         }
      }
      return data;
   }

   public SystemSummary getSensorNumGroupBySubCategory(SystemSummary data) {
      List<HashMap<String,Object>> results = summaryRepository.countSensorGroupByType();
      if(!results.isEmpty()) {
         for(HashMap<String,Object> map :results) {
            if(map.get("subCategory") == null) {
               continue;
            }
            switch ((String)map.get("subCategory")) {
            case "Humidity":
               data.setHumiditySensorNum((Integer)map.get("count"));
               break;
            case "Temperature":
               data.setTemperatureSensorNum((Integer)map.get("count"));
               break;
            case "AirFlow":
               data.setAirFlowSensorNum((Integer)map.get("count"));
               break;
            case "Smoke":
               data.setSmokeSensorNum((Integer)map.get("count"));
               break;
            case "Water":
               data.setWaterSensorNum((Integer)map.get("count"));
               break;
            default:
               break;
            }
         }
      }
      return data;
   }
}
