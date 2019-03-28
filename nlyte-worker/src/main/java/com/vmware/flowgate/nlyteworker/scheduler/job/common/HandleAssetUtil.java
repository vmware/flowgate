/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.scheduler.job.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.vmware.flowgate.nlyteworker.model.LocationGroup;
import com.vmware.flowgate.nlyteworker.model.Manufacturer;
import com.vmware.flowgate.nlyteworker.model.Material;
import com.vmware.flowgate.nlyteworker.model.NlyteAsset;
import com.vmware.flowgate.nlyteworker.model.UMounting;
import com.vmware.flowgate.nlyteworker.restclient.NlyteAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetStatus;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.MountingSide;
import com.vmware.flowgate.common.NetworkMapping;
import com.vmware.flowgate.common.PduMapping;
import com.vmware.flowgate.common.model.Asset;

public class HandleAssetUtil {

   public static final String locationGroupType_Region = "Region";
   public static final String locationGroupType_Country = "Country";
   public static final String locationGroupType_City = "City";
   public static final String locationGroupType_Building = "Building";
   public static final String locationGroupType_Floor = "Floor";
   public static final String locationGroupType_Room = "Room";
   public static final int bladeServerMaterial = 1;
   public static final int standardServerMaterial = 2;
   public static final int powerStripMaterial = 3;
   public static final int cabinetMaterials = 4;
   public static final int networkMaterials = 5;
   /**
    *   
    * @return
    */
   public Map<String,Asset> generateAssetsMap (List<Asset> assets) {
      Map<String,Asset> assetFromWormholeMap = null;
      if(assets == null || assets.isEmpty()) {
         return null;
      }
      assetFromWormholeMap = new HashMap<String,Asset>();
      for(Asset asset:assets) {
         assetFromWormholeMap.put(asset.getAssetSource()+"_"+asset.getAssetNumber(), asset);
      }
      return assetFromWormholeMap;
      
   }
   /**
    * get mapped asset
    * @return
    */
   public void filterAssetBySourceAndCategory(List<Asset> mappedAsset,String source,AssetCategory category) {
      if(mappedAsset.isEmpty()) {
         return ;
      }
      Iterator<Asset> iterator = mappedAsset.iterator();
      while(iterator.hasNext()) {
         Asset asset = iterator.next();
         if(!asset.getAssetSource().equals(source) && !asset.getCategory().equals(category)) {
            iterator.remove();
         }
      }
   }
   
   /**
    * init MaterialsMap information
    * 
    */
   public HashMap<Integer,Material> initMaterialsMap(NlyteAPIClient nlyteAPIclient){
      List<Material> bladeServerMaterials = nlyteAPIclient.getMaterials(true,bladeServerMaterial);
      HashMap<Integer,Material> materialMap = new HashMap<Integer,Material>();
      for(Material material:bladeServerMaterials) {
         material.setMaterialType(AssetCategory.Server);
         material.setMaterialSubtype(AssetSubCategory.Blade);
         materialMap.put(material.getMaterialID(), material);  
      }
      List<Material> standardServerMaterials = nlyteAPIclient.getMaterials(true,standardServerMaterial);
      for(Material material:standardServerMaterials) {
         material.setMaterialType(AssetCategory.Server);
         material.setMaterialSubtype(AssetSubCategory.Standard);
         materialMap.put(material.getMaterialID(), material);
      }
      return materialMap;
   }
   
   /**
    * initLocationGroupMap information
    * 
    */
   public HashMap<Integer,LocationGroup> initLocationGroupMap(NlyteAPIClient nlyteAPIclient){
      List<LocationGroup> locationGroups = nlyteAPIclient.getLocationGroups(true);
      HashMap<Integer,LocationGroup> LocationMap = null;
      if(locationGroups.isEmpty()) {
         return null;
      }
      LocationMap = new HashMap<Integer,LocationGroup>();
      for(LocationGroup locationGroup:locationGroups) {
         LocationMap.put(locationGroup.getLocationGroupID(), locationGroup);
      }
      return LocationMap;
   }
  
   /**
    * init ManufacturersMap information
    */
   public HashMap<Integer,Manufacturer> initManufacturersMap(NlyteAPIClient nlyteAPIclient){
      List<Manufacturer> manufacturers = nlyteAPIclient.getManufacturers(true);
     HashMap<Integer,Manufacturer> manufacturerMap = null;
      if(manufacturers.isEmpty()) {
         return null;
      }
      manufacturerMap = new HashMap<Integer,Manufacturer>();
      for(Manufacturer manufacturer:manufacturers) {
         manufacturerMap.put(manufacturer.getManufacturerID(), manufacturer);
      }
     return manufacturerMap;
   }
   /**
    * 
    * @param nlyteAssets
    * @return
    */
   public List<Asset> getAssetsFromNlyte(String nlyteSource,List<NlyteAsset> nlyteAssets,
                                 HashMap<Integer,LocationGroup> locationMap,
                                 HashMap<Integer,Material> materialMap,
                                 HashMap<Integer,Manufacturer> manufacturerMap) {
      List<Asset> assetsFromNlyte = new ArrayList<Asset>();
      Asset asset;
      for(NlyteAsset nlyteAsset:nlyteAssets) {
         if(nlyteAsset.isTemplateRelated() || !nlyteAsset.isActived()) {
            continue;
         }
         asset = new Asset();
         asset.setAssetNumber(nlyteAsset.getAssetNumber());
         asset.setCabinetAssetNumber(String.valueOf(nlyteAsset.getCabinetAssetID()));
         asset.setTag(nlyteAsset.getTag());
         asset.setSerialnumber(nlyteAsset.getSerialNumber());
         asset.setAssetName(nlyteAsset.getAssetName());
         asset = supplementLocation(asset,nlyteAsset.getLocationGroupID(),locationMap);
         asset = supplementMaterial(asset,nlyteAsset.getMaterialID(),manufacturerMap,materialMap);
         asset.setAssetSource(nlyteSource);
         AssetStatus status = new AssetStatus();
         status.setNetworkMapping(NetworkMapping.UNMAPPED);
         status.setPduMapping(PduMapping.UNMAPPED);
         asset.setStatus(status);
         UMounting uMounting = nlyteAsset.getuMounting();
         if(uMounting!=null) {
            asset.setCabinetUnitPosition(uMounting.getCabinetUNumber());
            switch (MountingSide.valueOf(uMounting.getMountingSide())) {
            case Front:
               asset.setMountingSide(MountingSide.Front);
               break;
            case Back:
               asset.setMountingSide(MountingSide.Back);
               break;
            case Unmounted:
               asset.setMountingSide(MountingSide.Unmounted);
               break;
            default:
               break;
            }
            
         }
         assetsFromNlyte.add(asset);
      }
      return assetsFromNlyte;
   }
   
   /**
    * Supplemental location information
    * @param asset
    * @param locationGroupID
    * @return asset
    */
   public Asset supplementLocation(Asset asset,int locationGroupID,
                          HashMap<Integer,LocationGroup> locationMap) {
      LocationGroup locationGroup = locationMap.get(locationGroupID);
      if(locationGroup == null) {
         return asset;
      }
      while(locationGroup.getParentLocationGroupID() != null) {
         switch (locationGroup.getLocationGroupType()) {
         case locationGroupType_Region:
            asset.setRegion(locationGroup.getLocationGroupName());
            break;
         case locationGroupType_Country:
            asset.setCountry(locationGroup.getLocationGroupName());
            break;
         case locationGroupType_City:
            asset.setCity(locationGroup.getLocationGroupName());
            break;
         case locationGroupType_Building:
            asset.setBuilding(locationGroup.getLocationGroupName());
            break;
         case locationGroupType_Floor:
            asset.setFloor(locationGroup.getLocationGroupName());
            break;
         case locationGroupType_Room:
            asset.setRoom(locationGroup.getLocationGroupName());
            break;
         default:
            break;
         }
         locationGroup = locationMap.get(locationGroup.getParentLocationGroupID());
      }
      return asset;
   }
   /**
    * Supplemental Material information
    * @return
    */
   public Asset supplementMaterial(Asset asset,int materialID,
                                    HashMap<Integer,Manufacturer> manufacturerMap,
                                    HashMap<Integer,Material> materialMap) {
      Material material = materialMap.get(materialID);
      if(material == null) {
         return asset;
      }
      Manufacturer manufacturer = manufacturerMap.get(material.getManufacturerID());
      asset.setManufacturer(manufacturer.getDetail());
      asset.setModel(material.getMaterialName());
      asset.setSubCategory(material.getMaterialSubtype());
      asset.setCategory(material.getMaterialType());
      Integer uHeight =  material.getuHeight();
      if(uHeight!=null) {
         asset.setCabinetsize(uHeight);
      }
      return asset; 
   }
   
   public List<Asset> handleAssets(List<Asset> toUpdateAssets,Map<String,Asset> exsitingaAssetMap){
      List<Asset> resultAsset = new ArrayList<Asset>();
      List<Asset> updateAsset = new ArrayList<Asset>();
      Asset exsitingAsset = null;
      if(exsitingaAssetMap == null) {
         return toUpdateAssets;
      }
      for(Asset asset:toUpdateAssets) {
         if(exsitingaAssetMap.containsKey(asset.getAssetSource()+"_"+asset.getAssetNumber())) {
            exsitingAsset = exsitingaAssetMap.get(asset.getAssetSource()+"_"+asset.getAssetNumber());
            exsitingAsset.setTag(asset.getTag());
            exsitingAsset.setSerialnumber(asset.getSerialnumber());
            exsitingAsset.setAssetName(asset.getAssetName());
            exsitingAsset.setRegion(asset.getRegion());
            exsitingAsset.setCountry(asset.getCountry());
            exsitingAsset.setCity(asset.getCity());
            exsitingAsset.setBuilding(asset.getBuilding());
            exsitingAsset.setFloor(asset.getFloor());
            exsitingAsset.setRoom(asset.getRoom());
            exsitingAsset.setModel(asset.getModel());
            exsitingAsset.setManufacturer(asset.getManufacturer());
            exsitingAsset.setCategory(asset.getCategory());
            exsitingAsset.setSubCategory(asset.getSubCategory());
            exsitingAsset.setLastupdate(System.currentTimeMillis());
            exsitingAsset.setMountingSide(asset.getMountingSide());
            updateAsset.add(exsitingAsset);
         }else {
            resultAsset.add(asset);
         }
      }
      resultAsset.addAll(updateAsset);
      return resultAsset;
   }
 
}
