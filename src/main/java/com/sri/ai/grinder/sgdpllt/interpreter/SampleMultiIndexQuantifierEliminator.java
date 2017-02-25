/*
 * Copyright (c) 2017, SRI International
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
package com.sri.ai.grinder.sgdpllt.interpreter;

import static com.sri.ai.util.Util.in;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.IndexExpressionsSet;
import com.sri.ai.expresso.api.IntensionalSet;
import com.sri.ai.expresso.api.Type;
import com.sri.ai.expresso.core.ExtensionalIndexExpressionsSet;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.helper.AssignmentsIterator;
import com.sri.ai.grinder.helper.AssignmentsSamplingIterator;
import com.sri.ai.grinder.helper.GrinderUtil;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.group.AssociativeCommutativeGroup;
import com.sri.ai.grinder.sgdpllt.group.Product;
import com.sri.ai.grinder.sgdpllt.library.controlflow.IfThenElse;
import com.sri.ai.grinder.sgdpllt.library.indexexpression.IndexExpressions;
import com.sri.ai.grinder.sgdpllt.library.number.Division;
import com.sri.ai.grinder.sgdpllt.library.set.Measure;
import com.sri.ai.grinder.sgdpllt.rewriter.api.Rewriter;
import com.sri.ai.grinder.sgdpllt.rewriter.api.TopRewriter;
import com.sri.ai.grinder.sgdpllt.rewriter.core.Exhaustive;
import com.sri.ai.grinder.sgdpllt.rewriter.core.Recursive;
import com.sri.ai.util.base.Pair;
import com.sri.ai.util.math.Rational;

/**
 * 
 * @author oreilly
 *
 */
public class SampleMultiIndexQuantifierEliminator extends AbstractIterativeMultiIndexQuantifierElimination {
	private int sampleSizeN;
	private boolean alwaysSample;
	private Rewriter indicesConditionRewriter;
	private Random random;
	
	public SampleMultiIndexQuantifierEliminator(TopRewriter topRewriter, int sampleSizeN, boolean alwaysSample, Rewriter indicesConditionRewriter, Random random) {
		super(topRewriter);
		this.sampleSizeN = sampleSizeN;
		this.alwaysSample = alwaysSample;
		this.indicesConditionRewriter = new Recursive(new Exhaustive(indicesConditionRewriter));
		this.random = random;
	}
	
	public SampleMultiIndexQuantifierEliminator(TopRewriterUsingContextAssignments topRewriterWithBaseAssignment, int sampleSizeN, boolean alwaysSample, Rewriter indicesConditionRewriter, Random random) {
		super(topRewriterWithBaseAssignment);
		this.sampleSizeN = sampleSizeN;
		this.alwaysSample = alwaysSample;
		this.indicesConditionRewriter = new Recursive(new Exhaustive(indicesConditionRewriter));
		this.random = random;
	}
	
	@Override
	public Iterator<Map<Expression, Expression>> makeAssignmentsIterator(List<Expression> indices, Expression indicesCondition, Context context) {
		Iterator<Map<Expression, Expression>> result;
		if (indices.size() == 2 && indices.get(1).equals(Expressions.TRUE)) {
			result = new AssignmentsSamplingIterator(indices.subList(0, 1), sampleSizeN, indicesCondition, indicesConditionRewriter, random, context);
		}
		else {
			result = new AssignmentsIterator(indices, context);
		}
		return result;
	}
	
	@Override
	public Expression makeSummand(AssociativeCommutativeGroup group, List<Expression> indices, Expression indicesCondition, Expression body, Context context) {
		Expression result;
		if (indices.size() == 2 && indices.get(1).equals(Expressions.TRUE)) {
			// NOTE: the AssignmentsSamplingIterator takes the indicesCondition into account 
			// so no need to take it into account here (which is the case in BruteForceMultiIndexQuantifierEliminator).
			result = body;
		}
		else {
			result = IfThenElse.make(indicesCondition, body, group.additiveIdentityElement());
		}		
		return result;
	}
	
	@Override
	public Expression solve(AssociativeCommutativeGroup group, List<Expression> indices, Expression indicesCondition, Expression body, Context context) {
		Expression result = null;
		
		// Check if we want to sample		
		if (indices.size() == 1) {						
			
			// SetOfI = {{ (on I in Domain) I : Condition }}
			Pair<Rational, Boolean> measureSetOfIAndSample = computeMeasureAndDetermineIfShouldSample(indices.get(0), indicesCondition, group.additiveIdentityElement(), context);
			Rational measureSetOfI = measureSetOfIAndSample.first;
			Boolean  sample        = measureSetOfIAndSample.second;
			
			if (sample) {
				// Quantifier({{ (on I in Samples) Head }} )							
				if (group instanceof Product) {
					// In the case of the 'product' group we are computing a geometic mean: 
					// https://en.wikipedia.org/wiki/Geometric_mean
					// which suffers from arithmetic overflow and underflow, we will therefore directly
					// compute the log-average:
					// https://en.wikipedia.org/wiki/Geometric_mean#Relationship_with_logarithms
					// in order to attempt to address these issues directly
					result = computeUsingLogAverage((Product) group, indices.get(0), indicesCondition, body, context, measureSetOfI);
				}
				else {
					// NOTE: we are using the indices[2] with 2nd arg=TRUE so that the sampling logic can determine when it should activate
					// in makeAssignmentsInterator() and makeSummand().
					Expression sampleGroupSum = super.solve(group, Arrays.asList(indices.get(0), Expressions.TRUE), indicesCondition,  body, context);			
				
					// Average = Quantifier( {{ (on I in Samples) Head }}) / n
					Expression average = group.addNTimes(sampleGroupSum, Division.make(Expressions.ONE, Expressions.makeSymbol(sampleSizeN)), context);
					
					// return Average * | SetOfI |
					result = group.addNTimes(average, Expressions.makeSymbol(measureSetOfI), context);
				}								
			}
		}
				
		if (result == null) {
			result = super.solve(group, indices, indicesCondition,  body, context);
		}
		
		return result;
	}
	
	// https://en.wikipedia.org/wiki/Geometric_mean#Relationship_with_logarithms
	private Expression computeUsingLogAverage(Product group, Expression index, Expression condition, Expression body, Context context, Rational measureSetOfI) {		
		Expression summand = body;
		
		double logSum = 0.0;
		Rewriter rewriter = new Recursive(new Exhaustive(getTopRewriterUsingContextAssignments()));
		Iterator<Map<Expression, Expression>> assignmentsIterator = new AssignmentsSamplingIterator(Arrays.asList(index), sampleSizeN, condition, indicesConditionRewriter, random, context);
		for (Map<Expression, Expression> indicesValues : in(assignmentsIterator)) {
			Context extendedContext = extendAssignments(indicesValues, context);			
			Expression bodyEvaluation = rewriter.apply(summand, extendedContext);
			if (group.isAdditiveAbsorbingElement(bodyEvaluation)) {
				return bodyEvaluation;
			}
			
			double bodyValue = getBoundedDoubleValue(bodyEvaluation);
			if (bodyValue < 0) {
				throw new UnsupportedOperationException("Currently do not support negative terms when computing log-average : "+bodyEvaluation);
			}
			logSum += Math.log(bodyValue);
		}
		
		double quotient   = logSum / sampleSizeN;
		double dResult    = getBoundedDoubleValue(Math.exp(quotient * getBoundedDoubleValue(measureSetOfI)));
		Expression result = Expressions.makeSymbol(dResult);
		
		return result;		
	}
	
	private double getBoundedDoubleValue(Expression numberExpression) {
		Rational rational = numberExpression.rationalValue();
		double   result   = getBoundedDoubleValue(rational);
		return result;
	}
	
	private double getBoundedDoubleValue(Rational rational) {		
		double result = getBoundedDoubleValue(rational.doubleValue());
		return result;
	}
	
	private double getBoundedDoubleValue(double result) {
		// Handle cases where result is too large
		if (result == Double.POSITIVE_INFINITY) {		
			result = Double.MAX_VALUE;
		}
		else if (result == Double.NEGATIVE_INFINITY) {
			result = -Double.MAX_VALUE;
		}
		return result;
	}
	
	private Pair<Rational, Boolean> computeMeasureAndDetermineIfShouldSample(Expression index, Expression indexCondition, Expression additiveIdentityElement, Context context) {
		Pair<Rational, Boolean> result;
		
		Expression indexType = GrinderUtil.getTypeExpression(index, context);
		IndexExpressionsSet indexExpressionsSet = new ExtensionalIndexExpressionsSet(IndexExpressions.makeIndexExpression(index, indexType));
		
		Expression intensionalSet = IntensionalSet.intensionalMultiSet(indexExpressionsSet, index, indexCondition);
		
		Rational measureSetOfI = Measure.get(intensionalSet, context);
		
		boolean sample = true;
		if (!alwaysSample) {			
			Type type = GrinderUtil.getType(index, context);
			// NOTE: We always sample from continuous domains
			if (type != null && type.isDiscrete()) {
				if (measureSetOfI.compareTo(sampleSizeN) <= 0) {
					// Domain is discrete and sample size is >= the size of the domain
					// so we don't want to sample in this instance
					sample = false;
				}
			}			
		}
		
		result = new Pair<>(measureSetOfI, sample);
		
		return result;
	}
}