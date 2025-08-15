package com.honzel.core.util.bean;

import com.honzel.core.constant.ArrayConstants;
import com.honzel.core.util.ConcurrentReferenceHashMap;
import com.honzel.core.util.converter.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 简单bean工具类
 * @author honzel
 *
 */
public class SimplePropertyUtilsBean extends BasePropertyUtilsBean<Method, Method, Class<?>> {

	private static final Logger log = LoggerFactory.getLogger(SimplePropertyUtilsBean.class);


	private static final SimplePropertyUtilsBean propertyUtilsBean = new SimplePropertyUtilsBean(new ConcurrentReferenceHashMap<>());

	private SimplePropertyUtilsBean(Map<Class<?>, Object[]> descriptorsCache) {
		super(descriptorsCache);
	}


	public static SimplePropertyUtilsBean getInstance() {
		return propertyUtilsBean;
	}


	@Override
	protected Class<?> initMethodLookup(Class<?> beanClass) {
		return beanClass;
	}

	@Override
	protected Method initGetter(PropertyDescriptor descriptor, Class<?> beanClass, Class<?> lookup) {
		Method method = descriptor.getReadMethod();
		if (method == null) {
			return null;
		}
		try {
			trySetAccessible(method);
		} catch (Exception e) {
			log.warn("Failed to trySetAccessible[{}] getter method", descriptor.getName(), e);
		}
		return method;
	}

	@Override
	protected Method initSetter(PropertyDescriptor descriptor, Class<?> beanClass, Class<?> lookup) {
		Method method = descriptor.getWriteMethod();
		if (method == null) {
			return null;
		}
		try {
			trySetAccessible(method);
		} catch (Exception e) {
			log.warn("Failed to trySetAccessible[{}] setter method", descriptor.getName(), e);
		}
		return method;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T newInstance(Class<T> beanClass) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		if (beanClass == null) {
			throw new NullPointerException("Class cannot be null");
		}
		Object[] beanInfoArray = findBeanInfoArray(beanClass);
		Constructor<T> constructor = (Constructor<T>) beanInfoArray[CONSTRUCTOR];
		if (constructor == null) {
			/*
			 * Use beanInfoArray as the lock object
			 * because it is an internally managed cache object,
			 * and for the same beanClass, it is nearly the same object instance,
			 * and the purpose of synchronization is to modify its item values.
			 */
			synchronized (beanInfoArray) {
				// DCL
				if ((constructor = (Constructor<T>) beanInfoArray[CONSTRUCTOR]) == null) {
					// Supplier
					try {
						beanInfoArray[CONSTRUCTOR] = constructor = beanClass.getDeclaredConstructor(ArrayConstants.EMPTY_CLASS_ARRAY);
						trySetAccessible(constructor);
					} catch (RuntimeException | NoSuchMethodException e) {
						// direct throw out
						throw e;
					} catch (Throwable e) {
						//  wrap
						throw new InvocationTargetException(e);
					}
				}
			}
		}
		try {
			return constructor.newInstance(ArrayConstants.EMPTY_OBJECT_ARRAY);
		} catch (InstantiationException e) {
			throw new InvocationTargetException(e);
		}
	}

	@Override
	protected boolean invokeWriteMethod(Object bean, Method setter, PropertyDescriptor descriptor, Object value) {
		if (setter == null) {
			return false;
		}
		try {
			if (typeConverter != null) {
				// 转换类型
				value = typeConverter.convert(value, descriptor.getPropertyType());
			}
			setter.invoke(bean, value);
			return true;
		} catch (Throwable e) {
			error(e, "Fail to set the property '" + descriptor.getName() + "' for the bean of the type '"
					+ bean.getClass().getName() + "', reason: " + e.getMessage());
		}
		return false;
	}

	@Override
	protected Object invokeReadMethod(Object bean, PropertyDescriptor descriptor) {
		Method readMethod = descriptor.getReadMethod();
		if (readMethod != null) {
			trySetAccessible(readMethod);
		}
		return invokeReadMethod(bean, readMethod, descriptor);
	}

	@Override
	protected Object invokeReadMethod(Object bean, Method getter, PropertyDescriptor descriptor) {
		if (getter == null) {
			return null;
		}
		try {
			return getter.invoke(bean);
		} catch (Throwable e) {
			error(e, "Fail to get the property '" + descriptor.getName() + "' for the bean of the type '"
					+ bean.getClass().getName() + "', reason: " + e.getMessage());
		}
		return null;
	}

	@Override
	protected Method getGetter(Object[] propertyArray) {
		return propertyArray != null ? (Method) propertyArray[GETTER] : null;
	}

	@Override
	protected Method getSetter(Object[] propertyArray) {
		return propertyArray != null ? (Method) propertyArray[SETTER] : null;
	}





}
