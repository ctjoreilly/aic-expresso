package com.sri.ai.grinder.sgdpllt.library.bounds;

import static com.sri.ai.expresso.helper.Expressions.apply;
import static com.sri.ai.expresso.helper.Expressions.makeSymbol;
import static com.sri.ai.grinder.helper.GrinderUtil.getIndexExpressionsOfFreeVariablesIn;
import static com.sri.ai.grinder.sgdpllt.library.FunctorConstants.AND;
import static com.sri.ai.grinder.sgdpllt.library.FunctorConstants.EQUAL;
import static com.sri.ai.grinder.sgdpllt.library.FunctorConstants.GREATER_THAN_OR_EQUAL_TO;
import static com.sri.ai.grinder.sgdpllt.library.FunctorConstants.IN;
import static com.sri.ai.grinder.sgdpllt.library.FunctorConstants.PLUS;
import static com.sri.ai.grinder.sgdpllt.library.FunctorConstants.SUM;
import static com.sri.ai.grinder.sgdpllt.library.FunctorConstants.TIMES;
import static com.sri.ai.grinder.sgdpllt.library.set.extensional.ExtensionalSets.getElements;
import static com.sri.ai.grinder.sgdpllt.library.set.extensional.ExtensionalSets.removeNonDestructively;
import static com.sri.ai.util.Util.in;
import static com.sri.ai.util.Util.mapIntoArrayList;
import static com.sri.ai.util.Util.println;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.ExtensionalSet;
import com.sri.ai.expresso.api.IndexExpressionsSet;
import com.sri.ai.expresso.api.IntensionalSet;
import com.sri.ai.expresso.core.DefaultExistentiallyQuantifiedFormula;
import com.sri.ai.expresso.core.DefaultExtensionalUniSet;
import com.sri.ai.expresso.core.ExtensionalIndexExpressionsSet;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.Theory;
import com.sri.ai.grinder.sgdpllt.library.set.extensional.ExtensionalSets;
import com.sri.ai.util.base.NullaryFunction;
import com.sri.ai.util.collect.CartesianProductIterator;


public class Bounds {
	// a bound is a set of expressions representing its extreme points
	
	static boolean debug = false;
	
	
	/**
	 * Assumes that each element of the bound is a factor with the same domain
	 * Normalizes each factor of the bound. In latex notation: 
	 * 			{\phi/sum_{var(\phi)}\phi : \phi in bound} 
	 * @param bound
	 * @param theory
	 * @param context
	 * @return  bound of normalized factors
	 */
	public static Expression normalize(Expression bound, Theory theory, Context context){
		List<Expression> listOfBound = ExtensionalSets.getElements(bound);
		if(listOfBound.size() == 0){
			return null;
		}
		
		Expression phi = makeSymbol("phi");
	
		Expression phi1 = listOfBound.get(0);
		IndexExpressionsSet indices = getIndexExpressionsOfFreeVariablesIn(phi1, context);
		Expression noCondition = makeSymbol(true);
		Expression setOfFactorInstantiations = IntensionalSet.makeMultiSet(
				indices,
				phi,//head
				noCondition);
		
		Expression sumOnPhi = apply(SUM, setOfFactorInstantiations);
		Expression f =  apply("/", phi, sumOnPhi);
		Expression result = applyFunctionToBound(f, phi, bound, theory, context);
		return result;
	}
	
	
	/**
	 * Computes the product of each term of a list of bounds
	 * @param theory
	 * @param context
	 * @param listOfBounds
	 * @return bound resulting from the product of bounds
	 */
	public static Expression boundProduct(Theory theory, Context context, Expression...listOfBounds){
		
		ArrayList<NullaryFunction<Iterator<Expression>>> iteratorForBoundList = 
				mapIntoArrayList(listOfBounds, bound -> () -> getElements(bound).iterator());
		
		Iterator<ArrayList<Expression>> cartesianProduct = new CartesianProductIterator<Expression>(iteratorForBoundList);
		
		ArrayList<Expression> resultList = new ArrayList<>();
		for (ArrayList<Expression> element : in(cartesianProduct)){
			Expression product = apply("*",element);
			Expression evaluation = theory.evaluate(product,context);
			resultList.add(evaluation);
		}
		
		Expression result =  new DefaultExtensionalUniSet(resultList);
		
		// Updating extreme points
		result = updateExtremes(result, theory, context);
		
		return result;		
	}
	/*public static Expression boundProduct(Theory theory, Context context, Expression...listOfBounds){
		if(listOfBounds.length == 0){ 
			return null;
		}
		
		Expression result= boundProduct (0, theory, context, listOfBounds);
		return result;
	}

	private static Expression boundProduct(int i,  Theory theory, Context context, Expression...listOfBounds){
		if(listOfBounds.length - 1 == i){
			return listOfBounds[i];
		}
		
		Expression productOfOthers = boundProduct(i + 1, theory, context, listOfBounds);
		Expression b = listOfBounds[i];

		List<Expression> listOfb = ExtensionalSet.getElements(b);
		List<Expression> listOfProductOfOthers = ExtensionalSet.getElements(productOfOthers);
		
		ArrayList<Expression> elements = new ArrayList<>(listOfb.size()*listOfProductOfOthers.size());
		
		for (Expression phi1 : listOfb){
			for (Expression phi2 : listOfProductOfOthers){
				Expression product = apply("*",phi1,phi2);
				Expression evaluation = theory.evaluate(product,context);
				elements.add(evaluation);
			}
		}
		
		DefaultExtensionalUniSet productBound = new DefaultExtensionalUniSet(elements);
		//Updating extreme points
		Expression result = updateExtremes(productBound,theory,context);
		return result;
	}*/
	
	/**
	 * apply a function (f) to each term of a bound (b) 
	 * @param f 
	 * 			function to be applied to the factors
	 * @param variableName
	 * 			The variable in f to be replaced by phi (for each phi in b). 
	 * 			If if is a function of the variable v, VariableName is v
	 * @param b
	 * 			Bound
	 * @param theory
	 * @param context
	 * @return {f(\phi) : \phi \in b}
	 */
	public static Expression applyFunctionToBound(Expression f, Expression variableName, Expression b, Theory theory, Context context){
		ExtensionalSet bAsExtensionalSet = (ExtensionalSet) b;
		int numberOfExtremes = bAsExtensionalSet.getArguments().size();
		ArrayList<Expression> elements = new ArrayList<>(numberOfExtremes);
		for(Expression phi : ExtensionalSets.getElements(bAsExtensionalSet)){
			Expression substitution = f.replaceAllOccurrences(variableName, phi, context);
			//debuging
			if (debug) println("evaluating: " + substitution);
			Expression evaluation = theory.evaluate(substitution, context); // problem in evaluation method...
			//debuging
			if (debug) println("result: " + evaluation);
			elements.add(evaluation);
		}
		DefaultExtensionalUniSet fOfb = new DefaultExtensionalUniSet(elements);
		//Updating extreme points
		Expression result = updateExtremes(fOfb,theory,context);		
		return result;
	}
	
	/**
	 * Eliminate factors not in Ext(C.Hull(B)) 
	 * @param B
	 * @return 
	 */
	private static Expression updateExtremes(Expression B,Theory theory, Context context){
		List<Expression> listOfB = getElements(B);
		ArrayList<Expression> elements = new ArrayList<>(listOfB.size());
		int indexPhi = 0;
		for(Expression phi : listOfB){
			if (isExtremePoint(phi,indexPhi,B,theory,context)){
				elements.add(phi);
			}
			indexPhi++;
		}
		DefaultExtensionalUniSet result = new DefaultExtensionalUniSet(elements);
		return result;
	}
	
	/**
	 * Checks if \phi is a convex combination of the elements in bound
	 * @param phi
	 * 			factor
	 * @param bound
	 * @return
	 */
	public static boolean isExtremePoint(Expression phi,int indexPhi, Expression bound, Theory theory, Context context){
		//TODO
		Expression boundWithoutPhi = removeNonDestructively(bound, indexPhi);//caro pq recopia a lista toda
		List<Expression> listOfB = getElements(boundWithoutPhi);
		int n = listOfB.size();
		
		Expression[] c = new Expression[n];
		for(int i = 0;i<n;i++){
			c[i] = makeSymbol("c" + i);
			context = context.extendWithSymbolsAndTypes("c" + i,"Real");
		}
		
		// 0<=ci<=1
		ArrayList<Expression> listOfC = new ArrayList<>(listOfB.size());
		for(int i = 0;i<n;i++){
			Expression cibetwen0And1 = 
					apply(AND,apply(GREATER_THAN_OR_EQUAL_TO,1,c[i]),
							  apply(GREATER_THAN_OR_EQUAL_TO,c[i],0)
						  );
			listOfC.add(cibetwen0And1);
		}
		Expression allcibetwen0And1 = apply(AND, listOfC);
		
		//sum over ci =1
		listOfC = new ArrayList<>(Arrays.asList(c));
		Expression sumOverCiEqualsOne = apply(EQUAL,1,apply(PLUS,listOfC));

		//sum of ci*phi1 = phi
		ArrayList<Expression> prodciphii = new ArrayList<>(listOfB.size());
		int i = 0;
		for(Expression phii : listOfB){
			prodciphii.add(apply(TIMES,phii,c[i]));
			i++;
		}
		Expression convexSum = apply(EQUAL,phi,apply(PLUS, prodciphii));
		
		ArrayList<Expression> listOfCiInReal = new ArrayList<>(listOfB.size());
		for(i = 0; i <n; i++){
			listOfCiInReal.add(apply(IN,c[i],"Real"));
		}
		IndexExpressionsSet thereExistsCiInReal = new ExtensionalIndexExpressionsSet(listOfCiInReal);
		
		Expression body = apply(AND, allcibetwen0And1, sumOverCiEqualsOne, convexSum);
		Expression isExtreme = new DefaultExistentiallyQuantifiedFormula(thereExistsCiInReal,body);
		
		if (debug) println(isExtreme);
		//Expression result = theory.evaluate(isExtreme, context);
		return true;
	}	
}