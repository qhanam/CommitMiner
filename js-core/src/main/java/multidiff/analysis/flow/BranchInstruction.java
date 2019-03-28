package multidiff.analysis.flow;

import multidiffplus.cfg.AnalysisState;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;

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
    protected void transferStateOverInstruction(CallStack callStack) {

	// Update the pre-transfer state by joining all incoming states.
	AnalysisState preTransferState = edge.getBeforeState();
	CFGNode node = edge.getFrom();
	if (preTransferState == null) {
	    preTransferState = node.getAfterState();
	} else {
	    preTransferState = preTransferState.join(node.getAfterState());
	}
	edge.setBeforeState(preTransferState);

	// Update the post-transfer state by interpreting the statement.
	if (edge.getAfterState() == null)
	    edge.setAfterState(edge.getBeforeState().interpretBranchCondition(edge));
	else
	    edge.setAfterState(edge.getAfterState()
		    .join(edge.getBeforeState().interpretBranchCondition(edge)));

    }

    @Override
    public String toString() {
	return edge.getCondition() == null ? "null" : edge.getCondition().toString();
    }

}