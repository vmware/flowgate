package com.vmware.flowgate.aggregator.tool;
import com.vmware.flowgate.aggregator.tool.basic.*;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;

public class Greedy {
	private Bag[] bags;
    private ArrayList<Item> items = new ArrayList<Item>();
    private ArrayList<Item> unusedItems = new ArrayList<>();

    public Greedy(Item[] items, Bag[] bags) {

        this.bags = bags;

        for (int i = 0; i < items.length; i++) {
            items[i].setValueAndBenefitInBag(bags);
            this.items.add(items[i]);
        }
    }

    /*
       Selects "best" item to add based on calculated benefit (value/weight)
       If that item can't be added it is discarded.
       Solution is terminated when no items can be added or all items have already been added to the knapsacks.

     */
    public void solve() {

        for (int i = 0; i < this.items.size(); i++) {
            addToKnapsack(items.get(i));
        }

    }

    /**
     *
     * Adds incoming item to the knapsack with the least amount of remaining space but enough space to fit the item
     * If item can not be added to any knapsack it is removed from the items list and added to the unused items list.
     *
     * @param item item to be added to knapsack or discarded
     */
    private void addToKnapsack(Item item) {

        Bag bestFit = null;
        Pair<Double, Integer>[] valueInBag = item.getPower();
        int finalBag = -1;
        for (int i = 0; i < valueInBag.length; i++) {
            int bagIndex = valueInBag[i].getSecond();
            if (bags[bagIndex].availableMemory() >= item.getMemory() && bags[bagIndex].availableCPU() >= item.getCPU_capacity()) {

                if (bestFit == null) {
                    bestFit = bags[bagIndex];
                    finalBag = i;
                    break;

                }
            }

        }

        if (bestFit != null) {
            bestFit.addItem(item, finalBag);

        } else {
            unusedItems.add(item);

        }

    }

    public void printResult() {

        System.out.println("Knapsacks: ");

        for (int i = 0; i < bags.length; i++) {
            System.out.println();

            System.out.println("Capacity " + bags[i].getMemory());
            System.out.println("Space left " + bags[i].availableMemory());
            System.out.println("Number of items " + bags[i].getItems().size());
            System.out.println("Total value " + bags[i].getTotalPower());
            for (int j = 0; j < bags[i].getItems().size(); j++) {
                Pair<Double, Integer>[] valueInBag = bags[i].getItems().get(j).getPower();
                for (int m = 0; m < valueInBag.length; m++) {
                    if (valueInBag[m].getSecond() == i)
                        System.out.println("in bag value " + valueInBag[m].getFirst());
                    else
                        System.out.println(valueInBag[m].getFirst());

                }
            }

        }

    }

    public static void main(String[] args) {

        int nbrOfItems = 5;
        int nbrOfBags = 2;

        Bag[] test_bags;
        Item[] test_items;

        Samples samples = new Samples();

        //samples.generateRandomValues(nbrOfItems, nbrOfBags);

        test_bags = new Bag[nbrOfBags];
        test_items = new Item[nbrOfItems];
        test_items[0] = new Item("1", 10, 2, 2);
        test_items[1] = new Item("2", 20, 3, 3);
        test_items[2] = new Item("3", 18, 4, 4);
        test_items[3] = new Item("4", 15, 3, 3);
        test_items[4] = new Item("5", 22, 5, 5);
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
        test_bags[0] = new Bag(70, 10, params0, 0);
        test_bags[1] = new Bag(50, 10, params1, 1);
        Greedy greedy = new Greedy(test_items, test_bags);
        greedy.solve();
        greedy.printResult();


    }

    public Bag[] getBags() {
        return bags;
    }

    public ArrayList<Item> getUnusedItems() {
        return unusedItems;
    }
}
