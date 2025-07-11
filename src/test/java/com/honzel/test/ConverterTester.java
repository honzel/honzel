package com.honzel.test;

import com.honzel.core.util.bean.BeanHelper;
import com.honzel.core.util.converter.DateConverter;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

public class ConverterTester {
	
	public static void main(String[] args) {
		ConverterTester tester = new ConverterTester();
		tester.testAbstractArrayConverter();
//		tester.testDateConverter();
	}
	
	public void testAbstractArrayConverter() {
		registerConverters();
		String input = "2025-11-20 23:00,2025-11-22 22:22-08:00,2025-11-22 22:22+09:00";
		Object result = BeanHelper.convert(input, LocalDateTime[].class);
		System.out.println(BeanHelper.convert(result, String.class));
	}

	private void registerConverters() {
		DateConverter converter = new DateConverter(BeanHelper.lookup(String.class));
		converter.setPatterns(
				new Class[] {
						Timestamp.class,
						Time.class,
						Date.class,
				},
				new String[] {
				"yyyy-MM-dd HH:mm:ss",
				"HH:mm",
				"yyyy-MM-dd", 
				"yyyy-MM-dd HH:mm",
				"yyyy-MM-dd"
			});
		BeanHelper.registerConverter(java.sql.Date.class, converter);
		BeanHelper.registerConverter(java.sql.Time.class, converter);
		BeanHelper.registerConverter(java.sql.Timestamp.class, converter);
		BeanHelper.registerConverter(Calendar.class, converter);
		BeanHelper.registerConverter(Date.class, converter);
		BeanHelper.registerConverter(Long.class, converter);
		BeanHelper.registerConverter(Long.TYPE, converter);
		BeanHelper.registerConverter(String.class, converter);
	}
	
	public void testDateConverter() {
		DateConverter converter = new DateConverter(BeanHelper.lookup(String.class));
		converter.setPatterns(new String[] {"yyyy-MM-dd HH:mm","yyyy-MM-dd", "yyyyMMddHHmm"});
		Calendar calendar = (Calendar) converter.convert("201411201744", Calendar.class);
		System.out.println(converter.convert(calendar, String.class));
		System.out.println(converter.convert(calendar, Long.class));
		
	}

}
