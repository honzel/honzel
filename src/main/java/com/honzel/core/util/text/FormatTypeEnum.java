package com.honzel.core.util.text;

import com.honzel.core.constant.NumberConstants;
import com.honzel.core.util.time.LocalDateTimeUtils;
import com.honzel.core.util.web.WebUtils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static com.honzel.core.util.text.TextUtils.*;

/**
 *
 */
public enum FormatTypeEnum implements TextFormatType {
    SIMPLE(TextUtils.EMPTY) {
        public boolean preliminaryMatch(String content) {
            return isEmpty(content);
        }

        public String formatValue(Object value, String[] parameters) {
            if (isNotEmpty(value) && Objects.nonNull(parameters) && parameters.length > 0 && isNotEmpty(parameters[0])) {
                String pattern = parameters[0];
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
            }
            return TextUtils.toString(value);
        }
        @Override
        public void appendValue(StringBuilder formattedContent, String value) {
            formattedContent.append(value);
        }
    },
    JSON("json") {
        public boolean preliminaryMatch(String content) {
            return content.startsWith(BRACE_START) && content.endsWith(BRACE_END) || content.startsWith(BRACKET_START) && content.endsWith(BRACKET_END);
        }
        @Override
        public void appendValue(StringBuilder formattedContent, String value) {
            int lastIndex = -1;
            for (int i = 0, len = value.length(); i < len; i++) {
                char ch = value.charAt(i);
                if (ch >= ' ' && ch != '\"' && ch != '\\') {
                    continue;
                }
                if (lastIndex + 1 < i) {
                    formattedContent.append(value, lastIndex + 1, i);
                }
                lastIndex = i;
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
                        String s = Integer.toHexString(ch);
                        formattedContent.append("\\u");
                        for (int offset = s.length(); offset < 4; ++offset) {
                            formattedContent.append('0');
                        }
                        formattedContent.append(s);
                        break;
                }
            }
            if (lastIndex >= 0) {
                formattedContent.append(value, lastIndex + 1, value.length());
            } else {
                formattedContent.append(value);
            }
        }
    },
    XML("xml") {
        public boolean preliminaryMatch(String content) {
            return content.startsWith("<") && content.endsWith(">");
        }
        @Override
        public void appendValue(StringBuilder formattedContent, String value) {
            for (int i = 0, len = value.length(); i < len; i++) {
                char ch = value.charAt(i);
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
    URL_ENCODING("url") {
        public boolean preliminaryMatch(String content) {
            return content.lastIndexOf("://", NumberConstants.INTEGER_TEN) > NumberConstants.INTEGER_ZERO;
        }
        @Override
        public void appendValue(StringBuilder formattedContent, String value) {
            formattedContent.append(WebUtils.encode(value));
        }
    },
    ;

    private final String tag;

     FormatTypeEnum(String tag) {
        this.tag = tag;
    }

    @Override
    public String getTag() {
        return tag;
    }



}
