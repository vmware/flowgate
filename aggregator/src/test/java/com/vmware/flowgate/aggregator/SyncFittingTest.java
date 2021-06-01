package com.vmware.flowgate.aggregator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.csvreader.CsvReader;

import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.aggregator.scheduler.job.AggregatorService;
import com.vmware.flowgate.aggregator.tool.SyncFittingTool;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.MetricData;


import junit.framework.TestCase;

public class SyncFittingTest {
	 @MockBean
	   private StringRedisTemplate template;
	   @Spy
	   @InjectMocks
	   private AggregatorService aggregatorService = new AggregatorService();

	   @MockBean
	   private WormholeAPIClient restClient;

	   @MockBean
	   private ServiceKeyConfig serviceKeyConfig;


	  
	   @Test
	   public void testSyncFitting() throws Exception {
		  try {
			  List<MetricData> MetricDatas;
			  SyncFittingTool tool = new SyncFittingTool();
		  	  MetricDatas = new ArrayList<>();

	          CsvReader csvReader = new CsvReader("testData.csv");
	          boolean re = csvReader.readHeaders();
	          int n = 0;
	          while (csvReader.readRecord()) {
		          String rawRecord = csvReader.getRawRecord();
		          String[] line = rawRecord.split(",");
		          MetricData cpu = new MetricData();
		       	  cpu.setMetricName("CpuUsage");
		       	  cpu.setValueNum(Double.valueOf(line[0]));
		       	  cpu.setTimeStamp(n);
		       	  MetricDatas.add(cpu);
		          MetricData power = new MetricData();
		          power.setMetricName("Power");
		          power.setValueNum(Double.valueOf(line[1]));
	              power.setTimeStamp(n);
	              MetricDatas.add(power);
	              n+=1;
	         }

	         List<Double> results =  tool.doFitting(MetricDatas);
	         		         
	         double [] arr_result = new double[results.size()];     
	         for (int i = 0; i < results.size(); i++) {	        	 
					  arr_result[i] = results.get(i);
	         }
		     double[] testAnswer = {67.61123636351215, 2.0250676734875026, -0.04034063855153128, 3.8173496754197417E-4, -1.2569420548906328E-6};
			 Assert.assertArrayEquals(arr_result, testAnswer, 0.5);	 
			 Asset asset = new Asset();
		     asset.setFittingResults(results);
		     restClient.saveAssets(asset);	
			  
		 } catch(Exception e) {
			  Assert.fail();
		  }
		  
	   }

	  
}
