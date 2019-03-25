package multidiffplus.jsanalysis.flow;

import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.ScriptNode;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.FunctionEvaluator;
import multidiffplus.cfg.IState;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.initstate.StateFactory;
import multidiffplus.jsanalysis.interpreter.BranchConditionInterpreter;
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

    /**
     * The built-in state (control flow, points-to, type-state and change-impact).
     */
    private State state;

    /**
     * The set of states for user-specified analyses.
     */
    private IState[] userStates;

    private JavaScriptAnalysisState(State state, IState[] userStates) {
	this.state = state;
	this.userStates = userStates;
    }

    @Override
    public IState interpretStatement(CFGNode node) {
	State newState = this.state.clone();
	state.trace = state.trace.update(node.getStatement().getID());
	StatementInterpreter.interpret(node, newState);
	IState[] newUserStates = new IState[userStates.length];
	for (int i = 0; i < userStates.length; i++) {
	    newUserStates[i] = userStates[i].interpretStatement(node);
	}
	return new JavaScriptAnalysisState(newState, newUserStates);
    }

    @Override
    public IState interpretBranchCondition(CFGEdge edge) {
	State newState = this.state.clone();
	if (edge.getCondition() != null) {
	    state.trace = state.trace.update(edge.getCondition().getID());
	}
	BranchConditionInterpreter.interpret(edge, newState);
	IState[] newUserStates = new IState[userStates.length];
	for (int i = 0; i < userStates.length; i++) {
	    newUserStates[i] = userStates[i].interpretBranchCondition(edge);
	}
	return new JavaScriptAnalysisState(newState, newUserStates);
    }

    @Override
    public IState join(IState s) {
	JavaScriptAnalysisState that = (JavaScriptAnalysisState) s;
	IState[] newUserStates = new IState[userStates.length];
	for (int i = 0; i < userStates.length; i++) {
	    newUserStates[i] = this.userStates[i].join(that.userStates[i]);
	}
	return new JavaScriptAnalysisState(this.state.join(that.state), newUserStates);
    }

    @Override
    public boolean equivalentTo(IState state) {
	JavaScriptAnalysisState that = (JavaScriptAnalysisState) state;
	StateComparator comparator = new StateComparator(this.state, that.state);
	return comparator.isEqual();
    }

    @Override
    public Integer getAnalysisId() {
	return 0;
    }

    @Override
    public FunctionEvaluator initializeFunctionState(ClassifiedASTNode callSite) {
	FunctionCall fc = (FunctionCall) callSite;
	State newState = this.state.clone();
	state.trace = state.trace.update(fc.getID());
	return null;
    }

    public State getUnderlyingState() {
	return state;
    }

    /**
     * Creates an initial AnalysisState for a function.
     */
    public static IState initializeFunctionState(State state, IState[] userStates) {
	return new JavaScriptAnalysisState(state, userStates);
    }

    /**
     * Creates an initial AnalysisState for a script.
     * 
     * This should only be called once per analysis. All other states should be
     * created by either interpreting statements or joining two states.
     */
    public static IState initializeScriptState(ClassifiedASTNode root, CfgMap cfgMap,
	    IState[] userStates) {
	State state = StateFactory.createInitialState((ScriptNode) root, cfgMap);
	return new JavaScriptAnalysisState(state, userStates);
    }

}
