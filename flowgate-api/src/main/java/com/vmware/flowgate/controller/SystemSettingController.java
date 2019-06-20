package com.vmware.flowgate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.exception.WormholeRequestException;

@RestController
@RequestMapping("/v1/setting")
public class SystemSettingController {

   @Autowired
   StringRedisTemplate template;

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/datapersistenttime", method = RequestMethod.GET)
   public Long getExpiredTimeRange() {
      long expiredTimeRange = 0l;
      String expiredTimeRangeValue = template.opsForValue().get(EventMessageUtil.EXPIREDTIMERANGE);
      if(expiredTimeRangeValue != null) {
         expiredTimeRange = Long.valueOf(expiredTimeRangeValue);
      }else {
         expiredTimeRange = FlowgateConstant.DEFAULTEXPIREDTIMERANGE;
      }
      return expiredTimeRange;
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(value = "/datapersistenttime/{time}", method = RequestMethod.PUT)
   public void updateExpiredTimeRange(@PathVariable("time") Long time) {
      if(time < FlowgateConstant.DEFAULTEXPIREDTIMERANGE) {
         throw new WormholeRequestException("Expired time range must more than 90 days.");
      }
      template.opsForValue().set(EventMessageUtil.EXPIREDTIMERANGE, String.valueOf(time));
   }
}
