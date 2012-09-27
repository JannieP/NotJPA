package net.c0nan.dao.test.connection;

import java.net.MalformedURLException;

import net.c0nan.dao.connection.manager.ConnectionManager;

public class ExampleConnectionManager extends ConnectionManager {

	public ExampleConnectionManager() throws MalformedURLException {
		super("org.apache.derby.jdbc.EmbeddedDriver","jdbc:derby:myDB;create=false;user=me;password=mine");
		fallCackConnectionManager = new ExampleFallBackConnectionManager();
	}
	
}
