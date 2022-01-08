package com.honzel.test;

import com.honzel.core.util.text.TextUtils;
import com.honzel.core.util.time.TimeRange;
import com.honzel.core.util.time.TimeRangeUtils;
import com.honzel.core.util.web.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextTester {
	
	private static final Logger log = LoggerFactory.getLogger(TextTester.class);

	public static void main(String[] args) throws Exception {
		TextTester tester = new TextTester();
		tester.testWeb();
	}
	private static class FileItemInfo extends FileItem {

		public FileItemInfo(File file) {
			super(file);
		}

		public FileItemInfo(String filePath) {
			super(filePath);
		}

		public FileItemInfo(String fileName, byte[] content) {
			super(fileName, content);
		}

		public FileItemInfo(String fileName, byte[] content, String mimeType) {
			super(fileName, content, mimeType);
		}
	}

	private void testWeb() {
		List<TimeRange> timeRangeList = new ArrayList<>();
		timeRangeList.add(new TimeRange(LocalTime.parse("04:30"), LocalTime.parse("12:00")));
		timeRangeList.add(new TimeRange(LocalTime.parse("12:00"), LocalTime.parse("16:00")));
		timeRangeList.add(new TimeRange(LocalTime.parse("18:00"), LocalTime.parse("02:30")));
		System.out.println("getTimeRanges:------------------------------------------");
		System.out.println(TextUtils.toString(TimeRangeUtils.getTimeRanges(TimeRangeUtils.fromTimeRanges(timeRangeList), 60, true)));
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

	private void testFormat() {
		// {"a":"${Action}","b":"{Version}"}
		String format = "\\{\"a\":[\"${Format[+\",\"]}\"],\"b\":\"${Version}\",\"c\":\"${time[#;yyyy-MM-dd]}\"}";
		java.util.Map<String, Object> paras = new java.util.TreeMap<>();
		// 1. 系统参数
		paras.put("SignatureMethod", "HMAC-SHA1");
		paras.put("SignatureNonce", "${$nonce}");
		paras.put("AccessKeyId", "${$accessKeyId}");
		paras.put("SignatureVersion", "1.0");

		paras.put("Timestamp", "${$timestamp}");
		paras.put("Format", Arrays.asList("XML", "JSON"));
		paras.put("time", LocalDateTime.now());
		// 2. 业务API参数
		paras.put("Action", "SendSms\\;");
		paras.put("Version", "2017-05-25");
		paras.put("RegionId", "cn-hangzhou");
		paras.put("PhoneNumbers", "${$receiver}");
		paras.put("SignName", "${$signName}");
		paras.put("TemplateParam", "{\"code\":\"${code}\"}");
		paras.put("TemplateCode", "SMS00001");
		paras.put("type", "1");
		paras.put("code", "SMS00008");

		System.out.println(TextUtils.simplifiedFormat("原格式: {}", format));
		System.out.println(TextUtils.simplifiedFormat("parseParamMap解析参数结果: {}", TextUtils.parseParamMap(format, paras)));
		System.out.println(TextUtils.simplifiedFormat("parseSimplifiedParamMap解析参数结果: {}", TextUtils.parseSimplifiedParamMap(format, paras)));

		System.out.println(TextUtils.simplifiedFormat("format解析参数结果: {}", TextUtils.format(format, paras)));
		System.out.println(TextUtils.simplifiedFormat("simplifiedFormat解析参数结果: {}", TextUtils.simplifiedFormat(format, paras)));

		System.out.println(TextUtils.alternateFormat("\\https://localhost/$(type[#1=aa;*=bb/%s])$([?code=]code)", paras));
		System.out.println(TextUtils.alternateFormat("\\https://localhost/$(type[#1=aa;*=bb/%s])$([?code=]code)"));
		System.out.println(TextUtils.alternateFormat(String.format("\\https://localhost/$(type[#1=aa;*=bb/%s])$([?code=]code)", "me")));
		System.out.println(String.format("\\https://localhost/$(type[#1=aa;*=bb/%s])$([?code=]code)", "me"));
	}

}
