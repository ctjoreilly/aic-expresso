package com.sri.ai.test.grinder.plaindpll.application;

import static com.sri.ai.expresso.helper.Expressions.parse;
import static com.sri.ai.grinder.plaindpll.application.Compilation.compile;
import static com.sri.ai.util.Util.list;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.helper.GrinderUtil;
import com.sri.ai.grinder.plaindpll.api.InputTheory;
import com.sri.ai.grinder.plaindpll.theory.AtomsOnConstraintTheoryWithEquality;
import com.sri.ai.grinder.plaindpll.theory.DefaultInputTheory;
import com.sri.ai.grinder.plaindpll.theory.EqualityConstraintTheory;
import com.sri.ai.grinder.plaindpll.theory.term.SymbolTermTheory;
import com.sri.ai.util.Util;

public class CompilationTest {

	@Test
	public void test() {
		
		Expression input; 
		Expression expected;
		InputTheory theory;
		Map<String, String> mapFromCategoricalTypeNameToSizeString;
		Map<String, String> mapFromVariableNameToTypeName;

		GrinderUtil.setTraceAndJustificationOffAndTurnOffConcurrency();
		
		theory = new DefaultInputTheory(new EqualityConstraintTheory(new SymbolTermTheory()));
		input = Expressions.parse(""
						+ "if X = a and Y = a and Z = a then 0.1 else "
						+ "if X = a and Y = a and Z = b then 0.1 else "
						+ "if X = a and Y = a and Z = c then 0.1 else "
						+ "if X = a and Y = b and Z = a then 0.1 else "
						+ "if X = a and Y = b and Z = b then 0.1 else "
						+ "if X = a and Y = b and Z = c then 0.1 else "
						+ "if X = a and Y = c and Z = a then 0.1 else "
						+ "if X = a and Y = c and Z = b then 0.1 else "
						+ "if X = a and Y = c and Z = c then 0.1 else "
						+ "if X = b and Y = a and Z = a then 0.2 else "
						+ "if X = b and Y = a and Z = b then 0.2 else "
						+ "if X = b and Y = a and Z = c then 0.2 else "
						+ "if X = b and Y = b and Z = a then 0.2 else "
						+ "if X = b and Y = b and Z = b then 0.2 else "
						+ "if X = b and Y = b and Z = c then 0.2 else "
						+ "if X = b and Y = c and Z = a then 0.2 else "
						+ "if X = b and Y = c and Z = b then 0.2 else "
						+ "if X = b and Y = c and Z = c then 0.2 else "
						+ "if X = c and Y = a and Z = a then 0.3 else "
						+ "if X = c and Y = a and Z = b then 0.3 else "
						+ "if X = c and Y = a and Z = c then 0.3 else "
						+ "if X = c and Y = b and Z = a then 0.3 else "
						+ "if X = c and Y = b and Z = b then 0.3 else "
						+ "if X = c and Y = b and Z = c then 0.3 else "
						+ "if X = c and Y = c and Z = a then 0.3 else "
						+ "if X = c and Y = c and Z = b then 0.3 else "
						+  /* X = c and Y = c and Z = c ; no need as it is implied by domain definition */  "0.3"); 
		expected = parse("if X = a then 0.1 else if X = b then 0.2 else 0.3");
		mapFromCategoricalTypeNameToSizeString   = Util.map("Everything", "3");
		mapFromVariableNameToTypeName = Util.map("X", "Everything", "Y", "Everything", "Z", "Everything");
		runTest(input, expected, theory, mapFromCategoricalTypeNameToSizeString, mapFromVariableNameToTypeName);
		
		// Same thing, but with non-capitalized variables that should still be recognized as variables
		theory = new DefaultInputTheory(new EqualityConstraintTheory(new SymbolTermTheory()));
		input = Expressions.parse(""
						+ "if x = a and y = a and z = a then 0.1 else "
						+ "if x = a and y = a and z = b then 0.1 else "
						+ "if x = a and y = a and z = c then 0.1 else "
						+ "if x = a and y = b and z = a then 0.1 else "
						+ "if x = a and y = b and z = b then 0.1 else "
						+ "if x = a and y = b and z = c then 0.1 else "
						+ "if x = a and y = c and z = a then 0.1 else "
						+ "if x = a and y = c and z = b then 0.1 else "
						+ "if x = a and y = c and z = c then 0.1 else "
						+ "if x = b and y = a and z = a then 0.2 else "
						+ "if x = b and y = a and z = b then 0.2 else "
						+ "if x = b and y = a and z = c then 0.2 else "
						+ "if x = b and y = b and z = a then 0.2 else "
						+ "if x = b and y = b and z = b then 0.2 else "
						+ "if x = b and y = b and z = c then 0.2 else "
						+ "if x = b and y = c and z = a then 0.2 else "
						+ "if x = b and y = c and z = b then 0.2 else "
						+ "if x = b and y = c and z = c then 0.2 else "
						+ "if x = c and y = a and z = a then 0.3 else "
						+ "if x = c and y = a and z = b then 0.3 else "
						+ "if x = c and y = a and z = c then 0.3 else "
						+ "if x = c and y = b and z = a then 0.3 else "
						+ "if x = c and y = b and z = b then 0.3 else "
						+ "if x = c and y = b and z = c then 0.3 else "
						+ "if x = c and y = c and z = a then 0.3 else "
						+ "if x = c and y = c and z = b then 0.3 else "
						+  /* x = c and y = c and z = c ; no need as it is implied by domain definition */  "0.3"); 
		expected = parse("if x = a then 0.1 else if x = b then 0.2 else 0.3");
		mapFromCategoricalTypeNameToSizeString   = Util.map("Everything", "3");
		mapFromVariableNameToTypeName = Util.map("x", "Everything", "y", "Everything", "z", "Everything");
		runTest(input, expected, theory, mapFromCategoricalTypeNameToSizeString, mapFromVariableNameToTypeName);

		theory = new DefaultInputTheory(new AtomsOnConstraintTheoryWithEquality(new EqualityConstraintTheory(new SymbolTermTheory())));
		input = Expressions.parse(""
						+ "if not g0 and (g1 = consg1_0)\r\n" + 
						"then 0.0001\r\n" + 
						"else if not g0 and (g1 = consg1_1)\r\n" + 
						"     then 1\r\n" + 
						"     else if not g0 and (g1 = consg1_2)\r\n" + 
						"          then 0.0001\r\n" + 
						"          else if not g0 and (g1 = consg1_3)\r\n" + 
						"               then 1\r\n" + 
						"               else if g0 and (g1 = consg1_0)\r\n" + 
						"                    then 1\r\n" + 
						"                    else if g0 and (g1 = consg1_1)\r\n" + 
						"                         then 1\r\n" + 
						"                         else if g0 and (g1 = consg1_2)\r\n" + 
						"                              then 1\r\n" + 
						"                              else 1\r\n" + 
				""); 
		expected = parse("if g0 then 1 else if g1 = consg1_0 then 0.0001 else if g1 = consg1_1 then 1 else if g1 = consg1_2 then 0.0001 else 1");
		mapFromCategoricalTypeNameToSizeString   = Util.map("G1Type", "4", "Boolean", "2");
		mapFromVariableNameToTypeName = Util.map("g0", "Boolean", "g1", "G1Type");
		runTest(input, expected, theory, mapFromCategoricalTypeNameToSizeString, mapFromVariableNameToTypeName);

		theory = new DefaultInputTheory(new AtomsOnConstraintTheoryWithEquality(new EqualityConstraintTheory(new SymbolTermTheory())));
		input = Expressions.parse("if not g0 then 1 else 1"); 
		expected = parse("1");
		mapFromCategoricalTypeNameToSizeString   = Util.map("G1Type", "4", "Boolean", "2");
		mapFromVariableNameToTypeName = Util.map("g0", "Boolean", "g1", "G1Type");
		runTest(input, expected, theory, mapFromCategoricalTypeNameToSizeString, mapFromVariableNameToTypeName);
	}

	private void runTest(Expression input, Expression expected, InputTheory theory, Map<String, String> mapFromCategoricalTypeNameToSizeString, Map<String, String> mapFromVariableNameToTypeName) {
		Expression actual = compile(input, theory, mapFromCategoricalTypeNameToSizeString, list(), mapFromVariableNameToTypeName);
		assertEquals(expected, actual);
	}
}
