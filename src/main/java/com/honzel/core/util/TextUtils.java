package com.honzel.core.util;

import com.honzel.core.util.resolver.Resolver;
import com.honzel.core.util.web.WebUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 字符文本工具类
 * @author honzel
 * @date 2021/2/27
 */
public class TextUtils {

	public static final String SEPARATOR = ",";
	public static final String EMPTY = "";

	private static final String HOLDER_FLAG = "$";

	private static final String BRACE_START = "{";
	private static final String BRACE_END = "}";

	private static final String PARENTHESES_START = "(";
	private static final String PARENTHESES_END = ")";

	private static final String BRACKET_START = "[";
	private static final String BRACKET_END = "]";

	private static final String EQUAL = "=";
	private static final String SEMICOLON = ";";

	private static final char EXPR_FLAG = '#';

	private TextUtils() {
	}

	public static final int DATA_TYPE_JSON = 1;

	public static final int DATA_TYPE_QUERY_STRING = 2;

	public static final int DATA_TYPE_XML = 3;

	public static final int DATA_TYPE_TEXT = 4;


	public static int getDataType(String content) {
		if (isEmpty(content)) {
			return DATA_TYPE_TEXT;
		}
		content = content.trim();
		if (content.startsWith(BRACE_START) && content.endsWith(BRACE_END)) {
			return DATA_TYPE_JSON;
		}
		if (content.startsWith(BRACKET_START) && content.endsWith(BRACKET_END)) {
			return DATA_TYPE_JSON;
		}
		if (content.startsWith("<") && content.endsWith(">")) {
			return DATA_TYPE_XML;
		}
		if (content.startsWith("&") || content.startsWith("http")) {
			return DATA_TYPE_QUERY_STRING;
		}
		return DATA_TYPE_TEXT;
	}

	/**
	 * 获取格式中的参数占位符map
	 * @param result 结果
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseParamMap(Map<String, Object> result, String pattern, Object params) {
		return parseParamMap0(result, false, pattern, params);
	}
	/**
	 * 获取格式中的参数占位符map
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseParamMap(String pattern, Object params) {
		return parseParamMap0(new HashMap<>(), false, pattern, params);
	}
	/**
	 * 获取格式中的参数占位符map
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseAlternateParamMap(String pattern, Object params) {
		return parseParamMap0(new HashMap<>(), true, pattern, params);
	}

	/**
	 * 获取格式中的参数占位符map
	 * @param result 结果
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseAlternateParamMap(Map<String, Object> result, String pattern, Object params) {
		return parseParamMap0(result, true, pattern, params);
	}
	/**
	 * 获取格式中的参数占位符map
	 *
	 * @param result 参数结果
	 * @param alternateHolderEnabled 是否使用备选占位符
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	private static Map<String, Object> parseParamMap0(Map<String, Object> result, boolean alternateHolderEnabled, String pattern, Object params) {
		// 判断是否可能有占位符
		if (isEmpty(pattern) || !pattern.contains(HOLDER_FLAG)) {
			// 如果没有占位符
			return Collections.emptyMap();
		}
		// 使用解析器
		Resolver resolver = createResolver(alternateHolderEnabled);
		char holder = alternateHolderEnabled ? PARENTHESES_START.charAt(0) : BRACE_START.charAt(0);
		// 使用$符号进行初步搜索定位解析
		resolver.reset(pattern).useTokens(HOLDER_FLAG);
		// 解析keys
		int offset = 0;
		while (resolver.hasNext()) {
			// 判断是否为${xxx}格式的占位符
			if (resolver.isInTokens()) {
				// 判断是否为${xxx}格式的占位符
				if (pattern.charAt(resolver.getStart()) != holder) {
					// 跳过$符号
					resolver.reset(resolver.getStart(false));
					continue;
				}
				// 解析当前小段内容
				resolver.resetToCurrent().useTokens(BRACKET_START).reset(resolver.getStart() + 1).hasNext();
				if (!resolver.isInTokens() && resolver.isEmpty() && !resolver.isLast()) {
					// 忽略空串解析下一部分
					resolver.hasNext();
				}
				//判断是否前置常量串
				if (resolver.isInTokens() && resolver.getInput().charAt(resolver.getStart()) != EXPR_FLAG) {
					// 解析下一部分
					resolver.hasNext();
				}
				// 参数偏移量, 使用数组参数进行格式化是使用到
				if (resolver.isInTokens()) {
					//空key
					Object value = getItemValue(params, offset ++);
					// 放入参数值
					result.putIfAbsent(value == params ? EMPTY: Integer.toString(offset - 1), value);

				} else {
					// 变量
					if (!pattern.startsWith(HOLDER_FLAG, resolver.getStart())) {
						// 属性值占位符
						result.putIfAbsent(resolver.next(), getPropertyValue(resolver, params, offset ++));
					}
					// 获取下一步
					resolver.hasNext();
				}
				// 起始位置
				if (resolver.isInTokens() && resolver.getInput().charAt(resolver.getStart()) == EXPR_FLAG) {
					// 表达式
					int outerTerminal = resolver.getTerminal();
					resolver.resetToCurrent().reset(resolver.getStart() + 1).useTokens(EQUAL + SEMICOLON);
					// 获取映射值
					boolean isSimpleValuePattern = true;
					while (resolver.hasNext()) {
						if (isSimpleValuePattern) {
							if (resolver.isEmpty() && resolver.endsInTokens(SEMICOLON)) {
								resolver.hasNext();
							} else {
								isSimpleValuePattern = false;
							}
							if (resolver.isLast()) {
								// 最后一个
								if (!isSimpleValuePattern) {
									// 如果不是简单格式化模板，获取参数
									parseParamMap0(result, !alternateHolderEnabled, resolver.next(), params);
								}
								break;
							}
							isSimpleValuePattern = false;
						}
						if (resolver.endsInTokens(EQUAL) && resolver.hasNext(SEMICOLON)) {
							// 获取表达式参数
							parseParamMap0(result, !alternateHolderEnabled, resolver.next(), params);
						}
					}
					resolver.useTerminal(outerTerminal);
				}
				// 该段解析结束，准备解析后一段的内容
				resolver.resetToBeyond(1).useTokens(HOLDER_FLAG);
			}
		}
		return result;
	}
	/**
	 * 格式化字符串文本
	 * @param dataType 数据类型
	 * @param pattern 待格式化内容
	 * @param configParams 配置
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String format(int dataType, String pattern, Object configParams, Object params) {
		return format0(false, dataType, pattern, configParams, params, null);
	}

	/**
	 * 格式化字符串文本
	 * @param pattern 待格式化内容
	 * @param param 参数
	 * @return 返回格式化后内容
	 */
	public static String format(String pattern, Object param) {
		return format0(false, getDataType(pattern), pattern, null, param, null);
	}
	/**
	 * 格式化字符串文本
	 * @param dataType 数据类型
	 * @param pattern 待格式化内容
	 * @param param 参数
	 * @return 返回格式化后内容
	 */
	public static String format(int dataType, String pattern, Object param) {
		return format0(false, dataType, pattern, null, param, null);
	}

	/**
	 * 格式化字符串文本
	 * @param pattern 待格式化内容
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String format(String pattern, Object... params) {
		return format0(false, getDataType(pattern), pattern, null, params, null);
	}
	/**
	 * 格式化字符串文本
	 * @param dataType 数据类型
	 * @param pattern 待格式化内容
	 * @param configParams 配置
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateFormat(int dataType, String pattern, Object configParams, Object params) {
		return format0(true, dataType, pattern, configParams, params, null);
	}
	/**
	 * 格式化字符串文本
	 * @param dataType 数据类型
	 * @param pattern 待格式化内容
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateFormat(int dataType, String pattern, Object params) {
		return format0(true, dataType, pattern, null, params, null);
	}
	/**
	 * 格式化字符串文本
	 * @param pattern 待格式化内容
	 * @param param 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateFormat(String pattern, Object param) {
		return format0(true, getDataType(pattern), pattern, null, param, null);
	}
	/**
	 * 格式化字符串文本
	 * @param pattern 待格式化内容
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateFormat(String pattern, Object... params) {
		return format0(true, getDataType(pattern), pattern, null, params, null);
	}
	/**
	 * 格式化字符串文本
	 * @param alternateHolderEnabled 是否使用备选占位符
	 * @param dataType 数据类型
	 * @param pattern 待格式化内容
	 * @param configParams 配置
	 * @param params 参数
	 * @param thisValue 当前值
	 * @return 返回格式化后内容
	 */
	private static String format0(boolean alternateHolderEnabled, int dataType, String pattern, Object configParams, Object params, Object thisValue) {
		if (isEmpty(pattern) || (!pattern.contains(HOLDER_FLAG) && pattern.indexOf('\\') < 0)) {
			// 内容为空时
			return pattern;
		}
		// 解析后的内容
		StringBuilder content = new StringBuilder();
		// 使用解析器
		Resolver resolver = createResolver(alternateHolderEnabled);
		char holder = alternateHolderEnabled ? PARENTHESES_START.charAt(0) : BRACE_START.charAt(0);
		// 使用$符号进行初步搜索定位解析
		resolver.reset(pattern).useTokens(HOLDER_FLAG);
		// 参数偏移量, 使用数组参数进行格式化是使用到
		int offset = 0;
		// 解析格式
		while (resolver.hasNext()) {
			// 判断是否为${xxx}/$(xxx)格式的占位符
			if (resolver.isInTokens()) {
				// 判断是否为${xxx}格式的占位符
				if (pattern.charAt(resolver.getStart()) == holder) {
					// ${xxx}格式的占位符内容时解析并附加参数值
					offset = appendValue(content, resolver, dataType, thisValue, configParams, params, offset, alternateHolderEnabled);
				} else {
					// 添加$符号
					content.append(HOLDER_FLAG);
					resolver.reset(resolver.getStart(false));
				}
			} else {
				// 非占位符时文本原样添加到结果内容
				resolver.appendTo(content, false);
			}
		}
		// 返回解析后结果
		return content.toString();
	}

	private static Resolver createResolver(boolean alternateHolderEnabled) {
		if (alternateHolderEnabled) {
			return ResolverUtils.createResolver(HOLDER_FLAG + BRACKET_START + EQUAL + SEMICOLON, PARENTHESES_END + BRACKET_END, true);
		} else {
			return ResolverUtils.createResolver(HOLDER_FLAG + BRACKET_START + EQUAL + SEMICOLON, BRACE_END + BRACKET_END, true);
		}
	}



	/**
	 * 解析参数内容
	 * @param content 内容
	 * @param resolver 解析器
	 * @param dataType 数据类型
	 * @param thisValue 当前值环境
	 * @param configParams 配置参数
	 * @param params 普通参数
	 * @param offset 解析的参数的偏移量
	 * @param alternateHolderEnabled 是否启用候选格式
	 * @return 返回下一个参数的偏移量
	 */
	private static int appendValue(StringBuilder content, Resolver resolver, int dataType, Object thisValue, Object configParams, Object params, int offset, boolean alternateHolderEnabled) {
		// 解析当前小段内容
		int startIndex = resolver.getStart() + 1;
		// 初始位置
		int originPosition = content.length();
		resolver.resetToCurrent().useTokens(BRACKET_START).reset(startIndex).hasNext();
		if (!resolver.isInTokens() && resolver.isEmpty() && !resolver.isLast()) {
			// 忽略空串解析下一部分
			resolver.hasNext();
		}
		// 是否值为null进行附加内容
		boolean appendForEmpty = false;
		// 输入数据
		String format = (String) resolver.getInput();
		//判断是否前置常量串
		if (resolver.isInTokens() && format.charAt(resolver.getStart()) != EXPR_FLAG) {
			// 前置字符串是否为null时附加的内容, 预先附加上内容
			if (format.charAt(resolver.getStart()) == '^') {
				appendForEmpty = true;
				resolver.appendTo(content, 1);
			} else {
				resolver.appendTo(content);
			}
			// 解析下一部分
			resolver.hasNext();
		}
		// 参数值
		Object value;
		//后置常量串
		if (!resolver.isInTokens() && format.startsWith(HOLDER_FLAG, resolver.getStart())) {
			//如果是配置属性
			value = getConfigValue(resolver, thisValue, configParams);
		} else {
			//如果是参数占位符
			value = getParamValue(resolver, params, offset);
			offset ++;
		}

		int startLen = content.length();
		if (value instanceof Iterable) {
			// 获取解析开始位置
			int resolverStart = resolver.isInTokens() ? resolver.getStart(false) - 1 : resolver.getStart();
			// 前缀
			String prefix = startLen == originPosition ? EMPTY : content.substring(originPosition);
			// 循环处理项
			for (Object itemValue : ((Iterable) value)) {
				// 先格式化
				itemValue = formatValue(resolver, itemValue, configParams, params, alternateHolderEnabled);
				// 附加值
				originPosition = appendFormatValue(content, resolver, dataType, itemValue, appendForEmpty, originPosition);
				// 添加分隔符
				content.append(SEPARATOR).append(prefix);
				// 重置开始解析
				resolver.reset(resolverStart).hasNext();
			}
		} else if (value instanceof Object[]) {
			// 获取解析开始位置
			int resolverStart = resolver.isInTokens() ? resolver.getStart(false) - 1 : resolver.getStart();
			// 前缀
			String prefix = startLen == originPosition ? EMPTY : content.substring(originPosition);
			// 循环处理项
			for (Object itemValue : (Object[]) value) {
				// 先格式化
				itemValue = formatValue(resolver, itemValue, configParams, params, alternateHolderEnabled);
				// 附加值
				originPosition = appendFormatValue(content, resolver, dataType, itemValue, appendForEmpty, originPosition);
				// 添加分隔符
				content.append(SEPARATOR).append(prefix);
				// 重置开始解析
				resolver.reset(resolverStart).hasNext();
			}
		}
		if (startLen == content.length()) {
			// 先格式化
			value = formatValue(resolver, value, configParams, params, alternateHolderEnabled);
			// 没有解析到内容时
			appendFormatValue(content, resolver, dataType, value, appendForEmpty, originPosition);
		} else {
			// 已解析到集合内容时, 去掉最后加入的部分内容
			content.setLength(originPosition - SEPARATOR.length());
		}
		// 该段解析结束，准备解析后一段的内容
		resolver.resetToBeyond(1).useTokens(HOLDER_FLAG);
		return offset;
	}

	private static int appendFormatValue(StringBuilder content, Resolver resolver, int dataType, Object value, boolean appendForEmpty, int originPosition) {
		// 格式化值
		String stringValue = toString(value);
		// 判断是否去掉前缀
		if (appendForEmpty != isEmpty(stringValue)) {
			// 如果不匹配，则去掉前缀
			content.setLength(originPosition);
		}
		//添加参数值
		appendDataTypeValue(content, stringValue, dataType);
		// 后置内容处理
		String format = (String) resolver.getInput();
		boolean next = true;
		while (next && resolver.isInTokens()) {
			if (format.startsWith("^", resolver.getStart())) {
				// 如果为null时才附加，则值为null时进行附加
				if (isEmpty(stringValue)) {
					resolver.appendTo(content, 1);
				}
			} else {
				// 如果为非null时才附加，则值为非null时进行附加
				if (!isEmpty(stringValue)) {
					resolver.appendTo(content);
				}
			}
			next = resolver.hasNext();
		}
		// 需要包含紧接着的逗号
		return content.length() + SEPARATOR.length();
	}

	/**
	 * 获取配置值
	 * @param resolver 解析对象
	 * @param thisValue 当前项值
	 * @param configParams 配置参数对象
	 * @return 返回配置值
	 */
	private static Object getConfigValue(Resolver resolver, Object thisValue, Object configParams) {
		// 是否有格式指定
		String format = (String) resolver.getInput();
		int startIndex = resolver.getStart() + 1;
		int endIndex = resolver.getEnd();
		// 获取下一步内容
		resolver.hasNext();
		// 获取当前值
		if (thisValue != null && format.startsWith("this", startIndex)) {
			if (endIndex == startIndex + 4) {
				return thisValue;
			}
			if (format.charAt(startIndex + 4) == '.') {
				return BeanHelper.getProperty(thisValue, format.substring(startIndex + 5, endIndex));
			}
		}
		// 返回属性值
		return BeanHelper.getProperty(configParams, format.substring(startIndex, endIndex));
	}

	private static Object formatValue(Resolver resolver, Object value, Object configParams, Object params, boolean alternateHolderEnabled) {
		if (!resolver.isInTokens() || resolver.getInput().charAt(resolver.getStart()) != EXPR_FLAG) {
			// 没有指定格式化格式
			return value;
		}
		// 起始位置
		int outerTerminal = resolver.getTerminal();
		resolver.resetToCurrent().reset(resolver.getStart() + 1).useTokens(EQUAL + SEMICOLON);
		// 获取映射值
		value =  getMappingValue(resolver, value, configParams, params, alternateHolderEnabled);
		// 返回
		resolver.resetToBeyond(1).useTerminal(outerTerminal).useTokens(BRACKET_START).hasNext();
		return value;
	}

	private static Object getMappingValue(Resolver resolver, Object value, Object configParams, Object params, boolean alternateHolderEnabled) {
		//
		String stringValue = null;
		boolean isSimpleValuePattern = true;
		int dataType = 0;
		while (resolver.hasNext()) {
			if (isSimpleValuePattern) {
				if (resolver.endsInTokens(SEMICOLON)) {
					if (!resolver.isEmpty()) {
						if (resolver.nextEquals("json")) {
							dataType = DATA_TYPE_JSON;
						} else if (resolver.nextEquals("xml")) {
							dataType = DATA_TYPE_XML;
						} else if (resolver.nextEquals("url")) {
							dataType = DATA_TYPE_QUERY_STRING;
						}
						isSimpleValuePattern = false;
					}
					resolver.hasNext();
				} else {
					isSimpleValuePattern = false;
				}
				if (resolver.isLast()) {
					String pattern = resolver.next();
					if (isSimpleValuePattern) {
						// 基本类型或日期格式转化
						return formatSimpleValue(value, pattern);
					}
					if (isEmpty(pattern) || "*".equals(pattern)) {
						return value;
					}
					int useDataType = dataType > 0 ? dataType : getDataType(pattern);
					return format0(!alternateHolderEnabled, useDataType, pattern, configParams, params, value);
				}
				stringValue = (value != null) ? toString(value) : "null";
				isSimpleValuePattern = false;
			}
			if (resolver.nextEquals(stringValue) || resolver.nextEquals("*")) {
				if (resolver.isLast() || !resolver.endsInTokens(EQUAL)) {
					// 返回原值
					return value;
				}
				resolver.hasNext(SEMICOLON);
				String pattern = resolver.next();
				int useDataType = dataType > 0 ? dataType : getDataType(pattern);
				stringValue = format0(!alternateHolderEnabled, useDataType, pattern, configParams, params, value);
				// 映射值
				return "null".equals(stringValue) ? null : stringValue;
			}
		}
		return null;
	}

	/**
	 * 日期格式转化
	 * @param value 数据值
	 * @param pattern 格式化模板
	 * @return 返回格式结果
	 */
	private static Object formatSimpleValue(Object value, String pattern) {
		try {
			if (!isEmpty(pattern)) {
				if (value instanceof TemporalAccessor) {
					return DateTimeFormatter.ofPattern(pattern).format((TemporalAccessor) value);
				} else if (value instanceof Date) {
					return new SimpleDateFormat(pattern).format((Date) value);
				} else if (value instanceof Calendar) {
					return new SimpleDateFormat(pattern).format(((Calendar) value).getTime());
				} else if (value instanceof Number) {
					return new DecimalFormat(pattern).format(value);
				}
			}
		} catch (Exception e) {
			System.err.println("数据格式化失败: " + e.getMessage());
			e.printStackTrace();
		}
		return value;
	}

	/**
	 * 获取参数值
	 * @param resolver 解析对象
	 * @param params 参数对象
	 * @param offset 参数偏移量
	 * @return 返回参数值
	 */
	private static Object getParamValue(Resolver resolver, Object params, int offset) {
		if (resolver.isInTokens()) {
			return getItemValue(params, offset);
		} else {
			Object propValue = getPropertyValue(resolver, params, offset);
			resolver.hasNext();
			return propValue;
		}
	}

	private static Object getPropertyValue(Resolver resolver, Object params, int offset) {
		boolean noProperty = true;
		String name = resolver.next(false, true);
		Object propValue = null;
		if (name.contains(".")) {
			// 获取属性
			propValue = BeanHelper.getProperty(params, name);
			if (propValue != null || BeanHelper.getPropertyType(params, name) != null) {
				noProperty = false;
			}
		}
		if (noProperty && params != null) {
			int index = resolver.nextInt();
			propValue = getItemValue(params, index < 0 ? offset : index);
			if (propValue == params && !isEmpty(name)) {
				// 如果不是列表或数组
				propValue = BeanHelper.getProperty(params, name);
				if (propValue == null && (offset == 0 && index < 0 || index == 0)
						&& Modifier.isFinal(params.getClass().getModifiers()) && BeanHelper.getPropertyType(params, name) == null) {
					propValue = params;
				}
			}
		}
		return propValue;
	}


	private static Object getItemValue(Object params, int index) {
		if (params.getClass().isArray()) {
			return index < Array.getLength(params) ? Array.get(params, index) : null;
		}
		if (params instanceof List) {
			return index < ((List) params).size() ? ((List) params).get(index) : null;
		}
		return params;
	}

	private static void appendDataTypeValue(StringBuilder message, String value, int dataType) {
		if (isEmpty(value)) {
			return;
		}
		switch (dataType) {
			case DATA_TYPE_JSON:
				appendJsonValue(message, value);
				break;
			case DATA_TYPE_QUERY_STRING:
				message.append(WebUtils.encode(value)) ;
				break;
			case DATA_TYPE_XML:
				message.append(getXmlValue(value));
				break;
			default:
				message.append(value);
				break;
		}
	}

	private static String getXmlValue(String value) {
		// &"<>'符号转义
		return value.replace("&", "&amp;").replace("\"", "&quot;")
				.replace("<", "&lt;").replace(">", "&gt;")
				.replace("'", "&apos;");
	}

	private static void appendJsonValue(StringBuilder content, String value) {
		int lastIndex = -1;
		for (int i = 0, len = value.length(); i < len; i++) {
			char ch = value.charAt(i);
			if (ch >= ' ' && ch != '\"' && ch != '\\') {
				continue;
			}
			if (lastIndex + 1 < i) {
				content.append(value, lastIndex + 1, i);
			}
			lastIndex = i;
			switch(ch) {
				case '\\':
					content.append("\\\\");
					break;
				case '\"':
					content.append("\\\"");
					break;
				case '\b':
					content.append("\\b");
					break;
				case '\t':
					content.append("\\t");
					break;
				case '\n':
					content.append("\\n");
					break;
				case '\r':
					content.append("\\r");
					break;
				case '\f':
					content.append("\\f");
					break;
				default:
					content.append("\\u").append(Integer.toHexString(ch));
					break;
			}
		}
		if (lastIndex >= 0) {
			content.append(value, lastIndex + 1, value.length());
		} else {
			content.append(value);
		}
	}


	/**
	 * 字符串值列表中是否包含该值
	 * @param valueList 字符串值集合(用','隔开各值)
	 * @param value 搜索的字符串值
	 * @return true-字符串值列表中包含该值,否则为false
	 */
	public static boolean containsValue(String valueList, Object value) {
		return containsValue(valueList, value, SEPARATOR);
	}


	/**
	 * 查询指定值在对应列表的位置， 第一个值的位置为0，第二个值为1...
	 * @param valueList 字符串值集合(用','隔开各值)
	 * @param value 搜索的字符串值
	 * @return int
	 */
	public static int indexOfValue(String valueList, Object value) {
		return indexOfValue(valueList, value, true, SEPARATOR);
	}


	/**
	 * 字符串值列表中是否包含该值
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param value 搜索的字符串值
	 * @param separator 项分隔符
	 * @return true-字符串值列表中包含该值,否则为false
	 */
	public static boolean containsValue(String valueList, Object value, String separator) {
		return indexOfValue(valueList, value, separator) >= 0;
	}


	/**
	 * 查询指定值在对应列表的位置， 第一个值的位置为0，第二个值为1...
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param value 搜索的字符串值
	 * @param separator 项分隔符
	 * @return int
	 */
	public static int indexOfValue(String valueList, Object value, String separator) {
		return indexOfValue(valueList, value, true, separator);
	}

	/**
	 * 查询指定值在对应列表的位置， 第一个值的位置为0，第二个值为1...
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param value 搜索的字符串值
	 * @param valueIndexOfList true-返回值在列表中的值索引，false-返回值在列表中的字符索引
	 * @param separator 项分隔符
	 * @return int
	 */
	public static int indexOfValue(String valueList, Object value, boolean valueIndexOfList, String separator) {
		String item = value != null ? value.toString() : null;
		if (item == null) {
			return -1;
		}
		return indexOfValue(valueList, item, 0, item.length(), valueIndexOfList, separator);
	}

	private static int indexOfValue(String valueList, String value, int offset, int len, boolean valueIndexOfList, String separator) {
		if (valueList == null || len < 0 || valueList.length() < len) {
			return -1;
		}
		if (isEmpty(separator)) {
			return valueList.indexOf(value.substring(offset, offset + len));
		}
		if (valueList.regionMatches(0, value, offset, len) && (valueList.length() == len || valueList.startsWith(separator, len))) {
			return 0;
		}
		int count = 1;
		int start = valueList.indexOf(separator);
		while (start >= 0) {
			start += separator.length();
			if (valueList.regionMatches(start, value, offset, len)) {
				int end = start + len;
				if (valueList.length() == end || valueList.startsWith(separator, end)) {
					return valueIndexOfList ? count : start;
				}
			}
			start = valueList.indexOf(separator, start);
			count ++;
		}
		return -1;
	}


	/**
	 * 移除值列表中对应的第一个值, 如果不存在, 则不变
	 * @param valueList 字符串值集合(用','隔开各值)
	 * @param value 移除的字符串值
	 * @return 返回移除后的值列表, 如果不变, 则返回原列表串对象
	 */
	public static String removeValue(String valueList, Object value) {
		return removeValue(valueList, value, SEPARATOR);
	}



	/**
	 * 移除值列表中对应的第一个值, 如果不存在, 则不变
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param value 移除的字符串值
	 * @param separator 项分隔符
	 * @return 返回移除后的值列表, 如果不变, 则返回原列表串对象
	 */
	public static String removeValue(String valueList, Object value, String separator) {
		String item = value != null ? value.toString() : null;
		if (item == null) {
			return valueList;
		}
		int start = indexOfValue(valueList, item, 0, item.length(), false, separator);
		if (start > 0) {
			//
			return valueList.substring(0, start - separator.length()) + valueList.substring(start + item.length());
		}
		if (start == 0) {
			return valueList.length() > item.length() ? valueList.substring(item.length() + separator.length()) : EMPTY;
		}
		return valueList;
	}
	/**
	 * 移除值列表中对应位置的值, 如果不存在, 则不变
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param valueIndex 位置
	 * @param separator 项分隔符
	 * @return 返回移除后的值列表, 如果不变, 则返回原列表串对象
	 */
	public static String remove(String valueList, int valueIndex, String separator) {
		if (valueList == null) {
			return null;
		}
		if (isEmpty(separator)) {
			if (valueIndex < valueList.length()) {
				return valueList.substring(0, valueIndex) + valueList.substring(valueIndex + 1);
			} else {
				return valueList;
			}
		}
		int startIndex;
		int endIndex;
		if (valueIndex >= 0) {
			startIndex = 0;
			while ((endIndex = valueList.indexOf(separator, startIndex)) >= 0) {
				if (valueIndex == 0) {
					break;
				}
				startIndex = endIndex + separator.length();
				valueIndex --;
			}
			if (startIndex > 0) {
				startIndex -= separator.length();
			}
		} else {
			endIndex = valueList.length();
			valueIndex ++;
			while ((startIndex = valueList.lastIndexOf(separator, endIndex - 1)) >= 0) {
				if (valueIndex == 0) {
					break;
				}
				endIndex = startIndex;
				valueIndex ++;
			}
		}
		if (valueIndex != 0) {
			return valueList;
		}
		// 获取前缀
		String prefix = startIndex > 0 ? valueList.substring(0, startIndex) : EMPTY;
		//
		if (endIndex >= 0) {
			// 有前缀时
			if (startIndex < 0) {
				return endIndex == valueList.length() ? EMPTY : valueList.substring(endIndex + separator.length());
			} else {
				return endIndex < valueList.length() ? prefix + valueList.substring(endIndex) : prefix;
			}
		} else {
			// 没有后缀
			return prefix;
		}
	}
	/**
	 * 移除值列表中对应位置的值, 如果不存在, 则不变
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param valueIndex 位置
	 * @return 返回移除后的值列表, 如果不变, 则返回原列表串对象
	 */
	public static String remove(String valueList, int valueIndex) {
		return remove(valueList, valueIndex, SEPARATOR);
	}

	/**
	 * 字符串值集合中添加值, 如果已存在则不变
	 * @param valueList 字符串值集合(用','隔开各值)
	 * @param value 字符串值
	 * @return 返回添加后的值列表, 如果不变, 则返回原列表串对象
	 */
	public static String addValue(String valueList, Object value) {
		return addValue(valueList, value, SEPARATOR);
	}

	/**
	 * 字符串值集合中添加值, 如果已存在则不变
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param value 字符串值
	 * @param separator 项分隔符
	 * @return 返回添加后的值列表, 如果不变, 则返回原列表串对象
	 */
	public static String addValue(String valueList, Object value, String separator) {
		String item = value != null ? value.toString() : null;
		if (isEmpty(item) || indexOfValue(valueList, item, 0, item.length(), false, separator) >= 0) {
			return valueList;
		}
		if (isEmpty(valueList)) {
			return item;
		}
		if (isEmpty(separator)) {
			return valueList + item;
		}
		if (item.contains(separator)) {
			int start = indexOfValue(item, valueList, 0, valueList.length(), false, separator);
			if (start == 0) {
				return item;
			}
			if (start > 0) {
				return valueList + separator + item.substring(0, start - separator.length()) + item.substring(start + valueList.length());
			}
			if (valueList.contains(separator)) {
				return valueList;
			}
		}
		return valueList + separator + item;
	}


	/**
	 * 字符串值列表中是否包含指定所有值
	 * @param valueList 字符串值集合(用','隔开各值)
	 * @param values 搜索的多个值(用','隔开各值)
	 * @return true-字符串值列表中包含该值,否则为false
	 */
	public static boolean containsAll(String valueList, String values) {
		return containsAll(valueList, values, SEPARATOR);
	}

	/**
	 * 字符串值列表中是否包含指定所有值
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param values 搜索的多个值(用项分隔符隔开各值)
	 * @param separator 项分隔符
	 * @return true-字符串值列表中包含该值,否则为false
	 */
	public static boolean containsAll(String valueList, String values, String separator) {
		if (valueList == null || values == null) {
			return false;
		}
		if (indexOfValue(valueList, values, 0, values.length(), false, separator) >= 0) {
			return true;
		}
		if (isEmpty(separator)) {
			return false;
		}
		if (!values.contains(separator)) {
			return false;
		}
		int start = 0;
		while (start < values.length()) {
			int end = values.indexOf(separator, start);
			if (end < 0) {
				return indexOfValue(valueList, values, start, values.length() - start, false, separator) >= 0;
			}
			if (indexOfValue(valueList, values, start, end - start, false, separator) < 0) {
				return false;
			}
			start = end + separator.length();
		}
		return true;
	}

	/**
	 * 移除值集合中对应的多个值
	 * @param valueList 字符串值集合(用','隔开各值)
	 * @param values 多个值(用','隔开各值)
	 * @return 返回移除后的值列表, 如果不变, 则返回原列表串对象
	 */
	public static String removeAll(String valueList, String values) {
		return removeAll(valueList, values, SEPARATOR);
	}

	/**
	 * 移除值集合中对应的多个值
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param values 多个值(用项分隔符隔开各值)
	 * @param separator 项分隔符
	 * @return 返回移除后的值列表, 如果不变, 则返回原列表串对象
	 */
	public static String removeAll(String valueList, String values, String separator) {
		if (valueList == null || valueList.isEmpty() || values == null) {
			return valueList;
		}
		if (isEmpty(separator)) {
			return valueList.equals(values) ? EMPTY : valueList.replace(values, EMPTY);
		}
		int start = 0;
		while (start < values.length()) {
			int end = values.indexOf(separator, start);
			if (end < 0) {
				end = values.length();
			}
			int len = end - start;
			if (len >= 0) {
				int index = indexOfValue(valueList, values, start, len, false, separator);
				if (index > 0) {
					valueList =  valueList.substring(0, index - separator.length()) + valueList.substring(index + len);
				} else if (index == 0) {
					valueList = valueList.length() > len ? valueList.substring(len + separator.length()) : EMPTY;
				}
			}
			start = end + separator.length();
		}
		return valueList;
	}

	/**
	 * 值集合中添加多个值，对于已存在的值忽略添加
	 * @param valueList 字符串值集合(用','隔开各值)
	 * @param values 多个值(用','隔开各值)
	 * @return 返回添加后的值列表, 如果不变, 则返回原列表串对象
	 */
	public static String addAll(String valueList, String values) {
		return addAll(valueList, values, SEPARATOR);
	}

	/**
	 * 值集合中添加多个值，对于已存在的值忽略添加
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param values 多个值(用项分隔符隔开各值)
	 * @param separator 项分隔符
	 * @return 返回添加后的值列表, 如果不变, 则返回原列表串对象
	 */
	public static String addAll(String valueList, String values, String separator) {
		if (isEmpty(values) || indexOfValue(valueList, values, 0, values.length(), false, separator) >= 0) {
			return valueList != null ? valueList : values;
		}
		if (isEmpty(valueList)) {
			return values;
		}
		if (isEmpty(separator)) {
			return valueList + values;
		}
		if (!values.contains(separator)) {
			return  valueList + separator + values;
		}
		int start = 0;
		StringBuilder result = new StringBuilder(valueList);
		while (start < values.length()) {
			int end = values.indexOf(separator, start);
			if (end < 0) {
				end = values.length();
			}
			if (start < end && indexOfValue(valueList, values, start, end - start, false, separator) < 0) {
				result.append(separator).append(values, start, end);
			}
			start = end + separator.length();
		}
		return result.toString();
	}

	/**
	 * 获取两个集合的交集, 如果没有交集则返回空串
	 * @param firstList 字符串值集合(用','隔开各值)
	 * @param secondList 多个值(用','隔开各值)
	 * @return 返回添加后的值集合,并按第一个字符串集合的顺序返回
	 */
	public static String retainAll(String firstList, String secondList) {
		return retainAll(firstList, secondList, SEPARATOR);
	}



	/**
	 * 获取两个集合的交集, 如果没有交集则返回空串
	 * @param firstList 字符串值集合(用项分隔符隔开各值)
	 * @param secondList 多个值(用项分隔符隔开各值)
	 * @param separator 项分隔符
	 * @return 返回添加后的值集合,并按第一个字符串集合的顺序返回
	 */
	public static String retainAll(String firstList, String secondList, String separator) {
		if (firstList == null || secondList == null) {
			return null;
		}
		if (firstList.isEmpty() || secondList.isEmpty()) {
			return firstList.isEmpty() ? firstList : secondList;
		}

		if (Objects.equals(firstList, secondList)
				|| (secondList.length() > firstList.length() && indexOfValue(secondList, firstList, 0, firstList.length(), false, separator) >= 0)) {
			return firstList;
		}
		if (firstList.length() > secondList.length() && indexOfValue(firstList, secondList, 0, secondList.length(), false, separator) >= 0) {
			return secondList;
		}
		if (isEmpty(separator) || !firstList.contains(separator) || !secondList.contains(separator)) {
			return EMPTY;
		}
		int start = 0;
		StringBuilder result = new StringBuilder();
		while (start <= firstList.length()) {
			int end = firstList.indexOf(separator, start);
			if (end < 0) {
				end = firstList.length();
			}
			if (start <= end && indexOfValue(secondList, firstList, start, end - start, false, separator) >= 0) {
				if (result.length() > 0) {
					result.append(separator);
				}
				result.append(firstList, start, end);
			}
			start = end + separator.length();
		}
		return result.toString();
	}


	/**
	 * 获取值列表元素个数
	 * @param valueList　字符串值集合(用','隔开各值)
	 * @return 值列表元素个数
	 */
	public static int getSize(String valueList) {
		return getSize(valueList, SEPARATOR);
	}

	/**
	 * 获取值列表元素个数
	 * @param valueList　字符串值集合(用项分隔符隔开各值)
	 * @param separator 项分隔符
	 * @return 值列表元素个数
	 */
	public static int getSize(String valueList, String separator) {
		if (isEmpty(valueList)) {
			return 0;
		}
		if (isEmpty(separator)) {
			return valueList.length();
		}
		int start = 0;
		int count = 1;
		while ((start = valueList.indexOf(separator, start)) >= 0) {
			count ++;
			start += separator.length();
		}
		return count;
	}

	public static boolean isEmpty(Object value) {
		return value == null || EMPTY.equals(value);
	}

	/**
	 * 获取指定位置的值, 如果列表是null或值索引超出列表元素边界时返回null
	 * @param valueList 字符串值集合(用','隔开各值)
	 * @param valueIndex 值在列表中的值索引，负值代表从后面开始算
	 * @return 返回对应索引的项字符串值
	 */
	public static String getValue(String valueList, int valueIndex) {
		return getValue(valueList, valueIndex, SEPARATOR);
	}


	/**
	 * 获取指定位置的值, 如果列表是null或值索引超出列表元素边界时返回null
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param valueIndex 值在列表中的值索引，负值代表从后面开始算
	 * @param separator 项分隔符
	 * @return 返回对应索引的项字符串值
	 */
	public static String getValue(String valueList, int valueIndex, String separator) {
		if (valueList == null) {
			return null;
		}
		if (isEmpty(separator)) {
			return valueIndex < valueList.length() ? String.valueOf(valueList.charAt(valueIndex)) : null;
		}
		int startIndex;
		int endIndex;
		if (valueIndex >= 0) {
			startIndex = 0;
			while ((endIndex = valueList.indexOf(separator, startIndex)) >= 0) {
				if (valueIndex == 0) {
					break;
				}
				startIndex = endIndex + separator.length();
				valueIndex --;
			}
		} else {
			endIndex = valueList.length();
			valueIndex ++;
			while ((startIndex = valueList.lastIndexOf(separator, endIndex - 1)) >= 0) {
				if (valueIndex == 0) {
					break;
				}
				endIndex = startIndex;
				valueIndex ++;
			}
			startIndex = startIndex < 0 ? 0 : startIndex + separator.length();
		}
		if (valueIndex != 0) {
			return null;
		}
		if (endIndex >= 0) {
			return valueList.substring(startIndex, endIndex);
		} else {
			return valueList.substring(startIndex);
		}
	}

	/**
	 * 转化成字符串列表
	 * @param valueList 字符串值集合(用','隔开各值)
	 * @return 字符串列表
	 */
	public static List<String> asList(String valueList) {
		return asList(valueList, SEPARATOR);
	}

	/**
	 * 转化成list
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param separator 项分隔符
	 * @return 字符串列表
	 */
	public static List<String> asList(String valueList, String separator) {
		if (isEmpty(valueList)) {
			return Collections.emptyList();
		}
		if (isEmpty(separator)) {
			return Stream.of(valueList.toCharArray()).map(String::valueOf).collect(Collectors.toList());
		}
		int startIndex = 0;
		int endIndex;
		List<String> result = new ArrayList<>();
		while ((endIndex = valueList.indexOf(separator, startIndex)) >= 0) {
			result.add(valueList.substring(startIndex, endIndex));
			startIndex = endIndex + separator.length();
		}
		if (startIndex < valueList.length()) {
			result.add(valueList.substring(startIndex));
		} else {
			result.add(EMPTY);
		}
		return result;
	}


	/**
	 * 存在项的前缀为指定串时返回true, 否则返回false
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param prefix 前缀
	 * @param separator 项分隔符
	 * @return 是否匹配
	 */
	public static boolean anyStartsWith(String valueList, String prefix, String separator) {
		if (valueList == null || prefix == null) {
			return false;
		}
		int start = valueList.indexOf(separator);
		if (start < 0 || isEmpty(separator)) {
			return valueList.startsWith(prefix);
		}
		if (valueList.startsWith(prefix) && prefix.length() <= start) {
			return true;
		}
		while (start >= 0) {
			start += separator.length();
			if (valueList.startsWith(prefix, start)) {
				int end = valueList.indexOf(separator, start);
				if (end < 0 || start + prefix.length() <= end) {
					return true;
				}
				start = end;
			} else {
				start = valueList.indexOf(separator, start);
			}
		}
		return false;
	}

	/**
	 * 存在项的前缀为指定串时返回true, 否则返回false
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param prefix 前缀
	 * @return 是否匹配
	 */
	public static boolean anyStartsWith(String valueList, String prefix) {
		return anyStartsWith(valueList, prefix, SEPARATOR);
	}

	/**
	 * 转化成字符串
	 * @param values 字符串值集合
	 * @param separator 项分隔符
	 * @return 字符串
	 */
	public static String toString(Iterable<?> values, String separator) {
		if (values == null) {
			return EMPTY;
		}
		if (separator == null) {
			separator = EMPTY;
		}
		StringBuilder result = new StringBuilder();
		for (Object value : values) {
			if (value != null) {
				result.append(value).append(separator);
			} else {
				result.append(separator);
			}
		}
		if (result.length() > 0 && !separator.isEmpty()) {
			result.setLength(result.length() - separator.length());
		}
		return result.toString();
	}

	/**
	 * 转化成字符串
	 * @param values 数组
	 * @param separator 项分隔符
	 * @return 字符串
	 */
	public static<T> String toString(T[] values, String separator) {
		if (values == null || values.length == 0) {
			return EMPTY;
		}
		if (separator == null) {
			separator = EMPTY;
		}
		StringBuilder result = new StringBuilder();
		for (Object value : values) {
			if (value != null) {
				result.append(value).append(separator);
			} else {
				result.append(separator);
			}
		}
		if (result.length() > 0 && !separator.isEmpty()) {
			result.setLength(result.length() - separator.length());
		}
		return result.toString();
	}

	/**
	 * 转化成字符串
	 * @param values 数组
	 * @return 字符串
	 */
	@SafeVarargs
	public static<T> String toString(T... values) {
		return toString(values, SEPARATOR);
	}
	/**
	 * 转化成字符串, null为转化为空串
	 * @param value 值
	 * @return 字符串
	 */
	public static<T> String toString(T value) {
		if (value instanceof Iterable) {
			return toString((Iterable<?>) value);
		}
		if (value instanceof Object[]) {
			return toString((Object[]) value);
		}
		return value != null ? value.toString() : null;
	}

	/**
	 * 转化成字符串
	 * @param values 集合
	 * @return 字符串
	 */
	public static String toString(Iterable<?> values) {
		return toString(values, SEPARATOR);
	}

	/**
	 * 移除后缀
	 * @param value 值
	 * @param suffix 后缀
	 * @return 返回移除后缀后的结果
	 */
	public static String removeSuffix(String value, String suffix) {
		if (!isEmpty(value) && !isEmpty(suffix) && value.endsWith(suffix)) {
			if (value.length() == suffix.length()) {
				return EMPTY;
			}
			return value.substring(0, value.length() - suffix.length());
		}
		return value;
	}

	/**
	 * 移除前缀
	 * @param value 值
	 * @param prefix 前缀
	 * @return 返回移除前缀后的结果
	 */
	public static String removePrefix(String value, String prefix) {
		if (!isEmpty(value) && !isEmpty(prefix) && value.startsWith(prefix)) {
			if (value.length() == prefix.length()) {
				return EMPTY;
			}
			return value.substring(prefix.length());
		}
		return value;
	}
}
