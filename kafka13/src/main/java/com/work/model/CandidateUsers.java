package com.work.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CandidateUsers {

	private static List<User> users=generateUsers();
	
	private static List<User> generateUsers(){
		List<User> list=new ArrayList<User>();
		User user1=new User("user1","BJ","male",29);
		User user2=new User("user2","SH","female",39);
		list.add(user1);
		list.add(user2);
		return list;
	}
	
	public static User randomUser(){
		Random r=new Random();
		int i=r.nextInt(users.size());
		return users.get(i);
		
	}
}
