package multidiffplus.jsanalysis.interpreter;

import java.util.LinkedList;
import java.util.List;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ObjectLiteral;

import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.FunctionEvaluator;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Closure.FunctionOrSummary;
import multidiffplus.jsanalysis.abstractdomain.Control;
import multidiffplus.jsanalysis.abstractdomain.Dependencies;
import multidiffplus.jsanalysis.abstractdomain.InternalFunctionProperties;
import multidiffplus.jsanalysis.abstractdomain.Obj;
import multidiffplus.jsanalysis.abstractdomain.Scratchpad;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Store;
import multidiffplus.jsanalysis.abstractdomain.Undefined;
import multidiffplus.jsanalysis.abstractdomain.Variable;
import multidiffplus.jsanalysis.flow.JavaScriptAnalysisState;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * An interpreter for transferring the analysis state over a statement.
 */
public class CallSiteInterpreter {

    private State state;
    private ExpEval expEval;
    private CfgMap cfgs;

    private CallSiteInterpreter(State state, CfgMap cfgs) {
	this.state = state;
	this.expEval = new ExpEval(state, cfgs);
	this.cfgs = cfgs;
    }

    /**
     * Performs an abstract interpretation on the function call.
     */
    private FunctionEvaluator initialize(FunctionCall fc) {

	/* The state after the function call. */
	State newState = null;

	/* Keep track of callback functions. */
	List<Address> callbacks = new LinkedList<Address>();

	// Attempt to resolve the function and its parent object.
	BValue funVal = expEval.resolveValue(fc.getTarget());

	// Attempt to resolve the object underlying the function.
	BValue objVal = expEval.resolveSelf(fc.getTarget());

	// Create the argument values.
	Scratchpad scratch = evalArgs(fc, funVal);

	// If the function is not a member variable, it is local and we use the
	// object of the currently executing function as self.
	Address objAddr = state.trace.toAddr("this");
	if (objVal == null)
	    objAddr = state.selfAddr;
	else
	    state.store = state.store.alloc(objAddr, objVal, new Name());

	if (funVal != null) {

	    // Update the control-call domain for the function call.
	    Control control = state.control;
	    control = control.update(fc);

	    // Get either (1) the initial state of the callee if funVal resolves
	    // to a CFG, or (2) the post-IState of the current stack frame if
	    // the funVal resolves to a function summary.
	    FunctionEvaluator evaluator = applyClosure(funVal, objAddr, state.store, scratch,
		    state.trace, control);

	    if (evaluator.resolved()) {
		return evaluator;
	    }

	}

	// TODO: The function couldn't be resolved. Build a dummy function summary.
	FunctionEvaluator evaluator = new FunctionEvaluator();
	evaluator.joinPostCallState(stateToJoin);

	if (newState == null) {
	    // Because our analysis is not complete, the identifier may not
	    // point to any function object. In this case, we assume the
	    // (local) state is unchanged, but add BValue.TOP as the return
	    // value.

	    // CASE 1: Function does not resolve:
	    // (1) If the function call is inserted, the return value is new.
	    // (2) If the target function has changed, the return value has changed.
	    // (3) If any of the argument values have changed, the return value has changed.
	    Change retValChange;
	    if (Change.test(fc))
		retValChange = Change.conv(fc, Dependencies::injectValueChange);
	    else if (Change.testU(fc.getTarget()))
		retValChange = Change.convU(fc.getTarget(), Dependencies::injectValueChange);
	    else
		retValChange = argChange;

	    // Create the return value.
	    BValue retVal = BValue.top(retValChange, Dependencies.injectValue(fc));

	    // Add the function call's value to the scratch space.
	    state.scratch = state.scratch.weakUpdate(fc, retVal);
	    newState = new State(state.store, state.env, state.scratch, state.trace, state.control,
		    state.selfAddr);
	} else {
	    // The function exists and must be evaluated.

	    // CASE 2: Function resolves.
	    // (1) If the function call is inserted, the return value is new.
	    // (2) If the target function has changed, the return value has changed.
	    // (3) If the return value has changed, the return value has changed.
	    Change retValChange;
	    if (Change.test(fc))
		// The entire call is new.
		retValChange = Change.conv(fc, Dependencies::injectValueChange);
	    else if (Change.testU(fc.getTarget()))
		// The target has changed.
		retValChange = Change.convU(fc.getTarget(), Dependencies::injectValueChange);
	    else if (funVal.change.isChanged())
		// The target has changed.
		retValChange = funVal.change;
	    else
		retValChange = Change.u();

	    BValue retVal = newState.scratch.applyReturn();
	    if (retVal == null) {
		// Functions with no return statement return undefined.
		retVal = Undefined.inject(Undefined.top(), retValChange,
			Dependencies.injectValue(fc));
		newState.scratch = newState.scratch.weakUpdate(fc, retVal);
	    } else {
		// This could be a new value if the call is new.
		newState.scratch = newState.scratch.weakUpdate(fc,
			retVal.join(BValue.bottom(retValChange, Dependencies.bot())));
	    }

	}

    }

    /**
     * Performs an abstract interpretation on the function call.
     */
    private void interpret(FunctionCall fc, JavaScriptAnalysisState exitState) {

	// Resolve the function and its parent object.
	BValue funVal = expEval.resolveValue(fc.getTarget());

	// Get the exit state of the analysis.
	State returnState = exitState.getUnderlyingState();

	// (1) If the function call is inserted, the return value is new.
	// (2) If the target function has changed, the return value has changed.
	// (3) If the return value has changed, the return value has changed.
	Change retValChange;
	if (Change.test(fc))
	    // The entire call is new.
	    retValChange = Change.conv(fc, Dependencies::injectValueChange);
	else if (Change.testU(fc.getTarget()))
	    // The target has changed.
	    retValChange = Change.convU(fc.getTarget(), Dependencies::injectValueChange);
	else if (funVal.change.isChanged())
	    // The target has changed.
	    retValChange = funVal.change;
	else
	    retValChange = Change.u();

	BValue retVal = returnState.scratch.applyReturn();
	if (retVal == null) {
	    // Functions with no return statement return undefined.
	    retVal = Undefined.inject(Undefined.top(), retValChange, Dependencies.injectValue(fc));
	    returnState.scratch = returnState.scratch.strongUpdate(retVal, null);
	} else {
	    // This could be a new value if the call is new.
	    returnState.scratch = returnState.scratch.strongUpdate(
		    retVal.join(BValue.bottom(retValChange, Dependencies.bot())), null);
	}

	this.state.store = returnState.store;
	return newState.scratch.applyReturn();

	// TODO: Add a map of FunctionCalls BValues to State. Isn't this just the return
	// value? Yes, but we may have return values for multiple functions (e.g.,
	// foo() + bar()).
	// TODO: Update the state of the FunctionCall's BValue.

	// TODO: How do we add async functions if we can't access the call stack?
	// Analyze any callbacks if we are running an analysis. We are running
	// an analysis if there is a callstack.
	// if (callStack != null) {
	// for (Address addr : callbacks) {
	// Obj funct = newState.store.getObj(addr);
	// InternalFunctionProperties ifp = (InternalFunctionProperties)
	// funct.internalProperties;
	// FunctionClosure closure = (FunctionClosure) ifp.closure;
	// callStack.addAsync(new JavaScriptAsyncFunctionCall(closure, state.selfAddr,
	// state.store, state.trace));
	// }
	// }

    }

    private Scratchpad evalArgs(FunctionCall fc, BValue funVal) {

	BValue[] args = new BValue[fc.getArguments().size()];
	Change argChange = Change.bottom();
	int i = 0;
	for (AstNode arg : fc.getArguments()) {

	    // Get the value of the object. It could be a function, object literal, etc.
	    BValue argVal = expEval.eval(arg);

	    if (Change.test(fc))
		// The entire call is new.
		argVal.change = argVal.change
			.join(Change.conv(fc, Dependencies::injectValueChange));
	    else if (Change.testU(fc.getTarget()))
		// The target has changed.
		argVal.change = argVal.change
			.join(Change.convU(fc.getTarget(), Dependencies::injectValueChange));
	    else if (funVal != null && funVal.change.isChanged())
		// The target has changed.
		argVal.change = argVal.change.join(funVal.change);
	    else if (Change.testU(arg))
		// The argument has changed.
		argVal.change = argVal.change
			.join(Change.convU(arg, Dependencies::injectValueChange));

	    // Aggregate the change across all args.
	    argChange = argChange.join(argVal.change);

	    if (arg instanceof ObjectLiteral) {
		// If this is an object literal, make a fake var in the
		// environment and point it to the object literal. This is
		// useful for function reachability.
		Address address = state.trace.makeAddr(arg.getID(), "");
		String argName = arg.getID().toString();
		state.env.strongUpdateNoCopy(argName,
			Variable.inject(argName, address, Change.bottom(), Dependencies.bot()));
		state.store = state.store.alloc(address, argVal, new Name());
	    }

	    args[i] = argVal;

	    /* Arguments of bind, call or apply are not callbacks. */
	    // AstNode target = fc.getTarget();
	    // if (!(target instanceof PropertyGet
	    // && ((PropertyGet) target).getRight().toSource().equals("bind")))
	    // callbacks.addAll(extractFunctions(argVal, new LinkedList<Address>(),
	    // new HashSet<Address>()));

	    i++;

	}

	return Scratchpad.initialize(args);

    }

    /**
     * @param funVal
     *            The address(es) of the function object to execute.
     * @param selfAddr
     *            The value of the 'this' identifier (a set of objects).
     * @param store
     *            The store at the caller.
     * @param sp
     *            Scratchpad memory.
     * @param trace
     *            The trace at the caller.
     * @return The final state of the closure.
     */
    public static FunctionEvaluator applyClosure(BValue funVal, Address selfAddr, Store store,
	    Scratchpad sp, Trace trace, Control control) {

	FunctionEvaluator evaluator = new FunctionEvaluator();

	// Get the results for each possible target function.
	for (Address address : funVal.addressAD.addresses) {

	    // Get the function object to execute.
	    Obj functObj = store.getObj(address);

	    // Ignore addresses that don't resolve to objects.
	    if (functObj == null
		    || !(functObj.internalProperties instanceof InternalFunctionProperties)) {
		continue;
	    }
	    InternalFunctionProperties ifp = (InternalFunctionProperties) functObj.internalProperties;

	    // The closure will either yield a function to evaluate, or an updated
	    // state computed from a function summary.
	    FunctionOrSummary fos = ifp.closure.initializeOrRun(selfAddr, store, sp, trace,
		    control);

	    // Add the result of the function to the evaluator.
	    if (fos.isFunctionSummary()) {
		evaluator.joinPostCallState(fos.getPostCallStateOfSummary());
	    } else {
		evaluator.addInitialTargetState(fos.getInitialStateOfFunction());
	    }

	    // TODO: This should go in a new method in IState... one that consumes
	    // the return value of the function call and updates the state.

	    // /* Run the function. */
	    // JavaScriptAnalysisState endState = ifp.closure.run(selfAddr, store, sp,
	    // trace, control,
	    // callStack);
	    //
	    // if (endState == null)
	    // // The function could not be resolved by the store.
	    // continue;
	    // if (state == null)
	    // // This is the first function to be resolved.
	    // state = endState.getUnderlyingState();
	    // else {
	    // // This is not the first function to be resolved. For example,
	    // // callback parameters frequently point to more than one
	    // // function definition. In this case, we must join states.
	    // state.store = state.store.join(endState.getUnderlyingState().store);
	    // state.scratch = state.scratch.join(endState.getUnderlyingState().scratch);
	    // }

	}

	return evaluator;

    }

}
