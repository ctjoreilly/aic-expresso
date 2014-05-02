/*
 * Copyright (c) 2013, SRI International
 * All rights reserved.
 * Licensed under the The BSD 3-Clause License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * http://opensource.org/licenses/BSD-3-Clause
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of the aic-expresso nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sri.ai.expresso.core;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sri.ai.expresso.ExpressoConfiguration;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.Symbol;
import com.sri.ai.expresso.api.SyntaxTree;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.expresso.helper.SyntaxTrees;
import com.sri.ai.util.AICUtilConfiguration;
import com.sri.ai.util.base.BinaryProcedure;
import com.sri.ai.util.math.Rational;

/**
 * An Expression with a single node representing a symbol. Symbols can stand for
 * variables or values; however, in this library, variables are
 * indistinguishable from string values at the expression representation level.
 * Strings are regarded as variable names at the semantic, rather than
 * syntactic, level. For example, in an expression <code>sum(i, f(i))</code>,
 * <code>i</code> might be considered a variable because it is the index of
 * function <code>sum</code>, while <code>f</code> is considered a constant.
 * This decision fundamentally depends on the semantics of <code>sum</code>,
 * which "knows" that the first argument is the index. The semantic level may
 * choose any criteria whatever to decide what is a variable. For example, it
 * could always consider <code>x</code>, <code>y</code>, and <code>z</code> as
 * variables, or capitalized strings (as in Prolog). This expression
 * representation library does not commit to any set of such criteria.
 * 
 * Numbers are represented with {@link Rational} and therefore have arbitrary
 * precision. {@link #intValue()}, {@link #intValueExact()} and
 * {@link #doubleValue()} are available but may decrease precision, so they
 * should not be used in contexts in which arbitrary precision is expected,
 * which is probably true if the results are going to be stored again in a
 * Symbol.
 * 
 * @author braz
 */
@Beta
public class DefaultSymbol extends AbstractSyntaxTree implements Symbol  {
	private static final long serialVersionUID = 1L;
	
	public static int _displayNumericPrecision                = ExpressoConfiguration.getDisplayNumericPrecisionForSymbols();
	public static int _displayScientificGreaterNIntegerPlaces = ExpressoConfiguration.getDisplayScientificGreaterNIntegerPlaces();
	public static int _displayScientificAfterNDecimalPlaces   = ExpressoConfiguration.getDisplayScientificAfterNDecimalPlaces();
	//
	// Well known static Symbols
	private static final Expression SYMBOL_TRUE  = Expressions.createSymbol(true);
	private static final Expression SYMBOL_FALSE = Expressions.createSymbol(false);
	private static final Expression SYMBOL_0     = Expressions.createSymbol(new Rational(0));
	private static final Expression SYMBOL_1     = Expressions.createSymbol(new Rational(1));
	private static final Expression SYMBOL_2     = Expressions.createSymbol(new Rational(2));
	private static final Expression SYMBOL_3     = Expressions.createSymbol(new Rational(3));
	private static final Expression SYMBOL_4     = Expressions.createSymbol(new Rational(4));
	private static final Expression SYMBOL_5     = Expressions.createSymbol(new Rational(5));
	private static final Expression SYMBOL_6     = Expressions.createSymbol(new Rational(6));
	private static final Expression SYMBOL_7     = Expressions.createSymbol(new Rational(7));
	private static final Expression SYMBOL_8     = Expressions.createSymbol(new Rational(8));
	private static final Expression SYMBOL_9     = Expressions.createSymbol(new Rational(9));
	//
	private static boolean                      _useGlobalSymbolTable = ExpressoConfiguration.isUseGlobalSymbolTable();
	private static boolean                      _cacheNumericSymbols  = ExpressoConfiguration.isGlobalSymbolTableToCacheNumerics();
	private static Cache<Object, Expression>    _globalSymbolTable    = newSymbolTable();
	static {
		flushGlobalSymbolTable();
	}
	//
	private int hashCode = -1; // lazy init and re-use the calculated hashCode.
	
	public Object getValue() {
		return valueOrRootSyntaxTree;
	}

	public static void flushGlobalSymbolTable() {
		if (AICUtilConfiguration.isRecordCacheStatistics()) {
			System.out.println("Global Symbol Table Cache Stats="+_globalSymbolTable.stats());
		}
		// Causes relevant flags to be reset.
		_useGlobalSymbolTable = ExpressoConfiguration.isUseGlobalSymbolTable();
		_cacheNumericSymbols  = ExpressoConfiguration.isGlobalSymbolTableToCacheNumerics();
		
		_globalSymbolTable.invalidateAll();
		_globalSymbolTable = newSymbolTable();
		// Add well known symbols to the table
		// The Booleans.
		_globalSymbolTable.put(true,          SYMBOL_TRUE);
		_globalSymbolTable.put(Boolean.TRUE,  SYMBOL_TRUE);
		_globalSymbolTable.put("true",        SYMBOL_TRUE);
		_globalSymbolTable.put(false,         SYMBOL_FALSE);
		_globalSymbolTable.put(Boolean.FALSE, SYMBOL_FALSE);
		_globalSymbolTable.put("false",       SYMBOL_FALSE);
		// Common Numbers
		_globalSymbolTable.put("0",             SYMBOL_0);
		_globalSymbolTable.put(new Integer(0),  SYMBOL_0);
		_globalSymbolTable.put(new Rational(0), SYMBOL_0);
		_globalSymbolTable.put("1",             SYMBOL_1);
		_globalSymbolTable.put(new Integer(1),  SYMBOL_1);
		_globalSymbolTable.put(new Rational(1), SYMBOL_1);
		_globalSymbolTable.put("2",             SYMBOL_2);
		_globalSymbolTable.put(new Integer(2),  SYMBOL_2);
		_globalSymbolTable.put(new Rational(2), SYMBOL_2);
		_globalSymbolTable.put("3",             SYMBOL_3);
		_globalSymbolTable.put(new Integer(3),  SYMBOL_3);
		_globalSymbolTable.put(new Rational(3), SYMBOL_3);
		_globalSymbolTable.put("4",             SYMBOL_4);
		_globalSymbolTable.put(new Integer(4),  SYMBOL_4);
		_globalSymbolTable.put(new Rational(4), SYMBOL_4);
		_globalSymbolTable.put("5",             SYMBOL_5);
		_globalSymbolTable.put(new Integer(5),  SYMBOL_5);
		_globalSymbolTable.put(new Rational(5), SYMBOL_5);
		_globalSymbolTable.put("6",             SYMBOL_6);
		_globalSymbolTable.put(new Integer(6),  SYMBOL_6);
		_globalSymbolTable.put(new Rational(6), SYMBOL_6);
		_globalSymbolTable.put("7",             SYMBOL_7);
		_globalSymbolTable.put(new Integer(7),  SYMBOL_7);
		_globalSymbolTable.put(new Rational(7), SYMBOL_7);
		_globalSymbolTable.put("8",             SYMBOL_8);
		_globalSymbolTable.put(new Integer(8),  SYMBOL_8);
		_globalSymbolTable.put(new Rational(8), SYMBOL_8);
		_globalSymbolTable.put("9",             SYMBOL_9);
		_globalSymbolTable.put(new Integer(9),  SYMBOL_9);
		_globalSymbolTable.put(new Rational(9), SYMBOL_9);
	}
	
	public static Expression createSymbol(Object value) {
		Expression result = null;
		// If global symbol table to be used and the symbol's value is not
		// an expression - i.e. quoted expressions of the form:
		// <X>
		// as these need to be instantiated each time in order to be
		// parsed correctly.
		if (_useGlobalSymbolTable && !(value instanceof Expression)) {
			
			result = _globalSymbolTable.getIfPresent(value);
			if (result == null) {
				result = new DefaultSymbol(value);
				if (!(!_cacheNumericSymbols && result.getValue() instanceof Number)) {
					_globalSymbolTable.put(value, result);
				}
			}
		} 
		else {
			result = new DefaultSymbol(value);
		}
		
		return result;
	}
	
	/**
	 * Escape a String that is intended to be converted to a symbol of value
	 * string so that it can be parsed safely.
	 * 
	 * @param aStringSymbol
	 *            a string that is intended to be used to instantiate a string
	 *            valued symbol.
	 * @return a string that can be safely parsed with surrounding ' and
	 *         embedded escape characters, as necessary.
	 */
	public static String makeStringValuedSymbolParseSafe(String aStringSymbol) {
		String result = aStringSymbol;
		boolean containsSpace       = aStringSymbol.contains(" ");
		// Note: trailing ''' are allowed for symbol names, e.g. aSymbol'
		boolean containsSingleQuote = StringUtils.stripEnd(aStringSymbol, "'").contains("'");
		
		if (containsSpace || containsSingleQuote) {
			StringBuilder sb = new StringBuilder();
			if (!aStringSymbol.startsWith("'")) {
				sb.insert(0, "'");
			}
			sb.append(aStringSymbol);
			if (!aStringSymbol.endsWith("'")) {
				sb.append("'");
			}
			
			String substring;
			for (int i = 1; i < sb.length()-1; i++) {
				substring = sb.substring(i, i+1);
				if ("'".equals(substring)) {
					if (!escapedAlready(sb, i)) {
						sb.insert(i, "\\");
						// move forward an additional 1 as have inserted
						i++; 
					}
				} 
	 		}
			
			result = sb.toString();
		}
		
		return result;
	}

	@Override
	public int hashCode() {
		if (hashCode == -1) {
			hashCode = getValue().hashCode();
		}
		return hashCode;
	}
	
	@Override
	public boolean equals(Object another) {
		if (this == another) {
			return true;
		}
		
		Symbol anotherSymbol = null;
		if (another instanceof Expression && ((Expression) another).getSyntaxTree() instanceof Symbol) {
			anotherSymbol = (Symbol) ((Expression) another).getSyntaxTree();
		} 
		else {
			anotherSymbol = SyntaxTrees.makeSymbol(another);
			// Test again, as may have had self returned from the symbol table.
			if (this.getSyntaxTree() == anotherSymbol) {
				return true;
			}
		}
		
		if (getSyntaxTree().hashCode() == anotherSymbol.hashCode()) {
			return getSyntaxTree().getValue().equals(anotherSymbol.getValue());
		}
		return false;
	}

	@Override
	public String defaultToString() {
		String result = "";
		if (getSyntaxTree().getLabel() instanceof String) {
			result = makeStringValuedSymbolParseSafe((String)valueOrRootSyntaxTree);
		}
		else if (getSyntaxTree().getLabel() instanceof Expression) {
			result = "<" + valueOrRootSyntaxTree + ">";
		}
		else if (getSyntaxTree().getLabel() instanceof Number && _displayNumericPrecision != 0) {
			Rational rLabel = ((Rational) valueOrRootSyntaxTree);
			if (rLabel.isInteger()) {
				result = rLabel.toString();
			} 
			else {	
				Rational absValue       = rLabel.abs();
				Rational integerPart    = absValue.round(Rational.ROUND_FLOOR);
				
				// We don't want to loose any precision in the integer part.
				String formattedIntegerPart = integerPart.toString();
				int displayNumericPrecision = Math.max(formattedIntegerPart.length(), _displayNumericPrecision);
				
				String formattedAbsResult = removeTrailingZerosToRight(absValue.toStringDotRelative(displayNumericPrecision));
				
				// Once we have precision taken care of, now determine if we should instead
				// output the result in scientific notation.
				int[] integerAndFractionalPartSizes = getIntegerAndFractionalPartSizes(formattedAbsResult);
				if (integerAndFractionalPartSizes[0] > _displayScientificGreaterNIntegerPlaces ||
					integerAndFractionalPartSizes[1] > _displayScientificAfterNDecimalPlaces     ) {
					result = rLabel.toStringExponent(displayNumericPrecision);
				}
				else {
					result = (rLabel.isNegative() ? "-" : "") + formattedAbsResult;
				}
			}
		}
		else {
			result = valueOrRootSyntaxTree.toString();
		}
		
		return result;
	}

	public Expression clone() {
		return Expressions.createSymbol(getValue());
	}

	@Override
	public int intValue() {
		if (getSyntaxTree().getLabel() instanceof Number) {
			return ((Number) getSyntaxTree().getLabel()).intValue();
		}
		throw new Error("Expression.intValue() invoked on " + this + ", which is not a number.");
	}

	@Override
	public int intValueExact() throws ArithmeticException {
		if (getSyntaxTree().getLabel() instanceof Rational) {
			return ((Rational) getSyntaxTree().getLabel()).intValueExact();
		}
		throw new Error("Expression.intValueExact() invoked on " + this + ", which is not a number.");
	}

	@Override
	public double doubleValue() {
		if (getSyntaxTree().getLabel() instanceof Number) {
			return ((Number) getSyntaxTree().getLabel()).doubleValue();
		}
		throw new Error("Expression.doubleValue() invoked on " + this + ", which is not a number.");
	}


	@Override
	public Rational rationalValue() {
		if (getSyntaxTree().getLabel() instanceof Number) {
			return (Rational) getSyntaxTree().getLabel();
		}
		throw new Error("Expression.rationalValue() invoked on " + this + ", which is not a number.");
	}
	
	//
	// PRIVATE METHODS
	//
	// Note: Can only instantiate Symbols via the factory method.
	private DefaultSymbol(Object value) {
		
		if (value instanceof Number && !(value instanceof Rational)) {
			value = new Rational(((Number)value).doubleValue());
		} 
		else if (value.equals("true")) {
			value = Boolean.TRUE;
		} 
		else if (value.equals("false")) {
			value = Boolean.FALSE;
		} 
		else if (value instanceof String) {
			try {
				value = new Rational((String)value);
			}
			catch (NumberFormatException e) {
				// ignore
			}
		}

		this.valueOrRootSyntaxTree = value;
		syntaxTree = this;
	}
	
	private static String removeTrailingZerosToRight(String number) {
		String result = number;
		int dot = result.indexOf('.');
		int end = result.length();
		
		if (dot >= 0) {
			for (int i = end-1; i > dot; i--) {
				if (result.charAt(i) == '0') {
					end--;
				}
				else {
					break;
				}
			}
			result = result.substring(0, end);
		}
		// 11.0 would get converted to 11. 
		// This logic ensures the trailing dot is removed.
		if (result.endsWith(".")) {
			result = result.substring(0, result.length()-1);
		}
	
		return result;
	}
	
	private static int[] getIntegerAndFractionalPartSizes(String absoluteValue) {
		int[] result = new int[] {0, 0};
		
		int dot = absoluteValue.indexOf('.');
		// No Fractional part
		if (dot == -1) {
			result[0] = absoluteValue.length();
		}
		else {
			result[0] = dot;
			result[1] = absoluteValue.length() - dot - 1; // exclude the dot as well
		}
		
		return result;
	}
	
	private static boolean escapedAlready(StringBuilder stringBuilder, int pos) {
		boolean escaped = false;
		
		int numberEscapes = 0;
		for (int i = pos-1; i >= 0; i--) {
			if ("\\".equals(stringBuilder.substring(i, i + 1))) {
				numberEscapes++;
			} 
			else {
				break;
			}
		}
		
		// If escaped and the number of sequential escapes is odd.
		if (numberEscapes > 0 && (numberEscapes % 2) == 1) {
			escaped = true;
		}
		
		return escaped;
	}
	
	private static Cache<Object, Expression> newSymbolTable() {
		CacheBuilder<Object, Object> cb = CacheBuilder.newBuilder();
		
		long maximumSize = ExpressoConfiguration.getGlobalSymbolTableMaximumSize();
		// Note: a maximumSize of 
		// < 0 means no size restrictions
		// = 0 means no cache
		// > 0 means maximum size of cache
		if (maximumSize >= 0L) {			
			cb.maximumSize(maximumSize);
		}
		if (AICUtilConfiguration.isRecordCacheStatistics()) {
			cb.recordStats();
		}
		
		Cache<Object, Expression> result = cb.build();
		
		return result;
	}
//}/*
	@Override
	public SyntaxTree getRootTree() {
		return null;
	}

	@Override
	public Object getLabel() {
		return valueOrRootSyntaxTree;
	}

	@Override
	public Iterator<SyntaxTree> getImmediateSubTreesIncludingRootOneIterator() {
		List<SyntaxTree> emptyList = Collections.emptyList();
		return emptyList.iterator();
	}

	@Override
	public int numberOfImmediateSubTreesIncludingRootOneIterator() {
		return 0;
	}

	@Override
	public SyntaxTree setImmediateSubTree(int i, Object newIthArgument) {
		throw new Error("Attempt at changing " + i + "-th sub-syntax tree of Symbol: " + this + " with " + newIthArgument + ", but Symbols have no sub-trees");
	}

	@Override
	public SyntaxTree replaceSubTreesAllOccurrences(Function<SyntaxTree, SyntaxTree> replacementFunction, Predicate<SyntaxTree> prunePredicate, BinaryProcedure<SyntaxTree, SyntaxTree> listener) {
		return replaceSubTreesFirstOccurrence(replacementFunction, prunePredicate, listener);
	}

	@Override
	public SyntaxTree replaceSubTreesFirstOccurrence(Function<SyntaxTree, SyntaxTree> replacementFunction, Predicate<SyntaxTree> prunePredicate, BinaryProcedure<SyntaxTree, SyntaxTree> listener) {
		if (prunePredicate != null && prunePredicate.apply(this)) {
			return this;
		}
		SyntaxTree replacement = replacementFunction.apply(this);
		if (replacement == null) {
			replacement = this;
		}
		if (listener != null) {
			listener.apply(this, replacement);
		}
		return replacement;
	}
	
	@Override
	public SyntaxTree replaceSubTrees(Function<SyntaxTree, SyntaxTree> replacementFunction,
			boolean onlyTheFirstOne, Predicate<SyntaxTree> prunePredicate,
			BinaryProcedure<SyntaxTree, SyntaxTree> listener, boolean ignoreTopExpression) {
		if (ignoreTopExpression) {
			return this;
		}
		return replaceSubTreesFirstOccurrence(replacementFunction, prunePredicate, listener);
	}

	@Override
	public SyntaxTree replaceSubTrees(Function<SyntaxTree, SyntaxTree> replacementFunction) {
		return this;
	}

}
//*/