/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.couchbase.client.java.repository.annotation.Id;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetStatus;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.MountingSide;
import com.vmware.flowgate.common.model.ServerSensorData.ServerSensorType;

public class Asset implements Serializable, BaseDocument {

   /**
    * Created by wormhole, if use MongoDB, it will be the object id created by MongoDB _id
    */
   @Id
   private String id;

   /**
    * A unique number that can identify an asset from third part DCIM/CMDB systems.
    */
   private long assetNumber;

   /**
    * The name of the asset in the third part DCIM/CMDB systems. Usually it will be a unique
    * identifier of an asset
    */
   private String assetName;

   /**
    * From which third part systems does this asset comes from. It will refer to a source collection
    * which contains all the thirdpart systems
    */
   private String assetSource;

   /**
    * The category of the asset.
    */
   private AssetCategory category;

   /**
    * The subcategory of the asset. Only apply to some systems.
    */
   private AssetSubCategory subCategory;

   /**
    * The manufacture name , same manufacturer in different system may have different names. Eg
    * VMware vs VMware Inc.
    */
   private String manufacturer;

   /**
    * The model of the asset.
    */
   private String model;

   /**
    * The SN number of the asset, this number can be used to identify an asset. But only some
    * systems have this number.
    */
   private String serialnumber;

   /**
    * Some system will use tag to identify an asset. It can be either an number or a string.
    */
   private String tag;

   /**
    * The access address of the asset.
    */
   private AssetAddress assetAddress;

   /**
    * The location region of the asset
    */
   private String region;
   /**
    * The location country of the asset
    */
   private String country;
   /**
    * The location city of the asset
    */
   private String city;

   /**
    * The location building of the asset
    */
   private String building;

   /**
    * The location floor of the asset
    */
   private String floor;
   /**
    * The location room of the asset
    */
   private String room;
   private String row;
   private String col;

   /**
    * Extra location information. Only valid for some system.
    */
   private String extraLocation;

   /**
    * The cabinet name where this asset is located. If the asset is cabinet then this filed is
    * empty.
    */
   private String cabinetName;

   /**
    * The cabinet unit number
    */
   private int cabinetUnitPosition;

   /**
    * The cabinet unit mounting side
    */
   private MountingSide mountingSide;

   /**
    * The cabinet size(only for cabinet type)
    */
   private int cabinetsize;

   /**
    * The asset number of the cabinet. Will be used to search more detail information about the
    * cabinet.
    */
   private String cabinetAssetNumber;

   /**
    * Only valid for sensor type of asset.
    */
   private AssetRealtimeDataSpec assetRealtimeDataSpec;

   /**
    * It’s a collection. Can be any of the above fields. We use this filed to save the user input
    * value.
    */
   private HashMap<String, String> Justificationfields = new HashMap<String, String>();

   private Map<ServerSensorType, String> sensorsformulars =
         new HashMap<ServerSensorType, String>();

   private long lastupdate;
   private long created;

   private List<String> pdus;
   private List<String> switches;
   private AssetStatus status;


   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public long getAssetNumber() {
      return assetNumber;
   }

   public void setAssetNumber(long assetNumber) {
      this.assetNumber = assetNumber;
   }

   public String getAssetName() {
      return assetName;
   }

   public void setAssetName(String assetName) {
      this.assetName = assetName;
   }

   public String getAssetSource() {
      return assetSource;
   }

   public void setAssetSource(String assetSource) {
      this.assetSource = assetSource;
   }

   public AssetCategory getCategory() {
      return category;
   }

   public void setCategory(AssetCategory category) {
      this.category = category;
   }

   public AssetSubCategory getSubCategory() {
      return subCategory;
   }

   public void setSubCategory(AssetSubCategory subCategory) {
      this.subCategory = subCategory;
   }

   public String getManufacturer() {
      return manufacturer;
   }

   public void setManufacturer(String manufacturer) {
      this.manufacturer = manufacturer;
   }

   public String getModel() {
      return model;
   }

   public void setModel(String model) {
      this.model = model;
   }

   public String getSerialnumber() {
      return serialnumber;
   }

   public void setSerialnumber(String serialnumber) {
      this.serialnumber = serialnumber;
   }

   public String getTag() {
      return tag;
   }

   public void setTag(String tag) {
      this.tag = tag;
   }

   public AssetAddress getAssetAddress() {
      return assetAddress;
   }

   public void setAssetAddress(AssetAddress assetAddress) {
      this.assetAddress = assetAddress;
   }

   public String getRegion() {
      return region;
   }

   public void setRegion(String region) {
      this.region = region;
   }

   public String getCountry() {
      return country;
   }

   public void setCountry(String country) {
      this.country = country;
   }

   public String getCity() {
      return city;
   }

   public void setCity(String city) {
      this.city = city;
   }

   public String getBuilding() {
      return building;
   }

   public void setBuilding(String building) {
      this.building = building;
   }

   public String getFloor() {
      return floor;
   }

   public void setFloor(String floor) {
      this.floor = floor;
   }

   public String getRoom() {
      return room;
   }

   public void setRoom(String room) {
      this.room = room;
   }

   public String getRow() {
      return row;
   }

   public void setRow(String row) {
      this.row = row;
   }

   public String getCol() {
      return col;
   }

   public void setCol(String col) {
      this.col = col;
   }

   public String getExtraLocation() {
      return extraLocation;
   }

   public void setExtraLocation(String extraLocation) {
      this.extraLocation = extraLocation;
   }

   public String getCabinetName() {
      return cabinetName;
   }

   public void setCabinetName(String cabinetName) {
      this.cabinetName = cabinetName;
   }

   public int getCabinetUnitPosition() {
      return cabinetUnitPosition;
   }

   public void setCabinetUnitPosition(int cabinetUnitPosition) {
      this.cabinetUnitPosition = cabinetUnitPosition;
   }

   public MountingSide getMountingSide() {
      return mountingSide;
   }

   public void setMountingSide(MountingSide mountingSide) {
      this.mountingSide = mountingSide;
   }

   public int getCabinetsize() {
      return cabinetsize;
   }

   public void setCabinetsize(int cabinetsize) {
      this.cabinetsize = cabinetsize;
   }

   public String getCabinetAssetNumber() {
      return cabinetAssetNumber;
   }

   public void setCabinetAssetNumber(String cabinetAssetNumber) {
      this.cabinetAssetNumber = cabinetAssetNumber;
   }

   public AssetRealtimeDataSpec getAssetRealtimeDataSpec() {
      return assetRealtimeDataSpec;
   }

   public void setAssetRealtimeDataSpec(AssetRealtimeDataSpec assetRealtimeDataSpec) {
      this.assetRealtimeDataSpec = assetRealtimeDataSpec;
   }

   public HashMap<String, String> getJustificationfields() {
      return Justificationfields;
   }

   public void setJustificationfields(HashMap<String, String> justificationfields) {
      Justificationfields = justificationfields;
   }

   public Map<ServerSensorType, String> getSensorsformulars() {
      return sensorsformulars;
   }

   public void setSensorsformulars(Map<ServerSensorType, String> sensorsformulars) {
      this.sensorsformulars = sensorsformulars;
   }

   public long getLastupdate() {
      return lastupdate;
   }

   public void setLastupdate(long lastupdate) {
      this.lastupdate = lastupdate;
   }

   public long getCreated() {
      return created;
   }

   public void setCreated(long created) {
      this.created = created;
   }

   public List<String> getPdus() {
      return pdus;
   }

   public void setPdus(List<String> pdus) {
      this.pdus = pdus;
   }

   public List<String> getSwitches() {
      return switches;
   }

   public void setSwitches(List<String> switches) {
      this.switches = switches;
   }

   public AssetStatus getStatus() {
      return status;
   }

   public void setStatus(AssetStatus status) {
      this.status = status;
   }

   public boolean isExpired(long currentTime, long expiredTime) {
      long time = 0;
      long lastUpdateTime = this.getLastupdate();
      long createTime = this.getCreated();
      if(lastUpdateTime != 0) {
         time = lastUpdateTime;
      }else {
         time = createTime;
      }
      if(currentTime - time >= expiredTime) {
         return true;
      }
      return false;
   }
}
