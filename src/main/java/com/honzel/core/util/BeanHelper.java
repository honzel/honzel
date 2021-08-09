package com.honzel.core.util;

import com.honzel.core.util.bean.NestedPropertyUtilsBean;
import com.honzel.core.util.converters.Converter;
import com.honzel.core.util.converters.TypeConverter;

import java.beans.PropertyDescriptor;
import java.util.Map;
/**
 * Utility methods for using Java Reflection APIs to facilitate generic
 * property getter and setter operations on Java objects. 
 * @author honzy
 *
 */
@SuppressWarnings({"rawtypes" })
public class BeanHelper {
	
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
	 * 
	 */
	private BeanHelper() {
		
	}
	/**
     * Register a custom {@link Converter} for the specified destination
     * <code>Class</code>, replacing any previously registered Converter.
     *
     * @param toType Destination class for conversions performed by this
     *  Converter
     * @param converter Converter to be registered
	 * @param overridenByGlobalEnableException whether or not to  override the converter enable exception set. 
	 */
	public static void  registerConverter(Class toType, Converter converter, boolean overridenByGlobalEnableException) {
		NestedPropertyUtilsBean.getInstance().registerConverter(toType, converter, overridenByGlobalEnableException);
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
	public static Converter lookup(Class toType) {
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
		return NestedPropertyUtilsBean.getInstance().getPropertyDescriptors(beanClass);
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
     * Test whether or not exists the  specified name for the specified property of the
     * specified bean class<br>
     * @param beanClass Bean class for which a property descriptor is requested
     * @param name  Possibly indexed and/or nested name of the property for
     *  which a property descriptor is requested
     * @return if exists，return true, otherwise return false
     */
	public static  boolean hasProperty(Class beanClass , String name) {
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
		return NestedPropertyUtilsBean.getInstance().getSimpleProperty(bean, name);
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
		return NestedPropertyUtilsBean.getInstance().setSimpleProperty(bean, name, value);
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
			return NestedPropertyUtilsBean.getInstance().copyProperties(source, target);
		}

}
