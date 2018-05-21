package com.honzel.core.util.exceptions;

/**
 * Thrown when an error is encountered whilst attempting to set/get a property
 * using the {@link com.honzel.core.util.bean.NestedPropertyUtilsBean} utility class.
 * @author honzy
 *
 */
public class PropertyException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1490518783425191759L;

	public PropertyException(String message, Throwable cause) {
		super(message, cause);
	}

	public PropertyException(String message) {
		super(message);
	}

	public PropertyException(Throwable cause) {
		super(cause);
	}
	
}
