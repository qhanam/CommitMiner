package multidiffplus.cfg;

import multidiff.analysis.flow.CallStack;

/**
 * An abstract state and abstract interpreter for built-in abstract domains.
 * 
 * This interpreter performs the underlying control and data flow analysis
 * needed by checkers. The results of this analysis (ie. control flow, variable
 * dependencies, data dependencies and type state) are both made available to
 * injected checkers.
 */
public interface IState {

    /**
     * Updates a copy of the abstract state and abstract call stack in-place by
     * interpreting the given JavaScript statement. By interpreting the instruction
     * on a copy of the abstract state, the original state is left intact.
     * 
     * @param statement
     *            The statement to interpret.
     * @param callstack
     *            The current abstract call stack. The interpreter pushes new stack
     *            frames onto the call stack when needed.
     * @return A new analysis state state, which is the state of the analysis after
     *         the statement is interpreted.
     */
    IState interpretStatement(CFGNode node, CallStack callStack);

    /**
     * Updates a copy of the abstract state and abstract call stack in-place by
     * interpreting the given JavaScript statement. By interpreting the instruction
     * on a copy of the abstract state, the original state is left intact.
     * 
     * @param statement
     *            The statement to interpret.
     * @param callstack
     *            The current abstract call stack. The interpreter pushes new stack
     *            frames onto the call stack when needed.
     * @return A new analysis state, which is the state of the analysis after the
     *         statement is interpreted.
     */
    IState interpretBranchCondition(CFGEdge edge, CallStack callStack);

    /**
     * Return a new analysis state, which is the join of {@code this} state and
     * {@code that} state.
     * 
     * @param that
     *            The AnalysisState to join with this AnalysisState.
     */
    IState join(IState that);

}
