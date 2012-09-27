package net.c0nan.dao.test.service;

import net.c0nan.dao.exception.NotJPAException;
import net.c0nan.dao.test.dto.ExampleDBDTO;
import net.c0nan.dao.test.dto.ExampleDTO;
import net.c0nan.service.impl.SharedBean;

public class ExampleServiceBean extends SharedBean<ExampleDTO, ExampleDBDTO> {
	public void createDB() throws NotJPAException {
		daoManager.runQuery(daoManager.getNamedQuery("createDB"));
	}
	public void deleteDB() throws NotJPAException {
		daoManager.runQuery(daoManager.getNamedQuery("deleteDB"));
	}
}
