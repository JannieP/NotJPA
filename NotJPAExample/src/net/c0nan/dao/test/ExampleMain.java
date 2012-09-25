package net.c0nan.dao.test;

import net.c0nan.dao.exception.NotJPAClientException;
import net.c0nan.dao.exception.NotJPAException;

public class ExampleMain {

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
		System.out.println(id);

		ExampleDTO dto1 = new ExampleDTO();
		dto1.setCode("CDE");
		try {
			dto1 = sb.getE(dto);
		} catch (NotJPAClientException e) {
			e.printStackTrace();
		}
		System.out.println(dto1.getId()+"|"+dto1.getCode()+"|"+dto1.getDescription());
		
		try {
			sb.deleteDB();
		} catch (NotJPAException e) {
			//DB probably does NOT exist
			e.printStackTrace();
		}
		
	}

}
