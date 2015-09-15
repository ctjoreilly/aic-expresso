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
package com.sri.ai.grinder.sgdpll2.theory.propositional;

import static com.sri.ai.expresso.helper.Expressions.apply;
import static com.sri.ai.grinder.library.FunctorConstants.NOT;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.sgdpll2.api.ConstraintTheory;
import com.sri.ai.grinder.sgdpll2.core.AbstractSingleVariableConstraint;
import com.sri.ai.util.base.Pair;

/**
 * A single-variable propositional constraint.
 * 
 * @author braz
 *
 */
public class SingleVariablePropositionalConstraint extends AbstractSingleVariableConstraint {

	private static final long serialVersionUID = 1L;

	public SingleVariablePropositionalConstraint(Expression variable, ConstraintTheory constraintTheory) {
		super(variable, constraintTheory);
	}

	public SingleVariablePropositionalConstraint(SingleVariablePropositionalConstraint other) {
		super(other);
	}

	@Override
	public SingleVariablePropositionalConstraint clone() {
		SingleVariablePropositionalConstraint result = new SingleVariablePropositionalConstraint(this);
		return result;
	}

	@Override
	public AbstractSingleVariableConstraint destructiveUpdateOrNullAfterInsertingNewAtom(boolean sign, Expression atom, RewritingProcess process) {
		return this;
	}

	@Override
	public Expression fromNegativeAtomToLiteral(Expression negativeAtom) {
		return apply(NOT, negativeAtom);
	}

	@Override
	public Pair<Boolean, Expression> fromLiteralOnVariableToSignAndAtom(Expression variable, Expression literal) {
		Pair<Boolean, Expression> result;
		if (literal.hasFunctor(NOT)) {
			result = Pair.make(false, literal.get(0));
		}
		else {
			result = Pair.make(true, literal);
		}
		return result;
	}

	@Override
	public boolean atomMayImplyLiteralsOnDifferentAtoms() {
		return false;
	}

	@Override
	public boolean impliesLiteralWithDifferentAtom(boolean sign1, Expression atom1, boolean sign2, Expression atom2, RewritingProcess process) {
		throw new Error("This method should not have been invoked because this class's atomMayImplyLiteralsOnDifferentAtoms returns 'false'.");
	}
}