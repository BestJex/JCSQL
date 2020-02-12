package pers.jc.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DBA {
	private static String url;
	private static String username;
	private static String password;
	protected static ConcurrentLinkedQueue<Connection> pool = new ConcurrentLinkedQueue<>();
	private static int amount = 0;
	private static int minIdle = 5;
	private static int maxActive = 20;
	private static long clearInterval = 3000;
	
	public static void init() throws IOException, ClassNotFoundException {
		Properties properties = new Properties();
		properties.load(DBA.class.getResourceAsStream("/sql.properties"));
		
		url = "jdbc:mysql://"+properties.getProperty("url") 
			+ "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false";
		username = properties.getProperty("username");
		password = properties.getProperty("password");
		
		String minIdleValue = properties.getProperty("minIdle");
		String maxActiveValue = properties.getProperty("maxActive");
		String clearIntervalValue = properties.getProperty("clearInterval");
		
		if (minIdleValue != null) {
			minIdle = Integer.valueOf(minIdleValue);
		}
		if (maxActiveValue != null) {
			maxActive = Integer.valueOf(maxActiveValue);
		}
		if (clearIntervalValue != null) {
			clearInterval = Integer.valueOf(clearIntervalValue);
		}
		
		Class.forName("com.mysql.cj.jdbc.Driver");
		
		keepMinIdle();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					try {
						Thread.sleep(clearInterval);
						closeConnection(pool.poll());
						keepMinIdle();
					} catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
		
	private static void keepMinIdle() {
		while (pool.size() < minIdle) {
			Connection connection = newConnection();
			if (connection != null) {
				pool.add(connection);
			} else {
				break;
			}
		}
	}
	
	private static Connection newConnection() {
		if(readyNewConnection()){
			try {
				return DriverManager.getConnection(url, username, password);
			} catch (SQLException e) {
				e.printStackTrace();
				getAmount(-1);
				return null;
			}
		}
		return null;
	}
	
	protected static Connection getConnection() {
		Connection connection = pool.poll();
		if (connection != null) {
			return connection;
		}
		connection = newConnection();
		if (connection != null) {
			return connection;
		}
		synchronized (DBA.class) {
			while((connection = pool.poll()) == null){}
			return connection;
		}
	}
	
	private static synchronized void closeConnection(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
				getAmount(-1);
			} catch (SQLException e){
				e.printStackTrace();
			}
		}
	}
	
	private static synchronized boolean readyNewConnection() {
		if (getAmount(0) < maxActive) {
			getAmount(1);
			return true;
		}
		return false;
	}
	
	private static synchronized int getAmount(int variety) {
		amount += variety;
		return amount;
	}
}

