package com.honzel.core.vo;

import java.io.Serializable;
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
	
	public String toString() {
		return "value:" + value;
	}
}
