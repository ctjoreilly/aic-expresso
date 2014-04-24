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
package com.sri.ai.expresso.api;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.util.base.BinaryPredicate;
import com.sri.ai.util.base.TernaryFunction;
import com.sri.ai.util.base.TernaryProcedure;
import com.sri.ai.util.math.Rational;

/**
 * An Expression is a Expression based on a syntax tree, and wrapped up with
 * information so it can be used by rewriters. This information consists in a
 * collection of implicitly quantified variables.
 * 
 * @author braz
 */
@Beta
public interface Expression extends Cloneable, Serializable {
	
	/**
	 * Provides the sub-expression of this expression.
	 * They must always be the same objects.
	 */
	Iterator<ExpressionAndContext> getImmediateSubExpressionsAndContextsIterator(RewritingProcess process);
	Expression replace(ExpressionAndContext replacement);
	
	List<Expression> getSubExpressions();
	
	Expression replaceFirstOccurrence(Expression replaced, Expression replacement, RewritingProcess process);
	Expression replaceAllOccurrences(Expression replaced, Expression replacement, RewritingProcess process);
	Expression replaceFirstOccurrence(Expression replaced, Expression replacement, BinaryPredicate<Expression, RewritingProcess> prunePredicate, RewritingProcess process);
	Expression replaceAllOccurrences(Expression replaced, Expression replacement, BinaryPredicate<Expression, RewritingProcess> prunePredicate, RewritingProcess process);
	Expression replaceFirstOccurrence(Function<Expression, Expression> replacementFunction, RewritingProcess process);
	Expression replaceAllOccurrences(Function<Expression, Expression> replacementFunction, RewritingProcess process);
	Expression replaceFirstOccurrence(Function<Expression, Expression> replacementFunction, BinaryPredicate<Expression, RewritingProcess> prunePredicate, RewritingProcess process);
	Expression replaceAllOccurrences(Function<Expression, Expression> replacementFunction, BinaryPredicate<Expression, RewritingProcess> prunePredicate, RewritingProcess process);
	Expression replaceFirstOccurrence(Function<Expression, Expression> replacementFunction, TernaryFunction<Expression, Function<Expression, Expression>, ExpressionAndContext, Function<Expression, Expression>> makeSpecificSubExpressionAndContextReplacementFunction, BinaryPredicate<Expression, RewritingProcess> prunePredicate, TernaryFunction<Expression, BinaryPredicate<Expression, RewritingProcess>, ExpressionAndContext, BinaryPredicate<Expression, RewritingProcess>> makeSpecificSubExpressionAndContextPrunePredicate, RewritingProcess process);
	Expression replaceAllOccurrences(Function<Expression, Expression> replacementFunction, TernaryFunction<Expression, Function<Expression, Expression>, ExpressionAndContext, Function<Expression, Expression>> makeSpecificSubExpressionAndContextReplacementFunction, BinaryPredicate<Expression, RewritingProcess> prunePredicate, TernaryFunction<Expression, BinaryPredicate<Expression, RewritingProcess>, ExpressionAndContext, BinaryPredicate<Expression, RewritingProcess>> makeSpecificSubExpressionAndContextPrunePredicate, RewritingProcess process);
	Expression replaceFirstOccurrence(Function<Expression, Expression> replacementFunction, TernaryProcedure<Expression, Expression, RewritingProcess> listener, RewritingProcess process);
	Expression replaceAllOccurrences(Function<Expression, Expression> replacementFunction, TernaryProcedure<Expression, Expression, RewritingProcess> listener, RewritingProcess process);
	Expression replaceFirstOccurrence(Expression replaced, Expression replacement, TernaryProcedure<Expression, Expression, RewritingProcess> listener, RewritingProcess process);
	Expression replaceAllOccurrences(Expression replaced, Expression replacement, TernaryProcedure<Expression, Expression, RewritingProcess> listener, RewritingProcess process);
	Expression replaceFirstOccurrence(Expression replaced, Expression replacement, BinaryPredicate<Expression, RewritingProcess> prunePredicate, TernaryProcedure<Expression, Expression, RewritingProcess> listener, RewritingProcess process);
	Expression replaceAllOccurrences(Expression replaced, Expression replacement, BinaryPredicate<Expression, RewritingProcess> prunePredicate, TernaryProcedure<Expression, Expression, RewritingProcess> listener, RewritingProcess process);
	Expression replaceFirstOccurrence(Function<Expression, Expression> replacementFunction, BinaryPredicate<Expression, RewritingProcess> prunePredicate, TernaryProcedure<Expression, Expression, RewritingProcess> listener, RewritingProcess process);
	Expression replaceAllOccurrences(Function<Expression, Expression> replacementFunction, BinaryPredicate<Expression, RewritingProcess> prunePredicate, TernaryProcedure<Expression, Expression, RewritingProcess> listener, RewritingProcess process);
	Expression replace(Function<Expression, Expression> replacementFunction, boolean onlyTheFirstOne, BinaryPredicate<Expression, RewritingProcess> prunePredicate, TernaryProcedure<Expression, Expression, RewritingProcess> listener, RewritingProcess process, boolean ignoreTopExpression);
	/**
	 * Returns the result of replacing one or all sub-expressions of this expression
	 * according to a replacement function.
	 * @param makeSpecificSubExpressionAndContextPrunePredicate Takes the current expression, the current replacement function and the sub-expression and its context about to be processed (the top one inclusive), and returns the pruning predicate to be used for that specific sub-expression.
	 * @param replacementFunction: takes a expression and returns a new expression, or itself in case no replacement is warranted.
	 * @param makeSpecificSubExpressionAndContextReplacementFunction: Takes the current expression, the current replacement function and the sub-expression and its context about to be processed (the top one inclusive), and returns the replacement function to be used for that specific sub-expression.
	 * @param prunePredicate: a predicate evaluating as true for sub-expressions that should be pruned (ignored).
	 * @param makeSpecificSubExpressionAndContextPrunePredicate: Takes the current prune predicate and the sub-expression and its context about to be processed (the top one inclusive), and returns the prune predicate to be used for that specific sub-expression.
	 * @param onlyTheFirstOne: if true, replaces at most one sub-expression.
	 * @param ignoreTopExpression: does not try to replace this expression as a whole; examines sub-expressions only.
	 * @param listener: binary procedure receiving original and replacement expression every time such a replacement occurs. If a sub-expression is replaced, it is invoked for that sub-expression as well as for all its "super-expressions", since they are all being replaced by a new expression.
	 * @param process: the rewriting process, used here for defining what is a sub-expression of what.
	 */
	Expression replace(Function<Expression, Expression> replacementFunction, 
			           TernaryFunction<Expression, Function<Expression, Expression>, ExpressionAndContext, Function<Expression, Expression>> makeSpecificSubExpressionAndContextReplacementFunction, 
			           BinaryPredicate<Expression, RewritingProcess> prunePredicate, 
			           TernaryFunction<Expression, BinaryPredicate<Expression, RewritingProcess>, ExpressionAndContext, BinaryPredicate<Expression, RewritingProcess>> makeSpecificSubExpressionAndContextPrunePredicate, 
			           boolean onlyTheFirstOne, 
			           boolean ignoreTopExpression, 
			           TernaryProcedure<Expression, Expression, RewritingProcess> listener, 
			           RewritingProcess process);

	/**
	 * Indicates what syntactic form the expression is.
	 * Syntactic forms are the primitive types of expressions in a logic.
	 * For example, in FOL we have the forms: term, predicate, simple formula, quantified formula, etc.
	 * HOL typically has function applications and lambda expressions as its basic syntactic forms.
	 */
	public Object getSyntacticFormType();

	public SyntaxTree getSyntaxTree();

	public Iterator<ExpressionAndContext> getImmediateSubExpressionsAndContextsIterator();
	public Iterator<Expression> getImmediateSubExpressionsIterator();
	
	public static final Function<Expression, Expression> GET_SYNTAX_TREE = new Function<Expression, Expression>() {
		@Override
		public Expression apply(Expression expression) {
			return expression;
		}
	};

	public Object clone() throws CloneNotSupportedException;
	
	///////////////////////// FUNCTION APPLICATION METHODS //////////////////////
	// The following methods are only valid for function applications.
	// They are undefined for other types of expressions, but for error detection purposes it is
	// useful that they return <code>null</code> or throw an exception.
	// Perhaps in the future there will be an extension of Expression for them,
	// in which case these methods would be present only there.
	// Or, instead, because function applications are the most common type of expression,
	// we will leave them here for convenience.
	
	/** Returns the functor if the expression is a function application, or <code>null</code> otherwise. */
	public Expression getFunctor();
	
	/**
	 * Returns the functor if the expression is a function application, or the symbol otherwise.
	 * It assumes that the expression is of one or the other type.
	 */
	public Expression getFunctorOrSymbol();
	
	/** Indicates whether expression is a function application with given functor. */
	public boolean hasFunctor(Object functor);
	
	/**
	 * Returns the arguments of a function application expression if this is one.
	 */
	public List<Expression> getArguments();
	
	/**
	 * Same as {@link #getArguments()}<code>.size()</code>, but potentially more efficient.
	 */
	public int numberOfArguments();
	
	/**
	 * Same as {@link #getArguments()}<code>.get(i)</code>, but potentially more efficient.
	 */
	public Expression get(int i);
	
	/**
	 * If this is a function application,
	 * returns an expression equal to this one, but for replacing the i-th argument by the given one.
	 * Generates an error otherwise.
	 */
	Expression set(int i, Expression newIthArgument);
	
	///////////////////////// SYMBOL METHODS //////////////////////

	/**
	 * Returns the value of an expression if it is based on a symbol.
	 */
	public Object getValue();

	/** 
	 * Returns the value of the symbol as an int. This may involve rounding or truncation.
	 * An error is thrown if the value of the symbol is not a number.
	 */
	int intValue();

	/** 
	 * Returns the value of the symbol as an int. This may involve rounding or truncation.
	 * An {@link java.lang.ArithmeticException.ArithmeticException} is thrown if the value of the symbol is not a number OR not an integral value.
	 */
	int intValueExact() throws ArithmeticException;

	/** 
	 * Returns the value of the symbol as a double. This may involve rounding or truncation.
	 * An error is thrown if the value of the symbol is not a number.
	 */
	double doubleValue();

	/** 
	 * Returns the value of the symbol as a Rational.
	 * An error is thrown if the value of the symbol is not a number.
	 */
	Rational rationalValue();
}