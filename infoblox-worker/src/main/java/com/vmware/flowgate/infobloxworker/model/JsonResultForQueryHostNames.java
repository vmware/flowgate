/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.infobloxworker.model;

import java.util.List;

public class JsonResultForQueryHostNames {
	private List<InfobloxIpv4addressItem> result;
	
	public List<InfobloxIpv4addressItem> getResult() {
		return result;
	}
	public void setResult(List<InfobloxIpv4addressItem> result) {
        this.result = result;
    }
}
