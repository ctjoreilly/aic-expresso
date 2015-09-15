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
package com.sri.ai.grinder.sgdpll2.tester;

import static com.sri.ai.expresso.helper.Expressions.TRUE;
import static com.sri.ai.expresso.helper.Expressions.makeSymbol;
import static com.sri.ai.util.Util.join;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.DefaultRewritingProcess;
import com.sri.ai.grinder.library.CommonInterpreter;
import com.sri.ai.grinder.library.boole.And;
import com.sri.ai.grinder.library.boole.ThereExists;
import com.sri.ai.grinder.library.indexexpression.IndexExpressions;
import com.sri.ai.grinder.sgdpll2.api.Constraint;
import com.sri.ai.grinder.sgdpll2.api.ConstraintTheory;
import com.sri.ai.grinder.sgdpll2.core.CompleteMultiVariableConstraint;
import com.sri.ai.grinder.sgdpll2.core.DefaultMultiVariableConstraint;
import com.sri.ai.util.base.NullaryFunction;

/**
 * A class for testing a {@link ConstraintTheory} and its unsatisfiability detection.
 * 
 * @author braz
 *
 */
public class ConstraintTheoryTester {
	
	private static void output(String message) {
		//System.out.println(message); // uncomment out if detailed output is desired.
	}

	/**
	 * Measures the time taken to run a given number of single-variable constraint tests with a maximum number of literals,
	 * without brute-force correctness testing.
	 * Throws an {@link Error} with the failure description if a test fails.
	 * @param random
	 * @param constraintTheory
	 * @param numberOfTests
	 * @param maxNumberOfLiterals
	 * @param outputCount
	 */
	public static long measureTimeSingleVariableConstraints(Random random, ConstraintTheory constraintTheory, int numberOfTests, int maxNumberOfLiterals, boolean outputCount) {
		
		long start = System.currentTimeMillis();
		testSingleVariableConstraints(random, constraintTheory, numberOfTests, maxNumberOfLiterals, outputCount, false /* no correctness */);
		long result = System.currentTimeMillis() - start;
		if (outputCount) {
			System.out.println("Total time: " + result + " ms.");
		}	
		return result;
	}

	/**
	 * Given a constraint theory and a number <code>n</code> of single-variable constraint tests,
	 * generates <code>n</code> formulas in the theory
	 * and see if those detected as unsatisfiable by the corresponding solver
	 * are indeed unsatisfiable (checked by brute force).
	 * Throws an {@link Error} with the failure description if a test fails.
	 * @param random
	 * @param constraintTheory
	 * @param numberOfTests
	 * @param maxNumberOfLiterals
	 * @param outputCount
	 */
	public static void testSingleVariableConstraints(Random random, ConstraintTheory constraintTheory, int numberOfTests, int maxNumberOfLiterals, boolean outputCount) {
		testSingleVariableConstraints(random, constraintTheory, numberOfTests, maxNumberOfLiterals, true, outputCount);
	}
	
	private static void testSingleVariableConstraints(
			Random random, ConstraintTheory constraintTheory, int numberOfTests, int maxNumberOfLiterals, boolean testCorrectness, boolean outputCount) {
		
		NullaryFunction<Constraint> makeConstraint = () -> constraintTheory.makeSingleVariableConstraint(makeSymbol(constraintTheory.getTestingVariable()));

		RewritingProcess process = constraintTheory.extendWithTestingInformation(new DefaultRewritingProcess(null));
		
		NullaryFunction<Expression> makeRandomLiteral = () -> constraintTheory.makeRandomLiteralOnTestingVariable(random, process);

		boolean isComplete = constraintTheory.singleVariableConstraintIsCompleteWithRespectToItsVariable();

		test(constraintTheory, makeConstraint, makeRandomLiteral, isComplete, numberOfTests, maxNumberOfLiterals, testCorrectness, outputCount, process);
	}

	/**
	 * Given a constraint theory and a number <code>n</code> of multi-variable constraint tests,
	 * generates <code>n</code> formulas in the theory
	 * and see if those detected as unsatisfiable by the corresponding solver
	 * are indeed unsatisfiable (checked by brute force).
	 * Throws an {@link Error} with the failure description if a test fails.
	 * @param random
	 * @param constraintTheory
	 * @param numberOfTests
	 * @param maxNumberOfLiterals
	 * @param outputCount
	 */
	public static void testMultiVariableConstraints(Random random, ConstraintTheory constraintTheory, int numberOfTests, int maxNumberOfLiterals, boolean outputCount) {
		testMultiVariableConstraints(random, constraintTheory, numberOfTests, maxNumberOfLiterals, true, outputCount);
	}
	
	private static void testMultiVariableConstraints(
			Random random, ConstraintTheory constraintTheory, int numberOfTests, int maxNumberOfLiterals, boolean testCorrectness, boolean outputCount) {
		
		NullaryFunction<Constraint> makeConstraint = () -> new DefaultMultiVariableConstraint(constraintTheory);

		RewritingProcess process = constraintTheory.extendWithTestingInformation(new DefaultRewritingProcess(null));
		
		NullaryFunction<Expression> makeRandomLiteral = () -> constraintTheory.makeRandomLiteral(random, process);

		boolean isComplete = false; // multi-variable implementation is current incomplete for all theories

		test(constraintTheory, makeConstraint, makeRandomLiteral, isComplete, numberOfTests, maxNumberOfLiterals, testCorrectness, outputCount, process);
	}

	/**
	 * Given a constraint theory and a number <code>n</code> of multi-variable constraint tests,
	 * generates <code>n</code> formulas in the theory
	 * and see if those detected as unsatisfiable by the corresponding solver
	 * are indeed unsatisfiable (checked by brute force).
	 * Throws an {@link Error} with the failure description if a test fails.
	 * @param random
	 * @param constraintTheory
	 * @param numberOfTests
	 * @param maxNumberOfLiterals
	 * @param outputCount
	 */
	public static void testCompleteMultiVariableConstraints(Random random, ConstraintTheory constraintTheory, int numberOfTests, int maxNumberOfLiterals, boolean outputCount) {
		testCompleteMultiVariableConstraints(random, constraintTheory, numberOfTests, maxNumberOfLiterals, true, outputCount);
	}
	
	private static void testCompleteMultiVariableConstraints(
			Random random, ConstraintTheory constraintTheory, int numberOfTests, int maxNumberOfLiterals, boolean testCorrectness, boolean outputCount) {
		
		NullaryFunction<Constraint> makeConstraint = () -> new CompleteMultiVariableConstraint(constraintTheory);

		RewritingProcess process = constraintTheory.extendWithTestingInformation(new DefaultRewritingProcess(null));
		
		NullaryFunction<Expression> makeRandomLiteral = () -> constraintTheory.makeRandomLiteral(random, process);

		boolean isComplete = true;

		test(constraintTheory, makeConstraint, makeRandomLiteral, isComplete, numberOfTests, maxNumberOfLiterals, testCorrectness, outputCount, process);
	}

	/**
	 * Given a constraint theory, generators for constraints, random variables,
	 * whether the constraint solver is complete,
	 * and a number <code>n</code> of tests,
	 * generates <code>n</code> formulas in the theory
	 * and see if those detected as unsatisfiable by the corresponding solver
	 * are indeed unsatisfiable (checked by brute force).
	 * Throws an {@link Error} with the failure description if a test fails.
	 * @param constraintTheory
	 * @param makeConstraint a thunk generating new constraints
	 * @param makeRandomLiteral a think generating appropriate new literals
	 * @param isComplete whether the constraint solver always returns <code>null</code> for unsatisfiable conjunctions of literals
	 * @param numberOfTests the number of tests to run
	 * @param maxNumberOfLiterals the maximum number of literals to add to each test conjunction
	 * @param testCorrectness actually tests the result, as opposed to simply running tests (for measuring time, for example)
	 * @param outputCount whether to output the test count
	 * @param process a rewriting process
	 * @throws Error
	 */
	public static void test(ConstraintTheory constraintTheory, NullaryFunction<Constraint> makeConstraint, NullaryFunction<Expression> makeRandomLiteral, boolean isComplete, int numberOfTests, int maxNumberOfLiterals, boolean testCorrectness, boolean outputCount, RewritingProcess process) throws Error {
		for (int i = 1; i != numberOfTests + 1; i++) {
			Constraint constraint = makeConstraint.apply();
			Collection<Expression> literals = new LinkedHashSet<>();
			
			output("\n\nStarting new conjunction");	
			for (int j = 0; constraint != null && j != maxNumberOfLiterals; j++) {
				Expression literal = makeRandomLiteral.apply();
				literals.add(literal);
				output("\nAdded " + literal + " (current conjunction: " + And.make(literals) + ")");	
				constraint = conjoin(literal, constraint, constraintTheory, isComplete, testCorrectness, literals, process);
			}
			if (outputCount && i % 100 == 0) {
				System.out.println("Tested " + i + " examples for " + constraintTheory.getClass().getSimpleName());
			}	
		}
	}

	private static Constraint conjoin(Expression literal, Constraint constraint, ConstraintTheory constraintTheory, boolean isComplete, boolean testCorrectness, Collection<Expression> literals, RewritingProcess process) throws Error {
		Constraint newConstraint = constraint.conjoin(literal, process);
		if (testCorrectness) {
			if (newConstraint == null) {
				solverSaysItIsUnsatisfiable(literals, constraintTheory, process);
			}
			else {
				if (isComplete) {
					solverSaysItIsSatisfiable(literals, newConstraint, constraintTheory, process);
				}
				else {
					// if constraint is not null, the conjunction of literals may or may not be satisfiable,
					// because solver is incomplete, so in this case we do not check.
					output("Solver does not know yet if it is satisfiable or not. Current constraint is " + newConstraint);
				}
			}
		}
		return newConstraint;
	}

	/**
	 * @param literals
	 * @param constraint
	 * @param constraintTheory
	 * @param process
	 * @throws Error
	 */
	protected static void solverSaysItIsSatisfiable(Collection<Expression> literals, Constraint constraint, ConstraintTheory constraintTheory, RewritingProcess process) throws Error {
		output("Solver thinks it is satisfiable. Current constraint is " + constraint);	
		Expression formula = And.make(literals);
		boolean isUnsatisfiable = !ConstraintTheoryTester.isSatisfiableByBruteForce(formula, constraintTheory, process);;
//		Map<Expression, Expression> satisfyingAssignment = getSatisfyingAssignmentByBruteForce(formula, constraintTheory, process);
//		boolean isUnsatisfiable = satisfyingAssignment == null;
		if (isUnsatisfiable) {
			String message = join(literals, " and ") + " is unsatisfiable (by brute-force) but " + 
			constraintTheory.getClass().getSimpleName() + "'s says it is satisfiable. " +
			"Current constraint is " + constraint;
			output(message);
			throw new Error(message);
		}
		else {
			output("Brute-force satisfiability test agrees that it is satisfiable. Constraint is " + constraint);	
		}
	}

	/**
	 * @param literals
	 * @param constraintTheory
	 * @param process
	 * @throws Error
	 */
	protected static void solverSaysItIsUnsatisfiable(Collection<Expression> literals, ConstraintTheory constraintTheory, RewritingProcess process) throws Error {
		output("Solver thinks it is unsatisfiable.");	
		Expression formula = And.make(literals);
		boolean isSatisfiable = ConstraintTheoryTester.isSatisfiableByBruteForce(formula, constraintTheory, process);;
//		Map<Expression, Expression> satisfyingAssignment = getSatisfyingAssignmentByBruteForce(formula, constraintTheory, process);
//		boolean isSatisfiable = satisfyingAssignment != null;
		if (isSatisfiable) {
			String message = join(literals, " and ") + " is satisfiable (by brute-force) but " + 
			constraintTheory.getClass().getSimpleName() + "'s says it is not. "
//			+ "Satisfying assignment is " + satisfyingAssignment + "."
			;
			output(message);
			throw new Error(message);
		}
		else {
			output("Brute-force satisfiability test agrees that it is unsatisfiable.");	
		}
	}

	/**
	 * Determines whether a formula is satisfiable by adding existential quantifiers for each of its variables
	 * (according to the constraint theory provided) and evaluating it.
	 * @param formula
	 * @param constraintTheory
	 * @param process
	 * @return whether the formula is satisfiable.
	 */
	public static boolean isSatisfiableByBruteForce(Expression formula, ConstraintTheory constraintTheory, RewritingProcess process) {
		Map<String, String> variableNamesAndTypeNamesForTesting = constraintTheory.getVariableNamesAndTypeNamesForTesting();
		Expression quantifiedFormula = formula;
		Collection<Expression> variables = constraintTheory.getVariablesIn(formula, process);
		for (Expression variable : variables) {
			Expression type = makeSymbol(variableNamesAndTypeNamesForTesting.get(variable));
			quantifiedFormula = ThereExists.make(IndexExpressions.makeIndexExpression(variable, type), quantifiedFormula);
		}
		Expression evaluation = new CommonInterpreter().apply(quantifiedFormula, process);
		boolean result = evaluation.equals(TRUE);
		return result;
	}
}