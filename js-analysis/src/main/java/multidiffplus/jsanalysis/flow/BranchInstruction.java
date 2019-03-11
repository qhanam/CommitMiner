package multidiffplus.jsanalysis.flow;

import multidiffplus.cfg.CFGEdge;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.interpreter.ExpEval;

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
    public void addInstructionsToKontinuation(CallStack callStack, State incomingState) {
	StackFrame stackFrame = callStack.peek();
	Instruction instruction = stackFrame.getInstructionForNode(edge.getTo());
	instruction.initPreTransferState(incomingState);
	stackFrame.pushInstruction(instruction);
    }

    @Override
    protected void transferOverInstruction(CallStack callStack, State postTransferState) {
	TransferEdge transferFunction = new TransferEdge(postTransferState, edge,
		new ExpEval(callStack, postTransferState));
	transferFunction.transfer();
    }

    @Override
    protected void setPostTransferState(State state) {
	edge.setAfterState(state);
    }

    @Override
    protected State getPreTransferState() {
	return (State) edge.getBeforeState();
    }

    @Override
    protected void setPreTransferState(State state) {
	edge.setBeforeState(state);
    }

    @Override
    public String toString() {
	return edge.getCondition() == null ? "null" : edge.getCondition().toString();
    }

}