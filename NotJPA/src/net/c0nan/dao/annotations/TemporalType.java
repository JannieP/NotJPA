package net.c0nan.dao.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Jannie Pieterse
 * - useage explanation inside net.c0nan.doc.NotJPADoc
 * - Use this annotation to tag a field as a TemporalType
 * - Type - DATE, TIME, TIMESTAMP - Ensures correct data type
 * - Format - Ensures correct format e.g: yyyy/MM/dd
 * - HandleAsString - will enclose the value in single quotes 
 * 
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface TemporalType {
	
	public enum TemporalTypes{
		DATE,TIME,TIMESTAMP
	}
	public TemporalTypes Type() default TemporalTypes.DATE;
	public String Format() default "";
	public boolean HandleAsString() default false;
}
