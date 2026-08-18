package com.honzel.core.stratery;

/**
 * 业务链常量
 * @author honzel
 * date 2021/12/21
 */
public interface ChainConstants {

    /**
     * 默认链类型
     */
    int CHAIN_TYPE_DEFAULT = 0;
    /**
     * 掩码位数
     */
    int CHAIN_MASK_BITS = 15;
    /**
     * 低位掩码
     */
    int CHAIN_MASK_LOW = ~(-1 << CHAIN_MASK_BITS);

    /**
     * 低位类型标识
     */
    int MASK_LOW_FLAG = 1 << (CHAIN_MASK_BITS << 1);

    /**
     * 属性链类型
     */
    String ATTRIBUTE_CHAIN_TYPE = "chainType";
    /**
     * 属性处理链
     */
    String ATTRIBUTE_PROCESS_FOR = "processFor";
    /**
     * 属性处理类型
     */
    String ATTRIBUTE_PROCESS_TYPE = "processType";
    /**
     * 属性Future时是否等待结果
     */
    String ATTRIBUTE_FUTURE_WAIT = "futureWait";
    /**
     * 属性是否为可选方法
     */
    String ATTRIBUTE_PROCESS_OPTIONAL = "optional";
}
