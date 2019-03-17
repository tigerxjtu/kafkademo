package com.work.model;

import java.util.ArrayList;
import java.util.List;

public class CategoryItemSum {
	private String category;
	private List<ItemSum> topItems=new ArrayList<>();
	
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public List<ItemSum> getTopItems() {
		return topItems;
	}
	public void setTopItems(List<ItemSum> topItems) {
		this.topItems = topItems;
	}
	
	
	
	
}
