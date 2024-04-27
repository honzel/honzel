package com.honzel.core.stratery;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 属性映射器
 * @author honzel
 * @param <T> 注解参数类型
 * date 2021/12/21
 */
public interface ProcessorAttributesMapper<T extends Annotation> {
    /**
     * 属性映射
     * @param srcMethod 来源方法
     * @param srcAnnotation 来源注解
     * @return 需要映射的属性
     */
    Map<String, Object> mappingAttributes(Method srcMethod, T srcAnnotation);
}
