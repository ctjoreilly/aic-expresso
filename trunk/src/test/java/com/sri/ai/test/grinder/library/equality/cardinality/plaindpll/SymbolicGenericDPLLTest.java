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
package com.sri.ai.test.grinder.library.equality.cardinality.plaindpll;

import static com.sri.ai.expresso.helper.Expressions.apply;
import static com.sri.ai.expresso.helper.Expressions.makeSymbol;
import static com.sri.ai.expresso.helper.Expressions.parse;
import static com.sri.ai.grinder.helper.GrinderUtil.getAllVariables;
import static com.sri.ai.grinder.library.FunctorConstants.CARDINALITY;
import static com.sri.ai.grinder.library.indexexpression.IndexExpressions.makeIndexExpression;
import static com.sri.ai.util.Util.mapIntoArrayList;
import static com.sri.ai.util.Util.toArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.Symbol;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.Rewriter;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.core.DefaultRewritingProcess;
import com.sri.ai.grinder.core.TotalRewriter;
import com.sri.ai.grinder.helper.GrinderUtil;
import com.sri.ai.grinder.library.Associative;
import com.sri.ai.grinder.library.DirectCardinalityComputationFactory;
import com.sri.ai.grinder.library.PlainSubstitution;
import com.sri.ai.grinder.library.boole.And;
import com.sri.ai.grinder.library.boole.Or;
import com.sri.ai.grinder.library.controlflow.IfThenElse;
import com.sri.ai.grinder.library.controlflow.IfThenElseBranchesAreIdentical;
import com.sri.ai.grinder.library.number.GreaterThan;
import com.sri.ai.grinder.library.number.LessThanOrEqualTo;
import com.sri.ai.grinder.library.number.Minus;
import com.sri.ai.grinder.library.number.Plus;
import com.sri.ai.grinder.library.number.Times;

@Beta
public abstract class SymbolicGenericDPLLTest {

	protected static final Symbol DEFAULT_EVERYTHING_CARDINALITY_VALUE = makeSymbol(10);

	protected abstract Rewriter makeRewriter();

	protected abstract Expression makeProblem(Expression expression, List<Expression> indexExpressions);

	protected static final Expression everythingType = makeSymbol("Everything");

	protected static final Expression everythingCardinality = apply(CARDINALITY, everythingType);

	protected static class Parse implements Function<String, Expression> {
		@Override
		public Expression apply(String input) {
			return parse(input);
		}
	}

	protected void runSymbolicAndNonSymbolicTests(Expression expression, Collection<String> indicesStrings, Expression expected) {
		runTest(expression, indicesStrings, expected, true /* no type size */);
		
		RewritingProcess process = DirectCardinalityComputationFactory.newCardinalityProcess();
		
		process.putGlobalObject(everythingCardinality, DEFAULT_EVERYTHING_CARDINALITY_VALUE);
		
		TotalRewriter normalizer = new TotalRewriter(
				new PlainSubstitution(),
				new Associative("+"), new Associative("*"), new Associative("and"), new Associative("or"),
				new Plus(), new Minus(), new Times(), new GreaterThan(), new LessThanOrEqualTo(),
				new Or(), new And(),
				new IfThenElse(),
				new IfThenElseBranchesAreIdentical()
				);
		
		Map<Expression, Expression> freeSymbolsAndTypes = new HashMap<Expression, Expression>();
		for (Expression freeSymbol : Expressions.freeSymbols(expected, process)) {
			freeSymbolsAndTypes.put(freeSymbol, everythingType);
		}
		process = GrinderUtil.extendContextualSymbols(freeSymbolsAndTypes, process);
				
		Expression expectedWithTypeSize = normalizer.rewrite(expected, process);
		runTest(expression, indicesStrings, expectedWithTypeSize, false /* use type size */);
	}
	
	protected void runTest(Expression expression, Collection<String> indicesStrings, Expression expected, boolean noTypeSize) {
		DefaultRewritingProcess process = new DefaultRewritingProcess(expression, null);
		
		Collection<Expression> indices;
		if (indicesStrings != null) {
			indices = mapIntoArrayList(indicesStrings, new Parse());
		}
		else {
			indices = getAllVariables(expression, process);
		}
		
		if (! noTypeSize) {
			process.putGlobalObject(everythingCardinality, DEFAULT_EVERYTHING_CARDINALITY_VALUE);
		}
		
		List<Expression> indexExpressions =
				indices
				.stream()
				.map(index -> makeIndexExpression(index, everythingType))
				.collect(toArrayList(indices.size()));
		
		Rewriter rewriter = makeRewriter();
		Expression problem = makeProblem(expression, indexExpressions);
		System.out.println("Problem: " + problem);
		RewritingProcess subProcess = GrinderUtil.extendContextualSymbols(fromFreeSymbolsToEverything(problem, process), process);
		Expression actual = rewriter.rewrite(problem, subProcess);
		System.out.println("Solution: " + actual);
		System.out.println("Expected: " + expected + "\n");
		Assert.assertEquals(expected, actual);
	}

	private Map<Expression, Expression> fromFreeSymbolsToEverything(Expression expression, RewritingProcess process) {
		Map<Expression, Expression> result = new LinkedHashMap<Expression, Expression>();
		Collection<Expression> freeSymbols = Expressions.freeSymbols(expression, process);
		freeSymbols.forEach(freeSymbol -> result.put(freeSymbol, everythingType));
		return result;
	}
}