package com.vmware.flowgate.config;

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
}
