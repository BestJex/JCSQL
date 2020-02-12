package pers.jc.sql;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

public class DBO {
	public static int delete(Class<?> modelClass, Object[] ids) {
		int deleteRowCount = 0;
		Connection connection = DBA.getConnection();
		PreparedStatement preparedStatement = null;
		try {
			TableInfo tableInfo = getTableInfo(modelClass);
			if (tableInfo.idInfo == null) {
				throw new Exception();
			}
			String sql = "DELETE FROM " 
				+ tableInfo.tableName 
				+ " WHERE "
				+ tableInfo.idInfo.columnLabel
				+ " in (";
			boolean firstColumn = true;
			for (int i = 0; i < ids.length; i++) {
				if(!firstColumn){
					sql += ',';
				}
				firstColumn = false;
				sql += '?';
			}
			sql += ')';
			preparedStatement = connection.prepareStatement(sql);
			int parameterIndex = 1;
			for (Object id: ids) {
				setPreparedStatementValue(preparedStatement, parameterIndex, id);
				parameterIndex++;
			}
			deleteRowCount = preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			finallyHandle(null, preparedStatement, connection);
		}
		return deleteRowCount;
	}
	
	public static int delete(Class<?> modelClass, String condition) {
		Table table = modelClass.getAnnotation(Table.class);
		String tableName = table.name().equals("") ? modelClass.getSimpleName() : table.name();
		return delete(tableName, condition);
	}
	
	public static int delete(String tableName, String condition) {
		int deleteRowCount = 0;
		Connection connection = DBA.getConnection();
		PreparedStatement preparedStatement = null;
		try {
			String sql = "DELETE FROM " + tableName + " " + condition;
			preparedStatement = connection.prepareStatement(sql);
			deleteRowCount = preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			finallyHandle(null, preparedStatement, connection);
		}
		return deleteRowCount;
	}
	
	public static int update(Object model) {
		int updateRowCount = 0;
		Connection connection = DBA.getConnection();
		PreparedStatement preparedStatement = null;
		try {
			TableInfo tableInfo = getTableInfo(model.getClass());
			if (tableInfo.idInfo == null) {
				throw new Exception();
			}
			String sql = "UPDATE " + tableInfo.tableName + " SET ";
			boolean firstColumn = true;
			for (FieldInfo fieldInfo : tableInfo.fieldInfos) {
				if (fieldInfo.columnLabel.equals(tableInfo.idInfo.columnLabel)) {
					continue;
				} else {
					if(!firstColumn){
						sql += ',';
					}
					firstColumn = false;
					sql += fieldInfo.columnLabel + " = ?";
				}
			}
			sql += " WHERE " + tableInfo.idInfo.columnLabel + " = ?";
			preparedStatement = connection.prepareStatement(sql);
			int parameterIndex = 1;
			for (FieldInfo fieldInfo : tableInfo.fieldInfos){
				if (fieldInfo.columnLabel.equals(tableInfo.idInfo.columnLabel)) {
					continue;
				} else {
					Object value = fieldInfo.getter.invoke(model, new Object[]{});
					setPreparedStatementValue(preparedStatement, parameterIndex, value);
					parameterIndex++;
				}
			}
			Object value = tableInfo.idInfo.getter.invoke(model, new Object[]{});
			setPreparedStatementValue(preparedStatement, parameterIndex, value);
			updateRowCount = preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			finallyHandle(null, preparedStatement, connection);
		}
		return updateRowCount;
	}
	
	public static <T> T insert(T model) {
		Connection connection = DBA.getConnection();
		PreparedStatement preparedStatement = null;
		try {
			TableInfo tableInfo = getTableInfo(model.getClass());
			String sqlHead = "INSERT INTO " + tableInfo.tableName + '(';
			String sqlTail = "VALUES (";
			boolean firstColumn = true;
			for (FieldInfo fieldInfo : tableInfo.fieldInfos) {
				if (fieldInfo.columnLabel.equals(tableInfo.idInfo.columnLabel) && tableInfo.idInfo.autoIncrement) {
					continue;
				} else {
					if (!firstColumn) {
						sqlHead += ',';
						sqlTail += ',';
					}
					firstColumn = false;
					sqlHead += fieldInfo.columnLabel;
					sqlTail += '?';
				}
			}
			sqlHead += ") ";
			sqlTail += ')';
			String sql = sqlHead + sqlTail;
			preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			int parameterIndex = 1;
			for (FieldInfo fieldInfo : tableInfo.fieldInfos) {
				if (fieldInfo.columnLabel.equals(tableInfo.idInfo.columnLabel) 
						&& tableInfo.idInfo.autoIncrement) {
					continue;
				} else {
					Object value = fieldInfo.getter.invoke(model, new Object[]{});
					setPreparedStatementValue(preparedStatement, parameterIndex, value);
					parameterIndex++;
				}
			}
			int row = preparedStatement.executeUpdate();
			if (tableInfo.idInfo.autoIncrement && tableInfo.idInfo.autoUpdate) {
				if(row > 0) { 
					ResultSet resultSet = preparedStatement.getGeneratedKeys();
					resultSet.next();
					Method method = model.getClass().getMethod("setId", new Class<?>[]{tableInfo.idInfo.type});
					method.invoke(model, new Object[]{resultSet.getInt(1)});
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			finallyHandle(null, preparedStatement, connection);
		}
		return model;
	}

	public static <T> ArrayList<T> selectAll(Class<T> modelClass) {
		return select(modelClass, "");
	}
	
	public static <T> T selectOne(Class<T> modelClass, String condition) {
		return select(modelClass, condition).get(0);
	}
	
	public static <T> ArrayList<T> select(Class<T> modelClass, String condition){
		ArrayList<T> list = new ArrayList<>();
		Connection connection = DBA.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			TableInfo tableInfo = getTableInfo(modelClass);
			String sql = "SELECT ";
			for (int i = 0; i < tableInfo.fieldInfos.size(); i++) {
				if (i > 0) {
					sql += ',';
				}
				sql += tableInfo.fieldInfos.get(i).columnLabel;
			}
			sql += " FROM " + tableInfo.tableName + " " + condition;
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				T model = modelClass.newInstance();
				for (FieldInfo fieldInfo : tableInfo.fieldInfos) {
					fieldInfo.setter.invoke(model, getResultSetValue(resultSet, fieldInfo.columnLabel, fieldInfo.type));
				}
				list.add(model);
			}
		} catch (Exception e) {
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
		DBA.pool.add(connection);
	}
	
	private static void setPreparedStatementValue(PreparedStatement preparedStatement
			, int parameterIndex, Object value) throws Exception { 
		Method method = preparedStatement.getClass().getMethod("set"
			+getTypeName(value.getClass()), new Class<?>[]{int.class, getType(value.getClass())});
		method.invoke(preparedStatement, new Object[]{parameterIndex, value});
	}
	
	private static Object getResultSetValue(ResultSet resultSet
			, String columnLabel, Class<?> type) throws Exception {
		Method method = resultSet.getClass().getMethod("get"
			+getTypeName(type), new Class<?>[]{String.class});
		return method.invoke(resultSet, new Object[]{columnLabel});
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
	
	private static TableInfo getTableInfo(Class<?> modelClass) throws Exception {
		TableInfo tableInfo = new TableInfo();
		Table table = modelClass.getAnnotation(Table.class);
		if (table.name().equals("")) {
			String className = modelClass.getSimpleName();
			if (table.underline()) {
				String newTableName = "";
				char[] charArray = className.toCharArray();
				for (int i = 0; i < charArray.length; i++) {
					if (Character.isUpperCase(charArray[i])) {
						if (newTableName.length() > 0) {
							newTableName += '_';
						}
						newTableName += Character.toLowerCase(charArray[i]);
					} else {
						newTableName += charArray[i];
					}
				}
				tableInfo.tableName = newTableName;
			} else {
				tableInfo.tableName = className;
			}
		} else {
			tableInfo.tableName = table.name();
		}
		tableInfo.fieldInfos = new ArrayList<>();
		for(Field field : modelClass.getDeclaredFields()){
			Id id = field.getAnnotation(Id.class);
			Column column = field.getAnnotation(Column.class);
			if(id != null || column != null){
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
					fieldInfo.columnLabel = id.name().equals("") ? field.getName() : id.name();
					tableInfo.idInfo = new IdInfo();
					tableInfo.idInfo.type = fieldInfo.type;
					tableInfo.idInfo.getter = fieldInfo.getter;
					tableInfo.idInfo.setter = fieldInfo.setter;
					tableInfo.idInfo.columnLabel = fieldInfo.columnLabel;
					tableInfo.idInfo.autoIncrement = id.autoIncrement();
					tableInfo.idInfo.autoUpdate = id.autoUpdate();
				}
				if (column != null) {
					fieldInfo.columnLabel = column.name().equals("") ? field.getName() : column.name();
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
	public boolean autoIncrement;
	public boolean autoUpdate;
}
