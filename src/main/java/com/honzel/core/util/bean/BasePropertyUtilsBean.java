package com.honzel.core.util.bean;

import com.honzel.core.constant.ArrayConstants;
import com.honzel.core.util.converter.TypeConverter;
import com.honzel.core.util.exception.PropertyException;
import com.honzel.core.util.lambda.LambdaUtils;
import com.honzel.core.util.lambda.MethodHandleUtils;
import com.honzel.core.vo.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * 简单bean工具类
 * @author honzel
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
abstract class BasePropertyUtilsBean<G, S, F> {

	private static final Logger log = LoggerFactory.getLogger(BasePropertyUtilsBean.class);
	private static final Function<AccessibleObject, Boolean> TRY_SET_ACCESSIBLE_FUNCTION;
	static {
		// init functions
		TRY_SET_ACCESSIBLE_FUNCTION = initTrySetAccessibleFunction();
	}
	/**
	 * 获取java9+的trySetAccessible方法
	 * @return 返回trySetAccessible的function
	 */
	@SuppressWarnings("unchecked")
	private static Function<AccessibleObject, Boolean> initTrySetAccessibleFunction() {
		try {
			// 1. 获取 MethodHandles.Lookup 对象
			MethodHandles.Lookup lookup = MethodHandleUtils.lookup(AccessibleObject.class);
			// 获取 MethodHandles.trySetAccessible 的 MethodHandle
			MethodHandle handle = lookup.findVirtual(AccessibleObject.class, "trySetAccessible", MethodType.methodType(boolean.class));
			MethodType targetSignature = MethodType.methodType(Boolean.class, AccessibleObject.class);
			// 3. 创建 CallSite
			CallSite callSite = LambdaMetafactory.metafactory(lookup, "apply", // Function 的抽象方法名
					LambdaUtils.METHOD_TYPE_FUNCTION, // 工厂方法签名
					targetSignature.generic(), // 泛型擦除后的 Function.apply 签名 (Object)Object
					handle, // 目标方法句柄
					targetSignature // 实际方法签名 (AccessibleObject)boolean
			);
			// 4. 获取 Function 实例
			return  (Function<AccessibleObject, Boolean>) callSite.getTarget().invokeExact();
		} catch (Throwable e) {
			log.info("JVM version(less than java9) - AccessibleObject#trySetAccessible method is not exits.");
		}
		return null;
	}
	static final int LOOKUP = 0;
	static final int PROPERTY_MAP = 1;
	static final int CONSTRUCTOR = 2;

	static final int DESCRIPTOR = 0;
	static final int GETTER = 1;
	static final int SETTER = 2;

	private final Map<Class<?>, Object[]> descriptorsCache;

	protected static final TypeConverter typeConverter = new TypeConverter();


	/**
	 * disable exception or not
	 * @param disableException whether or not to disable throw exception
	 */
	public static void setDisableException(boolean disableException) {
		typeConverter.setDisableException(disableException);
	}

	public static boolean isDisableException() {
		return typeConverter.isDisableException();
	}


	public static TypeConverter getTypeConverter() {
		return typeConverter;
	}

	protected BasePropertyUtilsBean(Map<Class<?>, Object[]> descriptorsCache) {
		this.descriptorsCache = descriptorsCache;
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
		Class beanClass = bean.getClass();
		Object[] propertyArray = null;
		if (!mapped && key != null) {
			// get property descriptor
			propertyArray = getPropertyArray(beanClass,  key.toString());
		}
		try {
			if (propertyArray == null) {
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
					// get property descriptor
					propertyArray = getPropertyArray(beanClass,  key.toString());
				}
				if (propertyArray == null) {
					throw new PropertyException("The property  '" + key + "' of bean type  '"
							+ beanClass.getName() + "' isn't exists.");
				}
			}
			S setter = getSetter(propertyArray);
			if (setter == null) {
				throw new PropertyException("The setter of property  '" + key + "' of bean type  '"
						+ beanClass.getName() + "' isn't exists.");
			}
			return invokeWriteMethod(bean, setter, getDescriptor(propertyArray), value);
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
		Object[] propertyArray = null;
		if (!mapped && key != null) {
			// get property descriptor
			propertyArray = getPropertyArray(bean.getClass(), key.toString());
		}
		if (propertyArray == null) {
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
				// get property descriptor
				propertyArray = getPropertyArray(bean.getClass(), key.toString());
			}
			if (propertyArray == null) {
				// No property descriptor
				return null;
			}
		}
		return invokeReadMethod(bean, getGetter(propertyArray), getDescriptor(propertyArray));
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
			value = typeConverter.convert(value, collectionClass.getComponentType());
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
		if (beanClass == null) {
			return null;
		}
		PropertyDescriptor descriptor = getDescriptor(getPropertyArray(beanClass, name));
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

	private void copyBeanToBean(Object source, Object target) {
		Map<String, Object[]> dstPropertyMap = findPropertyMap(target.getClass());
		if (dstPropertyMap.isEmpty()) {
			return;
		}
		Map<String, Object[]> srcPropertyMap = findPropertyMap(source.getClass());
		for (Map.Entry<String, Object[]> entry : dstPropertyMap.entrySet()) {
			Object[] dstPropertyArray = entry.getValue();
			S setter = getSetter(dstPropertyArray);
			if (setter == null) {
				// No setter for the specified property
				continue;
			}
			Object[] srcPropertyArray = srcPropertyMap.get(entry.getKey());
			G getter = getGetter(srcPropertyArray);
			if (getter == null) {
				// No getter for the specified property
				continue;
			}
			Object value = invokeReadMethod(source, getter, getDescriptor(srcPropertyArray));
			invokeWriteMethod(target, setter, getDescriptor(dstPropertyArray), value);
		}
	}

	private void copyBeanToMap(Object source, Map target) {
		Class sourceType = source.getClass();
		Map<String, Object[]> srcPropertyMap = findPropertyMap(sourceType);
		for (Map.Entry<String, Object[]> entry : srcPropertyMap.entrySet()) {
			G getter = getGetter(entry.getValue());
			if (getter == null || "class".equals(entry.getKey())) {
				// No getter for the specified property or "class" property
				continue;
			}
			try {
				target.put(entry.getKey(), invokeReadMethod(source, getter, getDescriptor(entry.getValue())));
			} catch (Exception e) {
				this.error(e, "Fail to get the specified property '" + entry.getKey() + "' for  the specified bean of the type '" + sourceType.getName() + "', reason: " + e);
			}
		}

	}

	private void copyMapToBean(Map source, Object target) {
		if (source.isEmpty()) {
			return;
		}
		Set<Map.Entry> entries = source.entrySet();
		Map<String, Object[]> propertyMap = findPropertyMap(target.getClass());
		for (Map.Entry entry : entries) {
			if (!(entry.getKey() instanceof String)) {
				continue;
			}
			Object[] propertyArray = propertyMap.get(entry.getKey());
			S setter = getSetter(propertyArray);
			if (setter != null) {
				invokeWriteMethod(target, setter, getDescriptor(propertyArray), entry.getValue());
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
	public<T> T copyProperties(Object source, T target) {
		if (source == null || target == null || source == target) {
			return target;
		}
		if (source instanceof Map) {
			if (target instanceof Map) {
				((Map) target).putAll((Map) source);
			} else {
				copyMapToBean((Map) source, target);
			}
		} else {
			if (target instanceof Map) {
				copyBeanToMap(source, (Map) target);
			} else {
				copyBeanToBean(source, target);
			}
		}
		return target;
	}

	/**
	 * This method factory
	 */
	public F getMethodLookup(Class beanClass) {
		return beanClass != null ? (F) findBeanInfoArray(beanClass)[LOOKUP] : null;
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
	public PropertyDescriptor getPropertyDescriptor(Class beanClass, String name) {
		return beanClass != null ? getDescriptor(getPropertyArray(beanClass, name)) : null;
    }

	protected abstract F initMethodLookup(Class<?> beanClass);

	protected abstract G initGetter(PropertyDescriptor descriptor, Class<?> beanClass, F lookup);

	protected abstract S initSetter(PropertyDescriptor descriptor, Class<?> beanClass, F lookup);

	private Object[] initBeanInfoArray(Class<?> beanClass) {
		Object[] beanInfoArray = new Object[3];
		F lookup = initMethodLookup(beanClass);
		beanInfoArray[LOOKUP] = lookup;
		PropertyDescriptor[] descriptors;
		try {
			descriptors = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();
		} catch (IntrospectionException e) {
			log.warn("Fail to get the bean info for the specified bean of the type '{}'", beanClass.getName(), e);
			descriptors = null;
		}
		if (descriptors == null || descriptors.length == 0) { // 没有属性
			beanInfoArray[PROPERTY_MAP] = Collections.emptyMap();
			return beanInfoArray;
		}
		Map<String, Object[]> propertyMap = new LinkedHashMap();
		beanInfoArray[PROPERTY_MAP] = propertyMap;
		for (PropertyDescriptor descriptor : descriptors) {
			String name = descriptor.getName();
			Object[] propertyArray = new Object[3];
			propertyMap.put(name, propertyArray);
			try {
				propertyArray[DESCRIPTOR] = descriptor;
				// init getter
				propertyArray[GETTER] = initGetter(descriptor, beanClass, lookup);
				// init setter
				propertyArray[SETTER] = initSetter(descriptor, beanClass, lookup);
			} catch (Exception e) {
				log.warn("Failed to get the specified property '{}' for  the specified bean type '{}', reason: {}", name, beanClass.getName(), e.getMessage(), e);
			}
		}
		return beanInfoArray;
	}

	public boolean containsClass(Class beanClass) {
		return descriptorsCache.containsKey(beanClass);
	}

	Object[] findBeanInfoArray(Class beanClass) {
		return descriptorsCache.computeIfAbsent(beanClass, this::initBeanInfoArray);
	}

	/**
     * <p>Retrieve the property descriptors for the specified class,
     * introspecting and caching them the first time a particular bean class
     * is encountered.</p>
     *
     * @param beanClass Bean class for which property descriptors are requested
	 * @return the property descriptors for the specified class
	 */
	Map<String, Object[]> findPropertyMap(Class beanClass) {
		return (Map<String, Object[]>) findBeanInfoArray(beanClass)[PROPERTY_MAP];
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
		return beanClass != null ? findPropertyMap(beanClass).values().stream().map(a -> (PropertyDescriptor)a[DESCRIPTOR]).toArray(PropertyDescriptor[]::new) : ArrayConstants.EMPTY_DESCRIPTOR_ARRAY;
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
	public abstract <T> T newInstance(Class<T> beanClass) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

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
		if (isDisableException()) {
			log.warn(exception.getMessage(), e);
		} else {
			throw exception;
		}
	}


	//	@SuppressWarnings("unchecked")
	public boolean copyOnCondition(Object source, Object target, BiPredicate<PropertyDescriptor, Object> condition) {
		if (source == null || target == null) {
			return false;
		}
		Map<String, Object[]> dstPropertyMap = findPropertyMap(target.getClass());
		boolean result = false;
		if (source instanceof Map) {
			// 原类型为map
			for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) source).entrySet()) {
				Object name = entry.getKey();
				if (!(name instanceof String)) {
					continue;
				}
				Object[] propertyArray = dstPropertyMap.get(name);
				S setter = getSetter(propertyArray);
				if (setter == null) {
					// 没有目标setter
					continue;
				}
				PropertyDescriptor targetDescriptor = getDescriptor(propertyArray);
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
			Map<String, Object[]> srcPropertyMap = findPropertyMap(source.getClass());
			for (Map.Entry<String, Object[]> entry : dstPropertyMap.entrySet()) {
				S setter = getSetter(entry.getValue());
				if (setter == null) {
					// No setter
					continue;
				}
				Object[] srcPropertyArray = srcPropertyMap.get(entry.getKey());
				G getter = getGetter(srcPropertyArray);
				if (getter == null) {
					// No getter
					continue;
				}
				PropertyDescriptor targetDescriptor = getDescriptor(entry.getValue());
				// 获取原属性值
				Object value = invokeReadMethod(source, getter, getDescriptor(srcPropertyArray));
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

	protected abstract boolean invokeWriteMethod(Object bean, S setter, PropertyDescriptor descriptor, Object value);

	boolean invokeWriteMethod(Object bean, Object[] propertyArray, Object value) {
		return invokeWriteMethod(bean, getSetter(propertyArray), getDescriptor(propertyArray), value);
	}

	protected abstract Object invokeReadMethod(Object bean, PropertyDescriptor descriptor);

	protected abstract  Object invokeReadMethod(Object bean, G getter, PropertyDescriptor descriptor);


	Object[] getPropertyArray(Class<?> beanClass, String name) {
		return ((Map<String, Object[]>) findBeanInfoArray(beanClass)[PROPERTY_MAP]).get(name);
	}

	PropertyDescriptor getDescriptor(Object[] propertyArray) {
		return propertyArray != null ? (PropertyDescriptor)propertyArray[DESCRIPTOR] : null;
	}

	protected abstract G getGetter(Object[] propertyArray);

	protected abstract S getSetter(Object[] propertyArray);

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
			Entry<String, Object> targetEntry = new Entry<>();
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
			Map<String, Object[]> srcPropertyMap = findPropertyMap(source.getClass());
			for (Map.Entry<String, Object[]> entry : srcPropertyMap.entrySet()) {
				G getter = getGetter(entry.getValue());
				String name = entry.getKey();
				if (getter == null || "class".equals(name)) {
					// 没有getter
					continue;
				}
				targetEntry.setKey(name);
				targetEntry.setValue(target.get(targetEntry.getKey()));
				// 获取原属性值
				Object value = invokeReadMethod(source, getter, getDescriptor(entry.getValue()));
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

	public static boolean trySetAccessible(AccessibleObject accessible) {
		if (TRY_SET_ACCESSIBLE_FUNCTION != null) {
			return TRY_SET_ACCESSIBLE_FUNCTION.apply(accessible);
		}
		// java8
		accessible.setAccessible(true);
		return true;
	}

}
