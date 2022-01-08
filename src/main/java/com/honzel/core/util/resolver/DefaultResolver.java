package com.honzel.core.util.resolver;


import java.io.IOException;
import java.util.Objects;

/**
 * Default Property Name Expression {@link Resolver} Implementation.
 * @author honzel
 */
public class DefaultResolver implements Resolver {
	
	private static final int MAX_SIZE = (32 - 3);
	
	public static final char NUMERIC_START = '0';
	public static final char NUMERIC_END = '9';
	
	/**
	 * the integer value represents the {@link Resolver#START} end type, which must be less than 0;
	 */
	private static final int TYPE_INDEX_OF_START = -1;
	/**
	 * the integer value represents the {@link Resolver#LINK} end type, which must be less than 0;
	 */
	private static final int TYPE_INDEX_OF_LINK = -2;
	/**
	 * the integer value represents the {@link Resolver#END} end type, which must be less than 0;
	 */
	private static final int TYPE_INDEX_OF_END = -3;
	
	
	 /**
     * 
     */
    public DefaultResolver() {
		this(".", "");
	}
    /**
     * 
     * @param opened the opened delimiters,  the size of  which cannot greater than {@code MAX_SIZE}
     * @param closed the closed delimiters,  the size of which cannot greater than the opened delimiters.
     */
    public DefaultResolver(String opened, String closed) {
    	if (opened == null) {
    		opened = "";
    	}
    	if (closed == null) {
    		closed = "";
    	}
    	int length = opened.length();
		if (length > MAX_SIZE) {
    		throw new ArrayIndexOutOfBoundsException("the length of the opened delimiters is more than " + MAX_SIZE + ": ("
    				+length + " > " + MAX_SIZE + ")");
    	}
    	if (length < closed.length()) {
    		throw new ArrayIndexOutOfBoundsException("the length of  closed delimiters is more than the opened delimiters: ("
    				+closed.length() + " > " + length + ")");
    	}
		this.opened = opened;
		this.closed = closed;
		
		this.endType = TYPE_INDEX_OF_END;
		this.type = TYPE_INDEX_OF_END;
		this.disableTerminal = true;
		// default use all types
		useTypes(ALL_OF_TYPES); 
		// default use '\' character as escape.
//		useEscape('\\'); 
	}
	

	
 
	/**
     * the current properties
     */
    private boolean isPair;
    private int start;
    private int type;
    private int end;
    private boolean hasEscape;
    private int endType;
    private int curTerminal;
    private int startTrim;
    
	/**
	 * the reset properties
	 */
	private CharSequence input;
	
	/**
     * the global properties
     */
    private boolean fetchOne;
    private int targets;
    private  char escape;
    private boolean toEscape;
    private String escapeForChars;
	private boolean trim;
	private boolean disableTerminal;
	private int terminal;
	
	/**
	 * the fixed properties.
	 */
	private final String opened;
	private  final String closed;


	public int getType() {
		switch (type) {
		case TYPE_INDEX_OF_START:
			return START;
		case TYPE_INDEX_OF_END:
			return END;
		case TYPE_INDEX_OF_LINK:
			return LINK;
		default:
			return (1 << type);
		}
	}

	public int getEndType() {
		if (endType == TYPE_INDEX_OF_END)
			return END;
		return (1 << endType);
	}

	private int getCurrentTerminal() {
		int len = input.length();
		if (disableTerminal || len < terminal) {
			return  len;
		}
		return terminal;
	}

	public static int getType(int typeIndex) {
		if (typeIndex >= -3 && typeIndex < MAX_SIZE)
			return (1 << typeIndex);
		throw new IllegalArgumentException("the argument is illegal: " + typeIndex);
	}

	public int getStart() {
		return getStart(trim);
	}

	public int getStart(boolean trim) {
		int startIndex = start;
		int endIndex = end;
		if (startIndex >= endIndex)
			return startIndex;
		if (trim && startTrim >= 0)
			return startTrim;
		if (isPair) {
			if (type != TYPE_INDEX_OF_LINK) {
				startIndex += 1;
				endIndex -= 1;
			}
		} else {
			if (type != TYPE_INDEX_OF_START) {
				startIndex += 1;
			}
		}
		if (trim) {
			while (startIndex < endIndex && input.charAt(startIndex) <= ' ')
				startIndex ++;
			startTrim = startIndex;
		}
		return startIndex;
	}



	public int getEnd() {
		return getEnd(trim);
	}

	public int getEnd(boolean trim) {
		int endIndex = end;
		if (isPair && type != TYPE_INDEX_OF_LINK)
			endIndex -= 1;
		if (trim) {
			int startIndex = getStart(trim);
			if (startIndex >= endIndex)
				return endIndex;
			int originalEnd = endIndex;
			while (endIndex > startIndex && input.charAt(endIndex - 1) <= ' ')
				endIndex --;
			if (hasEscape && endIndex != originalEnd
					&& (escapeForChars == null || escapeForChars.indexOf(input.charAt(endIndex)) >= 0)) {
				int count = 0;
				if (escapeForChars == null || escapeForChars.indexOf(escape) >= 0) {
					for (int i = endIndex - 1; i >= startIndex && input.charAt(i) == escape; i --)
						count ++;
				} else if (input.charAt(endIndex - 1) == escape) {
					count ++;
				}
				if (count % 2 == 1)
					endIndex ++;
			}
		}
		return endIndex;
	}

	public int getTerminal() {
		return curTerminal == 0 && input != null ? getCurrentTerminal() : curTerminal;
	}

	public Resolver useTerminal(int terminal) {
		this.terminal = terminal;
		this.disableTerminal = (terminal < 0);
        curTerminal = 0;
		return this;
	}

	public boolean isPair() {
		return isPair && type != TYPE_INDEX_OF_LINK;
	}

	public boolean isLast() {
		return end >= curTerminal;
	}

	public boolean isFirst() {
		return type == TYPE_INDEX_OF_START;
	}

	public Resolver useTypes(int types) {
		targets = NONE_OF_TYPES;
		fetchOne = false;
		int length = opened.length();
		if (types == NONE_OF_TYPES || length == 0) {
			return this;
		}
		if (types == ALL_OF_TYPES) {
            fetchOne = (length == 1);
			targets = fetchOne ? 0 : ALL_OF_TYPES;
			return this;
		}
		for (int i = 0; i < length; ++ i) {
			int type = (1 << i);
			if ((types & type) != 0) {
				if (fetchOne) {
					fetchOne = false;
					targets = types;
					break;
				} else {
					fetchOne = true;
					targets = i;
					if (types == type)
						break;
				}
			}
		}
		return this;
	}

	public boolean isEmpty() {
		return isEmpty(trim);
	}

	public boolean isEmpty(boolean trim) {
		return getStart(trim) >= getEnd(false);
	}

	public String next() {
		return next(true, trim);
	}

	public String next(boolean escaped) {
		return next(escaped, trim);
	}


	public String next(boolean escaped, boolean trim, boolean containsToken) {
		if (!containsToken) {
			return next(escaped, trim);
		}
		if (input == null || start > end)
			return null;
		if (start == end)
			return "";
		if (checkCutOffInput(escaped, trim)) {
			return input.subSequence(start, end).toString();
		}
		String result = next(escaped, trim);
		if (isPair) {
			if (type != TYPE_INDEX_OF_LINK) {
				return input.charAt(start) + result + input.charAt(end - 1);
			}
		} else {
			if (type != TYPE_INDEX_OF_START) {
				return input.charAt(start) + result;
			}
		}
		return result;
	}

	private boolean checkCutOffInput(boolean escaped, boolean trim) {
		if (escaped && hasEscape) {
			return false;
		}
		if (trim) {
			int from = getStart(false);
			int to = getEnd(false);
			return from >= to || (input.charAt(from) > ' ' && input.charAt(to - 1) > ' ');
		}
		return true;
	}

	public String next(boolean escaped, boolean trim) {
		return substring(escaped, getStart(trim), getEnd(trim));
	}



	public String next(int startOffset) {
		return next(startOffset, 0);
	}

	public String next(int startOffset, int endOffset) {
		if (input == null) {
			return null;
		}
		int startIndex = getStart(trim, startOffset);
		int endIndex;
		if (endOffset != 0) {
			endIndex = getStart(trim, endOffset);
		} else {
			endIndex = getEnd(trim);
		}
		if (startOffset < 0 && startIndex < getStart(trim)) {
			return null;
		}
		if (endOffset > 0 && endIndex > getEnd(trim)) {
			return null;
		}
		return substring(true, startIndex, endIndex);
	}

	private String substring(boolean escaped, int startIndex, int endIndex) {
		if (startIndex > endIndex)
			return null;
		if (startIndex == endIndex)
			return "";
		if (hasEscape && escaped) {
			StringBuilder sb = new StringBuilder();
			for (int i = startIndex; i < endIndex; i++) {
				char ch = input.charAt(i);
				if (ch == escape && (escapeForChars == null
								|| (i < curTerminal - 1 && escapeForChars.indexOf(input.charAt(i + 1)) >= 0))) {
					if (++i == endIndex)
						break;
					ch = input.charAt(i);
				}
				sb.append(ch);
			}
			return sb.toString();
		}
		return input.subSequence(startIndex, endIndex).toString();
	}

	private int intValue(boolean escaped, int startIndex, int endIndex) {
		if (endIndex <= startIndex)
			return -1;
		int result = 0;
		if (hasEscape && escaped) {
			for (int i = startIndex; i < endIndex; i++) {
				char ch = input.charAt(i);
				if (isEscapeChar(ch, i)) {
					if (++i == endIndex)
						break;
					ch = input.charAt(i);
				}
				if (ch < NUMERIC_START || ch > NUMERIC_END)
					return  -1;
				result = result * 10 + (ch - NUMERIC_START);
			}
		} else {
			for (int i = startIndex; i < endIndex; i++) {
				char ch = input.charAt(i);
				if (ch < NUMERIC_START || ch > NUMERIC_END)
					return -1;
				result = result * 10 + (ch - NUMERIC_START);
			}
		}
		return result;
	}

	public int nextInt() {
		return nextInt(trim);
	}


	public int nextInt(boolean trim) {
		return intValue(false, getStart(trim), getEnd(trim));
	}

	public int nextInt(int startOffset) {
		return nextInt(startOffset, 0);
	}

	public int nextInt(int startOffset, int endOffset) {
		int startIndex = this.getStart(trim, startOffset);
		int endIndex = endOffset != 0 ? getStart(trim, endOffset) : getEnd(trim);
		if (startOffset < 0 && startIndex < getStart(trim)) {
			return -1;
		}
		if (endOffset > 0 && endIndex > getEnd(trim)) {
			return -1;
		}
		return intValue(false, startIndex, endIndex);
	}

	public boolean hasNext() {
		if (input == null) {
			return false;
		}
		boolean firstFetch = false;
		if (end == start - 1) {
			firstFetch = true;
			end ++;
		}
		curTerminal = getCurrentTerminal();
		findNextToken(firstFetch);
		return start < curTerminal;
	}

	public boolean hasNext(String tokens) {
		boolean fetchOneBak = fetchOne;
		int targetsBak = targets;
		this.useTokensByTotal(tokens, opened);
		boolean result = hasNext();
		this.fetchOne = fetchOneBak;
		this.targets = targetsBak;
		return result;
	}


	private void findNextToken(boolean firstFetch) {
		hasEscape = false;
		startTrim = -1;
		start = end;
		//fetch the next opened token
		int result = findNextOpenToken(start);
		endType = result;
		if (checkHitToken(firstFetch, result)) {
			return;
		}
		type = fetchNext(firstFetch, result);
	}

	private int fetchNext(boolean firstFetch, int result) {
		while (true) {
			if (result < closed.length()) { //the closed token
				end = findNextPosition(closed.charAt(result), start + 1);
				isPair = true;
				if (end >= curTerminal) {
					endType = TYPE_INDEX_OF_END;
					if (fetchOne || targets == NONE_OF_TYPES)
						return TYPE_INDEX_OF_LINK;
					result = opened.indexOf(input.charAt(start), result + 1);
					if (result > 0 && ((targets & (1 << result)) != 0)) {
						end = start;
						continue;
					}
					return TYPE_INDEX_OF_LINK;
				}
				end += 1;
			} else { //the opened token
				if (isPair && type != TYPE_INDEX_OF_LINK)
					return TYPE_INDEX_OF_LINK;
				isPair = false;
				if (firstFetch)
					return TYPE_INDEX_OF_START;
				endType = findNextOpenToken(start + 1);
			}
		    return result;
		}
	}

	private boolean checkHitToken(boolean firstFetch, int result) {
		if (end > start) {
			if (firstFetch) {
				type = TYPE_INDEX_OF_START;
				return true;
			}
			if (type == TYPE_INDEX_OF_END)
				isPair = true;
			else if (isPair && type == TYPE_INDEX_OF_LINK)
				isPair = false;
			type = TYPE_INDEX_OF_LINK;
			return true;
		}
		if (result == TYPE_INDEX_OF_END) {
			isPair = false;
			type = (firstFetch && start == 0) ? TYPE_INDEX_OF_START
					: TYPE_INDEX_OF_END;
			return true;
		}
		return false;
	}

	private int findNextPosition(char token, int fromIndex) {
		for ( ; fromIndex < curTerminal; fromIndex++) {
			char ch = input.charAt(fromIndex);
			if (toEscape && isEscapeChar(ch, fromIndex)) {
				hasEscape = true;
				if (++fromIndex == curTerminal)
					break;
				continue;
			}
			if (ch == token)
				break;
		}
		return fromIndex;
	}

	private boolean isEscapeChar(char ch, int fromIndex) {
		return ch == escape && (escapeForChars == null
				|| (fromIndex < curTerminal - 1 && escapeForChars.indexOf(input.charAt(fromIndex + 1)) >= 0));
	}

	/**
	 *
	 * @param fromIndex the started position to find the opened token
	 * @return int
	 */
	private int findNextOpenToken(int fromIndex) {
		int result = TYPE_INDEX_OF_END;
		if (fetchOne) { // use one token
			if (!toEscape && disableTerminal && (input instanceof String)) {
				fromIndex = ((String) input).indexOf(opened.charAt(targets), fromIndex);
				if (fromIndex < 0)
					fromIndex = curTerminal;
			} else {
				fromIndex = findNextPosition(opened.charAt(targets), fromIndex);
			}
			if (fromIndex < curTerminal)
				result = targets;
		} else { //use more than one tokens
			for ( ; fromIndex < curTerminal; fromIndex ++) {
				char ch = input.charAt(fromIndex);
				if (toEscape && isEscapeChar(ch, fromIndex)) {
					hasEscape = true;
					if (++fromIndex == curTerminal)
						break;
					continue;
				}
				if (targets == NONE_OF_TYPES)
					continue;
				int ind = opened.indexOf(ch);
				if (ind >= 0 && (targets & (1 << ind)) != 0) {
					result = ind;
					break;
				}
			}
		}
		this.end = fromIndex;
		return result;
	}



	public Resolver reset(CharSequence input) {
		return reset(input, 0);
	}


	public Resolver reset(CharSequence input, int startPos) {
		this.input = input;
		return reset(startPos);
	}

	public Resolver reset(int startPos) {
		isPair = false;
		endType = TYPE_INDEX_OF_END;
		type = TYPE_INDEX_OF_END;
		hasEscape = false;
		if (startPos < 0)
			startPos = 0;
		start = startPos;
		end = start - 1;
		return this;
	}

	public CharSequence getInput() {
		return input;
	}


	public Resolver useEscape(int escapeChar) {
		return useEscape(escapeChar, null);
	}




	public Resolver useEscape(int escapeChar, String forChars) {
		if (escapeChar < 0) {
			escapeChar = 0;
		}
		if (this.escape != escapeChar) {
			this.hasEscape = false;
			this.escape = (char) escapeChar;
			this.toEscape = (escapeChar > 0);
			escapeForChars = forChars;
		} else if (!Objects.equals(escapeForChars, forChars)) {
			this.hasEscape = false;
			escapeForChars = forChars;
		}
		return this;
	}




	public String toString() {
		return "input: " + input + " \ncurrent: " + (end <= start ? ""	: input.subSequence(start, end));
	}

	public Resolver useTrim(boolean trim) {
		this.trim = trim;
		return this;
	}

	public boolean containsEscape() {
		return hasEscape;
	}

	public char getEscape() {
		return escape;
	}

	public boolean isInTypes(int types) {
		return (types & (1 << type)) != 0;
	}
	public boolean endsInTypes(int types) {
		return (types & (1 << endType)) != 0;
	}

	/**
     * Tests if the next token string from this resolver starts with the specified prefix.
     *
     * @param   prefix   the prefix.
     * @return  the result is <code>false</code> if the input character sequence of this resolver is <code>null</code>.
     *           the result is the same
     *          as the result of the expression
     *          <pre>
     *          this.next().startsWith(prefix, 0)
     *          </pre>
	 */
	public boolean startsWith(String prefix) {
		return match(prefix, 0, false);
	}
	 /**
     * Tests if the substring of the next token string from this resolver beginning at the
     * specified index starts with the specified prefix.
     *
     * @param   prefix    the prefix.
     * @param   startOffset   where to begin looking in the next token string from this resolver.
     * @return  the result is <code>false</code> if the input character sequence of this resolver is <code>null</code>.
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
	public boolean startsWith(String prefix, int startOffset) {
		return match(prefix, startOffset, false);
	}

	public boolean nextEquals(String other, int startOffset) {
		return match(other, startOffset, true);
	}

	public boolean nextEquals(String other) {
		return match(other, 0, true);
	}


	/**
     * Tests if the substring of the next token string from this resolver beginning at the
     * specified index match the specified prefix.
     *
     * @param  prefix    the prefix.
     * @param  startOffset   where to begin looking in the next token string from this resolver.
     * @param identical Whether they are identical or not.
     * @return boolean
	 */
	private boolean match(String prefix, int startOffset, boolean identical) {
		if (input == null || prefix == null)
			return false;
		int fromIndex = getStart(trim, startOffset);
		int interval = 0;
		if (startOffset >= 0) {
			interval = getEnd(trim) - fromIndex - prefix.length();
			if (interval < 0)
				return false;
		} else {
			if (identical) {
				if (prefix.length() != -startOffset)
					return false;
			} else {
				if (prefix.length() + startOffset  > 0)
					return false;
			}
			int begin = getStart(trim);
			if (fromIndex < begin)
				return false;
		}
		if (hasEscape) {
			for (int i = 0, len = prefix.length(); i < len; i ++, fromIndex ++) {
				char ch = input.charAt(fromIndex);
				if (isEscapeChar(ch, fromIndex)) {
					if (startOffset >= 0) {
						if (interval == 0)
							return false;
						interval --;
					}
					if (++fromIndex == curTerminal)
						break;
					ch = input.charAt(fromIndex);
				}
				if (ch != prefix.charAt(i))
					return false;
			}
		} else {
			if (input instanceof String) {
				if (!((String) input).startsWith(prefix, fromIndex))
					return false;
			} else {
				for (int i = prefix.length() - 1; i >= 0; i --) {
					char ch = input.charAt(fromIndex + i);
					if (ch != prefix.charAt(i))
						return false;
				}
			}
		}
		return !identical || startOffset < 0 || interval == 0;
	}


	private int getStart(boolean trim, int startOffset) {
		if (startOffset >= 0) {
			int begin = getStart(trim);
			startOffset += begin;
			if (hasEscape && startOffset != begin) {
				for (int i = begin; i < startOffset && startOffset <= end; i ++) {
					char ch = input.charAt(i);
					if (ch == escape && (escapeForChars == null
							|| (i < curTerminal - 1 && escapeForChars.indexOf(input.charAt(i + 1)) >= 0))) {
						startOffset ++;
						i ++;
					}
				}
			}
		} else {
			int end = getEnd(trim);
			startOffset += end;
			if (hasEscape) {
				boolean flag = true;
				for (int i = end - 1; i >= startOffset && startOffset >= start; i --) {
					char ch = input.charAt(i);
					if (ch == escape && flag && (escapeForChars == null
							|| (i < curTerminal - 1 && escapeForChars.indexOf(input.charAt(i + 1)) >= 0))) {
						startOffset --;
						flag = false;
					} else {
						flag = true;
					}
				}
			}
		}
		return startOffset;
	}
	private Resolver useTokensByTotal(String tokens, String totalTokens) {
		if (tokens == null) {
			if (totalTokens.length() == opened.length())
				return useTypes(ALL_OF_TYPES);
			tokens = totalTokens;
		}
		fetchOne = false;
		targets = NONE_OF_TYPES;
		boolean first = true;
		for (int i = tokens.length() - 1; i >= 0; i -- ) {
			int index = totalTokens.indexOf(tokens.charAt(i));
			if (index < 0)
				continue;
			addTargets(index, first);
			first= false;
		}
		return this;
	}
	public Resolver useTokens(String tokens) {
		return useTokensByTotal(tokens, opened);
	}

	public Resolver useTokens(String openTokens, String closeTokens) {
		if (closeTokens == null)
			return useTokensByTotal(openTokens, opened);
		if (openTokens == null)
			return useTokensByTotal(closeTokens, closed);
		fetchOne = false;
		targets = NONE_OF_TYPES;
		boolean first = true;
		for (int i = openTokens.length() - 1; i >= 0; i -- ) {
			int index = findNextIndex(i, openTokens, closeTokens);
			if (index < 0)
				continue;
			addTargets(index, first);
			first = false;
		}
		return this;
	}

	private void addTargets(int index, boolean first) {
		if (first) {
			fetchOne = true;
			targets = index;
		} else if (fetchOne) {
			if (targets != index) {
				targets = (1 << targets) | (1 << index);
				fetchOne = false;
			}
		} else {
			targets |= (1 << index);
		}
	}

	private int findNextIndex(int i, String openTokens, String closeTokens) {
		int index;
		if (i < closeTokens.length()) {
			index = closed.indexOf(closeTokens.charAt(i));
			while (index >= 0 && openTokens.charAt(i) != opened.charAt(index))
				index = closed.indexOf(closeTokens.charAt(i), index + 1);
		} else {
			index = opened.indexOf(openTokens.charAt(i), closed.length());
		}
		return index;
	}

	public boolean isInTokens() {
		return this.type >= 0;
	}

	public boolean isInTokens(String tokens) {
		return inTokens(type, tokens, opened);
	}

	private boolean inTokens(int type, String tokens, String totalTokens) {
		if (type < 0)
			return false;
		if (tokens != null) {
			return  (tokens.indexOf(totalTokens.charAt(type)) >= 0);
		}
		return type < totalTokens.length();
	}

	public boolean isInTokens(String openTokens, String closeTokens) {
		if (closeTokens == null)
			return inTokens(type, openTokens, opened);
		if (openTokens == null)
			return inTokens(type, closeTokens, closed);
		if (type < 0)
			return false;
		char token = opened.charAt(type);
		if (type >= closed.length())
			return openTokens.indexOf(token, closeTokens.length()) >= 0;
		char token2 = closed.charAt(type);
		int index = closeTokens.indexOf(token2);
		while (index >= 0 && index < openTokens.length() && openTokens.charAt(index) != token)
			index =closeTokens.indexOf(token2, index + 1);
		return (index >= 0 && index < openTokens.length());
	}

	public boolean endsInTokens(String tokens) {
		return inTokens(endType, tokens, opened);
	}


	public int findTypes(String tokens) {
		return findTypesByTotal(tokens, opened);
	}

	private int findTypesByTotal(String tokens, String totalTokens) {
		if (tokens == null) {
			if (opened.length() == totalTokens.length())
				return totalTokens.length() > 0 ? ALL_OF_TYPES : NONE_OF_TYPES;
			tokens = totalTokens;
		}
		int types = NONE_OF_TYPES;
		for (int i = tokens.length() - 1; i >= 0; i -- ) {
			int index = totalTokens.indexOf(tokens.charAt(i));
			if (index < 0)
				continue;
			types |= (1 << index);
		}
		return types;
	}

	public int findTypes(String openTokens, String closeTokens) {
		if (closeTokens == null)
			return findTypesByTotal(openTokens, opened);
		if (openTokens == null)
			return findTypesByTotal(closeTokens, closed);
		int type = NONE_OF_TYPES;
		for (int i = openTokens.length() - 1; i >= 0; i -- ) {
			int index = findNextIndex(i, openTokens, closeTokens);
			if (index < 0)
				continue;
			type |= (1 << index);
		}
		return type;
	}

	public Resolver resetToCurrent() {
		return resetToCurrent(0);
	}

	@Override
	public Resolver resetToCurrent(int offset) {
		return useTerminal(getEnd(false)).reset(getStart() + offset);
	}

	public Resolver resetToBeyond(int offset) {
		return reset(getCurrentTerminal() + offset).useTerminal(DISABLED_TERMINAL);
	}

	private boolean appendTo(Appendable appendable, boolean trim, int startOffset, int endOffset) {
		if(input == null) {
			return false;
		}
		int startIndex = getStart(trim, startOffset);
		int endIndex = endOffset != 0 ? getStart(trim, endOffset) : getEnd(trim);
		if (startIndex > endIndex || startOffset < 0 && startIndex < getStart(trim) || endOffset > 0 && endIndex > getEnd(trim)) {
			return false;
		}
		try {
			if (hasEscape) {
				appendable.append(substring(true, startIndex, endIndex));
			} else {
				appendable.append(input, startIndex, endIndex);
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean appendTo(Appendable appendable, int startOffset, int endOffset) {
		return appendTo(appendable, trim, startOffset, endOffset);
	}

	public boolean appendTo(Appendable appendable, int startOffset) {
		return appendTo(appendable, trim, startOffset, 0);
	}

	public boolean appendTo(Appendable appendable) {
		return appendTo(appendable, trim, 0, 0);
	}

	public boolean appendTo(Appendable appendable, boolean trim) {
		return appendTo(appendable, trim, 0, 0);
	}
}
