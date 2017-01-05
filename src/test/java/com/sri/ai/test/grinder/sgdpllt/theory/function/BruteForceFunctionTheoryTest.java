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
package com.sri.ai.test.grinder.sgdpllt.theory.function;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.annotations.Beta;
import com.sri.ai.grinder.sgdpllt.group.Max;
import com.sri.ai.grinder.sgdpllt.group.Sum;
import com.sri.ai.grinder.sgdpllt.tester.SGDPLLTTester;
import com.sri.ai.grinder.sgdpllt.tester.TheoryTestingSupport;
import com.sri.ai.grinder.sgdpllt.theory.function.BruteForceFunctionTheory;
import com.sri.ai.test.grinder.sgdpllt.theory.base.AbstractTheoryTest;

/**
 * Test of compound theory of equalities, difference arithmetic and propositional theories.
 * Still quite slow (as of January 2016), hence the small number of test problems.
 * @author braz
 *
 */
@Beta
public class BruteForceFunctionTheoryTest extends AbstractTheoryTest {

	@Override
	protected TheoryTestingSupport makeTheoryTestingSupport() {
		TheoryTestingSupport result = TheoryTestingSupport.make(new BruteForceFunctionTheory());
		return result;
	}
	
	/**
	 * Indicates whether correctness should be checked against brute-force methods when possible.
	 * @return
	 */
	@Override
	protected boolean getTestAgainstBruteForce() {
		return true;
	}

	@Test
	public void testSingleVariableConstraints() {
		SGDPLLTTester.testSingleVariableConstraints(
				getTestAgainstBruteForce(),
				makeTheoryTestingSupport(),
				100 /* number of tests */,
				30 /* number of literals per test */,
				true /* output count */);
	}

	@Test
	public void testMultiVariableConstraints() {
		SGDPLLTTester.testMultiVariableConstraints(
				getTestAgainstBruteForce(),
				makeTheoryTestingSupport(),
				1 /* number of tests */,
				30 /* number of literals per test */,
				true /* output count */);
	}

	@Test
	public void testCompleteMultiVariableConstraints() {
		SGDPLLTTester.testCompleteMultiVariableConstraints(
				getTestAgainstBruteForce(),
				makeTheoryTestingSupport(),
				20 /* number of tests */,
				50 /* number of literals per test */,
				true /* output count */);
	}

	@Test
	public void testModelCountingForSingleVariableConstraints() {
		SGDPLLTTester.testModelCountingForSingleVariableConstraints(
				getTestAgainstBruteForce(),
				makeTheoryTestingSupport(),
				200 /* number of tests */,
				30 /* number of literals per test */,
				true /* output count */);
	}

	@Ignore
	@Test
	public void testSumForSingleVariableConstraints() {
		SGDPLLTTester.testGroupProblemSolvingForSingleVariableConstraints(
				getTestAgainstBruteForce(),
				new Sum(),
				makeTheoryTestingSupport(),
				10 /* number of tests */,
				20 /* number of literals per test */,
				3, /* body depth */
				true /* output count */);
	}

	@Ignore
	@Test
	public void testMaxForSingleVariableConstraints() {
		SGDPLLTTester.testGroupProblemSolvingForSingleVariableConstraints(
				getTestAgainstBruteForce(),
				new Max(),
				makeTheoryTestingSupport(),
				10 /* number of tests */,
				20 /* number of literals per test */,
				3, /* body depth */
				true /* output count */);
	}
}