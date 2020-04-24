/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

   @Autowired
   private StringRedisTemplate redisTemplate;

   public boolean opsForSetToAdd(String key, int timeout, String[] values) {
      redisTemplate.opsForSet().add(key, values);
      if(redisTemplate.hasKey(key)) {
         redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
         return true;
      }
      return false;
   }

   public boolean isMember(String key, String content) {
      return redisTemplate.opsForSet().isMember(key, content);
   }

   public long getValueSize(String key) {
      return redisTemplate.opsForSet().size(key);
   }

   public boolean hasKey(String key) {
      return redisTemplate.hasKey(key);
   }

   public List<String> scan(String key, ScanOptions options){
      List<String> result = new ArrayList<String>();
      Cursor<String> curosr = redisTemplate.opsForSet().scan(key, options);
      while (curosr.hasNext()) {
         result.add(curosr.next());
      }
      return result;
   }
}
