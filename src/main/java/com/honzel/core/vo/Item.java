package com.honzel.core.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 对项值的封装
 * @author honzel
 *
 */
public class Item<T> implements Serializable {
	private static final long serialVersionUID = 7635117985620374898L;
	private T value;
	public Item() {
	}
	
	public Item(T value) {
		this.value = value;
	}

	public T getValue() {
		return value;
	}
	
	public void setValue(T value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Item<?> item = (Item<?>) o;
		return Objects.equals(value, item.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	public String toString() {
		return "value:" + value;
	}
}
