package com.honzel.core.util.lambda;

import java.io.Serializable;

/**
 * 可序列化的三参数断言接口
 * @param <T> 参数1类型
 * @param <U> 参数2类型
 * @param <P> 参数3类型
 * @author honzel
 */
@FunctionalInterface
public interface STiPredicate<T, U, P> extends TiPredicate<T, U, P>, Serializable {
}
