package com.honzel.core.util.time;

import com.honzel.core.util.text.TextUtils;

import javax.annotation.PostConstruct;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

    private static final int START_TIME_TIME_SHIFT = TIME_BITS;
    private static final long DATE_START_TIME = ~(-FIRST_BIT << START_TIME_BITS) << START_TIME_TIME_SHIFT;

    /**
     * 全部日期
     */
    private static final int WEEKDAY_BITS = 7;
    private static final int WEEKDAY_SHIFT = TIME_BITS + START_TIME_BITS;
    public static final long ALL_WEEKDAYS = ~(-FIRST_BIT << WEEKDAY_BITS) << WEEKDAY_SHIFT;
    /**
     * 班次时间标识
     */
    public static final long SHIFT_TIME_FLAG = FIRST_BIT << (WEEKDAY_SHIFT + WEEKDAY_BITS);

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
        return getTimeRanges0(timeRangeStamp, null, 0, 0, divisionDuration, halfDivisionDurationEnabled);
    }

    /**
     * 获取时间范围列表
     * @param timeRangeStamp 时间段值
     * @param adjTime 调整时间串
     * @param adjStart 调整开始位置
     * @param adjEnd 调整结束位置
     * @param divisionDuration 切割时长（单位为分钟), 0为不切割
     * @param halfDivisionDurationEnabled 是否步长为一半切割时长, true-步长为切割时长的一半, false-步长与切割时长相等
     * @return 返回时间段列表
     * @param <TimeRange> 时间段类型
     */
    public static<TimeRange extends com.honzel.core.util.time.TimeRange> List<TimeRange> getTimeRanges0(long timeRangeStamp, String adjTime, int adjStart, int adjEnd, int divisionDuration, boolean halfDivisionDurationEnabled) {
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
        for (int i = 0; i < TIME_BITS; i ++, times >>>= 1) {
            if ((times & FIRST_BIT) == NONE) {
                if (timeRange != null) {
                    int seq = (offset + i) % TIME_BITS;
                    if (shiftFlag) {
                        timeRange.setEndTime(i == TIME_BITS - 1 && seq == i ? LocalTime.MAX : parseTime(seq + 1));
                    } else {
                        timeRange.setEndTime(parseTime(seq));
                    }
                    // 应用调整值
                    List<TimeRange> subRanges = applyAdjustments(timeRange, adjTime, adjStart, adjEnd);
                    if (subRanges != null) {
                        // 如果有拆分成多个时间段则遍历子时间段
                        for (TimeRange subRange : subRanges) {
                            // 按切割时长拆分时间段
                            addDivideTimeRange(subRange, timeRangeList, divisionDuration, halfDivisionDurationEnabled);
                        }
                    } else {
                        // 按切割时长拆分时间段
                        addDivideTimeRange(timeRange, timeRangeList, divisionDuration, halfDivisionDurationEnabled);
                    }
                    timeRange = null;
                }
                if (times == NONE) {
                    break;
                }
            } else {
                if (timeRange == null) {
                    timeRange = (TimeRange) getInstance().newTimeRange();
                    timeRange.setStartTime(parseTime((offset + i) % TIME_BITS));
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
            addDivideTimeRange(timeRange, timeRangeList, divisionDuration, halfDivisionDurationEnabled);
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
    private static<T extends TimeRange> void addDivideTimeRange(T timeRange, List<T> timeRangeList, int divisionDuration, boolean halfDivisionDurationEnabled) {
        timeRangeList.add(timeRange);
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
        int index = getFirstStartIndex(timeRangeStamp);
        return index != INVALID ? parseTime(index) : null;
    }
    private static int getFirstStartIndex(long timeRangeStamp) {
        if (timeRangeStamp != NONE) {
            int offset = getOffsetIndex(timeRangeStamp);
            for (int i = 0; i < TIME_BITS; i ++) {
                int seq = (offset + i) % TIME_BITS;
                if ((timeRangeStamp & (FIRST_BIT << seq)) != NONE) {
                    return seq;
                }
            }
        }
        return INVALID;
    }



    /**
     * 获取最起始时间点
     * @param minuteTime 分钟精度时间段值
     * @param dayOfWeek 天
     * @return 返回最起始时间点
     */
    public static LocalTime getFirstStartTime(String minuteTime, DayOfWeek dayOfWeek) {
        return dayOfWeek != null ? getMinOrMaxTime0(minuteTime, FIRST_BIT << (WEEKDAY_SHIFT + dayOfWeek.ordinal()), false) : null;
    }


    /**
     * 获取最后结束时间点
     * @param timeRangeStamp 时间段值
     * @return 返回最后结束时间点
     */
    public static LocalTime getLastEndTime(long timeRangeStamp) {
        int index = getLastEndTimeIndex(timeRangeStamp);
        return index != INVALID ? (index == TIME_BITS || index == 0) ? LocalTime.MAX : parseTime(index) : null;
    }

    /**
     * 获取最后结束时间点
     * @param minuteTime 分钟精度时间段值
     * @return 返回最后结束时间点
     */
    public static LocalTime getLastEndTime(String minuteTime, DayOfWeek dayOfWeek) {
        return dayOfWeek != null ? getMinOrMaxTime0(minuteTime, FIRST_BIT << (WEEKDAY_SHIFT + dayOfWeek.ordinal()), true) : null;
    }
    /**
     * 获取最后结束时间点
     * @param timeRangeStamp 时间段值
     * @return 返回最后结束时间点
     */
    public static int getLastEndTimeIndex(long timeRangeStamp) {
        if (timeRangeStamp != NONE) {
            // 开始时间位置
            int offset = getOffsetIndex(timeRangeStamp);
            for (int i = TIME_BITS - 1; i >= 0; i --) {
                int seq = (offset + i) % TIME_BITS;
                if ((timeRangeStamp & (FIRST_BIT << seq)) != NONE) {
                    return (timeRangeStamp & SHIFT_TIME_FLAG) != NONE ? (seq + 2) % TIME_BITS : seq + 1;
                }
            }
        }
        return INVALID;
    }


    public static LocalTime getMinOrMaxTime0(String minuteTime, long timeRangeMask, boolean end) {
        // 获取指定天的起始时间位置
        int pos = matchMinuteTime0(minuteTime, timeRangeMask, null, null);
        if (pos == INVALID) {
            return null;
        }
        // 获取时间段的结束位置
        int entryEnd = getEntryEnd(minuteTime, pos, minuteTime.length());
        // 获取调整值的结束位置
        int adjustmentEnd = getAdjustmentEnd(minuteTime, pos, entryEnd);
        if (adjustmentEnd == INVALID) {
            // 没有调整值
            long time = parseTimeRangeStamp(minuteTime, pos, entryEnd);
            return time == INVALID ? null : getFirstStartTime(time);
        }
        // 获取调整值的时间段值
        long time = parseTimeRangeStamp(minuteTime, adjustmentEnd + 1, entryEnd);
        if (time == INVALID) {
            return null;
        }
        // 解析调整值的分钟数
        if (end) {
            int index = getLastEndTimeIndex(time);
            if (index == INVALID) {
                return null;
            }
            // 调整值的分钟数
            int minutes = endOfAdjustment(minuteTime, pos, adjustmentEnd, index);
            return minutes != INVALID ? LocalTime.MIN.plusMinutes(minutes) : (index == TIME_BITS || index == 0) ? LocalTime.MAX : parseTime(index);
        } else {
            int index = getFirstStartIndex(time);
            if (index == INVALID) {
                return null;
            }
            // 调整值的分钟数
            int minutes = startOfAdjustment(minuteTime, pos, adjustmentEnd, index);
            return minutes != INVALID ? LocalTime.MIN.plusMinutes(minutes) : parseTime(index);
        }
    }

    private static int startOfAdjustment(String minuteTime, int adjustmentStart, int adjustmentEnd, int timeIndex) {
        //TODO 需要根据具体需求实现
        return INVALID;
    }
    private static int endOfAdjustment(String minuteTime, int adjustmentStart, int adjustmentEnd, int timeIndex) {
        //TODO 需要根据具体需求实现
        return INVALID;
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
            if (((time |= (time << 1)) & (FIRST_BIT << TIME_BITS)) != NONE) {
                // 最后一位非0，移一位后，需要补到开头
                time = (time & ALL_TIMES) | FIRST_BIT;
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
        return time != null && containsDay(timeRangeStamp, time.toLocalDate()) && containsTime(timeRangeStamp, time.toLocalTime());
    }
    /**
     * 是否包含指定日期
     * @param timeRangeStamp  时间段值
     * @param date 指定日期
     * @return 是否包含指定日期
     */
    public static boolean containsDay(long timeRangeStamp, LocalDate date) {
        return date != null && containsDay(timeRangeStamp, date.getDayOfWeek());
    }
    /**
     * 是否包含指定星期
     * @param timeRangeStamp  时间段值
     * @param dayOfWeek 指定星期
     * @return 是否包含指定星期
     */
    public static boolean containsDay(long timeRangeStamp, DayOfWeek dayOfWeek) {
        if (timeRangeStamp != NONE && dayOfWeek != null) {
            int day = dayOfWeek.ordinal();
            return ((timeRangeStamp >>> (WEEKDAY_SHIFT + day)) & FIRST_BIT) != NONE;
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
     * 时间段内是否包含有该时间区间
     * @param timeRangeStamp  时间段值
     * @param timeRange 时间段
     * @return 是否时间段值包含该时间段
     */
    public static boolean containsTimeRange(long timeRangeStamp, TimeRange timeRange) {
        return timeRange != null && containsTimeRange(timeRangeStamp, timeRange.getStartTime(), timeRange.getEndTime());
    }
    /**
     * 时间段内是否包含有该时间区间
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
        timeRangeStamp >>>= WEEKDAY_SHIFT;

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
            String value = utils.weekDayName(i + 1);
            if (TextUtils.containsValue(weekdays, value)) {
                result |= (FIRST_BIT << i);
                if (weekdays.length() == value.length()) {
                    // 匹配完全，跳出循环
                    break;
                }
            }
        }
        return result << WEEKDAY_SHIFT;
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
     * @return 时间段值
     */
    public static long fromShiftTimeRanges(List<? extends TimeRange> timeRanges, StringBuilder minuteTime) {
        return fromTimeRanges0(timeRanges, true, minuteTime, true);
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
        long boundary = NONE;
        for (int i = 0; i < timeRanges.size(); i++) {
            TimeRange timeRange = timeRanges.get(i);
            LocalTime startTime = timeRange.getStartTime();
            if (startTime == null) {
                throw new DateTimeException("开始时间不能为空");
            }
            LocalTime endTime = timeRange.getEndTime();
            if (endTime == null) {
                throw new DateTimeException("结束时间不能为空");
            }
            int startMinutes = startTime.get(ChronoField.MINUTE_OF_DAY);
            int endMinutes = endTime.getSecond() > 0 ? endTime.get(ChronoField.MINUTE_OF_DAY) : endTime.get(ChronoField.MINUTE_OF_DAY) + 1;
            if (startMinutes < endMinutes && endMinutes - startMinutes <= TIME_UNIT_IN_MINUTES) {
                throw new DateTimeException("时间段长度必须都大于" + TIME_UNIT_IN_MINUTES + "分钟");
            }
            long time = fromTimeRange0(startMinutes, endMinutes, false, false);
            long boundaryStart = FIRST_BIT << getStartIndex0(startMinutes);
            long boundaryEnd = FIRST_BIT << (getEndIndex0(endMinutes) - 1);
            long retain = time & result;
            if (retain != NONE) {
                if ((retain != boundaryStart && retain != boundaryEnd) || (retain & boundary) == NONE) {
                    throw new DateTimeException("时间段不能出现重叠");
                }
                if (retain == boundaryStart) {
                    for (int j = i - 1; j >= 0; j--) {
                        LocalTime otherStart = timeRanges.get(j).getStartTime();
                        if (fromTime(otherStart) == boundaryStart && getHalfHourSeconds(otherStart) >= getHalfHourSeconds(startTime)) {
                            throw new DateTimeException("时间段不能出现重叠");
                        }
                    }
                }
                if (retain == boundaryEnd) {
                    for (int j = i - 1; j >= 0; j--) {
                        LocalTime otherEnd = timeRanges.get(j).getEndTime();
                        if (fromTime(otherEnd) == boundaryEnd && getHalfHourSeconds(otherEnd) <= getHalfHourSeconds(endTime)) {
                            throw new DateTimeException("时间段不能出现重叠");
                        }
                    }
                }
            }
            boundary |= (boundaryStart | boundaryEnd);
            result |= time;
        }
    }

    private static int getHalfHourSeconds(LocalTime time) {
        int minute = time.getMinute();
        return (minute >= TIME_UNIT_IN_MINUTES ? minute - TIME_UNIT_IN_MINUTES : minute) + time.getSecond();
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
                long nonShiftResult = forceShift ? nonShift(result) : result;
                if (nonShiftResult != NONE && (nonShiftResult & (forceShift ? nonShift(range) : range)) != NONE) {
                    // 时间段有交集，实现调整值的合并
                    updateAdjustments(minuteTime, adjustOffset, nonShiftResult, startMinutes, endMinutes);
                } else {
                    // 时间没交集
                    appendAdjustments(minuteTime, adjustOffset, startMinutes, endMinutes);
                }
            }
            // 只获取第一次的跨天位置
            if (fetchOffset && (range & DATE_START_TIME) != NONE) {
                fetchOffset = false;
            }
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
        return (forceShift && result != NONE) ? (result | SHIFT_TIME_FLAG) : result;
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
     * @param forceShift 是否强制分隔班次
     * @param timeRanges 时间范围列表
     * @return 时间段值
     */
    public static long from(String weekdays, List<? extends TimeRange> timeRanges, boolean forceShift) {
        return fromWeekDays(weekdays) | fromTimeRanges0(timeRanges, forceShift, null, false);
    }
    /**
     * 获取时间段值
     * @param weekdays 星期(周一为1;周二为2;...), 多个用英文逗号(,)分隔
     * @param timeRanges 时间范围列表
     * @param minuteTime 分钟精度的时间段值
     * @return 时间段值
     */
    public static long from(String weekdays, List<? extends TimeRange> timeRanges, StringBuilder minuteTime) {
        return from(weekdays, timeRanges, false, minuteTime);
    }
    /**
     * 获取时间段值
     * @param weekdays 星期(周一为1;周二为2;...), 多个用英文逗号(,)分隔
     * @param timeRanges 时间范围列表
     * @param forceShift 是否强制分隔班次
     * @param minuteTime 分钟精度的时间段值
     * @return 时间段值
     */
    public static long from(String weekdays, List<? extends TimeRange> timeRanges, boolean forceShift, StringBuilder minuteTime) {
        if (minuteTime == null) {
            return fromWeekDays(weekdays) | fromTimeRanges0(timeRanges, forceShift, null, false);
        }
        int offset = minuteTime.length();
        // 获取时间段值
        long time = fromWeekDays(weekdays) | fromTimeRanges0(timeRanges, forceShift, minuteTime, false);
        if (minuteTime.length() != offset) {
            minuteTime.append(ADJ_TIME_SEPARATOR);
        } else if (offset > 0) {
            minuteTime.append(TIME_ENTRY_SEPARATOR);
        }
        minuteTime.append(Long.toUnsignedString(time, TIME_RANGE_RADIX));
        return time;
    }




    /**
     * 获取指定星期的时间范围集合
     * <p>从 {@link #from(String, List, StringBuilder)} 生成的 minuteTime 中解析出指定星期对应的时间范围列表。
     * 解析条目中的时间戳位值判断是否包含目标星期，再结合调整值还原分钟精度的时间边界。
     * 当时间戳的日期区域为 0 时表示适用所有日期。</p>
     * @param minuteTime 分钟精度的时间段值
     * @param divisionDuration 切割时长（单位为分钟)
     * @return 返回该星期对应的时间范围列表，不包含则返回空列表
     * @param <T> 时间范围类型
     */
    public static<T extends TimeRange> List<T> getMinuteTimeRanges(String minuteTime, int divisionDuration) {
        return getMinuteTimeRanges(minuteTime, NONE, divisionDuration, false);
    }

    /**
     * 获取指定星期的时间范围集合
     * <p>从 {@link #from(String, List, StringBuilder)} 生成的 minuteTime 中解析出指定星期对应的时间范围列表。
     * 解析条目中的时间戳位值判断是否包含目标星期，再结合调整值还原分钟精度的时间边界。
     * 当时间戳的日期区域为 0 时表示适用所有日期。</p>
     * @param minuteTime 分钟精度的时间段值
     * @param divisionDuration 切割时长（单位为分钟)
     * @param halfDivisionDurationEnabled 是否步长为一半切割时长, true-步长为切割时长的一半, false-步长与切割时长相等
     * @return 返回该星期对应的时间范围列表，不包含则返回空列表
     * @param <T> 时间范围类型
     */
    public static<T extends TimeRange> List<T> getMinuteTimeRanges(String minuteTime, int divisionDuration, boolean halfDivisionDurationEnabled) {
        return getMinuteTimeRanges(minuteTime, NONE, divisionDuration, halfDivisionDurationEnabled);
    }
    /**
     * 获取指定星期的时间范围集合
     * <p>从 {@link #from(String, List, StringBuilder)} 生成的 minuteTime 中解析出指定星期对应的时间范围列表。
     * 解析条目中的时间戳位值判断是否包含目标星期，再结合调整值还原分钟精度的时间边界。
     * 当时间戳的日期区域为 0 时表示适用所有日期。</p>
     * @param minuteTime 分钟精度的时间段值
     * @param dayOfWeek 目标日期
     * @return 返回该星期对应的时间范围列表，不包含则返回空列表
     * @param <T> 时间范围类型
     */
    public static<T extends TimeRange> List<T> getMinuteTimeRanges(String minuteTime, DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return Collections.emptyList();
        }
        long day = FIRST_BIT << (WEEKDAY_SHIFT + dayOfWeek.ordinal());
        return getMinuteTimeRanges(minuteTime, day, 0, false);
    }


    /**
     * 时间段内是否包含有该时间区间
      * @param minuteTime 分钟精度的时间段值
     * @param timeRangeMask 时间范围掩码, 包含日期和时间范围, 如果为0, 则表示获取首个时间范围集合
     *  @param startTime 时间段开始时间点
     * @param endTime 时间段结束时间
     * @return 是否时间段值包含该时间段
     */
    public static boolean containsTimeRange(String minuteTime, long timeRangeMask, LocalTime startTime, LocalTime endTime) {
        return startTime != null && endTime != null
                && matchMinuteTime0(minuteTime, timeRangeMask, startTime, endTime) != INVALID;
    }
    /**
     * 时间段内是否包含有该时间区间
      * @param minuteTime 分钟精度的时间段值
     * @param dayOfWeek 日期
     *  @param startTime 时间段开始时间点
     * @param endTime 时间段结束时间
     * @return 是否时间段值包含该时间段
     */
    public static boolean containsTimeRange(String minuteTime, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        return dayOfWeek != null && startTime != null && endTime != null
                && matchMinuteTime0(minuteTime, FIRST_BIT << (WEEKDAY_SHIFT + dayOfWeek.ordinal()), startTime, endTime) != INVALID;
    }

    /**
     * 时间段内是否包含有该时间区间
      * @param minuteTime 分钟精度的时间段值
     * @param timeRangeMask 时间范围掩码, 包含日期和时间范围, 如果为0, 则表示获取首个时间范围集合
     * @param time 指定的时间
     * @return 是否时间段值包含该时间段
     */
    public static boolean containsTime(String minuteTime, long timeRangeMask, LocalTime time) {
        return time != null && matchMinuteTime0(minuteTime, timeRangeMask, time, null) != INVALID;
    }

    /**
     * 时间段内是否包含有该时间区间
      * @param minuteTime 分钟精度的时间段值
     *  @param startTime 时间段开始时间点
     * @param endTime 时间段结束时间
     * @return 是否时间段值包含该时间段
     */
    public static boolean containsTimeRange(String minuteTime, LocalTime startTime, LocalTime endTime) {
        return startTime != null && endTime != null && matchMinuteTime0(minuteTime, NONE, startTime, endTime) != INVALID;
    }
    /**
     * 时间段内是否包含有该时间
     * @param minuteTime 分钟精度的时间段值
     * @param time 指定的时间
     * @return 是否时间段值包含该时间
     */
    public static boolean containsTime(String minuteTime, LocalTime time) {
        return time != null && matchMinuteTime0(minuteTime, NONE, time, null) != INVALID;
    }
    /**
     * 时间段内是否包含有该时间
     * @param minuteTime 分钟精度的时间段值
     * @param time 指定的时间
     * @return 是否时间段值包含该时间
     */
    public static boolean containsDateTime(String minuteTime, LocalDateTime time) {
        return time != null && matchMinuteTime0(minuteTime, FIRST_BIT << (WEEKDAY_SHIFT + time.getDayOfWeek().ordinal()), time.toLocalTime(), null) != INVALID;
    }

    /**
     * 获取指定星期的时间范围集合
     * <p>从 {@link #from(String, List, StringBuilder)} 生成的 minuteTime 中解析出指定星期对应的时间范围列表。
     * 解析条目中的时间戳位值判断是否包含目标星期，再结合调整值还原分钟精度的时间边界。
     * 当时间戳的日期区域为 0 时表示适用所有日期。</p>
     * @param minuteTime 分钟精度的时间段值
     * @param timeRangeMask 时间范围掩码, 包含日期和时间范围, 如果为0, 则表示获取首个时间范围集合
     *  @param startTime 时间段开始时间点
     * @param endTime 时间段结束时间
     *
     * @return 返回该星期对应的时间范围列表，不包含则返回空列表
     */
    private static int matchMinuteTime0(String minuteTime, long timeRangeMask, LocalTime startTime, LocalTime endTime) {
        if (TextUtils.isEmpty(minuteTime)) {
            return INVALID;
        }
        int days = timeRangeMask == NONE ? 0 : (int)((timeRangeMask & ALL_WEEKDAYS) >>> WEEKDAY_SHIFT);
        if (days != 0) {
            timeRangeMask &= ~ALL_WEEKDAYS;
        }
        // weekday 位在 base-32 时间戳中从末尾向前定位
        int pos = 0;
        int len = minuteTime.length();
        while (pos < len) {
            // 定位当前条目的分隔符位置
            int entryEnd = getEntryEnd(minuteTime, pos, len);
            // 从末尾反向定位调整值与时间戳分隔符
            int timeSep = getAdjustmentEnd(minuteTime, pos, entryEnd);
            int timeStart = timeSep + 1;
            if (entryEnd == timeStart) {
                pos = entryEnd + TIME_ENTRY_SEPARATOR.length();
                continue;
            }
            // 直接读取 weekday 区域的两个字符，提取 7 位 weekday 值
            int weekdays = parseWeekdays(minuteTime, timeStart, entryEnd);
            // weekday 区域为 0 表示适用所有日期，否则检查对应日期位
            if (weekdays != INVALID && (weekdays == 0 || days == 0 || (weekdays & days) != 0)) {
                // 时间戳
                long stamp = parseTimeRangeStamp(minuteTime, timeStart, entryEnd);
                if (stamp != INVALID) {
                    long time;
                    if (timeRangeMask != NONE) {
                        time = ALL_TIMES & nonShift(stamp) & nonShift(timeRangeMask);
                    } else {
                        time = ALL_TIMES & nonShift(stamp);
                    }
                    // 从时间戳获取基础时间范围
                    boolean result = time != NONE && (startTime == null
                            || (endTime != null ? containsTimeRange(time, startTime, endTime) : containsTime(time, startTime))
                            && (pos >= timeSep || containsMinuteRange0(minuteTime, pos, timeSep, startTime, endTime)));
                    if (result || weekdays == 0 || (weekdays & days) == days) {
                        return result ? pos : INVALID;
                    }
                }
            }
            pos = entryEnd + TIME_ENTRY_SEPARATOR.length();
        }
        return INVALID;
    }

    private static int getAdjustmentEnd(String minuteTime, int entryStart, int entryEnd) {
        int timeSep = entryEnd - 1;
        while (timeSep >= entryStart && minuteTime.charAt(timeSep) != ADJ_TIME_SEPARATOR) {
            timeSep--;
        }
        return timeSep;
    }

    private static int getEntryEnd(String minuteTime, int entryStart, int totalLen) {
        int entryEnd = minuteTime.indexOf(TIME_ENTRY_SEPARATOR, entryStart);
        if (entryEnd == INVALID) {
            entryEnd = totalLen;
        }
        return entryEnd;
    }

    private static boolean containsMinuteRange0(String minuteTime, int adjStart, int adjEnd, LocalTime startTime, LocalTime endTime) {
        //TODO 实现 containsMinuteRange
        return true;
    }

    /**
     * 获取指定星期的时间范围集合
     * <p>从 {@link #from(String, List, StringBuilder)} 生成的 minuteTime 中解析出指定星期对应的时间范围列表。
     * 解析条目中的时间戳位值判断是否包含目标星期，再结合调整值还原分钟精度的时间边界。
     * 当时间戳的日期区域为 0 时表示适用所有日期。</p>
     *
     * @param <T>                         时间范围类型
     * @param minuteTime                  分钟精度的时间段值
     * @param timeRangeMask               时间范围掩码, 包含日期和时间范围, 如果为0, 则表示获取首个时间范围集合
     * @param divisionDuration            切割时长（单位为分钟)
     * @param halfDivisionDurationEnabled 是否步长为一半切割时长, true-步长为切割时长的一半, false-步长与切割时长相等
     * @return 返回该星期对应的时间范围列表，不包含则返回空列表
     */
    public static<T extends TimeRange> List<T> getMinuteTimeRanges(String minuteTime, long timeRangeMask, int divisionDuration, boolean halfDivisionDurationEnabled) {
        if (TextUtils.isEmpty(minuteTime)) {
            return Collections.emptyList();
        }
        int days = timeRangeMask == NONE ? 0 : (int)((timeRangeMask & ALL_WEEKDAYS) >>> WEEKDAY_SHIFT);
        if (days != 0) {
            timeRangeMask &= ~ALL_WEEKDAYS;
        }
        // weekday 位在 base-32 时间戳中从末尾向前定位
        int pos = 0;
        int len = minuteTime.length();
        while (pos < len) {
            // 定位当前条目的分隔符位置
            int entryEnd = getEntryEnd(minuteTime, pos, len);
            // 从末尾反向定位调整值与时间戳分隔符
            int timeSep = getAdjustmentEnd(minuteTime, pos, entryEnd);
            int timeStart = timeSep + 1;
            if (entryEnd == timeStart) {
                pos = entryEnd + TIME_ENTRY_SEPARATOR.length();
                continue;
            }
            // 直接读取 weekday 区域的两个字符，提取 7 位 weekday 值
            int weekdays = parseWeekdays(minuteTime, timeStart, entryEnd);
            // weekday 区域为 0 表示适用所有日期，否则检查对应日期位
            if (weekdays != INVALID && (weekdays == 0 || days == 0 || (weekdays & days) != 0)) {
                // 时间戳
                long stamp = parseTimeRangeStamp(minuteTime, timeStart, entryEnd);
                if (stamp != INVALID) {
                    if (timeRangeMask != NONE) {
                        long shiftFlag = stamp & SHIFT_TIME_FLAG;
                        if ((SHIFT_TIME_FLAG & timeRangeMask) == shiftFlag) {
                            // 班次标识一致
                            stamp = (stamp & ~ALL_TIMES) | stamp & timeRangeMask & ALL_TIMES;
                        } else {
                            // 班次标识不一致
                            stamp = (stamp & ~ALL_TIMES) | stamp & nonShift(timeRangeMask) & ALL_TIMES;
                        }
                    }
                    // 从时间戳获取基础时间范围
                    return getTimeRanges0(stamp, minuteTime, pos, timeSep, divisionDuration, halfDivisionDurationEnabled);
                }
            }
            pos = entryEnd + TIME_ENTRY_SEPARATOR.length();
        }
        return Collections.emptyList();
    }

    private static int parseWeekdays(String minuteTime, int timeStart, int timeEnd) {
        // weekday 位 (bits 54-60) 在 base-32 编码中从末尾向前定位
        int pos = (WEEKDAY_SHIFT - 1) / TIME_RANGE_BITS + 1;
        int weekdays = 0;
        int timeLen = timeEnd - timeStart;
        if (timeLen < pos) {
            return weekdays;
        }
        int offset = WEEKDAY_SHIFT % TIME_RANGE_BITS;
        for (int i = 0; i < WEEKDAY_BITS && pos <= timeLen; i += (TIME_RANGE_BITS - offset)) {
            int d = Character.digit(minuteTime.charAt(timeEnd - pos++), TIME_RANGE_RADIX);
            if (d == INVALID) {
                return INVALID;
            }
            if (i != 0 && offset != 0) {
                // 不是第一次循环，需要重置 offset为0
                offset = 0;
            }
            weekdays |= (d >> offset) << i;
        }
        return weekdays & ((1 << WEEKDAY_BITS) - 1);

    }

    /**
     * 应用调整值到时间范围列表，还原分钟精度的边界
     *
     * @param timeRange  基础时间范围列表（30分钟精度）
     * @param adjustmentTime  调整值字符串
     * @param adjStart  调整值起始位置
     * @param adjEnd    调整值结束位置
     */
    private static<T extends TimeRange> List<T> applyAdjustments(TimeRange timeRange, String adjustmentTime, int adjStart, int adjEnd) {
        if (TextUtils.isEmpty(adjustmentTime) || adjStart >= adjEnd) {
            return null;
        }
        //TODO 按调整值字符串解析并应用到时间范围，如果需要从一个时间范围拆分出多个时间范围时，返回调整后的时间范围列表，否则返回null
        return null;
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
            result |= (long)end << START_TIME_TIME_SHIFT;
        }
        return result;
    }



    private static int getOffsetIndex(long timeRangeStamp) {
        return (int) ((timeRangeStamp & DATE_START_TIME) >>> START_TIME_TIME_SHIFT);
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


    private static final char ADJ_TIME_SEPARATOR = 'x';
    private static final String ADJ_ITEMS_SEPARATOR = "y";
    private static final String TIME_ENTRY_SEPARATOR = "z";
    private static final char END_TIME_FLAG = 'w';

    private static final int ADJ_RADIX = 30;
    private static final int TIME_RANGE_BITS = 5;
    private static final int TIME_RANGE_RADIX = 1 << TIME_RANGE_BITS;
    private static final int INVALID = -1;


    /**
     * 追加调整值
     * @param minuteTime 分钟精度时间段字符串
     * @param adjustOffset 调整值偏移量
     * @param startMinutes 开始分钟
     * @param endMinutes 结束分钟
     */
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

    /**
     * 更新调整值
     * @param minuteTime 分钟精度时间段字符串
     * @param adjustOffset 调整值偏移量
     * @param result 时间范围结果
     * @param startMinutes 开始分钟
     * @param endMinutes 结束分钟
     */
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
            int minutes = parseAdj(minuteTime, (end ? pos + 1 : pos), valueEnd);
            if (minutes == INVALID) {
                // 无效数字
                continue;
            }
            // 判断该调整点是否落在新时间范围内（内部边界，需要移除）
            boolean inside;
            if (startMinutes < endMinutes || endMinutes == 0) {
                // 非跨天并且不是正好边界
                inside = minutes > startMinutes && (endMinutes == 0 || minutes < endMinutes);
            } else {
                // 跨天并且不是正好边界：start > end，范围是 [start, 24:00) + [0, end)
                inside = minutes > startMinutes || minutes < endMinutes;
            }
            if (inside) {
                if (deleteStart == INVALID) {
                    deleteStart = pos;
                }
            } else {
                if (deleteStart != INVALID) {
                    // 移除该调整值（含后导分隔符）
                    minuteTime.delete(deleteStart, pos);
                    // delete 后后续内容左移到 deleteStart
                    valueEnd -= (pos - deleteStart);
                    deleteStart = INVALID;
                }
                if (end) {
                    if (endSlot != INVALID && Math.abs(endMinutes - minutes) < TIME_UNIT_IN_MINUTES && getEndIndex0(minutes) == endSlot) {
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
            if (deleteStart != adjustOffset) {
                // 如果不是从头开始删除，则删除的起始位置需要减最后一个分隔符
                deleteStart -= ADJ_ITEMS_SEPARATOR.length();
            }
            // 移除剩余的调整值
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

    private static int parseAdj(CharSequence minuteTime, int start, int end) {
        int result = 0;
        while (start < end) {
            int digit = Character.digit(minuteTime.charAt(start++), ADJ_RADIX);
            if (digit < 0) {
                return INVALID;
            }
            result = result * ADJ_RADIX + digit;
        }
        return result;
    }

    private static long parseTimeRangeStamp(CharSequence minuteTime, int start, int end) {
        long result = NONE;
        while (start < end) {
            int digit = Character.digit(minuteTime.charAt(start++), TIME_RANGE_RADIX);
            if (digit < 0) {
                return INVALID;
            }
            result = result * TIME_RANGE_RADIX + digit;
        }
        return result;
    }

}
