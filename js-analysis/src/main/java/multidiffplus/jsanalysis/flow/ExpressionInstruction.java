package multidiffplus.jsanalysis.flow;

import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.transfer.ExpEval;
import multidiffplus.jsanalysis.transfer.TransferNode;

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
    public void addInstructionsToKontinuation(CallStack callStack, State incomingState) {
	StackFrame stackFrame = callStack.peek();
	for (CFGEdge edge : node.getOutgoingEdges()) {
	    // Add an instruction (edge) to the stack frame if:
	    // (1) The edge has not been visited.
	    // (2) semaphore == 0 OR the edge is a loop edge
	    // Loops are executed once.
	    if (!stackFrame.visited(edge.getId()) && (semaphore == 0 || edge.isLoopEdge)) {
		// TODO: Initialize the pre-transfer state.
		Instruction instruction = new BranchInstruction(edge);
		instruction.initPreTransferState(incomingState);
		stackFrame.visit(edge.getId());
		stackFrame.pushInstruction(instruction);
	    }
	}
    }

    @Override
    protected void transferOverInstruction(CallStack callStack, State postTransferState) {
	TransferNode transferFunction = new TransferNode(postTransferState, node,
		new ExpEval(callStack, postTransferState));
	transferFunction.transfer();
    }

    @Override
    protected void setPostTransferState(State state) {
	semaphore--;
	node.setAfterState(state);
    }

    @Override
    protected State getPreTransferState() {
	return (State) node.getBeforeState();
    }

    @Override
    protected void setPreTransferState(State state) {
	node.setBeforeState(state);
    }

    @Override
    public String toString() {
	return node.getStatement().toString();
    }

}