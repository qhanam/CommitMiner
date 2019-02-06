package multidiffplus.jsanalysis.flow;

import multidiffplus.jsanalysis.abstractdomain.State;

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
	initPreTransferState(callStack, stackFrame.getState());

	/* Transfer the abstract state over the node. */
	State postTransferState = getPreTransferState().clone();

	transferOverInstruction(callStack, postTransferState);

	// Has a frame been added to the call stack?
	if (callStack.peek() == stackFrame) {
	    // The state has been transfered over the current instruction.
	    // Remove it from the kontinuation stack so that the next
	    // instruction can be analyzed.
	    stackFrame.popInstruction();
	    stackFrame.setState(postTransferState);
	    setPostTransferState(postTransferState);
	    addInstructionsToKontinuation(callStack);
	} else {
	    // A new stack frame has been added to the call stack. We must
	    // analyze it before continuing with the current instruction and
	    // therefore do not update the post-transfer state.
	}
    }

    /**
     * Pushes the next instructions onto the frame's kontinuation.
     */
    public abstract void addInstructionsToKontinuation(CallStack callStack);

    protected abstract void transferOverInstruction(CallStack callStack, State postTransferState);

    protected abstract State getPreTransferState();

    protected abstract void setPreTransferState(State state);

    protected abstract void setPostTransferState(State state);

    /**
     * Initializes the state of the instruction before it is executed, by joining
     * the incoming state from the previous instruction.
     * 
     * @param incomingState
     *            The post-transfer state of the previous instruction.
     * @return The pre-transfer state.
     */
    private void initPreTransferState(CallStack callStack, State incomingState) {
	State preTransferState;
	if (getPreTransferState() == null)
	    preTransferState = incomingState;
	else
	    preTransferState = incomingState.join(getPreTransferState());
	setPreTransferState(preTransferState);
	callStack.peek().setState(preTransferState);
    }

}
