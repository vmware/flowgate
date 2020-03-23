/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.repository;

import java.util.HashMap;
import java.util.List;

import org.springframework.data.couchbase.core.query.Query;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;

/**
 * @author haoyul
 *
 */
public interface SystemSummaryRepository
      extends CouchbasePagingAndSortingRepository<HashMap<String,Object>, String>{
   public static final String hackid = "hid";
   public static final int cas = 89657;

   @Query("SELECT COUNT(_class) AS count FROM #{#n1ql.bucket} WHERE _class = $1")
   int countByClass(String classpath);

   @Query("SELECT COUNT(1) AS count FROM #{#n1ql.bucket} WHERE _class = $1 and type = $2")
   int countByClassAndType(String classpath,String value);

   @Query("SELECT COUNT(1) AS count FROM #{#n1ql.bucket} WHERE _class = 'com.vmware.flowgate.common.model.ServerMapping' and vcID = $1")
   int countServerMappingByVC(String vcID);

   @Query("SELECT COUNT(1) AS count FROM #{#n1ql.bucket} WHERE _class = 'com.vmware.flowgate.common.model.ServerMapping' and vroID = $1")
   int countServerMappingByVRO(String vroID);

   @Query("SELECT count(category) AS count,category,\""+hackid+"\" as _ID, "+cas+" as _CAS FROM #{#n1ql.bucket} WHERE _class = 'com.vmware.flowgate.common.model.Asset' GROUP BY category")
   List<HashMap<String,Object>> countAssetGroupByType();

   @Query("SELECT count(subCategory) AS count, subCategory, \""+hackid+"\" as _ID, "+cas+" as _CAS FROM #{#n1ql.bucket} WHERE _class = 'com.vmware.flowgate.common.model.Asset' AND category = 'Sensors' GROUP BY subCategory")
   List<HashMap<String,Object>> countSensorGroupByType();

   @Query("SELECT count(category) AS count, category, \""+hackid+"\" as _ID, "+cas+" as _CAS FROM #{#n1ql.bucket} WHERE _class = 'com.vmware.flowgate.common.model.Asset' AND assetSource like $1 GROUP BY category")
   List<HashMap<String,Object>> countAssetGroupByTypeAndSource(String assetSource);

}
