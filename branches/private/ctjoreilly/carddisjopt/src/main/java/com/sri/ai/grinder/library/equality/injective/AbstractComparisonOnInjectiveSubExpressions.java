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
package com.sri.ai.grinder.library.equality.injective;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.AbstractRewriter;
import com.sri.ai.grinder.library.function.InjectiveModule;
import com.sri.ai.util.Util;

/**
 * 
 * @author braz
 *
 */
@Beta
public abstract class AbstractComparisonOnInjectiveSubExpressions extends
AbstractRewriter {

	protected abstract String getFunctor();

	abstract protected Expression conditionForComparisonOfSubExpressions(Expression syntaxTree1, Expression syntaxTree2);

	public Expression rewriteAfterBookkeeping(Expression expression, RewritingProcess process) {
		if (expression.hasFunctor(getFunctor()) && expression.numberOfArguments() == 2) {
			Expression expression1 = expression.get(0);
			Expression expression2 = expression.get(1);
			Object injectiveFunctionToken1 = getInjectiveFunctionToken(expression1, process);
			Object injectiveFunctionToken2 = getInjectiveFunctionToken(expression2, process);
			if (
					Util.notNullAndEquals(injectiveFunctionToken1, injectiveFunctionToken2) &&
					expression1.getSubExpressions().size() == expression2.getSubExpressions().size()) {
				Expression result = conditionForComparisonOfSubExpressions(expression1, expression2);
				return result;
			}
		}
		return expression;
	}

	private InjectiveModule injectiveModule;
	private boolean injectiveModuleAlreadySearched = false;

	private void getInjectiveModule(RewritingProcess process) {
		if ( ! injectiveModuleAlreadySearched) {
			injectiveModule = (InjectiveModule) process.findModule(InjectiveModule.class);
		}
		injectiveModuleAlreadySearched = true;
	}

	private Object getInjectiveFunctionToken(Expression expression, RewritingProcess process) {
		getInjectiveModule(process);
		if (injectiveModule != null) {
			return injectiveModule.getInjectiveFunctionToken(expression, process);
		}
		return null;
	}
}
