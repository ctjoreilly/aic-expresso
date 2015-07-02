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
package com.sri.ai.grinder.plaindpll.core;

import static com.sri.ai.util.Util.myAssert;
import static com.sri.ai.util.Util.throwSafeguardError;

import java.util.Map;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.plaindpll.api.Theory;
import com.sri.ai.grinder.plaindpll.util.DPLLUtil;
import com.sri.ai.util.base.BinaryFunction;

@Beta
/** 
 * Basic implementation of simplification methods of {@link Theory}.
 */
abstract public class AbstractTheory implements Theory {

	protected abstract boolean usesDefaultImplementationOfSimplifyByOverridingGetFunctionApplicationSimplifiersAndGetSyntacticFormTypeSimplifiers();
	
	/**
	 * Provides a map from functors's getValue() values (Strings) to a function mapping a
	 * function application of that functor and a rewriting process to an equivalent, simplified formula
	 * according to this constraintTheoryWithEquality.
	 * Only required if {@link #simplify(Expression, RewritingProcess)} is not overridden by code not using it. 
	 * @return
	 */
	protected Map<String, BinaryFunction<Expression, RewritingProcess, Expression>> getFunctionApplicationSimplifiers() {
		throwSafeguardError( // OPTIMIZATION: much of this, if not all or even extra information, could be obtained by reflection inside throwAppropriateSafeguardError
				getClass().getSimpleName(),
				"getFunctionApplicationSimplifiers",
				"AbstractConstraintTheory",
				"simplify(Expression, RewritingProcess)");
		return null; // never used, as safeguardCheck throws an error no matter what.
	}

	/**
	 * Provides a map from syntactic form types (Strings) to a function mapping a
	 * function application of that functor and a rewriting process to an equivalent, simplified formula
	 * according to this constraintTheoryWithEquality.
	 * Only required if {@link #simplify(Expression, RewritingProcess)} is not overridden by code not using it. 
	 * @return
	 */
	protected Map<String, BinaryFunction<Expression, RewritingProcess, Expression>> getSyntacticFormTypeSimplifiers() {
		throwSafeguardError(
				getClass().getSimpleName(),
				"getSyntacticFormTypeSimplifiers",
				"AbstractConstraintTheory",
				"simplify(Expression, RewritingProcess)");
		return null; // never used, as safeguardCheck throws an error no matter what.
	}

	/**
	 * Default implementation that simplifies an expression by exhaustively simplifying its top expression with
	 * the simplifiers provided by {@link #getFunctionApplicationSimplifiers()} and {@link #getSyntacticFormTypeSimplifiers()},
	 * then simplifying its sub-expressions,
	 * and again exhaustively simplifying its top expression.
	 * @param expression
	 * @param topSimplifier
	 * @param process
	 * @return
	 */
	@Override
	public Expression simplify(Expression expression, RewritingProcess process) {
		myAssert(
				() -> usesDefaultImplementationOfSimplifyByOverridingGetFunctionApplicationSimplifiersAndGetSyntacticFormTypeSimplifiers(),
				() -> getClass() + " is using default implementation of simplify, even though its usesDefaultImplementationOfSimplifyByOverridingGetFunctionApplicationSimplifiersAndGetSyntacticFormTypeSimplifiers method returns false");
		return DPLLUtil.simplify(expression, getFunctionApplicationSimplifiers(), getSyntacticFormTypeSimplifiers(), process);
	}
}