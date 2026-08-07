
package com.honzel.core.util.lambda;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * 可序列化的Supplier接口
 * @param <R> 返回类型
 * @author honzel
 */
@FunctionalInterface
public interface SSupplier<R> extends Supplier<R>, Serializable {
}
