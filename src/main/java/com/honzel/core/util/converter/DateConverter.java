package com.honzel.core.util.converter;

import com.honzel.core.util.exception.ConversionException;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
/**
 * This converter handles conversion to the following types:
 * <ul>
 *     <li><code>java.util.Date</code></li>
 *     <li><code>java.util.Calendar</code></li>
 *     <li><code>java.sql.Date</code></li>
 *     <li><code>java.sql.Time</code></li>
 *     <li><code>java.sql.Timestamp</code></li>
 * </ul>
 * extends conversion to the following types :
 * <ul>
 *     <li><code>java.lang.Long</code></li>
 *     <li><code>java.lang.String</code></li>
 * </ul>
 * @author honzel
 *
 */
@SuppressWarnings({"rawtypes"})
public class DateConverter extends AbstractConverter {
	
	
   public DateConverter() {
   }
   
   /**
     * For the type of registration and this converter is not responsible for conversion, 
     * using the specified one as default converter.
     * @param defaultConverter the specified default converter, may be null.
    * @see AbstractConverter#AbstractConverter(Converter)
    */
   public DateConverter(Converter defaultConverter) {
	   super(defaultConverter);
   }
   /**
     * For the type of registration and this converter is not responsible for conversion, 
     * using the specified one as a default converter.
     * @param defaultConverter the specified default converter, may be null.
     * @param disableException whether or not disable to throw exception when error ocurrs that 
     * 					this converter and the default converter (if exists) converting the input object to the target type.
    * @see AbstractConverter#AbstractConverter(Converter)
    */
   public DateConverter(Converter defaultConverter, boolean disableException) {
	   super(defaultConverter, disableException);
   }
	
    private String[] patterns;

    private Class[] dateTypes;

	private TimeZone timeZone;
	public void setTimeZone(String timeZone) {
		this.timeZone = TimeZone.getTimeZone(timeZone);
	}

	private Object parseDate(Class toType, String text) {
		if (java.util.Calendar.class.equals(toType)) {
			return parse(text, toType);
		}  else if (java.util.Date.class.equals(toType)
				|| java.sql.Timestamp.class.equals(toType)
				|| java.sql.Date.class.equals(toType)
				|| java.sql.Time.class.equals(toType)
				) {
			Calendar calendar = parse(text, toType);
			return toDate(toType, calendar.getTimeInMillis());
		}
		return null;
	}




    // --------------------------------------------------------- Public Methods

    /**
     * Set the date format patterns  to convert
     * dates to/from a <code>java.lang.String</code>.
     *
     * @param patterns Array of format patterns.
     */
    public void setPatterns(String... patterns) {
       setPatterns(null, patterns);
    }


   /**
     * Set the date format patterns  to convert
     * dates to/from a <code>java.lang.String</code>.
     *
    * @param types the specfied types that the type of it to use the default pattern by index in the patterns
    * @param ptns the specified patterns that types map to.
    */
    public void setPatterns(Class[] types, String[] ptns) {
    	this.dateTypes  = types;
    	this.patterns = ptns;
    }

    /**
     * Convert an input Date/Calendar object into a String.
     * @param value The input value to be converted
     * @return the converted String value.
     */
    protected String convertToString(Object value) throws ConversionException {
    	Date date = null;
    	int begin = -1;
    	if (value instanceof Date) {
    		begin = beginIndexOfPatterns(value.getClass());
			if (dateTypes != null && (begin < 0 || begin >= dateTypes.length) && !Date.class.equals(value.getClass())) {
    			begin = beginIndexOfPatterns(Date.class);
			}
    		date = (Date) value;
    	} else if (value instanceof Calendar) {
    		begin = beginIndexOfPatterns(value.getClass());
    		if (dateTypes != null && (begin < 0 || begin >= dateTypes.length)) {
    			begin = beginIndexOfPatterns(Calendar.class);
			}
			date = ((Calendar) value).getTime();
		}
		if (date != null) {
			if (begin >= 0) {
				DateFormat format = getFormat(null, patterns[begin]);
				return format.format(date);
			} else {
				return Long.toString(date.getTime());
			}
    	}
		return super.convertToString(value);
    }

    /**
     * Convert a long value to the specified Date type for this
     * <i>Converter</i>.
     * <p>
     *
     * This method handles conversion to the following types:
     * <ul>
     *     <li><code>java.util.Date</code></li>
     *     <li><code>java.util.Calendar</code></li>
     *     <li><code>java.sql.Date</code></li>
     *     <li><code>java.sql.Time</code></li>
     *     <li><code>java.sql.Timestamp</code></li>
     * </ul>
     *
     * @param type Data type to which this value should be converted
     * @param value The long value to be converted
     * @return The converted date value.
     */
    private Object toDate(Class type, long value) {
        if (type.equals(Date.class)) {
            return new Date(value);
        }
        if (type.equals(Calendar.class)) {
        	Calendar calendar = Calendar.getInstance();
        	calendar.setTimeInMillis(value);
        	calendar.setLenient(false);
        	return calendar;
        }
        if (type.equals(java.sql.Timestamp.class)) {
        	return new java.sql.Timestamp(value);
        }
        if (type.equals(java.sql.Date.class)) {
            return new java.sql.Date(value);
        }
        if (type.equals(java.sql.Time.class)) {
            return new java.sql.Time(value);
        }
        return null;
    }
    /**
     * Return a <code>DateFormat</code> instance for the Locale.
     * @param locale The Locale to create the Format with (may be null)
     * @param timeZone The Time Zone create the Format with (may be null)
     *
     * @return A Date Format.
     */
    protected DateFormat getFormat(Locale locale, TimeZone timeZone) {
        DateFormat format = null;
        if (locale == null) {
            format = DateFormat.getDateInstance(DateFormat.SHORT);
        } else {
            format = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        }
        if (timeZone != null) {
            format.setTimeZone(timeZone);
        }
        return format;
    }


    /**
     * Returns the first pattern index which the type to use.
     * @param type the specified date type.
     * @return
     */
	private int beginIndexOfPatterns(Class type) {
    	return nextIndexOfPatterns(type, -1);
    }

	/**
     * Returns the next pattern index which match the type to use.
     * @param type the specified date type.
	 * @param previousIndex the previous pattern index which  the date type used.
	 * @return
	 */
	private int nextIndexOfPatterns(Class type, int previousIndex) {
		if (patterns == null)
			return -1;
		previousIndex ++;
		if (dateTypes != null && dateTypes.length > previousIndex) {
			if (type != null) {
				boolean matchNull = true;
				while (previousIndex < dateTypes.length) {
					if (dateTypes[previousIndex] == null) {
						if (matchNull)
							break;
						matchNull = false;
					} else if (type.equals(dateTypes[previousIndex])) {
						break;
					} else {
						matchNull = false;
					}
					previousIndex ++;
				}
			} else {
				previousIndex = dateTypes.length;
			}
		}
		if (patterns.length > previousIndex)
			return previousIndex;
		return -1;
	}

    /**
     * Create a date format for the specified pattern.
     *
     * @param pattern The date pattern
     * @return The DateFormat
     */
    private  SimpleDateFormat getFormat(SimpleDateFormat format, String pattern) {
    	if (format == null) {
    		return new SimpleDateFormat(pattern);
		}
		format.applyPattern(pattern);
    	return format;
    }

	public SimpleDateFormat getFormat(SimpleDateFormat format, String pattern, TimeZone timeZone) {
		SimpleDateFormat dateFormat = getFormat(format, pattern);
		if (timeZone != null) {
			dateFormat.setTimeZone(timeZone);
		}
		return dateFormat;
	}

	/**
	 * Parse a String date value using the set of patterns.
	 *
	 * @param text The String date value.
	 * @param toType the target type.
	 *
	 * @return The converted Date object.
	 */
	public Calendar parse(String text, Class toType)  {
		int index = beginIndexOfPatterns(toType);
		if (index >= 0) {
			ParsePosition pos = new ParsePosition(0);
			SimpleDateFormat format = null;
			do {
				try {
					format = getFormat(format, patterns[index]);
					format.setLenient(false);
					pos.setErrorIndex(-1);
					pos.setIndex(0);
					Date parsedDate = format.parse(text, pos);
					if (pos.getErrorIndex() < 0 && pos.getIndex() == text.length() && parsedDate != null) {
						return format.getCalendar();
					}
					index = nextIndexOfPatterns(toType, index);
				} catch (Exception e) { e.printStackTrace();}
			} while (index >= 0);
		}
		long longValue = Long.parseLong(text);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(longValue);
		return calendar;
	}

	protected Object convertToType(Object value, Class toType) throws ConversionException {
		Object firstValue = fetchFirst(value, toType);
		long longValue = 0L;
		int validType = 1;
		if (firstValue instanceof Date) {
			longValue = ((Date) firstValue).getTime();
		} else if (firstValue instanceof Calendar) {
			longValue = ((Calendar) firstValue).getTimeInMillis();
		} else if (firstValue instanceof Number) {
			longValue =((Number) firstValue).longValue();
			validType = 2;
		} else {
			validType = 0;
		}
		if (validType > 0) {
			Object result = toDate(toType, longValue);
			if(result != null)
				return result;
			if(validType == 1 && (Long.class.equals(toType) || Long.TYPE.equals(toType))) {
				return longValue;
			}
		} else if ((firstValue instanceof CharSequence) && ((CharSequence) firstValue).length() > 0) {
			Object result = parseDate(toType, firstValue.toString());
			if(result != null)
				return result;
		}
		return super.convertToType(value, toType);
	}
    
}
