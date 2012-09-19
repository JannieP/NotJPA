package net.c0nan.dao.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Jannie Pieterse
 * - useage explanation inside net.c0nan.doc.NotJPADoc
 * - Use this annotation to tag a field as an audit field, thus it will be auto populated 
 * - AuditFieldType:
 * - - String - currently does nothing
 * - - DATE,TIME,TIMESTAMP - will insert the respected values into the tagged fields every time the record is changed
 * - Format - Relates to any date/time format... e.g: yyyy/mm/dd
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface AuditField {
	public enum AuditFieldType{STRING,DATE,TIME,TIMESTAMP;}
	public AuditFieldType Type();
	public String Format() default "";
}
