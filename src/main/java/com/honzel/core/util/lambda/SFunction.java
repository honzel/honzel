package com.honzel.core.util.lambda;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * 可序列化的Function接口
 * @param <T> 参数类型
 * @param <R> 返回类型
 * @author honzel
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {

    /**
     * 获取Iterable的第一个元素
     * @return 返回Iterable的第一个元素
     */
    default SFunction<Iterable<T>, T> first() {
        return t -> {
            if (t instanceof Queue) {
                return ((Queue<T>) t).isEmpty() ? null : ((Queue<T>) t).peek();
            }
            if (t instanceof List<?>) {
                return ((List<T>) t).isEmpty() ? null : ((List<T>) t).get(0);
            }
            Iterator<T> iterator;
            return t != null && (iterator = t.iterator()).hasNext() ? iterator.next() : null;
        };
    }
    /**
     * 获取Iterable的最后一个元素
     * @return 获取Iterable的最后一个元素
     */
    default SFunction<List<T>, T> last() {
        return t -> {
            if (t instanceof Stack || t instanceof Deque) {
                return t.isEmpty() ? null : ((Deque<T>) t).peek();
            }
            if (t instanceof List<?>) {
                int size = ((List<T>) t).size();
                return ((List<T>) t).isEmpty() ? null : ((List<T>) t).get(size - 1);
            }
            Iterator<T> iterator = t.iterator();
            return iterator.hasNext() ? iterator.next() : null;
        };
    }
}
