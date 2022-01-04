package com.honzel.core.stratery;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

/**
 * 映射mask属性
 * @author honzel
 * @date 2021/12/21
 */
class MaskProcessorAttributesMapper implements ProcessorAttributesMapper<MaskBusinessProcessor> {

    @Override
    public Map<String, Object> mappingAttributes(Method srcMethod, MaskBusinessProcessor srcAnnotation) {
        int[] high = srcAnnotation.high();
        int[] low = srcAnnotation.low();
        int[] chainTypes = new int[high.length * low.length];
        int top = 0;
        for (int h : high) {
            for (int l : low) {
                chainTypes[top ++] = ChainProcessUtils.getMaskChainType(h, l);
            }
        }
        return Collections.singletonMap(ChainConstants.ATTRIBUTE_CHAIN_TYPE, chainTypes);
    }
}
