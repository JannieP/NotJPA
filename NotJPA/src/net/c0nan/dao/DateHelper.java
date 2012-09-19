package net.c0nan.dao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Jannie Pieterse
 * - Does some date conversions
 */

public class DateHelper {
	public static final SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyyMMdd");
	public static final SimpleDateFormat HHmmss = new SimpleDateFormat("HHmmss");
	public static final SimpleDateFormat yyyyMMddHHmmss = 
		new SimpleDateFormat("yyyyMMddHHmmss");
	

	public static int now_yyyyMMdd() {
		return Integer.parseInt(yyyyMMdd.format(new Date()));
	}
	
	public static int now_HHmmss() {
		return Integer.parseInt(HHmmss.format(new Date()));
	}
	
	public static Calendar convertTOCalendar(int date) throws ParseException {
		if (date <= 0) {
			return null;
		} else {
			String s = ""+date;
			return convert(s, yyyyMMdd);			
		}
	}

	public static Calendar convertTOCalendar(int date , int time) throws ParseException {
		if (time <= 0) {
			return convertTOCalendar(date);
		} else {
		  String sTime = (time < 99999) ? "0"+time : ""+time; 
		  
		  // need to pad with 0's
		  while(sTime.length()<6)
		  {
		  	sTime = "0" + sTime;
		  }
		  
		  String s = "" + date  + sTime;
		  return convert(s, yyyyMMddHHmmss);			 
		}
	}
	
	private static Calendar convert(String date, SimpleDateFormat sdf) throws ParseException{
		Calendar cal = Calendar.getInstance();
		cal.setTime(sdf.parse(date));
		return cal;				
	}
	
	public static int convertTOInt(Calendar cal) {
		return Integer.parseInt(yyyyMMdd.format(cal.getTime()));
	}
	
	public static int getDatePart(Calendar cal) {
		return Integer.parseInt(yyyyMMddHHmmss.format(cal.getTime()).substring(0,8));
	}
	
	public static int getTimePart(Calendar cal) {
		return Integer.parseInt(yyyyMMddHHmmss.format(cal.getTime()).substring(8));
	}

}
