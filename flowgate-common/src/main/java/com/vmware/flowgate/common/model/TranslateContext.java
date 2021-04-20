/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.flowgate.common.model;


import java.util.HashMap;
import java.util.Map;

public class TranslateContext {

   private Map<String, ValueUnit> valueData = new HashMap<String, ValueUnit>();
   private String formulars = null;
   private String displayName = null;

   public static TranslateContext buildByValueUnitsAndDisplayNameAndFormulars(Map<String, ValueUnit> valueUnits, String displayName, String formulars) {
      TranslateContext translateContext = new TranslateContext();
      translateContext.valueData = valueUnits;
      translateContext.displayName = displayName;
      translateContext.formulars = formulars;
      return translateContext;
   }

   public void setValueUnits(Map<String, ValueUnit> valueUnits) {
      this.valueData = valueUnits;
   }

   public Map<String, ValueUnit> getValueUnits() {
      return this.valueData;
   }

   public void setFormulars(String formulars) {
      this.formulars = formulars;
   }

   public String getFormulars() {
      return this.formulars;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public String getDisplayName() {
      return this.displayName;
   }

}