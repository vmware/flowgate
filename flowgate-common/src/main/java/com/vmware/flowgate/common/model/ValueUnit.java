/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.util.HashMap;
import java.util.Map;

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

   public enum MetricUnit {
       MV, V, KV, VOLTS, MW, W, KW, KWH, MA, A, KA, AMPS, C, F, PERCENT
   }
   private static final Map<MetricUnit, Double> unitMap =new HashMap<MetricUnit, Double>() {{
       put(MetricUnit.MV, 0.001);
       put(MetricUnit.KV, (double) 1000);
       put(MetricUnit.V, (double) 1);
       put(MetricUnit.VOLTS, (double) 1);
       put(MetricUnit.MA, 0.001);
       put(MetricUnit.KA, (double) 1000);
       put(MetricUnit.A, (double) 1);
       put(MetricUnit.AMPS, (double) 1);
       put(MetricUnit.MW, 0.001);
       put(MetricUnit.KW, (double) 1000);
       put(MetricUnit.KWH, (double) 1000);
       put(MetricUnit.W, (double) 1);
       put(MetricUnit.C, (double) 1);
       put(MetricUnit.F, (double) 1);
   }};

   public String translateUnit(String val, MetricUnit sourceUnit, MetricUnit targetUnit) throws WormholeException  {
       if(sourceUnit == null || targetUnit == null) {
           throw new WormholeException("sourceUnit or targetUnit is NULL!");
       }
       switch(sourceUnit) {
       case F:
             if(targetUnit == MetricUnit.C) {
               return String.valueOf((Double.parseDouble(val) - 32)*5/9);
             }else if(targetUnit == MetricUnit.F) {
                 return val;
             }
       case C:
             if(targetUnit == MetricUnit.C) {
                 return val;
             }else if(targetUnit == MetricUnit.F) {
                 return String.valueOf((Double.parseDouble(val)*9/5 +32));
             }
       case KWH:
       case KW:
       case W:
       case MW:
       case KA:
       case A:
       case AMPS:
       case MA:
       case KV:
       case V:
       case VOLTS:
       case MV:
           return String.valueOf(Double.parseDouble(val) * (unitMap.get(sourceUnit)/unitMap.get(targetUnit)));
       default:
           return val;
       }
   }
}
