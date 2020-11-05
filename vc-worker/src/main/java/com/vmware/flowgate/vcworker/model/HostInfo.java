package com.vmware.flowgate.vcworker.model;

import java.util.Calendar;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class HostInfo {
   
   private boolean maintenanceModeSupported; // is maintenance mode supported
   private boolean rebootSupported; // Flag indicating whether rebooting the host is supported
   private String connectionState; // The host connection state. (connected / disconnected / notResponding)
   private String powerState; // The host power state. ( poweredOff / poweredOn / standBy / unknown)
   private boolean rebootRequired; //Indicates whether or not the host requires a reboot due to a configuration change.

   /*The maximum number of virtual machines that can be run on the host. An unset value indicates that the value could not be obtained.
   this value is the minimum of
   (i) the maximum number supported by the hardware.
   (ii) the maximum number permitted by the host license.*/
   private int maxRunningVms;

   /*The maximum number of virtual CPUs that can be run on the host. An unset value indicates that the value could not be obtained.
   this value is the minimum of
   (i) the maximum number supported by the hardware and
   (ii) the maximum number permitted by the host license. */
   private int maxSupportedVcpus;

   /*The maximum number of registered virtual machines supported by the host.
   If this capability is not set, the number is unknown.*/
   private int maxRegisteredVMs;// The maximum number of registered virtual machines supported by the host. 
   private long uptime; // The system uptime of the host in seconds.
   private String hypervisorVersion; //Dot-separated version string. eg. 6.7.0
   private String hypervisorBuildVersion; //Build string for the server on which this call is made. eg. 10302608
   private String hypervisorFullName; //The complete product name, including the version information. eg. "VMware ESXi 6.7.0 build-10302608"
   private String hypervisorLicenseProductName; //The license product name. eg. "VMware ESX Server"
   private String hypervisorLicenseProductVersion; //The license product version. eg. "6.0"
   private String hypervisorVendor; //Name of the vendor of this product. eg. "VMware, Inc."
   @JsonIgnore
   private String hostModel; //The system model identification.  eg. "PowerEdge T430"
   private long bootTime; // The time when the host was booted.
   private short cpuTotalCores; // Number of physical CPU cores on the host. Physical CPU cores are the processors contained by a CPU package.
   
   /*Number of physical CPU packages on the host. Physical CPU packages are chips that contain one or more processors. 
   Processors contained by a package are also known as CPU cores. For example, 
   one dual-core package is comprised of one chip that contains two CPU cores.*/
   private short cpuTotalPackages;
   private short cpuTotalThreads; //Number of physical CPU threads on the host.
   
   /*The speed of the CPU cores. This is an average value if there are multiple speeds. 
    * The product of cpuMhz and numCpuCores is approximately equal to the sum of the MHz for all the individual cores on the host.
    */
   private int singleCoreCpuMhz; 
   private long memoryCapacity; // The physical memory size in bytes.
   private long diskCapacity; //Maximum capacity of this datastore, in bytes. This value is updated periodically by the server. It can be explicitly refreshed with the Refresh operation. 
   private List<HostNic> hostNics; // nics of host
   private EsxiMetadata esxiMetadata; // esxi metadata

   public boolean isMaintenanceModeSupported() {
      return maintenanceModeSupported;
   }

   public void setMaintenanceModeSupported(boolean maintenanceModeSupported) {
      this.maintenanceModeSupported = maintenanceModeSupported;
   }

   public boolean isRebootSupported() {
      return rebootSupported;
   }

   public void setRebootSupported(boolean rebootSupported) {
      this.rebootSupported = rebootSupported;
   }

   public String getConnectionState() {
      return connectionState;
   }

   public void setConnectionState(String connectionState) {
      this.connectionState = connectionState;
   }

   public String getPowerState() {
      return powerState;
   }

   public void setPowerState(String powerState) {
      this.powerState = powerState;
   }

   public boolean isRebootRequired() {
      return rebootRequired;
   }

   public void setRebootRequired(boolean rebootRequired) {
      this.rebootRequired = rebootRequired;
   }

   public int getMaxRunningVms() {
      return maxRunningVms;
   }

   public void setMaxRunningVms(int maxRunningVms) {
      this.maxRunningVms = maxRunningVms;
   }

   public int getMaxSupportedVcpus() {
      return maxSupportedVcpus;
   }

   public void setMaxSupportedVcpus(int maxSupportedVcpus) {
      this.maxSupportedVcpus = maxSupportedVcpus;
   }

   public int getMaxRegisteredVMs() {
      return maxRegisteredVMs;
   }

   public void setMaxRegisteredVMs(int maxRegisteredVMs) {
      this.maxRegisteredVMs = maxRegisteredVMs;
   }

   public long getUptime() {
      return uptime;
   }

   public void setUptime(long uptime) {
      this.uptime = uptime;
   }

   public String getHypervisorVersion() {
      return hypervisorVersion;
   }

   public void setHypervisorVersion(String hypervisorVersion) {
      this.hypervisorVersion = hypervisorVersion;
   }

   public String getHypervisorBuildVersion() {
      return hypervisorBuildVersion;
   }

   public void setHypervisorBuildVersion(String hypervisorBuildVersion) {
      this.hypervisorBuildVersion = hypervisorBuildVersion;
   }

   public String getHypervisorFullName() {
      return hypervisorFullName;
   }

   public void setHypervisorFullName(String hypervisorFullName) {
      this.hypervisorFullName = hypervisorFullName;
   }

   public String getHypervisorLicenseProductName() {
      return hypervisorLicenseProductName;
   }

   public void setHypervisorLicenseProductName(String hypervisorLicenseProductName) {
      this.hypervisorLicenseProductName = hypervisorLicenseProductName;
   }

   public String getHypervisorLicenseProductVersion() {
      return hypervisorLicenseProductVersion;
   }

   public void setHypervisorLicenseProductVersion(String hypervisorLicenseProductVersion) {
      this.hypervisorLicenseProductVersion = hypervisorLicenseProductVersion;
   }

   public String getHypervisorVendor() {
      return hypervisorVendor;
   }

   public void setHypervisorVendor(String hypervisorVendor) {
      this.hypervisorVendor = hypervisorVendor;
   }

   public String getHostModel() {
      return hostModel;
   }

   public void setHostModel(String hostModel) {
      this.hostModel = hostModel;
   }

   public long getBootTime() {
      return bootTime;
   }

   public void setBootTime(long bootTime) {
      this.bootTime = bootTime;
   }

   public short getCpuTotalCores() {
      return cpuTotalCores;
   }

   public void setCpuTotalCores(short cpuTotalCores) {
      this.cpuTotalCores = cpuTotalCores;
   }

   public short getCpuTotalPackages() {
      return cpuTotalPackages;
   }

   public void setCpuTotalPackages(short cpuTotalPackages) {
      this.cpuTotalPackages = cpuTotalPackages;
   }

   public short getCpuTotalThreads() {
      return cpuTotalThreads;
   }

   public void setCpuTotalThreads(short cpuTotalThreads) {
      this.cpuTotalThreads = cpuTotalThreads;
   }

   public int getSingleCoreCpuMhz() {
      return singleCoreCpuMhz;
   }

   public void setSingleCoreCpuMhz(int singleCoreCpuMhz) {
      this.singleCoreCpuMhz = singleCoreCpuMhz;
   }

   public long getMemoryCapacity() {
      return memoryCapacity;
   }

   public void setMemoryCapacity(long memoryCapacity) {
      this.memoryCapacity = memoryCapacity;
   }

   public long getDiskCapacity() {
      return diskCapacity;
   }

   public void setDiskCapacity(long diskCapacity) {
      this.diskCapacity = diskCapacity;
   }

   public List<HostNic> getHostNics() {
      return hostNics;
   }

   public void setHostNics(List<HostNic> hostNics) {
      this.hostNics = hostNics;
   }

   public EsxiMetadata getEsxiMetadata() {
      return esxiMetadata;
   }

   public void setEsxiMetadata(EsxiMetadata esxiMetadata) {
      this.esxiMetadata = esxiMetadata;
   }

}
