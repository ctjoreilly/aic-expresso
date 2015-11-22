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
package com.sri.ai.grinder.core.simplifier;

import static com.sri.ai.util.Util.list;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.sri.ai.grinder.api.MapBasedSimplifier;
import com.sri.ai.grinder.api.Simplifier;
import com.sri.ai.util.collect.FunctionIterator;
import com.sri.ai.util.collect.NestedIterator;

/**
 * A collection of utility methods for merging maps of elementary simplifiers.
 * 
 * @author braz
 *
 */
@Beta
public class Merge  {

	/**
	 * Iterates over function application simplifiers, <b>going over the {@link MapBasedSimplifier}s first</b>
	 * to create an effect of overriding.
	 * @param functionApplicationSimplifiers
	 * @param simplifiers
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static
	Iterator<Map<String, Simplifier>>
	functionApplicationSimplifiersIterator(Map<String, Simplifier> functionApplicationSimplifiers, MapBasedSimplifier... simplifiers) {
		return new NestedIterator<>(functionApplicationSimplifiersIterator(simplifiers), list(functionApplicationSimplifiers));
	}

	/**
	 * Iterates over syntact form type simplifiers, <b>going over the {@link MapBasedSimplifier}s first</b>
	 * to create an effect of overriding.
	 * @param syntacticFormTypeSimplifiers
	 * @param simplifiers
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static
	Iterator<Map<String, Simplifier>>
	syntacticFormTypeSimplifiersIterator(Map<String, Simplifier> syntacticFormTypeSimplifiers, MapBasedSimplifier... simplifiers) {
		return new NestedIterator<>(syntacticFormTypeSimplifiersIterator(simplifiers), list(syntacticFormTypeSimplifiers));
	}

	public static
	Iterator<Map<String, Simplifier>>
	functionApplicationSimplifiersIterator(MapBasedSimplifier... simplifiers) {
		return FunctionIterator.make(simplifiersList(simplifiers), fromSimplifierToFunctionApplicationSimplifiers());
	}

	public static
	Iterator<Map<String, Simplifier>>
	syntacticFormTypeSimplifiersIterator(MapBasedSimplifier... simplifiers) {
		return FunctionIterator.make(simplifiersList(simplifiers), fromSimplifierToSyntacticFormTypeSimplifiers());
	}

	public static
	Function<MapBasedSimplifier, Map<String, Simplifier>>
	fromSimplifierToFunctionApplicationSimplifiers() {
		return s -> s.getFunctionApplicationSimplifiers();
	}

	private static
	Function<MapBasedSimplifier, Map<String, Simplifier>>
	fromSimplifierToSyntacticFormTypeSimplifiers() {
		return s -> s.getSyntacticFormTypeSimplifiers();
	}

	private static List<MapBasedSimplifier> simplifiersList(MapBasedSimplifier... simplifiers) {
		return Arrays.asList(simplifiers);
	}
	
}