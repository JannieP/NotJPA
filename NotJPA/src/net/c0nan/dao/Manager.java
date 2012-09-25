package net.c0nan.dao;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.c0nan.dao.annotations.Bindable;
import net.c0nan.dao.annotations.NamedQueries;
import net.c0nan.dao.annotations.NamedQuery;
import net.c0nan.dao.connection.manager.ConnectionManager;
import net.c0nan.dao.exception.NotJPAException;
import net.c0nan.dao.exception.NotJPAExceptionIncompleteKey;
import net.c0nan.dao.exception.NotJPAExceptionNoDataChanged;
import net.c0nan.dao.exception.NotJPAExceptionNoDataFound;
import net.c0nan.dao.exception.NotJPAExceptionToManyRows;

/**
 * @author Jannie Pieterse - Does all the DBMS stuff, like executes the SQL
 *         etc... used Binder to build the SQL
 * 
 */

public class Manager<A> {

	static Logger logger = Logger.getLogger(Manager.class.getName());
	private Connection connection = null;
	private PreparedStatement ps = null;
	private ResultSet rs = null;
	private Binder<A> binder = new Binder<A>();
	private Class<A> clazz;
	private Class<ConnectionManager> connectionManagerclazz;
	private ConnectionManager connectionManager;
	private Map<String, Object> generatedKeys = null;
	private boolean generatedKeysMapped = false;
	private int rowsAffected = 0;

	public int getRowsAffected() {
		return rowsAffected;
	}

	public Manager(Class<A> mainclazz) {

		super();
		clazz = mainclazz;
		Bindable mainbindable = clazz.getAnnotation(Bindable.class);
		if (mainbindable == null)
			throw new IllegalArgumentException("Bindable annotation missing on Main class " + mainclazz.getName());

	}

	public boolean runQuery(NamedQuery query) throws NotJPAException {
		return run(query.Query());
	}

	public boolean runQuery(NamedQuery query, String... parameters) throws NotJPAException {
		return run(handleParameters(query.Query(), parameters));
	}

	public boolean runQuery(NamedQuery query, HashMap<String, String> parameters) throws NotJPAException {
		return run(handleParameters(query.Query(), parameters));
	}

	public A find(NamedQuery query, String... parameters) throws NotJPAException {
		return find(handleParameters(query.Query(), parameters));
	}

	public A find(NamedQuery query, HashMap<String, String> parameters) throws NotJPAException {
		return find(handleParameters(query.Query(), parameters));
	}

	public A find(String sql, HashMap<String, String> parameters) throws NotJPAException {
		return find(handleParameters(sql, parameters));
	}

	private A find(String sql) throws NotJPAException {

		if (sql == null)
			return null;

		List<A> results = get(sql);
		if (results.size() > 0)
			return results.get(0);
		return null;

	}

	public List<A> findAll(NamedQuery query, String... parameters) throws NotJPAException {
		return findAll(handleParameters(query.Query(), parameters));
	}

	public List<A> findAll(NamedQuery query, HashMap<String, String> parameters) throws NotJPAException {
		return findAll(handleParameters(query.Query(), parameters));
	}

	public List<A> findAll(String sql, HashMap<String, String> parameters) throws NotJPAException {
		return findAll(handleParameters(sql, parameters));
	}

	public List<A> findAll(String sql) throws NotJPAException {

		if (sql == null)
			return null;
		List<A> results = get(sql);
		return results;

	}

	public A find(A obj) throws NotJPAException {

		int rowcount = 0;
		if (exists(obj, rowcount)) {
			if (rowcount > 1) {
				throw new NotJPAExceptionToManyRows("More than one row returned");
			}

			String sql = binder.bind(Binder.QueryType.SELECT, obj);
			List<A> results = get(sql);
			if (results.size() > 0)
				return results.get(0);
			return null;
		}

		return null;

	}

	public List<A> findAll(A obj) throws NotJPAException {
		if (exists(obj)) {
			String sql = binder.bind(Binder.QueryType.SELECT, obj);
			List<A> results = get(sql);
			return results;
		}
		return new ArrayList<A>();
	}

	public A findByKey(A obj) throws NotJPAException {

		String sql = binder.bindByPK(Binder.QueryType.SELECT, obj);
		List<A> results = get(sql);
		obj = results.get(0);
		return obj;

	}

	public boolean existsByKey(A obj) throws NotJPAException {

		String sql = binder.bindByPK(Binder.QueryType.SELECTCOUNT, obj);
		return exists(sql, new Integer(0).intValue());

	}

	public void add(A obj) throws NotJPAException {
		if (checkKey(obj)) {
			if (existsByKey(obj))
				throw new NotJPAException("Row allready exists for PK");
		}
		String sql = binder.bind(Binder.QueryType.INSERT, obj);
		run(sql);
		populateGeneratedKeys(obj);
	}

	public boolean populateGeneratedKeys(A obj) throws NotJPAException {

		Map<String, Boolean> keys = binder.getKeys();
		if (!keys.isEmpty()) {
			if (generatedKeysMapped) {
				binder.mapFieldValues(obj, getGeneratedKeys());
			}
		}
		return true;
	}

	public boolean checkKey(A obj) throws NotJPAException {
		return binder.CheckKey(obj);
	}

	public void update(A obj) throws NotJPAException {
		try {

			if (!checkKey(obj)) {
				log("Incomplete Key", loglevel.warning);
				throw new NotJPAExceptionIncompleteKey("Incomplete Key");
			}
			A result = getClazz().newInstance();
			binder.extractPK(result, obj);
			String sql = binder.bindByPK(Binder.QueryType.SELECT, result);
			List<A> results = get(sql);
			result = results.get(0);
			List<Field> changedFields = binder.bindChanged(result, obj);
			if (changedFields.size() > 0) {
				sql = binder.bind(Binder.QueryType.UPDATE, result, changedFields);
				run(sql, true);
				if (rowsAffected == 0)
					throw new NotJPAException("Row does NOT exist with supplied PK");
			} else {
				throw new NotJPAExceptionNoDataChanged("No Changes Found");
			}
		} catch (IllegalAccessException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new NotJPAException(e.getLocalizedMessage());
		} catch (InstantiationException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new NotJPAException(e.getLocalizedMessage());
		}
	}

	public void delete(A obj) throws NotJPAException {
		String sql = "";
		sql = binder.bind(Binder.QueryType.DELETE, obj);
		run(sql, true);
		if (rowsAffected == 0)
			throw new NotJPAException("Row does NOT exist with supplied PK");
	}

	public boolean exists(A obj) throws NotJPAException {
		return exists(obj, new Integer(0).intValue());
	}

	public boolean exists(A obj, int rowcount) throws NotJPAException {
		String sql = binder.bind(Binder.QueryType.SELECTCOUNT, obj);
		return exists(sql, rowcount);
	}

	private boolean exists(String sql, int rowcount) throws NotJPAException {
		try {
			rs = getResultSet(sql, false);

			if (rs != null && rs.next()) {
				rowcount = rs.getInt("CNT");
				if (rowcount == 0) {
					return false;
				} else {
					return true;
				}
			} else {
				throw new NotJPAException("Failed to execute Query");
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new NotJPAException(e.getLocalizedMessage());
		} finally {
			breakdown();
		}
	}

	@SuppressWarnings("unchecked")
	private void setup() throws NotJPAException {

		try {
			if (this.connection != null && !this.connection.isClosed())
				return;

			Bindable bindable = getClazz().getAnnotation(Bindable.class);
			
			if (connectionManager == null){
				this.connectionManagerclazz = (Class<ConnectionManager>) bindable.ConnectionManager();
				this.connectionManager = this.connectionManagerclazz.newInstance();
			}
			this.connection = this.connectionManager.getConnection();

			ps = null;
			rs = null;
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new NotJPAException(e.getLocalizedMessage(), e);
		} catch (IllegalAccessException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new NotJPAException(e.getLocalizedMessage(), e);
		} catch (InstantiationException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new NotJPAException(e.getLocalizedMessage(), e);
		}
	}

	private void breakdown() throws NotJPAException {

		try {
			if (this.connection == null)
				return;
			this.connectionManager.closeAll(rs, ps, connection);
		} finally {
			this.connection = null;
			this.rs = null;
			this.ps = null;
		}

	}

	private boolean run(String sql) throws NotJPAException {
		return run(sql, false);
	}

	private boolean run(String sql, boolean update) throws NotJPAException {

		boolean success = false;
		log(sql, loglevel.debug);
		try {
			setup();

			this.generatedKeysMapped = false;
			if (sql.toUpperCase().startsWith("INSERT")) {
				if (binder.getKeys().isEmpty()) {
					ps = connection.prepareStatement(sql);
				} else {
					List<String> autogeneratedKeysList = new ArrayList<String>();

					for (String key : binder.getKeys().keySet()) {
						if (binder.getKeys().get(key)) // Should return a
														// boolean
							autogeneratedKeysList.add(key);
					}
					if (!autogeneratedKeysList.isEmpty()) {
						String[] autogeneratedKeys = autogeneratedKeysList.toArray(new String[autogeneratedKeysList.size()]);
						ps = connection.prepareStatement(sql, autogeneratedKeys);
						this.generatedKeysMapped = true;
					} else {
						ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
					}
				}
			} else {
				ps = connection.prepareStatement(sql);
			}
			if (update) {
				success = ((rowsAffected = ps.executeUpdate()) > 0) ? true : false;
			} else {
				success = ps.execute();
			}
			if (generatedKeysMapped)
				mapGeneratedKeys(ps.getGeneratedKeys());
			return success;

		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			SQLException sqle = e.getNextException();
			while (sqle != null) {
				logger.log(Level.SEVERE, sqle.getLocalizedMessage(), sqle);
				sqle = sqle.getNextException();
			}
			throw new NotJPAException(e.getLocalizedMessage());
		} finally {
			breakdown();
		}

	}

	private void mapGeneratedKeys(ResultSet rsKeys) {
		try {
			Map<String, Object> keys = new HashMap<String, Object>();
			if (rsKeys.next()) {
				ResultSetMetaData rsmd = rsKeys.getMetaData();
				int numColumns = rsmd.getColumnCount();
				for (int i = 1; i < numColumns + 1; i++) {
					keys.put(rsmd.getColumnName(i), rsKeys.getObject(i));
				}
				generatedKeys = keys;
			}
		} catch (SQLException e) {
			this.generatedKeysMapped = false;
		}

	}

	private List<A> get(String sql) throws NotJPAException {

		List<A> results = new ArrayList<A>();
		try {
			rs = getResultSet(sql, false);
			if (rs == null)
				throw new NotJPAExceptionNoDataFound("Failed to return results");

			results = new ArrayList<A>();
			A result;
			result = getClazz().newInstance();
			results = binder.bind(result, rs);
			if (results.isEmpty())
				results.add(result);

		} catch (IllegalAccessException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new NotJPAException(e.getLocalizedMessage());
		} catch (InstantiationException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new NotJPAException(e.getLocalizedMessage());
		} finally {
			breakdown();
		}

		return results;

	}

	public String handleParameters(String string, HashMap<String, String> vals) throws NotJPAException {

		StringBuffer buffer = new StringBuffer(string);

		Iterator<Entry<String, String>> iterator = vals.entrySet().iterator();

		while (iterator.hasNext()) {

			Entry<String, String> me = (Entry<String, String>) iterator.next();
			buffer.replace(buffer.indexOf(":" + me.getKey()), buffer.indexOf(":" + me.getKey()) + (":" + me.getKey()).length(), (me.getValue() == null) ? "" : me.getValue());
		}

		return buffer.toString();

	}

	public String handleParameters(String string, String... vals) throws NotJPAException {

		StringBuffer buffer = new StringBuffer(string);

		for (String val : vals) {
			buffer.replace(buffer.indexOf("?"), buffer.indexOf("?") + "?".length(), val);
		}

		return buffer.toString();

	}

	public ResultSet getResultSet(NamedQuery query, String... parameters) throws NotJPAException {
		return getResultSet(handleParameters(query.Query(), parameters), true);
	}

	public ResultSet getResultSet(NamedQuery query, HashMap<String, String> parameters) throws NotJPAException {
		return getResultSet(handleParameters(query.Query(), parameters), true);
	}

	public ResultSet getResultSet(NamedQuery query, boolean breakdown, HashMap<String, String> parameters) throws NotJPAException {
		return getResultSet(handleParameters(query.Query(), parameters), breakdown);
	}

	private ResultSet getResultSet(String sql, boolean breakdown) throws NotJPAException {
		log(sql, loglevel.debug);
		try {

			setup();
			ps = connection.prepareStatement(sql);
			return (ps.executeQuery());

		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new NotJPAException(e.getLocalizedMessage());
		} finally {
			if (breakdown)
				breakdown();
		}
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	private Class<A> getClazz() {
		return clazz;
	}

	public NamedQuery getNamedQuery(String namedQuery) {

		NamedQueries daoQueries = getClazz().getAnnotation(NamedQueries.class);

		for (NamedQuery daoQuery : daoQueries.Queries()) {
			if (daoQuery.Name().equals(namedQuery))
				return daoQuery;
		}
		log("getNamedQuery: " + namedQuery + " not found in " + getClass().getName(), loglevel.warning);
		return null;
	}

	protected void finalise() {
		try {
			breakdown();
		} catch (NotJPAException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	private void log(String msg, loglevel level) {
		msg = "[NotJPA]:" + msg;
		if (level == loglevel.warning)
			logger.log(Level.WARNING, msg);
		if (level == loglevel.info)
			logger.log(Level.INFO, msg);
		if (level == loglevel.debug)
			logger.log(Level.FINE, msg);
	}

	private enum loglevel {
		info, debug, warning;
	}

	public Map<String, Object> getGeneratedKeys() {
		if (this.generatedKeys == null)
			this.generatedKeys = new HashMap<String, Object>();
		return generatedKeys;
	}

	public boolean isGeneratedKeysMapped() {
		return generatedKeysMapped;
	}
}
