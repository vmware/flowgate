package com.vmware.flowgate.service;

import java.util.Comparator;

import com.vmware.flowgate.common.model.ValueUnit;

public class CompareValueUnit implements Comparator<ValueUnit>{
   @Override
   //The time of Server Energy Consumption is the end time
   //The start time of Server Energy Consumption in the extraInfo
   public int compare(ValueUnit v1, ValueUnit v2) {
      if(v1.getTime() > v2.getTime()) {
         return 1;
      }else if(v1.getTime() == v2.getTime()) {
         long valueUnit1StartTime = Long.parseLong(v1.getExtraidentifier());
         long valueUnit2StartTime = Long.parseLong(v2.getExtraidentifier());
         if(valueUnit1StartTime < valueUnit2StartTime) {
            return 1;
         }else if(valueUnit1StartTime > valueUnit2StartTime){
            return -1;
         }else {
            return 0;
         }
      }
      return -1;
   }
}
