package com.honzel.test;

import com.honzel.core.util.text.TextUtils;
import com.honzel.core.util.web.WebUtils;

public class WebTester {


	public static void main(String[] args) throws Exception {
		WebTester tester = new WebTester();
		tester.testWeb();
	}

	private static class WebUtils1 extends WebUtils {
		static {
			new WebUtils1();
		}
	}

	private void testWeb() {
		System.out.println(TextUtils.simplifiedFormat("webUtils1: {}", WebUtils1.encode("aaaaa")));
	}

}
