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
    static<T> SFunction<Iterable<T>, T> first() {
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
    static<T> SFunction<Collection<T>, T> last() {
        return t -> {
            int size = t.size();
            if (size == 0) {
                return null;
            }
            if (t instanceof Deque) {
                return ((Deque<T>) t).peekLast();
            }
            if (t instanceof List<?>) {
                return ((List<T>) t).get(size - 1);
            }
            if (size == 1) {
                return t.iterator().next();
            }
            Iterator<T> iterator = t.iterator();
            T next = null;
            while (iterator.hasNext()) {
                next = iterator.next();
            }
            return next;
        };
    }
}
