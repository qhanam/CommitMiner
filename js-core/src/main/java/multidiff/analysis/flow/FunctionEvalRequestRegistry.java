package multidiff.analysis.flow;

import java.util.ArrayDeque;
import java.util.Queue;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.IBuiltinState;

/**
 * A registry for interpreters to request function calls be evaluated with
 * respect to a callsite (ie. by adding a new frame to the call stack).
 */
public class FunctionEvalRequestRegistry {

    /** The CFGs of every function. */
    private CfgMap cfgMap;

    /** Requests to evaluate function calls synchronously. */
    private Queue<FunctionCall> requests;

    protected FunctionEvalRequestRegistry(CfgMap cfgMap) {
	this.cfgMap = cfgMap;
	this.requests = new ArrayDeque<FunctionCall>();
    }

    /**
     * Registers a function eval request.
     * 
     * @param state
     *            The abstract state at the time of the call.
     * @param caller
     *            The AST node that contains the call.
     * @param callee
     *            The CFG of the function being called.
     */
    public void request(IBuiltinState state, ClassifiedASTNode caller, CFG callee) {
	requests.add(new FunctionCall(state, caller, callee));
    }

    /**
     * Returns the map of function AST nodes -> CFG.
     */
    public CfgMap getCfgMap() {
	return cfgMap;
    }

    /**
     * Returns {@code true} if there is a function eval request.
     */
    public boolean hasRequest() {
	return !requests.isEmpty();
    }

    protected FunctionCall getNextRequest() {
	return requests.poll();
    }

    protected class FunctionCall {

	/** The abstract state at the time of the call. */
	private IBuiltinState state;

	/** The AST node that contains the call. */
	private ClassifiedASTNode caller;

	/** The CFG of the function being called. */
	private CFG callee;

	private FunctionCall(IBuiltinState state, ClassifiedASTNode caller, CFG callee) {
	    this.state = state;
	    this.caller = caller;
	    this.callee = callee;
	}

	public IBuiltinState state() {
	    return state;
	}

	public ClassifiedASTNode caller() {
	    return caller;
	}

	public CFG cfg() {
	    return callee;
	}
    }

}
