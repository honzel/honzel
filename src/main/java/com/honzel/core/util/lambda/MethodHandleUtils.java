package com.honzel.core.util.lambda;

import com.honzel.core.util.bean.SimplePropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.*;

/**
 * This class consists exclusively of static methods that operate on or return method handles.
 * @author luhz@trendit.cn
 * date 2025/8/12
 */
public class MethodHandleUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePropertyUtilsBean.class);

    public static final MethodType METHOD_TYPE_SUPPLIER = MethodType.methodType(Supplier.class);

    public static final MethodType METHOD_TYPE_FUNCTION = MethodType.methodType(Function.class);

    public static final MethodType METHOD_TYPE_CONSUMER = MethodType.methodType(Consumer.class);

    public static final MethodType METHOD_TYPE_BI_FUNCTION = MethodType.methodType(BiFunction.class);

    public static final MethodType METHOD_TYPE_BI_CONSUMER = MethodType.methodType(BiConsumer.class);


    private static final BiFunction<Class<?>, MethodHandles.Lookup, MethodHandles.Lookup> LOOKUP_FUNCTION;
    private static final Function<Class<?>, MethodHandles.Lookup> LOW_VERSION_LOOKUP_FUNCTION;
    static {
        // init functions
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        LOW_VERSION_LOOKUP_FUNCTION = Objects.isNull(LOOKUP_FUNCTION = initCreateLookupFunction(lookup)) ? initJavaLowVersionCreateLookupFunction(lookup) : null;
    }

    /**
     * 获取java8兼容的全权限Lookup对象
     * @param lookup 初始默认Lookup对象
     * @return Lookup生成函数
     */
    @SuppressWarnings("unchecked")
    private static Function<Class<?>, MethodHandles.Lookup> initJavaLowVersionCreateLookupFunction(MethodHandles.Lookup lookup) {
        try {
            // 2. 获取构造方法的 MethodHandle（实际签名是 (Class,int)Lookup，但通过绑定固定权限值）
            Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
            MethodHandle handle = lookup.unreflectConstructor(constructor);
            // 1. 定义函数式接口类型 (Function<Class<?>, Lookup>)
            MethodType funcType = MethodType.methodType(MethodHandles.Lookup.class, Class.class);
            // 绑定全权限标志位（15）
            MethodHandle boundHandle = MethodHandles.insertArguments(handle, 1, Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC);
            // 3. 创建 CallSite（模拟方法引用 Lookup::new）
            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup,
                    "apply",
                    METHOD_TYPE_FUNCTION,
                    funcType.generic(),
                    boundHandle,
                    funcType
            );
            return (Function<Class<?>, MethodHandles.Lookup>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            LOGGER.error("Unsupported JVM version - no private lookup mechanism available", e);
        }
        return null;
    }

    /**
     * 获取java9+兼容的Lookup对象
     * @param lookup 默认Lookup对象
     * @return Lookup生成函数
     */
    @SuppressWarnings("unchecked")
    private static BiFunction<Class<?>, MethodHandles.Lookup, MethodHandles.Lookup> initCreateLookupFunction(MethodHandles.Lookup lookup) {
        try {
            // 1. 定义函数式接口类型（BiFunction<Class, Lookup, Lookup>）
            MethodType targetSignature = MethodType.methodType(MethodHandles.Lookup.class, Class.class, MethodHandles.Lookup.class);
            // 获取 MethodHandles.privateLookupIn 的 MethodHandle
            MethodHandle privateLookupInHandle = lookup.findStatic(MethodHandles.class, "privateLookupIn", targetSignature);
            // 3. 创建 CallSite
            CallSite callSite = LambdaMetafactory.metafactory(lookup, "apply", // BiFunction 的抽象方法名
                    METHOD_TYPE_BI_FUNCTION, // 工厂方法签名
                    targetSignature.generic(), // 泛型擦除后的 BiFunction.apply 签名 (Object,Object)Object
                    privateLookupInHandle, // 目标方法句柄
                    targetSignature // 实际方法签名 (Class,Lookup)Lookup
            );
            // 4. 获取 BiFunction 实例
            return  (BiFunction<Class<?>, MethodHandles.Lookup, MethodHandles.Lookup>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            LOGGER.info("JVM version(less than java9) - privateLookupIn static method is not exits.");
        }
        return null;
    }


    /**
     * This lookup method is the alternate implementation of the lookup method with a leading caller class argument which is non-caller-sensitive.
     * This method is only invoked by reflection and method handle
     * @param targetClass – the target class
     * @return a lookup object for the target class
     */
    public static MethodHandles.Lookup lookup(Class<?> targetClass) {
        return lookup(targetClass, null);
    }

    /**
     * Returns a lookup object on a target class to emulate all supported bytecode behaviors, including private access.
     * The returned lookup object can provide access to classes in modules and packages,
     * and members of those classes, outside the normal rules of Java access control,
     * instead conforming to the more permissive rules for modular deep reflection.
     * @param targetClass – the target class
     * @param caller – the caller lookup object
     * @return a lookup object for the target class, with private access
     */
    public static MethodHandles.Lookup lookup(Class<?> targetClass, MethodHandles.Lookup caller) {
        try {
            if (targetClass == null) {
                // default Object.class
                targetClass = Object.class;
            }
            if (LOOKUP_FUNCTION != null) {
                // java 9+
                return LOOKUP_FUNCTION.apply(targetClass, caller != null ? caller : MethodHandles.lookup());
            }
            if (LOW_VERSION_LOOKUP_FUNCTION != null) {
                // java 8
                return LOW_VERSION_LOOKUP_FUNCTION.apply(targetClass);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get private lookup", e);
        }
        return caller != null ? caller : MethodHandles.lookup();
    }
}
