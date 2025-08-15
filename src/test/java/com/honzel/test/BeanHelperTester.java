package com.honzel.test;

import com.honzel.core.util.bean.BeanHelper;
import com.honzel.core.util.bean.LambdaPropertyUtilsBean;
import com.honzel.core.util.bean.SimplePropertyUtilsBean;
import com.honzel.core.util.converter.DateConverter;
import com.honzel.core.util.converter.StandardConverter;
import com.honzel.core.vo.Branch;
import org.apache.commons.lang3.time.StopWatch;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

	private void testBeanAccess() throws Throwable {
		// TODO Auto-generated method stub
		Branch<Long, Object, Object> bean = BeanHelper.newInstance(Branch.class);
		Method readMethod = BeanHelper.getPropertyDescriptor(Branch.class, "key").getReadMethod();
		Method writtenMethod = BeanHelper.getPropertyDescriptor(Branch.class, "key").getWriteMethod();
		Function<Branch, Long> getter = BeanHelper.getPropertyGetter(Branch.class, "key");
		BiConsumer<Branch, Long> setter = BeanHelper.getPropertySetter(Branch.class, "key");
		MethodHandles.Lookup lookup = LambdaPropertyUtilsBean.getInstance().getMethodLookup(Branch.class);
		MethodHandle getterHandle = lookup.unreflect(readMethod);
		MethodHandle setterHandle = lookup.unreflect(writtenMethod);

		long count = 10_0000_0000;
		bean.setKey(count);
		StopWatch stopWatch = new StopWatch();
		System.out.println("每一个循环" + count + "次");

		stopWatch.start();
		for (long i = 0; i < count; i++) {
			Long key = bean.getKey();
		}
		stopWatch.suspend();
		System.out.println("getKey: " + stopWatch.getTime() + "毫秒");
		stopWatch.reset();

		stopWatch.start();
		for (long i = 0; i < count; i++) {
			Object key = getter.apply(bean);
		}
		stopWatch.suspend();
		System.out.println("getter: " + stopWatch.getTime() + "毫秒");
		stopWatch.reset();

		stopWatch.start();
		for (long i = 0; i < count; i++) {
			Object key = getterHandle.invoke(bean);
		}
		stopWatch.suspend();
		System.out.println("getterHandle: " + stopWatch.getTime() + "毫秒");
		stopWatch.reset();

		stopWatch.start();
		for (long i = 0; i < count; i++) {
			Object key = readMethod.invoke(bean);
		}
		stopWatch.suspend();
		System.out.println("readMethod: " + stopWatch.getTime() + "毫秒");
		stopWatch.reset();

		stopWatch.start();
		for (long i = 0; i < count; i++) {
			Object key = LambdaPropertyUtilsBean.getInstance().getProperty(bean, "key", false);
		}
		stopWatch.suspend();
		System.out.println("getProperty: " + stopWatch.getTime() + "毫秒");
		stopWatch.reset();

		stopWatch.start();
		for (long i = 0; i < count; i++) {
			bean.setKey(i);
		}
		stopWatch.suspend();
		System.out.println("setKey: " + stopWatch.getTime() + "毫秒");
		stopWatch.reset();

		stopWatch.start();
		for (long i = 0; i < count; i++) {
			setter.accept(bean, i);
		}
		stopWatch.suspend();
		System.out.println("setter: " + stopWatch.getTime() + "毫秒");
		stopWatch.reset();

		stopWatch.start();
		for (long i = 0; i < count; i++) {
			setterHandle.invoke(bean, i);
		}
		stopWatch.suspend();
		System.out.println("setterHandle: " + stopWatch.getTime() + "毫秒");
		stopWatch.reset();

		stopWatch.start();
		for (long i = 0; i < count; i++) {
			writtenMethod.invoke(bean, i);
		}
		stopWatch.suspend();
		System.out.println("writtenMethod: " + stopWatch.getTime() + "毫秒");
	}


	public static void main(String[] args) throws Throwable {
		BeanHelperTester tester = new BeanHelperTester().init();
		tester.testBeanAccess();
	}

}
