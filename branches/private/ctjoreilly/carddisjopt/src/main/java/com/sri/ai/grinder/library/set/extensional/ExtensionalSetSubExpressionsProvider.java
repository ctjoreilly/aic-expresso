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
package com.sri.ai.grinder.library.set.extensional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.ExpressionAndContext;
import com.sri.ai.expresso.api.SyntaxTree;
import com.sri.ai.expresso.core.DefaultExpressionAndContext;
import com.sri.ai.expresso.helper.ExpressionKnowledgeModule;
import com.sri.ai.grinder.api.NoOpRewriter;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.AbstractRewriter;
import com.sri.ai.util.collect.FunctionIterator;

/**
 * An (@link ExpressionKnowledgeModule.Provider} for extensional set expressions.
 * 
 * @author braz
 *
 */
@Beta
public class ExtensionalSetSubExpressionsProvider extends AbstractRewriter
implements
NoOpRewriter,
ExpressionKnowledgeModule.Provider
{
	private static final List<Integer> _emptyPath = Collections.emptyList();
	private static final List<Integer> _pathZero  = Collections.unmodifiableList(Arrays.asList(new Integer(0)));

	@Override
	public Iterator<ExpressionAndContext> getImmediateSubExpressionsAndContextsIterator(Expression expression, final RewritingProcess process) {
		if (knowledgeApplies(expression)) {
			List<Expression> arguments   = ExtensionalSet.getElements(expression);
			List<Integer> pathToElements = null;
			if (usesKleeneList(expression)) {
				pathToElements = _pathZero;
			}
			else {
				pathToElements = _emptyPath;
			}
			Iterator<ExpressionAndContext> subExpressionsAndContextsIterator =
				new FunctionIterator<Expression, ExpressionAndContext>(
						arguments,
						new DefaultExpressionAndContext.
						MakerFromExpressionAndSuccessivePathsFormedFromABasePath(pathToElements));
			return subExpressionsAndContextsIterator;
		}
		return null;
	}

	private boolean usesKleeneList(Expression expression) {
		boolean hasSubTrees = expression.getSyntaxTree().numberOfImmediateSubTrees() > 0;
		if (hasSubTrees) {
			SyntaxTree firstSubTree = expression.getSyntaxTree().getSubTree(0);
			SyntaxTree rootTree = firstSubTree.getRootTree();
			if (rootTree == null) {
				return false;
			}
			boolean rootTreeOfFirstSubTreeIsKleeneListString = rootTree.equals("kleene list");
			return hasSubTrees && rootTreeOfFirstSubTreeIsKleeneListString;
		}
		return false;
	}

	// the methods below seem to be pretty much boilerplate, much shared with, say, BracketedExpression.
	// We should abstract this.
	
	private boolean knowledgeApplies(Expression expression) {
		return expression != null && ExtensionalSet.isExtensionalSet(expression);
	}

	@Override
	public Object getSyntacticFormType(Expression expression, RewritingProcess process) {
		if (knowledgeApplies(expression)) {
			return "Extensional set";
		}
		return null;
	}

	@Override
	public void rewritingProcessInitiated(RewritingProcess process) {
		ExpressionKnowledgeModule knowledgeBasedExpressionModule =
			(ExpressionKnowledgeModule) process.findModule(ExpressionKnowledgeModule.class);
		if (knowledgeBasedExpressionModule != null) {
			knowledgeBasedExpressionModule.register(this);
		}
	}

	@Override
	public Expression rewriteAfterBookkeeping(Expression expression, RewritingProcess process) {
		// Note: is a NoOpRewriter
		return expression; // will be removed eventually, not a real rewriter, just a module.
	}
}
