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
    @Value("${spring.couchbase.bucket.password}")
    protected String password;

    @Override
    public String getConnectionString() {
        return "couchbase://" + hosts;
    }

    @Override
    public String getUserName() {
        return "Administrator";
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getBucketName() {
        return bucket;
    }
}
