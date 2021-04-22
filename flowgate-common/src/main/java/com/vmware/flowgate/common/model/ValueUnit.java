/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import com.vmware.flowgate.common.exception.WormholeException;

public class ValueUnit {
   private String extraidentifier;//This property is used to describe some asset's attribute.
   private String key;
   private String value;
   private double valueNum;
   private String unit;
   private long time;

   public String getExtraidentifier() {
      return extraidentifier;
   }

   public void setExtraidentifier(String extraidentifier) {
      this.extraidentifier = extraidentifier;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public double getValueNum() {
      return valueNum;
   }

   public void setValueNum(double valueNum) {
      this.valueNum = valueNum;
   }

   public String getUnit() {
      return unit;
   }

   public void setUnit(String unit) {
      this.unit = unit;
   }

   public long getTime() {
      return time;
   }

   public void setTime(long time) {
      this.time = time;
   }

   public String getKey() {
      return key;
   }

   public void setKey(String key) {
      this.key = key;
   }

   /**
    * According to the Metric Prefix in the International System of Units,
    * M => 1000000; m => 0.001; k => 1000
    * Link: https://en.wikipedia.org/wiki/International_System_of_Units
    *
    */
   public enum MetricUnit {
       //These units include KW,KA,KWH,KB,KBps,PERCENT will be deprecated in Flowgate-1.3
       V("VOLTAGE",1), VOLTS("VOLTAGE",1), KV("VOLTAGE",1000), kV("VOLTAGE",1000),
       A("CURRENT",1), KA("CURRENT",1000), kA("CURRENT",1000), AMPS("CURRENT",1),
       W("POWER",1), KW("POWER",1000), kW("POWER",1000), BTUPerHr("POWER",0.293071),
       KWH("ENERGY",1000), kWh("ENERGY",1000),
       C("TEMPRETURE",1),F("TEMPRETURE",1),
       PERCENT("PERCENT",1),percent("PERCENT",1),
       KB("STORAGE",1), kB("STORAGE",1),
       KBps("RATE", 1), kBps("RATE", 1),
       Mhz("FREQUENCY", 1);

       private final String group;
       private final double factor;

       private MetricUnit(String group, double factor) {
          this.group = group;
          this.factor = factor;
       }
      public String getGroup() {
         return group;
      }
      public double getFactor() {
         return factor;
      }
   }

   public double translateUnit(double val, MetricUnit sourceUnit, MetricUnit targetUnit) throws WormholeException {
      if(sourceUnit == null || targetUnit == null) {
         throw new WormholeException("sourceUnit or targetUnit is NULL!");
      }
      if(!sourceUnit.getGroup().equals(targetUnit.getGroup())) {
         throw new WormholeException("error, sourceUnit and targetUnit is not a same group!");
      }
      switch(sourceUnit) {
         case F:
            if(targetUnit == MetricUnit.C) {
               return (val - 32) * 5 / 9;
            }else if(targetUnit == MetricUnit.F) {
               return val;
            }
         case C:
            if(targetUnit == MetricUnit.C) {
               return val;
            }else if(targetUnit == MetricUnit.F) {
               return val * 9 / 5 + 32;
            }
         default:
            return (val * sourceUnit.getFactor()) / targetUnit.getFactor();
      }
   }

   public String translateUnit(String val, MetricUnit sourceUnit, MetricUnit targetUnit) throws WormholeException {
      if (val == null || val.isEmpty()) {
         throw new WormholeException("value is NULL!");
      }
      return String.valueOf(translateUnit(Double.parseDouble(val), sourceUnit, targetUnit));
   }

   @Override
   public String toString() {
      return "ValueUnit{" +
               "extraidentifier='" + extraidentifier + '\'' +
               ", key='" + key + '\'' +
               ", value='" + value + '\'' +
               ", valueNum=" + valueNum +
               ", unit='" + unit + '\'' +
               ", time=" + time +
               '}';
   }

}
