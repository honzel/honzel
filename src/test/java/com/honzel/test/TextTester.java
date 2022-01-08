package com.honzel.test;

import com.honzel.core.util.text.TextUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

public class TextTester {
	

	public static void main(String[] args) {
		TextTester tester = new TextTester();
		tester.testFormat();
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
