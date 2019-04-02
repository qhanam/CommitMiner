package multidiffplus.jsanalysis.abstractdomain;

import org.apache.commons.lang3.tuple.Pair;

import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.IBuiltinState;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * A native (builtin) function. The abstract interpretation of the function is
 * specified in Java, usually as a lambda expression.
 */
public abstract class Closure {

    /**
     * Evaluates a function as follows: (1) if the closure is a function summary,
     * returns the function's exit state, or (2) if the closure is a CFG, returns
     * the function (ie. its CFG) and its initial state.
     * 
     * @param preTransferState
     *            The state before the function is interpreted.
     * @param selfAddr
     *            The value of the 'this' variable (a set of objects).
     * @param store
     *            Main memory.
     * @param scratchpad
     *            Scratchpad memory containing the argument array object.
     * @param trace
     *            The execution trace.
     * @param control
     *            Tracks control flow changes.
     */
    public abstract FunctionOrSummary initializeOrRun(State preTransferState, Address selfAddr,
	    Store store, Scratchpad scratchpad, Trace trace, Control control, CfgMap cfgs);

    /**
     * Contains (1) a function (ie. a CFG and an initial state) to evaluate, or (2)
     * the state after evaluating a function summary.
     */
    public class FunctionOrSummary {
	private Pair<CFG, IBuiltinState> initState;
	private IBuiltinState newState;

	public FunctionOrSummary(Pair<CFG, IBuiltinState> initState) {
	    this.initState = initState;
	    this.newState = null;
	}

	public FunctionOrSummary(IBuiltinState newState) {
	    this.initState = null;
	    this.newState = newState;
	}

	public boolean isFunctionSummary() {
	    return newState != null;
	}

	public Pair<CFG, IBuiltinState> getInitialStateOfFunction() {
	    return initState;
	}

	public IBuiltinState getPostCallStateOfSummary() {
	    return newState;
	}

    }

}