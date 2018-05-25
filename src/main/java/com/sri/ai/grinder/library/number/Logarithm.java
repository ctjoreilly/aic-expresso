package com.sri.ai.grinder.library.number;

import static com.sri.ai.expresso.helper.Expressions.ONE;
import static com.sri.ai.expresso.helper.Expressions.ZERO;
import static com.sri.ai.expresso.helper.Expressions.makeSymbol;

import com.google.common.annotations.Beta;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.api.Context;
import com.sri.ai.grinder.rewriter.api.Simplifier;
import com.sri.ai.util.math.Rational;

/**
 * Class to implement the natural (neparian) logarithm ln(x).
 * It assumes that it has only 1 argument.
 * @author Sarah Perrin
 *
 */
@Beta
public class Logarithm implements Simplifier {
	
	@Override
	public Expression applySimplifier(Expression expression, Context context) {
		return simplify(expression);
	}
	
	public static Expression simplify(Expression expression) {

		if (expression.get(0).compareTo(ZERO)<=0) {
			throw new Error("Undefined value for log of non positive values " + expression);
		}

		if (expression.get(0).equals(makeSymbol(1))) { // if numerator and denominator are equal, result is 1.
			return ZERO;
		}

		if (expression.get(0).equals(makeSymbol(Math.E))) { // if numerator is 0, fraction is 0.
			return ONE;
		}

		if (Expressions.isNumber(expression.get(0))) {

			Expression log = makeSymbol(Math.log(expression.get(0).doubleValue()));
			
			Rational result = log.rationalValue();
			if (result != null) {
				return Expressions.makeSymbol(result);
			}
		}

		return expression;
	}
	
}
