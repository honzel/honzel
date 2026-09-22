package com.honzel.core.util.time;

import com.honzel.core.constant.NumberConstants;
import com.honzel.core.util.text.TextUtils;
import com.honzel.core.vo.KeyValue;

import javax.annotation.PostConstruct;
import java.time.*;
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
    public static<T extends TimeRange> List<T> getTimeRanges(long timeRangeStamp) {
        return getTimeRanges(timeRangeStamp, 0, false);
    }
    /**
     * 获取时间范围列表
     * @param timeRangeStamp 时间段值
     * @param divisionDuration 切割时长（单位为分钟)
     * @return 返回拆分后的时间段列表
     */
    public static<T extends TimeRange> List<T> getTimeRanges(long timeRangeStamp, int divisionDuration) {
        return getTimeRanges(timeRangeStamp, divisionDuration, false);
    }

    /**
     * 获取时间范围列表
     * @param timeRangeStamp 时间段值
     * @param divisionDuration 切割时长（单位为分钟), 0为不切割
     * @param halfDivisionDurationEnabled 是否步长为一半切割时长, true-步长为切割时长的一半, false-步长与切割时长相等
     * @return 返回拆分后的时间段列表
     */
    public static<T extends TimeRange> List<T> getTimeRanges(long timeRangeStamp, int divisionDuration, boolean halfDivisionDurationEnabled) {
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
     * @param <T> 时间段类型
     */
    private static<T extends TimeRange> List<T> getTimeRanges0(long timeRangeStamp, String adjTime, int adjStart, int adjEnd, int divisionDuration, boolean halfDivisionDurationEnabled) {
        long times;
        if (timeRangeStamp == NONE || (times = timeRangeStamp & ALL_TIMES) == NONE) {
            return Collections.emptyList();
        }
        //是否班次时间
        boolean shiftFlag = (timeRangeStamp & SHIFT_TIME_FLAG) != NONE;
        List<T> timeRangeList = new ArrayList<>();
        // 日期起始位
        int offset = getOffsetIndex(timeRangeStamp);
        if (offset > 0) {
            times = (times >>> offset) | ((~(-FIRST_BIT << offset) & times) << (TIME_BITS - offset));
        }
        // 获取第一个开始位
        int firstStart = Long.numberOfTrailingZeros(times);
        if (firstStart != 0) {
            times >>>= firstStart;
        }
        T firstRange = (T) getInstance().newTimeRange();
        firstRange.setStartTime(parseTime((offset + firstStart) % TIME_BITS));
        // 获取第一个结束位
        int firstBits = Long.numberOfTrailingZeros(~times);
        int firstEnd = firstStart + firstBits;
        addEndTimeAndDivision(timeRangeList, firstRange, adjTime, adjStart, adjEnd, divisionDuration, halfDivisionDurationEnabled, shiftFlag, offset, firstEnd);
        if ((times >>>= firstBits) == NONE) {
            // 只有一个时间段
            return timeRangeList;
        }
        T timeRange = null;
        for (int i = firstEnd; i < TIME_BITS; i ++, times >>>= 1) {
            if ((times & FIRST_BIT) == NONE) {
                if (timeRange != null) {
                    addEndTimeAndDivision(timeRangeList, timeRange, adjTime, adjStart, adjEnd, divisionDuration, halfDivisionDurationEnabled, shiftFlag, offset, i);
                    timeRange = null;
                }
                if (times == NONE) {
                    break;
                }
            } else {
                if (timeRange == null) {
                    timeRange = (T) getInstance().newTimeRange();
                    timeRange.setStartTime(parseTime((offset + i) % TIME_BITS));
                }
            }
        }
        if (timeRange != null) {
            // 添加最后一个时间段
            addEndTimeAndDivision(timeRangeList, timeRange, adjTime, adjStart, adjEnd, divisionDuration, halfDivisionDurationEnabled, shiftFlag, offset, TIME_BITS);
        }
        return timeRangeList;
    }

    private static <T extends TimeRange> void addEndTimeAndDivision(List<T> timeRangeList, T timeRange, String adjTime, int adjStart, int adjEnd, int divisionDuration, boolean halfDivisionDurationEnabled, boolean shiftFlag, int offset, int end) {
        if (shiftFlag) {
            timeRange.setEndTime(end == TIME_BITS - 1 && offset == 0 ? LocalTime.MAX : parseTime((offset + end) % TIME_BITS + 1));
        } else {
            timeRange.setEndTime(end == TIME_BITS && offset == 0 ? LocalTime.MAX : parseTime((offset + end) % TIME_BITS));
        }
        // 应用调整值
        List<T> subRanges = applyAdjustments(timeRange, adjTime, adjStart, adjEnd);
        if (subRanges != null) {
            // 如果有拆分成多个时间段则遍历子时间段
            for (T subRange : subRanges) {
                // 按切割时长拆分时间段
                addDivideTimeRange(subRange, timeRangeList, divisionDuration, halfDivisionDurationEnabled);
            }
        } else {
            // 按切割时长拆分时间段
            addDivideTimeRange(timeRange, timeRangeList, divisionDuration, halfDivisionDurationEnabled);
        }
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
            maxDuration = ONE_DAY_MINUTES + maxDuration;
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
            int start;
            if (offset != 0) {
                start = Long.numberOfTrailingZeros(timeRangeStamp & (-FIRST_BIT << offset));
            } else {
                start = Long.numberOfTrailingZeros(timeRangeStamp);
            }
            return start >= TIME_BITS ? INVALID : start;
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
        return dayOfWeek != null ? getFirstOrLastTime0(minuteTime, FIRST_BIT << (WEEKDAY_SHIFT + dayOfWeek.ordinal()), false) : null;
    }


    /**
     * 获取最后结束时间点
     * @param timeRangeStamp 时间段值
     * @return 返回最后结束时间点
     */
    public static LocalTime getLastEndTime(long timeRangeStamp) {
        int index = getLastEndTimeIndex(timeRangeStamp);
        return index != INVALID ? (index == TIME_BITS ? LocalTime.MAX : parseTime(index)) : null;
    }

    /**
     * 获取最后结束时间点
     * @param minuteTime 分钟精度时间段值
     * @return 返回最后结束时间点
     */
    public static LocalTime getLastEndTime(String minuteTime, DayOfWeek dayOfWeek) {
        return dayOfWeek != null ? getFirstOrLastTime0(minuteTime, FIRST_BIT << (WEEKDAY_SHIFT + dayOfWeek.ordinal()), true) : null;
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
            if (offset != 0) {
                // 跨天时，返回当日的最后一段的结束位置
                return offset;
            }
            int index = Long.SIZE - Long.numberOfLeadingZeros(timeRangeStamp & ALL_TIMES);
            if ((timeRangeStamp & SHIFT_TIME_FLAG) != NONE) {
                // 是班次时间
                index ++;
            }
            return index == 0 ? TIME_BITS : index;
        }
        return INVALID;
    }


    private static LocalTime getFirstOrLastTime0(String minuteTime, long timeRangeMask, boolean last) {
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
            long time = parseTimeValue(minuteTime, pos, entryEnd);
            return time == INVALID ? null : last ? getLastEndTime(time) : getFirstStartTime(time);
        }
        // 获取调整值的时间段值
        long time = parseTimeValue(minuteTime, adjustmentEnd + 1, entryEnd);
        if (time == INVALID) {
            return null;
        }
        // 解析调整值的分钟数
        if (last) {
            int index = getLastEndTimeIndex(time);
            if (index == INVALID) {
                return null;
            }
            // 调整值的分钟数
            int minutes = endOfAdjustment(minuteTime, pos, adjustmentEnd, index);
            return minutes != INVALID ? LocalTime.MIN.plusMinutes(minutes) : index == TIME_BITS ? LocalTime.MAX : parseTime(index);
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

    /**
     * 获取分钟精度下实际的最早开始时间点
     * <p>在位图首个覆盖 slot（{@code timeIndex}）内查找调整值，根据该 slot 内分钟值最小（即时间上最早）的调整值判定起始状态：</p>
     * <ul>
     *   <li>最早调整值为 START：slot 边界未被覆盖，覆盖从该 START 分钟开始，返回其分钟值</li>
     *   <li>最早调整值为 END 或该 slot 无调整值：覆盖从 slot 边界（{@code timeIndex * 30}）开始，返回 {@link #INVALID} 由调用方回退到边界值</li>
     * </ul>
     *
     * @param minuteTime      分钟精度时间段字符串
     * @param adjustmentStart 调整值区域起始位置
     * @param adjustmentEnd   调整值区域结束位置
     * @param timeIndex       位图首个覆盖的 slot 下标
     * @return 实际最早开始的分钟数（minute-of-day）；若从 slot 边界对齐开始则返回 {@link #INVALID}
     */
    private static int startOfAdjustment(String minuteTime, int adjustmentStart, int adjustmentEnd, int timeIndex) {
        // 记录首个覆盖 slot 内分钟值最小（时间最早）的调整值及其类型
        int first = INVALID;
        boolean firstIsEnd = false;
        int pos = adjustmentStart;
        while (pos < adjustmentEnd) {
            boolean isEnd = minuteTime.charAt(pos) == END_TIME_FLAG;
            int valueStart = isEnd ? pos + 1 : pos;
            int valueEnd = minuteTime.indexOf(ADJ_ITEMS_SEPARATOR, pos);
            if (valueEnd == INVALID || valueEnd > adjustmentEnd) {
                valueEnd = adjustmentEnd;
            }
            pos = valueEnd + ADJ_ITEMS_SEPARATOR.length();
            int minutes = parseAdjValue(minuteTime, valueStart, valueEnd);
            if (minutes == INVALID || getStartIndex0(minutes) != timeIndex) {
                continue;
            }
            if (first == INVALID || minutes < first) {
                first = minutes;
                firstIsEnd = isEnd;
            }
        }
        // 最早调整值为 START 才表示覆盖从该分钟开始；为 END 或无调整值时从 slot 边界对齐开始
        return firstIsEnd ? INVALID : first;
    }
    /**
     * 获取分钟精度下实际的最晚结束时间点
     * <p>{@code timeIndex} 为位图的排他结束 slot 下标，故最后覆盖 slot 为 {@code timeIndex - 1}。
     * 在该 slot 内查找调整值，根据分钟值最大（即时间上最晚）的调整值判定结束状态：</p>
     * <ul>
     *   <li>最晚调整值为 END：覆盖不延伸到 slot 边界，在该 END 分钟结束，返回其分钟值</li>
     *   <li>最晚调整值为 START 或该 slot 无调整值：覆盖延伸到 slot 边界（{@code timeIndex * 30}），返回 {@link #INVALID} 由调用方回退到边界值</li>
     * </ul>
     *
     * @param minuteTime      分钟精度时间段字符串
     * @param adjustmentStart 调整值区域起始位置
     * @param adjustmentEnd   调整值区域结束位置
     * @param timeIndex       位图排他结束的 slot 下标
     * @return 实际最晚结束的分钟数（minute-of-day）；若延伸到 slot 边界对齐结束则返回 {@link #INVALID}
     */
    private static int endOfAdjustment(String minuteTime, int adjustmentStart, int adjustmentEnd, int timeIndex) {
        // 最后覆盖 slot 为排他结束下标的前一个 slot
        int lastSlot = timeIndex - 1;
        // 记录最后覆盖 slot 内分钟值最大（时间最晚）的调整值及其类型
        int last = INVALID;
        boolean lastIsEnd = false;
        int pos = adjustmentStart;
        while (pos < adjustmentEnd) {
            boolean isEnd = minuteTime.charAt(pos) == END_TIME_FLAG;
            int valueStart = isEnd ? pos + 1 : pos;
            int valueEnd = minuteTime.indexOf(ADJ_ITEMS_SEPARATOR, pos);
            if (valueEnd == INVALID || valueEnd > adjustmentEnd) {
                valueEnd = adjustmentEnd;
            }
            pos = valueEnd + ADJ_ITEMS_SEPARATOR.length();
            int minutes = parseAdjValue(minuteTime, valueStart, valueEnd);
            if (minutes == INVALID || getStartIndex0(minutes) != lastSlot) {
                continue;
            }
            if (last == INVALID || minutes > last) {
                last = minutes;
                lastIsEnd = isEnd;
            }
        }
        // 最晚调整值为 END 才表示覆盖在该分钟结束；为 START 或无调整值时延伸到 slot 边界对齐结束
        return lastIsEnd ? last : INVALID;
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
        return (timeRangeStamp & range) == range;
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
        return result != NONE ? result << WEEKDAY_SHIFT : NONE;
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
            int startMinutes = getTimeMinutes(startTime, false);
            int endMinutes = getTimeMinutes(endTime, true);
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
     * @param appendTimestamp 附加时间位图
     * @return 时间段值
     */
    private static long fromTimeRanges0(List<? extends TimeRange> timeRanges, boolean forceShift, StringBuilder minuteTime, boolean appendTimestamp) {
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
            int startMinutes = getTimeMinutes(startTime, false);
            int endMinutes = getTimeMinutes(endTime, true);
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
                    appendAdjustments(minuteTime, adjustOffset, true, startMinutes, true, endMinutes);
                }
            }
            // 只获取第一次的跨天位置
            if (fetchOffset && (range & DATE_START_TIME) != NONE) {
                fetchOffset = false;
            }
            // 并入时段
            result |= range;
        }
        if (forceShift && result != NONE) {
            result |= SHIFT_TIME_FLAG;
        }
        if (hasMinuteTimes) {
            if (appendTimestamp) {
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
        return result;
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
     * 获取所有时间范围集合
     * <p>从 {@link #from(String, List, StringBuilder)} 生成的 minuteTime 中解析出指定星期对应的时间范围列表。
     * 解析条目中的时间戳位值判断是否包含目标星期，再结合调整值还原分钟精度的时间边界。返回日期范围与时间范围的集合(key: 日期范围, value: 时间范围列表)
     * 当时间戳的日期区域为 0 时表示适用所有日期。</p>
     * @param minuteTime 分钟精度的时间段值
     * @param timeRangeMask 时间范围掩码, 包含日期和时间范围, 如果为0, 则表示获取首个时间范围集合
     * @return 返回该星期对应的时间范围列表
     * @param <T> 时间范围类型
     */
    public static<T extends TimeRange> List<KeyValue<String, List<T>>> getAllTimeRanges(String minuteTime, long timeRangeMask) {
        if (TextUtils.isEmpty(minuteTime)) {
            return Collections.emptyList();
        }
        // 拆分掉掩码中的日期区域，剩下的仅作为时间位掩码
        int days;
        if (timeRangeMask != NONE) {
            days = (int)((timeRangeMask & ALL_WEEKDAYS) >>> WEEKDAY_SHIFT);
            timeRangeMask &= ALL_TIMES;
        } else {
            days = 0;
        }
        List<KeyValue<String, List<T>>> result = new ArrayList<>();
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
                long stamp = parseTimeValue(minuteTime, timeStart, entryEnd);
                if (stamp != INVALID) {
                    if (timeRangeMask != NONE) {
                        // 时间范围掩码
                        stamp = (stamp & ~ALL_TIMES) | (stamp & timeRangeMask);
                    }
                    // 从时间戳获取基础时间范围
                    List<T> timeRanges = getTimeRanges0(stamp, minuteTime, pos, timeSep, 0, false);
                    if (!timeRanges.isEmpty()) {
                        // key 为该条目的日期范围（weekday 位不受时间掩码影响）
                        result.add(new KeyValue<>(getWeekDays(stamp), timeRanges));
                    }
                }
            }
            pos = entryEnd + TIME_ENTRY_SEPARATOR.length();
        }
        return result;
    }
    /**
     * 获取所有时间范围集合
     * <p>从 {@link #from(String, List, StringBuilder)} 生成的 minuteTime 中解析出指定星期对应的时间范围列表。
     * 解析条目中的时间戳位值判断是否包含目标星期，再结合调整值还原分钟精度的时间边界。返回日期范围与时间范围的集合(key: 日期范围, value: 时间范围列表)
     * 当时间戳的日期区域为 0 时表示适用所有日期。</p>
     * @param minuteTime 分钟精度的时间段值
     * @return 返回该星期对应的时间范围列表
     * @param <T> 时间范围类型
     */
    public static<T extends TimeRange> List<KeyValue<String, List<T>>> getAllTimeRanges(String minuteTime) {
        return getAllTimeRanges(minuteTime, NONE);
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
        timeRangeMask &= ALL_TIMES;
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
                long stamp = parseTimeValue(minuteTime, timeStart, entryEnd);
                if (stamp != INVALID) {
                    // 获取时间戳对应的时间范围
                    long time = timeRangeMask != NONE ? nonShift(stamp) & timeRangeMask : nonShift(stamp);
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
        int startMinutes = getTimeMinutes(startTime, false);
        int endMinutes;
        if (endTime != null) {
            int rawEnd = getTimeMinutes(endTime, true);
            // 结束分钟为 0（24:00）时按一天末尾 1440 处理，便于统一的边界比较
            endMinutes = rawEnd == 0 ? ONE_DAY_MINUTES : rawEnd;
        } else {
            // endTime 为 null 表示点查询：判断 startMinutes 这一点是否被覆盖，等价于查询 1 分钟区间 [startMinutes, startMinutes+1)。
            // 这样可保证 queryEndSlot 与 queryStartSlot 落在同一 slot（避免对齐点/00:00 时 getEndIndex0 把点当作排他结束而错位到前一 slot），
            // 且点恰好命中 START 调整值时（minutes == startMinutes）视为已覆盖（endGap 要求 minutes >= startMinutes+1 才判间隙）。
            endMinutes = startMinutes + 1;
        }
        // 跨天：开始分钟 > 结束分钟，查询范围为 [start, 24:00) + [0, end)
        boolean crossing = startMinutes > endMinutes;
        int queryStartSlot = getStartIndex0(startMinutes);
        int queryEndSlot = getEndIndex0(endMinutes) - 1;
        int pos = adjStart;
        // 查询起点所在 slot：记录 <= startMinutes 的最大调整值，若为结束调整值则起点落在间隙
        int startBnd = INVALID;
        boolean startGap = false;
        // 查询终点所在 slot：记录 >= endMinutes 的最小调整值，若为开始调整值则终点前落在间隙
        int endBnd = Integer.MAX_VALUE;
        boolean endGap = false;
        while (pos < adjEnd) {
            boolean isEnd = minuteTime.charAt(pos) == END_TIME_FLAG;
            int valueStart = isEnd ? pos + 1 : pos;
            int valueEnd = minuteTime.indexOf(ADJ_ITEMS_SEPARATOR, pos);
            if (valueEnd == INVALID || valueEnd > adjEnd) {
                valueEnd = adjEnd;
            }
            pos = valueEnd + ADJ_ITEMS_SEPARATOR.length();
            int minutes = parseAdjValue(minuteTime, valueStart, valueEnd);
            if (minutes == INVALID) {
                continue;
            }
            // 一、调整值落在查询范围内部：内部边界必然把查询范围切开 → 存在间隙
            boolean inside = crossing ? (minutes > startMinutes || minutes < endMinutes)
                    : (minutes > startMinutes && minutes < endMinutes);
            if (inside) {
                return false;
            }
            // 二、调整值在查询范围外，只可能出现在查询起点/终点所在的 slot 上，判断边界是否落在间隙
            int slot = getStartIndex0(minutes);
            if (slot == queryStartSlot && minutes <= startMinutes && minutes > startBnd) {
                // 起点侧最近的边界：结束调整值→起点未覆盖；开始调整值→起点已覆盖
                startBnd = minutes;
                startGap = isEnd;
            }
            if (slot == queryEndSlot && minutes >= endMinutes && minutes < endBnd) {
                // 终点侧最近的边界：开始调整值→终点前未覆盖；结束调整值→终点前已覆盖
                endBnd = minutes;
                endGap = !isEnd;
            }
        }
        return !startGap && !endGap;
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
        int days;
        if (timeRangeMask != NONE) {
            days = (int)((timeRangeMask & ALL_WEEKDAYS) >>> WEEKDAY_SHIFT);
            timeRangeMask &= ALL_TIMES;
        } else {
            days = 0;
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
                long stamp = parseTimeValue(minuteTime, timeStart, entryEnd);
                if (stamp != INVALID) {
                    if (timeRangeMask != NONE) {
                        // 时间范围掩码
                        stamp = (stamp & ~ALL_TIMES) | (stamp & timeRangeMask);
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
     * <p>规则：</p>
     * <ul>
     *   <li>位图内没有调整值命中的 slot 表示完整覆盖 [slotStart, slotEnd)</li>
     *   <li>slot 内首个调整值决定该 slot 起始状态：END 表示从 slot 边界起被覆盖，START 表示 slot 边界起未被覆盖</li>
     *   <li>slot 内末个调整值决定该 slot 结束状态：START 表示覆盖延伸到 slot 边界，END 表示不延伸</li>
     *   <li>调整值成对交替出现，START 触发 outside→inside，END 触发 inside→outside</li>
     * </ul>
     *
     * @param timeRange  基础时间范围（30分钟精度，方法可能就地修改其 startTime/endTime）
     * @param adjustmentTime  调整值字符串
     * @param adjStart  调整值起始位置
     * @param adjEnd    调整值结束位置
     * @return 若拆分为多个子时间段则返回列表；未拆分（含无调整值命中或仅得到单一子段）则返回 null，
     *         其中单一子段的情形下已就地修改 {@code timeRange} 的 startTime/endTime
     */
    private static<T extends TimeRange> List<T> applyAdjustments(T timeRange, String adjustmentTime, int adjStart, int adjEnd) {
        if (TextUtils.isEmpty(adjustmentTime) || adjStart >= adjEnd) {
            return null;
        }
        LocalTime startTime = timeRange.getStartTime();
        int startMinutes = getTimeMinutes(startTime, false);
        LocalTime endTime = timeRange.getEndTime();
        int endMinutes = getTimeMinutes(endTime, true);
        if (endMinutes == 0) {
            endMinutes = ONE_DAY_MINUTES;
        }
        // 跨天
        boolean crossing = startMinutes > endMinutes;
        List<T> subRanges = null;

        int minStart = INVALID;
        int maxEnd = INVALID;

        int sepLen = ADJ_ITEMS_SEPARATOR.length();
        int pos = adjStart;
        while (pos < adjEnd) {
            boolean isEnd = adjustmentTime.charAt(pos) == END_TIME_FLAG;
            int valueStart = isEnd ? pos + 1 : pos;
            int valueEnd = adjustmentTime.indexOf(ADJ_ITEMS_SEPARATOR, valueStart);
            if (valueEnd == INVALID || valueEnd > adjEnd) {
                valueEnd = adjEnd;
            }
            int minutes = parseAdjValue(adjustmentTime, valueStart, valueEnd);
            pos = valueEnd + sepLen;
            if (minutes == INVALID) {
                continue;
            }
            if (crossing) {
                if (minutes >= endMinutes && minutes < startMinutes) {
                    // 调整值在时间范围之外忽略
                    continue;
                }
            } else {
                if (minutes >= endMinutes || minutes < startMinutes) {
                    // 调整值在时间范围之外忽略
                    continue;
                }
            }
            if (isEnd) {
                if (minutes >= endMinutes - TIME_UNIT_IN_MINUTES && minutes < endMinutes) { //同一endSlot
                    if (maxEnd == INVALID) {
                        maxEnd = minutes;
                    } else {
                        if (minutes > maxEnd) {
                            int t = maxEnd; maxEnd = minutes; minutes = t;
                        }
                        subRanges = splitTimeRanges(subRanges, timeRange, startMinutes, crossing, minutes, true);
                    }
                    continue;
                }
            } else {
                if (minutes >= startMinutes && minutes < startMinutes + TIME_UNIT_IN_MINUTES) { //同一startSlot
                    if (minStart == INVALID) {
                        minStart = minutes;
                    } else {
                        if (minutes < minStart) {
                            int t = minStart; minStart = minutes; minutes = t;
                        }
                        subRanges = splitTimeRanges(subRanges, timeRange, startMinutes, crossing, minutes, false);
                    }
                    continue;
                }
            }
            subRanges = splitTimeRanges(subRanges, timeRange, startMinutes, crossing, minutes, isEnd);
        }
        if (subRanges == null) {
            // 不需要拆分
            return resetStartEndTime(timeRange, minStart, maxEnd, endMinutes - TIME_UNIT_IN_MINUTES == startMinutes);
        }
        if (minStart != INVALID) {
            T firstRange = subRanges.get(0);
            if (firstRange.getEndTime() != null
                    && (relativePos(startMinutes, getTimeMinutes(firstRange.getEndTime(), true), crossing) > relativePos(startMinutes, minStart, crossing))) {
                firstRange.setStartTime(LocalTime.MIN.plusMinutes(minStart));
            } else {
                subRanges = splitTimeRanges(subRanges, timeRange, startMinutes, crossing, minStart, false);
            }
        }
        if (maxEnd != INVALID) {
            T lastRange = subRanges.get(subRanges.size() - 1);
            if (lastRange.getStartTime() != null
                    && (relativePos(startMinutes, getTimeMinutes(lastRange.getStartTime(), false), crossing) < relativePos(startMinutes, maxEnd, crossing))) {
                lastRange.setEndTime(LocalTime.MIN.plusMinutes(maxEnd));
            } else {
                subRanges = splitTimeRanges(subRanges, timeRange, startMinutes, crossing, maxEnd, true);
            }
        }
        // 处理拆分后的子范围null的值
        resolveNullEnds(subRanges, timeRange, startMinutes, crossing);
        return subRanges;
    }

    private static <T extends TimeRange> List<T> resetStartEndTime(T timeRange, int minStart, int maxEnd, boolean sameSlot) {
        if (minStart < maxEnd) {
            if (minStart != INVALID) {
                timeRange.setStartTime(LocalTime.MIN.plusMinutes(minStart));
            }
            timeRange.setEndTime(LocalTime.MIN.plusMinutes(maxEnd));
        } else {
            if (sameSlot && maxEnd != INVALID) {
                // 创建新的时间范围
                T nextRange = createTimeRange(LocalTime.MIN.plusMinutes(minStart), timeRange.getEndTime());
                // 调整结束时间
                timeRange.setEndTime(LocalTime.MIN.plusMinutes(maxEnd));
                List<T> subRangesList = new ArrayList<>(NumberConstants.INTEGER_TWO);
                subRangesList.add(timeRange);
                subRangesList.add(nextRange);
                return subRangesList;
            } else {
                if (minStart != INVALID) {
                    // 调整开始时间
                    timeRange.setStartTime(LocalTime.MIN.plusMinutes(minStart));
                }
                if (maxEnd != INVALID) {
                    // 调整结束时间
                    timeRange.setEndTime(LocalTime.MIN.plusMinutes(maxEnd));
                }
            }
        }
        return null;
    }


    /**
     * 拆分时间范围（根据调整值拆分或开启/闭合子范围）
     * <p>subRanges 按时间顺序排列，但 minutes 插入位置不一定在末尾（startSlot/endSlot 交换或跨天场景），
     * 因此需要先定位 minutes 所在的子范围或插入点，再对对应时段做拆分处理。</p>
     * <p>规则：</p>
     * <ul>
     *   <li>{@code isEnd=true} 确认在subRanges的哪一段[start, end)，end!=null拆成[start, minutes), [null, end);
     *     如果end==null合并为[start, minutes)</li>
     *   <li>{@code isEnd=false} 确认在subRanges的哪一段[start, end)，start!=null拆成[start, null), [minutes, end);
     *      *     start==null合并为[minutes, end)</li>
     * </ul>
     *
     * @param subRanges 当前已拆分的子范围列表（按时间顺序），null 表示尚未拆分
     * @param minutes   调整值分钟数
     * @param isEnd     true 表示 END 调整值，false 表示 START 调整值
     * @return 更新后的子范围列表
     */
    private static <T extends TimeRange> List<T> splitTimeRanges(List<T> subRanges, T timeRange, int startMinutes, boolean crossing, int minutes, boolean isEnd) {
        boolean beforeStart = false;
        int insertIndex;
        if (subRanges == null) {
            // 初始为完整覆盖段 [timeRange.start, timeRange.end]
            subRanges = new ArrayList<>(NumberConstants.INTEGER_TWO);
            subRanges.add(createTimeRange(timeRange.getStartTime(), timeRange.getEndTime()));
            insertIndex = 0;
        } else {
            // 遍历已拆分的子范围，定位 minutes 所在的子范围或插入点
            insertIndex = INVALID;
            int pos = relativePos(startMinutes, minutes, crossing);
            for (int i = subRanges.size() - 1; i >= 0; i--) {
                T seg = subRanges.get(i);
                if (seg.getStartTime() != null) {
                    // 有开始时间
                    if (pos < relativePos(startMinutes, getTimeMinutes(seg.getStartTime(), false), crossing)) {
                        // 如果在当前段开始时间之前，则继续检查前一段
                        beforeStart = true;
                        continue;
                    }
                    // 匹配到时间段
                    insertIndex = i;
                    beforeStart = false;
                    break;
                }
                // 没有开始时间必然有结束时间
                if (pos >= relativePos(startMinutes, getTimeMinutes(seg.getEndTime(), true), crossing)) {
                    // 如果在当前段结束时间之后，则认为在后一段
                    insertIndex = i + 1;
                } else {
                    // 如果在当前段结束时间之前，则认为在当前段
                    insertIndex = i;
                    beforeStart = false;
                }
                break;
            }
            // 如果 insertIndex 未更新，则返回原列表
            if (insertIndex == INVALID || insertIndex == subRanges.size()) {
                return subRanges;
            }
        }
        // 获取插入位置的子范围
        T seg = subRanges.get(insertIndex);
        LocalTime minuteTime = LocalTime.MIN.plusMinutes(minutes);
        if (isEnd) {
            if (beforeStart) {
                // 在当前段开始时间之前，插入 [null, minutes)
                subRanges.add(insertIndex, createTimeRange(null, minuteTime));
            } else if (seg.getEndTime() != null) {
                // [start, end) 拆成 [start, minutes), [null, end)
                LocalTime oldEnd = seg.getEndTime();
                seg.setEndTime(minuteTime);
                subRanges.add(insertIndex + 1, createTimeRange(null, oldEnd));
            } else {
                // end==null 合并为 [start, minutes)
                seg.setEndTime(minuteTime);
            }
        } else {
            if (seg.getStartTime() != null) {
                if (beforeStart) {
                    // 在当前段开始时间之前，插入 [null, minutes)
                    subRanges.add(insertIndex, createTimeRange(minuteTime, null));
                } else {
                    // [start, end) 拆成 [start, null), [minutes, end)
                    LocalTime oldEnd = seg.getEndTime();
                    seg.setEndTime(null);
                    subRanges.add(insertIndex + 1, createTimeRange(minuteTime, oldEnd));
                }
            } else {
                // start==null 合并为 [minutes, end)
                seg.setStartTime(minuteTime);
            }
        }
        return subRanges;
    }

    /**
     * 所有调整值处理完毕后，将 subRanges 中为 null 的端用另一端相邻值所在 slot 的开始/结束位置替代：
     * <ul>
     *   <li>{@code [start, null)}：null end 用后一段 start 值所在 slot 的开始位置替代（无后段则用 timeRange.end）</li>
     *   <li>{@code [null, end)}：null start 用前一段 end 值所在 slot 的结束位置替代（无前段则用 timeRange.start）</li>
     * </ul>
     * <p><b>扩张语义：</b>null 端表示「此处无显式边界，覆盖延续到下一个真实边界」。解析完成后若
     * {@code start >= end}（相对位）则该段无效，就地移除；移除后相邻段的 null 端会自然扩张到下一个
     * <i>真实</i>边界，从而覆盖被移除段留下的空隙。这是设计意图，而非缺陷。</p>
     * <p><b>重要：</b>必须保持「倒序遍历 + 解析后立即移除」的单趟结构。因为倒序下处理 {@code seg[i]} 的
     * null end 时，{@code get(i+1)} 已在上一次迭代处理并可能已被移除，故能越过无效段扩张到下一真实边界；
     * 若改成「先全部解析、再统一移除」的两趟式，{@code get(i+1)} 仍指向无效段，扩张语义将被破坏。</p>
     * <p><b>方向不对称：</b>null end 向后看 {@code get(i+1)}（已处理、可能已移除）会跨无效段扩张；
     * null start 向前看 {@code get(i-1)}（倒序下尚未处理、不会被移除）不会跨无效段扩张。对规范的成对交替
     * 输入无影响。</p>
     */
    private static <T extends TimeRange> void resolveNullEnds(List<T> subRanges, T timeRange, int startMinutes, boolean crossing) {
        if (subRanges == null) {
            return;
        }
        for (int i = subRanges.size() - 1; i >= 0; i--) {
            T seg = subRanges.get(i);
            if (seg.getStartTime() == null) {
                if (i > 0) {
                    // 向前看 get(i-1)：倒序下前段尚未处理、不会被移除，故不跨无效段扩张
                    LocalTime preEndTime = subRanges.get(i - 1).getEndTime();
                    if (preEndTime == null) {
                        // 前段结束时间为空，用前段开始时间所在 slot 的开始位置替代,即两段合并, 与外层 i-- 叠加后指向 P 前一段
                        seg.setStartTime(subRanges.remove(--i).getStartTime());
                    } else {
                        seg.setStartTime(parseTime(getEndIndex0(getTimeMinutes(preEndTime, true))));
                    }
                } else {
                    seg.setStartTime(timeRange.getStartTime());
                }
            } else if (seg.getEndTime() == null) {
                if (i < subRanges.size() - 1) {
                    // 向后看 get(i+1)：已在上一次迭代处理并可能已被移除，故此处会越过无效段扩张到下一真实边界
                    LocalTime nextStartTime = subRanges.get(i + 1).getStartTime();
                    seg.setEndTime(parseTime(getStartIndex0(getTimeMinutes(nextStartTime, false))));
                } else {
                    seg.setEndTime(timeRange.getEndTime());
                }
            }
            // 倒序遍历，解析后立即删除无效段（start >= end 相对位）；
            // 该「就地移除」是相邻 null 端扩张到下一真实边界的前提，勿改为两趟式
            if (relativePos(startMinutes, getTimeMinutes(seg.getStartTime(), false), crossing)
                    >= relativePos(startMinutes, getTimeMinutes(seg.getEndTime(), true), crossing)) {
                subRanges.remove(i);
            }
        }
    }

    /**
     * 计算 minute 相对于 timeRange.startTime 的位置（用于跨天场景下的顺序比较）
     */
    private static int relativePos(int startMinutes, int minute, boolean crossing) {
        if (crossing) {
            return minute >= startMinutes ? minute - startMinutes : ONE_DAY_MINUTES + minute - startMinutes;
        }
        return minute - startMinutes;
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
        return fromTimeRange0(getTimeMinutes(startTime, false), getTimeMinutes(endTime, true), forceShift, fetchOffset);
    }

    private static int getTimeMinutes(LocalTime time, boolean isEnd) {
        return isEnd && time.getSecond() > 0 ? time.get(ChronoField.MINUTE_OF_DAY) + 1 : time.get(ChronoField.MINUTE_OF_DAY);
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

    /**
     * 一天的分钟数
     */
    private static final int ONE_DAY_MINUTES = TIME_BITS * TIME_UNIT_IN_MINUTES;

    /**
     * 调整值进制
     */
    private static final int ADJ_RADIX = TIME_UNIT_IN_MINUTES;
    /**
     * 调整值最大限制
     */
    private static final int ADJ_MAX_LIMIT = TIME_BITS;
    /**
     * 时间段位数
     */
    private static final int TIME_RANGE_BITS = 5;
    /**
     * 时间段进制
     */
    private static final int TIME_RANGE_RADIX = 1 << TIME_RANGE_BITS;
    /**
     * 时间段最大限制
     */
    private static final long TIME_RANGE_MAX_LIMIT = Long.MAX_VALUE >>> TIME_RANGE_BITS;
    /**
     * 无效值
     */
    private static final int INVALID = -1; // 无效值


    /**
     * 追加调整值
     * @param minuteTime 分钟精度时间段字符串
     * @param adjustOffset 调整值偏移量
     * @param startMinutes 开始分钟
     * @param endMinutes 结束分钟
     */
    private static void appendAdjustments(StringBuilder minuteTime, int adjustOffset, boolean addStart, int startMinutes, boolean andEnd, int endMinutes) {
        if (addStart && startMinutes % TIME_UNIT_IN_MINUTES != 0) {
            if (minuteTime.length() > adjustOffset) {
                minuteTime.append(ADJ_ITEMS_SEPARATOR);
            }
            minuteTime.append(Integer.toUnsignedString(startMinutes, ADJ_RADIX));
        }
        if (andEnd && endMinutes % TIME_UNIT_IN_MINUTES != 0) {
            if (minuteTime.length() > adjustOffset) {
                minuteTime.append(ADJ_ITEMS_SEPARATOR + END_TIME_FLAG);
            } else {
                minuteTime.append(END_TIME_FLAG);
            }
            minuteTime.append(Integer.toUnsignedString(endMinutes, ADJ_RADIX));
        }
    }

    /**
     * 更新调整值（合并调整值，保持成对特性且不相互覆盖）
     * <p>扫描已有的调整值对，将落在 [startMinutes, endMinutes) 内的内部边界移除，
     * 并根据相邻调整值类型决定是否补充新的开始/结束调整值。</p>
     *
     * @param minuteTime     分钟精度时间段字符串
     * @param adjustOffset   调整值偏移量
     * @param timestamp      时间戳
     * @param startMinutes   开始分钟
     * @param endMinutes     结束分钟
     */
    private static void updateAdjustments(StringBuilder minuteTime, int adjustOffset, long timestamp, int startMinutes, int endMinutes) {
        int startSlot = getStartIndex0(startMinutes);
        int endSlotIdx = getEndIndex0(endMinutes);

        // slot 未覆盖时直接确定需要加入，不需要计算 nearly 值
        boolean needNearlyStart = (timestamp & (FIRST_BIT << startSlot)) != NONE;
        boolean needNearlyEnd = (timestamp & (FIRST_BIT << (endSlotIdx - 1))) != NONE;

        // 同一 slot 中最靠近 startMinutes/endMinutes 的最靠近的调整值
        int nearlyStart = INVALID;
        boolean nearlyStartIsEnd = false;
        int nearlyEnd = INVALID;
        boolean nearlyEndIsEnd = false;

        int pos = adjustOffset;
        int deleteStart = INVALID;

        while (pos < minuteTime.length()) {
            int valueEnd = minuteTime.indexOf(ADJ_ITEMS_SEPARATOR, pos);
            if (valueEnd == -1) {
                valueEnd = minuteTime.length();
            }
            boolean isEnd = minuteTime.charAt(pos) == END_TIME_FLAG;
            int minutes = parseAdjValue(minuteTime, (isEnd ? pos + 1 : pos), valueEnd);
            if (minutes == INVALID) {
                pos = valueEnd + 1;
                continue;
            }
            // 类型敏感的边界判断：
            // END 类型 -> [startMinutes, endMinutes)；START 类型 -> (startMinutes, endMinutes]
            boolean startInside;
            boolean endInside;
            if (isEnd) {
                startInside = minutes >= startMinutes;
                endInside = minutes < endMinutes;
            } else {
                startInside = minutes > startMinutes;
                endInside = minutes <= endMinutes;
            }
            boolean inside = startMinutes < endMinutes ? startInside && endInside : startInside || endInside;
            if (inside) {
                // 规则1：在时间范围内的调整值移除
                if (deleteStart == INVALID) {
                    deleteStart = pos;
                }
            } else {
                if (deleteStart != INVALID) {
                    // 移除连续删除段
                    minuteTime.delete(deleteStart, pos);
                    valueEnd -= (pos - deleteStart);
                    deleteStart = INVALID;
                }
            }
            // 只有需要时才计算同一 slot 中的 nearly 值
            if (needNearlyStart && getStartIndex0(minutes) == startSlot) {
                if (nearlyStart == INVALID || Math.abs(minutes - startMinutes) < Math.abs(nearlyStart - startMinutes)) {
                    nearlyStart = minutes;
                    nearlyStartIsEnd = isEnd;
                }
            }
            if (needNearlyEnd && getEndIndex0(isEnd ? minutes : minutes + 1) == endSlotIdx) {
                if (nearlyEnd == INVALID || Math.abs(minutes - endMinutes) < Math.abs(nearlyEnd - endMinutes)) {
                    nearlyEnd = minutes;
                    nearlyEndIsEnd = isEnd;
                }
            }
            pos = valueEnd + 1;
        }
        // 处理末尾连续的删除区域
        if (deleteStart != INVALID) {
            if (deleteStart != adjustOffset) {
                deleteStart -= ADJ_ITEMS_SEPARATOR.length();
            }
            minuteTime.delete(deleteStart, minuteTime.length());
        }

        // 规则2：判断是否加入 startMinutes
        boolean addStart;
        if (needNearlyStart) {
            addStart = nearlyStart != INVALID && ((nearlyStartIsEnd && nearlyStart < startMinutes) || (!nearlyStartIsEnd && nearlyStart > startMinutes));
        } else {
            addStart = true;
        }
        // 规则3：判断是否加入 endMinutes
        boolean addEnd;
        if (needNearlyEnd) {
            addEnd = nearlyEnd != INVALID && ((nearlyEndIsEnd && nearlyEnd < endMinutes) || (!nearlyEndIsEnd && nearlyEnd > endMinutes));
        } else {
            addEnd = true;
        }
        // 新调整值追加到末尾
        appendAdjustments(minuteTime, adjustOffset, addStart, startMinutes, addEnd, endMinutes);
    }

    private static int parseAdjValue(CharSequence minuteTime, int start, int end) {
        int result = 0;
        while (start < end) {
            int digit = Character.digit(minuteTime.charAt(start++), ADJ_RADIX);
            if (digit < 0 || result > ADJ_MAX_LIMIT || digit != 0 && result == ADJ_MAX_LIMIT) {
                return INVALID;
            }
            result = result * ADJ_RADIX + digit;
        }
        return result;
    }

    private static long parseTimeValue(CharSequence minuteTime, int start, int end) {
        long result = NONE;
        while (start < end) {
            int digit = Character.digit(minuteTime.charAt(start++), TIME_RANGE_RADIX);
            if (digit < 0 || result >= TIME_RANGE_MAX_LIMIT) {
                return INVALID;
            }
            result = (result << TIME_RANGE_BITS) | digit;
        }
        return result;
    }

}
