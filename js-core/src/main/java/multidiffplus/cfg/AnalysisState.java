package multidiffplus.cfg;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiff.analysis.flow.CallStack;
import multidiff.analysis.flow.StackFrame;

public class AnalysisState {

    private IState builtinState;

    private IState[] userStates;

    private AnalysisState(IState builtinState, IState[] userStates) {
	this.builtinState = builtinState;
	this.userStates = userStates;
    }

    public AnalysisState interpretStatement(ClassifiedASTNode statement) {
	IState newBuiltinState = builtinState.interpretStatement(statement);
	IState[] newUserStates = new IState[userStates.length];
	for (int i = 0; i < userStates.length; i++) {
	    newUserStates[i] = userStates[i].interpretStatement(statement);
	}
	return new AnalysisState(newBuiltinState, newUserStates);
    }

    public AnalysisState interpretBranchCondition(CFGEdge edge) {
	IState newBuiltinState = builtinState.interpretBranchCondition(edge);
	IState[] newUserStates = new IState[userStates.length];
	for (int i = 0; i < userStates.length; i++) {
	    newUserStates[i] = userStates[i].interpretBranchCondition(edge);
	}
	return new AnalysisState(newBuiltinState, newUserStates);
    }

    public AnalysisState interpretCallSite(ClassifiedASTNode callSite, CallStack callStack) {
	// Check if there is a CFG with a new initial state. If there is, put
	// that CFG on the call stack and return (can return null in this case).
	// Otherwise, return merged states.
	FunctionEvaluator builtinEvaluator = builtinState.buildFunctionEvaluator(callSite);
	for (Entry<CFG, IState> entry : builtinEvaluator.getInitialTargetStates().entrySet()) {
	    AnalysisState newState, oldState, primeState;

	    // Check for changes to target's initial state.
	    newState = AnalysisState.initializeAnalysisState(entry.getValue(), new IState[0]);
	    oldState = entry.getKey().getEntryNode().getBeforeState();

	    if (oldState == null) {
		// We need to analyze this target for the first time.
		callStack.push(new StackFrame(entry.getKey(), newState));
		return null;
	    }

	    primeState = oldState.join(newState);

	    if (!oldState.equivalentTo(primeState)) {
		// We need to re-analyze this target.
		callStack.push(new StackFrame(entry.getKey(), primeState));
		return null;
	    }

	}

	// There are no changes; so we can safely interpret the call.
	IState newBuiltinState = builtinEvaluator.getPostCallState();
	IState[] newUserStates = new IState[userStates.length];

	// Interpret the exit state of function calls.
	List<IState> builtinExitStates = builtinEvaluator.getInitialTargetStates().keySet().stream()
		.map(cfg -> cfg.getMergedExitState().builtinState).collect(Collectors.toList());

	if (newBuiltinState == null && builtinExitStates.isEmpty())
	    throw new Error("There was no return value for " + callSite.toString());

	// Join the function and summary states.
	if (newBuiltinState == null) {
	    newBuiltinState = builtinState.interpretCallSite(callSite, builtinExitStates);
	} else if (!builtinExitStates.isEmpty()) {
	    newBuiltinState = newBuiltinState
		    .join(builtinState.interpretCallSite(callSite, builtinExitStates));
	}

	return new AnalysisState(newBuiltinState, newUserStates);
    }

    public AnalysisState join(AnalysisState that) {
	IState newBuiltinState = this.builtinState.join(that.builtinState);
	IState[] newUserStates = new IState[userStates.length];
	for (int i = 0; i < userStates.length; i++) {
	    newUserStates[i] = this.userStates[i].join(that.userStates[i]);
	}
	return new AnalysisState(newBuiltinState, newUserStates);
    }

    public IState getBuiltinState() {
	return builtinState;
    }

    public boolean equivalentTo(AnalysisState that) {
	if (!this.builtinState.equivalentTo(that.builtinState))
	    return false;
	if (this.userStates.length != that.userStates.length)
	    return false;
	for (int i = 0; i < this.userStates.length; i++) {
	    if (!this.userStates[i].equivalentTo(that.userStates[i]))
		return false;
	}
	return true;
    }

    /**
     * Returns an initialized analysis state.
     * 
     * @param builtinState
     *            the state of the built-in control flow/data flow analysis.
     * @param userState
     *            the state of the user-specified flow analysis.
     */
    public static AnalysisState initializeAnalysisState(IState builtinState, IState[] userState) {
	return new AnalysisState(builtinState, userState);
    }

}
