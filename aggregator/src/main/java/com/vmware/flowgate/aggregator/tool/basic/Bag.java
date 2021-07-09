package com.vmware.flowgate.aggregator.tool.basic;
import java.util.ArrayList;
import java.util.Random;
public class Bag {
	int Memory;
    double currentMemory;
    int CPU;
    double currentCPU;
    int index;
    double totalPower;
    double[] params;
    ArrayList<Item> items;
    Random random = new Random();

    public Bag(int memory, int cpu, double[] params, int idx) {
        this.Memory = memory;
        this.CPU = cpu;
        this.currentMemory = 0;
        this.currentCPU = 0;
        this.params = params;
        this.index  = idx;
        items = new ArrayList<>();
    }

    public int getIndex() {
        return index;
    }
    
    public int getCPU() {
    	return CPU;
    }
    
    public double getCurrentCPU() {
    	return currentCPU;
    }

    public int getMemory() {
        return Memory;
    }

    public double getValueInBag(double value) {
        double res = 0;
        for (int i = 0; i < params.length; i++) {
            res += params[i] * Math.pow(value, i);
        }
        return  res;
    }

    public double getCurrentMemory() {
        return currentMemory;
    }
    


    public double getTotalPower() {
        return totalPower;
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public Item getRandomItem(){
        int index = 0;
        if(!items.isEmpty()) {
            index = random.nextInt(items.size());
            return items.get(index);

        } else {
            return null;
        }

    }

    public void addItem(Item item, int finalBag) {
        if (!items.contains(item)) {
            items.add(item);
            currentMemory = currentMemory + item.getMemory();
            currentCPU = currentCPU + item.getCPU();
            this.totalPower += item.getPower()[finalBag].getFirst();
        }


    }

    public double availableMemory(){
        return (Memory-currentMemory);
    }

    public double availableCPU() {
    	return (CPU - currentCPU);
    }
    
    public Item removeItem(Item item, int finalBag) {


        currentMemory = currentMemory - item.getMemory();
        currentCPU = currentCPU - item.getCPU();
        int index = items.indexOf(item);
        this.totalPower -= item.getPower()[finalBag].getFirst();
        return items.remove(index);
    }

    public Bag clone() {
        Bag newBag = new Bag(Memory, CPU, params, index);
        newBag.currentMemory = currentMemory;
        newBag.currentCPU = currentCPU;
        newBag.totalPower = totalPower;
        newBag.CPU = CPU;
        newBag.items = new ArrayList<>(items);

        return newBag;
    }

    public String toString(){
    	String s =  "Bag(index: " + this.index + ", Memory: " + this.Memory + ", current memory: " +this.currentMemory + ", CPU: " + this.CPU +", currentCPU: " + this.currentCPU+", value: "+totalPower+")" + "\n";
    	for (int i = 0; i < items.size(); i++) {
    		s += items.get(i).toStringByIdx(this.index);
    		s += "\n";
    	}
    	return s;
    }
    public String toFullString(){
        return "Bag(Memory: " + this.Memory + ", current Memory: " +this.currentMemory + ", items: "+this.items+")";
    }

}
