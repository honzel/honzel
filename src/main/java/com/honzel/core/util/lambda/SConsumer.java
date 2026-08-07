package com.honzel.core.util.lambda;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * 可序列化的Consumer接口
 * @param <T> 参数类型
 * @author honzel
 */
@FunctionalInterface
public interface SConsumer<T> extends Consumer<T>, Serializable {
}
