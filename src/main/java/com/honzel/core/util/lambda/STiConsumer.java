package com.honzel.core.util.lambda;

import java.io.Serializable;

/**
 * 可序列化的三参数消费者接口
 * @param <T> 参数1类型
 * @param <U> 参数2类型
 * @param <P> 参数3类型
 * @author honzel
 */
@FunctionalInterface
public interface STiConsumer<T, U, P> extends TiConsumer<T, U, P>, Serializable {
}
