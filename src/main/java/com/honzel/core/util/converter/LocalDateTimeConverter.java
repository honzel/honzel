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
 *     <li><code>java.lang.Integer</code></li>
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
				return formatter.format((TemporalAccessor) value);
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
			} else if (dateType.equals(type)) {
				break;
			} else {
				matchNull = false;
			}
		}
		return formatters.length > previousIndex ? previousIndex : NumberConstants.INTEGER_MINUS_ONE;
	}


    private LocalDateTime parseByFormatter(CharSequence text, Class<?> toType) {
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
			// 获取默认值
			return getDefault(value, toType);
		}
		Object targetValue;
		if (LocalDateTime.class.equals(toType)) {
			// LocalDateTime
			targetValue = convertToLocalDateTime(firstValue);
		} else if (LocalDate.class.equals(toType)) {
			// LocalDate
			targetValue =  convertToLocalDate(firstValue);
		} else if (LocalTime.class.equals(toType)) {
			// LocalTime
			targetValue =  convertToLocalTime(firstValue);
		} else if (Instant.class.equals(toType)) {
			// Instant
			targetValue =  convertToInstant(firstValue);

		} else if (value instanceof TemporalAccessor) {
			// 时间转其他
			targetValue = temporalToOther((TemporalAccessor) value, toType);
		} else {
			targetValue = null;
		}
		return targetValue != null ? targetValue : super.convertToType(value, toType);
	}

	private Instant convertToInstant(Object value) {
		if (value instanceof CharSequence) {
			LocalDateTime localDateTime;
			return Objects.nonNull(localDateTime = parseByFormatter((CharSequence) value, LocalDateTime.class)) ? localDateTime.atZone(ofZoneId(localDateTime)).toInstant() : Instant.parse((CharSequence) value);
		}
		if (value instanceof LocalDateTime) {
			return ((LocalDateTime) value).atZone(ofZoneId(value)).toInstant();
		}
		if (value instanceof LocalDate) {
			return ((LocalDate) value).atStartOfDay(ofZoneId(value)).toInstant();
		}
		if (value instanceof LocalTime) {
			return ((LocalTime) value).atDate(LocalDateTimeUtils.EPOCH_DATE).atZone(ofZoneId(value)).toInstant();
		}
		return ofInstant(value);
	}

	private Object temporalToOther(TemporalAccessor value, Class toType) {
		Instant instant;
		if (Long.class.equals(toType) || Long.TYPE.equals(toType)) {
			// Long
			if ((instant = convertToInstant(value)) != null) {
				return instant.toEpochMilli();
			}
		} else if (Integer.class.equals(toType) || Integer.TYPE.equals(toType)) {
			// Integer
			if ((instant = convertToInstant(value)) != null) {
				return instant.getEpochSecond();
			}
		} else if (Date.class.equals(toType)) {
			// Date
			if ((instant = ofInstant(value)) != null) {
				return Date.from(instant);
			}
		} else if (Calendar.class.equals(toType)) {
			// Calendar
			if ((instant = ofInstant(value)) != null) {
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(ofZoneId(value)));
				cal.setTimeInMillis(instant.toEpochMilli());
				return cal;
			}
		}
		return null;
	}


	private LocalTime convertToLocalTime(Object value) {
		if (value instanceof CharSequence) {
			LocalDateTime localDateTime;
			return Objects.nonNull(localDateTime = parseByFormatter((CharSequence) value, LocalTime.class)) ? localDateTime.toLocalTime() : LocalTime.parse((CharSequence) value);
		}
		LocalTime localTime;
		if (value instanceof TemporalAccessor && Objects.nonNull(localTime = ((TemporalAccessor) value).query(TemporalQueries.localTime()))) {
			return localTime;
		}
		Instant instant = ofInstant(value);
		if (instant != null) {
			ZoneOffset offset = ofZoneId(value).getRules().getOffset(instant);
			return LocalTime.ofNanoOfDay(TimeUnit.SECONDS.toNanos((instant.getEpochSecond() + (long)offset.getTotalSeconds()) % TimeUnit.DAYS.toSeconds(1L)) + instant.getNano());
		}
		return null;
	}

	private LocalDate convertToLocalDate(Object value) {
		if (value instanceof CharSequence) {
			LocalDateTime localDateTime;
			return Objects.nonNull(localDateTime = parseByFormatter((CharSequence) value, LocalDate.class)) ? localDateTime.toLocalDate() : LocalDate.parse((CharSequence) value);
		}
		LocalDate localDate;
		if (value instanceof TemporalAccessor && Objects.nonNull(localDate = ((TemporalAccessor) value).query(TemporalQueries.localDate()))) {
			return localDate;
		}
		Instant instant = ofInstant(value);
		if (instant != null) {
			ZoneOffset offset = ofZoneId(value).getRules().getOffset(instant);
			return LocalDate.ofEpochDay((instant.getEpochSecond() + (long)offset.getTotalSeconds()) / TimeUnit.DAYS.toSeconds(1L));
		}
		return null;
	}

	private ZoneId ofZoneId(Object value) {
		ZoneId zoneId = null;
		if (value instanceof TemporalAccessor) {
			TemporalAccessor temporal = (TemporalAccessor) value;
			zoneId =  temporal.query(TemporalQueries.zone());
		} else if (value instanceof Calendar) {
			zoneId = ((Calendar) value).getTimeZone().toZoneId();
		}
		return zoneId != null ? zoneId : ZoneId.systemDefault();
	}

	private LocalDateTime convertToLocalDateTime(Object value) {
		LocalDateTime localDateTime;
		if (value instanceof CharSequence) {
			return Objects.nonNull(localDateTime = parseByFormatter((CharSequence) value, LocalDateTime.class)) ? localDateTime : LocalDateTime.parse((CharSequence) value);
		}
		if (value instanceof TemporalAccessor) {
			TemporalAccessor temporal = (TemporalAccessor) value;
			LocalDate localDate = temporal.query(TemporalQueries.localDate());
			LocalTime localTime = temporal.query(TemporalQueries.localTime());
			if (localDate != null || localTime != null) {
				return LocalDateTime.of(localDate != null ? localDate : LocalDateTimeUtils.EPOCH_DATE, localTime != null ? localTime : LocalTime.MIN);
			}
		}
		Instant instant = ofInstant(value);
		return instant != null ? LocalDateTime.ofInstant(instant, ofZoneId(value)) : null;
	}

	private Instant ofInstant(Object value) {

		if (value instanceof Instant) {
			return  (Instant) value;
		}
		if (value instanceof Date) {
			return  ((Date) value).toInstant();
		}
		if (value instanceof Calendar) {
			return  ((Calendar) value).toInstant();
		}
		if (value instanceof Long) {
			return Instant.ofEpochMilli((Long) value);
		}
		if (value instanceof Integer) {
			return Instant.ofEpochSecond((Integer) value);
		}
		if (value instanceof TemporalAccessor) {
			TemporalAccessor temporal = (TemporalAccessor) value;
			long seconds;
			boolean supported;
			if (supported = temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
				seconds = temporal.getLong(ChronoField.INSTANT_SECONDS);
			} else {
				if (supported = temporal.isSupported(ChronoField.EPOCH_DAY)) {
					seconds = TimeUnit.DAYS.toSeconds(temporal.getLong(ChronoField.EPOCH_DAY));
				} else {
					seconds = 0L;
				}
				if (temporal.isSupported(ChronoField.SECOND_OF_DAY)) {
					seconds += temporal.getLong(ChronoField.SECOND_OF_DAY);
					supported = true;
				}
			}
			if (supported) {
				long nano;
				if (temporal.isSupported(ChronoField.NANO_OF_SECOND)) {
					nano = temporal.getLong(ChronoField.NANO_OF_SECOND);
				} else {
					nano = 0L;
				}
				return Instant.ofEpochSecond(seconds, nano);
			}
		}
		return null;
	}


}
