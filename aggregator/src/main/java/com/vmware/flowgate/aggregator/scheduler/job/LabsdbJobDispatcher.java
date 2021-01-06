package com.vmware.flowgate.aggregator.scheduler.job;

import java.io.IOException;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig.SoftwareType;
import com.vmware.flowgate.common.model.redis.message.EventType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;
import com.vmware.flowgate.jobs.BaseJob;

public class LabsdbJobDispatcher extends BaseJob implements Job {

   @Autowired
   private WormholeAPIClient restClient;

   @Autowired
   private ServiceKeyConfig serviceKeyConfig;

   @Autowired
   private StringRedisTemplate template;

   @Autowired
   private MessagePublisher publisher;

   private static final Logger logger = LoggerFactory.getLogger(LabsdbJobDispatcher.class);

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {
      // TODO Auto-generated method stub
      //every day we will trigger a sync unmapped data job.
      //every week for full sync job.
      String execountString = template.opsForValue().get(EventMessageUtil.LABSDB_EXECOUNT);
      if (execountString == null || "".equals(execountString)) {
         execountString = "0";
      }
      long execount = Long.valueOf(execountString);
      restClient.setServiceKey(serviceKeyConfig.getServiceKey());
      boolean fullSync = execount++ % 7 == 0;
      logger.info("Send Sync command for Labsdb");
      try {
         template.opsForValue().set(EventMessageUtil.LABSDB_EXECOUNT, String.valueOf(execount));
      }catch(Exception e) {
         logger.error("Failed to set execount", e);
      }
      FacilitySoftwareConfig[] labsdbs =
            restClient.getFacilitySoftwareInternalByType(SoftwareType.Labsdb).getBody();
      if (labsdbs == null || labsdbs.length == 0) {
         logger.info("No labsdb server find");
         return;
      }
      try {
         if (fullSync) {
            logger.info("Send Sync Labsdb all assets commands");
            template.opsForList().leftPushAll(EventMessageUtil.labsdbJobList,
                  EventMessageUtil.generateFacilityMessageListByType(EventType.Labsdb,
                        EventMessageUtil.Labsdb_SyncAllWireMapData, labsdbs));
         } else {
            logger.info("Send Sync labsdb unmapped assets commands");
            template.opsForList().leftPushAll(EventMessageUtil.labsdbJobList,
                  EventMessageUtil.generateFacilityMessageListByType(EventType.Labsdb,
                        EventMessageUtil.Labsdb_SyncUnMappedAssetWiremapData, labsdbs));
         }
         publisher.publish(EventMessageUtil.LabsdbTopic,
               EventMessageUtil.generateFacilityNotifyMessage(EventType.Labsdb));
      } catch (IOException e) {
         logger.error("Failed to send out message", e);
      }
      logger.info("Finish send sync data command for Labsdb.");
   }

}
