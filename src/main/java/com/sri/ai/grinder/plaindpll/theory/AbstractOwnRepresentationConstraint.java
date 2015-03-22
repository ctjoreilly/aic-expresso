package com.sri.ai.grinder.plaindpll.theory;

import java.util.Collection;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.plaindpll.api.Constraint;
import com.sri.ai.grinder.plaindpll.core.Contradiction;

/** An abstract class implementing a {@link Constraint} that represents its own constraint, instead of relying on base constraint objects. */	
@SuppressWarnings("serial")
public abstract class AbstractOwnRepresentationConstraint extends AbstractConstraint {
	
	protected Collection<Expression> supportedIndices;

	public AbstractOwnRepresentationConstraint(Collection<Expression> supportedIndices) {
		this.supportedIndices = supportedIndices;
	}
	
	public abstract AbstractOwnRepresentationConstraint clone();
	
	@Override
	public Collection<Expression> getSupportedIndices() {
		return supportedIndices;
	}

	@Override
	public Constraint incorporate(boolean splitterSign, Expression splitter, RewritingProcess process) {
		Constraint result;

		Expression normalizedSplitterGivenConstraint = normalizeSplitterGivenConstraint(splitter, process);
		
		if (normalizedSplitterGivenConstraint.equals(splitterSign)) {
			result = this; // splitter is redundant given constraint
		}
		else if (normalizedSplitterGivenConstraint.equals( ! splitterSign)) {
			result = null; // splitter is contradictory given constraint
		}
		else {
			try {
				result = applyNormalizedSplitter(splitterSign, normalizedSplitterGivenConstraint, process);
			}
			catch (Contradiction e) {
				result = null;
			}
		}

		return result;
	}

	private Constraint applyNormalizedSplitter(boolean splitterSign, Expression splitter, RewritingProcess process) {
		AbstractOwnRepresentationConstraint newConstraint = clone();
		newConstraint.applyNormalizedSplitterDestructively(splitterSign, splitter, process);
		return newConstraint;
	}

	/**
	 * Modify this constraint's inner representation to include this splitter.
	 */
	abstract protected void applyNormalizedSplitterDestructively(boolean splitterSign, Expression splitter, RewritingProcess process);
}