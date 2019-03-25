package multidiff.analysis.flow;

import multidiffplus.cfg.AnalysisState;
import multidiffplus.cfg.CFGEdge;

/**
 * An branch condition source code instruction.
 * 
 * Branch condition instructions appear within branch conditions.
 */
public class BranchInstruction extends Instruction {

    /** The edge containing the instruction. **/
    public CFGEdge edge;

    public BranchInstruction(CFGEdge edge) {
	this.edge = edge;
    }

    @Override
    public void addInstructionsToKontinuation(CallStack callStack, AnalysisState incomingState) {
	StackFrame stackFrame = callStack.peek();
	Instruction instruction = stackFrame.getInstructionForNode(edge.getTo());
	instruction.joinPreTransferState(incomingState);
	stackFrame.pushInstruction(instruction);
    }

    @Override
    protected AnalysisState transferStateOverInstruction(CallStack callStack) {
	AnalysisState postTransferState, preTransferState = edge.getBeforeState();
	postTransferState = preTransferState.interpretBranchCondition(edge.getCondition());
	return postTransferState;
    }

    @Override
    protected void joinPostTransferState(AnalysisState outgoingState) {
	if (edge.getAfterState() != null) {
	    outgoingState = outgoingState.join(edge.getAfterState());
	}
	edge.setAfterState(outgoingState);
    }

    @Override
    protected AnalysisState getPreTransferState() {
	return edge.getBeforeState();
    }

    @Override
    protected void joinPreTransferState(AnalysisState incomingState) {
	if (edge.getBeforeState() != null) {
	    incomingState = incomingState.join(edge.getAfterState());
	}
	edge.setBeforeState(incomingState);
    }

    @Override
    public String toString() {
	return edge.getCondition() == null ? "null" : edge.getCondition().toString();
    }

}