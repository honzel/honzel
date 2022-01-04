package com.honzel.core.stratery;

import com.honzel.core.constant.ArrayConstants;
import com.honzel.core.util.bean.BeanHelper;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 业务类型方法列表对象
 * @author honzel
 * 2021/6/12
 */
class ChainMethodList {
    /**
     * 返回值位
     */
    private static final int HASH_PARAM = 0;
    /**
     * 返回值位
     */
    private static final int HASH_RESULT = 1;
    /**
     * 返回值位
     */
    private static final int HASH_CHAIN_TYPE = 2;
    /**
     * 参数总位数
     */
    private static final int ARGS_LENGTH = 3;
    /**
     * 参数预初始化长度
     */
    private static final int PRE_ARGS_INIT_LENGTH = 3;
    /**
     * 所有参数标识
     */
    private static final int HASH_ALL_ARGS = (1 << ARGS_LENGTH) - 1;

    /**
     * 日志对象
     */
    private final Logger log;
    /**
     * 无效方法
     */
    private static final Method INVALID_METHOD = BeanHelper.findDeclaredMethod(Object.class, "toString", ArrayConstants.EMPTY_CLASS_ARRAY);
    /**
     * 空参数上下文对象
     */
    private static final ProcessResult EMPTY_CONTEXT = new ProcessResult(){};
    /**
     *
     */
    private static final int PROCESS_TYPE_LENGTH = ProcessType.values().length;
    /**
     * 处理器
     */
    private final Object[] processors;

    /**
     * 类型开始位置
     */
    private int[] offsets = new int[PROCESS_TYPE_LENGTH];
    /**
     * 处理器方法列表
     */
    private Method[][] processMethods = new Method[PROCESS_TYPE_LENGTH][];
    /**
     * 处理方法参数标识
     */
    private int[][] argumentHashArray = new int[PROCESS_TYPE_LENGTH][];

    /**
     * 最后解析位置是否默认
     */
    private boolean[] topDefaults = new boolean[PROCESS_TYPE_LENGTH];

    /**
     * 是否统一参数
     */
    private int argumentFlags;
    /**
     * 最新解析的位置
     */
    private int topIndex;

    /**resultClass
     * 参数对象上下文类型
     */
    private Class<?> resultClass;

    /** 临时结果类型
     * 参数对象上下文类型
     */
    private Class<?> tempResultClass;

    /**
     * 默认处理链类型
     */
    private ChainMethodList defaultMethodList;

    ChainMethodList(Object[] processors, Logger log) {
        this.processors = processors;
        this.log = log;
    }

    /**
     * 添加方法
     * @param index 第几个处理器
     * @param method 方法
     * @param allArgumentTypes 所有方法
     * @param processType 处理类型
     * @param isDefault 是否是默认链的方法
     */
    boolean addMethod(int index, Method method, Class<?>[] allArgumentTypes, ProcessType processType, boolean isDefault) {
        Class<?>[] actTypes = method.getParameterTypes();
        // 默认方法先校验参数是否匹配, 不匹配时直接忽略
        if (isDefault && hash(allArgumentTypes, actTypes, null) < 0) {
            // 默认链的类型不匹配
            return false;
        }
        // 校验上一个方法结果如果需要
        checkTopIndex(index, allArgumentTypes);
        // 方法类型
        final int typeIndex = processType.ordinal();
        // 如果为空时初始化
        Method[] methods = processMethods[typeIndex];
        int offset;
        if (methods == null) {
            // 方式数组初始化
            processMethods[typeIndex] = (methods = new Method[processors.length - index]);
            // 参数初始化
            argumentHashArray[typeIndex] = new int[methods.length];
            // 链方法起始位置
            offsets[typeIndex] = index;
            offset = 0;
        } else {
            offset = index - offsets[typeIndex];
        }
        if (methods[offset] != null) {
            boolean topDefault;
            if ((topDefault = topDefaults[typeIndex]) == isDefault) {
                if (log.isWarnEnabled()) {
                    log.warn("同一处理器下对应相同的链类型有重复的处理方法, 后面一个将被忽略掉: [{}]<==>[{}]", toShortName(methods[offset], actTypes), toShortName(method, actTypes));
                }
                return false;
            }
            if (!topDefault) {
                return false;
            }
        }
        // 设置是否默认标识
        topDefaults[typeIndex] = isDefault;

        if (!isDefault) {
            // 非默认链的方法解析参数和返回结果
            parseResultClass(typeIndex, offset, allArgumentTypes, actTypes, method);
        }
        // 设置为可访问
        method.setAccessible(true);
        // 添加方法
        methods[offset] = method;

        return true;
    }

    private void checkTopIndex(int parseIndex, Class<?>[] allArgumentTypes) {
        if (topIndex == parseIndex) {
            return;
        }
        if (topDefaults != null) {
            for (int i = 0; i < topDefaults.length; i++) {
                if (topDefaults[i]) {
                    // 解析结果类型
                    int offset = topIndex - offsets[i];
                    parseResultClass(i, offset, allArgumentTypes, processMethods[i][offset].getParameterTypes(), null);
                    // 去掉默认
                    topDefaults[i] = false;
                }
            }
        }
        topIndex = parseIndex;
    }

    private String toShortName(Method method, Class<?>[] parameterTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getSimpleName()).append('.');
        sb.append(method.getName());
        sb.append('(');
        int len = method.getParameterCount();
        if (len > 0) {
            if (parameterTypes == null) {
                parameterTypes = method.getParameterTypes();
            }
            sb.append(parameterTypes[0].getSimpleName());
            for (int i = 1; i < len; ++i) {
                sb.append(',').append(parameterTypes[i].getSimpleName());
            }
        }
        sb.append(')');
        return sb.toString();
    }

    private void parseResultClass(int typeIndex, int offset, Class<?>[] allArgumentTypes, Class<?>[] actTypes, Method throwableMethod) {
        // 解析参数位
        int result;
        if ((result = hash(allArgumentTypes, actTypes, throwableMethod)) < 0) {
            // 参数不匹配
            return;
        }
        if (result > 0) {
            // 参数位标记
            argumentHashArray[typeIndex][offset] = result;
            argumentFlags |= (1 << (result - 1));
        }
        if ((result & (1 << HASH_RESULT)) == 0) {
            // 如果没有结果参数
            return;
        }
        Class<?> resultType = throwableMethod != null ? resultClass : tempResultClass;
        if (resultType == null) {
            // 实际类型是之前类型的子类型时，使用子类型
            resultType = allArgumentTypes[HASH_RESULT];
        }
        // 解析参数类型中的结果类型位置
        int pos = HASH_RESULT;
        for (int i = 0; i < HASH_RESULT; ++i) {
            if ((result & (1 << i)) == 0) {
                --pos;
            }
        }
        if (resultType.equals(actTypes[pos])) {
            // 相等时不替换
            return;
        }
        // 如果是结果类型及类型不相等
        if (resultType.isAssignableFrom(actTypes[pos])) {
            // 实际类型是之前类型的子类型时，使用子类型
            if (throwableMethod != null) {
                resultClass = actTypes[pos];
            } else {
                tempResultClass = actTypes[pos];
            }
            return;
        }
        if (throwableMethod != null && !actTypes[pos].isAssignableFrom(resultType)) {
            // 参数的结果类型有冲突
            throw new RuntimeException(String.format("处理器方法[%s]的结果对象类型[%s]与该业务链或其他处理器的结果类型[%s]有冲突",
                    toShortName(throwableMethod, actTypes), actTypes[pos].getSimpleName(), resultType.getSimpleName()));
        }
    }


    private int hash(Class<?>[] allArgumentTypes, Class<?>[] actTypes, Method throwableMethod) {
        int argsNum;
        if ((argsNum = actTypes.length) > allArgumentTypes.length) {
            // 参数数量不匹配
            if (throwableMethod == null) {
                return -1;
            }
            throw new RuntimeException(String.format("处理器方法[%s]参数数量太多, 不能超过%s个", toShortName(throwableMethod, actTypes), allArgumentTypes.length));
        }
        if (argsNum == 0) {
            return 0;
        }
        // 校验参数类型及解析参数类型中的结果类型
        int pos = 0;
        int result = 0;
        for (int i = 0; i < allArgumentTypes.length; ++i) {
            if (actTypes[pos].isAssignableFrom(allArgumentTypes[i]) || allArgumentTypes[i].isAssignableFrom(actTypes[pos])) {
                if (++pos >= argsNum) {
                    // 已匹配完成
                    if (i < HASH_RESULT && matchResultType(allArgumentTypes[HASH_RESULT], actTypes[pos - 1])) {
                        // 如果是结果类型，则优先作为结果类型
                        result |= (1 << HASH_RESULT);
                    } else {
                        // 作为普通参数
                        result |= (1 << i);
                    }
                    break;
                }
                result |= (1 << i);
            } else if (i == HASH_RESULT && pos > 0 && matchResultType(allArgumentTypes[HASH_RESULT], actTypes[pos - 1])) {
                // 如果是结果类型，则优先作为结果类型
                result = ~(1 << (pos - 1)) & result | (1 << HASH_RESULT);
            }
        }
        if (pos < argsNum) {
            // 参数类型不匹配
            if (throwableMethod == null) {
                return -1;
            }
            throw new RuntimeException(String.format("处理器方法[%s]第%s个参数的类型[%s]与业务链要求的类型[%s]不匹配",
                    toShortName(throwableMethod, actTypes), pos + 1, actTypes[pos].getSimpleName(), allArgumentTypes[pos].getSimpleName()));
        }
        return result;
    }

    private boolean matchResultType(Class<?> argumentType, Class<?> actType) {
        return argumentType.isAssignableFrom(actType) || ProcessResult.class.isAssignableFrom(actType) && actType.isAssignableFrom(argumentType);
    }

    /**
     * 结束解析
     * @param allArgumentTypes 所有参数类型
     */
    void finish(Class<?>[] allArgumentTypes) {
        // 校验上一个方法
        checkTopIndex(topIndex + 1, allArgumentTypes);
        // 重新计算方法数组
	    boolean first = true;
        for (int i = processMethods.length - 1; i >= 0; --i) {
            if (processMethods[i] == null) {
                continue;
            }
            if (first) {
                if (i + 1 < processMethods.length) {
                    // 调整方法数组大小
                    processMethods = Arrays.copyOf(processMethods, i + 1);
                    offsets = Arrays.copyOf(offsets, i + 1);
                    argumentHashArray = Arrays.copyOf(argumentHashArray, i + 1);
                }
                first = false;
	        }
            // 只有check不需要distinct
            boolean distinct = i != ProcessType.CHECK.ordinal();
            // 调整链方法大小
            processMethods[i] = resizeMethods(processMethods[i], topIndex - offsets[i], distinct);
            // 是否长度有调整
            if (argumentHashArray[i].length != processMethods[i].length) {
                // 如果长度有调整
                argumentHashArray[i] = Arrays.copyOf(argumentHashArray[i], processMethods[i].length);
            }
        }
        topIndex --;
        // 返回结果类型
        if (resultClass == null) {
            // 设置默认结果类型
            resultClass = (tempResultClass != null ? tempResultClass : allArgumentTypes[HASH_RESULT]);
            //
        } else if (tempResultClass != null && !resultClass.equals(tempResultClass) && resultClass.isAssignableFrom(tempResultClass)) {
            // 如果临时结果类型为子类型时，使用子类型做为结果类型
            resultClass = tempResultClass;
        }
        // 不再用的数据置为空
        tempResultClass = null;
        topDefaults = null;
    }

    private Method[] resizeMethods(Method[] methods, int len, boolean distinct) {
        if (methods == null) {
            return null;
        }
        // 处理掉重复的保存方法，只取最后一个
        int newLength = Math.min(len, methods.length);
        // 获取是否第一次
        boolean first = true;
        // resize数组
        for (int i = newLength - 1; i > 0; --i) {
            Method method = methods[i];
            if (method == null || method == INVALID_METHOD) {
                if (first) {
                    --newLength;
                }
                continue;
            }
            if (distinct) {
                first = false;
                for (int j = i - 1; j >= 0; --j) {
                    if (method.equals(methods[j])) {
                        methods[j] = INVALID_METHOD;
                    }
                }
            } else {
                break;
            }
        }
        if (newLength < methods.length) {
            return Arrays.copyOf(methods, newLength);
        }
        return methods;
    }

    /**
     * 设置默认处理链方法
     * @param defaultMethodList 处理链方法列表
     */
    void setDefaultMethodList(ChainMethodList defaultMethodList) {
        if (defaultMethodList != this) {
            this.defaultMethodList = defaultMethodList;
        }
    }

    /**
     * 初始化参数数组
     * @param param 入参
     * @param processResult 处理上下文结果
     * @param chainType 业务链类型
     * @return 返回初始化数组
     */
    Object[][] initArgumentsArray(int totalArgumentFlags, Object param, Object processResult, int chainType) {
        // 如果为0代表不需要传参数
        if (totalArgumentFlags == 0) {
            // 没有初始时返回空初始化设置
            return null;
        }
        // 解析总参数长度
        int lengthAndHash = parseLengthAndHash(totalArgumentFlags);
        if (lengthAndHash > 0 && lengthAndHash <= HASH_ALL_ARGS) {
            // 仅包含长度
            int len = lengthAndHash;
            if ((totalArgumentFlags & (1 << (HASH_ALL_ARGS - 1))) == 0) {
                // 加上全参数
                len += 1;
            }
            // 初始化参数数组
            Object[][] result = new Object[len][];
            // 初始化最后一个
            result[result.length - 1] = new Object[]{param, processResult, chainType};
            return result;
        }
        // 获取长度
        int len = lengthAndHash & HASH_ALL_ARGS;
        // 初始化参数数组
        Object[][] result = new Object[len][];
        for (int i = 0; i < len; i++) {
            // 获取参数板式
            int hash = (lengthAndHash >>>= ARGS_LENGTH) & HASH_ALL_ARGS;
            if (hash == HASH_ALL_ARGS) {
                // 全参数
                result[i] = new Object[] {param, processResult, chainType};
                continue;
            }
            // 初始化参数
            Object[] args = new Object[parseLength(hash)];
            int pos = 0;
            for (int j = 0; j < ARGS_LENGTH; j ++) {
                if ((hash & (1 << j)) == 0) {
                    // 没有该参数
                    continue;
                }
                switch (j) {
                    case HASH_PARAM:
                        args[pos ++] = param;
                        break;
                    case HASH_RESULT:
                        args[pos ++] = processResult;
                        break;
                    case HASH_CHAIN_TYPE:
                        args[pos ++] = chainType;
                        break;
                }
                if (pos == args.length) {
                    // 已解析
                    break;
                }
            }
            result[i] = args;
        }
        return result;
    }


    private int parseLengthAndHash(int totalArgumentFlags) {
        int len = 0;
        int hashList = 0;
        int pos = 1;
        do {
            if ((totalArgumentFlags & 1) != 0 && (++len <= PRE_ARGS_INIT_LENGTH)) {
                // 预解析参数
                hashList |= (pos << (len * ARGS_LENGTH));
            }
        } while (++pos <= HASH_ALL_ARGS && (totalArgumentFlags >>>= 1) != 0);
        // 返回解析结果
        return len <= PRE_ARGS_INIT_LENGTH ? hashList | len : len;
    }

    private int parseLength(int flags) {
        int len = 0;
        do {
            if ((flags & 1) == 1) {
                len += 1;
            }
        } while ((flags >>>= 1) != 0);
        return len;
    }

    int getTotalArgumentFlags(ChainMethodList[] secondaries) {
        // 处理次要方法列表对象
        int result = argumentFlags;
        if (secondaries != null) {
            for (ChainMethodList secondary : secondaries) {
                result |= secondary.argumentFlags;
            }
        }
        if (defaultMethodList != null) {
            result |= defaultMethodList.argumentFlags;
        }
        return result;
    }

    /**
     * 执行检验方法
     * @param allArguments 参数
     * @param totalArgumentFlags 全参数标识
     * @param secondaries 次要方法列表
     * @return 返回校验是否成功
     */
    boolean doCheck(Object[][] allArguments, int totalArgumentFlags, ChainMethodList[] secondaries) {
        return doNonSave(allArguments, totalArgumentFlags, secondaries, ProcessType.CHECK);
    }
    /**
     * 执行保存后处理方法
     * @param allArguments 参数
     * @param totalArgumentFlags 全参数标识
     * @param secondaries 次要方法列表
     */
    void doAfter(Object[][] allArguments, int totalArgumentFlags, ChainMethodList[] secondaries) {
        doNonSave(allArguments, totalArgumentFlags, secondaries, ProcessType.AFTER);
    }


    private boolean doNonSave(Object[][] allArguments, int totalArgumentFlags, ChainMethodList[] secondaries, ProcessType processType) {
        // 处理次要方法列表对象
        int typeIndex = processType.ordinal();
        // 获取起始位置
        int offset = getOffset(secondaries, typeIndex);
        // 循环执行方法
        for (int i = offset; i < processors.length; i++) {
            // 获取检验方法
            ChainMethodList methodList = getMatchMethodList(i, secondaries, typeIndex);
            if (methodList == null) {
                // 没有可用调用方法
                continue;
            }
            // 获取index
            int index = (i - methodList.offsets[typeIndex]);
            // 执行检验方法
            Method method = methodList.processMethods[typeIndex][index];
            if (invokeProcessMethod(processors[i], method, allArguments, totalArgumentFlags, methodList.argumentHashArray[typeIndex][index])) {
                // 如果是继续时执行下一个方法
                continue;
            }
            // 中断校验方法的执行
            if (log.isDebugEnabled()) {
                log.debug("执行方法[{}]返回false结果, 直接跳过后续的所有处理器处理", toShortName(method, null));
            }
            return false;
        }
        return true;
    }

    /**
     * 执行保存方法
     * @param allArguments 参数
     * @param totalArgumentFlags 全参数标识
     * @param secondaries 次要方法列表
     */
    void doSave(Object[][] allArguments, int totalArgumentFlags, ChainMethodList[] secondaries) {
        // 处理次要方法列表对象
        int saveIndex = ProcessType.SAVE.ordinal();
        // 获取起始位置
        int offset = getOffset(secondaries, saveIndex);
        // 循环执行方法
        for (int i = offset; i < processors.length; i++) {
            // 获取检验方法
            ChainMethodList methodList = getMatchMethodList(i, secondaries, saveIndex);
            if (methodList != null) {
                // 获取index
                int index = (i - methodList.offsets[saveIndex]);
                // 获取保存方法
                Method saveMethod = methodList.processMethods[saveIndex][index];
                // 执行保存方法
                invokeProcessMethod(processors[i], saveMethod, allArguments, totalArgumentFlags, methodList.argumentHashArray[saveIndex][index]);
            }
        }
    }

    private boolean invokeProcessMethod(Object processor, Method processMethod, Object[][] allArguments, int totalArgumentFlags, int hash) {
        if (processMethod == null || processMethod == INVALID_METHOD) {
            // 返回是否继续
            return true;
        }
        if (log.isDebugEnabled()) {
            log.debug("执行方法[{}]", toShortName(processMethod, null));
        }
        try {
            Object result = processMethod.invoke(processor, getMethodActualArguments(processMethod, allArguments, totalArgumentFlags, hash));
            if (Boolean.TYPE.equals(processMethod.getReturnType())) {
                //校验时返回是否继续下一步的处理
                return (Boolean) result;
            }
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException) {
                // 如果是运行时异常则直接抛出
                throw (RuntimeException) e.getTargetException();
            } else {
                // 非运行时异常值包装成运行时异常抛出
                throw new RuntimeException(e.getTargetException());
            }
        } catch (Exception e) {
            // 其他异常
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * 获取方法实际参数
     * @param processMethod 方法
     * @param allArguments 所有参数
     * @param totalArgumentFlags 全参数标识
     * @param hash 方法参数
     * @return 返回方法的实际参数
     */
    private Object[] getMethodActualArguments(Method processMethod, Object[][] allArguments, int totalArgumentFlags, int hash) {
        // 参数数量
        int parameterCount = processMethod.getParameterCount();
        // 不需要参数
        if (allArguments == null || parameterCount == 0) {
            return ArrayConstants.EMPTY_OBJECT_ARRAY;
        }
        // 获取参数位置
        int index = getArgumentIndex(totalArgumentFlags, hash, allArguments.length);
        // 获取参数
        Object[] args = allArguments[index];
        if (args == null) {
            // 创建
            args = initActualArguments(allArguments[allArguments.length - 1], hash, parameterCount);
            // 放入缓存
            allArguments[index] = args;
        }
        return args;
    }

    private Object[] initActualArguments(Object[] maxArguments, int hash, int count) {
        // 获取参数标识
        // 初始化参数
        Object[] args = new Object[count];
        for (int i = maxArguments.length - 1; i >= 0 && count > 0; i--) {
            if ((hash & (1 << i)) != 0) {
                // 设置参数
                args[--count] = maxArguments[i];
            }
        }
        return args;
    }

    private int getArgumentIndex(int totalFlags, int hash, int len) {
        if (len == 1 || hash == 1) {
            return 0;
        }
	    int flag = 1 << --hash;
        // 只取小于的部分
        int flags = totalFlags & (flag - 1);
        if (flags == 0) {
            return 0;
        }
        if (len == 2) {
            return 1;
        }
        if (len == 3) {
            return (flag | flags) == totalFlags ? 2 : 1;
        }
        //
        for (int i = hash - 1; i >= 0; --i) {
            // 如果头位为空位(0)
            if ((flags & 1) == 0) {
                // 位置减1
                --hash;
            }
            if (i > 0 && (flags >>>= 1) == 0) {
                // 如果剩余全部为0，减去剩余数量
                hash -= i;
                break;
            }
        }
        return hash;
    }

    /**
     * 获取方法
     * @param index 处理器索引
     * @param secondaries 次要方法列表
     * @param typeIndex 操作类型索引
     * @return 返回保存方法
     */
    private ChainMethodList getMatchMethodList(int index, ChainMethodList[] secondaries, int typeIndex) {
        // 匹配主链方法
        if (matchMethod(index, this, typeIndex)) {
            // 返回主链对象
            return this;
        }
        if (secondaries != null) {
            for (ChainMethodList secondary : secondaries) {
                // 匹配辅链方法
                if (matchMethod(index, secondary, typeIndex)) {
                    return secondary;
                }
            }
        }
        // 匹配默认链方法
        return matchMethod(index, defaultMethodList, typeIndex) ? defaultMethodList : null;
    }


    private boolean matchMethod(int index, ChainMethodList methodList, int typeIndex) {
        Method[] methods;
        int offset;
        if (existsList(methodList, typeIndex) && index >= (offset = methodList.offsets[typeIndex]) && index < offset + (methods = methodList.processMethods[typeIndex]).length) {
            // 是否匹配到类型
            return methods[index - offset] != null;
        }
        return false;
    }

    /**
     * 获取保存方法起始位置
     * @return 返回保存方法开始位置
     * @param secondaries 次链方法列表
     */
    private int getOffset(ChainMethodList[] secondaries, int typeIndex) {
        int offset = getListOffset(this, typeIndex);
        if (secondaries != null) {
            for (ChainMethodList secondary : secondaries) {
                // 取最小
                offset = Math.min(offset, getListOffset(secondary, typeIndex));
            }
        }
        // 取最小
        return Math.min(offset, getListOffset(defaultMethodList, typeIndex));
    }

    private int getListOffset(ChainMethodList methodList, int typeIndex) {
        return existsList(methodList, typeIndex) ? methodList.offsets[typeIndex] : processors.length;
    }

    /**
     * 创建结果上下文对象
     * @return 返回处理上下文结果
     */
    <R extends ProcessResult> R initProcessResult(ChainMethodList[] secondaries) {
        // 获取结果类型
        Class<R> resultCls = getResultClass(secondaries);
        if (ProcessResult.class.equals(resultCls)) {
            return resultCls.cast(EMPTY_CONTEXT);
        }
        try {
            // 创建参数上下文对象并初始化
            return ChainProcessUtils.newInstance(resultCls);
        } catch (Exception e) {
            throw new RuntimeException("参数上下文对象创建失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private<R extends ProcessResult> Class<R> getResultClass(ChainMethodList[] secondaries) {
        Class<?> resultCls = resultClass;
        if (secondaries != null) {
            for (ChainMethodList secondary : secondaries) {
                Class<?> secondaryCls = secondary.resultClass;
                if (secondaryCls != null && !resultCls.equals(secondaryCls) && resultCls.isAssignableFrom(secondaryCls)) {
                    resultCls = secondaryCls;
                }
            }
        }
        if (defaultMethodList != null) {
            Class<?> defaultCls = defaultMethodList.resultClass;
            if (defaultCls != null && !resultCls.equals(defaultCls) && resultCls.isAssignableFrom(defaultCls)) {
                resultCls = defaultCls;
            }
        }
        return (Class<R>) resultCls;
    }

    /**
     * 判断是否可保存
     * @param secondaries 副处理链方法
     * @return 返回是否可保存结果，true则需要保存，false则不需要保存
     */
    boolean canDoSave(ChainMethodList[] secondaries) {
        int saveIndex = ProcessType.SAVE.ordinal();
        if (existsList(this, saveIndex)) {
            return true;
        }
        if (secondaries != null) {
            for (ChainMethodList secondary : secondaries) {
                if (existsList(secondary, saveIndex)) {
                    return true;
                }
            }
        }
        return existsList(defaultMethodList, saveIndex);
    }

    private boolean existsList(ChainMethodList methodList, int typeIndex) {
        return methodList != null && typeIndex < methodList.offsets.length && methodList.processMethods[typeIndex] != null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Method[] methods : processMethods) {
            if (methods == null) {
                continue;
            }
            for (Method method : methods) {
                if (method == null || method == INVALID_METHOD) {
                    continue;
                }
                builder.append(" ==> ").append(toShortName(method, null)).append("\n");
            }
        }
        return builder.toString();
    }
}
