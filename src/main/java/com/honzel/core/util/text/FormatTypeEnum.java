package com.honzel.core.util.text;

import com.honzel.core.constant.NumberConstants;
import com.honzel.core.util.bean.BeanHelper;
import com.honzel.core.util.time.LocalDateTimeUtils;
import com.honzel.core.util.web.WebUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.*;

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
            if (parameters.length > 0 && isNotEmpty(parameters[0])) {
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
        public void appendValue(StringBuilder formattedContent, String formattedValue, boolean nonForce) {
            if (nonForce && formattedContent.length() > 0 && preliminaryMatch(formattedValue)) {
                String objectStarts = BRACKET_START + BRACE_START;
                for (int i = formattedContent.length() - 1; i >= 0; i--) {
                    char ch = formattedContent.charAt(i);
                    if (objectStarts.indexOf(ch) != -1) {
                        continue;
                    }
                    if (ch != '"') {
                        formattedContent.append(formattedValue);
                        return;
                    }
                    break;
                }
            }
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
        public void appendValue(StringBuilder formattedContent, String formattedValue, boolean nonForce) {
            if (nonForce && preliminaryMatch(formattedValue)) {
                formattedContent.append(formattedValue);
                return;
            }
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
        public void appendValue(StringBuilder formattedContent, String formattedValue, boolean nonForce) {
            if (nonForce && formattedContent.length() > 0 && "=&?".indexOf(formattedContent.charAt(formattedContent.length() - 1)) == -1) {
                formattedContent.append(formattedValue);
            } else {
                formattedContent.append(WebUtils.encode(formattedValue));
            }
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
            if (parameters.length == 0) {
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
            if (parameters.length == 0 || EMPTY.equals(parameters[0])) {
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
    CALC("c") {
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
            if (parameters.length < 1) {
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
                            value = number.divide(otherNumber, new MathContext(20, RoundingMode.HALF_UP));
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
    /**
     * 摘要计算
     */
    DIGEST("digest") {
        private static final String ENCODED_BASE64 = "base64";
        private static final String ENCODED_HEX = "hex";
        /**
         * 格式化值
         * @param value 值
         * @param parameters 参数 (参数1:摘要算法 参数2:字符集 参数3:编码算法[hex/base64]),(参数1:编码算法[hex/base64] 参数2:字符集)
         * @return 格式化后的值
         */
        public String formatValue(Object value, String... parameters) {
            if (parameters.length == 0) {
                return TextUtils.toString(value);
            }
            // 获取进制
            String algorithm = parameters[0];
            String encodeAlgorithm = parameters.length > 2 ? parameters[2] : null;
            if (isEmpty(encodeAlgorithm)) {
                if (ENCODED_HEX.equalsIgnoreCase(algorithm) || ENCODED_BASE64.equalsIgnoreCase(algorithm)) {
                    encodeAlgorithm = algorithm;
                    algorithm = EMPTY;
                } else {
                    encodeAlgorithm = ENCODED_HEX;
                }
            }
            Charset charset = parameters.length > 1 && !EMPTY.equals(parameters[1]) ? Charset.forName(parameters[1]) : StandardCharsets.UTF_8;

            byte[] dataBytes;
            if (value instanceof byte[]) {
                dataBytes = (byte[]) value;
            } else if (value instanceof ByteBuffer) {
                dataBytes = ((ByteBuffer) value).array();
            } else if (value instanceof ByteArrayOutputStream) {
                dataBytes = ((ByteArrayOutputStream) value).toByteArray();
            } else {
                dataBytes = TextUtils.toString(value).getBytes(charset);
            }
            if (!algorithm.isEmpty()) {
                try {
                    dataBytes = MessageDigest.getInstance(algorithm).digest(dataBytes);
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            }
            if (ENCODED_BASE64.equalsIgnoreCase(encodeAlgorithm)) {
                // Base64
                return Base64.getEncoder().encodeToString(dataBytes);
            }
            // Hex
            return HexFormat.of().formatHex(dataBytes);
        }
    },
    /**
     * 时间格式化
     */
    TIME("time") {
        private static final String EPOCH_PATTERN = "@";
        private static final String EPOCH_SECONDS_PATTERN = "@s";
        private static final String EPOCH_MILLISECONDS_PATTERN = "@ms";
        private static final String EPOCH_DAYS_PATTERN = "@d";
        /**
         * 格式化值
         * @param value 值
         * @param parameters 参数 (参数1:时间转字符串格式 参数2:解析成时间格式),(参数1:时间转字符串格式)
         * @return 格式化后的值
         */
        @Override
        public String formatValue(Object value, String... parameters) {
            // 目标时间格式
            String toPattern = parameters.length > 0 ? parameters[0] : null;
            // 源时间格式
            String fromPattern = parameters.length > 1 ? parameters[1] : null;

            if (value instanceof TemporalAccessor) {
                // 时间格式化
                return formatTime((TemporalAccessor) value, toPattern);
            }
            LocalDateTime time;
            if (TextUtils.isNotEmpty(fromPattern) && value instanceof CharSequence) {
                // 字符串解析成时间
                time = parseTime(value.toString(), fromPattern);
            } else {
                // 其他类型转成时间
                time = BeanHelper.convert(value, LocalDateTime.class);
            }
            // 时间格式化
            return time == null ? null : formatTime(time, toPattern);
        }

        private LocalDateTime parseTime(String valueStr, String pattern) {
            if (StringUtils.isEmpty(pattern)) {
                pattern = EPOCH_PATTERN;
            }
            // 时间戳
            switch (pattern) {
                case EPOCH_PATTERN:
                    if (StringUtils.isNumeric(valueStr)) {
                        long timestamp = Long.parseLong(valueStr);
                        if (timestamp < Integer.MAX_VALUE) {
                            return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                        } else {
                            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                        }
                    } else {
                        return BeanHelper.convert(valueStr, LocalDateTime.class);
                    }
                case EPOCH_SECONDS_PATTERN:
                    // 时间戳(s)转时间
                    if (StringUtils.isNumeric(valueStr)) {
                        return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(valueStr)), ZoneId.systemDefault());
                    } else {
                        return BeanHelper.convert(valueStr, LocalDateTime.class);
                    }
                case EPOCH_MILLISECONDS_PATTERN:
                    // 时间戳(ms)转时间
                    if (StringUtils.isNumeric(valueStr)) {
                        return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(valueStr)), ZoneId.systemDefault());
                    } else {
                        return BeanHelper.convert(valueStr, LocalDateTime.class);
                    }
                case EPOCH_DAYS_PATTERN:
                    // 日期转时间
                    if (StringUtils.isNumeric(valueStr)) {
                        return LocalDate.ofEpochDay(Long.parseLong(valueStr)).atTime(LocalTime.MIN);
                    } else {
                        return BeanHelper.convert(valueStr, LocalDateTime.class);
                    }
                default:
                    // 字符串转时间
                    return LocalDateTimeUtils.parse(valueStr, LocalDateTimeUtils.getFormatter(pattern));
            }
        }

        private String formatTime(TemporalAccessor value, String pattern) {
            if (StringUtils.isEmpty(pattern)) {
                pattern = EPOCH_PATTERN;
            }
            Instant instant;
            switch (pattern) {
                case EPOCH_PATTERN:
                case EPOCH_SECONDS_PATTERN:
                    if (value.isSupported(ChronoField.INSTANT_SECONDS)) {
                        return String.valueOf(value.getLong(ChronoField.INSTANT_SECONDS));
                    }
                    if ((instant = BeanHelper.convert(value, Instant.class)) != null) {
                        return String.valueOf(instant.getEpochSecond());
                    } else {
                        return TextUtils.toString(value);
                    }
                case EPOCH_MILLISECONDS_PATTERN:
                    if (value.isSupported(ChronoField.INSTANT_SECONDS)) {
                        long seconds = value.getLong(ChronoField.INSTANT_SECONDS);
                        int nanos = value.isSupported(ChronoField.NANO_OF_SECOND) ? value.get(ChronoField.NANO_OF_SECOND) : 0;
                        return String.valueOf(toEpochMilli(seconds, nanos));
                    }
                    if ((instant = BeanHelper.convert(value, Instant.class)) != null) {
                        return String.valueOf(instant.toEpochMilli());
                    } else {
                        return TextUtils.toString(value);
                    }
                case EPOCH_DAYS_PATTERN:
                    if (value.isSupported(ChronoField.EPOCH_DAY)) {
                        return String.valueOf(value.getLong(ChronoField.EPOCH_DAY));
                    }
                    LocalDate localDate = BeanHelper.convert(value, LocalDate.class);
                    if (localDate != null) {
                        return String.valueOf(localDate.toEpochDay());
                    } else {
                        return TextUtils.toString(value);
                    }
            }
            return LocalDateTimeUtils.format(value, pattern);
        }
        private long toEpochMilli(long seconds, int nanos) {
            if (seconds < 0 && nanos > 0) {
                long millis = (seconds + 1) * 1000;
                long adjustment = nanos / 1000_000 - 1000;
                return Math.addExact(millis, adjustment);
            } else {
                long millis = seconds * 1000;
                return Math.addExact(millis, nanos / 1000_000);
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
