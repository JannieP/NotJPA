package net.c0nan.dao.test.connection;

import java.net.MalformedURLException;

import net.c0nan.dao.connection.manager.ConnectionManager;

public class ExampleFallBackConnectionManager extends ConnectionManager {

	public ExampleFallBackConnectionManager() throws MalformedURLException {
		super("org.apache.derby.jdbc.EmbeddedDriver","jdbc:derby:myDB2;create=true;user=me;password=mine");
	}
	
}
