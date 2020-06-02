/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vmware.flowgate.common.model.JobConfig;
import com.vmware.flowgate.common.model.JobConfig.JobType;
import com.vmware.flowgate.common.model.redis.message.MessagePublisher;
import com.vmware.flowgate.repository.FacilitySoftwareConfigRepository;
import com.vmware.flowgate.repository.JobsRepository;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.service.ServerValidationService;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class JobsControllerTest {
    @Rule
    public final JUnitRestDocumentation restDocumentation =
          new JUnitRestDocumentation("target/generated-snippets");

    private MockMvc mockMvc;

    @Autowired
    private FacilitySoftwareConfigRepository facilitySoftwareRepository;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JobsRepository jobsRepository;

    @SpyBean
    private ServerValidationService serverValidationService;

    @SpyBean
    private AccessTokenService tokenService;

    @MockBean
    private StringRedisTemplate template;
    @MockBean
    private MessagePublisher publisher;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {
       this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
             .apply(documentationConfiguration(this.restDocumentation)).build();
    }

    @Test
    public void startFullMappingAggregationExample() throws JsonProcessingException, Exception {

       Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());

       this.mockMvc
             .perform(post("/v1/jobs/mergeservermapping").contentType(MediaType.APPLICATION_JSON))
             .andDo(document("JobsController-startFullMappingAggregation-example"))
             .andReturn();
    }
    @Test
    public void generateServerPDUMappingExample() throws JsonProcessingException, Exception {

       Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());

       this.mockMvc
             .perform(post("/v1/jobs/pduservermapping").contentType(MediaType.APPLICATION_JSON))
             .andDo(document("JobsController-generateServerPDUMapping-example"))
             .andReturn();
    }

    @Test
    public void syncHostnameByIpExample() throws JsonProcessingException, Exception {

       Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());

       String ip = "127.0.0.1";
      MvcResult result = this.mockMvc
             .perform(post("/v1/jobs/synchostnamebyip/"+ip+"").contentType(MediaType.APPLICATION_JSON))
             .andDo(document("JobsController-syncHostnameByIp-example"))
             .andReturn();

      TestCase
            .assertTrue(result.getResolvedException().getMessage().equals("Invalid Ip: 127.0.0.1"));
    }

    @Test
    public void getVROJobsExample() throws JsonProcessingException, Exception {

        JobConfig jobconfig1 = createJobConfig();
        jobconfig1.setId("1");
        jobconfig1.setJobType(JobType.VRO);
        jobsRepository.save(jobconfig1);
        JobConfig jobconfig2 = createJobConfig();
        jobconfig2.setId("2");
        jobconfig2.setJobType(JobType.VRO);
        jobsRepository.save(jobconfig2);

        Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());
        FieldDescriptor[] fieldpath = new FieldDescriptor[] {
                fieldWithPath("id").description("ID of jobconfig, created by flowgate"),
                fieldWithPath("jobName").description("jobName."),
                fieldWithPath("jobGroup").description("jobGroup"),
                fieldWithPath("triggerGroup").description("triggerGroup"),
                fieldWithPath("triggerName").description(
                      "triggerName."),
                fieldWithPath("jobInfo").description(
                        "jobInfo."),
                fieldWithPath("cronExpression").description(
                        "cronExpression"),
                fieldWithPath("jobClass").description(
                        "jobClass"),
                fieldWithPath("jobType").description(
                        "jobType.").optional()
                };

       this.mockMvc
             .perform(get("/v1/jobs/vrojobs"))
             .andExpect(status().isOk())
             .andDo(document("JobsController-getVROJobs-example", responseFields(
                     fieldWithPath("[]").description("An array of vro jobs"))
                     .andWithPrefix("[].", fieldpath)));

        jobsRepository.delete(jobconfig1);
        jobsRepository.delete(jobconfig2);
    }

    @Test
    public void getVCJobsExample() throws JsonProcessingException, Exception {

        JobConfig jobconfig1 = createJobConfig();
        jobconfig1.setId("1");
        jobconfig1.setJobType(JobType.VCENTER);
        jobsRepository.save(jobconfig1);
        JobConfig jobconfig2 = createJobConfig();
        jobconfig2.setId("2");
        jobconfig2.setJobType(JobType.VCENTER);
        jobsRepository.save(jobconfig2);

        Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());
        FieldDescriptor[] fieldpath = new FieldDescriptor[] {
                fieldWithPath("id").description("ID of jobconfig, created by flowgate"),
                fieldWithPath("jobName").description("jobName."),
                fieldWithPath("jobGroup").description("jobGroup"),
                fieldWithPath("triggerGroup").description("triggerGroup"),
                fieldWithPath("triggerName").description(
                      "triggerName."),
                fieldWithPath("jobInfo").description(
                        "jobInfo."),
                fieldWithPath("cronExpression").description(
                        "cronExpression"),
                fieldWithPath("jobClass").description(
                        "jobClass"),
                fieldWithPath("jobType").description(
                        "jobType.").optional()
                };

       this.mockMvc
             .perform(get("/v1/jobs/vcjobs"))
             .andExpect(status().isOk())
             .andDo(document("JobsController-getVCJobs-example", responseFields(
                     fieldWithPath("[]").description("An array of vc jobs"))
                     .andWithPrefix("[].", fieldpath)));

        jobsRepository.delete(jobconfig1);
        jobsRepository.delete(jobconfig2);
    }

    @Test
    public void getJobsByTypeExample() throws JsonProcessingException, Exception {

        JobConfig jobconfig1 = createJobConfig();
        jobconfig1.setId("1");
        jobconfig1.setJobType(JobType.VCENTER);
        jobsRepository.save(jobconfig1);
        JobConfig jobconfig2 = createJobConfig();
        jobconfig2.setId("2");
        jobconfig2.setJobType(JobType.VCENTER);
        jobsRepository.save(jobconfig2);

        Mockito.doNothing().when(publisher).publish(Mockito.anyString(), Mockito.anyString());
        FieldDescriptor[] fieldpath = new FieldDescriptor[] {
                fieldWithPath("id").description("ID of jobconfig, created by flowgate"),
                fieldWithPath("jobName").description("jobName."),
                fieldWithPath("jobGroup").description("jobGroup"),
                fieldWithPath("triggerGroup").description("triggerGroup"),
                fieldWithPath("triggerName").description(
                      "triggerName."),
                fieldWithPath("jobInfo").description(
                        "jobInfo."),
                fieldWithPath("cronExpression").description(
                        "cronExpression"),
                fieldWithPath("jobClass").description(
                        "jobClass"),
                fieldWithPath("jobType").description(
                        "jobType.").optional()
                };

        JobType jobtype = JobType.VCENTER;
       this.mockMvc
             .perform(get("/v1/jobs/type/"+ jobtype +""))
             .andExpect(status().isOk())
             .andDo(document("JobsController-getJobsByType-example", responseFields(
                     fieldWithPath("[]").description("An array of jobs"))
                     .andWithPrefix("[].", fieldpath)));

        jobsRepository.delete(jobconfig1);
        jobsRepository.delete(jobconfig2);
    }

    JobConfig createJobConfig()
    {
        JobConfig job = new JobConfig();
        job.setId("jobconfig");
        job.setJobClass("jobclass");
        job.setJobGroup("jobgroup");
        job.setJobInfo("jobinfo");
        job.setJobName("jobname");
        job.setJobType(JobType.VRO);
        job.setTriggerGroup("triggerGroup");
        job.setTriggerName("triggerName");
        job.setCronExpression("cronExpression");

        return job;
    }
}
