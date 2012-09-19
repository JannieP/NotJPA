package net.c0nan.dao.factory;

import java.util.HashMap;
import java.util.Map;

public abstract class DBRegistryFactory {

	static final Map<String, String> map = new HashMap<String, String>();

	static {
//		this.getClassLoader().loadClass("TrueFalseQuestion");
//		this.getClassLoader().loadClass("AnotherTypeOfQuestion");
//		Class.forName("yourpackage.TrueFalseQuestion");
	}

	public static void registerType(String questionName, String ques) {
		map.put(questionName, ques);
	}

	protected Map<String, String> getRegistry() {
		return map;
	}

}
