package com.honzel.core.util.converters;
/**
  * <p>General purpose data type converter that can be registered and used
 * within the BeanHelper package to manage the conversion of objects from
 * one type to another.
 * @author honzel
 *
 */
@SuppressWarnings({"rawtypes"})
public interface Converter {
	/**
     * Convert the specified input object into an output object of the
     * specified type.
     *
     * @param value The input value to be converted
     * @param toType Data type to which this value should be converted
	 * @return The converted  value.
	 */
	public Object convert(Object value, Class toType); 
}
