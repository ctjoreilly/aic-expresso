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
package com.sri.ai.grinder.sgdpll2.api;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.sgdpll2.core.solver.ContextDependentProblemSolver;

/**
 * An interface for step-solvers for problems involving free variables constrained by a contextual {@link Constraint2}.
 * The problem may either have the same solution for all free variable assignments under the context, or not.
 * Method {@link #step(Constraint2, RewritingProcess)} returns a {@link SolutionStep},
 * which is either a {@link Solution} with {@link Solution#getExpression()} returning the solution,
 * or a {@link ItDependsOn} with {@link ItDependsOn#getExpression()} returning a literal
 * that, if used to split the contextual constraint
 * (by conjoining the contextual constraint with the literal and with its negation, successively),
 * will help disambiguate the problem.
 * 
 * @author braz
 *
 */
@Beta
public interface ContextDependentProblemStepSolver {

	/**
	 * A solution step of a {@link ContextDependentProblemStepSolver}.
	 * If {@link #itDepends()} returns <code>true</code>, the solution cannot be determined
	 * unless the contextual constraint be restricted according to the literal returned by {@link #getExpression()}.
	 * Otherwise, the expression returned by {@link #getExpression()} is the solution.
	 * @author braz
	 *
	 */
	public static interface SolutionStep {
		boolean itDepends();
		Expression getExpression();
		/**
		 * Returns a {@link ContextDependentProblemStepSolver} to be used for finding the final solution
		 * in case the expression is defined as true by the contextual constraint.
		 * This is merely an optimization, and using the original step solver should still work,
		 * but will perform wasted working re-discovering that expressions is already true.
		 * @return
		 */
		ContextDependentProblemStepSolver getStepSolverForWhenExpressionIsTrue();
		
		/**
		 * Same as {@link #getStepSolverForWhenExpressionIsTrue()} but for when expression is false.
		 * @return
		 */
		ContextDependentProblemStepSolver getStepSolverForWhenExpressionIsFalse();
	}
	
	/**
	 * Returns a solution step for the problem: either the solution itself, if independent
	 * on the values for free variables, or a literal that, if used to split the contextual constraint,
	 * will bring the problem closer to a solution.
	 * @param contextualConstraint
	 * @param process
	 * @return
	 */
	SolutionStep step(Constraint2 contextualConstraint, RewritingProcess process);

	/**
	 * Convenience method invoking
	 * {@link ContextDependentProblemSolver#solve(ContextDependentProblemStepSolver, Constraint2, RewritingProcess)}
	 * on this step solver.
	 * @param contextualConstraint
	 * @param process
	 * @return
	 */
	default Expression solve(Constraint2 contextualConstraint, RewritingProcess process) {
		Expression result = ContextDependentProblemSolver.solve(this, contextualConstraint, process);
		return result;
	}
	
	public static abstract class AbstractSolutionStep implements SolutionStep {

		private Expression expression;
		
		public AbstractSolutionStep(Expression expression) {
			this.expression = expression;
		}
		
		@Override
		public abstract boolean itDepends();

		@Override
		public Expression getExpression() {
			return expression;
		}
	}
	
	public static class ItDependsOn extends AbstractSolutionStep {

		private ContextDependentProblemStepSolver stepSolverIfExpressionIsTrue;
		private ContextDependentProblemStepSolver stepSolverIfExpressionIsFalse;
		
		/**
		 * Represents a solution step in which the final solution depends on the definition of a given expression
		 * by the contextual constraint.
		 * Step solvers specialized for whether expression is true or false can be provided
		 * that already know about the definition of expression either way, for efficiency;
		 * however, if this step solver is provided instead, things still work because 
		 * the step solver will end up determining anyway that expression is now defined and move on.
		 * @param expression
		 * @param stepSolverIfExpressionIsTrue
		 * @param stepSolverIfExpressionIsFalse
		 */
		public ItDependsOn(Expression expression, ContextDependentProblemStepSolver stepSolverIfExpressionIsTrue, ContextDependentProblemStepSolver stepSolverIfExpressionIsFalse) {
			super(expression);
			this.stepSolverIfExpressionIsTrue  = stepSolverIfExpressionIsTrue;
			this.stepSolverIfExpressionIsFalse = stepSolverIfExpressionIsFalse;
		}
		
		@Override
		public boolean itDepends() {
			return true;
		}

		@Override
		public ContextDependentProblemStepSolver getStepSolverForWhenExpressionIsTrue() {
			return stepSolverIfExpressionIsTrue;
		}
		
		@Override
		public ContextDependentProblemStepSolver getStepSolverForWhenExpressionIsFalse() {
			return stepSolverIfExpressionIsFalse;
		}
		
		@Override
		public String toString() {
			return "It depends on " + getExpression();
		}
	}
	
	
	public static class Solution extends AbstractSolutionStep {

		public Solution(Expression expression) {
			super(expression);
		}
		
		@Override
		public boolean itDepends() {
			return false;
		}

		@Override
		public String toString() {
			return getExpression().toString();
		}

		@Override
		public ContextDependentProblemStepSolver getStepSolverForWhenExpressionIsTrue() {
			throw new Error("Solution has no sub-step solvers since it does not depend on any expression");
		}

		@Override
		public ContextDependentProblemStepSolver getStepSolverForWhenExpressionIsFalse() {
			throw new Error("Solution has no sub-step solvers since it does not depend on any expression");
		}
	}
}