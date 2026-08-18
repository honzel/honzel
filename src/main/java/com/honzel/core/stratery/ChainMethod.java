package com.honzel.core.stratery;

import com.honzel.core.constant.ArrayConstants;
import com.honzel.core.util.text.TextUtils;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

/**
 * 业务链单个方法对象
 * @author honzel
 * 2021/6/12
 */
class ChainMethod {
    /**
     * 参数返回值位
     */
    public static final int HASH_PARAM = 0;
    /**
     * 结果返回值位
     */
    public static final int HASH_RESULT = 1;
    /**
     * 链类型返回值位
     */
    public static final int HASH_CHAIN_TYPE = 2;

    private String methodRefName;

    /**
     * 处理器方法
     */
    private Method processMethod;

    /**
     * 日志
     */
    private final Logger log;

    /**
     * 处理方法参数标识
     */
    private int hash;

    /**
     * 方法自身的结果类型
     */
    private Class<?> resultType;

    /**
     * 方法返回值为Future时是否等待结果
     */
    private boolean futureWait;

    /**
     * 是否为可选方法
     */
    private boolean optional;

    ChainMethod(Method method, Logger log) {
        this.processMethod = method;
        this.log = log;
    }

    public String getMethodRefName() {
        if (processMethod == null) {
            return TextUtils.EMPTY;
        }
        if (methodRefName == null) {
            methodRefName = toShortName(processMethod, processMethod.getParameterTypes());
        }
        return methodRefName;
    }

    private static String toShortName(Method method, Class<?>[] parameterTypes) {
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

    /**
     * 是否是同一个处理器方法
     * @param other 另一个处理器方法
     * @return 返回是否是同一个处理器方法
     */
    public boolean isSameProcessMethod(ChainMethod other) {
        return processMethod != null && processMethod.equals(other.processMethod);
    }

    /**
     * 解析方法参数匹配
     * @param allArgumentTypes 所有参数类型
     * @param throwException 是否抛出异常
     * @return 返回解析是否成功
     */
    public boolean parseMethod(Class<?>[] allArgumentTypes, boolean throwException) {
        Class<?>[] actTypes = processMethod.getParameterTypes();
        int result = hash(allArgumentTypes, actTypes, throwException);
        if (result < 0) {
            return false;
        }
        hash = result;
        // 计算方法自身的结果类型
        if ((result & (1 << HASH_RESULT)) != 0) {
            int pos = HASH_RESULT;
            for (int i = 0; i < HASH_RESULT; ++i) {
                if ((result & (1 << i)) == 0) {
                    --pos;
                }
            }
            resultType = actTypes[pos];
        }
        return true;
    }

    /**
     * 获取方法的结果类型(仅当方法包含结果参数时返回非null)
     * @return 返回方法的结果类型
     */
    public Class<?> getResultType() {
        if (resultType == null && hash != 0 && (hash & (1 << HASH_RESULT)) != 0) {
            int pos = HASH_RESULT;
            for (int i = 0; i < HASH_RESULT; ++i) {
                if ((hash & (1 << i)) == 0) {
                    --pos;
                }
            }
            resultType = processMethod.getParameterTypes()[pos];
        }
        return resultType;
    }

    /**
     * 更新结果类型到链的结果类
     * @param allArgumentTypes 所有参数类型
     * @param isDefault 是否是默认方法
     * @param currentResultClass 当前结果类型
     */
    public Class<?> checkResultClass(Class<?>[] allArgumentTypes, boolean isDefault, Class<?> currentResultClass) {
        if (currentResultClass == null) {
            currentResultClass = allArgumentTypes[HASH_RESULT];
        }
        Class<?> actType = getResultType();
        if (actType == null || currentResultClass.equals(actType)) {
            // 结果类型相同则不替换
            return currentResultClass;
        }
        if (currentResultClass.isAssignableFrom(actType)) {
            // 实际类型是之前类型的子类型时，使用子类型
            return actType;
        }
        if (isDefault || actType.isAssignableFrom(currentResultClass)) {
            // 默认方法或实际类型是之前类型的父类型时，使用当前类型
            return currentResultClass;
        }
        // 参数的结果类型有冲突(仅默认方法抛异常)
        throw new RuntimeException(String.format("处理器方法[%s]的结果对象类型[%s]与该业务链或其他处理器的结果类型[%s]有冲突",
                getMethodRefName(), actType.getSimpleName(), currentResultClass.getSimpleName()));
    }

    public int getHash() {
        return hash;
    }

    public void setFutureWait(boolean futureWait) {
        this.futureWait = futureWait;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    private int hash(Class<?>[] allArgumentTypes, Class<?>[] actTypes, boolean throwException) {
        int argsNum;
        if ((argsNum = actTypes.length) > allArgumentTypes.length) {
            if (throwException) {
                throw new RuntimeException(String.format("处理器方法[%s]参数数量太多, 不能超过%s个", getMethodRefName(), allArgumentTypes.length));
            }
            return -1;
        }
        if (argsNum == 0) {
            return 0;
        }
        int pos = 0;
        int result = 0;
        for (int i = 0; i < allArgumentTypes.length; ++i) {
            if (actTypes[pos].isAssignableFrom(allArgumentTypes[i]) || allArgumentTypes[i].isAssignableFrom(actTypes[pos])) {
                if (++pos >= argsNum) {
                    if (i < HASH_RESULT && matchResultType(allArgumentTypes[HASH_RESULT], actTypes[pos - 1])) {
                        result |= (1 << HASH_RESULT);
                    } else {
                        result |= (1 << i);
                    }
                    break;
                }
                result |= (1 << i);
            } else if (i == HASH_RESULT && pos > 0 && matchResultType(allArgumentTypes[HASH_RESULT], actTypes[pos - 1])) {
                if (result == 1) {
                    result <<= HASH_RESULT;
                } else {
                    for (int j = HASH_RESULT - 1; j >= pos - 1; --j) {
                        int h;
                        if ((h = result & (1 << j)) != 0) {
                            result = ~h & result | (1 << HASH_RESULT);
                            break;
                        }
                    }
                }
            }
        }
        if (pos < argsNum) {
            if (throwException) {
                throw new RuntimeException(String.format("处理器方法[%s]第%s个参数的类型[%s]与业务链要求的类型[%s]不匹配",
                        getMethodRefName(), pos + 1, actTypes[pos].getSimpleName(), allArgumentTypes[pos].getSimpleName()));
            }
            return -1;
        }
        return result;
    }

    private boolean matchResultType(Class<?> argumentType, Class<?> actType) {
        return argumentType.isAssignableFrom(actType) || ProcessResult.class.isAssignableFrom(actType) && actType.isAssignableFrom(argumentType);
    }

    boolean isValid() {
        return processMethod != null;
    }


    /**
     * 设置为无效方法
     */
    void setToInvalid() {
        processMethod = null;
    }

    /**
     * 执行方法
     * @param processor 处理器实例
     * @param allArguments 所有参数
     * @param totalArgumentFlags 全参数标识
     * @return 返回是否继续
     */
    public boolean invoke(Object processor, Object[][] allArguments, int totalArgumentFlags) {
        if (processMethod == null) {
            return true;
        }
        if (log.isDebugEnabled()) {
            log.debug("执行方法[{}]", getMethodRefName());
        }
        try {
            Object result = processMethod.invoke(processor, getMethodActualArguments(allArguments, totalArgumentFlags));
            if (Boolean.TYPE.equals(processMethod.getReturnType())) {
                // 如果方法返回类型是boolean，则直接返回
                return (Boolean) result;
            }
            if (futureWait && result instanceof Future) {
                Object o = ((Future<?>) result).get();
                if (o instanceof Boolean) {
                    // 如果future的返回结果是Boolean类型，则直接返回
                    return (Boolean) o;
                }
            }
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t != null) {
                throw new RuntimeException(t.getMessage(), t);
            } else {
                throw new RuntimeException(e.getMessage(), e);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return true;
    }

    private Object[] getMethodActualArguments(Object[][] allArguments, int totalArgumentFlags) {
        int parameterCount = processMethod.getParameterCount();
        if (allArguments == null || parameterCount == 0) {
            return ArrayConstants.EMPTY_OBJECT_ARRAY;
        }
        int index = getArgumentIndex(totalArgumentFlags, hash, allArguments.length);
        Object[] args = allArguments[index];
        if (args == null) {
            args = initActualArguments(allArguments[allArguments.length - 1], hash, parameterCount);
            allArguments[index] = args;
        }
        return args;
    }

    private Object[] initActualArguments(Object[] maxArguments, int hash, int count) {
        Object[] args = new Object[count];
        for (int i = maxArguments.length - 1; i >= 0 && count > 0; i--) {
            if ((hash & (1 << i)) != 0) {
                args[--count] = maxArguments[i];
            }
        }
        return args;
    }

    /**
     * 根据参数总标识计算当前方法在参数数组中的槽位索引
     * <p>
     * allArguments数组按totalFlags中位的位置顺序分配槽位, 该方法通过计算totalFlags中
     * 位于当前方法hash位之前的有效位数(已设置的位数)来确定当前方法对应的槽位索引
     * </p>
     * @param totalFlags 所有方法的参数组合标识, 第i位为1代表存在hash值为i+1的参数组合
     * @param hash 当前方法的参数标识(1-7), 每一位代表一个参数(bit0=param, bit1=result, bit2=chainType)
     * @param len 参数数组(allArguments)的长度
     * @return 返回当前方法在参数数组中对应的槽位索引
     */
    private int getArgumentIndex(int totalFlags, int hash, int len) {
        // 只有一个槽位或仅需要param参数时, 始终在位置0
        if (len == 1 || hash == 1) {
            return 0;
        }
        // 转为0-based位位置, 计算当前方法对应的位标识
        int flag = 1 << --hash;
        // 获取当前方法位以下的所有位标识(即排在当前方法前面的潜在组合数)
        int flags = totalFlags & (flag - 1);
        // 前面没有更低的位, 当前方法是第一个组合
        if (flags == 0) {
            return 0;
        }
        // 只有2个组合时, 当前方法一定在位置1
        if (len == 2) {
            return 1;
        }
        // 3个组合时: 如果当前方法的位是totalFlags中最高位, 则在位置2, 否则在位置1
        if (len == 3) {
            return (flag | flags) == totalFlags ? 2 : 1;
        }
        // 通用情况: 从flags中减去0位的数量来得到索引
        // hash初始为0-based位位置(=该位以下的位数), 每遇到一个0位则减1, 最终结果=有效位数(已设置的位数)
        for (int i = hash - 1; i >= 0; --i) {
            if ((flags & 1) == 0) {
                --hash;
            }
            if (i > 0 && (flags >>>= 1) == 0) {
                hash -= i;
                break;
            }
        }
        return hash;
    }


    @Override
    public String toString() {
        return getMethodRefName();
    }
}
