package com.honzel.core.util.bean;

import com.honzel.core.constant.ArrayConstants;
import com.honzel.core.util.ConcurrentReferenceHashMap;
import com.honzel.core.util.converter.TypeConverter;
import com.honzel.core.util.exception.PropertyException;
import com.honzel.core.util.lambda.MethodHandleUtils;
import com.honzel.core.vo.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 简单bean工具类
 * @author honzel
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SimplePropertyUtilsBean {

	private static final Logger logger = LoggerFactory.getLogger(SimplePropertyUtilsBean.class);

	private static  SimplePropertyUtilsBean propertyUtilsBean = new SimplePropertyUtilsBean(
			new ConcurrentReferenceHashMap(),
			new TypeConverter()
	);

	private static final MethodType GETTER_FACTORY = MethodHandleUtils.METHOD_TYPE_FUNCTION;
	private static final Function<Object, Object> INVALID_GETTER = Function.identity();
	private static final MethodType GETTER_ERASE = MethodType.methodType(Object.class, Object.class);
	private static final MethodType SETTER_FACTORY = MethodHandleUtils.METHOD_TYPE_BI_CONSUMER;
	private static final MethodType SETTER_ERASE = MethodType.methodType(void.class, Object.class, Object.class);
	private static final MethodType CONSTRUCTOR_FACTORY = MethodHandleUtils.METHOD_TYPE_SUPPLIER;
	private static final MethodType VOID_TYPE = MethodType.methodType(void.class);
	private static final BiConsumer<Object, Object> INVALID_SETTER = (b, v) -> {};
	private static final int DESCRIPTORS = 0;
	private static final int DESCRIPTOR_MAP = 1;
	private static final int LOOKUP = 2;
	private static final int GETTER_MAP = 3;
	private static final int SETTER_MAP = 4;
	private static final int CONSTRUCTOR = 5;


	private  final  Map descriptorsCache;

	private TypeConverter typeConverter;

	/**
	 * disable exception or not
	 */
	private boolean disableException;

	/**
	 * disable exception or not
	 * @param disableException whether or not to disable throw exception
	 */
	public void setDisableException(boolean disableException) {
		this.disableException = disableException;
		if (typeConverter != null) {
			typeConverter.setDisableException(disableException);
		}
	}

	public boolean isDisableException() {
		return disableException;
	}


	public TypeConverter getTypeConverter() {
		return typeConverter;
	}

	private SimplePropertyUtilsBean(Map descriptorsCache, TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
		this.descriptorsCache = descriptorsCache;
	}

	/**
	 * Return a new instance of <code>PropertyUtilsBean</code>
	 * @param typeConverter the specified converter.
	 * @return Return the SimplePropertyUtilsBean bean instance.
	 */
	public static SimplePropertyUtilsBean getInstance(TypeConverter typeConverter) {
		return new SimplePropertyUtilsBean(null, typeConverter);
	}

	public static SimplePropertyUtilsBean getInstance() {
		return propertyUtilsBean;
	}

	boolean isList(Class clz) {
		return clz != null && (clz.isArray() || Iterable.class.isAssignableFrom(clz));
	}

	private boolean isList(Object bean) {
		return bean != null && isList(bean.getClass());
	}


	/**
	 * Set the value of the specified simple property of the specified bean,
	 * with no type conversions.
	 *
	 * @param bean Bean whose property is to be modified
	 * @param key Key/Name of the property to be modified
	 * @param value Value to which the property should be set
	 * @param mapped whether is mapped or not
	 * @return Return true when success to set,otherwise return false
	 */
	public boolean setProperty(Object bean, Object key, Object value, boolean mapped) {
		if (bean == null) {
			error(null, "Bean missing");
			return false;
		}
		PropertyDescriptor descriptor = null;
		Class beanClass = bean.getClass();
		Object[] descriptorArray = getDescriptorArray(beanClass);
		String name = null;
		if (!mapped && key != null) {
			descriptor = getDescriptor0(descriptorArray, name = key.toString());
		}
		try {
			if (descriptor == null) {
				if (Map.class.isAssignableFrom(beanClass)) {
					((Map) bean).put(key, value);
					return true;
				}
				if (isList(beanClass)) {
					int ind = findIndex(key);
					setItem(bean, ind, value);
					return true;
				}
				if (mapped) {
					descriptor = getDescriptor0(descriptorArray, name = key.toString());
				}
				if (descriptor == null) {
					throw new PropertyException("The property  '" + key + "' of bean type  '"
							+ beanClass.getName() + "' isn't exists.");
				}
			}
			BiConsumer<Object, Object> setter = getSetter(descriptorArray, name);
			if (setter == null) {
				throw new PropertyException("The setter of property  '" + key + "' of bean type  '"
						+ beanClass.getName() + "' isn't exists.");
			}
			return invokeWriteMethod(bean, setter, descriptor, value);
		} catch (PropertyException e) {
			error(e, null);
		} catch (Exception e) {
			error(e, "Fail to set the property '" + key + "' for the bean of the type '"
					+ beanClass.getName() + "', reason: " + e);
			//
		}
		return false;
	}


	int findIndex(Object key) {
		int index = -1;
		if (key instanceof Number) {
			index = ((Number) key).intValue();
		} else {
			try {
				index = Integer.parseInt(key.toString());
			} catch (Exception ignore) {}
		}
		return index;
	}

	/**
     * Return the value of the specified simple property of the specified
     * bean, with no type conversions.
     *
     * @param bean Bean whose property is to be extracted
     * @param key Possibly indexed and/or simple name of the property  to be extracted
     * @param mapped whether map or not.
     * @return Return the value of the specified simple property
     */
    public Object getProperty(Object bean, Object key, boolean mapped) {
		if (bean == null) {
			return null;
		}
		Object[] descriptorArray = getDescriptorArray(bean.getClass());
		String name = null;
		Function<Object, Object> getter = null;
		if (!mapped && key != null) {
			getter = getGetter(descriptorArray, name = key.toString());
		}
		if (getter == null) {
			if (bean instanceof Map) {
				return ((Map) bean).get(key);
			}
			if (key == null) {
				return null;
			}
			if (isList(bean)) {
				int ind = findIndex(key);
				return getItem(bean, ind);
			}
			if (mapped) {
				getter = getGetter(descriptorArray, name = key.toString());
			}
			if (getter == null) {
				return null;
			}
		}
		return invokeReadMethod(bean, getter, descriptorArray, name);
    }

	/**
     * Return the value of the specified indexed element of the specified
     * bean, with no type conversions.  The zero-relative index of the
     * required value must be included (in square brackets) as a suffix to
     * the property name, or <code>IllegalArgumentException</code> will be
     * thrown.  In addition to supporting the JavaBeans specification, this
     * method has been extended to support <code>List</code> objects as well.
     *
     * @param collection Collection whose element to be extracted
     * @param index index of the collection  to be extracted
	 * @return Return the value of the specified indexed element of the specified bean
	 */
	private Object getItem(Object collection, int index) {
		if (index < 0)
			return null;
		if (collection != null && collection.getClass().isArray()) {
			if (index < Array.getLength(collection)) {
				return Array.get(collection, index);
			}
		} else if (collection instanceof List) {
			if (index < ((List) collection).size()) {
				return ((List) collection).get(index);
			}
		} else if (collection instanceof Iterable) {
			Iterator iterator = ((Iterable) collection).iterator();
			while(index-- > 0 && iterator.hasNext()) {
				iterator.next();
			}
			if (iterator.hasNext()) {
				return iterator.next();
			}
		}
		return null;
	}

	/**
     * Set the value of the specified indexed element of the specified
     * bean, with no type conversions.  The zero-relative index of the
     * required value, In addition to supporting the JavaBeans specification, this
     * method has been extended to support <code>List</code> objects as well.
     *
     * @param collection collection  is to be modified
     * @param index index of the specified  collection to be modified
	 * @param value Value to which the specified indexed element
	 */

	private void setItem(Object collection, int index, Object value) {
		Class collectionClass = collection.getClass();
		if (index < 0)
			throw new ArrayIndexOutOfBoundsException(index);
		if (collectionClass.isArray()) {
			if (typeConverter != null) {
				value = typeConverter.convert(value, collectionClass.getComponentType());
			}
			Array.set(collection, index, value);
		} else if (List.class.isAssignableFrom(collectionClass)) {
			List list = (List) collection;
			if (index < list.size()) {
				list.set(index, value);
				return ;
			}
			for (int i = list.size(); i < index; i++) {
				list.add(null);
			}
			list.add(value);
		} else if (Collection.class.isAssignableFrom(collectionClass)) {
			((Collection) collection).add(value);
		} else {
			throw new UnsupportedOperationException("Fail to set the value into the  "
					+ "specified indexed bean of the type '" + collectionClass.getName() + "'");
		}
	}

	/**
     * Return the Java Class representing the property type of the specified
     * simple property, or <code>null</code> if there is no such property for the
     * specified bean.
     * @param beanClass Bean class for which a property descriptor is requested
     * @param name Simple name of the property for which a property descriptor is requested
	 * @return The property type of the specified bean class
	 */
	public Class getPropertyType(Class beanClass, String name) {
		PropertyDescriptor descriptor = getDescriptor0(getDescriptorArray(beanClass), name);
		if (descriptor != null) {
			return descriptor.getPropertyType();
		}
		if (Map.class.isAssignableFrom(beanClass)) {
			return Object.class;
		}
		if (isList(beanClass)) {
			int ind = findIndex(name);
			if (ind < 0) {
				return null;
			}
			if (beanClass.isArray()) {
				return beanClass.getComponentType();
			} else {
				return Object.class;
			}
		}
		return null;
	}

	void copyBeanByBean(Object target, Object source) {
		Object[] srcDescriptorArray = getDescriptorArray(source.getClass());
		PropertyDescriptor[] descriptors0 = getDescriptors0(srcDescriptorArray);
		if (descriptors0.length == 0) {
			return;
		}
		Class targetType = target.getClass();
		Object[] destDescriptorArray = getDescriptorArray(targetType);
		for (PropertyDescriptor descriptor0 : descriptors0) {
			if (descriptor0 == null || descriptor0.getReadMethod() == null) {
				continue;
			}
			String name = descriptor0.getName();
			BiConsumer<Object, Object> setter = getSetter(destDescriptorArray, name);
			if (setter != null) {
				Object value = invokeReadMethod(source, getGetter(srcDescriptorArray, name), srcDescriptorArray, name);
				invokeWriteMethod(target, getDescriptor0(destDescriptorArray, name), value);
			}
		}
	}

	void copyMapByBean(Map target, Object source) {
		Class sourceType = source.getClass();
		Object[] descriptorArray = getDescriptorArray(sourceType);
		for (PropertyDescriptor descriptor : getDescriptors0(descriptorArray)) {
			String name = descriptor.getName();
			if ("class".equals(name) || descriptor.getReadMethod() == null) {
				continue;
			}
			try {
				target.put(name, invokeReadMethod(source, getGetter(descriptorArray, name), descriptorArray, name));
			} catch (Exception e) {
				this.error(e, "Fail to get the specified property '" + name + "' for  the specified bean of the type '" + sourceType.getName() + "', reason: " + e);
			}
		}

	}

	private void copyBeanByMap(Object target, Map source) {
		if (source.isEmpty()) {
			return;
		}
		Set<Map.Entry> entries = source.entrySet();
		Class<?> targetType = target.getClass();
		Object[] descriptorArray = getDescriptorArray(targetType);
		for (Map.Entry entry : entries) {
			if (!(entry.getKey() instanceof String)) {
				continue;
			}
			String name = (String) entry.getKey();
			BiConsumer<Object, Object> setter = getSetter(descriptorArray, name);
			if (setter == null) {
				// No setter
				continue;
			}
			try {
				PropertyDescriptor descriptor = getDescriptor0(descriptorArray, name);
				Object value = entry.getValue();
				if (typeConverter != null) {
					//
					value = typeConverter.convert(value, descriptor.getPropertyType());
				}
				propertyUtilsBean.invokeWriteMethod(target, setter, descriptor, value);
			} catch (Exception e) {
				error(e, "Fail to set the specified property '" + name + "' for the specified bean of the type '"
						+ targetType.getName() + "', reason: " + e);
			}
		}
	}


	/**
	 * <p>Copy property values from the "origin" bean to the "destination" bean
	 * for all cases where the property names are the same (even though the
	 * actual getter and setter methods might have been customized via
	 * <code>BeanInfo</code> classes or specified).
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be set in the destination
	 * bean.<strong>Note</strong> that this method is intended to perform
	 * a "shallow copy" of the properties and so complex properties
	 * (for example, nested ones) will not be copied.</p>
	 *
	 * @param source Origin bean whose properties are retrieved
	 * @param target Destination bean whose properties are modified
	 * @param <T> the destination bean type
	 * @return returns the destination bean whose properties are modified
	 */
	public <T> T copyProperties(Object source, T target) {
		if (source == null || target == null || source == target) {
			return target;
		}
		if (source instanceof Map) {
			if (target instanceof Map) {
				((Map) target).putAll((Map) source);
			} else {
				copyBeanByMap(target, (Map) source);
			}
		} else {
			if (target instanceof Map) {
				copyMapByBean((Map) target, source);
			} else {
				copyBeanByBean(target, source);
			}
		}
		return target;
	}

	/**
	 * <p>Retrieve the property descriptor for the specified property of the
     * specified bean class, or return <code>null</code> if there is no such
     * descriptor. </p>
     * @param beanClass Bean class for which a property descriptor is requested
     * @param name Possibly indexed and/or nested name of the property for
     *  which a property descriptor is requested
	 * @return the property descriptor for the specified property of the
     * specified bean, or return <code>null</code>
	 */
	public PropertyDescriptor getDescriptor(Class beanClass, String name) {
		return getDescriptor0(getDescriptorArray(beanClass), name);
    }


	private Object[] getDescriptorArray(Class beanClass) {
		if (beanClass == null) {
			return null;
        }
		return (Object[]) getInstance().descriptorsCache.computeIfAbsent(beanClass, clazz -> {
			MethodHandles.Lookup lookup = MethodHandleUtils.lookup(beanClass);
			Object[] descriptorArray = new Object[6];
			descriptorArray[LOOKUP] = lookup;
			PropertyDescriptor[] descriptors;
			try {
				descriptors = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();
			} catch (IntrospectionException e) {
				logger.warn("Fail to get the bean info for the specified bean of the type '{}'", beanClass.getName(), e);
				descriptors = null;
			}
			if (descriptors == null || descriptors.length == 0) { // 没有属性
				descriptorArray[DESCRIPTORS] = ArrayConstants.EMPTY_DESCRIPTOR_ARRAY;
				descriptorArray[DESCRIPTOR_MAP] = Collections.emptyMap();
				descriptorArray[GETTER_MAP] = Collections.emptyMap();
				descriptorArray[SETTER_MAP] = Collections.emptyMap();
				return descriptorArray;
			}
			Map descriptorMap = new LinkedHashMap();
			Map<String, Function> getterMap =  new HashMap<>();
			Map<String, BiConsumer<Object, Object>> setterMap =  new HashMap<>();
			descriptorArray[DESCRIPTORS] = descriptors;
			descriptorArray[DESCRIPTOR_MAP] = descriptorMap;
			descriptorArray[GETTER_MAP] = getterMap;
			descriptorArray[SETTER_MAP] = setterMap;
			MethodType getterSignature = MethodType.methodType(Object.class, beanClass);
			for (PropertyDescriptor descriptor : descriptors) {
				String name = descriptor.getName();
				descriptorMap.put(name, descriptor);
				try {
					Method method = descriptor.getReadMethod();
					if (method != null) {
						// getter
						try {
							MethodHandle handle = lookup.unreflect(method);
							CallSite callSite = LambdaMetafactory.metafactory(lookup, "apply", GETTER_FACTORY, GETTER_ERASE, handle, getterSignature);
							getterMap.put(name, (Function<Object, Object>) callSite.getTarget().invokeExact());
						} catch (Throwable e) {
							logger.warn("Failed to generate getter[{}] lambda", name, e);
							method.setAccessible(true);
							getterMap.put(name, INVALID_GETTER);
						}
					}
					method = descriptor.getWriteMethod();
					if (method != null) {
						// setter
						try {
							MethodHandle handle = lookup.unreflect(method);
							MethodType methodType = MethodType.methodType(void.class, beanClass, descriptor.getPropertyType());
							CallSite callSite = LambdaMetafactory.metafactory(lookup, "accept", SETTER_FACTORY, SETTER_ERASE, handle, methodType);
							setterMap.put(name, (BiConsumer<Object, Object>) callSite.getTarget().invokeExact());
						} catch (Throwable e) {
							logger.warn("Failed to generate setter[{}] lambda", name, e);
							method.setAccessible(true);
							setterMap.put(name, INVALID_SETTER);
						}
					}
				} catch (Exception e) {
					logger.warn("Failed to get the specified property '{}' for  the specified bean type '{}', reason: {}", name, beanClass.getName(), e.getMessage(), e);
				}
			}
			return descriptorArray;
		});
	}

	/**
     * <p>Retrieve the property descriptors for the specified class,
     * introspecting and caching them the first time a particular bean class
     * is encountered.</p>
     *
     * @param beanClass Bean class for which property descriptors are requested
	 * @return the property descriptors for the specified class
	 */
	Map getDescriptorMap(Class beanClass) {
		Object[] descriptorArray = getDescriptorArray(beanClass);
		if (descriptorArray != null) {
			return (Map) descriptorArray[DESCRIPTOR_MAP];
        }
		return null;
	}

	/**
     * <p>Retrieve the property descriptors for the specified class,
     * introspecting and caching them the first time a particular bean class
     * is encountered.</p>
     *
     * @param beanClass Bean class for which property descriptors are requested
	 * @return the property descriptors for the specified class
	 */
	public PropertyDescriptor[] getPropertyDescriptors(Class beanClass) {
		Object[] descriptorArray = getDescriptorArray(beanClass);
		if (descriptorArray != null) {
			return (PropertyDescriptor[]) descriptorArray[DESCRIPTORS];
		}
		return ArrayConstants.EMPTY_DESCRIPTOR_ARRAY;
	}

	/**
	 * Creates a new instance of the class represented by this {@code Class}
	 * object.  The class is instantiated as if by a {@code new}
	 * expression with an empty argument list.  The class is initialized if it
	 * has not already been initialized.
	 * @param clazz the class to create an instance of
	 * @return  a newly allocated instance of the class represented by this
	 * @throws  InvocationTargetException InvocationTargetException
	 * @throws NoSuchMethodException if a matching method is not found
	 *         or if the name is "&lt;init&gt;"or "&lt;clinit&gt;".
	 * @throws  IllegalAccessException  if the class or its nullary
	 *          constructor is not accessible.
	 */
	public<T> T newInstance(Class<T> clazz) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		if (clazz == null) {
			throw new NullPointerException("Class cannot be null");
		}
		Object[] descriptorArray = getDescriptorArray(clazz);
		Supplier<T> supplier = (Supplier<T>) descriptorArray[CONSTRUCTOR];
		if (supplier != null) {
			// 构造函数
			return supplier.get();
		}
		synchronized (clazz) {
			// DCL
			if ((supplier = (Supplier<T>) descriptorArray[CONSTRUCTOR]) == null) {
				MethodHandles.Lookup lookup = (MethodHandles.Lookup) descriptorArray[LOOKUP];
				// Supplier
				MethodHandle constructor = lookup.findConstructor(clazz, VOID_TYPE);
				try {
					CallSite callSite = LambdaMetafactory.metafactory(
							lookup,
							"get",
							MethodHandleUtils.METHOD_TYPE_SUPPLIER,
							MethodType.methodType(Object.class),
							constructor,
							MethodType.methodType(clazz)
					);
					descriptorArray[CONSTRUCTOR] = (supplier = (Supplier<T>) callSite.getTarget().invokeExact());
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

		/**
		 * exception or error info
		 * @param e cause exception
		 * @param info error info
		 */
		public void error(Throwable e, String info) {
			PropertyException exception;
			if (e == null) {
				exception = new PropertyException(info);
			} else if (info == null) {
				if (e instanceof PropertyException) {
					exception = (PropertyException) e;
				} else {
					exception = new PropertyException(e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e);
				}
			} else {
				exception = new PropertyException(info, e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e);
			}
			if (disableException) {
				exception.printStackTrace();
			} else {
				throw exception;
			}
		}

//	@SuppressWarnings("unchecked")
		public boolean copyOnCondition(Object source, Object target, BiPredicate<PropertyDescriptor, Object> condition) {
			if (source == null || target == null) {
				return false;
			}
			Object[] destDescriptorArray = getDescriptorArray(target.getClass());
			if (destDescriptorArray == null) {
				return false;
			}
			boolean result = false;
			if (source instanceof Map) {
				// 原类型为map
				for (Map.Entry<String, Object> entry : ((Map<String, Object>) source).entrySet()) {
					BiConsumer<Object, Object> setter = getSetter(destDescriptorArray, entry.getKey());
					if (setter == null) {
						// 没有目标setter
						continue;
					}
					PropertyDescriptor targetDescriptor = getDescriptor0(destDescriptorArray, entry.getKey());
					// 获取原属性值
					Object value = entry.getValue();
					// 条件校验
					if (condition != null && !condition.test(targetDescriptor, value)) {
						continue;
					}
					if (invokeWriteMethod(target, setter, targetDescriptor, value)) {
						result = true;
					}
				}
			} else {
				// 原始类型为bean
				Object[] srcDescriptorArray = getDescriptorArray(source.getClass());
				for (PropertyDescriptor sourceDescriptor : getDescriptors0(srcDescriptorArray)) {
					if (sourceDescriptor.getReadMethod() == null) {
						// 没有getter
						continue;
					}
					String name = sourceDescriptor.getName();
					BiConsumer<Object, Object> setter = getSetter(destDescriptorArray, name);
					if (setter == null) {
						// 没有目标setter
						continue;
					}
					PropertyDescriptor targetDescriptor = getDescriptor0(destDescriptorArray, name);
					// 获取原属性值
					Object value = invokeReadMethod(source, getGetter(srcDescriptorArray, name), srcDescriptorArray, name);
					// 条件校验
					if (condition != null && !condition.test(targetDescriptor, value)) {
						continue;
					}
					if (invokeWriteMethod(target, setter, targetDescriptor, value)) {
						result = true;
					}
				}
			}
			return result;
		}

		boolean invokeWriteMethod(Object bean, PropertyDescriptor descriptor, Object value) {
			return invokeWriteMethod(bean, getSetter(getDescriptorArray(bean.getClass()), descriptor.getName()), descriptor, value);
		}
		private boolean invokeWriteMethod(Object bean, BiConsumer<Object, Object> setter, PropertyDescriptor descriptor, Object value) {
			if (setter == null) {
				return false;
			}
			if (typeConverter != null) {
				// 转换类型
				value = typeConverter.convert(value, descriptor.getPropertyType());
			}
			try {
				if (setter != INVALID_SETTER) {
					setter.accept(bean, value);
				} else {
					descriptor.getWriteMethod().invoke(bean, value);
				}
				return true;
			} catch (Throwable e) {
				error(e, "Fail to set the property '" + descriptor.getName() + "' for the bean of the type '"
						+ bean.getClass().getName() + "', reason: " + e.getMessage());
			}
			return false;
		}

		Object invokeReadMethod(Object bean, PropertyDescriptor descriptor) {
			Object[] descriptorArray = getDescriptorArray(bean.getClass());
			return invokeReadMethod(bean, getGetter(descriptorArray, descriptor.getName()), descriptorArray, descriptor.getName());
		}
		private Object invokeReadMethod(Object bean, Function<Object, Object> getter, Object[] descriptorArray, String name) {
			try {
				if (getter != null) {
					if (getter != INVALID_GETTER) {
						return getter.apply(bean);
					} else {
						getDescriptor0(descriptorArray, name).getReadMethod().invoke(bean);
					}
				}
			} catch (Throwable e) {
				error(e, "Fail to get the property '" + name + "' for the bean of the type '"
						+ bean.getClass().getName() + "', reason: " + e.getMessage());
			}
			return null;
		}


		private Function<Object, Object> getGetter(Object[] descriptorArray, String name) {
			return descriptorArray != null ? ((Map<String, Function<Object, Object>>)descriptorArray[GETTER_MAP]).get(name) :  null;
		}
		private BiConsumer<Object, Object> getSetter(Object[] descriptorArray, String name) {
			return descriptorArray != null ? ((Map<String, BiConsumer<Object, Object>>)descriptorArray[SETTER_MAP]).get(name) :  null;
		}
		private PropertyDescriptor getDescriptor0(Object[] descriptorArray, String name) {
			return descriptorArray != null ? ((Map<String, PropertyDescriptor>)descriptorArray[DESCRIPTOR_MAP]).get(name) :  null;
		}
		private PropertyDescriptor[] getDescriptors0(Object[] descriptorArray) {
			return descriptorArray != null ? ((PropertyDescriptor[])descriptorArray[DESCRIPTORS]) :  null;
		}


		/**
		 * 复制原对象中符合条件的属性到目标对象
		 *
		 * @param source    原对象
		 * @param target    目标对象
		 * @param condition 条件
		 * @return 如果所有属性都没进行复制操作则返回false, 否则返回true
		 */
//	@SuppressWarnings("unchecked")
		public boolean copyToMapOnCondition(Object source, Map<String, Object> target, BiPredicate<Entry<String, Object>, Object> condition) {
			if (source == null || target == null) {
				return false;
			}
			boolean result = false;
			if (source instanceof Map) {
				if (condition == null) {
					if (((Map) source).isEmpty()) {
						return false;
					}
					target.putAll((Map<String, Object>) source);
					return true;
				}
				Entry<String, Object> targetEntry = new Entry();
				// 原类型为map
				for (Map.Entry<String, Object> entry : ((Map<String, Object>) source).entrySet()) {
					// 获取原属性值
					String key = entry.getKey();
					targetEntry.setKey(key);
					targetEntry.setValue(target.get(key));
					// 条件校验
					if (condition.test(targetEntry, entry.getValue())) {
						target.put(key, entry.getValue());
						result = true;
					}
				}
			} else {
				// 原始类型为bean
				Entry<String, Object> targetEntry = new Entry();
				Object[] srcDescriptorArray = getDescriptorArray(source.getClass());
				for (PropertyDescriptor sourceDescriptor : getDescriptors0(srcDescriptorArray)) {
					String name = sourceDescriptor.getName();
					if (sourceDescriptor.getReadMethod() == null || "class".equals(name)) {
						// 没有getter
						continue;
					}
					targetEntry.setKey(name);
					targetEntry.setValue(target.get(targetEntry.getKey()));
					// 获取原属性值
					Object value = invokeReadMethod(source, getGetter(srcDescriptorArray, name), srcDescriptorArray, name);
					// 条件校验
					if (condition != null && !condition.test(targetEntry, value)) {
						continue;
					}
					target.put(name, value);
					result = true;
				}
			}
			return result;
		}



}
