package com.honzel.core.stratery;

import com.honzel.core.util.bean.BeanHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.honzel.core.constant.ArrayConstants.*;
import static com.honzel.core.stratery.ChainConstants.*;

/**
 * 业务链工具类
 * @author honzel
 * date 2021/1/22
 */
@SuppressWarnings({"unused", "unchecked", "UnusedReturnValue", "WeakerAccess"})
public class ChainProcessUtils {

    private static final Logger log = LoggerFactory.getLogger(ChainProcessUtils.class);

    /**
     * 获取底位
     * @param chainType 链类型
     * @return 掩码类型
     */
    public static int getMaskLow(int chainType) {
        int low = chainType & CHAIN_MASK_LOW;
        return low == chainType ? CHAIN_TYPE_DEFAULT : low;
    }
    /**
     * 获取底位
     * @param chainType 链类型
     * @return 掩码类型
     */
    public static int getMaskHigh(int chainType) {
        int high = chainType >>> CHAIN_MASK_BITS;
        return high == CHAIN_TYPE_DEFAULT ? chainType : (high & CHAIN_MASK_LOW);
    }

    /**
     * 获取mask组合类型
     * @param high 高位
     * @param low 低位
     * @return mask组合类型
     */
    public static int getMaskChainType(int high, int low) {
        if (high != CHAIN_TYPE_DEFAULT) {
            if (low == CHAIN_TYPE_DEFAULT) {
                return high;
            }
            return (high & CHAIN_MASK_LOW) << CHAIN_MASK_BITS | (low & CHAIN_MASK_LOW);
        }
        if (low == CHAIN_TYPE_DEFAULT) {
            return CHAIN_TYPE_DEFAULT;
        }
        return MASK_LOW_FLAG | (low & CHAIN_MASK_LOW);
    }


    private static final Map<Class<?>, List<Method>> processMethodCache = new WeakHashMap<>();
    private static final Map<AnnotatedElement, Annotation[]> declaredProcessorAnnotationCache = new WeakHashMap<>();
    private static final Map<ProcessorAnnotationKey, BusinessProcessor> processorMethodAnnotationCache = new WeakHashMap<>();
    private static final Map<Class<?>, Map<String, String>> processorAttributesAliasCache = new WeakHashMap<>();
    private static final Map<Class<?>, ProcessorAttributesMapper> processorAttributesMapperCache = new WeakHashMap<>();
    /**
     * Cache key for the AnnotatedElement cache.
     */
    private static final class ProcessorAnnotationKey {

        private final AnnotatedElement element;


        private final Class<? extends Annotation> annotationType;

        ProcessorAnnotationKey(AnnotatedElement element, Class<? extends Annotation> annotationType) {
            this.element = element;
            this.annotationType = annotationType;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ProcessorAnnotationKey)) {
                return false;
            }
            ProcessorAnnotationKey otherKey = (ProcessorAnnotationKey) other;
            return (this.element.equals(otherKey.element) && this.annotationType.equals(otherKey.annotationType));
        }

        @Override
        public int hashCode() {
            return (this.element.hashCode() * 29 + this.annotationType.hashCode());
        }

        @Override
        public String toString() {
            return "@" + this.annotationType + " on " + this.element;
        }

    }
    /**
     * 获取处理方法
     *
     * @param processorClass 处理器类型
     * @return 处理方法
     */
    static synchronized List<Method> getProcessMethodList(Class<?> processorClass) {
        List<Method> methods = processMethodCache.get(processorClass);
        if (methods == null) {
            methods = new ArrayList<>();
            Set<Class<?>> foundTypes = new HashSet<>();
            Class<?> targetCls = processorClass;
            // 获取本类Annotation方法
            addAnnotationMethods(methods, targetCls, foundTypes, false);
            // 获取所有父类
            while (targetCls != null && !Object.class.equals(targetCls)) {
                // 获取父类Annotation方法
                targetCls = addAnnotationMethods(methods, targetCls, foundTypes, true);
            }
            processMethodCache.put(processorClass, methods);
        }
        return methods;
    }

    static synchronized BusinessProcessor getProcessorAnnonation(Method sourceMethod, Class<?> chainCls) {
        // 获取声明处理器Annotation
        BusinessProcessor annotation = sourceMethod.getDeclaredAnnotation(BusinessProcessor.class);
        if (annotation != null && ArrayUtils.contains(annotation.processFor(), chainCls)) {
            // 直接匹配到处理器Annotation
            return annotation;
        }
        for (Annotation nestAnnotation : getDeclaredProcessorAnnotations(sourceMethod)) {
            if (annotation != null && annotation.equals(nestAnnotation)) {
                continue;
            }
            ProcessorAnnotationKey mappingKey = new ProcessorAnnotationKey(sourceMethod, nestAnnotation.annotationType());
            BusinessProcessor processor = processorMethodAnnotationCache.get(mappingKey);
            if (processor == null) {
                // 获取属性信息
                AnnotationAttributes annotationAttributes = mergeAnnotationAttributes(AnnotationUtils.getAnnotationAttributes(sourceMethod, nestAnnotation), sourceMethod, nestAnnotation, false);
                // 合成代理Annotation
                processor = AnnotationUtils.synthesizeAnnotation(annotationAttributes, BusinessProcessor.class, sourceMethod);
                // 放入缓存
                processorMethodAnnotationCache.put(mappingKey, processor);
            }
            if (ArrayUtils.contains(processor.processFor(), chainCls)) {
                // 匹配到链类型
                return processor;
            }
        }
        return null;
    }

    private  static Annotation[] getDeclaredProcessorAnnotations(AnnotatedElement annotatedElement) {
        Annotation[] annotations = declaredProcessorAnnotationCache.get(annotatedElement);
        if (annotations == null) {
            List<Annotation> annotationList = null;
            Annotation[] declaredAnnotations = annotatedElement.getDeclaredAnnotations();
            for (Annotation annotation : declaredAnnotations) {
                if (AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
                    continue;
                }
                Class<? extends Annotation> nestAnnotationType = annotation.annotationType();
                // 判断是否属于@BusinessProcessor注解
                if (BusinessProcessor.class.equals(nestAnnotationType) || AnnotationUtils.isAnnotationMetaPresent(nestAnnotationType, BusinessProcessor.class)) {
                    (annotationList == null ? (annotationList = new ArrayList<>(declaredAnnotations.length)) : annotationList).add(annotation);
                }
            }
            // 转成数组
            annotations = (annotationList == null) ? EMPTY_ANNOTATION_ARRAY : annotationList.toArray(EMPTY_ANNOTATION_ARRAY);
            // 放入临时缓存
            declaredProcessorAnnotationCache.putIfAbsent(annotatedElement, annotations);
        }
        return annotations;
    }


    private  static AnnotationAttributes mergeAnnotationAttributes(AnnotationAttributes annotationAttributes, Method sourceMethod, Annotation annotation, boolean metaAnnotation) {
        // 获取属性映射
        Class<? extends Annotation> annotationType = annotation.annotationType();
        // 合并别名属性
        mergeAliasAttributes(annotationAttributes, annotationType);
        // 合并映射属性
        mergeMappingAttributes(annotationAttributes, sourceMethod, annotation, annotationType, metaAnnotation);
        // 解析上级元注解
        for (Annotation nestAnnotation : getDeclaredProcessorAnnotations(annotationType)) {
            // 判断是否属于@BusinessProcessor注解
            if (BusinessProcessor.class.equals(nestAnnotation.annotationType())) {
                // 如果是注解本身
                // 补充未获取的属性
                AnnotationUtils.getAnnotationAttributes(sourceMethod, nestAnnotation).forEach(annotationAttributes::putIfAbsent);
            } else {
                // 合并元数据注解属性
                mergeAnnotationAttributes(annotationAttributes, sourceMethod, nestAnnotation, true);
            }
            // 只获取第一个匹配
            break;
        }
        return annotationAttributes;
    }


    private static void mergeMappingAttributes(AnnotationAttributes annotationAttributes, Method sourceMethod, Annotation annotation, Class<? extends Annotation> annotationType, boolean metaAnnotation) {
        // 获取属性映射
        ProcessorAttributesMapping mapping = annotationType.getDeclaredAnnotation(ProcessorAttributesMapping.class);
        if (mapping == null) {
            // 如果没映射注解
            if (metaAnnotation) {
                // 如果为元数据注解, 添加属性
                // 补充未获取的属性
                AnnotationUtils.getAnnotationAttributes(sourceMethod, annotation).forEach(annotationAttributes::putIfAbsent);
            }
            return;
        }
        // 属性映射类
        Class<? extends ProcessorAttributesMapper> mapperType = mapping.value();
        // 获取属性映射器
        ProcessorAttributesMapper mapper = processorAttributesMapperCache.get(mapperType);
        if (mapper == null) {
            mapper = newInstance(mapperType);
            processorAttributesMapperCache.put(mapperType, mapper);
        }
        // 属性名
        Set<String> overidedAttributeNames = Collections.emptySet();
        Annotation forAnnotation;
        if (metaAnnotation) {
            // 合并属性
            // 原属性
            boolean allOriginValueUsed = true;
            AnnotationAttributes oriAnnotationAttributes = AnnotationUtils.getAnnotationAttributes(sourceMethod, annotation);
            for (Map.Entry<String, Object> entry : oriAnnotationAttributes.entrySet()) {
                Object value = entry.getValue();
                Object oldValue = annotationAttributes.putIfAbsent(entry.getKey(), value);
                if (oldValue == null) {
                    // 添加设置成功的属性
                    (overidedAttributeNames.isEmpty() ? (overidedAttributeNames = new HashSet<>(oriAnnotationAttributes.size() + 1)) : overidedAttributeNames).add(entry.getKey());

                } else if (allOriginValueUsed && !Objects.deepEquals(oldValue, value)) {
                    // 是否数组并相等
                    allOriginValueUsed = false;
                }
            }
            if (allOriginValueUsed) {
                // 所有属性都新时用原注解时
                forAnnotation = annotation;
            } else {
                // 如果是元注解时，合成属性相关注解
                forAnnotation = AnnotationUtils.synthesizeAnnotation(annotationAttributes, annotationType, sourceMethod);
            }
        } else {
            //
            forAnnotation = annotation;
        }
        // 获取定制属性
        Map<String, Object> attributes =  mapper.mappingAttributes(sourceMethod, forAnnotation);
        if (attributes != null && !attributes.isEmpty()) {
            if (metaAnnotation) {
                for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                    // 只覆盖新设置的属性
                    if (overidedAttributeNames.contains(entry.getKey())) {
                        // 如果是可覆盖的属性
                        annotationAttributes.put(entry.getKey(), entry.getValue());
                    } else {
                        // 不存在才添加的属性
                        annotationAttributes.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                }
            } else {
                attributes.forEach(annotationAttributes::put);
            }
        }
    }
    /**
     *
     */
    static<T> T newInstance(Class<? extends T> type) {
        if (Modifier.isAbstract(type.getModifiers())) {
            // 如果该类型为抽象类则创建代理类
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(type);
            enhancer.setCallback((MethodInterceptor) (target, method, args, methodProxy) -> {
                if (Modifier.isAbstract(method.getModifiers())) {
                    log.warn("调用了抽象类型的抽象方法[{}.{}], 忽略执行，返回默认值", type.getSimpleName(), method.getName());
                    return BeanHelper.convert(null, method.getReturnType());
                }
                return methodProxy.invokeSuper(target, args);
            });
            return (T) enhancer.create();
        }
        try {
            Constructor<? extends T> constructor = type.getDeclaredConstructor(EMPTY_CLASS_ARRAY);
            if (!(Modifier.isPublic(type.getModifiers()) && Modifier.isPublic(constructor.getModifiers()))) {
                // 设置为可访问
                constructor.setAccessible(true);
            }
            return constructor.newInstance(EMPTY_OBJECT_ARRAY);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void mergeAliasAttributes(AnnotationAttributes annotationAttributes, Class<? extends Annotation> annotationType) {
        // 获取别名列表
        Map<String, String> aliasNameMap = processorAttributesAliasCache.get(annotationType);
        if (aliasNameMap == null) {
            Method[] declaredMethods = annotationType.getDeclaredMethods();
            for (Method method : declaredMethods) {
                // 获取别名
                AliasFor aliasFor = method.getDeclaredAnnotation(AliasFor.class);
                // 其他注解的别名
                if (aliasFor != null && aliasFor.annotation() != Annotation.class && !aliasFor.annotation().equals(annotationType)) {
                    // 获取属性别名
                    String alias = aliasFor.attribute().isEmpty() ? aliasFor.value() : aliasFor.attribute();
                    // 如果别名有值并且原属性集合不存在该别名同名属性时加入
                    if (!alias.isEmpty() && !alias.equals(method.getName())) {
                        // 加入有效别名
                        (aliasNameMap == null ? (aliasNameMap = new HashMap<>(declaredMethods.length + 1)) : aliasNameMap).put(method.getName(), alias);
                    }
                }
            }
            processorAttributesAliasCache.put(annotationType, aliasNameMap == null ? Collections.emptyMap() : aliasNameMap);
        }
        // 有有效别名时设置别名属性
        if (aliasNameMap != null && !aliasNameMap.isEmpty()) {
            // 如果存在别名
            aliasNameMap.forEach((attr, alias) -> {
                if (!annotationAttributes.containsKey(alias)) {
                    // 原属性值
                    Object value = annotationAttributes.get(attr);
                    if (value != null) {
                        // 说置别明值
                        annotationAttributes.put(alias, value);
                    }
                }
            });
        }
    }

    private static Class<?> addAnnotationMethods(List<Method> annotatedMethods, Class<?> cls, Set<Class<?>> foundTypes, boolean superFound) {
        boolean firstFound = foundTypes.isEmpty();
        Class<?> targetCls = superFound ? cls.getSuperclass() : cls;
        if (foundTypes.contains(targetCls)) {
            return null;
        }
        foundTypes.add(targetCls);
        if (targetCls != null && !Object.class.equals(targetCls)) {
            for (Method method : targetCls.getDeclaredMethods()) {
                if (!method.isBridge() && AnnotatedElementUtils.hasAnnotation(method, BusinessProcessor.class)) {
                    annotatedMethods.add(method);
                }
            }
        }
        if (!firstFound || superFound) {
            for (Class<?> face : cls.getInterfaces()) {
                addAnnotationMethods(annotatedMethods, face, foundTypes, false);
            }
        }
        return superFound ? targetCls : null;
    }
}
