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
package com.sri.ai.grinder.api;

import static com.sri.ai.grinder.core.solver.ExpressionStepSolverToLiteralSplitterStepSolverAdapter.toExpressionLiteralSplitterStepSolver;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.core.constraint.ContextSplitting;

/**
 * A {@link StepSolver} specialized for problems with {@link Expression}-typed solutions.
 * <p>
 * Ideally, a {@link Solution} object returned by such a step solver should not contain literals
 * (if it does, then it depends on that literals and therefore that literals should have been returned in a {@link ItDependsOn} solver step.
 * However, this is not currently enforced (TODO: it may be in the future).
 * 
 * @author braz
 *
 */
@Beta
public interface ExpressionStepSolver extends StepSolver<Expression>, Cloneable {

	default Expression solve(Context context) {
		ExpressionLiteralSplitterStepSolver literalSplitterStepSolver = toExpressionLiteralSplitterStepSolver(this);
		Expression result = literalSplitterStepSolver.solve(context);
		return result;
	}
	
	@Override
	ExpressionStepSolver clone();
	
	/**
	 * Returns a solver step for the problem: either the solution itself, if independent
	 * on the values for free variables, or a literal that, if used to split the context,
	 * will bring the problem closer to a solution.
	 * @param context
	 * @return
	 */
	@Override
	Step step(Context context);
	
	public static interface Step extends StepSolver.Step<Expression> {		 
		/**
		 * Returns a {@link ExpressionLiteralSplitterStepSolver} to be used for finding the final solution
		 * in case the literal is defined as true by the context.
		 * This is merely an optimization, and using the original step solver should still work,
		 * but will perform wasted working re-discovering that expressions is already true.
		 * @return
		 */
		@Override
		ExpressionStepSolver getStepSolverForWhenSplitterIsTrue();
		
		/**
		 * Same as {@link #getStepSolverForWhenSplitterIsTrue()} but for when literal is false.
		 * @return
		 */
		@Override
		ExpressionStepSolver getStepSolverForWhenSplitterIsFalse();
	}
	
	public static class ItDependsOn extends StepSolver.ItDependsOn<Expression> implements Step {

		public ItDependsOn(
				Expression formula,
				ContextSplitting contextSplitting,
				ExpressionStepSolver stepSolverIfExpressionIsTrue,
				ExpressionStepSolver stepSolverIfExpressionIsFalse) {
			super(formula, contextSplitting, stepSolverIfExpressionIsTrue, stepSolverIfExpressionIsFalse);
		}
		
		@Override
		public ExpressionStepSolver getStepSolverForWhenSplitterIsTrue() {
			return (ExpressionStepSolver) super.getStepSolverForWhenSplitterIsTrue();
		}
		
		@Override
		public ExpressionStepSolver getStepSolverForWhenSplitterIsFalse() {
			return (ExpressionStepSolver) super.getStepSolverForWhenSplitterIsFalse();
		}
	}
	
	public static class Solution extends StepSolver.Solution<Expression> implements Step {

		public Solution(Expression value) {
			super(value);
		}
		
		public Expression getFormula() {
			throw new Error("Solution does not define getFormula().");
		}
		
		@Override
		public ExpressionStepSolver getStepSolverForWhenSplitterIsTrue() {
			throw new Error("Solution has no sub-step solvers since it does not depend on any expression");
		}

		@Override
		public ExpressionStepSolver getStepSolverForWhenSplitterIsFalse() {
			throw new Error("Solution has no sub-step solvers since it does not depend on any expression");
		}
	}
}