/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.flowgate.config;

import com.couchbase.client.java.query.QueryScanConsistency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;

@Configuration
@EnableCouchbaseRepositories(basePackages = {"com.vmware.flowgate.repository"})
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {

    @Value("${spring.couchbase.bootstrap-hosts}")
    protected String hosts;

    @Value("${spring.couchbase.bucket.name}")
    protected String bucket;

    @Value("${spring.couchbase.username:flowgate}")
    protected String username;

    @Value("${spring.couchbase.bucket.password}")
    protected String password;

    @Value("${spring.data.couchbase.auto-index}")
    protected boolean autoindex;

    @Value("${spring.couchbase.scan-consistency:not_bounded}")
    protected String consistency;

    @Override
    public String getConnectionString() {
        return hosts;
    }

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getBucketName() {
        return bucket;
    }

    @Override
    protected boolean autoIndexCreation() {
        return autoindex;
    }

    @Override
    public QueryScanConsistency getDefaultConsistency() {
        return consistency.equals(QueryScanConsistency.REQUEST_PLUS.toString()) ?
                    QueryScanConsistency.REQUEST_PLUS : QueryScanConsistency.NOT_BOUNDED;
    }
}
