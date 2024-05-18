package com.honzel.core.util.text;

import com.honzel.core.constant.NumberConstants;
import com.honzel.core.util.time.LocalDateTimeUtils;
import com.honzel.core.util.web.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static com.honzel.core.util.text.TextUtils.*;


/**
 * 文本格式化类型
 * @author luhz
 * date 2024/4/27
 */
public enum FormatTypeEnum implements TextFormatType {

    /**
     * 默认格式化类型
     */
    SIMPLE(EMPTY) {
        public boolean preliminaryMatch(String format) {
            return isEmpty(format);
        }

        public String formatValue(Object value, String... parameters) {
            if (isNotEmpty(value) && parameters.length > 0 && isNotEmpty(parameters[0])) {
                String pattern = parameters[0];
                try {
                    if (value instanceof TemporalAccessor) {
                        return LocalDateTimeUtils.format((TemporalAccessor) value, pattern);
                    } else if (value instanceof Number) {
                        return new DecimalFormat(pattern).format(value);
                    } else if (value instanceof Date) {
                        return new SimpleDateFormat(pattern).format((Date) value);
                    } else if (value instanceof Calendar) {
                        return new SimpleDateFormat(pattern).format(((Calendar) value).getTime());
                    } else {
                        // 不支持格式化
                        return null;
                    }
                } catch (Exception e) {
                    log.error("数据格式化失败: {}", e.getMessage(), e);
                    return EMPTY;
                }
            }
            return TextUtils.toString(value);
        }
    },
    /**
     * JSON格式化类型
     */
    JSON("json") {
        public boolean preliminaryMatch(String format) {
            return Objects.nonNull(format) && !EMPTY.equals(format = format.trim()) && (format.startsWith(BRACE_START) && format.endsWith(BRACE_END) || format.startsWith(BRACKET_START) && format.endsWith(BRACKET_END));
        }
        @Override
        public void appendValue(StringBuilder formattedContent, String formattedValue) {
            int lastIndex = -1;
            for (int i = 0, len = formattedValue.length(); i < len; i++) {
                char ch = formattedValue.charAt(i);
                if (ch >= ' ' && ch != '\"' && ch != '\\' && !Character.isHighSurrogate(ch)) {
                    continue;
                }
                if (lastIndex + 1 < i) {
                    formattedContent.append(formattedValue, lastIndex + 1, i);
                }
                switch(ch) {
                    case '\\':
                        formattedContent.append("\\\\");
                        break;
                    case '\"':
                        formattedContent.append("\\\"");
                        break;
                    case '\b':
                        formattedContent.append("\\b");
                        break;
                    case '\t':
                        formattedContent.append("\\t");
                        break;
                    case '\n':
                        formattedContent.append("\\n");
                        break;
                    case '\r':
                        formattedContent.append("\\r");
                        break;
                    case '\f':
                        formattedContent.append("\\f");
                        break;
                    default:
                        if (Character.isHighSurrogate(ch) && i < len - 1) {
                            char c2 = formattedValue.charAt(++i);
                            if (Character.isLowSurrogate(c2)) {
                                int codePoint = Character.toCodePoint(ch, c2);
                                if (Character.isSupplementaryCodePoint(codePoint)) {
                                    formattedContent.append("\\u").append(Integer.toHexString(codePoint));
                                } else {
                                    formattedContent.append("\\u00").append(Integer.toHexString(codePoint));
                                }
                            } else {
                                formattedContent.append("\\u").append(Integer.toHexString(ch));
                                formattedContent.append("\\u").append(Integer.toHexString(c2));
                            }
                        } else {
                            formattedContent.append("\\u").append(Integer.toHexString(ch));
                        }
                        break;
                }
                lastIndex = i;
            }
            if (lastIndex >= 0) {
                formattedContent.append(formattedValue, lastIndex + 1, formattedValue.length());
            } else {
                formattedContent.append(formattedValue);
            }
        }
    },
    /**
     * XML格式化类型
     */
    XML("xml") {
        public boolean preliminaryMatch(String format) {
            return Objects.nonNull(format) && !EMPTY.equals(format = format.trim()) && format.startsWith("<") && format.endsWith(">");
        }
        @Override
        public void appendValue(StringBuilder formattedContent, String formattedValue) {
            for (int i = 0, len = formattedValue.length(); i < len; i++) {
                char ch = formattedValue.charAt(i);
                switch (ch) {
                    case '&': formattedContent.append("&amp;");
                        break;
                    case '"': formattedContent.append("&quot;");
                        break;
                    case '<': formattedContent.append("&lt;");
                        break;
                    case '>': formattedContent.append("&gt;");
                        break;
                    case '\'': formattedContent.append("&apos;");
                        break;
                    default:
                        formattedContent.append(ch);
                        break;
                }
            }
        }
    },
    /**
     * URL编码格式化类型
     */
    URL_ENCODING("url") {
        public boolean preliminaryMatch(String format) {
            return Objects.nonNull(format) && !EMPTY.equals(format = format.trim()) && format.lastIndexOf("://", NumberConstants.INTEGER_TEN * NumberConstants.INTEGER_TWO) > NumberConstants.INTEGER_ZERO;
        }
        @Override
        public void appendValue(StringBuilder formattedContent, String formattedValue) {
            formattedContent.append(WebUtils.encode(formattedValue));
        }
    },
    /**
     * 子字符串格式化类型
     */
    SUB_STR("str") {
        public String formatValue(Object value, String... parameters) {
            String stringValue = TextUtils.toString(value);
            if (isNotEmpty(stringValue) && parameters.length > 0) {
                // 获取偏移量
                int offset = EMPTY.equals(parameters[0]) ? NumberConstants.INTEGER_ZERO : Integer.parseInt(parameters[0]);
                // 计算结束位置
                int end = parameters.length > 1 && !EMPTY.equals(parameters[1]) ? offset + Integer.parseInt(parameters[1]) : stringValue.length();
                // 校正偏移量
                offset = offset < NumberConstants.INTEGER_ZERO ? Math.max(stringValue.length() + offset, NumberConstants.INTEGER_ZERO) : Math.min(offset, stringValue.length());
                // 校正结束位置
                end = end < NumberConstants.INTEGER_ZERO ? Math.max(stringValue.length() + end, NumberConstants.INTEGER_ZERO) : Math.min(end, stringValue.length());
                // 返回子字符串
                return offset == end ? EMPTY : offset < end ? stringValue.substring(offset, end) : stringValue.substring(end, offset);
            }
            return stringValue;
        }
    },
    ;

    private static final Logger log = LoggerFactory.getLogger(FormatTypeEnum.class);
    private final String uniqueId;

     FormatTypeEnum(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }



}
