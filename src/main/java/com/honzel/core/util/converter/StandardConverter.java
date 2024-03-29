package com.honzel.core.util.converter;

import com.honzel.core.util.exception.ConversionException;
import com.honzel.core.util.text.TextUtils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/**
 * This converter handles conversion to the following types:
 * <ul>
 *     <li><code>short</code></li>
 *     <li><code>boolean</code></li>
 *     <li><code>double</code></li>
 *     <li><code>float</code></li>
 *     <li><code>int</code></li>
 *     <li><code>long</code></li>
 *     <li><code>byte</code></li>
 *     <li><code>char</code></li>
 *     <li><code>java.lang.Short</code></li>
 *     <li><code>java.lang.Integer</code></li>
 *     <li><code>java.lang.Long</code></li>
 *     <li><code>java.lang.Byte</code></li>
 *     <li><code>java.lang.Character</code></li>
 *     <li><code>java.lang.Boolean</code></li>
 *     <li><code>java.lang.Float</code></li>
 *     <li><code>java.lang.Double</code></li>
 *     <li><code>java.lang.BigInteger</code></li>
 *     <li><code>java.lang.BigDecimal</code></li>
 *     <li><code>java.lang.Enum</code></li>
 *     <li><code>java.lang.Class</code></li>
 *     <li><code>java.lang.String</code></li>
 * </ul>
 * @author honzel
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class StandardConverter  extends AbstractConverter  {
	
	public StandardConverter() {
		super();
	}
	/**
	 * 
	 * @param disableException  whether or not disable to throw exception when error ocurrs that 
     * 					this converter and the default converter (if exists) converting the input object to the target type.
	 */
	public StandardConverter(boolean disableException) {
		setDisableException(disableException);
	}
	
	protected Object convertToType(Object value, Class toType)
			throws ConversionException {
		Object firstValue = fetchFirst(value, toType);
		if (TextUtils.isEmpty(firstValue)) {
			return getDefault(firstValue, toType);
		}
		if (toType.isPrimitive()) {
			Object defValue = primitiveDefaults.get(toType);
			if (defValue != null && defValue.getClass().equals(value.getClass()))
				return value;
		}
		if ((toType == Integer.class) || (toType == Integer.TYPE))
			return (int) longValue(firstValue);
		
		if ((toType == Double.class) || (toType == Double.TYPE))
			return doubleValue(firstValue);
		
		if ((toType == Boolean.class) || (toType == Boolean.TYPE))
			return booleanValue(firstValue) ? Boolean.TRUE : Boolean.FALSE;
		
		if ((toType == Long.class) || (toType == Long.TYPE))
			return longValue(firstValue);

		if (toType == Class.class) {
			return classValue(convertToString(firstValue));
		}
		if ((toType == Byte.class) || (toType == Byte.TYPE))
			return (byte) longValue(firstValue);
		
		if ((toType == Character.class) || (toType == Character.TYPE))
			return (char) longValue(firstValue);
		
		if ((toType == Short.class) || (toType == Short.TYPE))
			return (short) longValue(firstValue);
		
		if ((toType == Float.class) || (toType == Float.TYPE))
			return (float) doubleValue(firstValue);
		
		if (toType == BigInteger.class)
			return bigIntValue(firstValue);
		
		if (toType == BigDecimal.class)
			return bigDecValue(firstValue);
		
		if (Enum.class.isAssignableFrom(toType))
			return enumValue(toType, firstValue);
		
		return super.convertToType(value, toType);
	}


	private boolean booleanValue(Object value) {
	        if (value == null) return false;
	        
	        Class c = value.getClass();
	        if(value instanceof String)
	        	return Boolean.parseBoolean((String) value);
	        
	        if (c == Boolean.class)
	            return (Boolean) value;
	        
	        if (c == Character.class)
	            return (Character) value != 0;
	        
	        if (value instanceof Number)
	            return ((Number) value).doubleValue() != 0;
	        
	        return true; // non-null
	    }
	    
		private Enum enumValue(Class toClass, Object o) {
	        if(o != null) {
	        	return Enum.valueOf(toClass, convertToString(o));
	        }
	        return null;
	    }
		/**
		 * Evaluates the given string  as a class instance.
		 * @param typeName
		 * @return the class instance implied by the given string
		 */
		private Class classValue(String typeName) {
			if(typeName == null) 
				return null;
			
			int dimensions = 0;
			while(typeName.endsWith("[]")) {
				typeName = typeName.substring(0, typeName.length() - 2).trim();
				dimensions ++;
			}
			Class type = (Class) primitiveTypeMap.get(typeName);
			if(type == null) {
				try {
					type = Class.forName(typeName);
				} catch (Throwable e) {
					throw new IllegalArgumentException(e);
				}
			}
			if(dimensions > 1) {
				type = Array.newInstance(type, new int[dimensions]).getClass();
			} else if(dimensions == 1) {
				type = Array.newInstance(type, 0).getClass();
			}
			return type;
		}

	    /**
	     * Evaluates the given object as a long integer.
	     * 
	     * @param value
	     *            an object to interpret as a long integer
	     * @return the long integer value implied by the given object
	     * @throws NumberFormatException
	     *             if the given object can't be understood as a long integer
	     */
		private long longValue(Object value) throws NumberFormatException {
	        if (value == null)
	            return 0L;

	        if(value instanceof String)
	        	return Long.parseLong((String) value);

	        Class c = value.getClass();
	        if (c.getSuperclass() == Number.class)
	            return ((Number) value).longValue();

	        if (c == Boolean.class)
	            return (Boolean) value ? 1 : 0;

	        if (c == Character.class)
	            return (Character) value;

	        return Long.parseLong(convertToString(value));
	    }

	    /**
	     * Evaluates the given object as a double-precision floating-point number.
	     * 
	     * @param value
	     *            an object to interpret as a double
	     * @return the double value implied by the given object
	     * @throws NumberFormatException
	     *             if the given object can't be understood as a double
	     */
		private double doubleValue(Object value) throws NumberFormatException {
	        if (value == null)  return 0.0;
	        
	        if(value instanceof String)
	        	return Double.parseDouble((String) value);
	        
	        Class c = value.getClass();
	        if (c.getSuperclass() == Number.class)
	            return ((Number) value).doubleValue();
	        
	        if (c == Boolean.class)
	            return (Boolean) value ? 1 : 0;
	        
	        if (c == Character.class)
	            return (Character) value;
	        
	        return Double.parseDouble(convertToString(value));
	    }

	    /**
	     * Evaluates the given object as a BigInteger.
	     * 
	     * @param value
	     *            an object to interpret as a BigInteger
	     * @return the BigInteger value implied by the given object
	     * @throws NumberFormatException
	     *             if the given object can't be understood as a BigInteger
	     */
		private BigInteger bigIntValue(Object value)
	            throws NumberFormatException {
	        if (value == null)
	            return BigInteger.valueOf(0L);
	        
	        if(value instanceof String)
	        	return new BigInteger((String) value);
	        
	        Class c = value.getClass();
	        if (c == BigInteger.class)
	            return (BigInteger) value;
	        
	        if (c == BigDecimal.class)
	            return ((BigDecimal) value).toBigInteger();
	        
	        if (c.getSuperclass() == Number.class)
	            return BigInteger.valueOf(((Number) value).longValue());
	        
	        if (c == Boolean.class)
	            return BigInteger.valueOf((Boolean) value ? 1 : 0);
	        
	        if (c == Character.class)
	            return BigInteger.valueOf((Character) value);
	        
	        return new BigInteger(convertToString(value));
	    }

	    /**
	     * Evaluates the given object as a BigDecimal.
	     * 
	     * @param value
	     *            an object to interpret as a BigDecimal
	     * @return the BigDecimal value implied by the given object
	     * @throws NumberFormatException
	     *             if the given object can't be understood as a BigDecimal
	     */
		private BigDecimal bigDecValue(Object value)
	            throws NumberFormatException {
	        if (value == null)
	            return BigDecimal.valueOf(0L);
	        
	        if(value instanceof String)
	        	return new BigDecimal((String) value);
	        
	        Class c = value.getClass();
	        if (c == BigDecimal.class)
	            return (BigDecimal) value;
	        
	        if (c == BigInteger.class)
	            return new BigDecimal((BigInteger) value);
	        
	        if (c.getSuperclass() == Number.class)
	            return new BigDecimal(((Number) value).doubleValue());
	        
	        if (c == Boolean.class)
	            return BigDecimal.valueOf((Boolean) value ? 1 : 0);
	        
	        if (c == Character.class)
	            return BigDecimal.valueOf((Character) value);
	        
	        return new BigDecimal(convertToString(value));
	        
	    }

	    protected String convertToString(Object value) throws RuntimeException {
	    	if (Class.class.equals(value.getClass())) {
	    		return getTypeName((Class) value);
	    	}
	    	return super.convertToString(value);
	    }
	    /**
	     * 
	     * @param type
	     * @return
	     */
		private String getTypeName(Class type) {
			int dimensions = 0;
			while (type.isArray()) {
				type = type.getComponentType();
				dimensions ++;
			}
			StringBuilder typeName = new StringBuilder(type.getName());
			for(int i = 0; i < dimensions; i ++)
				typeName.append("[]");
			return typeName.toString();
		}
	
		public Object getDefault(Object value, Class toType) {
			Object result = super.getDefault(value, toType);
			if(result == null) {
				return primitiveDefaults.get(toType);
			}
			return result;
		}
		/**
		 * if the specified type parameter is  a primitive type ,return  a default value represents this type,
		 * or return null
		 * @param toType  the specified type
		 * @return return  a default value represents this type
		 */
		public static Object primitiveDefault(Class toType) {
			return primitiveDefaults.get(toType);
		}
		

		private static final Map primitiveDefaults = new HashMap();
		private static final  Map primitiveTypeMap = new HashMap();
		static {
			primitiveDefaults.put(Short.TYPE, (short) 0);
			primitiveDefaults.put(Integer.TYPE, 0);
			primitiveDefaults.put(Long.TYPE, 0L);
			primitiveDefaults.put(Boolean.TYPE, Boolean.FALSE);
			primitiveDefaults.put(Byte.TYPE, (byte) 0);
			primitiveDefaults.put(Character.TYPE, (char) 0);
			primitiveDefaults.put(Float.TYPE, 0.0f);
			primitiveDefaults.put(Double.TYPE, 0.0);
			primitiveDefaults.put(Void.TYPE, null);
			for (Object o : primitiveDefaults.keySet()) {
				Class type = (Class) o;
				primitiveTypeMap.put(type.getName(), type);
			}
		}

}
