package com.honzel.core.util.bean;

import com.honzel.core.util.lambda.LambdaUtils;
import com.honzel.core.util.lambda.MethodHandleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.invoke.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 简单bean工具类
 * @author honzel
 *
 */
@SuppressWarnings({ "unchecked"})
public class LambdaPropertyUtilsBean extends BasePropertyUtilsBean<Function<Object, Object>, BiConsumer<Object, Object>, MethodHandles.Lookup> {

	private static final Logger log = LoggerFactory.getLogger(LambdaPropertyUtilsBean.class);

	private static final MethodType GETTER_FACTORY = LambdaUtils.METHOD_TYPE_FUNCTION;
	private static final MethodType GETTER_ERASE = MethodType.methodType(Object.class, Object.class);
	private static final MethodType SETTER_FACTORY = LambdaUtils.METHOD_TYPE_BI_CONSUMER;
	private static final MethodType SETTER_ERASE = MethodType.methodType(void.class, Object.class, Object.class);
	private static final MethodType CONSTRUCTOR_FACTORY = LambdaUtils.METHOD_TYPE_SUPPLIER;
	private static final MethodType VOID_TYPE = MethodType.methodType(void.class);
	private static final Function<Object, Object> INVALID_GETTER = Function.identity();
	private static final BiConsumer<Object, Object> INVALID_SETTER = (b, v) -> {};

	private static final LambdaPropertyUtilsBean propertyUtilsBean = new LambdaPropertyUtilsBean(new ConcurrentHashMap<>());

	private LambdaPropertyUtilsBean(Map<Class<?>, Object[]> descriptorsCache) {
		super(descriptorsCache);
	}


	public static LambdaPropertyUtilsBean getInstance() {
		return propertyUtilsBean;
	}

	@Override
	protected MethodHandles.Lookup initMethodLookup(Class<?> beanClass) {
		return MethodHandleUtils.lookup(beanClass);
	}

	@Override
	protected Function<Object, Object> initGetter(PropertyDescriptor descriptor, Class<?> beanClass, MethodHandles.Lookup lookup) {
		Method method = descriptor.getReadMethod();
		if (method == null) {
			return null;
		}
		if ("class".equals(descriptor.getName())) {
			// getter
			return INVALID_GETTER;
		}
		try {
			MethodHandle handle = lookup.unreflect(method);
			MethodType getterSignature = MethodType.methodType(descriptor.getPropertyType(), beanClass);
			CallSite callSite = LambdaMetafactory.metafactory(lookup, "apply", GETTER_FACTORY, GETTER_ERASE, handle, getterSignature);
			return (Function<Object, Object>) callSite.getTarget().invokeExact();
		} catch (Throwable e) {
			log.warn("Failed to generate getter[{}] lambda", descriptor.getName(), e);
			return INVALID_GETTER;
		}
	}

	@Override
	protected BiConsumer<Object, Object> initSetter(PropertyDescriptor descriptor, Class<?> beanClass, MethodHandles.Lookup lookup) {
		Method method = descriptor.getWriteMethod();
		if (method == null) {
			return null;
		}
		try {
			MethodHandle handle = lookup.unreflect(method);
			MethodType methodType = MethodType.methodType(void.class, beanClass, descriptor.getPropertyType());
			CallSite callSite = LambdaMetafactory.metafactory(lookup, "accept", SETTER_FACTORY, SETTER_ERASE, handle, methodType);
			return (BiConsumer<Object, Object>) callSite.getTarget().invokeExact();
		} catch (Throwable e) {
			log.warn("Failed to generate setter[{}] lambda", descriptor.getName(), e);
			return INVALID_SETTER;
		}
	}

	@Override
	protected Function<Object, Object> getGetter(Object[] propertyArray) {
		return propertyArray != null ? (Function<Object, Object>) propertyArray[GETTER] : null;
	}

	@Override
	protected BiConsumer<Object, Object> getSetter(Object[] propertyArray) {
		return propertyArray != null ? (BiConsumer<Object, Object>) propertyArray[SETTER] : null;
	}


	/**
	 * fetch getter
	 * @param beanClass bean class
	 * @param name property name
	 * @return getter
	 */
	public<T, P> Function<T, P> getPropertyGetter(Class<T> beanClass, String name) {
		if (beanClass == null) {
			return null;
		}
		Object[] propertyArray = getPropertyArray(beanClass, name);
		Function<Object, Object> getter = getGetter(propertyArray);
		if (getter != INVALID_GETTER) {
			return (Function<T, P>) getter;
		}
		return bean -> (P) invokeReadMethod(bean, INVALID_GETTER, getDescriptor(propertyArray));
	}
	/**
	 * fetch setter
	 * @param beanClass bean class
	 * @param name property name
	 * @return setter
	 */
	public<T, P> BiConsumer<T, P> getPropertySetter(Class<T> beanClass, String name) {
		if (beanClass == null) {
			return null;
		}
		Object[] propertyArray = getPropertyArray(beanClass, name);
		BiConsumer<Object, Object> setter = getSetter(propertyArray);
		if (setter != INVALID_SETTER) {
			return (BiConsumer<T, P>) setter;
		}
		return (bean, value) -> invokeWriteMethod(bean, INVALID_SETTER, getDescriptor(propertyArray), value);
	}

	/**
	 * Creates a new instance of the class represented by this {@code Class}
	 * object.  The class is instantiated as if by a {@code new}
	 * expression with an empty argument list.  The class is initialized if it
	 * has not already been initialized.
	 * @param beanClass the class to create an instance of
	 * @return  a newly allocated instance of the class represented by this
	 * @throws  InvocationTargetException InvocationTargetException
	 * @throws NoSuchMethodException if a matching method is not found
	 *         or if the name is "&lt;init&gt;"or "&lt;clinit&gt;".
	 * @throws  IllegalAccessException  if the class or its nullary
	 *          constructor is not accessible.
	 */
	@SuppressWarnings("SynchronizationOnLocalVariable")
	public<T> T newInstance(Class<T> beanClass) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		if (beanClass == null) {
			throw new NullPointerException("Class cannot be null");
		}
		Object[] beanInfoArray = findBeanInfoArray(beanClass);
		Supplier<T> supplier = (Supplier<T>) beanInfoArray[CONSTRUCTOR];
		if (supplier != null) {
			// 构造函数
			return supplier.get();
		}
		/*
		 * Use beanInfoArray as the lock object
		 * because it is an internally managed cache object,
		 * and for the same beanClass, it is nearly the same object instance,
		 * and the purpose of synchronization is to modify its item values.
		 */
		synchronized (beanInfoArray) {
			// DCL
			if ((supplier = (Supplier<T>) beanInfoArray[CONSTRUCTOR]) == null) {
				MethodHandles.Lookup lookup = (MethodHandles.Lookup) beanInfoArray[LOOKUP];
				// Supplier
				MethodHandle constructor = lookup.findConstructor(beanClass, VOID_TYPE);
				try {
					CallSite callSite = LambdaMetafactory.metafactory(
							lookup,
							"get",
							CONSTRUCTOR_FACTORY,
							MethodType.methodType(Object.class),
							constructor,
							MethodType.methodType(beanClass)
					);
					beanInfoArray[CONSTRUCTOR] = (supplier = (Supplier<T>) callSite.getTarget().invokeExact());
				} catch (RuntimeException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
					// direct throw out
					throw e;
				} catch (Throwable e) {
					//  wrap
					throw new InvocationTargetException(e);
				}
			}
		}
		return supplier.get();
	}


	protected boolean invokeWriteMethod(Object bean, BiConsumer<Object, Object> setter, PropertyDescriptor descriptor, Object value) {
		if (setter == null) {
			return false;
		}
		try {
			// 转换类型
			value = typeConverter.convert(value, descriptor.getPropertyType());
			if (setter != INVALID_SETTER) {
				setter.accept(bean, value);
			} else {
				Method writeMethod = descriptor.getWriteMethod();
				trySetAccessible(writeMethod);
				writeMethod.invoke(bean, value);
			}
			return true;
		} catch (Throwable e) {
			error(e, "Fail to set the property '" + descriptor.getName() + "' for the bean of the type '"
					+ bean.getClass().getName() + "', reason: " + e.getMessage());
		}
		return false;
	}
	boolean invokeWriteMethod(Object bean, Object[] propertyArray, Object value) {
		return invokeWriteMethod(bean, getSetter(propertyArray), getDescriptor(propertyArray), value);
	}

	protected Object invokeReadMethod(Object bean, PropertyDescriptor descriptor) {
		return invokeReadMethod(bean, INVALID_GETTER, descriptor);
	}
	protected Object invokeReadMethod(Object bean, Function<Object, Object> getter, PropertyDescriptor descriptor) {
		if (getter == null) {
			return null;
		}
		try {
			if (getter != INVALID_GETTER) {
				return getter.apply(bean);
			}
			Method readMethod = descriptor.getReadMethod();
			trySetAccessible(readMethod);
			readMethod.invoke(bean);
		} catch (Throwable e) {
			error(e, "Fail to get the property '" + descriptor.getName() + "' for the bean of the type '"
					+ bean.getClass().getName() + "', reason: " + e.getMessage());
		}
		return null;
	}

}
