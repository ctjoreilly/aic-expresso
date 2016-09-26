package com.sri.ai.test.grinder.sgdpllt.core.solver;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.Type;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ExpressionStepSolver;
import com.sri.ai.grinder.sgdpllt.api.Theory;
import com.sri.ai.grinder.sgdpllt.core.solver.ContextDependentExpressionProblemSolver;
import com.sri.ai.grinder.sgdpllt.core.solver.EvaluatorStepSolver;
import com.sri.ai.grinder.sgdpllt.core.solver.ExpressionStepSolverToLiteralSplitterStepSolverAdapter;
import com.sri.ai.grinder.sgdpllt.library.boole.And;
import com.sri.ai.grinder.sgdpllt.library.boole.Not;
import com.sri.ai.grinder.sgdpllt.library.boole.Or;
import com.sri.ai.grinder.sgdpllt.library.controlflow.IfThenElse;
import com.sri.ai.grinder.sgdpllt.theory.propositional.PropositionalTheory;

public class ExpressionStepSolverToLiteralSplitterStepSolverAdapterTest {

	@Test
	public void testFixedPropositionalDisjunctiveFormulas() {
		Theory theory = new PropositionalTheory();
		
		Map<String, Type> variablesAndTypes = new LinkedHashMap<>(theory.getVariableNamesAndTypesForTesting());
		Type booleanType = variablesAndTypes.get("P");
		variablesAndTypes.put("S", booleanType);
		variablesAndTypes.put("T", booleanType);
		theory.setVariableNamesAndTypesForTesting(variablesAndTypes);
		
		Context context = theory.makeContextWithTestingInformation();
	
		runTest(new GeneralFormulaExpressionTestStepSolver(Expressions.parse("P or Q"), Expressions.parse("R or Q")), 
				Expressions.parse("if P then if R then (P or Q) and (R or Q) else if Q then (P or Q) and (R or Q) else (P or Q) and not (R or Q) else if Q then (P or Q) and (R or Q) else if R then not (P or Q) and (R or Q) else not (P or Q) and not (R or Q)"),
				context);
		
		runTest(new GeneralFormulaExpressionTestStepSolver(Expressions.parse("P or Q"), Expressions.parse("R or S")), 
				Expressions.parse("if P then if R then (P or Q) and (R or S) else if S then (P or Q) and (R or S) else (P or Q) and not (R or S) else if Q then if R then (P or Q) and (R or S) else if S then (P or Q) and (R or S) else (P or Q) and not (R or S) else if R then not (P or Q) and (R or S) else if S then not (P or Q) and (R or S) else not (P or Q) and not (R or S)"),
				context);
	}
	
	private void runTest(GeneralFormulaExpressionTestStepSolver generalFormulaExpressionTestStepSolver, Expression expected, Context context) {
		ExpressionStepSolverToLiteralSplitterStepSolverAdapter stepSolver = new ExpressionStepSolverToLiteralSplitterStepSolverAdapter(generalFormulaExpressionTestStepSolver);
		System.out.println("Evaluating " + generalFormulaExpressionTestStepSolver.getExpressionToSolve());
		Expression solution = ContextDependentExpressionProblemSolver.staticSolve(stepSolver, context);
		System.out.println(generalFormulaExpressionTestStepSolver.getExpressionToSolve() + " -----> " + solution + "\n");
		assertEquals(expected, solution);
		
		testLeavesAreTrue(solution, context);
	}
	
	private void testLeavesAreTrue(Expression solution, Context context) {
		if (IfThenElse.isIfThenElse(solution)) {
			Context thenContext = context.conjoinWithLiteral(IfThenElse.condition(solution), context);
			Expression thenSolution = IfThenElse.thenBranch(solution);
			testLeavesAreTrue(thenSolution, thenContext);
			
			Context elseContext = context.conjoinWithLiteral(Not.make(IfThenElse.condition(solution)), context);
			Expression elseSolution = IfThenElse.elseBranch(solution);
			testLeavesAreTrue(elseSolution, elseContext);
		}
		else {
			EvaluatorStepSolver evaluatorStepSolver = new EvaluatorStepSolver(solution);
			assertEquals(Expressions.TRUE, evaluatorStepSolver.solve(context));			
		}
	}
	
	static class GeneralFormulaExpressionTestStepSolver implements ExpressionStepSolver {
		private List<Expression> conjuncts = new ArrayList<>();
		private List<Expression> solutionConjuncts = new ArrayList<>();
		
		public GeneralFormulaExpressionTestStepSolver(Expression... formulas) {
			for (Expression conjunct : formulas) {
				conjuncts.add(conjunct);
			}
		}
		
		public GeneralFormulaExpressionTestStepSolver(Theory theory, Random random, Context context) {
			for (int i = 0; i < theory.getVariableNamesForTesting().size(); i++) {
				Expression literal1 = theory.makeRandomLiteralOn(theory.getVariableNamesForTesting().get(i), random, context);
				Expression literal2 = theory.makeRandomLiteral(random, context);
				while (theory.getVariablesIn(literal1, context).equals(theory.getVariablesIn(literal2, context))) {
					literal2 = theory.makeRandomAtom(random, context);
				}
				
				conjuncts.add(Or.make(literal1, literal2)); 
			}
		}
		
		public Expression getExpressionToSolve() {
			return And.make(conjuncts);
		}
		
		@Override
		public GeneralFormulaExpressionTestStepSolver clone()  {
			try {
				return (GeneralFormulaExpressionTestStepSolver) super.clone();
			}
			catch (CloneNotSupportedException e) {
				throw new Error(e);
			}
		}
		
		@Override
		public Step step(Context context) {
			Step result = null;
			for (int i = solutionConjuncts.size(); i < conjuncts.size(); i++) {
				Expression conjunct = conjuncts.get(i);
				EvaluatorStepSolver evaluatorStepSolver = new EvaluatorStepSolver(conjunct);
				Expression conjunctResult = evaluatorStepSolver.solve(context);
				if (Expressions.TRUE.equals(conjunctResult)) {
					solutionConjuncts.add(conjunct);
				}
				else if (Expressions.FALSE.equals(conjunctResult)) {
					solutionConjuncts.add(Not.make(conjunct));
				}
				else {
					GeneralFormulaExpressionTestStepSolver ifTrue = this.clone();
					ifTrue.solutionConjuncts = new ArrayList<>(ifTrue.solutionConjuncts);
					ifTrue.solutionConjuncts.add(conjunct);
					GeneralFormulaExpressionTestStepSolver ifFalse = this.clone();
					ifFalse.solutionConjuncts = new ArrayList<>(ifFalse.solutionConjuncts);
					ifFalse.solutionConjuncts.add(Not.make(conjunct));
					
					result = new ExpressionStepSolver.ItDependsOn(conjunct, null, ifTrue, ifFalse);
					break;
				}
			}
			
			if (result == null) {
				result = new ExpressionStepSolver.Solution(And.make(solutionConjuncts));
			}
				
			return result;
		}
	}
}