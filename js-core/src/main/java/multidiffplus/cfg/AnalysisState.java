package multidiffplus.cfg;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
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

    public AnalysisState interpretCallSite(ClassifiedASTNode callSite) {
	// TODO: How should AnalysisState interact with the control flow
	// analysis? We need to merge return values somehow. IDEA: All
	// function summaries should automatically update the BValue of
	// the call site. All functions should return an exit state, which
	// should be interpreted by the analysis.
	//
	// Either (1) an initial function state was returned; compare the
	// initial state, or (2) a function summary was returned; move to the
	// next call site.

	// Either (1) There is a function that needs to be evaluated with the new
	// initial state, or (2) all functions have been evaluated.

	// TODO: Check if there is a CFG with a new initial state. If there is,
	// put that CFG on the call stack and return (can return null in this case).
	// Otherwise, return merged states.
	StackFrame stackFrame = builtinState.interpretCallSite(callSite);
	return null;
    }

    public AnalysisState join(AnalysisState that) {
	IState newBuiltinState = this.builtinState.join(that.builtinState);
	IState[] newUserStates = new IState[userStates.length];
	for (int i = 0; i < userStates.length; i++) {
	    newUserStates[i] = this.userStates[i].join(that.userStates[i]);
	}
	return new AnalysisState(newBuiltinState, newUserStates);
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
