package com.sri.ai.grinder.sgdpll2.core.solver;

import static com.sri.ai.expresso.helper.Expressions.isSubExpressionOf;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.condition;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.elseBranch;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.isIfThenElse;
import static com.sri.ai.grinder.library.controlflow.IfThenElse.thenBranch;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.library.controlflow.IfThenElse;
import com.sri.ai.grinder.plaindpll.group.AssociativeCommutativeGroup;
import com.sri.ai.grinder.sgdpll2.api.Constraint;
import com.sri.ai.grinder.sgdpll2.api.ConstraintTheory;
import com.sri.ai.grinder.sgdpll2.api.ContextDependentProblemStepSolver;
import com.sri.ai.grinder.sgdpll2.api.SingleVariableConstraint;
import com.sri.ai.grinder.sgdpll2.core.constraint.ConstraintSplitting;

/**
 * An abstract implementation for step solvers for quantified expressions
 * (the quantification being based on an associative commutative group's operation).
 * <p>
 * This is done by extending {@link AbstractLiteralConditionerStepSolver} based on the body expression,
 * picking literals in it according to the contextual constraint conjoined with the index constraint,
 * and "intercepting" literals containing the indices and splitting the quantifier
 * based on that, solving the two resulting sub-problems.
 * <p>
 * For example, if we have <code>sum({{ (on X in SomeType) if Y != bob then 2 else 3 | X != john }})</code>
 * under contextual constraint <code>Z = alice</code>,
 * {@link AbstractLiteralConditionerStepSolver#step(Constraint, RewritingProcess)} is
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
 * {@link #stepGivenLiteralFreeBody(Constraint, SingleVariableConstraint, Expression, RewritingProcess)}
 * (which for sums with constant bodies will be equal to the model count of the index constraint
 * under the contextual constraint times the constant).
 * <p>
 * Extending classes must define method
 * {@link #stepGivenLiteralFreeBody(Constraint, SingleVariableConstraint, Expression, RewritingProcess)
 * to solve the case in which the body is its given literal-free version,
 * for the given contextual constraint and index constraint.
 * <p>
 * At the time of this writing,
 * {@link AbstractLiteralConditionerStepSolver} supports only expressions that are composed of
 * function applications or symbols only,
 * so this extension inherits this restriction if that is still in place.
 * 
 * @author braz
 *
 */
@Beta
public abstract class AbstractQuantifierStepSolver extends AbstractLiteralConditionerStepSolver implements Cloneable {

	private static final String ABSTRACT_QUANTIFIER_STEP_SOLVER_CONTEXTUAL_CONSTRAINT = "AbstractQuantifierStepSolver2_ContextualConstraint";

	private AssociativeCommutativeGroup group;
	
	private SingleVariableConstraint indexConstraint;
	
	public AbstractQuantifierStepSolver(AssociativeCommutativeGroup group, SingleVariableConstraint indexConstraint, Expression body) {
		super(body);
		this.group = group;
		this.indexConstraint = indexConstraint;
	}

	/**
	 * Abstract method defining a quantified expression with a given index constraint and literal-free body is to be solved.
	 * @param indexConstraint the index constraint
	 * @param literalFreeBody literal-free body
	 */
	protected abstract SolutionStep stepGivenLiteralFreeBody(
			Constraint contextualConstraint,
			SingleVariableConstraint indexConstraint,
			Expression literalFreeBody,
			RewritingProcess process);

	public AbstractQuantifierStepSolver clone() {
		AbstractQuantifierStepSolver result = null;
		try {
			result = (AbstractQuantifierStepSolver) super.clone();
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
	protected AbstractQuantifierStepSolver makeWithNewIndexConstraint(SingleVariableConstraint newIndexConstraint) {
		AbstractQuantifierStepSolver result = clone();
		result.group = group;
		result.indexConstraint = newIndexConstraint;
		return result;
	}

	public AssociativeCommutativeGroup getGroup() {
		return group;
	}
	
	public SingleVariableConstraint getIndexConstraint() {
		return indexConstraint;
	}
	
	/**
	 * Convenience method for <code>indexConstraint.getConstraintTheory()</code>.
	 * @return
	 */
	public ConstraintTheory getConstraintTheory() {
		return indexConstraint.getConstraintTheory();
	}
	
	public Expression getIndex() {
		return indexConstraint.getVariable();
	}
	
	@Override
	public SolutionStep step(Constraint contextualConstraint, RewritingProcess process) {
		SolutionStep result;

		Constraint contextualConstraintForBody = contextualConstraint.conjoin(getIndexConstraint(), process);
		if (contextualConstraintForBody == null) {
			result = new Solution(group.additiveIdentityElement());
		}
		else {
			/**
			 * We invoke super method with contextualConstraintForBody,
			 * but we want to have contextualConstraint available if the body expression
			 * is already literal-free (because then we need to invoke {@link AbstractQuantifierStepSolver#stepgivenLiteralFreeBody(Constraint, SingleVariableConstraint, Expression, RewritingProcess)}.
			 * The not-very-elegant solution is to "smuggle" it in the process's global objects.
			 */
			process.putGlobalObject(ABSTRACT_QUANTIFIER_STEP_SOLVER_CONTEXTUAL_CONSTRAINT, contextualConstraint);
			SolutionStep superSolutionStep = super.step(contextualConstraintForBody, process);
			
			// Now we "intercept" literals containing the index and split the quantifier based on it
			if (superSolutionStep.itDepends() && isSubExpressionOf(getIndex(), superSolutionStep.getExpression())) {
				Expression literalOnIndex = superSolutionStep.getExpression();
				result = resultIfLiteralContainsIndex(contextualConstraint, literalOnIndex, process);
			}
			else {
				result = superSolutionStep;
			}
		}

		return result;
	}

	@Override
	protected SolutionStep stepGivenLiteralFreeExpression(
			Expression literalFreeExpression,
			Constraint contextualConstraintForBody,
			RewritingProcess process) {
		Constraint contextualConstraint = (Constraint) process.getGlobalObject(ABSTRACT_QUANTIFIER_STEP_SOLVER_CONTEXTUAL_CONSTRAINT);
		SolutionStep result = stepGivenLiteralFreeBody(contextualConstraint, indexConstraint, literalFreeExpression, process);
		return result;
	}

	private SolutionStep resultIfLiteralContainsIndex(Constraint contextualConstraint, Expression literal, RewritingProcess process) {
		
		// if the splitter contains the index, we must split the quantifier:
		// Quant_x:C Body  --->   (Quant_{x:C and L} Body) op (Quant_{x:C and not L} Body)
		
		SolutionStep result;
		ConstraintSplitting split = new ConstraintSplitting(getIndexConstraint(), literal, contextualConstraint, process);
		switch (split.getResult()) {
		case CONSTRAINT_IS_CONTRADICTORY:
			result = new Solution(group.additiveIdentityElement());
			break;
		case LITERAL_IS_UNDEFINED:
			Expression subSolution1 = solveSubProblem(split.getConstraintAndLiteral(),         contextualConstraint, process);
			Expression subSolution2 = solveSubProblem(split.getConstraintAndLiteralNegation(), contextualConstraint, process);
			Expression solutionValue = combine(subSolution1, subSolution2, contextualConstraint, process);
			result = new Solution(solutionValue);
			break;
		case LITERAL_IS_TRUE:
			solutionValue = solveSubProblem(split.getConstraintAndLiteral(), contextualConstraint, process);
			result = new Solution(solutionValue);
			break;
		case LITERAL_IS_FALSE:
			solutionValue = solveSubProblem(split.getConstraintAndLiteralNegation(), contextualConstraint, process);
			result = new Solution(solutionValue);
			break;
		default: throw new Error("Unrecognized result for " + ConstraintSplitting.class + ": " + split.getResult());
		}
		return result;
	}

	private Expression solveSubProblem(Constraint newIndexConstraint, Constraint contextualConstraint, RewritingProcess process) {
		SingleVariableConstraint newIndexConstraintAsSingleVariableConstraint = (SingleVariableConstraint) newIndexConstraint;
		ContextDependentProblemStepSolver subProblem = makeWithNewIndexConstraint(newIndexConstraintAsSingleVariableConstraint);
		Expression result = ContextDependentProblemSolver.solve(subProblem, contextualConstraint, process);
		return result;
	}

	protected Expression combine(Expression solution1, Expression solution2, Constraint contextualConstraint, RewritingProcess process) {
		Expression result;
		if (isIfThenElse(solution1)) {
			// (if C1 then A1 else A2) op solution2 ---> if C1 then (A1 op solution2) else (A2 op solution2)
			ConstraintSplitting split = new ConstraintSplitting(contextualConstraint, condition(solution1), process);
			switch (split.getResult()) {
			case CONSTRAINT_IS_CONTRADICTORY:
				result = null;
				break;
			case LITERAL_IS_UNDEFINED:
				Expression subSolution1 = combine(thenBranch(solution1), solution2, split.getConstraintAndLiteral(), process);
				Expression subSolution2 = combine(elseBranch(solution1), solution2, split.getConstraintAndLiteralNegation(), process);
				result = IfThenElse.make(condition(solution1), subSolution1, subSolution2, true);
				break;
			case LITERAL_IS_TRUE:
				result = combine(thenBranch(solution1), solution2, split.getConstraintAndLiteral(), process);
				break;
			case LITERAL_IS_FALSE:
				result = combine(elseBranch(solution1), solution2, split.getConstraintAndLiteral(), process);
				break;
			default: throw new Error("Unrecognized result for " + ConstraintSplitting.class + ": " + split.getResult());
			}
		}
		else if (isIfThenElse(solution2)) {
			// solution1 op (if C2 then B1 else B2) ---> if C2 then (solution1 op B2) else (solution1 op B2)
			ConstraintSplitting split = new ConstraintSplitting(contextualConstraint, condition(solution2), process);
			switch (split.getResult()) {
			case CONSTRAINT_IS_CONTRADICTORY:
				result = null;
				break;
			case LITERAL_IS_UNDEFINED:
				Expression subSolution1 = combine(solution1, thenBranch(solution2), split.getConstraintAndLiteral(), process);
				Expression subSolution2 = combine(solution1, elseBranch(solution2), split.getConstraintAndLiteralNegation(), process);
				result = IfThenElse.make(condition(solution2), subSolution1, subSolution2, true);
				break;
			case LITERAL_IS_TRUE:
				result = combine(solution1, thenBranch(solution2), split.getConstraintAndLiteral(), process);
				break;
			case LITERAL_IS_FALSE:
				result = combine(solution1, elseBranch(solution2), split.getConstraintAndLiteralNegation(), process);
				break;
			default: throw new Error("Unrecognized result for " + ConstraintSplitting.class + ": " + split.getResult());
			}
		}
		else {
			result = group.add(solution1, solution2, process);
		}
		return result;
	}
}