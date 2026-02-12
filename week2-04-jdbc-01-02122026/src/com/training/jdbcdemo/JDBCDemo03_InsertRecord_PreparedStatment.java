package com.training.jdbcdemo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCDemo03_InsertRecord_PreparedStatment {

	public static void main(String[] args)  {
		// TODO Auto-generated method stub
		
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			
			//1.load the driver class 
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			
			//2.create the connection 
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/revadb","root","Root123");
			
			
			//3. create statement object 
			
			String sql = "INSERT INTO Employee VALUES (?, ?, ?, ?)";
			
			statement = connection.prepareStatement(sql);
			
			Employee employee = new Employee(104, "Samiksha", "Nagpur", 20000);
			
			statement.setInt(1, employee.getId());
			statement.setString(2, employee.getName());
			statement.setString(3, employee.getCity());
			statement.setInt(4, employee.getSalary());
	
			int rows = statement.executeUpdate();
			
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
