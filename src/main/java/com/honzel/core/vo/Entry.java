package com.honzel.core.vo;

import java.util.Map;
import java.util.Objects;

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

	public void setNewValue(V value) {
		this.value = value;
	}

	public V setValue(V value) {
		V oldValue = this.value;
		this.value = value;
		return oldValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Entry<?, ?> entry = (Entry<?, ?>) o;
		return key.equals(entry.key) &&
				value.equals(entry.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	public String toString() {
		return "key:" + key + ",value:" + value;
	}
	
}
