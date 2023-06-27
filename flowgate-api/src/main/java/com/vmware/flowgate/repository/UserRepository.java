/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import org.springframework.data.couchbase.repository.CouchbaseRepository;
import com.vmware.flowgate.common.model.WormholeUser;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends
            CouchbaseRepository<WormholeUser, String> {
   WormholeUser findOneByUserName(String userName);
}
