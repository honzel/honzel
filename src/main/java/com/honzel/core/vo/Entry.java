package com.honzel.core.vo;

import java.io.Serializable;
/**
 * 键值对类型
 * @author honzy
 *
 */
public class Entry<K, V> implements Serializable {
	
	private static final long serialVersionUID = 4806914956857026385L;
	private K key;
	private V value;
	public Entry() {
	}
	
	public Entry(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public K getKey() {
		return key;
	}

	public void setKey(K key) {
		this.key = key;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}
	
	public String toString() {
		return "key:" + key + ",value:" + value;
	}
	
}
