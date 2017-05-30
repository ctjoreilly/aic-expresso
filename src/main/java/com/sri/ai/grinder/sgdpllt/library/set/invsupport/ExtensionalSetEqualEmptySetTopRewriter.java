package com.sri.ai.grinder.sgdpllt.library.set.invsupport;

import static com.sri.ai.util.Util.map;

import com.sri.ai.grinder.sgdpllt.library.FunctorConstants;
import com.sri.ai.grinder.sgdpllt.rewriter.core.Switch;

public class ExtensionalSetEqualEmptySetTopRewriter extends Switch<String> {
	
	public ExtensionalSetEqualEmptySetTopRewriter() {
		super(Switch.FUNCTOR,
				map(FunctorConstants.EQUAL, new ExtensionalSetEqualEmptySetSimplifier()));
	}
}