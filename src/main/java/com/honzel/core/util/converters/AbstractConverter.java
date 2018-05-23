package com.honzel.core.util.converters;

import java.lang.reflect.Array;

import com.honzel.core.util.exceptions.ConversionException;

/**
 * Base {@link Converter} implementation that provides the structure
 * for handling conversion <b>to</b> and <b>from</b> a specified type.
 * <p>
 * This implementation provides the basic structure for
 * converting to/from a specified type
 * <p>
 * Implementations should provide conversion to the specified
 * type and from the type of the input object.
 * @author honzy
 *
 */
@SuppressWarnings({"rawtypes"})
public abstract  class AbstractConverter implements Converter {
	/**
	 * the default converter
	 */
    private Converter defaultConverter;
    /**
     * whether or not disable to throw exception when error ocurrs that 
     * 	this converter and the default converter (if exists) converting the input object to the target type.
     */
//    private boolean enableException;
    private boolean disableException;


    public Converter getDefaultConverter() {
		return defaultConverter;
	}

    public AbstractConverter() {
	}
    /**
     *Construct an <code>Converter</code> with a default converter
     * For the type of registration and this converter is not responsible for conversion,
     * using the specified one as a default converter.
     * @param defaultConverter the specified default converter, may be null.
     */
    public AbstractConverter(Converter defaultConverter) {
 	   this.defaultConverter = defaultConverter;
    }
    /**
     *Construct an <code>Converter</code> with a default converter
     * For the type of registration and this converter is not responsible for conversion,
     * using the specified one as a default converter.
     * @param defaultConverter the specified default converter, may be null.
     * @param disableException whether or not disable to throw exception when error ocurrs that
     * 		this converter and the default converter (if exists) converting the input object to the target type.
     *
     */
    public AbstractConverter(Converter defaultConverter, boolean disableException) {
    	this.defaultConverter = defaultConverter;
    	this.disableException = disableException;
    }

    /**
     * when disable to  throw an exception when a conversion error ocurrs, return false, or return true.
     * @return whether throw an exception or not.
     */
    public boolean isDisableException() {
		return disableException;
	}

    /**
	 *
     * Set <code>false</code> if the this converter and the default converter (if exists) should
     * throw an exception when a conversion error occurs, otherwise
     * set <code>true</code>(default <code>false</code>)
     *
	 * @param disableException Whether or not disable to  throw an excception when a conversion error ocurrs.
	 */
	public void setDisableException(boolean disableException) {
		this.disableException = disableException;
	}

	public Object convert(Object value, Class toType) {
		if (toType == null) {
			throwException(null, "Target type is missing");
			return null;
		}
		if(value == null) {
			return getDefault(value, toType);
		}
		try {
			if (toType.equals(value.getClass())) {
				return value;
			}
			if (String.class.equals(toType)) {
				// object -> String
				return convertToString(value);
			} else {
				// object -> Type
				return convertToType(value, toType);
			}
		} catch (ConversionException e) {
			throwException(e, null);
		} catch (RuntimeException e) {
			throwException(e, "Error occurs when Converting the value of the type  '" + getTypeName(value)
					+ "' to an object of the type '" + getName(toType) + "'.");
		}
		return getDefault(null, toType);
	}
    /**
     * Convert the input object  into an output object of the
     * specified type.
     * <p>
     *
     * @param value The input value to be converted.
     * @param toType Data type to which this value should be converted.
     * @return The converted value.
     * @throws RuntimeException if an error occurs converting to the specified type
     */
    protected Object convertToType(Object value, Class toType) throws ConversionException {
    	return getDefault(value, toType);
    }


    /**
     * Convert the input object(usually not null)  into a String.
     * <p>
     * <b>N.B.</b>This implementation simply uses  the value's  <code>toString()</code> method
     * 	when the default value is <code>null</code>
     * <p>
     *  it should be overriden if a more sophisticated mechanism for <i>conversion to a String</i>
     * is required.
     *
     * @param value The input value to be converted.
     * @return the converted String value.
     * @throws RuntimeException if an error occurs converting to a String
     */
    protected String convertToString(Object value) throws ConversionException {
    	if (defaultConverter != null) {
    		if (defaultConverter instanceof AbstractConverter) {
    			return ((AbstractConverter) defaultConverter).convertToString(value);
    		} else {
    			return (String) defaultConverter.convert(value, String.class);
    		}
    	}
        return value.toString();
    }

	/**
	 * Return default value of the specified type,
	 * if this converter has a default converter, use the default converter first .
	 * <p>
	* @param value The specified value to be converted to the specified type.
	 * @param toType Data type to which this value should be converted
	 * @return the converted default value
	 */
    public Object getDefault(Object value, Class toType) {
	   if (defaultConverter != null) {
			try {
				return defaultConverter.convert(value, toType);
			} catch (ConversionException e) {
				throwException(e, null);
			} catch (RuntimeException e) {
				throwException(e, "Error occurs when Converting the value of the type  '" + getTypeName(value)
						+ "' to an object of the type '" + getName(toType) + "'.");
			}
		} else if (value != null && !"".equals(value)) {
			throwException(null, "Error occurs when Converting the value of the type  '" + getTypeName(value)
					+ "' to an object of the type '" + getName(toType) + "' .");
		}
		return null;
	}

	protected String getName(Class type) {
		return type == null ? "null" : type.getName();
	}

	protected String getTypeName(Object value) {
		return value == null ? "null" : this.getName(value.getClass());
	}




   /**
    * If enable to throw the exception when a conversion error occurs.
    * when the specified  exception   is not <code>null</code> and the message  is  <code>null</code>,
    * 	 throw the unchanged exception. otherwise throw a <b>ConversionException</b>.
    * @param exception the specified exception.
    * @param message the error message where  a conversion erorr occurs.
    */
   public void throwException(RuntimeException exception, String message)
		   throws RuntimeException {
	   if (exception == null) {
		   exception = new ConversionException(message);
	   } else if (message != null) {
		   exception = new ConversionException(message, exception);
	   }
	   if (disableException) {
		   exception.printStackTrace();
	   } else {
		   throw exception;
	   }
   }

   /**
    * Return the first element from an array
    * or the value unchanged if not an array or the toType is array type.
    *
     * N.B. This needs to be overriden for array/Collection converters.
    * @param value The value to convert
    * @param toType The target type
    * @return The first element in an array
    * elsewise the value unchanged if not an array or the target type is a array type.
    */
   protected Object fetchFirst (Object value, Class toType) {
	   if (value == null ) {
		   return null;
	   }
	   if (!toType.isArray() && value.getClass().isArray()) {
		   if (Array.getLength(value) > 0) {
			   return Array.get(value, 0);
		   } else {
			   return null;
		   }
	   }
	   return value;
   }
}
