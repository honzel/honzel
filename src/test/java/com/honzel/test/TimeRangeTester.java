package com.honzel.test;

import com.honzel.core.util.text.TextUtils;
import com.honzel.core.util.time.TimeRange;
import com.honzel.core.util.time.TimeRangeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimeRangeTester {


	public static void main(String[] args) throws Exception {
		TimeRangeTester tester = new TimeRangeTester();
		tester.testTimeRange();
	}

	private static class TimeRangeUtils1 extends TimeRangeUtils {
	    static {
	        new TimeRangeUtils1();
        }
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
