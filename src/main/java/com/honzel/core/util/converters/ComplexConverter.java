package com.honzel.core.util.converters;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author honzel
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class ComplexConverter extends TypeConverter {
	
	public ComplexConverter() {
		super();
	}

	public ComplexConverter(boolean disableException) {
		super(disableException);
	}

	/**
     * Register a custom {@link Converter} for the specified destination
     * <code>Class</code> and the specified source <code>Class</code>, replacing any previously registered Converter.
     *
     * @param sourceType The type of input object 
     * @param targetType Destination class for conversions performed by this
     *  Converter
     * @param converter Converter to be registered
	 */
//	@SuppressWarnings("unchecked")
	public void  register(Class sourceType, Class targetType, Converter converter) {
		if (targetType == null) {
			throw new IllegalArgumentException("Target type is missing.");
		}
		Map sourceConverters = (Map) converters.get(targetType);
		if (sourceConverters == null) {
			sourceConverters = new HashMap(5);
			converters.put(targetType, sourceConverters);
			if (sourceType != null && !sourceConverters.containsKey(null)) {
				sourceConverters.put(null, converter);
			}
		}
		sourceConverters.put(sourceType, converter);
	}
	
	/**
     * Look up and return any registered {@link Converter} for the specified
     * destination class; if there is no registered Converter, return
     * <code>null</code>.
     *
     * @param value the input object.
     * @param targetType Class for which to return a registered Converter
	 * @return The registered {@link Converter} or <code>null</code> if not found
	 */
	public Converter lookup(Object value, Class targetType) {
		Map sourceConverters = (Map) converters.get(targetType);
		if (sourceConverters == null) {
			return null;
		}
		Class sourceType = null;
		if (value != null) {
			sourceType = value.getClass();
		}
		Converter converter = (Converter) sourceConverters.get(sourceType);
		if (converter == null && sourceType != null) {
			converter = lookupAncestors(sourceType, sourceConverters, true);
		}
		return converter;
	}
	
	/**
	 * Convert the specified array to an  array of the specified element type.
	 * @param value the input array object.
	 * @param elementType the element type of  ouput array 
	 * @return the converted array
	 */
	protected Object convertToArray(Object[] value, Class elementType) {
		Object result = Array.newInstance(elementType, value.length);
		 for(int i = 0; i < value.length; i++) {
			 Array.set(result, i, convertToType(value[i], elementType));
		 }
		return result;
	}
	

	private Converter lookupAncestors(Class sourceType, Map sourceConverters, boolean fetchSuperClass) {
		Converter converter = null;
		if(fetchSuperClass && !sourceType.isInterface()) {
			Class supperClass = sourceType.getSuperclass();
			if(supperClass != null) {
				converter = (Converter) sourceConverters.get(supperClass);
				if(converter == null && !Object.class.equals(supperClass)) {
					converter = lookupAncestors(supperClass, sourceConverters, true);
				}
				if(converter != null) {
					return converter;
				}
			}
		}
		Class[] superTypes = sourceType.getInterfaces();
		if (superTypes != null) {
			for (int i = 0; i < superTypes.length; i++) {
				converter = (Converter) sourceConverters.get(superTypes[i]);
				if (converter == null) {
					 converter = lookupAncestors(superTypes[i], sourceConverters, false);
				}
				if (converter != null) {
					return converter;
				}
			}
		}
		if (converter == null) {
			converter = (Converter) sourceConverters.get(null);
		}
		return converter;
	}
}
