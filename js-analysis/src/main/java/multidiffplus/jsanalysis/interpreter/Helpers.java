package multidiffplus.jsanalysis.interpreter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ScriptNode;

import multidiff.analysis.flow.CallStack;
import multidiff.analysis.flow.StackFrame;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.IBuiltinState;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Closure;
import multidiffplus.jsanalysis.abstractdomain.Closure.FunctionOrSummary;
import multidiffplus.jsanalysis.abstractdomain.Dependencies;
import multidiffplus.jsanalysis.abstractdomain.Environment;
import multidiffplus.jsanalysis.abstractdomain.FunctionClosure;
import multidiffplus.jsanalysis.abstractdomain.InternalFunctionProperties;
import multidiffplus.jsanalysis.abstractdomain.InternalObjectProperties;
import multidiffplus.jsanalysis.abstractdomain.JSClass;
import multidiffplus.jsanalysis.abstractdomain.Num;
import multidiffplus.jsanalysis.abstractdomain.Obj;
import multidiffplus.jsanalysis.abstractdomain.Property;
import multidiffplus.jsanalysis.abstractdomain.Scratchpad;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Store;
import multidiffplus.jsanalysis.abstractdomain.Undefined;
import multidiffplus.jsanalysis.abstractdomain.Variable;
import multidiffplus.jsanalysis.flow.JavaScriptAnalysisState;
import multidiffplus.jsanalysis.hoisting.FunctionLiftVisitor;
import multidiffplus.jsanalysis.hoisting.GlobalVisitor;
import multidiffplus.jsanalysis.hoisting.VariableLiftVisitor;
import multidiffplus.jsanalysis.initstate.StoreFactory;
import multidiffplus.jsanalysis.trace.Trace;

public class Helpers {

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
	    Store store, Trace trace, AstNode node) {
	Address propAddr = trace.makeAddr(propID, prop);
	store = store.alloc(propAddr, propVal, node);
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
		Num.inject(Num.top(), Change.u(), Dependencies.bot()), external, store, trace,
		new Name());

	InternalFunctionProperties internal = new InternalFunctionProperties(
		Address.inject(StoreFactory.Function_proto_Addr, Change.u(), Dependencies.bot()),
		closure, JSClass.CFunction);

	store = store.alloc(address, new Obj(external, internal));

	return store;

    }

    /**
     * Initializes the parameters for a function call. This involves (1) creating
     * the argument object and (2) initializing the parameters as local variables
     * which point to the argument object.
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
    public static Store initParams(Environment env, Store store, Scratchpad scratchpad, Trace trace,
	    FunctionNode function) {

	/* Create the arguments object. */
	Map<String, Property> ext = new HashMap<String, Property>();
	int i = 0;
	for (BValue argVal : scratchpad.applyArgs()) {
	    AstNode dependentToken = i < function.getParams().size() ? function.getParams().get(i)
		    : new Name();
	    store = Helpers.addProp(function.getID(), String.valueOf(i), argVal, ext, store, trace,
		    dependentToken);
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
		env.strongUpdateNoCopy(name, identity);
	    }
	    i++;
	}

	return store;

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
    public static Store lift(Environment env, Store store, ScriptNode function, CfgMap cfgMap,
	    Trace trace) {

	/*
	 * Lift variables into the function's environment and initialize to undefined.
	 */
	List<Name> localVars = VariableLiftVisitor.getVariableDeclarations(function);
	for (Name localVar : localVars) {
	    Address address = trace.makeAddr(localVar.getID(), "");

	    // Initialize the variable.
	    env.strongUpdateNoCopy(localVar.toSource(),
		    Variable.inject(localVar.toSource(), address,
			    Change.convU(localVar, Dependencies::injectVariableChange),
			    Dependencies.injectVariable(localVar)));

	    // Initialize the value to undefined.
	    store = store.alloc(address,
		    Undefined.inject(Undefined.top(),
			    Change.convU(localVar,
				    (node) -> Dependencies.injectValueChange(new Name())),
			    Dependencies.injectValue(localVar)),
		    new Name());
	}

	/*
	 * Lift functions into the function's environment and initialize to function
	 * objects.
	 */
	List<FunctionNode> children = FunctionLiftVisitor.getFunctionDeclarations(function);
	for (FunctionNode child : children) {
	    if (child.getName().isEmpty())
		continue; // Not accessible.
	    Address address = trace.makeAddr(child.getID(), "");
	    address = trace.modAddr(address, JSClass.CFunction);

	    /* The function name variable points to our new function. */
	    String name = child.getName();
	    env.strongUpdateNoCopy(name,
		    Variable.inject(name, address,
			    Change.convU(child.getFunctionName(),
				    Dependencies::injectVariableChange),
			    Dependencies.injectVariable(child.getFunctionName())));
	    Change valueChange = Change.conv(child, Dependencies::injectValueChange);
	    store = store.alloc(address,
		    Address.inject(address, valueChange, Dependencies.injectValue(child)), child);

	    /* Create a function object. */
	    Closure closure = new FunctionClosure(cfgMap.getCfgFor(child), env, cfgMap);
	    store = createFunctionObj(closure, store, trace, address, child);
	}

	return store;

    }

    /**
     * Finds functions which are reachable from the current scope and have not yet
     * been analyzed and adds them to a set to be analyzed later.
     */
    public static Set<Pair<CFG, IBuiltinState>> findReachableFunctions(CallStack callStack,
	    CfgMap cfgs) {
	StackFrame stackFrame = callStack.peek();

	/* Get the set of local vars to search for unanalyzed functions. */
	Set<String> localVars = new HashSet<String>();
	List<Name> localVarNames = VariableLiftVisitor.getVariableDeclarations(
		(ScriptNode) stackFrame.getCFG().getEntryNode().getStatement());
	for (Name localVarName : localVarNames)
	    localVars.add(localVarName.toSource());

	if (callStack.isScriptLevel()) {
	    /* Add globals. */
	    for (String globalVarName : GlobalVisitor
		    .getGlobals((ScriptNode) stackFrame.getCFG().getEntryNode().getStatement())) {
		localVars.add(globalVarName);
	    }
	} else {
	    /* Add params. */
	    FunctionNode function = (FunctionNode) stackFrame.getCFG().getEntryNode()
		    .getStatement();
	    for (AstNode name : function.getParams()) {
		localVars.add(name.toSource());
	    }
	}

	/* Get the set of local functions to search for unanalyzed functions. */
	List<FunctionNode> localFunctions = FunctionLiftVisitor.getFunctionDeclarations(
		(ScriptNode) stackFrame.getCFG().getEntryNode().getStatement());
	for (FunctionNode localFunction : localFunctions) {
	    Name name = localFunction.getFunctionName();
	    if (name != null)
		localVars.add(name.toSource());
	}

	/*
	 * Case where control cannot reach the end of the function because an error is
	 * thrown on the single path (e.g., function() { throw new Error(); })
	 */
	if (stackFrame.getCFG().getExitNodes().stream().anyMatch((node) -> {
	    return node.getIncommingEdgeCount() == 0;
	})) {
	    return Collections.emptySet();
	}

	/* Analyze reachable functions. */
	State exitState = ((JavaScriptAnalysisState) stackFrame.getCFG().getMergedExitState()
		.getBuiltinState()).getUnderlyingState();
	return Helpers.analyzeEnvReachable(exitState, exitState.env.environment, exitState.selfAddr,
		new HashSet<Address>(), localVars, cfgs);
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
    private static Set<Pair<CFG, IBuiltinState>> analyzeEnvReachable(State state,
	    Map<String, Variable> vars, Address selfAddr, Set<Address> visited,
	    Set<String> localvars, CfgMap cfgs) {

	Set<Pair<CFG, IBuiltinState>> reachables = new HashSet<>();

	for (Map.Entry<String, Variable> entry : vars.entrySet()) {
	    for (Address addr : entry.getValue().addresses.addresses) {
		reachables.addAll(analyzePublic(state, entry.getKey(), addr, selfAddr, visited,
			localvars, cfgs));
	    }
	}

	return reachables;

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
    private static Set<Pair<CFG, IBuiltinState>> analyzeObjReachable(State state,
	    Map<String, Property> props, Address selfAddr, Set<Address> visited,
	    Set<String> localvars, CfgMap cfgs) {

	Set<Pair<CFG, IBuiltinState>> reachables = new HashSet<>();

	for (Map.Entry<String, Property> entry : props.entrySet()) {
	    reachables.addAll(analyzePublic(state, entry.getKey(), entry.getValue().address,
		    selfAddr, visited, localvars, cfgs));
	}

	return reachables;

    }

    /**
     * Returns a set of functions which are reachable from an object property and
     * that have not already been analyzed.
     * 
     * @param state
     *            The end state of the parent function.
     * @param var
     *            The name of the property or variable
     * @param addr
     *            The address pointed to by the property or variable.
     * @param visited
     *            Prevent circular lookups.
     */
    private static Set<Pair<CFG, IBuiltinState>> analyzePublic(State state, String name,
	    Address addr, Address selfAddr, Set<Address> visited, Set<String> localvars,
	    CfgMap cfgs) {

	Set<Pair<CFG, IBuiltinState>> reachables = new HashSet<>();

	BValue val = state.store.apply(addr, new Name());

	/*
	 * Do not visit local variables which were declared at a higher level, and
	 * therefore can be analyzed later.
	 */
	if (localvars != null && !localvars.contains(name) && !name.equals("~retval~")
		&& !StringUtils.isNumeric(name))
	    return reachables;

	/* Avoid circular references. */
	if (visited.contains(addr))
	    return reachables;
	visited.add(addr);

	// Identify all reachable functions. If a function has not yet been analyzed
	for (Address objAddr : val.addressAD.addresses) {
	    Obj obj = state.store.getObj(objAddr);

	    /* We may need to analyze this function. */
	    if (obj.internalProperties.klass == JSClass.CFunction) {

		InternalFunctionProperties ifp = (InternalFunctionProperties) obj.internalProperties;
		FunctionClosure fc = (FunctionClosure) ifp.closure;

		/* FunctionClosures may not be defined for some built-in functions. */
		if (fc == null)
		    continue;

		if (fc.cfg.getEntryNode().getBeforeState() == null) {
		    FunctionOrSummary functionOrSummary = fc.initializeOrRun(state, selfAddr,
			    state.store, Scratchpad.empty(), state.trace, state.control, cfgs);
		    if (functionOrSummary.isFunctionSummary())
			continue;
		    reachables.add(functionOrSummary.getInitialStateOfFunction());
		}
	    }

	    /* Recursively look for object properties that are functions. */
	    reachables.addAll(
		    analyzeObjReachable(state, obj.externalProperties, addr, visited, null, cfgs));

	}

	return reachables;
    }

}