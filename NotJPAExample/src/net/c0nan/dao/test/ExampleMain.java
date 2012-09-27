package net.c0nan.dao.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.c0nan.dao.exception.NotJPAClientException;
import net.c0nan.dao.exception.NotJPAException;
import net.c0nan.dao.test.dto.ExampleDTO;
import net.c0nan.dao.test.service.ExampleServiceBean;

public class ExampleMain {

	private static Logger logger = Logger.getLogger(ExampleMain.class.getName());
	
	public static void main(String[] args) {

		ExampleServiceBean sb = new ExampleServiceBean();
		Long id = null;
		
		try {
			sb.createDB();
		} catch (NotJPAException e1) {
			//Most likely because the table already exists
			e1.printStackTrace();
		}
		
		ExampleDTO dto = new ExampleDTO();
		dto.setCode("CDE");
		dto.setDescription("qwerty");
		try {
			dto = sb.addE(dto);
			id = dto.getId();
		} catch (NotJPAClientException e) {
			e.printStackTrace();
		}
		logger.log(Level.INFO,"ID:"+id);

		ExampleDTO dto1 = new ExampleDTO();
		dto1.setCode("CDE");
		try {
			dto1 = sb.getE(dto);
		} catch (NotJPAClientException e) {
			e.printStackTrace();
		}
		logger.log(Level.INFO,"Record:"+dto1.getId()+"|"+dto1.getCode()+"|"+dto1.getDescription());
		
		try {
			sb.deleteDB();
		} catch (NotJPAException e) {
			//DB probably does NOT exist
			e.printStackTrace();
		}
		
	}

}
