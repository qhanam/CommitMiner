package multidiffplus.jsanalysis.abstractdomain;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ScriptNode;

import multidiff.analysis.flow.CallStack;
import multidiff.analysis.flow.StackFrame;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.IState;
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

    /**
     * @param cfg
     *            The control flow graph for the function.
     * @param environment
     *            The environment of the parent closure. Does not yet contain local
     *            variables of this function.
     */
    public FunctionClosure(CFG cfg, Environment environment) {
	this.cfg = cfg;
	this.environment = environment;
    }

    @Override
    public JavaScriptAnalysisState run(Address selfAddr, Store store, Scratchpad scratchpad,
	    Trace trace, Control control) {
	// return Helpers.getMergedExitState(cfg);
	return (JavaScriptAnalysisState) cfg.getMergedExitState();
    }

    @Override
    public JavaScriptAnalysisState run(Address selfAddr, Store store, Scratchpad scratchpad,
	    Trace trace, Control control, CallStack callStack) {

	// Advance the trace.
	trace = trace.update(environment, store, selfAddr,
		(ScriptNode) cfg.getEntryNode().getStatement());

	// Create the initial state if needed.
	IState newState = null;
	IState oldState = cfg.getEntryNode().getBeforeState();
	IState primeState = JavaScriptAnalysisState.initializeFunctionState(
		initState(selfAddr, store, scratchpad, trace, control, callStack));
	IState exitState = null;

	if (oldState == null) {
	    // Create the initial state for the function call by lifting local vars and
	    // functions into the environment.
	    newState = primeState;
	    // Add a new frame to the call stack so that the callee is executed next.
	    callStack.push(new StackFrame(cfg, newState));
	    // We do not have an exit state, because we must first evaluate the new call.
	    return null;
	}

	newState = oldState.join(primeState);

	if (!oldState.equivalentTo(newState)) {
	    // We have a new initial state for the function. Add a new frame to the call
	    // stack so that
	    // the callee is analyzed next.
	    callStack.push(new StackFrame(cfg, newState));
	    // We do not have an exit state, because we must first evaluate the new call.
	    return null;
	}

	// The initial state of the function has not changed, do we don't
	// need to re-analyze the function.
	exitState = cfg.getMergedExitState();

	if (exitState == null) {
	    // We are in a recursive loop. Don't update the state.
	    return (JavaScriptAnalysisState) newState;
	}

	return (JavaScriptAnalysisState) exitState;

    }

    /**
     * Lift local variables and function declarations into the environment and
     * create the initial state for the function call.
     * 
     * @return The environment for the closure, including parameters and
     *         {@code this}.
     */
    private State initState(Address selfAddr, Store store, Scratchpad scratchpad, Trace trace,
	    Control control, CallStack callStack) {

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
	store = Helpers.lift(env, store, (ScriptNode) cfg.getEntryNode().getStatement(),
		callStack.getCfgMap(), trace);

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
