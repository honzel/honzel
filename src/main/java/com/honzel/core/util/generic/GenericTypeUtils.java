package com.honzel.core.util.generic;
import com.honzel.core.constant.ArrayConstants;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 泛型工具类
 */
public class GenericTypeUtils {

    /**
     * 获取泛型基类的实际类型参数数组
     *
     * @param subClass  子类的Class对象
     * @param baseClass 泛型基类的Class对象
     * @return 实际类型参数的Class数组，若无法解析则返回空数组
     */
    public static Class<?>[] getActualTypeArguments(Class<?> subClass, Class<?> baseClass) {
        if (baseClass == null || subClass == null) {
            return ArrayConstants.EMPTY_CLASS_ARRAY;
        }
        // 获取基类的泛型参数类型
        TypeVariable<?>[] typeParameters = baseClass.getTypeParameters();
        if (typeParameters.length == 0) {
            return ArrayConstants.EMPTY_CLASS_ARRAY;
        }
        // 创建一个类型映射表，用于记录类型参数的映射关系
        Map<TypeVariable<?>, Type> typeMap = new HashMap<>();
        // 从子类开始，逐级向上查找类型参数的映射关系
        Class<?> currentClass = subClass;
        while (!baseClass.equals(currentClass)) {
            // 获取当前类的泛型超类
            Type genericSuper = currentClass.getGenericSuperclass();
            // 判断泛型超类是否为参数化类型
            if (genericSuper instanceof ParameterizedType) {
                // 参数化类型，解析类型参数
                ParameterizedType parameterizedType = (ParameterizedType) genericSuper;
                // 获取参数化类型的实际类型
                Type[] actualTypes = parameterizedType.getActualTypeArguments();
                // 获取参数化类型的原始类型
                Class<?> rawType = erase(parameterizedType.getRawType());
                // 获取参数化类型的类型参数
                TypeVariable<?>[] typeVariables = baseClass.equals(rawType) ? typeParameters : rawType.getTypeParameters();
                // 解析并记录类型参数到typeMap中
                for (int i = 0; i < typeVariables.length; i++) {
                    // 获取类型参数的映射关系
                    Type type = actualTypes[i];
                    typeMap.put(typeVariables[i], type instanceof TypeVariable ? typeMap.getOrDefault(type, type) : type);
                }
                currentClass = rawType;
            } else {
                // 非参数化类型，直接获取父类
                if (Objects.isNull(currentClass = currentClass.getSuperclass())) {
                    // 如果找不到基类，则返回空数组
                    return ArrayConstants.EMPTY_CLASS_ARRAY;
                }
            }
        }
        Class<?>[] result = new Class<?>[typeParameters.length];
        for (int i = 0; i < typeParameters.length; i++) {
            Type resolvedType = typeMap.getOrDefault(typeParameters[i], typeParameters[i]);
            result[i] = erase(resolvedType);
        }
        return result;
    }

    // 将Type转换为Class，处理ParameterizedType的情况
    public static Class<?> erase(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return erase(((ParameterizedType) type).getRawType());
        }
        if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length > 0 ? erase(bounds[0]) : Object.class;
        }
        if (type instanceof WildcardType) {
            Type[] bounds  = ((WildcardType) type).getUpperBounds();
            return bounds.length > 0 ? erase(bounds[0]) : Object.class;
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType)type;
            return Array.newInstance(erase(gat.getGenericComponentType()), 0).getClass();
        }
        return Object.class;
    }

}