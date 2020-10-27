package com.vmware.flowgate.vcworker.model;

import javax.annotation.Generated;

public class HostRealtimeData {

   private int cpuUsedInMhz;// CPU usage in megahertz during the interval
   private int cpuUsage; // CPU usage as a percentage during the interval
   private long ActiveMemory;//(KB) Amount of guest physical memory that is being actively read or written by guest. Activeness is estimated by ESXi
   private long SwapMemory;//(KB) Swap storage space consumed
   private long SharedMemory;//(KB) Amount of host physical memory that backs shared guest physical memory (Shared)
   private long BalloonMemory;//(KB) Amount of guest physical memory reclaimed from the virtual machine by the balloon driver in the guest
   private long ConsumedMemory;//(KB) Amount of host physical memory consumed for backing up guest physical memory pages
   private int memoryUsage; // Percentage of host physical memory that has been consumed
   private long storageUsed;//(MB) Amount of host total storage usage
   private int storageUsage; // Percentage of host total storage usage
   private long storageIORateUsage; // (KBps) Aggregated disk I/O rate. For hosts, this metric includes the rates for all virtual machines running on the host during the collection interval.
   private long networkUtilization; // (KBps) Network utilization (combined transmit-rates and receive-rates) during the interval

   public int getCpuUsedInMhz() {
      return cpuUsedInMhz;
   }

   public void setCpuUsedInMhz(int cpuUsedInMhz) {
      this.cpuUsedInMhz = cpuUsedInMhz;
   }

   public int getCpuUsage() {
      return cpuUsage;
   }

   public void setCpuUsage(int cpuUsage) {
      this.cpuUsage = cpuUsage;
   }

   public long getActiveMemory() {
      return ActiveMemory;
   }

   public void setActiveMemory(long activeMemory) {
      ActiveMemory = activeMemory;
   }

   public long getSwapMemory() {
      return SwapMemory;
   }

   public void setSwapMemory(long swapMemory) {
      SwapMemory = swapMemory;
   }

   public long getSharedMemory() {
      return SharedMemory;
   }

   public void setSharedMemory(long sharedMemory) {
      SharedMemory = sharedMemory;
   }

   public long getBalloonMemory() {
      return BalloonMemory;
   }

   public void setBalloonMemory(long balloonMemory) {
      BalloonMemory = balloonMemory;
   }

   public long getConsumedMemory() {
      return ConsumedMemory;
   }

   public void setConsumedMemory(long consumedMemory) {
      ConsumedMemory = consumedMemory;
   }

   public int getMemoryUsage() {
      return memoryUsage;
   }

   public void setMemoryUsage(int memoryUsage) {
      this.memoryUsage = memoryUsage;
   }

   public long getStorageUsed() {
      return storageUsed;
   }

   public void setStorageUsed(long storageUsed) {
      this.storageUsed = storageUsed;
   }

   public int getStorageUsage() {
      return storageUsage;
   }

   public void setStorageUsage(int storageUsage) {
      this.storageUsage = storageUsage;
   }

   public long getStorageIORateUsage() {
      return storageIORateUsage;
   }

   public void setStorageIORateUsage(long storageIORateUsage) {
      this.storageIORateUsage = storageIORateUsage;
   }

   public long getNetworkUtilization() {
      return networkUtilization;
   }

   public void setNetworkUtilization(long networkUtilization) {
      this.networkUtilization = networkUtilization;
   }


}
