package com.honzel.core.util.web;

import com.honzel.core.util.text.TextUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 通用的返回结果
 * @author honzel
 * @param <C> 错误编码类型
 * @param <T> 数据类型
 * @param <THIS> 本对象子类型
 */
@SuppressWarnings("unchecked")
public interface ApiResponse<C, T, THIS extends ApiResponse<C, T, THIS>> extends Serializable {

    /**
     * 是否请求成功，实现返回是否正确返回期望的数据
     *
     * @return 是否请求成功
     */
    boolean isSuccess();

    /**
     * 错误编码
     *
     * @return 返回错误编码
     */
    C getCode();

    /**
     * 返回信息
     *
     * @return 返回信息
     */
    String getMsg();

    /**
     * 请求成功时返回的数据
     *
     * @return 返回数据
     */
    T getData();


    /**
     * 处理错误结果
     * @param consumer 消费者
     * @return 本对象
     */
    default THIS handleError(Consumer<? super THIS> consumer) {
        THIS r = (THIS) this;
        if (!isSuccess()) {
            consumer.accept(r);
        }
        return r;
    }
    /**
     * 处理成功结果
     * @param consumer 消费者
     * @return 本对象
     */
    default THIS handleSuccess(Consumer<T> consumer) {
        if (isSuccess()) {
            consumer.accept(getData());
        }
        return (THIS) this;
    }

    /**
     * 处理错误
     * @param errorHandler 错误处理函数
     * @return 默认结果
     */
    default T getOrHandleError(Function<? super THIS, T> errorHandler) {
        return isSuccess() ? getData() : errorHandler.apply((THIS) this);
    }
    /**
     * 处理错误
     * @param defaultValue 默认值
     * @return 数据结果
     */
    default T getOrDefault(T defaultValue) {
        return isSuccess() ? getData() : defaultValue;
    }
    /**
     * 获取结果或处理空值
     * @param emptyHandler 空值处理函数
     * @return 数据结果
     */
    default T getOrHandleEmpty(Function<? super THIS, T> emptyHandler) {
       return getOrHandleEmpty(null, emptyHandler);
    }

    /**
     * 映射非null结果
     * @param mapper 映射函数
     * @param <R> 返回结果
     * @return 映射结果
     */
    default<R> R mapData(Function<T, R> mapper) {
        return mapData(mapper, null);
    }

    /**
     * 映射非null结果
     * @param mapper 映射函数
     * @param <R> 返回结果
     * @return 映射结果
     */
    default <R> R mapData(Function<T, R> mapper, R defaultValue) {
        T data = getData();
        R result;
        return data != null && (result = mapper.apply(data)) != null ? result : defaultValue;
    }
    /**
     * 获取结果或处理空值
     * @param getter 获取函数
     * @param emptyHandler 空值处理函数
     * @param <R>  映射结果类型
     * @return 映射结果
     */
    default<R> R getOrHandleEmpty(Function<T, R> getter, Function<? super THIS, R> emptyHandler) {
        R data = getter != null ? mapData(getter) : (R) getData();
        boolean empty = TextUtils.isEmpty(data);
        if (!empty) {
            if (data instanceof Collection) {
                empty = ((Collection<?>) data).isEmpty();
            } else if (data instanceof Map) {
                empty = ((Map<?,?>) data).isEmpty();
            }
        }
        return empty ? emptyHandler.apply((THIS) this) : data;
    }
}
