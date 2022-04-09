package com.honzel.core.util.web;

import com.honzel.core.constant.ArrayConstants;
import com.honzel.core.constant.NumberConstants;
import com.honzel.core.util.text.TextUtils;

import java.util.*;
import java.util.function.Function;


/**
 * 签名排序工具类
 * @author honzel
 * @date 2021/4/9
 */
public class SignSortUtils {


	/**
	 * 获取按key自然排序的签名内容
	 * @param params 签名参数
	 * @param separatorAfterKey key后的分隔符
	 * @param separatorAfterValue value后的分隔符
	 * @param ignoreKeys 忽略的key
	 * @return 返回组装的签名内容
	 */
	public static String getSortedKeyContent(Map<String, Object> params, String separatorAfterKey, String separatorAfterValue, Function<Object, String> valueMapping, String... ignoreKeys) {
		if (params == null || params.isEmpty()) {
			return TextUtils.EMPTY;
		}
		// 自然排序
		Map<String, Object> sortedMap = params instanceof SortedMap  ? params : new TreeMap<>(params);

		StringBuilder sb = new StringBuilder();
		// 排除sign和空值参数
		for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
			// 获取值
			if (entry.getKey() == null || entry.getValue() == null || matchKey(ignoreKeys, entry.getKey(), false)) {
				continue;
			}
			Object value = valueMapping != null ? valueMapping.apply(entry.getValue()) : entry.getValue();
			if (value == null) {
				continue;
			}
			if (separatorAfterKey != null) {
				sb.append(entry.getKey());
				if (!separatorAfterKey.isEmpty()) {
					sb.append(separatorAfterKey);
				}
			}
			sb.append(value);
			if (TextUtils.isNotEmpty(separatorAfterValue)) {
				sb.append(separatorAfterValue);
			}
		}
		if (sb.length() > NumberConstants.INTEGER_ZERO && TextUtils.isNotEmpty(separatorAfterValue)) {
			sb.setLength(sb.length() - separatorAfterValue.length());
		}
		return sb.toString();
	}
	/**
	 * 获取按key自然排序的签名内容
	 * @param params 签名参数
	 * @param separatorAfterKey key后的分隔符
	 * @param separatorAfterValue value后的分隔符
	 * @param ignoreKeys 忽略的key
	 * @return 返回组装的签名内容
	 */
	public static String getSortedKeyContent(Map<String, Object> params, String separatorAfterKey, String separatorAfterValue,  String... ignoreKeys) {
		return getSortedKeyContent(params, separatorAfterKey, separatorAfterValue, null, ignoreKeys);
	}
	/**
	 * 获取按key自然排序的签名内容
	 * @param params 签名参数
	 * @param separatorAfterKey key后的分隔符
	 * @param separatorAfterValue value后的分隔符
	 * @return 返回组装的签名内容
	 */
	public static String getSortedKeyContent(Map<String, Object> params, String separatorAfterKey, String separatorAfterValue) {
		return getSortedKeyContent(params, separatorAfterKey, separatorAfterValue, null, ArrayConstants.EMPTY_STRING_ARRAY);
	}
	/**
	 * 获取按value自然排序的签名内容
	 * @param params 签名参数
	 * @param separator 值拼僵分隔符
	 * @param secret 密钥
	 * @param keys 拼接value值的key, 如果为null或空数组时不限制
	 * @return 返回组装的签名内容
	 */
	public static String getSortedValueContent(Map<String, Object> params, String separator, String secret, Function<Object, String> valueMapping, String... keys) {
		if (params == null || params.isEmpty()) {
			return TextUtils.EMPTY;
		}
		// 自然排序
		List<String> valueList = new ArrayList<>();
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) {
				continue;
			}
			if (matchKey(keys, entry.getKey(), true)) {
				String value = valueMapping != null ? valueMapping.apply(entry.getValue()) : entry.getValue().toString();
				if (value != null) {
					valueList.add(value);
				}
			}
		}
		if (TextUtils.isNotEmpty(secret)) {
			valueList.add(secret);
		}
		Comparator<? super String> comparator;
		if (!(params instanceof SortedMap) || (comparator = ((SortedMap<String, Object>) params).comparator()) == null) {
			comparator = Comparator.naturalOrder();
		}
		valueList.sort(comparator);
		return String.join(separator != null ? separator : TextUtils.EMPTY, valueList);
	}

	private static boolean matchKey(String[] keys, String key, boolean sortKey) {
		if (keys == null || keys.length == 0) {
			return sortKey;
		}
		for (String k : keys) {
			if (key.equals(k)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取按value自然排序的签名内容
	 * @param params 签名参数
	 * @param separator 值拼僵分隔符
	 * @param secret 密钥
	 * @param keys 拼接value值的key, 如果为null或空数组时不限制
	 * @return 返回组装的签名内容
	 */
	public static String getSortedValueContent(Map<String, Object> params, String separator, String secret, String... keys) {
		return getSortedValueContent(params, separator, secret, null, keys);
	}
	/**
	 * 获取按value自然排序的签名内容
	 * @param params 签名参数
	 * @param separator 值拼僵分隔符
	 * @param secret 密钥
	 * @return 返回组装的签名内容
	 */
	public static String getSortedValueContent(Map<String, Object> params, String separator, String secret) {
		return getSortedValueContent(params, separator, secret, ArrayConstants.EMPTY_STRING_ARRAY);
	}
	
}

