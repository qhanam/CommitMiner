package multidiff.analysis.flow;

import multidiffplus.cfg.AnalysisState;

/**
 * A source code instruction.
 */
public abstract class Instruction {

    /**
     * Transfers the state over this instruction.
     * 
     * @param incomingState
     *            incoming state from the previous instruction.
     * @return outgoing state from this instruction.
     */
    public void transfer(CallStack callStack) {
	StackFrame stackFrame = callStack.peek();

	/* Transfer the abstract state over the node. */
	AnalysisState postTransferState = transferStateOverInstruction(callStack);

	// Has a frame been added to the call stack?
	if (callStack.peek() == stackFrame) {
	    // The state has been transfered over the current instruction.
	    // Remove it from the kontinuation stack so that the next
	    // instruction can be analyzed.
	    stackFrame.popInstruction();
	    joinPostTransferState(postTransferState);
	    addInstructionsToKontinuation(callStack, postTransferState);
	} else {
	    // A new stack frame has been added to the call stack. We must
	    // analyze it before continuing with the current instruction and
	    // therefore do not update the post-transfer state.
	}
    }

    /**
     * Pushes the next instructions onto the frame's kontinuation.
     */
    public abstract void addInstructionsToKontinuation(CallStack callStack,
	    AnalysisState incomingState);

    /**
     * Updates the abstract state by evaluating the instruction.
     */
    protected abstract AnalysisState transferStateOverInstruction(CallStack callStack);

    /**
     * Returns the state of the program before the instruction executes.
     */
    protected abstract AnalysisState getPreTransferState();

    /**
     * Joins the incoming state with the state of the program before the instruction
     * is interpreted.
     */
    protected abstract void joinPreTransferState(AnalysisState incomingState);

    /**
     * Joins the outgoing state with the state of the program after the instruction
     * is interpreted.
     */
    protected abstract void joinPostTransferState(AnalysisState outgoingState);

}
