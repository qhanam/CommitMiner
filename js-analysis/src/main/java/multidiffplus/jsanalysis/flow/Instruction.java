package multidiffplus.jsanalysis.flow;

import multidiffplus.jsanalysis.abstractdomain.State;

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
	State postTransferState = getPreTransferState().clone();

	transferOverInstruction(callStack, postTransferState);

	// Has a frame been added to the call stack?
	if (callStack.peek() == stackFrame) {
	    // The state has been transfered over the current instruction.
	    // Remove it from the kontinuation stack so that the next
	    // instruction can be analyzed.
	    stackFrame.popInstruction();
	    setPostTransferState(postTransferState);
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
    public abstract void addInstructionsToKontinuation(CallStack callStack, State incomingState);

    /**
     * Updates the abstract state by evaluating the instruction.
     */
    protected abstract void transferOverInstruction(CallStack callStack, State postTransferState);

    /**
     * Returns the state of the program before the instruction executes.
     */
    protected abstract State getPreTransferState();

    /**
     * Assigns the state of the program before the instruction executes.
     */
    protected abstract void setPreTransferState(State state);

    /**
     * Assigns the state of the program after the instruction executes.
     */
    protected abstract void setPostTransferState(State state);

    /**
     * Initializes the state of the instruction before it is executed, by joining
     * the incoming state from the previous instruction.
     * 
     * @param incomingState
     *            The post-transfer state of the previous instruction.
     * @return The pre-transfer state.
     */
    protected void initPreTransferState(State incomingState) {
	State preTransferState;
	if (getPreTransferState() == null)
	    preTransferState = incomingState;
	else
	    preTransferState = incomingState.join(getPreTransferState());
	setPreTransferState(preTransferState);
    }

}
