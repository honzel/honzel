package com.honzel.core.stratery;

import java.lang.annotation.*;

/**
 * 该注解添加在声明格式类似为: 方法名(P, R, int)里面
 * 其中P, R为业务处理链的实际入参类型及处理结果类型, 最后的int 为处理链类型chainType
 * 参数数量可以少于3个，但接受的参数必须从P开始并按上面一致的顺序声明方法
 * 对于指定为<code>ProcessType.CHECK</code>的方法，如果返回值为boolean类型并且返回false时会跳过该链后续的所有方法并返回
 * @see AbstractBusinessChain
 *
 * @author honzel
 * @date 2021/1/18
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BusinessProcessor {


    /**
     * 指定目标处理链类
     */
    Class<? extends AbstractBusinessChain>[] processFor() default AbstractBusinessChain.class;
    /**
     * 指定目标处理类型，默认为校验
     */
    ProcessType processType() default ProcessType.CHECK;
    /**
     * 指定目标链类型
     */
    int[] chainType() default ChainConstants.CHAIN_TYPE_DEFAULT;

    /**
     * 是否掩码低位值
     */
    boolean maskLow() default false;

}
