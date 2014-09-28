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
package com.sri.ai.expresso.core;

import static com.sri.ai.expresso.helper.SyntaxTrees.makeCompoundSyntaxTree;
import static com.sri.ai.util.Util.mapIntoArrayList;

import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.LambdaExpression;
import com.sri.ai.expresso.api.SyntaxTree;
import com.sri.ai.expresso.helper.SyntaxTrees;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.library.indexexpression.IndexExpressions;
import com.sri.ai.grinder.library.lambda.Lambda;

/**
 * A default implementation of a {@link LambdaExpression}.
 * 
 * @author braz
 */
@Beta
public class DefaultLambdaExpression extends AbstractQuantifiedExpressionWithABody implements LambdaExpression {

	private static final long serialVersionUID = 1L;
	
	private SyntaxTree cachedSyntaxTree;

	public DefaultLambdaExpression(List<Expression> indexExpressions, Expression body) {
		super(indexExpressions, body);
		cachedSyntaxTree = makeSyntaxTree();
	}

	@Override
	public Object getSyntacticFormType() {
		return "Lambda expression";
	}

	@Override
	public SyntaxTree getSyntaxTree() {
		return cachedSyntaxTree;
	}
	
	private SyntaxTree makeSyntaxTree() {
		List<SyntaxTree> indexExpressionsSyntaxTrees = mapIntoArrayList(getIndexExpressions(), Expression::getSyntaxTree);
		SyntaxTree parameterList = SyntaxTrees.makeKleeneListIfNeeded(indexExpressionsSyntaxTrees);
		SyntaxTree result = makeCompoundSyntaxTree(Lambda.ROOT, parameterList, getBody().getSyntaxTree());
		return result;
	}

	@Override
	public DefaultLambdaExpression setIndexExpressions(List<Expression> newIndexExpressions) {
		DefaultLambdaExpression result;
		if (newIndexExpressions != getIndexExpressions()) {
			result = new DefaultLambdaExpression(newIndexExpressions, getBody());
		}
		else {
			result = this;
		}
		return result;
	}

	@Override
	public DefaultLambdaExpression setBody(Expression newBody) {
		DefaultLambdaExpression result;
		if (newBody != getBody()) {
			result = new DefaultLambdaExpression(getIndexExpressions(), newBody);
		}
		else {
			result = this;
		}
		return result;
	}

	@Override
	public Expression renameSymbol(Expression symbol, Expression newSymbol, RewritingProcess process) {
		DefaultLambdaExpression result = this;
		
		Function<Expression, Expression> renameSymbol = e -> IndexExpressions.renameSymbol(e, symbol, newSymbol, process);
		ArrayList<Expression> newIndexExpression = mapIntoArrayList(getIndexExpressions(), renameSymbol);
		
		Expression newBody = getBody().renameSymbol(symbol, newSymbol, process);
		
		result = replaceIfNeeded(result, newIndexExpression, newBody);

		return result;
	}

	private DefaultLambdaExpression replaceIfNeeded(DefaultLambdaExpression lambdaExpression, ArrayList<Expression> newIndexExpression, Expression newBody) {
		if (newIndexExpression != getIndexExpressions() || newBody != getBody()) {
			lambdaExpression = new DefaultLambdaExpression(newIndexExpression, newBody);
		}
		return lambdaExpression;
	}

	@Override
	public Expression clone() {
		DefaultLambdaExpression result = new DefaultLambdaExpression(getIndexExpressions(), getBody());
		return result;
	}
}
