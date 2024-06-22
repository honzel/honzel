package com.honzel.core.util.text;

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
            int start = 0;
            for (int i = 0, len = formattedValue.length(); i < len; i++) {
                char ch = formattedValue.charAt(i);
                if (ch >= ' ' && ch != '\"' && ch != '\\') {
                    continue;
                }
                if (start < i) {
                    formattedContent.append(formattedValue, start, i);
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
                        formattedContent.append("\\u").append(Integer.toHexString(ch));
                        break;
                }
                start = i + 1;
            }
            if (start > 0) {
                if (start != formattedValue.length()) {
                    formattedContent.append(formattedValue, start, formattedValue.length());
                }
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
            return Objects.nonNull(format) && !EMPTY.equals(format = format.trim()) && format.lastIndexOf("://", 20) > 0;
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
            // 获取偏移量
            boolean noneOffset = EMPTY.equals(parameters[0]);
            boolean existsLength = parameters.length > 1 && !EMPTY.equals(parameters[1]);
            // 计算结束位置
            int end = existsLength ? Integer.parseInt(parameters[1]) : valueLen;
            boolean backward = end < 0;
            int offset;
            if (noneOffset) {
                // 获取偏移量
                offset = backward && valueLen > 0 ?  valueLen - 1 : 0;
            } else {
                // 获取偏移量
                if ((offset = Integer.parseInt(parameters[0])) < 0) {
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
                int t = end; end = offset + 1; offset = t + 1;
            }
            //
            String pad = parameters.length > 2 ? parameters[2] : EMPTY;
            int padLen = pad.length();
            if (padLen == 0) {
                // 没有填充字符串
                if (end >= 0 && offset <= valueLen) {
                    // 返回子字符串
                    return offset == end ? EMPTY : stringValue.substring(Math.max(offset, 0), Math.min(end, valueLen));
                }
                // 超出字符串范围
                return null;
            }
            return padString(stringValue, valueLen, pad, padLen, offset, end, noneOffset, backward);
        }

        private String padString(String value, int valueLen, String pad, int padLen, int offset, int end, boolean noneOffset, boolean backward) {
            if (offset >= 0 && end <= valueLen) {
                // 返回子字符串
                return noneOffset ? value : value.substring(offset, end);
            }
            if (offset == end)  {
                return EMPTY;
            }
            StringBuilder buf = new StringBuilder();
            if (offset < 0) {
                // 左侧填充
                appendPad(buf, pad, padLen, Math.min(end, 0) - offset, backward);
                offset = 0;
            }
            if (valueLen > 0) {
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
                appendPad(buf, pad, padLen, end - offset, backward);
            }
            return buf.toString();
        }

        private void appendPad(StringBuilder buf, String pad, int padLen, int len, boolean backward) {
            int i;
            if (backward && padLen != 1 && (i = len % padLen) != 0) {
                buf.append(pad, padLen - i, padLen);
                len -= i;
            }
            for (i = padLen; i <= len; i += padLen) {
                buf.append(pad);
            }
            if (!backward && padLen != 1 && i < len + padLen) {
                buf.append(pad, 0, len + padLen - i);
            }
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
