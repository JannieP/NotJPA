package net.c0nan.dao;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;


/**
 * @author Jannie Pieterse
 * - Does some date conversions
 * 
 */

public class Converter {
	static Logger logger = Logger.getLogger(Converter.class.getName());
	public Object convert(Object destination, Object source) {
		
		Class<?> destClass = destination.getClass();
		Class<?> sourceClass = source.getClass();

		if (Calendar.class.isAssignableFrom(sourceClass)) {
			if (Integer.class.isAssignableFrom(destClass)){
				return new Integer(DateHelper.convertTOInt((Calendar)source));
			}
		} else if (Integer.class.isAssignableFrom(sourceClass)){
			if (Calendar.class.isAssignableFrom(destClass)){
				try {
					return DateHelper.convertTOCalendar(((Integer)source).intValue());	
				} catch (ParseException pe) {
					throw new RuntimeException(source + " cannot be converted to a date.");	
				}			
			}else if(Date.class.isAssignableFrom(destClass)){
				DateFormat df = new SimpleDateFormat("yyyyMMdd");
				try {
					return new Date(df.parse(((Integer)source).toString()).getTime());
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}else if (Date.class.isAssignableFrom(sourceClass)){
			if (BigDecimal.class.isAssignableFrom(destClass)){
				DateFormat df = new SimpleDateFormat("yyyyMMdd");
				return new BigDecimal(df.format((Date)source));
			}
		}else if (BigDecimal.class.isAssignableFrom(sourceClass)){
			if(Date.class.isAssignableFrom(destClass)){
				DateFormat df = new SimpleDateFormat("yyyyMMdd");
				try {
					return new Date(df.parse(((BigDecimal)source).toString()).getTime());
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			
		}else {
			throw new RuntimeException(getClass() + " does not support " + sourceClass + " at this time.");
		}

		return null;

	}
}
