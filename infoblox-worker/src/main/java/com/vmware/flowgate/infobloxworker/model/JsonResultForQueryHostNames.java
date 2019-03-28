/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.infobloxworker.model;

import java.util.List;

public class JsonResultForQueryHostNames {
	private List<Infoblox> result;
	
	public List<Infoblox> getResult() {
		return result;
	}
	public void setResult(List<Infoblox> result) {
        this.result = result;
    }
}
