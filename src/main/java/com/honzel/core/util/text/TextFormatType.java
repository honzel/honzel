package com.honzel.core.util.text;


/**
 * 文本格式化类型
 * date 2024/4/27
 */
public interface TextFormatType {

    /**
     * 获取唯一标识
     * @return 唯一标识
     */
    String getUniqueId();

    /**
     * 是否支持内容自动匹配
     * @return 是否支持内容自动匹配
     */
    default boolean supportsAutoMatch() {
        return false;
    }
    /**
     * 预匹配(简易匹配)
     * @param format 格式内容
     * @return 是否匹配
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
