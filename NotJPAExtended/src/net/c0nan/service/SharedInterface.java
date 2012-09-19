package net.c0nan.service;

import java.util.List;

import net.c0nan.dao.exception.NotJPAClientException;
import net.c0nan.dto.BaseDTO;

/**
 * @author Jannie Pieterse
 * 
 */
public interface SharedInterface<A extends BaseDTO> {
	public List<A> find(A dto);
	public A add(A dto);
	public A update(A dto);
	public A delete(A dto);

	public List<A> findE(A dto) throws NotJPAClientException;
	public A getE(A dto) throws NotJPAClientException;
	public A addE(A dto) throws NotJPAClientException;
	public A updateE(A dto) throws NotJPAClientException;
	public A deleteE(A dto) throws NotJPAClientException;
}
