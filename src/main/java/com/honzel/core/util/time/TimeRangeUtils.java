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
import java.util.List;

/**
 * 时间段值转换
 * @author honzel
 * date 2021/1/4
 */

@SuppressWarnings("unchecked")
public class TimeRangeUtils {
    /**
     * 全部时间
     */
    public static final long ALL_TIMES;

    /**
     * 全部日期
     */
    public static final long ALL_WEEKDAYS;
    /**
     * 班次时间标识
     */
    public static final long SHIFT_TIME_FLAG;
    /**
     * 无设置值
     */
    public static final long NONE = 0L;
    /**
     * 日期开始时间
     */
    private static final long DATE_START_TIME;

    private static final int TIME_BITS = 48;

    private static final int START_TIME_BITS = 6;

    private static final int WEEKDAY_BITS = 7;

    private static final int TIME_UNIT_IN_MINUTES = 30;


    private static final long FIRST_BIT = 1L;


    static {
        // 时间位
        long result = NONE;
        for (int i = 0; i < TIME_BITS; i ++) {
            result |= (FIRST_BIT << i);
        }
        ALL_TIMES = result;
        // 开始位置位
        result = NONE;
        for (int i = TIME_BITS; i < TIME_BITS + START_TIME_BITS; i ++) {
            result |= (FIRST_BIT << i);
        }
        DATE_START_TIME = result;
        // 日期位
        result = NONE;
        for (int i = TIME_BITS + START_TIME_BITS; i < TIME_BITS + START_TIME_BITS + WEEKDAY_BITS; i ++) {
            result |= (FIRST_BIT << i);
        }
        ALL_WEEKDAYS = result;
        // 班次时间标识
        SHIFT_TIME_FLAG = FIRST_BIT << (TIME_BITS + START_TIME_BITS + WEEKDAY_BITS);
    }


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
            times = (times >>> offset) | ((~(-1 << offset) & times) << (TIME_BITS - offset));
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
            subRange.setStartTime(startTime.plusMinutes(i * stepDuration));
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
            if (TextUtils.containsValue(weekdays, i + 1)) {
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
        return fromTimeRanges0(timeRanges, false);
    }
    /**
     * 获取班次时间段值
     * @param timeRanges 时间范围列表
     * @return 时间段值
     */
    public static long fromShiftTimeRanges(List<? extends TimeRange> timeRanges) {
        return fromTimeRanges0(timeRanges, true);
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
            if (timeRange.getEndTime() == null) {
                throw new DateTimeException("结束时间不能为空");
            }
            long time = fromTimeRange0(timeRange.getStartTime(), timeRange.getEndTime(), true, false);
            if (time == NONE) {
                throw new DateTimeException("时间段长度必须都大于" + TIME_UNIT_IN_MINUTES + "分钟");
            }
            if ((time & result) != NONE) {
                throw new DateTimeException("时间段不能出现重叠");
            }
            long end = FIRST_BIT << (getIndexByTime(timeRange.getEndTime(), true) - 1);
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
     * @return 时间段值
     */
    private static long fromTimeRanges0(List<? extends TimeRange> timeRanges, boolean forceShift) {
        if (timeRanges == null || timeRanges.isEmpty()) {
            return NONE;
        }
        boolean fetchOffset = true;
        long result = NONE;
        for (TimeRange timeRange : timeRanges) {
            // 获取时间段
            long range = fromTimeRange0(timeRange.getStartTime(), timeRange.getEndTime(), forceShift, fetchOffset);
            // 只获取第一次的跨天位置
            fetchOffset = fetchOffset && (range & START_TIME_BITS) == NONE;
            // 并入时段
            result |= range;
        }
        return (forceShift && result != NONE) ? result | SHIFT_TIME_FLAG : result;
    }


    /**
     * 获取时间段值
     * @param weekdays 星期(周一为1;周二为2;...), 多个用英文逗号(,)分隔
     * @param timeRanges 时间范围列表
     * @return 时间段值
     */
    public static long from(String weekdays, List<? extends TimeRange> timeRanges) {
        return fromWeekDays(weekdays) | fromTimeRanges(timeRanges);
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
        return time != null ? FIRST_BIT << getIndexByTime(time, false) : NONE;
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
        int start = getIndexByTime(startTime, false);
        int end = getIndexByTime(endTime, true);
        long startValue = FIRST_BIT << start;
        if (end == TIME_BITS || start < end && startTime.isBefore(endTime)) {
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


    private static int getOffsetIndex(long timeRangeStamp) {
        return (int) ((timeRangeStamp & DATE_START_TIME) >>> TIME_BITS);
    }

    private static LocalTime parseTime(int seq) {
        return LocalTime.MIN.plusMinutes(seq * TIME_UNIT_IN_MINUTES);
    }

    /**
     * 获取时间对应的段值
     * @param time 时间
     * @param isEnd 是否是时间段的结束点
     * @return 返回时间index
     */
    private static int getIndexByTime(LocalTime time, boolean isEnd) {
        int minutes = time.get(ChronoField.MINUTE_OF_DAY);
        if (isEnd) {
            // 如果结束时间为零，可以认为是24:00
            return minutes == 0 ? TIME_BITS : (minutes - 1) / TIME_UNIT_IN_MINUTES + 1;
        }
        return minutes / TIME_UNIT_IN_MINUTES;
    }



}
