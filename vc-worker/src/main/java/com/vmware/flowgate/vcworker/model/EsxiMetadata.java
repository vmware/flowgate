package com.vmware.flowgate.vcworker.model;

public class EsxiMetadata {

   private String instanceId; //instanceid for vcenter.
   private String clusterMobid; //mobid for cluster.
   private String hostMobid; //mobid for host.
   private String hostName; //name of host.
   private String clusterName; //name of cluster.
   private int clusterTotalCpuCores; //Number of physical CPU cores.
   private int clusterTotalCpuThreads; //Aggregated number of CPU threads.
   private int clusterEffectiveHostsNum; //Total number of effective hosts.
   private int clusterHostsNum; //Total number of hosts.
   private int clusterTotalCpu; // Aggregated CPU resources of all hosts, in MHz.
   private long clusterTotalMemory; // Aggregated memory resources of all hosts, in bytes.
   private boolean clusterDPMenabled; // DPM status.
   private String clusterDRSBehavior;// fullyAutomated / manual / partiallyAutomated
   private boolean clusterVSANenabled; // VSAN status.
   private boolean hostDPMenabled; // DPM of host status.
   private boolean hostVSANenabled; // VSAN of host status.
   private boolean hostVsanSupported; // VSAN of host is supported.

   public String getInstanceId() {
      return instanceId;
   }

   public void setInstanceId(String instanceId) {
      this.instanceId = instanceId;
   }

   public String getClusterMobid() {
      return clusterMobid;
   }

   public void setClusterMobid(String clusterMobid) {
      this.clusterMobid = clusterMobid;
   }

   public String getHostMobid() {
      return hostMobid;
   }

   public void setHostMobid(String hostMobid) {
      this.hostMobid = hostMobid;
   }

   public String getHostName() {
      return hostName;
   }

   public void setHostName(String hostName) {
      this.hostName = hostName;
   }

   public String getClusterName() {
      return clusterName;
   }

   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   public int getClusterTotalCpuCores() {
      return clusterTotalCpuCores;
   }

   public void setClusterTotalCpuCores(int clusterTotalCpuCores) {
      this.clusterTotalCpuCores = clusterTotalCpuCores;
   }

   public int getClusterTotalCpuThreads() {
      return clusterTotalCpuThreads;
   }

   public void setClusterTotalCpuThreads(int clusterTotalCpuThreads) {
      this.clusterTotalCpuThreads = clusterTotalCpuThreads;
   }

   public int getClusterEffectiveHostsNum() {
      return clusterEffectiveHostsNum;
   }

   public void setClusterEffectiveHostsNum(int clusterEffectiveHostsNum) {
      this.clusterEffectiveHostsNum = clusterEffectiveHostsNum;
   }

   public int getClusterHostsNum() {
      return clusterHostsNum;
   }

   public void setClusterHostsNum(int clusterHostsNum) {
      this.clusterHostsNum = clusterHostsNum;
   }

   public int getClusterTotalCpu() {
      return clusterTotalCpu;
   }

   public void setClusterTotalCpu(int clusterTotalCpu) {
      this.clusterTotalCpu = clusterTotalCpu;
   }

   public long getClusterTotalMemory() {
      return clusterTotalMemory;
   }

   public void setClusterTotalMemory(long clusterTotalMemory) {
      this.clusterTotalMemory = clusterTotalMemory;
   }

   public boolean isClusterDPMenabled() {
      return clusterDPMenabled;
   }

   public void setClusterDPMenabled(boolean clusterDPMenabled) {
      this.clusterDPMenabled = clusterDPMenabled;
   }

   public String getClusterDRSBehavior() {
      return clusterDRSBehavior;
   }

   public void setClusterDRSBehavior(String clusterDRSBehavior) {
      this.clusterDRSBehavior = clusterDRSBehavior;
   }

   public boolean isClusterVSANenabled() {
      return clusterVSANenabled;
   }

   public void setClusterVSANenabled(boolean clusterVSANenabled) {
      this.clusterVSANenabled = clusterVSANenabled;
   }

   public boolean isHostDPMenabled() {
      return hostDPMenabled;
   }

   public void setHostDPMenabled(boolean hostDPMenabled) {
      this.hostDPMenabled = hostDPMenabled;
   }

   public boolean isHostVSANenabled() {
      return hostVSANenabled;
   }

   public void setHostVSANenabled(boolean hostVSANenabled) {
      this.hostVSANenabled = hostVSANenabled;
   }

   public boolean isHostVsanSupported() {
      return hostVsanSupported;
   }

   public void setHostVsanSupported(boolean hostVsanSupported) {
      this.hostVsanSupported = hostVsanSupported;
   }

}
