package com.honzel.core.util.text;

import com.honzel.core.util.time.LocalDateTimeUtils;
import com.honzel.core.util.web.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
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
        @Override
        public boolean supportsAutoMatch() {
            return true;
        }
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
                        String hexString = Integer.toHexString(ch);
                        formattedContent.append("\\u").append("0000", hexString.length(), 4).append(hexString);
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
        @Override
        public boolean supportsAutoMatch() {
            return true;
        }
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
        @Override
        public boolean supportsAutoMatch() {
            return true;
        }
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
         * @param parameters 参数 (参数1:偏移量 参数2:长度 参数3:分隔符)
         * @return 格式化后的值
         */
        public String formatValue(Object value, String... parameters) {
            String stringValue = TextUtils.toString(value);
            if (stringValue == null || parameters.length == 0) {
                return stringValue;
            }
            int valueLen = stringValue.length();
            // 获取偏移量
            boolean existsLength = parameters.length > 1 && !EMPTY.equals(parameters[1]);
            // 计算结束位置
            int len = existsLength ? Integer.parseInt(parameters[1]) : valueLen;
            int offset;
            if (EMPTY.equals(parameters[0])) {
                // 获取偏移量
                offset = len < 0 && valueLen > 0 ?  -1 : 0;
            } else {
                // 获取偏移量
                if ((offset = Integer.parseInt(parameters[0])) < 0 && !existsLength) {
                    len -= offset;
                }
            }
            return getValues(stringValue, offset, len, parameters.length > 2 ? parameters[2] : null);
        }
    },
    /**
     * 字符填充
     */
    PAD("pad") {
        /**
         * 格式化值
         * @param value 值
         * @param parameters 参数 (参数1：长度，参数2：填充字符串)
         * @return 格式化后的值
         */
        public String formatValue(Object value, String... parameters) {
            String stringValue = TextUtils.toString(value);
            if (stringValue == null || parameters.length == 0 || EMPTY.equals(parameters[0])) {
                return stringValue;
            }
            // 计算结束位置
            int len = Integer.parseInt(parameters[0]);
            if (len == 0) {
                return stringValue;
            }
            int valueLen = stringValue.length();
            boolean backward;
            if (len < 0) {
                len = -len;
                backward = false;
            } else {
                backward = true;
            }
            int pads = len - valueLen;
            if (pads <= 0) {
                return stringValue;
            }
            // 填充字符串
            String padChar = parameters.length > 1 ? parameters[1] : EMPTY;
            if (EMPTY.equals(padChar)) {
                padChar = " ";
            }
            int padLen = padChar.length();
            if (pads <= padLen) {
                //  不超过填充字符长度
                return backward ? padChar.substring(0, pads).concat(stringValue) : stringValue.concat(padChar.substring(0, pads));
            }
            char[] padding = new char[pads];
            if (padLen == 1) {
                // 单字符填充
                Arrays.fill(padding, 0, pads, padChar.charAt(0));
            } else {
                // 多字符填充
                char[] padChars = padChar.toCharArray();
                int i = 0;
                while (pads > padLen) {
                    System.arraycopy(padChars, 0, padding, i, padLen);
                    pads -= padLen;
                    i += padLen;
                }
                System.arraycopy(padChars, 0, padding, i, pads);
            }
            return backward ? new String(padding).concat(stringValue) : stringValue.concat(new String(padding));
        }
    },
//    /**
//     * URL编码格式化类型
//     */
//    RADIX("radix") {
//        /**
//         * 格式化值
//         * @param value 值
//         * @param parameters 参数 (进制：长度)
//         * @return 格式化后的值
//         */
//        public String formatValue(Object value, String... parameters) {
//            if (isNotEmpty(value) && parameters.length > 0 && !EMPTY.equals(parameters[0])) {
//                // 获取进制
//                int toRadix = Integer.parseInt(parameters[0]);
//                if (toRadix < Character.MIN_RADIX || toRadix > Character.MAX_RADIX) {
//                    toRadix = 10;
//                }
//                int oriRadix = parameters.length > 1 && !EMPTY.equals(parameters[0]) ? Integer.parseInt(parameters[1]) : 10;
//                if (oriRadix < Character.MIN_RADIX || oriRadix > Character.MAX_RADIX) {
//                    oriRadix = 10;
//                }
//                Number number;
//                if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
//                    return Long.toString(((Number) value).longValue(), toRadix);
//                }
//                if (value instanceof BigInteger) {
//                    return ((BigInteger) value).toString(toRadix);
//                }
//                if (value instanceof BigDecimal) {
//                    value = ((BigDecimal) value).unscaledValue();
//                } else {
//                    value = value.toString();
//                }
//
//            }
//            return toString(value);
//        }
//    },
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
