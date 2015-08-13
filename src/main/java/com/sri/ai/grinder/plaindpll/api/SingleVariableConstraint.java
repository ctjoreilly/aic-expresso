package com.sri.ai.grinder.plaindpll.api;

import java.util.Collection;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;

/**
 * An {@link Expression} with efficient internal representation for incrementally deciding satisfiability of a boolean formulas on literals in a certain theory.
 * 
 * @author braz
 *
 */
public interface SingleVariableConstraint extends Expression {

	/**
	 * Returns the {@link ConstraintTheory} for this constraint.
	 * @return the constraint theory.
	 */
	ConstraintTheory getConstraintTheory();
	
	/**
	 * @return the variable term constrained by this constraint.
	 */
	Expression getVariable();
	
	/**
	 * Returns the type of the variable.
	 */
	Expression getVariableDomain(RewritingProcess process);

	/**
	 * Returns the variable domain size, if determined, or -1 otherwise.
	 * @param process
	 * @return
	 */
	long getVariableDomainSize(RewritingProcess process);

	/**
	 * Returns the literals that are part of this constraint, but not defined on the variable.
	 * @return
	 */
	Collection<Expression> getExternalLiterals();

	/**
	 * Returns an {@link SingleVariableConstraint} representing a constraint equivalent to this one given that 'externalLiteral' is true.
	 * For example, <code>x != 3 and y = 3</code> is equivalent to <code>x != 3</code> under <code>y = 3</code>.
	 * While this method could in principle be defined for literals that are not external,
	 * that is, literals on the variable of this single-variable constraint,
	 * that would imply the literal being applied being kept somewhere else,
	 * which is inadvisable because all literals on the same variables should eventually be kept in the same place.
	 * <p>
	 * If the external literal is a splitter generated by this constraint, and that literal cannot be looked easily from the contextual constraint,
	 * then this method must transform the constraint in such a way that it will not be picked anymore,
	 * in order to avoid infinite picking of the same splitter.
	 * 
	 * @param literal a literal assumed to be true
	 * @param process the rewriting process
	 * @return a constraint equivalent to this one given the literal
	 */
	SingleVariableConstraint simplifyGiven(Expression externalLiteral, RewritingProcess process);
	
	/**
	 * Returns an {@link SingleVariableConstraint} representing the conjunction of this constraint and a given literal,
	 * or null if they are contradictory.
	 * @param literal the literal
	 * @param process the rewriting process
	 * @return the application result or <code>null</code> if contradiction.
	 */
	SingleVariableConstraint conjoin(Expression literal, RewritingProcess process);
	
	/**
	 * Picks a splitter whose value is necessary in order to determine the model count of the constraint.
	 * 
	 * The representation must be chosen such that a splitter is either no longer generated after an application of {@link #simplifyGiven(Expression, RewritingProcess)}
	 * with it or its negation, or no longer generated if the contextual constraint can be used to check whether it is valid or not.
	 * This avoids infinite picking of the same splitter.
	 * 
	 * @param process the current rewriting process
	 * @return the splitter
	 */
	Expression pickSplitter(RewritingProcess process);
	
	/**
	 * The number of assignments to the variable that satisfies the constraint.
	 * @param process the current rewriting process
	 * @return an expression representing the model count
	 */
	Expression modelCount(RewritingProcess process);
	
	SingleVariableConstraint clone();
}