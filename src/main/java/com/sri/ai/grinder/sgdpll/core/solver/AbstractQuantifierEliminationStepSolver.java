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

import static com.sri.ai.expresso.helper.Expressions.isSubExpressionOf;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.condition;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.elseBranch;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.isIfThenElse;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.thenBranch;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.Context;
import com.sri.ai.grinder.library.controlflow.IfThenElse;
import com.sri.ai.grinder.sgdpll.api.Constraint;
import com.sri.ai.grinder.sgdpll.api.ConstraintTheory;
import com.sri.ai.grinder.sgdpll.api.ContextDependentExpressionProblemStepSolver;
import com.sri.ai.grinder.sgdpll.api.SingleVariableConstraint;
import com.sri.ai.grinder.sgdpll.core.constraint.ConstraintSplitting;
import com.sri.ai.grinder.sgdpll.core.constraint.ContextualConstraintSplitting;
import com.sri.ai.grinder.sgdpll.group.AssociativeCommutativeGroup;
import com.sri.ai.grinder.sgdpll.simplifier.api.Simplifier;
import com.sri.ai.grinder.sgdpll.simplifier.api.TopSimplifier;

/**
 * An abstract implementation for step solvers for quantified expressions
 * (the quantification being based on an associative commutative group's operation).
 * <p>
 * This is done by applying a {@link EvaluatorStepSolver} on the body expression,
 * picking literals in it according to the contextual constraint conjoined with the index constraint,
 * and "intercepting" literals containing the indices and splitting the quantifier
 * based on that, solving the two resulting sub-problems.
 * <p>
 * For example, if we have <code>sum({{ (on X in SomeType) if Y != bob then 2 else 3 | X != john }})</code>
 * under contextual constraint <code>Z = alice</code>,
 * {@link EvaluatorStepSolver#step(Context, Context)} is
 * invoked with contextual constraint <code>Z = alice and X != john</code>.
 * The solution step will depend on literal <code>Y != bob</code>.
 * <p>
 * If however the quantified expression is
 * <code>sum({{ (on X in SomeType) if X != bob then 2 else 3 | X != john }})</code>,
 * the solution step will not be one depending on a literal, but a definite solution equivalent to
 * <code>sum({{ (on X in SomeType) 2 | X != john and X != bob}}) +
 *       sum({{ (on X in SomeType) 3 | X != john and X = bob}})</code>.
 * <p>
 * Because these two sub-problems have literal-free bodies <code>2</code> and <code>3</code>,
 * they will be solved by the extension's
 * {@link #eliminateQuantifierForLiteralFreeBodyAndSingleVariableConstraint(SingleVariableConstraint, Expression, Context, Context)}
 * (which for sums with constant bodies will be equal to the model count of the index constraint
 * under the contextual constraint times the constant).
 * <p>
 * Extending classes must define method
 * {@link #eliminateQuantifierForLiteralFreeBodyAndSingleVariableConstraint(SingleVariableConstraint, Expression, Context, Context)
 * to solve the case in which the body is its given literal-free version,
 * for the given contextual constraint and index constraint.
 * <p>
 * At the time of this writing,
 * {@link EvaluatorStepSolver} supports only expressions that are composed of
 * function applications or symbols only,
 * so this extension inherits this restriction if that is still in place.
 * 
 * @author braz
 *
 */
@Beta
public abstract class AbstractQuantifierEliminationStepSolver implements QuantifierEliminationStepSolver {

	public static boolean useEvaluatorStepSolverIfNotConditioningOnIndexFreeLiteralsFirst = true;
	
	protected AssociativeCommutativeGroup group;
	
	protected SingleVariableConstraint indexConstraint;

	protected Expression body;
	
	private ContextDependentExpressionProblemStepSolver initialBodyEvaluationStepSolver;
	
	protected Simplifier simplifier;

	public AbstractQuantifierEliminationStepSolver(AssociativeCommutativeGroup group, Simplifier simplifier, SingleVariableConstraint indexConstraint, Expression body) {
		this.group = group;
		this.indexConstraint = indexConstraint;
		this.body = body;
		this.simplifier = simplifier;
	}

	/**
	 * Abstract method defining a quantified expression with a given index constraint and literal-free body is to be solved.
	 * @param indexConstraint the index constraint
	 * @param literalFreeBody literal-free body
	 */
	protected abstract SolutionStep eliminateQuantifierForLiteralFreeBodyAndSingleVariableConstraint(
			SingleVariableConstraint indexConstraint,
			Expression literalFreeBody,
			Context contextualConstraint,
			Context context);

	@Override
	public AbstractQuantifierEliminationStepSolver clone() {
		AbstractQuantifierEliminationStepSolver result = null;
		try {
			result = (AbstractQuantifierEliminationStepSolver) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Creates a new version of this object with a new index constraint.
	 * @param newIndexConstraint
	 * @return
	 */
	abstract protected AbstractQuantifierEliminationStepSolver makeWithNewIndexConstraint(SingleVariableConstraint newIndexConstraint);
	
	@Override
	public AssociativeCommutativeGroup getGroup() {
		return group;
	}
	
	@Override
	public SingleVariableConstraint getIndexConstraint() {
		return indexConstraint;
	}
	
	/**
	 * Convenience method for <code>indexConstraint.getConstraintTheory()</code>.
	 * @return
	 */
	@Override
	public ConstraintTheory getConstraintTheory() {
		return indexConstraint.getConstraintTheory();
	}
	
	@Override
	public Expression getIndex() {
		return indexConstraint.getVariable();
	}
	
	private ContextDependentExpressionProblemStepSolver getInitialBodyStepSolver(ConstraintTheory constraintTheory) {
		if (initialBodyEvaluationStepSolver == null) {
			initialBodyEvaluationStepSolver
			= new EvaluatorStepSolver(body, constraintTheory.getTopSimplifier());
		}
		return initialBodyEvaluationStepSolver;
	}

	@Override
	public SolutionStep step(Context contextualConstraint, Context context) {

		if (indexConstraint.isContradiction()) {
			return new Solution(group.additiveIdentityElement());
		}
		
		SolutionStep result;

		Context contextualConstraintForBody = contextualConstraint.conjoin(getIndexConstraint(), context);
		if (contextualConstraintForBody.isContradiction()) {
			result = new Solution(group.additiveIdentityElement()); // any solution is vacuously correct
		}
		else {
			ContextDependentExpressionProblemStepSolver bodyStepSolver = getInitialBodyStepSolver(contextualConstraint.getConstraintTheory());
			SolutionStep bodyStep = bodyStepSolver.step(contextualConstraintForBody, context);

			if (bodyStep.itDepends()) {
				// "intercept" literals containing the index and split the quantifier based on it
				if (isSubExpressionOf(getIndex(), bodyStep.getLiteral())) {
					Expression literalOnIndex = bodyStep.getLiteral();
					result = resultIfLiteralContainsIndex(literalOnIndex, contextualConstraint, context);
				}
				else { // not on index, just pass the expression on which we depend on, but with appropriate sub-step solvers (this, for now)
					AbstractQuantifierEliminationStepSolver subStepSolverForWhenLiteralIsTrue = clone();
					AbstractQuantifierEliminationStepSolver subStepSolverForWhenLiteralIsFalse = clone();
					subStepSolverForWhenLiteralIsTrue.initialBodyEvaluationStepSolver = bodyStep.getStepSolverForWhenLiteralIsTrue();
					subStepSolverForWhenLiteralIsFalse.initialBodyEvaluationStepSolver = bodyStep.getStepSolverForWhenLiteralIsFalse();
					result = new ItDependsOn(bodyStep.getLiteral(), null, subStepSolverForWhenLiteralIsTrue, subStepSolverForWhenLiteralIsFalse);
					// we cannot directly re-use bodyStep.getConstraintSplitting() because it was not obtained from the same contextual constraint,
					// but from the contextual constraint conjoined with the index constraint.
				}
			}
			else { // body is already literal free
				result
				= eliminateQuantifierForLiteralFreeBodyAndSingleVariableConstraint(
						indexConstraint, bodyStep.getValue(), contextualConstraint, context);
			}
		}

		return result;
	}

	private SolutionStep resultIfLiteralContainsIndex(Expression literal, Context contextualConstraint, Context context) {
		
		// if the splitter contains the index, we must split the quantifier:
		// Quant_x:C Body  --->   (Quant_{x:C and L} Body) op (Quant_{x:C and not L} Body)
		
		SolutionStep result;
		ConstraintSplitting split = new ConstraintSplitting(literal, getIndexConstraint(), context);
		Expression solutionValue;
		switch (split.getResult()) {
		case CONSTRAINT_IS_CONTRADICTORY:
			solutionValue = null;
			break;
		case LITERAL_IS_UNDEFINED:
			Expression subSolution1 = solveSubProblem(split.getConstraintAndLiteral(),         contextualConstraint, context);
			Expression subSolution2 = solveSubProblem(split.getConstraintAndLiteralNegation(), contextualConstraint, context);
			solutionValue = combine(subSolution1, subSolution2, contextualConstraint, context);
			break;
		case LITERAL_IS_TRUE:
			solutionValue = solveSubProblem(split.getConstraintAndLiteral(), contextualConstraint, context);
			break;
		case LITERAL_IS_FALSE:
			solutionValue = solveSubProblem(split.getConstraintAndLiteralNegation(), contextualConstraint, context);
			break;
		default: throw new Error("Unrecognized result for " + ConstraintSplitting.class + ": " + split.getResult());
		}

		if (solutionValue == null) {
			result = null;
		}
		else {
			result = new Solution(solutionValue);
		}
		
		return result;
	}

	private Expression solveSubProblem(Constraint newIndexConstraint, Context contextualConstraint, Context context) {
		SingleVariableConstraint newIndexConstraintAsSingleVariableConstraint = (SingleVariableConstraint) newIndexConstraint;
		ContextDependentExpressionProblemStepSolver subProblem = makeWithNewIndexConstraint(newIndexConstraintAsSingleVariableConstraint);
		Expression result = subProblem.solve(contextualConstraint, context);
		return result;
	}

	protected Expression combine(Expression solution1, Expression solution2, Context contextualConstraint, Context context) {
		Expression result;
		if (isIfThenElse(solution1)) {
			// (if C1 then A1 else A2) op solution2 ---> if C1 then (A1 op solution2) else (A2 op solution2)
			ContextualConstraintSplitting split = new ContextualConstraintSplitting(condition(solution1), contextualConstraint, context);
			switch (split.getResult()) {
			case CONSTRAINT_IS_CONTRADICTORY:
				result = null;
				break;
			case LITERAL_IS_UNDEFINED:
				Expression subSolution1 = combine(thenBranch(solution1), solution2, split.getConstraintAndLiteral(), context);
				Expression subSolution2 = combine(elseBranch(solution1), solution2, split.getConstraintAndLiteralNegation(), context);
				result = IfThenElse.make(condition(solution1), subSolution1, subSolution2, true);
				break;
			case LITERAL_IS_TRUE:
				result = combine(thenBranch(solution1), solution2, split.getConstraintAndLiteral(), context);
				break;
			case LITERAL_IS_FALSE:
				result = combine(elseBranch(solution1), solution2, split.getConstraintAndLiteral(), context);
				break;
			default: throw new Error("Unrecognized result for " + ContextualConstraintSplitting.class + ": " + split.getResult());
			}
		}
		else if (isIfThenElse(solution2)) {
			// solution1 op (if C2 then B1 else B2) ---> if C2 then (solution1 op B2) else (solution1 op B2)
			ContextualConstraintSplitting split = new ContextualConstraintSplitting(condition(solution2), contextualConstraint, context);
			switch (split.getResult()) {
			case CONSTRAINT_IS_CONTRADICTORY:
				result = null;
				break;
			case LITERAL_IS_UNDEFINED:
				Expression subSolution1 = combine(solution1, thenBranch(solution2), split.getConstraintAndLiteral(), context);
				Expression subSolution2 = combine(solution1, elseBranch(solution2), split.getConstraintAndLiteralNegation(), context);
				result = IfThenElse.make(condition(solution2), subSolution1, subSolution2, true);
				break;
			case LITERAL_IS_TRUE:
				result = combine(solution1, thenBranch(solution2), split.getConstraintAndLiteral(), context);
				break;
			case LITERAL_IS_FALSE:
				result = combine(solution1, elseBranch(solution2), split.getConstraintAndLiteralNegation(), context);
				break;
			default: throw new Error("Unrecognized result for " + ContextualConstraintSplitting.class + ": " + split.getResult());
			}
		}
		else {
			result = group.add(solution1, solution2, context);
		}
		return result;
	}

	/**
	 * @param expression
	 * @param simplifier
	 * @return
	 */
	public static ContextDependentExpressionProblemStepSolver makeEvaluator(Expression expression, TopSimplifier topSimplifier) {
		ContextDependentExpressionProblemStepSolver evaluator;
		evaluator = new EvaluatorStepSolver(expression, topSimplifier);
		return evaluator;
	}
}