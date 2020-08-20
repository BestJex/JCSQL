package pers.jc.sql;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;

class Handle {
	
	public static int count(String tableName, String columnName) {
		int count = 0;
		Connection connection = Access.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		String sql = "SELECT COUNT(" + columnName + ") AS res FROM " + tableName;
		try {
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			while(resultSet.next()) {
				count = resultSet.getInt("res");
			}
		} catch (Exception e) {
			Access.closeConnection(connection);
			connection = null;
			e.printStackTrace();
		} finally{
			finallyHandle(resultSet, preparedStatement, connection);
		}
		return count;
	}

	public static int excuteUpdate(String sql, int autoGeneratedKeys) {
		int key = 0;
		int updateCount = 0;
		Connection connection = Access.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			preparedStatement = connection.prepareStatement(sql, autoGeneratedKeys);
			updateCount = preparedStatement.executeUpdate();
			if (autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS) {
				if (updateCount > 0) {
					resultSet = preparedStatement.getGeneratedKeys();
					resultSet.next();
					key = resultSet.getInt(1);
				}
			}
		} catch (Exception e) {
			Access.closeConnection(connection);
			connection = null;
			e.printStackTrace();
		} finally{
			finallyHandle(resultSet, preparedStatement, connection);
		}
		if (autoGeneratedKeys == Statement.NO_GENERATED_KEYS) {
			return updateCount;
		}
		if (autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS) {
			return key;
		}
		return 0;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> int delete(T... models) {
		if (!isSameClass(models)) {
			return 0;
		}
		int updateCount = 0;
		Connection connection = Access.getConnection();
		PreparedStatement preparedStatement = null;
		try {
			connection.setAutoCommit(false);
			TableInfo tableInfo = getTableInfo(models[0].getClass());
			if (tableInfo.idInfo == null) {
				throw new Exception();
			}
			String sql = new SQL(){{
				DELETE_FROM(tableInfo.tableName);
				WHERE(tableInfo.idInfo.columnLabel + " = ?");
			}}.toString();
			preparedStatement = connection.prepareStatement(sql);
			for (T model : models) {
				Object id = tableInfo.idInfo.getter.invoke(model, new Object[]{});
				setPreparedStatementValue(preparedStatement, 1, id);
				preparedStatement.addBatch();
			}
			int[] updateCounts = preparedStatement.executeBatch();
			for (int count : updateCounts) {
				updateCount += count;
			}
			connection.commit();
		} catch (Exception e) {
			updateCount = 0;
			try {
				connection.rollback();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally{
			try {
				connection.setAutoCommit(true);
			} catch (Exception e) {
				Access.closeConnection(connection);
				connection = null;
				e.printStackTrace();
			} finally {
				finallyHandle(null, preparedStatement, connection);
			}
		}
		return updateCount;
	}

	@SuppressWarnings("unchecked")
	public static <T> int update(T... models) {
		if (!isSameClass(models)) {
			return 0;
		}
		int updateCount = 0;
		Connection connection = Access.getConnection();
		PreparedStatement preparedStatement = null;
		try {
			connection.setAutoCommit(false);
			TableInfo tableInfo = getTableInfo(models[0].getClass());
			if (tableInfo.idInfo == null) {
				throw new Exception();
			}
			String sql = new SQL(){{
				UPDATE(tableInfo.tableName);
				for (FieldInfo fieldInfo : tableInfo.fieldInfos) {
					if (tableInfo.idInfo.isSameColumn(fieldInfo)) {
						WHERE(fieldInfo.columnLabel + " = ?");
					} else {
						SET(fieldInfo.columnLabel + " = ?");
					}
				}
			}}.toString();
			preparedStatement = connection.prepareStatement(sql);
			for (T model : models) {
				int parameterIndex = 1;
				for (FieldInfo fieldInfo : tableInfo.fieldInfos){
					if (tableInfo.idInfo.isSameColumn(fieldInfo)) {
						continue;
					} else {
						Object value = fieldInfo.getter.invoke(model, new Object[]{});
						setPreparedStatementValue(preparedStatement, parameterIndex, value);
						parameterIndex++;
					}
				}
				Object id = tableInfo.idInfo.getter.invoke(model, new Object[]{});
				setPreparedStatementValue(preparedStatement, parameterIndex, id);
				preparedStatement.addBatch();
			}
			int[] updateCounts = preparedStatement.executeBatch();
			for (int count : updateCounts) {
				updateCount += count;
			}
			connection.commit();
		} catch (Exception e) {
			updateCount = 0;
			try {
				connection.rollback();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally{
			try {
				connection.setAutoCommit(true);
			} catch (Exception e) {
				Access.closeConnection(connection);
				connection = null;
				e.printStackTrace();
			} finally {
				finallyHandle(null, preparedStatement, connection);
			}
		}		
		return updateCount;
	}

	@SuppressWarnings("unchecked")
	public static <T> int insert(int autoGeneratedKeys, T... models) {
		if (!isSameClass(models)) {
			return 0;
		}
		int updateCount = 0;
		Connection connection = Access.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			connection.setAutoCommit(false);
			TableInfo tableInfo = getTableInfo(models[0].getClass());
			String sql = new SQL(){{
				INSERT_INTO(tableInfo.tableName);
				for (FieldInfo fieldInfo : tableInfo.fieldInfos) {
					if (tableInfo.idInfo.isSameColumn(fieldInfo)
						&& tableInfo.idInfo.generatedValue) {
						continue;
					} else {
						VALUES(fieldInfo.columnLabel, "?");
					}
				}
			}}.toString();
			preparedStatement = connection.prepareStatement(sql, autoGeneratedKeys);
			for (T model : models) {
				int parameterIndex = 1;
				for (FieldInfo fieldInfo : tableInfo.fieldInfos) {
					if (tableInfo.idInfo.isSameColumn(fieldInfo)
						&& tableInfo.idInfo.generatedValue) {
						continue;
					} else {
						Object value = fieldInfo.getter.invoke(model, new Object[]{});
						setPreparedStatementValue(preparedStatement, parameterIndex, value);
						parameterIndex++;
					}
				}
				preparedStatement.addBatch();
			}
			int[] updateCounts = preparedStatement.executeBatch();
			for (int count : updateCounts) {
				updateCount += count;
			}
			if (autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS) {
				resultSet = preparedStatement.getGeneratedKeys();
				for (int i = 0; i < updateCounts.length; i++) {
					if (updateCounts[i] > 0) {
						resultSet.next();
						int key = resultSet.getInt(1);
						tableInfo.idInfo.setter.invoke(models[i], new Object[]{key});
					}
				}
			}
			connection.commit();
		} catch (Exception e) {
			updateCount = 0;
			try {
				connection.rollback();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally{
			try {
				connection.setAutoCommit(true);
			} catch (Exception e) {
				Access.closeConnection(connection);
				connection = null;
				e.printStackTrace();
			} finally {
				finallyHandle(resultSet, preparedStatement, connection);
			}
		}
		return updateCount;
	}

	public static <T> ArrayList<T> select(Class<T> modelClass, String sql) {
		ArrayList<T> list = new ArrayList<>();
		Connection connection = Access.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			TableInfo tableInfo = getTableInfo(modelClass);
			while (resultSet.next()) {
				T model = modelClass.newInstance();
				for (FieldInfo fieldInfo : tableInfo.fieldInfos) {
					fieldInfo.setter.invoke(model, getResultSetValue(resultSet, fieldInfo.columnLabel, fieldInfo.type));
				}
				list.add(model);
			}
		} catch (Exception e) {
			Access.closeConnection(connection);
			connection = null;
			e.printStackTrace();
		} finally {
			finallyHandle(resultSet, preparedStatement, connection);
		}
		return list;
	}
	
	private static void finallyHandle(ResultSet resultSet, PreparedStatement preparedStatement, Connection connection) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (preparedStatement != null) {
			try {
				preparedStatement.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (connection != null) {
			Access.pool.add(connection);
		}
	}
	
	public static void setPreparedStatementValue(PreparedStatement preparedStatement, int parameterIndex, Object value) throws Exception { 
		Method method = preparedStatement.getClass().getMethod("set" + getTypeName(value.getClass()), new Class<?>[]{int.class, getType(value.getClass())});
		method.invoke(preparedStatement, new Object[]{parameterIndex, value});
	}
	
	public static Object getResultSetValue(ResultSet resultSet, String columnLabel, Class<?> type) throws Exception {
		Method method = resultSet.getClass().getMethod("get" + getTypeName(type), new Class<?>[]{String.class});
		return method.invoke(resultSet, new Object[]{columnLabel});
	}
	
	public static boolean isSameClass(Object[] models) {
		if (models.length >= 1) {
			for (int i = 1; i < models.length; i++) {
				if (!models[i].getClass().equals(models[i-1])) {
					continue;
				} else {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	private static Class<?> getType(Class<?> type){
		if (type == String.class) {
			return String.class;
		}
		if (type == Integer.class || type == int.class) {
			return int.class;
		}
		if (type == Long.class || type == long.class) {
			return long.class;
		}
		if (type == Float.class || type == float.class) {
			return float.class;
		}
		if (type == Double.class || type == double.class) {
			return double.class;
		}
		if (type == Boolean.class || type == boolean.class) {
			return boolean.class;
		}
		if (type == Date.class) {
			return Date.class;
		}
		if (type == Time.class) {
			return Time.class;
		}
		if (type == Timestamp.class) {
			return Timestamp.class;
		}
		return null;
	}
	
	private static String getTypeName(Class<?> type) {
		if (type == String.class) {
			return "String";
		}
		if (type == Integer.class || type == int.class) {
			return "Int";
		}
		if (type == Long.class || type == long.class) {
			return "Long";
		}
		if (type == Float.class || type == float.class) {
			return "Float";
		}
		if (type == Double.class || type == double.class) {
			return "Double";
		}
		if (type == Boolean.class || type == boolean.class) {
			return "Boolean";
		}
		if (type == Date.class) {
			return "Date";
		}
		if (type == Time.class) {
			return "Time";
		}
		if (type == Timestamp.class) {
			return "Timestamp";
		}
		return null;
	}
	
	public static TableInfo getTableInfo(Class<?> modelClass) throws Exception {
		TableInfo tableInfo = new TableInfo();
		
		Table table = modelClass.getAnnotation(Table.class);
		if (table.value().equals("")) {
			tableInfo.tableName = modelClass.getSimpleName();
		} else {
			tableInfo.tableName = table.value();
		}
		
		tableInfo.fieldInfos = new ArrayList<>();
		for (Field field : modelClass.getDeclaredFields()) {
			Id id = field.getAnnotation(Id.class);
			Column column = field.getAnnotation(Column.class);
			if (id != null || column != null) {
				FieldInfo fieldInfo = new FieldInfo();
				fieldInfo.type = field.getType();
				fieldInfo.getter = modelClass.getMethod(
					((field.getType() == boolean.class || field.getType() == Boolean.class) ? "is" : "get")
					+ field.getName().substring(0, 1).toUpperCase()
					+ field.getName().substring(1), new Class<?>[]{});
				fieldInfo.setter = modelClass.getMethod("set" 
					+ field.getName().substring(0, 1).toUpperCase()
					+ field.getName().substring(1), new Class<?>[]{field.getType()});
				if (id != null) {
					fieldInfo.columnLabel = id.value().equals("") ? field.getName() : id.value();
					tableInfo.idInfo = new IdInfo(fieldInfo);
					GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
					if (generatedValue == null) {
						tableInfo.idInfo.generatedValue = false;
					} else {
						tableInfo.idInfo.generatedValue = true;
					}
				}
				if (column != null) {
					fieldInfo.columnLabel = column.value().equals("") ? field.getName() : column.value();
				}
				tableInfo.fieldInfos.add(fieldInfo);
			}
		}
		return tableInfo;
	}
}
class TableInfo {
	public String tableName;
	public IdInfo idInfo;
	public ArrayList<FieldInfo> fieldInfos;
}
class FieldInfo {
	public Class<?> type;
	public Method getter;
	public Method setter;
	public String columnLabel;
}
class IdInfo extends FieldInfo {
	public boolean generatedValue;
	
	public IdInfo(FieldInfo fieldInfo) {
		this.type = fieldInfo.type;
		this.getter = fieldInfo.getter;
		this.setter = fieldInfo.setter;
		this.columnLabel = fieldInfo.columnLabel;
	}
	
	public boolean isSameColumn(FieldInfo fieldInfo) {
		return fieldInfo.columnLabel.equals(this.columnLabel);
	}
}
