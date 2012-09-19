package net.c0nan.dto;

import java.io.Serializable;

public class PagingDTO implements Serializable,Cloneable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Long from;
	private Long to;
	
	public Long getFrom() {
		if (from == null) from = 0L;
		return from;
	}
	public void setFrom(Long from) {
		this.from = from;
	}
	public Long getTo() {
		if (to == null) to = 0L;
		return to;
	}
	public void setTo(Long to) {
		this.to = to;
	}
}
