package com.vmware.flowgate.aggregator.tool.basic;
import java.util.Random;
public class Samples {
	Item[] items;
    Bag[] bags;

    private static int LOWEST_ITEM_WEIGHT = 5;
    private static int ITEM_WEIGHT_VARIATION = 30;

    private static int LOWEST_ITEM_VALUE = 1;
    private static int ITEM_VALUE_VARIATION = 30;
    
    private static int LOWEST_BAG_CAPACITY = 30;
    private static int BAG_CAPACITY_VARIATION = 70;


    public Samples() {
    }

    /**
     * Generate random values between 1-30 for items (Weights and values),
     * random values between 10 and 30 for bags (capacity).
     * Items and bags can be obtained through the corresponding get-methods.
     * @param nbrOfItems - number of items in the problem space
     * @param nbrOfBags - number of bags in the problem space
     */
    public void generateRandomValues(int nbrOfItems, int nbrOfBags){
        Random random = new Random();
        items = new Item[nbrOfItems];
        bags = new Bag[nbrOfBags];
        for(int i = 0; i<items.length; i++){
        	int cpu = random.nextInt(ITEM_VALUE_VARIATION)+LOWEST_ITEM_VALUE;
            items[i] = new Item(String.valueOf(i), (double)random.nextInt(ITEM_WEIGHT_VARIATION)+LOWEST_ITEM_WEIGHT, (double)cpu, (double)cpu+ random.nextInt(ITEM_WEIGHT_VARIATION));
        }
        for(int i = 0; i<bags.length;i++){
            double[] params = new double[5];
            for (int j = 0; j < params.length; j++)
            {
                params[i] = random.nextDouble() * 1.2;
            }
            bags[i] = new Bag(random.nextInt(BAG_CAPACITY_VARIATION)+LOWEST_BAG_CAPACITY, random.nextInt(BAG_CAPACITY_VARIATION)+LOWEST_BAG_CAPACITY, params, i);
        }
    }

    public Item[] getItems() {
        return items;
    }

    public Bag[] getBags() {
        return bags;
    }

}
