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
package com.sri.ai.grinder.sgdpll.theory.linearrealarithmetic;

import static com.sri.ai.expresso.helper.Expressions.apply;
import static com.sri.ai.expresso.helper.Expressions.makeSymbol;
import static com.sri.ai.grinder.library.FunctorConstants.GREATER_THAN;
import static com.sri.ai.grinder.library.FunctorConstants.GREATER_THAN_OR_EQUAL_TO;
import static com.sri.ai.grinder.library.FunctorConstants.IN;
import static com.sri.ai.grinder.library.FunctorConstants.LESS_THAN;
import static com.sri.ai.grinder.library.FunctorConstants.LESS_THAN_OR_EQUAL_TO;
import static com.sri.ai.grinder.library.set.Sets.EMPTY_SET;
import static com.sri.ai.util.Util.getFirst;
import static com.sri.ai.util.Util.list;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.core.DefaultExtensionalUniSet;
import com.sri.ai.expresso.core.DefaultIntensionalUniSet;
import com.sri.ai.grinder.api.Context;
import com.sri.ai.grinder.library.boole.And;
import com.sri.ai.grinder.sgdpll.theory.numeric.AbstractSingleVariableNumericConstraintFeasibilityRegionStepSolver;

/**
 * A step solver computing the possible values of the variable of
 * a single-variable linear arithmetic constraint.
 * @author braz
 *
 */
@Beta
public class ValuesOfSingleVariableLinearRealArithmeticConstraintStepSolver extends AbstractSingleVariableLinearRealArithmeticConstraintFeasibilityRegionStepSolver {

	public ValuesOfSingleVariableLinearRealArithmeticConstraintStepSolver(SingleVariableLinearRealArithmeticConstraint constraint) {
		super(constraint);
	}
	
	@Override
	public ValuesOfSingleVariableLinearRealArithmeticConstraintStepSolver clone() {
		return (ValuesOfSingleVariableLinearRealArithmeticConstraintStepSolver) super.clone();
	}

	@Override
	protected SolutionStep getSolutionStepAfterBoundsAreCheckedForFeasibility(Expression lowerBound, Expression upperBound, AbstractSingleVariableNumericConstraintFeasibilityRegionStepSolver sequelBase, Context context) {
		Expression variable = getConstraint().getVariable();
		Expression indexExpression = apply(IN, variable, makeSymbol("Real"));

		boolean lowerBoundIsStrict = getMapFromLowerBoundsToStrictness(context).get(lowerBound);
		String lowerBoundOperator = lowerBoundIsStrict? LESS_THAN : LESS_THAN_OR_EQUAL_TO;
		
		boolean upperBoundIsStrict = getMapFromLowerBoundsToStrictness(context).get(upperBound);
		String upperBoundOperator = upperBoundIsStrict? GREATER_THAN : GREATER_THAN_OR_EQUAL_TO;
		
		Expression lowerBoundCondition = apply(lowerBoundOperator, lowerBound, variable);
		Expression upperBoundCondition = apply(upperBoundOperator, variable, upperBound);
		
		Expression condition = And.make(lowerBoundCondition, upperBoundCondition);
		
		Expression result = new DefaultIntensionalUniSet(list(indexExpression), variable, condition);
		
		return new Solution(result);
	}

	@Override
	public boolean unboundedVariableProducesShortCircuitSolution() {
		return false;
	}

	@Override
	public Expression getSolutionExpressionForUnboundedVariables() {
		throw new Error("getSolutionExpressionForUnboundedVariables should not be used because just knowing that the variable is unbounded is not enough to determine value set");
	}

	@Override
	public Expression getSolutionExpressionForBoundVariable() {
		Expression oneOfTheVariableValues = getFirst(getEquals());
		DefaultExtensionalUniSet result = new DefaultExtensionalUniSet(oneOfTheVariableValues);
		return result;
	}

	@Override
	protected Expression getSolutionExpressionGivenContradiction() {
		return EMPTY_SET;
	}
}