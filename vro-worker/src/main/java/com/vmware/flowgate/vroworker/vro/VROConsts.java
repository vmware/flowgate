/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker.vro;

public class VROConsts {
   public static final String ADPTERKIND_VMARE_KEY = "VMWARE";
   public static final String RESOURCEKIND_HOSTSYSTEM_KEY = "HostSystem";
   public static final String RESOURCEKIND_VIRTUALVM_KEY = "VirtualMachine";

   // public static final String ALERT_DEFINITION_TEMPERATURE_ID="AlertDefinition-VMWARE-High-Host-Temperature";
   public static final String ALERT_DEFINITION_TEMPERATURE_NAME = "High host temperature";
   public static final String ALERT_DEFINITION_TEMPERATURE_DESCRIPTION =
         "This Alert will be triggerd when the host's front or back panel temperature is above the threshold.";
   public static final String ALERT_DEFINITION_HUMIDITY_NAME = "The humidity is abnormal";
   public static final String ALERT_DEFINITION_HUMIDITY_DESCRIPTION =
         "The environment huimdity is above the threshold";
   public static final String ALERT_DEFINITION_PDU_POWER_NAME = "The PDU load is too high";
   public static final String ALERT_DEFINITION_PDU_POWER_DESCRIPTION =
         "The alert will triggered when the PDU AMPS load is above the defined shreshold";
   public static final String ALTER_DEFINITION_POWER = "High_Host_Power";
   //public static final String SYMPTOM_HOSTSYSTEM_TEMPERATURE_ID="SymptomDefinition-VMWARE-HostTemperatureStatus";
   public static final String SYMPTOM_HOSTSYSTEM_FRONT_TEMPERATURE_NAME =
         "Host front panel temperature is too high";
   public static final String SYMPTOM_HOSTSYSTEM_BACK_TEMPERATURE_NAME =
         "Host back panel temperature is too high";
   public static final String SYMPTOM_HOSTSYSTEM_FRONT_HUMIDITY_NAME =
         "Host front panel humidity is too high";
   public static final String SYMPTOM_HOSTSYSTEM_BACK_HUMIDITY_NAME =
         "Host back panel humidity is too high";
   public static final String SYMPTOM_HOSTSYSTEM_POWER_NAME = "The connected PDU is over loaded";


   public static final String ENVRIONMENT_FRONT_TEMPERATURE_METRIC =
         "Environment|FrontTemperature(°C)";
   public static final String ENVRIONMENT_BACK_TEMPERATURE_METRIC =
         "Environment|BackTemperature(°C)";
   public static final String ENVRIONMENT_HOSTSENSOR_TEMPERATURE_METRIC =
         "Environment|BiosTemperature(°C)";
   public static final String ENVRIONMENT_PDU_AMPS_LOAD_METRIC = "Environment|PDU AMPS(%)";
   public static final String ENVRIONMENT_PDU_POWER_LOAD_METRIC = "Environment|PDU POWER(%)";
   public static final String ENVRIONMENT_PDU_POWER_METRIC = "Environment|PDU Power";
   public static final String ENVRIONMENT_PDU_AMPS_METRIC = "Environment|PDU AMPS";
   public static final String ENVRIONMENT_PDU_VOLTS_METRIC = "Environment|PDU VOLTS";
   public static final String ENVRIONMENT_HOST_AMPS_METRIC = "Environment|AMPS(A)";
   public static final String ENVRIONMENT_HOST_VOLTS_METRIC = "Environment|VOLTS(V)";
   public static final String ENVRIONMENT_FRONT_HUMIDITY_METRIC = "Environment|FrontHumidity(%)";
   public static final String ENVRIONMENT_BACK_HUMIDITY_METRIC = "Environment|BackHumidity(%)";

   public static final String LOCATION_REGION = "Location|Region";
   public static final String LOCATION_COUNTRY = "Location|Country";
   public static final String LOCATION_CITY = "Location|City";
   public static final String LOCATION_BUILDING = "Location|Building";
   public static final String LOCATION_FLOOR = "Location|Floor";
   public static final String LOCATION_ROOM = "Location|Room";
   public static final String LOCATION_ROW = "Location|Row";
   public static final String LOCATION_COL = "Location|Col";
   public static final String LOCATION_CABINET = "Location|Cabinet";
   public static final String LOCATION_CABINET_NUMBER = "Location|CabinetUNumber";

   public static final String ENVRIONMENT_TEMPERATURE_METRIC_MAX = "35";
   public static final String ENVRIONMENT_HUMIDITY_METRIC_MAX = "75"; // 75%
   public static final String ENVRIONMENT_PDU_AMPS_METRIC_MAX = "67"; //66.67%
   //this mean we will never overload by default. It will only be used to
   //create the default symptom which need to update as soon as the facility and server mapping created.
   public static final String ENVRIONMENT_POWER_METRIC_MAX = "10000";
  // public static final String RECOMMENDATION_ACTION_MOVE_VM = "Move VM to other hosts";

   public static final String RECOMMENDATION_ACTION_ADAPTER_VCENTER =
         "VMWARE";
   public static final String RECOMMENDATION_MOVEVM_TO_OTHERHOSTS_DESCRIPTION =
         "Move All virtual machines on this host to a different host which connect to different PDU and placed in different Cabinet unit";
   public static final String RECOMMENDATION_POWER_OFF_VM_DESCRIPTION =
         "Power off or migrate the unused/less important VMs to reduce the PDU load.";

   public static String VROSDKURL = "https://%s/suite-api";
}
