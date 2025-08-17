package com.honzel.core.util.lambda;

import com.honzel.core.util.ConcurrentReferenceHashMap;
import com.honzel.core.vo.Entry;

import java.io.Serializable;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.function.*;

/**
 * LambdaUtils
 * @author honzel
 * date 2022/1/4
 */
public class LambdaUtils {

    public static final MethodType METHOD_TYPE_VOID = MethodType.methodType(void.class);
    public static final MethodType METHOD_TYPE_SUPPLIER = MethodType.methodType(Supplier.class);
    public static final MethodType METHOD_TYPE_FUNCTION = MethodType.methodType(Function.class);
    public static final MethodType METHOD_TYPE_CONSUMER = MethodType.methodType(Consumer.class);
    public static final MethodType METHOD_TYPE_BI_FUNCTION = MethodType.methodType(BiFunction.class);
    public static final MethodType METHOD_TYPE_BI_CONSUMER = MethodType.methodType(BiConsumer.class);
    public static final MethodType METHOD_TYPE_TI_FUNCTION = MethodType.methodType(SerializeTiFunction.class);
    public static final MethodType METHOD_TYPE_TI_CONSUMER = MethodType.methodType(SerializeTiConsumer.class);

    protected LambdaUtils() {
    }

    private static final Map<Class<?>, Entry<Method, WeakReference<SerializedLambda>>> FUNC_CACHE = new ConcurrentReferenceHashMap<>();
    private static final String WRITE_REPLACE_METHOD = "writeReplace";
    private static final String LAMBADA_BLOCK_METHOD_PREFIX = "lambda$";


    public static SerializedLambda resolveLambda(Serializable lambda, boolean ignoreCapturedArgs) {
        Class<?> clazz = lambda.getClass();
        // 获取缓存数据
        Entry<Method, WeakReference<SerializedLambda>> entry = FUNC_CACHE.computeIfAbsent(clazz,
                cls -> new Entry<>(createSerializedLambdaMethod(cls), null));
        SerializedLambda serializedLambda;
        boolean hasLambdaValue;
        if (Objects.nonNull(entry.getValue()) && Objects.nonNull(serializedLambda = entry.getValue().get())) {
            if (ignoreCapturedArgs || isStaticLambda(serializedLambda)) {
                // 如果只能获取类型信息时
                return serializedLambda;
            }
            hasLambdaValue = true;
        } else {
            hasLambdaValue = false;
        }
        // 如果缓存不存在或已gc回收, 重新获取序列化对象
        try {
            serializedLambda = (SerializedLambda) entry.getKey().invoke(lambda);
            if (!hasLambdaValue) {
                entry.setNewValue(new WeakReference<>(serializedLambda));
            }
            return serializedLambda;
        } catch (Exception e) {
            throw new IllegalArgumentException("解析lambda对象失败: " + e.getMessage(), e);
        }
    }
    public static SerializedLambda resolveLambda(Serializable lambda) {
        return resolveLambda(lambda, true);
    }


//    @SuppressWarnings("unchecked")
    private static Method createSerializedLambdaMethod(Class<?> lambdaClass) throws IllegalArgumentException {
        if (!lambdaClass.isSynthetic()) {
            // 非合成类（lambda)
            throw new IllegalArgumentException("该方法仅能传入 lambda 表达式产生的合成类");
        }
        try {
            Method method = lambdaClass.getDeclaredMethod(WRITE_REPLACE_METHOD);
            return method;
//            MethodHandles.Lookup lookup = MethodHandleUtils.lookup(lambdaClass);
//            // 获取writeReplace方法句柄
//            MethodHandle handle = lookup.findVirtual(lambdaClass, "writeReplace", MethodType.methodType(Object.class));
//            MethodType methodType = MethodType.methodType(SerializedLambda.class, lambdaClass);
//            // 使用LambdaMetafactory绑定到writeReplace方法
//            CallSite callSite = LambdaMetafactory.metafactory(lookup, "apply", METHOD_TYPE_FUNCTION, methodType.generic(), handle, methodType);
//            return (Function<Serializable, SerializedLambda>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new IllegalArgumentException("解析lambda生成SerializedLambda对象的方法失败: " + e.getMessage(), e);
        }
    }

    public static boolean isSpecifiedMethodLambda(SerializedLambda serializedLambda) {
        return serializedLambda != null && !serializedLambda.getImplMethodName().startsWith(LAMBADA_BLOCK_METHOD_PREFIX);
    }


    public static boolean isStaticLambda(SerializedLambda serializedLambda) {
        return serializedLambda != null && serializedLambda.getCapturedArgCount() <= 0;
    }




    /**
     * 无参数
     * @param <R> 返回类型
     */
    @FunctionalInterface
    public interface SerializeSupplier<R> extends Supplier<R>, Serializable {}

    /**
     * 单参数
     * @param <T> 参数类型
     * @param <R> 返回类型
     */
    @FunctionalInterface
    public interface SerializeFunction<T, R> extends Function<T, R>, Serializable {}

    /**
     * 二参数
     * @param <T> 参数1类型
     * @param <U> 参数2类型
     * @param <R> 返回类型
     */
    @FunctionalInterface
    public interface SerializeBiFunction<T, U, R> extends BiFunction<T, U, R>, Serializable {}

    /**
     * 单参数
     * @param <T> 参数类型
     */
    @FunctionalInterface
    public interface SerializeConsumer<T> extends Consumer<T>, Serializable {}

    /**
     * 二参数
     * @param <T> 参数1类型
     * @param <R> 返回类型
     */
    @FunctionalInterface
    public interface SerializeBiConsumer<T, R> extends BiConsumer<T, R>, Serializable {}

    /**
     * 三参数
     * @param <T> 参数1类型
     * @param <U> 参数2类型
     * @param <P> 参数3类型
     * @param <R> 返回类型
     */
    @FunctionalInterface
    public interface TiFunction<T, U, P, R> {
        R apply(T t, U u, P p);
    }
    /**
     * 三参数
     * @param <T> 参数1类型
     * @param <U> 参数2类型
     * @param <P> 参数3类型
     */
    @FunctionalInterface
    public interface TiPredicate<T, U, P>  {
        boolean test(T t, U u, P p);
    }


    /**
     * 三参数
     * @param <T> 参数1类型
     * @param <U> 参数2类型
     * @param <P> 参数3类型
     */
    @FunctionalInterface
    public interface TiConsumer<T, U, P> {
        void accept(T t, U u, P p);
    }
    /**
     * 三参数
     * @param <T> 参数1类型
     * @param <U> 参数2类型
     * @param <P> 参数3类型
     * @param <R> 返回类型
     */
    @FunctionalInterface
    public interface SerializeTiFunction<T, U, P, R>  extends TiFunction<T, U, P, R>, Serializable {}
    /**
     * 三参数
     * @param <T> 参数1类型
     * @param <U> 参数2类型
     * @param <P> 参数3类型
     */
    @FunctionalInterface
    public interface SerializeTiConsumer<T, U, P>  extends TiConsumer<T, U, P>, Serializable {}
    /**
     * 三参数
     * @param <T> 参数1类型
     * @param <U> 参数2类型
     * @param <P> 参数3类型
     */
    @FunctionalInterface
    public interface SerializeTiPredicate<T, U, P>  extends TiPredicate<T, U, P>, Serializable {}


}
