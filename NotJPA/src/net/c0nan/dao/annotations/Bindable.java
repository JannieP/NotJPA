package net.c0nan.dao.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Jannie Pieterse
 * - useage explanation inside net.c0nan.doc.NotJPADoc
 * - Use this annotation to tag a class as an DBDTO bindable class, thus it will be accepted in Binder
 * - DBMS - this is to identify which DBMS connection to use when executing the SQL
 * - Schema - Needed mostly for where the schema is not set up on the server  
 * - Table - The ACTUAL table name that this DBDTO class represents, the SQL will be based on this
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Bindable {
	
	 public String Schema() default "";
	 public String Table();
	 public Class<?> ConnectionManager();

}
