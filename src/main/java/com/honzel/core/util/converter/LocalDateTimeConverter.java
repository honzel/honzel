package com.honzel.core.util.converter;

import com.honzel.core.constant.NumberConstants;
import com.honzel.core.util.exception.ConversionException;
import com.honzel.core.util.text.TextUtils;
import com.honzel.core.util.time.LocalDateTimeUtils;

import java.text.ParsePosition;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * This converter handles conversion to the following types:
 * <ul>
 *     <li><code>java.time.LocalTime</code></li>
 *     <li><code>java.time.LocalDate</code></li>
 *     <li><code>java.time.LocalDateTime</code></li>
 *     <li><code>java.time.Instant</code></li>
 * </ul>
 * extends conversion to the following types :
 * <ul>
 *     <li><code>java.lang.Long</code></li>
 *     <li><code>java.util.Date</code></li>
 *     <li><code>java.util.Calendar</code></li>
 * </ul>
 * @author honzel
 *
 */
@SuppressWarnings({"rawtypes"})
public class LocalDateTimeConverter extends AbstractConverter {


   public LocalDateTimeConverter() {
   }

   /**
     * For the type of registration and this converter is not responsible for conversion,
     * using the specified one as default converter.
     * @param defaultConverter the specified default converter, may be null.
    * @see AbstractConverter#AbstractConverter(Converter)
    */
   public LocalDateTimeConverter(Converter defaultConverter) {
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
   public LocalDateTimeConverter(Converter defaultConverter, boolean disableException) {
	   super(defaultConverter, disableException);
   }



    private DateTimeFormatter[] formatters;

    private Class<?>[] dateTypes;






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
    * @param patterns the specified patterns that types map to.
    */
    public void setPatterns(Class<?>[] types, String[] patterns) {
    	this.dateTypes  = types;
		formatters = new DateTimeFormatter[patterns.length];
		for (int i = 0; i < patterns.length; i++) {
			formatters[i] = LocalDateTimeUtils.getFormatter(patterns[i]);
		}
    }

	/**
	 * Convert an input Date/Calendar object into a String.
	 * @param value The input value to be converted
	 * @return the converted String value.
	 */
	protected String convertToString(Object value) throws ConversionException {
		if (value instanceof LocalDateTime || value instanceof LocalDate || value instanceof LocalTime || value instanceof Instant) {
			Class<?> type = value.getClass();
			int begin = beginIndexOfPatterns(type);
			DateTimeFormatter formatter;
			if (begin >= 0 && (formatter = formatters[begin]) != null) {
				return formatter.format((Temporal) value);
			}
			return value.toString();
		}
		return super.convertToString(value);
	}




    /**
     * Returns the first pattern index which the type to use.
     * @param type the specified date type.
     * @return
     */
	private int beginIndexOfPatterns(Class type) {
    	return nextIndexOfPatterns(type, NumberConstants.INTEGER_MINUS_ONE);
    }

	/**
     * Returns the next pattern index which match the type to use.
     * @param type the specified date type.
	 * @param previousIndex the previous pattern index which  the date type used.
	 * @return
	 */
	private int nextIndexOfPatterns(Class<?> type, int previousIndex) {
		if (formatters == null) {
			return NumberConstants.INTEGER_MINUS_ONE;
		}
		if (dateTypes == null || dateTypes.length <= previousIndex) {
			return formatters.length > ++previousIndex ? previousIndex : NumberConstants.INTEGER_MINUS_ONE;
		}
		if (type == null) {
			return NumberConstants.INTEGER_MINUS_ONE;
		}
		boolean matchNull = true;
		while (++previousIndex < dateTypes.length) {
			Class<?> dateType = dateTypes[previousIndex];
			if (dateType == null) {
				if (matchNull)
					break;
				matchNull = false;
			} else if (dateType.equals(type)) {
				break;
			} else {
				matchNull = false;
			}
		}
		return formatters.length > previousIndex ? previousIndex : NumberConstants.INTEGER_MINUS_ONE;
	}



	/**
	 * Parse a String date value using the set of patterns.
	 *
	 * @param text The String date value.
	 * @param toType the target type.
	 *
	 * @return The converted Date object.
	 */
	private Object parse(String text, Class<?> toType)  {
		if (TextUtils.isEmpty(text)) {
			return null;
		}
		LocalDateTime localDateTime;
		if (LocalDateTime.class.equals(toType)) {
			return Objects.nonNull(localDateTime = parseByFormatter(text, toType)) ? localDateTime : LocalDateTime.parse(text);
		}
		if (LocalDate.class.equals(toType)) {
			return Objects.nonNull(localDateTime = parseByFormatter(text, toType)) ? localDateTime.toLocalDate() : LocalDate.parse(text);
		}
		if (LocalTime.class.equals(toType)) {
			return Objects.nonNull(localDateTime = parseByFormatter(text, toType)) ? localDateTime.toLocalTime() : LocalTime.parse(text);
		}
		if (Instant.class.equals(toType)) {
			return Objects.nonNull(localDateTime = parseByFormatter(text, toType)) ? localDateTime.atZone(ZoneId.systemDefault()).toInstant() : Instant.parse(text);
		}
		return null;
	}

	private LocalDateTime parseByFormatter(String text, Class<?> toType) {
		int index = beginIndexOfPatterns(toType);
		ParsePosition pos = new ParsePosition(NumberConstants.INTEGER_ZERO);
		if (index >= 0) {
			do {
				try {
					DateTimeFormatter formatter = formatters[index];
					LocalDateTime time;
					if (formatter != null && (time = LocalDateTimeUtils.parse(text, pos, formatter)) != null) {
						return time;
					}
					index = nextIndexOfPatterns(toType, index);
				} catch (RuntimeException e) {
					throwException(e, null);
				}
				pos.setErrorIndex(NumberConstants.INTEGER_MINUS_ONE);
				pos.setIndex(NumberConstants.INTEGER_ZERO);
			} while (index >= 0);
		}
		LocalDate localDate  = LocalDateTimeUtils.parseDate(text, pos, DateTimeFormatter.ISO_LOCAL_DATE, "T ");
		if (LocalDate.class.equals(toType)) {
			return localDate != null ? LocalDateTime.of(localDate, LocalTime.MIN) : null;
		}
		if (localDate == null) {
			pos.setErrorIndex(NumberConstants.INTEGER_MINUS_ONE);
			pos.setIndex(NumberConstants.INTEGER_ZERO);
		}
		LocalTime localTime = LocalDateTimeUtils.parseTime(text, pos, DateTimeFormatter.ISO_LOCAL_TIME, TextUtils.EMPTY);
		if (pos.getErrorIndex() >= NumberConstants.INTEGER_ZERO || (localDate == null && localTime == null)) {
			return null;
		}
		return LocalDateTime.of(localDate != null ? localDate : LocalDateTimeUtils.EPOCH_DATE, localTime != null ? localTime : LocalTime.MIN);
	}

	protected Object convertToType(Object value, Class toType) throws ConversionException {
		Object firstValue = fetchFirst(value, toType);
		if (TextUtils.isEmpty(value)) {
			return getDefault(value, toType);
		}
		Object targetValue;
		if (firstValue instanceof CharSequence) {
			targetValue =  parse(firstValue.toString(), toType);
		} else if (firstValue instanceof Instant) {
			targetValue =  convertInstant((Instant) firstValue, toType, null);
		} else if (firstValue instanceof TemporalAccessor) {
			targetValue =  convertTemporal((TemporalAccessor) firstValue, toType);
		} else if (firstValue instanceof Date) {
			targetValue =  convertInstant(((Date) firstValue).toInstant(), toType, null);
		} else if (firstValue instanceof Calendar) {
			Calendar cal = (Calendar) firstValue;
			targetValue =  convertInstant(cal.toInstant(), toType, cal.getTimeZone() != null ? cal.getTimeZone().toZoneId() : null);
		} else if (firstValue instanceof Long) {
			targetValue =  convertInstant(Instant.ofEpochMilli((Long) firstValue), toType, null);
		} else {
			targetValue = null;
		}
		return targetValue != null ? targetValue : super.convertToType(value, toType);
	}



	private Object convertInstant(Instant instant, Class<?> toType, ZoneId zoneId) {
		if (toType.isAssignableFrom(instant.getClass())) {
			return instant;
		}
		if (zoneId == null) {
			zoneId = ZoneId.systemDefault();
		}
		if (LocalDateTime.class.equals(toType)) {
			return instant.atZone(zoneId).toLocalDateTime();
		}
		if (LocalDate.class.equals(toType)) {
			return instant.atZone(zoneId).toLocalDate();
		}
		if (LocalTime.class.equals(toType)) {
			return instant.atZone(zoneId).toLocalTime();
		}
		if (Long.class.equals(toType) || Long.TYPE.equals(toType)) {
			return instant.toEpochMilli();
		}
		if (Date.class.equals(toType)) {
			return Date.from(instant);
		}
		if (Calendar.class.equals(toType)) {
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
			cal.setTimeInMillis(instant.toEpochMilli());
			return cal;
		}
		return null;
	}

	private Object convertTemporal(TemporalAccessor temporal, Class toType) {
		if (toType.isInstance(temporal.getClass())) {
			return temporal;
		}
		if (LocalDate.class.equals(toType)) {
			return temporal.query(TemporalQueries.localDate());
		}
		if (LocalTime.class.equals(toType)) {
			return temporal.query(TemporalQueries.localTime());
		}
		if (LocalDateTime.class.equals(toType)) {
			LocalDate localDate = temporal.query(TemporalQueries.localDate());
			LocalTime localTime = temporal.query(TemporalQueries.localTime());
			if (localDate != null || localTime != null) {
				return LocalDateTime.of(localDate != null ? localDate : LocalDateTimeUtils.EPOCH_DATE, localTime != null ? localTime : LocalTime.MIN);
			} else {
				return null;
			}
		}
		if (Long.class.equals(toType) || Long.TYPE.equals(toType) || Instant.class.equals(toType)
				|| Calendar.class.equals(toType) || Date.class.equals(toType)) {
			long seconds;
			if (temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
				seconds = temporal.get(ChronoField.INSTANT_SECONDS);
			} else {
				if (temporal.isSupported(ChronoField.EPOCH_DAY)) {
					seconds = TimeUnit.DAYS.toSeconds(temporal.get(ChronoField.EPOCH_DAY));
				} else {
					seconds = 0L;
				}
				if (temporal.isSupported(ChronoField.SECOND_OF_DAY)) {
					seconds += temporal.get(ChronoField.SECOND_OF_DAY);
				}
			}
			long nano;
			if (temporal.isSupported(ChronoField.NANO_OF_SECOND)) {
				nano = temporal.get(ChronoField.NANO_OF_SECOND);
			} else {
				nano = 0L;
			}
			if (Instant.class.equals(toType)) {
				return Instant.ofEpochSecond(seconds, nano);
			}
			long time = TimeUnit.SECONDS.toMillis(seconds) + TimeUnit.NANOSECONDS.toMillis(nano);
			if (Date.class.equals(toType)) {
				return new Date(time);
			}
			if (Calendar.class.equals(toType)) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(time);
				return calendar;
			}
			return time;
		}
		return null;
	}



}
