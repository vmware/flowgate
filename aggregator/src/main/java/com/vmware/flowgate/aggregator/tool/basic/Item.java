package com.vmware.flowgate.aggregator.tool.basic;
import org.apache.commons.math3.util.Pair;

import java.util.*;
public class Item {
	 double memory;//Memory
	    double CPU;//CPUUsage
	    double benefit;
	    double CPU_capacity;
	    Pair<Double, Integer>[] power;
	    String id;
	    //Pair<Double, Integer>[] benefitInBag;

	    public Item(String id, double memory, double cpu, double cpu_capacity) {
	    	this.id = id;
	        this.memory = memory;
	        this.CPU = cpu;
	        this.CPU_capacity  = cpu_capacity;
	        benefit = -1 * (double) cpu / (double) memory;
	    }

	    public double getMemory() {
	        return memory;
	    }

	    public String getId() {
	    	return id;
	    }
	    
	    public Pair<Double, Integer>[] getPower() {
	        return this.power;
	    }

	    public double getCPU_capacity () {
	    	return this.CPU_capacity;
	    }
	    
	    public int getValueIdxThroughBagIdx(int BagIdx) {
	        for (int i = 0; i < this.getPower().length; i++) {
	            if (this.getPower()[i].getSecond() == BagIdx)
	            {
	                return i;
	            }
	        }
	        return -1;
	    }

	    public void setValueAndBenefitInBag(Bag[] bags) {
	        this.power = new Pair[bags.length];
	        for (int i = 0; i < bags.length; i++) {
	            Bag bag = bags[i];
	            this.power[i] = new Pair<Double, Integer>(bag.getValueInBag(CPU), i);
	        }
	        Arrays.sort(this.power, new Comparator<Pair<Double, Integer>>()  {
	            @Override
	            public int compare(Pair<Double, Integer> o1, Pair<Double, Integer> o2) {
	                if(o1.getFirst()==o2.getFirst()){
	                    return 0;
	                }else if (o1.getFirst() > o2.getFirst()){
	                    return -1;
	                }
	                else return 1;
	            }
	        });
	        /*this.benefitInBag = new Pair[bags.length];
	        for (int i = 0; i < bags.length; i++)
	        {
	            this.benefitInBag[i] = new Pair<Double, Integer>(this.power[i].getFirst() / this.memory, this.power[i].getSecond());
	        }
*/
	    }

	    public double getCPU() {
	        return CPU;
	    }

	    public double getBenefit() {
	        return getCPU()/memory;
	    }

	    public int compareTo(Item o) {
	        return 0;
	    }


	    @Override
	    public int hashCode() {
	        return Objects.hash(memory,CPU);
	    }

	    @Override
	    public boolean equals(Object o)
	    {
	        if (this == o) {
	            return true;
	        }
	        if (o == null) {
	            return false;
	        }
	        if (this.getClass() != o.getClass()) {
	            return false;
	        }
	        Item other = (Item)o;
	        if (!Objects.equals(this.memory, other.getMemory())) {
	            return false;
	        }
	        if (!Objects.equals(this.CPU, other.getCPU())) {
	            return false;
	        }
	        return true;
	    }

	    public String toString() {
	        return "Item(Id: " + this.getId() + ", Memory: "+this.memory +", CPU used: "+this.CPU +", CPU capacity: " + this.CPU_capacity + ", hash: "+this.hashCode()+")";
	    }
	    
	    public String toStringByIdx(int idx) {
	        return "Item(Id: " + this.getId() + ", Memory: "+this.memory +", CPU used: "+this.CPU +", CPU capacity: " + this.CPU_capacity + ", power: " + power[getValueIdxThroughBagIdx(idx)].getFirst() + ", hash: "+this.hashCode()+")";
	    }
}
