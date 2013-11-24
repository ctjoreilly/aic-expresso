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
package com.sri.ai.grinder.library.controlflow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.AbstractRewriter;
import com.sri.ai.grinder.core.FunctionApplicationProvider;
import com.sri.ai.grinder.core.HasFunctor;

/**
 * An atomic rewriter for conditional expressions. Returns the then or else
 * branches directly if condition is trivial. Includes related helper methods.
 * 
 * @author braz
 * 
 */
@Beta
public class IfThenElse extends AbstractRewriter {

	public static final String FUNCTOR = "if . then . else .";
	//
	private static final List<Integer> _pathToFunctor   = Collections.unmodifiableList(Arrays.asList(FunctionApplicationProvider.INDEX_OF_FUNCTOR_IN_FUNCTION_APPLICATIONS));
	private static final List<Integer> _pathToCondition = Collections.unmodifiableList(Arrays.asList(0));
	private static final List<Integer> _pathToThen      = Collections.unmodifiableList(Arrays.asList(1));
	private static final List<Integer> _pathToElse      = Collections.unmodifiableList(Arrays.asList(2));

	public IfThenElse() {
		this.setReifiedTests(new HasFunctor(FUNCTOR));
	}
	
	@Override
	public Expression rewriteAfterBookkeeping(Expression expression, RewritingProcess process) {
		
		if (expression.get(0).equals(true)) {
			return expression.get(1);
		}
		if (expression.get(0).equals(false)) {
			return expression.get(2);
		}
		
		return expression;
	}
	
	public static List<Integer> getPathToFunctor() {
		return _pathToFunctor;
	}
	
	public static List<Integer> getPathToCondition() {
		return _pathToCondition;
	}
	
	public static List<Integer> getPathToThen() {
		return _pathToThen;
	}
	
	public static List<Integer> getPathToElse() {
		return _pathToElse;
	}

	/**
	 * Make an if then else expression, returning the then or else branches directly if condition is trivial.
	 */
	public static Expression make(Expression condition, Expression thenBranch, Expression elseBranch) {
		if (condition.equals(true)) {
			return thenBranch;
		}
		if (condition.equals(false)) {
			return elseBranch;
		}
		if (thenBranch.equals(true) && elseBranch.equals(false)) {
			return condition;
		}
		Expression result = Expressions.make(FUNCTOR, condition, thenBranch, elseBranch);
		return result;
	}

	/**
	 * Makes an if then else expression by receiving the branches but, instead of relying on the argument order to decide which branch is the then branch and which one is the else branch,
	 * receives their respective indices (they must be 1 and 2, or 2 and 1 -- index 0 is the index of the condition itself).
	 */
	public static Expression make(Expression condition, int indexOfFirstBranch, Expression firstBranch, int indexOfSecondBranch, Expression secondBranch) {
		if (indexOfFirstBranch == 1) {
			return IfThenElse.make(condition, firstBranch, secondBranch);
		}
		return IfThenElse.make(condition, secondBranch, firstBranch);
	}

	/**
	 * Given an index of one of the branches of an if then else expression,
	 * returns the index of the other branch.
	 * @see #make(Expression, int, Expression, int, Expression)
	 */
	public static int oppositeBranchIndex(int branchIndex) {
		return branchIndex == 1 ? 2 : 1;
	}

	public static boolean isIfThenElse(Expression expression) {
		boolean result = expression.hasFunctor(FUNCTOR);
		return result;
	}
	
	/** Returns the condition of an if then else expression. */
	public static Expression getCondition(Expression expression) {
		Expression result = expression.get(0);
		return result;
	}
	
	/** Returns the then branch of an if then else expression. */
	public static Expression getThenBranch(Expression expression) {
		Expression result = expression.get(1);
		return result;
	}
	
	/** Returns the else branch of an if then else expression. */
	public static Expression getElseBranch(Expression expression) {
		Expression result = expression.get(2);
		return result;
	}

	/**
	 * Make a copy of a given if then else condition, but for a replaced condition,
	 * possibly simplifying it if new condition is true or false.
	 */
	public static Expression copyWithReplacedCondition(Expression ifThenElse, Expression newCondition) {
		if (newCondition.equals(Expressions.TRUE)) {
			return getThenBranch(ifThenElse);
		}
		if (newCondition.equals(Expressions.FALSE)) {
			return getElseBranch(ifThenElse);
		}
		Expression result = IfThenElse.make(newCondition, getThenBranch(ifThenElse), getElseBranch(ifThenElse));
		return result;
	}
}