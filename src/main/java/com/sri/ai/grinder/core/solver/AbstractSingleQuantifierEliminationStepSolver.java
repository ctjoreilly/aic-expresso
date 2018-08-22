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
package com.sri.ai.grinder.core.solver;

import static com.sri.ai.expresso.helper.Expressions.isSubExpressionOf;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.condition;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.elseBranch;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.isIfThenElse;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.thenBranch;
import static com.sri.ai.util.Util.in;
import static com.sri.ai.util.Util.println;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.Constraint;
import com.sri.ai.grinder.api.Context;
import com.sri.ai.grinder.api.ExpressionLiteralSplitterStepSolver;
import com.sri.ai.grinder.api.ExpressionStepSolver;
import com.sri.ai.grinder.api.SingleQuantifierEliminationProblem;
import com.sri.ai.grinder.api.SingleVariableConstraint;
import com.sri.ai.grinder.api.Theory;
import com.sri.ai.grinder.core.constraint.ConstraintSplitting;
import com.sri.ai.grinder.core.constraint.ContextSplitting;
import com.sri.ai.grinder.group.AssociativeCommutativeGroup;
import com.sri.ai.grinder.helper.AssignmentMapsIterator;
import com.sri.ai.grinder.interpreter.Assignment;
import com.sri.ai.grinder.interpreter.BruteForceCommonInterpreter;
import com.sri.ai.grinder.library.controlflow.IfThenElse;
import com.sri.ai.grinder.rewriter.core.Recursive;

/**
 * An abstract implementation for step solvers for quantified expressions
 * (the quantification being based on an associative commutative group's operation).
 * <p>
 * This is done by applying an evaluator step solver on the body expression,
 * picking literals in it according to the context conjoined with the index constraint,
 * and "intercepting" literals containing the indices and splitting the quantifier
 * based on that, solving the two resulting sub-problems.
 * <p>
 * For example, if we have <code>sum({{ (on X in SomeType) if Y != bob then 2 else 3 | X != john }})</code>
 * under context <code>Z = alice</code>,
 * the evaluator step solver is
 * invoked with context <code>Z = alice and X != john</code>.
 * The solver step will depend on literal <code>Y != bob</code>.
 * <p>
 * If however the quantified expression is
 * <code>sum({{ (on X in SomeType) if X != bob then 2 else 3 | X != john }})</code>,
 * the solver step will not be one depending on a literal, but a definite solution equivalent to
 * <code>sum({{ (on X in SomeType) 2 | X != john and X != bob}}) +
 *       sum({{ (on X in SomeType) 3 | X != john and X = bob}})</code>.
 * <p>
 * Because these two sub-problems have literal-free bodies <code>2</code> and <code>3</code>,
 * they will be solved by the extension's
 * {@link #eliminateQuantifierForLiteralFreeBody(Expression, Context)}
 * (which for sums with constant bodies will be equal to the model count of the index constraint
 * under the context times the constant).
 * <p>
 * Extending classes must define method
 * {@link #eliminateQuantifierForLiteralFreeBody(Expression, Context)
 * to solve the case in which the body is its given literal-free version,
 * for the given context and index constraint.
 * <p>
 * At the time of this writing,
 * {@link Recursive} (on which evaluator step solvers are based)
 * supports only expressions that are composed of
 * function applications or symbols only,
 * so this extension inherits this restriction if that is still in place.
 * <p>
 * Ideally, step solvers should only return literal-free solutions, but
 * this abstract implementation currently may do that.
 * This may (or perhaps should) be changed in the future.
 * 
 * @author braz
 *
 */
@Beta
public abstract class AbstractSingleQuantifierEliminationStepSolver implements SingleQuantifierEliminationStepSolver {

	private SingleQuantifierEliminationProblem problem;
	
	private ExpressionLiteralSplitterStepSolver initialBodyEvaluationStepSolver;
	
	private Context initialContextForBody;
	
	/**
	 * Key for {@link Context} global object indicating whether to compare non-conditional solutions with the result provided by {@link BruteForceCommonInterpreter};
	 * check is done if key is present.
	 */
	public static final String BRUTE_FORCE_CHECKING_OF_NON_CONDITIONAL_PROBLEMS = "Brute force checking of non-conditional problems";

	public AbstractSingleQuantifierEliminationStepSolver(SingleQuantifierEliminationProblem problem) {
		this.problem = problem;
	}

	/**
	 * Abstract method defining a quantified expression with a given index constraint and literal-free body is to be solved.
	 * @param literalFreeBody literal-free body
	 */
	protected abstract Step eliminateQuantifierForLiteralFreeBody(
			Expression literalFreeBody,
			Context context);

	private ExpressionLiteralSplitterStepSolver getInitialBodyStepSolver(Theory theory) {
		if (initialBodyEvaluationStepSolver == null) {
			initialBodyEvaluationStepSolver = theory.makeEvaluatorStepSolver(getBody());
		}
		return initialBodyEvaluationStepSolver;
	}

	private Context getContextForBody(Context context) {
		Context result;
		if (initialContextForBody == null) {
			result = context.conjoin(getIndexConstraint(), context);
		}
		else {
			result = initialContextForBody;
		}
		return result;
	}

	@Override
	public Step step(Context context) {
		
		Step result;

		Context contextForBody = getContextForBody(context);
		
		if (contextForBody.isContradiction()) {
			result = new Solution(getGroup().additiveIdentityElement());
		}
		else {
			result = solveProblemWithConsistentContextForBody(contextForBody, context);
		}
		
		bruteForceCheckingOfNonConditionalProblemsIfRequested(result, context);

		return result;
	}

	private Step solveProblemWithConsistentContextForBody(Context contextForBody, Context context) {
		Step result;
		ExpressionLiteralSplitterStepSolver.Step bodyStep = getBodyStep(contextForBody, context);
		if (bodyStep.itDepends()) {
			result = solveConditionalProblem(context, contextForBody, bodyStep);
		}
		else {
			result = solveNonConditionalProblem(bodyStep, context);
		}
		return result;
	}

	private ExpressionLiteralSplitterStepSolver.Step getBodyStep(Context contextForBody, Context context) {
		ExpressionLiteralSplitterStepSolver bodyStepSolver = getInitialBodyStepSolver(context.getTheory());
		ExpressionLiteralSplitterStepSolver.Step bodyStep = bodyStepSolver.step(contextForBody); 
		
		// At this point, bodyStep may be a non-conditional solver step
		// that nonetheless contains literals (we will probably prohibit step solvers from returning such "solutions" in the future).
		// If one of these literals is the quantifier index, we *must* detect it.
		// Therefore, we run EvaluatorStepSolver on it to make sure to detect literals before going on.
		//
		// One may ask: if the body is solved using an EvaluatorStepSolver,
		// why is it that running *another* EvaluatorStepSolver on its result will
		// now guarantee that literals are detected?
		// Why do we get the guarantee only when running it a second time?
		// The answer lies in the fact that EvaluatorStepSolver returns solutions with literals
		// because *this* class (which EvaluatorStepSolver uses to eliminate quantifiers)
		// does so. Once all quantifiers are eliminated,
		// EvaluatorStepSolver does no longer return such solutions.
		// The solution to this whole situation is to change *this* class
		// so it does not return solutions with literals any longer.
		// This happens in quantifier splits, when the two sub-solutions
		// are computed with an exhaustive solve,
		// which may return solutions with literals
		// (it is only the step solvers that we want to prevent from doing this,
		// not exhaustive solving).
		// Check (**) in this file to see where this happens
		if ( ! bodyStep.itDepends()) {
			ExpressionLiteralSplitterStepSolver evaluatorStepSolver = context.getTheory().makeEvaluatorStepSolver(bodyStep.getValue());
			bodyStep = evaluatorStepSolver.step(context);
		}
		return bodyStep;
	}

	private Step solveConditionalProblem(Context context, Context contextForBody,
			ExpressionLiteralSplitterStepSolver.Step bodyStep) {
		Step result;
		if (isSubExpressionOf(getIndex(), bodyStep.getSplitterLiteral())) {
			result = splitQuantifierOnLiteralContainingIndex(bodyStep, contextForBody, context);
		}
		else {
			result = splitOnNonIndexVariable(bodyStep, context);
		}
		return result;
	}

	private Step splitOnNonIndexVariable(ExpressionLiteralSplitterStepSolver.Step bodyStep, Context context) {

		AbstractSingleQuantifierEliminationStepSolver ifTrue = makeStepSolverIfTrue(bodyStep);
		AbstractSingleQuantifierEliminationStepSolver ifFalse = makeStepSolverIfFalse(bodyStep);
		ContextSplitting split = makeOriginalContextSplitting(bodyStep, context);

		Step result = new ItDependsOn(bodyStep.getSplitterLiteral(), split, ifTrue, ifFalse);
		
		return result;
	}

	private AbstractSingleQuantifierEliminationStepSolver makeStepSolverIfTrue(ExpressionLiteralSplitterStepSolver.Step bodyStep) {
		AbstractSingleQuantifierEliminationStepSolver ifTrue = clone();
		ifTrue.initialBodyEvaluationStepSolver  = bodyStep.getStepSolverForWhenSplitterIsTrue();
		ifTrue.initialContextForBody  = bodyStep.getContextSplittingWhenSplitterIsLiteral().getContextAndLiteral();
		return ifTrue;
	}

	private AbstractSingleQuantifierEliminationStepSolver makeStepSolverIfFalse(ExpressionLiteralSplitterStepSolver.Step bodyStep) {
		AbstractSingleQuantifierEliminationStepSolver ifFalse = clone();
		ifFalse.initialBodyEvaluationStepSolver = bodyStep.getStepSolverForWhenSplitterIsFalse();
		ifFalse.initialContextForBody = bodyStep.getContextSplittingWhenSplitterIsLiteral().getContextAndLiteralNegation();
		return ifFalse;
	}

	private ContextSplitting makeOriginalContextSplitting(ExpressionLiteralSplitterStepSolver.Step bodyStep, Context context) {
		// to compute the result's constraint splitting,
		// we cannot directly re-use bodyStep.getConstraintSplitting() because it was not obtained from
		// the context it is returning to,
		// but from the context conjoined with the index constraint.
		// In order to provide two contexts to work with the sequel step solvers,
		// we calculate the splittings here.
		// TODO: In the future, we expect it to be possible to efficiently extract the contextForBody component relative
		// to the original context only, excluding the index.
		ContextSplitting split = new ContextSplitting(bodyStep.getSplitterLiteral(), context);
		return split;
	}

	private Step solveNonConditionalProblem(ExpressionLiteralSplitterStepSolver.Step bodyStep, Context context) {

		Expression literalFreeBody = bodyStep.getValue();
		Step result = eliminateQuantifierForLiteralFreeBody(literalFreeBody, context);
		
		registerGroupIntegration(literalFreeBody, result, context);
		
		return result;
	}

	private void registerGroupIntegration(Expression literalFreeBody, Step result, Context context) {
		boolean solutionToQuantifiedLiteralFreeBodyIsNotConditionalItself = !result.itDepends(); 
		if (solutionToQuantifiedLiteralFreeBodyIsNotConditionalItself) {
			IntegrationRecording.registerGroupIntegration(problem, literalFreeBody, result, context);
		}
	}

	protected Step splitQuantifierOnLiteralContainingIndex(ExpressionLiteralSplitterStepSolver.Step bodyStep, Context contextForBody, Context context) {
		// if the splitter contains the index, we must split the quantifier:
		// Quant_x:C Body  --->   (Quant_{x:C and L} Body) op (Quant_{x:C and not L} Body)
		ConstraintSplitting indexConstraintSplitting = computeIndexConstraintSplitting(bodyStep, context);
		Step result = computeSolution(bodyStep, indexConstraintSplitting, context);
		return result;
	}

	private ConstraintSplitting computeIndexConstraintSplitting(ExpressionLiteralSplitterStepSolver.Step bodyStep,
			Context context) {
		// Here, we need to obtain the new index constraints, for the case in which the splitter literal is true and false,
		// to create the corresponding sub-problems, solve them, and combine them.
		// However, it is important to remember that bodyStep.getContextSplittingWhenSplitterIsLiteral()
		// contains the splitting of contextForBody with the splitter literal,
		// so the information on the new index constraints is already there in some form.
		// TODO: We current don't have a Constraint-generic way to extract it, but expect to do it in the future.
		// For now, we split the index constraint separately
		Expression literal = bodyStep.getSplitterLiteral();
		ConstraintSplitting indexConstraintSplitting = new ConstraintSplitting(literal, getIndexConstraint(), context);
		return indexConstraintSplitting;
	}

	private Step computeSolution(
			ExpressionLiteralSplitterStepSolver.Step bodyStep,
			ConstraintSplitting indexConstraintSplitting, 
			Context context) {
		
		Expression solutionValue = computeSolutionValue(bodyStep, indexConstraintSplitting, context);
		Step result = makeSolution(solutionValue);
		return result;
	}

	private Expression computeSolutionValue(ExpressionLiteralSplitterStepSolver.Step bodyStep,
			ConstraintSplitting indexConstraintSplitting, Context context) throws Error {
		Constraint indexConstraintAndLiteral = indexConstraintSplitting.getConstraintAndLiteral();
		Constraint indexConstraintAndLiteralNegation = indexConstraintSplitting.getConstraintAndLiteralNegation();
		
		Expression solutionValue;
		switch (indexConstraintSplitting.getResult()) {
		case CONSTRAINT_IS_CONTRADICTORY:
			solutionValue = null;
			break;
		case LITERAL_IS_UNDEFINED:
			solutionValue = solveSubProblemIfSplitterIsUndefined(bodyStep, indexConstraintAndLiteral, indexConstraintAndLiteralNegation, context);
			break;
		case LITERAL_IS_TRUE:
			solutionValue = solveSubProblemIfSplitterIsTrue(bodyStep, indexConstraintAndLiteral, context);
			break;
		case LITERAL_IS_FALSE:
			solutionValue = solveSubProblemIfSplitterIsFalse(bodyStep, indexConstraintAndLiteralNegation, context);
			break;
		default: throw new Error("Unrecognized result for " + ConstraintSplitting.class + ": " + indexConstraintSplitting.getResult());
		}
		return solutionValue;
	}

	private Expression solveSubProblemIfSplitterIsUndefined(
			ExpressionLiteralSplitterStepSolver.Step bodyStep,
			Constraint indexConstraintAndLiteral, 
			Constraint indexConstraintAndLiteralNegation, 
			Context context) {
		
		// (**) IF DELETING THIS MARKER, DELETE ALL THE REFERENCES TO IT IN THIS FILE
		// This is where this step solver may return a Solution with literals in it:
		// solveSubProblem uses an exhaustive solve.
		ExpressionStepSolver subProblemIfSplitterIsTrue = makeSubProblem(true, bodyStep, indexConstraintAndLiteral);
		ExpressionStepSolver subProblemIfSplitterIsFalse = makeSubProblem(false, bodyStep, indexConstraintAndLiteralNegation);
		Expression solutionValue = solveSubProblems(subProblemIfSplitterIsTrue, subProblemIfSplitterIsFalse, context);
		return solutionValue;
	}

	private Expression solveSubProblemIfSplitterIsTrue(
			ExpressionLiteralSplitterStepSolver.Step bodyStep, 
			Constraint indexConstraintAndLiteral, 
			Context context) {
		
		ExpressionStepSolver subProblemIfSplitterIsTrue = makeSubProblem(true, bodyStep, indexConstraintAndLiteral);
		Expression solutionValue = solveSubProblem(subProblemIfSplitterIsTrue, context);
		return solutionValue;
	}

	private Expression solveSubProblemIfSplitterIsFalse(
			ExpressionLiteralSplitterStepSolver.Step bodyStep,
			Constraint indexConstraintAndLiteralNegation, 
			Context context) {
		
		ExpressionStepSolver subProblemIfSplitterIsFalse = makeSubProblem(false, bodyStep, indexConstraintAndLiteralNegation);
		Expression solutionValue = solveSubProblem(subProblemIfSplitterIsFalse, context);
		return solutionValue;
	}

	private Step makeSolution(Expression solutionValue) {
		Step result = solutionValue == null? null : new Solution(solutionValue);
		return result;
	}
	
	protected Expression solveSubProblems(ExpressionStepSolver subProblem1, ExpressionStepSolver subProblem2, Context context) {
		// (**) IF DELETING THIS MARKER, DELETE ALL THE REFERENCES TO IT IN THIS FILE
		// This is where this step solver may return a Solution with literals in it:
		// solveSubProblem uses an exhaustive solve.
		Expression subSolution1 = solveSubProblem(subProblem1, context);
		Expression subSolution2 = solveSubProblem(subProblem2, context);
		Expression result = combine(subSolution1, subSolution2, context);
		return result;
	}

	protected Expression solveSubProblem(ExpressionStepSolver subProblem, Context context) {	
		Expression result = subProblem.solve(context);
		// (**) IF DELETING THIS, DELETE ALL OTHER OCCURRENCES IN THIS FILE
		// The above code line is the exhaustive solve mentioned in other occurrences of (**)
		return result;
	}
	
	protected ExpressionStepSolver makeSubProblem(boolean splitterValue, ExpressionLiteralSplitterStepSolver.Step bodyStep, Constraint newIndexConstraint) {

		SingleVariableConstraint newIndexConstraintAsSingleVariableConstraint = (SingleVariableConstraint) newIndexConstraint;
		AbstractSingleQuantifierEliminationStepSolver result = makeWithNewIndexConstraint(newIndexConstraintAsSingleVariableConstraint);
		result.initialBodyEvaluationStepSolver =
				splitterValue
				? bodyStep.getStepSolverForWhenSplitterIsTrue() 
				: bodyStep.getStepSolverForWhenSplitterIsFalse();
		result.initialContextForBody = 
				splitterValue
				? bodyStep.getContextSplittingWhenSplitterIsLiteral().getConstraintAndLiteral() 
				: bodyStep.getContextSplittingWhenSplitterIsLiteral().getConstraintAndLiteralNegation();
		return result;
	}

	protected Expression combine(Expression solution1, Expression solution2, Context context) {
		Expression result;
		if (isIfThenElse(solution1)) {
			result = addSolution1AndSolution2GivenSolution1IsConditional(solution1, solution2, context);
		}
		else if (isIfThenElse(solution2)) {
			result = addSolution1AndSolution2GivenSolution2IsConditional(solution1, solution2, context);
		}
		else {
			result = addNonConditionalSolutions(solution1, solution2, context);
		}
		return result;
	}

	private Expression addSolution1AndSolution2GivenSolution1IsConditional(Expression solution1, Expression solution2, Context context) {
		// (if C1 then A1 else A2) op solution2 ---> if C1 then (A1 op solution2) else (A2 op solution2)
		Expression result;
		ContextSplitting split = new ContextSplitting(condition(solution1), context);
		switch (split.getResult()) {
		case CONSTRAINT_IS_CONTRADICTORY:
			result = null;
			break;
		case LITERAL_IS_UNDEFINED:
			Expression subSolution1 = combine(thenBranch(solution1), solution2, split.getContextAndLiteral());
			Expression subSolution2 = combine(elseBranch(solution1), solution2, split.getContextAndLiteralNegation());
			result = IfThenElse.make(condition(solution1), subSolution1, subSolution2, true);
			break;
		case LITERAL_IS_TRUE:
			result = combine(thenBranch(solution1), solution2, split.getContextAndLiteral());
			break;
		case LITERAL_IS_FALSE:
			result = combine(elseBranch(solution1), solution2, split.getContextAndLiteralNegation());
			break;
		default: throw new Error("Unrecognized result for " + ContextSplitting.class + ": " + split.getResult());
		}
		return result;
	}

	private Expression addSolution1AndSolution2GivenSolution2IsConditional(Expression solution1, Expression solution2, Context context) {
		// solution1 op (if C2 then B1 else B2) ---> if C2 then (solution1 op B2) else (solution1 op B2)
		Expression result;
		ContextSplitting split = new ContextSplitting(condition(solution2), context);
		switch (split.getResult()) {
		case CONSTRAINT_IS_CONTRADICTORY:
			result = null;
			break;
		case LITERAL_IS_UNDEFINED:
			Expression subSolution1 = combine(solution1, thenBranch(solution2), split.getContextAndLiteral());
			Expression subSolution2 = combine(solution1, elseBranch(solution2), split.getContextAndLiteralNegation());
			result = IfThenElse.make(condition(solution2), subSolution1, subSolution2, true);
			break;
		case LITERAL_IS_TRUE:
			result = combine(solution1, thenBranch(solution2), split.getContextAndLiteral());
			break;
		case LITERAL_IS_FALSE:
			result = combine(solution1, elseBranch(solution2), split.getContextAndLiteralNegation());
			break;
		default: throw new Error("Unrecognized result for " + ContextSplitting.class + ": " + split.getResult());
		}
		return result;
	}

	private Expression addNonConditionalSolutions(Expression solution1, Expression solution2, Context context) {
		Expression result = getGroup().add(solution1, solution2, context);
		return result;
	}

	@Override
	public AbstractSingleQuantifierEliminationStepSolver clone() {
		AbstractSingleQuantifierEliminationStepSolver result = null;
		try {
			result = (AbstractSingleQuantifierEliminationStepSolver) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public SingleQuantifierEliminationProblem getProblem() {
		return problem;
	}

	@Override
	public AssociativeCommutativeGroup getGroup() {
		return problem.getGroup();
	}

	@Override
	public SingleVariableConstraint getIndexConstraint() {
		return (SingleVariableConstraint) problem.getConstraint();
	}

	@Override
	public Expression getIndex() {
		return problem.getIndex();
	}

	@Override
	public Expression getBody() {
		return problem.getBody();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " on " + problem;
	}

	protected
	AbstractSingleQuantifierEliminationStepSolver makeWithNewIndexConstraint(SingleVariableConstraint newIndexConstraint) {
		try {
			SingleQuantifierEliminationProblem newProblem = getProblem().makeWithNewIndexConstraint(newIndexConstraint);
			AbstractSingleQuantifierEliminationStepSolver result = getClass().getConstructor(SingleQuantifierEliminationProblem.class).newInstance(newProblem);
			return result;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new Error(e);
		} 
	}

	private void bruteForceCheckingOfNonConditionalProblemsIfRequested(Step result, Context context) throws Error {
		if (context.getGlobalObject(BRUTE_FORCE_CHECKING_OF_NON_CONDITIONAL_PROBLEMS) != null) {
			if ( ! result.itDepends()) {
				Expression indexType = context.getTypeExpressionOfRegisteredSymbol(getIndex());
				SingleQuantifierEliminationProblem problem = new DefaultSingleQuantifierEliminationProblem(getGroup(), getIndex(), indexType, getIndexConstraint(), getBody());
				Expression problemExpression = problem.toExpression();
				Set<Expression> freeVariables = Expressions.freeVariables(problemExpression, context);
				AssignmentMapsIterator assignments = new AssignmentMapsIterator(freeVariables, context);
				for (Map<Expression, Expression> assignment : in(assignments)) {
					BruteForceCommonInterpreter bruteForceCommonInterpreter = new BruteForceCommonInterpreter();
					Context extendedContext = Assignment.extendAssignments(assignment, context);
					// Only go on if the assignment satisfies the context:
					if (bruteForceCommonInterpreter.apply(context, extendedContext).equals(Expressions.TRUE)) {
						Expression bruteForceResult = bruteForceCommonInterpreter.apply(problemExpression, extendedContext);
						Expression resultGivenAssignment = bruteForceCommonInterpreter.apply(result.getValue(), extendedContext);
						Expression evaluatedProblem = bruteForceCommonInterpreter.apply(problemExpression, extendedContext);
						if ( ! bruteForceResult.equals(resultGivenAssignment)) {
							String message = 
									"Disagreement on " + problemExpression + "\nunder " + assignment + ".\n"
											+ "Context: " + context + ".\n"
											+ "Evaluated problem: " + evaluatedProblem + ".\n"
											+ "Brute force says " + bruteForceResult + ", symbolic says " + resultGivenAssignment;
							println(message);
							throw new Error(message);
						}
						else {
							String message = 
									"Agreement on " + problemExpression + "\nunder " + assignment + ".\n"
											+ "Context: " + context + ".\n"
											+ "Evaluated problem: " + evaluatedProblem + ".\n"
											+ "Brute force says " + bruteForceResult + ", symbolic says " + resultGivenAssignment;
							println(message);
						}
					}
				}
			}
		}
	}
}