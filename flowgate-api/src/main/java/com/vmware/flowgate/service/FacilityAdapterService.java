package com.vmware.flowgate.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.vmware.flowgate.common.FlowgateConstant;
import com.vmware.flowgate.common.model.AdapterJobCommand;
import com.vmware.flowgate.common.model.FacilityAdapter;
import com.vmware.flowgate.controller.AssetController;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.FacilityAdapterRepository;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.util.BaseDocumentUtil;

@Service
public class FacilityAdapterService {

   @Autowired
   private FacilityAdapterRepository facilityAdapterRepo;

   @Autowired
   private FacilitySoftwareConfigRepository facilitySoftwareRepo;

   @Autowired
   private StringRedisTemplate redis;

   private static final String QUEUE_NAME_SUFFIX = ":joblist";
   private static final String JOIN_FLAG = "_";
   private static Set<String> predefineName = new HashSet<String>();
   static {
      predefineName.add("Nlyte");
      predefineName.add("PowerIQ");
      predefineName.add("Device42");
      predefineName.add("InfoBlox");
      predefineName.add("Labsdb");
   }

   public HttpHeaders create(FacilityAdapter adapter) {
      String displayName = adapter.getDisplayName();
      if(displayName == null || predefineName.contains(displayName)) {
         throw WormholeRequestException.InvalidFiled("DisplayName", displayName);
      }
      FacilityAdapter oldAdapter = facilityAdapterRepo.findByDisplayName(displayName);
      if(oldAdapter != null) {
         throw new WormholeRequestException("Adapter with dispalyName : "+displayName+" is existed");
      }
      List<AdapterJobCommand> commands = adapter.getCommands();
      if(commands == null || commands.isEmpty()) {
         throw new WormholeRequestException("The Commands field is required.");
      }
      HttpHeaders httpHeaders = new HttpHeaders();
      BaseDocumentUtil.generateID(adapter);
      String unique_value = adapter.getType().name()+ JOIN_FLAG + UUID.randomUUID().toString().replaceAll("-", "");
      adapter.setTopic(unique_value);
      adapter.setSubCategory(unique_value);
      adapter.setQueueName(adapter.getSubCategory() + QUEUE_NAME_SUFFIX);
      String randomKey = UUID.randomUUID().toString().replaceAll("-", "");
      String serviceKey = DigestUtils.sha256Hex(randomKey);
      adapter.setServiceKey(serviceKey);
      facilityAdapterRepo.save(adapter);
      cacheServiceKey(serviceKey);
      httpHeaders.setLocation(linkTo(AssetController.class).slash(adapter.getId()).toUri());
      return httpHeaders;
   }

   public FacilityAdapter findById(String id) {
      Optional<FacilityAdapter> adapterOptional = facilityAdapterRepo.findById(id);
      if(!adapterOptional.isPresent()) {
         throw WormholeRequestException.NotFound("FacilityAdapter", "id", id);
      }
      return adapterOptional.get();
   }

   public void deleteAdapter(String id) {
      FacilityAdapter adapter = findById(id);
      int count = facilitySoftwareRepo.countFacilityBySubcategory(adapter.getSubCategory());
      if(count > 0) {
         throw new WormholeRequestException("Adapter deletion failed, there are some integration instances are using it");
      }else {
         facilityAdapterRepo.deleteById(id);
         if(redis.opsForSet().isMember(FlowgateConstant.SERVICE_KEY_SET, adapter.getServiceKey())) {
            redis.opsForSet().remove(FlowgateConstant.SERVICE_KEY_SET, adapter.getServiceKey());
         }
      }
   }

   private void cacheServiceKey(String value) {
      redis.opsForSet().add(FlowgateConstant.SERVICE_KEY_SET, value);
   }

}
