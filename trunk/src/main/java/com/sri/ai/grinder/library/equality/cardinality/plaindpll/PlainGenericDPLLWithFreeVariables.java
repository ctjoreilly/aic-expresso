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
package com.sri.ai.grinder.library.equality.cardinality.plaindpll;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.expresso.helper.SubExpressionsDepthFirstIterator;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.AbstractHierarchicalRewriter;
import com.sri.ai.grinder.library.controlflow.IfThenElse;
import com.sri.ai.util.base.Pair;

/**
 * A "plain" implementation of DPLL (without using Grinder-style contexts and simplifications)
 * that is more ad hoc than rewriter-based approaches but also much faster.
 * 
 * This generic skeleton allows the algorithm to be used for different tasks.
 * 
 * @author braz
 *
 */
public abstract class PlainGenericDPLLWithFreeVariables extends AbstractHierarchicalRewriter {

	/*
	 * Implementation notes.
	 * 
	 * Pseudo-code for version without free variables and for model counting
	 * (actual code allows methods to be overridden for different problems):
	 * 
	 * count(F, C, I): // assume F simplified in itself and with respect to C
	 * 
	 * if F is false, return 0
	 *     
	 * pick atom X = T, T a variable or constant, from formula or constraint  // linear in F size
	 * if no atom
	 *     return | C |_I // linear in number of symbols
	 * else
	 *     C1 = C[X/T] // linear in F size
	 *     C2 = C and X != T // amortized constant
	 *     F'  = simplify(F[X/T])
	 *     F'' = simplify(F with "X != T" replaced by true and "X = T" replaced by false)
	 *     return count( F' , C1, I - {X} ) + count( F'', C2 )
	 * 
	 * simplify must exploit short circuits and eliminate atoms already determined in the contextual constraint.
	 * 
	 * To compute | C |_I
	 * 
	 * C is a conjunction of disequalities, but represented in a map data structure for greater efficiency.
	 * Assume a total ordering of variables (we use alphabetical order)
	 * C is represented as a map that is null if C is false,
	 * or that maps each variable V to the set diseq(V) of terms it is constrained to be distinct,
	 * excluding variables V' > V.
	 * The idea is that values for variables are picked in a certain order and
	 * we exclude values already picked for variables constrained to be distinct and constants.
	 * Now,
	 * solution = 1
	 * For each variable V according to the total ordering
	 *     solution *= ( |type(V)| - |diseq(V)| )
	 * return solution
	 * 
	 * Free variables require a few additions.
	 * They are assumed to always come before indices in the general ordering,
	 * because they are assumed to have a fixed (albeit unknown) value.
	 * If the splitter atom contains a free variable and not an index, we condition on it but combine
	 * the results in an if-then-else condition, with the splitter as condition.
	 * Otherwise (the splitter is on an index), the recursive results must be summed.
	 * However, now the recursive results can be conditional themselves,
	 * which requires merging their trees.
	 * When the splitter atom is picked and it contains a free variable and an index (as second argument),
	 * we invert it to make sure the index (if there is one) is the variable to be eliminated.
	 */
	
	///////////////////////////// BEGINNING OF ABSTRACT METHODS //////////////////////////////////////

	/**
	 * Derives formula and indices to be used from intensional set passed to rewriter as argument.
	 */
	abstract protected Pair<Expression, List<Expression>> getFormulaAndIndicesFromRewriterProblemArgument(Expression set, RewritingProcess process);

	/**
	 * Defines the solution the combination of which with any other solution S produces S itself (for example, 0 in model counting and false in satisfiability).
	 */
	protected abstract Expression bottomSolution();

	/**
	 * Indicates whether given solution for a sub-problem makes the other sub-problem for an index conditioning irrelevant
	 * (for example, the solution 'true' in satisfiability, or the total number of possible models, in model counting).
	 */
	protected abstract boolean isTopSolution(Expression solutionForSubProblem);
	
	/**
	 * Combines two unconditional solutions for split sub-problems
	 * (for example, disjunction of two boolean constants in satisfiability, or addition in model counting).
	 */
	protected abstract Expression combineUnconditionalSolutions(Expression solution1, Expression solution2);

	/**
	 * Converts a conjunction of atoms into the equivalent {@link #TheoryConstraint} object.
	 */
	protected abstract TheoryConstraint makeConstraint(Expression atomsConjunction, Collection<Expression> indices, RewritingProcess process);

	/**
	 * Returns given subExpression (or an atom equivalent to it) if it is appropriate to be a DPLL splitter atom,
	 * or <code>null</code> otherwise.
	 */
	protected abstract Expression expressionIsSplitterCandidate(Expression subExpression, Collection<Expression> indices, RewritingProcess process);
	
	/**
	 * Returns simplification of formula given splitter.
	 */
	protected abstract Expression applySplitterTo(Expression formula, Expression splitter, RewritingProcess process);

	/**
	 * Returns simplification of formula given splitter negation.
	 */
	protected abstract Expression applySplitterNegationTo(Expression formula, Expression splitter, RewritingProcess process);

	protected abstract Expression completeSimplifySolutionGivenSplitter(Expression solution, Expression splitter, RewritingProcess process);

	protected abstract Expression completeSimplifySolutionGivenSplitterNegation(Expression solution, Expression splitter, RewritingProcess process);

	/**
	 * Returns indices to be used given splitter.
	 */
	protected abstract Collection<Expression> getIndicesUnderSplitter(Expression splitter, Collection<Expression> indices);
	
	/**
	 * Returns indices to be used given splitter negation.
	 */
	protected abstract Collection<Expression> getIndicesUnderSplitterNegation(Expression splitter, Collection<Expression> indices);
	
	abstract protected boolean splitterIsOnFreeVariablesOnly(Expression splitter, Collection<Expression> indices);

	///////////////////////////// END OF ABSTRACT METHODS //////////////////////////////////////

	///////////////////////////// BEGINNING OF FIXED PART OF GENERIC DPLL //////////////////////////////////////
	
	@Override
	public Expression rewriteAfterBookkeeping(Expression expression, RewritingProcess process) {
		// rewriter gets | { (on I) ... | formula } |
		Expression set = expression.get(0);
		Pair<Expression, List<Expression>> formulaAndIndices = getFormulaAndIndicesFromRewriterProblemArgument(set, process);
//		RewritingProcess subProcess = GrinderUtil.extendContextualSymbolsAndConstraintWithIntensionalSet(set, process);
		RewritingProcess subProcess = process;
		Expression result = solve(formulaAndIndices.first, Expressions.TRUE, formulaAndIndices.second, subProcess);
		return result;
	}

	/**
	 * Returns the solution of a problem under a given constraint, which must be a conjunction of atoms, or true.
	 * Assumes it is already simplified with respect to constraint.
	 */
	private Expression solve(Expression formula, Expression constraintConjunctionOfAtoms, Collection<Expression> indices, RewritingProcess process) {
		TheoryConstraint constraint = makeConstraint(constraintConjunctionOfAtoms, indices, process);
		Expression result = solve(formula, constraint, indices, process);
		return result;
	}

	/**
	 * Solves a problem for the given formula.
	 * Assumes it is already simplified with respect to context represented by constraint.
	 */
	private Expression solve(Expression formula, TheoryConstraint constraint, Collection<Expression> indices, RewritingProcess process) {
		
		Expression result = null;
		
		if (formula.equals(Expressions.FALSE) || constraint == null) {
			result = bottomSolution();
		}
		else {
			Expression splitter = pickAtomFromFormula(formula, indices, process);
			if (splitter == null) { // formula is 'true'
				// check if we need to split in order for constraint to get ready to yield solution (such as providing number of solutions
				splitter = constraint.pickAtom(indices, process);
				// the splitting stops only when the formula has no atoms, *and* when the constraint satisfies some necessary conditions
				if (splitter == null) {
					// formula is 'true' and constraint is ready to yield solution
					result = constraint.solution(indices, process);
				}
			}
	
			if (splitter != null) { // it is tempting to make this an else to if(splitter == null) but that would not work because it needs to be an else for the inner if as well.
				Expression solutionUnderSplitter = solveUnderSplitter(splitter, formula, constraint, indices, process);
	
				boolean conditionIsOnFreeVariablesOnly = splitterIsOnFreeVariablesOnly(splitter, indices);
	
				if ( ! conditionIsOnFreeVariablesOnly && isTopSolution(solutionUnderSplitter)) {
					result = solutionUnderSplitter;
				}
				else {
					Expression solutionUnderSplitterNegation = solveUnderSplitterNegation(splitter, formula, constraint, indices, process);
	
					if (conditionIsOnFreeVariablesOnly) {
						result = IfThenElse.make(splitter, solutionUnderSplitter, solutionUnderSplitterNegation);
					}
					else {
						result = combineSymbolicResults(solutionUnderSplitter, solutionUnderSplitterNegation, process);
					}
				}
			}
		}
		
		return result;
	}

	protected Expression solveUnderSplitter(Expression splitter, Expression formula, TheoryConstraint constraint, Collection<Expression> indices, RewritingProcess process) {
		Expression formulaUnderSplitter = applySplitterTo(formula, splitter, process);
		TheoryConstraint constraintUnderSplitter = constraint.applySplitter(splitter, indices, process);
		Collection<Expression> indicesUnderSplitter = getIndicesUnderSplitter(splitter, indices);
		Expression result = solve(formulaUnderSplitter, constraintUnderSplitter, indicesUnderSplitter, process);
		return result;
	}

	protected Expression solveUnderSplitterNegation(Expression splitter, Expression formula, TheoryConstraint constraint, Collection<Expression> indices, RewritingProcess process) {
		Expression formulaUnderSplitterNegation = applySplitterNegationTo(formula, splitter, process);
		TheoryConstraint constraintUnderSplitterNegation = constraint.applySplitterNegation(splitter, indices, process);
		Collection<Expression> indicesUnderSplitterNegation = getIndicesUnderSplitterNegation(splitter, indices);
		Expression result = solve(formulaUnderSplitterNegation, constraintUnderSplitterNegation, indicesUnderSplitterNegation, process);
		return result;
	}

	/**
	 * Receives formula assumed to not be 'false',
	 * and returns atom to do next splitting, or null if there is none.
	 * Since the formula is not 'false', a returned null implies that the formula is 'true'.
	 */
	private Expression pickAtomFromFormula(Expression formula, Collection<Expression> indices, RewritingProcess process) {

		Expression result = null;

		Iterator<Expression> subExpressionIterator = new SubExpressionsDepthFirstIterator(formula);
		while (result == null && subExpressionIterator.hasNext()) {
			Expression subExpression = subExpressionIterator.next();
			result = expressionIsSplitterCandidate(subExpression, indices, process);
		}

		return result;
	}

	/**
	 * If solutions are unconditional expressions, simply combine them.
	 * If they are conditional, perform distributive on conditions.
	 */
	private Expression combineSymbolicResults(Expression solution1, Expression solution2, RewritingProcess process) {

		Expression result = null;

		if (IfThenElse.isIfThenElse(solution1)) {
			Expression condition  = IfThenElse.getCondition(solution1);
			Expression thenBranch = IfThenElse.getThenBranch(solution1);
			Expression elseBranch = IfThenElse.getElseBranch(solution1);
			Expression solution2UnderCondition    = completeSimplifySolutionGivenSplitter        (solution2, condition, process);
			Expression solution2UnderNotCondition = completeSimplifySolutionGivenSplitterNegation(solution2, condition, process);
			Expression newThenBranch = combineSymbolicResults(thenBranch, solution2UnderCondition,    process);
			Expression newElseBranch = combineSymbolicResults(elseBranch, solution2UnderNotCondition, process);
			result = IfThenElse.make(condition, newThenBranch, newElseBranch);
		}
		else if (IfThenElse.isIfThenElse(solution2)) {
			Expression condition  = IfThenElse.getCondition(solution2);
			Expression thenBranch = IfThenElse.getThenBranch(solution2);
			Expression elseBranch = IfThenElse.getElseBranch(solution2);
			Expression solution1UnderCondition    = completeSimplifySolutionGivenSplitter        (solution1, condition, process);
			Expression solution1UnderNotCondition = completeSimplifySolutionGivenSplitterNegation(solution1, condition, process);
			Expression newThenBranch = combineSymbolicResults(solution1UnderCondition,    thenBranch, process);
			Expression newElseBranch = combineSymbolicResults(solution1UnderNotCondition, elseBranch, process);
			result = IfThenElse.make(condition, newThenBranch, newElseBranch);
		}
		else {
			result = combineUnconditionalSolutions(solution1, solution2);
		}

		// The code below is left to show what I tried when separating externalization from the main algorithm.
		// I would simply sum the counts without trying to externalize, and have a separate counting algorithm-with-externalization
		// using this non-externalized one and doing the externalization as a post-processing step. It was almost half the speed,
		// possible because doing the externalization on the fly allows for simplifications to be performed sooner.
		// I also tried using the code below with general-purpose externalization right after the function application case,
		// and it was indeed almost as fast as the above, but still about 10% slower, so I decided to stick with the general-purpose case.

		//		if (count1.equals(Expressions.ZERO)) {
		//			result = count2;
		//		}
		//		else if (count2.equals(Expressions.ZERO)) {
		//			result = count1;
		//		}
		//		else if (count1.getSyntacticFormType().equals("Function application") ||
		//				count2.getSyntacticFormType().equals("Function application")) {
		//
		//			result = Expressions.apply(FunctorConstants.PLUS, count1, count2);
		//		}
		//		else {
		//			result = Expressions.makeSymbol(count1.rationalValue().add(count2.rationalValue()));
		//		}

		return result;
	}

	///////////////////////////// END OF FIXED PART OF GENERIC DPLL //////////////////////////////////////
}