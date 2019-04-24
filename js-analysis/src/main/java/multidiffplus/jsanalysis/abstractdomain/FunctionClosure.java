package multidiffplus.jsanalysis.abstractdomain;

import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.ScriptNode;

import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CfgMap;
import multidiffplus.jsanalysis.flow.JavaScriptAnalysisState;
import multidiffplus.jsanalysis.interpreter.Helpers;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * The abstract domain for function closures.
 */
public class FunctionClosure extends Closure {

    /** The function. **/
    public CFG cfg;

    /** The closure environment? **/
    public Environment environment;

    /** A map of AstNode -> CFG. */
    public CfgMap cfgs;

    /**
     * @param cfg
     *            The control flow graph for the function.
     * @param environment
     *            The environment of the parent closure. Does not yet contain local
     *            variables of this function.
     */
    public FunctionClosure(CFG cfg, Environment environment, CfgMap cfgs) {
	this.cfg = cfg;
	this.environment = environment;
	this.cfgs = cfgs;
    }

    @Override
    public FunctionOrSummary initializeOrRun(State preTransferState, Address selfAddr, Store store,
	    Scratchpad scratchpad, Trace trace, Control control, CfgMap cfgs) {

	// Store any callsite dependencies.
	control.getCall().change.getDependencies().getDependencies()
		.forEach(criterion -> ((AstNode) cfg.getEntryNode().getStatement())
			.addDependency(criterion.getType().toString(), criterion.getId()));

	// Advance the trace.
	trace = trace.update(environment, store, selfAddr,
		(ScriptNode) cfg.getEntryNode().getStatement());

	// Return initial state.
	return new FunctionOrSummary(Pair.of(cfg, JavaScriptAnalysisState.initializeFunctionState(
		initState(selfAddr, store, scratchpad, trace, control), cfgs)));

    }

    /**
     * Lift local variables and function declarations into the environment and
     * create the initial state for the function call.
     * 
     * @return The environment for the closure, including parameters and
     *         {@code this}.
     */
    private State initState(Address selfAddr, Store store, Scratchpad scratchpad, Trace trace,
	    Control control) {

	Environment env = this.environment.clone();

	/* Match parameters with arguments. */
	if (this.cfg.getEntryNode().getStatement() instanceof FunctionNode) {
	    FunctionNode function = (FunctionNode) this.cfg.getEntryNode().getStatement();
	    store = Helpers.initParams(env, store, scratchpad, trace, function);
	}

	/*
	 * Lift local variables and function declarations into the environment. This has
	 * to happen after the parameters are added to the environment so that the
	 * parameters are available in the closure of functions declared within this
	 * function.
	 */
	store = Helpers.lift(env, store, (ScriptNode) cfg.getEntryNode().getStatement(), cfgs,
		trace);

	/* Add 'this' to environment (points to caller's object or new object). */
	env = env.strongUpdate("this", Variable.inject("this", selfAddr, Change.u(),
		Dependencies.injectVariable((AstNode) cfg.getEntryNode().getStatement())));

	/* Create the initial state for the function call. */
	return new State(store, env, scratchpad, trace, control, selfAddr);

    }

    @Override
    public String toString() {
	return this.cfg.getEntryNode().getStatement().toString();
    }

}
