package com.honzel.core.util.text;

public interface TextFormatType {

    String getTag();


    default boolean preliminaryMatch(String content) {
        return false;
    }

    default String formatValue(Object value, String[] parameters) {
        return TextUtils.toString(value);
    }

    void appendValue(StringBuilder formattedContent, String value);


}
