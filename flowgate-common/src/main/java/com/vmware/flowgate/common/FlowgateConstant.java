/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common;

public class FlowgateConstant {

   public static int maxPageSize = 1000;

   public static int defaultPageSize = 20;

   public static int defaultPageNumber = 1;

   public static String serviceKey = "FLOWGATESERVICEKEY";

   public static String systemUser = "systemUser";

   public static int COUCHBASEIDLENGTH = 32;

   //Constant for justification field in asset
   public static String PDU_SOURCE = "PDU_SOURCE";

   /**
    * Unique id of asset generated by FLOWGATE
    */
   public static String PDU_ASSET_ID = "PDU_ASSET_ID";

   /**
    * Unique id of sensor generated by PowerIQ
    */
   public static String SENSOR_ID_FROM_POWERIQ = "Sensor_ID";

   /**
    * Unique id of PDU generated by PowerIQ
    */
   public static String PDU_ID_FROM_POWERIQ = "Pdu_ID";

   /**
    * Wire map info of PDU and Server
    */
   public static String PDU_PORT_FOR_SERVER = "DEVICE_PORT_FOR_SERVER";

   /**
    * Wire map info of Network and Server
    */
   public static String NETWORK_PORT_FOR_SERVER = "NETWORK_PORT_FOR_SERVER";

   public static String SEPARATOR = "_FIELDSPLIT_";

   public static String SPILIT_FLAG = ",";

   public static int MAXNUMBEROFRETRIES = 3;

   public static int DEFAULTNUMBEROFRETRIES = 0;

   public static long DEFAULTEXPIREDTIMERANGE = 90*24*3600*1000l;//three months

   public static String Role_admin = "admin";
}
