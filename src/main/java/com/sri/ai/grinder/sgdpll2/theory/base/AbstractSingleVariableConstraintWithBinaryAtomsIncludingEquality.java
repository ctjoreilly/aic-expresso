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
package com.sri.ai.grinder.sgdpll2.theory.base;

import static com.sri.ai.expresso.helper.Expressions.apply;
import static com.sri.ai.grinder.library.FunctorConstants.EQUALITY;
import static com.sri.ai.grinder.library.boole.Not.not;
import static com.sri.ai.util.Util.arrayList;

import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.sgdpll2.api.Constraint2;
import com.sri.ai.grinder.sgdpll2.api.ConstraintTheory;
import com.sri.ai.grinder.sgdpll2.core.constraint.AbstractSingleVariableConstraintWithDependentNormalizedAtoms;

/**
 * An extension of {@link AbstractSingleVariableConstraintWithBinaryAtoms}
 * for theories including equality.
 * <p>
 * This type of constraint is aware of equality and, as soon as the constraint's variable is bound to a value,
 * the solver propagates all already-present normalized atoms (after replacing the constraint variable in them by its new value).
 * For example, if a constraint is <code>X != Y and X != Z</code> and receives <code>X = W</code>,
 * it will become <code>X = W</code> with external literals <code>W != Y</code> and <code>W != Z</code>.
 * <p>
 * Once the constraint's variable becomes bound, all incoming conjoined literals are converted in such a manner
 * and directly made into an external literal.
 * <p>
 * The advantage of doing so is that literals are propagated right away, and only once,
 * through the external literals mechanism, as opposed to remaining implicit in the constraint
 * and requiring an extra stage to be obtained.
 * 
 * @author braz
 *
 */
@Beta
public abstract class AbstractSingleVariableConstraintWithBinaryAtomsIncludingEquality extends AbstractSingleVariableConstraintWithBinaryAtoms {

	private static final long serialVersionUID = 1L;

	private boolean propagateAllLiteralsWhenVariableIsBound;
	
	public AbstractSingleVariableConstraintWithBinaryAtomsIncludingEquality(Expression variable, boolean propagateAllLiteralsWhenVariableIsBound, ConstraintTheory constraintTheory) {
		super(variable, constraintTheory);
		this.propagateAllLiteralsWhenVariableIsBound = propagateAllLiteralsWhenVariableIsBound;
		
	}

	public AbstractSingleVariableConstraintWithBinaryAtomsIncludingEquality(
			Expression variable,
			ArrayList<Expression> positiveNormalizedAtoms,
			ArrayList<Expression> negativeNormalizedAtoms,
			List<Expression> externalLiterals,
			boolean propagateAllLiteralsWhenVariableIsBound,
			ConstraintTheory constraintTheory) {
		
		super(variable, positiveNormalizedAtoms, negativeNormalizedAtoms, externalLiterals, constraintTheory);
		this.propagateAllLiteralsWhenVariableIsBound = propagateAllLiteralsWhenVariableIsBound;
	}

	public AbstractSingleVariableConstraintWithBinaryAtomsIncludingEquality(AbstractSingleVariableConstraintWithBinaryAtomsIncludingEquality other) {
		super(other);
		this.propagateAllLiteralsWhenVariableIsBound = other.propagateAllLiteralsWhenVariableIsBound;
	}

	/**
	 * Returns whether this constraint propagates all other literals once variable is bound.
	 * @return
	 */
	public boolean getPropagateAllLiteralsWhenVariableIsBound() {
		return propagateAllLiteralsWhenVariableIsBound;
	}
	
	/**
	 * Method is overridden to identify when the constraint's variable is bound,
	 * in which case it propagates everything but that binding as external literals.
	 * It does that by intercepting two cases:
	 * <ul>
	 * <li> the constraint's variable is bound; in this case, the sign and normalized atom are used to form a new literal in which the constraint's variable is replaced by the value it is bound to;
	 * <li> the constraint's variable is not bound, sign is positive and normalized atom is an equality; in this case, all existing normalized atoms are propagated in terms of the value the variable is bound to, and only the binding of the variable is kept.
	 * </ul> 
	 */
	@Override
	protected AbstractSingleVariableConstraintWithDependentNormalizedAtoms
	conjoinNonTrivialSignAndNormalizedAtom(boolean sign, Expression normalizedAtom, RewritingProcess process) {

		AbstractSingleVariableConstraintWithDependentNormalizedAtoms result;
		
//		if (propagateAllLiteralsWhenVariableIsBound) {
//			throw new Error("Propagating literals when bound, should not at this point");
//		}
		
		if ( ! propagateAllLiteralsWhenVariableIsBound) { // disabled; just do the default
			result = super.conjoinNonTrivialSignAndNormalizedAtom(sign, normalizedAtom, process);
		}
		else if (onlyConstraintOnVariableIsBinding()) {
			result = conjoinNonTrivialSignAndNormalizedAtomToConstraintWithBoundVariable(sign, normalizedAtom, process);
			System.out.println("Propagated:");	
			System.out.println("Original: " + this);	
			System.out.println("Incoming: " + (sign? normalizedAtom : getConstraintTheory().getLiteralNegation(normalizedAtom, process)));	
			System.out.println("After   : " + result);	
		}
		else if (sign && normalizedAtom.hasFunctor(EQUALITY)) {
			result = conjoinNonTrivialNormalizedEqualityToConstraintWithNonBoundVariable(sign, normalizedAtom, process);
			System.out.println("Propagated:");	
			System.out.println("Original: " + this);	
			System.out.println("Incoming: " + (sign? normalizedAtom : getConstraintTheory().getLiteralNegation(normalizedAtom, process)));	
			System.out.println("After   : " + result);	
		}
		else {
			result = super.conjoinNonTrivialSignAndNormalizedAtom(sign, normalizedAtom, process);
		}
		
		return result;
	}

	/**
	 * Indicates whether the only constraint on the constraint's variable is an equality.
	 */
	private boolean onlyConstraintOnVariableIsBinding() {
		boolean result =
				getNegativeNormalizedAtoms().isEmpty()
				&& getPositiveNormalizedAtoms().size() == 1
				&& getPositiveNormalizedAtoms().get(0).hasFunctor(EQUALITY);
		return result;
	}

	private AbstractSingleVariableConstraintWithDependentNormalizedAtoms
	conjoinNonTrivialSignAndNormalizedAtomToConstraintWithBoundVariable(boolean sign, Expression normalizedAtom, RewritingProcess process) {
	
		Constraint2 result;
	
		// first, use super's implementation to detect inconsistencies
		AbstractSingleVariableConstraintWithDependentNormalizedAtoms
		conjunctionWithSignAndNormalizedAtom
		= super.conjoinNonTrivialSignAndNormalizedAtom(sign, normalizedAtom, process);
	
		if (conjunctionWithSignAndNormalizedAtom == null) {
			result = null;
		}
		else {
			Expression binding = getPositiveNormalizedAtoms().get(0); // this assumes the original single positive normalized atom stays as the first one in the conjoined constraint
			// create a fresh constraint with the binding and external literals only
			result = makeSimplification(arrayList(binding), arrayList(), getExternalLiterals());
			// apply new normalized atom after replacing constraint's variable by its value (making it an external literal)
			Expression newExternalLiteral = rewriteSignAndNormalizedAtomForValueVariableIsBoundTo(sign, normalizedAtom, binding.get(1), process);
			result = result.conjoinWithLiteral(newExternalLiteral, process);
		}
	
		return (AbstractSingleVariableConstraintWithDependentNormalizedAtoms) result;
		
		// Note: a simpler, more expensive version of this method could create an empty constraint,
		// conjoin it with the binding, with each external literal, and the new normalized atom, converted to external literal,
		// as opposed to using makeRefinementWith with all external literals at once.
		// That solution would require the application of external literals one by one, however, whereas the above just copies them all at once.
	}

	protected AbstractSingleVariableConstraintWithDependentNormalizedAtoms conjoinNonTrivialNormalizedEqualityToConstraintWithNonBoundVariable(boolean sign, Expression normalizedAtom, RewritingProcess process) {
	
		Constraint2 result;
	
		// first, use super's implementation to detect inconsistencies
		AbstractSingleVariableConstraintWithDependentNormalizedAtoms
		conjunctionWithSignAndNormalizedAtom
		= super.conjoinNonTrivialSignAndNormalizedAtom(sign, normalizedAtom, process);
	
		if (conjunctionWithSignAndNormalizedAtom == null) {
			result = null;
		}
		else {
			Expression binding = normalizedAtom;
			Expression valueVariableIsBoundTo = binding.get(1);
			// create a fresh constraint with the binding only and external literals
			result = makeSimplification(arrayList(binding), arrayList(), getExternalLiterals());
			// convert all other normalized atoms to external literals with valueVariableIsBoundTo standing for constraint's variable
			result = conjoinWithSignAndNormalizedAtomsOnValueVariableIsBoundTo(result,  true, getPositiveNormalizedAtoms(), valueVariableIsBoundTo, process);
			if (result != null) {
				result = conjoinWithSignAndNormalizedAtomsOnValueVariableIsBoundTo(result, false, getNegativeNormalizedAtoms(), valueVariableIsBoundTo, process);
			}
		}
	
		return (AbstractSingleVariableConstraintWithDependentNormalizedAtoms) result;
	}

	private Constraint2 conjoinWithSignAndNormalizedAtomsOnValueVariableIsBoundTo(Constraint2 result, boolean sign, ArrayList<Expression> normalizedAtoms, Expression valueVariableIsBoundTo, RewritingProcess process) {
		for (Expression normalizedAtom : normalizedAtoms) {
			if ( ! isEqualityBindingVariableToItsValue(sign, normalizedAtom, valueVariableIsBoundTo)) {
				Expression newLiteral = rewriteSignAndNormalizedAtomForValueVariableIsBoundTo(sign, normalizedAtom, valueVariableIsBoundTo, process);
				result = result.conjoinWithLiteral(newLiteral, process);
				if (result == null) {
					return null;
				}
			}
		}
		return result;
	}

	private boolean isEqualityBindingVariableToItsValue(boolean sign, Expression normalizedAtom, Expression valueVariableIsBoundTo) {
		boolean result =
				sign &&
				normalizedAtom.hasFunctor(EQUALITY) &&
				normalizedAtom.get(1).equals(valueVariableIsBoundTo);
		return result;
	}

	private Expression rewriteSignAndNormalizedAtomForValueVariableIsBoundTo(boolean sign, Expression normalizedAtom, Expression valueVariableIsBoundTo, RewritingProcess process) {
		Expression normalizedAtomInTermsOfValueVariableIsBoundTo = apply(normalizedAtom.getFunctor(), valueVariableIsBoundTo, normalizedAtom.get(1));
		Expression literal = sign? normalizedAtomInTermsOfValueVariableIsBoundTo : not(normalizedAtomInTermsOfValueVariableIsBoundTo);
		Expression simplifiedLiteral = getConstraintTheory().simplify(literal, process);
		return simplifiedLiteral;
	}
}