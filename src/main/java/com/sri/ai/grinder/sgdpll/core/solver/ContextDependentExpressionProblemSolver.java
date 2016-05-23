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
package com.sri.ai.grinder.sgdpll.core.solver;

import static com.sri.ai.expresso.helper.Expressions.parse;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.Context;
import com.sri.ai.grinder.library.controlflow.IfThenElse;
import com.sri.ai.grinder.sgdpll.api.ContextDependentExpressionProblemStepSolver;
import com.sri.ai.grinder.sgdpll.api.ContextDependentProblemStepSolver;
import com.sri.ai.grinder.sgdpll.core.constraint.ContextSplitting;
import com.sri.ai.grinder.sgdpll.theory.equality.EqualityConstraintTheory;
import com.sri.ai.grinder.sgdpll.theory.equality.SatisfiabilityOfSingleVariableEqualityConstraintStepSolver;
import com.sri.ai.grinder.sgdpll.theory.equality.SingleVariableEqualityConstraint;

/**
 * Solves a {@link ContextDependentExpressionProblemStepSolver} by successively conditioning the context on provided splitters.
 * 
 * @author braz
 *
 */
@Beta
public class ContextDependentExpressionProblemSolver {
	/**
	 * Returns the solution for a problem using a step solver, or null if the context is found to be inconsistent.
	 * @param stepSolver
	 * @param context
	 * @return
	 */
	public static Expression solve(ContextDependentProblemStepSolver<Expression> stepSolver, Context context) {
		ContextDependentProblemStepSolver.SolutionStep<Expression> step = stepSolver.step(context);
		if (step == null) {
			// context is found to be inconsistent
			return null;
		}
		else if (step.itDepends()) {
			Expression splitter = step.getLiteral();
			ContextSplitting split = (ContextSplitting) step.getContextSplitting();
			switch (split.getResult()) {
			case CONSTRAINT_IS_CONTRADICTORY:
				return null;
			case LITERAL_IS_UNDEFINED:
				Expression subSolution1 = solve(step.getStepSolverForWhenLiteralIsTrue (), split.getConstraintAndLiteral());
				Expression subSolution2 = solve(step.getStepSolverForWhenLiteralIsFalse(), split.getConstraintAndLiteralNegation());
				if (subSolution1 == null || subSolution2 == null) {
					return null;
				}
				else {
					return IfThenElse.make(splitter, subSolution1, subSolution2, true);
				}
			// there is no reason for step solvers to return 'itDepends' steps when the literal is true (or false),
			// because in that case they can always simply add the literal (or its negation) to the context
			// and take another step. However, we include these possibilities here for safety.
			// TODO: At some point it may make sense to simply remove these as valid possibilities.	
			case LITERAL_IS_TRUE: 
				return solve(step.getStepSolverForWhenLiteralIsTrue (), split.getConstraintAndLiteral());
			case LITERAL_IS_FALSE:
				return solve(step.getStepSolverForWhenLiteralIsFalse(), split.getConstraintAndLiteralNegation());
			default:
				throw new Error("Undefined " + ContextSplitting.class + " result value: " + split.getResult());
			}
		}
		else {
			return step.getValue();
		}
	}
	
	public static void main(String[] args) {
		
		EqualityConstraintTheory constraintTheory = new EqualityConstraintTheory(true, true);
		Context context = constraintTheory.makeContextWithTestingInformation();
		SingleVariableEqualityConstraint constraint = new SingleVariableEqualityConstraint(parse("X"), false, constraintTheory);
		constraint = constraint.conjoin(parse("X = Y"), context);
		constraint = constraint.conjoin(parse("X = Z"), context);
		constraint = constraint.conjoin(parse("X != W"), context);
		constraint = constraint.conjoin(parse("X != U"), context);
		
		ContextDependentExpressionProblemStepSolver problem = new SatisfiabilityOfSingleVariableEqualityConstraintStepSolver(constraint);

		Expression result = solve(problem, context);
		
		System.out.println("result: " + result);	
	}
}