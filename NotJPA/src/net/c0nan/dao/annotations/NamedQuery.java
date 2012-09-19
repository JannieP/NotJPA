package net.c0nan.dao.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Jannie Pieterse
 * - useage explanation inside net.c0nan.doc.NotJPADoc
 * - Use this annotation to ad a named query to a DBDTO class
 * - Name - Any name used for calling the named query
 * - Query - Actual SQL query use ":PARAM" without quotes
 * - - e.g: Select * from TESTTABLE where CHARFIELD1 = ':FLD1' and NUMFIELD1 = :NUM1
 * - - Then just use the "FLD1" and "NUM1" in the replacement parameters in you runtime code 
 * 
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface NamedQuery {
	String Name();
	String Query();
}
