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
package com.sri.ai.grinder.core.solver;

import static com.sri.ai.util.Util.map;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.IntensionalSet;
import com.sri.ai.expresso.core.ExtensionalIndexExpressionsSet;
import com.sri.ai.grinder.api.MultiQuantifierEliminator;
import com.sri.ai.grinder.group.AssociativeCommutativeGroup;
import com.sri.ai.grinder.library.set.Sets;
import com.sri.ai.grinder.rewriter.api.Simplifier;
import com.sri.ai.grinder.rewriter.core.Switch;

/**
 * A {@link Switch<String>} extension solving aggregate functions on intensional sets,
 * given the functor, group, and {@link MultiQuantifierEliminator}.
 *
 * @author braz
 *
 */
@Beta
public class QuantifierEliminatorOfFunctionOnIntensionalSetTopRewriter extends Switch<String> {

	public QuantifierEliminatorOfFunctionOnIntensionalSetTopRewriter(
			String functor, 
			AssociativeCommutativeGroup group, 
			MultiQuantifierEliminator quantifierEliminator) {
		
		super(
				Switch.FUNCTOR,
				map(
						functor, 
						simplifierForAggregateOnIntensionalSet(group, quantifierEliminator)
				)
		);
	}

	private static Simplifier simplifierForAggregateOnIntensionalSet(
			AssociativeCommutativeGroup group, 
			MultiQuantifierEliminator quantifierEliminator) {
		
		return (e, c) -> {
			Expression result;
			try {
				if (Sets.isIntensionalMultiSet(e.get(0))) {
					IntensionalSet intensionalSet = (IntensionalSet) e.get(0);
					ExtensionalIndexExpressionsSet indexExpressions = 
							(ExtensionalIndexExpressionsSet) intensionalSet.getIndexExpressions();
					// the set is intensional, but not the set of index expressions!
					result =
							quantifierEliminator.extendContextAndSolve(
									group,
									indexExpressions,
									intensionalSet.getCondition(),
									intensionalSet.getHead(),
									c);
				}
				else {
					result = e;
				}
			}
			catch (IllegalArgumentException exception) {
				result = e;
			}
			return result;
		};
	}
}