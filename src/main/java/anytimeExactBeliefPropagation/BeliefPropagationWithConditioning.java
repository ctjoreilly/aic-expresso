package anytimeExactBeliefPropagation;

import static com.sri.ai.util.Util.arrayList;
import static com.sri.ai.util.Util.println;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.core.DefaultExtensionalMultiSet;
import com.sri.ai.grinder.sgdpllt.library.bounds.Bound;
import com.sri.ai.grinder.sgdpllt.library.bounds.Bounds;

import anytimeExactBeliefPropagation.Model.Model;
import anytimeExactBeliefPropagation.Model.Node.FactorNode;
import anytimeExactBeliefPropagation.Model.Node.VariableNode;

public class BeliefPropagationWithConditioning {
	private Model model;
	private boolean AllExplored;
	public PartitionTree partitionTree;
	
	public BeliefPropagationWithConditioning(Model model) {
		this.model = model;
		
		VariableNode query = model.getQuery();
		this.partitionTree = new PartitionTree(query,model);
		
		AllExplored = false;
	}
	
	public Bound inference(){
		AllExplored = model.AllExplored();
		
		Bound result = variableMessage(partitionTree, new HashSet<VariableNode>());
	
		return result;
	}
	
	public Bound variableMessage(PartitionTree partitionInAVariableNode, Set<VariableNode> SeparatorVariablesOnLevelAbove){//or notToSumVariables
		if(!partitionInAVariableNode.node.isVariable()){
			println("error in S-BP!!!");
			return null;
		}
		/** 
		 * compute the separator. 3 types:
		 * 						separators for levels above this 	(SeparatorVariablesOnLevelAbove)
		 * 						separators for this level 			(SeparatorOnThisLevel)
		 * 						separators for levels below this 	(SeparatorForNextLevels)
		 */
		Set<VariableNode> SeparatorOnThisLevel = ComputeSeparator(partitionInAVariableNode);
		SeparatorOnThisLevel.remove((VariableNode) partitionInAVariableNode.node);
		//exclude the variables on other levels. they will be summed afterwards(TODO not so sure about it...)
		SeparatorOnThisLevel.removeAll(SeparatorVariablesOnLevelAbove);
		
		Set<VariableNode> SeparatorForNextLevels = new HashSet<>();
		SeparatorForNextLevels.addAll(SeparatorOnThisLevel);
		SeparatorForNextLevels.addAll(SeparatorVariablesOnLevelAbove);
		
		// calling children partitions. for each partition, call message passing, 
		// store bound
		Bound[]  boundsOfChildrenMessages = new Bound[partitionInAVariableNode.partition.size()];
		Set<Expression> variablesToSumOut = new HashSet<>();
		for(VariableNode v : SeparatorOnThisLevel){
			variablesToSumOut.add(v.getValue());
		}
		
		// if this node is not exhausted (see definition in Model) it means that the message coming to it is the 
		// simplex, no matter how it is what comes below in the partition.
		// obs. it can be equivalently thought as attaching a "simplex factor" to non exhausted nodes.
		if(!AllExplored && !model.isExhausted((VariableNode) partitionInAVariableNode.node)){
			Expression var = partitionInAVariableNode.node.getValue();
			Bound bound = Bounds.simplex(arrayList(var), model.getTheory(), model.getContext(), model.isExtensional());
//			partitionInAVariableNode.node.setBound(bound);
			return bound;
		}
		
		int i = 0;
		for(PartitionTree p : partitionInAVariableNode.partition){
			Bound boundInP = factorMessage(p,SeparatorForNextLevels);
			//Bound boundInP = p.node.getBound();
			boundsOfChildrenMessages[i] = boundInP;
			i++;
		}
		
		Bound bound = Bounds.boundProduct(model.getTheory(), model.getContext(), model.isExtensional(), boundsOfChildrenMessages);
		
		ArrayList<Expression> varToSumOutList = new ArrayList<>();
		varToSumOutList.addAll(variablesToSumOut);
		Expression varToSumOut = new DefaultExtensionalMultiSet(varToSumOutList);
		
		bound = bound.summingBound(varToSumOut, model.getContext(), model.getTheory());
		
		return bound;
		//partitionInAVariableNode.node.setBound(bound);
	}
	public Bound factorMessage(PartitionTree partitionInAFactorNode, Set<VariableNode> SeparatorVariablesOnLevelAbove){
		if(!partitionInAFactorNode.node.isFactor()){
			println("error in S-BP!!!");
			return null;
		}
		/** 
		 * compute the separator. 3 types:
		 * 						separators for levels above this 	(SeparatorVariablesOnLevelAbove)
		 * 						separators for this level 			(SeparatorOnThisLevel)
		 * 						separators for levels below this 	(SeparatorForNextLevels)
		 */
		Set<VariableNode> SeparatorOnThisLevel = ComputeSeparator(partitionInAFactorNode);
		//exclude the variables on other levels. they will be summed afterwards(TODO not so sure about it...)
		SeparatorOnThisLevel.removeAll(SeparatorVariablesOnLevelAbove);
		
		Set<VariableNode> SeparatorForNextLevels = new HashSet<>();
		SeparatorForNextLevels.addAll(SeparatorOnThisLevel);
		SeparatorForNextLevels.addAll(SeparatorVariablesOnLevelAbove);
		
		// calling children partitions. for each partition, call message passing, 
		// store VariableNode (we are going to sum them all out) and
		// store bound
		Bound[]  boundsOfChildrenMessages = new Bound[partitionInAFactorNode.partition.size()];
		Set<Expression> variablesToSumOut = new HashSet<>();
		for(VariableNode v : SeparatorOnThisLevel){
			variablesToSumOut.add(v.getValue());
		}
		
		int i =0;
		for(PartitionTree p : partitionInAFactorNode.partition){
			Bound boundInP = variableMessage(p,SeparatorForNextLevels);
			//Bound boundInP = p.node.getBound();
			boundsOfChildrenMessages[i] = boundInP;
			variablesToSumOut.add(p.node.getValue());
			i++;
		}
		
		for(VariableNode v : SeparatorVariablesOnLevelAbove){
			variablesToSumOut.remove(v.getValue());
		}
		
		 
		Bound bound = Bounds.boundProduct(model.getTheory(), model.getContext(), model.isExtensional(), boundsOfChildrenMessages);
		
		ArrayList<Expression> varToSumOutList = new ArrayList<>();
		varToSumOutList.addAll(variablesToSumOut);
		Expression varToSumOut = new DefaultExtensionalMultiSet(varToSumOutList);
		
		bound = bound.summingPhiTimesBound(varToSumOut, partitionInAFactorNode.node.getValue(), model.getContext(), model.getTheory());
		return bound;
		//partitionInAFactorNode.node.setBound(bound);
	}
		
	/**
	 * Given the partition, compute the separator. TODO more efficient implementation
	 * @param p
	 * @return
	 */
	public Set<VariableNode> ComputeSeparator(PartitionTree pTree){
		//Create sets with the variables in each partition
		List<Set<VariableNode>> VariablePartition = new ArrayList<Set<VariableNode>>();
		for(PartitionTree p : pTree.partition){
			Set<VariableNode> variablesOfP = new HashSet<>();
			for(FactorNode phi : p.setOfFactors){
				Collection<VariableNode> VarsOfPhi= model.getExploredGraph().getAsOfB(phi);
				variablesOfP.addAll(VarsOfPhi);
			}
			VariablePartition.add(variablesOfP);
		}
		//take the variables that compose the intersection of those sets
		Set<VariableNode> separatorVariables = new HashSet<>();
		
		for (int i = 0; i < VariablePartition.size(); i++) {
			for (int j = i+1; j <VariablePartition.size(); j++) {
				Set<VariableNode> intersectionAti = new HashSet<>();
				intersectionAti.addAll(VariablePartition.get(i));
				intersectionAti.retainAll(VariablePartition.get(j));
				
				separatorVariables.addAll(intersectionAti);
			}
		}
		return separatorVariables;
	}
}