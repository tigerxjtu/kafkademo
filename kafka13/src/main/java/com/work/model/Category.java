package com.work.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Category {

	private String category;
	private Map<String, ItemSum> items=new HashMap<>();
	
	
	public Map<String, ItemSum> getItems() {
		return items;
	}

	public void setItems(Map<String, ItemSum> items) {
		this.items = items;
	}

	public Category(){}
	
	public Category(String category){
		this.category=category;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}
	
	public void putItem(String itemName, double price, int count){
		ItemSum itemSum=new ItemSum(itemName, price, count);
		items.put(itemName, itemSum);
	}
	
	public void addItem(String itemName, int count){
		ItemSum itemSum=items.get(itemName);
		itemSum.add(count);
	}
	
	public void addItem(ItemSum itemSum){
		//System.out.println("----addItem:"+JSON.toJSONString(itemSum.getItemName()));
		ItemSum is=items.get(itemSum.getItemName());
		if (is==null){
			//System.out.println("----putItem:"+itemSum.getItemName());
			items.put(itemSum.getItemName(), itemSum);
		}else{
			is.add(itemSum.getCount());
		}
		
	}
	
	private List<ItemSum> orderItems(){
		List<ItemSum> topItems=new ArrayList<>();
		List<ItemSum> allItems=new ArrayList<>(items.values());
		allItems.sort(new Comparator<ItemSum>(){
			@Override
			public int compare(ItemSum is1, ItemSum is2) {
				return (int)(is2.getAmmount()-is1.getAmmount());
			}			
		});
		int rank=0;
		for (int i=0; i<10 && i<allItems.size(); i++){
			ItemSum itemSum=allItems.get(i);
			itemSum.setRank(++rank);
			topItems.add(itemSum);
		}
		return topItems;
	}
	
	public CategoryItemSum orderItemSum(){
		CategoryItemSum catItems=new CategoryItemSum();
		catItems.setCategory(category);
		catItems.setTopItems(orderItems());
		return catItems;
	}
	
}
