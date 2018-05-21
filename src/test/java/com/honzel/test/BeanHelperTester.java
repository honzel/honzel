package com.honzel.test;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.honzel.core.util.BeanHelper;
import com.honzel.core.util.converters.DateConverter;
import com.honzel.core.util.converters.StandardConverter;
import com.honzel.core.vo.Branch;
import com.honzel.core.vo.Entry;

public class BeanHelperTester {
	
	private BeanHelperTester init() {
		// TODO Auto-generated method stub
//		BeanHelper.setDisableException(true);
		DateConverter dateConverter = new DateConverter(new StandardConverter());
		dateConverter.setPatterns("yyyy-MM-dd HH:mm:ss", "yyyyMMdd");
		BeanHelper.registerConverter(java.sql.Date.class, dateConverter);
		BeanHelper.registerConverter(java.sql.Timestamp.class, dateConverter);
		BeanHelper.registerConverter(java.sql.Time.class, dateConverter);
		BeanHelper.registerConverter(java.util.Date.class, dateConverter);
		BeanHelper.registerConverter(java.util.Calendar.class, dateConverter);
		BeanHelper.registerConverter(Long.class, dateConverter);
		BeanHelper.registerConverter(String.class, dateConverter);
		return this;
	}
	
	private void testBeanAccess() {
		// TODO Auto-generated method stub
		Branch bean = new Branch();
		BeanHelper.setProperty(bean, "key", "中国人");
		BeanHelper.setProperty(bean, "left", new Branch());
		BeanHelper.setProperty(bean, "right", new Entry());
		BeanHelper.setProperty(bean, "left.key", "美国人");
		BeanHelper.setProperty(bean, "left['right']", BeanHelper.convert(new Date(), String.class));
		BeanHelper.setProperty(bean, "left.left", BeanHelper.convert(1001234, String.class));
		BeanHelper.setProperty(bean, "right.key", "英国人");
		BeanHelper.setProperty(bean, "right.key", BeanHelper.convert("20150104", Calendar.class));
		BeanHelper.setProperty(bean, "right.value", BeanHelper.convert("20150104", Long.class));
		Map result = new HashMap();
		System.out.println(bean);
		BeanHelper.copyProperties(bean, result);
		System.out.println(result);

	}
	
	
	
	public static void main(String[] args) {
		BeanHelperTester tester = new BeanHelperTester().init();
		tester.testBeanAccess();
	}

}
