package multidiff.analysis.flow;

import multidiffplus.cfg.IState;

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
	IState postTransferState = transferStateOverInstruction(callStack);

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
    public abstract void addInstructionsToKontinuation(CallStack callStack, IState incomingState);

    /**
     * Updates the abstract state by evaluating the instruction.
     */
    protected abstract IState transferStateOverInstruction(CallStack callStack);

    /**
     * Returns the state of the program before the instruction executes.
     */
    protected abstract IState getPreTransferState();

    /**
     * Joins the incoming state with the state of the program before the instruction
     * is interpreted.
     */
    protected abstract void joinPreTransferState(IState incomingState);

    /**
     * Joins the outgoing state with the state of the program after the instruction
     * is interpreted.
     */
    protected abstract void joinPostTransferState(IState outgoingState);

}
