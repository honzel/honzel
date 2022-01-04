package com.honzel.core.stratery;


import com.honzel.core.util.bean.BeanHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.honzel.core.constant.NumberConstants.INTEGER_ONE;
import static com.honzel.core.constant.NumberConstants.INTEGER_ZERO;
import static com.honzel.core.stratery.ChainConstants.CHAIN_TYPE_DEFAULT;
import static com.honzel.core.stratery.ChainConstants.MASK_LOW_FLAG;
import static com.honzel.core.stratery.ChainProcessUtils.getMaskHigh;
import static com.honzel.core.stratery.ChainProcessUtils.getMaskLow;


/**
 * 基础处理器链对象
 * @param <P> 入参
 * @param <R> 参数上下文对象类型
 * @author honzel
 * date 2021/1/17
 */
@SuppressWarnings({"unchecked", "unused", "WeakerAccess"})
public abstract class AbstractBusinessChain<P, R extends ProcessResult> {


	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * 参数对象上下文类型
	 */
	private Class<R> resultClass;
	/**
	 * 参数对象上下文类型
	 */
	private Class<P> paramClass;

	/**
	 * 是否需要时使用掩码, true-是;false-否
	 */
	private boolean maskIfNecessary;
	/**
	 * 默认业务链方法
	 */
	private ChainMethodList defaultMethodList;
	/**
	 * 特定类型的业务链map(chainType->chainMethodList)
	 */
	private Map<Integer, ChainMethodList> specifiedChainMethodMap;

	@Resource
	private ChainSaveContext saveContext;


	@PostConstruct
	private void init() {
		initProcessors();
	}



	/**
	 * 初始化processors
	 * @param processors 添加的处理器
	 */
	protected final void addProcessors(Object... processors) {
		addProcessors0(false, processors);
	}
	/**
	 * 初始化掩码processors
	 * @param processors 添加的处理器
	 */
	protected final void addMaskProcessors(Object... processors) {
		addProcessors0(true, processors);
	}

	/**
	 * 初孀化processors
	 * @param maskIfNecessary 是否需要时使用掩码, true-是;false-否
	 * @param processors 添加的处理器
	 */
	private void addProcessors0(boolean maskIfNecessary, Object[] processors) {
		this.maskIfNecessary = maskIfNecessary;
		// 安类型解析业务链
		Map<Integer, ChainMethodList> chainMethodMap = new HashMap<>();
		// 默认处理链
		this.defaultMethodList = parseChainMethodList(processors, chainMethodMap);
		// 其他业务链方法
		this.specifiedChainMethodMap = chainMethodMap.isEmpty() ? Collections.emptyMap() :  chainMethodMap;
	}

	protected boolean isMaskIfNecessary() {
		return maskIfNecessary;
	}

	/**
	 * 解析所有业务链类型方法
	 * @param processors 处理器
	 * @param chainMethodMap 业务链方法map
	 * @return 返回默认的业务链方法
	 */
	private ChainMethodList parseChainMethodList(Object[] processors, Map<Integer, ChainMethodList> chainMethodMap) {
		// 参数格式
		Class<?>[] allArgumentTypes = {paramClass, resultClass, Integer.TYPE};
		// 本类型
		Class<?> targetChain = AopUtils.getTargetClass(this);
		// 获取处理器对应业务链的方法
		for (int i = 0; i < processors.length; ++i) {
			// 获取处理方法
			boolean nonMatch = true;
			Class<?> processorClass = AopUtils.getTargetClass(processors[i]);
			for (Method method : ChainProcessUtils.getProcessMethodList(processorClass)) {
				// 获取该方法的BusinessProcessor
				BusinessProcessor annotation;
				if ((annotation = ChainProcessUtils.getProcessorAnnonation(method, targetChain)) != null) {
					// 按业务链类型解析
					if (addChainMethod(i, method, annotation, allArgumentTypes, processors, chainMethodMap, false) && nonMatch) {
						nonMatch = false;
					}
					continue;
				}
				// 解析默认链类型
				Class<?> currentType = targetChain;
				while ((currentType = currentType.getSuperclass()) != null && !currentType.equals(Object.class)) {
					// 获取默认链类型
					if ((annotation = ChainProcessUtils.getProcessorAnnonation(method, currentType)) != null) {
						// 按默认链类型解析
						if (addChainMethod(i, method, annotation, allArgumentTypes, processors, chainMethodMap, true) && nonMatch) {
							nonMatch = false;
						}
						break;
					}
				}
			}
			// 处理器没有匹配到任何处理方法
			if (nonMatch) {
				log.warn("处理器[{}]没有对应业务链的任何处理方法， 请用@BusinessProcessor声明指定", processorClass.getSimpleName());
			}
		}
		// 默认业务链
		ChainMethodList defaultResult = chainMethodMap.remove(CHAIN_TYPE_DEFAULT);
		if (defaultResult != null) {
			// 完成解析
			defaultResult.finish(allArgumentTypes);
		}

		chainMethodMap.forEach((chainType, methodList) -> {
			// 设置默认链方法
			methodList.setDefaultMethodList(defaultResult);
			// 完成解析
			methodList.finish(allArgumentTypes);
		});
		return defaultResult;
	}

	private boolean addChainMethod(int index, Method method, BusinessProcessor annotation, Class<?>[] argumentTypes, Object[] processors, Map<Integer, ChainMethodList> chainMethodMap, boolean isDefault) {
		// 按业务链类型解析
		boolean match = false;
		ProcessType processType = annotation.processType();
		boolean maskLow = annotation.maskLow();
		for (int chainType : annotation.chainType()) {
			int maskChainType = maskLow ? MASK_LOW_FLAG | chainType : chainType;
			// 获取之前解析的对象
			ChainMethodList chainMethodList = chainMethodMap.get(maskChainType);
			if (chainMethodList == null) {
				// 如果没有则新建初始化
				chainMethodList = new ChainMethodList(processors, log);
				// 放入map
				chainMethodMap.put(maskChainType, chainMethodList);
			}
			// 添加入方法
			if (chainMethodList.addMethod(index, method, argumentTypes, processType, isDefault) && !match) {
				match = true;
			}
		}
		return match;
	}

	public AbstractBusinessChain() {
		paramClass = (Class<P>) BeanHelper.getGenericActualType(getClass(), 0);
		resultClass = (Class<R>) BeanHelper.getGenericActualType(getClass(), 1);
	}

	/**
	 * 对处理器processors进行初始化, 调用addProcessors方法
	 */
	protected abstract void initProcessors();

    /**
     * 执行业务处理
     * @param param 参数
     * @return 返回结果
     */
	public R execute(P param) {
		// 执行处理
		return execute(param, getDefaultChainType(param));
	}
    /**
     * 执行业务处理
     * @param param 参数
     * @return 返回结果
     */
	public R execute(P param, R processResult) {
		// 执行处理
		return execute(param, processResult, getDefaultChainType(param));
	}
    /**
     * 执行业务处理
     * @param param 参数
     * @return 返回结果
     */
	public R execute(P param, int chainType) {
		// 主链
		ChainMethodList main = lookupMain(chainType);
		// 副链
		ChainMethodList[] secondaries = lookupSecondaries(main, chainType);
		// 执行处理
		return execute0(main, secondaries, param, main.initProcessResult(secondaries), chainType);
	}
    /**
     * 执行业务处理
     * @param param 参数
     * @return 返回结果
     */
	public R execute(P param, R processResult, int chainType) {
		ChainMethodList main = lookupMain(chainType);
		return execute0(main, lookupSecondaries(main, chainType), param, processResult, chainType);
	}
    /**
     * 执行业务处理
     *
	 * @param secondaries 次处理方法
	 * @param param 参数
	 * @return 返回结果
     */
	private R execute0(ChainMethodList main, ChainMethodList[] secondaries, P param, R processResult, int chainType) {
		try {
			// 执行前预处理
			preProcess(param, processResult, chainType);
			// 执行校验及处理过程
			doProcess(main, secondaries, param, processResult, chainType);
			// 执行后处理
			postProcess(param, processResult, chainType);
		} catch (RuntimeException e) {
			// 异常处理
			handleException(e, param, processResult, chainType);
		}
		return processResult;
	}

	/**
	 * 处理出现异常时的处理
	 * @param e 异常
	 * @param param 参数
	 * @param processResult 处理上下文结果
	 * @param chainType 链类型
	 */
	protected void handleException(RuntimeException e, P param, R processResult, int chainType) {
		throw e;
	}

	public<Q> R execute(P param, Q initData, BiConsumer<R, Q> initHandler) {
		// 执行结果
		return execute(param, initData, initHandler, getDefaultChainType(param));
	}

	public<Q> R execute(P param, Q initData, BiConsumer<R, Q> initHandler, int chainType) {
		// 主链
		ChainMethodList main = lookupMain(chainType);
		// 副链
		ChainMethodList[] secondaries = lookupSecondaries(main, chainType);
		// 构建参数上下文
		R processResult = main.initProcessResult(secondaries);
		// 参数转换
		initHandler.accept(processResult, initData);
		// 执行结果
		return execute0(main, secondaries, param, processResult, chainType);
	}

	/**
	 * 按对应链类型获取初始化的结果对象
	 * @param chainType 业务链类型
	 * @return 返回结果对象
	 */
	public R initProcessResult(int chainType) {
		// 构建参数上下文
		ChainMethodList main = lookupMain(chainType);
		return main.initProcessResult(lookupSecondaries(main, chainType));
	}

	/**
	 * 处理方法
	 * @param main 主业务链
	 * @param secondaries 次业务链
	 * @param param 输入参数
	 * @param processResult 处理上下文结果
	 * @param chainType 处理链类型
	 */
	private void doProcess(ChainMethodList main, ChainMethodList[] secondaries, P param, R processResult, int chainType) {
		// 获取全参数位标识
		int totalArgumentFlags = main.getTotalArgumentFlags(secondaries);
		// 初始化参数
		Object[][] allArguments = main.initArgumentsArray(totalArgumentFlags, param, processResult, chainType);
		// 校验方法执行返回true时才可执行保存方法
		if (main.doCheck(allArguments, totalArgumentFlags, secondaries)) {
			// 是否能执行保存方法
			if (main.canDoSave(secondaries)) {
				if (saveContext != null) {
					// 在事内执行
					saveContext.executeInTransaction(allArguments, totalArgumentFlags, secondaries, main::doSave);
				} else {
					// 没有事务环境
					main.doSave(allArguments, totalArgumentFlags, secondaries);
				}
			}
			// 保存后处理
			try {
				main.doAfter(allArguments, totalArgumentFlags, secondaries);
			} catch (RuntimeException e) {
				log.warn("执行后处理时发生异常: {}", e.getMessage());
				throw e;
			}
		}
	}

	/**
	 * 查找主链方法
	 * @param chainType 业务链类型
	 * @return 返回对应的参数列表
	 */
	private ChainMethodList lookupMain(int chainType) {
		ChainMethodList main;
		if ((main = specifiedChainMethodMap.get(chainType)) != null) {
			return main;
		}
		if (maskIfNecessary) {
			// 使用掩码
			if ((main = specifiedChainMethodMap.get(MASK_LOW_FLAG | getMaskLow(chainType))) != null) {
				// 如果低位掩码存在
				return main;
			}
			main = specifiedChainMethodMap.getOrDefault(getMaskHigh(chainType), defaultMethodList);
		} else {
			main = defaultMethodList;
		}
		if (main == null) {
			// 链名称
			String chainName = AopUtils.getTargetClass(this).getSimpleName();

			log.error("业务链[{}]不支持链类型[chainType={}]的处理", chainName, chainType);
			// 参数数量不匹配
			throw new RuntimeException(String.format("业务链[%s]不支持链类型[chainType=%s]的处理", chainName, chainType));
		}
		return main;
	}
	/**
	 * 查找副链方法
	 *
	 * @param main 主处理方法列表
	 * @param chainType 业务链类型
	 * @return 返回对应的参数列表
	 */
	private ChainMethodList[] lookupSecondaries(ChainMethodList main, int chainType) {
		if (maskIfNecessary) {
			ChainMethodList secondary1 = specifiedChainMethodMap.get(MASK_LOW_FLAG | getMaskLow(chainType));
			int len = INTEGER_ZERO;
			if (secondary1 != null && !secondary1.equals(main)) {
				len += INTEGER_ONE;
			}
			ChainMethodList secondary2 = specifiedChainMethodMap.get(getMaskHigh(chainType));
			ChainMethodList[] result;
			if (secondary2 != null && !secondary2.equals(main)) {
				if (len == INTEGER_ZERO || !secondary1.equals(secondary2)) {
					result = new ChainMethodList[len + INTEGER_ONE];
					if (len > INTEGER_ZERO) {
						result[INTEGER_ZERO] = secondary1;
					}
					result[len] = secondary2;
				} else {
					result = new ChainMethodList[] {secondary1};
				}
			} else {
				result = len > INTEGER_ZERO ? new ChainMethodList[] {secondary1} : null;
			}
			return result;
		}
		return null;
	}

	/**
	 * 实现类指定业务处理链楼
	 * @return 业务链类型
	 * @param param 入参
	 */
	protected int getDefaultChainType(P param) {
		return CHAIN_TYPE_DEFAULT;
	}
	/**
	 * 执行前处理
     * @param param 入参
     * @param processResult 处理上下文结果
     * @param chainType 业务处理链
     */
	protected void preProcess(P param, R processResult, int chainType) {

	}


	/**
	 * 执行后处理
     * @param param 入参
     * @param processResult 处理上下文结果
     * @param chainType 处理链类型
     */
	protected void postProcess(P param, R processResult, int chainType) {

	}



}
