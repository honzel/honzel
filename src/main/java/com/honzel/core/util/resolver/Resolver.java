package com.honzel.core.util.resolver;


/**
* Property Name Expression Resolver.it is not safe working in multithread.
* @author honzy
*/
public interface Resolver {
	
	/**
	 * the constant represented all types.
	 */
	public static final int ALL_OF_TYPES = -1;
	
	/**
	 * the constant represented none type.
	 */
	public static final int NONE_OF_TYPES = 0;
	/**
	 * the constant represented not to escape.
	 */
	public static final int NOT_ESCAPE = 0;
	
	/**
	 * disabled to  use of the terminal position.
	 */
	public static final int DISABLED_TERMINAL = -1;
	
	/**
	 * the constant represented  link type of result
	 */
	public static final int LINK = (1 << -2);
	
	
	/**
	 * the constant represented  end type of result
	 */
	public static final int END = (1 << -3);
	/**
	 * the constant represented  start type of result
	 */
	public static final int START = (1 << -1);

	/**
	 * Return the current expression type
	 * @return the current expression type.
	 */
	int getType();

	/**
	 *
	 * Return the end type of the current expression
	 *
	 * @return the end type of the current expression.
	 */
	int getEndType();

	/**
	 * Return the start of the current expression in the input expression .
	 * @return the index of the current expression in the input expression
	 */
	int getStart();

	/**
	 * Return the start of the current expression in the input expression .
	 * @param trim whether to trim or not
	 * @return the index of the current expression in the input expression
	 */
	int getStart(boolean trim);
	/**
	 * Return the end index of the current expression in the input expression  .
	 * @return the end index of the current expression in the input expression
	 */
	int getEnd();
	/**
	 * Return the end of the current expression in the input expression .
	 * @param trim  whether to trim or not
	 * @return the end of the current expression in the input expression .
	 */
	int getEnd(boolean trim);
	/**
	 * Return  the terminal point in the input expression .
	 * @return the terminal index in the input expression .
	 */
	int getTerminal();


	/**
	 * Return true if the tokens is pair.
	 * @return Return true if the tokens is pair, otherwise return false.
	 */
	boolean isPair();
	/**
	 * Return whether the current expression is the last one or not.
	 * @return return true when the current expression is the last one
	 */
	boolean isLast();
	/**
	 * Return whether the current expression is the first one (starts with 0 index of the input character sequence) or not.
	 * @return return true when  the current expression is the first one.
	 */
	boolean isFirst();
	/**
	 * specify  some tokens to use.
	 * @param types  the specified types that  represent  the  tokens.
	 * @return  Return this resolver.
	 */
	Resolver useTypes(int types);
	/**
	 * specify  some tokens to use.
	 * @param tokens  the specified tokens that  will to use to parse the input string.
	 * @return Return this resolver.
	 */
	Resolver useTokens(String tokens);
	/**
	 * specify  some tokens to use.
	 * @param openTokens  the specified opened tokens that  will to use to parse the input string.
	 * @param closeTokens the specified closed tokens that  will to use to parse the input string.
	 * @return  Return this resolver.
	 */
	Resolver useTokens(String openTokens, String closeTokens);
	/**
	 * Return true if the current type is in the specified types  that  represent  the  tokens.
	 * @param types  the specified types that  represent  the  tokens.
	 * @return Return true if the current type is in the specified types  that  represent  the  tokens
	 */
	boolean isInTypes(int types);
	/**
	 * Return true if the current expression is end in the specified tokens.
	 * @param types  the specified tokens.
	 * @return Return true if the current expression is end in the specified tokens
	 */
	boolean endsInTypes(int types);
	/**
	 * Return true if the current expression is in the specified tokens.
	 * @param tokens  the specified tokens.
	 * @return Return true if the current expression is in the specified tokens.
	 */
	boolean isInTokens(String tokens);

	/**
	 * Return true if the current expression is in the specified tokens.
	 * @return Return true if the current expression is in the specified tokens.
	 */
	boolean isInTokens();

	/**
	 * Return true if the current expression is end in the specified tokens.
	 * @param tokens  the specified tokens.
	 * @return Return true if the current expression is end in the specified tokens.
	 */
	boolean endsInTokens(String tokens);
	/**
	 * Return whether current content is empty or not.
	 * @return Return whether current content is empty or not.
	 */
	boolean isEmpty();
	/**
	 * Return whether current content is empty or not.
	 * @param trim whether to trim
	 * @return Return whether current content is empty or not.
	 */
	public boolean isEmpty(boolean trim);
	/**
	 * Return the content from the current  expression,remove the escape char that  is  in the current expression
	 *
	 * @return The current content
	 */
	String next();
	/**
	 * Return the content from the current  expression.
	 * @param escaped if true, remove the escape char that  is  in the current expression ,otherwise not
	 * @return  The current content
	 */
	String next(boolean escaped);
	/**
	 * Return the content from the current  expression.
	 * @param escaped if true, remove the escape char that is  in the current expression ,otherwise not
	 * @param trim  whether to trim the current expression or not.
	 * @return  The current content
	 */
	String next(boolean escaped, boolean trim);

	/**
	 * Return the substring of  the current  expression.
	 * @param startOffset  where to begin looking in the next token string from this resolver.
	 * @param endOffset where to end looking in the next token string from this resolver.
	 * @return Return the substring of  the current  expression if the <code>startOffset</code> and the <code>endOffset</code> is valid.
	 *           the result is the same
	 *          as the result of the expression
	 *          <pre>
	 *          this.next().substring(startIndex, endIndex);
	 *          </pre>
	 *           therein:
	 *          <pre>
	 *         		startIndex = startOffset &gt;= 0 ? startOffset : this.next().length() + startOffset;
	 *          </pre>
	 *          <pre>
	 *         		endIndex = endOffset &gt; 0 ? endOffset : this.next().length() + endOffset;
	 *          </pre>
	 *        if  endIndex/startIndex is out bound of the current expression or (startIndex &gt; endIndex),
	 *       returns <code>null</code>.
	 */
	String next(int startOffset, int endOffset);
	/**
	 * Return the substring of  the current  expression.
	 * @param startOffset  where to begin looking in the next token string from this resolver.
	 * @return Return the substring of  the current  expression if the <code>startOffset</code>  is valid.
	 *           the result is the same
	 *          as the result of the expression
	 *          <pre>
	 *          this.next().substring(startIndex);
	 *          </pre>
	 *           therein:
	 *          <pre>
	 *         		startIndex = startOffset &gt;= 0 ? startOffset : this.next().length() + startOffset;
	 *          </pre>
	 *       otherwise,
	 *        if  the <code>startIndex</code>  is out bound of the current expression,
	 *       returns <code>null</code>.
	 */
	String next(int startOffset);
	/**
	 * Return the content from the current  expression.
	 * @param escaped if true, remove the escape char that is  in the current expression ,otherwise not
	 * @param trim  whether to trim the current expression or not.
	 * @param containsToken whether to contains the token in current expression or not
	 * @return  The current content
	 */
	String next(boolean escaped, boolean trim, boolean containsToken);

	/**
	 * Extract the unsigned integer value from the current  expression
	 *
	 * @return The unsigned integer value or -1 if the current expression is not indexed
	 */
	int nextInt();
	/**
	 * Extract the unsigned integer value from the current  expression
	 *@param trim whether to trim the current expression.
	 * @return The unsigned integer value or -1 if the current expression is not indexed
	 */
	int nextInt(boolean trim);
	/**
	 * Extract the unsigned integer value from the current substring.
	 *@param startOffset the start offset.
	 *@param endOffset the end offset.
	 * @return The unsigned integer value or -1 if the current expression is not indexed
	 */
	int nextInt(int startOffset, int endOffset);
	/**
	 * Extract the unsigned integer value from the current substring.
	 *@param startOffset the start offset.
	 * @return The unsigned integer value or -1 if the current expression is not indexed
	 */
	int nextInt(int startOffset);
	/**
	 * Go to the next simple  expression from the input expression.
	 *
	 * @return Return false if it hit end,otherwise return true
	 */
	boolean hasNext();
	/**
	 * Go to the next simple  expression from the input expression with the specified tokens.
	 *
	 * @param tokens  the specified tokens that  will to use to parse the input string.
	 * @return Return false if it hit end,otherwise return true
	 */
	boolean hasNext(String tokens);
	/**
	 * reset the input expression and use 0 as  start position and the length of input as end position
	 *
	 * @param input property name expression
	 * @return Return this resolver.
	 */
	Resolver reset(CharSequence input);
	/**
	 *  reset the start position, can not reset the end position and input.
	 * @param startPos  the index of the start position.
	 * @return  Return this resolver.
	 */
	Resolver reset(int startPos);
	/**
	 *  reset the input expression , start position. use the length of the input as  the end position
	 * @param input The property name expression
	 * @param startPos  the index of the start position.
	 * @return  Return this resolver.
	 */
	Resolver reset(CharSequence input, int startPos);

	/**
	 *  reset the input expression and use current start index as start position and the current end index of input as term position
	 * @return Return this resolver.
	 */
	Resolver resetToCurrent();

	/**
	 *  reset the input expression and use current term index as start position
	 * @param offset the offset of term position
	 * @return Return this resolver.
	 */
	Resolver resetToBeyond(int offset);

	/**
	 * return the input expression last set
	 * @return return the input expression last set
	 */
	CharSequence getInput();
	/**
	 * Set the escape char
	 * @param escapeChar if the char is greater than 0,apply it as escape char in parse input expression
	 * @return Return this resolver
	 */
	Resolver useEscape(int escapeChar) ;
	/**
	 * Set the escape
	 * @param escapeChar if the char is greater than 0,apply it as escape char in parse input expression
	 * @param forChars the escape only for this characters , if the parameter is null ,for all characters.
	 * @return Return this resolver
	 */
	Resolver useEscape(int escapeChar, String forChars) ;

	/**
	 * set whether default to use trim when fetch the current expression string.
	 *@param trim whether default to trim the current expression.
	 * @return Return this resolver
	 */
	Resolver useTrim(boolean trim);

	/**
	 * set the terminal position of this resolver, if terminal is  equal or greater than 0, use it as the terminal position.
	 * otherwise use the input character sequences length as the terminal position.
	 * @param terminal the specified terminal position
	 * @return Return this resolver
	 */
	Resolver useTerminal(int terminal);
	/**
	 * return the current expression has escape or not.
	 * @return return the current expression has escape or not.
	 */
	boolean containsEscape();

	/**
	 * return the escape char.
	 * @return return the escape char.
	 */
	char getEscape();
	/**
	 * Return the types of the specified tokens.
	 * @param tokens  the specified tokens that  will to use to parse the input string.
	 * @return Return the types of the specified tokens.
	 */
	int findTypes(String tokens);
	/**
	 * Return the types of the specified tokens.
	 * @param openTokens  the specified opened tokens
	 * @param closeTokens the specified closed tokens
	 * @return Return the types of the specified tokens.
	 */
	int findTypes(String openTokens, String closeTokens);


	/**
	 * Tests if the substring of the next token string from this resolver beginning at the
	 * specified index starts with the specified prefix.
	 *
	 * @param   prefix    the prefix.
	 * @param   startOffset   where to begin looking in the next token string from this resolver.
	 * @return  the result is <code>false</code> if the input character sequence of this resolver is <code>null</code>.<br>
	 * 			if <code>startOffset</code> is  equal or greater than 0,
	 *           the result is the same
	 *          as the result of the expression
	 *          <pre>
	 *          this.next().startsWith(prefix, startOffset)
	 *          </pre>
	 *          , otherwise the result is the same
	 *          as the result of the expression
	 *          <pre>
	 *          this.next().startsWith(prefix, this.next().length() + startOffset)
	 *          </pre>
	 */
	boolean startsWith(String prefix, int startOffset);

	/**
	 * Tests if the next token string from this resolver starts with the specified prefix.
	 *
	 * @param   prefix   the prefix.
	 * @return  the result is <code>false</code> if the input character sequence of this resolver is <code>null</code>.<br>
	 *           the result is the same
	 *          as the result of the expression
	 *          <pre>
	 *          this.next().startsWith(prefix)
	 *          </pre>
	 */
	boolean startsWith(String prefix);
	/**
	 * Tests if the substring of the next token string from this resolver beginning at the
	 * specified index equals the specified string.
	 *
	 * @param   other    the other string.
	 * @param   startOffset   where to begin looking in the next token string from this resolver.
	 * @return  the result is <code>false</code> if the input character sequence of this resolver is <code>null</code>.<br>
	 * 			if <code>startOffset</code> is  equal or greater than 0,
	 *           the result is the same
	 *          as the result of the expression
	 *          <pre>
	 *          this.next().substring(startOffset).equals(other)
	 *          </pre>
	 *          , otherwise the result is the same
	 *          as the result of the expression
	 *          <pre>
	 *          this.next().substring(this.next().length() + startOffset).equals(other)
	 *          </pre>
	 */
	boolean nextEquals(String other, int startOffset);
	/**
	 * Tests if the next token string from this resolver equals the specified prefix.
	 *
	 * @param other   the other.
	 * @return  the result is <code>false</code> if the input character sequence of this resolver is <code>null</code>.<br>
	 *         otherwise, the result is the same
	 *          as the result of the expression
	 *          <pre>
	 *          this.next().equals(prefix)
	 *          </pre>
	 */
	boolean nextEquals(String other);

	boolean appendTo(Appendable appendable);

	boolean appendTo(Appendable appendable, boolean trim);

	boolean appendTo(Appendable appendable, int startOffset);

	boolean appendTo(Appendable appendable, int startOffset, int endOffset);
}
