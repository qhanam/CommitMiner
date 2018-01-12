package multidiffplus.jsanalysis.flow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.time.StopWatch;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ScriptNode;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.factories.StateFactory;
import multidiffplus.jsanalysis.transfer.ExpEval;
import multidiffplus.jsanalysis.transfer.TransferEdge;
import multidiffplus.jsanalysis.transfer.TransferNode;

/**
 * Runs an analysis of a JavaScript function.
 */
public class Analysis {
	
	/** A stack frame that needs to be loaded. **/
	private StackFrame frameToLoad;
	
	/** {@code true} if a new frame was placed on the stack during transfer. **/
	private boolean newFrameLoaded;

	/** How long the analysis should run before timing out. **/
	private static final long TIMEOUT = 60;
	
	/** Used for timing out the analysis. **/
	private StopWatch timer = new StopWatch();

	/** A reference to the list of CFGs to use for executing methods. **/
	public Map<AstNode, CFG> cfgs;
	
	/**
	 * The virtual call stack.
	 */
	private Stack<StackFrame> callStack;
	
	/**
	 * @param cfg The initial CFG for the function.
	 * @param inAbsState The incoming abstract state of the system before the function is executed.
	 */
	private Analysis(StackFrame initialState, Map<AstNode, CFG> cfgs, StopWatch timer) {
		this.callStack = new Stack<StackFrame>();
		this.callStack.push(initialState);
		this.cfgs = cfgs;
		this.timer = timer;
		this.timer.start();
		this.timer.suspend();
	}
	
	/**
	 * @return the next instruction in the stack
	 */
	public Instruction peek() {
		return callStack.peek().instrStack.peek();
	}
	
	/**
	 * Advance the analysis to the next statement in the program.
	 * @return the instruction that was last executed
	 */
	public Instruction advance() {

		State postTransferState;

		timer.resume();
		
		/* Get the next function from the top of the stack. */
		StackFrame funState = callStack.pop();
		
		/* Get the next instruction from the top of the stack. */
		Instruction instruction = funState.instrStack.pop();
		
		switch(funState.progress) {
		case NONE:
			/* Transfer over the edge. */
			postTransferState = transferEdge(funState, instruction);
			if(postTransferState == null) {
				/* A function was added to the call stack. */
				timer.suspend();
				return advance(); // Problem here. Why is postTransferState null?
			}
		case EDGE:
		default:
			/* Transfer over the node. */
			postTransferState = transferNode(funState, instruction);
			if(newFrameLoaded) {
				/* A function was added to the call stack. */
				newFrameLoaded = false;
				timer.suspend();
				return advance();
			}
			else if(postTransferState == null) {
				timer.suspend();
				funState.completeNode();
				if(!funState.isFinished()) callStack.push(funState);
				return null;
			}
		}

		/* We are done visiting the edge. */
		funState.edgesVisited++;
		
		/* If we are not at the end of the function, push it back onto the stack. */
		if(!funState.isFinished()) 
			callStack.push(funState);

		timer.suspend();
		
		return instruction;

	}
	
	/**
	 * @return {@code true} if the analysis has completed.
	 */
	public boolean isFinished() {
		return this.callStack.isEmpty();
	}
	
	/**
	 * Tell the analysis to push the given function call onto the call stack.
	 */
	public void pushFunctionCall(StackFrame call) {
		this.frameToLoad = call;
	}
	
	/**
	 * @return The current call stack for the analysis. 
	 */
	public Stack<StackFrame> getCallStack() {
		return this.callStack;
	}

	/**
	 * @return a new instance of the analysis.
	 */
	public static Analysis build(ClassifiedASTNode root, List<CFG> cfgs) {
		
		/* Store params. */
		StopWatch timer = new StopWatch();
		
		/* Build a map of AstNodes to CFGs. Used for inter-proc CFA. */
		Map<AstNode, CFG> cfgMap = new HashMap<AstNode, CFG>();
		for(CFG cfg : cfgs) {
			cfg.attachTimer(timer, TIMEOUT);
			cfgMap.put((AstNode)cfg.getEntryNode().getStatement(), cfg);
		}

		/* Setup the analysis with the root script and an initial state. */
		State state = StateFactory.createInitialState((ScriptNode) root, cfgMap);
		return new Analysis(new StackFrame(cfgMap.get(root), state), cfgMap, timer);
		
	}
	
	/**
	 * Transfer over a CFG node.
	 * @return The abstract state after the transfer, or null if a function 
	 * call was added to the stack.
	 */
	private State transferNode(StackFrame funState, Instruction pathState) {

		/* Join the lattice elements from the current node and 'incoming'
		 * edge. */
		State preTransferState;
		if(pathState.edge.getTo().getBeforeState() == null)
			preTransferState = (State)pathState.edge.getAfterState();
		else
			preTransferState = ((State)pathState.edge.getAfterState()).join((State)pathState.edge.getTo().getBeforeState());
		pathState.edge.getTo().setBeforeState(preTransferState);

		/* If it does not exist, put it in the map and initialize the
		 * semaphore value to the number of incoming edges for the node. */
		Integer semVal = funState.iesMap.get(pathState.edge.getTo());
		if(semVal == null) semVal = pathState.edge.getTo().getIncommingEdgeCount() - 1;

		/* Transfer the abstract state over the node, only if we need to make
		* progress in the analysis by visiting a downstream edge. */
		if(!Analysis.doTransfer(pathState.edge.getTo(), semVal)) return null;

		/* Transfer the abstract state over the edge. */
		State postTransferState;
		postTransferState = preTransferState.clone();
		
		TransferNode transferFunction;
		transferFunction = new TransferNode(postTransferState, 
											pathState.edge.getTo(), 
											evaluator(postTransferState));
		transferFunction.transfer();

		/* If the transfer function requested a new call be pushed onto the
		* stack, we need to (1) 'pause' this analysis (by pushing this
		* instruction back onto the stack), and (2) 'call' the new function (by
		* push the new AnalysisState onto the call stack) */
		if(this.frameToLoad != null) {
			funState.instrStack.push(pathState);
			this.callStack.push(funState);
			this.callStack.push(this.frameToLoad);
			this.frameToLoad = null;
			this.newFrameLoaded = true;
			return null;
		}

		pathState.edge.getTo().setAfterState(postTransferState);

		/* Add all unvisited edges to the stack.
		 * We currently only execute loops once. */
		for(CFGEdge edge : pathState.edge.getTo().getOutgoingEdges()) {

			/* Only visit an edge if the semaphore for the current node is zero or if one of the
			* edges is a loop edge. */
			if(!pathState.visited.contains(edge)
					&& (semVal == 0 || edge.isLoopEdge)) {
				Set<CFGEdge> newVisited = new HashSet<CFGEdge>(pathState.visited);
				newVisited.add(edge);
				Instruction newState = new Instruction(edge, newVisited);
				funState.instrStack.push(newState);
			}

		}

		/* Update the state of progress to node complete. */
		funState.completeNode();

		return postTransferState;
		
	}

	/**
	 * Transfer over a CFG edge.
	 * @return The abstract state after the transfer, or null if a function 
	 * call was added to the stack.
	 */
	private State transferEdge(StackFrame funState, Instruction pathState) {
		
		/* Join the lattice elements from the current edge and 'from'
		 * node. */
		State preTransferState;
		preTransferState = ((State)pathState.edge.getFrom().getAfterState()).join((State)pathState.edge.getBeforeState());
		pathState.edge.setBeforeState(preTransferState);

		/* Transfer the abstract state over the edge. */
		State postTransferState;
		postTransferState = preTransferState.clone();

		TransferEdge transferFunction;
		transferFunction = new TransferEdge(postTransferState, 
											pathState.edge, 
											evaluator(postTransferState));
		transferFunction.transfer();

		/* If the transfer function requested a new call be pushed onto the
		* stack, we need to (1) 'pause' this analysis (by pushing this
		* instruction back onto the stack), and (2) 'call' the new function (by
		* push the new AnalysisState onto the call stack) */
		if(this.frameToLoad != null) {
			funState.instrStack.push(pathState);
			this.callStack.push(funState);
			this.callStack.push(this.frameToLoad);
			this.frameToLoad = null;
			return null;
		}

		/* Join with the old abstract state. */
		pathState.edge.setAfterState(postTransferState.join((State)pathState.edge.getAfterState()));

		/* Look up the number of times this node has been visited in the
		 * visitedSemaphores map. */
		Integer semVal = funState.iesMap.get(pathState.edge.getTo());

		/* If it does not exist, put it in the map and initialize the
		 * semaphore value to the number of incoming edges for the node. */
		if(semVal == null) semVal = pathState.edge.getTo().getIncommingEdgeCount();

		/* Decrement the semaphore by one since we visited the node. */
		semVal = semVal - 1;
		funState.iesMap.put(pathState.edge.getTo(), semVal);
		
		/* Update the state of progress to edge complete. */
		funState.completeEdge();
		
		return postTransferState;

	}
	
	/**
	 * @return an evaluator for the given state.
	 */
	public ExpEval evaluator(State state) {
		return new ExpEval(this, state);
	}

	/**
	 * Decide whether or not to transfer over a node. We transfer if (a) all
	 * incoming edges have been visited (semVal == 0) or if (b) there is an
	 * unvisited loop edge. If semVal == -1, do not proceed because a call has
	 * been added to the call stack and must first be analyzed.
	 * @param pathState The current state of the path.
	 * @param semVal The number of incoming edges that have not yet been visited.
	 * @return {@code true} if we need to transfer over the node.
	 */
	public static boolean doTransfer(CFGNode node, int semVal) {
		if(semVal == 0) return true;
		for(CFGEdge edge : node.getOutgoingEdges())
			if(edge.isLoopEdge) return true;
		return false;
	}

}
