package com.honzel.core.util.bean;

import com.honzel.core.util.lambda.LambdaUtils;
import com.honzel.core.util.resolver.ResolverUtils;
import com.honzel.core.util.converter.AbstractConverter;
import com.honzel.core.util.converter.Converter;
import com.honzel.core.util.converter.TypeConverter;
import com.honzel.core.util.resolver.Resolver;

import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * 嵌套bean工具类
 * @author honzel
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class NestedPropertyUtilsBean {

	private static final NestedPropertyUtilsBean simpleNestedPropertyUtilsBean = new NestedPropertyUtilsBean(SimplePropertyUtilsBean.getInstance());
	private static final NestedPropertyUtilsBean lambdaNestedPropertyUtilsBean = new NestedPropertyUtilsBean(LambdaPropertyUtilsBean.getInstance());


	private static final String opened = "[.";
	private static final String closed = "]";

	private static final int ERROR_TYPES = Resolver.LINK | Resolver.END;


	private final BasePropertyUtilsBean propertyUtilsBean;

	private NestedPropertyUtilsBean(BasePropertyUtilsBean propertyUtilsBean) {
		this.propertyUtilsBean = propertyUtilsBean;
	}


	public static NestedPropertyUtilsBean getInstance() {
		return simpleNestedPropertyUtilsBean;
	}
	public static NestedPropertyUtilsBean getLambdaInstance() {
		return lambdaNestedPropertyUtilsBean;
	}
	public static NestedPropertyUtilsBean getInstance(Class<?> beanClass) {
		if (lambdaNestedPropertyUtilsBean.propertyUtilsBean.containsClass(beanClass)) {
			return lambdaNestedPropertyUtilsBean;
		}
		return simpleNestedPropertyUtilsBean;
	}



	/**
	 * disable exception or not
	 * @param disableException whether or not to disable throw exception.
	 */
	public void setDisableException(boolean disableException) {
		BasePropertyUtilsBean.setDisableException(disableException);
	}

	public boolean isDisableException() {
		return BasePropertyUtilsBean.isDisableException();
	}

	/**
     * Register a custom {@link Converter} for the specified destination
     * <code>Class</code>, replacing any previously registered Converter.
     * overrides the converter enable exception set by the global one.
     * @param toType Destination class for conversions performed by this
     *  Converter
     * @param converter Converter to be registered
     */

	public void  registerConverter(Class toType, Converter converter) {
		registerConverter(toType, converter, true);
	}

	/**
	 * Returns the {@link TypeConverter}
	 * @return returns the type converter of this util
	 */
	public TypeConverter getTypeConverter() {
		return BasePropertyUtilsBean.getTypeConverter();
	}

	/**
     * Register a custom {@link Converter} for the specified destination
     * <code>Class</code>, replacing any previously registered Converter.
     *
     * @param toType Destination class for conversions performed by this
     *  Converter
     * @param converter Converter to be registered
	 * @param overriddenByGlobalEnableException whether or not to  override the converter enable exception set.
	 */
	public void  registerConverter(Class toType, Converter converter, boolean overriddenByGlobalEnableException) {
		getTypeConverter().register(toType, converter);
		if (overriddenByGlobalEnableException) {
			while (converter instanceof AbstractConverter) {
				((AbstractConverter) converter).setDisableException(isDisableException());
				converter = ( (AbstractConverter) converter).getDefaultConverter();
			}
		}
	}

	/**
     * Look up and return any registered {@link Converter} for the specified
     * destination class; if there is no registered Converter, return
     * <code>null</code>.
     *
     * @param toType Class for which to return a registered Converter
	 * @return returns the converter for the specified type
     */
	public Converter lookup(Class toType) {
		return getTypeConverter().lookup(toType);
	}

	/**
	 * <p>Convert the specified value to an object of the specified class (if
	 * possible).  Otherwise, return default value(usually <code>null</code>) which defined in the converter.</p>
	 *
	 * @param value Value to be converted (may be null)
	 * @param toType Java class to be converted to
	 * @param <E> the target type
	 * @return returns the converted value
	 */
	public  <E> E convert(Object value, Class<E> toType) {
		return (E) getTypeConverter().convert(value, toType);
	}

	/**
     * <p>Retrieve the property descriptors for the specified bean class,
     * 	introspecting and caching them the first time a particular bean class
     *	 is encountered.</p>
     * @param beanClass Bean class for which property descriptors are requested
	 * @return the property descriptors
	 */
	public Map<String, PropertyDescriptor> getPropertyDescriptorMap(Class beanClass) {
		if (beanClass == null) {
			return null;
		}
		Map<String, Object[]> descriptorMap = propertyUtilsBean.findPropertyMap(beanClass);
		if (descriptorMap.isEmpty()) {
			return Collections.emptyMap();
		}
		return descriptorMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> propertyUtilsBean.getDescriptor(entry.getValue())));
	}

	/**
	 * <p>Retrieve the property descriptors for the specified bean class,
	 * 	introspecting and caching them the first time a particular bean class
	 *	 is encountered.</p>
	 * @param beanClass Bean class for which property descriptors are requested
	 * @return the property descriptors
	 */
	public PropertyDescriptor[] getPropertyDescriptors(Class beanClass) {
		return propertyUtilsBean.getPropertyDescriptors(beanClass);
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
		return getPropertyDescriptor(beanClass, name, true);
	}

	/**
	 * <p>Retrieve the property descriptor for the specified property of the
     * specified bean, or return <code>null</code> if there is no such
     * descriptor. </p>
     * @param bean Bean  for which a property descriptor is requested
     * @param name Possibly indexed and/or nested name of the property for
     *  which a property descriptor is requested
	 * @return the property descriptor for the specified property of the
     * specified bean, or return <code>null</code>
	 */
	public PropertyDescriptor getPropertyDescriptor(Object bean, String name) {
		return getPropertyDescriptor(bean, name, false);
	}


	/**
     * Return the Java Class representing the property type of the specified
     * property, or <code>null</code> if there is no such property for the
     * specified bean.  This method follows the same name resolution rules
     * used by <code>getPropertyDescriptor()</code>, so if the last element
     *  has no property with the
     * specified name, <code>null</code> is returned.
     *
     * @param bean Bean for which a property descriptor is requested
     * @param name Possibly indexed and/or nested name of the property for
     *  which a property descriptor is requested
	 * @return returns the Java Class representing the property type of the specified  property
	 */
	public Class getPropertyType(Object bean, String name) {
		return getPropertyType(bean, name, false);
	}

   /**
    * Return the Java Class representing the property type of the specified
    * property, or <code>null</code> if there is no such property for the
    * specified bean.  This method follows the same name resolution rules
    * used by <code>getPropertyDescriptor()</code>, so if the last element
    *  has no property with the
    * specified name, <code>null</code> is returned.
    *
    * @param beanClass Bean class for which a property descriptor is requested
    * @param name Possibly indexed and/or nested name of the property for
    *  which a property descriptor is requested
    * @return returns the Java Class representing the property type of the specified  property
    */
   public  Class getPropertyType(Class beanClass, String name) {
	   return getPropertyType(beanClass, name, true);
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
		return (T) propertyUtilsBean.copyProperties(source, target);
	}

	private  PropertyDescriptor getPropertyDescriptor(Object bean, String name, boolean classInstance) {
		if(bean == null || name == null) {
			return null;
		}
		name = name.trim();
		int pos = nestedPos(name);
		Class<?> beanClass = classInstance ?  (Class<?>) bean : bean.getClass();
		if(pos < 0) {
			return propertyUtilsBean.getPropertyDescriptor(beanClass, name);
		}
		Resolver resolver = getResolver(name, pos);
		while (resolver.hasNext() && !resolver.isLast()) {
			if (resolver.getType() == Resolver.LINK) {
				if (resolver.isEmpty())
					continue;
				error(null, "The property expression '" + name + "' is invalid .");
				return null;
			}
		}
		if (resolver.isInTypes(ERROR_TYPES)) {
			error(null, "The property expression '" + name + "' is invalid .");
			return null;
		} else if (resolver.getType() != Resolver.START) {
			name = name.substring(0, resolver.getStart(false) - 1);
			if(classInstance) {
				beanClass = getPropertyType(bean, name, true);
			} else {
				Object prop = getProperty(bean, name);
				if(prop == null) {
					beanClass = getPropertyType(bean, name, false);
				} else {
					beanClass = prop.getClass();
				}
			}
		}
		if (beanClass != null) {
			Object key = findKey(resolver, bean);
			if (key != null) {
				return propertyUtilsBean.getPropertyDescriptor(beanClass, key.toString());
			}
		}
		return null;
	}
	public <T> T newInstance(Class<T> beanClass) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		return (T) propertyUtilsBean.newInstance(beanClass);
	}

	/**
     * Return the Java Class representing the property type of the specified
     * property, or <code>null</code> if there is no such property for the
     * specified bean.  This method follows the same name resolution rules
     * used by <code>getPropertyDescriptor()</code>, so if the last element
     *  has no property with the
     * specified name, <code>null</code> is returned.
     *
     * @param bean Bean (or bean class) for which a property descriptor is requested
     * @param name Possibly indexed and/or nested name of the property for
     *  which a property descriptor is requested
    * @param classIntance whether it is  class instance or not.
    * @return
    */
	Class<?> getPropertyType(Object bean, String name, boolean classIntance) {
		if(bean == null)
			return null;
		Class<?> beanClass;
		if(classIntance) {
			beanClass = (Class<?>) bean;
		} else {
			beanClass = bean.getClass();
		}
		if(name != null) {
			name = name.trim();
		}
		int pos = nestedPos(name);
		try {
			if(pos < 0) {
				return propertyUtilsBean.getPropertyType(beanClass, name);
			}
			Object root = bean;
			Class<?> parentClass = null;
			Object pKey = null;
			if(pos > 0) {
				String key = name.substring(0, pos).trim();
				pKey = key;
				parentClass = beanClass;
				if(!classIntance) {
					Object prop = propertyUtilsBean.getProperty(bean, key, false);
					if(prop != null) {
						bean = prop;
						beanClass = prop.getClass();
					} else {
						classIntance = true;
					}
				}
				if(classIntance) {
					beanClass = propertyUtilsBean.getPropertyType(beanClass, key);
					if(beanClass == null) {
						return null;
					}
				}
			}
			Resolver resolver = getResolver(name, pos);
			while (resolver.hasNext()) {
				if (pos > 0 && resolver.getType() == Resolver.START)
					continue;
				if (resolver.getType() == Resolver.LINK) {
					if (resolver.isEmpty())
						continue;
					error(null, "The property expression '" + name + "' is invalid .");
					return null;
				}
				boolean mapped = resolver.isPair();
				Object key = findKey(resolver, root);
				if (!classIntance &&  !resolver.isLast()) {
					Object prop = propertyUtilsBean.getProperty(bean, key, mapped);
					if (null == prop) {
						classIntance = true;
					} else {
						pKey = key;
						parentClass = beanClass;
						bean = prop;
						beanClass = bean.getClass();
						continue;
					}
				}
				PropertyDescriptor descriptor = null;
				Class<?> propClass;
				if(beanClass.isArray()) { // array type
					int ind = propertyUtilsBean.findIndex(key);
					if(ind < 0) return null;
					propClass = beanClass.getComponentType();
				} else //map type
					if(Map.class.isAssignableFrom(beanClass)) {
						if(!mapped) {
							descriptor = propertyUtilsBean.getPropertyDescriptor(beanClass, (String) key);
						}
						if(descriptor != null) {
							propClass = descriptor.getPropertyType();
						} else
							propClass = getItemClass(parentClass, pKey);
					} else //collection type
						if(Iterable.class.isAssignableFrom(beanClass)) {
							if(!mapped) {
								descriptor = propertyUtilsBean.getPropertyDescriptor(beanClass, (String) key);
							}
							if(descriptor != null) {
								propClass = descriptor.getPropertyType();
							} else {
								int ind = propertyUtilsBean.findIndex(key);
								if(ind < 0) return null;
								propClass = getItemClass(parentClass, pKey);
							}
						} else { //common type
							String pName = null;
							if(key != null) {
								pName = key.toString();
							}
							descriptor = propertyUtilsBean.getPropertyDescriptor(beanClass, pName);
							if(descriptor == null) {
								return null;
							}
							propClass = descriptor.getPropertyType();
						}
				if(propClass == null) {
					return null;
				}
				pKey = key;
				parentClass = beanClass;
				beanClass = propClass;
			}
			return beanClass;
		} catch (Exception e) {
			error(e, null);
		}
		return null;
	}

	private Class<?> getItemClass(Class<?> beanClass, Object property) {
		if (beanClass == null || property == null) {
			return Object.class;
		} else {
			PropertyDescriptor descriptor = propertyUtilsBean.getPropertyDescriptor(beanClass, property.toString());
			return (descriptor != null) ? getItemClass(descriptor) : Object.class;
		}
	}

	/**
     * Return the value of the specified property of the specified bean,
     * no matter which property reference format is used, with no
     * type conversions.
     *
     * @param bean Bean whose property is to be extracted
     * @param name Possibly indexed and/or nested name of the property
     *  to be extracted, such as user.roles[1]
     * @return Returns the value of the specified property of the specified bean
     */
	public Object getSimpleProperty(Object bean , String name) {
		return propertyUtilsBean.getProperty(bean, name, false);
	}

	/**
     * Return the value of the specified property of the specified bean,
     * no matter which property reference format is used, with no
     * type conversions.
     *
     * @param bean Bean whose property is to be extracted
     * @param name Possibly indexed and/or nested name of the property
     *  to be extracted, such as user.roles[1]
     * @return Returns the value of the specified property of the specified bean
     */
	public  Object getProperty(Object bean , String name) {
		if (bean == null) {
			return null;
		}
		if(name != null) {
			name = name.trim();
		}
		int pos = nestedPos(name);
		if(pos < 0) {
			return propertyUtilsBean.getProperty(bean, name, false);
		}
		Object root = bean;
		if(pos > 0) {
			if (bean instanceof Map && ((Map<?, ?>) bean).containsKey(name)) {
				// 如果是map
				return ((Map) bean).get(name);
			}
			String key = name.substring(0, pos).trim();
			if ((bean = propertyUtilsBean.getProperty(bean, key, false)) == null) {
				return null;
			}
		}
		Resolver resolver = getResolver(name, pos);
		while (resolver.hasNext()) {
			if (pos > 0 && resolver.getType() == Resolver.START)
				continue;
			if (resolver.getType() == Resolver.LINK) {
				if (resolver.isEmpty())
					continue;
				error(null, "The property expression '" + name + "' is invalid .");
				return null;
			}
			bean = propertyUtilsBean.getProperty(
					bean, findKey(resolver, root), resolver.isPair());
			if (bean == null) {
				return null;
			}
		}
		return bean;
	}


	/**
	 * <p>Set the specified property value, performing type conversions as
	 * required to conform to the type of the destination property.</p>
	 *
	 * @param bean Bean on which setting is to be performed
	 * @param name Property name (can be nested/indexed/mapped/combo)
	 * @param value Value to be set
	 * @return Return true when success to set,otherwise return false
	 */

	public boolean setSimpleProperty(Object bean, String name, Object value) {
		return propertyUtilsBean.setProperty(bean, name, value, false);
	}

	public boolean copyToMapOnCondition(Object source, Map<String, Object> target, BiPredicate<String, Object> condition) {
		return propertyUtilsBean.copyToMapOnCondition(source, target, condition);
	}
	public boolean copyToMapOnCondition(Object source, Object target, LambdaUtils.TiPredicate<String, Object, Object> condition) {
		return propertyUtilsBean.copyToMapOnCondition(source, (Map) target, condition);
	}

	public boolean copyToBeanOnCondition(Object source, Object target, BiPredicate<String, Object> condition) {
		return propertyUtilsBean.copyToBeanOnCondition(source, target, condition);
	}

	public boolean copyToBeanOnCondition(Object source, Object target, LambdaUtils.TiPredicate<String, Object, Object> condition) {
		return propertyUtilsBean.copyToBeanOnCondition(source, target, condition);
	}

	/**
     * <p>Set the specified property value, performing type conversions as
     * required to conform to the type of the destination property.</p>
     *
     * @param bean Bean on which setting is to be performed
     * @param name Property name (can be nested/indexed/mapped/combo)
     * @param value Value to be set
     * @return Return true when success to set,otherwise return false
     */

	public boolean setProperty(Object bean, String name, Object value) {
		if (bean == null) {
			error(null, "Bean is missing");
			return false;
		}
		if(nestedPos(name) < 0) {
			if(name != null) {
				name = name.trim();
			}
			return propertyUtilsBean.setProperty(bean, name, value, false);
		}
		Object root = bean;
		Resolver resolver = getResolver(name, 0);
		Object splitBean = null;
		Object splitProp = null;
		Object splitKey = null;
		boolean splitMapped = false;
		boolean split = false;
		try {
			boolean result = false;
			while (resolver.hasNext() && !resolver.isLast()) {
				if (resolver.getType() == Resolver.LINK) {
					if (resolver.isEmpty())
						continue;
					throw new IllegalArgumentException("The property expression '" + name + "' is invalid .");
				}
				boolean mapped = resolver.isPair();
				Object key = findKey(resolver, root);
				Object prop = propertyUtilsBean.getProperty(bean, key, mapped);
				Class propClass = null;
				Object[] propertyArray = null;
				if (prop != null) {
					propClass = prop.getClass();
				} else {
					if (!mapped) {
						propertyArray = propertyUtilsBean.getPropertyArray(bean.getClass(), key.toString());
					}
					if (propertyArray == null) {
						if(bean.getClass().isArray()) {
							mapped = true;
							propClass = bean.getClass().getComponentType();
						} else if (mapped) {
							mapped = false;
							propertyArray = propertyUtilsBean.getPropertyArray(bean.getClass(), name);
						}
					}
					if (propClass == null && propertyArray != null) {
						propClass = propertyUtilsBean.getDescriptor(propertyArray).getPropertyType();
					}
					if (propClass == null) {
						throw new IllegalArgumentException("Can not be introspected the property type of  the property '"
								+ key + "' for the specified bean of the type '" + getTypeName(bean) + "'.");
					}
				}
				if (propClass.isArray() || Iterable.class.isAssignableFrom(propClass)
						|| Map.class.isAssignableFrom(propClass)) {
					resolver.hasNext();
					if (!resolver.isLast() && resolver.getType() == Resolver.LINK && resolver.isEmpty())
						resolver.hasNext();
					if (resolver.getType() == Resolver.LINK)
						throw new IllegalArgumentException("The property expression '" + name + "' is invalid .");
					boolean isNew = false;
					Object itemKey = findKey(resolver, root);
					boolean itemMapped = resolver.isPair();
					Class itemClass = null;
					if (prop == null) { // the property is null and create one.
						isNew = true;
						if (propClass.isArray()) {
							int ind = propertyUtilsBean.findIndex(itemKey);
							if (itemMapped && ind < 0) {
								throw new ArrayIndexOutOfBoundsException(ind);
							}
							itemClass = propClass.getComponentType();
							prop = Array.newInstance(itemClass, ind + 1);
						} else if (propClass.isAssignableFrom(ArrayList.class)) {
							prop = new ArrayList();
						} else if (propClass.isAssignableFrom(HashMap.class)) {
							prop = new HashMap();
						} else if (propClass.isAssignableFrom(LinkedHashSet.class)) {
							prop = new LinkedHashSet();
						} else {
							prop = propertyUtilsBean.newInstance(propClass);
						}
					} else //array object
						if (propClass.isArray()) {
							int ind = propertyUtilsBean.findIndex(itemKey);
							if (itemMapped && ind < 0) {
								throw new ArrayIndexOutOfBoundsException(ind);
							}
							int len = Array.getLength(prop);
							itemClass = propClass.getComponentType();
							if (ind >= len) {
								Object arr = Array.newInstance(itemClass, ind + 1);
								System.arraycopy(prop, 0, arr, 0, len);
								prop = arr;
								isNew = true;
							}
						}
					if (resolver.isLast()) {
						if (!propertyUtilsBean.setProperty(prop, itemKey, value, itemMapped)) {
							return false;
						}
						if (isNew && !propertyUtilsBean.setProperty(bean, key, prop, mapped)) {
							return false;
						}
						if (split) {
							result = true;
							break;
						} else {
							return true;
						}
					}
					Object item = null;
					if (!isNew) {
						item = propertyUtilsBean.getProperty(prop, itemKey, itemMapped);
					}
					if (item == null) {
						if (itemClass == null) {
							if (propertyArray == null) {
								propertyArray = propertyUtilsBean.getPropertyArray(bean.getClass(), key.toString());
							}
							itemClass = getItemClass(propertyUtilsBean.getDescriptor(propertyArray));
						}
						if (itemClass.isArray()) {
							item = Array.newInstance(itemClass, 0);
						} else {
							item = propertyUtilsBean.newInstance(itemClass);
						}
						if (split) {
							if(!propertyUtilsBean.setProperty(prop, itemKey, item, itemMapped)) {
								return false;
							}
							if(isNew && !propertyUtilsBean.setProperty(bean, key, prop, mapped)) {
								return false;
							}
						} else if (isNew) {
							if(!propertyUtilsBean.setProperty(prop, itemKey, item, itemMapped)) {
								return false;
							}
							splitMapped = mapped;
							splitBean = bean;
							splitProp = prop;
							splitKey = key;
							split = true;
						} else {
							splitMapped = itemMapped;
							splitBean = prop;
							splitProp = item;
							splitKey = itemKey;
							split = true;
						}
					}
					prop = item;
				} else // if the usual property is null
					if (prop == null) {
						prop = propertyUtilsBean.newInstance(propClass);
						if (split) {
							if (!propertyUtilsBean.invokeWriteMethod(bean, propertyArray, prop)) {
								// invoke write method failed
								return false;
							}
						} else {
							splitBean = bean;
							splitProp = prop;
							splitKey = key;
							splitMapped = mapped;
							split = true;
						}
					}
				bean = prop;
			}
			if (resolver.isInTypes(ERROR_TYPES)) {
				throw new IllegalArgumentException(
						"The property expression '" + name + "' is invalid .");
			}
			boolean mapped = resolver.isPair();
			if (result || propertyUtilsBean.setProperty(bean, findKey(resolver, root), value, mapped)) {
				//set the nested property
				return !split || propertyUtilsBean.setProperty(splitBean, splitKey, splitProp, splitMapped);
			}
		} catch (Throwable t) {
			error(t, "Fail to set the specified property '" + name + "' for the specified bean of the type '"
					+ getTypeName(root) + "', reason: " + t);
		}
		return false;
	}


	public int nestedPos(String name) {
		if (name == null) {
			return -1;
		}
		int end = name.indexOf('\\');
		int minPos = -1;
		for (int i = 0, len = opened.length(); i < len; ++ i) {
			int pos = name.indexOf(opened.charAt(i));
			if (pos >= 0 && (minPos < 0 || minPos > pos)) {
				minPos = pos;
			}
		}
		if (end < 0 || minPos < end)
			return minPos;
		return 0;
	}

	private   Resolver getResolver(String name, int pos) {
		return ResolverUtils.createResolver(opened, closed, true, '\\').reset(name, pos);
	}

	/**
     * <p>Return an element type for  this accessible indexed property,
     * if there is one then return; otherwise return <code>null</code>.</p>
	 * @param descriptor
	 * @return
	 */
	private Class getItemClass(PropertyDescriptor descriptor) {
		Class itemClass = null;
		if(descriptor != null) {
			Type tp = null;
			try {
				Method method = descriptor.getReadMethod();
				if(method != null) {
					tp = method.getGenericReturnType();
					if(!(tp instanceof ParameterizedType)) {
						method = descriptor.getWriteMethod();
						if(method != null) {
							tp = method.getGenericParameterTypes()[0];
						}
					}
				} else {
					method = descriptor.getWriteMethod();
					if(method != null) {
						tp = method.getGenericParameterTypes()[0];
					}
				}

				if(tp instanceof ParameterizedType) {
					Type[] type = ((ParameterizedType)tp).getActualTypeArguments();
					tp = type[type.length - 1];
					if(tp instanceof Class) {
						itemClass = (Class)tp;
					} else if(tp instanceof ParameterizedType) {
						itemClass = (Class)((ParameterizedType)tp).getRawType();
					}
				} else if(tp instanceof Class) {
					if(((Class)tp).isArray()) {
						itemClass = ((Class)tp).getComponentType();
					} else {
						itemClass = Object.class;
					}
				}
			} catch (Exception e) {}
		}
		return itemClass;
	}

	/**
     *
     * @param resolver
     * @param root
     * @return
     */
	private Object findKey(Resolver resolver, Object root) {
		if (resolver.isInTypes(ERROR_TYPES))
			return null;
		if (!resolver.isPair()) {
			return resolver.next();
		}
		CharSequence input = resolver.getInput();
		int start = resolver.getStart();
		int end = resolver.getEnd();
		if (end > start + 1) {
			char first = input.charAt(start);
			char last = input.charAt(end - 1);
			if (first == last && (first == '\'' || first == '"')) {
				return resolver.next(1, -1);
			}
		}

		String expr = resolver.next(false);
		int baseType = this.checkBasic(expr);
		if(baseType < 0) {
			return getProperty(root, expr);
		}
		if(baseType == 0) {//null
			return null;
		}
		if(baseType == 1) {//true/false
			return Boolean.valueOf(expr);
		}

		int ch = expr.charAt(expr.length() - 1);
		if (ch < '0' || ch > '9') {
			expr = expr.substring(0, expr.length() - 1);
		}

		Object key = null;

		try {
			switch(baseType) {
				case 2:
					key = Byte.valueOf(expr);
					break;
				case 3 :
					key = Short.valueOf(expr);
					break;
				case 4 :
					key = Integer.valueOf(expr);
					break;
				case 5 :
					key = Long.valueOf(expr);
					break;
				case 6 :
					key = new BigInteger(expr);
					break;
				case 7 :
					key = Float.valueOf(expr);
					break;
				case 8 :
					key = Double.valueOf(expr);
					break;
				case 9 :
					key = new BigDecimal(expr);
					break;
			}
		} catch (Exception e) {
			key = getProperty(root, expr);
		}
		return key;
	}

	/**
	 * check numeric type　2-byte, 3-short, 4-int, 5-long, 6-biginteger, 7-float, 8-double
	 * @param value String object
	 * @return
	 */
	private int checkBasic(String value) {
		if(value == null || value.length() == 0) {
			return -1;
		}
		if("null".equals(value)) {
			return 0;
		}
		if("false".equals(value) || "true".equals(value)) {
			return 1;
		}
		int end = value.length();
		int start = 0;
		char ch = value.charAt(end - 1);
		int type = "BbSsIiLlHhFfDd".indexOf(ch);
		if(type > 0) {
			type = type/2 + 2;
			end--;
		}
		ch = value.charAt(0);
		if(ch == '+' || ch == '-') {
			start++;
		}
		if(end - start <= 0) return -1;
		ch = value.charAt(start);
		if(ch == '.' || value.charAt(end - 1) == '.') return -1;
		if(ch == '0' && end - start > 1 && value.charAt(start + 1) != '.') {
			return -1;
		}
		boolean fraction = false;
		for(int i = start; i < end; i++) {
			switch(value.charAt(i)) {
				case '.' :
					if(fraction) {
						return -1;
					}
					fraction = true;
					break;
				case '0' :
				case '1' :
				case '2' :
				case '3' :
				case '4' :
				case '5' :
				case '6' :
				case '7' :
				case '8' :
				case '9' :
					break;
				default :
					return -1;
			}
		}
		if(fraction) {
			if(type == -1) {
				type = 8; //double
			} else if(type < 7) {
				return -1;
			}
		} else {
			if(type == -1) {
				type = (end - start < 10) ? 4 : (end - start < 19) ? 5 : 6;
			}
		}
		return type;
	}

	/**
	 * exception or error info
	 * @param e cause exception
	 * @param info error info
	 */
	public void error(Throwable e, String info) {
		propertyUtilsBean.error(e, info);
	}

	private String getTypeName(Object bean) {
		return bean == null ? "null" : bean.getClass().getName();
	}
}
