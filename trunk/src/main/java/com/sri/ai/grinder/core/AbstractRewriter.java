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
package com.sri.ai.grinder.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.Beta;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.core.AbstractSyntaxTree;
import com.sri.ai.expresso.core.DefaultCompoundSyntaxTree;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.Rewriter;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.helper.Justification;
import com.sri.ai.grinder.helper.RewriterLogging;
import com.sri.ai.grinder.helper.Trace;
import com.sri.ai.util.Util;
import com.sri.ai.util.Util.SelectPairResult;
import com.sri.ai.util.base.BinaryFunction;
import com.sri.ai.util.base.BinaryPredicate;
import com.sri.ai.util.base.Pair;

/**
 * A basic, default implementation of some of the {@link Rewriter} methods.
 * 
 * @author braz
 */
@Beta
public abstract class AbstractRewriter implements Rewriter {
	
	private static final List<Rewriter> _emptyChildList = Collections.unmodifiableList(new ArrayList<Rewriter>());
	//
	private String name = null;

	/**
	 * A general rewriting utility which receives an expression, looks for a
	 * pair of its arguments satisfying a given set of predicates (both
	 * individually and as a pair), performs a binary operation on them, and
	 * returns a new expression with the same functor as the original, with the
	 * pair of arguments removed and the result of the binary operation in the
	 * original position of the first one. If such a pair is not found, simply
	 * returns the original expression. If true, argument
	 * <code>noUnaryApplication</code> prevents the new operation from being a
	 * unary application, returning instead its only argument (this is useful
	 * for operators whose singleton application is the same as the identity,
	 * such as conjunction, disjunction, addition, etc).
	 */
	public static Expression expressionAfterBinaryOperationIsPerformedOnPairOfArgumentsSatisfyingPredicates(
			Expression expression, BinaryFunction<Expression, Expression, Expression> binaryOperation, Predicate<Expression> unaryPredicate1, Predicate<Expression> unaryPredicate2, BinaryPredicate<Expression, Expression> binaryPredicate,
			boolean noUnaryApplication) {

		SelectPairResult<Expression> pair =
			Util.selectPairInEitherOrder(expression.getArguments(), unaryPredicate1, unaryPredicate2, binaryPredicate);

		if (pair != null) {
			Expression operationResult = binaryOperation.apply(pair.satisfiesFirstPredicate, pair.satisfiesSecondPredicate);

			Pair<List<Expression>, List<Expression>> slices =
				Util.slicesBeforeIAndRestWithoutJ(
						expression.getArguments(),
						pair.indexOfFirst, pair.indexOfSecond);

			@SuppressWarnings("unchecked")
			List<Expression> arguments = Util.addAllToANewList(slices.first, Lists.newArrayList(operationResult), slices.second);

			if (noUnaryApplication && arguments.size() == 1) {
				return arguments.get(0);
			}

			AbstractSyntaxTree result = DefaultCompoundSyntaxTree.make(expression.getFunctor(), arguments);
			return result;
		}
		return expression;
	}
	
	public static List<Rewriter> addRewritersBefore(List<Rewriter> rewriters,
			Pair<Class<?>, Rewriter>... rwsBefore) {
		for (int i = 0; i < rwsBefore.length; i++) {
			addRewriterBefore(rewriters, rwsBefore[i].first, rwsBefore[i].second);
		}
		return rewriters;
	}
	
	public static List<Rewriter> addRewriterBefore(List<Rewriter> rewriters,
			Class<?> clazzBefore, Rewriter addR) {
		boolean found = false;
		for (int i = 0; i < rewriters.size(); i++) {
			if (clazzBefore == rewriters.get(i).getClass()) {
				rewriters.add(i, addR);
				found = true;
				break;
			}
		}

		if (!found) {
			throw new IllegalArgumentException(
					"Cannot find rewriter to add new rewriter before:"
							+ clazzBefore.getName());
		}

		return rewriters;
	}
	
	public AbstractRewriter() {
		super();
	}
	
	//
	// START-Rewriter
	@Override
	public String getName() {
		if (name == null) {
			name = Util.camelCaseToSpacedString(getClass().getSimpleName());
		}
		return name;
	}

	@Override
	public Expression rewrite(Expression expression, RewritingProcess process) {
		Expression result   = expression;
		Expression original = expression;
		
		String previousRewriterName = null;
		if (Trace.isEnabled() || Justification.isEnabled()) {
			previousRewriterName = RewriterLogging.setCurrentRewriterName(getName());
		}
		
		if (isTraceInAndOutOfRewriter()) {
			if (Trace.isEnabled()) {
				Trace.in("+"+getName()+"({}) - under context variables = {}, constrained by {}", expression, process.getContextualVariables(), process.getContextualConstraint());
			}
		}
		
		if (process.getRootRewriter() == null) {
			process.setRootRewriter(this);
		}
		Expression preProcessing = process.rewritingPreProcessing(this, expression);
		if (preProcessing != null) {
			result = preProcessing;
		} 
		else if (process.getContextualConstraint().equals(Expressions.FALSE)) {
			result = Rewriter.FALSE_CONTEXTUAL_CONTRAINT_RETURN_VALUE;
		} 
		else {
			result = rewriteAfterBookkeeping(expression, process);

			if (result != original && original == process.getRootExpression()) {
				process.setRootExpression(result);
			}
			process.rewritingPostProcessing(this, original, result);
		}
		
		if (isTraceInAndOutOfRewriter()) {
			if (Trace.isEnabled()) {
				Trace.out(RewriterLogging.REWRITER_PROFILE_INFO, "-"+getName()+"={}", result);
			}
		}
		
		if (Trace.isEnabled() || Justification.isEnabled()) {
			RewriterLogging.setCurrentRewriterName(previousRewriterName);
		}
		
		return result;
	}
	
	@Override
	public Expression rewrite(Expression expression) {
		return rewrite(expression, makeRewritingProcess(expression));
	}

	@Override
	public Expression rewrite(Expression expression, Predicate<Expression> isConstantPredicate) {
		return rewrite(expression, makeRewritingProcess(expression, isConstantPredicate));
	}
	
	@Override
	public Expression rewrite(Expression expression, Map<Object, Object> globalObjects) {
		return rewrite(expression,  makeRewritingProcess(expression, globalObjects));
	}

	@Override
	public Iterator<Rewriter> getChildrenIterator() {
		return _emptyChildList.iterator();
	}
	
	@Override
	public void rewritingProcessInitiated(RewritingProcess process) {
	}

	@Override
	public void rewritingProcessFinalized(RewritingProcess process) {
	}
	
	// END-Rewriter
	//
	
	public abstract Expression rewriteAfterBookkeeping(Expression expression, RewritingProcess process);

	@Override
	public String toString() {
		return Util.camelCaseToSpacedString(getClass().getSimpleName());
	}
	
	//
	// PROTECTED METHODS
	//	
	protected void setName(String name) {
		this.name = name;
	}
	
	protected boolean isTraceInAndOutOfRewriter() {
		return false;
	}
	
	/**
	 * Makes a brand new rewriting process. Extensions may have to override this if they
	 * use specific types of processes.
	 */
	protected DefaultRewritingProcess makeRewritingProcess(Expression expression) {
		return new DefaultRewritingProcess(expression, this);
	}

	/**
	 * Makes a brand new rewriting process. Extensions may have to override this if they
	 * use specific types of processes.
	 */
	protected DefaultRewritingProcess makeRewritingProcess(Expression expression,
			Map<Object, Object> globalObjects) {
		return new DefaultRewritingProcess(expression, this, globalObjects);
	}

	/**
	 * Makes a brand new rewriting process. Extensions may have to override this if they
	 * use specific types of processes.
	 */
	protected DefaultRewritingProcess makeRewritingProcess(
			Expression expression, Predicate<Expression> isConstantPredicate) {
		return new DefaultRewritingProcess(expression, this, isConstantPredicate, new HashMap<Object, Object>());
	}
}
