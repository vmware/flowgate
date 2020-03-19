/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker.vro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.SensorSetting;
import com.vmware.ops.api.client.controllers.AlertDefinitionsClient;
import com.vmware.ops.api.client.controllers.RecommendationsClient;
import com.vmware.ops.api.client.controllers.SymptomDefinitionsClient;
import com.vmware.ops.api.model.alertdefinition.AlertDefinition;
import com.vmware.ops.api.model.alertdefinition.AlertDefinitionImpact;
import com.vmware.ops.api.model.alertdefinition.AlertDefinitionQuery;
import com.vmware.ops.api.model.alertdefinition.AlertDefinitionState;
import com.vmware.ops.api.model.alertdefinition.ImpactType;
import com.vmware.ops.api.model.alertdefinition.SymptomSet;
import com.vmware.ops.api.model.common.CompareOperator;
import com.vmware.ops.api.model.common.CompositeOperator;
import com.vmware.ops.api.model.common.Criticality;
import com.vmware.ops.api.model.common.types.RelationshipType;
import com.vmware.ops.api.model.recommendation.Recommendation;
import com.vmware.ops.api.model.recommendation.Recommendation.Recommendations;
import com.vmware.ops.api.model.recommendation.RecommendationQuery;
import com.vmware.ops.api.model.recommendation.RecommendedAction;
import com.vmware.ops.api.model.symptomdefinition.AggregationType;
import com.vmware.ops.api.model.symptomdefinition.HTCondition;
import com.vmware.ops.api.model.symptomdefinition.SymptomDefinition;
import com.vmware.ops.api.model.symptomdefinition.SymptomDefinition.SymptomDefinitions;
import com.vmware.ops.api.model.symptomdefinition.SymptomDefinitionQuery;
import com.vmware.ops.api.model.symptomdefinition.SymptomState;
import com.vmware.ops.api.model.symptomdefinition.ThresholdType;

public class AlertClient extends VROBase {

   private static final Set<String> PredefinedAlertNames =
         new HashSet<String>(Arrays.asList(VROConsts.ALERT_DEFINITION_HUMIDITY_NAME,
               VROConsts.ALERT_DEFINITION_TEMPERATURE_NAME,
               VROConsts.ALERT_DEFINITION_PDU_POWER_NAME));

   private static final Set<String> predefinedRecommendationNames = new HashSet<String>(
         Arrays.asList(VROConsts.RECOMMENDATION_MOVEVM_TO_OTHERHOSTS_DESCRIPTION,
               VROConsts.RECOMMENDATION_POWER_OFF_VM_DESCRIPTION));


   private static Set<String> predefinedSymptomNames =
         new HashSet<String>(Arrays.asList(VROConsts.SYMPTOM_HOSTSYSTEM_FRONT_TEMPERATURE_NAME,
               VROConsts.SYMPTOM_HOSTSYSTEM_BACK_TEMPERATURE_NAME,
               VROConsts.SYMPTOM_HOSTSYSTEM_FRONT_HUMIDITY_NAME,
               VROConsts.SYMPTOM_HOSTSYSTEM_BACK_HUMIDITY_NAME,
               VROConsts.SYMPTOM_HOSTSYSTEM_POWER_NAME));


   @Autowired
   private WormholeAPIClient restClient;

   public AlertClient(VROConfig config) {
      super(config);
   }

   private static final Logger logger = LoggerFactory.getLogger(AlertClient.class);

   @Override
   public void run() {

      //first check all the predefined recommendation, if not create recommendations

      Map<String, Recommendation> preDefinedRecommendations = checkPredefinedRecommendations();
      //Second check all the predefined symptoms. if not create symptoms
      // 1> front panel temp symptom
      // 2> backend panel temp symptom
      // 3> humidity symptom
      // 4> PDU power load symptom
      Map<String, SymptomDefinition> preDefinedSymptoms = checkPredefinedSymptoms();

      //Third check all the predefined alerts. if not exist create.

      checkPredefinedAlerts(preDefinedRecommendations, preDefinedSymptoms);
   }

   private Map<String, Recommendation> checkPredefinedRecommendations() {
      Map<String, Recommendation> existRecommendations = getPredefinedRecommendations();
      if (existRecommendations.size() < predefinedRecommendationNames.size()) {
         //create the missing recommendations.
         Set<String> missingRecommendations = new HashSet<String>(predefinedRecommendationNames);
         missingRecommendations.removeAll(existRecommendations.keySet());
         createPredefinedRecommendations(missingRecommendations, existRecommendations);
      }
      return existRecommendations;
   }

   private Map<String, SymptomDefinition> checkPredefinedSymptoms() {
      Map<String, SymptomDefinition> existSymptoms = getPredefinedSymptoms();
      if (existSymptoms.size() < predefinedSymptomNames.size()) {
         Set<String> missingSymptoms = new HashSet<String>(predefinedSymptomNames);
         missingSymptoms.removeAll(existSymptoms.keySet());
         createPredefinedSymptoms(missingSymptoms, existSymptoms);
      } else {
         //  check whether we need to update the threshold of the symptoms
         List<SensorSetting> sensorSettings = new ArrayList<SensorSetting>();
         SensorSetting backTempSetting = new SensorSetting();
         backTempSetting.setMaxNum(35);
         backTempSetting.setType(MetricName.SERVER_BACK_TEMPREATURE);
         sensorSettings.add(backTempSetting);
         SensorSetting frontTempSetting = new SensorSetting();
         frontTempSetting.setMaxNum(35);
         frontTempSetting.setType(MetricName.SERVER_FRONT_TEMPERATURE);
         sensorSettings.add(frontTempSetting);
         SensorSetting backHumiditySetting = new SensorSetting();
         backHumiditySetting.setMaxNum(75);
         backHumiditySetting.setType(MetricName.SERVER_BACK_HUMIDITY);
         sensorSettings.add(backHumiditySetting);
         SensorSetting frontHumiditySetting = new SensorSetting();
         frontHumiditySetting.setMaxNum(75);
         frontHumiditySetting.setType(MetricName.SERVER_FRONT_HUMIDITY);
         sensorSettings.add(frontHumiditySetting);
         SensorSetting currentLoadSetting = new SensorSetting();
         currentLoadSetting.setMaxNum(67);
         currentLoadSetting.setType(MetricName.PDU_CURRENT_LOAD);
         sensorSettings.add(currentLoadSetting);

         Map<String, SensorSetting> symptomCondtionValues =
               getSymptomCondtionValues(sensorSettings);
         SymptomDefinitionsClient sd = getClient().symptomDefinitionsClient();
         for (SymptomDefinition symptom : existSymptoms.values()) {
            HTCondition condition = (HTCondition) symptom.getState().getCondition();
            SensorSetting sensorSetting = symptomCondtionValues.get(symptom.getName());
            if (sensorSetting != null) {
               if (!condition.getValue().equals((String.valueOf(sensorSetting.getMaxNum())))) {
                  condition.setValue(String.valueOf(sensorSetting.getMaxNum()));
                  sd.updateSymptomDefinition(symptom);
               }
            }

         }
      }
      return existSymptoms;
   }

   private void createPredefinedRecommendations(Set<String> missingRecommendations,
         Map<String, Recommendation> existRecommendations) {
      for (String res : missingRecommendations) {
         //need to find a better way.
         switch (res) {
         case VROConsts.RECOMMENDATION_MOVEVM_TO_OTHERHOSTS_DESCRIPTION:
            existRecommendations.put(res, createMoveVMRecommendation());
            break;
         case VROConsts.RECOMMENDATION_POWER_OFF_VM_DESCRIPTION:
            existRecommendations.put(res, createPowerOffVMRecommendation());
            break;
         default:
            break;
         }
      }
   }

   private void createPredefinedSymptoms(Set<String> missingSymptoms,
         Map<String, SymptomDefinition> existSymptoms) {
      for (String sym : missingSymptoms) {
         switch (sym) {
         case VROConsts.SYMPTOM_HOSTSYSTEM_FRONT_TEMPERATURE_NAME: {
            SymptomDefinition sd = createEnvrionmentSymptom(sym, 2, 2, Criticality.CRITICAL,
                  VROConsts.ENVRIONMENT_FRONT_TEMPERATURE_METRIC, CompareOperator.GT,
                  VROConsts.ENVRIONMENT_TEMPERATURE_METRIC_MAX);
            existSymptoms.put(sym, sd);
         }
            break;
         case VROConsts.SYMPTOM_HOSTSYSTEM_BACK_TEMPERATURE_NAME: {
            SymptomDefinition sd = createEnvrionmentSymptom(sym, 2, 2, Criticality.CRITICAL,
                  VROConsts.ENVRIONMENT_BACK_TEMPERATURE_METRIC, CompareOperator.GT,
                  VROConsts.ENVRIONMENT_TEMPERATURE_METRIC_MAX);
            existSymptoms.put(sym, sd);
         }
            break;
         case VROConsts.SYMPTOM_HOSTSYSTEM_FRONT_HUMIDITY_NAME: {
            SymptomDefinition sd = createEnvrionmentSymptom(sym, 2, 2, Criticality.CRITICAL,
                  VROConsts.ENVRIONMENT_FRONT_HUMIDITY_METRIC, CompareOperator.GT,
                  VROConsts.ENVRIONMENT_HUMIDITY_METRIC_MAX);
            existSymptoms.put(sym, sd);
         }
            break;
         case VROConsts.SYMPTOM_HOSTSYSTEM_BACK_HUMIDITY_NAME: {
            SymptomDefinition sd = createEnvrionmentSymptom(sym, 2, 2, Criticality.CRITICAL,
                  VROConsts.ENVRIONMENT_BACK_HUMIDITY_METRIC, CompareOperator.GT,
                  VROConsts.ENVRIONMENT_HUMIDITY_METRIC_MAX);
            existSymptoms.put(sym, sd);
         }
            break;
         case VROConsts.SYMPTOM_HOSTSYSTEM_POWER_NAME: {
            SymptomDefinition sd = createEnvrionmentSymptom(sym, 2, 2, Criticality.CRITICAL,
                  VROConsts.ENVRIONMENT_PDU_AMPS_LOAD_METRIC, CompareOperator.GT,
                  VROConsts.ENVRIONMENT_PDU_AMPS_METRIC_MAX);
            existSymptoms.put(sym, sd);
         }
            break;
         default:
            break;
         }
      }
   }

   private Map<String, AlertDefinition> checkPredefinedAlerts(
         Map<String, Recommendation> preDefinedRecommendations,
         Map<String, SymptomDefinition> preDefinedSymptoms) {
      Map<String, AlertDefinition> existAlerts = getPredefinedAlerts();
      if (existAlerts.size() < PredefinedAlertNames.size()) {
         Set<String> missingAlerts = new HashSet<String>(PredefinedAlertNames);
         missingAlerts.removeAll(existAlerts.keySet());
         createPredefinedAlerts(missingAlerts, existAlerts, preDefinedRecommendations,
               preDefinedSymptoms);
      }
      return existAlerts;
   }

   private void createPredefinedAlerts(Set<String> missingAlerts,
         Map<String, AlertDefinition> existAlerts,
         Map<String, Recommendation> preDefinedRecommendations,
         Map<String, SymptomDefinition> preDefinedSymptoms) {
      for (String alertName : missingAlerts) {
         switch (alertName) {
         case VROConsts.ALERT_DEFINITION_TEMPERATURE_NAME: {
            Map<String, Integer> recommendationIds = new HashMap<String, Integer>();
            int priority = 1;
            recommendationIds.put(
                  preDefinedRecommendations
                        .get(VROConsts.RECOMMENDATION_MOVEVM_TO_OTHERHOSTS_DESCRIPTION).getId(),
                  priority);
            Set<String> symptomIds = new HashSet<String>();
            symptomIds.add(preDefinedSymptoms
                  .get(VROConsts.SYMPTOM_HOSTSYSTEM_FRONT_TEMPERATURE_NAME).getId());
            symptomIds.add(preDefinedSymptoms
                  .get(VROConsts.SYMPTOM_HOSTSYSTEM_BACK_TEMPERATURE_NAME).getId());
            existAlerts.put(alertName,
                  createAlertDefinition(alertName,
                        VROConsts.ALERT_DEFINITION_TEMPERATURE_DESCRIPTION, symptomIds,
                        recommendationIds));
         }
            break;
         case VROConsts.ALERT_DEFINITION_HUMIDITY_NAME: {
            Map<String, Integer> recommendationIds = new HashMap<String, Integer>();
            int priority = 1;
            recommendationIds.put(
                  preDefinedRecommendations
                        .get(VROConsts.RECOMMENDATION_MOVEVM_TO_OTHERHOSTS_DESCRIPTION).getId(),
                  priority);
            Set<String> symptomIds = new HashSet<String>();
            symptomIds
                  .add(preDefinedSymptoms.get(VROConsts.SYMPTOM_HOSTSYSTEM_FRONT_HUMIDITY_NAME).getId());
            symptomIds
            .add(preDefinedSymptoms.get(VROConsts.SYMPTOM_HOSTSYSTEM_BACK_HUMIDITY_NAME).getId());
            existAlerts.put(alertName, createAlertDefinition(alertName,
                  VROConsts.ALERT_DEFINITION_HUMIDITY_DESCRIPTION, symptomIds, recommendationIds));
         }
            break;
         case VROConsts.ALERT_DEFINITION_PDU_POWER_NAME: {
            Map<String, Integer> recommendationIds = new HashMap<String, Integer>();
            int priority = 1;
            recommendationIds.put(preDefinedRecommendations
                  .get(VROConsts.RECOMMENDATION_POWER_OFF_VM_DESCRIPTION).getId(), priority);
            Set<String> symptomIds = new HashSet<String>();
            symptomIds.add(preDefinedSymptoms.get(VROConsts.SYMPTOM_HOSTSYSTEM_POWER_NAME).getId());
            existAlerts.put(alertName, createAlertDefinition(alertName,
                  VROConsts.ALERT_DEFINITION_PDU_POWER_DESCRIPTION, symptomIds, recommendationIds));
         }
            break;
         default:
            break;
         }
      }
   }

   private Map<String, AlertDefinition> getPredefinedAlerts() {
      AlertDefinitionsClient alertClient = getClient().alertDefinitionsClient();
      Map<String, AlertDefinition> existingAlerts = new HashMap<String, AlertDefinition>();
      AlertDefinitionQuery adq = new AlertDefinitionQuery();
      adq.setAdapterKind(VROConsts.ADPTERKIND_VMARE_KEY);
      adq.setResourceKind(VROConsts.RESOURCEKIND_HOSTSYSTEM_KEY);
      for (AlertDefinition ad : alertClient.queryAlertDefinitions(adq, null)
            .getAlertDefinitions()) {
         if (PredefinedAlertNames.contains(ad.getName())) {
            existingAlerts.put(ad.getName(), ad);
         }

      }
      return existingAlerts;
   }

   // Symptom name and value;
   private Map<String, SensorSetting> getSymptomCondtionValues(List<SensorSetting> sensorSettings) {
      Map<String, SensorSetting> result = new HashMap<String, SensorSetting>();
      for (SensorSetting setting : sensorSettings) {
         switch (setting.getType()) {
         case MetricName.SERVER_FRONT_TEMPERATURE:
            result.put(VROConsts.SYMPTOM_HOSTSYSTEM_FRONT_TEMPERATURE_NAME, setting);
            break;
         case MetricName.SERVER_BACK_TEMPREATURE:
            result.put(VROConsts.SYMPTOM_HOSTSYSTEM_BACK_TEMPERATURE_NAME, setting);
            break;
         case MetricName.PDU_CURRENT_LOAD:
            result.put(VROConsts.SYMPTOM_HOSTSYSTEM_POWER_NAME, setting);
            break;
         case MetricName.SERVER_FRONT_HUMIDITY:
            result.put(VROConsts.SYMPTOM_HOSTSYSTEM_FRONT_HUMIDITY_NAME, setting);
            break;
         case MetricName.SERVER_BACK_HUMIDITY:
            result.put(VROConsts.SYMPTOM_HOSTSYSTEM_BACK_HUMIDITY_NAME, setting);
            break;
         default:
            break;
         }
      }
      return result;
   }

   public void deleteAlertDefinition(String alertID) {
      try {
         AlertDefinitionsClient client = getClient().alertDefinitionsClient();
         client.deleteAlertDefinition(alertID);
      } catch (Exception e) {
         // do nothing;
         logger.error("Failed to delete the Alert, please try again later.", e);
      }
   }


   private AlertDefinition createAlertDefinition(String name, String description,
         Set<String> symptomDefinitionIds, Map<String, Integer> recommendationPriorityMap) {
      AlertDefinitionsClient client = getClient().alertDefinitionsClient();
      AlertDefinition alertDefinition = new AlertDefinition();
      alertDefinition.setName(name);
      alertDefinition.setDescription(description);
      alertDefinition.setAdapterKindKey(VROConsts.ADPTERKIND_VMARE_KEY);
      alertDefinition.setResourceKindKey(VROConsts.RESOURCEKIND_HOSTSYSTEM_KEY);
      alertDefinition.setWaitCycles(2);
      alertDefinition.setCancelCycles(3);
      alertDefinition.setType(17);
      alertDefinition.setSubType(18);
      List<AlertDefinitionState> alertDefinitionStateList = new ArrayList<AlertDefinitionState>();
      AlertDefinitionState state1 = new AlertDefinitionState();
      AlertDefinitionImpact definitionImpact = new AlertDefinitionImpact();
      definitionImpact.setDetail("health");
      definitionImpact.setImpactType(ImpactType.BADGE);
      state1.setSeverity(Criticality.CRITICAL);
      state1.setImpact(definitionImpact);

      SymptomSet symptomSet1 = new SymptomSet();
      symptomSet1.setSymptomDefinitionReferences(symptomDefinitionIds);
      symptomSet1.setRelation(RelationshipType.SELF);
      symptomSet1.setAggregation(AggregationType.ALL);
      symptomSet1.setSymptomSetOperator(CompositeOperator.OR);
      state1.setSymptoms(symptomSet1);
      state1.setRecommendationPriorityMap(recommendationPriorityMap);
      alertDefinitionStateList.add(state1);
      alertDefinition.setAlertDefinitionStates(alertDefinitionStateList);
      return client.createAlertDefinition(alertDefinition);
   }

   protected AlertDefinition getAlertDefinition(String id) {
      AlertDefinitionsClient alertClient = getClient().alertDefinitionsClient();
      AlertDefinition alertDefinition = alertClient.getAlertDefinitionById(id);
      return alertDefinition;
   }

   public AlertDefinition getPredefinedAlert(String key) {
      AlertDefinitionsClient alertClient = getClient().alertDefinitionsClient();
      AlertDefinitionQuery adq = new AlertDefinitionQuery();
      adq.setAdapterKind(VROConsts.ADPTERKIND_VMARE_KEY);
      adq.setResourceKind(VROConsts.RESOURCEKIND_HOSTSYSTEM_KEY);
      for (AlertDefinition ad : alertClient.queryAlertDefinitions(adq, null)
            .getAlertDefinitions()) {
         if (ad.getName().equals(key)) {
            return ad;
         }
      }
      return null;
   }

   protected SymptomDefinition createEnvrionmentSymptom(String name, int waitCycles,
         int cancleCycles, Criticality serverity, String conditionKey,
         CompareOperator conditionOperator, String value) {
      SymptomDefinitionsClient sd = getClient().symptomDefinitionsClient();
      SymptomDefinition aSymptomDefinition = new SymptomDefinition();
      aSymptomDefinition.setName(name);
      aSymptomDefinition.setAdapterKindKey(VROConsts.ADPTERKIND_VMARE_KEY);
      aSymptomDefinition.setResourceKindKey(VROConsts.RESOURCEKIND_HOSTSYSTEM_KEY);
      aSymptomDefinition.setWaitCycles(waitCycles);
      aSymptomDefinition.setCancelCycles(cancleCycles);
      SymptomState symptomState = new SymptomState();
      symptomState.setSeverity(serverity);
      HTCondition condition = new HTCondition();
      condition.setKey(conditionKey);
      condition.setOperator(conditionOperator);
      // must set threshold type to STATKEY and set the TargetKey
      condition.setThresholdType(ThresholdType.STATIC);
      //condition.setTargetKey("cpu|availablemhz");
      condition.setValue(value);
      //        condition.setValueType(Condition.ValueTypeEnum.NUMERIC); // necessary only when value is a string
      condition.setInstanced(false);
      symptomState.setCondition(condition);
      aSymptomDefinition.setState(symptomState);
      aSymptomDefinition = sd.createSymptomDefinition(aSymptomDefinition);

      SymptomDefinitionQuery sdq = new SymptomDefinitionQuery();
      sdq.setAdapterKind(VROConsts.ADPTERKIND_VMARE_KEY);
      sdq.setResourceKind(VROConsts.RESOURCEKIND_HOSTSYSTEM_KEY);
      sdq.setId(new String[] { aSymptomDefinition.getId() });

      SymptomDefinitions allSymptoms = sd.querySymptomDefinitions(sdq, null);
      return allSymptoms.getSymptomDefinitions().get(0);

   }

   protected SymptomDefinitions getSysmptomDefinitions(String[] ids) {
      SymptomDefinitionsClient sd = getClient().symptomDefinitionsClient();
      SymptomDefinitionQuery sdq = new SymptomDefinitionQuery();
      sdq.setId(ids);
      return sd.querySymptomDefinitions(sdq, null);
   }

   protected Map<String, SymptomDefinition> getPredefinedSymptoms() {
      SymptomDefinitionsClient sdc = getClient().symptomDefinitionsClient();
      SymptomDefinitionQuery sdq = new SymptomDefinitionQuery();
      sdq.setAdapterKind(VROConsts.ADPTERKIND_VMARE_KEY);
      sdq.setResourceKind(VROConsts.RESOURCEKIND_HOSTSYSTEM_KEY);
      Map<String, SymptomDefinition> result = new HashMap<String, SymptomDefinition>();
      for (SymptomDefinition sd : sdc.querySymptomDefinitions(sdq, null).getSymptomDefinitions()) {
         if (predefinedSymptomNames.contains(sd.getName())) {
            result.put(sd.getName(), sd);
         }
      }
      return result;
   }

   /**
    * since Move VM action don't support HostSystem Object type, so we will not create Action for
    * MoveVM recommendation. But Since we use adpater kind to search the recommendation. So
    * we have to give a action to the Move VM. Should be fixed later.
    *
    * @return
    */
   protected Recommendation createMoveVMRecommendation() {
      return createRecommendation(VROConsts.RECOMMENDATION_MOVEVM_TO_OTHERHOSTS_DESCRIPTION, "Power Off VM");
   }

   protected Recommendation createPowerOffVMRecommendation() {
      return createRecommendation(VROConsts.RECOMMENDATION_POWER_OFF_VM_DESCRIPTION,
            "Power Off VM");
   }

   private Recommendation createRecommendation(String description, String method) {
      RecommendationsClient rc = getClient().recommendationsClient();
      Recommendation reco = new Recommendation();
      reco.setDescription(description);
      if (method != null) {
         RecommendedAction action = new RecommendedAction();
         action.setActionAdapterTypeId(VROConsts.RECOMMENDATION_ACTION_ADAPTER_VCENTER);
         action.setTargetAdapterTypeId(VROConsts.ADPTERKIND_VMARE_KEY);
         action.setTargetResourceTypeId(VROConsts.RESOURCEKIND_HOSTSYSTEM_KEY);
         action.setTargetMethod(method);
         reco.setAction(action);
      }
      try {
         reco = rc.createRecommendation(reco);
      } catch (Exception e) {
         throw e;
      }
      return reco;
   }


   protected Recommendations getRecommendations(String[] ids) {
      RecommendationsClient recoClient = getClient().recommendationsClient();
      RecommendationQuery rq = new RecommendationQuery();
      rq.setId(ids);
      return recoClient.getRecommendations(rq);
   }

   protected Map<String, Recommendation> getPredefinedRecommendations() {
      RecommendationsClient recoClient = getClient().recommendationsClient();
      RecommendationQuery rq = new RecommendationQuery();
      rq.setTargetAdapterKind(VROConsts.ADPTERKIND_VMARE_KEY);
      rq.setTargetResourceKind(VROConsts.RESOURCEKIND_HOSTSYSTEM_KEY);
      Map<String, Recommendation> result = new HashMap<String, Recommendation>();
      List<Recommendation> recommendations = recoClient.getRecommendations(rq).getRecommendations();
      for (Recommendation r : recommendations) {
         if (predefinedRecommendationNames.contains(r.getDescription())) {
            result.put(r.getDescription(), r);
         }
      }
      return result;
   }


   public WormholeAPIClient getRestClient() {
      return restClient;
   }

   public void setRestClient(WormholeAPIClient restClient) {
      this.restClient = restClient;
   }


}
