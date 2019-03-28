package multidiff.analysis.flow;

import multidiffplus.cfg.AnalysisState;
import multidiffplus.cfg.CallSiteNode;

/**
 * An expression source code instruction.
 * 
 * Expression instructions appear outside of branch conditions.
 */
public class CallSiteInstruction extends Instruction {

    /** The call site. */
    public CallSiteNode callSite;

    public CallSiteInstruction(CallSiteNode callSite) {
	this.callSite = callSite;
    }

    @Override
    protected void transferStateOverInstruction(CallStack callStack) {

	// Update the pre-transfer state by joining all incoming states.
	AnalysisState preTransferState = callSite.getBeforeState();
	if (preTransferState == null) {
	    preTransferState = callSite.getPredecessorAfterState();
	} else {
	    preTransferState = preTransferState.join(callSite.getPredecessorAfterState());
	}
	callSite.setBeforeState(preTransferState);

	// Update the post-transfer state by interpreting the call site.
	// TODO: Pass the call stack to interpretCallSite, so that a new frame can be
	// added when necessary.
	callSite.setAfterState(callSite.getAfterState()
		.join(callSite.getBeforeState().interpretCallSite(callSite.getCallSite())));

    }

    @Override
    public String toString() {
	return callSite.toString();
    }

}