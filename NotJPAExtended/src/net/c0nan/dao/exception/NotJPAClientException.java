package net.c0nan.dao.exception;

public class NotJPAClientException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Throwable e; 

	public NotJPAClientException(String string) {
		super(string);
	}

	public NotJPAClientException(String string,Throwable e) {
		super(string);
		this.e = e;
	}

	public Throwable getE() {
		return e;
	}
}
