package com.sri.ai.grinder.sgdpllt.anytime;
import com.sri.ai.util.collect.ManyToManyRelation;
import static com.sri.ai.expresso.helper.Expressions.apply;
import static com.sri.ai.expresso.helper.Expressions.makeSymbol;
import static com.sri.ai.grinder.sgdpllt.library.FunctorConstants.IF_THEN_ELSE;
import static com.sri.ai.grinder.sgdpllt.library.FunctorConstants.EQUAL;
import static com.sri.ai.grinder.sgdpllt.library.FunctorConstants.GREATER_THAN;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.core.DefaultSymbol;

public class Model {
	public ManyToManyRelation<Expression, Expression> Map;
	public Set<VariableComponent> InitializeVComponent;
	public Set<FactorComponent> InitializeFComponent;
	
	public Model(Set<Expression> Factor){
		this.Map = new ManyToManyRelation<Expression, Expression> ();
		this.InitializeFComponent = new HashSet<FactorComponent>();
		this.InitializeVComponent = new HashSet<VariableComponent>();
		for (Expression f : Factor){
			for (Expression v : getVariables(f)){
				Map.add(f, v);
			}
		}
	}
	
	public static Set<Expression> getVariables(Expression f){
		Set<Expression> res = new HashSet<Expression>();
		for (Expression e : f.getArguments()){
			if (e.getFunctor()==null){
				res.add(e);
			}
			else{
				res.addAll(getVariables(e));
			}
		}
		return res;
	}
	
	public Set<Expression> getNeighbors(Expression e){
		Set<Expression> res = new HashSet<Expression>();
		if (e.getFunctor() == null){
			res.addAll(Map.getAsOfB(e));
			return res;
		}
		else {
			res.addAll(Map.getBsOfA(e));
			return res;
		}
	}
	
	public Set<Expression> getNeighborsOfSet(Set<Expression> E){
		Set<Expression> res = new HashSet<Expression>();
		if (E == null){
			return res;
		}
		for (Expression e : E){
			res.addAll(getNeighbors(e));
		}
		return res;
	}
	
	public Collection<Expression> getFactor(){
		return Map.getAs();
	}
	
	public Collection<Expression> getVariable(){
		return Map.getBs();
	}
	
	public void printInitialized(){
		System.out.println("Variables : ");
		for (VariableComponent v : this.InitializeVComponent){
			System.out.println("\t" + v.V);
			System.out.println("\t\t" + "Parents" + v.Parent);
			System.out.println("\t\t" + "Dext" + v.Dext);
			System.out.println("\t\t" + "D" + v.D);
		}
		System.out.println("Factor : ");
		for (FactorComponent p : this.InitializeFComponent){
			System.out.println("\t" + p.Phi);
			System.out.println("\t\t" + "Parents" + p.Parent);
			System.out.println("\t\t" + "Dext" + p.Dext);
			System.out.println("\t\t" + "D" + p.D);
		}
			
	}
	
	public Set<Expression> getInitializedFactor(){
		Set<Expression> res = new HashSet<Expression>();
		for (FactorComponent f : this.InitializeFComponent){
			res.add(f.Phi);
		}
		return res;
	}
	
	public Set<Expression> getInitializedVariable(){
		Set<Expression> res = new HashSet<Expression>();
		for (VariableComponent v : this.InitializeVComponent){
			res.add(v.V);
		}
		return res;
	}
}