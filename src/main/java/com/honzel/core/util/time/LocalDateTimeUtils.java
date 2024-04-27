package com.honzel.core.util.time;

import com.honzel.core.constant.NumberConstants;
import com.honzel.core.util.text.TextUtils;

import java.text.ParsePosition;
import java.time.*;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.temporal.*;
import java.util.*;

/**
 * 日期时间工具类
 * @author honzel
 * date 2022/1/4
 */
public class LocalDateTimeUtils {

    /**
     * 日期时间格式化
     */
    public static final String DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    /**
     * 日期格式化
     */
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    /**
     * 时间格式化
     */
    public static final String TIME_FORMAT_PATTERN = "HH:mm:ss";
    /**
     * 时间格式化
     */
    public static final String HOUR_MINUTE_FORMAT_PATTERN = "HH:mm";

    /**
     * 日期时间格式化
     */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN);
    /**
     * 日期格式化
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
    /**
     * 时间格式化
     */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT_PATTERN);
    /**
     * 时分格式化
     */
    public static final DateTimeFormatter HOUR_MINUTE_FORMATTER = DateTimeFormatter.ofPattern(HOUR_MINUTE_FORMAT_PATTERN);
    // 类型
    private static final Map<TemporalUnit, List<TemporalField>> BASE_UNIT_FIELD_LIST_MAP;
    // 解析标准单位
    private static final TemporalUnit[] UNITS = Arrays.copyOf(ChronoUnit.values(), ChronoUnit.FOREVER.ordinal());
    public static final LocalDate EPOCH_DATE = LocalDate.of(1970, 1, 1);

    public static final LocalTime MAX_SECOND_TIME = LocalTime.of(23, 59, 59);

    static {
        BASE_UNIT_FIELD_LIST_MAP = new HashMap<>();
        ChronoField[] fields = ChronoField.values();
        for (int i = ChronoField.ERA.ordinal(); i >= 0; i--) {
            BASE_UNIT_FIELD_LIST_MAP.computeIfAbsent(fields[i].getBaseUnit(), u -> new ArrayList<>()).add(fields[i]);
        }
        // iso时间补充
        BASE_UNIT_FIELD_LIST_MAP.computeIfAbsent(IsoFields.DAY_OF_QUARTER.getBaseUnit(), u -> new ArrayList<>()).add(IsoFields.DAY_OF_QUARTER);
        //
        BASE_UNIT_FIELD_LIST_MAP.computeIfAbsent(IsoFields.WEEK_BASED_YEAR.getBaseUnit(), u -> new ArrayList<>()).add(IsoFields.WEEK_BASED_YEAR);
        //
        BASE_UNIT_FIELD_LIST_MAP.computeIfAbsent(IsoFields.WEEK_OF_WEEK_BASED_YEAR.getBaseUnit(), u -> new ArrayList<>()).add(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        //
        BASE_UNIT_FIELD_LIST_MAP.computeIfAbsent(IsoFields.QUARTER_OF_YEAR.getBaseUnit(), u -> new ArrayList<>()).add(IsoFields.QUARTER_OF_YEAR);
        // julian时间补充
        BASE_UNIT_FIELD_LIST_MAP.computeIfAbsent(JulianFields.JULIAN_DAY.getBaseUnit(), u -> new ArrayList<>()).add(JulianFields.JULIAN_DAY);
        // julian时间补充
        BASE_UNIT_FIELD_LIST_MAP.computeIfAbsent(JulianFields.MODIFIED_JULIAN_DAY.getBaseUnit(), u -> new ArrayList<>()).add(JulianFields.MODIFIED_JULIAN_DAY);
        // julian时间补充
        BASE_UNIT_FIELD_LIST_MAP.computeIfAbsent(JulianFields.RATA_DIE.getBaseUnit(), u -> new ArrayList<>()).add(JulianFields.RATA_DIE);

    }

    protected LocalDateTimeUtils() {
    }

    /**
     * 格式化日期
     * @param temporal 时间
     * @param pattern 模板
     * @return 返回格式化后结果
     */
    public static String format(TemporalAccessor temporal, String pattern) {
        DateTimeFormatter formatter;
        return (formatter = getFormatter(pattern)) != null ? formatter.format(temporal) : pattern;
    }

    /**
     * 获取模板对应格式化器
     * @param pattern 模板内容
     * @return 格式化对象
     */
    public static DateTimeFormatter getFormatter(String pattern) {
        if (TextUtils.isEmpty(pattern)) {
            return null;
        }
        switch (pattern) {
            case DATE_TIME_FORMAT_PATTERN:
                return DATE_TIME_FORMATTER;
            case DATE_FORMAT_PATTERN:
                return DATE_FORMATTER;
            case TIME_FORMAT_PATTERN:
                return TIME_FORMATTER;
            case HOUR_MINUTE_FORMAT_PATTERN:
                return HOUR_MINUTE_FORMATTER;
            default:
                return DateTimeFormatter.ofPattern(pattern);
        }
    }

    /**
     * 解析时间
     * @param input 输入文本
     * @param position 解析位置, 如果为null 默认从0开始
     * @param formatter 时间格式对象
     * @param endDelimiters 验证结束符, 为null时表示必须完全匹配格式, 为非空字符集时表示部分解析的结束分隔符必须为该结束符否则返回null
     * @return 返回解析结果
     */
    private static TemporalAccessor parse0(CharSequence input, ParsePosition position, DateTimeFormatter formatter, String endDelimiters) {
        if (input == null || input.length() == 0 || (position != null && position.getIndex() >= input.length())) {
            // 没有解析内容
            return null;
        }
        if (position == null) {
            position = new ParsePosition(0);
        }
        // 解析日期时间
        TemporalAccessor parsed = formatter.parseUnresolved(input, position);
        if (parsed == null || position.getErrorIndex() >= 0) {
            // 解析文本格式错误
            return null;
        }
        if (position.getIndex() < input.length()) {
            if (endDelimiters == null) {
                // 结束符为null时必须匹配到输入串到最后
                return null;
            }
            if (!endDelimiters.isEmpty()) {
                if (endDelimiters.indexOf(input.charAt(position.getIndex())) < 0) {
                    // 结束字符有误
                    return null;
                }
                position.setIndex(position.getIndex() + 1);
            }
        }
        return parsed;
    }

    private static ZoneId getZoneId(TemporalAccessor parsed, DateTimeFormatter formatter) {
        ZoneId zoneId = parsed.query(TemporalQueries.zone());
        if (zoneId == null) {
            zoneId = formatter.getZone();
            if (zoneId == null) {
                zoneId = parsed.query(TemporalQueries.offset());
            }
        }
        return zoneId != null ? zoneId : ZoneId.systemDefault();
    }

    /**
     * 解析日期
     * @param input 输入文本
     * @param position 解析位置
     * @param formatter 时间格式对象
     * @param endDelimiters 验证结束符, 为null时表示必须完全匹配格式, 为非空字符集时表示部分解析的结束分隔符必须为该结束符否则返回null
     * @return 返回解析结果
     */
    public static LocalDate parseDate(CharSequence input, ParsePosition position, DateTimeFormatter formatter, String endDelimiters) {
        TemporalAccessor parsed = parse0(input, position, formatter, endDelimiters);
        LocalDateTime dateTime = parseFromInstant(parsed, formatter);
        return dateTime != null ? dateTime.toLocalDate() : resolveDate(parsed, formatter);
    }


    private static Chronology getChronology(TemporalAccessor parsed, DateTimeFormatter formatter) {
        Chronology chronology = parsed.query(TemporalQueries.chronology());
        if (chronology == null) {
            chronology = formatter.getChronology();
        }
        return chronology == null ? IsoChronology.INSTANCE : chronology;
    }


    /**
     * 解析日期
     * @param input 输入文本
     * @param position 解析位置
     * @param formatter 时间格式对象
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalDate parseDate(CharSequence input, ParsePosition position, DateTimeFormatter formatter) {
        return parseDate(input, position, formatter, null);
    }
    /**
     * 解析日期
     * @param input 输入文本
     * @param formatter 时间格式对象
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalDate parseDate(CharSequence input, DateTimeFormatter formatter) {
        return parseDate(input, null, formatter, null);
    }

    /**
     * 按格式yyyy-MM-dd解析日期
     * @param input 输入文本
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalDate parseDate(CharSequence input) {
        return parseDate(input, null, DATE_FORMATTER, null);
    }

    /**
     * 解析时间
     * @param input 输入文本
     * @param position 解析位置
     * @param formatter 时间格式对象
     * @param endDelimiters 验证结束符, 为null时表示必须完全匹配格式, 为非空字符集时表示部分解析的结束分隔符必须为该结束符否则返回null
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalTime parseTime(CharSequence input, ParsePosition position, DateTimeFormatter formatter, String endDelimiters) {
        TemporalAccessor parsed = parse0(input, position, formatter, endDelimiters);
        LocalDateTime dateTime = parseFromInstant(parsed, formatter);
        return dateTime != null ? dateTime.toLocalTime() : resolveTime(parsed, formatter);
    }

    private static LocalDate resolveDate(TemporalAccessor parsed, DateTimeFormatter formatter) {
        if (parsed == null) {
            return null;
        }
        // 获取开始域名
        TemporalField dateField = getFirstField(parsed, formatter, ChronoUnit.DAYS.ordinal(), UNITS.length);
        if (dateField == null) {
            // 找不到日期单位, 返回默认
            return null;
        }
        Map<TemporalField, Long> fieldValues = new HashMap<>(NumberConstants.INTEGER_SEVEN);
        for (int i = ChronoUnit.DAYS.ordinal(); i < UNITS.length; i++) {
            TemporalField field = getField(parsed, UNITS[i], formatter);
            if (field != null && field.isDateBased()) {
                long value = parsed.getLong(field);
                if (!field.range().isValidValue(value)) {
                    return null;
                }
                fieldValues.put(field, value);
            }
        }
        TemporalUnit unit;
        while (!Objects.equals(unit = dateField.getRangeUnit(), dateField.getBaseUnit()) && (dateField = getField(parsed, unit, formatter)) != null) {
            if (fieldValues.containsKey(dateField)) {
                continue;
            }
            long value = parsed.getLong(dateField);
            if (!dateField.range().isValidValue(value)) {
                return null;
            }
        }
        // 解析日期
        ChronoLocalDate localDate;
        try {
            localDate = getChronology(parsed, formatter).resolveDate(fieldValues, formatter.getResolverStyle());
        } catch (Exception e) {
            e.printStackTrace();
            localDate = null;
        }
        if (localDate == null && !fieldValues.isEmpty()) {
            localDate = EPOCH_DATE;
            for (Map.Entry<TemporalField, Long> entry : fieldValues.entrySet()) {
                localDate = localDate.with(entry.getKey(), entry.getValue());
            }
        }
        // 返回localDate
        return localDate != null ? (localDate instanceof LocalDate ? (LocalDate) localDate : localDate.query(TemporalQueries.localDate())) : null;
    }

    private static LocalTime resolveTime(TemporalAccessor parsed, DateTimeFormatter formatter) {
        if (parsed == null) {
            // 解析格式有误
            return null;
        }
        // 获取秒下的时间
        TemporalField subSecondField = getFirstField(parsed, formatter, 0, ChronoUnit.SECONDS.ordinal());
        if (subSecondField != null && ChronoUnit.DAYS.equals(subSecondField.getRangeUnit())) {
            // 如果是按天获取钠秒
            return LocalTime.ofNanoOfDay(getNanoSeconds(parsed, subSecondField));
        }
        // 获取秒级时间字段
        TemporalField timeField = getFirstField(parsed, formatter, ChronoUnit.SECONDS.ordinal(), ChronoUnit.DAYS.ordinal());
        if (timeField == null) {
            // 如果没有秒级以上时间
            return subSecondField != null ? LocalTime.ofNanoOfDay(getNanoSeconds(parsed, subSecondField)) : null;
        }
        // 获取秒数
        TemporalUnit unit = timeField.getBaseUnit();
        long secondsOfDay = 0L;
        do {
            long value = parsed.getLong(timeField);
            ValueRange range;
            if (!(range = timeField.range()).isValidValue(value)) {
                return null;
            }
            secondsOfDay += getTimeSeconds(value, unit, range);
        } while (!Objects.equals(unit = timeField.getRangeUnit(), timeField.getBaseUnit()) && (timeField = getField(parsed, unit, formatter)) != null && !unit.isDateBased());

        // 获取
        int scale = subSecondField != null ? (int) subSecondField.getRangeUnit().getDuration().getSeconds() : 0;
        if (scale > 1) {
            secondsOfDay = (secondsOfDay / scale) * scale;
        }
        // 只取一天以内的时间
        secondsOfDay %= ChronoUnit.DAYS.getDuration().getSeconds();
        // 获取钠秒数
        long nanoSeconds = getNanoSeconds(parsed, subSecondField);
        LocalTime localTime;
        if (nanoSeconds > 0) {
            localTime = LocalTime.ofNanoOfDay(secondsOfDay * ChronoUnit.SECONDS.getDuration().toNanos() + nanoSeconds);
        } else {
            localTime = LocalTime.ofSecondOfDay(secondsOfDay);
        }
        if (timeField == null && unit instanceof ChronoUnit && unit.isTimeBased()) {
            // 添加跳级时间解析
            return withSkipTime(localTime, (ChronoUnit) unit, parsed, formatter);
        }
        return localTime;
    }

    private static LocalTime withSkipTime(LocalTime localTime, ChronoUnit unit, TemporalAccessor parsed, DateTimeFormatter formatter) {
        // 如果时间解析中断, 从中断之后再重新解析
        for (int i = unit.ordinal() + 1; i < ChronoUnit.DAYS.ordinal(); ++i) {
            TemporalField field = getField(parsed, UNITS[i], formatter);
            if (field == null) {
                continue;
            }
            long value = parsed.getLong(field);
            if (!field.range().isValidValue(value)) {
                return null;
            }
            localTime = localTime.with(field, value);
        }
        return localTime;
    }

    private static long getTimeSeconds(long value, TemporalUnit unit, ValueRange range) {
        long minimum = range.getMinimum();
        if (minimum > 0) {
            long maximum = range.getMaximum();
            if (value > maximum - minimum) {
                value -= (maximum - minimum + 1);
            }
        }
        return value * unit.getDuration().getSeconds();
    }

    private static long getNanoSeconds(TemporalAccessor parsed, TemporalField field) {
        if (field == null) {
            return 0L;
        }
        long value = parsed.getLong(field);
        if (!field.range().isValidValue(value)) {
            return 0;
        }
        long nano = field.getBaseUnit().getDuration().toNanos();
        return value * nano;
    }

    private static TemporalField getFirstField(TemporalAccessor parsed, DateTimeFormatter formatter, int start, int end) {
        if (parsed == null) {
            return null;
        }
        for (int i = start; i < end; ++i) {
            TemporalField field = getField(parsed, UNITS[i], formatter);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    /**
     * 解析时间
     * @param input 输入文本
     * @param position 解析位置
     * @param formatter 时间格式对象
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalTime parseTime(CharSequence input, ParsePosition position, DateTimeFormatter formatter) {
        return parseTime(input, position, formatter, null);
    }

    /**
     * 解析时间
     * @param input 输入文本
     * @param formatter 时间格式对象
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalTime parseTime(CharSequence input, DateTimeFormatter formatter) {
        return parseTime(input, null, formatter, null);
    }
    /**
     * 按格式HH:mm:ss解析时间
     * @param input 输入文本
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalTime parseTime(CharSequence input) {
        return parseTime(input, null, TIME_FORMATTER, null);
    }

    /**
     * 解析日期时间
     * @param input 输入文本
     * @param position 解析位置
     * @param formatter 时间格式对象
     * @param endDelimiters 验证结束符, 为null时表示必须完全匹配格式, 为非空字符集时表示部分解析的结束分隔符必须为该结束符否则返回null
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalDateTime parse(CharSequence input, ParsePosition position, DateTimeFormatter formatter, String endDelimiters) {
        // 解析时间
        TemporalAccessor parsed = parse0(input, position, formatter, endDelimiters);
        //
        LocalDateTime dateTime = parseFromInstant(parsed, formatter);
        if (dateTime != null) {
            return dateTime;
        }
        LocalDate date = resolveDate(parsed, formatter);
        LocalTime time = resolveTime(parsed, formatter);
        if (date != null || time != null) {
            return LocalDateTime.of(date != null ? date : EPOCH_DATE, time != null ? time : LocalTime.MIN);
        }
        // 日期数字无效
        return null;
    }

    private static LocalDateTime parseFromInstant(TemporalAccessor parsed, DateTimeFormatter formatter) {
        if (parsed != null && parsed.isSupported(ChronoField.INSTANT_SECONDS)) {
            // 获取秒数
            long epochSeconds = parsed.getLong(ChronoField.INSTANT_SECONDS);
            // 获取秒下的时间
            TemporalField subSecondField = getFirstField(parsed, formatter, 0, ChronoUnit.SECONDS.ordinal());
            // 获取
            int scale = subSecondField != null ? (int) subSecondField.getRangeUnit().getDuration().getSeconds() : 0;
            if (scale > 1) {
                epochSeconds = (epochSeconds / scale) * scale;
            }
            // 获取钠秒数
            long nanoSeconds = getNanoSeconds(parsed, subSecondField);
            // 获取实例时间
            Instant instant;
            if (nanoSeconds > 0) {
                instant = Instant.ofEpochSecond(epochSeconds, nanoSeconds);
            } else {
                instant = Instant.ofEpochSecond(epochSeconds);
            }
            return LocalDateTime.ofInstant(instant, getZoneId(parsed, formatter));
        }
        return null;
    }

    /**
     * 解析日期时间
     * @param input 输入文本
     * @param position 解析位置
     * @param formatter 时间格式对象
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalDateTime parse(CharSequence input, ParsePosition position, DateTimeFormatter formatter) {
        return parse(input, position, formatter, null);
    }
    /**
     * 解析日期时间
     * @param input 输入文本
     * @param formatter 时间格式对象
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalDateTime parse(CharSequence input, DateTimeFormatter formatter) {
        return parse(input, null, formatter, null);
    }
    /**
     * 解析日期时间
     * @param input 输入文本
     * @return 返回解析结果, 如果解析错误返回null
     */
    public static LocalDateTime parse(CharSequence input) {
        return parse(input, null, DATE_TIME_FORMATTER, null);
    }


    private static TemporalField getField(TemporalAccessor temporalAccessor, TemporalUnit unit, DateTimeFormatter formatter) {
        if (unit == null || ChronoUnit.FOREVER.equals(unit)) {
            return null;
        }
        List<TemporalField> list = BASE_UNIT_FIELD_LIST_MAP.getOrDefault(unit, Collections.emptyList());
        if (list.isEmpty()) {
            return null;
        }
        Set<TemporalField> resolverFields = formatter.getResolverFields();
        for (TemporalField field : list) {
            if (temporalAccessor.isSupported(field) && (resolverFields == null || resolverFields.contains(field))) {
                return field;
            }
        }
        return null;
    }



    /**
     * 安全获取时间的某个属性，属性不存在返回最小值
     *
     * @param temporalAccessor 需要获取的时间对象
     * @param field            需要获取的属性
     * @return 时间的值，如果无法获取则默认为 0
     */
    public static int get(TemporalAccessor temporalAccessor, TemporalField field) {
        if (temporalAccessor.isSupported(field)) {
            return temporalAccessor.get(field);
        }
        return (int)field.range().getMinimum();
    }


}
