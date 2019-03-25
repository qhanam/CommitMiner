package multidiffplus.cfg;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A container for storing the state before and after a function call. Used to
 * support inter-procedural analysis across user-specified domains.
 */
public class FunctionEvaluator {

    /**
     * The state of the current stack frame immediately before the call is executed.
     */
    private IState preCallState;

    /**
     * The state of the current stack frame immediately after the call is executed.
     * 
     * A postCallState is returned when (1) the target points to one or more
     * function summaries (ie. rather than a CFG), or (2) the target does not
     * resolve to a function. If postCallState is {@code null}, then neither of
     * these conditions hold, and any side effects to the state are ignored.
     */
    private IState postCallState;

    /**
     * The initial state of the callee's stack frames, with respect to the current
     * call site.
     * 
     * The functions that the call site target points to are stored as their CFGs,
     * and the initial state is stored as the IState.
     */
    private List<Pair<CFG, IState>> initialTargetState;

    public FunctionEvaluator() {
	this.preCallState = null;
	this.postCallState = null;
	this.initialTargetState = new ArrayList<Pair<CFG, IState>>();
    }

    /**
     * Set the initial state of a target's stack frame, with respect to the current
     * call site.
     */
    public void addInitialTargetState(Pair<CFG, IState> initialTargetState) {
	this.initialTargetState.add(initialTargetState);
    }

    /**
     * Merge the interpretation of a function summary with the interpretations of
     * other function summaries pointed to by the target.
     */
    public void joinPostCallState(IState stateToJoin) {
	if (postCallState == null)
	    postCallState = stateToJoin;
	else
	    postCallState = postCallState.join(stateToJoin);
    }

    /**
     * Returns {@code true} if the target was resolved to at least one function or
     * function summary.
     */
    public boolean resolved() {
	return initialTargetState.isEmpty() && postCallState == null;
    }

}
