/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Sensor {

   private int id;
   @JsonProperty(value = "pdu_id")
   private Integer pduId;
   private int ordinal;
   @JsonProperty(value = "attribute_name")
   private String attributeName;
   private String name;
   @JsonProperty(value = "decommissioned_at")
   private String decommissionedAt;
   private String position;
   @JsonProperty(value = "vertical_position")
   private String verticalPosition;
   @JsonProperty(value = "serial_number")
   private String serialNumber;
   private String type;
   private Parent parent;
   @JsonProperty(value = "pdu_sensor_id")
   private int pduSensorId;
   private String label;
   private String removed;
   private SensorReading reading;
   //private State state;

   public void setId(int id) {
      this.id = id;
   }

   public int getId() {
      return id;
   }

   public Integer getPduId() {
      return pduId;
   }

   public void setPduId(Integer pduId) {
      this.pduId = pduId;
   }

   public void setOrdinal(int ordinal) {
      this.ordinal = ordinal;
   }

   public int getOrdinal() {
      return ordinal;
   }

   public String getAttributeName() {
      return attributeName;
   }

   public void setAttributeName(String attributeName) {
      this.attributeName = attributeName;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDecommissionedAt() {
      return decommissionedAt;
   }

   public void setDecommissionedAt(String decommissionedAt) {
      this.decommissionedAt = decommissionedAt;
   }

   public String getPosition() {
      return position;
   }

   public void setPosition(String position) {
      this.position = position;
   }

   public String getVerticalPosition() {
      return verticalPosition;
   }

   public void setVerticalPosition(String verticalPosition) {
      this.verticalPosition = verticalPosition;
   }

   public String getSerialNumber() {
      return serialNumber;
   }

   public void setSerialNumber(String serialNumber) {
      this.serialNumber = serialNumber;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public Parent getParent() {
      return parent;
   }

   public void setParent(Parent parent) {
      this.parent = parent;
   }

   public int getPduSensorId() {
      return pduSensorId;
   }

   public void setPduSensorId(int pduSensorId) {
      this.pduSensorId = pduSensorId;
   }

   public String getLabel() {
      return label;
   }

   public void setLabel(String label) {
      this.label = label;
   }

   public String getRemoved() {
      return removed;
   }

   public void setRemoved(String removed) {
      this.removed = removed;
   }

   public SensorReading getReading() {
      return reading;
   }

   public void setReading(SensorReading reading) {
      this.reading = reading;
   }
}