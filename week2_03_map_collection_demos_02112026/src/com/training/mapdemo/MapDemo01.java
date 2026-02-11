package com.training.mapdemo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapDemo01 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Map<Integer, String> map = new HashMap<>();
		
		
		map.put(101,  "Rohit");
		map.put(102,  "Mohit");
		map.put(103,  "Amit");
		map.put(104,  "Sumit");
		map.put(105,  "Mahesh");
		
		map.put(101, "Suresh");
		
		
//		System.out.println(map.get(101));
		
		//3 ways to iterate over maps 
		
//		Collection<String> names = map.values();
//		
//		for(String name : names) {
//			System.out.println(name);
//		}
		
		
			
//		Set<Integer> keys = map.keySet();
//		
//		for(Integer key: keys) {
//			System.out.println(map.get(key));
//		}
		
		Set<Map.Entry<Integer, String>> set = map.entrySet();
		
		for(Map.Entry<Integer, String> entry : set) {
			System.out.println("key :" + entry.getKey() + "  Value: " + entry.getValue());
		}

	}

}
