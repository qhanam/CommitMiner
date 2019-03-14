package multidiffplus.jsanalysis.flow;

import org.mozilla.javascript.ast.ScriptNode;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiff.analysis.flow.CallStack;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.cfg.CfgMap;
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

    private State state;

    private JavaScriptAnalysisState(State state) {
	this.state = state;
    }

    @Override
    public IState interpretStatement(CFGNode node, CallStack callStack) {
	State newState = this.state.clone();
	state.trace = state.trace.update(node.getStatement().getID());
	StatementInterpreter.interpret(node, newState, callStack);
	return new JavaScriptAnalysisState(newState);
    }

    @Override
    public IState interpretBranchCondition(CFGEdge edge, CallStack callStack) {
	State newState = this.state.clone();
	if (edge.getCondition() != null) {
	    state.trace = state.trace.update(edge.getCondition().getID());
	}
	BranchConditionInterpreter.interpret(edge, newState, callStack);
	return new JavaScriptAnalysisState(newState);
    }

    @Override
    public IState join(IState s) {
	JavaScriptAnalysisState that = (JavaScriptAnalysisState) s;
	return new JavaScriptAnalysisState(this.state.join(that.state));
    }

    @Override
    public IState clone() {
	return new JavaScriptAnalysisState(state.clone());
    }

    @Override
    public boolean equivalentTo(IState state) {
	JavaScriptAnalysisState that = (JavaScriptAnalysisState) state;
	StateComparator comparator = new StateComparator(this.state, that.state);
	return comparator.isEqual();
    }

    public State getUnderlyingState() {
	return state;
    }

    /**
     * Creates an initial AnalysisState for a function.
     * 
     * This should be called whenever a new frame is being added to a call stack.
     */
    public static IState initializeFunctionState(State state) {
	return new JavaScriptAnalysisState(state);
    }

    /**
     * Creates an initial AnalysisState for a script.
     * 
     * This should only be called once per analysis. All other states should be
     * created by either interpreting statements or joining two states.
     */
    public static IState initializeScriptState(ClassifiedASTNode root, CfgMap cfgMap) {
	State state = StateFactory.createInitialState((ScriptNode) root, cfgMap);
	return new JavaScriptAnalysisState(state);
    }

}
