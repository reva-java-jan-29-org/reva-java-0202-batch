package com.training.jdbcdemo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JDBCDemo04_selectCommand {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Connection connection = null;
		PreparedStatement statement = null;
		
		try {
			
			//1.load the driver class 
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			
			//2.create the connection 
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/revadb","root","Root123");
			
			
			//3. create statement object 
			
			String sql = "SELECT id, name, city, salary FROM Employee";
			
			statement = connection.prepareStatement(sql);
			
			ResultSet resultSet = statement.executeQuery();
			
			List<Employee> list = new ArrayList<>();
			
			while(resultSet.next()) {
				
				int id = resultSet.getInt("id");
				String name = resultSet.getString("name");
				String city = resultSet.getString("city");
				int salary = resultSet.getInt("salary");
				
				Employee employee = new Employee(id, name, city, salary);
				System.out.println(employee);
				
				list.add(employee);
			}
			
			resultSet.close();	
			
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
