package com.training;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Test {

	public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException, InstantiationException, IllegalArgumentException {
		// TODO Auto-generated method stub
		
		
		Class c = Class.forName("com.training.Employee");
		
		Object employeeObject = c.getDeclaredConstructor().newInstance();
		
//		String type = c.getTypeName();
//		System.out.println("Type of class :" + type);
//		
		Field[] fields =  c.getDeclaredFields();
//		
		for(Field f : fields) {
		
			System.out.println("field type :" + f.getType().getSimpleName() + "  Name of the field: " + f.getName());
		
			if(f.getName() == "salary") {
				f.set(employeeObject, 1000.00);
			}
		}		

		Method method =  c.getMethod("increaseSalary", double.class);
		method.invoke(employeeObject, 10.00);

	}

}
