package multidiffplus.cfg;

import java.util.List;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;

/**
 * An abstract state and abstract interpreter for built-in abstract domains.
 * 
 * This interpreter performs the underlying control and data flow analysis
 * needed by checkers. The results of this analysis (ie. control flow, variable
 * dependencies, data dependencies and type state) are both made available to
 * injected checkers.
 */
public interface IBuiltinState {

    /**
     * Updates a copy of the abstract state in-place by interpreting the given
     * JavaScript statement. By interpreting the instruction on a copy of the
     * abstract state, the original state is left intact.
     * 
     * @param statement
     *            The statement to interpret.
     * @param functReg
     *            The registry for requesting function calls be evaluated (added to
     *            the abstract call stack) by the analysis.
     * @return A new analysis state state, which is the state of the analysis after
     *         the statement is interpreted.
     */
    IBuiltinState interpretStatement(ClassifiedASTNode statement);

    /**
     * Updates a copy of the abstract state in-place by interpreting the given
     * JavaScript statement. By interpreting the instruction on a copy of the
     * abstract state, the original state is left intact.
     * 
     * @param statement
     *            The statement to interpret.
     * @param functReg
     *            The registry for requesting function calls be evaluated (added to
     *            the abstract call stack) by the analysis.
     * @return A new analysis state, which is the state of the analysis after the
     *         statement is interpreted.
     */
    IBuiltinState interpretBranchCondition(CFGEdge edge);

    /**
     * Evaluates the state of the current stack frame up to the function call,
     * resolves the call site targets and prepares the initial state of the stack
     * frame for the callee.
     * 
     * @return The pre/post execution state of the call site, the initial state of
     *         the callee and the list of resolved targets.
     */
    FunctionEvaluator initializeCallsite(ClassifiedASTNode callSite);

    /**
     * Updates a copy of the abstract state in-place by interpreting the given
     * JavaScript call site.
     * 
     * @param callSite
     *            The AST node containing the call site.
     * @param functionExitStates
     *            The exit states of the call site's target functions.
     * @return A new analysis state, which is the state of the analysis after the
     *         call site's target functions are interpreted.
     */
    IBuiltinState interpretCallSite(ClassifiedASTNode callSite,
	    List<IBuiltinState> functionExitStates);

    /**
     * Return a new analysis state, which is the join of {@code this} state and
     * {@code that} state.
     * 
     * @param that
     *            The AnalysisState to join with this AnalysisState.
     */
    IBuiltinState join(IBuiltinState that);

    /**
     * Returns true if this state is equivalent to that state.
     */
    boolean equivalentTo(IBuiltinState that);

}
