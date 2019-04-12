package multidiff.analysis.flow;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import multidiffplus.cfg.AnalysisState;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.cfg.CallSiteNode;

/**
 * A frame in a call stack.
 * 
 * A stack frame stores the state of a function being executed by the abstract
 * machine.
 */
public class StackFrame {

    /**
     * List of instructions to traverse.
     */
    private Queue<Instruction> instructions;

    private CFG cfg;

    public StackFrame(CFG cfg, AnalysisState initialState) {
	this.cfg = cfg;
	this.instructions = initializeInstructions(cfg, initialState);
    }

    /**
     * @return {@code true} if the frame has more instructions to execute.
     */
    public boolean hasInstruction() {
	return !instructions.isEmpty();
    }

    /**
     * @return The next instruction in the stack frame.
     */
    public Instruction popInstruction() {
	return instructions.remove();
    }

    /**
     * @return The next instruction in the stack frame.
     */
    public Instruction peekInstruction() {
	return instructions.element();
    }

    public CFG getCFG() {
	return cfg;
    }

    /**
     * Creates a queue of instructions that represents the intra-procedural control
     * flow analysis order for the stack frame.
     */
    private static Queue<Instruction> initializeInstructions(CFG cfg, AnalysisState initialState) {

	// The ordered instructions.
	Queue<Instruction> instructions = new ArrayDeque<>();
	cfg.getEntryNode().setBeforeState(initialState);
	instructions.add(new ExpressionInstruction(cfg.getEntryNode()));

	// The set of edges that have been visited within this
	// stack frame.
	Set<Integer> visitedEdges = new HashSet<Integer>();

	// A map of nodes to the number of incoming edges that have been visited.
	Map<Integer, Integer> visitedNodes = new HashMap<>();

	// Initialize the queue with the edges leaving the entry node.
	Queue<CFGEdge> edges = new ArrayDeque<>();
	visitedNodes.put(cfg.getEntryNode().getId(), 0);
	cfg.getEntryNode().getOutgoingEdges().forEach(edge -> {
	    visitedEdges.add(edge.getId());
	    edges.add(edge);
	});

	while (!edges.isEmpty()) {
	    // Add a new instruction for the edge's branch condition.
	    CFGEdge edge = edges.remove();
	    addBranchInstructions(edge, instructions);

	    CFGNode node = edge.getTo();

	    // Decrement the number of incoming edges that have not yet been visited.
	    int semaphore;
	    if (visitedNodes.containsKey(node.getId()))
		semaphore = visitedNodes.get(node.getId());
	    else
		semaphore = node.getIncommingEdgeCount();
	    semaphore = semaphore - 1;
	    visitedNodes.put(node.getId(), semaphore);

	    List<CFGEdge> unvisitedEdges = node.getOutgoingEdges().stream().filter(
		    child -> !child.outgoingLoopEdge && !visitedEdges.contains(child.getId()))
		    .collect(Collectors.toList());

	    List<CFGEdge> unvisitedLoopEdges = node.getOutgoingEdges().stream().filter(
		    child -> child.outgoingLoopEdge && !visitedEdges.contains(child.getId()))
		    .collect(Collectors.toList());

	    if (semaphore == 0) {
		// There are no more incoming edges.
		addNodeInstructions(node, instructions);
		unvisitedEdges.forEach(child -> {
		    edges.add(child);
		});
	    }

	    if (semaphore != 0 && semaphore == node.getIncommingEdges().stream()
		    .filter(e -> e.incomingLoopEdge).count()) {
		// } else if (semaphore == unvisitedLoopEdges.size()) {
		// There are only incoming edges that are reachable from outgoing edges.
		addNodeInstructions(node, instructions);
		unvisitedLoopEdges.forEach(child -> {
		    edges.add(child);
		});
	    }
	}

	return instructions;

    }

    /**
     * Add an ExpressionInstruction, preceded by any CallSiteInstructions contained
     * within the expression.
     */
    private static void addNodeInstructions(CFGNode node, Queue<Instruction> instructions) {
	for (CallSiteNode callSiteNode : node.getCallSiteNodes()) {
	    instructions.add(new CallSiteInstruction(callSiteNode));
	}
	instructions.add(new ExpressionInstruction(node));
    }

    private static void addBranchInstructions(CFGEdge edge, Queue<Instruction> instructions) {
	for (CallSiteNode callSiteNode : edge.getCallSiteNodes()) {
	    instructions.add(new CallSiteInstruction(callSiteNode));
	}
	instructions.add(new BranchInstruction(edge));
    }

}
