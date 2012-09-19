package net.c0nan.dao;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.c0nan.dao.annotations.AuditField;
import net.c0nan.dao.annotations.Bindable;
import net.c0nan.dao.annotations.DBField;
import net.c0nan.dao.annotations.DBKey;
import net.c0nan.dao.annotations.TemporalType;
import net.c0nan.dao.annotations.DBField.CaseType;
import net.c0nan.dao.annotations.TemporalType.TemporalTypes;
import net.c0nan.dao.exception.NotJPAException;



/**
 * @author Jannie Pieterse
 * - Builds the SQL statement from the DBDTO class, and complementary other functions 
 * 
 */

public class Binder<A> {
	
	static Logger logger = Logger.getLogger(Binder.class.getName());
	private Map<String,Boolean> keys;

	public enum QueryType {
		/*
		 * 2012/07/19 - JHP - Isolate place holders for more flexibility
		 * {1} - Table Name(s)
		 * {2} - where clause
		 * {3} - insert values
		 * {4} - insert fields
		 * {5} - update set list
		 */
		
		SELECTCOUNT("Select count(*) as CNT from {1}{2}"),
		SELECT("Select * from {1}{2}"), 
		INSERT("Insert into {1} ({4}) values({3})"), 
		UPDATE("Update {1} set {5}{2}"), 
		DELETE("Delete from {1}{2}");

		public String query;

		QueryType(String query) {
			this.query = query;
		}
	}

	public String bind(QueryType querytype, A source) throws NotJPAException  {
		return bind(querytype,source,false,false,null);
	}
	public String bindByPK(QueryType querytype, A source) throws NotJPAException{
		return bind(querytype,source,true,false,null);
	}
	public String bind(QueryType querytype, A source,List<Field> fields) throws NotJPAException{
		return bind(querytype,source,false,false,fields);
	}
	public String bind(QueryType querytype, A source,boolean keyOnly) throws NotJPAException{
		return bind(querytype,source,keyOnly,false,false,true,null);
	}
	public String bind(QueryType querytype, A source,boolean keyOnly,List<Field> fields) throws NotJPAException{
		return bind(querytype,source,keyOnly,false,false,true,fields);
	}
	public String bind(QueryType querytype, A source,boolean keyOnly,boolean includeAuditFields) throws NotJPAException{
		return bind(querytype,source,keyOnly,includeAuditFields,false,true,null);
	}
	public String bind(QueryType querytype, A source,boolean keyOnly,boolean includeAuditFields,List<Field> fields) throws NotJPAException  {
		return bind(querytype,source,keyOnly,includeAuditFields,false,true,fields);
	}
	public String bind(QueryType querytype, A source,boolean keyOnly,boolean includeAuditFields,boolean includeBlankFields) throws NotJPAException{
		return bind(querytype,source,keyOnly,includeAuditFields,includeBlankFields,true,null);
	}
	public String bind(QueryType querytype, A source,boolean keyOnly,boolean includeAuditFields,boolean includeBlankFields,boolean exactMatch) throws NotJPAException{
		return bind(querytype,source,keyOnly,includeAuditFields,includeBlankFields,exactMatch,null);
	}

	public String bind(QueryType querytype, A source,boolean keyOnly,boolean includeAuditFields,boolean includeBlankFields,boolean exactMatch,List<Field> fields) throws NotJPAException  {
		StringBuilder result;
		result = new StringBuilder(querytype.query);

		String type = result.substring(0, result.indexOf(" ")).toUpperCase();

		Class<?> mainclazz = source.getClass();
		Field[] mainfields = getFields(mainclazz,null);

		Bindable mainbindable = mainclazz.getAnnotation(Bindable.class);
		if (mainbindable == null)
			throw new NotJPAException("Bindable annotation missing on Main class " + mainclazz.getName());

		String schema = mainbindable.Schema();
		String table = mainbindable.Table();

		StringBuilder newValue2 = new StringBuilder("");
		StringBuilder newValue3 = new StringBuilder("");

		boolean isFirst = true;
		boolean isFirstKey = true;

		for (Field field : mainfields) {
			DBField daofield = field.getAnnotation(DBField.class);
			if (daofield == null)continue;

			if (isAllowedTypeList(field.getType())) {

				String mapto = daofield.MapTo();
				if (mapto.isEmpty()) {
					mapto = field.getName();
				}

				DBKey daokey = field.getAnnotation(DBKey.class);
				if (daokey != null){
					if (!getKeys().containsKey(mapto)){
						getKeys().put(mapto, daokey.AutoGenerated());
					}
				}
				
				AuditField auditfield = field.getAnnotation(AuditField.class);
				
				String val = getFieldValue(mainclazz, field, source);
				
				if (val != null)val = val.replaceAll("'", "''");
				
				TemporalType tp = field.getAnnotation(TemporalType.class);
				if (tp != null){
					if (tp.HandleAsString()){
						val = "'" + val + "'";
					}
				}

				if (QueryType.SELECT.toString().equals(type) || QueryType.SELECTCOUNT.toString().equals(type)) {
					if (keyOnly && daokey == null) continue;
					if (!includeAuditFields && auditfield != null) continue;
					if (val == null || val.isEmpty()){
						if (includeBlankFields){
							if (!isFirst)
								newValue2.append(" AND ");
							isFirst = false;
							if (val == null){
								newValue2.append(mapto).append(" is null");
							}else{
								if (String.class.isAssignableFrom(field.getType())) {
									newValue2.append(mapto).append("='").append(val).append("'");
								}else{
									continue;
								}
							}
						}else{
							continue;
						}
					}else{
						if (!isFirst)
							newValue2.append(" AND ");
						isFirst = false;
						if(Number.class.isAssignableFrom(field.getType())){
							newValue2.append(mapto).append("=").append(val);
						}else{
							if (CaseType.UPPERCASE == daofield.Case()) val = val.toUpperCase();
							if (CaseType.lowercase == daofield.Case()) val = val.toLowerCase();
							newValue2.append(mapto).append("='").append(val).append("'");
						}
					}
				} else if (QueryType.INSERT.toString().equals(type)) {
					if (auditfield != null){
						String auditVal = getAuditFieldValue(auditfield.Type(),auditfield.Format());
						if (auditVal != null){
							val = auditVal;
						}
					}
					if (val == null || val.isEmpty()) continue;

					if (!isFirst) {
						newValue2.append(",");
						newValue3.append(",");
					}
					isFirst = false;
					newValue2.append(mapto);
					if(Number.class.isAssignableFrom(field.getType())){
						newValue3.append(val);
					}else{
						if (CaseType.UPPERCASE == daofield.Case()) val = val.toUpperCase();
						if (CaseType.lowercase == daofield.Case()) val = val.toLowerCase();
						newValue3.append("'").append(val).append("'");
					}
					
				} else if (QueryType.UPDATE.toString().equals(type)) {
					if (auditfield != null){
						String auditVal = getAuditFieldValue(auditfield.Type(),auditfield.Format());
						if (auditVal != null){
							val = auditVal;
						}
					}
					if (daokey == null) {
						
						if (fields != null){
							if (!fields.contains(field) && (auditfield == null)){
								continue;
							}
						}else if((val == null || val.isEmpty())){
							continue;
						}
						if (!isFirst) {
							newValue2.append(",");
						}
						isFirst = false;
						if (val == null){
							if (daofield.UseNullStringForNull()){
								newValue2.append(mapto).append("=''");
							}else{
								if (!("".equals(daofield.replaceNull()))){
								   newValue2.append(mapto).append("=").append(daofield.replaceNull());
								}else{
								   newValue2.append(mapto).append("=").append(val);
								}
							}
						}else{
							if(Number.class.isAssignableFrom(field.getType())){
								newValue2.append(mapto).append("=").append(val);
							}else{
								if (CaseType.UPPERCASE == daofield.Case()) val = val.toUpperCase();
								if (CaseType.lowercase == daofield.Case()) val = val.toLowerCase();
								newValue2.append(mapto).append("='").append(val).append("'");
							}

						}
					} else {
						if (val == null || val.isEmpty())continue;
						if (!isFirstKey) {
							newValue3.append(" AND ");
						}
						isFirstKey = false;
						if(Number.class.isAssignableFrom(field.getType())){
							newValue3.append(mapto).append("=").append(val);
						}else{
							if (CaseType.UPPERCASE == daofield.Case()) val = val.toUpperCase();
							if (CaseType.lowercase == daofield.Case()) val = val.toLowerCase();
							newValue3.append(mapto).append("='").append(val).append("'");
						}

					}
				} else if (QueryType.DELETE.toString().equals(type)) {
					if (!includeAuditFields && auditfield != null) continue;
					if (val == null || val.isEmpty())
						continue;
					if (daokey != null) {
						if (!isFirstKey) {
							newValue2.append(" AND ");
						}
						isFirstKey = false;
						if(Number.class.isAssignableFrom(field.getType())){
							newValue2.append(mapto).append("=").append(val);
						}else{
							if (CaseType.UPPERCASE == daofield.Case()) val = val.toUpperCase();
							if (CaseType.lowercase == daofield.Case()) val = val.toLowerCase();
							newValue2.append(mapto).append("='").append(val).append("'");
						}

					}
				}
			}else{
				logger.log(Level.WARNING,"Invalid Data Type:" + field.getName() + "[" + field.getType().getName() + "]");
			}
		}

		/*
		 * 2012/07/19 - JHP - Isolate place holders for more flexibility
		 * {1} - Table Name(s)
		 * {2} - where clause
		 * {3} - insert values
		 * {4} - insert fields
		 * {5} - update set list
		 */

		result = new StringBuilder(result.toString().replaceAll("\\{1\\}",(schema.isEmpty()) ? table : schema + "." + table));

		if (QueryType.SELECT.toString().equals(type)) {
			result = new StringBuilder(result.toString().replaceAll("\\{2\\}",(newValue2.toString().isEmpty())? "" : " where " + newValue2.toString()));
		} else if (QueryType.INSERT.toString().equals(type)) {
			result = new StringBuilder(result.toString().replaceAll("\\{4\\}",newValue2.toString()));
			result = new StringBuilder(result.toString().replaceAll("\\{3\\}",newValue3.toString()));
		} else if (QueryType.UPDATE.toString().equals(type)) {
			result = new StringBuilder(result.toString().replaceAll("\\{5\\}",newValue2.toString()));
			result = new StringBuilder(result.toString().replaceAll("\\{2\\}",(newValue3.toString().isEmpty())? "" : " where " + newValue3.toString()));
		} else if (QueryType.DELETE.toString().equals(type)) {
			result = new StringBuilder(result.toString().replaceAll("\\{2\\}",(newValue2.toString().isEmpty())? "" : " where " + newValue2.toString()));
		}

		return result.toString();
	}
	
	private String getAuditFieldValue(AuditField.AuditFieldType type,String format){
		
		if (format == null) format = "";
		
		if (type == AuditField.AuditFieldType.STRING){
			return null; // currently default ignores this 
		}
		if (type == AuditField.AuditFieldType.DATE){
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
			if (!format.isEmpty())
				df = new SimpleDateFormat(format);
			return df.format(new java.util.Date());
		}
		if (type == AuditField.AuditFieldType.TIME){
			SimpleDateFormat tf = new SimpleDateFormat("HHmmss");
			if (!format.isEmpty())
				tf = new SimpleDateFormat(format);
			return tf.format(new java.util.Date());
		}
		if (type == AuditField.AuditFieldType.TIMESTAMP){
			SimpleDateFormat ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
			if (!format.isEmpty())
				ts = new SimpleDateFormat(format);
			return ts.format(new java.util.Date());
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public List<A> bind(A destination, ResultSet rs) throws NotJPAException {
		List<A> v = new ArrayList<A>();
		Class<?> mainclazz = destination.getClass();
		Field[] mainfields = getFields(mainclazz,null);
		String mapto ="";

		try {
			if (rs != null && rs.next()) {
				for (Field field : mainfields) {
					DBField daofield = field.getAnnotation(DBField.class);
					if (daofield == null)
						continue;
					mapto = daofield.MapTo();
					if (mapto.isEmpty()) {
						mapto = field.getName();
					}

					setFieldValue(mainclazz, field,destination ,rs.getString(mapto));
				}
				v.add(destination);
			
				while (rs.next()) {
					A dest;
					try {
						dest = (A)mainclazz.newInstance();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						throw new NotJPAException(e.getLocalizedMessage());
					} catch (InstantiationException e) {
						e.printStackTrace();
						throw new NotJPAException(e.getLocalizedMessage());
					}
					for (Field field : mainfields) {
						DBField daofield = field.getAnnotation(DBField.class);
						if (daofield == null)
							continue;
						mapto = daofield.MapTo();
						if (mapto.isEmpty()) {
							mapto = field.getName();
						}
	
						setFieldValue(mainclazz, field,dest ,rs.getString(mapto));
					}
					v.add(dest);
				}
			}else{
				
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new NotJPAException(e.getLocalizedMessage());
		} catch (NotJPAException e) {
			e.printStackTrace();
			throw e;
		} catch (Exception e){
			e.printStackTrace();
			throw new NotJPAException(e.getLocalizedMessage() + ":" + mainclazz.getSimpleName() + ":" + mapto);
		}

		return v;
	}
	
	public void mapFieldValues(A destination, Map<String,Object> values) throws NotJPAException{
		Class<?> mainclazz = destination.getClass();
		Field[] mainfields = getFields(mainclazz,null);
		String mapto ="";

		for (Field field : mainfields) {
			DBField daofield = field.getAnnotation(DBField.class);
			if (daofield == null)
				continue;
			mapto = daofield.MapTo();
			if (mapto.isEmpty()) {
				mapto = field.getName();
			}
			if (values.containsKey(mapto)){
				setFieldValue(mainclazz, field,destination ,values.get(mapto));
			}
		}
	}
	
	public void extractPK(A destination, A source) throws NotJPAException  {

		Class<?> sourceclazz = source.getClass();
		Field[] mainfields = getFields(sourceclazz,null);
		
		Class<?> destinationclazz = source.getClass();

		Bindable mainbindable = sourceclazz.getAnnotation(Bindable.class);
		if (mainbindable == null)
			throw new NotJPAException("DAOBindable annotation missing on Source class " + sourceclazz.getName());

		for (Field field : mainfields) {
			if (isAllowedTypeList(field.getType())) {
				DBField daofield = field.getAnnotation(DBField.class);
				if (daofield == null)
					continue;
				DBKey daokey = field.getAnnotation(DBKey.class);
				if (daokey == null) 
					continue;
				
				String val = getFieldValue(sourceclazz, field, source);
				setFieldValue(destinationclazz, field,destination ,val);
			}
		}
	}
	
	public Map<String,String> extractMappedPK(A source) throws NotJPAException  {
		
		Map<String,String> pks = new HashMap<String, String>();

		Class<?> sourceclazz = source.getClass();
		Field[] mainfields = getFields(sourceclazz,null);
		
		Bindable mainbindable = sourceclazz.getAnnotation(Bindable.class);
		if (mainbindable == null)
			throw new NotJPAException("DAOBindable annotation missing on Source class " + sourceclazz.getName());

		for (Field field : mainfields) {
			if (isAllowedTypeList(field.getType())) {
				DBField daofield = field.getAnnotation(DBField.class);
				if (daofield == null)
					continue;
				DBKey daokey = field.getAnnotation(DBKey.class);
				if (daokey == null) 
					continue;
				
				String val = getFieldValue(sourceclazz, field, source);
				pks.put(field.getName(),val);
			}
		}
		return pks;
	}

	private boolean isAllowedTypeList(Class<?> type){

		if (type == String.class | 
			type == BigDecimal.class | 
			type == Date.class | 
			type == Time.class |
			type == Short.class | 
			type == Long.class | 
			type == Integer.class){
			return true;
		}
		return false;
	}

	public List<Field> bindChanged(A destination, A source) throws NotJPAException  {

		Class<?> sourceclazz = source.getClass();
		Field[] mainfields = getFields(sourceclazz,null);
		Class<?> destinationclazz = source.getClass();
		List<Field> fields = new ArrayList<Field>();
		
		Bindable mainbindable = sourceclazz.getAnnotation(Bindable.class);
		if (mainbindable == null)
			throw new NotJPAException("DAOBindable annotation missing on Source class " + sourceclazz.getName());

		for (Field field : mainfields) {
			if (isAllowedTypeList(field.getType())) {
				DBField daofield = field.getAnnotation(DBField.class);
				if (daofield == null)
					continue;
				
				DBKey daokey = field.getAnnotation(DBKey.class);
				
				AuditField auditfield = field.getAnnotation(AuditField.class);
				
				String val1 = getFieldValue(sourceclazz, field, source);
				String val2 = getFieldValue(destinationclazz, field, destination);
				
				if ((!NVL(val1,"").equals(NVL(val2,""))) && 
					daokey == null && auditfield == null){
					logger.log(Level.FINE,"[NotJPA] Field Changed [" + mainbindable.Table() + "].[" + daofield.MapTo() + "] from " + val2 + " to " + val1);
					fields.add(field);
					setFieldValue(sourceclazz, field,destination ,val1);
				}
			}
		}

		return fields;

	}
	
	private Object NVL(Object in,Object out){
		if (in == null)return out;
		
		if (in instanceof String){
			if (((String) in).trim().equals("")) return out;
		}
		
		if (in instanceof Number){
			return in;
		}
		
		Class<?> clazz = in.getClass();
		try{
			Method m = clazz.getMethod("isEmpty");
			if ((Boolean)m.invoke(in)) return out;
		}catch(Exception e){
			return out;
		}

		if (in.toString().trim() == "") return out;

		return in;
	}
	
	private String getFieldValue(Class<?> clazz, Field field, Object source) throws NotJPAException {
		String methodname = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
		Object val = "";
		String result = "";
		Method method;
		try {
			method = clazz.getMethod(methodname);
			try {
				val = method.invoke(source);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				throw new NotJPAException(e.getLocalizedMessage());
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new NotJPAException(e.getLocalizedMessage());
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				throw new NotJPAException(e.getLocalizedMessage());
			}
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new NotJPAException(e.getLocalizedMessage());
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			throw new NotJPAException(e.getLocalizedMessage());
		}
		if (val != null && field.getType() == String.class){
			result = (String)val;
		}else if(val != null && field.getType() == BigDecimal.class){
			result = ((BigDecimal)val).toString();
		}else if(val != null && (field.getType() == Short.class |field.getType() == Long.class |field.getType() == Integer.class )){
			result = val.toString();
		}else if (val != null && (field.getType() == Date.class || field.getType() == Time.class)){
			TemporalType tp = field.getAnnotation(TemporalType.class);
			if (tp == null){
				if((field.getType() == Date.class)){
					result = new SimpleDateFormat("yyyyMMdd").format(((Date)val).getTime());
				}else if ((field.getType() == Time.class)){
					result = new SimpleDateFormat("HHmmss").format(((Time)val).getTime());
				}
			}else{
				String format = tp.Format();
				if (!"".equals(format)){
					result = new SimpleDateFormat(tp.Format()).format(((Date)val).getTime());
				}else if (tp.Type() == TemporalTypes.DATE){
					result = new SimpleDateFormat("yyyy-MM-dd").format(((Date)val).getTime());
				}else if(tp.Type() == TemporalTypes.TIME){
					result = new SimpleDateFormat("HH:mm:ss.SSSSSS").format(((Date)val).getTime());							
				}else if(tp.Type() == TemporalTypes.TIMESTAMP){
					result = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS").format(((Date)val).getTime());
				}
				
			}
			

		}else if(val != null){
			result = val.toString();
		}else if(val == null){
			return null;
		}
		return result.trim();
	}

	private void setFieldValue(Class<?> clazz, Field field,Object source, Object value) throws NotJPAException {
		String methodname = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
		Method method;
		try {
			method = clazz.getMethod(methodname, new Class[] { field.getType() });
			try {
				
				if (field.getType() == String.class){
					method.invoke(source, value == null ? null : ((String)value).trim());
				}else if (field.getType() == BigDecimal.class){
					method.invoke(source, NVL(value,"").equals("") ? null : new BigDecimal((String)value));
				}else if ((field.getType() == Short.class)){
					method.invoke(source, NVL(value,"").equals("") ? null : Short.valueOf(value.toString()).shortValue());
				}else if ((field.getType() == Long.class)){
					method.invoke(source, NVL(value,"").equals("") ? null : Long.valueOf(value.toString()).longValue());
				}else if ((field.getType() == Integer.class)){
					method.invoke(source, NVL(value,"").equals("") ? null : Integer.valueOf(value.toString()).intValue());
				}else if ((field.getType() == Date.class) || (field.getType() == Time.class)){
					TemporalType tp = field.getAnnotation(TemporalType.class);
					DateFormat df = new SimpleDateFormat();
					if (tp == null){
						if((field.getType() == Date.class)){
							df = new SimpleDateFormat("yyyyMMdd");
						}else if ((field.getType() == Time.class)){
							df = new SimpleDateFormat("HHmmss");
						}
					}else{
						String format = tp.Format();
						if (!"".equals(format)){
							df = new SimpleDateFormat(tp.Format());
						}else if (tp.Type() == TemporalTypes.DATE){
							df = new SimpleDateFormat("yyyy-MM-dd");
						}else if(tp.Type() == TemporalTypes.TIME){
							df = new SimpleDateFormat("HH:mm:ss.SSSSSS");							
						}else if(tp.Type() == TemporalTypes.TIMESTAMP){
							df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
						}
					}
					try{
						if( (field.getType() == Date.class)){
							method.invoke(source, NVL(value,"").equals("") ? null : new Date(df.parse((String)value).getTime()));
						}else if((field.getType() == Time.class)){
							method.invoke(source, NVL(value,"").equals("") ? null : new Time(df.parse((String)value).getTime()));
						}
					}catch (ParseException e){
						//Skip i f invalid date | time
					}
				}
			}catch(Exception e){
				e.printStackTrace();
				throw new NotJPAException(e.getLocalizedMessage());
			}
				
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new NotJPAException(e.getLocalizedMessage());
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			throw new NotJPAException(e.getLocalizedMessage());
		}
	}
	
	public boolean CheckKey(A source) throws NotJPAException{

		boolean result = false;

		Class<?> sourceclazz = source.getClass();
		Field[] mainfields = getFields(sourceclazz,null);
		
		Bindable mainbindable = sourceclazz.getAnnotation(Bindable.class);
		if (mainbindable == null)
			throw new NotJPAException("Bindable annotation missing on Source class " + sourceclazz.getName());

		for (Field field : mainfields) {
			if (isAllowedTypeList(field.getType())) {
				
				DBKey daokey = field.getAnnotation(DBKey.class);
				if (daokey == null) 
					continue;
				
				String val1 = getFieldValue(sourceclazz, field, source);
				if (!NVL(val1,"").equals("")){ 
					result = true;
				}else{
					result = false;
				}
				
			}
		}
		
		return result;
	}
	public Map<String, Boolean> getKeys() {
		if (this.keys == null) this.keys = new HashMap<String,Boolean>();
		return this.keys;
	}
	
	private Field[] getFields(Class<?> clazz, Field[] fields){
		Field[] result = clazz.getDeclaredFields();
		if (fields != null){
			result = concat(fields,result);
		}
		if (clazz.getSuperclass() != null) result = getFields(clazz.getSuperclass(), result);
		for (Field field : result){
			field.setAccessible(true);
		}
		return result;
	}
	
	Field[] concat(Field[] A, Field[] B) {
		   Field[] C= new Field[A.length+B.length];
		   System.arraycopy(A, 0, C, 0, A.length);
		   System.arraycopy(B, 0, C, A.length, B.length);
		   return C;
	}

	
}