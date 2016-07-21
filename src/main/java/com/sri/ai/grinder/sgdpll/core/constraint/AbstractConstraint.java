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
package com.sri.ai.grinder.sgdpll.core.constraint;

import static com.sri.ai.expresso.helper.Expressions.FALSE;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.AbstractExpressionWrapper;
import com.sri.ai.grinder.sgdpll.api.Constraint;
import com.sri.ai.grinder.sgdpll.api.Theory;

/**
 * An abstract {@link Constraint} implementing constraint theory, contradiction and cloning mechanisms. 
 * 
 * @author braz
 *
 */
@Beta
public abstract class AbstractConstraint extends AbstractExpressionWrapper implements Constraint {

	private static final long serialVersionUID = 1L;
	
	private Theory theory;
	protected boolean isContradiction = false;
	
	public AbstractConstraint(Theory theory) {
		this.theory = theory;
	}
	
	@Override
	public Theory getTheory() {
		return theory;
	}

	@Override
	public AbstractConstraint clone() {
		AbstractConstraint result = null;
		try {
			result = (AbstractConstraint) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public boolean isContradiction() {
		return isContradiction;
	}

	@Override
	public Constraint makeContradiction() {
		AbstractConstraint result = clone();
		result.isContradiction = true;
		return result;
	}
	
	@Override
	protected Expression computeInnerExpression() {
		Expression innerExpression;
		if (isContradiction) {
			innerExpression = FALSE;
		}
		else {
			innerExpression = computeInnerExpressionIfNotContradiction();
		}
		return innerExpression;
	}

	abstract protected Expression computeInnerExpressionIfNotContradiction();
}