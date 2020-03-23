package pers.jc.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

class Access {
	private static String url;
	private static String username;
	private static String password;
	protected static ConcurrentLinkedQueue<Connection> pool = new ConcurrentLinkedQueue<>();
	private static volatile int activeCount = 0;
	private static int minIdle = 5;
	private static int maxActive = 20;
	private static long clearInterval = 3000;
	
	protected static void init() throws Exception {
		Properties properties = new Properties();
		properties.load(Access.class.getResourceAsStream("/sql.properties"));
		
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
		
		Class.forName(properties.getProperty("driver"));
		
		keepMinIdle();
		
		new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(clearInterval);
					closeConnection(pool.poll());
					keepMinIdle();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
		
	private static void keepMinIdle() {
		while (pool.size() < minIdle && activeCount < maxActive) {
			Connection connection = createConnection();
			if (connection != null) {
				pool.add(connection);
			} else {
				break;
			}
		}
	}
	
	protected static Connection getConnection() {
		Connection connection = pool.poll();
		if (connection != null) {
			return connection;
		}
		connection = createConnection();
		if (connection != null) {
			return connection;
		}
		synchronized ("waiting for a connection") {
			while((connection = pool.poll()) == null){}
			return connection;
		}
	}
	
	private static Connection createConnection() {
		if(addActiveCount()){
			try {
				return DriverManager.getConnection(url, username, password);
			} catch (Exception e) {
				subActiveCount();
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	public static void closeConnection(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				subActiveCount();
			}
		}
	}
	
	private static boolean addActiveCount() {
		synchronized ("add or sub count") {
			if (activeCount < maxActive) {
				activeCount++;
				return true;
			}
			return false;
		}
	}
	
	private static boolean subActiveCount() {
		synchronized ("add or sub count") {
			if (activeCount > 0) {
				activeCount--;
				return true;
			}
			return false;
		}
	}
}

