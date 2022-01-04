package com.honzel.core.util;

import java.util.Map;
import java.util.WeakHashMap;
/**
 * A value that is provided per (thread) context classloader.
 * @author honzel
 *
 * @param <T>  value type
 */
public class ContextLocal<T> {
    private  Map<ClassLoader, T> valueMap = new WeakHashMap<>();
    private  boolean gValInitialized = false;
    private   T gVal;
    public  T initialValue() {
        return null;
    }
	public synchronized T get() {
		if (gValInitialized) {
			 return gVal;
		}
        valueMap.size();
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader != null) {
            	T value = valueMap.get(loader);
                if ((value == null) && !valueMap.containsKey(loader)) {
                    value = initialValue();
                    valueMap.put(loader, value);
                }
                return value;
            }
        } catch (SecurityException ignored) { }
        if (!gValInitialized) {
            gVal = initialValue();
            gValInitialized = true;
        }
        return gVal;
    }
}
