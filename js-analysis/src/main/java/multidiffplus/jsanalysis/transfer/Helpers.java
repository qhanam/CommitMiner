package multidiffplus.jsanalysis.transfer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ScriptNode;

import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGNode;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.Addresses;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Closure;
import multidiffplus.jsanalysis.abstractdomain.Control;
import multidiffplus.jsanalysis.abstractdomain.DefinerIDs;
import multidiffplus.jsanalysis.abstractdomain.Environment;
import multidiffplus.jsanalysis.abstractdomain.FunctionClosure;
import multidiffplus.jsanalysis.abstractdomain.InternalFunctionProperties;
import multidiffplus.jsanalysis.abstractdomain.JSClass;
import multidiffplus.jsanalysis.abstractdomain.Num;
import multidiffplus.jsanalysis.abstractdomain.Obj;
import multidiffplus.jsanalysis.abstractdomain.Property;
import multidiffplus.jsanalysis.abstractdomain.Scratchpad;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Store;
import multidiffplus.jsanalysis.abstractdomain.Undefined;
import multidiffplus.jsanalysis.abstractdomain.Variable;
import multidiffplus.jsanalysis.factories.StoreFactory;
import multidiffplus.jsanalysis.flow.CallStack;
import multidiffplus.jsanalysis.trace.Trace;
import multidiffplus.jsanalysis.visitors.FunctionLiftVisitor;
import multidiffplus.jsanalysis.visitors.VariableLiftVisitor;

public class Helpers {

    /**
     * Adds a property to the object and allocates the property's value on the
     * store.
     * 
     * @param prop
     *            The name of the property to add to the object.
     */
    public static Store addProp(String prop, BValue propVal, Map<String, Address> ext, Store store,
	    Trace trace) {
	Address propAddr = trace.toAddr(prop);
	store = store.alloc(propAddr, propVal);
	ext.put(prop, propAddr);
	return store;
    }

    /**
     * Adds a property to the object and allocates the property's value on the
     * store.
     * 
     * @param propID
     *            The node id of the property being added to the object.
     * @param propVal
     *            The value of the property.
     */
    public static Store addProp(int propID, String prop, BValue propVal, Map<String, Property> ext,
	    Store store, Trace trace) {
	Address propAddr = trace.makeAddr(propID, prop);
	store = store.alloc(propAddr, propVal);
	ext.put(prop, new Property(propID, prop, propAddr));
	return store;
    }

    /**
     * Creates and allocates a regular function from a closure stack.
     * 
     * @param closures
     *            The closure for the function.
     * @return The function object.
     */
    public static Store createFunctionObj(Closure closure, Store store, Trace trace,
	    Address address, FunctionNode function) {

	Map<String, Property> external = new HashMap<String, Property>();
	store = addProp(function.getID(), "length",
		Num.inject(Num.top(), Change.u(), DefinerIDs.bottom()), external, store, trace);

	InternalFunctionProperties internal = new InternalFunctionProperties(
		Address.inject(StoreFactory.Function_proto_Addr, Change.u(), DefinerIDs.bottom()),
		closure, JSClass.CFunction);

	store = store.alloc(address, new Obj(external, internal));

	return store;

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
    public static State applyClosure(BValue funVal, Address selfAddr, Store store, Scratchpad sp,
	    Trace trace, Control control, CallStack callStack) {

	State state = null;

	/* Get the results for each possible function. */
	for (Address address : funVal.addressAD.addresses) {

	    /* Get the function object to execute. */
	    Obj functObj = store.getObj(address);

	    /* Ignore addresses that don't resolve to objects. */
	    if (functObj == null
		    || !(functObj.internalProperties instanceof InternalFunctionProperties)) {
		continue;
	    }
	    InternalFunctionProperties ifp = (InternalFunctionProperties) functObj.internalProperties;

	    /* Run the function. */
	    State endState = ifp.closure.run(selfAddr, store, sp, trace, control, callStack);

	    if (state == null)
		state = endState;
	    else {
		/*
		 * Join the store and scratchpad only. Environment, trace and control no longer
		 * apply.
		 */
		state.store = state.store.join(endState.store);
		state.scratch = state.scratch.join(endState.scratch);
	    }

	}

	return state;

    }

    /**
     * Lifts local variables and function definitions into the environment.
     * 
     * @param env
     *            The environment (or closure) in which the function executes.
     * @param store
     *            The current store.
     * @param function
     *            The code we are analyzing.
     * @param cfgs
     *            The control flow graphs for the file. Needed for initializing
     *            lifted functions.
     * @param trace
     *            The program trace including the call site of this function.
     * @return The new store. The environment is updated directly (no new object is
     *         created)
     */
    public static Store lift(Environment env, Store store, ScriptNode function,
	    Map<AstNode, CFG> cfgs, Trace trace) {

	/*
	 * Lift variables into the function's environment and initialize to undefined.
	 */
	List<Name> localVars = VariableLiftVisitor.getVariableDeclarations(function);
	for (Name localVar : localVars) {
	    Address address = trace.makeAddr(localVar.getID(), "");
	    env.strongUpdateNoCopy(localVar.toSource(), new Variable(localVar.getID(),
		    localVar.toSource(), Change.convU(localVar), new Addresses(address)));
	    store = store.alloc(address,
		    Undefined.inject(Undefined.top(), Change.u(), DefinerIDs.bottom()));
	}

	/*
	 * Get a list of function declarations to lift into the function's environment.
	 */
	List<FunctionNode> children = FunctionLiftVisitor.getFunctionDeclarations(function);
	for (FunctionNode child : children) {
	    if (child.getName().isEmpty())
		continue; // Not accessible.
	    Address address = trace.makeAddr(child.getID(), "");
	    address = trace.modAddr(address, JSClass.CFunction);

	    /* The function name variable points to our new function. */
	    env.strongUpdateNoCopy(child.getName(), new Variable(child.getID(), child.getName(),
		    Change.convU(child.getFunctionName()), new Addresses(address))); // Env update
										     // with env
										     // change type
	    store = store.alloc(address,
		    Address.inject(address, Change.convU(child), DefinerIDs.inject(child.getID())));

	    /* Create a function object. */
	    Closure closure = new FunctionClosure(cfgs.get(child), env);
	    store = createFunctionObj(closure, store, trace, address, child);

	}

	return store;

    }

    /**
     * Merges the exit states of a function into one single state.
     * 
     * @return The merged exit states of the function.
     */
    public static State getMergedExitState(CFG cfg) {
	State exitState = null;
	for (CFGNode exitNode : cfg.getExitNodes()) {
	    if (exitState == null)
		exitState = (State) exitNode.getBeforeState();
	    else
		exitState = exitState.join((State) exitNode.getBeforeState());
	}
	return exitState;
    }

    /**
     * Analyze functions which are reachable from the environment and that have not
     * already been analyzed.
     * 
     * @param state
     *            The end state of the parent function.
     * @param visited
     *            Prevent circular lookups.
     */
    public static boolean analyzeEnvReachable(State state, Map<String, Variable> vars,
	    Address selfAddr, Map<AstNode, CFG> cfgMap, Set<Address> visited, Set<String> localvars,
	    CallStack callStack) {

	for (Map.Entry<String, Variable> entry : vars.entrySet()) {
	    for (Address addr : entry.getValue().addresses.addresses) {
		if (analyzePublic(state, entry.getKey(), addr, selfAddr, cfgMap, visited, localvars,
			callStack))
		    return true;
	    }
	}

	return false;

    }

    /**
     * Analyze functions which are reachable from an object property and that have
     * not already been analyzed.
     * 
     * @param state
     *            The end state of the parent function.
     * @param visited
     *            Prevent circular lookups.
     */
    private static boolean analyzeObjReachable(State state, Map<String, Property> props,
	    Address selfAddr, Map<AstNode, CFG> cfgMap, Set<Address> visited, Set<String> localvars,
	    CallStack callStack) {

	for (Map.Entry<String, Property> entry : props.entrySet()) {
	    if (analyzePublic(state, entry.getKey(), entry.getValue().address, selfAddr, cfgMap,
		    visited, localvars, callStack))
		return true;
	}

	return false;

    }

    /**
     * Analyze functions which are reachable from an object property and that have
     * not already been analyzed.
     * 
     * @param state
     *            The end state of the parent function.
     * @param var
     *            The name of the property or variable
     * @param addr
     *            The address pointed to by the property or variable.
     * @param visited
     *            Prevent circular lookups.
     * @return {@code true} when a reachable frame has been added to the call stack.
     */
    private static boolean analyzePublic(State state, String name, Address addr, Address selfAddr,
	    Map<AstNode, CFG> cfgMap, Set<Address> visited, Set<String> localvars,
	    CallStack callStack) {

	BValue val = state.store.apply(addr);

	/*
	 * Do not visit local variables which were declared at a higher level, and
	 * therefore can be analyzed later.
	 */
	if (localvars != null && !localvars.contains(name) && !name.equals("~retval~")
		&& !StringUtils.isNumeric(name))
	    return false;

	/* Avoid circular references. */
	if (visited.contains(addr))
	    return false;
	visited.add(addr);

	for (Address objAddr : val.addressAD.addresses) {
	    Obj obj = state.store.getObj(objAddr);

	    /* We may need to analyze this function. */
	    if (obj.internalProperties.klass == JSClass.CFunction) {

		InternalFunctionProperties ifp = (InternalFunctionProperties) obj.internalProperties;
		FunctionClosure fc = (FunctionClosure) ifp.closure;

		if (ifp.closure instanceof FunctionClosure
			&& fc.cfg.getEntryNode().getBeforeState() == null) {

		    /* Create the control domain. */
		    Control control = new Control();

		    /* Create the argument object. */
		    Scratchpad scratch = new Scratchpad(null, new BValue[0]);

		    /*
		     * Analyze the function. Use a fresh call stack because we don't have any
		     * knowledge of it.
		     */
		    ifp.closure.run(selfAddr, state.store, scratch, state.trace, control,
			    callStack);

		    /*
		     * We can only request one frame be pushed onto the call stack at once. After
		     * the function is analyzed, we will get back to this function and analyze the
		     * rest.
		     */
		    return true;

		    /* Check the function object. */
		    // TODO: We ignore this for now. We would have to assume the function is being
		    // run as a constructor.

		}
	    }

	    /* Recursively look for object properties that are functions. */
	    analyzeObjReachable(state, obj.externalProperties, addr, cfgMap, visited, null,
		    callStack);

	}

	return false;
    }

}