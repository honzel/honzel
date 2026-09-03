package com.honzel.test;

import com.honzel.core.util.text.TextUtils;
import com.honzel.core.util.time.TimeRange;
import com.honzel.core.util.time.TimeRangeUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TimeRangeTester {


	public static void main(String[] args) throws Exception {
		TimeRangeTester tester = new TimeRangeTester();
		tester.testTimeRange();
		System.out.println();
		tester.testGetTimeRangesOfDay();
	}

	private static class TimeRangeUtils1 extends TimeRangeUtils {
	    static {
	        new TimeRangeUtils1();
        }
    }

	private void testGetTimeRangesOfDay() {
		// 周一的时间范围：08:15-12:10, 18:00-02:15
		StringBuilder minuteTime = new StringBuilder();
		List<TimeRange> mondayRanges = new ArrayList<>();
		mondayRanges.add(new TimeRange(LocalTime.parse("08:15"), LocalTime.parse("12:10")));
		mondayRanges.add(new TimeRange(LocalTime.parse("18:00"), LocalTime.parse("02:15")));
		TimeRangeUtils.from("1", mondayRanges, minuteTime);
		System.out.println("minuteTime: " + minuteTime);

		List<TimeRange> result = TimeRangeUtils.getMinuteTimeRanges(minuteTime.toString(), DayOfWeek.MONDAY);
		System.out.println("Monday time ranges: " + result);

		// 周二应该没有数据
		List<TimeRange> tuesdayResult = TimeRangeUtils.getMinuteTimeRanges(minuteTime.toString(), DayOfWeek.TUESDAY);
		System.out.println("Tuesday time ranges: " + tuesdayResult);

		// 多个日期共用同一个 minuteTime
		StringBuilder multiMinuteTime = new StringBuilder();
		List<TimeRange> tueRanges = new ArrayList<>();
		tueRanges.add(new TimeRange(LocalTime.parse("09:00"), LocalTime.parse("17:30")));
		TimeRangeUtils.from("2", tueRanges, multiMinuteTime);
		System.out.println("\nMulti-day minuteTime: " + multiMinuteTime);

		List<TimeRange> monResult2 = TimeRangeUtils.getMinuteTimeRanges(multiMinuteTime.toString(), DayOfWeek.MONDAY);
		System.out.println("Monday from multi: " + monResult2);
		List<TimeRange> tueResult2 = TimeRangeUtils.getMinuteTimeRanges(multiMinuteTime.toString(), DayOfWeek.TUESDAY);
		System.out.println("Tuesday from multi: " + tueResult2);

		// 对齐边界的情况（无调整值）
		StringBuilder alignedMinuteTime = new StringBuilder();
		List<TimeRange> alignedRanges = new ArrayList<>();
		alignedRanges.add(new TimeRange(LocalTime.parse("08:00"), LocalTime.parse("12:00")));
		TimeRangeUtils.from("1", alignedRanges, alignedMinuteTime);
		System.out.println("\nAligned minuteTime: " + alignedMinuteTime);
		List<TimeRange> alignedResult = TimeRangeUtils.getMinuteTimeRanges(alignedMinuteTime.toString(), DayOfWeek.MONDAY);
		System.out.println("Aligned Monday: " + alignedResult);

		// 不指定星期（weekday 位为 0），适用所有日期
		StringBuilder allDaysMinuteTime = new StringBuilder();
		List<TimeRange> allDaysRanges = new ArrayList<>();
		allDaysRanges.add(new TimeRange(LocalTime.parse("10:15"), LocalTime.parse("14:30")));
		TimeRangeUtils.fromTimeRanges(allDaysRanges, allDaysMinuteTime);
		System.out.println("\nAll-days minuteTime: " + allDaysMinuteTime);
		List<TimeRange> allDaysResult1 = TimeRangeUtils.getMinuteTimeRanges(allDaysMinuteTime.toString(), DayOfWeek.WEDNESDAY);
		System.out.println("Wednesday from all-days: " + allDaysResult1);
		List<TimeRange> allDaysResult2 = TimeRangeUtils.getMinuteTimeRanges(allDaysMinuteTime.toString(), DayOfWeek.SUNDAY);
		System.out.println("Sunday from all-days: " + allDaysResult2);
	}

	private void testTimeRange() {
		List<TimeRange> timeRangeList = new ArrayList<>();
		timeRangeList.add(new TimeRange(LocalTime.parse("04:30"), LocalTime.parse("12:00")));
		timeRangeList.add(new TimeRange(LocalTime.parse("12:00"), LocalTime.parse("16:00")));
		timeRangeList.add(new TimeRange(LocalTime.parse("18:00"), LocalTime.parse("02:30")));
		System.out.println("getTimeRanges:------------------------------------------");
		System.out.println(TextUtils.toString(TimeRangeUtils1.getTimeRanges(TimeRangeUtils.fromTimeRanges(timeRangeList), 60, true)));
		System.out.println("getTimeRanges:------------------------------------------");
		System.out.println(TextUtils.toString(TimeRangeUtils.getTimeRanges(TimeRangeUtils.fromTimeRanges(timeRangeList), 60, false)));
		System.out.println("getTimeRanges:------------------------------------------");
		System.out.println(TextUtils.toString(TimeRangeUtils.getTimeRanges(TimeRangeUtils.fromTimeRanges(timeRangeList))));
		System.out.println("getTimeRanges:------------------------------------------");
		System.out.println(TextUtils.toString(TimeRangeUtils.getTimeRanges(TimeRangeUtils.fromTimeRanges(timeRangeList), 30)));

		System.out.println("getShiftTimeRanges:------------------------------------------");
		System.out.println(TextUtils.toString(TimeRangeUtils.getTimeRanges(TimeRangeUtils.fromShiftTimeRanges(timeRangeList), 60, true)));
		System.out.println("getShiftTimeRanges:------------------------------------------");
		System.out.println(TextUtils.toString(TimeRangeUtils.getTimeRanges(TimeRangeUtils.fromShiftTimeRanges(timeRangeList), 60, false)));
		System.out.println("getShiftTimeRanges:------------------------------------------");
		System.out.println(TextUtils.toString(TimeRangeUtils.getTimeRanges(TimeRangeUtils.fromShiftTimeRanges(timeRangeList), 30)));
		System.out.println("getShiftTimeRanges:------------------------------------------");
		System.out.println(TextUtils.toString(TimeRangeUtils.getTimeRanges(TimeRangeUtils.fromShiftTimeRanges(timeRangeList))));
		System.out.println("nonShift:------------------------------------------");
		System.out.println(TextUtils.toString(TimeRangeUtils.getTimeRanges(TimeRangeUtils.nonShift(TimeRangeUtils.fromShiftTimeRanges(timeRangeList)))));
	}

}
