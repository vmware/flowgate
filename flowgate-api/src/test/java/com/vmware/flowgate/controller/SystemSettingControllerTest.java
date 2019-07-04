/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.vmware.flowgate.exception.WormholeRequestException;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class SystemSettingControllerTest {
   @Rule
   public final JUnitRestDocumentation restDocumentation =
         new JUnitRestDocumentation("target/generated-snippets");

   private MockMvc mockMvc;

   @MockBean
   private StringRedisTemplate template;

   @Autowired
   private WebApplicationContext context;

   @Rule
   public ExpectedException expectedEx = ExpectedException.none();

   @Before
   public void setUp() {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation)).build();
   }

   @Test
   public void getExpiredTimeRangeTest() {
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      try {
         this.mockMvc.perform(get("/v1/setting/datapersistenttime"))
               .andExpect(status().isOk()).andDo(document("SystemSetting-getExpiredTimeRange-example"
                     ))
               .andReturn();
      } catch (Exception e) {
         TestCase.fail();
      }
   }

   @Test
   public void updateExpiredTimeRangeTest() throws Exception{
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      this.mockMvc.perform(put("/v1/setting/datapersistenttime/89000000000"))
               .andExpect(status().isOk()).andDo(document("SystemSetting-updateExpiredTimeRange-example"))
               .andReturn();
   }

   @Test
   public void updateExpiredTimeRangeTest1() throws Exception{
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Expired time range must more than 90 days.");
         MvcResult result = this.mockMvc.perform(put("/v1/setting/datapersistenttime/900"))
               .andReturn();
         if (result.getResolvedException() != null) {
            throw result.getResolvedException();
         }
   }
}
