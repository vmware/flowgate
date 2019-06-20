package com.vmware.flowgate.controller;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.redis.message.impl.EventMessageUtil;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class SystemSettingControllerTest {
   @Rule
   public final JUnitRestDocumentation restDocumentation =
         new JUnitRestDocumentation("target/generated-snippets");

   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   @MockBean
   private StringRedisTemplate template;

   @Autowired
   private WebApplicationContext context;



   @Before
   public void setUp() {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation)).build();
   }

   @Test
   public void getExpiredTimeRangeTest() {
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      valueOperations.append(EventMessageUtil.EXPIREDTIMERANGE, "999999999");
      valueOperations.set(EventMessageUtil.EXPIREDTIMERANGE, "999999999");
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      try {
//         FieldDescriptor[] fieldpath = new FieldDescriptor[] {
//               fieldWithPath("time").description("ID of FacilitySoftwareConfig, created by flowgate")};
         MvcResult result = this.mockMvc.perform(get("/v1/setting/datapersistenttime"))
               .andExpect(status().isOk()).andDo(document("SystemSetting-getExpiredTimeRange-example",
                     responseFields(fieldWithPath("/").description("An array of asserts"))))
               .andReturn();
         String res = result.getResponse().getContentAsString();
         //syso
      } catch (Exception e) {
         TestCase.fail();
      }
   }
}
