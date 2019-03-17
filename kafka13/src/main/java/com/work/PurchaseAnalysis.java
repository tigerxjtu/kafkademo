package com.work;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.KeyValueMapper;

import com.alibaba.fastjson.JSON;
import com.work.model.Category;
import com.work.model.Item;
import com.work.model.ItemSum;
import com.work.model.Order;
import com.work.model.User;
import com.work.serdes.SerdesFactory;
import com.work.timeextractor.OrderTimestampExtractor;


public class PurchaseAnalysis {

	public static void main(String[] args) throws IOException, InterruptedException {
		Properties props = new Properties();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-purchase-analysis3");
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.1.21:9092");
		props.put(StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, "192.168.1.21:2181");
		props.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		props.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(StreamsConfig.TIMESTAMP_EXTRACTOR_CLASS_CONFIG, OrderTimestampExtractor.class);
		//props.put(StreamsConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG, 1000*60*60); // one hour
		
		KStreamBuilder streamBuilder = new KStreamBuilder();
		KStream<String, Order> orderStream = streamBuilder.stream(Serdes.String(), SerdesFactory.serdFrom(Order.class), "orders2");
		KTable<String, User> userTable = streamBuilder.table(Serdes.String(), SerdesFactory.serdFrom(User.class), "users2", "users-state-store");
		KTable<String, Item> itemTable = streamBuilder.table(Serdes.String(), SerdesFactory.serdFrom(Item.class), "items2", "items-state-store");
		//itemTable.toStream().foreach((String itemName, Item item) -> System.out.printf("Item info %s-%s-%s-%s\n", item.getItemName(), item.getAddress(), item.getType(), item.getPrice()));
		KStream<String, ItemSum> kStreamDetail = orderStream
				.leftJoin(userTable, (Order order, User user) -> OrderUser.fromOrderUser(order, user), Serdes.String(), SerdesFactory.serdFrom(Order.class))
				.filter((String userName, OrderUser orderUser) -> orderUser.userAddress != null)
				.map((String userName, OrderUser orderUser) -> new KeyValue<String, OrderUser>(orderUser.itemName, orderUser))
				.through(Serdes.String(), SerdesFactory.serdFrom(OrderUser.class), (String key, OrderUser orderUser, int numPartitions) -> (orderUser.getItemName().hashCode() & 0x7FFFFFFF) % numPartitions, "orderuser2-repartition-by-item")
				.leftJoin(itemTable, (OrderUser orderUser, Item item) -> OrderUserItem.fromOrderUser(orderUser, item), Serdes.String(), SerdesFactory.serdFrom(OrderUser.class))
				.filter((String item, OrderUserItem orderUserItem) -> orderUserItem.getAge()>=18 && orderUserItem.getAge()<=35) //to ensure item and user in same address
				//.map((String item, OrderUserItem orderUserItem) -> KeyValue.<String, OrderSum>pair(orderUserItem.userAddress+","+orderUserItem.gender, OrderSum.fromOrderUserItem(orderUserItem)))
				.map((String item, OrderUserItem orderUserItem) -> KeyValue.<String, ItemSum>pair(orderUserItem.getItemType(), 
						new ItemSum(orderUserItem.getItemName(), orderUserItem.getItemType(),
								orderUserItem.getItemPrice(),orderUserItem.getQuantity(), orderUserItem.getTransactionDate())));
		//kStreamDetail.foreach((str, dou) -> System.out.printf("%s-%s\n", str, JSON.toJSONString(dou)));
		KTable<String, Category> kTable=kStreamDetail.groupByKey(Serdes.String(), SerdesFactory.serdFrom(ItemSum.class))
				.aggregate(new Initializer<Category>(){
					@Override
					public Category apply() {
						return new Category();
					}}, new Aggregator<String, ItemSum, Category>(){
						@Override
						public Category apply(String aggKey, ItemSum value, Category aggregate) {
							if (StringUtils.isBlank(aggregate.getCategory())){
								aggregate.setCategory(aggKey);
							}
							aggregate.addItem(value);
							return aggregate;
						}},
						SerdesFactory.serdFrom(Category.class), "category");
						//TimeWindows.of(60*60*1000L).advanceBy(5000L),SerdesFactory.serdFrom(Category.class), "category-windwo");
		kTable.foreach((str, dou) -> System.out.printf("%s-%s\n", JSON.toJSONString(str), JSON.toJSONString(dou)));
		
		//kTable.toStream().map
		kTable.toStream().map(new KeyValueMapper<String, Category, KeyValue<String, String>>(){
			@Override
			public KeyValue<String, String> apply(String key, Category value) {
				
				return new KeyValue<String, String>(key,JSON.toJSONString(value.orderItemSum()));
			}	
		}).to("category-item-sum");
		/*kTable
			.toStream()
			.map((Windowed<String> key, Category cat) -> new KeyValue<String, String>(key.toString(), JSON.toJSONString(cat)))
			.to("category-item-sum");*/
		
		KafkaStreams kafkaStreams = new KafkaStreams(streamBuilder, props);
		kafkaStreams.cleanUp();
		kafkaStreams.start();
		
		System.in.read();
		kafkaStreams.close();
		kafkaStreams.cleanUp();
	}
	
	public static class OrderUser {
		private String userName;
		private String itemName;
		private long transactionDate;
		private int quantity;
		private String userAddress;
		private String gender;
		private int age;
		
		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		public String getItemName() {
			return itemName;
		}

		public void setItemName(String itemName) {
			this.itemName = itemName;
		}

		public long getTransactionDate() {
			return transactionDate;
		}

		public void setTransactionDate(long transactionDate) {
			this.transactionDate = transactionDate;
		}

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}

		public String getUserAddress() {
			return userAddress;
		}

		public void setUserAddress(String userAddress) {
			this.userAddress = userAddress;
		}

		public String getGender() {
			return gender;
		}

		public void setGender(String gender) {
			this.gender = gender;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public static OrderUser fromOrder(Order order) {
			OrderUser orderUser = new OrderUser();
			if(order == null) {
				return orderUser;
			}
			orderUser.userName = order.getUserName();
			orderUser.itemName = order.getItemName();
			orderUser.transactionDate = order.getTransactionDate();
			orderUser.quantity = order.getQuantity();
			return orderUser;
		}
		
		public static OrderUser fromOrderUser(Order order, User user) {
			OrderUser orderUser = fromOrder(order);
			if(user == null) {
				return orderUser;
			}
			orderUser.gender = user.getGender();
			orderUser.age = user.getAge();
			orderUser.userAddress = user.getAddress();
			return orderUser;
		}
	}
	
	public static class OrderUserItem {
		private String userName;
		private String itemName;
		private long transactionDate;
		private int quantity;
		private String userAddress;
		private String gender;
		private int age;
		private String itemAddress;
		private String itemType;
		private double itemPrice;
		
		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		public String getItemName() {
			return itemName;
		}

		public void setItemName(String itemName) {
			this.itemName = itemName;
		}

		public long getTransactionDate() {
			return transactionDate;
		}

		public void setTransactionDate(long transactionDate) {
			this.transactionDate = transactionDate;
		}

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}

		public String getUserAddress() {
			return userAddress;
		}

		public void setUserAddress(String userAddress) {
			this.userAddress = userAddress;
		}

		public String getGender() {
			return gender;
		}

		public void setGender(String gender) {
			this.gender = gender;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public String getItemAddress() {
			return itemAddress;
		}

		public void setItemAddress(String itemAddress) {
			this.itemAddress = itemAddress;
		}

		public String getItemType() {
			return itemType;
		}

		public void setItemType(String itemType) {
			this.itemType = itemType;
		}

		public double getItemPrice() {
			return itemPrice;
		}

		public void setItemPrice(double itemPrice) {
			this.itemPrice = itemPrice;
		}

		public static OrderUserItem fromOrderUser(OrderUser orderUser) {
			OrderUserItem orderUserItem = new OrderUserItem();
			if(orderUser == null) {
				return orderUserItem;
			}
			orderUserItem.userName = orderUser.userName;
			orderUserItem.itemName = orderUser.itemName;
			orderUserItem.transactionDate = orderUser.transactionDate;
			orderUserItem.quantity = orderUser.quantity;
			orderUserItem.userAddress = orderUser.userAddress;
			orderUserItem.gender = orderUser.gender;
			orderUserItem.age = orderUser.age;
			return orderUserItem;
		}

		public static OrderUserItem fromOrderUser(OrderUser orderUser, Item item) {
			OrderUserItem orderUserItem = fromOrderUser(orderUser);
			if(item == null) {
				return orderUserItem;
			}
			orderUserItem.itemAddress = item.getAddress();
			orderUserItem.itemType = item.getType();
			orderUserItem.itemPrice = item.getPrice();
			return orderUserItem;
		}
	}
}
