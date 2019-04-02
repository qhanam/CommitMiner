package multidiffplus.cfg;

import java.util.List;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;

/**
 * An abstract state and abstract interpreter for user-specified abstract
 * domains.
 */
public interface IUserState {

    /**
     * Updates a copy of the abstract state in-place by interpreting the given
     * JavaScript statement. By interpreting the instruction on a copy of the
     * abstract state, the original state is left intact.
     * 
     * @param builtin
     *            The state of the builtin analysis (ie. control flow, data flow and
     *            types).
     * @param statement
     *            The statement to interpret.
     * @return A new analysis state state, which is the state of the analysis after
     *         the statement is interpreted.
     */
    IUserState interpretStatement(IBuiltinState builtin, ClassifiedASTNode statement);

    /**
     * Updates a copy of the abstract state in-place by interpreting the given
     * JavaScript statement. By interpreting the instruction on a copy of the
     * abstract state, the original state is left intact.
     * 
     * @param builtin
     *            The state of the builtin analysis (ie. control flow, data flow and
     *            types).
     * @param statement
     *            The statement to interpret.
     * @return A new analysis state, which is the state of the analysis after the
     *         statement is interpreted.
     */
    IUserState interpretBranchCondition(IBuiltinState builtin, CFGEdge edge);

    /**
     * Updates a copy of the abstract state in-place by interpreting the given
     * JavaScript call site.
     * 
     * @param builtin
     *            The state of the builtin analysis (ie. control flow, data flow and
     *            types).
     * @param callSite
     *            The AST node containing the call site.
     * @return A new analysis state, which is the state of the analysis after the
     *         call site's target functions are interpreted.
     */
    IUserState interpretCallSite(IBuiltinState builtin, ClassifiedASTNode callSite,
	    List<IUserState> functionExitStates);

    /**
     * Initializes the state of the callback function passed to the given call site.
     * 
     * @param callSite
     *            The call site containing the callback argument.
     * @param function
     *            The callback function given as an argument to the call site
     *            target.
     * @return The initial state of the call back function.
     */
    IUserState initializeCallback(IBuiltinState builtin, ClassifiedASTNode callSite, CFG function);

    /**
     * Return a new analysis state, which is the join of {@code this} state and
     * {@code that} state.
     * 
     * @param that
     *            The AnalysisState to join with this AnalysisState.
     */
    IUserState join(IUserState that);

    /**
     * Returns true if this state is equivalent to that state.
     */
    boolean equivalentTo(IUserState that);

}
