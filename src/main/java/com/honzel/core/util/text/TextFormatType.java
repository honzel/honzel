package com.honzel.core.util.text;


/**
 * 文本格式化类型
 * date 2024/4/27
 */
public interface TextFormatType {

    String getTag();

    /**
     * 预匹配
     * @param format 格式内容
     * @return
     */
    default boolean preliminaryMatch(String format) {
        return false;
    }

    /**
     * 格式化值
     * @param value 占位值
     * @param parameters 参数
     * @return 返回格式化后的值
     */
    default String formatValue(Object value, String... parameters) {
        return TextUtils.toString(value);
    }

    /**
     * 添加格式化值
     * @param formattedContent 格式化后的内容
     * @param formattedValue 格式化后的值
     */
    default void appendValue(StringBuilder formattedContent, String formattedValue) {
        formattedContent.append(formattedValue);
    }


}
