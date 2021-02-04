/**
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.openmanage.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * "@odata.type": "#PowerService.Settings",
   "Id": 1,
   "Name": "TEMPERATURE_DISPLAY_UNIT",
   "DefaultValue": 1,
   "Value": 1
 * @author pengpengw
 *
 */
public class PowerSetting {

   @JsonProperty(value="@odata.type")
   private String odataType;
   @JsonProperty(value="Id")
   private int id;
   @JsonProperty(value="Name")
   private String name;
   @JsonProperty(value="DefaultValue")
   private int defaultValue;
   @JsonProperty(value="Value")
   private int value;
   public String getOdataType() {
      return odataType;
   }
   public void setOdataType(String odataType) {
      this.odataType = odataType;
   }
   public int getId() {
      return id;
   }
   public void setId(int id) {
      this.id = id;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public int getDefaultValue() {
      return defaultValue;
   }
   public void setDefaultValue(int defaultValue) {
      this.defaultValue = defaultValue;
   }
   public int getValue() {
      return value;
   }
   public void setValue(int value) {
      this.value = value;
   }

}
