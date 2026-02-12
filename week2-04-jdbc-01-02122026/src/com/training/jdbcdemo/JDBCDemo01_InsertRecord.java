package com.training.jdbcdemo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCDemo01_InsertRecord {

	public static void main(String[] args)  {
		// TODO Auto-generated method stub

		Connection connection = null;
		Statement statement = null;
		try {
			
			//1.load the driver class 
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			
			//2.create the connection 
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/revadb","root","Root123");
			
			
			//3. create statement object 
			
			statement = connection.createStatement();
			
			Employee employee = new Employee(103, "Rutuja", "Satara", 30000);
	
			//4. execute the sql command 
            String insertCommand = "INSERT INTO Employee VALUES("+employee.getId()+", '"+employee.getName()+"', '"+employee.getCity()+"', "+employee.getSalary()+")";

			int rows = statement.executeUpdate(insertCommand);
			
			if(rows > 0)
				System.out.println("Employee Record inserted");
			else 
				System.out.println("Failed to insert employee record");
			
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			
			if(connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
