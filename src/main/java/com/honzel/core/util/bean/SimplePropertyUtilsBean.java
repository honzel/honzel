package com.honzel.core.util.bean;

import com.honzel.core.constant.ArrayConstants;
import com.honzel.core.util.ConcurrentReferenceHashMap;
import com.honzel.core.util.converter.TypeConverter;
import com.honzel.core.util.exception.PropertyException;
import com.honzel.core.vo.Entry;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiPredicate;

/**
 * 
 * @author honzel
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SimplePropertyUtilsBean {

	private static  SimplePropertyUtilsBean propertyUtilsBean = new SimplePropertyUtilsBean(
		new ConcurrentReferenceHashMap(),
		new TypeConverter()
	);
	private static final int DESCRIPTORS = 0;
	private static final int DESCRIPTOR_MAP = 1;
	
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
	     	if (!mapped && key != null) {
				descriptor = getDescriptor(beanClass, key.toString());
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
	     				descriptor = getDescriptor(beanClass, key.toString());
	     			}
	     			if (descriptor == null) {
	     				throw new PropertyException("The property  '" + key + "' of bean type  '"
	     						+ beanClass.getName() + "' isn't exists.");
	     			}
	     		}
				Method writeMethod = descriptor.getWriteMethod();
	     		if (writeMethod == null) {
					throw new PropertyException("The setter of property  '" + key + "' of bean type  '"
							+ beanClass.getName() + "' isn't exists.");
				}
	     		return invokeWriteMethod(bean, descriptor, value);
	     	} catch (PropertyException e) {
				error(e, null);
	     	} catch (Exception e) {
	     		error(e, "Fail to set the property '" + key + "' for the bean of the type '"
	     				+ beanClass.getName() + "', reason: " + e.toString());
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
    	PropertyDescriptor descriptor = null;
    	if (!mapped && key != null) {
    		descriptor = getDescriptor(bean.getClass(), key.toString());
    	}
    	if (descriptor == null) {
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
    			descriptor = getDescriptor(bean.getClass(), key.toString());
    		}
    		if (descriptor == null)
    			return null;
    	}
		try {
			Method readMethod = descriptor.getReadMethod();
			if (readMethod != null) {
				return readMethod.invoke(bean, (Object[]) null);
			}
		} catch (Exception e) {
			error(e, null);
		}
		return null;
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
		PropertyDescriptor descriptor = getDescriptor(beanClass, name);
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
		PropertyDescriptor[] descriptors0 = this.getPropertyDescriptors(source.getClass());
		if (descriptors0.length == 0) {
			return;
		}
		Class targetType = target.getClass();
		Map descriptors = getDescriptorMap(targetType);
		if (descriptors == null) {
            return;
		}
		for (PropertyDescriptor descriptor0 : descriptors0) {
			if (descriptor0 != null && descriptor0.getReadMethod() != null) {
				PropertyDescriptor descriptor = (PropertyDescriptor) descriptors.get(descriptor0.getName());
				if (descriptor != null && descriptor.getWriteMethod() != null) {
					invokeWriteMethod(target, descriptor, invokeReadMethod(source, descriptor0));
				}
			}
		}
	}

	void copyMapByBean(Map target, Object source) {
		Class sourceType = source.getClass();
		PropertyDescriptor[] descriptors0 = getPropertyDescriptors(sourceType);
		for (PropertyDescriptor descriptor : descriptors0) {
			if (descriptor == null || descriptor.getReadMethod() == null) {
				continue;
			}
			if ("class".equals(descriptor.getName())) {
				continue;
			}
			try {
				Object value = descriptor.getReadMethod().invoke(source, (Object[]) null);
				target.put(descriptor.getName(), value);
			} catch (Exception e) {
				this.error(e, "Fail to get the specified property '" + descriptor.getName() + "' for  the specified bean of the type '" + sourceType.getName() + "', reason: " + e.toString());
			}
		}

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
	public PropertyDescriptor  getDescriptor(Class beanClass, String name) {
    	Map map = getDescriptorMap(beanClass);
		return map == null ? null : (PropertyDescriptor) map.get(name);
    }


	private Object[] getDescriptorArray(Class beanClass) {
		if (beanClass == null) {
			 return null;
       }
		Object[] descriptorArray = (Object[]) getInstance().descriptorsCache.get(beanClass);
       if (descriptorArray  !=  null) {
           return descriptorArray;
       }
       BeanInfo beanInfo;
       try {
           beanInfo = Introspector.getBeanInfo(beanClass);
       } catch (IntrospectionException e) {
           return null;
       }
       PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
       if (descriptors == null) {
       	 return null;
       }
       Map descriptorMap = new HashMap();
       descriptorArray = new Object[2];
       descriptorArray[DESCRIPTOR_MAP] = descriptorMap;
       descriptorArray[DESCRIPTORS] = descriptors;
		for (PropertyDescriptor descriptor : descriptors) {
			if (descriptor != null) {
				descriptorMap.put(descriptor.getName(), descriptor);
				try {
					Method method = descriptor.getReadMethod();
					if (method != null) {
						method.setAccessible(true);
					}
					method = descriptor.getWriteMethod();
					if (method != null) {
						method.setAccessible(true);
					}
				} catch (SecurityException ignored) {}
			}
		}
       getInstance().descriptorsCache.put(beanClass, descriptorArray);
       return descriptorArray;
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
		Map<String, PropertyDescriptor> targetDescriptorMap = getDescriptorMap(target.getClass());
		if (targetDescriptorMap == null) {
			return false;
		}
		boolean result = false;
		if (source instanceof Map) {
			// 原类型为map
			for (Map.Entry<String, Object> entry : ((Map<String, Object>) source).entrySet()) {
				PropertyDescriptor targetDescriptor = targetDescriptorMap.get(entry.getKey());
				if (targetDescriptor == null || targetDescriptor.getWriteMethod() == null) {
					// 没有目标setter
					continue;
				}
				// 获取原属性值
				Object value = entry.getValue();
				// 条件校验
				if (condition != null && !condition.test(targetDescriptor, value)) {
					continue;
				}
				if (invokeWriteMethod(target, targetDescriptor, value)) {
					result = true;
				}
			}
		} else {
			// 原始类型为bean
			PropertyDescriptor[] sourceDescriptors = getPropertyDescriptors(source.getClass());
			for (PropertyDescriptor sourceDescriptor : sourceDescriptors) {
				if (sourceDescriptor.getReadMethod() == null) {
					// 没有getter
					continue;
				}
				PropertyDescriptor targetDescriptor = targetDescriptorMap.get(sourceDescriptor.getName());
				if (targetDescriptor == null || targetDescriptor.getWriteMethod() == null) {
					// 没有目标setter
					continue;
				}
				// 获取原属性值
				Object value = invokeReadMethod(source, sourceDescriptor);
				// 条件校验
				if (condition != null && !condition.test(targetDescriptor, value)) {
					continue;
				}
				if (invokeWriteMethod(target, targetDescriptor, value)) {
					result = true;
				}
			}
		}
		return result;
	}

	private boolean invokeWriteMethod(Object bean, PropertyDescriptor descriptor, Object value) {
		if (typeConverter != null) {
			value = typeConverter.convert(value, descriptor.getPropertyType());
		}
		try {
			descriptor.getWriteMethod().invoke(bean, value);
			return true;
		} catch (Exception e) {
			error(e, "Fail to set the property '" + descriptor.getName() + "' for the bean of the type '"
					+ bean.getClass().getName() + "', reason: " + e.toString());
		}
		return false;
	}
	Object invokeReadMethod(Object bean, PropertyDescriptor descriptor) {
		try {
			return descriptor.getReadMethod().invoke(bean, ArrayConstants.EMPTY_OBJECT_ARRAY);
		} catch (Exception e) {
			error(e, "Fail to get the property '" + descriptor.getName() + "' for the bean of the type '"
					+ bean.getClass().getName() + "', reason: " + e.toString());
		}
		return null;
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
				targetEntry.setKey(entry.getKey());
				targetEntry.setValue(target.get(entry.getKey()));
				// 条件校验
				if (condition.test(targetEntry, entry.getValue())) {
					target.put(entry.getKey(), entry.getValue());
					result = true;
				}
			}
		} else {
			// 原始类型为bean
			Entry<String, Object> targetEntry = new Entry();
			PropertyDescriptor[] sourceDescriptors = getPropertyDescriptors(source.getClass());
			for (PropertyDescriptor sourceDescriptor : sourceDescriptors) {
				if (sourceDescriptor.getReadMethod() == null) {
					// 没有getter
					continue;
				}
				targetEntry.setKey(sourceDescriptor.getName());
				targetEntry.setValue(target.get(targetEntry.getKey()));
				// 获取原属性值
				Object value = invokeReadMethod(source, sourceDescriptor);
				// 条件校验
				if (condition != null && !condition.test(targetEntry, value)) {
					continue;
				}
				target.put(sourceDescriptor.getName(), value);
				result = true;
			}
		}
		return result;
	}
}
