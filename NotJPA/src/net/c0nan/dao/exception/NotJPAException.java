package net.c0nan.dao.exception;
/**
 * @author Jannie Pieterse
 * - Base Exception for DAO Framework
 * 
*/
public class NotJPAException extends Exception {

	private static final long serialVersionUID = 1L;

	public NotJPAException(String string) {
		super(string);
	}

}
