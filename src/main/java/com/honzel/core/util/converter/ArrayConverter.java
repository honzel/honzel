package com.honzel.core.util.converter;

import com.honzel.core.util.exception.ConversionException;

import java.lang.reflect.Array;



/**
 * {@link Converter} implementaion that handles conversion 
 * to and from <b>array</b>.
 * @author honzel
 *
 */
public class ArrayConverter extends AbstractConverter {

	/**
	 * Converter used to convert
     *  individual array elements.
	 */
	protected TypeConverter elementConverter;
	
	private char delimiter = ',';
	
    /**
     * Set the delimiter to be used for parsing a delimited String.
     *
     * @param delimiter The delimiter [default ',']
     */
	public void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}
	public char getDelimiter() {
		return delimiter;
	}


	public Converter getElementConverter() {
		return elementConverter;
	}
	  /**
     * Construct an <b>array</b> <code>Converter</code> with the specified
     * <b>element</b> <code>Converter</code>.
     * @param elementConverter Converter used to convert individual array elements.
     * @param defaultConverter the specified default converter, may be null.
     */
	public ArrayConverter(TypeConverter elementConverter, Converter defaultConverter) {
		super(defaultConverter);
		if (elementConverter == null) {
            throw new IllegalArgumentException("Component Converter is missing.");
        }
		this.elementConverter = elementConverter;
	}

	/**
     * Construct an <b>array</b> <code>Converter</code> with the specified
     * <b>element</b> <code>Converter</code>.
     * @param elementConverter Converter used to convert individual array elements.
     * @param disableException whether or not disable to throw exception when error ocurrs that
     * 		this converter and the default converter (if exists) converting the input object to the target type.
	 */
	public ArrayConverter(TypeConverter elementConverter, boolean disableException) {
		this(elementConverter, null);
		setDisableException(disableException);
	}
	  /**
     * Construct an <b>array</b> <code>Converter</code> with the specified
     * <b>element</b> <code>Converter</code>.
     * @param elementConverter Converter used to convert individual array elements.
     */
	public ArrayConverter(TypeConverter elementConverter) {
		this(elementConverter, null);
	}

	protected String convertToString(Object value) throws ConversionException {
		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			if (length == 0)
				return "";
			String item = (String) elementConverter.convert(Array.get(value, 0), String.class);
			if (length == 1) {
				return item == null ? "" : item;
			}
			StringBuffer buf = new StringBuffer(item == null ? "" : item);
			for (int i = 1; i < length; i++) {
				item = (String) elementConverter.convert(Array.get(value, i), String.class);
				buf.append(delimiter).append(item == null ? "" : item);
			}
			return buf.toString();
		}
        return super.convertToString(value);
    }

	protected Object convertToType(Object value, Class toType) throws ConversionException {
		if (toType.isArray()) {
			Class<?> sourceType = value.getClass();
			if(String.class.equals(sourceType)) {
				return this.stringToArray(value, toType);
			}
			Class<?> elementType = toType.getComponentType();
			if (sourceType.isArray()) {
				return this.convertToArray((Object[])value, elementType);
			}
			Object item = elementConverter.convert(value, elementType);
			Object result = Array.newInstance(elementType, 1);
			Array.set(result, 0, item);
			return result;
		}
		return super.convertToType(value, toType);
	}


	protected Object stringToArray(Object value, Class<?> toType) throws ConversionException {
		String text = (String) value;
		int index = -1;
		int count = 0;
		while ((index = text.indexOf(delimiter, index + 1)) >= 0)
			count ++;
		Class<?> elementType = toType.getComponentType();
		if (count == 0 && text.length() == 0)
			return Array.newInstance(elementType, 0);
		Converter converter = elementConverter.lookup(elementType);
		Object result = Array.newInstance(elementType, count + 1);
		int prev = 0;
		for (int i = 0; i < count; i++) {
			index = text.indexOf(delimiter, prev);
			Object element = elementConverter.convert(text.substring(prev, index), elementType, converter);
			Array.set(result, i, element);
			prev = index + 1;
		}
		if (prev > 0) {
			text = text.substring(prev);
		}
		Object element = elementConverter.convert(text, elementType, converter);
		Array.set(result, count, element);
		return result;
	}

	/**
	 * Convert the specified array to an  array of the specified element type.
	 * @param array the input array object.
	 * @param elementType the element type of  ouput array
	 * @return
	 */
	private Object convertToArray(Object[] array, Class<?> elementType) {
		Object result = Array.newInstance(elementType, array.length);
		if (elementType.isArray()) {
			 for (int i = 0; i < array.length; i++) {
				 if (array[i] != null) {
					 Array.set(result, i, convertToType(array[i], elementType));
				 }
			 }
		} else {
			Converter converter = elementConverter.lookup(elementType);
			for (int i = 0; i < array.length; i++) {
				Object item = elementConverter.convert(array[i], elementType, converter);
				Array.set(result, i, item);
			}
		}
		return result;
	}
	
}
