package multidiffplus.jsanalysis.transfer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.UnaryExpression;

import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.Addresses;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Bool;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Closure;
import multidiffplus.jsanalysis.abstractdomain.Control;
import multidiffplus.jsanalysis.abstractdomain.Dependencies;
import multidiffplus.jsanalysis.abstractdomain.FunctionClosure;
import multidiffplus.jsanalysis.abstractdomain.InternalFunctionProperties;
import multidiffplus.jsanalysis.abstractdomain.InternalObjectProperties;
import multidiffplus.jsanalysis.abstractdomain.JSClass;
import multidiffplus.jsanalysis.abstractdomain.Null;
import multidiffplus.jsanalysis.abstractdomain.Num;
import multidiffplus.jsanalysis.abstractdomain.Obj;
import multidiffplus.jsanalysis.abstractdomain.Property;
import multidiffplus.jsanalysis.abstractdomain.Scratchpad;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Str;
import multidiffplus.jsanalysis.abstractdomain.Undefined;
import multidiffplus.jsanalysis.abstractdomain.Variable;
import multidiffplus.jsanalysis.flow.CallStack;
import multidiffplus.jsanalysis.flow.ReachableFunction;
import multidiffplus.jsanalysis.flow.StackFrame;

public class ExpEval {

    private CallStack callStack;
    private State state;

    /**
     * Use inside transfer functions.
     */
    public ExpEval(CallStack callStack, State state) {
	this.callStack = callStack;
	this.state = state;
    }

    /**
     * Only use when functions are not called (ie. by CFG visitors).
     */
    public ExpEval(State state) {
	this.callStack = null;
	this.state = state;
    }

    /**
     * Evaluate an expression to a BValue.
     * 
     * @param node
     *            The expression to evaluate.
     * @return The value of the expression.
     */
    public BValue eval(AstNode node) {

	if (node instanceof Name) {
	    return evalName((Name) node);
	} else if (node instanceof InfixExpression) {
	    return evalInfixExpression((InfixExpression) node);
	} else if (node instanceof ElementGet) {
	    return evalElementGet((ElementGet) node);
	} else if (node instanceof UnaryExpression) {
	    return evalUnaryExpression((UnaryExpression) node);
	} else if (node instanceof KeywordLiteral) {
	    return evalKeywordLiteral((KeywordLiteral) node);
	} else if (node instanceof StringLiteral) {
	    return evalStringLiteral((StringLiteral) node);
	} else if (node instanceof NumberLiteral) {
	    return evalNumberLiteral((NumberLiteral) node);
	} else if (node instanceof ObjectLiteral) {
	    return evalObjectLiteral((ObjectLiteral) node);
	} else if (node instanceof ArrayLiteral) {
	    return evalArrayLiteral((ArrayLiteral) node);
	} else if (node instanceof FunctionNode) {
	    return evalFunctionNode((FunctionNode) node);
	} else if (node instanceof FunctionCall) {
	    return evalFunctionCall((FunctionCall) node);
	} else if (node instanceof ParenthesizedExpression) {
	    return evalParenthesizedExpression((ParenthesizedExpression) node);
	}

	/* We could not evaluate the expression. Return top. */
	return BValue.top(Change.convU(node, Dependencies::injectValueChange),
		Dependencies.injectValue(node));

    }

    /**
     * @param pe
     *            The parenthesized expression.
     * @return The value of the expression inside the parentheses.
     */
    private BValue evalParenthesizedExpression(ParenthesizedExpression pe) {
	return eval(pe.getExpression());
    }

    /**
     * @param eg
     *            The element get expression.
     * @return the value of the element.
     */
    private BValue evalElementGet(ElementGet eg) {
	/* This is an identifier.. so we attempt to dereference it. */
	BValue val = resolveValue(eg);
	if (val == null)
	    return BValue.top(Change.u(), Dependencies.injectValue(eg));
	return val;
    }

    /**
     * Creates a new function from a function definition.
     * 
     * @param f
     *            The function definition.
     * @return A BValue that points to the new function object.
     */
    public BValue evalFunctionNode(FunctionNode f) {
	Closure closure = new FunctionClosure(callStack.getCFGs().get(f), state.env);
	Address addr = state.trace.makeAddr(f.getID(), "");
	addr = state.trace.modAddr(addr, JSClass.CFunction);
	state.store = Helpers.createFunctionObj(closure, state.store, state.trace, addr, f);
	return Address.inject(addr, Change.convU(f, Dependencies::injectValueChange),
		Dependencies.injectValue(f));
    }

    /**
     * Creates a new object from an object literal.
     * 
     * @param ol
     *            The object literal.
     * @return A BValue that points to the new object literal.
     */
    public BValue evalObjectLiteral(ObjectLiteral ol) {
	Map<String, Property> ext = new HashMap<String, Property>();
	InternalObjectProperties in = new InternalObjectProperties();

	for (ObjectProperty property : ol.getElements()) {
	    AstNode prop = property.getLeft();
	    String propName = "~unknown~";
	    if (prop instanceof Name)
		propName = prop.toSource();
	    else if (prop instanceof StringLiteral)
		propName = ((StringLiteral) prop).getValue();
	    else if (prop instanceof NumberLiteral)
		propName = ((NumberLiteral) prop).getValue();
	    BValue propVal = this.eval(property.getRight());
	    Address propAddr = state.trace.makeAddr(property.getID(), propName);
	    state.store = state.store.alloc(propAddr, propVal, property.getLeft());
	    if (propName != null)
		ext.put(propName, new Property(property.getID(), propName, propAddr));
	}

	Obj obj = new Obj(ext, in);
	Address objAddr = state.trace.makeAddr(ol.getID(), "");
	state.store = state.store.alloc(objAddr, obj);

	return Address.inject(objAddr, Change.convU(ol, Dependencies::injectValueChange),
		Dependencies.injectValue(ol));
    }

    /**
     * Creates a new array from an array literal.
     * 
     * @param al
     *            The array literal.
     * @return A BValue that points to the new array literal.
     */
    public BValue evalArrayLiteral(ArrayLiteral al) {

	Map<String, Property> ext = new HashMap<String, Property>();
	InternalObjectProperties in = new InternalObjectProperties();

	Integer i = 0;
	for (AstNode element : al.getElements()) {
	    BValue propVal = this.eval(element);
	    Address propAddr = state.trace.makeAddr(element.getID(), "");
	    state.store = state.store.alloc(propAddr, propVal, new Name());
	    ext.put(i.toString(), new Property(element.getID(), i.toString(), propAddr));
	    i++;
	}

	Obj obj = new Obj(ext, in);
	Address objAddr = state.trace.makeAddr(al.getID(), "");
	state.store = state.store.alloc(objAddr, obj);

	return Address.inject(objAddr, Change.convU(al, Dependencies::injectValueChange),
		Dependencies.injectValue(al));

    }

    /**
     * @param ue
     *            The unary expression.
     * @return the abstract interpretation of the expression.
     */
    public BValue evalUnaryExpression(UnaryExpression ue) {

	BValue operand = this.eval(ue.getOperand());

	// Change propagation:
	//
	// (1) If the underlying value has changes, the post-operator value has also
	// changed and inherits the original values dependencies.
	//
	// (2) If the operator has changed, the post-operator value has also changed and
	// a new criterion (the unary-operator expression) is added.
	Change operatorChange = Change.conv(ue, Dependencies::injectValueChange);

	// Join the operand and operator changes.
	operatorChange = operand.change.join(operatorChange);

	// Conservatively estimate the unary operator evaluations.
	switch (ue.getType()) {
	case Token.NOT:
	    return Bool.inject(Bool.top(), operatorChange,
		    Dependencies.injectValue(ue).join(operand.deps));
	case Token.INC:
	case Token.DEC:
	    return Num.inject(Num.top(), operatorChange,
		    Dependencies.injectValue(ue).join(operand.deps));
	case Token.TYPEOF:
	    return Str.inject(Str.top(), operatorChange,
		    Dependencies.injectValue(ue).join(operand.deps));
	default:
	    return BValue.top(operatorChange, Dependencies.injectValue(ue).join(operand.deps));
	}

    }

    /**
     * @param ie
     *            The infix expression.
     * @return the abstract interpretation of the name
     */
    public BValue evalInfixExpression(InfixExpression ie) {

	/* If this is an assignment, we need to interpret it through state. */
	switch (ie.getType()) {
	case Token.ASSIGN:
	    /*
	     * We need to interpret this assignment and propagate the value left.
	     */
	    this.evalAssignment((Assignment) ie);
	    return this.eval(ie.getLeft());
	case Token.GETPROPNOWARN:
	case Token.GETPROP: {
	    /* This is an identifier.. so we attempt to dereference it. */
	    BValue val = resolveValue(ie);
	    if (val == null)
		return BValue.top(Change.u(), Dependencies.injectValue(ie));
	    return val;
	}
	case Token.ADD:
	    return evalPlus(ie);
	case Token.SUB:
	case Token.MUL:
	case Token.DIV:
	case Token.MOD:
	    return evalMathOp(ie);
	case Token.ASSIGN_ADD: {
	    BValue val = evalPlus(ie);
	    this.evalAssignment(ie.getLeft(), val);
	    return this.eval(ie.getLeft());
	}
	case Token.ASSIGN_SUB:
	case Token.ASSIGN_MUL:
	case Token.ASSIGN_DIV:
	case Token.ASSIGN_MOD: {
	    BValue val = evalMathOp(ie);
	    this.evalAssignment(ie.getLeft(), val);
	    return this.eval(ie.getLeft());
	}
	default:
	    return this.evalBinOp(ie);
	}

    }

    /**
     * Evaluates an unknown binary operator on two BValues.
     */
    public BValue evalBinOp(InfixExpression ie) {

	BValue left = this.eval(ie.getLeft());
	BValue right = this.eval(ie.getRight());

	// Change propagation:
	//
	// (1) If the underlying value has changes, the post-operator value has also
	// changed and inherits the original values dependencies.
	//
	// (2) If the operator has changed, the post-operator value has also changed and
	// a new criterion (the unary-operator expression) is added.
	Change operatorChange = Change.conv(ie, Dependencies::injectValueChange);

	// Join the operand and operator changes.
	operatorChange = left.change.join(right.change).join(operatorChange);

	// Conservatively estimate the binary operator evaluations.
	return BValue.top(operatorChange,
		Dependencies.injectValue(ie).join(left.deps.join(right.deps)));

    }

    /**
     * Evaluates the plus operation on two BValues.
     */
    public BValue evalPlus(InfixExpression ie) {

	BValue left = this.eval(ie.getLeft());
	BValue right = this.eval(ie.getRight());

	// Change propagation:
	//
	// (1) If either of the underlying values have changed, the post-operator value
	// has also changed and inherits the original values dependencies.
	//
	// (2) If the operator has changed, the post-operator value has also changed and
	// a new criterion (the unary-operator expression) is added.
	Change operatorChange = Change.conv(ie, Dependencies::injectValueChange);

	// Join the operand and operator changes.
	operatorChange = left.change.join(right.change).join(operatorChange);

	// Conservatively estimate the binary operator evaluations.
	BValue val = BValue.bottom(operatorChange,
		Dependencies.injectValue(ie).join(left.deps).join(right.deps));

	// Strings
	if (left.stringAD.le != Str.LatticeElement.BOTTOM
		|| right.stringAD.le != Str.LatticeElement.BOTTOM) {
	    val = val.join(Str.inject(Str.top(), Change.bottom(), Dependencies.bot()));
	}
	// Numbers
	if (left.numberAD.le != Num.LatticeElement.BOTTOM
		|| right.numberAD.le != Num.LatticeElement.BOTTOM) {
	    val = val.join(Num.inject(Num.top(), Change.bottom(), Dependencies.bot()));
	}
	// Booleans
	if (left.booleanAD.le != Bool.LatticeElement.BOTTOM
		|| right.booleanAD.le != Bool.LatticeElement.BOTTOM) {
	    val = val.join(Num.inject(Num.top(), Change.bottom(), Dependencies.bot()));
	}
	// Nulls
	if (left.nullAD.le != Null.LatticeElement.BOTTOM
		|| right.nullAD.le != Null.LatticeElement.BOTTOM) {
	    val = val.join(Num.inject(Num.top(), Change.bottom(), Dependencies.bot()));
	}
	// Undefined
	if (left.undefinedAD.le != Undefined.LatticeElement.BOTTOM
		|| right.undefinedAD.le != Undefined.LatticeElement.BOTTOM) {
	    val = val.join(Num.inject(Num.top(), Change.bottom(), Dependencies.bot()));
	}
	// Objects and Arrays
	if (left.addressAD.le != Addresses.LatticeElement.BOTTOM
		|| right.addressAD.le != Addresses.LatticeElement.BOTTOM) {
	    // Objects
	    val = val.join(Num.inject(Num.top(), Change.bottom(), Dependencies.bot()));
	    // Arrays
	    val = val.join(Str.inject(Str.top(), Change.bottom(), Dependencies.bot()));
	}

	return val;

    }

    /**
     * Evaluates a math operation on two BValues.
     */
    public BValue evalMathOp(InfixExpression ie) {

	BValue left = this.eval(ie.getLeft());
	BValue right = this.eval(ie.getRight());

	// Change propagation:
	//
	// (1) If either of the underlying values have changed, the post-operator value
	// has also changed and inherits the original values dependencies.
	//
	// (2) If the operator has changed, the post-operator value has also changed and
	// a new criterion (the unary-operator expression) is added.
	Change operatorChange = Change.conv(ie, Dependencies::injectValueChange);
	if (operatorChange.isChanged()) {
	    // Add a criterion ID to the AST node.
	    ie.addCriterion("VALUE_CHANGE", ie.getID());
	}

	// Join the operand and operator changes.
	operatorChange = left.change.join(right.change).join(operatorChange);

	// Conservatively estimate the binary operator evaluations.
	return Num.inject(Num.top(), operatorChange,
		Dependencies.injectValue(ie).join(left.deps).join(right.deps));

    }

    /**
     * @param name
     * @return the abstract interpretation of the name
     */
    public BValue evalName(Name name) {
	BValue val = resolveValue(name);
	if (val == null) {
	    // TODO: We should never reach this point.
	    return BValue.top(Change.u(), Dependencies.injectValue(name));
	}
	return val;
    }

    /**
     * @param numl
     * @return the abstract interpretation of the number literal
     */
    public BValue evalNumberLiteral(NumberLiteral numl) {
	return Num.inject(new Num(Num.LatticeElement.VAL, numl.getValue()),
		Change.convU(numl, Dependencies::injectValueChange),
		Dependencies.injectValue(numl));
    }

    /**
     * @param strl
     *            The keyword literal.
     * @return the abstract interpretation of the string literal
     */
    public BValue evalStringLiteral(StringLiteral strl) {

	Str str = null;
	String val = strl.getValue();
	Change change = Change.convU(strl, Dependencies::injectValueChange);

	if (val.equals(""))
	    str = new Str(Str.LatticeElement.SBLANK);
	else if (NumberUtils.isCreatable(val)) {
	    str = new Str(Str.LatticeElement.SNUMVAL, val);
	} else {
	    str = new Str(Str.LatticeElement.SNOTNUMNORSPLVAL, val);
	}

	return Str.inject(str, change, Dependencies.injectValue(strl));

    }

    /**
     * @param kwl
     *            The keyword literal.
     * @return the abstract interpretation of the keyword literal.
     */
    public BValue evalKeywordLiteral(KeywordLiteral kwl) {
	Change change = Change.conv(kwl, Dependencies::injectValueChange);
	switch (kwl.getType()) {
	case Token.THIS:
	    return state.store.apply(state.selfAddr, kwl);
	case Token.NULL:
	    return Null.inject(Null.top(), change, Dependencies.injectValue(kwl));
	case Token.TRUE:
	    return Bool.inject(new Bool(Bool.LatticeElement.TRUE), change,
		    Dependencies.injectValue(kwl));
	case Token.FALSE:
	    return Bool.inject(new Bool(Bool.LatticeElement.FALSE), change,
		    Dependencies.injectValue(kwl));
	case Token.DEBUGGER:
	default:
	    return BValue.bottom(change, Dependencies.injectValue(kwl));
	}
    }

    /**
     * Updates the store based on abstract interpretation of assignments.
     * 
     * @param a
     *            The assignment.
     */
    public void evalAssignment(Assignment a) {
	evalAssignment(a.getLeft(), a.getRight());
    }

    /**
     * Helper function since variable initializers and assignments do the same
     * thing.
     */
    public void evalAssignment(AstNode lhs, AstNode rhs) {

	/* Resolve the left hand side to a set of addresses. */
	// TODO: 'resolveOrCreate' shouldn't store the deps in the AST node.
	Set<Address> addrs = this.resolveOrCreate(lhs);

	/* Resolve the right hand side to a value. */
	BValue val = this.eval(rhs);

	/* Update the values in the store. */
	for (Address addr : addrs) {
	    state.store = state.store.strongUpdate(addr, val, lhs);
	}

    }

    /**
     * Helper function since variable initializers and assignments do the same
     * thing.
     */
    public void evalAssignment(AstNode lhs, BValue val) {

	/* Resolve the left hand side to a set of addresses. */
	Set<Address> addrs = this.resolveOrCreate(lhs);

	/* Update the values in the store. */
	for (Address addr : addrs) {
	    state.store = state.store.strongUpdate(addr, val, lhs);
	}

    }

    /**
     * Evaluate a function call expression to a BValue.
     * 
     * @param fc
     *            The function call.
     * @return The return value of the function call.
     */
    public BValue evalFunctionCall(FunctionCall fc) {

	/* The state after the function call. */
	State newState = null;

	/* Keep track of callback functions. */
	List<Address> callbacks = new LinkedList<Address>();

	/* Create the argument values. */
	BValue[] args = new BValue[fc.getArguments().size()];
	Change argChange = Change.bottom();
	int i = 0;
	for (AstNode arg : fc.getArguments()) {

	    /* Get the value of the object. It could be a function, object literal, etc. */
	    BValue argVal = eval(arg);

	    if (Change.test(fc))
		// The entire call is new.
		argVal.change = argVal.change
			.join(Change.conv(fc, Dependencies::injectValueChange));
	    else if (Change.testU(fc.getTarget()))
		// The target has changed.
		argVal.change = argVal.change
			.join(Change.convU(fc.getTarget(), Dependencies::injectValueChange));
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
	    AstNode target = fc.getTarget();
	    if (!(target instanceof PropertyGet
		    && ((PropertyGet) target).getRight().toSource().equals("bind")))
		callbacks.addAll(extractFunctions(argVal, new LinkedList<Address>(),
			new HashSet<Address>()));

	    i++;

	}

	Scratchpad scratch = new Scratchpad(null, args);

	/* Attempt to resolve the function and its parent object. */
	BValue funVal = resolveValue(fc.getTarget());
	BValue objVal = resolveSelf(fc.getTarget());

	/*
	 * If the function is not a member variable, it is local and we use the object
	 * of the currently executing function as self.
	 */
	Address objAddr = state.trace.toAddr("this");
	if (objVal == null)
	    objAddr = state.selfAddr;
	else
	    state.store = state.store.alloc(objAddr, objVal, new Name());

	if (funVal != null) {

	    /* Update the control-call domain for the function call. */
	    Control control = state.control;
	    control = control.update(fc);

	    // Save the current stack frame so that we can check if a new stack frame has
	    // been added.
	    StackFrame current = callStack.peek();

	    /* Call the function and get a join of the new states. */
	    newState = Helpers.applyClosure(funVal, objAddr, state.store, scratch, state.trace,
		    control, callStack);

	    // If a stack frame has been added, we must evaluate it in the next analysis
	    // iteration before we can continue.
	    if (callStack.peek() != current) {
		return BValue.bottom();
	    }
	}

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

	    // Add the return value to the scratch space.
	    state.scratch = state.scratch.strongUpdate(retVal, null);
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
	    else
		retValChange = Change.u();

	    BValue retVal = newState.scratch.applyReturn();
	    if (retVal == null) {
		// Functions with no return statement return undefined.
		retVal = Undefined.inject(Undefined.top(), retValChange,
			Dependencies.injectValue(fc));
		newState.scratch = newState.scratch.strongUpdate(retVal, null);
	    } else {
		// This could be a new value if the call is new.
		newState.scratch = newState.scratch.strongUpdate(
			retVal.join(BValue.bottom(retValChange, Dependencies.bot())), null);
	    }

	}

	// Analyze any callbacks if we are running an analysis. We are running
	// an analysis if there is a callstack.
	if (callStack != null) {
	    for (Address addr : callbacks) {
		Obj funct = newState.store.getObj(addr);
		InternalFunctionProperties ifp = (InternalFunctionProperties) funct.internalProperties;
		FunctionClosure closure = (FunctionClosure) ifp.closure;
		callStack.addReachableFunction(
			new ReachableFunction(closure, state.selfAddr, state.store, state.trace));
	    }
	}

	this.state.store = newState.store;
	return newState.scratch.applyReturn();

    }

    /**
     * Resolves a variable to its addresses in the store.
     * 
     * @return The addresses as a BValue.
     */
    public BValue resolveValue(AstNode node) {

	BValue value = null;

	/* Resolve the identifier. */
	Set<Address> addrs = this.resolveOrCreate(node);
	if (addrs == null)
	    return null;
	for (Address addr : addrs) {
	    if (value == null)
		value = state.store.apply(addr, node);
	    else
		value = value.join(state.store.apply(addr, node));
	}

	return value;

    }

    /**
     * Resolves a function's parent object.
     * 
     * @return The parent object (this) and the function object.
     */
    public BValue resolveSelf(AstNode node) {
	if (node instanceof Name) {
	    /* This is a variable name, not a field. */
	    return null;
	} else if (node instanceof InfixExpression) {
	    /* We have a qualified name. Recursively find the addresses. */
	    InfixExpression ie = (InfixExpression) node;
	    Set<Address> addrs = this.resolveOrCreate(ie.getLeft());
	    if (addrs == null)
		return null;

	    /*
	     * Resolve all the objects in the address list and create a new BValue which
	     * points to those objects.
	     */
	    Addresses selfAddrs = new Addresses(Addresses.LatticeElement.SET);
	    Dependencies dependencies = Dependencies.bot();
	    for (Address addr : addrs) {
		BValue val = this.state.store.apply(addr, ie.getLeft());
		for (Address objAddr : val.addressAD.addresses) {
		    Obj obj = this.state.store.getObj(objAddr);
		    if (obj != null) {
			selfAddrs.addresses.add(objAddr);
			dependencies = dependencies.join(val.deps);
		    }
		}
	    }
	    if (selfAddrs.addresses.isEmpty()) {
		return BValue.bottom(Change.u(), Dependencies.injectValue(node));
	    }

	    return Addresses.inject(selfAddrs, Change.u(), dependencies);
	} else {
	    /* Ignore everything else (e.g., method calls) for now. */
	    return null;
	}
    }

    /**
     * Base case: A simple name in the environment.
     * 
     * @param node
     *            A Name node
     * @return The set of addresses this identifier can resolve to.
     */
    private Set<Address> resolveOrCreateBaseCase(Name node) {

	Set<Address> result = new HashSet<Address>();

	Addresses addrs = state.env.apply(node);
	if (addrs == null) {
	    // TODO: Since we search for global variables beforehand, we should
	    // never reach this.

	    // Assume the variable exists in the environment (ie. not a
	    // TypeError) and add it to the environment/store as BValue.TOP
	    // since we know nothing about it.
	    Address addr = state.trace.makeAddr(node.getID(), "");
	    String name = node.toSource();
	    state.env = state.env.strongUpdate(name,
		    Variable.inject(name, addr, Change.bottom(), Dependencies.bot()));
	    state.store = state.store.alloc(addr,
		    Addresses.dummy(Change.bottom(), Dependencies.injectValue(node)), new Name());
	    addrs = new Addresses(addr);
	}

	result.addAll(addrs.addresses);
	return result;

    }

    /**
     * Recursive case: A property access.
     * 
     * @param ie
     *            A qualified name node.
     * @return The set of addresses this identifier can resolve to.
     */
    private Set<Address> resolveOrCreateProperty(PropertyGet ie) {

	Set<Address> result = new HashSet<Address>();

	/* We do not handle the cases where the rhs is an expression. */
	if (!(ie.getRight() instanceof Name))
	    return result;

	/*
	 * We have a qualified name. Recursively find all the addresses that lhs can
	 * resolve to.
	 */
	Set<Address> lhs = resolveOrCreate(ie.getLeft());

	/* Just in case we couldn't resolve or create the sub-expression. */
	if (lhs == null)
	    return result;

	/*
	 * Lookup the current property at each of these addresses. Ignore type errors
	 * and auto-boxing for now.
	 */
	for (Address valAddr : lhs) {

	    /* Get the value at the address. */
	    BValue val = state.store.apply(valAddr, ie.getLeft());

	    /*
	     * We may need to create a dummy object if 'val' doesn't point to any objects.
	     */
	    if (val.addressAD.addresses.size() == 0) {
		Map<String, Property> ext = new HashMap<String, Property>();
		// Create a dummy value. This will not exist in the output, because the value
		// and variable initialization exists outside the file.
		// AstNode dummyNode = new ObjectLiteral();
		Obj dummy = new Obj(ext, new InternalObjectProperties());
		Address addr = state.trace.makeAddr(ie.getLeft().getID(), "");
		state.store = state.store.alloc(addr, dummy);
		val = val.join(Address.inject(addr, Change.bottom(),
			Dependencies.injectValue(ie.getLeft())));
		state.store = state.store.strongUpdate(valAddr, val, ie.getLeft());
	    }

	    for (Address objAddr : val.addressAD.addresses) {

		/* Get the Obj from the store. */
		Obj obj = state.store.getObj(objAddr);

		/* Look up the property. */
		Address propAddr = obj.apply(ie.getRight().toSource());

		if (propAddr != null) {
		    result.add(propAddr);
		} else {
		    // This property was not found, which means it is either undefined or was
		    // initialized somewhere outside the analysis. Create it and give it the value
		    // BValue.TOP.

		    // Create a new address (BValue) for the property and put it in the store.
		    propAddr = state.trace.makeAddr(ie.getRight().getID(),
			    ie.getRight().toSource());

		    // Create a dummy value. This will not exist in the output, because the value
		    // and variable initialization exists outside the file.
		    // AstNode dummyNode = new NumberLiteral();
		    BValue propVal = Addresses.dummy(Change.bottom(), Dependencies.injectValue(ie));
		    state.store = state.store.alloc(propAddr, propVal, ie);

		    /* Add the property to the external properties of the object. */
		    Map<String, Property> ext = new HashMap<String, Property>(
			    obj.externalProperties);
		    ext.put(ie.getRight().toSource(), new Property(ie.getRight().getID(),
			    ie.getRight().toSource(), propAddr));

		    /*
		     * We need to create a new object so that the previous states are not affected
		     * by this update.
		     */
		    Obj newObj = new Obj(ext, obj.internalProperties);
		    state.store = state.store.strongUpdate(objAddr, newObj);

		    result.add(propAddr);
		}
	    }
	}

	return result;

    }

    /**
     * Recursive case: An element access.
     * 
     * @param eg
     *            An element access node.
     * @return The set of addresses this element can resolve to.
     */
    private Set<Address> resolveOrCreateElementCase(ElementGet eg) {

	Set<Address> result = new HashSet<Address>();

	/*
	 * We have a qualified name. Recursively find all the addresses that lhs can
	 * resolve to.
	 */
	Set<Address> lhs = resolveOrCreate(eg.getTarget());

	/* Just in case we couldn't resolve or create the sub-expression. */
	if (lhs == null)
	    return result;

	/*
	 * Lookup the current property at each of these addresses. Ignore type errors
	 * and auto-boxing for now.
	 */
	for (Address valAddr : lhs) {

	    /* Get the value at the address. */
	    BValue val = state.store.apply(valAddr, eg.getTarget());

	    /*
	     * We may need to create a dummy object if 'val' doesn't point to any objects.
	     */
	    if (val.addressAD.addresses.size() == 0) {
		Map<String, Property> ext = new HashMap<String, Property>();
		Obj dummy = new Obj(ext, new InternalObjectProperties());
		Address addr = state.trace.makeAddr(eg.getID(), "");
		state.store = state.store.alloc(addr, dummy);
		val = val.join(Address.inject(addr, Change.bottom(), Dependencies.bot()));
		state.store = state.store.strongUpdate(valAddr, val, eg.getTarget());
	    }

	    for (Address objAddr : val.addressAD.addresses) {

		/* Get the Obj from the store. */
		Obj obj = state.store.getObj(objAddr);

		/* Look up the property. */
		BValue elementValue = this.eval(eg.getElement());

		String elementString = null;
		if (elementValue.numberAD.le == Num.LatticeElement.VAL) {
		    elementString = elementValue.numberAD.val;
		} else if (elementValue.stringAD.le == Str.LatticeElement.SNOTNUMNORSPLVAL
			|| elementValue.stringAD.le == Str.LatticeElement.SNUMVAL
			|| elementValue.stringAD.le == Str.LatticeElement.SSPLVAL) {
		    elementString = elementValue.stringAD.val;
		} else {
		    elementString = "~unknown~";
		}

		Address propAddr = obj.apply(elementString);

		if (propAddr != null) {
		    result.add(propAddr);
		} else {
		    /*
		     * This property was not found, which means it is either undefined or was
		     * initialized somewhere outside the analysis. Create it and give it the value
		     * BValue.TOP.
		     */

		    /*
		     * Create a new address (BValue) for the property and put it in the store.
		     */
		    propAddr = state.trace.makeAddr(eg.getID(), elementString);

		    BValue propVal = Addresses.dummy(Change.bottom(), Dependencies.injectValue(eg));
		    state.store = state.store.alloc(propAddr, propVal, eg);

		    /* Add the property to the external properties of the object. */
		    Map<String, Property> ext = new HashMap<String, Property>(
			    obj.externalProperties);
		    ext.put(elementString,
			    new Property(eg.getTarget().getID(), elementString, propAddr));

		    /*
		     * We need to create a new object so that the previous states are not affected
		     * by this update.
		     */
		    Obj newObj = new Obj(ext, obj.internalProperties);
		    state.store = state.store.strongUpdate(objAddr, newObj);

		    result.add(propAddr);
		}
	    }

	}

	return result;

    }

    /**
     * Resolve an identifier which we don't currently handle, such as an array
     * access.
     * 
     * @return the address of a new (stopgap) value.
     */
    public Set<Address> resolveOrCreateExpression(AstNode node) {

	/* Resolve the expression to a value. */
	BValue val = this.eval(node);

	/* Place the value on the store */
	Set<Address> addrs = new HashSet<Address>();
	Address addr = state.trace.makeAddr(node.getID(), "");
	state.store = state.store.alloc(addr, val, new Name());
	addrs.add(addr);

	return addrs;

    }

    /**
     * Resolve an identifier to an address in the store.
     * 
     * @param node
     *            The identifier to resolve.
     * @return The set of addresses this identifier can resolve to.
     */
    public Set<Address> resolveOrCreate(AstNode node) {

	/* Base Case: A simple name in the environment. */
	if (node instanceof Name) {
	    return resolveOrCreateBaseCase((Name) node);
	}

	/* Recursive Case: A property access. */
	else if (node instanceof PropertyGet) {
	    return resolveOrCreateProperty((PropertyGet) node);
	}

	/* Recursive Case: An element access. */
	else if (node instanceof ElementGet) {
	    return resolveOrCreateElementCase((ElementGet) node);
	}

	/* This must be an expression, which we need to resolve. */
	else {
	    return resolveOrCreateExpression(node);
	}

    }

    /**
     * @return The return value.
     */
    public BValue resolveReturn() {
	return this.state.scratch.applyReturn();
    }

    /**
     * @return The list of functions pointed to by the value.
     */
    private List<Address> extractFunctions(BValue val, List<Address> functionAddrs,
	    Set<Address> visited) {

	for (Address objAddr : val.addressAD.addresses) {
	    Obj obj = state.store.getObj(objAddr);

	    if (obj.internalProperties.klass == JSClass.CFunction) {
		InternalFunctionProperties ifp = (InternalFunctionProperties) obj.internalProperties;
		if (ifp.closure instanceof FunctionClosure) {
		    functionAddrs.add(objAddr);
		}
	    }

	    /* Recursively look for object properties that are functions. */
	    for (Property property : obj.externalProperties.values()) {

		/* Avoid circular references. */
		if (visited.contains(property.address))
		    continue;
		visited.add(property.address);

		extractFunctions(state.store.apply(property.address, new Name()), functionAddrs,
			visited);
	    }

	}

	return functionAddrs;

    }

}