package multidiff.analysis.flow;

import multidiffplus.cfg.AnalysisState;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.cfg.CallSiteNode;

/**
 * An expression source code instruction.
 * 
 * Expression instructions appear outside of branch conditions.
 */
public class ExpressionInstruction extends Instruction {

    /** The node containing the instruction. */
    private CFGNode node;

    public ExpressionInstruction(CFGNode node) {
	this.node = node;
    }

    @Override
    protected void transferStateOverInstruction(CallStack callStack) {

	// Update the pre-transfer state by joining all incoming states.
	AnalysisState preTransferState = node.getBeforeState();
	if (node.getCallSiteNodes().length > 0) {
	    // The predecessor is a call site.
	    CallSiteNode[] callSiteNodes = node.getCallSiteNodes();
	    AnalysisState afterState = callSiteNodes[callSiteNodes.length - 1].getAfterState();
	    if (preTransferState == null) {
		preTransferState = afterState;
	    } else {
		preTransferState = preTransferState.join(afterState);
	    }
	} else {
	    // The predecessor is a set of edges.
	    for (CFGEdge edge : node.getIncommingEdges()) {
		if (edge.getAfterState() == null) {
		    continue;
		}
		if (preTransferState == null) {
		    preTransferState = edge.getAfterState();
		} else {
		    preTransferState = preTransferState.join(edge.getAfterState());
		}
	    }
	}
	node.setBeforeState(preTransferState);

	// Update the post-transfer state by interpreting the statement.
	if (node.getAfterState() == null)
	    node.setAfterState(node.getBeforeState().interpretStatement(node.getStatement()));
	else
	    node.setAfterState(node.getAfterState()
		    .join(node.getBeforeState().interpretStatement(node.getStatement())));

    }

    @Override
    public String toString() {
	return node.getStatement().toString();
    }

}