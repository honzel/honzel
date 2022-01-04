package com.honzel.core.vo;

import java.io.Serializable;
import java.util.Map;

/**
 * 键值对类型
 * @author honzel
 *
 */
public class Entry<K, V> implements Map.Entry<K, V> {

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

	public V setValue(V value) {
		V oldValue = this.value;
		this.value = value;
		return oldValue;
	}
	
	public String toString() {
		return "key:" + key + ",value:" + value;
	}
	
}
