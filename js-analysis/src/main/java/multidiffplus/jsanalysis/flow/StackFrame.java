package multidiffplus.jsanalysis.flow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.jsanalysis.abstractdomain.State;

/**
 * The analysis state for a function.
 */
public class StackFrame {
	
	/** Flag for deciding when the function execution is being interleaved. **/
	boolean interleaved;
	
	/** Flag for figuring out where to restart when analysis is paused. **/
	Progress progress;
	
	/** 
	 * Keep track of how many edges we've visited for terminating a long running
	 * analysis. **/
	long edgesVisited;
	
	/**
	 * The CFG for the function being analyzed.
	 */
	CFG cfg;
	
	/** 
	 * Stores semaphores for tracking the number of incoming edges that have
	 * been traversed to a node. Stands for "I(ncoming) E(dges) S{emaphore} Map.
	 */
	Map<CFGNode, Integer> iesMap;
	
	/**
	 * Stack of instructions to traverse.
	 */
	Stack<Instruction> instrStack;
	
	public StackFrame(CFG cfg, State absState) {
		
		/* Initialize member vars. */
		this.progress = Progress.NONE;
		this.cfg = cfg;
		this.edgesVisited = 0;
		this.iesMap = new HashMap<CFGNode, Integer>();
		this.instrStack = new Stack<Instruction>();
		
		/* Initialize the stack for a depth-first traversal. */
		for(CFGEdge edge : cfg.getEntryNode().getOutgoingEdges()) {
			this.instrStack.add(new Instruction(edge, new HashSet<CFGEdge>()));
		}
		
		/* Update the function's initial state. */
		cfg.getEntryNode().setBeforeState(absState);
		cfg.getEntryNode().setAfterState(absState);
		
		/* Assume no interleaving. */
		this.interleaved = false;

	}
	
	/**
	 * Set the interleaving flag.
	 */
	public void setInterleaved() {
		this.interleaved = true;
	}
	
	/**
	 * @return {@code true} if the exection of the stack frame is being
	 * interleaved.
	 */
	public boolean isInterleaved() {
		return this.interleaved;
	}
	
	/**
	 * @return {@code true} if the function analysis is complete.
	 */
	public boolean isFinished() {
		return instrStack.isEmpty();
	}
	
	/**
	 * Update the progress state to edge complete.
	 */
	public void completeEdge() {
		progress = Progress.EDGE;
	}
	
	/**
	 * Update the progress state to node complete (none for next instruction).
	 */
	public void completeNode() {
		progress = Progress.NONE;
	}
	
	/** Flag for figuring out where to restart when analysis is paused. **/
	public enum Progress {
		NONE, EDGE
	}
	
	/**
	 * @return The function loaded into the stack frame.
	 */
	public CFG getCFG() {
		return cfg;
	}

}
