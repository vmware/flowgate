package com.vmware.flowgate.service;

import java.util.Comparator;

import com.vmware.flowgate.common.model.ValueUnit;

public class CompareValueUnitByTime implements Comparator<ValueUnit>{
   @Override
   //The time of Server Energy Consumption is the end time
   //The start time of Server Energy Consumption in the extraInfo
   public int compare(ValueUnit v1, ValueUnit v2) {
      if(v1.getTime() >= v2.getTime()) {
         return 1;
      }
      return -1;
   }

}
