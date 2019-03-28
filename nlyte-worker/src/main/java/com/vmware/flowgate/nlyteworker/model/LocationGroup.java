/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.model;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LocationGroup {
   @JsonProperty(value="LocationGroupID")
   private int locationGroupID;
   @JsonProperty(value="LocationGroupName")
   private String locationGroupName;
   @JsonProperty(value="ParentLocationGroupID")
   private Integer parentLocationGroupID;
   @JsonProperty(value="LocationGroupType")
   private String locationGroupType;
   @JsonProperty(value="DuplicateLocationGroupName")
   private String duplicateLocationGroupName;
   @JsonProperty(value="Move")
   private boolean move;
   @JsonProperty(value="Recycle")
   private boolean recycle;
   @JsonProperty(value="Decommission")
   private boolean decommission;
   @JsonProperty(value="Tag")
   private String tag;
   @JsonProperty(value="LocationAlias")
   private String locationAlias;
   @JsonProperty(value="LocationPurposeId")
   private String locationPurposeId;
   @JsonProperty(value="Longitude")
   private String longitude;
   @JsonProperty(value="Latitude")
   private String latitude;
   @JsonProperty(value="HasChildren")
   private boolean hasChildren;
   @JsonProperty(value="HasFLoorPlannerRoom")
   private boolean hasFLoorPlannerRoom;
   @JsonProperty(value="IsAccessible")
   private boolean isAccessible;
   public int getLocationGroupID() {
      return locationGroupID;
   }
   public void setLocationGroupID(int locationGroupID) {
      this.locationGroupID = locationGroupID;
   }
   public String getLocationGroupName() {
      return locationGroupName;
   }
   public void setLocationGroupName(String locationGroupName) {
      this.locationGroupName = locationGroupName;
   }
   public Integer getParentLocationGroupID() {
      return parentLocationGroupID;
   }
   public void setParentLocationGroupID(Integer parentLocationGroupID) {
      this.parentLocationGroupID = parentLocationGroupID;
   }
   public String getLocationGroupType() {
      return locationGroupType;
   }
   public void setLocationGroupType(String locationGroupType) {
      this.locationGroupType = locationGroupType;
   }
   public String getDuplicateLocationGroupName() {
      return duplicateLocationGroupName;
   }
   public void setDuplicateLocationGroupName(String duplicateLocationGroupName) {
      this.duplicateLocationGroupName = duplicateLocationGroupName;
   }
   public boolean isMove() {
      return move;
   }
   public void setMove(boolean move) {
      this.move = move;
   }
   public boolean isRecycle() {
      return recycle;
   }
   public void setRecycle(boolean recycle) {
      this.recycle = recycle;
   }
   public boolean isDecommission() {
      return decommission;
   }
   public void setDecommission(boolean decommission) {
      this.decommission = decommission;
   }
   public String getTag() {
      return tag;
   }
   public void setTag(String tag) {
      this.tag = tag;
   }
   public String getLocationAlias() {
      return locationAlias;
   }
   public void setLocationAlias(String locationAlias) {
      this.locationAlias = locationAlias;
   }
   public String getLocationPurposeId() {
      return locationPurposeId;
   }
   public void setLocationPurposeId(String locationPurposeId) {
      this.locationPurposeId = locationPurposeId;
   }
   public String getLongitude() {
      return longitude;
   }
   public void setLongitude(String longitude) {
      this.longitude = longitude;
   }
   public String getLatitude() {
      return latitude;
   }
   public void setLatitude(String latitude) {
      this.latitude = latitude;
   }
   public boolean isHasChildren() {
      return hasChildren;
   }
   public void setHasChildren(boolean hasChildren) {
      this.hasChildren = hasChildren;
   }
   public boolean isHasFLoorPlannerRoom() {
      return hasFLoorPlannerRoom;
   }
   public void setHasFLoorPlannerRoom(boolean hasFLoorPlannerRoom) {
      this.hasFLoorPlannerRoom = hasFLoorPlannerRoom;
   }
   public boolean isAccessible() {
      return isAccessible;
   }
   public void setAccessible(boolean isAccessible) {
      this.isAccessible = isAccessible;
   }
}
