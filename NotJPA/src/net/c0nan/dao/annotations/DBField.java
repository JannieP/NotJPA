package net.c0nan.dao.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Jannie Pieterse
 * - useage explanation inside net.c0nan.doc.NotJPADoc
 * - Use this annotation to tag a field as an DBDTO bindable field, thus it will be included by Binder
 * - Mapto - The actual field name on the Database
 * - Changed - Deprecated
 * - UseNullStringForNull - this is for where the DBMS does not accept NULL, but will accept ''
 * - ReplaceNull - Overrides UseNullString with any value ALPHANUMERIC
 * - Case - Forces the selected case onto the field before generating SQL
 * 
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface DBField {
	public enum CaseType{
		UPPERCASE,lowercase,None
	}
    public String MapTo() default "";
    public boolean Changed() default false;
    public boolean UseNullStringForNull() default true;
    public String replaceNull() default ""; 
    public CaseType Case() default CaseType.None;
}
