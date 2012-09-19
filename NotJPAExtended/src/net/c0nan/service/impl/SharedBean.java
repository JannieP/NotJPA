package net.c0nan.service.impl;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.c0nan.beanutils.BeanUtils;
import net.c0nan.dao.Manager;
import net.c0nan.dao.dto.SharedDBDTO;
import net.c0nan.dao.exception.NotJPAClientException;
import net.c0nan.dao.exception.NotJPAExceptionNoDataChanged;
import net.c0nan.dao.exception.NotJPAExceptionNoDataFound;
import net.c0nan.dto.BaseDTO;
import net.c0nan.service.SharedInterface;



public abstract class SharedBean<A extends BaseDTO,B extends SharedDBDTO> implements SharedInterface<A>{

	Logger logger = Logger.getLogger(SharedBean.class.getName());
	
	@SuppressWarnings("unchecked")
	public Class<B> createBLocal(){
		Class<B> clazz = ((Class<B>)((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[1]);  
		return clazz;
	}
	
	public Manager<B> daoManager = new Manager<B>(createBLocal());
	

	@Deprecated
	@Override
	public List<A> find(A dto){
		List<A> DTOs = new ArrayList<A>();
		try {
			return findE(dto);
		} catch (NotJPAClientException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(),e);
			dto.setE(e);
			dto.setMessage(e.getLocalizedMessage());
			dto.setSuccess(false);
			DTOs.clear();
			DTOs.add(dto);
			return DTOs;
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<A> findE(A dto) throws NotJPAClientException {;

		List<A> DTOs = new ArrayList<A>();
		List<B> DBDTOs = new ArrayList<B>();

		try {
			B DBDTO = createBLocal().newInstance();

			BeanUtils.getInstance().copyProperties(DBDTO, dto);

			DBDTOs = daoManager.findAll(DBDTO);

			for (B DBDTo : DBDTOs) {
				A DTO = (A)dto.getClass().newInstance();
				BeanUtils.getInstance().copyProperties(DTO, DBDTo);
				DTO.setSuccess(true);
				DTOs.add(DTO);
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE,e.getLocalizedMessage(),e);
			throw new NotJPAClientException(e.getLocalizedMessage(),e);
		}
		return DTOs;
	}
	
	@Override
	public A getE(A dto) throws NotJPAClientException {
		try{
			List<A> dtos = findE(dto);
			if (dtos.size() > 1){
				throw new NotJPAClientException("Too Many Rows Returned");
			}
			if(dtos.size() == 0){
				throw new NotJPAExceptionNoDataFound("Now Rows Returned");
			}
			A Dto = dtos.get(0);
			Dto.setSuccess(true);
			return Dto;
		} catch (NotJPAClientException e) {
			logger.log(Level.WARNING,e.getLocalizedMessage());
			throw new NotJPAClientException(e.getLocalizedMessage(),e);
		}catch (NotJPAExceptionNoDataFound e) {
			logger.log(Level.INFO,e.getLocalizedMessage());
			throw new NotJPAClientException(e.getLocalizedMessage(),e);
		}
	};
	
	@Deprecated
	@Override
	public A add(A dto){
		try {
			return addE(dto);
		} catch (Exception e) {
			logger.log(Level.SEVERE,e.getLocalizedMessage(),e);
			dto.setE(e);
			dto.setMessage(e.getLocalizedMessage());
			dto.setSuccess(false);
			return dto;
		}
	}

	@Override
	public A addE(A dto) throws NotJPAClientException{
		try {
			B DBDTO = createBLocal().newInstance();
			BeanUtils.getInstance().copyProperties(DBDTO, dto);
			if (daoManager.checkKey(DBDTO)){
				if(daoManager.existsByKey(DBDTO)){
					dto.setMessage("Key violation");
					dto.setSuccess(false);
					return dto;
				}
			}
			daoManager.add(DBDTO);
			BeanUtils.getInstance().copyProperties(dto,DBDTO);
		} catch (Exception e) {
			logger.log(Level.SEVERE,e.getLocalizedMessage(),e);
			throw new NotJPAClientException(e.getLocalizedMessage(),e);
		}
		dto.setSuccess(true);
		return dto;
	}
	
	@Deprecated
	@Override
	public A update(A dto){
		try {
			return updateE(dto);
		} catch (Exception e) {
			logger.log(Level.SEVERE,e.getLocalizedMessage(),e);
			dto.setE(e);
			dto.setMessage(e.getLocalizedMessage());
			dto.setSuccess(false);
			return dto;
		}
	}

	@Override
	public A updateE(A dto) throws NotJPAClientException{
		
		try {
			B DBDTO = createBLocal().newInstance();
			BeanUtils.getInstance().copyProperties(DBDTO, dto);
			//if (daoManager.checkKey(DBDTO)){
			//	if(daoManager.existsByKey(DBDTO)){
					daoManager.update(DBDTO);
					if (daoManager.getRowsAffected() == 0)
						addE(dto);
			//	}else{
			//		addE(dto);
			//	}
			//}else{
			//	addE(dto);
			//}
		}catch (NotJPAExceptionNoDataChanged ce){
			dto.setSuccess(true);
			dto.setUpdatesuccess(false);
			return dto;
		}catch (Exception e) {
			logger.log(Level.SEVERE,e.getLocalizedMessage(),e);
			throw new NotJPAClientException(e.getLocalizedMessage(),e);
		}
		dto.setSuccess(true);
		dto.setUpdatesuccess(true);
		return dto;
	}
	
	@Deprecated
	@Override
	public A delete(A dto){
		try {
			return deleteE(dto);
		} catch (Exception e) {
			logger.log(Level.SEVERE,e.getLocalizedMessage(),e);
			dto.setE(e);
			dto.setMessage(e.getLocalizedMessage());
			dto.setSuccess(false);
			return dto;
		}
	}
	
	@Override
	public A deleteE(A dto)throws NotJPAClientException{
		try {
			B communicationDetailsDBDTO = createBLocal().newInstance();
			BeanUtils.getInstance().copyProperties(communicationDetailsDBDTO, dto);
			daoManager.delete(communicationDetailsDBDTO);
		} catch (Exception e) {
			logger.log(Level.SEVERE,e.getLocalizedMessage(),e);
			throw new NotJPAClientException(e.getLocalizedMessage(),e);
		}
		dto.setSuccess(true);
		return dto;
	}
	
}
