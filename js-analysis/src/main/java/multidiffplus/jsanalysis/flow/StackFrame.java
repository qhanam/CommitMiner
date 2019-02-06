package multidiffplus.jsanalysis.flow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGNode;
import multidiffplus.jsanalysis.abstractdomain.State;

/**
 * A frame in a call stack.
 */
public class StackFrame {

    /**
     * The set of edges (instructions) that have been visited within this stack
     * frame.
     */
    private Set<Integer> visitedEdges;

    /**
     * The set of nodes (instructions) that have been visited within this stack
     * frame.
     */
    private Map<Integer, ExpressionInstruction> visitedNodes;

    /**
     * The CFG for the function being analyzed.
     */
    private CFG cfg;

    /**
     * Stack of instructions to traverse.
     */
    private Stack<Instruction> kontinuation;

    public StackFrame(CFG cfg, State initialState) {

	// Initialize member vars.
	this.cfg = cfg;
	this.kontinuation = new Stack<Instruction>();
	this.visitedEdges = new HashSet<Integer>();
	this.visitedNodes = new HashMap<Integer, ExpressionInstruction>();

	// Add the first instruction for a depth-first traversal.
	Instruction instruction = getInstructionForNode(cfg.getEntryNode());
	instruction.initPreTransferState(initialState);
	this.kontinuation.add(instruction);

    }

    /**
     * @return The instruction for the control flow node.
     */
    public Instruction getInstructionForNode(CFGNode node) {
	if (visitedNodes.containsKey(node.getId())) {
	    return visitedNodes.get(node.getId());
	}
	ExpressionInstruction instruction = new ExpressionInstruction(node);
	visitedNodes.put(node.getId(), instruction);
	return instruction;
    }

    /**
     * @return {@code true} if the edge has been visited by the analysis within the
     *         stack frame.
     */
    public boolean visited(Integer edgeId) {
	return visitedEdges.contains(edgeId);
    }

    /**
     * Records that the edge has been visited by the analysis within the stack
     * frame.
     */
    public void visit(Integer edgeId) {
	visitedEdges.add(edgeId);
    }

    /**
     * @return {@code true} if the function analysis is complete.
     */
    public boolean isFinished() {
	return kontinuation.isEmpty();
    }

    /**
     * @return The function loaded into the stack frame.
     */
    public CFG getCFG() {
	return cfg;
    }

    /**
     * @return the number of edges visited by the analysis within the stack frame.
     */
    public int visitedEdges() {
	return visitedEdges.size();
    }

    /**
     * Pushes the instruction onto the stack frame.
     */
    public void pushInstruction(Instruction instruction) {
	kontinuation.push(instruction);
    }

    /**
     * @return {@code true} if the frame has more instructions to execute.
     */
    public boolean hasInstruction() {
	return !kontinuation.isEmpty();
    }

    /**
     * @return The next instruction in the stack frame.
     */
    public Instruction popInstruction() {
	return kontinuation.pop();
    }

    /**
     * @return The next instruction in the stack frame.
     */
    public Instruction peekInstruction() {
	return kontinuation.peek();
    }

}
