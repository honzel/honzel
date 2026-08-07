package com.honzel.core.util.lambda;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * 可序列化的BiFunction接口
 * @param <T> 参数1类型
 * @param <U> 参数2类型
 * @param <R> 返回类型
 * @author honzel
 */
@FunctionalInterface
public interface SBiFunction<T, U, R> extends BiFunction<T, U, R>, Serializable {



}
