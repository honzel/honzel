package com.honzel.core.util.bean;

import com.honzel.core.constant.ArrayConstants;
import com.honzel.core.util.converter.Converter;
import com.honzel.core.util.converter.TypeConverter;
import com.honzel.core.util.lambda.LambdaUtils;
import com.honzel.core.util.lambda.MethodHandleUtils;
import com.honzel.core.util.text.TextUtils;

import java.beans.PropertyDescriptor;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utility methods for using Java Reflection APIs to facilitate generic
 * property getter and setter operations on Java objects. 
 * @author honzel
 *
 */
@SuppressWarnings({"rawtypes" })
public class BeanHelper {

    protected BeanHelper() {
    }

	private static final String[] GETTER_SETTER_PREFIXES = {"get", "set", "is"};
    /**
	 * disable exception or not
	 * @param disableException  disable exception or not
	 */
	public static void setDisableException(boolean disableException) {
		NestedPropertyUtilsBean.getInstance().setDisableException(disableException);
	}

	public static boolean isDisableException() {
		return NestedPropertyUtilsBean.getInstance().isDisableException();
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
	public static void  registerConverter(Class toType, Converter converter, boolean overriddenByGlobalEnableException) {
		NestedPropertyUtilsBean.getInstance().registerConverter(toType, converter, overriddenByGlobalEnableException);
	}

	/**
     * Register a custom {@link Converter} for the specified destination
     * <code>Class</code>, replacing any previously registered Converter.
     * overrides the converter enable exception set by the global one.
     * @param toType Destination class for conversions performed by this
     *  Converter
     * @param converter Converter to be registered
     */
	public static void  registerConverter(Class toType, Converter converter) {
		NestedPropertyUtilsBean.getInstance().registerConverter(toType, converter);
	}

	/**
	 * Returns the {@link TypeConverter}
	 * @return the <code>TypeConverter</code> instance.
	 */
	public static TypeConverter getTypeConverter() {
		return NestedPropertyUtilsBean.getInstance().getTypeConverter();
	}

	/**
	 * Look up and return any registered {@link Converter} for the specified
	 * destination type; if there is no registered Converter, return
	 * <code>null</code>.
	 *
	 * @param toType Class for which to return a registered Converter
	 * @return the registered <code>Converter</code> for the specified type
	 */
	public static Converter lookupConverter(Class toType) {
		return NestedPropertyUtilsBean.getInstance().lookup(toType);
	}

	/**
	 * <p>Convert the specified value to an object of the specified class (if
	 * possible).  Otherwise, return default value(usually <code>null</code>) which defined in the converter.</p>
	 *
	 * @param value Value to be converted (may be null)
	 * @param toType Java class to be converted to
	 * @param <E> the target type
	 * @return the converted value
	 */
	public static <E> E convert(Object value, Class<E> toType) {
		return NestedPropertyUtilsBean.getInstance().convert(value, toType);
	}


	/**
     * <p>Retrieve the property descriptors for the specified bean class,
     * 	introspecting and caching them the first time a particular bean class
     *	 is encountered.</p>
     * @param beanClass Bean class for which property descriptors are requested
	 * @return the property descriptors
	 */
	public static Map<String, PropertyDescriptor> getPropertyDescriptorMap(Class beanClass) {
		return NestedPropertyUtilsBean.getInstance().getPropertyDescriptorMap(beanClass);
	}

	/**
	 * <p>Retrieve the property descriptors for the specified bean class,
	 * 	introspecting and caching them the first time a particular bean class
	 *	 is encountered.</p>
	 * @param beanClass Bean class for which property descriptors are requested
	 * @return the property descriptors
	 */
	public static  PropertyDescriptor[] getPropertyDescriptors(Class beanClass) {
		return SimplePropertyUtilsBean.getInstance().getPropertyDescriptors(beanClass);
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
	public static PropertyDescriptor getPropertyDescriptor(Class beanClass, String name) {
		return NestedPropertyUtilsBean.getInstance().getPropertyDescriptor(beanClass, name);
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
	public static PropertyDescriptor getPropertyDescriptor(Object bean, String name) {
		return NestedPropertyUtilsBean.getInstance().getPropertyDescriptor(bean, name);
	}

	/**
	 * fetch getter
	 * @param beanClass bean class
	 * @param name property name
	 * @return getter
	 */
	public static<T, P> Function<T, P> getPropertyGetter(Class<T> beanClass, String name) {
		return LambdaPropertyUtilsBean.getInstance().getPropertyGetter(beanClass, name);
	}


	/**
	 * fetch setter
	 * @param beanClass bean class
	 * @param name property name
	 * @return setter
	 */
	public static<T, P> BiConsumer<T, P> getPropertySetter(Class<T> beanClass, String name) {
		return LambdaPropertyUtilsBean.getInstance().getPropertySetter(beanClass, name);
	}


	/**
	 * If this method is invoked by JNI code with no caller class on the stack, the accessible flag can only be set if the member and the declaring class are public, and the class is in a package that is exported unconditionally.
	 * If there is a security manager, its checkPermission method is first called with a ReflectPermission("suppressAccessChecks") permission.
	 * Returns:
	 * true if the accessible flag is set to true; false if access cannot be enabled.
	 * Throws:
	 * SecurityException – if the request is denied by the security manager
	 * @param accessible the accessible object
	 * @return true if the accessible flag is set to true; false if access cannot be enabled.
	 */
	public static boolean trySetAccessible(AccessibleObject accessible) {
		return MethodHandleUtils.trySetAccessible(accessible);
	}

	/**
     * Test whether or not exists the  specified name for the specified property of the
     * specified bean class<br>
     * @param beanClass Bean class for which a property descriptor is requested
     * @param name  Possibly indexed and/or nested name of the property for
     *  which a property descriptor is requested
     * @return if exists，return true, otherwise return false
     */
	public static boolean hasProperty(Class beanClass , String name) {
		return NestedPropertyUtilsBean.getInstance().getPropertyDescriptor(beanClass, name) != null;
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
	 * @return The property type
	 */
   public static Class getPropertyType(Object bean, String name) {
	   return NestedPropertyUtilsBean.getInstance().getPropertyType(bean, name);
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
    * @return  The property type
    */
   public static  Class getPropertyType(Class beanClass, String name) {
	   return NestedPropertyUtilsBean.getInstance().getPropertyType(beanClass, name);
   }


	/**
     * Return the value of the specified property of the specified bean,
     * no matter which property reference format is used, with no
     * type conversions. 
     *
     * @param bean Bean whose property is to be extracted
     * @param name simple property name
     * @return Return the value of the specified simple property
     */
	public static  Object getSimpleProperty(Object bean , String name) {
		return SimplePropertyUtilsBean.getInstance().getProperty(bean, name, false);
	}


	/**
     * Return the value of the specified property of the specified bean,
     * no matter which property reference format is used, with no
     * type conversions.
     *
     * @param bean Bean whose property is to be extracted
     * @param name Possibly indexed and/or nested name of the property
     *  to be extracted, such as user.roles[1]
     * @return Return the value of the specified nested property
     */
	public static  Object getProperty(Object bean , String name) {
		return NestedPropertyUtilsBean.getInstance().getProperty(bean, name);
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
	public static boolean setProperty(Object bean , String name, Object value) {
		return NestedPropertyUtilsBean.getInstance().setProperty(bean, name, value);
	}
    /**
     * <p>Set the specified property value, performing type conversions as
     * required to conform to the type of the destination property.</p>
     *
     * @param bean Bean on which setting is to be performed
     * @param name Property simple name
     * @param value Value to be set
     * @return Return true when success to set,otherwise return false
     */
	public static boolean setSimpleProperty(Object bean , String name, Object value) {
		return SimplePropertyUtilsBean.getInstance().setProperty(bean, name, value, false);
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
     * @param <T> the target bean type
     * @return returns the destination bean whose properties are modified
     */
	public static<T>  T copyProperties(Object source, T target) {
		return target != null ? NestedPropertyUtilsBean.getInstance(target.getClass()).copyProperties(source, target) : null;
	}

	/**
	 * Find a method with the given method name and the given parameter types,
	 * declared on the given class or one of its superclasses. Will return a public,
	 * protected, package access, or private method.
	 * <p>Checks {@code Class.getDeclaredMethod}, cascading upwards to all superclasses.
	 * @param clazz clazz The class to introspect
	 * @param name the name of the method to find
	 * @param paramTypes the parameter types of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @see Class#getDeclaredMethod
	 */
	public static Method findDeclaredMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
		if (TextUtils.isEmpty(name) || clazz == null) {
			return null;
		}
		int index = name.lastIndexOf('.');
		if (index >= 0) {
			clazz = NestedPropertyUtilsBean.getInstance(clazz).getPropertyType(clazz, name.substring(0, index));
			if (clazz == null) {
				return null;
			}
			name = name.substring(index + 1);
		}
		return getDeclaredMethod0(clazz, name, paramTypes);
	}

	private static Method getDeclaredMethod0(Class<?> clazz, String name, Class<?>[] paramTypes) {
		try {
			return clazz.getDeclaredMethod(name, paramTypes);
		} catch (NoSuchMethodException ex) {
			if (clazz.getSuperclass() != null) {
				return getDeclaredMethod0(clazz.getSuperclass(), name, paramTypes);
			}
		}
		return null;
	}

	/**
	 * Creates a new instance of the class represented by this {@code Class}
	 * object.  The class is instantiated as if by a {@code new}
	 * expression with an empty argument list.  The class is initialized if it
	 * has not already been initialized.
	 * @param clazz the class to create an instance of
	 * @return  a newly allocated instance of the class represented by this
	 * @throws InvocationTargetException InvocationTargetException
	 * @throws NoSuchMethodException if a matching method is not found
	 *         or if the name is "&lt;init&gt;"or "&lt;clinit&gt;".
	 * @throws  IllegalAccessException  if the class or its nullary
	 *          constructor is not accessible.
	 */
	public static<T> T newInstance(Class<T> clazz) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		return NestedPropertyUtilsBean.getInstance(clazz).newInstance(clazz);
	}

	/**
	 * <p>Copy property values from the "origin" bean to the "destination" bean
	 * for all cases where the property names are the same and on the specified condition (even though the
	 * actual getter and setter methods might have been customized via
	 * <code>BeanInfo</code> classes or specified).
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be set in the destination
	 * bean.
	 *
	 * @param source Origin bean whose properties are retrieved
	 * @param target Destination bean whose properties are modified
	 * @param condition the condition
	 * @return returns false when none property is copied, otherwise returns true
	 */
	public static boolean copyOnCondition(Object source, Object target, BiPredicate<PropertyDescriptor, Object> condition) {
		return target != null && NestedPropertyUtilsBean.getInstance(target.getClass()).copyToBeanOnCondition(source, target, condition);
	}

	/**
	 * <p>Copy property values from the "origin" bean to the "destination" bean
	 * for all cases where the property names are the same and on the specified condition (even though the
	 * actual getter and setter methods might have been customized via
	 * <code>BeanInfo</code> classes or specified).
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be set in the destination
	 * bean.
	 *
	 * @param source Origin bean whose properties are retrieved
	 * @param target Destination bean whose properties are modified
	 * @param condition the condition
	 * @return returns false when none property is copied, otherwise returns true
	 */
	public static boolean copyOnCondition(Object source, Object target, LambdaUtils.TiPredicate<String, Object, Object> condition) {
		if (target instanceof Map) {
			return source != null && NestedPropertyUtilsBean.getInstance(source.getClass()).copyToMapOnCondition(source, target, condition);
		} else {
			return target != null && NestedPropertyUtilsBean.getInstance(target.getClass()).copyToBeanOnCondition(source, target, condition);
		}
	}

	/**
	 * <p>Copy property values from the "origin" bean to the "destination" map
	 * for all cases where the property names are the same with map key and on the specified value condition is pass.
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be put in the destination
	 * map.
	 *
	 * @param origin Origin bean whose properties are retrieved
	 * @param target Destination bean whose properties are modified
	 * @param condition the condition
	 * @return returns false when none property is copied, otherwise returns true
	 */
	public static boolean copyToMapOnCondition(Object origin, Map<String, Object> target, BiPredicate<String, Object> condition) {
		return origin != null && NestedPropertyUtilsBean.getInstance(origin.getClass()).copyToMapOnCondition(origin, target, condition);
	}

	/**
	 * <p>Copy property values from the "origin" bean to the "destination" bean
	 * for all cases where the property names are the same and on the specified condition (even though the
	 * actual getter and setter methods might have been customized via
	 * <code>BeanInfo</code> classes or specified).
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be set in the destination
	 * bean.
	 *
	 * @param origin Origin bean whose properties are retrieved
	 * @param target Destination bean whose properties are modified
	 * @param valueCondition the origin value condition
	 * @return returns false when none property is copied, otherwise returns true
	 */
	@SuppressWarnings("unchecked")
	public static boolean copyOnCondition(Object origin, Object target, Predicate<Object> valueCondition) {
		if (target instanceof Map) {
			if (valueCondition == null) {
				return copyToMapOnCondition(origin, (Map) target, (k, v) -> true);
			} else {
				return copyToMapOnCondition(origin, (Map) target, (k, v) -> valueCondition.test(v));
			}
		} else {
			if (valueCondition == null) {
				return copyOnCondition(origin, target, (d, v) -> true);
			} else {
				return copyOnCondition(origin, target, (d, v) -> valueCondition.test(v));
			}
		}
	}

	/**
	 * <p>Copy property values from the "origin" bean to the "destination" bean
	 * for all cases where the property names are the same and on the specified condition (even though the
	 * actual getter and setter methods might have been customized via
	 * <code>BeanInfo</code> classes or specified).
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be set in the destination
	 * bean.
	 *
	 * @param origin Origin bean whose properties are retrieved
	 * @param target Destination bean whose properties are modified
	 * @param valueCondition the origin value condition
	 * @param ignoreProperties the ignore properties
	 * @return returns false when none property is copied, otherwise returns true
	 */
	@SafeVarargs
	public static <T> boolean copyOnCondition(Object origin, T target, Predicate<Object> valueCondition, LambdaUtils.SerializeFunction<T, ?>... ignoreProperties) {
		String[] names = parseGetterOrSetterNames(ignoreProperties);
		if (names.length == 0 && valueCondition == null) {
			// 没有条件
			return copyOnCondition(origin, target, (BiPredicate<PropertyDescriptor, Object>) null);
		}
		return copyOnCondition(origin, target, (d, v) -> !matchTargetProperty(names, d.getName()) && (valueCondition == null || valueCondition.test(v)));
	}


	/**
	 * <p>Copy property values from the "origin" bean to the "destination" bean
	 * for all cases where the property names are the same and on the specified condition (even though the
	 * actual getter and setter methods might have been customized via
	 * <code>BeanInfo</code> classes or specified).
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be set in the destination
	 * bean.
	 *
	 * @param origin Origin bean whose properties are retrieved
	 * @param target Destination bean whose properties are modified
	 * @param preValueCondition the target previous value condition
	 * @param ignoreProperties the ignore properties
	 * @return returns false when none property is copied, otherwise returns true
	 */
	@SafeVarargs
	public static <T> boolean copyOnPreValueCondition(Object origin, T target, Predicate<Object> preValueCondition, LambdaUtils.SerializeFunction<T, ?>... ignoreProperties) {
		if (target == null) {
			return false;
		}
		String[] names = parseGetterOrSetterNames(ignoreProperties);
		if (preValueCondition == null) {
			return NestedPropertyUtilsBean.getInstance(target.getClass()).copyToBeanOnCondition(origin, target, (d, value) -> !matchTargetProperty(names, d.getName()));
		}
		return NestedPropertyUtilsBean.getInstance(target.getClass()).copyToBeanOnCondition(origin, target, (k, pv, v) -> !matchTargetProperty(names, k) && preValueCondition.test(pv));
	}

	/**
	 * <p>Copy property values from the "origin" bean to the "destination" bean
	 * for all cases where the property names are the same and on the specified condition (even though the
	 * actual getter and setter methods might have been customized via
	 * <code>BeanInfo</code> classes or specified).
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be set in the destination
	 * bean.
	 *
	 * @param origin Origin bean whose properties are retrieved
	 * @param target Destination bean whose properties are modified
	 * @param preValueCondition the target previous value condition
	 * @return returns false when none property is copied, otherwise returns true
	 */
	public static boolean copyOnPreValueCondition(Object origin, Object target, Predicate<Object> preValueCondition) {
		if (preValueCondition == null) {
			// 没有条件
			return copyOnCondition(origin, target, (Predicate<Object>) null);
		}
		return copyOnCondition(origin, target, (k, pv, v) -> preValueCondition.test(pv));
	}

	/**
	 * <p>Copy property values from the "origin" bean to the "destination" map
	 * for all cases where the property names are the same with map key and on the specified value condition is pass.
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be put in the destination
	 * map.
	 *
	 * @param origin Origin bean whose properties are retrieved
	 * @param target Destination Map whose properties are modified
	 * @param valueCondition the origin value condition
	 * @param ignoreProperties the ignore properties
	 * @return returns false when none property is copied, otherwise returns true
	 */
	@SafeVarargs
	public static <T> boolean copyToMapOnCondition(T origin, Map<String, Object> target, Predicate<Object> valueCondition, LambdaUtils.SerializeFunction<T, ?>... ignoreProperties) {
		String[] names = parseGetterOrSetterNames(ignoreProperties);
		if (names.length == 0 && valueCondition == null) {
			return copyToMapOnCondition(origin, target, null);
		}
		return copyToMapOnCondition(origin, target, (k, v) -> !matchTargetProperty(names, k) && (valueCondition == null || valueCondition.test(v)));
	}

	/**
	 * <p>Copy property values from the "origin" bean to the "destination" bean
	 * for the value has changed where the property names are the same with map key and on the specified value condition is pass.
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be put in the destination
	 * map.
	 *
	 * @param origin Origin bean whose properties are retrieved
	 * @param target Destination bean whose properties are modified
	 * @param valueCondition the origin value condition
	 * @param ignoreProperties the ignore properties
	 * @return returns false when none property is copied, otherwise returns true
	 */
	@SafeVarargs
	public static <T> boolean copyChangeOnCondition(Object origin, T target, Predicate<Object> valueCondition, LambdaUtils.SerializeFunction<T, ?>... ignoreProperties) {
		if (target == null) {
			return false;
		}
		String[] names = parseGetterOrSetterNames(ignoreProperties);
		return NestedPropertyUtilsBean.getInstance(target.getClass()).copyToBeanOnCondition(origin, target, (k, pv, v) -> {
			if (matchTargetProperty(names, k) || (valueCondition != null && !valueCondition.test(v))) {
				return false;
			}
			return !Objects.equals(pv, v);
		});
	}

	/**
	 * <p>Copy property values from the "origin" bean to the "destination" Map
	 * for the value has changed where the property names are the same with map key and on the specified value condition is pass.
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be put in the destination
	 * map.
	 *
	 * @param origin Origin bean whose properties are retrieved
	 * @param target Destination Map whose properties are modified
	 * @param valueCondition the origin value condition
	 * @param ignoreProperties the ignore properties
	 * @return returns false when none property is copied, otherwise returns true
	 */
	@SafeVarargs
	public static <T> boolean copyChangeToMapOnCondition(T origin, Map<String, Object> target, Predicate<Object> valueCondition, LambdaUtils.SerializeFunction<T, ?>... ignoreProperties) {
		if (origin == null) {
			return false;
		}
		String[] names = parseGetterOrSetterNames(ignoreProperties);
		return NestedPropertyUtilsBean.getInstance(origin.getClass()).copyToMapOnCondition(origin, target, (k, pv, v) -> {
			if (matchTargetProperty(names, k) || (valueCondition != null && !valueCondition.test(v))) {
				return false;
			}
			return !Objects.equals(pv, v);
		});
	}

	/**
	 * <p>Copy property values from the "origin" bean to the "destination" bean
	 * for the value has changed where the property names are the same with map key and on the specified value condition is pass.
	 *
	 * <p>If the origin "bean" is actually a <code>Map</code>, it is assumed
	 * to contain String-valued <strong>simple</strong> property names as the keys, pointing
	 * at the corresponding property values that will be put in the destination
	 * map.
	 *
	 * @param origin Origin bean whose properties are retrieved
	 * @param target Destination bean whose properties are modified
	 * @param valueCondition the origin value condition
	 * @return returns false when none property is copied, otherwise returns true
	 */
	@SuppressWarnings("unchecked")
	public static boolean copyChangeOnCondition(Object origin, Object target, Predicate<Object> valueCondition) {
		if (target instanceof Map) {
			return copyChangeToMapOnCondition(origin, (Map<String, Object>) target, valueCondition, (LambdaUtils.SerializeFunction<Object, ?>[]) null);
		} else {
			return copyChangeOnCondition(origin, target, valueCondition, (LambdaUtils.SerializeFunction<Object, ?>[]) null);
		}
	}


	private static boolean matchTargetProperty(String[] getterOrSetterNames, String name) {
		if (getterOrSetterNames.length == 0 || name == null || name.isEmpty()) {
			return false;
		}
		for (String getterOrSetterName : getterOrSetterNames) {
			for (String prefix : GETTER_SETTER_PREFIXES) {
				if (!getterOrSetterName.startsWith(prefix)) {
					continue;
				}
				if (getterOrSetterName.length() == name.length() + prefix.length()) {
					if (getterOrSetterName.endsWith(name)) {
						return true;
					}
					if (getterOrSetterName.regionMatches(prefix.length() + 1, name, 1, name.length() - 1)
							&& getterOrSetterName.charAt(prefix.length()) == Character.toUpperCase(name.charAt(0))) {
						return true;
					}
				}
				break;
			}
		}
		return false;
	}


	private static String[] parseGetterOrSetterNames(LambdaUtils.SerializeFunction[] properties) {
		if (properties == null || properties.length == 0) {
			return ArrayConstants.EMPTY_STRING_ARRAY;
		}
		String[] names = new String[properties.length];
		for (int i = 0; i < names.length; i++) {
			SerializedLambda lambda = LambdaUtils.resolveLambda(properties[i]);
			names[i] = lambda.getImplMethodName();
		}
		return names;
	}
}
