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
package com.sri.ai.grinder.helper;

import static com.sri.ai.expresso.helper.Expressions.TRUE;
import static com.sri.ai.grinder.library.boole.Not.not;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.sgdpll2.api.Constraint2;
import com.sri.ai.grinder.sgdpll2.api.ContextDependentProblemStepSolver;
import com.sri.ai.util.base.ContinuationIterable;
import com.sri.ai.util.base.ContinuationIterator;

/**
 * A step solver for the context-dependent problem of whether all literals 
 * provided by a continuation iterable
 * are defined by the contextual constraint,
 * returning a {@link ItDepends} object indicating the first found non-defined literal,
 * or a {@link Solution} with {@link Expressions#TRUE} if all are defined.
 * <p>
 * For efficiency, the iterable is required to be a {@link ContinuationIterable}
 * so that the sub-step solvers are incremental (start from where this one left off).
 * <p>
 * When a non-defined literal is found, the step solver returns a {@link ItDependsOn} object
 * with sub-step solvers that will continue the search next time from the next pair,
 * assuming all previous ones are already defined.
 * 
 * @author braz
 *
 */
@Beta
public class ContextDependentDefinedLiteralsStepSolver implements ContextDependentProblemStepSolver {

	private ContinuationIterable<Expression> iterable;
	
	/**
	 * Creates a step solver that checks whether all literals made by a literal maker on ordered pairs
	 * of elements in given literal are already defined by the contextual constraint.
	 * @param list
	 * @param literalMaker
	 */
	public ContextDependentDefinedLiteralsStepSolver(ContinuationIterable<Expression> iterable) {
		this.iterable = iterable;
	}

	@Override
	public ContextDependentProblemStepSolver clone() {
		try {
			return (ContextDependentProblemStepSolver) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error(e.getMessage());
		}
	}

	@Override
	public SolutionStep step(Constraint2 contextualConstraint, RewritingProcess process) {
		ContinuationIterator<Expression> iterator = iterable.iterator();
		while (iterator.hasNext()) {
			Expression literal = iterator.next();
			System.out.println("Checking : " + literal);	
			boolean defined =
					contextualConstraint.implies(literal, process)
					||
					contextualConstraint.implies(not(literal), process);
			if ( ! defined) {
				ContinuationIterable<Expression> iterableFromNowOn = iterator.makeContinuationIterable();
				ContextDependentProblemStepSolver stepSolverFromNowOn = new ContextDependentDefinedLiteralsStepSolver(iterableFromNowOn);
				return new ItDependsOn(literal, stepSolverFromNowOn, stepSolverFromNowOn);
			}
		}
		
		return new Solution(TRUE); // all literals are defined
	}
}