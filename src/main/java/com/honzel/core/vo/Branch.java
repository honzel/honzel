package com.honzel.core.vo;

import java.io.Serializable;
/**
 * 分叉对象类型
 * @author honzy
 *
 */
public class Branch<K, L, R> implements Serializable {
	
	private static final long serialVersionUID = 2421427916056650696L;
	public Branch() {}
	
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
	
	public K getKey() {
		return key;
	}

	public void setKey(K key) {
		this.key = key;
	}

	public L getLeft() {
		return left;
	}

	public void setLeft(L left) {
		this.left = left;
	}

	public R getRight() {
		return right;
	}

	public void setRight(R right) {
		this.right = right;
	}
	
	@Override
	public String toString() {
		return "key:" + key + ",left:" + left + ",right:" + right;
	}
	
}
