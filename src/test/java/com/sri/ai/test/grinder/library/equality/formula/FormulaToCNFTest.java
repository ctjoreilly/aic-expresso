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
package com.sri.ai.test.grinder.library.equality.formula;

import org.junit.Assert;
import org.junit.Test;

import com.sri.ai.brewer.api.Grammar;
import com.sri.ai.brewer.core.CommonGrammar;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.library.DirectCardinalityComputationFactory;
import com.sri.ai.grinder.library.equality.formula.FormulaToCNF;
import com.sri.ai.test.grinder.AbstractGrinderTest;

public class FormulaToCNFTest extends AbstractGrinderTest {
	
	@Override
	public Grammar makeGrammar() {
		return new CommonGrammar();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test_convertToCNF_notAFormula() {
		RewritingProcess process = newProcess();

		FormulaToCNF.convertToCNF(parse("or(f(a))"), process);
	}
	
	@Test
	public void test_convertToCNF_OrNormalization() {
		RewritingProcess process = newProcess();

		// or() -> false
		Assert.assertEquals(parse("false"), FormulaToCNF.convertToCNF(parse("or()"), process));
		
		//  or(..., true, ...) -> true
		Assert.assertEquals(parse("true"), FormulaToCNF.convertToCNF(parse("or(X = a, true, Y = b)"), process));
		
		// or(..., false, ...) -> or(..., ...)
		Assert.assertEquals(parse("and(or(X = a, Y = b))"), FormulaToCNF.convertToCNF(parse("or(X = a, false, Y = b)"), process));
		
		// or(X = Y, X = Y) -> or(X = Y)
		Assert.assertEquals(parse("and(or(X = Y))"), FormulaToCNF.convertToCNF(parse("or(X = Y, X = Y)"), process));

		// or(X = Y, ..., X != Y) -> true
		Assert.assertEquals(parse("true"), FormulaToCNF.convertToCNF(parse("or(X = Y, Z = a, X != Y)"), process));
	}
	
	@Test
	public void test_convertToCNF_AndNormalization() {
		RewritingProcess process = newProcess();
		// and() -> true
		Assert.assertEquals(parse("true"), FormulaToCNF.convertToCNF(parse("and()"), process));
		
		// and(..., true, ...) -> and(..., ...)
		Assert.assertEquals(parse("and(or(X = a), or(Y = b))"), FormulaToCNF.convertToCNF(parse("and(or(X = a), true, or(Y = b))"), process));
		
		// and(X = Y, X = Y) -> and(X = Y)
		Assert.assertEquals(parse("and(or(X = Y))"), FormulaToCNF.convertToCNF(parse("and(X = Y, X = Y)"), process));
		
		// and(..., false, ...) -> false
		Assert.assertEquals(parse("false"), FormulaToCNF.convertToCNF(parse("and(or(X = a), false, or(Y = b))"), process));
		
		// and(X = Y, ..., X != Y) -> false
		Assert.assertEquals(parse("false"), FormulaToCNF.convertToCNF(parse("and(X = Y, Z = a, X != Y)"), process));
	}
	
	@Test
	public void test_convertToCNF_EqualityNormalization() {
		RewritingProcess process = newProcess();
		
		// a = X -> X = a
		Assert.assertEquals(parse("and(or(X = a))"), FormulaToCNF.convertToCNF(parse("or(a = X)"), process));
		
		// a != X -> X != a
		Assert.assertEquals(parse("and(or(X != a))"), FormulaToCNF.convertToCNF(parse("or(a != X)"), process));
		
		// B = A -> A = B
		Assert.assertEquals(parse("and(or(A = B))"), FormulaToCNF.convertToCNF(parse("or(B = A)"), process));
		
		// B != A -> A != B
		Assert.assertEquals(parse("and(or(A != B))"), FormulaToCNF.convertToCNF(parse("or(B != A)"), process));
		
		// X = X -> true
		Assert.assertEquals(parse("true"), FormulaToCNF.convertToCNF(parse("or(X = X)"), process));
		Assert.assertEquals(parse("true"), FormulaToCNF.convertToCNF(parse("or(a = a)"), process));
		
		// X != X -> false
		Assert.assertEquals(parse("false"), FormulaToCNF.convertToCNF(parse("or(X != X)"), process));
		Assert.assertEquals(parse("false"), FormulaToCNF.convertToCNF(parse("or(a != a)"), process));
		
		// a = b -> false
		Assert.assertEquals(parse("false"), FormulaToCNF.convertToCNF(parse("or(a = b)"), process));
		
		// a != b -> true
		Assert.assertEquals(parse("true"), FormulaToCNF.convertToCNF(parse("or(a != b)"), process));
		
		// X = Y = Z = -> X = Y and Y = Z
		Assert.assertEquals(parse("and(or(X = Y), or(Y = Z))"), FormulaToCNF.convertToCNF(parse("or(X = Y = Z)"), process));		
	}
	
	@Test
	public void test_convertToCNF_singleLiteral() {
		RewritingProcess process = newProcess();
		
		// X = Y -> and(or(X = Y))
		Assert.assertEquals(parse("and(or(X = Y))"), FormulaToCNF.convertToCNF(parse("X = Y"), process));
		
		// X != Y -> and(or(X != Y))
		Assert.assertEquals(parse("and(or(X != Y))"), FormulaToCNF.convertToCNF(parse("X != Y"), process));
	}
	
	@Test
	public void test_convertToCNF_ImplicationsOut() {
		RewritingProcess process = newProcess();
		
		// F1 => F2  -> not(F1) or F2
		Assert.assertEquals(parse("and(or(X != Y, W = Z))"), FormulaToCNF.convertToCNF(parse("(X = Y) => (Z = W)"), process));
		
		// F1 <=> F2 -> (not(F1) or F2) and (F1 or not(F2))
		Assert.assertEquals(parse("and(or(X != Y, W = Z), or(X = Y, W != Z))"), FormulaToCNF.convertToCNF(parse("(X = Y) <=> (Z = W)"), process));
	}
	
	@Test
	public void test_convertToCNF_NegationsIn() {
		RewritingProcess process = newProcess();
		
		// not(X = Y) -> X != Y
		Assert.assertEquals(parse("and(or(X != Y))"), FormulaToCNF.convertToCNF(parse("not(X = Y)"), process));
		
		// not(X != Y) -> X = Y
		Assert.assertEquals(parse("and(or(X = Y))"), FormulaToCNF.convertToCNF(parse("not(X != Y)"), process));
		
		// not(not(F)) -> F
		Assert.assertEquals(parse("and(or(X = Y))"), FormulaToCNF.convertToCNF(parse("not(not(X = Y))"), process));
		Assert.assertEquals(parse("and(or(X != Y))"), FormulaToCNF.convertToCNF(parse("not(not(X != Y))"), process));
		
		// not(F1 and F2)          -> not(F1) or not(F2)
		Assert.assertEquals(parse("and(or(X != Y, W !=Z))"), FormulaToCNF.convertToCNF(parse("not(and(X = Y, W = Z))"), process));
		
		// not(F1 or F2)           -> not(F1) and not(F2)
		Assert.assertEquals(parse("and(or(X != Y), or(W != Z))"), FormulaToCNF.convertToCNF(parse("not(or(X = Y, W = Z))"), process));
		
		// not(for all X : F)      -> there exists X : not(F)
		Assert.assertEquals(parse("and(or(X != Y))"), FormulaToCNF.convertToCNF(parse("not(for all X: X = Y)"), process));
		
		// not(there exists X : F) -> for all X : not(F)
		Assert.assertEquals(parse("and(or(X != Y))"), FormulaToCNF.convertToCNF(parse("not(there exists X: X = Y)"), process));
	}
	
	@Test
	public void test_convertToCNF_StandardizeVariables() {
		RewritingProcess process = newProcess();
		
		Assert.assertEquals(parse("and(or(X != Y), or(X' = Z))"), FormulaToCNF.convertToCNF(parse("(for all X : X != Y) and (for all X : X = Z)"), process));
		Assert.assertEquals(parse("and(or(X != Y), or(X' = Z))"), FormulaToCNF.convertToCNF(parse("(there exists X : X != Y) and (there exists X : X = Z)"), process));
	}
	
	@Test
	public void test_convertToCNF_ExistentialsOut() {
		RewritingProcess process = newProcess();
		
		Assert.assertEquals(parse("and(or(X != Y))"), FormulaToCNF.convertToCNF(parse("there exists X : X != Y"), process));
		Assert.assertEquals(parse("and(or(X != Y, X' = Z))"), FormulaToCNF.convertToCNF(parse("(there exists X : X != Y) or (there exists X : X = Z)"), process));
		
// TODO - tests related to skolem functions when logic supported.		
	}
	
	@Test
	public void test_convertToCNF_AllsOut() {
		RewritingProcess process = newProcess();
		
		Assert.assertEquals(parse("and(or(X != Y))"), FormulaToCNF.convertToCNF(parse("for all X : X != Y"), process));
	}
	
	@Test
	public void test_convertToCNF_Distribution() {
		RewritingProcess process = newProcess();
		
		// F1 or (F1 and F2) -> (F1 or F2) and (F1 or F3)
		Assert.assertEquals(parse("and(or(A = B, B = C), or(A = B, C = D))"), FormulaToCNF.convertToCNF(parse("or(A = B, and(B = C, C = D))"), process));
		Assert.assertEquals(parse("and(or(A = B, E = F, B = C), or(A = B, E = F, C = D))"), FormulaToCNF.convertToCNF(parse("or(A = B, E = F, and(B = C, C = D))"), process));
		
		// (F1 and F2) or F3 -> (F1 or F3) and (F2 or F3)
		Assert.assertEquals(parse("and(or(A = B, B = C), or(A = B, C = D))"), FormulaToCNF.convertToCNF(parse("or(and(B = C, C = D), A = B)"), process));
		Assert.assertEquals(parse("and(or(A = B, E = F, B = C), or(A = B, E = F, C = D))"), FormulaToCNF.convertToCNF(parse("or(and(B = C, C = D), A = B, E = F)"), process));

		// F0 or (F1 or ... or Fn)  -> (F0 or F1 or ... or Fn)
		Assert.assertEquals(parse("and(or(A = B, B = C, C = D))"), FormulaToCNF.convertToCNF(parse("or(A = B, or(B = C, C = D))"), process));
		
		// (F1 or ... or Fn) or F0    -> (F1 or ... or Fn or F0)
		Assert.assertEquals(parse("and(or(B = C, C = D, A = B))"), FormulaToCNF.convertToCNF(parse("or(or(B = C, C = D), A = B)"), process));
		
		// F0 and (F1 and ... and Fn) -> (F0 and F1 and ... and Fn)
		Assert.assertEquals(parse("and(or(A = B), or(B = C), or(C = D))"), FormulaToCNF.convertToCNF(parse("and(A = B, and(B = C, C = D))"), process));
		
		// (F1 and ... and Fn) and F0 -> (F1 and ... and Fn and F0) 
		Assert.assertEquals(parse("and(or(B = C), or(C = D), or(A = B))"), FormulaToCNF.convertToCNF(parse("and(and(B = C, C = D), A = B)"), process));
		
		
		// L1 and L2 -> and(or(L1), or(L2))
		Assert.assertEquals(parse("and(or(A = B), or(B = C))"), FormulaToCNF.convertToCNF(parse("A = B and B = C"), process));
	}
	
	//
	// PRIVATE
	//
	private RewritingProcess newProcess() {
		return DirectCardinalityComputationFactory.newCardinalityProcess(Expressions.TRUE);
	}
}
