/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.flowgate.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MetricKeyName;
import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.RealtimeDataUnit;
import com.vmware.flowgate.common.model.MetricData;
import com.vmware.flowgate.common.model.TranslateContext;
import com.vmware.flowgate.common.model.ValueUnit;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class TranslateFunctionServiceTest {

   @Test
   public void testServerConvert() {
      String assetId = "752c0c7637104a39a4242031cd48785e";
      long currentTimeMillis = System.currentTimeMillis();

      ValueUnit valueUnit = new ValueUnit();
      valueUnit.setTime(currentTimeMillis);
      valueUnit.setKey(MetricName.SERVER_CPUUSAGE);
      valueUnit.setUnit(ValueUnit.MetricUnit.percent.name());
      valueUnit.setValueNum(4.67);

      String displayName = MetricName.SERVER_CPUUSAGE;
      Function<TranslateContext, MetricData> function = TranslateFunctionService.convert;
      Map<String, ValueUnit> valueUnitMap = new HashMap<>();
      valueUnitMap.put(assetId, valueUnit);
      TranslateContext translateContext = TranslateContext.buildByValueUnitsAndDisplayNameAndFormula(valueUnitMap, displayName, assetId);
      MetricData metricData = function.apply(translateContext);
      TestCase.assertEquals(valueUnit.getKey(), metricData.getMetricName());
      TestCase.assertEquals(valueUnit.getTime(), metricData.getTimeStamp());
      TestCase.assertEquals(valueUnit.getValueNum(), metricData.getValueNum());
      TestCase.assertEquals(valueUnit.getUnit(), metricData.getUnit());
   }

   @Test
   public void testServerConvertValue() {
      String assetId = "752c0c7637104a39a4242031cd48785e";
      long currentTimeMillis = System.currentTimeMillis();

      ValueUnit valueUnit = new ValueUnit();
      valueUnit.setTime(currentTimeMillis);
      valueUnit.setKey(MetricName.SERVER_CPUUSAGE);
      valueUnit.setUnit(ValueUnit.MetricUnit.percent.name());
      valueUnit.setValue("value");

      String displayName = MetricName.SERVER_CPUUSAGE;
      Function<TranslateContext, MetricData> function = TranslateFunctionService.convert;
      Map<String, ValueUnit> valueUnitMap = new HashMap<>();
      valueUnitMap.put(assetId, valueUnit);
      TranslateContext translateContext = TranslateContext.buildByValueUnitsAndDisplayNameAndFormula(valueUnitMap, displayName, assetId);
      MetricData metricData = function.apply(translateContext);
      TestCase.assertEquals(valueUnit.getKey(), metricData.getMetricName());
      TestCase.assertEquals(valueUnit.getTime(), metricData.getTimeStamp());
      TestCase.assertEquals("value", metricData.getValue());
      TestCase.assertEquals(0.0, metricData.getValueNum());
      TestCase.assertEquals(valueUnit.getUnit(), metricData.getUnit());
   }

   @Test
   public void testConvertServerAverageUsedPowerEnergyConsumptionAverageTemperature() {
      String assetId = "752c0c7637104a39a4242031cd48785e";
      long startTime = System.currentTimeMillis();
      long endTime = startTime + 30000;

      ValueUnit valueUnit = new ValueUnit();
      valueUnit.setTime(endTime);
      valueUnit.setExtraidentifier(String.valueOf(startTime));
      valueUnit.setKey(MetricName.SERVER_AVERAGE_USED_POWER);
      valueUnit.setUnit(ValueUnit.MetricUnit.kW.toString());
      valueUnit.setValueNum(0.06906666666666665);

      String displayName = MetricName.SERVER_AVERAGE_USED_POWER;
      Function<TranslateContext, MetricData> function = TranslateFunctionService.serverFormulaKeyAndFunction.get(MetricName.SERVER_AVERAGE_USED_POWER);
      Map<String, ValueUnit> valueUnitMap = new HashMap<>();
      valueUnitMap.put(assetId, valueUnit);
      TranslateContext translateContext = TranslateContext.buildByValueUnitsAndDisplayNameAndFormula(valueUnitMap, displayName, assetId);
      MetricData metricData = function.apply(translateContext);
      TestCase.assertEquals(valueUnit.getKey(), metricData.getMetricName());
      TestCase.assertEquals(startTime, metricData.getTimeStamp());
      TestCase.assertEquals(valueUnit.getValueNum(), metricData.getValueNum());
      TestCase.assertEquals(valueUnit.getUnit(), metricData.getUnit());
   }

   @Test
   public void testConvertServerPeakUsedPowerMinimumUsedPowerPeakTemperature() {
      String assetId = "752c0c7637104a39a4242031cd48785e";
      long startTime = System.currentTimeMillis();
      long maxValueTime = startTime + 2000;
      long endTime = startTime + 30000;

      ValueUnit valueUnit = new ValueUnit();
      valueUnit.setTime(endTime);
      valueUnit.setExtraidentifier(startTime + FlowgateConstant.SEPARATOR + maxValueTime);
      valueUnit.setKey(MetricName.SERVER_PEAK_USED_POWER);
      valueUnit.setUnit(ValueUnit.MetricUnit.kW.toString());
      valueUnit.setValueNum(0.07);

      String displayName = MetricName.SERVER_PEAK_USED_POWER;
      Function<TranslateContext, MetricData> function = TranslateFunctionService.serverFormulaKeyAndFunction.get(MetricName.SERVER_PEAK_USED_POWER);
      Map<String, ValueUnit> valueUnitMap = new HashMap<>();
      valueUnitMap.put(assetId, valueUnit);
      TranslateContext translateContext = TranslateContext.buildByValueUnitsAndDisplayNameAndFormula(valueUnitMap, displayName, assetId);
      MetricData metricData = function.apply(translateContext);
      TestCase.assertEquals(valueUnit.getKey(), metricData.getMetricName());
      TestCase.assertEquals(maxValueTime, metricData.getTimeStamp());
      TestCase.assertEquals(valueUnit.getValueNum(), metricData.getValueNum());
      TestCase.assertEquals(valueUnit.getUnit(), metricData.getUnit());
   }

   @Test
   public void testCalculationFormulaConvert() {
      String assetId1 = "752c0c7637104a39a4242031cd48785e";
      String assetId2 = "5dab088ab8a94aa994fdf9ddd8acceb6";
      String formula = "(752c0c7637104a39a4242031cd48785e+5dab088ab8a94aa994fdf9ddd8acceb6)/2";
      long currentTimeMillis = System.currentTimeMillis();

      ValueUnit humidityValue1 = new ValueUnit();
      humidityValue1.setValueNum(20);
      humidityValue1.setTime(currentTimeMillis);
      humidityValue1.setUnit(RealtimeDataUnit.Percent.toString());
      humidityValue1.setKey(MetricName.HUMIDITY);

      ValueUnit humidityValue2 = new ValueUnit();
      humidityValue2.setValueNum(19);
      humidityValue2.setTime(currentTimeMillis);
      humidityValue2.setUnit(RealtimeDataUnit.Percent.toString());
      humidityValue2.setKey(MetricName.HUMIDITY);

      String displayName = MetricKeyName.SERVER_BACK_HUMIDITY_LOCATIONX;
      String backToName = String.format(displayName, "BACK");
      Function<TranslateContext, MetricData> function = TranslateFunctionService.convert;
      Map<String, ValueUnit> valueUnitMap = new HashMap<>();
      valueUnitMap.put(assetId1, humidityValue1);
      valueUnitMap.put(assetId2, humidityValue2);
      TranslateContext translateContext = TranslateContext.buildByValueUnitsAndDisplayNameAndFormula(valueUnitMap, backToName, formula);
      MetricData metricData = function.apply(translateContext);
      TestCase.assertEquals(backToName, metricData.getMetricName());
      TestCase.assertEquals(currentTimeMillis, metricData.getTimeStamp());
      TestCase.assertEquals(19.5, metricData.getValueNum());
      TestCase.assertEquals(ValueUnit.MetricUnit.percent.toString(), metricData.getUnit());
   }

   @Test
   public void testExceptionConvertServerAverageUsedPowerEnergyConsumptionAverageTemperature() {
      String assetId = "752c0c7637104a39a4242031cd48785e";
      long startTime = System.currentTimeMillis();
      long endTime = startTime + 30000;

      ValueUnit valueUnit = new ValueUnit();
      valueUnit.setTime(endTime);
      valueUnit.setKey(MetricName.SERVER_AVERAGE_USED_POWER);
      valueUnit.setUnit(ValueUnit.MetricUnit.kW.toString());
      valueUnit.setValueNum(0.06906666666666665);

      String displayName = MetricName.SERVER_AVERAGE_USED_POWER;
      Function<TranslateContext, MetricData> function = TranslateFunctionService.serverFormulaKeyAndFunction.get(MetricName.SERVER_AVERAGE_USED_POWER);
      Map<String, ValueUnit> valueUnitMap = new HashMap<>();
      valueUnitMap.put(assetId, valueUnit);
      TranslateContext translateContext = TranslateContext.buildByValueUnitsAndDisplayNameAndFormula(valueUnitMap, displayName, assetId);
      MetricData metricData = function.apply(translateContext);
      TestCase.assertNull(metricData);
   }

   @Test
   public void testExceptionConvertServerPeakUsedPowerMinimumUsedPowerPeakTemperature() {
      String assetId = "752c0c7637104a39a4242031cd48785e";
      long startTime = System.currentTimeMillis();
      long endTime = startTime + 30000;

      ValueUnit valueUnit = new ValueUnit();
      valueUnit.setTime(endTime);
      valueUnit.setKey(MetricName.SERVER_PEAK_USED_POWER);
      valueUnit.setUnit(ValueUnit.MetricUnit.kW.toString());
      valueUnit.setValueNum(0.07);

      String displayName = MetricName.SERVER_PEAK_USED_POWER;
      Function<TranslateContext, MetricData> function = TranslateFunctionService.serverFormulaKeyAndFunction.get(MetricName.SERVER_PEAK_USED_POWER);
      Map<String, ValueUnit> valueUnitMap = new HashMap<>();
      valueUnitMap.put(assetId, valueUnit);
      TranslateContext translateContext = TranslateContext.buildByValueUnitsAndDisplayNameAndFormula(valueUnitMap, displayName, assetId);
      MetricData metricData = function.apply(translateContext);
      TestCase.assertNull(metricData);

      valueUnit.setExtraidentifier(String.valueOf(startTime));
      translateContext = TranslateContext.buildByValueUnitsAndDisplayNameAndFormula(valueUnitMap, displayName, assetId);
      metricData = function.apply(translateContext);
      TestCase.assertNull(metricData);
   }

   @Test
   public void testConvertByExtraIdentifier() {
      String assetId = "752c0c7637104a39a4242031cd48785e";
      long currentTimeMillis = System.currentTimeMillis();

      ValueUnit valueUnit = new ValueUnit();
      valueUnit.setKey(MetricName.PDU_ACTIVE_POWER);
      valueUnit.setUnit(ValueUnit.MetricUnit.kW.toString());
      valueUnit.setExtraidentifier("OUTLET:7");
      valueUnit.setValueNum(0.2);
      valueUnit.setTime(currentTimeMillis);

      String displayName = MetricName.PDU_XLET_ACTIVE_POWER;
      Function<TranslateContext, MetricData> function = TranslateFunctionService.pduFormulaKeyAndFunction.get(MetricName.PDU_XLET_ACTIVE_POWER);
      Map<String, ValueUnit> valueUnitMap = new HashMap<>();
      valueUnitMap.put(assetId, valueUnit);
      TranslateContext translateContext = TranslateContext.buildByValueUnitsAndDisplayNameAndFormula(valueUnitMap, displayName, assetId);
      MetricData metricData = function.apply(translateContext);
      TestCase.assertEquals(String.format(displayName, valueUnit.getExtraidentifier()), metricData.getMetricName());
      TestCase.assertEquals(valueUnit.getTime(), metricData.getTimeStamp());
      TestCase.assertEquals(valueUnit.getValueNum(), metricData.getValueNum());
      TestCase.assertEquals(valueUnit.getUnit(), metricData.getUnit());
   }

   @Test
   public void testConvertPduCurrentVoltageFreeCapacityByExtraIdentifier() {
      String assetId = "752c0c7637104a39a4242031cd48785e";
      long currentTimeMillis = System.currentTimeMillis();

      ValueUnit valueUnit = new ValueUnit();
      valueUnit.setKey(MetricName.PDU_CURRENT);
      valueUnit.setUnit(ValueUnit.MetricUnit.A.toString());
      valueUnit.setExtraidentifier("INLET:1|L1");
      valueUnit.setValueNum(6);
      valueUnit.setTime(currentTimeMillis);

      String displayName = MetricName.PDU_INLET_XPOLE_CURRENT;
      Function<TranslateContext, MetricData> function = TranslateFunctionService.pduFormulaKeyAndFunction.get(MetricName.PDU_INLET_XPOLE_CURRENT);
      Map<String, ValueUnit> valueUnitMap = new HashMap<>();
      valueUnitMap.put(assetId, valueUnit);
      TranslateContext translateContext = TranslateContext.buildByValueUnitsAndDisplayNameAndFormula(valueUnitMap, displayName, assetId);
      MetricData metricData = function.apply(translateContext);
      TestCase.assertEquals("INLET:1|L1|Current", metricData.getMetricName());
      TestCase.assertEquals(valueUnit.getTime(), metricData.getTimeStamp());
      TestCase.assertEquals(valueUnit.getValueNum(), metricData.getValueNum());
      TestCase.assertEquals(valueUnit.getUnit(), metricData.getUnit());
   }

}
