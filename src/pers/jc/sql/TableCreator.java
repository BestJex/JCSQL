package pers.jc.sql;

//import java.io.File;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Enumeration;
//import java.util.List;
//import per.jc.test.Test;

public class TableCreator {

//	private static void createTable() {
//		TableInfo tableInfo = null;
//		String sql = "DROP TABLE IF EXISTS `" + tableInfo.tableName + "`;";
//		sql += "'CREATE TABLE `" + tableInfo.tableName + "` ('";
//		if (tableInfo.idInfo != null) {
//			
//		}
//	}
//	
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	private static List<TableInfo> loadTableInfos() {
//		List<TableInfo> list = new ArrayList<>();
//		List<Class> classes;
//		try {
//			classes = loadClassByLoader(Test.class.getClassLoader());
//			for (Class class1 : classes) {
//				if (class1.getAnnotation(TableInfo.class) != null) {
//					list.add(Handle.getTableInfo(class1));
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return list;
//	}
//	
//	@SuppressWarnings("rawtypes")
//	private static List<Class> loadClassByLoader(ClassLoader load) throws Exception{
//        Enumeration<URL> urls = load.getResources("");
//        List<Class> classes = new ArrayList<Class>();
//        while (urls.hasMoreElements()) {
//            URL url = urls.nextElement();
//            if (url.getProtocol().equals("file")) {
//                loadClassByPath(null, url.getPath(), classes, load);
//            }
//        }
//        return classes;
//    }
//    @SuppressWarnings("rawtypes")
//    private static void loadClassByPath(String root, String path, List<Class> list, ClassLoader load) {
//        File f = new File(path);
//        if(root==null) root = f.getPath();
//        if (f.isFile() && f.getName().matches("^.*\\.class$")) {
//            try {
//                String classPath = f.getPath();
//                String className = classPath.substring(root.length()+1,classPath.length()-6).replace('/','.').replace('\\','.');
//                list.add(load.loadClass(className));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else {
//            File[] fs = f.listFiles();
//            if (fs == null) return;
//            for (File file : fs) {
//                loadClassByPath(root,file.getPath(), list, load);
//            }
//        }
//    }
}
