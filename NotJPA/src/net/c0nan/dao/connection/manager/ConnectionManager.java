package net.c0nan.dao.connection.manager;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import net.c0nan.dao.exception.NotJPAException;

public abstract class ConnectionManager {

	private static Logger logger = Logger.getLogger(ConnectionManager.class.getName());
	protected Connection connection = null;
	protected URL[] driverFilePaths = null;
	protected String className = null;
	protected String connnectionURL = null;
	protected String userName = null;
	protected String passWord = null;
	protected String jndi = null;
	protected DataSource dataSource = null;
	protected ConnectionManager fallCackConnectionManager = null;

	public ConnectionManager(String jndi) {
		this.jndi = jndi;
	}

	public ConnectionManager(String className,String connnectionURL) {
		this(null,className,connnectionURL);
	}

	public ConnectionManager(URL[] driverFilePaths, String className,String connnectionURL) {
		this(driverFilePaths,className,connnectionURL,null,null);
	}

	public ConnectionManager(String className, String connnectionURL, String uswerName, String password) {
		this(null,className,connnectionURL,uswerName,password);
	}

	public ConnectionManager(URL[] driverFilePaths, String className, String connnectionURL, String uswerName, String password) {
		this.driverFilePaths = driverFilePaths;
		this.className = className;
		this.connnectionURL = connnectionURL;
		this.userName = uswerName;
		this.passWord = password;
	}


	public Connection getConnection() throws NotJPAException {
		try {
			if (connection == null || connection.isClosed()) {
				setupConnection();
			}
			if (connection == null) {
				return null;
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return this.connection;
	}
	
	public void closeAll(ResultSet resultSet,PreparedStatement preparedStatement, Connection connection){
		try {
			if (resultSet != null) resultSet.close();
			if (preparedStatement != null) preparedStatement.close();
			if (connection != null) connection.close();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	private void setupConnection() throws NotJPAException {
		if (this.jndi != null) {
			setConnectionFromJndi();
		} else if (this.className != null) {
			setConnectionFromDriver();
		}else {
			throw new NotJPAException("ConnectionManager not set up correctly!");
		}
		
	}

	protected void setConnectionFromDriver() {
		URLClassLoader classLoader = null;
		if (this.driverFilePaths != null){
			classLoader = new URLClassLoader(this.driverFilePaths);
		}
		Driver driver = null;
		try {
			
			if (classLoader != null){
				driver = (Driver) Class.forName(this.className, true, classLoader).newInstance();
				DriverManager.registerDriver(driver);
				classLoader.loadClass(this.className);
			}
			Class.forName(this.className);
			Properties properties = new Properties();

			if (this.userName != null || this.passWord != null) {
				properties.setProperty("user", this.userName);
				properties.setProperty("password", this.passWord);
			}
			connection = DriverManager.getConnection(this.connnectionURL, properties);

		} catch (IllegalAccessException e) {
			fallback(e);
		} catch (InstantiationException e) {
			fallback(e);
		} catch (ClassNotFoundException e) {
			fallback(e);
		} catch (SQLException e) {
			fallback(e);
		}
	}

	protected void setConnectionFromJndi() {
		InitialContext context = null;
		try {
			context = new InitialContext();
			dataSource = (DataSource) context.lookup("java:comp/env/" + this.jndi);
		} catch (NamingException e) {
			try {
				dataSource = (DataSource) context.lookup(this.jndi);
			} catch (NamingException e1) {
				logger.log(Level.SEVERE, e1.getLocalizedMessage(), e1);
			}
			if (dataSource == null) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		} finally {
			try {
				if (context != null) {
					context.close();
					context = null;
				}
			} catch (NamingException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}

	}
	
	private void fallback(Throwable e){
		if (fallCackConnectionManager != null){
			logger.log(Level.FINE, "Connection Failed - Attempting Fallback");
			try {
				connection = fallCackConnectionManager.getConnection();
			} catch (NotJPAException e1) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e1);
			}
		}else{
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	};
}
