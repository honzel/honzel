package com.honzel.test;

import com.honzel.core.util.text.FormatTypeEnum;
import com.honzel.core.util.text.TextUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

public class TextTester {
	

	public static void main(String[] args) {
		TextTester tester = new TextTester();
		tester.testFormat();
	}

	private void testGetValue() {

	}
	private void testFormat() {
		String format = "===========订单[{0}]信息不存在=======";
		System.out.println(TextUtils.simplifiedFormat(format, "100"));
		// {"a":"${Action}","b":"{Version}"}
//		String format = "\\{\"a\":[\"${Format[+\",\"]}\"],\"b\":\"${Version}\",\"c\":\"${time[#;yyyy-MM-dd]}\"}";
//		String format2="{\"touser\":\"${$receiver[^kkk]}\",\"data\":{\"first\":{\"value\":\"您的${;channelName}店铺审核${;auditStatus[#1=已通过;2=不通过]}\"},\"keyword1\":{\"value\":\"${auditStatus[#1=已通过;2=不通过]}\"},\"keyword2\":{\"value\":\"绑定${channelName}店铺审核\"},\"keyword3\":{\"value\":\"${time[#;yyyy-MM-dd]}\"},\"remark\":{\"value\":\"${xml;auditStatus[#1=$(channelName)可以打印订单&&啦;2=不通过原因:$(reason)]}\"}},\"template_id\":\"ppppp\"}";
//		java.util.Map<String, Object> paras = new java.util.TreeMap<>();
//		// 1. 系统参数
//		paras.put("channelName", "美团");
//		paras.put("auditStatus", "1");
//		paras.put("reason", "没有可能通过");
//		paras.put("SignatureMethod", "HMAC-SHA1");
//		paras.put("SignatureNonce", "${$nonce}");
//		paras.put("AccessKeyId", "${$accessKeyId}");
//		paras.put("SignatureVersion", "1.0");
//
//		paras.put("Timestamp", "${$timestamp}");
//		paras.put("Format", Arrays.asList("XML", "JSON"));
//		paras.put("time", LocalDateTime.now());
//		// 2. 业务API参数
//		paras.put("Action", "SendSms\\;");
//		paras.put("Version", "2017-05-25");
//		paras.put("RegionId", "cn-hangzhou");
//		paras.put("PhoneNumbers", "${$receiver}");
//		paras.put("SignName", "${$signName}");
//		paras.put("TemplateParam", "{\"code\":\"${code}\"}");
//		paras.put("TemplateCode", "SMS00001");
//		paras.put("type", "1");
//		paras.put("code", "SMS00008");
//
//		System.out.println(TextUtils.simplifiedFormat("原格式: {}", format));
//
//		System.out.println(TextUtils.simplifiedFormat("parseParamMap解析参数结果: {}", TextUtils.parseParamMap(format, paras)));
//		System.out.println(TextUtils.simplifiedFormat("parseSimplifiedParamMap解析参数结果: {}", TextUtils.parseSimplifiedParamMap(format, paras)));
//
//		System.out.println(TextUtils.simplifiedFormat("format解析参数结果: {}", TextUtils.format(format, paras)));
//		System.out.println(TextUtils.simplifiedFormat("simplifiedFormat解析参数结果: {}", TextUtils.simplifiedFormat(format, paras)));
//
//		System.out.println(TextUtils.alternateFormat("\\https://localhost/$(type[#1=aa;*=bb/%s])$([?code=]code)", paras));
//		System.out.println(TextUtils.alternateFormat("\\https://localhost/$(type[#1=aa;*=bb/%s])$([?code=]code)"));
//		System.out.println(TextUtils.alternateFormat(String.format("\\https://localhost/$(type[#1=aa;*=bb/%s])$([?code=]code)", "me")));
//		System.out.println(String.format("\\https://localhost/$(type[#1=aa;*=bb/%s])$([?code=]code)", "me"));
//
//		System.out.println(TextUtils.simplifiedFormat("原格式2: {}", format2));
//		System.out.println(TextUtils.simplifiedFormat("parseParamMap2解析参数结果: {}", TextUtils.parseParamMap(format2, paras)));
//		System.out.println(TextUtils.simplifiedFormat("format2解析参数结果: {}", TextUtils.format(TextUtils.DATA_TYPE_QUERY_STRING, format2, paras)));
//		System.out.println(TextUtils.simplifiedFormat("format2解析参数结果: {}", TextUtils.format(TextUtils.DATA_TYPE_XML, format2, paras)));
//		System.out.println(TextUtils.simplifiedFormat("format2解析参数结果: {}", TextUtils.format(TextUtils.DATA_TYPE_TEXT, format2, paras)));
//		System.out.println(TextUtils.simplifiedFormat("format2解析参数结果: {}", TextUtils.format(TextUtils.DATA_TYPE_JSON, format2, paras)));
//		System.out.println(TextUtils.simplifiedFormat("format2解析参数结果: {}", TextUtils.format(TextUtils.DATA_TYPE_NONE, format2, paras)));

		String src = ",aaa,bbb,ccc,ddd,";
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, 0)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, 1)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, 2)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, 3)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, 4)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, 5)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, 6)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, 7)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, -1)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, -2)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, -3)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, -4)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, -5)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, -6)));
		System.out.println(TextUtils.simplifiedFormat("---------------|{}|---------------", TextUtils.getValue(src, -7)));

	}

}
