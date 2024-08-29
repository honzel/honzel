package com.honzel.core.util.text;

import com.honzel.core.constant.NumberConstants;
import com.honzel.core.util.time.LocalDateTimeUtils;
import com.honzel.core.util.web.WebUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
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
    /**
     * 数字运算
     */
    CALC("calc") {
        /**
         * 数字运算
         * 操作符:
         *  数学运算: +加法(v+p) -减法(v-p) *乘法(v*p) /除法(v/p) %取余(v%p) abs取绝对值 pow取n次方 point小数点左移n位
         *  位运算: <<左移n位  >>右移n位 &按位与  ^按位异或  |按位或  &~按位与取反(v &~p)
         * @param value 值
         * @param parameters 参数 (参数1:操作符 参数2:另一值 参数3:数字格式)
         * @return 格式化后的值
         */
        public String formatValue(Object value, String... parameters) {
            if (value == null || parameters.length < 1) {
                return TextUtils.toString(value);
            }
            // 计算符
            String op = parameters[0];
            // 另一值
            String otherValue = parameters.length > 1 ? parameters[1] : EMPTY;
            // 数字格式化
            String pattern = parameters.length > 2 ? parameters[2] : EMPTY;
            //
            if (value instanceof CharSequence && NumberUtils.isNumber((String)(value = value.toString()))) {
                // 字符串转换为数字
                value = new BigDecimal((String) value);
            }
            if (!EMPTY.equals(op) && value instanceof Number && (EMPTY.equals(otherValue) || NumberUtils.isNumber(otherValue))) {
                // 数字运算
                BigDecimal otherNumber = EMPTY.equals(otherValue) ? null : new BigDecimal(otherValue);
                BigDecimal number;
                if (value instanceof BigDecimal) {
                    number = (BigDecimal) value;
                } else if (value instanceof BigInteger) {
                    number = new BigDecimal((BigInteger) value);
                } else {
                    long longValue = ((Number) value).longValue();
                    double doubleValue = ((Number)value).doubleValue();
                    if (doubleValue > Long.MIN_VALUE && doubleValue < Long.MAX_VALUE) {
                        if (doubleValue < 0) {
                            number = doubleValue >= longValue ? BigDecimal.valueOf(longValue) : BigDecimal.valueOf(doubleValue);
                        } else {
                            number = doubleValue <= longValue ? BigDecimal.valueOf(longValue) : BigDecimal.valueOf(doubleValue);
                        }
                    } else {
                        number =  BigDecimal.valueOf(doubleValue);
                    }
                }
                switch (op) {
                    case "+":
                        value = Objects.nonNull(otherNumber) ? number.add(otherNumber) : number.plus();
                        break;
                    case "-":
                        value = Objects.nonNull(otherNumber) ? number.subtract(otherNumber) : number.negate();
                        break;
                    case "*":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.multiply(otherNumber);
                        }
                        break;
                    case "/":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.divide(otherNumber, 20, RoundingMode.HALF_UP);
                        }
                        break;
                    case "%":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.remainder(otherNumber);
                        }
                        break;
                    case "<<":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.toBigInteger().shiftLeft(otherNumber.intValue());
                        }
                        break;
                    case ">>":
                    case ">>>":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.toBigInteger().shiftRight(otherNumber.intValue());
                        }
                        break;
                    case "|":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.toBigInteger().or(otherNumber.toBigInteger());
                        }
                        break;
                    case "&":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.toBigInteger().and(otherNumber.toBigInteger());
                        }
                        break;
                    case "^":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.toBigInteger().xor(otherNumber.toBigInteger());
                        }
                        break;
                    case "~":
                        value = number.toBigInteger().not();
                        break;
                    case "&~":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.toBigInteger().andNot(otherNumber.toBigInteger());
                        }
                        break;
                    case "abs":
                        value = number.abs();
                        break;
                    case "pow":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.pow(otherNumber.intValue());
                        }
                        break;
                    case "point":
                        if (Objects.nonNull(otherNumber)) {
                            value = number.movePointLeft(otherNumber.intValue());
                        }
                        break;
                    default:
                        break;
                }
            }
            // 如果是数字，则格式化
            return StringUtils.isNotEmpty(pattern) && value instanceof Number ? new DecimalFormat(pattern).format(value) : TextUtils.toString(value);
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
