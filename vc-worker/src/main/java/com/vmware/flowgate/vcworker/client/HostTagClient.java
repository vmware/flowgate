/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vcworker.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vmware.cis.tagging.Category;
import com.vmware.cis.tagging.CategoryModel;
import com.vmware.cis.tagging.CategoryTypes;
import com.vmware.cis.tagging.Tag;
import com.vmware.cis.tagging.TagAssociation;
import com.vmware.cis.tagging.TagModel;
import com.vmware.cis.tagging.TagTypes;
import com.vmware.vapi.std.DynamicID;
import com.vmware.cis.tagging.CategoryModel.Cardinality;
import com.vmware.flowgate.vcworker.model.VCConstants;
import com.vmware.vcenter.Host;
import com.vmware.vcenter.HostTypes;
import com.vmware.vcenter.HostTypes.Summary;


public class HostTagClient extends VSphereAutomationClientBase {

   private Host hostService;
   private Category categoryService;
   private Tag taggingService;
   private TagAssociation tagAssociation;


   public HostTagClient(String server, String userName, String password,
         boolean skipCertVerification) {
      super(server, userName, password, skipCertVerification);
   }

   public void initConnection() throws Exception {
      login();
      this.hostService = vapiAuthHelper.getStubFactory().createStub(Host.class, sessionStubConfig);
      this.categoryService =
            this.vapiAuthHelper.getStubFactory().createStub(Category.class, sessionStubConfig);
      this.taggingService =
            this.vapiAuthHelper.getStubFactory().createStub(Tag.class, sessionStubConfig);
      this.tagAssociation = this.vapiAuthHelper.getStubFactory().createStub(TagAssociation.class,
            sessionStubConfig);
   }

   public void getHosts() {
      HostTypes.FilterSpec builder = new HostTypes.FilterSpec();
      List<Summary> hosts = hostService.list(builder);
      System.out.println(hosts.size());
   }

   public List<String> getTagCategories() {
      return this.categoryService.list();
   }

   public List<String> getTags() {
      return this.taggingService.list();
   }

   public List<TagModel> getPredefinedTags() {
      List<String> tagIDs = getTags();
      List<TagModel> tags = new ArrayList<TagModel>();
      for (String tagID : tagIDs) {
         TagModel tag = taggingService.get(tagID);
         if (VCConstants.predefinedTags.keySet().contains(tag.getName())) {
            tags.add(tag);
            if (tags.size() == VCConstants.predefinedTags.size()) {
               break;
            }
         }
      }
      return tags;
   }

   public TagModel getTagByName(String name) {
      List<String> tagIDs = getTags();
      for (String tagID : tagIDs) {
         TagModel tag = taggingService.get(tagID);
         if (tag.getName().equals(name)) {
            return tag;
         }
      }
      return null;
   }

   public String createTagCategory(String name, String description, Cardinality cardinality) {
      CategoryTypes.CreateSpec createSpec = new CategoryTypes.CreateSpec();
      createSpec.setName(name);
      createSpec.setDescription(description);
      createSpec.setCardinality(cardinality);

      Set<String> associableTypes = new HashSet<String>(); // empty hash set
      createSpec.setAssociableTypes(associableTypes);
      return this.categoryService.create(createSpec);
   }
   public CategoryModel getTagCategoryByName(String categoryName) {
      List<String>categoryIDs = categoryService.list();
      for(String id:categoryIDs) {
         CategoryModel cate= categoryService.get(id);
         if(cate.getName().equals(categoryName)) {
            return cate;
         }
      }
      return null;
   }
   public void deleteTagCategory(String categoryId) {
      this.categoryService.delete(categoryId);
   }

   public String createTag(String name, String description, String categoryId) {
      TagTypes.CreateSpec spec = new TagTypes.CreateSpec();
      spec.setName(name);
      spec.setDescription(description);
      spec.setCategoryId(categoryId);

      return this.taggingService.create(spec);
   }

   public void updateTag(String tagId, String description) {
      TagTypes.UpdateSpec updateSpec = new TagTypes.UpdateSpec();
      updateSpec.setDescription(description);
      this.taggingService.update(tagId, updateSpec);
   }

   public void deleteTag(String tagId) {
      this.taggingService.delete(tagId);
   }

   public void attachTagToHost(String tagID, String hostMobID) {
      DynamicID hostDynamicID = new DynamicID("HostSystem", hostMobID);
      this.tagAssociation.attach(tagID, hostDynamicID);
   }

   public void detachTagFromHost(String tagID, String hostMobID) {
      DynamicID hostDynamicID = new DynamicID("HostSystem", hostMobID);
      this.tagAssociation.detach(tagID, hostDynamicID);
   }

//   public static void main(String[] args) {
//      HostTagClient bb =
//            new HostTagClient("10.112.113.179", "administrator@vsphere.local", "Admin!23", true);
//      try {
//         bb.initConnection();
//
//      } catch (Exception e) {
//         e.printStackTrace();
//      }
//      List<String> tags = bb.getTags();
//      List<String> category = bb.getTagCategories();
//      bb.close();
//   }
}
