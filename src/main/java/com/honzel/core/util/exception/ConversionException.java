package com.honzel.core.util.exception;

/**
 * <p>A <b>ConversionException</b> indicates that a call to
 * <code>Converter.convert()</code> has failed to complete successfully.
 * @author honzel
 *
 */
public class ConversionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5581881894437293008L;

	public ConversionException() {
		super();
	}

	/**
	 * Constructs a new <b>ConversionException</b> with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <code>null</code> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
	 */
	public ConversionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConversionException(String message) {
		super(message);
	}

	public ConversionException(Throwable cause) {
		super(cause);
	}
	
}
