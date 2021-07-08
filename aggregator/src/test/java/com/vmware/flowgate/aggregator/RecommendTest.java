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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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


import com.vmware.flowgate.aggregator.config.ServiceKeyConfig;
import com.vmware.flowgate.aggregator.scheduler.job.AggregatorService;
import com.vmware.flowgate.aggregator.tool.SyncFittingTool;
import com.vmware.flowgate.client.WormholeAPIClient;
import com.vmware.flowgate.common.AssetCategory;
import com.vmware.flowgate.common.model.Asset;
import com.vmware.flowgate.common.model.MetricData;
import com.vmware.flowgate.aggregator.tool.*;
import com.vmware.flowgate.aggregator.tool.basic.*;


import junit.framework.TestCase;

public class RecommendTest {
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
	   public void testRecommend() throws Exception {
		  try {
			  int nbrOfItems = 5;
		        int nbrOfBags = 2;
		        Item[] items = new Item[nbrOfItems];
		        Bag[] bags = new Bag[nbrOfBags];
		        items[0] = new Item("1", 10, 2, 2.2);
		        items[1] = new Item("2", 20, 3, 3.1);
		        items[2] = new Item("3", 18, 4, 4.3);
		        items[3] = new Item("4", 15, 3, 3.4);
		        items[4] = new Item("5", 22, 5, 5.1);
		        double[] params0 = new double[5], params1 = new double[5];
		        params0[0] = 50;
		        params0[1] = 2;
		        params0[2] = -2.4;
		        params0[3] = -0.8;
		        params0[4] = 0.2;
		        params1[0] = 99;
		        params1[1] = -4;
		        params1[2] = -1.2;
		        params1[3] = 0.5;
		        params1[4] = -0.1;
		        bags[0] = new Bag(70, 10, params0, 0);
		        bags[1] = new Bag(50, 10, params1, 1);
		        NeighborhoodSearch ns = new NeighborhoodSearch(bags,  new ArrayList<>(Arrays.asList(items)));
		        ns.search();
		        Bag[] bag_result = ns.getBags();
			    double[] bag0_result = {bag_result[0].getCurrentMemory(), bag_result[0].getCurrentCPU(), bag_result[0].getTotalPower()};
			    double[] bag0_answer = {40.0, 9.0, 44.6};
			    double[] bag1_result = {bag_result[1].getCurrentMemory(), bag_result[1].getCurrentCPU(), bag_result[1].getTotalPower()};
			    double[] bag1_answer = {45.0, 8.0, 251.8};
			    ArrayList<Item> bag0_items = bag_result[0].getItems();
			    ArrayList<Item> bag1_items = bag_result[1].getItems();
			    HashSet<String> bag0items_answer = new HashSet<>(); 
			    HashSet<String> bag0items_result = new HashSet<>(); 
			    HashSet<String> bag1items_answer = new HashSet<>(); 
			    HashSet<String> bag1items_result = new HashSet<>(); 

			    for (int i = 0; i < bag0_items.size(); i++) {
			    	bag0items_result.add(bag0_items.get(i).getId());
			    }
			    for (int i = 0; i < bag1_items.size(); i++) {
			    	bag1items_result.add(bag1_items.get(i).getId());
			    }
			    bag0items_answer.add("3");
			    bag0items_answer.add("5");
			    bag1items_answer.add("1");
			    bag1items_answer.add("4");
			    bag1items_answer.add("2");

				Assert.assertArrayEquals(bag0_result, bag0_answer, 0.5);	 
				Assert.assertArrayEquals(bag1_result, bag1_answer, 0.5);

				if (!bag0items_result.equals(bag0items_answer)) {
					System.out.print(false);
					Assert.fail();
				}
				if (!bag1items_result.equals(bag1items_answer)) {
					Assert.fail();
				}
			  
		 } catch(Exception e) {
			  Assert.fail();
		  }
		  
	   }

	  
}