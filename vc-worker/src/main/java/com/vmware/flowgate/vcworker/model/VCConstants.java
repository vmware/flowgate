/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vcworker.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VCConstants {
   public static final String LOCATION_REGION = "Location.Region";
   public static final String LOCATION_COUNTRY = "Location.Country";
   public static final String LOCATION_CITY = "Location.City";
   public static final String LOCATION_BUILDING = "Location.Building";
   public static final String LOCATION_FLOOR = "Location.Floor";
   public static final String LOCATION_ROOM = "Location.Room";
   public static final String LOCATION_CABINET = "Location.Cabinet";
   public static final String LOCATION_CABINET_NUMBER = "Location.Cabinetnumber";
   public static final String ASSET_ASSETNAME = "Asset.AssetName";
   public static final String ASSET_SERIALNUMBER = "Asset.SerialNumber";
   public static final String ASSET_PDUs = "Asset.PDUs";
   public static final String ASSET_SWITCHs ="Asset.Switches";
   
   public static final String HOSTSYSTEM = "HostSystem";
   public static final String CLUSTERCOMPUTERESOURCE = "ClusterComputeResource";
   
   public static final String SDKURL = "https://%s/sdk";

   public static final Map<String, String> hostCustomAttrMapping;
   static {
      Map<String, String> kvMap = new HashMap<String, String>();
      kvMap.put("region", VCConstants.LOCATION_REGION);
      kvMap.put("country", VCConstants.LOCATION_COUNTRY);
      kvMap.put("city", VCConstants.LOCATION_CITY);
      kvMap.put("building", VCConstants.LOCATION_BUILDING);
      kvMap.put("floor", VCConstants.LOCATION_FLOOR);
      kvMap.put("room", VCConstants.LOCATION_ROOM);
      kvMap.put("cabinetName", VCConstants.LOCATION_CABINET);
      kvMap.put("cabinetUnitPosition", VCConstants.LOCATION_CABINET_NUMBER);
      kvMap.put("assetName", VCConstants.ASSET_ASSETNAME);

      hostCustomAttrMapping = Collections.unmodifiableMap(kvMap);
   }


   public static String categoryName = "FlowgateAsset";
   public static String categoryDescription = "All datacenter assets enhanced by Flowgate";
   public static String locationAntiAffinityTagName = "Cluster host location anti-affinity";
   public static String locationAntiAffinityTagDescription =
         "This host in the cluster valiate location anti-affinity rules";

   public static final Map<String, String> predefinedTags;
   static {
      Map<String, String> map = new HashMap<String, String>();
      map.put(locationAntiAffinityTagName, locationAntiAffinityTagDescription);
      predefinedTags = Collections.unmodifiableMap(map);
   }

}
