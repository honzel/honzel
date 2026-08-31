package com.honzel.core.util.time;

import com.honzel.core.util.text.TextUtils;

import javax.annotation.PostConstruct;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 时间段值转换
 * @author honzel
 * date 2021/1/4
 */

@SuppressWarnings("unchecked")
public class TimeRangeUtils {
    /**
     * 无设置值
     */
    public static final long NONE = 0L;

    private static final long FIRST_BIT = 1L;


    /**
     * 全部时间
     */
    private static final int TIME_BITS = 48;
    public static final long ALL_TIMES = ~(-FIRST_BIT << TIME_BITS);

    /**
     * 日期开始时间
     */
    private static final int START_TIME_BITS = 6;
    private static final long DATE_START_TIME = ~(-FIRST_BIT << START_TIME_BITS) << TIME_BITS;

    /**
     * 全部日期
     */
    private static final int WEEKDAY_BITS = 7;
    public static final long ALL_WEEKDAYS = ~(-FIRST_BIT << WEEKDAY_BITS) << (TIME_BITS + START_TIME_BITS);
    /**
     * 班次时间标识
     */
    public static final long SHIFT_TIME_FLAG = FIRST_BIT << (TIME_BITS + START_TIME_BITS + WEEKDAY_BITS);

    private static final int TIME_UNIT_IN_MINUTES = 30;

    private static volatile TimeRangeUtils utils;

    protected TimeRangeUtils() {}

    @PostConstruct
    protected void init() {
        synchronized (TimeRangeUtils.class) {
            utils = this;
        }
    }

    private static TimeRangeUtils getInstance() {
        if (utils == null) {
            synchronized (TimeRangeUtils.class) {
                if (utils == null) {
                    new TimeRangeUtils().init();
                }
            }
        }
        return utils;
    }

    /**
     * 获取时间范围列表
     * @param timeRangeStamp 时间段值
     * @return 返回时间段列表
     */
    public static<TimeRange extends com.honzel.core.util.time.TimeRange> List<TimeRange> getTimeRanges(long timeRangeStamp) {
        return getTimeRanges(timeRangeStamp, 0, false);
    }
    /**
     * 获取时间范围列表
     * @param timeRangeStamp 时间段值
     * @param divisionDuration 切割时长（单位为分钟)
     * @return 返回拆分后的时间段列表
     */
    public static<TimeRange extends com.honzel.core.util.time.TimeRange> List<TimeRange> getTimeRanges(long timeRangeStamp, int divisionDuration) {
        return getTimeRanges(timeRangeStamp, divisionDuration, false);
    }

    /**
     * 获取时间范围列表
     * @param timeRangeStamp 时间段值
     * @param divisionDuration 切割时长（单位为分钟), 0为不切割
     * @param halfDivisionDurationEnabled 是否步长为一半切割时长, true-步长为切割时长的一半, false-步长与切割时长相等
     * @return 返回拆分后的时间段列表
     */
    public static<TimeRange extends com.honzel.core.util.time.TimeRange> List<TimeRange> getTimeRanges(long timeRangeStamp, int divisionDuration, boolean halfDivisionDurationEnabled) {
        long times;
        if (timeRangeStamp == NONE || (times = timeRangeStamp & ALL_TIMES) == NONE) {
            return Collections.emptyList();
        }
        //是否班次时间
        boolean shiftFlag = (timeRangeStamp & SHIFT_TIME_FLAG) != NONE;
        List<TimeRange> timeRangeList = new ArrayList<>();
        TimeRange timeRange = null;
        // 日期起始位
        int offset = getOffsetIndex(timeRangeStamp);
        if (offset > 0) {
            times = (times >>> offset) | ((~(-FIRST_BIT << offset) & times) << (TIME_BITS - offset));
        }
        int adjIndex = 0;
        for (int i = 0; i < TIME_BITS; i ++, times >>>= 1) {
            if ((times & FIRST_BIT) == NONE) {
                if (timeRange != null) {
                    int seq = (offset + i) % TIME_BITS;
                    if (shiftFlag) {
                        timeRange.setEndTime(i == TIME_BITS - 1 && seq == i ? LocalTime.MAX : parseTime(seq + 1));
                    } else {
                        timeRange.setEndTime(parseTime(seq));
                    }
                    // 按切割时长拆分时间段
                    divideTimeRange(timeRange, timeRangeList, divisionDuration, halfDivisionDurationEnabled);
                    timeRange = null;
                }
                if (times == NONE) {
                    break;
                }
            } else {
                if (timeRange == null) {
                    timeRange = (TimeRange) getInstance().newTimeRange();
                    timeRange.setStartTime(parseTime((offset + i) % TIME_BITS));
                    timeRangeList.add(timeRange);
                }
            }
        }
        if (timeRange != null) {
            if (offset == 0) {
                // 如果结束时间为一天的最后，则设置当天最大值
                timeRange.setEndTime(LocalTime.MAX);
            } else {
                // 跨天时
                timeRange.setEndTime(parseTime(offset));
            }
            // 按切割时长拆分时间段
            divideTimeRange(timeRange, timeRangeList, divisionDuration, halfDivisionDurationEnabled);
        }
        return timeRangeList;
    }


    protected TimeRange newTimeRange() {
        return new TimeRange();
    }

    /**
     * 创建时间段信息
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段对象
     */
    public static<T extends  TimeRange> T createTimeRange(LocalTime startTime, LocalTime endTime) {
        T timeRange = (T) getInstance().newTimeRange();
        timeRange.setStartTime(startTime);
        timeRange.setEndTime(endTime);
        return timeRange;
    }

    /**
     * 按切割时长拆分时间段
     * @param timeRange 准备被切割拆分的时间段
     * @param timeRangeList 时间段列表
     * @param divisionDuration 切割时长（单位为分钟)
     * @param halfDivisionDurationEnabled 是否步长为一半切割时长, true-步长为切割时长的一半, false-步长与切割时长相等
     */
    private static<T extends TimeRange> void divideTimeRange(TimeRange timeRange, List<T> timeRangeList, int divisionDuration, boolean halfDivisionDurationEnabled) {
        if (divisionDuration < 1) {
            // 切割时长小于1时不切割
            return;
        }
        if (halfDivisionDurationEnabled && divisionDuration < 2) {
            // 如果小于半切割最小单位时按false处理
            halfDivisionDurationEnabled = false;
        }
        // 步长时长
        int stepDuration = halfDivisionDurationEnabled ? divisionDuration / 2 : divisionDuration;
        // 开始时间及结束时间
        LocalTime startTime = timeRange.getStartTime();
        LocalTime endTime = timeRange.getEndTime();
        // 计算总时间段数
        int count = calcTotalCount(startTime, endTime, stepDuration, halfDivisionDurationEnabled);
        // 前一个时间段
        TimeRange prevRange = timeRange;
        // 拆分时间段
        for (int i = 1; i < count; ++ i) {
            // 子时间段
            T subRange = (T) getInstance().newTimeRange();
            // 计算开始时间
            subRange.setStartTime(startTime.plusMinutes((long) i * stepDuration));
            if (halfDivisionDurationEnabled) {
                // 结束时间按切割时长处理
                prevRange.setEndTime(prevRange.getStartTime().plusMinutes(divisionDuration));
            } else {
                // 后一段开始时间点作为上一段的结束时间点
                prevRange.setEndTime(subRange.getStartTime());
            }
            // 将时间段添加入结果
            timeRangeList.add(subRange);
            //
            prevRange = subRange;
        }
        //结束时间
        prevRange.setEndTime(endTime);
    }

    private static int calcTotalCount(LocalTime startTime, LocalTime endTime, int stepDuration, boolean halfDivisionDurationEnabled) {
        // 计算总时长
        if (LocalTime.MAX.equals(endTime)) {
            endTime  = LocalTime.MIN;
        }
        int maxDuration = (int) ChronoUnit.MINUTES.between(startTime, endTime);
        if (maxDuration <= 0) {
            // 跨天时
            maxDuration = (int) (ChronoUnit.DAYS.getDuration().toMinutes() + maxDuration);
        }
        // 计算总时间段数
        int count = maxDuration / stepDuration;
        if (maxDuration % stepDuration == 0) {
            if (halfDivisionDurationEnabled) {
                count--;
            }
        } else {
            if (!halfDivisionDurationEnabled) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取最起始时间点
     * @param timeRangeStamp 时间段值
     * @return 返回最起始时间点
     */
    public static LocalTime getFirstStartTime(long timeRangeStamp) {
        if (timeRangeStamp != NONE) {
            int offset = getOffsetIndex(timeRangeStamp);
            for (int i = 0; i < TIME_BITS; i ++) {
                int seq = (offset + i) % TIME_BITS;
                if ((timeRangeStamp & (FIRST_BIT << seq)) != NONE) {
                    return parseTime(seq);
                }
            }
        }
        return LocalTime.MIN;
    }
    /**
     * 获取最后结束时间点
     * @param timeRangeStamp 时间段值
     * @return 返回最后结束时间点
     */
    public static LocalTime getLastEndTime(long timeRangeStamp) {
        if (timeRangeStamp != NONE) {
            //是否班次时间
            boolean shiftFlag = (timeRangeStamp & SHIFT_TIME_FLAG) != NONE;
            // 开始时间位置
            int offset = getOffsetIndex(timeRangeStamp);
            for (int i = TIME_BITS - 1; i >= 0; i --) {
                int seq = (offset + i) % TIME_BITS;
                if ((timeRangeStamp & (FIRST_BIT << seq)) != NONE) {
                    if (shiftFlag) {
                        ++ seq;
                    }
                    if (seq == TIME_BITS - 1) {
                        return LocalTime.MAX;
                    } else {
                        return parseTime(seq + 1);
                    }
                }
            }
        }
        return LocalTime.MIN;
    }

    /**
     * 是否时间有跨天
     * @param timeRangeStamp 时间段值
     * @return true代表跨天, false代表不跨天
     */
    public static boolean isTimeCrossDate(long timeRangeStamp) {
        return getOffsetIndex(timeRangeStamp) != 0;
    }

    /**
     * 抹除班次信息
     * @param timeRangeStamp 时间段值
     * @return 抹除班次信息的时间段值
     */
    public static long nonShift(long timeRangeStamp) {
        if (timeRangeStamp == NONE) {
            return NONE;
        }
        long result = ~SHIFT_TIME_FLAG & timeRangeStamp;
        if (result != timeRangeStamp) {
            //如果是班次时间
            long time = (result & ALL_TIMES);
            time |= (time << 1);
            if (time != (time = time & ALL_TIMES)) {
                time |= FIRST_BIT;
            }
            return result | time;
        }
        return result;
    }
    /**
     * 时间段内是否包含有该时间
     * @param timeRangeStamp  时间段值
     * @param time 指定的时间
     * @return 是否时间段值包含该时间
     */
    public static boolean containsDateTime(long timeRangeStamp, LocalDateTime time) {
        return containsDay(timeRangeStamp, time.toLocalDate()) && containsTime(timeRangeStamp, time.toLocalTime());
    }
    /**
     * 是否包含指定日期
     * @param timeRangeStamp  时间段值
     * @param date 指定日期
     * @return 是否包含指定日期
     */
    public static boolean containsDay(long timeRangeStamp, LocalDate date) {
        if (timeRangeStamp != NONE) {
            // 获取星期几
            int day = date.getDayOfWeek().ordinal();
            return ((timeRangeStamp >>> (TIME_BITS + START_TIME_BITS + day)) & FIRST_BIT) != NONE;
        }
        return false;
    }
    /**
     * 时间段内是否包含有该时间
     * @param timeRangeStamp  时间段值
     * @param time 指定的时间
     * @return 是否时间段值包含该时间
     */
    public static boolean containsTime(long timeRangeStamp, LocalTime time) {
        return (nonShift(timeRangeStamp) & fromTime(time)) != NONE;
    }

    /**
     * 时间段内是否包含有该时间
     * @param timeRangeStamp  时间段值
     * @param timeRange 时间段
     * @return 是否时间段值包含该时间段
     */
    public static boolean containsTimeRange(long timeRangeStamp, TimeRange timeRange) {
        return timeRange != null && containsTimeRange(timeRangeStamp, timeRange.getStartTime(), timeRange.getEndTime());
    }
    /**
     * 时间段内是否包含有该时间
     * @param timeRangeStamp  时间段值
     * @param startTime 时间段开始时间点
     * @param endTime 时间段结束时间
     * @return 是否时间段值包含该时间段
     */
    public static boolean containsTimeRange(long timeRangeStamp, LocalTime startTime, LocalTime endTime) {
        if (timeRangeStamp == NONE || startTime == null || endTime == null) {
            return false;
        }
        //是否班次时间
        boolean shiftFlag = (timeRangeStamp & SHIFT_TIME_FLAG) != NONE;
        //
        long range = fromTimeRange0(startTime, endTime, shiftFlag, false);
        if (range == NONE) {
            return containsTime(timeRangeStamp, startTime);
        }
        if ((timeRangeStamp & range) == range) {
            // 如果范围是重合时，是否跨天时间点, 如果跨天时间点在时间段范围内
            boolean containsInRange;
            LocalTime offsetTime = parseTime(getOffsetIndex(timeRangeStamp));
            if (startTime.isBefore(endTime)) {
                containsInRange = offsetTime.isAfter(startTime) && offsetTime.isBefore(endTime);
            } else {
                containsInRange = offsetTime.isAfter(startTime) || offsetTime.isBefore(endTime);
            }
            return !containsInRange;
        }
        return false;
    }
    /**
     * 获取日期范围列表，周一为1,周二为2,...多个用英文逗号分隔
     * @param timeRangeStamp 日期段值
     * @return 返回星期值，多个用英文逗号分隔
     */
    public static String getWeekDays(long timeRangeStamp) {
        if (timeRangeStamp == NONE || (timeRangeStamp & ALL_WEEKDAYS) == NONE) {
            return TextUtils.EMPTY;
        }
        timeRangeStamp >>>= (TIME_BITS + START_TIME_BITS);

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < WEEKDAY_BITS; i ++) {
            if ((timeRangeStamp & (FIRST_BIT << i)) != NONE) {
                buf.append(getInstance().weekDayName(i + 1)).append(',');
            }
        }
        if (buf.length() > 0) {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    /**
     * dayOfWeek of week
     * @param dayOfWeek
     * @return
     */
    protected String weekDayName(int dayOfWeek) {
        return String.valueOf(dayOfWeek);
    }

    /**
     * 获取时间段值
     * @param weekdays 星期(周一为1;周二为2;...), 多个用英文逗号(,)分隔
     * @return 星期段值
     */
    public static long fromWeekDays(String weekdays) {
        if (TextUtils.isEmpty(weekdays)) {
            return NONE;
        }
        long result = NONE;
        for (int i = 0; i < WEEKDAY_BITS; i ++) {
            if (TextUtils.containsValue(weekdays, utils.weekDayName(i + 1))) {
                result |= (FIRST_BIT << i);
            }
        }
        return result << (TIME_BITS + START_TIME_BITS);
    }
    /**
     * 获取时间段值
     * @param timeRanges 时间范围列表
     * @return 时间段值
     */
    public static long fromTimeRanges(List<? extends TimeRange> timeRanges) {
        return fromTimeRanges0(timeRanges, false, null, false);
    }
    /**
     * 获取时间段值
     * @param timeRanges 时间范围列表
     * @param minuteTime 分钟精度时间段值
     * @return 时间段值
     */
    public static long fromTimeRanges(List<? extends TimeRange> timeRanges, StringBuilder minuteTime) {
        return fromTimeRanges0(timeRanges, false, minuteTime, true);
    }
    /**
     * 获取班次时间段值
     * @param timeRanges 时间范围列表
     * @return 时间段值
     */
    public static long fromShiftTimeRanges(List<? extends TimeRange> timeRanges) {
        return fromTimeRanges0(timeRanges, true, null, false);
    }

    /**
     * 获取班次时间段值
     * @param timeRanges 时间范围列表
     */
    public static void checkValidShiftTimeRanges(List<? extends TimeRange> timeRanges) {
        if (timeRanges == null || timeRanges.isEmpty()) {
            throw new DateTimeException("没有指定时间段");
        }
        long result = NONE;
        for (TimeRange timeRange : timeRanges) {
            if (timeRange.getStartTime() == null) {
                throw new DateTimeException("开始时间不能为空");
            }
            LocalTime endTime = timeRange.getEndTime();
            if (endTime == null) {
                throw new DateTimeException("结束时间不能为空");
            }
            long time = fromTimeRange0(timeRange.getStartTime(), endTime, true, false);
            if (time == NONE) {
                throw new DateTimeException("时间段长度必须都大于" + TIME_UNIT_IN_MINUTES + "分钟");
            }
            if ((time & result) != NONE) {
                throw new DateTimeException("时间段不能出现重叠");
            }
            int minuteOfDay = endTime.get(ChronoField.MINUTE_OF_DAY);
            long end = FIRST_BIT << (getEndIndex0(endTime.getSecond() > 0 ? minuteOfDay + 1 : minuteOfDay) - 1);
            if ((end & result) != NONE) {
                throw new DateTimeException("时间段不能出现重叠");
            }
            result = result | time | end;
        }
    }

    /**
     * 获取时间段值
     * @param timeRanges 时间范围列表
     * @param forceShift 是否强制分隔班次
     * @param minuteTime 分钟精度时间值
     * @return 时间段值
     */
    private static long fromTimeRanges0(List<? extends TimeRange> timeRanges, boolean forceShift, StringBuilder minuteTime, boolean appendTime) {
        if (timeRanges == null || timeRanges.isEmpty()) {
            return NONE;
        }
        // 调整值
        boolean hasMinuteTimes = minuteTime != null;
        if (hasMinuteTimes) {
            timeRanges = new ArrayList<>(timeRanges);
            timeRanges.sort(Comparator.comparing(TimeRange::getStartTime));
        }
        int adjustOffset;
        if (hasMinuteTimes && minuteTime.length() > 0) {
            minuteTime.append(TIME_ENTRY_SEPARATOR);
            adjustOffset = minuteTime.length();
        } else {
            adjustOffset = 0;
        }
        // 是否需要获取跨天位置
        boolean fetchOffset = true;
        // 时间段值
        long result = NONE;
        for (TimeRange timeRange : timeRanges) {
            LocalTime startTime = timeRange.getStartTime();
            LocalTime endTime = timeRange.getEndTime();
            if (startTime == null || endTime == null) {
               continue;
            }
            int startMinutes = startTime.get(ChronoField.MINUTE_OF_DAY);
            int endMinutes = endTime.getSecond() > 0 ? endTime.get(ChronoField.MINUTE_OF_DAY) : endTime.get(ChronoField.MINUTE_OF_DAY) + 1;
            // 获取时间段
            long range = fromTimeRange0(startMinutes, endMinutes, forceShift, fetchOffset);
            if (hasMinuteTimes) {
                // 需要调整值
                if (result != NONE && (result & range) != NONE) {
                    // 时间段有交集，实现调整值的合并
                    updateAdjustments(minuteTime, adjustOffset, result, startMinutes, endMinutes);
                } else {
                    // 时间没交集
                    appendAdjustments(minuteTime, adjustOffset, startMinutes, endMinutes);
                }
            }
            // 只获取第一次的跨天位置
            fetchOffset = fetchOffset && (range & DATE_START_TIME) == NONE;
            // 并入时段
            result |= range;
        }
        if (hasMinuteTimes) {
            if (appendTime) {
                if (minuteTime.length() != adjustOffset) {
                    minuteTime.append(ADJ_TIME_SEPARATOR);
                }
                minuteTime.append(Long.toUnsignedString(result, TIME_RANGE_RADIX));
            } else {
                if (minuteTime.length() == adjustOffset) {
                    minuteTime.setLength(adjustOffset - TIME_ENTRY_SEPARATOR.length());
                }
            }
        }
        return (forceShift && result != NONE) ? result | SHIFT_TIME_FLAG : result;
    }
    private static final String ADJ_TIME_SEPARATOR = "x";
    private static final String ADJ_ITEMS_SEPARATOR = "y";
    private static final String TIME_ENTRY_SEPARATOR = "z";
    private static final char END_TIME_FLAG = 't';

    private static final int ADJ_RADIX = 30;
    private static final int TIME_RANGE_RADIX = 32;
    private static final int INVALID = -1;

    private static void updateAdjustments(StringBuilder minuteTime, int adjustOffset, long result, int startMinutes, int endMinutes) {
        // 扫描之前的调整值，移除落在新时间范围内的调整点（合并后成为内部边界）
        int pos = adjustOffset;
        int deleteStart = INVALID;
        int startSlot = getStartIndex0(startMinutes);
        int endSlot = getEndIndex0(endMinutes);
        while (pos < minuteTime.length()) {
            // 定位当前值的范围 [valueEnd, pos)
            int valueEnd = minuteTime.indexOf(ADJ_ITEMS_SEPARATOR, pos);
            if (valueEnd == -1) {
                valueEnd = minuteTime.length();
            }
            // 解析调整值的分钟数
            boolean end = minuteTime.charAt(pos) == END_TIME_FLAG;
            int minutes = parseInt0(minuteTime, (end ? pos + 1 : pos), valueEnd, ADJ_RADIX);
            if (minutes == INVALID) {
                // 无效数字
                continue;
            }
            // 判断该调整点是否落在新时间范围内（内部边界，需要移除）
            boolean inside;
            if (startMinutes < endMinutes || endMinutes == 0) {
                // 非跨天
                inside = minutes >= startMinutes && (endMinutes == 0 || minutes < endMinutes);
            } else {
                // 跨天：start > end，范围是 [start, 24:00) + [0, end)
                inside = minutes >= startMinutes || minutes < endMinutes;
            }
            if (inside) {
                if (deleteStart == INVALID) {
                    deleteStart = pos;
                }
            } else {
                if (deleteStart != INVALID) {
                    // 移除该调整值（含后导分隔符）
                    minuteTime.delete(deleteStart, pos);
                }
                deleteStart = INVALID;
                if (end) {
                    if (endSlot != INVALID && (endSlot == TIME_BITS || Math.abs(endMinutes - minutes) < TIME_UNIT_IN_MINUTES) && getEndIndex0(minutes) == endSlot) {
                        endSlot = INVALID;
                    }
                } else {
                    if (startSlot != INVALID && Math.abs(minutes - startMinutes) < TIME_UNIT_IN_MINUTES && getStartIndex0(minutes) == startSlot) {
                        startSlot = INVALID;
                    }
                }
            }
            pos = valueEnd + 1;
        }
        if (deleteStart != INVALID) {
            minuteTime.delete(deleteStart, minuteTime.length());
        }
        // 开始时间不在之前的覆盖范围内 → 外部边界，追加开始调整值
        if (startSlot != INVALID && startMinutes % TIME_UNIT_IN_MINUTES != 0 && (result & (FIRST_BIT << startSlot)) == NONE) {
            if (minuteTime.length() > adjustOffset) {
                minuteTime.append(ADJ_ITEMS_SEPARATOR);
            }
            minuteTime.append(startMinutes);
        }
        // 结束时间不在之前的覆盖范围内 → 外部边界，追加结束调整值
        if (endSlot != INVALID && endMinutes % TIME_UNIT_IN_MINUTES != 0 && (result & (FIRST_BIT << (endSlot > 0 ? endSlot - 1 : 0))) == NONE) {
            if (minuteTime.length() > adjustOffset) {
                minuteTime.append(ADJ_ITEMS_SEPARATOR + END_TIME_FLAG);
            } else {
                minuteTime.append(END_TIME_FLAG);
            }
            minuteTime.append(Integer.toUnsignedString(endMinutes, ADJ_RADIX));
        }
    }


    /**
     * 获取时间段值
     * @param weekdays 星期(周一为1;周二为2;...), 多个用英文逗号(,)分隔
     * @param timeRanges 时间范围列表
     * @return 时间段值
     */
    public static long from(String weekdays, List<? extends TimeRange> timeRanges) {
        return fromWeekDays(weekdays) | fromTimeRanges0(timeRanges, false, null, false);
    }
    /**
     * 获取时间段值
     * @param weekdays 星期(周一为1;周二为2;...), 多个用英文逗号(,)分隔
     * @param timeRanges 时间范围列表
     * @param minuteTime 分钟精度的时间段值
     * @return 时间段值
     */
    public static long from(String weekdays, List<? extends TimeRange> timeRanges, StringBuilder minuteTime) {
        int offset = minuteTime.length();
        // 获取时间段值
        long timestamp = fromWeekDays(weekdays) | fromTimeRanges0(timeRanges, false, minuteTime, false);
        if (minuteTime.length() != offset) {
            minuteTime.append(ADJ_TIME_SEPARATOR);
        } else if (offset > 0) {
            minuteTime.append(TIME_ENTRY_SEPARATOR);
        }
        minuteTime.append(Long.toUnsignedString(timestamp, TIME_RANGE_RADIX));
        return timestamp;
    }



    /**
     * 获取时间范围对应的段值
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段值
     */
    public static long fromTimeRange(LocalTime startTime, LocalTime endTime) {
        return fromTimeRange0(startTime, endTime, false, true);
    }

    /**
     * 获取时间对应的段值
     * @param time 时间
     * @return 时间段值
     */
    public static long fromTime(LocalTime time) {
        return time != null ? FIRST_BIT << getStartIndex0(time.get(ChronoField.MINUTE_OF_DAY)) : NONE;
    }

    /**
     * 获取时间范围对应的段值
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param forceShift 是否强制分隔班次
     * @param fetchOffset 是否解析营业天开始时间
     * @return 时间段值
     */
    private static long fromTimeRange0(LocalTime startTime, LocalTime endTime, boolean forceShift, boolean fetchOffset) {
        if (startTime == null || endTime == null) {
            return NONE;
        }
        int startMinutes = startTime.get(ChronoField.MINUTE_OF_DAY);
        int endMinutes = endTime.getSecond() > 0 ? endTime.get(ChronoField.MINUTE_OF_DAY) : endTime.get(ChronoField.MINUTE_OF_DAY) + 1;
        return fromTimeRange0(startMinutes, endMinutes, forceShift, fetchOffset);
    }


    /**
     * 获取时间范围对应的段值
     * @param forceShift 是否强制分隔班次
     * @param fetchOffset 是否解析营业天开始时间
     * @return 时间段值
     */
    private static long fromTimeRange0(int startMinutes, int endMinutes, boolean forceShift, boolean fetchOffset) {
        int start = getStartIndex0(startMinutes);
        int end = getEndIndex0(endMinutes);
        long startValue = FIRST_BIT << start;
        if (end == TIME_BITS || startMinutes < endMinutes) {
            // 非跨天
            return (FIRST_BIT << (forceShift ? end - 1 : end)) - startValue;
        }
        // 跨天
        long result;
        if (forceShift) {
            result = start <= end ? (FIRST_BIT << (TIME_BITS - 1)) - FIRST_BIT:  ~(startValue - (FIRST_BIT << (end - 1))) & ALL_TIMES;
        } else {
            result = start <= end ? ALL_TIMES :  ~(startValue - (FIRST_BIT << end)) & ALL_TIMES;
        }
        if (fetchOffset) {
            result |= (long)end << TIME_BITS;
        }
        return result;
    }

    private static void appendAdjustments(StringBuilder minuteTime, int adjustOffset, int startMinutes, int endMinutes) {
        if (minuteTime != null) {
            if (startMinutes % TIME_UNIT_IN_MINUTES != 0) {
                if (minuteTime.length() > adjustOffset) {
                    minuteTime.append(ADJ_ITEMS_SEPARATOR);
                }
                minuteTime.append(Integer.toUnsignedString(startMinutes, ADJ_RADIX));
            }
            if (endMinutes % TIME_UNIT_IN_MINUTES != 0) {
                if (minuteTime.length() > adjustOffset) {
                    minuteTime.append(ADJ_ITEMS_SEPARATOR + END_TIME_FLAG);
                } else {
                    minuteTime.append(END_TIME_FLAG);
                }
                minuteTime.append(Integer.toUnsignedString(endMinutes, ADJ_RADIX));
            }
        }
    }


    private static int getOffsetIndex(long timeRangeStamp) {
        return (int) ((timeRangeStamp & DATE_START_TIME) >>> TIME_BITS);
    }

    private static LocalTime parseTime(int seq) {
        return LocalTime.MIN.plusMinutes((long) seq * TIME_UNIT_IN_MINUTES);
    }

    /**
     * 获取时间对应的段值
     * @param startMinutes 时间
     * @return 返回时间index
     */
    private static int getStartIndex0(int startMinutes) {
        return startMinutes / TIME_UNIT_IN_MINUTES;
    }
    /**
     * 获取时间对应的段值
     * @param endMinutes 时间
     * @return 返回时间index
     */
    private static int getEndIndex0(int endMinutes) {
        // 如果结束时间为零，可以认为是24:00
        return endMinutes == 0 ? TIME_BITS : (endMinutes - 1) / TIME_UNIT_IN_MINUTES + 1;
    }

    private static int parseInt0(CharSequence s, int i, int endIndex, int radix) throws NumberFormatException {
        int result = 0;
        while (i < endIndex) {
            int digit = Character.digit(s.charAt(i++), radix);
            if (digit < 0) {
                return -1;
            }
            result = result * radix + digit;
        }
        return result;
    }

}
