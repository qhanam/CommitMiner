package multidiff.analysis.flow;

import multidiffplus.cfg.AnalysisState;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;

/**
 * An expression source code instruction.
 * 
 * Expression instructions appear outside of branch conditions.
 */
public class ExpressionInstruction extends Instruction {

    /** The node containing the instruction. */
    private CFGNode node;

    /**
     * Tracks the number of incoming edges that have been followed to get to this
     * instruction.
     */
    private Integer semaphore;

    public ExpressionInstruction(CFGNode node) {
	this.node = node;
	this.semaphore = node.getIncommingEdgeCount() > 0 ? node.getIncommingEdgeCount() : 1;
    }

    @Override
    public void addInstructionsToKontinuation(CallStack callStack, AnalysisState incomingState) {
	StackFrame stackFrame = callStack.peek();
	for (CFGEdge edge : node.getOutgoingEdges()) {
	    // Add an instruction (edge) to the stack frame if:
	    // (1) The edge has not been visited.
	    // (2) semaphore == 0 OR the edge is a loop edge
	    // Loops are executed once.
	    if (!stackFrame.visited(edge.getId()) && (semaphore == 0 || edge.isLoopEdge)) {
		Instruction instruction = new BranchInstruction(edge);
		instruction.joinPreTransferState(incomingState);
		stackFrame.visit(edge.getId());
		stackFrame.pushInstruction(instruction);
	    }
	}
    }

    @Override
    protected AnalysisState transferStateOverInstruction(CallStack callStack) {
	AnalysisState postTransferState, preTransferState = node.getBeforeState();
	postTransferState = preTransferState.interpretStatement(node.getStatement());
	return postTransferState;
    }

    @Override
    protected void joinPostTransferState(AnalysisState outgoingState) {
	semaphore--;
	if (node.getAfterState() != null) {
	    outgoingState = outgoingState.join(node.getAfterState());
	}
	node.setAfterState(outgoingState);
    }

    @Override
    protected AnalysisState getPreTransferState() {
	return node.getBeforeState();
    }

    @Override
    protected void joinPreTransferState(AnalysisState incomingState) {
	if (node.getBeforeState() != null) {
	    incomingState = incomingState.join(node.getBeforeState());
	}
	node.setBeforeState(incomingState);
    }

    @Override
    public String toString() {
	return node.getStatement().toString();
    }

}