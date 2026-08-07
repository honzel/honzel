package com.honzel.core.util.lambda;

/**
 * 三参数函数接口
 * @param <T> 参数1类型
 * @param <U> 参数2类型
 * @param <P> 参数3类型
 * @param <R> 返回类型
 * @author honzel
 */
@FunctionalInterface
public interface TiFunction<T, U, P, R> {
    R apply(T t, U u, P p);
}
