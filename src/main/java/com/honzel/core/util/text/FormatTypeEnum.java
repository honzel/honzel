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

import static com.honzel.core.constant.NumberConstants.*;
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

        /**
         * 格式化值
         * @param value 值
         * @param parameters 参数
         * @return 格式化后的值
         */
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
            return Objects.nonNull(format) && !EMPTY.equals(format = format.trim()) && format.lastIndexOf("://", NumberConstants.INTEGER_TEN * NumberConstants.INTEGER_TWO) > INTEGER_ZERO;
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
        /**
         * 格式化值
         * @param value 值
         * @param parameters 参数 (参数1：起始位置，参数2：长度，参数3：填充字符串)
         * @return 格式化后的值
         */
        public String formatValue(Object value, String... parameters) {
            String stringValue = TextUtils.toString(value);
            if (stringValue == null || parameters.length == 0) {
                return stringValue;
            }
            int valueLen = stringValue.length();
            //
            String pad = parameters.length > INTEGER_TWO ? parameters[INTEGER_TWO] : EMPTY;
            int padLen = pad.length();
            // 获取偏移量
            boolean noneOffset = EMPTY.equals(parameters[INTEGER_ZERO]);
            boolean existsLength = parameters.length > INTEGER_ONE && !EMPTY.equals(parameters[INTEGER_ONE]);
            // 计算结束位置
            int end = existsLength ? Integer.parseInt(parameters[INTEGER_ONE]) : valueLen;
            boolean backward = end < INTEGER_ZERO;
            int offset;
            if (noneOffset) {
                // 获取偏移量
                offset = backward && valueLen > INTEGER_ZERO ?  valueLen - INTEGER_ONE : INTEGER_ZERO;
            } else {
                // 获取偏移量
                if ((offset = Integer.parseInt(parameters[INTEGER_ZERO])) < INTEGER_ZERO) {
                    // 校正结束位置
                    offset = valueLen + offset;
                }
            }
            // 计算结束位置
            if (existsLength) {
                end += offset;
            }
            if (backward) {
                // wrap value
                int t = end; end = offset + INTEGER_ONE; offset = t + INTEGER_ONE;
            }
            if (padLen == INTEGER_ZERO) {
                // 没有填充字符串
                if (end >= INTEGER_ZERO && offset <= valueLen) {
                    // 返回子字符串
                    return offset == end ? EMPTY : stringValue.substring(Math.max(offset, INTEGER_ZERO), Math.min(end, valueLen));
                }
                // 超出字符串范围
                return null;
            }
            if (offset >= INTEGER_ZERO && end <= valueLen) {
                // 返回子字符串
                return noneOffset ? stringValue : stringValue.substring(offset, end);
            }
            if (offset == end)  {
                return EMPTY;
            }
            return padString(stringValue, valueLen, pad, padLen, offset, end, noneOffset, backward);
        }

        private String padString(String value, int valueLen, String pad, int padLen, int offset, int end, boolean noneOffset, boolean backward) {
            StringBuilder buf = new StringBuilder();
            if (offset < INTEGER_ZERO) {
                // 左侧填充
                int padEnd = Math.min(end, INTEGER_ZERO);
                int i;
                if (backward && (i = (padEnd - offset) % padLen) != INTEGER_ZERO) {
                    buf.append(pad, padLen - i, padLen);
                    offset += i;
                }
                for (i = offset + padLen; i <= padEnd; i += padLen) {
                    buf.append(pad);
                }
                if (!backward && i < padEnd + padLen) {
                    buf.append(pad, INTEGER_ZERO, padEnd + padLen - i);
                }
                offset = INTEGER_ZERO;
            }
            if (valueLen > INTEGER_ZERO) {
                if (noneOffset) {
                    buf.append(value);
                    offset = valueLen;
                } else if (offset < valueLen && end > offset) {
                    // 追加子字符串
                    buf.append(value, offset, Math.min(end, valueLen));
                    offset = valueLen;
                }
            }
            if (end > offset) {
                // 右侧填充
                int i;
                if (backward && (i = (end - offset) % padLen) != INTEGER_ZERO) {
                    buf.append(pad, padLen - i, padLen);
                    end -= i;
                }
                for (i = offset + padLen; i <= end; i += padLen) {
                    buf.append(pad);
                }
                if (!backward && i < end + padLen) {
                    buf.append(pad, INTEGER_ZERO, end + padLen - i);
                }
            }
            return buf.toString();
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
