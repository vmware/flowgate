/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.util.HashMap;
import java.util.Map;

import com.vmware.flowgate.common.exception.WormholeException;

public class ValueUnit {
   private ValueType key;
   private String value;
   private double valueNum;
   private String unit;
   private long time;

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

   public ValueType getKey() {
      return key;
   }

   public void setKey(ValueType key) {
      this.key = key;
   }

   public enum ValueType {
      TEMP, HUMIDITY, PDU_RealtimeVoltage, PDU_RealtimeVoltagePercent, PDU_RealtimePower, 
      PDU_RealtimePowerPercent, PDU_RealtimeLoad, PDU_RealtimeLoadPercent,AirFlow,AirPressure,
      ContactClosure,Smoke,Water,Vibration
   }
   
   public enum MetricUnit {
       MV(ValueType.PDU_RealtimeVoltage), V(ValueType.PDU_RealtimeVoltage), KV(ValueType.PDU_RealtimeVoltage), VOLTS(ValueType.PDU_RealtimeVoltage),
       MW(ValueType.PDU_RealtimePower), W(ValueType.PDU_RealtimePower), KW(ValueType.PDU_RealtimePower), KWH(ValueType.PDU_RealtimePower),
       MA(ValueType.PDU_RealtimeLoad), A(ValueType.PDU_RealtimeLoad), KA(ValueType.PDU_RealtimeLoad), AMPS(ValueType.PDU_RealtimeLoad),
       C(ValueType.TEMP), F(ValueType.TEMP), PERCENT(ValueType.HUMIDITY)
       ;
    
       private final ValueType type;
       private MetricUnit(ValueType t) {
          this.type =t;
       }
       public ValueType getType() {
          return type;
       }
   }
   private static final Map<MetricUnit, Double> unitMap =new HashMap<MetricUnit, Double>() {{
       put(MetricUnit.MV, (double) 0.001);
       put(MetricUnit.KV, (double) 1000);
       put(MetricUnit.V, (double) 1);
       put(MetricUnit.VOLTS, (double) 1);
       put(MetricUnit.MA, (double) 0.001);
       put(MetricUnit.KA, (double) 1000);
       put(MetricUnit.A, (double) 1);
       put(MetricUnit.AMPS, (double) 1);
       put(MetricUnit.MW, (double) 0.001);
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
       if(sourceUnit.getType() != targetUnit.getType()) {
           throw new WormholeException("error, sourceUnit and targetUnit is not same type!");
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
