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
package com.sri.ai.grinder.sgdpllt.library.boole;

import static com.sri.ai.util.Util.map;

import java.util.Map;

import com.google.common.annotations.Beta;
import com.sri.ai.grinder.sgdpllt.library.Disequality;
import com.sri.ai.grinder.sgdpllt.library.Equality;
import com.sri.ai.grinder.sgdpllt.library.FunctorConstants;
import com.sri.ai.grinder.sgdpllt.library.controlflow.IfThenElse;
import com.sri.ai.grinder.sgdpllt.rewriter.api.Rewriter;
import com.sri.ai.grinder.sgdpllt.rewriter.core.Switch;
import com.sri.ai.grinder.sgdpllt.simplifier.api.Simplifier;

/**
 * A {@link Simplifier} with commonly boolean connectives plus conditionals:
 * 
 * <ul>
 * <li> boolean connectives (<code>and, or, not, <=>, =></code>)
 * <li> if then else
 * </ul>
 * 
 * @author braz
 *
 */
@Beta
public class BooleanSimplifier2 extends Switch<String> {
	
	public BooleanSimplifier2() {
		super(Switch.FUNCTOR, makeFunctionApplicationSimplifiers());
	}
	
	public static Map<String, Rewriter> makeFunctionApplicationSimplifiers() {
		return map(
				FunctorConstants.AND,             (Simplifier) (f, context) ->
				And.simplify(f),

				FunctorConstants.OR,              (Simplifier) (f, context) ->
				Or.simplify(f),

				FunctorConstants.NOT,             (Simplifier) (f, context) ->
				Not.simplify(f),

				FunctorConstants.IF_THEN_ELSE,    (Simplifier) (f, context) ->
				IfThenElse.simplify(f),

				FunctorConstants.EQUIVALENCE,     (Simplifier) (f, context) ->
				Equivalence.simplify(f),

				FunctorConstants.IMPLICATION,     (Simplifier) (f, context) ->
				Implication.simplify(f),

				FunctorConstants.EQUALITY,        (Simplifier) (f, context) ->
				Equality.simplify(f, context),

				FunctorConstants.DISEQUALITY,     (Simplifier) (f, context) ->
				Disequality.simplify(f, context)
				);
	}

	public static Map<String, Simplifier> makeSyntacticFormTypeSimplifiers() {
		return map();
	}
}