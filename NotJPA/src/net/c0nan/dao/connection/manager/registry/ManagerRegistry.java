package net.c0nan.dao.connection.manager.registry;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public abstract class ManagerRegistry {

	static final Map<String, Connection> map = new HashMap<String, Connection>();

	static {
//		this.getClassLoader().loadClass("TrueFalseQuestion");
//		this.getClassLoader().loadClass("AnotherTypeOfQuestion");
//		Class.forName("yourpackage.TrueFalseQuestion");
	}

	public static void registerType(String connectionName, Connection connection) {
		map.put(connectionName, connection);
	}

	protected Map<String, Connection> getRegistry() {
		return map;
	}

}
