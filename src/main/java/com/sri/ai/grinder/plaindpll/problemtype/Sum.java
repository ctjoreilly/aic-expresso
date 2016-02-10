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
package com.sri.ai.grinder.plaindpll.problemtype;

import static com.sri.ai.expresso.helper.Expressions.makeSymbol;

import java.util.Random;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.library.FunctorConstants;
import com.sri.ai.grinder.plaindpll.group.AssociativeCommutativeGroup;
import com.sri.ai.grinder.plaindpll.group.SymbolicPlusGroup;

/**
 * The sum problem type.
 * 
 * @author braz
 *
 */
public class Sum extends AbstractGroupProblemTypeWithFunctionApplicationExpression {

	/**
	 * Creates a sum problem type based on a {@link SymbolicPlusGroup} group.
	 */
	public Sum() {
		super(new SymbolicPlusGroup());
	}
	
	/**
	 * Creates a sum problem type based on a given group.
	 * This is useful for extensions that need to be based on an instance of some extension of
	 * {@link AssociativeCommutativeGroup} such as {@link AssociativeCommutativeSemiRing}.
	 * This is something need for problem types passed to {@link AbstractSemiRingProblemType}.
	 */
	protected Sum(AssociativeCommutativeGroup group) {
		super(group);
	}
	
	@Override
	public String getFunctorString() {
		return FunctorConstants.SUM;
	}

	@Override
	public Expression makeRandomConstant(Random random) {
		Expression result = makeSymbol(random.nextInt(10));
		return result;
	}
}
