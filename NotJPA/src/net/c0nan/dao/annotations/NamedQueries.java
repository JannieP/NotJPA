package net.c0nan.dao.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Jannie Pieterse
 * - useage explanation inside net.c0nan.doc.NotJPADoc
 * - Use this annotation to tag a DBDTO class as containing named queries
 * - Queries - Array of @NamedQuery
 * 
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface NamedQueries {
   NamedQuery[] Queries();  
}
