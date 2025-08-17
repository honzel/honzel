package com.honzel.core.stratery;


import com.honzel.core.util.lambda.LambdaUtils;

/**
 * 业务链保存上下文
 * @author honzel
 * date 2022/1/4
 */
public interface ChainSaveContext {


    /**
     * 事务中执行
     *
     * @param param       第一个参数
     * @param secondParam 第二个参数
     * @param thirdParam 第三个参数
     * @param consumer    消费者
     * @param <P> 第一个参数
     * @param <Q> 第二个参数
     * @param <R> 第三个参数
     */
    <P, Q, R> void executeInTransaction(P param, Q secondParam, R thirdParam, LambdaUtils.TiConsumer<P, Q, R> consumer);

}
