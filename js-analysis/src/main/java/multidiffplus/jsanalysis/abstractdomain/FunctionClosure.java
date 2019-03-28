package multidiffplus.jsanalysis.abstractdomain;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ScriptNode;

import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CfgMap;
import multidiffplus.jsanalysis.flow.JavaScriptAnalysisState;
import multidiffplus.jsanalysis.initstate.StoreFactory;
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
    public FunctionOrSummary initializeOrRun(Address selfAddr, Store store, Scratchpad scratchpad,
	    Trace trace, Control control) {

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

    // @Override
    // public JavaScriptAnalysisState run(Address selfAddr, Store store, Scratchpad
    // scratchpad,
    // Trace trace, Control control) {
    // // return Helpers.getMergedExitState(cfg);
    // return (JavaScriptAnalysisState) cfg.getMergedExitState();
    // }
    //
    // public IState createInitialFunctionState(Address selfAddr, Store store,
    // Scratchpad scratchpad,
    // Trace trace, Control control, FunctionEvalRequestRegistry callRegistry) {
    //
    // // Store any callsite dependencies.
    // control.getCall().change.getDependencies().getDependencies()
    // .forEach(criterion -> ((AstNode) cfg.getEntryNode().getStatement())
    // .addDependency(criterion.getType().toString(), criterion.getId()));
    //
    // // Advance the trace.
    // trace = trace.update(environment, store, selfAddr,
    // (ScriptNode) cfg.getEntryNode().getStatement());
    //
    // // Create the initial state.
    // return JavaScriptAnalysisState.initializeFunctionState(
    // initState(selfAddr, store, scratchpad, trace, control, callRegistry));
    //
    // }

    // /**
    // * Computes the initial state of the analysis.
    // *
    // * @param oldState
    // * The initial state of the stack frame before reaching this call
    // * site.
    // * @param primeState
    // * The initial state of the stack frame at this call site.
    // * @return the initial state of the stack frame, or {@code null} if the states
    // * are the same and no analysis is required.
    // */
    // private IState getInitialState(IState oldState, IState primeState) {
    //
    // if (oldState == null)
    // return primeState;
    //
    // IState newState = oldState.join(primeState);
    //
    // if (!oldState.equivalentTo(newState))
    // return newState;
    //
    // return null;
    //
    // }

    // @Override
    // public boolean requiresEvaluation(Address selfAddr, Store store, Scratchpad
    // scratchpad,
    // Trace trace, Control control, FunctionEvalRequestRegistry callRegistry) {
    // IState oldState = cfg.getEntryNode().getBeforeState();
    // IState primeState = createInitialFunctionState(selfAddr, store, scratchpad,
    // trace, control,
    // callRegistry);
    // IState initialState = getInitialState(oldState, primeState);
    // return initialState != null;
    // }

    // @Override
    // public JavaScriptAnalysisState run(Address selfAddr, Store store, Scratchpad
    // scratchpad,
    // Trace trace, Control control, FunctionEvalRequestRegistry callRegistry) {
    //
    // // Create the initial state if needed.
    // IState newState = null;
    // IState oldState = cfg.getEntryNode().getBeforeState();
    // IState primeState = createInitialFunctionState(selfAddr, store, scratchpad,
    // trace, control,
    // callRegistry);
    // IState exitState = null;
    //
    // if (oldState == null) {
    // // Create the initial state for the function call by lifting local vars and
    // // functions into the environment.
    // newState = primeState;
    // // Add a new frame to the call stack so that the callee is executed next.
    // callRegistry.request(newState, caller, callee);
    // callStack.push(new StackFrame(cfg, newState));
    // // We do not have an exit state, because we must first evaluate the new call.
    // return null;
    // }
    //
    // newState = oldState.join(primeState);
    //
    // if (!oldState.equivalentTo(newState)) {
    // // We have a new initial state for the function. Add a new frame to the call
    // // stack so that
    // // the callee is analyzed next.
    // callStack.push(new StackFrame(cfg, newState));
    // // We do not have an exit state, because we must first evaluate the new call.
    // return null;
    // }
    //
    // // The initial state of the function has not changed, do we don't
    // // need to re-analyze the function.
    // exitState = cfg.getMergedExitState();
    //
    // if (exitState == null) {
    // // We are in a recursive loop. Don't update the state.
    // return (JavaScriptAnalysisState) newState;
    // }
    //
    // return (JavaScriptAnalysisState) exitState;
    //
    // }

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

	    /* Create the arguments object. */
	    Map<String, Property> ext = new HashMap<String, Property>();
	    int i = 0;
	    for (BValue argVal : scratchpad.applyArgs()) {
		AstNode dependentToken = i < function.getParams().size()
			? function.getParams().get(i)
			: new Name();
		store = Helpers.addProp(function.getID(), String.valueOf(i), argVal, ext, store,
			trace, dependentToken);
		i++;
	    }

	    InternalObjectProperties internal = new InternalObjectProperties(
		    Address.inject(StoreFactory.Arguments_Addr, Change.u(), Dependencies.bot()),
		    JSClass.CObject);
	    Obj argObj = new Obj(ext, internal);

	    /* Put the argument object on the store. */
	    Address argAddr = trace.makeAddr(function.getID(), "");
	    store = store.alloc(argAddr, argObj);

	    i = 0;
	    for (AstNode param : function.getParams()) {
		if (param instanceof Name) {

		    Name paramName = (Name) param;
		    Property prop = argObj.externalProperties.get(String.valueOf(i));

		    if (prop == null) {

			/*
			 * No argument was given for this parameter. Create a dummy value.
			 */

			/* Add the argument address to the argument object. */
			BValue argVal = BValue.top(
				Change.convU(param, (n) -> Dependencies.injectValueChange(n)),
				Dependencies.injectValue(param));
			store = Helpers.addProp(param.getID(), String.valueOf(i), argVal,
				argObj.externalProperties, store, trace, param);
			prop = argObj.externalProperties.get(String.valueOf(i));

			/* Add or update the argument object to the store. */
			argAddr = trace.makeAddr(param.getID(), String.valueOf(i));
			store = store.alloc(argAddr, argObj);

		    }

		    String name = paramName.toSource();
		    Variable identity = Variable.inject(name, prop.address,
			    Change.convU(param, (n) -> Dependencies.injectVariableChange(n)),
			    Dependencies.injectVariable(param));
		    env = env.strongUpdate(name, identity);
		}
		i++;
	    }
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
