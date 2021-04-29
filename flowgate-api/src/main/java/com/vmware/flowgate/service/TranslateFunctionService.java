/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.flowgate.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.MetricData;
import com.vmware.flowgate.common.model.TranslateContext;
import com.vmware.flowgate.common.model.ValueUnit;

public class TranslateFunctionService {

   private static final Logger logger = LoggerFactory.getLogger(TranslateFunctionService.class);
   private static final JexlEngine jexl = new JexlBuilder().create();

   public static Map<String, Function<TranslateContext, MetricData>> serverFormulaKeyAndFunction = new HashMap<>();
   public static Map<String, Function<TranslateContext, MetricData>> pduFormulaKeyAndFunction = new HashMap<>();
   public static Map<String, Function<TranslateContext, MetricData>> defaultFormulaKeyAndFunction = new HashMap<>();
   private static final Function<TranslateContext, MetricData> basisConvert = translateContext -> {
      Map<String, ValueUnit> valueUnitMap = translateContext.getValueUnits();
      if (valueUnitMap == null || valueUnitMap.isEmpty() || translateContext.getFormula() == null || translateContext.getDisplayName() == null) {
         logger.error("This translateContext data is incomplete translateContext:{}", translateContext);
         return null;
      }
      ValueUnit valueUnit = valueUnitMap.entrySet().iterator().next().getValue();
      double valueNum;
      String[] ids = translateContext.getFormula().split("\\+|-|\\*|/|\\(|\\)");
      if (ids.length > 1) {
         String formula = translateContext.getFormula();
         for (Map.Entry<String, ValueUnit> valueUnitEntry : valueUnitMap.entrySet()) {
            formula = formula.replaceAll(valueUnitEntry.getKey(), String.valueOf(valueUnitEntry.getValue().getValueNum()));
         }

         JexlExpression jexlExpression = jexl.createExpression(formula);
         JexlContext jexlContext = new MapContext();
         valueNum = (Double) jexlExpression.evaluate(jexlContext);
      } else {
         valueNum = valueUnit.getValueNum();
      }
      String unit = AssetService.databaseUnitAndOutputUnitMap.get(valueUnit.getUnit());
      MetricData metricData = new MetricData();
      metricData.setMetricName(translateContext.getDisplayName());
      metricData.setUnit(unit == null ? valueUnit.getUnit() : unit);
      metricData.setTimeStamp(valueUnit.getTime());
      metricData.setValue(valueUnit.getValue());
      metricData.setValueNum(valueNum);
      return metricData;
   };

   public static final Function<TranslateContext, MetricData> convert = basisConvert::apply;

   public static final Function<TranslateContext, MetricData> convertMetricNameByDisplayNameAndExtraIdentifier = translateContext -> {
      MetricData metricData = basisConvert.apply(translateContext);
      ValueUnit valueUnit = translateContext.getValueUnits().entrySet().iterator().next().getValue();
      metricData.setMetricName(String.format(translateContext.getDisplayName(), valueUnit.getExtraidentifier()));
      return metricData;
   };

   public static final Function<TranslateContext, MetricData> convertServerAverageUsedPowerEnergyConsumptionAverageTemperature = translateContext -> {
      MetricData metricData = basisConvert.apply(translateContext);
      ValueUnit valueUnit = translateContext.getValueUnits().entrySet().iterator().next().getValue();

      String extraInfo = valueUnit.getExtraidentifier();
      if (extraInfo == null) {
         logger.error("The valueUnit extraIdentifier is null valueUnits:{}", translateContext.getValueUnits());
         return null;
      }
      metricData.setTimeStamp(Long.parseLong(extraInfo));
      return metricData;
   };

   public static final Function<TranslateContext, MetricData> convertServerPeakUsedPowerMinimumUsedPowerPeakTemperature = translateContext -> {
      MetricData metricData = basisConvert.apply(translateContext);
      ValueUnit valueUnit = translateContext.getValueUnits().entrySet().iterator().next().getValue();

      String extraInfo = valueUnit.getExtraidentifier();
      if (extraInfo == null) {
         logger.error("The valueUnit extraIdentifier is null valueUnits:{}", translateContext.getValueUnits());
         return null;
      }
      String[] sinceTimeAndMetricTime = extraInfo.split(FlowgateConstant.SEPARATOR);
      if (sinceTimeAndMetricTime.length < 2) {
         logger.error("The extraIdentifierList.length of this valueUnit is not two valueUnits:{}", translateContext.getValueUnits());
         return null;
      }
      metricData.setTimeStamp(Long.parseLong(sinceTimeAndMetricTime[1]));
      return metricData;
   };

   public static final Function<TranslateContext, MetricData> convertPduInletCurrentVoltageFreeCapacityByExtraIdentifier = translateContext -> {
      MetricData metricData = basisConvert.apply(translateContext);
      ValueUnit valueUnit = translateContext.getValueUnits().entrySet().iterator().next().getValue();

      String extraIdentifier = valueUnit.getExtraidentifier();
      if (extraIdentifier == null) {
         logger.error("The valueUnit extraIdentifier is null valueUnits:{}", translateContext.getValueUnits());
         return null;
      }
      String[] extraIdentifierList = extraIdentifier.split("\\|");
      if (extraIdentifierList.length != 2) {
         logger.error("The extraIdentifierList.length of this valueUnit is not two valueUnits:{}", translateContext.getValueUnits());
      }
      // when length=2 it mean that the phase data for the pdu.(L1,L2,L3)
      // extraIdentifier:INLET:1|L1
      String inlet = extraIdentifierList[0];
      String pole = extraIdentifierList[1];
      metricData.setMetricName(String.format(translateContext.getDisplayName(), inlet, pole));
      return metricData;
   };

   static {
      // Server
      serverFormulaKeyAndFunction.put(MetricName.SERVER_AVERAGE_USED_POWER, convertServerAverageUsedPowerEnergyConsumptionAverageTemperature);
      serverFormulaKeyAndFunction.put(MetricName.SERVER_ENERGY_CONSUMPTION, convertServerAverageUsedPowerEnergyConsumptionAverageTemperature);
      serverFormulaKeyAndFunction.put(MetricName.SERVER_AVERAGE_TEMPERATURE, convertServerAverageUsedPowerEnergyConsumptionAverageTemperature);
      serverFormulaKeyAndFunction.put(MetricName.SERVER_PEAK_USED_POWER, convertServerPeakUsedPowerMinimumUsedPowerPeakTemperature);
      serverFormulaKeyAndFunction.put(MetricName.SERVER_MINIMUM_USED_POWER, convertServerPeakUsedPowerMinimumUsedPowerPeakTemperature);
      serverFormulaKeyAndFunction.put(MetricName.SERVER_PEAK_TEMPERATURE, convertServerPeakUsedPowerMinimumUsedPowerPeakTemperature);
      serverFormulaKeyAndFunction = Collections.unmodifiableMap(serverFormulaKeyAndFunction);
      // PDU
      pduFormulaKeyAndFunction.put(MetricName.PDU_XLET_ACTIVE_POWER, convertMetricNameByDisplayNameAndExtraIdentifier);
      pduFormulaKeyAndFunction.put(MetricName.PDU_XLET_APPARENT_POWER, convertMetricNameByDisplayNameAndExtraIdentifier);
      pduFormulaKeyAndFunction.put(MetricName.PDU_XLET_FREE_CAPACITY, convertMetricNameByDisplayNameAndExtraIdentifier);
      pduFormulaKeyAndFunction.put(MetricName.PDU_XLET_CURRENT, convertMetricNameByDisplayNameAndExtraIdentifier);
      pduFormulaKeyAndFunction.put(MetricName.PDU_XLET_VOLTAGE, convertMetricNameByDisplayNameAndExtraIdentifier);
      pduFormulaKeyAndFunction.put(MetricName.PDU_INLET_XPOLE_FREE_CAPACITY, convertPduInletCurrentVoltageFreeCapacityByExtraIdentifier);
      pduFormulaKeyAndFunction.put(MetricName.PDU_INLET_XPOLE_CURRENT, convertPduInletCurrentVoltageFreeCapacityByExtraIdentifier);
      pduFormulaKeyAndFunction.put(MetricName.PDU_INLET_XPOLE_VOLTAGE, convertPduInletCurrentVoltageFreeCapacityByExtraIdentifier);
      pduFormulaKeyAndFunction = Collections.unmodifiableMap(pduFormulaKeyAndFunction);
   }

}