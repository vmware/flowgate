/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vmware.flowgate.common.model.BaseDocument;

public class BaseDocumentUtil {

   public static <T extends BaseDocument> void generateID(T document) {
      if (document.getId() == null) {
         document.setId(UUID.randomUUID().toString().replaceAll("-", ""));
      }
   }

   public static <T extends BaseDocument> void generateID(List<T> documents) {
      for (T document : documents) {
         generateID(document);
      }
   }

   public static <T> void applyChanges(T oldAsset, T newAsset) throws NoSuchFieldException,
         IllegalAccessException, SecurityException, JsonProcessingException {
      new HashMap<String, Object>();
      Class<?> oldC = oldAsset.getClass();
      for (Field fieldNew : newAsset.getClass().getDeclaredFields()) {
         Boolean accessible = fieldNew.isAccessible();
         fieldNew.setAccessible(true);
         Field fieldOld = oldC.getDeclaredField(fieldNew.getName());
         fieldOld.setAccessible(true);
         Object newValue = fieldNew.get(newAsset);
         Object oldValue = fieldOld.get(oldAsset);
         if (null != newValue) {
            if (fieldNew.get(newAsset).equals(fieldOld.get(oldAsset))) {
               continue;
            }
         } else {
            if (null == oldValue) {
               continue;
            }
         }
         fieldOld.set(oldAsset, newValue);
         fieldOld.setAccessible(accessible);
      }
   }
}
