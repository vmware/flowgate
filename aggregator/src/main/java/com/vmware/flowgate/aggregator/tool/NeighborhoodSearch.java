package com.vmware.flowgate.aggregator.tool;
import com.vmware.flowgate.aggregator.tool.basic.*;
import java.util.ArrayList;
import java.util.Arrays;
public class NeighborhoodSearch {
	Bag[] bags;
    ArrayList<Item> items;                          //Dynamic so we can use the list for storing an retrieving unused items
    Neighborhood globalOptima;
    ArrayList<Neighborhood> closestNeighbours;
    static final int SEARCHES = 100;                  //amount of searches to perform before termination
    Greedy greedy;
    int totalItemNumber = 0;
    
    public NeighborhoodSearch(Bag[] bags, ArrayList<Item> items) {
        
        this.bags = bags;
        this.items =items;
        this.totalItemNumber = this.items.size();
    }

    public Bag[] getBags() {
    	return this.globalOptima.getBags();
    }
    
    public ArrayList<Item> getItems() {
    	return this.items;
    }
    
    /**
     * Main method of the neighborhood search
     */
    public void search() {
        greedy = new Greedy(items.toArray(new Item[items.size()]), bags);
        greedy.solve();                           //Starting solution solved using greedy algorithm

        Neighborhood initial = new Neighborhood(greedy.getUnusedItems(), bags); //Starting point


        globalOptima = initial;                    //First solution has to be the best one so far
        items = greedy.getUnusedItems();
        
        Neighborhood lowestNeighbour = null;
        Neighborhood temp;

        //Amount of searches, outer loop
        for (int i = 0; i <= SEARCHES; i++) {
            closestNeighbours = new ArrayList<>();          //generate the neighborhood

            temp = new Neighborhood(items, bags);     //generate a neighbour for each rotation type
            rotateBags(temp.getBags(), 1);
            closestNeighbours.add(temp);

            temp = new Neighborhood(items, bags);
            rotateBags(temp.getBags(), 2);
            closestNeighbours.add(temp);

            temp = new Neighborhood(items, bags);
            rotateBags(temp.getBags(), 3);
            closestNeighbours.add(temp);

            temp = new Neighborhood(items, bags);
            rotateUnused(temp.getBags(), temp.getUnusedItems(), 1);
            closestNeighbours.add(temp);

            temp = new Neighborhood(items, bags);
            rotateUnused(temp.getBags(), temp.getUnusedItems(), 2);
            closestNeighbours.add(temp);

            temp = new Neighborhood(items, bags);
            rotateUnused(temp.getBags(), temp.getUnusedItems(), 3);
            closestNeighbours.add(temp);


            lowestNeighbour = closestNeighbours.get(0);      //initial neighbour to compare to

            for (int j = 0; j < closestNeighbours.size(); j++) {    //After generating neighborhood, compare which has lowest value


                if (closestNeighbours.get(j).getTotalPower() <= lowestNeighbour.getTotalPower()  && closestNeighbours.get(j).getBagsItemsNumber() == this.totalItemNumber) {

                	lowestNeighbour = closestNeighbours.get(j);

                    bags = lowestNeighbour.getBags();
                    items = lowestNeighbour.getUnusedItems();



                }
            }

            if (lowestNeighbour.getTotalPower() < globalOptima.getTotalPower()) {
                globalOptima = lowestNeighbour;                      //set new global optima if the local optima is < than previous global
            }

            closestNeighbours.clear();

        }
        System.out.println("\n\n\n----------------------------------------------------------");
        System.out.println("initial result:\n" + initial.toString());
        System.out.println("after neighborhoodsearch:\n" + globalOptima.toString());


    }


    //Utils - maybe move to a new class

    /**
     * Removes an item from a bag and puts it at the end of unused items-list
     *
     * @param bag
     * @param item
     */
    private void removeFromBagToUnused(Bag bag, Item item, ArrayList<Item> unused) {

        Item itemToAdd = bag.removeItem(item, item.getValueIdxThroughBagIdx(bag.getIndex()));

        unused.add(itemToAdd);
    }

    /**
     * Swaps items between bags by first attempting 30 times to remove
     * an random item in the first bag to insert it to the second bag by
     * also removing a random item from that bag (2nd bag). If 30 attempts
     * passed and no swap is made, method ends. If a swap is made, we try
     * to take the removed item from the 2nd bag and insert in the first bag
     * (in order to swap between the two bags) if that cant be made due to lack of
     * space, we will attempt to fit an item from the unused items-list to the first bag.
     *
     * @param bagFrom
     * @param bagTo
     * @return
     */
    private boolean moveFromBagToBag(Bag bagFrom, Bag bagTo) {
        for (int i = 0; i < 30; i++) {
            Item itemToAdd = bagFrom.getRandomItem();
            Item itemToRemove = bagTo.getRandomItem();

            if (itemToRemove == null || itemToAdd == null) {
                return false;
            }

            Item temp = null;
            if ((bagTo.availableMemory() + itemToRemove.getMemory()) >= itemToAdd.getMemory() && bagTo.availableCPU() + itemToRemove.getCPU_capacity() >= itemToAdd.getCPU_capacity()) {
                temp = bagTo.removeItem(itemToRemove, itemToRemove.getValueIdxThroughBagIdx(bagTo.getIndex()));

                bagTo.addItem(itemToAdd, itemToAdd.getValueIdxThroughBagIdx(bagTo.getIndex()));
                bagFrom.removeItem(itemToAdd, itemToAdd.getValueIdxThroughBagIdx(bagFrom.getIndex()));
                //try to move the item removed from first bag to the second bag
                if (bagFrom.availableMemory() >= temp.getMemory() && bagFrom.availableCPU() >= temp.getCPU_capacity()) {
                    bagFrom.addItem(temp, temp.getValueIdxThroughBagIdx(bagFrom.getIndex()));

                }
                else {
                    bagTo.getItems().add(temp); //we have to put it into the unused list if we cant fit it into the other bag
                    addFromUnusedToBag(bagFrom, bagFrom.getItems());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the first item of the unused-items list to a specified bag. If it doesnt fit,
     * it tries the whole list.
     *
     * @param bag
     * @return
     */
    private boolean addFromUnusedToBag(Bag bag, ArrayList<Item> unused) {
        for (int i = 0; i < unused.size(); i++) {
            if (bag.availableMemory() >= unused.get(i).getMemory() && bag.availableCPU() >= unused.get(i).getCPU_capacity()) {
                Item itemRemove = unused.remove(i);
                bag.addItem(itemRemove, itemRemove.getValueIdxThroughBagIdx(bag.getIndex()));
                return true;
            }
        }
        return false;
    }

    //ROTATIONS

    /**
     * Rotate x items between all bags, requires at lest 2 bags
     * if an item doesnt fit, it will just take the next one in list until it goes through the whole list
     * if no item fits, it skips.
     *
     * @param bags
     */
    private boolean rotateBags(Bag[] bags, int times) {
        if (bags.length == 2) {
            Bag bag1 = bags[0], bag2 = bags[1];
            for (int i = 0; i < times; i++)
                moveFromBagToBag(bag1, bag2);
        } else if (bags.length >= 2) {
            //Handle the first bag to swap with the last bag (in list)
            Bag bag1 = bags[0], bag2 = bags[bags.length - 1];
            for (int i = 0; i < times; i++)
                moveFromBagToBag(bag1, bag2);
            //Handle the rest of the bags
            for (int j = 1; j < bags.length; j++) {
                bag1 = bags[j];
                bag2 = bags[j - 1];
                for (int i = 0; i < times; i++)
                    moveFromBagToBag(bag1, bag2);
            }

        }
        return false;
    }

    /**
     * Removes first item in bag and places it last on the unused-items list, then take
     * the first item in the unused-items list, if the items doesnt fit it just skips
     *
     * @param bags
     * @return
     */
    private boolean rotateUnused(Bag[] bags, ArrayList<Item> unused, int times) {
        for (int i = 0; i < bags.length; i++) {
            for (int j = 0; j < times; j++) {

                Item randomItem = bags[i].getRandomItem();

                if (randomItem != null) {
                    removeFromBagToUnused(bags[i], randomItem, unused);
                    addFromUnusedToBag(bags[i], unused);
                }


            }
        }
        return false;
    }


    //For testing purposes
    /*public static void main(String[] args) {
        Samples sample = new Samples();
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
    }*/
}
