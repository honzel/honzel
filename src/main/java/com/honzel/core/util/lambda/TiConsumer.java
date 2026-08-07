package com.honzel.core.util.lambda;

/**
 * 三参数消费者接口
 * @param <T> 参数1类型
 * @param <U> 参数2类型
 * @param <P> 参数3类型
 * @author honzel
 */
@FunctionalInterface
public interface TiConsumer<T, U, P> {
    void accept(T t, U u, P p);
}
