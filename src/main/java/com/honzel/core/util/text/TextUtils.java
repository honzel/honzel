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
 * 字符文本工具类<br>
 * 一. 格式化时某些字符有特殊功能，需要转义后才能输出(转义字符为'\'):
 *  <br>1. 在占位符外的文本需要转义的字符:<ul>
 *    <li>
 *        format,alternateFormat方法: '$', 如AAA\$BBB${ccc[XXX]}, 第一个$需要转义
 *    </li>
 *    <li>
 *        simplifiedFormat方法: '{', 如AAA\{BBB{ccc[XXX]}, 第一个'{'需要转义
 *    </li>
 *   <li>
 *       alternateSimplifiedFormat方法: '(', 如AAA\(BBB(ccc[XXX]), 第一个'('需要转义
 *   </li>
 *  </ul>
 *  <br>2. 在占位符内并在[]之外的字符串需要转义的字符:<ul>
 *    <li>
 *        format,simplifiedFormat方法: '}[;', 如AAA${bbb\;ccc[XXX]}, 分号';'需要转义
 *    </li>
 *    <li>
 *        alternateFormat,alternateSimplifiedFormat方法: ')[;', 如AAA$(bbb\)ccc[XXX]), 第一个')'需要转义
 *    </li>
 *  </ul>
 *  <br>3. 在占位符内并在[]内的字符串需要转义的字符:<ul>
 *    <li>
 *        format,simplifiedFormat方法: '}]=;', 如AAA${bbb[ccc\]XXX]}, 第一个']'需要转义
 *    </li>
 *    <li>
 *        alternateFormat,alternateSimplifiedFormat 方法: ')]=;', 如AAA$(bbb[ccc\)XXX]), 第一个')'需要转义
 *    </li>
 *  </ul>
 *  二. 数据格式的文本标识(textFormatType:json,xml,url,txt): json-Json值;xml-XML标签内容或属性内容;url-url参数;txt-普通文本
 *  <br>格式化时某些字符作为开头字符的意义:<ul>
 *    <li>
 *        textFormatType+';'放占位符最前面: 如AAABBB${json;ccc[XXX]},
 *        表示ccc占位符按json转码再输出; 如果textFormatType为空当成普通文本输出;
 *        如果占位符没有该标识时使用默认数据格式进行转码,如${ccc}的ccc使用参数传入的默认数据格式
 *    </li>
 *    <li>
 *        ‘#'放在紧接占位符变量后面的[]中最前面: 如AAABBB${ccc[#1=FFF;2-GGG;*]},
 *        表示FFF,GGG为内层子格式文本,匹配逻辑是当ccc=1时输出FFF,ccc=2时输出GGG,
 *        *代表其他值原值输出, 不加*的话其他值都输出空值, 按占位符数据格式进行转码
 *    </li>
 *    <li>
 *        '#'+textFormatType+';'放在紧接占位符变量后面的[]中最前面: 如AAABBB${ccc[#json;1=FFF;2-GGG]},
 *        表示FFF,GGG为内层子格式文本,匹配逻辑是当ccc=1时输出FFF,ccc=2时输出GGG, 因为没加*其他值都输出空值,
 *        其中FFF,GGG子格式文本使用json作为默认转码格式进行格式化后得到最后结果, 再按占位符数据格式进行转码
 *    </li>
 *    <li>
 *        ‘^'放在[]中的最前面: 表示当占位符变量为空时输出,最前面不加'^'的表示占位符变量非空时与变量值一同输出，
 *        如AAABBB${[XXX]ccc[^YYY][ZZZ]}, 当ccc为空时输出YYY,当ccc非空时输出XXXcccZZZ,
 *        其中XXX,YYY,ZZZ都为常量串,直接输出不会进行转码; 需要转码时请使用上两条规则处理
 *    </li>
 *  </ul>
 *  <br>格式化文本与子格式化文本的占位符:<ul>
 *    <li>
 *        format(占位符:${})与alternateFormat(占位符:$())互为父子格式,
 *        当外层格式为format内层格式会使用alternateFormat,两者反过来也一样。
 *        如AAA${bbb[#1=CCC$(ddd);2=XXX$(yyy)ZZZ]}, 其中bbb为外层父格式变量,ddd,yyy为内层子格式变量
 *    </li>
 *    <li>
 *        simplifiedFormat(占位符:{})与alternateSimplifiedFormat(占位符:())互为父子格式,
 *        当外层格式为simplifiedFormat内层格式会使用alternateSimplifiedFormat,两者反过来也一样。
 *        如AAA{bbb[#1=CCC(ddd);2=XXX(yyy)ZZZ]}, 其中bbb为外层父格式变量,ddd,yyy为内层子格式变量
 *    </li>
 *  </ul>
 *
 * @author honzel
 * date 2021/2/27
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
	 * 注册格式类型, 如果已经存在同名则返回false
	 * @param textFormatType 格式类型
	 * @return 返回是否注册成功
	 */
	public static boolean registerFormatType(TextFormatType textFormatType) {
		TextFormatType oldFormatType = FORMAT_TYPE_MAP.put(textFormatType.getUniqueId(), textFormatType);
		if (textFormatType.supportsAutoMatch() && !textFormatType.equals(oldFormatType)) {
			if (Objects.nonNull(oldFormatType) && oldFormatType.supportsAutoMatch()) {
				AUTO_MATCH_FORMAT_TYPE_QUEUE.remove(oldFormatType);
			}
			AUTO_MATCH_FORMAT_TYPE_QUEUE.add(textFormatType);
		}
		return false;
	}

	public static TextFormatType getFormatType(String tag) {
		return FORMAT_TYPE_MAP.get(tag);
	}

	/**
	 * 查找格式类型
	 * @param content 内容
	 * @return 格式类型
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
	 * 获取格式中的参数占位符map, 使用${xxx}作为第一层占位符
	 * @param result 结果
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseParamMap(Map<String, Object> result, String pattern, Object params) {
		return parseParamMap0(result, false, pattern, params, false);
	}

	/**
	 * 获取格式中的参数占位符map, 使用${xxx}作为第一层占位符
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseParamMap(String pattern, Object params) {
		return parseParamMap0(new HashMap<>(), false, pattern, params, false);
	}

	/**
	 * 获取格式中的参数占位符map, 使用$(xxx)作为第一层占位符
	 * @param result 结果
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseAlternateParamMap(Map<String, Object> result, String pattern, Object params) {
		return parseParamMap0(result, true, pattern, params, false);
	}

	/**
	 * 获取格式中的参数占位符map, 使用$(xxx)作为第一层占位符
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseAlternateParamMap(String pattern, Object params) {
		return parseParamMap0(new HashMap<>(), true, pattern, params, false);
	}


	/**
	 * 获取格式中的参数占位符map, 使用{xxx}作为第一层占位符
	 * @param result 结果
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseSimplifiedParamMap(Map<String, Object> result, String pattern, Object params) {
		return parseParamMap0(result, false, pattern, params, true);
	}

	/**
	 * 获取格式中的参数占位符map, 使用{xxx}作为第一层占位符
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseSimplifiedParamMap(String pattern, Object params) {
		return parseParamMap0(new HashMap<>(), false, pattern, params, true);
	}

	/**
	 * 获取格式中的参数占位符map, 使用(xxx)作为第一层占位符
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
	 */
	public static Map<String, Object> parseAlternateSimplifiedParamMap(String pattern, Object params) {
		return parseParamMap0(new HashMap<>(), true, pattern, params, true);
	}

	/**
	 * 获取格式中的参数占位符map, 使用(xxx)作为第一层占位符
	 * @param result 结果
	 * @param pattern 格式模板
	 * @param params 占位符参数
	 * @return 每个参数占位符对应的值
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
	 * 格式化字符串文本, 使用${xxx}作为第一层占位符
	 * @param textFormatType 数据类型
	 * @param pattern 待格式化内容
	 * @param configParams 配置
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String format(TextFormatType textFormatType, String pattern, Object configParams, Object params) {
		return format0(false, textFormatType, pattern, configParams, params, null, null, false);
	}

	/**
	 * 格式化字符串文本, 使用${xxx}作为第一层占位符
	 * @param pattern 待格式化内容
	 * @param param 参数
	 * @return 返回格式化后内容
	 */
	public static String format(String pattern, Object param) {
		return format0(false, lookupFormatType(pattern), pattern, null, param, null, null, false);
	}

	/**
	 * 格式化字符串文本, 使用${xxx}作为第一层占位符
	 * @param textFormatType 数据类型
	 * @param pattern 待格式化内容
	 * @param param 参数
	 * @return 返回格式化后内容
	 */
	public static String format(TextFormatType textFormatType, String pattern, Object param) {
		return format0(false, textFormatType, pattern, null, param, null, null, false);
	}

	/**
	 * 格式化字符串文本, 使用${xxx}作为第一层占位符
	 * @param pattern 待格式化内容
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String format(String pattern, Object... params) {
		return format0(false, lookupFormatType(pattern), pattern, null, params, null, null, false);
	}

	/**
	 * 格式化字符串文本, 使用{xxx}作为第一层占位符
	 * @param textFormatType 数据类型
	 * @param pattern 待格式化内容
	 * @param configParams 配置
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String simplifiedFormat(TextFormatType textFormatType, String pattern, Object configParams, Object params) {
		return format0(false, textFormatType, pattern, configParams, params, null, null, true);
	}

	/**
	 * 格式化字符串文本, 使用{xxx}作为第一层占位符
	 * @param pattern 待格式化内容
	 * @param param 参数
	 * @return 返回格式化后内容
	 */
	public static String simplifiedFormat(String pattern, Object param) {
		return format0(false, lookupFormatType(pattern), pattern, null, param, null, null, true);
	}

	/**
	 * 格式化字符串文本, 使用{xxx}作为第一层占位符
	 * @param textFormatType 数据类型
	 * @param pattern 待格式化内容
	 * @param param 参数
	 * @return 返回格式化后内容
	 */
	public static String simplifiedFormat(TextFormatType textFormatType, String pattern, Object param) {
		return format0(false, textFormatType, pattern, null, param, null, null, true);
	}

	/**
	 * 格式化字符串文本, 使用{xxx}作为第一层占位符
	 * @param pattern 待格式化内容
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String simplifiedFormat(String pattern, Object... params) {
		return format0(false, lookupFormatType(pattern), pattern, null, params, null, null, true);
	}

	/**
	 * 格式化字符串文本, 使用(xxx)作为第一层占位符
	 * @param textFormatType 数据类型
	 * @param pattern 待格式化内容
	 * @param configParams 配置
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateSimplifiedFormat(TextFormatType textFormatType, String pattern, Object configParams, Object params) {
		return format0(true, textFormatType, pattern, configParams, params, null, null, true);
	}

	/**
	 * 格式化字符串文本, 使用(xxx)作为第一层占位符
	 * @param pattern 待格式化内容
	 * @param param 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateSimplifiedFormat(String pattern, Object param) {
		return format0(true, lookupFormatType(pattern), pattern, null, param, null, null, true);
	}

	/**
	 * 格式化字符串文本, 使用(xxx)作为第一层占位符
	 * @param textFormatType 数据类型
	 * @param pattern 待格式化内容
	 * @param param 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateSimplifiedFormat(TextFormatType textFormatType, String pattern, Object param) {
		return format0(true, textFormatType, pattern, null, param, null, null, true);
	}

	/**
	 * 格式化字符串文本, 使用(xxx)作为第一层占位符
	 * @param pattern 待格式化内容
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateSimplifiedFormat(String pattern, Object... params) {
		return format0(true, lookupFormatType(pattern), pattern, null, params, null, null, true);
	}

	/**
	 * 格式化字符串文本, 使用$(xxx)作为第一层占位符
	 * @param textFormatType 数据类型
	 * @param pattern 待格式化内容
	 * @param configParams 配置
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateFormat(TextFormatType textFormatType, String pattern, Object configParams, Object params) {
		return format0(true, textFormatType, pattern, configParams, params, null, null, false);
	}

	/**
	 * 格式化字符串文本, 使用$(xxx)作为第一层占位符
	 * @param textFormatType 数据类型
	 * @param pattern 待格式化内容
	 * @param params 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateFormat(TextFormatType textFormatType, String pattern, Object params) {
		return format0(true, textFormatType, pattern, null, params, null, null, false);
	}

	/**
	 * 格式化字符串文本, 使用$(xxx)作为第一层占位符
	 * @param pattern 待格式化内容
	 * @param param 参数
	 * @return 返回格式化后内容
	 */
	public static String alternateFormat(String pattern, Object param) {
		return format0(true, lookupFormatType(pattern), pattern, null, param, null, null, false);
	}

	/**
	 * 格式化字符串文本, 使用$(xxx)作为第一层占位符
	 * @param pattern 待格式化内容
	 * @param params 参数
	 * @return 返回格式化后内容
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
				if (propValue == null && (offset == 0 && index < 0 || index == 0)
						&& Modifier.isFinal(params.getClass().getModifiers()) && BeanHelper.getPropertyType(params, name) == null) {
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
		return params;
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

	public static boolean isNotEmpty(Object value) {
		return !isEmpty(value);
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
		return getValues(valueList, valueIndex, 1, separator);
	}

	/**
	 * 获取指定位置的值, 如果列表是null或值索引超出列表元素边界时返回null
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param offset 值在列表中的值起始索引，负值代表从后面开始算
	 * @param len 子列表长度, 负值代表向后算
	 * @return 返回对应索引的项字符串值
	 */
	public static String getValues(String valueList, int offset, int len) {
		return getValues(valueList, offset, len, SEPARATOR);
	}

	/**
	 * 获取指定位置的值, 如果列表是null或值索引超出列表元素边界时返回null
	 * @param valueList 字符串值集合(用项分隔符隔开各值)
	 * @param offset 值在列表中的值起始索引，负值代表从后面开始算
	 * @param len 子列表长度, 负值代表向后算
	 * @param separator 项分隔符
	 * @return 返回对应索引的项字符串值
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
	 * @param value 字符串值
	 * @param offset 起始位置，负值代表从后面开始算
	 * @param len 长度，负值代表向后算
	 * @return 子字符串
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
		return prefix != null && indexOf(valueList, prefix, 0, prefix.length(), false, true, separator) != -1;
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
	 * 转化成字符串
	 * @param values 数组
	 * @param separator 项分隔符
	 * @return 字符串
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
		return value != null ? getInstance().objectToString(value) : null;
	}

	protected String objectToString(Object value) {
		return value.toString();
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
