/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.flowgate.common.model;


import java.util.HashMap;
import java.util.Map;

public class TranslateContext {

   private Map<String, ValueUnit> valueData = new HashMap<String, ValueUnit>();
   private String formula = null;
   private String displayName = null;

   public static TranslateContext buildByValueUnitsAndDisplayNameAndFormula(Map<String, ValueUnit> valueUnits, String displayName, String formula) {
      TranslateContext translateContext = new TranslateContext();
      translateContext.valueData = valueUnits;
      translateContext.displayName = displayName;
      translateContext.formula = formula;
      return translateContext;
   }

   public void setValueUnits(Map<String, ValueUnit> valueUnits) {
      this.valueData = valueUnits;
   }

   public Map<String, ValueUnit> getValueUnits() {
      return this.valueData;
   }

   public void setFormula(String formula) {
      this.formula = formula;
   }

   public String getFormula() {
      return this.formula;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   @Override
   public String toString() {
      return "TranslateContext{" +
               "valueData=" + valueData +
               ", formula='" + formula + '\'' +
               ", displayName='" + displayName + '\'' +
               '}';
   }
}