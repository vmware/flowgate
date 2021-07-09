package com.vmware.flowgate.aggregator.tool.basic;
import java.util.ArrayList;
public class Neighborhood {
	 ArrayList<Item> unusedItems;
	    Bag[] bags;

	    public Neighborhood(ArrayList<Item> unusedItems, Bag[] bags) {
	        this.unusedItems = new ArrayList<>(unusedItems);

	        this.bags = new Bag[bags.length];
	        for(int i = 0; i<bags.length; i++) {
	            this.bags[i] = bags[i].clone();

	        }

	    }

	    public ArrayList<Item> getUnusedItems() {
	        return unusedItems;
	    }

	    public Bag[] getBags() {
	        return bags;
	    }
	    
	    public int getBagsItemsNumber () {
	    	int ret = 0;
	    	for (int i = 0; i < bags.length; i++) {
	    		ret += bags[i].getItems().size();
	    	}
	    	return ret;
	    }

	    public int getTotalPower(){
	        int res = 0;
	        for(Bag b : bags){
	            res+=b.getTotalPower();
	        }
	        return res;
	    }

	    public String toString(){
	        String res = "Neighborhood with value: " + getTotalPower() + "\n";

	        for(int i = 0; i<bags.length; i++) {
	            res += bags[i].toString() + "\n";
	        }

	        return res;
	    }
}
