package com.work.model;

public class ItemSum {
	private String itemName;
	private String category;
	private double price;
	private int count;
	private double ammount;
	private int rank;
	private long transactionDate;
	
	public ItemSum(){}
	
	public ItemSum(String itemName, double price, int count){
		this.itemName=itemName;
		this.price=price;
		this.count=count;
		this.ammount=price*count;
	}
	
	public ItemSum(String itemName, String category, double price, int count){
		this.itemName=itemName;
		this.category=category;
		this.price=price;
		this.count=count;
		this.ammount=price*count;
	}
	
	public ItemSum(String itemName, String category, double price, int count, long transactionDate){
		this.itemName=itemName;
		this.category=category;
		this.price=price;
		this.count=count;
		this.ammount=price*count;
		this.transactionDate=transactionDate;
	}
	
	public long getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(long transactionDate) {
		this.transactionDate = transactionDate;
	}

	public int getRank() {
		return rank;
	}
	public void setRank(int rank) {
		this.rank = rank;
	}
	public String getItemName() {
		return itemName;
	}
	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public double getAmmount() {
		return ammount;
	}
	public void setAmmount(double ammount) {
		this.ammount = ammount;
	}
	
	public void add(int count){
		this.count+=count;
		setAmmount(price*count);		
	}
	
	public ItemSum add(ItemSum itemSum){
		ItemSum is=new ItemSum(itemName, category, price, count);
		is.add(itemSum.getCount());
		return is;
	}
	
/*	public static ItemSum fromItem(Item item){
		ItemSum is=new ItemSum();
		is.setItemName(item.getItemName());
		is.setCategory(item.getCategory());
		is.setCount(1);
		is.setPrice(item.getPrice());
		is.setAmmount(is.getPrice()*is.getCount());
		return is;
	}*/
	
	
}
