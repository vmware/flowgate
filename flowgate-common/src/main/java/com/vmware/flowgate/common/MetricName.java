package com.vmware.flowgate.common;

public class MetricName {
   public static final String TEMPERATURE = "Temperature";
   public static final String HUMIDITY = "Humidity";
   public static final String AIRPRESSURE = "Airpressure";
   public static final String CONTACTCLOSURE = "Contactclousre";
   public static final String SMOKE = "Smoke";
   public static final String VIBRATION = "Vibraion";
   public static final String WATER_FLOW = "WaterFlow";
   public static final String AIR_FLOW = "AirFlow";

   public static final String PDU_VOLTAGE = "Voltage";
   public static final String PDU_ACTIVE_POWER = "ActivePower";
   public static final String PDU_APPARENT_POWER = "ApparentPower";
   public static final String PDU_CURRENT = "Current";
   public static final String PDU_FREE_CAPACITY = "FreeCapacity";
   public static final String PDU_TOTAL_CURRENT = "Current";
   public static final String PDU_TOTAL_POWER = "Power";
   public static final String PDU_CURRENT_LOAD = "CurrentLoad";
   public static final String PDU_POWER_LOAD = "PowerLoad";
   public static final String PDU_TEMPERATURE = "Temperature";
   public static final String PDU_HUMIDITY = "Humidity";

   //Formula key for pdu asset
   public static final String PDU_XLET_ACTIVE_POWER = "%s|ActivePower";
   public static final String PDU_XLET_APPARENT_POWER = "%s|ApparentPower";
   public static final String PDU_XLET_FREE_CAPACITY = "%s|FreeCapacity";
   public static final String PDU_XLET_CURRENT = "%s|Current";
   public static final String PDU_XLET_VOLTAGE = "%s|Voltage";
   public static final String PDU_INLET_XPOLE_CURRENT = "%s|%s|Current";
   public static final String PDU_INLET_XPOLE_FREE_CAPACITY = "%s|%s|FreeCapacity";
   public static final String PDU_INLET_XPOLE_VOLTAGE = "%s|%s|Voltage";

   public static final String SERVER_FRONT_TEMPERATURE = "FrontTemperature";
   public static final String SERVER_BACK_TEMPREATURE = "BackTemperature";
   public static final String SERVER_FRONT_HUMIDITY = "FrontHumidity";
   public static final String SERVER_BACK_HUMIDITY = "BackHumidity";
   public static final String SERVER_TOTAL_CURRENT = "TotalCurrent";
   public static final String SERVER_TOTAL_POWER = "TotalPower";
   public static final String SERVER_VOLTAGE = "Voltage";
   public static final String SERVER_CONNECTED_PDU_CURRENT = "Current";
   public static final String SERVER_CONNECTED_PDU_POWER = "Power";
   public static final String SERVER_CONNECTED_PDU_CURRENT_LOAD = "CurrentLoad";
   public static final String SERVER_CONNECTED_PDU_POWER_LOAD = "PowerLoad";
   public static final String SERVER_USED_PDU_OUTLET_CURRENT = "%s|Current";
   public static final String SERVER_USED_PDU_OUTLET_POWER = "%s|Power";
   public static final String SERVER_USED_PDU_OUTLET_VOLTAGE = "%s|Voltage";
   public static final String SERVER_ENERGY_CONSUMPTION = "EnergyConsumption";//(KWH)
   public static final String SERVER_AVERAGE_USED_POWER = "AveragePower";//(KW)
   public static final String SERVER_PEAK_USED_POWER = "PeakPower";//(KW)
   public static final String SERVER_MINIMUM_USED_POWER = "MinimumPower";//(KW)
   public static final String SERVER_AVERAGE_TEMPERATURE = "AverageTemperature";//(celsius)
   public static final String SERVER_TEMPERATURE = "Temperature";//(celsius)
   public static final String SERVER_PEAK_TEMPERATURE = "PeakTemperature";//(celsius)
   public static final String SERVER_POWER = "Power";

   public static final String SERVER_CPUUSEDINMHZ = "CpuUsedInMhz";//(MHZ)
   public static final String SERVER_CPUUSAGE = "CpuUsage";//(Percentage)
   public static final String SERVER_ACTIVEMEMORY = "ActiveMemory";//(KB)
   public static final String SERVER_SWAPMEMORY = "SwapMemory";//(KB)
   public static final String SERVER_SHAREDMEMORY = "SharedMemory";//(KB)
   public static final String SERVER_BALLOONMEMORY = "BalloonMemory";//(KB)
   public static final String SERVER_CONSUMEDMEMORY = "ConsumedMemory";//(KB)
   public static final String SERVER_MEMORYUSAGE = "MemoryUsage";//(Percentage)
   public static final String SERVER_STORAGEUSED = "StorageUsed";//(MB)
   public static final String SERVER_STORAGEUSAGE = "StorageUsage";//(Percentage)
   public static final String SERVER_STORAGEIORATEUSAGE = "StorageIORateUsage";//(KBps)
   public static final String SERVER_NETWORKUTILIZATION = "NetworkUtilization";//(KBps)Combined transmit-rates and receive-rates

}
