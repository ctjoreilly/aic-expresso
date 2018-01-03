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
package com.sri.ai.grinder.sgdpllt.interpreter;

import java.util.Iterator;
import java.util.List;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.helper.AssignmentsIterator;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.MultiQuantifierEliminationProblem;
import com.sri.ai.grinder.sgdpllt.core.solver.AbstractMultiQuantifierEliminator;
import com.sri.ai.grinder.sgdpllt.rewriter.api.TopRewriter;

/**
 * An extension of {@link AbstractMultiQuantifierEliminator}
 * that solves quantified expressions by brute force.
 * <p>
 * Additionally, it takes an assignment to symbols as a constructing parameter,
 * and throws an error when a symbol with unassigned value is found.
 * <p>
 * The reason this quantifier eliminator uses an assignment to keep a binding to variables,
 * as opposed to using equalities in the context, is that
 * the context can only deal with variables for which we have a satisfiability solver,
 * whereas an assignment can be used for any variables.
 *
 * @author braz
 *
 */
@Beta
public class BruteForceMultiQuantifierEliminator extends AbstractIterativeMultiQuantifierEliminator {

	public BruteForceMultiQuantifierEliminator(TopRewriter topRewriter) {
		super(topRewriter);
	}
	
	public BruteForceMultiQuantifierEliminator(TopRewriterUsingContextAssignments topRewriterWithBaseAssignment) {
		super(topRewriterWithBaseAssignment);
	}
	
	@Override
	public Iterator<Assignment> makeAssignmentsIterator(List<Expression> indices, Expression indicesCondition, Context context) {
		return new AssignmentsIterator(indices, context);
	}

	@Override
	public Expression makeSummand(MultiQuantifierEliminationProblem problem, Context context) {
		return problem.getConditionedBody();
	}
}