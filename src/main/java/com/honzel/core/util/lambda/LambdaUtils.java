package com.honzel.core.util.lambda;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

/**
 * LambdaUtils
 * @author honzel
 * @date 2022/1/4
 */
public class LambdaUtils {

    private static final Map<Class, WeakReference<SerializedLambda>> FUNC_CACHE = new ConcurrentHashMap<>();
    private static final String WRITE_REPLACE_METHOD = "writeReplace";
    private static final String LAMBADA_BLOCK_METHOD_PREFIX = "lambda$";

    public static SerializedLambda resolveLambda(Serializable lambda) {
        return resolveLambda(lambda, true);
    }

    public static SerializedLambda resolveLambda(Serializable lambda, boolean onlyClassInfo) {
        Class<?> clazz = lambda.getClass();
        // 获取缓存数据
        WeakReference<SerializedLambda> ref = FUNC_CACHE.get(clazz);
        SerializedLambda serializedLambda = (ref != null ? ref.get() : null);
        if (serializedLambda != null && (onlyClassInfo || isStaticLambda(serializedLambda))) {
            // 如果只能获取类型信息时
            return serializedLambda;
        }
        // 如果缓存不存在或已gc回收, 重新获取序列化对象
        if (!clazz.isSynthetic()) {
            // 非合成类（lambda)
            throw new IllegalArgumentException("该方法仅能传入 lambda 表达式产生的合成类");
        }
        try {
            Method method = clazz.getDeclaredMethod(WRITE_REPLACE_METHOD);
            method.setAccessible(true);
            serializedLambda = (SerializedLambda) method.invoke(lambda);
            if (ref == null || ref.get() == null) {
                FUNC_CACHE.put(clazz, new WeakReference<>(serializedLambda));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("解析lambda对象失败: " + e.getMessage(), e);
        }
        return serializedLambda;
    }

    public static boolean isSpecifiedMethodLambda(SerializedLambda serializedLambda) {
        return serializedLambda != null && !serializedLambda.getImplMethodName().startsWith(LAMBADA_BLOCK_METHOD_PREFIX);
    }


    public static boolean isStaticLambda(SerializedLambda serializedLambda) {
        return serializedLambda != null && serializedLambda.getCapturedArgCount() <= 0;
    }




    /**
     * 无参数
     * @param <R>
     */
    @FunctionalInterface
    public interface SerializeSupplier<R> extends Supplier<R>, Serializable {}

    /**
     * 单参数
     * @param <T>
     * @param <R>
     */
    @FunctionalInterface
    public interface SerializeFunction<T, R> extends Function<T, R>, Serializable {}

    /**
     * 二参数
     * @param <T>
     * @param <U>
     * @param <R>
     */
    @FunctionalInterface
    public interface SerializeBiFunction<T, U, R> extends BiFunction<T, U, R>, Serializable {}

    /**
     * 单参数
     * @param <T>
     */
    @FunctionalInterface
    public interface SerializeConsumer<T> extends Consumer<T>, Serializable {}

    /**
     * 二参数
     * @param <T>
     * @param <R>
     */
    @FunctionalInterface
    public interface SerializeBiConsumer<T, R> extends BiConsumer<T, R>, Serializable {}

    /**
     * 三参数
     * @param <T>
     * @param <U>
     * @param <P>
     * @param <R>
     */
    @FunctionalInterface
    public interface SerializeTiFunction<T, U, P, R>  extends Serializable {
        R apply(T t, U u, P p);
    }
    /**
     * 三参数
     * @param <T>
     * @param <U>
     * @param <P>
     */
    @FunctionalInterface
    public interface SerializeTiConsumer<T, U, P>  extends Serializable {
        void accept(T t, U u, P p);
    }
    /**
     * 三参数
     * @param <T>
     * @param <U>
     * @param <P>
     */
    @FunctionalInterface
    public interface SerializeTiPredicate<T, U, P>  extends Serializable {
        boolean test(T t, U u, P p);
    }

}
