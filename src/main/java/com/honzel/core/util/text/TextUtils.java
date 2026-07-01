package com.honzel.core.util.text;

import com.honzel.core.constant.ArrayConstants;
import com.honzel.core.util.bean.BeanHelper;
import com.honzel.core.util.resolver.Resolver;
import com.honzel.core.util.resolver.ResolverUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 字符串文本格式化工具类
 * <p>
 * 提供强大的模板化字符串格式化功能，支持多种占位符语法、转义字符处理、数据格式标识、
 * 条件匹配表达式、父子格式嵌套等高级特性。
 * </p>
 *
 * <h2>一、占位符语法</h2>
 * <p>支持四种占位符格式：</p>
 * <ul>
 *   <li><b>format</b>: 使用 {@code ${xxx}} 作为占位符</li>
 *   <li><b>alternateFormat</b>: 使用 {@code $(xxx)} 作为占位符</li>
 *   <li><b>simplifiedFormat</b>: 使用 {@code {xxx}} 作为占位符</li>
 *   <li><b>alternateSimplifiedFormat</b>: 使用 {@code (xxx)} 作为占位符</li>
 * </ul>
 *
 * <h3>基本用法示例：</h3>
 * <pre>{@code
 * // format 方法
 * TextUtils.format("Hello, ${name}!", map);  // map: {"name": "World"} -> "Hello, World!"
 *
 * // simplifiedFormat 方法
 * TextUtils.simplifiedFormat("Hello, {name}!", map);  // -> "Hello, World!"
 *
 * // alternateFormat 方法
 * TextUtils.alternateFormat("Hello, $(name)!", map);  // -> "Hello, World!"
 * }</pre>
 *
 * <h2>二、转义字符规则</h2>
 * <p>特殊字符需要使用反斜杠 {@code \} 进行转义：</p>
 *
 * <h3>1. 占位符外的转义</h3>
 * <ul>
 *   <li><b>format/alternateFormat</b>: {@code $} 需要转义
 *     <pre>{@code
 *     TextUtils.format("Price: \$${price}", map);  // map: {"price": 100} -> "Price: $100"
 *     }</pre>
 *   </li>
 *   <li><b>simplifiedFormat</b>: {@code {} 需要转义
 *     <pre>{@code
 *     TextUtils.simplifiedFormat("Set\\{item}", map);  // -> "Set{item}"
 *     }</pre>
 *   </li>
 *   <li><b>alternateSimplifiedFormat</b>: {@code (} 需要转义
 *     <pre>{@code
 *     TextUtils.alternateSimplifiedFormat("Call\\(func)", map);  // -> "Call(func)"
 *     }</pre>
 *   </li>
 * </ul>
 *
 * <h3>2. 占位符内（[]外）的转义</h3>
 * <ul>
 *   <li><b>format/simplifiedFormat</b>: {@code }[;] 需要转义
 *     <pre>{@code
 *     TextUtils.format("${key\\;value}", map);  // 分号需要转义
 *     }</pre>
 *   </li>
 *   <li><b>alternateFormat/alternateSimplifiedFormat</b>: {@code )[;] 需要转义
 *     <pre>{@code
 *     TextUtils.alternateFormat("$(key\\)value)", map);  // )需要转义
 *     }</pre>
 *   </li>
 * </ul>
 *
 * <h3>3. 占位符内（[]内）的转义</h3>
 * <ul>
 *   <li><b>format/simplifiedFormat</b>: {@code }]=;] 需要转义
 *     <pre>{@code
 *     TextUtils.format("${key[value\\]]}", map);  // ]需要转义
 *     }</pre>
 *   </li>
 *   <li><b>alternateFormat/alternateSimplifiedFormat</b>: {@code )]=;] 需要转义
 *     <pre>{@code
 *     TextUtils.alternateFormat("$(key[value\\)])", map);  // )需要转义
 *     }</pre>
 *   </li>
 * </ul>
 *
 * <h2>三、数据格式类型标识</h2>
 * <p>在占位符前添加格式类型标识，自动对值进行编码转换：</p>
 * <ul>
 *   <li><b>json</b>: JSON 格式编码</li>
 *   <li><b>xml</b>: XML 格式编码</li>
 *   <li><b>url</b>: URL 编码</li>
 *   <li><b>txt</b>: 普通文本（默认）</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // JSON 格式
 * TextUtils.format("${json;data}", map);  // map: {"data": "<tag>"} -> "\"<tag>\""
 *
 * // XML 格式
 * TextUtils.format("${xml;content}", map);  // 自动转义XML特殊字符
 *
 * // URL 编码
 * TextUtils.format("${url;param}", map);  // 自动URL编码
 * }</pre>
 *
 * <h2>四、条件匹配表达式</h2>
 * <p>使用 {@code #} 标志实现条件判断，语法：{@code ${var[#value1=result1;value2=result2;*]}}</p>
 *
 * <h3>基本用法：</h3>
 * <pre>{@code
 * // 简单条件匹配
 * TextUtils.format("${status[#1=成功;2=失败;*]}", map);
 * // status=1 -> "成功"
 * // status=2 -> "失败"
 * // status=3 -> "3" (*表示其他值原样输出)
 *
 * // 不带*的其他值输出空串
 * TextUtils.format("${type[#A=苹果;B=香蕉]}", map);
 * // type=A -> "苹果"
 * // type=C -> ""
 * }</pre>
 *
 * <h3>带格式类型的条件匹配：</h3>
 * <pre>{@code
 * // 子格式使用特定编码
 * TextUtils.format("${level[#json;1={\"msg\":\"高\"};2={\"msg\":\"低\"}]}", map);
 * }</pre>
 *
 * <h2>五、空值处理</h2>
 * <p>使用 {@code ^} 标志处理空值情况：</p>
 *
 * <h3>示例：</h3>
 * <pre>{@code
 * // 空值时显示替代文本
 * TextUtils.format("${name[^未填写]}", map);
 * // name=null 或 "" -> "未填写"
 * // name="张三" -> "张三"
 *
 * // 前后缀配合
 * TextUtils.format("姓名：${[先生/]name[^未知][女士]}", map);
 * // name=null -> "姓名：未知"
 * // name="张" -> "姓名：张女士"
 * }</pre>
 *
 * <h2>六、父子格式嵌套</h2>
 * <p>外层和内层使用不同的占位符语法，避免冲突：</p>
 *
 * <h3>嵌套规则：</h3>
 * <ul>
 *   <li><b>format</b> 与 <b>alternateFormat</b> 互为父子格式</li>
 *   <li><b>simplifiedFormat</b> 与 <b>alternateSimplifiedFormat</b> 互为父子格式</li>
 * </ul>
 *
 * <h3>示例：</h3>
 * <pre>{@code
 * // format 嵌套 alternateFormat
 * String template = "${user[#1=用户$(name);2=访客$(id)]}";
 * TextUtils.format(template, configMap);
 * // user=1, name="张三" -> "用户张三"
 * // user=2, id="007" -> "访客007"
 *
 * // simplifiedFormat 嵌套 alternateSimplifiedFormat
 * String template = "{type[#A=类别(alpha);B=类别(beta)]}";
 * TextUtils.simplifiedFormat(template, dataMap);
 * // type=A -> "类别alpha"
 * }</pre>
 *
 * <h2>七、值列表操作</h2>
 * <p>支持逗号分隔的值列表操作：</p>
 *
 * <h3>常用方法：</h3>
 * <pre>{@code
 * // 检查是否包含值
 * TextUtils.containsValue("apple,banana,orange", "banana");  // -> true
 *
 * // 查找值的索引
 * TextUtils.indexOfValue("apple,banana,orange", "banana");  // -> 1
 *
 * // 添加值
 * TextUtils.addValue("apple,banana", "orange");  // -> "apple,banana,orange"
 *
 * // 移除值
 * TextUtils.removeValue("apple,banana,orange", "banana");  // -> "apple,orange"
 *
 * // 获取交集
 * TextUtils.retainAll("apple,banana,orange", "banana,grape");  // -> "banana"
 *
 * // 获取大小
 * TextUtils.getSize("apple,banana,orange");  // -> 3
 * }</pre>
 *
 * @author honzel
 * @date 2021/2/27
 */
//@SuppressWarnings("unused")
public class TextUtils {
	public static final String SEPARATOR = ",";
	public static final String EMPTY = "";

	public static final String HOLDER_FLAG = "$";

	public static final String BRACE_START = "{";
	public static final String BRACE_END = "}";

	public static final String PARENTHESES_START = "(";
	public static final String PARENTHESES_END = ")";

	public static final String BRACKET_START = "[";
	public static final String BRACKET_END = "]";

	public static final String EQUAL = "=";
	public static final String SEMICOLON = ";";

	public static final char EXPR_FLAG = '#';

	public static final char JOIN_FLAG = '+';

	public static final char FOR_EMPTY_FLAG = '^';

	private static final int HOLDER_FLAG_TYPE = 1;


	private static final Map<String, TextFormatType> FORMAT_TYPE_MAP = new ConcurrentHashMap<>();

	private static final Queue<TextFormatType> AUTO_MATCH_FORMAT_TYPE_QUEUE = new ConcurrentLinkedQueue<>();

	private static volatile TextUtils utils;

	static {
		registerFormatType(FormatTypeEnum.SIMPLE);
		registerFormatType(FormatTypeEnum.JSON);
		registerFormatType(FormatTypeEnum.XML);
		registerFormatType(FormatTypeEnum.URL_ENCODING);
		registerFormatType(FormatTypeEnum.SUB_STR);
		registerFormatType(FormatTypeEnum.PAD);
		registerFormatType(FormatTypeEnum.CALC);
		registerFormatType(FormatTypeEnum.DIGEST);
	}

	protected TextUtils() {
	}



	@PostConstruct
    protected void init() {
		synchronized (TextUtils.class) {
			utils = this;
		}
	}

	private static TextUtils getInstance() {
		if (utils == null) {
			synchronized (TextUtils.class) {
				if (utils == null) {
					new  TextUtils().init();
				}
			}
		}
		return utils;
	}

	/**
	 * 注册格式类型
	 * <p>
	 * 向系统注册新的文本格式类型，如果已存在同名格式则替换旧格式。
	 * 支持自动匹配的格式会被加入自动匹配队列。
	 * </p>
	 *
	 * @param textFormatType 要注册的格式类型对象
	 * @return 始终返回 false（保留方法签名兼容性）
	 * @see TextFormatType
	 * @see FormatTypeEnum
	 */
	public static boolean registerFormatType(TextFormatType textFormatType) {
		TextFormatType oldFormatType = FORMAT_TYPE_MAP.put(textFormatType.getUniqueId(), textFormatType);
		if (textFormatType.supportsAutoMatch() && !textFormatType.equals(oldFormatType)) {
			if (Objects.nonNull(oldFormatType) && oldFormatType.supportsAutoMatch()) {
				AUTO_MATCH_FORMAT_TYPE_QUEUE.remove(oldFormatType);
			}
			AUTO_MATCH_FORMAT_TYPE_QUEUE.add(textFormatType);
			return true;
		}
		return false;
	}

	/**
	 * 根据标签获取已注册的格式类型
	 *
	 * @param tag 格式类型的唯一标识（如 "json", "xml", "url" 等）
	 * @return 对应的格式类型对象，未找到返回 null
	 * @see TextFormatType
	 */
	public static TextFormatType getFormatType(String tag) {
		return FORMAT_TYPE_MAP.get(tag);
	}

	/**
	 * 根据内容自动匹配格式类型
	 * <p>
	 * 遍历所有支持自动匹配的格式类型，找到第一个初步匹配的类型。
	 * 如果没有匹配到，返回默认的 SIMPLE 格式。
	 * </p>
	 *
	 * @param content 待检测的内容字符串
	 * @return 匹配的格式类型，未匹配返回 {@link FormatTypeEnum#SIMPLE}
	 * @example
	 * <pre>{@code
	 * TextUtils.lookupFormatType("{\"name\":\"test\"}");  // -> JSON格式
	 * TextUtils.lookupFormatType("<xml>data</xml>");      // -> XML格式
	 * TextUtils.lookupFormatType("plain text");           // -> SIMPLE格式
	 * }</pre>
	 */
	public static TextFormatType lookupFormatType(String content) {
		if (Objects.nonNull(content) && !EMPTY.equals(content = content.trim())) {
			for (TextFormatType value : AUTO_MATCH_FORMAT_TYPE_QUEUE) {
				if (value.preliminaryMatch(content)) {
					return value;
				}
			}
		}
		return FormatTypeEnum.SIMPLE;
	}
	@Deprecated
	public static TextFormatType getDataType(String content) {
		return lookupFormatType(content);
	}


	/**
	 * 解析模板中的参数占位符映射（使用 ${xxx} 语法）
	 * <p>
	 * 提取模板中所有 ${xxx} 格式的占位符及其对应的参数值，返回 Map 结构。
	 * 支持属性访问、数组索引、配置参数等多种参数形式。
	 * </p>
	 *
	 * @param result 用于存储结果的 Map，如果为 null 会创建新的 LinkedHashMap
	 * @param pattern 包含占位符的模板字符串
	 * @param params 参数对象（可以是 Map、List、数组或普通对象）
	 * @return 参数名到参数值的映射 Map
	 * @example
	 * <pre>{@code
	 * Map<String, Object> params = new HashMap<>();
	 * params.put("name", "张三");
	 * params.put("age", 25);
	 * params.put("gender", "男");
	 *
	 * Map<String, Object> paramMap = TextUtils.parseParamMap(
	 *     "姓名：${name}, 年龄：${age}", params);
	 * // paramMap: {"name": "张三", "age": 25}
	 * }</pre>
	 */
	public static Map<String, Object> parseParamMap(Map<String, Object> result, String pattern, Object params) {
		return parseParamMap0(result, false, pattern, params, false);
	}

	/**
	 * 解析模板中的参数占位符映射（使用 ${xxx} 语法）
	 * <p>
	 * 便捷方法，自动创建结果 Map。
	 * </p>
	 *
	 * @param pattern 包含占位符的模板字符串
	 * @param params 参数对象
	 * @return 参数名到参数值的映射 Map
	 * @see #parseParamMap(Map, String, Object)
	 */
	public static Map<String, Object> parseParamMap(String pattern, Object params) {
		return parseParamMap0(new LinkedHashMap<>(), false, pattern, params, false);
	}

	/**
	 * 解析模板中的参数占位符映射（使用 $(xxx) 语法）
	 * <p>
	 * 与 {@link #parseParamMap} 类似，但使用 $(xxx) 作为占位符语法。
	 * 适用于需要嵌套格式的场景。
	 * </p>
	 *
	 * @param result 用于存储结果的 Map
	 * @param pattern 包含占位符的模板字符串
	 * @param params 参数对象
	 * @return 参数名到参数值的映射 Map
	 * @example
	 * <pre>{@code
	 * Map<String, Object> params = Map.of("name", "李四","gender", "男");
	 * Map<String, Object> paramMap = TextUtils.parseAlternateParamMap(
	 *     "姓名：$(name)", params);
	 * // paramMap: {"name": "李四"}
	 * }</pre>
	 */
	public static Map<String, Object> parseAlternateParamMap(Map<String, Object> result, String pattern, Object params) {
		return parseParamMap0(result, true, pattern, params, false);
	}

	/**
	 * 解析模板中的参数占位符映射（使用 $(xxx) 语法）
	 * <p>
	 * 便捷方法，自动创建结果 Map。
	 * </p>
	 *
	 * @param pattern 包含占位符的模板字符串
	 * @param params 参数对象
	 * @return 参数名到参数值的映射 Map
	 * @see #parseAlternateParamMap(Map, String, Object)
	 */
	public static Map<String, Object> parseAlternateParamMap(String pattern, Object params) {
		return parseParamMap0(new LinkedHashMap<>(), true, pattern, params, false);
	}


	/**
	 * 解析模板中的参数占位符映射（使用 {xxx} 语法）
	 * <p>
	 * 简化版占位符语法，不使用 $ 符号。
	 * </p>
	 *
	 * @param result 用于存储结果的 Map
	 * @param pattern 包含占位符的模板字符串
	 * @param params 参数对象
	 * @return 参数名到参数值的映射 Map
	 * @example
	 * <pre>{@code
	 * Map<String, Object> params = Map.of("city", "北京","gender", "男");
	 * Map<String, Object> paramMap = TextUtils.parseSimplifiedParamMap(
	 *     "城市：{city}", params);
	 * // paramMap: {"city": "北京"}
	 * }</pre>
	 */
	public static Map<String, Object> parseSimplifiedParamMap(Map<String, Object> result, String pattern, Object params) {
		return parseParamMap0(result, false, pattern, params, true);
	}

	/**
	 * 解析模板中的参数占位符映射（使用 {xxx} 语法）
	 * <p>
	 * 便捷方法，自动创建结果 Map。
	 * </p>
	 *
	 * @param pattern 包含占位符的模板字符串
	 * @param params 参数对象
	 * @return 参数名到参数值的映射 Map
	 * @see #parseSimplifiedParamMap(Map, String, Object)
	 */
	public static Map<String, Object> parseSimplifiedParamMap(String pattern, Object params) {
		return parseParamMap0(new LinkedHashMap<>(), false, pattern, params, true);
	}

	/**
	 * 解析模板中的参数占位符映射（使用 (xxx) 语法）
	 * <p>
	 * 简化版备选占位符语法，不使用 $ 符号，使用圆括号。
	 * </p>
	 *
	 * @param pattern 包含占位符的模板字符串
	 * @param params 参数对象
	 * @return 参数名到参数值的映射 Map
	 * @example
	 * <pre>{@code
	 * Map<String, Object> params = Map.of("color", "红色","city", "北京");
	 * Map<String, Object> paramMap = TextUtils.parseAlternateSimplifiedParamMap(
	 *     "颜色：(color)", params);
	 * // paramMap: {"color": "红色"}
	 * }</pre>
	 */
	public static Map<String, Object> parseAlternateSimplifiedParamMap(String pattern, Object params) {
		return parseParamMap0(new LinkedHashMap<>(), true, pattern, params, true);
	}

	/**
	 * 解析模板中的参数占位符映射（使用 (xxx) 语法）
	 * <p>
	 * 便捷方法，允许传入自定义结果 Map。
	 * </p>
	 *
	 * @param result 用于存储结果的 Map
	 * @param pattern 包含占位符的模板字符串
	 * @param params 参数对象
	 * @return 参数名到参数值的映射 Map
	 * @see #parseAlternateSimplifiedParamMap(String, Object)
	 */
	public static Map<String, Object> parseAlternateSimplifiedParamMap(Map<String, Object> result, String pattern, Object params) {
		return parseParamMap0(result, true, pattern, params, true);
	}


	/**
	 * 获取格式中的参数占位符map
	 *
	 * @param result 参数结果
	 * @param alternateHolderEnabled 是否使用备选占位符(即第一层占位符是否使用'()'代替'{}')
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @param simplified 是否简化占位符（即占位符不带$)
	 * @return 每个参数占位符对应的值
	 */
	private static Map<String, Object> parseParamMap0(Map<String, Object> result, boolean alternateHolderEnabled, String pattern, Object params, boolean simplified) {
		// 判断是否可能有占位符
		if (isNormalText(pattern, alternateHolderEnabled, simplified)) {
			// 如果没有占位符
			return result;
		}
		// 使用解析器
		Resolver resolver = createResolver(alternateHolderEnabled, simplified);
		char holder = getHolderStartChar(alternateHolderEnabled, simplified);
		// 使用$符号进行初步搜索定位解析
		resolver.reset(pattern).useTypes(HOLDER_FLAG_TYPE);
		// 解析keys
		int offset = 0;
		while (resolver.hasNext()) {
			// 判断是否为${xxx}格式的占位符
			if (resolver.isInTokens()) {
				// 判断是否为${xxx}格式的占位符
				if (!simplified && pattern.charAt(resolver.getStart()) != holder) {
					// 跳过$符号
					resolver.reset(resolver.getStart(false));
					continue;
				}
				// 解析当前小段内容
				resolver.resetToCurrent(simplified ? 0 : 1).useTokens(BRACKET_START);
				if (resolver.hasNext(BRACKET_START + SEMICOLON)) {
					if (resolver.endsInTokens(SEMICOLON)) {
						resolver.hasNext();
					}
					if (!resolver.isInTokens() && resolver.isEmpty() && !resolver.isLast()) {
						// 忽略空串解析下一部分
						resolver.hasNext();
					}
				}
				//判断是否前置常量串
				if (resolver.isInTokens() && pattern.charAt(resolver.getStart()) != EXPR_FLAG) {
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
				// 表达式
				int outerTerminal = resolver.getTerminal();
				while (resolver.isInTokens() && resolver.getInput().charAt(resolver.getStart()) == EXPR_FLAG) {
					resolver.resetToCurrent(1).useTokens(EQUAL + SEMICOLON);
					// 获取映射值
					boolean first = true;
					while (resolver.hasNext()) {
						if (first) {
							if (resolver.endsInTokens(SEMICOLON)) {
								resolver.hasNext();
							}
							first = false;
						}
						if (resolver.isLast() || resolver.endsInTokens(EQUAL) && resolver.hasNext(SEMICOLON)) {
							// 获取表达式参数
							parseParamMap0(result, !alternateHolderEnabled, resolver.next(), params, simplified);
						}
					}
					resolver.useTerminal(outerTerminal).useTokens(BRACKET_START).hasNext();
				}
				// 该段解析结束，准备解析后一段的内容
				resolver.resetToBeyond(1).useTypes(HOLDER_FLAG_TYPE);
			}
		}
		return result;
	}

	/**
	 * 格式化字符串（使用 ${xxx} 占位符）
	 * <p>
	 * 将模板中的 ${xxx} 占位符替换为实际参数值，支持指定数据格式类型和配置参数。
	 * </p>
	 *
	 * @param textFormatType 数据格式类型（如 JSON、XML、URL 编码等），可为 null
	 * @param pattern 模板字符串，包含 ${xxx} 格式的占位符
	 * @param configParams 配置参数对象，用于访问配置属性
	 * @param params 占位符参数（可以是 Map、List、数组或普通对象）
	 * @return 格式化后的字符串
	 * @example
	 * <pre>{@code
	 * // 基本用法
	 * Map<String, Object> params = Map.of("name", "王五", "age", 30);
	 * String result = TextUtils.format(null, "姓名：${name}, 年龄：${age}", null, params);
	 * // result: "姓名：王五, 年龄：30"
	 *
	 * // 使用 JSON 格式
	 * Map<String, Object> data = Map.of("content", "<html>");
	 * String jsonResult = TextUtils.format(FormatTypeEnum.JSON, "${json;content}", null, data);
	 * // jsonResult: "\"<html>\""
	 * }</pre>
	 */
	public static String format(TextFormatType textFormatType, String pattern, Object configParams, Object params) {
		return format0(false, textFormatType, pattern, configParams, params, null, null, false);
	}

	/**
	 * 格式化字符串（使用 ${xxx} 占位符）
	 * <p>
	 * 便捷方法，自动检测数据格式类型。
	 * </p>
	 *
	 * @param pattern 模板字符串
	 * @param param 单个参数对象
	 * @return 格式化后的字符串
	 * @example
	 * <pre>{@code
	 * User user = new User("赵六", 28);
	 * String result = TextUtils.format("用户：${name}, 年龄：${age}", user);
	 * // result: "用户：赵六, 年龄：28"
	 * }</pre>
	 */
	public static String format(String pattern, Object param) {
		return format0(false, lookupFormatType(pattern), pattern, null, param, null, null, false);
	}

	/**
	 * 格式化字符串（使用 ${xxx} 占位符）
	 * <p>
	 * 指定数据格式类型的便捷方法。
	 * </p>
	 *
	 * @param textFormatType 数据格式类型
	 * @param pattern 模板字符串
	 * @param param 单个参数对象
	 * @return 格式化后的字符串
	 * @see #format(TextFormatType, String, Object, Object)
	 */
	public static String format(TextFormatType textFormatType, String pattern, Object param) {
		return format0(false, textFormatType, pattern, null, param, null, null, false);
	}

	/**
	 * 格式化字符串（使用 ${xxx} 占位符，数组参数）
	 * <p>
	 * 支持使用索引访问数组或列表元素：${0}, ${1}, ${2}...
	 * </p>
	 *
	 * @param pattern 模板字符串
	 * @param params 可变参数数组
	 * @return 格式化后的字符串
	 * @example
	 * <pre>{@code
	 * String result = TextUtils.format("姓名：${0}, 年龄：${1}, 城市：${2}",
	 *     "孙七", 35, "上海");
	 * // result: "姓名：孙七, 年龄：35, 城市：上海"
	 *
	 * // 访问对象属性
	 * String result2 = TextUtils.format("全名：${0.name}", userObj);
	 * // result2: "全名：孙七"
	 * }</pre>
	 */
	public static String format(String pattern, Object... params) {
		return format0(false, lookupFormatType(pattern), pattern, null, params, null, null, false);
	}

	/**
	 * 格式化字符串（使用 {xxx} 占位符）
	 * <p>
	 * 简化版格式化方法，使用 {xxx} 作为占位符语法。
	 * </p>
	 *
	 * @param textFormatType 数据格式类型
	 * @param pattern 模板字符串，包含 {xxx} 格式的占位符
	 * @param configParams 配置参数对象
	 * @param params 占位符参数
	 * @return 格式化后的字符串
	 * @example
	 * <pre>{@code
	 * Map<String, Object> data = Map.of("product", "手机", "price", 2999);
	 * String result = TextUtils.simplifiedFormat(null,
	 *     "商品：{product}, 价格：{price}元", null, data);
	 * // result: "商品：手机, 价格：2999元"
	 * }</pre>
	 */
	public static String simplifiedFormat(TextFormatType textFormatType, String pattern, Object configParams, Object params) {
		return format0(false, textFormatType, pattern, configParams, params, null, null, true);
	}

	/**
	 * 格式化字符串（使用 {xxx} 占位符）
	 * <p>
	 * 便捷方法，自动检测数据格式类型。
	 * </p>
	 *
	 * @param pattern 模板字符串
	 * @param param 单个参数对象
	 * @return 格式化后的字符串
	 * @see #simplifiedFormat(TextFormatType, String, Object, Object)
	 */
	public static String simplifiedFormat(String pattern, Object param) {
		return format0(false, lookupFormatType(pattern), pattern, null, param, null, null, true);
	}

	/**
	 * 格式化字符串（使用 {xxx} 占位符）
	 * <p>
	 * 指定数据格式类型的便捷方法。
	 * </p>
	 *
	 * @param textFormatType 数据格式类型
	 * @param pattern 模板字符串
	 * @param param 单个参数对象
	 * @return 格式化后的字符串
	 */
	public static String simplifiedFormat(TextFormatType textFormatType, String pattern, Object param) {
		return format0(false, textFormatType, pattern, null, param, null, null, true);
	}

	/**
	 * 格式化字符串（使用 {xxx} 占位符，数组参数）
	 * <p>
	 * 支持使用索引访问数组或列表元素。
	 * </p>
	 *
	 * @param pattern 模板字符串
	 * @param params 可变参数数组
	 * @return 格式化后的字符串
	 * @example
	 * <pre>{@code
	 * String result = TextUtils.simplifiedFormat(
	 *     "坐标：({0}, {1}, {2})", 100, 200, 300);
	 * // result: "坐标：(100, 200, 300)"
	 * }</pre>
	 */
	public static String simplifiedFormat(String pattern, Object... params) {
		return format0(false, lookupFormatType(pattern), pattern, null, params, null, null, true);
	}

	/**
	 * 格式化字符串（使用 (xxx) 占位符）
	 * <p>
	 * 简化版备选格式化方法，使用 (xxx) 作为占位符语法。
	 * 常用于嵌套格式的内层。
	 * </p>
	 *
	 * @param textFormatType 数据格式类型
	 * @param pattern 模板字符串，包含 (xxx) 格式的占位符
	 * @param configParams 配置参数对象
	 * @param params 占位符参数
	 * @return 格式化后的字符串
	 * @example
	 * <pre>{@code
	 * Map<String, Object> data = Map.of("x", 10, "y", 20);
	 * String result = TextUtils.alternateSimplifiedFormat(null,
	 *     "点：(x, y)", null, data);
	 * // result: "点：(10, 20)"
	 * }</pre>
	 */
	public static String alternateSimplifiedFormat(TextFormatType textFormatType, String pattern, Object configParams, Object params) {
		return format0(true, textFormatType, pattern, configParams, params, null, null, true);
	}

	/**
	 * 格式化字符串（使用 (xxx) 占位符）
	 * <p>
	 * 便捷方法，自动检测数据格式类型。
	 * </p>
	 *
	 * @param pattern 模板字符串
	 * @param param 单个参数对象
	 * @return 格式化后的字符串
	 * @see #alternateSimplifiedFormat(TextFormatType, String, Object, Object)
	 */
	public static String alternateSimplifiedFormat(String pattern, Object param) {
		return format0(true, lookupFormatType(pattern), pattern, null, param, null, null, true);
	}

	/**
	 * 格式化字符串（使用 (xxx) 占位符）
	 * <p>
	 * 指定数据格式类型的便捷方法。
	 * </p>
	 *
	 * @param textFormatType 数据格式类型
	 * @param pattern 模板字符串
	 * @param param 单个参数对象
	 * @return 格式化后的字符串
	 */
	public static String alternateSimplifiedFormat(TextFormatType textFormatType, String pattern, Object param) {
		return format0(true, textFormatType, pattern, null, param, null, null, true);
	}

	/**
	 * 格式化字符串（使用 (xxx) 占位符，数组参数）
	 * <p>
	 * 支持使用索引访问数组或列表元素。
	 * </p>
	 *
	 * @param pattern 模板字符串
	 * @param params 可变参数数组
	 * @return 格式化后的字符串
	 * @example
	 * <pre>{@code
	 * String result = TextUtils.alternateSimplifiedFormat(
	 *     "RGB：(r, g, b)", 255, 128, 64);
	 * // result: "RGB：(255, 128, 64)"
	 * }</pre>
	 */
	public static String alternateSimplifiedFormat(String pattern, Object... params) {
		return format0(true, lookupFormatType(pattern), pattern, null, params, null, null, true);
	}

	/**
	 * 格式化字符串（使用 $(xxx) 占位符）
	 * <p>
	 * 备选格式化方法，使用 $(xxx) 作为占位符语法。
	 * 常用于嵌套格式的外层，与 format 方法互为父子格式。
	 * </p>
	 *
	 * @param textFormatType 数据格式类型
	 * @param pattern 模板字符串，包含 $(xxx) 格式的占位符
	 * @param configParams 配置参数对象
	 * @param params 占位符参数
	 * @return 格式化后的字符串
	 * @example
	 * <pre>{@code
	 * Map<String, Object> data = Map.of("host", "localhost", "port", 8080);
	 * String result = TextUtils.alternateFormat(null,
	 *     "地址：$(host):$(port)", null, data);
	 * // result: "地址：localhost:8080"
	 * }</pre>
	 */
	public static String alternateFormat(TextFormatType textFormatType, String pattern, Object configParams, Object params) {
		return format0(true, textFormatType, pattern, configParams, params, null, null, false);
	}

	/**
	 * 格式化字符串（使用 $(xxx) 占位符）
	 * <p>
	 * 指定数据格式类型的便捷方法。
	 * </p>
	 *
	 * @param textFormatType 数据格式类型
	 * @param pattern 模板字符串
	 * @param params 参数对象
	 * @return 格式化后的字符串
	 * @see #alternateFormat(TextFormatType, String, Object, Object)
	 */
	public static String alternateFormat(TextFormatType textFormatType, String pattern, Object params) {
		return format0(true, textFormatType, pattern, null, params, null, null, false);
	}

	/**
	 * 格式化字符串（使用 $(xxx) 占位符）
	 * <p>
	 * 便捷方法，自动检测数据格式类型。
	 * </p>
	 *
	 * @param pattern 模板字符串
	 * @param param 单个参数对象
	 * @return 格式化后的字符串
	 * @see #alternateFormat(TextFormatType, String, Object, Object)
	 */
	public static String alternateFormat(String pattern, Object param) {
		return format0(true, lookupFormatType(pattern), pattern, null, param, null, null, false);
	}

	/**
	 * 格式化字符串（使用 $(xxx) 占位符，数组参数）
	 * <p>
	 * 支持使用索引访问数组或列表元素。
	 * </p>
	 *
	 * @param pattern 模板字符串
	 * @param params 可变参数数组
	 * @return 格式化后的字符串
	 * @example
	 * <pre>{@code
	 * String result = TextUtils.alternateFormat(
	 *     "路径：$(0)/$(1)/$(2)", "home", "user", "docs");
	 * // result: "路径：home/user/docs"
	 * }</pre>
	 */
	public static String alternateFormat(String pattern, Object... params) {
		return format0(true, lookupFormatType(pattern), pattern, null, params, null, null, false);
	}

	/**
	 * 格式化字符串文本
	 *
	 * @param alternateHolderEnabled 是否使用备选占位符(即第一层占位符是否使用'()'代替'{}')
	 * @param textFormatType         数据类型
	 * @param pattern                待格式化内容
	 * @param configParams           配置
	 * @param params                 参数
	 * @param thisValue              当前值
	 * @param thisIndex              当前值索引
	 * @param simplified             是否简化占位符（即占位符不带$)
	 * @return 返回格式化后内容
	 */
	private static String format0(boolean alternateHolderEnabled, TextFormatType textFormatType, String pattern, Object configParams, Object params, Object thisValue, Integer thisIndex, boolean simplified) {
		if (isNormalText(pattern, alternateHolderEnabled, simplified)) {
			// 普通文本
			return pattern;
		}
		// 解析后的内容
		StringBuilder content = new StringBuilder();
		// 使用解析器
		Resolver resolver = createResolver(alternateHolderEnabled, simplified);
		char holder = getHolderStartChar(alternateHolderEnabled, simplified);
		// 使用$符号进行初步搜索定位解析
		resolver.reset(pattern).useTypes(HOLDER_FLAG_TYPE);
		// 参数偏移量, 使用数组参数进行格式化是使用到
		int offset = 0;
		// 解析格式
		while (resolver.hasNext()) {
			// 判断是否为${xxx}/$(xxx)格式的占位符
			if (resolver.isInTokens()) {
				// 判断是否为${xxx}格式的占位符
				if (simplified || pattern.charAt(resolver.getStart()) == holder) {
					// ${xxx}格式的占位符内容时解析并附加参数值
					offset = appendValue(content, resolver, textFormatType, thisValue, thisIndex, configParams, params, offset, alternateHolderEnabled, simplified);
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


	private static char getHolderStartChar(boolean alternateHolderEnabled, boolean simplified) {
		if (simplified) {
			return  '\0';
		}
		return alternateHolderEnabled ? PARENTHESES_START.charAt(0) : BRACE_START.charAt(0);
	}

	private static boolean isNormalText(String pattern, boolean alternateHolderEnabled, boolean simplified) {
		if (isEmpty(pattern)) {
			// 内容为空时
			return true;
		}
		if (pattern.indexOf('\\') != -1) {
			// 包含转义符
			return false;
		}
		if (simplified) {
			return alternateHolderEnabled ? !pattern.contains(PARENTHESES_START) : !pattern.contains(BRACE_START);
		} else {
			return !pattern.contains(HOLDER_FLAG);
		}
	}

	private static Resolver createResolver(boolean alternateHolderEnabled, boolean simplified) {
		if (alternateHolderEnabled) {
			if (simplified) {
				return ResolverUtils.createResolver(PARENTHESES_START + BRACE_START + BRACKET_START + EQUAL + SEMICOLON + SEPARATOR, PARENTHESES_END + BRACE_END + BRACKET_END, true);
			} else {
				return ResolverUtils.createResolver(HOLDER_FLAG + BRACE_START + BRACKET_START + EQUAL + SEMICOLON + SEPARATOR, PARENTHESES_END + BRACE_END + BRACKET_END, true);
			}
		} else {
			if (simplified) {
				return ResolverUtils.createResolver(BRACE_START + PARENTHESES_START + BRACKET_START + EQUAL + SEMICOLON + SEPARATOR, BRACE_END + PARENTHESES_END + BRACKET_END, true);
			} else {
				return ResolverUtils.createResolver(HOLDER_FLAG + PARENTHESES_START + BRACKET_START + EQUAL + SEMICOLON + SEPARATOR, BRACE_END + PARENTHESES_END + BRACKET_END, true);
			}
		}
	}


	/**
	 * 解析参数内容
	 * @param content 内容
	 * @param resolver 解析器
	 * @param textFormatType 数据类型
	 * @param thisValue 当前值环境
	 * @param configParams 配置参数
	 * @param params 普通参数
	 * @param offset 解析的参数的偏移量
	 * @param alternateHolderEnabled 是否启用候选格式
	 * @param simplified 简化占位符
	 * @return 返回下一个参数的偏移量
	 */
	private static int appendValue(StringBuilder content, Resolver resolver, TextFormatType textFormatType, Object thisValue, Integer thisIndex, Object configParams, Object params, int offset, boolean alternateHolderEnabled, boolean simplified) {
		// 解析当前小段内容
		// 初始位置
		int originPosition = content.length();
		resolver.resetToCurrent(simplified ? 0 : 1).useTokens(BRACKET_START);
		// 格式化参数
		String[] parameters = null;
		// 参数标记
		String pFlag = alternateHolderEnabled ? BRACE_START : PARENTHESES_START;
		// 输入数据
		String format = (String) resolver.getInput();
		if (resolver.hasNext(BRACKET_START + SEMICOLON)) {
			if (resolver.endsInTokens(SEMICOLON)) {
				// 带格式化类型
				int terminal = resolver.getTerminal();
				resolver.resetToCurrent().hasNext(pFlag);
				// 获取标签
				TextFormatType localDataType = getFormatType(resolver.isInTokens() ? EMPTY : resolver.next(false));
				if (Objects.nonNull(localDataType)) {
					// 有格式化类型
					textFormatType = localDataType;
					// 格式化参数
					parameters = parseParameters(resolver, pFlag, BRACKET_START);
				}
				// 解析下一部分
				resolver.resetToBeyond(1).useTerminal(terminal).hasNext();
			}
			if (!resolver.isInTokens() && resolver.isEmpty() && !resolver.isLast()) {
				// 忽略空串解析下一部分
				resolver.hasNext();
			}
		}
		// 是否值为null进行附加内容
		boolean appendForEmpty = false;
		//判断是否前置常量串
		int start = resolver.getStart();
		if (resolver.isInTokens() && format.charAt(start) != EXPR_FLAG && format.charAt(start) != JOIN_FLAG) {
			// 前置字符串是否为null时附加的内容, 预先附加上内容
			if (format.charAt(start) == FOR_EMPTY_FLAG) {
				appendForEmpty = true;
				resolver.appendTo(content, 1);
			} else {
				resolver.appendTo(content);
			}
			// 解析下一部分
			resolver.hasNext();
			start = resolver.getStart();
		}
		// 参数值
		Object value;
		//后置常量串
		if (!resolver.isInTokens() && format.startsWith(HOLDER_FLAG, start)) {
			//如果是配置属性
			value = getConfigValue(resolver, thisValue, thisIndex, configParams);
		} else {
			//如果是参数占位符
			value = getParamValue(resolver, params, offset);
			offset ++;
		}
		boolean parsed = false;
		if (parameters == null || parameters.length > 0) {
			int startLen = content.length();
			if (value instanceof Iterable) {
				// 获取解析开始位置
				int resolverStart = resolver.isInTokens() ? resolver.getStart(false) - 1 : resolver.getStart();
				// 前缀
				String prefix = startLen == originPosition ? EMPTY : content.substring(originPosition);
				// 循环处理项
				Iterator<?> iterator = ((Iterable<?>) value).iterator();
				boolean hasNext = (parsed = iterator.hasNext());
				int index = 0;
				while (hasNext) {
					// 先格式化
					Object itemValue = formatValue(resolver, iterator.next(), index++, configParams, params, alternateHolderEnabled, simplified);
					hasNext = iterator.hasNext();
					// 附加值
					originPosition = appendFormatValue(content, resolver, textFormatType, null, itemValue, appendForEmpty, originPosition, !hasNext).length();
					if (hasNext) {
						if (!prefix.isEmpty()) {
							// 添加前缀
							content.append(prefix);
						}
						// 重置开始解析
						resolver.reset(resolverStart).hasNext();
					}
				}
				value = EMPTY;
			} else if (value instanceof Object[]) {
				// 获取解析开始位置
				int resolverStart = resolver.isInTokens() ? resolver.getStart(false) - 1 : resolver.getStart();
				// 前缀
				String prefix = startLen == originPosition ? EMPTY : content.substring(originPosition);
				// 循环处理项
				Object[] array = (Object[]) value;
				for (int i = 0, len = array.length; i < len; ++i) {
					// 先格式化
					Object itemValue = formatValue(resolver, array[i], i, configParams, params, alternateHolderEnabled, simplified);
					// 是否有新一个
					boolean hasNext = (i + 1 != len);
					// 附加值
					originPosition = appendFormatValue(content, resolver, textFormatType, parameters, itemValue, appendForEmpty, originPosition, !hasNext).length();
					if (hasNext) {
						if (!prefix.isEmpty()) {
							// 添加前缀
							content.append(prefix);
						}
						// 重置开始解析
						resolver.reset(resolverStart).hasNext();
					}
				}
				parsed = array.length > 0;
				value = EMPTY;
			}
		}
		if (!parsed) {
			// 先格式化
			value = formatValue(resolver, value, null, configParams, params, alternateHolderEnabled, simplified);
			// 没有解析到内容时
			appendFormatValue(content, resolver, textFormatType, parameters, value, appendForEmpty, originPosition, true);
		}
		// 该段解析结束，准备解析后一段的内容
		resolver.resetToBeyond(1).useTypes(HOLDER_FLAG_TYPE);
		return offset;
	}



	private static StringBuilder appendFormatValue(StringBuilder content, Resolver resolver, TextFormatType textFormatType, String[] parameters, Object value, boolean appendForEmpty, int originPosition, boolean isLastValue) {
		// 格式化值
		String stringValue = textFormatType.formatValue(value, Objects.nonNull(parameters) ? parameters : ArrayConstants.EMPTY_STRING_ARRAY);
		// 判断是否去掉前缀
		boolean emptyValue = isEmpty(stringValue);
		if (appendForEmpty != emptyValue) {
			// 如果不匹配，则去掉前缀
			content.setLength(originPosition);
		}
		if (!emptyValue) {
			//添加参数值
			textFormatType.appendValue(content, stringValue);
		}
		String format = (String) resolver.getInput();
		// 是否分隔符
		boolean nonSeparator = true;
		// 后置内容处理
		boolean next = true;
		while (next && resolver.isInTokens()) {
			int start = resolver.getStart();
			int offset = 0;
			if (format.charAt(start) ==  JOIN_FLAG) {
				if (isLastValue) {
					// 最后一个元素时不添加该值
					break;
				}
				nonSeparator = false;
				offset = 1;
				start += offset;
			}
			if (format.charAt(start) ==  FOR_EMPTY_FLAG) {
				// 如果为null时才附加，则值为null时进行附加
				if (emptyValue) {
					resolver.appendTo(content, offset + 1);
				}
			} else {
				// 如果为非null时才附加，则值为非null时进行附加
				if (!emptyValue) {
					resolver.appendTo(content, offset);
				}
			}
			next = resolver.hasNext();
		}
		if (!isLastValue && nonSeparator) {
			// 没有指定分隔符时默认使用英文逗号
			content.append(SEPARATOR);
		}
		// 需要包含紧接着的逗号
		return content;
	}

	/**
	 * 获取配置值
	 * @param resolver 解析对象
	 * @param thisValue 当前项值
	 * @param configParams 配置参数对象
	 * @return 返回配置值
	 */
	private static Object getConfigValue(Resolver resolver, Object thisValue, Integer thisIndex, Object configParams) {
		// 是否有格式指定
		String format = (String) resolver.getInput();
		int startIndex = resolver.getStart() + 1;
		int endIndex = resolver.getEnd();
		// 获取下一步内容
		resolver.hasNext();
		// 获取当前值
		if (format.startsWith("this", startIndex)) {
			// $this.xxx
			if (endIndex == startIndex + 4) {
				return thisValue;
			}
			if (format.charAt(startIndex + 4) == '.') {
				return BeanHelper.getProperty(thisValue, format.substring(startIndex + 5, endIndex));
			}
		} else if (format.startsWith("idx", startIndex)) {
			//返回索引: $idx
			if (endIndex == startIndex + 3) {
				return thisIndex;
			}
			if (format.charAt(startIndex + 3) == '.') {
				return BeanHelper.getProperty(thisIndex, format.substring(startIndex + 4, endIndex));
			}
		}
		// 返回属性值
		return BeanHelper.getProperty(configParams, format.substring(startIndex, endIndex));
	}

	private static Object formatValue(Resolver resolver, Object value, Integer valueIndex, Object configParams, Object params, boolean alternateHolderEnabled, boolean simplified) {
		int outerTerminal = resolver.getTerminal();
		Object valueHolder = value;
		while (resolver.isInTokens() && resolver.getInput().charAt(resolver.getStart()) == EXPR_FLAG) {
			// 起始位置
			resolver.resetToCurrent(1);
			// 获取映射值
			value = getMappingValue(resolver, value, valueHolder, valueIndex, configParams, params, alternateHolderEnabled, simplified);
			// 获取下一个表达式
			resolver.resetToBeyond(1).useTerminal(outerTerminal).useTokens(BRACKET_START).hasNext();
		}
		return value;
	}


	private static Object getMappingValue(Resolver resolver, Object filterValue, Object value, Integer valueIndex, Object configParams, Object params, boolean alternateHolderEnabled, boolean simplified) {
        //
        String stringValue = null;
		// 格式化类型
        TextFormatType textFormatType = null;
		// 格式化参数
		String[] parameters = null;
		// 参数标记
		String pFlag = alternateHolderEnabled ? BRACE_START : PARENTHESES_START;
		resolver.useTokens(EQUAL + SEMICOLON);
        boolean firstFomrat = true;
        while (resolver.hasNext()) {
            if (firstFomrat && resolver.endsInTokens(SEMICOLON)) {
				// 带格式化类型
				int terminal = resolver.getTerminal();
				resolver.resetToCurrent().hasNext(pFlag);
				// 格式化类型
				if (Objects.nonNull(textFormatType = getFormatType(resolver.isInTokens() ? EMPTY : resolver.next(false)))) {
					// 格式化参数
					parameters = parseParameters(resolver, pFlag, EQUAL + SEMICOLON);
					// 解析下一部分
					resolver.resetToBeyond(1).useTerminal(terminal);
					//
					resolver.hasNext();
				} else {
					// 解析下一部分
					resolver.resetToBeyond(1).useTerminal(terminal).hasNext();
				}
            }
			int start = resolver.getStart();
			int end = resolver.getEnd();
			boolean match = false;
			boolean matchResult = false;
			if (end == start + 1) {
				// 单个字符
				char ch = resolver.getInput().charAt(start);
				if (ch == FOR_EMPTY_FLAG) {
					// 匹配星号
					match = isEmpty(filterValue);
					matchResult = true;
				}  else if (ch == '*') {
					match = true;
					matchResult = true;
				}
			}
			boolean nestPattern = true;
			if (resolver.isLast()) {
				if (matchResult) {
					// 只有一个星号时
					if (Objects.isNull(parameters)) {
						return filterValue;
					}
					nestPattern = false;
				} else {
					// 如果是模板字符串
					if (isEmpty(filterValue)) {
						return filterValue;
					}
					if (firstFomrat && Objects.nonNull(textFormatType)
							&& EMPTY.equals(textFormatType.getUniqueId()) && end > start && (stringValue = textFormatType.formatValue(filterValue, resolver.next())) != null) {
						// 基本类型或日期格式转化
						return stringValue;
					}
					match = true;
				}
			} else {
				if (!matchResult) {
					if (filterValue == null) {
						// 匹配空值
						match = !resolver.containsEscape() && resolver.nextEquals("null");
					} else {
						// 匹配值
						if (stringValue == null && (stringValue = toString(filterValue)) == null) {
							stringValue = "null";
						}
						match = resolver.nextEquals(stringValue);
					}
				}
				if (match) {
					if (resolver.endsInTokens(EQUAL)) {
						resolver.hasNext(SEMICOLON);
					} else {
						// 返回原值
						if (Objects.isNull(parameters)) {
							return filterValue;
						}
						nestPattern = false;
					}
				} else {
					firstFomrat = false;
				}
			}
			if (match) {
				if (nestPattern) {
					// 默认类型
					TextFormatType defaultFormatType = Objects.nonNull(textFormatType) && Objects.isNull(parameters) ? textFormatType : getFormatType(EMPTY);
					// 格式化
					String pattern = resolver.next();
					stringValue = format0(!alternateHolderEnabled, defaultFormatType, pattern, configParams, params, value, valueIndex, simplified);
				}
				if ("null".equals(stringValue)) {
					// null 做为空值
					stringValue = null;
				}
				//
				if (Objects.nonNull(parameters)) {
					// 如果有带参数, 作为结果值的截取
					stringValue = textFormatType.formatValue(stringValue == null && !nestPattern ? filterValue : stringValue, parameters);
					//
					if (isNotEmpty(stringValue) && parameters.length == 0) {
						// 非空并且没有参数时，转化结果
						StringBuilder textBuilder = new StringBuilder(stringValue.length());
						textFormatType.appendValue(textBuilder, stringValue);
						stringValue = textBuilder.toString();
					}
				}
                // 映射值
                return stringValue;
            }
			if (resolver.endsInTokens(EQUAL)) {
				resolver.hasNext(SEMICOLON);
			}
        }
        return null;
    }

	private static String[] parseParameters(Resolver resolver, String pFlag, String oriDelim) {
		if (!resolver.isInTokens() && resolver.endsInTokens(pFlag)) {
			// 获取参数
			resolver.hasNext(pFlag);
		}
		// 参数
		if (resolver.isInTokens()) {
			int terminal = resolver.getTerminal();
			if (resolver.resetToCurrent().useTokens(SEPARATOR).hasNext() && resolver.isEmpty() && resolver.isLast()) {
				// 无参数
				return ArrayConstants.EMPTY_STRING_ARRAY;
			}
			List<String> parameters = new ArrayList<>();
			do {
				parameters.add(resolver.next(true, true));
			} while (resolver.hasNext());
			// 获取参数后还原上一次的状态
			resolver.useTerminal(terminal).useTokens(oriDelim);
			return parameters.toArray(ArrayConstants.EMPTY_STRING_ARRAY);
		}
		return null;
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
				if (propValue == null && (offset == 0 && index < 0 || index == 0) && Modifier.isFinal(params.getClass().getModifiers()) && BeanHelper.getPropertyType(params, name) == null) {
					propValue = params;
				}
			}
		}
		return propValue;
	}


	private static Object getItemValue(Object params, int index) {
		if (Objects.nonNull(params) && params.getClass().isArray()) {
			return index < Array.getLength(params) ? Array.get(params, index) : null;
		}
		if (params instanceof List) {
			return index < ((List<?>) params).size() ? ((List<?>) params).get(index) : null;
		}
		if (params instanceof Map) {
			String indexStr = Integer.toString(index);
			if (((Map<?, ?>) params).containsKey(indexStr)) {
				return ((Map<?, ?>) params).get(indexStr);
			}
			if (((Map<?, ?>) params).containsKey(EMPTY)) {
				return ((Map<?, ?>) params).get(EMPTY);
			}
		}
		return params;
	}


	/**
	 * 检查值列表中是否包含指定值（使用逗号分隔）
	 *
	 * @param valueList 字符串值列表，用逗号分隔各值
	 * @param value 要搜索的值
	 * @return 如果包含该值返回 true，否则返回 false
	 * @example
	 * <pre>{@code
	 * TextUtils.containsValue("apple,banana,orange", "banana");  // -> true
	 * TextUtils.containsValue("apple,banana,orange", "grape");   // -> false
	 * }</pre>
	 */
	public static boolean containsValue(String valueList, Object value) {
		return containsValue(valueList, value, SEPARATOR);
	}


	/**
	 * 查询指定值在值列表中的位置（使用逗号分隔）
	 * <p>
	 * 第一个值的位置为 0，第二个值为 1，依此类推。
	 * </p>
	 *
	 * @param valueList 字符串值列表，用逗号分隔各值
	 * @param value 要搜索的值
	 * @return 值的位置索引，未找到返回 -1
	 * @example
	 * <pre>{@code
	 * TextUtils.indexOfValue("apple,banana,orange", "banana");  // -> 1
	 * TextUtils.indexOfValue("apple,banana,orange", "grape");   // -> -1
	 * }</pre>
	 */
	public static int indexOfValue(String valueList, Object value) {
		return indexOfValue(valueList, value, true, SEPARATOR);
	}


	/**
	 * 检查值列表中是否包含指定值（使用自定义分隔符）
	 *
	 * @param valueList 字符串值列表，用指定分隔符分隔各值
	 * @param value 要搜索的值
	 * @param separator 项分隔符
	 * @return 如果包含该值返回 true，否则返回 false
	 * @example
	 * <pre>{@code
	 * TextUtils.containsValue("apple;banana;orange", "banana", ";");  // -> true
	 * TextUtils.containsValue("apple|banana|orange", "banana", "|");  // -> true
	 * }</pre>
	 */
	public static boolean containsValue(String valueList, Object value, String separator) {
		return indexOfValue(valueList, value, separator) >= 0;
	}


	/**
	 * 查询指定值在值列表中的位置（使用自定义分隔符）
	 *
	 * @param valueList 字符串值列表，用指定分隔符分隔各值
	 * @param value 要搜索的值
	 * @param separator 项分隔符
	 * @return 值的位置索引，未找到返回 -1
	 * @see #indexOfValue(String, Object)
	 */
	public static int indexOfValue(String valueList, Object value, String separator) {
		return indexOfValue(valueList, value, true, separator);
	}

	/**
	 * 查询指定值在值列表中的位置
	 *
	 * @param valueList 字符串值列表
	 * @param value 要搜索的值
	 * @param valueIndexOfList true-返回值在列表中的值索引，false-返回值在字符串中的字符索引
	 * @param separator 项分隔符
	 * @return 位置索引，未找到返回 -1
	 * @example
	 * <pre>{@code
	 * // 返回值索引
	 * TextUtils.indexOfValue("apple,banana,orange", "banana", true, ",");   // -> 1
	 *
	 * // 返回字符索引
	 * TextUtils.indexOfValue("apple,banana,orange", "banana", false, ",");  // -> 6
	 * }</pre>
	 */
	public static int indexOfValue(String valueList, Object value, boolean valueIndexOfList, String separator) {
		return indexOf(valueList, value, valueIndexOfList, false, separator);
	}
	/**
	 * 查询指定值在对应列表的位置， 第一个值的位置为0，第二个值为1...
	 *
	 * @param valueList        字符串值集合(用项分隔符隔开各值)
	 * @param value            搜索的字符串值
	 * @param valueIndexOfList true-返回值在列表中的值索引，false-返回值在列表中的字符索引
	 * @param startsLike true-只匹配前缀，false-精确匹配
	 * @param separator        项分隔符
	 * @return int
	 */
	public static int indexOf(String valueList, Object value, boolean valueIndexOfList, boolean startsLike, String separator) {
		String item = toString(value);
		if (item != null) {
			return indexOf(valueList, item, 0, item.length(), valueIndexOfList, startsLike, separator);
		}
		return -1;
	}
	/**
	 * 查询指定值在对应列表的位置， 第一个值的位置为0，第二个值为1...
	 *
	 * @param valueList        字符串值集合(用项分隔符隔开各值)
	 * @param prefix            搜索的前缀字符串
	 * @param valueIndexOfList true-返回值在列表中的值索引，false-返回值在列表中的字符索引
	 * @param separator        项分隔符
	 * @return int
	 */
	public static int indexOfPrefix(String valueList, Object prefix, boolean valueIndexOfList, String separator) {
		return indexOf(valueList, prefix, valueIndexOfList, true, separator);
	}
	/**
	 * 查询指定值在对应列表的位置， 第一个值的位置为0，第二个值为1...
	 *
	 * @param valueList        字符串值集合(用项分隔符隔开各值)
	 * @param prefix            搜索的前缀字符串
	 * @param separator        项分隔符
	 * @return int
	 */
	public static int indexOfPrefix(String valueList, Object prefix, String separator) {
		return indexOf(valueList, prefix, true, true, separator);
	}
	/**
	 * 查询指定值在对应列表的位置， 第一个值的位置为0，第二个值为1...
	 *
	 * @param valueList        字符串值集合(用项分隔符隔开各值)
	 * @param prefix            搜索的前缀字符串
	 * @return int
	 */
	public static int indexOfPrefix(String valueList, Object prefix) {
		return indexOf(valueList, prefix, true, true, SEPARATOR);
	}

	private static int indexOf(String valueList, String value, int offset, int len, boolean valueIndexOfList, boolean startsLike, String separator) {
		if (valueList == null || len < 0 || valueList.length() < len) {
			return -1;
		}
		if (isEmpty(separator)) {
			return valueList.indexOf(value.substring(offset, offset + len));
		}
		if (valueList.regionMatches(0, value, offset, len) && (startsLike || valueList.length() == len || valueList.startsWith(separator, len))) {
			return 0;
		}
		int count = 1;
		int start = valueList.indexOf(separator);
		while (start >= 0) {
			start += separator.length();
			if (valueList.regionMatches(start, value, offset, len)) {
				int end = start + len;
				if (startsLike || valueList.length() == end || valueList.startsWith(separator, end)) {
					return valueIndexOfList ? count : start;
				}
			}
			start = valueList.indexOf(separator, start);
			count ++;
		}
		return -1;
	}


	/**
	 * 从值列表中移除第一个匹配的值
	 *
	 * @param valueList 字符串值列表，用逗号分隔
	 * @param value 要移除的值
	 * @return 移除后的值列表，如果值不存在则返回原列表
	 * @example
	 * <pre>{@code
	 * TextUtils.removeValue("apple,banana,orange", "banana");  // -> "apple,orange"
	 * TextUtils.removeValue("apple,banana,orange", "grape");   // -> "apple,banana,orange"
	 * }</pre>
	 */
	public static String removeValue(String valueList, Object value) {
		return removeValue(valueList, value, SEPARATOR);
	}


	/**
	 * 从值列表中移除第一个匹配的值（使用自定义分隔符）
	 *
	 * @param valueList 字符串值列表
	 * @param value 要移除的值
	 * @param separator 项分隔符
	 * @return 移除后的值列表，如果值不存在则返回原列表
	 * @see #removeValue(String, Object)
	 */
	public static String removeValue(String valueList, Object value, String separator) {
		String item = toString(value);
		if (item == null) {
			return valueList;
		}
		int start = indexOf(valueList, item, 0, item.length(), false, false, separator);
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
	 * 从值列表中移除指定位置的值
	 *
	 * @param valueList 字符串值列表
	 * @param valueIndex 值的位置索引（负数表示从后往前数）
	 * @param separator 项分隔符
	 * @return 移除后的值列表，如果索引超出范围则返回原列表
	 * @example
	 * <pre>{@code
	 * TextUtils.remove("apple,banana,orange", 1, ",");    // -> "apple,orange"
	 * TextUtils.remove("apple,banana,orange", -1, ",");   // -> "apple,banana"
	 * TextUtils.remove("apple,banana,orange", 5, ",");    // -> "apple,banana,orange"
	 * }</pre>
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
			if (startIndex >= 0) {
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

//	/**
//	 * 替换值列表中对应位置的值, 如果不存在, 则不变
//	 * @param valueList 字符串值集合(用项分隔符隔开各值)
//	 * @param valueIndex 位置
//	 * @param value 新值
//	 * @param separator 项分隔符
//	 * @return 返回替换后的值列表, 如果不变, 则返回原列表串对象
//	 */
//	private static String resetValue0(String valueList, int valueIndex, String value, boolean insert, String separator) {
//		if (valueList == null || insert && value == null) {
//			return valueList;
//		}
//		if (isEmpty(separator)) {
//			return resetString(valueList, valueIndex, value, insert);
//		}
//		int startIndex;
//		int endIndex;
//		if (valueIndex >= 0) {
//			// 从左到右算
//			startIndex = 0;
//			while ((endIndex = valueList.indexOf(separator, startIndex)) >= 0) {
//				if (valueIndex == 0) {
//					// 获取到位置
//					break;
//				}
//				// 继续下一个
//				startIndex = endIndex + separator.length();
//				--valueIndex;
//			}
//			if (startIndex >= 0) {
//				startIndex -= separator.length();
//			}
//		} else {
//			endIndex = valueList.length();
//			++valueIndex;
//			while ((startIndex = valueList.lastIndexOf(separator, endIndex - 1)) >= 0) {
//				if (valueIndex == 0) {
//					break;
//				}
//				endIndex = startIndex;
//				++valueIndex;
//			}
//		}
//		if (valueIndex != 0 && (!insert || value.isEmpty())) {
//			return valueList;
//		}
//		if (valueList.isEmpty()) {
//			return isNotEmpty(value) ? value : valueList;
//		}
//		// 获取前缀
//		String prefix = startIndex > 0 ? valueList.substring(0, startIndex) : EMPTY;
//		//
//		if (endIndex >= 0) {
//			// 有前缀时
//			if (value == null) {
//				if (startIndex < 0) {
//					return endIndex == valueList.length() ? EMPTY : valueList.substring(endIndex + separator.length());
//				} else {
//					return endIndex < valueList.length() ? prefix + valueList.substring(endIndex) : prefix;
//				}
//			}
//		} else {
//			// 没有后缀
//			return prefix;
//		}
//	}

	private static String resetString(String valueList, int valueIndex, String value, boolean insert) {
		if (valueIndex < 0) {
			valueIndex += valueList.length();
		}
		if (insert) {
			// 插入
			if (valueIndex >= 0 && valueIndex <= valueList.length()) {
				return valueList.substring(0, valueIndex) + value + valueList.substring(valueIndex);
			} else {
				return valueList;
			}
		}
		if (valueIndex >= 0 && valueIndex < valueList.length()) {
			if (value == null) {
				// 删除
				return valueList.substring(0, valueIndex) + valueList.substring(valueIndex + 1);
			}
			// 替换
			return valueList.substring(0, valueIndex) + value + valueList.substring(valueIndex + 1);
		} else {
			// 保存不变
			return valueList;
		}
	}

	/**
	 * 从值列表中移除指定位置的值（使用逗号分隔）
	 *
	 * @param valueList 字符串值列表
	 * @param valueIndex 值的位置索引
	 * @return 移除后的值列表
	 * @see #remove(String, int, String)
	 */
	public static String remove(String valueList, int valueIndex) {
		return remove(valueList, valueIndex, SEPARATOR);
	}

	/**
	 * 向值列表中添加新值（如果已存在则不添加）
	 *
	 * @param valueList 字符串值列表，用逗号分隔
	 * @param value 要添加的值
	 * @return 添加后的值列表，如果值已存在则返回原列表
	 * @example
	 * <pre>{@code
	 * TextUtils.addValue("apple,banana", "orange");  // -> "apple,banana,orange"
	 * TextUtils.addValue("apple,banana", "apple");   // -> "apple,banana"
	 * }</pre>
	 */
	public static String addValue(String valueList, Object value) {
		return addValue(valueList, value, SEPARATOR);
	}

	/**
	 * 向值列表中添加新值（使用自定义分隔符）
	 *
	 * @param valueList 字符串值列表
	 * @param value 要添加的值
	 * @param separator 项分隔符
	 * @return 添加后的值列表，如果值已存在则返回原列表
	 * @see #addValue(String, Object)
	 */
	public static String addValue(String valueList, Object value, String separator) {
		String item = toString(value);
		if (isEmpty(item) || indexOf(valueList, item, 0, item.length(), false, false, separator) != -1) {
			return valueList;
		}
		if (isEmpty(valueList)) {
			return item;
		}
		if (isEmpty(separator)) {
			return valueList + item;
		}
		if (item.contains(separator)) {
			int start = indexOf(item, valueList, 0, valueList.length(), false, false, separator);
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
	 * 检查值列表是否包含所有指定的值（使用逗号分隔）
	 *
	 * @param valueList 字符串值列表
	 * @param values 要检查的多个值，用逗号分隔
	 * @return 如果包含所有值返回 true，否则返回 false
	 * @example
	 * <pre>{@code
	 * TextUtils.containsAll("apple,banana,orange", "apple,banana");  // -> true
	 * TextUtils.containsAll("apple,banana,orange", "apple,grape");   // -> false
	 * }</pre>
	 */
	public static boolean containsAll(String valueList, String values) {
		return containsAll(valueList, values, SEPARATOR);
	}

	/**
	 * 检查值列表是否包含所有指定的值（使用自定义分隔符）
	 *
	 * @param valueList 字符串值列表
	 * @param values 要检查的多个值
	 * @param separator 项分隔符
	 * @return 如果包含所有值返回 true，否则返回 false
	 * @see #containsAll(String, String)
	 */
	public static boolean containsAll(String valueList, String values, String separator) {
		if (valueList == null || values == null) {
			return false;
		}
		if (indexOf(valueList, values, 0, values.length(), false, false, separator) != -1) {
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
				return indexOf(valueList, values, start, values.length() - start, false, false, separator) != -1;
			}
			if (indexOf(valueList, values, start, end - start, false, false, separator) == -1) {
				return false;
			}
			start = end + separator.length();
		}
		return true;
	}

	/**
	 * 从值列表中批量移除多个值（使用逗号分隔）
	 *
	 * @param valueList 字符串值列表
	 * @param values 要移除的多个值，用逗号分隔
	 * @return 移除后的值列表
	 * @example
	 * <pre>{@code
	 * TextUtils.removeAll("apple,banana,orange,grape", "banana,grape");
	 * // -> "apple,orange"
	 * }</pre>
	 */
	public static String removeAll(String valueList, String values) {
		return removeAll(valueList, values, SEPARATOR);
	}

	/**
	 * 从值列表中批量移除多个值（使用自定义分隔符）
	 *
	 * @param valueList 字符串值列表
	 * @param values 要移除的多个值
	 * @param separator 项分隔符
	 * @return 移除后的值列表
	 * @see #removeAll(String, String)
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
				int index = indexOf(valueList, values, start, len, false, false, separator);
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
	 * 向值列表中添加多个值（对于已存在的值忽略添加）
	 *
	 * @param valueList 字符串值列表，用逗号分隔
	 * @param values 要添加的多个值，用逗号分隔
	 * @return 添加后的值列表
	 * @example
	 * <pre>{@code
	 * TextUtils.addAll("apple,banana", "banana,orange,grape");
	 * // -> "apple,banana,orange,grape"
	 * }</pre>
	 */
	public static String addAll(String valueList, String values) {
		return addAll(valueList, values, SEPARATOR);
	}

	/**
	 * 向值列表中添加多个值（使用自定义分隔符）
	 *
	 * @param valueList 字符串值列表
	 * @param values 要添加的多个值
	 * @param separator 项分隔符
	 * @return 添加后的值列表
	 * @see #addAll(String, String)
	 */
	public static String addAll(String valueList, String values, String separator) {
		if (isEmpty(values) || indexOf(valueList, values, 0, values.length(), false, false, separator) != -1) {
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
			if (start < end && indexOf(valueList, values, start, end - start, false, false, separator) == -1) {
				result.append(separator).append(values, start, end);
			}
			start = end + separator.length();
		}
		return result.toString();
	}

	/**
	 * 获取两个值列表的交集
	 * <p>
	 * 如果没有交集则返回空串，结果按第一个列表的顺序返回。
	 * </p>
	 *
	 * @param firstList 第一个字符串值列表，用逗号分隔
	 * @param secondList 第二个字符串值列表，用逗号分隔
	 * @return 交集结果，如果没有交集返回空串
	 * @example
	 * <pre>{@code
	 * TextUtils.retainAll("apple,banana,orange", "banana,grape");
	 * // -> "banana"
	 * }</pre>
	 */
	public static String retainAll(String firstList, String secondList) {
		return retainAll(firstList, secondList, SEPARATOR);
	}


	/**
	 * 获取两个值列表的交集（使用自定义分隔符）
	 * <p>
	 * 如果没有交集则返回空串，结果按第一个列表的顺序返回。
	 * </p>
	 *
	 * @param firstList 第一个字符串值列表
	 * @param secondList 第二个字符串值列表
	 * @param separator 项分隔符
	 * @return 交集结果，如果没有交集返回空串
	 * @see #retainAll(String, String)
	 */
	public static String retainAll(String firstList, String secondList, String separator) {
		if (firstList == null || secondList == null) {
			return null;
		}
		if (firstList.isEmpty() || secondList.isEmpty()) {
			return firstList.isEmpty() ? firstList : secondList;
		}

		if (Objects.equals(firstList, secondList)
				|| (secondList.length() > firstList.length() && indexOf(secondList, firstList, 0, firstList.length(), false, false, separator) != -1)) {
			return firstList;
		}
		if (firstList.length() > secondList.length() && indexOf(firstList, secondList, 0, secondList.length(), false, false, separator) != -1) {
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
			if (start <= end && indexOf(secondList, firstList, start, end - start, false, false, separator) != -1) {
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
	 * 获取值列表元素个数（使用逗号分隔）
	 *
	 * @param valueList 字符串值列表，用逗号分隔
	 * @return 值列表元素个数
	 * @example
	 * <pre>{@code
	 * TextUtils.getSize("apple,banana,orange");  // -> 3
	 * TextUtils.getSize("");                      // -> 0
	 * }</pre>
	 */
	public static int getSize(String valueList) {
		return getSize(valueList, SEPARATOR);
	}

	/**
	 * 获取值列表元素个数（使用自定义分隔符）
	 *
	 * @param valueList 字符串值列表
	 * @param separator 项分隔符
	 * @return 值列表元素个数
	 * @see #getSize(String)
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

	public static boolean isNotEmpty(Object value) {
		return !isEmpty(value);
	}

	/**
	 * 获取指定位置的值（使用逗号分隔）
	 * <p>
	 * 如果列表是 null 或值索引超出列表元素边界时返回 null。
	 * </p>
	 *
	 * @param valueList 字符串值列表，用逗号分隔
	 * @param valueIndex 值在列表中的索引，负值代表从后面开始算
	 * @return 对应索引的项字符串值，未找到返回 null
	 * @example
	 * <pre>{@code
	 * TextUtils.getValue("apple,banana,orange", 0);   // -> "apple"
	 * TextUtils.getValue("apple,banana,orange", 1);   // -> "banana"
	 * TextUtils.getValue("apple,banana,orange", -1);  // -> "orange"
	 * }</pre>
	 */
	public static String getValue(String valueList, int valueIndex) {
		return getValue(valueList, valueIndex, SEPARATOR);
	}


	/**
	 * 获取指定位置的值（使用自定义分隔符）
	 * <p>
	 * 如果列表是 null 或值索引超出列表元素边界时返回 null。
	 * </p>
	 *
	 * @param valueList 字符串值列表
	 * @param valueIndex 值在列表中的索引，负值代表从后面开始算
	 * @param separator 项分隔符
	 * @return 对应索引的项字符串值，未找到返回 null
	 * @see #getValue(String, int)
	 */
	public static String getValue(String valueList, int valueIndex, String separator) {
		return getValues(valueList, valueIndex, 1, separator);
	}

	/**
	 * 获取指定位置的多个值（使用逗号分隔）
	 * <p>
	 * 如果列表是 null 或值索引超出列表元素边界时返回 null。
	 * </p>
	 *
	 * @param valueList 字符串值列表，用逗号分隔
	 * @param offset 起始索引，负值代表从后面开始算
	 * @param len 子列表长度，负值代表向后算
	 * @return 对应范围的项字符串值
	 * @example
	 * <pre>{@code
	 * TextUtils.getValues("apple,banana,orange,grape", 1, 2);
	 * // -> "banana,orange"
	 * }</pre>
	 */
	public static String getValues(String valueList, int offset, int len) {
		return getValues(valueList, offset, len, SEPARATOR);
	}

	/**
	 * 获取指定位置的多个值（使用自定义分隔符）
	 * <p>
	 * 如果列表是 null 或值索引超出列表元素边界时返回 null。
	 * </p>
	 *
	 * @param valueList 字符串值列表
	 * @param offset 起始索引，负值代表从后面开始算
	 * @param len 子列表长度，负值代表向后算
	 * @param separator 项分隔符
	 * @return 对应范围的项字符串值
	 * @see #getValues(String, int, int)
	 */
	public static String getValues(String valueList, int offset, int len, String separator) {
		if (isEmpty(separator)) {
			return substr(valueList, offset, len);
		}
		if (valueList == null) {
			return null;
		}
		int end;
		if (offset < 0) {
			// 如果从后面位置开始算时，计算最后一个值的位置，并调整成向前获取
			if (len > 1) {
				if (offset + len >= 0) {
					len = offset;
					offset = -1;
				} else {
					offset += len - 1;
					len = -len;
				}
			}
		} else {
			// 如果从前面位置开始算时，计算最前一个值的位置，并调整成向后获取
			if (len < -1) {
				if (offset + len < 0) {
					len = offset + 1;
					offset = 0;
				} else {
					offset += len + 1;
					len = -len;
				}
			}
		}
		int start;
		int separatorLen = separator.length();
		boolean first = true;
		if (offset >= 0) {
			start = 0;
			end = 0;
			while ((end = valueList.indexOf(separator, end)) >= 0) {
				if (offset == 0) {
					if (!first) {
						break;
					} else {
						first = false;
						if (len > 1) {
							offset = len - 1;
						} else {
							break;
						}
					}
				}
				end += separatorLen;
				if (first) {
					start = end;
				}
				--offset;
			}
			if (first && offset == 0) {
				first = false;
			}
		} else {
			end = -1;
			start = valueList.length();
			++offset;
			while ((start = valueList.lastIndexOf(separator, start - 1)) >= 0) {
				if (offset == 0) {
					if (!first) {
						// 已获取过最后一个值
						break;
					} else {
						first = false;
						if (len < -1) {
							offset = len + 1;
						} else {
							break;
						}
					}
				}
				if (first) {
					// 记录最后一个值位置
					end = start;
				}
				++offset;
			}
			if (first) {
				if (offset == 0) {
					first = false;
					start = 0;
				}
			} else {
				start = start < 0 ? 0 : start + separatorLen;
			}
		}
		return first ? null : (len == 0 ? EMPTY : (end < 0 ? valueList.substring(start) : valueList.substring(start, end)));
	}

	/**
	 * 获取子字符串
	 * <p>
	 * 支持负数索引，负值代表从字符串末尾开始计算。
	 * </p>
	 *
	 * @param value 原始字符串
	 * @param offset 起始位置，负值代表从后面开始算
	 * @param len 长度，负值代表向后算
	 * @return 子字符串，超出范围返回 null
	 * @example
	 * <pre>{@code
	 * TextUtils.substr("Hello World", 0, 5);     // -> "Hello"
	 * TextUtils.substr("Hello World", -5, 5);    // -> "World"
	 * TextUtils.substr("Hello World", 6, -1);    // -> "W"
	 * }</pre>
	 */
	public static String substr(String value, int offset, int len) {
		if (value == null) {
			return null;
		}
		int end;
		int valueLength = value.length();
		if (offset < 0) {
			offset += valueLength;
		}
		end = offset + len;
		if (len < 0) {
			int t = end; end = offset + 1; offset = t + 1;
		}
		if (end >= 0 && offset <= valueLength) {
			// 返回子字符串
			return offset == end ? EMPTY : value.substring(Math.max(offset, 0), Math.min(end, valueLength));
		}
		// 超出字符串范围
		return null;
	}

	/**
	 * 将值列表转换为字符串列表（使用逗号分隔）
	 *
	 * @param valueList 字符串值列表，用逗号分隔
	 * @return 字符串列表
	 * @example
	 * <pre>{@code
	 * List<String> list = TextUtils.asList("apple,banana,orange");
	 * // list: ["apple", "banana", "orange"]
	 * }</pre>
	 */
	public static List<String> asList(String valueList) {
		return asList(valueList, SEPARATOR);
	}

	/**
	 * 将值列表转换为字符串列表（使用自定义分隔符）
	 *
	 * @param valueList 字符串值列表
	 * @param separator 项分隔符
	 * @return 字符串列表
	 * @see #asList(String)
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
	 * 检查值列表中是否存在以指定前缀开头的项（使用自定义分隔符）
	 *
	 * @param valueList 字符串值列表
	 * @param prefix 要匹配的前缀
	 * @param separator 项分隔符
	 * @return 如果存在匹配的项返回 true，否则返回 false
	 * @example
	 * <pre>{@code
	 * TextUtils.anyStartsWith("apple,banana,orange", "app", ",");  // -> true
	 * TextUtils.anyStartsWith("apple,banana,orange", "xyz", ",");  // -> false
	 * }</pre>
	 */
	public static boolean anyStartsWith(String valueList, String prefix, String separator) {
		return prefix != null && indexOf(valueList, prefix, 0, prefix.length(), false, true, separator) != -1;
	}

	/**
	 * 检查值列表中是否存在以指定前缀开头的项（使用逗号分隔）
	 *
	 * @param valueList 字符串值列表，用逗号分隔
	 * @param prefix 要匹配的前缀
	 * @return 如果存在匹配的项返回 true，否则返回 false
	 * @see #anyStartsWith(String, String, String)
	 */
	public static boolean anyStartsWith(String valueList, String prefix) {
		return anyStartsWith(valueList, prefix, SEPARATOR);
	}

	/**
	 * 将集合转换为字符串（使用自定义分隔符）
	 *
	 * @param values 可迭代集合
	 * @param separator 项分隔符
	 * @return 连接后的字符串
	 * @example
	 * <pre>{@code
	 * List<String> list = Arrays.asList("apple", "banana", "orange");
	 * String result = TextUtils.toString(list, ", ");
	 * // result: "apple, banana, orange"
	 * }</pre>
	 */
	public static String toString(Iterable<?> values, String separator) {
		if (values == null) {
			return EMPTY;
		}
		StringBuilder result = new StringBuilder();
		for (Object value : values) {
			if (value != null) {
				result.append(getInstance().objectToString(value));
			}
			if (separator != null && !separator.isEmpty()) {
				result.append(separator);
			}
		}
		if (result.length() > 0 && separator != null && !separator.isEmpty()) {
			result.setLength(result.length() - separator.length());
		}
		return result.toString();
	}

	/**
	 * 将数组转换为字符串（使用自定义分隔符）
	 *
	 * @param values 对象数组
	 * @param separator 项分隔符
	 * @return 连接后的字符串
	 * @example
	 * <pre>{@code
	 * String[] arr = {"apple", "banana", "orange"};
	 * String result = TextUtils.toString(arr, " - ");
	 * // result: "apple - banana - orange"
	 * }</pre>
	 */
	public static<T> String toString(T[] values, String separator) {
		if (values == null || values.length == 0) {
			return EMPTY;
		}
		StringBuilder result = new StringBuilder();
        for (int i = 0, len = values.length; i < len; ++i) {
            Object value = values[i];
            if (value != null) {
                result.append(getInstance().objectToString(value));
            }
            if (separator != null && !separator.isEmpty()) {
                result.append(separator);
            }
        }
		if (result.length() > 0 && separator != null && !separator.isEmpty()) {
			result.setLength(result.length() - separator.length());
		}
		return result.toString();
	}

	/**
	 * 将数组转换为字符串（使用逗号分隔）
	 *
	 * @param values 对象数组
	 * @return 连接后的字符串
	 * @see #toString(Object[], String)
	 */
	@SafeVarargs
	public static<T> String toString(T... values) {
		return toString(values, SEPARATOR);
	}

	/**
	 * 将对象转换为字符串
	 * <p>
	 * null 值转换为 null，Iterable 和数组会递归处理。
	 * </p>
	 *
	 * @param value 要转换的值
	 * @return 字符串表示，null 返回 null
	 * @example
	 * <pre>{@code
	 * TextUtils.toString("hello");        // -> "hello"
	 * TextUtils.toString(null);           // -> null
	 * TextUtils.toString(123);            // -> "123"
	 * }</pre>
	 */
	public static<T> String toString(T value) {
		if (value instanceof Iterable) {
			return toString((Iterable<?>) value);
		}
		if (value instanceof Object[]) {
			return toString((Object[]) value);
		}
		return value != null ? getInstance().objectToString(value) : null;
	}

	protected String objectToString(Object value) {
		return value.toString();
	}

	/**
	 * 将集合转换为字符串（使用逗号分隔）
	 *
	 * @param values 可迭代集合
	 * @return 连接后的字符串
	 * @see #toString(Iterable, String)
	 */
	public static String toString(Iterable<?> values) {
		return toString(values, SEPARATOR);
	}

	/**
	 * 移除字符串后缀
	 *
	 * @param value 原始字符串
	 * @param suffix 要移除的后缀
	 * @return 移除后缀后的结果，如果不匹配则返回原字符串
	 * @example
	 * <pre>{@code
	 * TextUtils.removeSuffix("hello.txt", ".txt");   // -> "hello"
	 * TextUtils.removeSuffix("hello.txt", ".jpg");   // -> "hello.txt"
	 * TextUtils.removeSuffix("test", "test");         // -> ""
	 * }</pre>
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
	 * 移除字符串前缀
	 *
	 * @param value 原始字符串
	 * @param prefix 要移除的前缀
	 * @return 移除前缀后的结果，如果不匹配则返回原字符串
	 * @example
	 * <pre>{@code
	 * TextUtils.removePrefix("hello.txt", "hello.");  // -> "txt"
	 * TextUtils.removePrefix("hello.txt", "world.");  // -> "hello.txt"
	 * TextUtils.removePrefix("test", "test");          // -> ""
	 * }</pre>
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
