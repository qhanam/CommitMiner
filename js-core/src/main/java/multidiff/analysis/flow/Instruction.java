package multidiff.analysis.flow;

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
	transferStateOverInstruction(callStack);

	// Has a frame been added to the call stack?
	if (callStack.peek() == stackFrame) {
	    // The state has been transfered over the current instruction.
	    // Remove it from the kontinuation stack so that the next
	    // instruction can be analyzed.
	    stackFrame.popInstruction();
	} else {
	    // A new stack frame has been added to the call stack. We must
	    // analyze it before continuing with the current instruction and
	    // therefore do not update the post-transfer state.
	}
    }

    /**
     * Updates the abstract state by evaluating the instruction.
     */
    protected abstract void transferStateOverInstruction(CallStack callStack);

}
