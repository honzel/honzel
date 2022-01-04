package com.honzel.core.stratery;

import java.lang.annotation.*;

/**
 * 拓展业务属性处理
 * @author luhz
 * @date 2021/12/21
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProcessorAttributesMapping {

    Class<? extends ProcessorAttributesMapper> value();
}
