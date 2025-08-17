package com.honzel.core.util.converter;

import com.honzel.core.util.exception.ConversionException;
import com.honzel.core.util.time.LocalDateTimeUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * The multi types converter
 * @author honzel
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class TypeConverter extends AbstractConverter {

	
	protected final Map converters;
	/**
	 * 
	 */
	private final AbstractConverter standardConverter;
	
	private final ArrayConverter arrayConverter;

	public void setDisableException(boolean disableException) {
		super.setDisableException(disableException);
		standardConverter.setDisableException(disableException);
		arrayConverter.setDisableException(disableException);
	}

	public TypeConverter(boolean disableException) {
		this();
		setDisableException(disableException);
	}

	public TypeConverter() {
		standardConverter = new StandardConverter();
		arrayConverter = new ArrayConverter(this, standardConverter);
		converters = new HashMap();
		registerStandard();
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
	protected void  register(Class sourceType, Class targetType, Converter converter) {
		if (targetType == null) {
			throw new IllegalArgumentException("Target type is missing");
		}
		if (converter == this) {
			throw new IllegalArgumentException("Cannot resister this converter self. ");
		}
		if (converter instanceof AbstractConverter) {
			Converter defaultConverter;
			if ((((AbstractConverter) converter).defaultConverter) == null && (defaultConverter = findDefaultConverter(converter)) != null) {
				((AbstractConverter) converter).defaultConverter = defaultConverter;
			}
		}
		converters.put(targetType, converter);
	}

	private Converter findDefaultConverter(Converter findConverter) {
		Converter defaultConverter = (Converter) converters.get(String.class);
		if (defaultConverter == null || findConverter == defaultConverter) {
			return null;
		}
		Converter converter = defaultConverter;
		int top = converters.size();
		while (converter instanceof AbstractConverter && top-- > 0) {
			Converter nextConverter;
			if ((nextConverter = ((AbstractConverter) converter).defaultConverter) == findConverter) {
				return null;
			}
			converter = nextConverter;
		}
		return defaultConverter;
	}

	/**
     * Register a custom {@link Converter} for the specified destination
     * <code>Class</code>, replacing any previously registered Converter.
     *
     * @param targetType Destination class for conversions performed by this
     *  Converter
     * @param converter Converter to be registered
     */
	public void  register(Class targetType, Converter converter) {
		register(null, targetType, converter);
	}

	/**
	 * Look up and return any registered {@link Converter} for the specified
	 * destination class; if there is no registered Converter, return
	 * <code>null</code>.
	 *
	 * @param targetType Class for which to return a registered Converter
	 * @return The registered {@link Converter} or <code>null</code> if not found
	 */
	public Converter lookup(Class targetType) {
		return lookup(null, targetType);
	}

	/**
	 * Look up and return the registered {@link Converter} for the specified
	 * destination class; if there is no registered Converter, return
	 * <code>null</code>.
	 *
	 * @param value the value being converted
	 * @param targetType Class of the value to be converted to
	 * @return The registered {@link Converter} or <code>null</code> if not found
	 */
	protected Converter lookup(Object value, Class targetType) {
		Converter converter = (Converter) converters.get(targetType);
		if (converter != null) {
			return converter;
		}
		if (targetType.isArray()) {
			return getArrayConverter();
		}
		if (targetType.isEnum() && Objects.nonNull(converter = (Converter) converters.get(Enum.class))) {
			return converter;
		}
		return (Converter) converters.get(Object.class);
	}

	protected Object convertToType(Object value, Class toType) throws ConversionException {
		return convert(value, toType, lookup(value, toType));
	}


	protected String convertToString(Object value) throws ConversionException {
		Converter converter = lookup(value, value.getClass());
		if (converter == null) {
			converter = this.lookup(value, String.class);
		}
		return (String)convert(value, String.class, converter);
	}

	public Object getDefault(Object value, Class toType) {
		if (value == null) {
			Converter converter = lookup(null, toType);
			if (converter != null) {
				try {
					return converter.convert(null, toType);
				} catch (RuntimeException e) {
					throwException(e, null);
				}
			}
		}
		return null;
	}
	/**
	 * array converter
	 * @return the array converter
	 */
	public ArrayConverter getArrayConverter() {
		return arrayConverter;
	}




	/**
	* <p>Convert the specified value to an object of the specified class with the specified converter (if
     * possible).  Otherwise, return default value(usually <code>null</code>) .</p>
     * @param value Value to be converted (may be null)
     * @param toType Java class to be converted to
	 * @param converter the specified converter
	 * @return  The converted value
	 */
	public Object convert(Object value, Class toType, Converter converter) {
		if (toType.isInstance(value)) {
			return  value;
		}
		if (converter == null) {
			 if(value != null) {
				throwException(null, "Couldn't find the  converter of the type  '" + getName(toType) + "'.");
			 }
			return null;
		}
		return converter.convert(value, toType);
	}

	private void registerStandard() {
		register(Short.TYPE, standardConverter);
		register(Boolean.TYPE, standardConverter);
		register(Byte.TYPE, standardConverter);
		register(Character.TYPE, standardConverter);
		register(Float.TYPE, standardConverter);
		register(Double.TYPE, standardConverter);
		register(Short.class, standardConverter);
//		register(Integer.TYPE, standardConverter);
//		register(Long.TYPE, standardConverter);
//		register(Integer.class, standardConverter);
//		register(Long.class, standardConverter);
		register(Boolean.class, standardConverter);
		register(Byte.class, standardConverter);
		register(Character.class, standardConverter);
		register(Float.class, standardConverter);
		register(Double.class, standardConverter);
		register(BigInteger.class, standardConverter);
		register(BigDecimal.class, standardConverter);
		register(Class.class, standardConverter);
		register(Enum.class, standardConverter);
		// local date time converter
		LocalDateTimeConverter dateTimeConverter = new LocalDateTimeConverter(standardConverter);
		register(LocalDate.class, dateTimeConverter);
		register(LocalTime.class, dateTimeConverter);
		register(LocalDateTime.class, dateTimeConverter);
		register(Instant.class, dateTimeConverter);
		register(Date.class, dateTimeConverter);
		register(Calendar.class, dateTimeConverter);
		register(Long.class, dateTimeConverter);
		register(Long.TYPE, dateTimeConverter);
		register(Integer.TYPE, dateTimeConverter);
		register(Integer.class, dateTimeConverter);
		register(String.class, dateTimeConverter);
	}
}
