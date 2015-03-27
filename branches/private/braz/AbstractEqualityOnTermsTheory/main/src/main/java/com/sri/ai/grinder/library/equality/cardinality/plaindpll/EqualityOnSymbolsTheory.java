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
package com.sri.ai.grinder.library.equality.cardinality.plaindpll;

import static com.sri.ai.expresso.helper.Expressions.apply;
import static com.sri.ai.expresso.helper.Expressions.makeSymbol;
import static com.sri.ai.grinder.library.FunctorConstants.CARDINALITY;
import static com.sri.ai.util.Util.getOrUseDefault;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.core.DefaultSyntacticFunctionApplication;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.helper.GrinderUtil;
import com.sri.ai.grinder.library.Disequality;
import com.sri.ai.grinder.library.Equality;
import com.sri.ai.grinder.library.FunctorConstants;
import com.sri.ai.grinder.library.IsVariable;
import com.sri.ai.grinder.library.boole.And;
import com.sri.ai.grinder.library.boole.Equivalence;
import com.sri.ai.grinder.library.boole.ForAll;
import com.sri.ai.grinder.library.boole.Implication;
import com.sri.ai.grinder.library.boole.Not;
import com.sri.ai.grinder.library.boole.Or;
import com.sri.ai.grinder.library.boole.ThereExists;
import com.sri.ai.grinder.library.controlflow.IfThenElse;
import com.sri.ai.grinder.library.number.Minus;
import com.sri.ai.grinder.library.number.Times;
import com.sri.ai.util.Util;
import com.sri.ai.util.base.BinaryFunction;
import com.sri.ai.util.base.Equals;

@Beta
/** 
 * A {@link Theory} for equality literals.
 */
public class EqualityOnSymbolsTheory extends AbstractTheory {

	private static Map<String, BinaryFunction<Expression, RewritingProcess, Expression>> functionApplicationSimplifiers =
			Util.<String, BinaryFunction<Expression, RewritingProcess, Expression>>map(
					FunctorConstants.EQUALITY,       (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					Equality.simplify(f, process),
					
					FunctorConstants.DISEQUALITY,    (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					Disequality.simplify(f, process),

					FunctorConstants.DISEQUALITY,    (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					Disequality.simplify(f, process),

					FunctorConstants.AND,            (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					And.simplify(f),

					FunctorConstants.OR,             (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					Or.simplify(f),

					FunctorConstants.NOT,            (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					Not.simplify(f),

					FunctorConstants.IF_THEN_ELSE,   (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					IfThenElse.simplify(f),

					FunctorConstants.EQUIVALENCE,    (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					Equivalence.simplify(f),

					FunctorConstants.IMPLICATION,    (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					Implication.simplify(f)
	);
	
	private static Map<String, BinaryFunction<Expression, RewritingProcess, Expression>> syntacticFormTypeSimplifiers =
			Util.<String, BinaryFunction<Expression, RewritingProcess, Expression>>map(
					ForAll.SYNTACTIC_FORM_TYPE,      (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					(new DPLLGeneralizedAndSymbolic(new EqualityOnSymbolsTheory(), new Tautologicality())).rewrite(f, process),
 
					ThereExists.SYNTACTIC_FORM_TYPE, (BinaryFunction<Expression, RewritingProcess, Expression>) (f, process) ->
					(new DPLLGeneralizedAndSymbolic(new EqualityOnSymbolsTheory(), new Satisfiability())).rewrite(f, process)
	);

	@Override
	public Map<String, BinaryFunction<Expression, RewritingProcess, Expression>> getFunctionApplicationSimplifiers() {
		return functionApplicationSimplifiers;
	}

	@Override
	public Map<String, BinaryFunction<Expression, RewritingProcess, Expression>> getSyntacticFormTypeSimplifiers() {
		return syntacticFormTypeSimplifiers;
	}

	/**
	 * If expression is a literal with at least one variable, turns it into a valid splitter, or returns null otherwise.
	 * @param expression
	 * @param indices
	 * @param process
	 * @return
	 */
	@Override
	public Expression makeSplitterIfPossible(Expression expression, Collection<Expression> indices, RewritingProcess process) {
		Expression result = null;
		if (expression.hasFunctor(FunctorConstants.EQUALITY) || expression.hasFunctor(FunctorConstants.DISEQUALITY)) {
			// remember that equality can have an arbitrary number of terms
			Expression variable  = Util.getFirstSatisfyingPredicateOrNull(expression.getArguments(), new IsVariable(process));
			Expression otherTerm = Util.getFirstSatisfyingPredicateOrNull(expression.getArguments(), com.sri.ai.util.base.Not.make(Equals.make(variable)));
			result = makeSplitterFromTwoTerms(variable, otherTerm, indices, process);
		}
		return result;
	}

	/**
	 * Assumes equality between terms is not trivial.
	 * @param term1
	 * @param term2
	 * @param indices
	 * @param process
	 * @return
	 */
	private static Expression makeSplitterFromTwoTerms(Expression term1, Expression term2, Collection<Expression> indices, RewritingProcess process) {
		Expression result;
		// if variable is a free variable or constant and other term is an index, we invert them because
		// the algorithm requires the first term to be an index if there are any indices in the atom.
		if (indices.contains(term1)) {
			result = Equality.make(term1, term2);
		}
		else if (indices.contains(term2)) {
			result = Equality.make(term2, term1);
		}
		else if (process.isVariable(term1)) {
			result = Equality.make(term1, term2);
		}
		else {
			result = Equality.make(term2, term1);
		}
		return result;
	}

	@Override
	public Expression applySplitterToExpression(boolean splitterSign, Expression splitter, Expression expression, RewritingProcess process) {
		if ( ! splitterSign) {
			return applySplitterNegationToExpression(splitter, expression, process);
		}
		
		Expression term1 = splitter.get(0);
		Expression term2 = splitter.get(1);
		Expression result = expression.replaceAllOccurrences(term1, term2, process);
		result = simplify(result, process);
		return result;
	}

	private Expression applySplitterNegationToExpression(Expression splitter, Expression expression, RewritingProcess process) {
		Expression term1 = splitter.get(0);
		Expression term2 = splitter.get(1);
		Expression result = expression.replaceAllOccurrences(new SimplifyLiteralGivenDisequality(term1, term2), process);
		result = simplify(result, process);
		return result;
	}

	@Override
	public boolean splitterDependsOnIndex(Expression splitter, Collection<Expression> indices) {
		boolean result = indices.contains(splitter.get(0));
		return result;
	}

	@Override
	public boolean applicationOfConstraintOnSplitterAlwaysEitherTrivializesItOrEffectsNoChangeAtAll() {
		return false;
	}

	@Override
	public Constraint makeConstraint(Collection<Expression> indices) {
		return new Constraint(indices);
	}
	
	private static final Times timesRewriter = new Times(); // for use in the class below

	@SuppressWarnings("serial")
	/**
	 * Represents and manipulates constraints in the equalityTheory of disequalities of symbols (variables and constants).
	 */
	@Beta
	public class Constraint extends LinkedHashMap<Expression, Collection<Expression>> implements Theory.Constraint {

		// The algorithm is based on the counting principle: to determine the model count, we
		// go over indices, in a certain order, and analyse how many possible values each one them has,
		// based on how many constants, free variables, and previous indices constrained to be disequal from it there are.
		// (free variables and constants are considered less than indices in the choosing order).

		// Equalities define equivalence classes.
		// Disequalities are represented on equivalent classes representatives only.
		
		// The "disequal" of a variable V is a term T that comes *before* V in the choosing order.
		// This means that this word is being used in a non-symmetric way.
		// When we mean "equal according to the equalityTheory", we say "constrained to be disequal".

		// We map each variable equivalent class representative (including free ones) to its set of disequals.
		
		// We use "distinct" to refer to non-equal Java objects (as opposed to terms not being equal on the equality equalityTheory level).
		
		// Invariants:
		// Symbols belong to equivalence classes depending on what equality splitters have been applied before.
		// Each equivalence class is represented *only* by its representative in the disequalities data structure (the map super class).
		// If an equivalent class contains a constant, that constant must be its representative (because it contains the extra implicit information about its disequality to other constants).
		// fromBoundIndexToValue maps indices to another term of its equivalence class.
		// fromBoundFreeVariableToValue does the same thing for free variables. It always points to either another free variable, or a constant. This derives from the fact that splitters always have index first if there is any, so free variables are only linked to another free variable or a constant (the second term is never an index).
		
		// The map (super class) keeps disequals.
		
		private Collection<Expression> indices;
		private Map<Expression, Expression> fromBoundVariableToBinding;
//		private Map<Expression, Expression> fromBoundIndexToBinding;
//		private Map<Expression, Expression> fromBoundFreeVariableToBinding;
		
		public Constraint(Collection<Expression> indices) {
			super();
			this.indices = indices;
			this.fromBoundVariableToBinding     = new LinkedHashMap<Expression, Expression>();
		}

		private Constraint(Constraint another) {
			//super(another);
			for (Map.Entry<Expression, Collection<Expression>> entry : another.entrySet()) {
				this.put(entry.getKey(), new LinkedHashSet<Expression>(entry.getValue())); // must copy sets to avoid interference. OPTIMIZATION: use a copy-as-needed implementation of set later.
			}
			this.indices = another.indices;
			this.fromBoundVariableToBinding     = new LinkedHashMap<Expression, Expression>(another.fromBoundVariableToBinding);
		}

		@Override
		public Collection<Expression> getIndices() {
			return indices;
		}

		@Override
		public Expression pickSplitter(RewritingProcess process) {
		
			// if there is an index X such that X has disequals Y and T,
			// we must know if Y and T are either the same or disequal before we can tell
			// how many possible values X has.
		
			for (Expression x : keySet()) {
				if (indices.contains(x)) {
					if ( ! variableIsBound(x)) { // optional, but more efficient
						Collection<Expression> disequalsOfX = getDisequals(x);
						for (Expression y : disequalsOfX) {
							if (process.isVariable(y)) { // we can restrict y to variables because at least one of y or t must be a variable (otherwise they would be two constants and we already know those are disequal).
								Expression t = getAnotherTermInCollectionThatIsNotConstrainedToBeDisequalToTerm(disequalsOfX, y, process);
								if (t != null) {
									Expression splitter = EqualityOnSymbolsTheory.makeSplitterFromTwoTerms(y, t, indices, process);
									return splitter;
								}
							}
						}
					}
				}
			}

			return null;
		}

		private Expression getBinding(Expression variable) {
			Expression result;
			result = fromBoundVariableToBinding.get(variable);
			return result;
		}

		private void setBinding(Expression variable, Expression binding) {
			if ( ! variable.equals(binding)) {
				fromBoundVariableToBinding.put(variable, binding);
			}
		}

		private int numberOfBoundIndices(Collection<Expression> indices) {
			int result = Util.count(indices, this::variableIsBound);
			return result;
		}

		private boolean variableIsBound(Expression variable) {
			boolean result = getBinding(variable) != null;
			return result;
		}

		/**
		 * A symbol's representative is itself, if the symbol is a constant,
		 * the final symbol in the current binding chain, if the symbol is a variable and it has a binding,
		 * or itself if it is a variable without a binding.
		 * If the symbol is a variable with a binding, sets its binding to the final symbol in the chain
		 * for greater efficiency next time the method is invoked.
		 * @param symbol
		 * @param process
		 * @return
		 */
		private Expression getRepresentative(Expression symbol, RewritingProcess process) {
			Expression current = symbol;
			Expression currentBinding;
			while (process.isVariable(current) && (currentBinding = getBinding(current)) != null) {
				current = currentBinding;
			}
			// now, 'current' is in the chain started at symbol,
			// and it is either a constant or a variable without binding, therefore it is the equivalence class representative.
			if (process.isVariable(symbol)) {
				setBinding(symbol, current); // optional recording so that we do not need to traverse the entire chain next time
			}
			return current;
		}
		
		@Override
		public Constraint applySplitter(boolean splitterSign, Expression splitter, RewritingProcess process) {
			Constraint result;

			Expression representative1 = getRepresentative(splitter.get(0), process);
			Expression representative2 = getRepresentative(splitter.get(1), process);

			Expression representativesEquality = Equality.makeWithConstantSimplification(representative1, representative2, process);

			if (representativesEquality.equals(splitterSign)) {
				result = this; // splitter is redundant with respect to this constraint, nothing to do.
			}
			else if (representativesEquality.equals( ! splitterSign)) {
				result = null; // splitter is contradiction with respect to this constraint, return null
			}
			else {
				Expression splitterOnEquivalentClassRepresentatives = makeSplitterFromTwoTerms(representative1, representative2, indices, process);
				if (splitterSign) {
					result = applySplitterDefinedOnEquivalentClassRepresentatives(splitterOnEquivalentClassRepresentatives, process);
				}
				else {
					result = applySplitterNegationDefinedOnEquivalentClassRepresentatives(splitterOnEquivalentClassRepresentatives, process);
				}
			}

			return result;
		}

		private Constraint applySplitterDefinedOnEquivalentClassRepresentatives(Expression splitter, RewritingProcess process) {
			Expression variable  = splitter.get(0);
			Expression otherTerm = splitter.get(1);

			Constraint result;
			
			if (termsAreConstrainedToBeDisequal(variable, otherTerm, process)) {
				result = null; // splitter is inconsistent with constraint
			}
			else {
				result = makeNewConstraintWithVariableReplacedByOtherTerm(variable, otherTerm, process);
			}
			
			return result;
		}

		private Constraint applySplitterNegationDefinedOnEquivalentClassRepresentatives(Expression splitter, RewritingProcess process) {
			Expression variable  = splitter.get(0);
			Expression otherTerm = splitter.get(1);
			Constraint result;
			result = new Constraint(this);
			result.applyDisequality(variable, otherTerm, process);
			return result;
		}

		private Constraint makeNewConstraintWithVariableReplacedByOtherTerm(Expression variable, Expression otherTerm, RewritingProcess process) {
			Constraint newConstraint = new Constraint(this);
			
			newConstraint.setBinding(variable, otherTerm);
			newConstraint.applyDisequalitiesBetweenAllDisequalsOfVariableAndOtherTerm(variable, otherTerm, process);
			newConstraint.remove(variable); // remove entry for disequals
			newConstraint.replaceVariableByOtherTermInAllEntriesButOtherTerms(variable, otherTerm, process);
			
			return newConstraint;
		}

		protected void addOneTermToTheOthersDisequalsInNewConstraintAccordingToChoosingOrder(Expression variable, Expression otherTerm, Constraint newConstraint, RewritingProcess process) {
			if (variableIsChosenAfterOtherTerm(variable, otherTerm, indices, process)) {
				copySetOfDisequalsFromTerm1AndAddTerm2AsDisequalOfTerm1AsWellInNewConstraint(variable, otherTerm, newConstraint);
			}
			else {
				copySetOfDisequalsFromTerm1AndAddTerm2AsDisequalOfTerm1AsWellInNewConstraint(otherTerm, variable, newConstraint);
			}
		}

		private void copySetOfDisequalsFromTerm1AndAddTerm2AsDisequalOfTerm1AsWellInNewConstraint(
				Expression term1, Expression term2, Constraint newConstraint) {
			
			Collection<Expression> newTerm1Disequals = makeCopyOfDisequalsOfTermInNewConstraintEvenIfEmpty(term1, newConstraint);
			newTerm1Disequals.add(term2);
			newConstraint.put(term1, newTerm1Disequals);
		}

		/**
		 * Makes a copy of disequals of term in new constraint (even if empty) and returns this set of disequals.
		 * @param term
		 * @param newConstraint
		 * @return
		 */
		private Collection<Expression> makeCopyOfDisequalsOfTermInNewConstraintEvenIfEmpty(Expression term, Constraint newConstraint) {
			Set<Expression> newTermDisequals = new LinkedHashSet<Expression>(getDisequals(term)); // TODO: OPTIMIZATION: create some kind of wrapper that only makes this copy if really needed (that is, when we try to insert a new value).
			newConstraint.put(term, newTermDisequals);
			return newTermDisequals;
		}

		private void applyDisequalitiesBetweenAllDisequalsOfVariableAndOtherTerm(Expression variable, Expression otherTerm, RewritingProcess process) {
			Collection<Expression> variableDisequals = getDisequals(variable);
			for (Expression variableDisequal : variableDisequals) {
				applyDisequality(otherTerm, variableDisequal, process);
			}
		}

		private void replaceVariableByOtherTermInAllEntriesButOtherTerms(Expression variable, Expression otherTerm, RewritingProcess process) {
			// needed to avoid invalidating iterator because map is modified during iteration
			Map<Expression, Collection<Expression>> disequalitiesCopy = new LinkedHashMap<Expression, Collection<Expression>>(this); 
			// OPTIMIZATION: we can avoid this copy if we succeed in re-write applyDisequality below in terms of Entry.setValue() only.
			
			for (Map.Entry<Expression, Collection<Expression>> entry : disequalitiesCopy.entrySet()) {
				Expression key = entry.getKey();
				if ( ! key.equals(otherTerm)) {
					Collection<Expression> keyDisequals = entry.getValue();
					if (keyDisequals.contains(variable)) { // for those keys that are disequals of variable
						// we will create a new set of disequals for the key in the new constraint, remove variable, and enforce possibly new disequality between key and otherTerm
						Set<Expression> newDisequalsOfKey = new LinkedHashSet<Expression>(keyDisequals);
						newDisequalsOfKey.remove(variable); // so we remove it from the set
						put(key, newDisequalsOfKey);
						applyDisequality(key, otherTerm, process); // add disequality between key and otherTerm (in the disequals set of whichever comes last in choosing order)
					}
					else { // for those not constrained to be different from variable, we simply re-use the set of constraints in new constraint
						put(key, keyDisequals); // shares sets between constraint
					}
				}
			}
		}

		/** Assumes disequality does not turn constraint into contradiction */
		private void applyDisequality(Expression term1, Expression term2, RewritingProcess process) {
			if (process.isVariable(term1) || process.isVariable(term2)) {
				if (process.isVariable(term1) && variableIsChosenAfterOtherTerm(term1, term2, indices, process)) {
					addFirstTermAsDisequalOfSecondTerm(term1, term2);
				}
				else { // term2 must be a variable because either term1 is not a variable, or it is but term2 comes later than term1 in ordering, which means it is a variable
					addFirstTermAsDisequalOfSecondTerm(term2, term1);
				}
			}
			// else they are both constants, and distinct ones, so no need to do anything.
		}

		private void addFirstTermAsDisequalOfSecondTerm(Expression term1, Expression term2) {
			Set<Expression> disequalsOfTerm1 = (Set<Expression>) Util.getValuePossiblyCreatingIt(((Constraint) this), term1, LinkedHashSet.class); // cannot use getDisequals(term1) here because that method does not create a new set if needed, but simply uses a constant empty collection. This prevents unnecessary creation of collections.
			disequalsOfTerm1.add(term2);
		}

		private Expression getAnotherTermInCollectionThatIsNotConstrainedToBeDisequalToTerm(Collection<Expression> terms, Expression term, RewritingProcess process) {
			Expression result = Util.getFirstSatisfyingPredicateOrNull(
					terms,
					anotherTerm -> ! anotherTerm.equals(term) && ! termsAreConstrainedToBeDisequal(anotherTerm, term, process));
			return result;
		}

		@Override
		public Expression modelCount(RewritingProcess process) {
			
			ArrayList<Expression> numberOfPossibleValuesForIndicesSoFar = new ArrayList<Expression>(indices.size() - numberOfBoundIndices(indices));
			
			for (Expression index : indices) {
				if ( ! variableIsBound(index)) {
					long numberOfNonAvailableValues = getDisequals(index).size();
					long typeSize = GrinderUtil.getTypeCardinality(index, process);
					Expression numberOfPossibleValuesForIndex;
					if (typeSize == -1) {
						Expression indexType = process.getContextualSymbolType(index);
						if (indexType == null) {
							indexType = new DefaultSyntacticFunctionApplication(FunctorConstants.TYPE, index);
						}
						Expression indexTypeCardinality = apply(CARDINALITY, indexType);
						numberOfPossibleValuesForIndex = Minus.make(indexTypeCardinality, Expressions.makeSymbol(numberOfNonAvailableValues));
					}
					else {
						numberOfPossibleValuesForIndex = makeSymbol(Math.max(0, typeSize - numberOfNonAvailableValues));
					}

					numberOfPossibleValuesForIndicesSoFar.add(numberOfPossibleValuesForIndex);
				}
			}
			
			Expression result = Times.make(numberOfPossibleValuesForIndicesSoFar);
			result = timesRewriter.rewrite(result, process);
			result = makeModelCountConditionedOnUndeterminedSplitters(
					result,
					getSplittersToBeSatisfied(indices, process), getSplittersToBeNotSatisfied(process),
					process);
			
			return result;
		}

		private Collection<Expression> getSplittersToBeSatisfied(Collection<Expression> indices, RewritingProcess process) {
			Collection<Expression> result = new LinkedHashSet<Expression>();
			for (Expression freeVariable : fromBoundVariableToBinding.keySet()) {
				if ( ! indices.contains(freeVariable)) {
					Expression representative = getRepresentative(freeVariable, process);
					// Note that a free variable's representative is never an index, because
					// in splitters indices always come before free variables,
					// and that is the order of the binding.
					// A splitter with a free variable as the first term will always have another free variable
					// or a constant on the right-hand side.
					// This matters because the conditional model count has to be in terms of
					// free variables and constants only, never indices.
					if ( ! representative.equals(freeVariable)) {
						Expression splitter = Expressions.apply(FunctorConstants.EQUALITY, freeVariable, representative); // making it with apply instead of Disequality.make sidesteps simplifications, which will not occur in this case because otherwise this condition would have either rendered the constraint a contradiction, or would have been eliminated from it
						result.add(splitter);
					}
				}
			}
			return result;
		}

		private Collection<Expression> getSplittersToBeNotSatisfied(RewritingProcess process) {
			Collection<Expression> result = new LinkedHashSet<Expression>();
			for (Map.Entry<Expression, Collection<Expression>> entry : entrySet()) {
				assert process.isVariable(entry.getKey());
				Expression variable = entry.getKey();
				if ( ! indices.contains(variable)) { // if variable is free
					for (Expression disequal : entry.getValue()) {
						Expression splitter = Expressions.apply(FunctorConstants.EQUALITY, variable, disequal); // making it with apply instead of Disequality.make sidesteps simplifications, which will not occur in this case because otherwise this condition would have either rendered the constraint a contradiction, or would have been eliminated from it
						result.add(splitter);
					}
				}
			}
			return result;
		}

		public Collection<Expression> getDisequals(Expression variable) {
			Collection<Expression> result = getOrUseDefault(this, variable, emptyList());
			return result;
		}

		private boolean termsAreConstrainedToBeEqual(Expression variable, Expression otherTerm, RewritingProcess process) {
			boolean result = getRepresentative(variable, process).equals(getRepresentative(otherTerm, process));
			return result;
		}

		private boolean termsAreConstrainedToBeDisequal(Expression term1, Expression term2, RewritingProcess process) {
			term1 = getRepresentative(term1, process);
			term2 = getRepresentative(term2, process);
			boolean result = false;
			if (process.isUniquelyNamedConstant(term1) && process.isUniquelyNamedConstant(term2)) {
				result = ! term1.equals(term2);
			}
			else if (getDisequals(term1).contains(term2)) {
				result = true;
			}
			else if (getDisequals(term2).contains(term1)) {
				result = true;
			}
			return result;
		}

		/**
		 * Indicates whether variable is chosen after otherTerm in choosing ordering.
		 */
		private boolean variableIsChosenAfterOtherTerm(Expression variable, Expression otherTerm, Collection<Expression> indices, RewritingProcess process) {
			boolean result = process.isUniquelyNamedConstant(otherTerm) || variableIsChosenAfterOtherVariable(otherTerm, variable, indices);
			return result;
		}

		/**
		 * Indicates whether variable in chosen after otherVariable in choosing ordering.
		 */
		private boolean variableIsChosenAfterOtherVariable(Expression variable, Expression otherVariable, Collection<Expression> indices) {
			boolean result;
			if (indices.contains(variable)) { // index
				if ( ! indices.contains(otherVariable)) { // free variable
					result = false; // free variables always precedes indices
				}
				else { // both are indices
					result = otherVariable.toString().compareTo(variable.toString()) < 0; // indices are compared alphabetically
				}
			}
			else if (indices.contains(otherVariable)) { // variable is free variable and otherVariable is index
				result = true; // free variable always precedes indices
			}
			else { // neither is index
				result = otherVariable.toString().compareTo(variable.toString()) < 0;	// alphabetically		
			}
			return result;
		}

		@Override
		public Expression checkIfSplitterOrItsNegationIsImplied(Expression splitter, RewritingProcess process) {
			if (termsAreConstrainedToBeEqual(splitter.get(0), splitter.get(1), process)) {
				return Expressions.TRUE;
			}
			if (termsAreConstrainedToBeDisequal(splitter.get(0), splitter.get(1), process)) {
				return Expressions.FALSE;
			}
			return splitter;
		}

		@Override
		public Expression normalize(Expression expression, RewritingProcess process) {
			String syntacticTypeForm = "Symbol";
			BinaryFunction<Expression, RewritingProcess, Expression> representativeReplacer =
					(BinaryFunction<Expression, RewritingProcess, Expression>) (s, p) -> getRepresentative(s, p);
		
			Expression result = DPLLUtil.simplifyWithExtraSyntacticFormTypeSimplifier(
					expression,
					EqualityOnSymbolsTheory.functionApplicationSimplifiers,
					EqualityOnSymbolsTheory.syntacticFormTypeSimplifiers,
					syntacticTypeForm, representativeReplacer,
					process);
			
			return result;
		}

		@Override
		public String toString() {
			String result =
					"Bindings: " + fromBoundVariableToBinding
					+ ", disequalities: " + super.toString();
			return result; 
		}
	}
}