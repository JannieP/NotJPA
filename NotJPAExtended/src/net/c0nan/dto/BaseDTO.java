package net.c0nan.dto;

import java.io.Serializable;

public class BaseDTO extends PagingDTO implements Serializable,Cloneable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1L;

	private Exception e;

	private String message = "";

	private Boolean success = false;
	
	private Boolean updatesuccess = false;
	
	private UserDetailsDTO userdetail;

	public Exception getE() {
		return e;
	}

	public void setE(Exception e) {
		this.e = e;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Boolean isSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public UserDetailsDTO getUserdetail() {
		return userdetail;
	}

	public void setUserdetail(UserDetailsDTO userdetail) {
		this.userdetail = userdetail;
	}

	public Boolean isUpdatesuccess() {
		return updatesuccess;
	}

	public void setUpdatesuccess(Boolean updatesuccess) {
		this.updatesuccess = updatesuccess;
	}

}
