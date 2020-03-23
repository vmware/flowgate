package com.vmware.flowgate.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.stereotype.Service;

import com.couchbase.client.java.query.N1qlQuery;
import com.vmware.flowgate.common.model.ServerMapping;

@Service
public class ServerMappingService {

   @Autowired
   CouchbaseTemplate couchbaseTemplate;

   public Set<String> getUnmappedServer() {
      N1qlQuery n1ql = N1qlQuery.simple(
            "SELECT META(`f`)._ID, META(`f`)._CAS,`f`.* FROM "
            + "(SELECT META(`flowgate`).id AS _ID, META(`flowgate`).cas AS _CAS, `flowgate`.* FROM `flowgate` where `_class` = \"com.vmware.flowgate.common.model.ServerMapping\") as f "
            + "where f.`asset` IS MISSING OR f.`asset` IS NULL");
      List<ServerMapping> mappings = couchbaseTemplate.findByN1QL(n1ql, ServerMapping.class);
      Set<String> result = new HashSet<String>();
      if (mappings != null && !mappings.isEmpty()) {
         for (ServerMapping mapping : mappings) {
            if (null != mapping.getVcHostName()) {
               result.add(mapping.getVcHostName());
            } else if (null != mapping.getVroVMEntityName()) {
               result.add(mapping.getVroVMEntityName());
            }
         }
      }
      return result;
   }
}
