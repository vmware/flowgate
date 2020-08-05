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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.AssetStatus;
import com.vmware.flowgate.common.AssetSubCategory;
import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.MountingSide;
import com.vmware.flowgate.common.NetworkMapping;
import com.vmware.flowgate.common.PduMapping;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.FlowgateChassisSlot;
import com.vmware.flowgate.common.model.Parent;
import com.vmware.flowgate.common.model.Tenant;
import com.vmware.flowgate.nlyteworker.model.CabinetU;
import com.vmware.flowgate.nlyteworker.model.ChassisMountedAssetMap;
import com.vmware.flowgate.nlyteworker.model.ChassisSlot;
import com.vmware.flowgate.nlyteworker.model.CustomField;
import com.vmware.flowgate.nlyteworker.model.LocationGroup;
import com.vmware.flowgate.nlyteworker.model.Manufacturer;
import com.vmware.flowgate.nlyteworker.model.Material;
import com.vmware.flowgate.nlyteworker.model.NlyteAsset;
import com.vmware.flowgate.nlyteworker.model.UMounting;
import com.vmware.flowgate.nlyteworker.restclient.NlyteAPIClient;

public class HandleAssetUtil {

   private static final Logger logger = LoggerFactory.getLogger(HandleAssetUtil.class);
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
   public static final int chassisMaterials = 6;
   public static final int NETWORK_SUBTYPE_STANDARD = 7;
   private static final String CabinetU_State_Full = "Full";
   private static final String CabinetU_State_Free = "Free";

   private ObjectMapper mapper = new ObjectMapper();
   /**
    *
    * @return
    */
   public Map<Long,Asset> generateAssetsMap (List<Asset> assets) {
      Map<Long,Asset> assetFromWormholeMap = null;
      if(assets == null || assets.isEmpty()) {
         return null;
      }
      assetFromWormholeMap = new HashMap<Long,Asset>();
      for(Asset asset:assets) {
         assetFromWormholeMap.put(asset.getAssetNumber(), asset);
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
   public HashMap<Integer,Material> initServerMaterialsMap(NlyteAPIClient nlyteAPIclient){
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
                                 HashMap<Integer,Manufacturer> manufacturerMap,
                                 HashMap<Long,String> chassisMountedAssetNumberAndChassisIdMap) {
      List<Asset> assetsFromNlyte = new ArrayList<Asset>();
      Asset asset;
      for(NlyteAsset nlyteAsset:nlyteAssets) {
         asset = new Asset();
         asset.setAssetNumber(nlyteAsset.getAssetNumber());
         asset.setCabinetAssetNumber(String.valueOf(nlyteAsset.getCabinetAssetID()));
         asset.setCabinetName(nlyteAsset.getCabinetName());
         asset.setTag(nlyteAsset.getTag());
         asset.setSerialnumber(nlyteAsset.getSerialNumber());
         asset.setAssetName(nlyteAsset.getAssetName());
         asset.setRow(nlyteAsset.getGridReferenceRow());
         asset.setCol(nlyteAsset.getGridReferenceColumn());
         asset = supplementLocation(asset,nlyteAsset.getLocationGroupID(),locationMap);
         asset = supplementMaterial(asset,nlyteAsset.getMaterialID(),manufacturerMap,materialMap);
         //we need to refactor the code
         if (asset.getCategory() == null) {
            continue;
         }
         if (asset.getCategory() == AssetCategory.Server) {
            if(chassisMountedAssetNumberAndChassisIdMap != null && chassisMountedAssetNumberAndChassisIdMap.containsKey(asset.getAssetNumber())) {
               Parent parent = new Parent();
               parent.setType(AssetCategory.Chassis.name());
               parent.setParentId(chassisMountedAssetNumberAndChassisIdMap.get(asset.getAssetNumber()));
               asset.setParent(parent);
            }
            //set the tenant information.
            List<CustomField> fields = nlyteAsset.getCustomFields();
            if (fields != null) {
               Tenant tenant = new Tenant();
               for (CustomField cf : fields) {
                  if (cf.getDataLabel().equals(CustomField.Owner)) {
                     String dataValue = cf.getDataValueString();
                     if (!StringUtils.isEmpty(dataValue)) {
                        tenant.setOwner(dataValue);
                        tenant.setTenant(dataValue);
                     }
                  } else if (cf.getDataLabel().equals(CustomField.Tenant_EndUser)) {
                     String dataValue = cf.getDataValueString();
                     if (!StringUtils.isEmpty(dataValue)) {
                        tenant.setTenant(dataValue);
                     }
                  } else if (cf.getDataLabel().equals(CustomField.HaaS_RequestedBy)) {
                     String dataValue = cf.getDataValueString();
                     if (!StringUtils.isEmpty(dataValue)) {
                        tenant.setTenant(dataValue);
                     }
                  } else if (cf.getDataLabel().equals(CustomField.Tenant_Manager)) {
                     String dataValue = cf.getDataValueString();
                     if (!StringUtils.isEmpty(dataValue)) {
                        tenant.setTenantManager(dataValue);
                     }
                  }
               }
               asset.setTenant(tenant);
            }
         }
         if(asset.getCategory().equals(AssetCategory.Cabinet)) {
            String contiguousUSpace = nlyteAsset.getContiguousUSpace();
            if(contiguousUSpace != null) {
               int freeSize = 0;
               String contiguousUSpaces[] = contiguousUSpace.split(FlowgateConstant.SPILIT_FLAG);
               for(String uSpace : contiguousUSpaces) {
                  freeSize += Integer.parseInt(uSpace);
               }
               asset.setFreeCapacity(freeSize);
            }
            List<CabinetU> cabinetus = nlyteAsset.getCabinetUs();
            if(cabinetus != null && !cabinetus.isEmpty()) {
               List<com.vmware.flowgate.common.model.CabinetU> flowgateCabinetUs = new ArrayList<com.vmware.flowgate.common.model.CabinetU>();
               for(CabinetU cabinetu : cabinetus) {
                  com.vmware.flowgate.common.model.CabinetU flowgateCabinetU = new com.vmware.flowgate.common.model.CabinetU();
                  flowgateCabinetU.setAssetsOnUnit(cabinetu.getAssetsOnU());
                  flowgateCabinetU.setCabinetUNumber(cabinetu.getCabinetUNumber());
                  if(CabinetU_State_Full.equals(cabinetu.getCabinetUState())) {
                     flowgateCabinetU.setUsed(true);
                  }else if(CabinetU_State_Free.equals(cabinetu.getCabinetUState())) {
                     flowgateCabinetU.setUsed(false);
                  }
                  flowgateCabinetUs.add(flowgateCabinetU);
               }
               try {
                  String cabinetUsInfo = mapper.writeValueAsString(flowgateCabinetUs);
                  HashMap<String,String> justficationfields = new HashMap<String, String>();
                  justficationfields.put(FlowgateConstant.CABINETUNITS, cabinetUsInfo);
                  asset.setJustificationfields(justficationfields);
               } catch (JsonProcessingException e) {
                  logger.error("Failed to get info of cabinetUs.");
               }
            }
         }
         if(asset.getCategory().equals(AssetCategory.Chassis)) {
            handleChassisSolts(asset,nlyteAsset);
         }
         if(asset.getCategory().equals(AssetCategory.Networks)) {
            if(chassisMountedAssetNumberAndChassisIdMap != null && chassisMountedAssetNumberAndChassisIdMap.containsKey(asset.getAssetNumber())) {
               Parent parent = new Parent();
               parent.setType(AssetCategory.Chassis.name());
               parent.setParentId(chassisMountedAssetNumberAndChassisIdMap.get(asset.getAssetNumber()));
               asset.setParent(parent);
            }
         }
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
         asset.setCreated(System.currentTimeMillis());
         assetsFromNlyte.add(asset);
      }
      return assetsFromNlyte;
   }

   public void handleChassisSolts(Asset asset, NlyteAsset nlyteAsset) {
      List<ChassisSlot> chassisSolts = nlyteAsset.getChassisSlots();
      List<ChassisMountedAssetMap> chassisMountedAssets = nlyteAsset.getChassisMountedAssetMaps();
      List<FlowgateChassisSlot> flowgateChassisSlots = new ArrayList<FlowgateChassisSlot>();
      int usedSolts = 0;
      if(chassisSolts == null || chassisSolts.isEmpty()) {
         if(chassisMountedAssets == null || chassisMountedAssets.isEmpty()) {
            return;
         }
         usedSolts = chassisMountedAssets.size();
         for(ChassisMountedAssetMap caMap : chassisMountedAssets) {
            FlowgateChassisSlot solt = new FlowgateChassisSlot();
            solt.setColumnPosition(caMap.getColumnPosition());
            solt.setMountingSide(caMap.getMountingSide());
            solt.setRowPosition(caMap.getRowPosition());
            solt.setSlotName(caMap.getSlotName());
            solt.setMountedAssetNumber(caMap.getMountedAssetID());
            flowgateChassisSlots.add(solt);
         }
      }else {
         HashMap<String, Integer> soltsAndmountedAssetNumberMap = new HashMap<String, Integer>();
         if(chassisMountedAssets != null && !chassisMountedAssets.isEmpty()) {
            usedSolts = chassisMountedAssets.size();
            for(ChassisMountedAssetMap caMap : chassisMountedAssets) {
               soltsAndmountedAssetNumberMap.put(caMap.getMountingSide()+caMap.getSlotName(), caMap.getMountedAssetID());
            }
         }
         for(ChassisSlot chassisSolt : chassisSolts) {
            FlowgateChassisSlot solt = new FlowgateChassisSlot();
            solt.setColumnPosition(chassisSolt.getColumnPosition());
            solt.setMountingSide(chassisSolt.getMountingSide());
            solt.setRowPosition(chassisSolt.getRowPosition());
            solt.setSlotName(chassisSolt.getSlotName());
            solt.setMountedAssetNumber(soltsAndmountedAssetNumberMap.get(solt.getMountingSide()+solt.getSlotName()));
            flowgateChassisSlots.add(solt);
         }
      }
      asset.setFreeCapacity(asset.getCapacity() - usedSolts);
      try {
         String chassisSoltsInfo = mapper.writeValueAsString(flowgateChassisSlots);
         supplementChassisInfo(asset, FlowgateConstant.CHASSISSLOTS, chassisSoltsInfo);
      } catch (JsonProcessingException e) {
         logger.error("Failed to get info of chassisSolts.");
      }
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
      switch (asset.getCategory()) {
      case Cabinet:
         if(uHeight!=null) {
            asset.setCapacity(uHeight);
         }
         break;
      case Networks:
         int totalSize = material.getTotalCopperPorts() + material.getTotalFibreOpticPorts() + material.getTotalUndefinedPorts();
         asset.setCapacity(totalSize);
         break;
      case Chassis:
         int columnsBack = material.getNumberOfColumnsBack();
         int rowsBack = material.getNumberOfRowsBack();
         int columnsFront = material.getNumberOfColumnsFront();
         int rowsFront = material.getNumberOfRowsFront();
         asset.setCapacity(columnsBack*rowsBack + columnsFront*rowsFront);
         supplementChassisInfo(asset, FlowgateConstant.CHASSIS_AIR_FLOW_TYPE, material.getAirflowTypeID());
         break;
      default:
         break;
      }
      return asset;
   }

   public void supplementChassisInfo(Asset asset, String key, String value) {
      if(value == null) {
         return;
      }
      HashMap<String, String> justficationMap = asset.getJustificationfields();
      String chassisInfo = justficationMap.get(FlowgateConstant.CHASSIS);
      Map<String, String> chassisInfoMap = null;
      try {
         if(chassisInfo != null) {
            chassisInfoMap = mapper.readValue(chassisInfo, new TypeReference<Map<String,String>>() {});
         }else {
            chassisInfoMap = new HashMap<String,String>();
         }
         chassisInfoMap.put(key, value);
         String newChassisInfo = mapper.writeValueAsString(chassisInfoMap);
         justficationMap.put(FlowgateConstant.CHASSIS, newChassisInfo);
         asset.setJustificationfields(justficationMap);
      } catch (Exception e) {
         logger.error("Failed to update the value: "+value+" with key: "+key+".");
      }
   }

   public List<Asset> handleAssets(List<Asset> toUpdateAssets,Map<Long,Asset> exsitingaAssetMap){
      List<Asset> resultAsset = new ArrayList<Asset>();
      List<Asset> updateAsset = new ArrayList<Asset>();
      Asset exsitingAsset = null;
      if(exsitingaAssetMap == null) {
         return toUpdateAssets;
      }
      for(Asset asset:toUpdateAssets) {
         boolean isUpdated = false;
         if(exsitingaAssetMap.containsKey(asset.getAssetNumber())) {
            exsitingAsset = exsitingaAssetMap.get(asset.getAssetNumber());
            if(valueIsChanged(exsitingAsset.getCabinetName(), asset.getCabinetName())) {
               exsitingAsset.setCabinetName(asset.getCabinetName());
               isUpdated = true;
            }
            if(valueIsChanged(exsitingAsset.getTag(),asset.getTag())) {
               exsitingAsset.setTag(asset.getTag());
               isUpdated = true;
            }
            if(valueIsChanged(exsitingAsset.getCapacity(),asset.getCapacity())) {
               exsitingAsset.setCapacity(asset.getCapacity());
               isUpdated = true;
            }
            if(valueIsChanged(exsitingAsset.getFreeCapacity(),asset.getFreeCapacity())) {
               exsitingAsset.setFreeCapacity(asset.getFreeCapacity());
            }
            exsitingAsset.setSerialnumber(asset.getSerialnumber());
            exsitingAsset.setAssetName(asset.getAssetName());
            exsitingAsset.setRegion(asset.getRegion());
            exsitingAsset.setCountry(asset.getCountry());
            exsitingAsset.setCity(asset.getCity());
            exsitingAsset.setBuilding(asset.getBuilding());
            exsitingAsset.setFloor(asset.getFloor());
            if(valueIsChanged(exsitingAsset.getRoom(), asset.getRoom())) {
               exsitingAsset.setRoom(asset.getRoom());
               isUpdated = true;
            }
            exsitingAsset.setModel(asset.getModel());
            exsitingAsset.setManufacturer(asset.getManufacturer());
            exsitingAsset.setCategory(asset.getCategory());
            exsitingAsset.setSubCategory(asset.getSubCategory());
            exsitingAsset.setLastupdate(System.currentTimeMillis());
            if(valueIsChanged(exsitingAsset.getRow(), asset.getRow())) {
               exsitingAsset.setRow(asset.getRow());
               isUpdated = true;
            }
            if(valueIsChanged(exsitingAsset.getCol(), asset.getCol())) {
               exsitingAsset.setCol(asset.getCol());
               isUpdated = true;
            }
            if(exsitingAsset.getMountingSide() != asset.getMountingSide()) {
               exsitingAsset.setMountingSide(asset.getMountingSide());
               isUpdated = true;
            }
            String oldOwner = null;
            String newOwner = null;
            String oldTenantValue = null;
            String newTenantValue = null;
            String oldTenantManager = null;
            String newTenantManager = null;
            Tenant oldTenant = exsitingAsset.getTenant();
            Tenant newTenant = asset.getTenant();
            if(oldTenant != null) {
               oldOwner = oldTenant.getOwner();
               oldTenantValue = oldTenant.getTenant();
               oldTenantManager = oldTenant.getTenantManager();
            }
            if(newTenant != null) {
               newOwner = newTenant.getOwner();
               newTenantValue = newTenant.getTenant();
               newTenantManager = newTenant.getTenantManager();
            }
            if (valueIsChanged(oldOwner,newOwner)
                  || valueIsChanged(oldTenantValue,newTenantValue)
                  || valueIsChanged(oldTenantManager,newTenantManager)) {
               exsitingAsset.setTenant(asset.getTenant());
               isUpdated = true;
            }
            if (exsitingAsset.getCategory().equals(AssetCategory.Cabinet)) {
               if (asset.getJustificationfields() != null
                     && asset.getJustificationfields().get(FlowgateConstant.CABINETUNITS) != null) {
                  exsitingAsset.getJustificationfields().put(FlowgateConstant.CABINETUNITS,
                        asset.getJustificationfields().get(FlowgateConstant.CABINETUNITS));
                  isUpdated = true;
               }
            }
            if(exsitingAsset.getCategory().equals(AssetCategory.Chassis)) {
               if (asset.getJustificationfields() != null
                     && asset.getJustificationfields().get(FlowgateConstant.CHASSIS) != null) {

                  HashMap<String, String> newJustficationMap = asset.getJustificationfields();
                  HashMap<String, String> oldJustficationMap = exsitingAsset.getJustificationfields();
                  String newChassisInfo = newJustficationMap.get(FlowgateConstant.CHASSIS);
                  String oldChassisInfo = oldJustficationMap.get(FlowgateConstant.CHASSIS);
                  Map<String, String> newChassisInfoMap = null;
                  Map<String, String> oldChassisInfoMap = null;
                  try {
                     if(newChassisInfo != null) {
                        newChassisInfoMap = mapper.readValue(newChassisInfo, new TypeReference<Map<String,String>>() {});
                     }
                     if(oldChassisInfo != null) {
                        oldChassisInfoMap = mapper.readValue(oldChassisInfo, new TypeReference<Map<String,String>>() {});
                     }
                  } catch (Exception e) {
                     logger.error("Failed to read the data of chassis");
                  }
                  if(oldChassisInfoMap == null) {
                     if(newChassisInfoMap != null) {
                        oldJustficationMap.put(FlowgateConstant.CHASSIS, newChassisInfo);
                        exsitingAsset.setJustificationfields(oldJustficationMap);
                        isUpdated = true;
                     }
                  }else {
                     if(newChassisInfoMap != null) {
                        if (valueIsChanged(
                              oldChassisInfoMap.get(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE),
                              newChassisInfoMap.get(FlowgateConstant.CHASSIS_AIR_FLOW_TYPE))) {
                           oldJustficationMap.put(FlowgateConstant.CHASSIS, newChassisInfo);
                           exsitingAsset.setJustificationfields(oldJustficationMap);
                           isUpdated = true;
                        }else {
                           String newChassisSlots = newChassisInfoMap.get(FlowgateConstant.CHASSISSLOTS);
                           String oldChassisSlots = oldChassisInfoMap.get(FlowgateConstant.CHASSISSLOTS);
                           List<FlowgateChassisSlot> newFlowgateChassisSlots = null;
                           List<FlowgateChassisSlot> oldFlowgateChassisSlots = null;
                           if(newChassisSlots != null) {
                              if(oldChassisSlots != null) {
                                 try {
                                    newFlowgateChassisSlots = mapper.readValue(newChassisSlots, new TypeReference<List<FlowgateChassisSlot>>() {});
                                    oldFlowgateChassisSlots = mapper.readValue(oldChassisSlots, new TypeReference<List<FlowgateChassisSlot>>() {});
                                 } catch (Exception e) {
                                    logger.error("Failed to read the data of chassis");
                                 }
                                 if (chassisSlotsIsChanged(oldFlowgateChassisSlots,
                                       newFlowgateChassisSlots)) {
                                    oldJustficationMap.put(FlowgateConstant.CHASSIS, newChassisInfo);
                                    exsitingAsset.setJustificationfields(oldJustficationMap);
                                    isUpdated = true;
                                 }
                              }else {
                                 oldJustficationMap.put(FlowgateConstant.CHASSIS, newChassisInfo);
                                 exsitingAsset.setJustificationfields(oldJustficationMap);
                                 isUpdated = true;
                              }
                           }
                        }
                     }
                  }
               }
            }
            if(exsitingAsset.getCategory().equals(AssetCategory.Server) ||
                  exsitingAsset.getCategory().equals(AssetCategory.Networks)) {
               Parent oldParent = exsitingAsset.getParent();
               String oldParentId = null;
               String oldParentType = null;
               if(oldParent != null) {
                  oldParentId = oldParent.getParentId();
                  oldParentType = oldParent.getType();
               }
               Parent newParent = asset.getParent();
               String newParentId = null;
               String newParentType = null;
               if(newParent != null) {
                  newParentId = newParent.getParentId();
                  newParentType = newParent.getType();
               }
               if(valueIsChanged(oldParentId, newParentId) ||
                     valueIsChanged(oldParentType, newParentType)) {
                  exsitingAsset.setParent(newParent);
                  isUpdated = true;
               }
            }
            if(isUpdated) {
               updateAsset.add(exsitingAsset);
            }
         }else {
            asset.setCreated(System.currentTimeMillis());
            resultAsset.add(asset);
         }
      }
      resultAsset.addAll(updateAsset);
      return resultAsset;
   }

   public boolean chassisSlotsIsChanged(List<FlowgateChassisSlot> oldFlowgateChassisSlots,
         List<FlowgateChassisSlot> newFlowgateChassisSlots) {
      Map<String, FlowgateChassisSlot> oldSlotsMap = new HashMap<String, FlowgateChassisSlot>();
      if(oldFlowgateChassisSlots != null) {
         for(FlowgateChassisSlot slot : oldFlowgateChassisSlots) {
            oldSlotsMap.put(slot.getMountingSide()+slot.getSlotName(), slot);
         }
      }
      Map<String, FlowgateChassisSlot> newSlotsMap = new HashMap<String, FlowgateChassisSlot>();
      if(newFlowgateChassisSlots != null) {
         for(FlowgateChassisSlot slot : newFlowgateChassisSlots) {
            newSlotsMap.put(slot.getMountingSide()+slot.getSlotName(), slot);
         }
      }
      if(oldSlotsMap.size() != newSlotsMap.size()) {
         return true;
      }
      for(Map.Entry<String, FlowgateChassisSlot> newSlotMap : newSlotsMap.entrySet()) {
         String key = newSlotMap.getKey();
         if(!oldSlotsMap.containsKey(key)) {
            return true;
         }else {
            FlowgateChassisSlot newSlot = newSlotMap.getValue();
            FlowgateChassisSlot oldSlot = oldSlotsMap.get(key);
            if(valueIsChanged(oldSlot.getMountedAssetNumber(), newSlot.getMountedAssetNumber())) {
               return true;
            }
            if(valueIsChanged(oldSlot.getColumnPosition(), newSlot.getColumnPosition())) {
               return true;
            }
            if(valueIsChanged(oldSlot.getRowPosition(), newSlot.getRowPosition())) {
               return true;
            }
         }
      }
      return false;
   }

   public boolean valueIsChanged(Integer oldValue, Integer newValue) {
      Integer value = oldValue == null ? -1 : oldValue;
      Integer comparedValue = newValue == null ? -1 : newValue;
      if(value.equals(comparedValue)) {
         return false;
      }
      return true;
   }

   public boolean valueIsChanged(String oldValue, String newValue) {
      String value = oldValue == null ? "" : oldValue;
      String comparedValue = newValue == null ? "" : newValue;
      if(value.equals(comparedValue)) {
         return false;
      }
      return true;
   }

   public List<NlyteAsset> filterUnActivedAsset(List<NlyteAsset> nlyteAssets, AssetCategory category) {
      List<NlyteAsset> assets = new ArrayList<NlyteAsset>();
      for (NlyteAsset nlyteAsset : nlyteAssets) {
         if (nlyteAsset.isTemplateRelated() || !nlyteAsset.isActived()) {
            continue;
         }
         switch (category) {
         case Server:
            if (nlyteAsset.getCabinetAssetID() <= 0) {
               continue;
            }
            break;
         case PDU:
            if (nlyteAsset.getCabinetAssetID() <= 0 && nlyteAsset.getuMounting() == null) {
               continue;
            }
            break;
         case Networks:
            if (nlyteAsset.getCabinetAssetID() <= 0) {
               continue;
            }
            break;
         case Cabinet:
         default:
            break;
         }
         assets.add(nlyteAsset);
      }
      return assets;
   }

}
