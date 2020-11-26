/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.infobloxworker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InfobloxIpv4addressItem {

    @JsonProperty(value="_ref")
    private String ref;
    @JsonProperty(value="ip_address")
    private String ipAddress;
    @JsonProperty(value="is_conflict")
    private boolean isConflict;
    @JsonProperty(value="mac_address")
    private String macAddress;
    @JsonProperty(value="names")
    private String[] hostNames;
    @JsonProperty(value="network")
    private String network;
    @JsonProperty(value="network_view")
    private String networkView;
    @JsonProperty(value="objects")
    private String[] objects;
    @JsonProperty(value="status")
    private String status;
    @JsonProperty(value="types")
    private String[] types;
    @JsonProperty(value="usage")
    private String[] usage;
    @JsonProperty(value = "discovered_data")
    private DiscoveredData discoveredData;

    public String get_ref(){
        return ref;
    }
    public String getIpAddress() {
        return ipAddress;
    }
    public boolean getIsConflict() {
        return isConflict;
    }
    public String getMacAddress() {
        return macAddress;
    }
    public String[] getHostNames() {
        return hostNames;
    }
    public String getNetwork() {
        return network;
    }
    public String getNetworkView() {
        return networkView;
    }
    public String[] getObjects() {
        return objects;
    }
    public String getStatus() {
        return status;
    }
    public String[] getTypes() {
        return types;
    }
    public String[] getUsage() {
        return usage;
    }
    public DiscoveredData getDiscoveredData() {
        return discoveredData;
    }
    public void set_ref(String ref){
        this.ref = ref;
    }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    public void setIsConflict(boolean isConflict) {
        this.isConflict = isConflict;
    }
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    public void setHostNames(String[] hostNames) {
        this.hostNames = hostNames;
    }
    public void setNetwork(String network) {
        this.network = network;
    }
    public void setNetworkView(String networkView) {
        this.networkView = networkView;
    }
    public void setObjects(String[] objects) {
        this.objects = objects;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public void setTypes(String[] types) {
        this.types = types;
    }
    public void setUsage(String[] usage) {
        this.usage = usage;
    }
    public void setDiscoveredData(DiscoveredData discoveredData) {
        this.discoveredData = discoveredData;
    }

}