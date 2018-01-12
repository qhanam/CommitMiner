package multidiffplus.jsanalysis.flow;

import java.util.Set;

import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;

/**
 * Stores the state of the flow analysis at one point in the program.
 */
public class Instruction {

	/** The edge to traverse next. **/
	public CFGEdge edge;
	
	/** The set of edges already visited. **/
	public Set<CFGEdge> visited;

	public Instruction (CFGEdge edge, Set<CFGEdge> visited) {
		this.edge = edge;
		this.visited = visited;
	}
	
	public Integer getInstructionID() {
		return edge.getTo().getId();
	}
	
	public CFGNode getInstruction() {
		return edge.getTo();
	}
	
	public boolean hasMappedInstruction() {
		return edge.getTo().getMappedNode() != null;
	}
	
	public CFGNode getMappedInstruction() {
		return edge.getTo().getMappedNode();
	}
	
	@Override
	public String toString() {
		return edge.getTo().getStatement().toString();
	}

}
