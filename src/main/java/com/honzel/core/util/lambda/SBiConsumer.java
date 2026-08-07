package com.honzel.core.util.lambda;

import java.io.Serializable;
import java.util.function.BiConsumer;

/**
 * 可序列化的BiConsumer接口
 * @param <T> 参数1类型
 * @param <U> 参数2类型
 * @author honzel
 */
@FunctionalInterface
public interface SBiConsumer<T, U> extends BiConsumer<T, U>, Serializable {
}
