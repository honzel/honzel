package com.honzel.core.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 分叉对象类型
 * @author honzel
 * @param <K> key类型
 * @param <L> left类型
 * @param <R> right类型
 */
public class Branch<K, L, R> implements Serializable {
	
	private static final long serialVersionUID = 2421427916056650696L;
	/**
	 * 默认构造函数
	 */
	public Branch() {}

	/**
	 * 构造函数
	 * @param key 中心点
	 * @param left 左侧
	 * @param right 右侧
	 */
	public Branch(K key, L left, R right) {
		this.key = key;
		this.left = left;
		this.right = right;
	}

	/**
	 * 中心点
	 */
	private K key;
	/**
	 * 左侧
	 */
	private L left;
	/**
	 * 右侧
	 */
	private R right;

	/**
	 * 获取中心点
	 * @return 中心点
	 */
	public K getKey() {
		return key;
	}

	/**
	 * 设置中心点
	 * @param key 中心点
	 */
	public void setKey(K key) {
		this.key = key;
	}

	/**
	 * 获取左侧
	 * @return 左侧
	 */
	public L getLeft() {
		return left;
	}

	/**
	 * 设置左侧
	 * @param left 左侧
	 */
	public void setLeft(L left) {
		this.left = left;
	}

	/**
	 * 获取右侧
	 * @return 右侧
	 */
	public R getRight() {
		return right;
	}

	/**
	 * 设置右侧
	 * @param right 右侧
	 */
	public void setRight(R right) {
		this.right = right;
	}

	/**
	 * 判断对象是否相等
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Branch<?, ?, ?> branch = (Branch<?, ?, ?>) o;
		return Objects.equals(key, branch.key) &&
				Objects.equals(left, branch.left) &&
				Objects.equals(right, branch.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, left, right);
	}

	@Override
	public String toString() {
		return "{key:" + key + ",left:" + left + ",right:" + right + "}";
	}
	
}
