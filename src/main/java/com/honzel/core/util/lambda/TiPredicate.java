package com.honzel.core.util.lambda;

/**
 * 三参数断言接口
 * @param <T> 参数1类型
 * @param <U> 参数2类型
 * @param <P> 参数3类型
 * @author honzel
 */
@FunctionalInterface
public interface TiPredicate<T, U, P> {
    boolean test(T t, U u, P p);
}
