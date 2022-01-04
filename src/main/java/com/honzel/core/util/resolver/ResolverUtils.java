package com.honzel.core.util.resolver;

import com.honzel.core.util.resolver.DefaultResolver;
import com.honzel.core.util.resolver.Resolver;

/**
 * 
 * @author honzel
 *
 */
public class ResolverUtils {


	public static Resolver createResolver(String opened, String closed) {
		return (new DefaultResolver(opened, closed)).useEscape('\\');
	}

	public static Resolver createResolver(String opened, String closed, CharSequence input) {
		return createResolver(opened, closed).reset(input);
	}

	public static Resolver createResolver(String opened, String closed, CharSequence input, int startPos) {
		return createResolver(opened, closed).reset(input, startPos);
	}

	public static Resolver createResolver(String opened, String closed, boolean trim) {
		return createResolver(opened, closed).useTrim(trim);
	}

	public static Resolver createResolver(String opened, String closed, boolean trim, int escapeChar) {
		return createResolver(opened, closed).useTrim(trim).useEscape(escapeChar);
	}

	public static int findTypes(String totalDelimiters, String sub) {
		return findTypes(totalDelimiters, sub, 0);
	}
	/**
	 * Return the types of the specified delimiters if is in the total delimiters.
	 * @param totalDelimiters the total opened/closed delimiters
	 * @param sub some delimiters.
	 * @param fromIndex the type index to start the search from
	 * @return Return the types of the specified delimiters if is in the total delimiters.
	 */
	public static int findTypes(String totalDelimiters, String sub, int fromIndex) {
		int result = 0;
		for (int i = sub.length() - 1; i >= 0; i -- ) {
			result |= findType(totalDelimiters, sub.charAt(i), fromIndex);
		}
		return result;
	}
	/**
	 * Return the type of the specified delimiter if is in the delimiters.
	 * @param totalDelimiters the total opened/closed delimiters
	 * @param ch the specified delimiter.
	 * @param fromIndex the type index to start the search from
	 * @return Return the type of the specified delimiter if is in the delimiters.
	 */
	public static int findType(String totalDelimiters, char ch, int fromIndex) {
		int result = 0;
		int index = totalDelimiters.indexOf(ch, fromIndex);
		if (index >= 0) {
			result = getType(index);
		}
		return result;
	}


	/**
	 * Return the type of the specified delimiters if is in the total delimiters.
	 * @param allDelimiters the total opened/closed delimiters
	 * @param ch the specified delimiter.
	 * @return Returns the type of the specified delimiters if is in the total delimiters.
	 */
	public static int findType(String allDelimiters, char ch) {
		return findType(allDelimiters, ch, 0);
	}


	/**
	 * Returns the type value at the specified index.
	 * @param typeIndex the specified index
	 * @return Returns the type value at the specified index.
	 */
	public static int getType(int typeIndex) {
		return DefaultResolver.getType(typeIndex);
	}

	/**
	 * Return the intersection between firstTypes and secondTypes
	 * @param firstTypes the first types
	 * @param secondTypes the second types
	 * @return Return the intersection between firstTypes and secondTypes
	 */
	public static int interTypes(int firstTypes, int secondTypes) {
		return (firstTypes & secondTypes);
	}
	
	
}
