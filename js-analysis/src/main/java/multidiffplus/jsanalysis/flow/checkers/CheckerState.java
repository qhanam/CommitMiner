package multidiffplus.jsanalysis.flow.checkers;

import org.mozilla.javascript.ast.AstNode;

import multidiffplus.jsanalysis.abstractdomain.State;

/**
 * An abstract interpreter for user-specified checkers.
 * 
 * Implement this interface to check some property of the program with flow
 * analysis. A checker has two main components which must be implemented: (1)
 * the checker's abstract domain and its join operation, and (2) interpreters
 * which update the checker's abstract state by interpreting either a statement
 * or a branch condition.
 */
public interface CheckerState {

    /**
     * Updates the abstract state and abstract call stack in-place by interpreting
     * the given JavaScript statement.
     * 
     * @param statement
     *            The statement to interpret.
     * @param state
     *            A copy of the built-in abstract state (after the statement is
     *            interpreted). Because it is a copy, changes to the state made by
     *            the checker will not affect the analysis.
     * @return A new checker state, which is the state of the checker after the
     *         statement is interpreted.
     */
    public CheckerState interpretStatement(AstNode statement, State state);

    /**
     * Updates the abstract state and abstract call stack in-place by interpreting
     * the given JavaScript branch condition.
     * 
     * @param condition
     *            The branch condition to interpret.
     * @param state
     *            A copy of the built-in abstract state (after the statement is
     *            interpreted). Because it is a copy, changes to the state made by
     *            the checker will not affect the analysis.
     * @return A new checker state, which is the state of the checker after the
     *         branch condition is interpreted.
     */
    public CheckerState interpretBranchCondition(AstNode condition, State state);

    /**
     * Return a new checker state, which is the join of {@code this} state and
     * {@code that} state.
     * 
     * @param that
     *            The CheckerState to join with this CheckerState.
     */
    public CheckerState join(CheckerState that);

}
