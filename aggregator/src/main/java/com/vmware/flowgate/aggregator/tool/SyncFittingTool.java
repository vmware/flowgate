package com.vmware.flowgate.aggregator.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.util.Pair;

import com.vmware.flowgate.common.model.MetricData;

public class SyncFittingTool {
	   
	   public List<Double> doubleToList(double[] arr_double) {
		    List<Double> list = new ArrayList<Double>();
		    int num = arr_double.length;
		    Double [] arr_Double = new Double[num];
		    for(int i = 0; i < num; i++) {
		        arr_Double[i] = arr_double[i];
		    }
		    list = Arrays.asList(arr_Double);
		    return list; 
		}
	   


	   public List<Pair<Double, Double>> MAD(List<Pair<Double, Double>> dataset, double n) {
	      List<Pair<Double, Double>> new_data = new ArrayList<>();
	      List<Double> CPU = new ArrayList<>();
	      for (int i = 0; i < dataset.size(); i++) {
	          CPU.add(dataset.get(i).getFirst());
	      }
	      double median = median(CPU);
	      List<Double> deviations = new ArrayList<>();
	      for (int i = 0; i < dataset.size(); i++) {
	          deviations.add(i,  Math.abs(dataset.get(i).getFirst() - median));
	      }
	      double mad = median(deviations);
	      for (int i = 0; i < dataset.size(); i++)
	      {
	          if (Math.abs(dataset.get(i).getFirst() - median) <= n * mad)
	          {
	              new_data.add(dataset.get(i));
	          }
	      }
	      return new_data;
	  }
	   public static int partition(List<Double> nums, int start, int end){
	      int left = start;
	      int right = end;
	      double pivot = nums.get(left);
	      while (left < right){
	          while (left < right && nums.get(right) >= pivot) {
	              right--;
	          }
	          if (left < right) {
	              nums.set(left, nums.get(right));
	              left++;
	          }
	          while (left < right && nums.get(left) <= pivot){
	              left++;
	          }
	          if (left < right) {
	              nums.set(right, nums.get(left));
	              right--;
	          }
	      }
	      nums.set(left, pivot);
	      return left;
	  }


	   public static double median(List<Double> nums){
	      if (nums.size() == 0)
	          return 0;
	      int start = 0;
	      int end = nums.size() - 1;
	      int index = partition(nums, start, end);
	      if (nums.size() % 2 == 0){
	          while (index != nums.size() / 2 - 1){
	              if (index > nums.size() / 2 - 1){
	                  index = partition(nums, start, index - 1);
	              } else {
	                  index=partition(nums, index+1, end);
	              }
	          }
	      } else {
	          while (index != nums.size() / 2) {
	              if (index > nums.size() / 2) {
	                  index = partition(nums, start, index - 1);
	              } else {
	                  index = partition(nums, index + 1, end);
	              }
	          }
	      }
	      return nums.get(index);
	   }
	   

	   public List<Double> doFitting(List<MetricData> MetricDatas) {
		   
		  List<Double> CPU = new ArrayList<>();
	      List<Double> power = new ArrayList<>();
	      List<Pair<Long, Double>> raw_CPU_list = new ArrayList<>();
	      List<Pair<Long, Double>> raw_power_list = new ArrayList<>();
	      
		  for (int i = 0; i < MetricDatas.size(); i++) {
			  if (MetricDatas.get(i).getMetricName() == "CpuUsage") {
				  raw_CPU_list.add(new Pair<Long, Double> (MetricDatas.get(i).getTimeStamp(), MetricDatas.get(i).getValueNum()));
			  }
			  else if (MetricDatas.get(i).getMetricName() == "Power") {
				  raw_power_list.add(new Pair<Long, Double> (MetricDatas.get(i).getTimeStamp(), MetricDatas.get(i).getValueNum()));
			  }
		  }
	      Pair<Long, Double>[] raw_CPU = new Pair[raw_CPU_list.size()];
	      Pair<Long, Double>[] raw_power =  new Pair[raw_power_list.size()];
	      for (int i = 0; i < raw_CPU_list.size(); i++) {
	    	  raw_CPU[i] = raw_CPU_list.get(i);
	      }
	      for (int i = 0; i < raw_power_list.size(); i++) {
	    	  raw_power[i] = raw_power_list.get(i);
	      }

	      //Sort the pair list according the time in reverse order.
		  Arrays.sort(raw_CPU, new Comparator<Pair<Long, Double>>()  {
	    	  @Override
	          public int compare(Pair<Long, Double> o1, Pair<Long, Double> o2) {
	              if(o1.getFirst()==o2.getFirst()){
	                  return 0;
	              }else if (o1.getFirst() > o2.getFirst()){
	                  return -1;
	              }
	              else return 1;
	          }
	      });
	      Arrays.sort(raw_power, new Comparator<Pair<Long, Double>>()  {
	    	  @Override
	          public int compare(Pair<Long, Double> o1, Pair<Long, Double> o2) {
	              if(o1.getFirst()==o2.getFirst()){
	                  return 0;
	              }else if (o1.getFirst() > o2.getFirst()){
	                  return -1;
	              }
	              else return 1;
	          }
	      });
	      
		  int idx_CPU = 0, idx_power = 0;
	      List<Pair<Double, Double>> raw_data = new ArrayList<>();
		  while (idx_CPU < raw_CPU.length && idx_power < raw_power.length) {
			  if (raw_CPU[idx_CPU].getFirst().compareTo(raw_power[idx_power].getFirst()) == 0) {
			      raw_data.add(new Pair<Double, Double>(raw_CPU[idx_CPU].getSecond(), raw_power[idx_power].getSecond()));
				  idx_CPU +=1;
				  idx_power +=1;
			  }  
			  else if (raw_CPU[idx_CPU].getFirst().compareTo(raw_power[idx_power].getFirst()) == 1) {
				  idx_CPU += 1;
			  }
			  else if (raw_CPU[idx_CPU].getFirst().compareTo(raw_power[idx_power].getFirst()) == -1) {
				  idx_power += 1;
			  }
		  }


	      List<Pair<Double, Double>> new_data = new ArrayList<>();
	      WeightedObservedPoints points = new WeightedObservedPoints();
	      while (raw_data.size() != 0) {
	         int count = 0;
	         for (int i = 1; i < raw_data.size(); i++) {

	            if (raw_data.get(i).getSecond() >= raw_data.get(i-1).getSecond() + 1)
	               break;
	            count += 1;
	            if (count > 0) {
	               List<Pair<Double, Double>> tmp = raw_data.subList(0, count + 1);
	               new_data.addAll(MAD(tmp, 1.5));
	            }
	           raw_data = raw_data.subList(count + 1, raw_data.size());
	         }
	      }
	      for(int i = 0; i < new_data.size(); i++)
	      {
	    	  points.add(new_data.get(i).getFirst(), new_data.get(i).getSecond());
	      }
	      int degree = 4;
	      PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree); 
	      double[] result = fitter.fit(points.toList());
	      List<Double> fitting_result = doubleToList(result);
	      
	      
	     
	      return fitting_result;
	   }
	   
}
