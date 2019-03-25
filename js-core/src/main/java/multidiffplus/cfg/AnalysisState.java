package multidiffplus.cfg;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;

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

    public AnalysisState interpretBranchCondition(ClassifiedASTNode condition) {
	IState newBuiltinState = builtinState.interpretBranchCondition(condition);
	IState[] newUserStates = new IState[userStates.length];
	for (int i = 0; i < userStates.length; i++) {
	    newUserStates[i] = userStates[i].interpretBranchCondition(condition);
	}
	return new AnalysisState(newBuiltinState, newUserStates);
    }

    public AnalysisState initializeFunctionState(ClassifiedASTNode callSite) {
	// TODO
	// FunctionEvaluator evaluator = builtinState.initializeFunctionState(callSite);
	// TOOD: Either (1) an initial function state was returned; compare the
	// initial state, or (2) a function summary was returned; move to the
	// next call site.
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
