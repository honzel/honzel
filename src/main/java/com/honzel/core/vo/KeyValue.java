package com.honzel.core.vo;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * 键值对类型
 * @author honzel
 * @param <K> key类型
 * @param <V> value类型
 */
public class KeyValue<K, V> implements Serializable {

	public KeyValue() {
	}

	public KeyValue(K key, V value) {
		this.key = key;
		this.value = value;
	}

	private K key;
	private V value;

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public void setKey(K key) {
		this.key = key;
	}

	public void setValue(V value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	public String toString() {
		return "{key:" + key + ",value:" + value + "}";
	}

	public Map.Entry<K, V> toMapEntry() {
		return new Entry<>(key, value);
	}

}
