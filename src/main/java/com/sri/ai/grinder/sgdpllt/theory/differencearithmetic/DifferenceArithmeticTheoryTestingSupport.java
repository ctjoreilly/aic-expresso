/*
 * Copyright (c) 2016, SRI International
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
package com.sri.ai.grinder.sgdpllt.theory.differencearithmetic;

import static com.sri.ai.expresso.helper.Expressions.apply;
import static com.sri.ai.expresso.helper.Expressions.makeSymbol;
import static com.sri.ai.expresso.helper.Expressions.parse;
import static com.sri.ai.util.Util.map;
import static com.sri.ai.util.Util.pickUniformly;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.Type;
import com.sri.ai.expresso.type.IntegerInterval;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.library.number.Plus;
import com.sri.ai.grinder.sgdpllt.library.number.UnaryMinus;
import com.sri.ai.grinder.sgdpllt.theory.base.AbstractTheoryWithBinaryAtomsTestingSupport;
import com.sri.ai.util.Util;

@Beta
public class DifferenceArithmeticTheoryTestingSupport extends AbstractTheoryWithBinaryAtomsTestingSupport {

	public static final IntegerInterval TESTING_INTEGER_INTERVAL_TYPE = new IntegerInterval("0..4");
	
	public DifferenceArithmeticTheoryTestingSupport(DifferenceArithmeticTheory theory, Random random) {
		super(theory, random);
		setVariableNamesAndTypesForTesting(map("I", TESTING_INTEGER_INTERVAL_TYPE, "J", TESTING_INTEGER_INTERVAL_TYPE, "K", TESTING_INTEGER_INTERVAL_TYPE));
	}
	
	/**
	 * Makes a random atom on variable by summing or subtracting terms from two random atoms generated by super class implementation.
	 */
	@Override
	public Expression makeRandomAtomOn(String mainVariable, Context context) {
		String mainVariableName = getVariableName(mainVariable);
		Type mainType = getTestingVariableType(mainVariable);
		
		List<String> variableNamesThatAreSubtypesOf = getVariableNamesWhoseTypesAreSubtypesOf(mainType);

		int maxNumberOfOtherVariablesInAtom = Math.min(variableNamesThatAreSubtypesOf.size(), 2);
		// used to be 3, but if literal has more than two variables, it steps out of difference arithmetic and may lead 
		// to multiplied variables when literals are propagated. 
		// For example, X = Y + Z and X = -Y - Z + 3 imply 2Y + 2Z = 3
		int numberOfOtherVariablesInAtom = getRandom().nextInt(maxNumberOfOtherVariablesInAtom); 
		// Note: that otherVariablesForAtom will contain only one or zero elements
		ArrayList<String> otherVariablesForAtom = new ArrayList<>();		
		if (numberOfOtherVariablesInAtom > 0) {
			otherVariablesForAtom.add(pickTestingVariableAtRandom(mainType, otherName -> !otherName.equals(mainVariableName)));
		}
		
		ArrayList<Expression> constants = new ArrayList<Expression>();
		int numberOfConstants = getRandom().nextInt(3);
		for (int i = 0; i != numberOfConstants; i++) {
			// Note: We know we can safely sample from the Difference Arithmetic Theory Types.
			Expression sampledConstant = mainType.sampleUniquelyNamedConstant(getRandom());
			Expression constant;
			if (getRandom().nextBoolean()) {
				constant = sampledConstant;
			}
			else {
				constant = makeSymbol(-sampledConstant.intValue());
			}
			constants.add(constant);
		}

		ArrayList<Expression> leftHandSideArguments = new ArrayList<Expression>();
		leftHandSideArguments.add(parse(mainVariable));
		// needs to be difference, so it's added as negative
		Util.mapIntoList(otherVariablesForAtom, otherVariable -> UnaryMinus.make(parse(otherVariable)), leftHandSideArguments);
		leftHandSideArguments.addAll(constants);

		int numberOfOtherVariablesToBeCanceled = getRandom().nextInt(otherVariablesForAtom.size() + 1);
		ArrayList<String> otherVariablesToBeCanceled = Util.pickKElementsWithoutReplacement(otherVariablesForAtom, numberOfOtherVariablesToBeCanceled, getRandom());
		// note that this term is positive, so it will cancel the previously negative term with the same "other variable"
		Util.mapIntoList(otherVariablesToBeCanceled, v -> parse(v), leftHandSideArguments); 
		// Note: it may seem odd to generate an "other variable" and add another term that will cancel it later. 
		// However, this is useful for making sure canceling works properly.
		
		Expression leftHandSide = Plus.make(leftHandSideArguments);
		String functor = pickUniformly(getTheoryFunctors(), getRandom());
		Expression unsimplifiedResult = apply(functor, leftHandSide, 0);		
		Expression result = getTheory().simplify(unsimplifiedResult, context);
		//System.out.println("Random literal: " + result);	
		// Note that simplify will eliminate negated variables;
		// however, we leave their generation and then elimination here as a sanity check,
		// as well as a useful feature for the day when we get assurance that literals will be simplified down the line,
		// allowing us to eliminate them here. TODO
		
		return result;
	}
}
