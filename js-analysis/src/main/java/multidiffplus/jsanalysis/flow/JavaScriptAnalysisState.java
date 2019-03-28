package multidiffplus.jsanalysis.flow;

import java.util.List;

import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.ScriptNode;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.AnalysisState;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.FunctionEvaluator;
import multidiffplus.cfg.IState;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.initstate.StateFactory;
import multidiffplus.jsanalysis.interpreter.BranchConditionInterpreter;
import multidiffplus.jsanalysis.interpreter.CallSiteInterpreter;
import multidiffplus.jsanalysis.interpreter.StateComparator;
import multidiffplus.jsanalysis.interpreter.StatementInterpreter;

/**
 * An abstract state and interpreter for JavaScript.
 * 
 * This interpreter performs the underlying control and data flow analysis
 * needed by checkers. The results of this analysis (ie. control flow, variable
 * dependencies, data dependencies and type state) are both made available to
 * injected checkers.
 */
public class JavaScriptAnalysisState implements IState {

    private State state;
    private CfgMap cfgs;

    private JavaScriptAnalysisState(State state, CfgMap cfgs) {
	this.state = state;
	this.cfgs = cfgs;
    }

    @Override
    public IState interpretStatement(ClassifiedASTNode statement) {
	State newState = this.state.clone();
	state.trace = state.trace.update(statement.getID());
	StatementInterpreter.interpret(statement, newState, cfgs);
	return new JavaScriptAnalysisState(newState, cfgs);
    }

    @Override
    public IState interpretBranchCondition(CFGEdge edge) {
	State newState = this.state.clone();
	if (edge.getCondition() != null) {
	    state.trace = state.trace.update(edge.getCondition().getID());
	}
	BranchConditionInterpreter.interpret(edge, newState, cfgs);
	return new JavaScriptAnalysisState(newState, cfgs);
    }

    @Override
    public IState join(IState s) {
	JavaScriptAnalysisState that = (JavaScriptAnalysisState) s;
	return new JavaScriptAnalysisState(this.state.join(that.state), cfgs);
    }

    @Override
    public boolean equivalentTo(IState state) {
	JavaScriptAnalysisState that = (JavaScriptAnalysisState) state;
	StateComparator comparator = new StateComparator(this.state, that.state);
	return comparator.isEqual();
    }

    @Override
    public FunctionEvaluator buildFunctionEvaluator(ClassifiedASTNode callSite) {
	FunctionCall fc = (FunctionCall) callSite;
	State newState = this.state.clone();
	state.trace = state.trace.update(fc.getID());
	return CallSiteInterpreter.initialize(callSite, newState, cfgs);
    }

    @Override
    public IState interpretCallSite(ClassifiedASTNode callSite, List<IState> exitStates) {
	State newState = this.state.clone();
	if (exitStates.size() == 0)
	    return null;

	// Merge the exit states (ie. return values) of the function targets.
	IState mergedExitState = null;
	for (IState exitState : exitStates) {
	    if (mergedExitState == null) {
		mergedExitState = exitState;
	    } else {
		mergedExitState = mergedExitState.join(exitState);
	    }
	}

	// Update the call site (return value) and the abstract store (side
	// effects).
	CallSiteInterpreter.interpret((FunctionCall) callSite, newState,
		(JavaScriptAnalysisState) mergedExitState, cfgs);

	return new JavaScriptAnalysisState(newState, cfgs);
    }

    public State getUnderlyingState() {
	return state;
    }

    /**
     * Creates an initial AnalysisState for a function.
     */
    public static IState initializeFunctionState(State state, CfgMap cfgs) {
	return new JavaScriptAnalysisState(state, cfgs);
    }

    /**
     * Creates an initial AnalysisState for a script.
     * 
     * This should only be called once per analysis. All other states should be
     * created by either interpreting statements or joining two states.
     */
    public static AnalysisState initializeScriptState(ClassifiedASTNode root, CfgMap cfgs,
	    IState[] userStates) {
	State state = StateFactory.createInitialState((ScriptNode) root, cfgs);
	return AnalysisState.initializeAnalysisState(new JavaScriptAnalysisState(state, cfgs),
		userStates);
    }

}
