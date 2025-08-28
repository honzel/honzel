package com.honzel.core.util.lambda;

import com.honzel.core.util.bean.SimplePropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This class consists exclusively of static methods that operate on or return method handles.
 * @author luhz@trendit.cn
 * date 2025/8/12
 */
public class MethodHandleUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePropertyUtilsBean.class);

    private static final Function<AccessibleObject, Boolean> TRY_SET_ACCESSIBLE_FUNCTION;
    private static final BiFunction<Class<?>, MethodHandles.Lookup, MethodHandles.Lookup> LOOKUP_FUNCTION;
    private static final Constructor<MethodHandles.Lookup> LOW_VERSION_LOOKUP_CONSTRUCTOR;
    static {
        // init functions
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        LOW_VERSION_LOOKUP_CONSTRUCTOR = Objects.isNull(LOOKUP_FUNCTION = initCreateLookupFunction(lookup)) ? initJavaLowVersionCreateLookupFunction(lookup) : null;
        TRY_SET_ACCESSIBLE_FUNCTION = initTrySetAccessibleFunction();
    }

    /**
     * 获取java9+的trySetAccessible方法
     * @return 返回trySetAccessible的function
     */
    @SuppressWarnings("unchecked")
    private static Function<AccessibleObject, Boolean> initTrySetAccessibleFunction() {
        try {
            // 1. 获取 MethodHandles.Lookup 对象
            MethodHandles.Lookup lookup = lookup(AccessibleObject.class);
            // 获取 MethodHandles.trySetAccessible 的 MethodHandle
            MethodHandle handle = lookup.findVirtual(AccessibleObject.class, "trySetAccessible", MethodType.methodType(boolean.class));
            MethodType targetSignature = MethodType.methodType(Boolean.class, AccessibleObject.class);
            // 3. 创建 CallSite
            CallSite callSite = LambdaMetafactory.metafactory(lookup, "apply", // Function 的抽象方法名
                    LambdaUtils.METHOD_TYPE_FUNCTION, // 工厂方法签名
                    targetSignature.generic(), // 泛型擦除后的 Function.apply 签名 (Object)Object
                    handle, // 目标方法句柄
                    targetSignature // 实际方法签名 (AccessibleObject)boolean
            );
            // 4. 获取 Function 实例
            return  (Function<AccessibleObject, Boolean>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            LOGGER.info("JVM version(less than java9) - AccessibleObject#trySetAccessible method is not exits");
        }
        return null;
    }

    /**
     * 获取java8兼容的全权限Lookup对象
     * @param lookup 初始默认Lookup对象
     * @return Lookup生成函数
     */
    @SuppressWarnings("unchecked")
    private static Constructor<MethodHandles.Lookup> initJavaLowVersionCreateLookupFunction(MethodHandles.Lookup lookup) {
        try {
            // 1. 获取构造方法的 MethodHandle（实际签名是 (Class,int)Lookup，但通过绑定固定权限值）,只有java8有
            Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
            return constructor;
//            MethodHandle handle = lookup.findConstructor(MethodHandles.Lookup.class, MethodType.methodType(void.class, Class.class, int.class));
//            // 2.绑定全权限标志位（15）
////            MethodHandle boundHandle = MethodHandles.insertArguments(handle, 1, Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC);
//            // 3. 定义函数式接口类型 (Function<Class<?>, Lookup>)
//            MethodType funcType = MethodType.methodType(MethodHandles.Lookup.class, Class.class, Integer.class);
//            // 4. 创建 CallSite（模拟方法引用 Lookup::new）
//            CallSite callSite = LambdaMetafactory.metafactory(
//                    lookup,
//                    "apply",
//                    LambdaUtils.METHOD_TYPE_BI_FUNCTION,
//                    funcType.generic(),
//                    handle,
//                    funcType
//            );
//            return (BiFunction<Class<?>, Integer, MethodHandles.Lookup>) callSite.getTarget().invokeExact();
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
                    LambdaUtils.METHOD_TYPE_BI_FUNCTION, // 工厂方法签名
                    targetSignature.generic(), // 泛型擦除后的 BiFunction.apply 签名 (Object,Object)Object
                    privateLookupInHandle, // 目标方法句柄
                    targetSignature // 实际方法签名 (Class,Lookup)Lookup
            );
            // 4. 获取 BiFunction 实例
            return  (BiFunction<Class<?>, MethodHandles.Lookup, MethodHandles.Lookup>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            LOGGER.info("JVM version(less than java9) - privateLookupIn static method is not exits");
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
            if (LOW_VERSION_LOOKUP_CONSTRUCTOR != null) {
                // java 8
                return LOW_VERSION_LOOKUP_CONSTRUCTOR.newInstance(targetClass, Modifier.PRIVATE);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get private lookup", e);
        }
        return caller != null ? caller : MethodHandles.lookup();
    }

    /**
     * If this method is invoked by JNI code with no caller class on the stack, the accessible flag can only be set if the member and the declaring class are public, and the class is in a package that is exported unconditionally.
     * If there is a security manager, its checkPermission method is first called with a ReflectPermission("suppressAccessChecks") permission.
     * Returns:
     * true if the accessible flag is set to true; false if access cannot be enabled.
     * Throws:
     * SecurityException – if the request is denied by the security manager
     * @param accessible the accessible object
     * @return true if the accessible flag is set to true; false if access cannot be enabled.
     */
    public static boolean trySetAccessible(AccessibleObject accessible) {
        if (TRY_SET_ACCESSIBLE_FUNCTION != null) {
            return TRY_SET_ACCESSIBLE_FUNCTION.apply(accessible);
        }
        // java8
        if (!accessible.isAccessible()) {
            accessible.setAccessible(true);
        }
        return true;
    }
}
