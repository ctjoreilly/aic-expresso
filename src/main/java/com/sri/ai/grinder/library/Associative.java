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
package com.sri.ai.grinder.library;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.annotations.Beta;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.HasKind;
import com.sri.ai.grinder.sgdpll.simplifier.api.TopSimplifier;
import com.sri.ai.util.Util;

/**
 * A rewriter collapsing nested applications of a given iterator into a single
 * one. For example, it can rewrite <code>f(x, f(y,z))</code> into
 * <code>f(x,y,z)</code>.
 * 
 * @author braz
 */
@Beta
public class Associative {

	/** A static version of associate when check for associative operator is done, with predicate indicating whether argument of same functor is to be associated. */
	public static Expression associateWhenSureOperatorIsAssociative(Expression expression) {
		Predicate<Expression> alwaysTrue = Predicates.alwaysTrue();
		Expression result = associateWhenSureOperatorIsAssociative(expression, alwaysTrue);
		return result;
	}

	/** A static version of associate when check for associative operator is done, with predicate indicating whether argument of same functor is to be associated. */
	public static Expression associateWhenSureOperatorIsAssociative(Expression expression, Predicate<Expression> isAssociatable) {
		if (expression.numberOfArguments() == 0) {
			return expression;
		}
		List<Expression> resultArguments = new LinkedList<Expression>();
		Expression functor = expression.getFunctor();
		boolean change = false;
		for (Expression argument : expression.getArguments()) {
			if (argument.hasFunctor(functor) &&
					argument.numberOfArguments() > 1 &&
					isAssociatable.apply(argument)) {
				resultArguments.addAll(argument.getArguments());
				change = true;
			}
			else {
				resultArguments.add(argument);
			}
		}
		
		if (change) {
			return Expressions.makeExpressionOnSyntaxTreeWithLabelAndSubTrees(functor, resultArguments);
		}
		
		return expression;
	}
}
