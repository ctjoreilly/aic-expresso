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
package com.sri.ai.grinder.library.equality.cardinality.direct.core;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.AbstractHierarchicalRewriter;
import com.sri.ai.grinder.helper.Trace;
import com.sri.ai.grinder.library.Equality;
import com.sri.ai.grinder.library.FunctorConstants;
import com.sri.ai.grinder.library.boole.ForAll;
import com.sri.ai.grinder.library.boole.ThereExists;
import com.sri.ai.grinder.library.equality.cardinality.CardinalityUtil;
import com.sri.ai.grinder.library.equality.cardinality.direct.CardinalityRewriter;
import com.sri.ai.util.Util;

/**
 * Default implementation of R_top_quantifier_elimination(Qx F).
 * 
 * @author oreilly
 *
 */
@Beta
public class TopQuantifierElimination extends AbstractHierarchicalRewriter implements CardinalityRewriter {
	
	public TopQuantifierElimination() {
	}
	
	
	@Override
	public String getName() {
		return R_top_quantifier_elimination;
	}
	
	/**
	 * @see CardinalityRewriter#R_top_quantifier_elimination
	 */
	@Override
	public Expression rewriteAfterBookkeeping(Expression expression, RewritingProcess process) {
		Expression result = null;
		
		// Assert input arguments, Qx F
		CardinalityRewriter.Quantification quantification  = null;
		Expression                         indexExpression = null;
		Expression                         f               = null;
		if (ForAll.isForAll(expression)) {
			quantification  = CardinalityRewriter.Quantification.FOR_ALL;
			indexExpression = ForAll.getIndexExpression(expression);
			f               = ForAll.getBody(expression);
		} 
		else if (ThereExists.isThereExists(expression)) {
			quantification  = CardinalityRewriter.Quantification.THERE_EXISTS;
			indexExpression = ThereExists.getIndexExpression(expression);
			f               = ThereExists.getBody(expression);
		} 
		else {
			throw new IllegalArgumentException("Invalid input argument expression, Qx F expected, where Q is a quantifier over x:"+expression);
		}
	
		Trace.log("F <- R_top_simplify(F)");
		f = process.rewrite(R_top_simplify, f);
		
		if (quantification == CardinalityRewriter.Quantification.FOR_ALL) {
			Trace.log("if Q is \"for all\"");
			Trace.log("    return R_normalize( R_card(|F|_x, Q) = |type(x)| )");
		} 
		else {
			Trace.log("if Q is \"there exists\"");
			Trace.log("    return R_normalize( R_card(|F|_x, Q) > 0)");
		}
		
		Expression cardinalityOfIndexedFormaulaF = CardinalityUtil.makeCardinalityOfIndexedFormulaExpression(f, indexExpression);
		Expression resultCard1                   = process.rewrite(R_card, CardinalityUtil.argForCardinalityWithQuantifierSpecifiedCall(cardinalityOfIndexedFormaulaF, quantification));
		
		if (quantification == CardinalityRewriter.Quantification.FOR_ALL) {
			Expression cardIndexX            = CardinalityUtil.makeCardinalityOfIndexExpressions(Util.list(indexExpression));
			Expression resultCard1EqualCardX = Equality.make(resultCard1, cardIndexX);
			
			result = process.rewrite(R_normalize, resultCard1EqualCardX);
		} 
		else {
			Expression resultCard1NotEqual0 = Expressions.makeExpressionOnSyntaxTreeWithLabelAndSubTrees(FunctorConstants.GREATER_THAN, resultCard1, Expressions.ZERO);
			
			result = process.rewrite(R_normalize, resultCard1NotEqual0);
		}
		
		return result;
	}
}