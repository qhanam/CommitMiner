package multidiffplus.diff;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;

/**
 * Computes an interleaving for each function pair.
 */
public class Interleaving {

	Map<Integer, CFG> srcCFGs;
	Map<Integer, CFG> dstCFGs;

	public Interleaving(List<CFG> srcCFGs, List<CFG> dstCFGs) {

		this.srcCFGs = new HashMap<Integer, CFG>();
		this.dstCFGs = new HashMap<Integer, CFG>();

		for(CFG srcCFG : srcCFGs)
			this.srcCFGs.put(srcCFG.getEntryNode().getStatement().getID(), srcCFG);

		for(CFG dstCFG : dstCFGs)
			this.dstCFGs.put(dstCFG.getEntryNode().getStatement().getID(), dstCFG);

	}

	public void computeInterleavings() {

		for(CFG srcCFG : srcCFGs.values()) {

			/* Find the matching CFG. */
			ClassifiedASTNode matched = srcCFG.getEntryNode().getStatement().getMapping();
			if(matched == null) continue;

			Integer dstID = matched.getID();
			if(dstID == null) continue;
			
			CFG dstCFG = dstCFGs.get(dstID);
			if(dstCFG == null) continue;
			
			/* Store this match. */
			srcCFG.setMappedCFG(dstCFG);
			dstCFG.setMappedCFG(srcCFG);

			/* Find an interleaving. */
			getInterleaving(srcCFG, dstCFG);
			
		}
		
	}

	/**
	 * Compute an execution interleaving for two CFGs. Interleaved nodes are
	 * accessible through {@code CFGNode.getMatchedNode()};
	 */
	private void getInterleaving(CFG cfgA, CFG cfgB) {

		List<CFGNode> orderA = getInstructionOrder(cfgA);
		List<CFGNode> orderB = getInstructionOrder(cfgB);

		int j = 0;
		for(int i = 0; i < orderB.size(); i++) {

			CFGNode nodeB = orderB.get(i);
			if(nodeB.getStatement().getMapping() == null) continue;
			Integer mappedID = nodeB.getStatement().getMapping().getID();
			
			/* Greedily find the next node that matches. */
			CFGNode nodeA;
			do {

				nodeA = orderA.get(j);
				j++;
				if(j == orderA.size()) return;
				if(nodeA.getStatement().getID() == null) continue;

			} while(nodeA.getStatement().getID() != mappedID);
			
			/* Link the two nodes. */
			nodeA.setMappedNode(nodeB);
			nodeB.setMappedNode(nodeA);
			
		}

	}
	
	/**
	 * @return the order that instructions will be executed in this function.
	 */
	private List<CFGNode> getInstructionOrder(CFG cfg) {
		
		List<CFGNode> order = new LinkedList<CFGNode>();
		Map<CFGNode, Integer> iesMap = new HashMap<CFGNode, Integer>();
		Set<CFGEdge> visited = new HashSet<CFGEdge>();
		Stack<CFGNode> instructions = new Stack<CFGNode>();

		instructions.add(cfg.getEntryNode());
		while(!instructions.isEmpty()) {
			
			CFGNode instruction = instructions.pop();
			order.add(instruction);
			
			/* Look up the number of times this node has been visited in the
			 * visitedSemaphores map. */
			Integer semVal = iesMap.get(instruction);

			/* If it does not exist, put it in the map and initialize the
			 * semaphore value to the number of incoming edges for the node. */
			if(semVal == null) semVal = instruction.getIncommingEdgeCount();

			/* Decrement the semaphore by one since we visited the node. */
			semVal = semVal == 0 ? 0 : semVal - 1;
			iesMap.put(instruction, semVal);

			for(CFGEdge edge : instruction.getOutgoingEdges()) {

				/* Only visit an edge if the semaphore for the current node is zero or if one of the
				* edges is a loop edge. */
				if(!visited.contains(edge) 
						&& (semVal == 0 || edge.isLoopEdge)) {
					instructions.push(edge.getTo());
					visited.add(edge);
				}

			}
			
		}
		
		return order;
		
	}

}
