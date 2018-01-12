package multidiffplus.cfg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.time.StopWatch;

/**
 * A low(er) level control flow graph or subgraph.
 *
 * The control flow graph contains an entry node where the graph begins. It
 * also keeps track of statements that exit the CFG. These include the last
 * statements in a block that exit the graph and jump statements including
 * break, continue, throw and return.
 */
public class CFG {
	
	private StopWatch timer;
	private long timeout;
	
	private CFGNode entryNode;
	private List<CFGNode> exitNodes;
	private List<CFGNode> breakNodes;
	private List<CFGNode> continueNodes;
	private List<CFGNode> throwNodes;
	private List<CFGNode> returnNodes;

	/** The corresponding source or destination CFG. */
	private CFG mappedCFG;

	public CFG(CFGNode entryNode) {
		this.entryNode = entryNode;
		this.exitNodes = new LinkedList<CFGNode>();
		this.breakNodes = new LinkedList<CFGNode>();
		this.continueNodes = new LinkedList<CFGNode>();
		this.throwNodes = new LinkedList<CFGNode>();
		this.returnNodes = new LinkedList<CFGNode>();
		this.timer = null;
	}
	
	/**
	 * Attach a timer to control the runtime of the analysis.
	 */
	public void attachTimer(StopWatch timer, long timeout) {
		this.timer = timer;
		this.timeout = timeout;
	}
	
	/**
	 * @return the runtime timer for the current analysis.
	 */
	public boolean hasTimedOut() {
		return (this.timer.getTime() / 1000) > this.timeout;
	}

	/**
	 * Accept a node visitor. Visit all nodes and edges in the CFG. Nodes and
	 * edges may be visited in any order.
	 */
	public void accept(ICFGVisitor visitor) {

		Set<CFGNode> visited = new HashSet<CFGNode>();
		Stack<CFGNode> stack = new Stack<CFGNode>();

		stack.push(this.getEntryNode());
		visited.add(this.getEntryNode());

		while(!stack.isEmpty()) {
			CFGNode node = stack.pop();
			node.accept(visitor);

			for(CFGEdge edge : node.getOutgoingEdges()) {
				edge.accept(visitor);
				if(!visited.contains(edge.getTo())) {
					stack.push(edge.getTo());
					visited.add(edge.getTo());
				}
			}
		}

	}

	/**
	 * @return a copy of the CFG.
	 */
	public CFG copy() {

		CFGNode entryNodeCopy = CFGNode.copy(this.entryNode);

		/* Keep track of the nodes we have copied. */
		Map<CFGNode, CFGNode> newNodes = new HashMap<CFGNode, CFGNode>();
		newNodes.put(this.entryNode, entryNodeCopy);

		/* Depth first traversal. */
		Stack<CFGNode> stack = new Stack<CFGNode>();
		stack.push(entryNodeCopy);

		/* Traverse and copy the CFG. */
		while(!stack.isEmpty()) {
			CFGNode node = stack.pop();

			/* Copy the list of edges. */
			List<CFGEdge> copiedEdges = new LinkedList<CFGEdge>(node.getOutgoingEdges());

			/* Re-assign the 'to' part of the edge. Copy the node if it hasn't
			 * been copied yet. */
			for(CFGEdge copiedEdge : copiedEdges) {
				CFGNode nodeCopy = newNodes.get(copiedEdge.getTo());
				if(nodeCopy == null) {
					nodeCopy = CFGNode.copy(copiedEdge.getTo());
					newNodes.put(copiedEdge.getTo(), nodeCopy);
					stack.push(nodeCopy);
				}
				copiedEdge.setTo(nodeCopy);
			}

			/* Assign the new edges to the current node. */
			node.setEdges(copiedEdges);
		}

		/* Copy the CFG and add all the jump nodes. */
		CFG cfg = new CFG(entryNodeCopy);

		for(CFGNode exitNode : this.exitNodes) {
			CFGNode node = newNodes.get(exitNode);
			if(node != null) cfg.addExitNode(node);
		}

		for(CFGNode node : this.breakNodes) {
			CFGNode copy = newNodes.get(node);
			if(copy != null) cfg.addExitNode(copy);
		}

		for(CFGNode node : this.continueNodes) {
			CFGNode copy = newNodes.get(node);
			if(copy != null) cfg.addExitNode(copy);
		}

		for(CFGNode node : this.returnNodes) {
			CFGNode copy = newNodes.get(node);
			if(copy != null) cfg.addExitNode(copy);
		}

		for(CFGNode node : this.throwNodes) {
			CFGNode copy = newNodes.get(node);
			if(copy != null) cfg.addExitNode(copy);
		}
		
		cfg.attachTimer(timer, timeout);

		return cfg;
	}

	/**
	 * Returns the entry node for this CFG.
	 * @return The entry Node.
	 */
	public CFGNode getEntryNode() {
		return entryNode;
	}

	/**
	 * Add an exit node to this CFG.
	 * @param node The last node before exiting an execution branch.
	 */
	public void addExitNode(CFGNode node) {
		this.exitNodes.add(node);
	}

	/**
	 * Adds all the exit nodes in the list.
	 * @param nodes
	 */
	public void addAllExitNodes(List<CFGNode> nodes) {
		this.exitNodes.addAll(nodes);
	}

	/**
	 * Get the exit nodes for this graph.
	 * @return The list of exit points.
	 */
	public List<CFGNode> getExitNodes() {
		return this.exitNodes;
	}

	/**
	 * Add a break node to this CFG.
	 * @param node The last node before breaking an execution branch.
	 */
	public void addBreakNode(CFGNode node) {
		this.breakNodes.add(node);
	}

	/**
	 * Adds all the break nodes in the list.
	 * @param nodes
	 */
	public void addAllBreakNodes(List<CFGNode> nodes) {
		this.breakNodes.addAll(nodes);
	}

	/**
	 * Get the break nodes for this graph.
	 * @return The list of break points.
	 */
	public List<CFGNode> getBreakNodes() {
		return this.breakNodes;
	}

	/**
	 * Add an continue node to this CFG.
	 * @param node The last node before continuing an execution branch.
	 */
	public void addContinueNode(CFGNode node) {
		this.continueNodes.add(node);
	}

	/**
	 * Adds all the continue nodes in the list.
	 * @param nodes
	 */
	public void addAllContinueNodes(List<CFGNode> nodes) {
		this.continueNodes.addAll(nodes);
	}

	/**
	 * Get the continue nodes for this graph.
	 * @return The list of continue points.
	 */
	public List<CFGNode> getContinueNodes() {
		return this.continueNodes;
	}

	/**
	 * Add an throw node to this CFG.
	 * @param node The last node before continuing an execution branch.
	 */
	public void addThrowNode(CFGNode node) {
		this.throwNodes.add(node);
	}

	/**
	 * Adds all the throw nodes in the list.
	 * @param nodes
	 */
	public void addAllThrowNodes(List<CFGNode> nodes) {
		this.throwNodes.addAll(nodes);
	}

	/**
	 * Get the throw nodes for this graph.
	 * @return The list of throw points.
	 */
	public List<CFGNode> getThrowNodes() {
		return this.throwNodes;
	}

	/**
	 * Add an return node to this CFG.
	 * @param node The last node before returning an execution branch.
	 */
	public void addReturnNode(CFGNode node) {
		this.returnNodes.add(node);
	}

	/**
	 * Adds all the return nodes in the list.
	 * @param nodes
	 */
	public void addAllReturnNodes(List<CFGNode> nodes) {
		this.returnNodes.addAll(nodes);
	}

	/**
	 * Get the return nodes for this graph.
	 * @return The list of return points.
	 */
	public List<CFGNode> getReturnNodes() {
		return this.returnNodes;
	}
	
	/**
	 * @return {@code true} if there is an interleaving between this CFG and the
	 * CFG of an alternate program version.
	 */
	public boolean hasMappedCFG() {
		if(mappedCFG == null) return false;
		return true;
	}
	
	/**
	 * @return the matched CFG in the alternate program version.
	 */
	public CFG getMappedCFG() {
		return mappedCFG;
	}
	
	/**
	 * @param cfg The matched CFG in an alternate program version.
	 */
	public void setMappedCFG(CFG mappedCFG) {
		this.mappedCFG = mappedCFG;
	}
	
}
