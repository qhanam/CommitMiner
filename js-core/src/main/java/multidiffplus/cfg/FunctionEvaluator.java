package multidiffplus.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A container for storing the state before and after a function call. Used to
 * support inter-procedural analysis across user-specified domains.
 */
public class FunctionEvaluator {

    /**
     * The state of the current stack frame immediately after the call is executed.
     * 
     * A postCallState is returned when (1) the target points to one or more
     * function summaries (ie. rather than a CFG), or (2) the target does not
     * resolve to a function. If postCallState is {@code null}, then neither of
     * these conditions hold, and any side effects to the state are ignored.
     */
    private IBuiltinState postCallState;

    /**
     * The initial state of the callee's stack frames, with respect to the current
     * call site.
     * 
     * The functions that the call site target points to are stored as their CFGs,
     * and the initial state is stored as the IState.
     */
    private Map<CFG, IBuiltinState> initialTargetState;

    /**
     * Callback functions passed as arguments to unresolved call site targets. These
     * will have their states initialized by built-in and user interpreters, and
     * added to the event loop to be analyzed later.
     */
    private List<Pair<CFG, IBuiltinState>> callbacks;

    public FunctionEvaluator() {
	this.postCallState = null;
	this.initialTargetState = new HashMap<CFG, IBuiltinState>();
	this.callbacks = new ArrayList<Pair<CFG, IBuiltinState>>();
    }

    /**
     * Set the initial state of a target's stack frame, with respect to the current
     * call site.
     */
    public void addInitialTargetState(Pair<CFG, IBuiltinState> initialTargetState) {
	this.initialTargetState.put(initialTargetState.getKey(), initialTargetState.getValue());
    }

    /**
     * Returns the initial state for each target.
     */
    public Map<CFG, IBuiltinState> getInitialTargetStates() {
	return initialTargetState;
    }

    /**
     * Merge the interpretation of a function summary with the interpretations of
     * other function summaries pointed to by the target.
     */
    public void joinPostCallState(IBuiltinState stateToJoin) {
	if (postCallState == null)
	    postCallState = stateToJoin;
	else
	    postCallState = postCallState.join(stateToJoin);
    }

    /**
     * Returns the merged function summaries.
     */
    public IBuiltinState getPostCallState() {
	return postCallState;
    }

    /**
     * Register a callback function to be added to the event loop.
     * 
     * Callback functions should only be added for call sites with targets that do
     * not resolve to actual functions or summaries. For cases where targets do
     * resolve, the fate of the callback will be evaluated within the analysis of
     * the target.
     */
    public void addCallback(Pair<CFG, IBuiltinState> functionState) {
	callbacks.add(functionState);
    }

    /**
     * Returns the callback functions in the call site.
     */
    public List<Pair<CFG, IBuiltinState>> getCallbacks() {
	return callbacks;
    }

    /**
     * Returns {@code true} if the target was resolved to at least one function or
     * function summary.
     */
    public boolean resolved() {
	return !initialTargetState.isEmpty() || postCallState != null;
    }

}
