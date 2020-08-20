package pers.jc.sql;

import java.sql.Statement;
import java.util.ArrayList;

public class CURD {
	
	public static void init() {
		try {
			Access.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static <T> ArrayList<T> select(Class<T> modelClass, SQL sql) {
		sql.SELECT_FROM(modelClass);
		return Handle.select(modelClass, sql.toString());
	}
	
	public static <T> T selectOne(Class<T> modelClass, SQL sql) {
		sql.SELECT_FROM(modelClass);
		ArrayList<T> list = Handle.select(modelClass, sql.toString());
		if (list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}
	
	public static <T> ArrayList<T> selectAll(Class<T> modelClass) {
		return Handle.select(modelClass, new SQL(){{
			SELECT_FROM(modelClass);
		}}.toString());
	}
	
	@SuppressWarnings("unchecked")
	public static <T> int insert(T... models) {
		return Handle.insert(Statement.NO_GENERATED_KEYS, models);
	}
	
	public static int insert(SQL sql) {
		return Handle.excuteUpdate(sql.toString(), Statement.NO_GENERATED_KEYS);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> int insertAndGenerateKeys(T... models) {
		return Handle.insert(Statement.RETURN_GENERATED_KEYS, models);
	}
	
	public static int insertAndReturnKey(SQL sql) {
		return Handle.excuteUpdate(sql.toString(), Statement.RETURN_GENERATED_KEYS);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> int update(T... models) {
		return Handle.update(models);
	}
	
	public static int update(SQL sql) {
		return Handle.excuteUpdate(sql.toString(), Statement.NO_GENERATED_KEYS);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> int delete(T... models) {
		return Handle.delete(models);
	}
	
	public static int delete(SQL sql) {
		return Handle.excuteUpdate(sql.toString(), Statement.NO_GENERATED_KEYS);
	}
	
	public static int count(Class<?> modelClass, String columnName) {
		String tableName = null;
		try {
			tableName = Handle.getTableInfo(modelClass).tableName;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Handle.count(tableName, columnName);
	}
	
	public static int count(String tableName, String columnName) {
		return Handle.count(tableName, columnName);
	}
}
