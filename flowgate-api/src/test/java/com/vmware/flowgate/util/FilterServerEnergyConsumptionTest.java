package com.vmware.flowgate.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.flowgate.common.MetricName;
import com.vmware.flowgate.common.model.ValueUnit;
import com.vmware.flowgate.common.model.ValueUnit.MetricUnit;
import com.vmware.flowgate.service.AssetService;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class FilterServerEnergyConsumptionTest {

   @Autowired
   private AssetService assetService;

   /**
    * The since time(from open manage) early with the start time(API parameter)
    */
   @Test
   public void testFilterServerEnergyConsumption() {
      long time = System.currentTimeMillis();
      int duration = 5*60*1000;
      List<ValueUnit> valueUnits = getValueUnitsFromVC(time, duration);
      //Sample value of Openmanage SERVER_ENERGY_CONSUMPTION metric
      ValueUnit valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      valueUnit.setKey(MetricName.SERVER_ENERGY_CONSUMPTION);
      valueUnit.setExtraidentifier(String.valueOf(1302663085000l));
      valueUnit.setUnit(MetricUnit.kWh.name());
      valueUnit.setValueNum(65633);
      valueUnits.add(valueUnit);
      List<ValueUnit> results =  assetService.filterServerEnergyConsumption(valueUnits, time);
      TestCase.assertEquals(15, results.size());
      DecimalFormat df = new DecimalFormat("#.0000");
      for(ValueUnit valueunit : results) {
         Long sinceTime = Long.parseLong(valueunit.getExtraidentifier());
         String valueString = df.format(valueunit.getValueNum());
         Double value = Double.valueOf(valueString);
         if (sinceTime == time) {
            TestCase.assertEquals(0.001, value);
         } else if (sinceTime == time + 20000) {
            TestCase.assertEquals(0.002, value);
         } else if (sinceTime == time + 40000) {
            TestCase.assertEquals(0.003, value);
         } else if (sinceTime == time + 60000) {
            TestCase.assertEquals(0.004, value);
         } else if (sinceTime == time + 80000) {
            TestCase.assertEquals(0.005, value);
         } else if (sinceTime == time + 100000) {
            TestCase.assertEquals(0.006, value);
         } else if (sinceTime == time + 120000) {
            TestCase.assertEquals(0.007, value);
         } else if (sinceTime == time + 140000) {
            TestCase.assertEquals(0.008, value);
         } else if (sinceTime == time + 160000) {
            TestCase.assertEquals(0.009, value);
         } else if (sinceTime == time + 180000) {
            TestCase.assertEquals(0.01, value);
         } else if (sinceTime == time + 200000) {
            TestCase.assertEquals(0.011, value);
         } else if (sinceTime == time + 220000) {
            TestCase.assertEquals(0.012, value);
         } else if (sinceTime == time + 240000) {
            TestCase.assertEquals(0.013, value);
         } else if (sinceTime == time + 260000) {
            TestCase.assertEquals(0.014, value);
         } else if (sinceTime == time + 280000) {
            TestCase.assertEquals(0.015, value);
         }
      }
   }

   /**
    * We want to cover the combination with the longest time in a specified time period.
    * If the coverage time is the same, take the combination with the least number of combinations
    */
   @Test
   public void testFilterServerEnergyConsumption1() {
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      int vcduration = 30*60*1000;
      List<ValueUnit> valueUnits = getValueUnitsFromVC(time, vcduration);
      valueUnits.addAll(getValueUnitsFromOpenmanage(time, duration));
      //Sample value of Openmanage SERVER_ENERGY_CONSUMPTION metric
      List<ValueUnit> results =  assetService.filterServerEnergyConsumption(valueUnits, 1618204305000l);
      TestCase.assertEquals(1, results.size());
      for(ValueUnit value : results) {
         TestCase.assertEquals(60.0 ,value.getValueNum());
      }
   }

   @Test
   public void testFilterServerEnergyConsumption2() {
      long time = System.currentTimeMillis();
      int duration = 30*60*1000;
      int vcduration = 20*60*1000;
      //The vc-worker miss 10 min datas
      List<ValueUnit> valueUnits = getValueUnitsFromVC(time, vcduration);
      valueUnits.addAll(getValueUnitsFromOpenmanage(time, duration));
      List<ValueUnit> results =  assetService.filterServerEnergyConsumption(valueUnits, 1618204305000l);
      TestCase.assertEquals(1, results.size());
      for(ValueUnit value : results) {
         TestCase.assertEquals(60.0 ,value.getValueNum());
      }
   }

   @Test
   public void testFilterServerEnergyConsumption3() {
      long time = System.currentTimeMillis();
      //The openmanage-worker miss 10 min datas
      int openmanageDuration = 20*60*1000;//only have 4 value in the duration
      int vcduration = 30*60*1000;//hava 90 values in the duration
      //The max coverage time is vcduration, so the all vc datas(90 values) meet it
      //but we have another combination, it's coverage time is also vcduration,
      //the combination size is 31,the combination include one openmanage data and 30 vc datas
      //Because the fourth data'coverage time of openmanage is same with the the sum of the vc first sixty data
      //Finally, we use the least number of combinations
      List<ValueUnit> valueUnits = getValueUnitsFromVC(time, vcduration);
      valueUnits.addAll(getValueUnitsFromOpenmanage(time, openmanageDuration));
      List<ValueUnit> results =  assetService.filterServerEnergyConsumption(valueUnits, 1618204305000l);
      TestCase.assertEquals(31, results.size());
      TestCase.assertEquals(40.0 ,results.get(0).getValueNum());
   }

   @Test
   public void testFilterServerEnergyConsumption4() {
      long time = System.currentTimeMillis();
      //Sample value of Openmanage SERVER_ENERGY_CONSUMPTION metric
      ValueUnit valueUnit = new ValueUnit();
      valueUnit.setTime(time);
      valueUnit.setKey(MetricName.SERVER_ENERGY_CONSUMPTION);
      valueUnit.setExtraidentifier(String.valueOf(1302663085000l));
      valueUnit.setUnit(MetricUnit.kWh.name());
      valueUnit.setValueNum(65633);
      List<ValueUnit> valueUnits = new ArrayList<ValueUnit>();
      valueUnits.add(valueUnit);
      List<ValueUnit> results =  assetService.filterServerEnergyConsumption(valueUnits, time);
      TestCase.assertEquals(0, results.size());
   }

   List<ValueUnit> getValueUnitsFromVC(long time, int duration){
      List<ValueUnit> valueUnits = new ArrayList<>();
      ValueUnit valueUnit;
      int oneValuePerTime = 1000*20;
      int interval = duration/oneValuePerTime;
      double[] energyValues = new double[interval];
      for(int i = 0; i< interval; i++) {
         energyValues[i] = 0.001*(i+1);
      }
      for (int i = 0; i < interval; i++) {
         long tempTime = time + ((i + 1) * oneValuePerTime);
         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_ENERGY_CONSUMPTION);
         valueUnit.setExtraidentifier(String.valueOf(tempTime - oneValuePerTime));
         valueUnit.setUnit(MetricUnit.kWh.name());
         valueUnit.setValueNum(energyValues[i]);
         valueUnits.add(valueUnit);
      }
      return valueUnits;
   }

   /**
    *
    * @param time
    * @param duration
    * @return
    */
   List<ValueUnit> getValueUnitsFromOpenmanage(long startTime, int duration){
      //Sample values of Openmanage SERVER_ENERGY_CONSUMPTION metric
      List<ValueUnit> valueUnits = new ArrayList<>();
      ValueUnit valueUnit;
      int oneValuePerTime = 1000*5*60;//Every 5 min create a SERVER_ENERGY_CONSUMPTION
      int interval = duration/oneValuePerTime;
      double[] energyValues = new double[interval];
      for(int i = 0; i< interval; i++) {
         energyValues[i] = 10*(i+1);
      }
      for (int i = 0; i < interval; i++) {
         long tempTime = startTime + ((i + 1) * oneValuePerTime);
         valueUnit = new ValueUnit();
         valueUnit.setTime(tempTime);
         valueUnit.setKey(MetricName.SERVER_ENERGY_CONSUMPTION);
         //The startTime of SERVER_ENERGY_CONSUMPTION value from Openmanage is not change
         valueUnit.setExtraidentifier(String.valueOf(startTime));
         valueUnit.setUnit(MetricUnit.kWh.name());
         valueUnit.setValueNum(energyValues[i]);
         valueUnits.add(valueUnit);
      }
      return valueUnits;
   }

}
