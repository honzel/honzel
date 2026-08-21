package com.honzel.core.stratery;

import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 业务类型方法列表对象
 * @author honzel
 * 2021/6/12
 */
class ChainMethodList {
    /**
     * 参数总长数
     */
    private static final int ARGS_LENGTH = 3;
    /**
     * 最多预初始化参数组数
     */
    private static final int PRE_ARGS_INIT_LENGTH = 3;
    /**
     * 二进制111，代表同时包含全部3个参数
     */
    private static final int HASH_ALL_ARGS = (1 << ARGS_LENGTH) - 1;

    /**
     * 日志对象
     */
    private final Logger log;
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
     * 处理器链方法列表, 按处理类型分组
     */
    private ChainMethod[][] chainMethods = new ChainMethod[PROCESS_TYPE_LENGTH][];

    /**
     * 最后解析位置是否默认
     */
    private boolean[] topDefaults = new boolean[PROCESS_TYPE_LENGTH];

    /**
     * 所有方法的参数标识
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
     * @param businessProcessor 处理标签
     * @param isDefault 是否是默认链的方法
     * @param exclude 是否排除当前方法
     */
    boolean addMethod(int index, Method method, Class<?>[] allArgumentTypes, BusinessProcessor businessProcessor, boolean isDefault, boolean exclude) {
        // 校验上一个方法结果如果需要
        checkTopIndex(index, allArgumentTypes);
        // 方法类型
        final int typeIndex = businessProcessor.processType().ordinal();
        // 如果为空时初始化
        ChainMethod[] methods = chainMethods[typeIndex];
        int offset;
        if (methods == null) {
            // 链方法数组初始化
            chainMethods[typeIndex] = (methods = new ChainMethod[processors.length - index]);
            // 链方法起始位置
            offsets[typeIndex] = index;
            offset = 0;
        } else {
            offset = index - offsets[typeIndex];
        }
        if (methods[offset] != null) {
            if (methods[offset].isValid() == exclude) {
                // 新老方法exclude类型不相同
                if (exclude) {
                    // 新方法是exclude方法, 老方法不是exclude方法, 跳过新方法
                    return false;
                }
            } else {
                // 新老方法exclude类型相同
                if (topDefaults[typeIndex] == isDefault) {
                    // 新老方法isDefault方式相同
                    if (!exclude && log.isWarnEnabled()) {
                        log.warn("同一处理器下对应相同的链类型有重复的处理方法, 后面一个将被忽略掉: [{}]<==>[{}]", methods[offset].getMethodRefName(), method);
                    }
                    return false;
                }
                if (isDefault) {
                    return false;
                }
            }
        }
        if (exclude) {
            // 没有其他的处理方法时, 创建一个空的ChainMethod对象(无处理方法)
            methods[offset] = new ChainMethod(null, log);
            methods[offset].setOptional(businessProcessor.optional());
            topDefaults[typeIndex] = isDefault;
            return true;
        }
        // 创建链方法对象
        ChainMethod chainMethod = new ChainMethod(method, log);
        if (isDefault) {
            // 解析参数匹配
            if (!chainMethod.parseMethod(allArgumentTypes, false)) {
                // 参数不匹配
                return false;
            }
        } else {
            // 解析参数匹配
            chainMethod.parseMethod(allArgumentTypes, true);
            // 更新结果类型
            resultClass = chainMethod.checkResultClass(allArgumentTypes, false, resultClass);
            // 更新参数标识
            int hash = chainMethod.getHash();
            if (hash > 0) {
                // 空参数不打标
                argumentFlags |= (1 << (hash - 1));
            }
        }
        // 设置futureWait和optional属性
        chainMethod.setFutureWait(businessProcessor.futureWait());
        chainMethod.setOptional(businessProcessor.optional());
        // 设置是否默认标识
        topDefaults[typeIndex] = isDefault;
        // 设置为可访问
        method.setAccessible(true);
        // 添加方法
        methods[offset] = chainMethod;
        return true;
    }

    private void checkTopIndex(int parseIndex, Class<?>[] allArgumentTypes) {
        if (topIndex == parseIndex) {
            return;
        }
        if (topDefaults != null) {
            for (int i = 0; i < topDefaults.length; i++) {
                if (topDefaults[i]) {
                    // 重新解析默认方法的结果类型
                    ChainMethod cm = chainMethods[i][topIndex - offsets[i]];
                    // 解析默认结果类型
                    tempResultClass = cm.checkResultClass(allArgumentTypes, true, tempResultClass);
                    int hash = cm.getHash();
                    if (hash > 0) {
                        // 空参数不打标
                        argumentFlags |= (1 << (hash - 1));
                    }
                    // 去掉默认
                    topDefaults[i] = false;
                }
            }
        }
        topIndex = parseIndex;
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
        for (int i = chainMethods.length - 1; i >= 0; --i) {
            if (chainMethods[i] == null) {
                continue;
            }
            // 判断是否全部为空
            int j = 0;
            while (j < chainMethods[i].length && chainMethods[i][j] == null) j++;
            // 如果全部为空
            if (j == chainMethods[i].length) {
                // 置空并跳过
                chainMethods[i] = null;
                continue;
            }
            if (first) {
                if (i + 1 < chainMethods.length) {
                    // 调整方法数组大小
                    chainMethods = Arrays.copyOf(chainMethods, i + 1);
                    offsets = Arrays.copyOf(offsets, i + 1);
                }
                first = false;
            }
            // 只有check不需要distinct
            boolean distinct = i != ProcessType.CHECK.ordinal();
            // 调整链方法大小
            chainMethods[i] = resizeMethods(chainMethods[i], j, topIndex - offsets[i], distinct);
            if (j > 0) {
                offsets[i] = offsets[i] + j;
            }
        }
        topIndex--;
        // 返回结果类型
        if (resultClass == null) {
            // 设置默认结果类型
            resultClass = (tempResultClass != null ? tempResultClass : allArgumentTypes[ChainMethod.HASH_RESULT]);
        } else if (tempResultClass != null
                && !resultClass.equals(tempResultClass) && resultClass.isAssignableFrom(tempResultClass)) {
            // 如果临时结果类型为子类型时，使用子类型做为结果类型
            resultClass = tempResultClass;
        }
        // 不再用的数据置为空
        tempResultClass = null;
        topDefaults = null;
    }

    private ChainMethod[] resizeMethods(ChainMethod[] methods, int offset, int len, boolean distinct) {
        int end = Math.min(len, methods.length);
        boolean first = true;
        for (int i = end - 1; i > offset; --i) {
            ChainMethod cm = methods[i];
            if (cm == null) {
                if (first) {
                    --end;
                }
                continue;
            }
            if (distinct) {
                if (first) {
                    first = false;
                }
                if (cm.isValid()) {
                    for (int j = i - 1; j >= offset; --j) {
                        ChainMethod method = methods[j];
                        if (method != null && cm.isSameProcessMethod(method)) {
                            method.setToInvalid();
                        }
                    }
                }
            } else {
                break;
            }
        }
        if (end - offset < methods.length) {
            // 调整方法数组大小
            return Arrays.copyOfRange(methods, offset, end);
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
            // 判断是否包含全参数，如果不包含则加上全参数
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
                    case ChainMethod.HASH_PARAM:
                        args[pos ++] = param;
                        break;
                    case ChainMethod.HASH_RESULT:
                        args[pos ++] = processResult;
                        break;
                    case ChainMethod.HASH_CHAIN_TYPE:
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


    /**
     * 解析参数组合数量和参数标识,当参数组合数量大于预初始化组合数量限制时，只返回参数组合数量
     * @param totalArgumentFlags 总参数标识
     * @return 返回参数长度和参数标识
     */
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
    boolean doAfter(Object[][] allArguments, int totalArgumentFlags, ChainMethodList[] secondaries) {
        return doNonSave(allArguments, totalArgumentFlags, secondaries, ProcessType.AFTER);
    }


    private boolean doNonSave(Object[][] allArguments, int totalArgumentFlags, ChainMethodList[] secondaries, ProcessType processType) {
        int typeIndex = processType.ordinal();
        // 获取起始位置
        int offset = getOffset(secondaries, typeIndex);
        // 循环执行方法
        for (int i = offset; i < processors.length; i++) {
            // 获取链方法
            ChainMethod method = getChainMethod(i, secondaries, typeIndex);
            if (method == null) {
                continue;
            }
            // 执行方法
            if (method.invoke(processors[i], allArguments, totalArgumentFlags)) {
                continue;
            }
            // 中断校验方法的执行
            if (log.isDebugEnabled()) {
                log.debug("执行方法[{}]返回false结果, 直接跳过后续的所有处理器处理", method.getMethodRefName());
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
        int saveIndex = ProcessType.SAVE.ordinal();
        // 获取起始位置
        int offset = getOffset(secondaries, saveIndex);
        // 循环执行方法
        for (int i = offset; i < processors.length; i++) {
            // 获取链方法
            ChainMethod chainMethod = getChainMethod(i, secondaries, saveIndex);
            if (chainMethod != null) {
                // 执行保存方法
                chainMethod.invoke(processors[i], allArguments, totalArgumentFlags);
            }
        }
    }

    /**
     * 获取链方法
     * @param index 处理器索引
     * @param secondaries 次要方法列表
     * @param typeIndex 操作类型索引
     * @return 返回链方法, 不存在返回null
     */
    private ChainMethod getChainMethod(int index, ChainMethodList[] secondaries, int typeIndex) {
        // 匹配主链方法
        ChainMethod cm = matchChainMethod(index, this, typeIndex);
        if (cm != null) {
            return cm;
        }
        if (secondaries != null) {
            for (ChainMethodList secondary : secondaries) {
                if ((cm = matchChainMethod(index, secondary, typeIndex)) != null) {
                    return cm;
                }
            }
        }
        // 匹配默认链方法
        return matchChainMethod(index, defaultMethodList, typeIndex);
    }

    private ChainMethod matchChainMethod(int index, ChainMethodList methodList, int typeIndex) {
        ChainMethod[] methods;
        int offset;
        if (existsList(methodList, typeIndex) && index >= (offset = methodList.offsets[typeIndex]) && index < offset + (methods = methodList.chainMethods[typeIndex]).length) {
            return methods[index - offset];
        }
        return null;
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
        return methodList != null && typeIndex < methodList.offsets.length && methodList.chainMethods[typeIndex] != null;
    }

    /**
     * 判断该链的所有方法是否都为可选方法
     * @return true表示所有方法都是可选的
     */
    boolean isAllOptional() {
        for (ChainMethod[] methods : chainMethods) {
            if (methods != null) {
                for (ChainMethod method : methods) {
                    if (method != null && !method.isOptional()) {
                        // 存在一个非可选方法，返回false
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (ChainMethod[] methods : chainMethods) {
            if (methods == null) {
                continue;
            }
            for (ChainMethod cm : methods) {
                if (cm != null && cm.isValid()) {
                    builder.append(" ==> ").append(cm).append("\n");
                }
            }
        }
        return builder.toString();
    }
}
