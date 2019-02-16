package multidiffplus.jsanalysis.transfer;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.UnaryExpression;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.Addresses;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Bool;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Control;
import multidiffplus.jsanalysis.abstractdomain.DefinerIDs;
import multidiffplus.jsanalysis.abstractdomain.JSClass;
import multidiffplus.jsanalysis.abstractdomain.Null;
import multidiffplus.jsanalysis.abstractdomain.Num;
import multidiffplus.jsanalysis.abstractdomain.Obj;
import multidiffplus.jsanalysis.abstractdomain.Scratchpad;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Str;
import multidiffplus.jsanalysis.abstractdomain.Undefined;
import multidiffplus.jsanalysis.abstractdomain.Variable;

public class ExpEvalImmutable {

    private State state;

    /**
     * Only use when functions are not called (ie. by CFG visitors).
     */
    public ExpEvalImmutable(State state) {
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
	}

	/* We could not evaluate the expression. Return top. */
	return BValue.top(Change.convU(node));

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
	    return BValue.top(Change.u());
	return val;
    }

    /**
     * Resolves the address of the function literal.
     * 
     * @param f
     *            The function definition.
     * @return A BValue that points to the new function object.
     */
    public BValue evalFunctionNode(FunctionNode f) {
	Address addr = state.trace.makeAddr(f.getID(), "");
	addr = state.trace.modAddr(addr, JSClass.CFunction);
	return Address.inject(addr, Change.convU(f), DefinerIDs.inject(f.getID()));
    }

    /**
     * Resolves the address of the object literal.
     * 
     * @param ol
     *            The object literal.
     * @return A BValue that points to the new object literal.
     */
    public BValue evalObjectLiteral(ObjectLiteral ol) {
	Address objAddr = state.trace.makeAddr(ol.getID(), "");
	return Address.inject(objAddr, Change.convU(ol), DefinerIDs.inject(ol.getID()));
    }

    /**
     * Resolves the address of the array literal.
     * 
     * @param al
     *            The array literal.
     * @return A BValue that points to the new array literal.
     */
    public BValue evalArrayLiteral(ArrayLiteral al) {
	Address objAddr = state.trace.makeAddr(al.getID(), "");
	return Address.inject(objAddr, Change.convU(al), DefinerIDs.inject(al.getID()));
    }

    /**
     * Resolves the value of the unary expression.
     * 
     * @param ue
     *            The unary expression.
     * @return the abstract interpretation of the expression.
     */
    public BValue evalUnaryExpression(UnaryExpression ue) {

	BValue operand = this.eval(ue.getOperand());

	// First create a bottom BValue with the proper change type. We will put
	// in values later.
	BValue val;
	if (operand.change.le == Change.LatticeElement.CHANGED
		|| operand.change.le == Change.LatticeElement.TOP) {
	    val = BValue.bottom(Change.c());
	} else if (ue.getChangeType() == ChangeType.INSERTED
		|| ue.getChangeType() == ChangeType.REMOVED
		|| ue.getChangeType() == ChangeType.UPDATED) {
	    val = BValue.bottom(Change.c());
	} else {
	    val = BValue.bottom(Change.u());
	}

	// Over-approximate the evaluation of the unary operator.
	switch (ue.getType()) {
	case Token.NOT:
	    val.booleanAD.le = Bool.LatticeElement.TOP;
	    return val;
	case Token.INC:
	case Token.DEC:
	    val.numberAD.le = Num.LatticeElement.TOP;
	    return val;
	case Token.TYPEOF:
	    val.stringAD.le = Str.LatticeElement.TOP;
	    return val;
	default:
	    val.undefinedAD.le = Undefined.LatticeElement.TOP;
	    return val;
	}

    }

    /**
     * Evaluate the value of the infix expression.
     * 
     * @param ie
     *            The infix expression.
     * @return the abstract interpretation of the name
     */
    public BValue evalInfixExpression(InfixExpression ie) {

	/* If this is an assignment, we need to interpret it through state. */
	switch (ie.getType()) {
	case Token.ASSIGN:
	    return this.eval(ie.getLeft());
	case Token.GETPROPNOWARN:
	case Token.GETPROP: {
	    /* This is an identifier.. so we attempt to dereference it. */
	    BValue val = resolveValue(ie);
	    if (val == null)
		return BValue.top(Change.u());
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
	    return this.eval(ie.getLeft());
	}
	case Token.ASSIGN_SUB:
	case Token.ASSIGN_MUL:
	case Token.ASSIGN_DIV:
	case Token.ASSIGN_MOD: {
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

	/*
	 * First create a bottom BValue with the proper change type. We will put in
	 * values later.
	 */
	BValue val;
	if (left.change.le == Change.LatticeElement.CHANGED
		|| left.change.le == Change.LatticeElement.TOP
		|| right.change.le == Change.LatticeElement.CHANGED
		|| right.change.le == Change.LatticeElement.TOP) {
	    val = BValue.top(Change.c());
	} else if (ie.getChangeType() == ChangeType.INSERTED
		|| ie.getChangeType() == ChangeType.REMOVED
		|| ie.getChangeType() == ChangeType.UPDATED) {
	    val = BValue.top(Change.c());
	} else {
	    val = BValue.top(Change.u());
	}

	return val;

    }

    /**
     * Evaluates the plus operation on two BValues.
     */
    public BValue evalPlus(InfixExpression ie) {

	BValue left = this.eval(ie.getLeft());
	BValue right = this.eval(ie.getRight());

	/*
	 * First create a bottom BValue with the proper change type. We will put in
	 * values later.
	 */
	BValue plus;
	if (left.change.le == Change.LatticeElement.CHANGED
		|| left.change.le == Change.LatticeElement.TOP
		|| right.change.le == Change.LatticeElement.CHANGED
		|| right.change.le == Change.LatticeElement.TOP) {
	    plus = BValue.bottom(Change.c());
	} else if (ie.getChangeType() == ChangeType.INSERTED
		|| ie.getChangeType() == ChangeType.REMOVED
		|| ie.getChangeType() == ChangeType.UPDATED) {
	    plus = BValue.top(Change.c());
	} else {
	    plus = BValue.bottom(Change.u());
	}

	/* Assign a definer ID to track this new value. */
	ie.setDummy();
	plus.definerIDs = plus.definerIDs.join(DefinerIDs.inject(ie.getID()));

	/*
	 * For now, just do a basic conservative estimate of binary operator
	 * evaluations.
	 */

	/* Strings. */
	if (left.stringAD.le != Str.LatticeElement.BOTTOM
		|| right.stringAD.le != Str.LatticeElement.BOTTOM) {
	    plus.stringAD.le = Str.LatticeElement.TOP;
	}
	/* Numbers. */
	if (left.numberAD.le != Num.LatticeElement.BOTTOM
		|| right.numberAD.le != Num.LatticeElement.BOTTOM) {
	    plus.numberAD.le = Num.LatticeElement.TOP;
	}
	/* Booleans and Nulls. */
	if ((left.booleanAD.le != Bool.LatticeElement.BOTTOM
		|| left.nullAD.le == Null.LatticeElement.TOP)
		&& (right.booleanAD.le != Bool.LatticeElement.BOTTOM
			|| right.nullAD.le == Null.LatticeElement.TOP)) {
	    plus.numberAD.le = Num.LatticeElement.TOP;
	}

	return plus;

    }

    /**
     * Evaluates a math operation on two BValues.
     */
    public BValue evalMathOp(InfixExpression ie) {

	BValue left = this.eval(ie.getLeft());
	BValue right = this.eval(ie.getRight());

	/*
	 * First create a bottom BValue with the proper change type. We will put in
	 * values later.
	 */
	BValue val;
	if (left.change.le == Change.LatticeElement.CHANGED
		|| left.change.le == Change.LatticeElement.TOP
		|| right.change.le == Change.LatticeElement.CHANGED
		|| right.change.le == Change.LatticeElement.TOP) {
	    val = BValue.bottom(Change.c());
	} else if (ie.getChangeType() == ChangeType.INSERTED
		|| ie.getChangeType() == ChangeType.REMOVED
		|| ie.getChangeType() == ChangeType.UPDATED) {
	    val = BValue.top(Change.c());
	} else {
	    val = BValue.bottom(Change.u());
	}

	/*
	 * For now, just do a basic conservative estimate of binary operator
	 * evaluations.
	 */

	val.numberAD.le = Num.LatticeElement.TOP;

	return val;

    }

    /**
     * @param name
     * @return the abstract interpretation of the name
     */
    public BValue evalName(Name name) {
	BValue val = resolveValue(name);
	if (val == null)
	    return BValue.top(Change.u());
	return val;
    }

    /**
     * @param numl
     * @return the abstract interpretation of the number literal
     */
    public BValue evalNumberLiteral(NumberLiteral numl) {
	return Num.inject(new Num(Num.LatticeElement.VAL, numl.getValue()), Change.convU(numl),
		DefinerIDs.inject(numl.getID()));
    }

    /**
     * @param strl
     *            The keyword literal.
     * @return the abstract interpretation of the string literal
     */
    public BValue evalStringLiteral(StringLiteral strl) {

	Str str = null;
	String val = strl.getValue();
	Change change = Change.convU(strl);
	if (val.equals(""))
	    str = new Str(Str.LatticeElement.SBLANK);
	else if (NumberUtils.isCreatable(val)) {
	    str = new Str(Str.LatticeElement.SNUMVAL, val);
	} else {
	    str = new Str(Str.LatticeElement.SNOTNUMNORSPLVAL, val);
	}

	return Str.inject(str, change, DefinerIDs.inject(strl.getID()));

    }

    /**
     * @param kwl
     *            The keyword literal.
     * @return the abstract interpretation of the keyword literal.
     */
    public BValue evalKeywordLiteral(KeywordLiteral kwl) {
	Change change = Change.conv(kwl);
	switch (kwl.getType()) {
	case Token.THIS:
	    return state.store.apply(state.selfAddr);
	case Token.NULL:
	    return Null.inject(Null.top(), change, DefinerIDs.inject(kwl.getID()));
	case Token.TRUE:
	    return Bool.inject(new Bool(Bool.LatticeElement.TRUE), change,
		    DefinerIDs.inject(kwl.getID()));
	case Token.FALSE:
	    return Bool.inject(new Bool(Bool.LatticeElement.FALSE), change,
		    DefinerIDs.inject(kwl.getID()));
	case Token.DEBUGGER:
	default:
	    return BValue.bottom(change);
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

	/* Create the argument values. */
	BValue[] args = new BValue[fc.getArguments().size()];
	int i = 0;
	for (AstNode arg : fc.getArguments()) {

	    /* Get the value of the object. It could be a function, object literal, etc. */
	    BValue argVal = eval(arg);

	    if (arg instanceof ObjectLiteral) {
		/*
		 * If this is an object literal, make a fake var in the environment and point it
		 * to the object literal.
		 */
		Address address = state.trace.makeAddr(arg.getID(), "");
		state.env.strongUpdateNoCopy(arg.getID().toString(),
			new Variable(arg.getID(), arg.getID().toString(), new Addresses(address)));
		state.store = state.store.alloc(address, argVal);
	    }

	    args[i] = argVal;
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

	if (funVal != null) {

	    /* Update the control-call domain for the function call. */
	    Control control = state.control;
	    control = control.update(fc);

	    /* Call the function and get a join of the new states. */
	    newState = Helpers.applyClosure(funVal, objAddr, state.store, scratch, state.trace,
		    control, null);
	}

	BValue retVal = BValue.top(Change.convU(fc));

	if (newState != null) {
	    retVal = newState.scratch.applyReturn();
	    if (retVal == null) {
		/* Functions with no return statement return undefined. */
		retVal = Undefined.inject(Undefined.top(), Change.u(), DefinerIDs.bottom());
	    }
	}

	return retVal;

    }

    /**
     * Resolves a variable to its addresses in the store.
     * 
     * @return The addresses as a BValue.
     */
    public BValue resolveValue(AstNode node) {

	BValue value = null;

	/* Resolve the identifier. */
	Set<Address> addrs = this.resolve(node);
	if (addrs == null)
	    return null;
	for (Address addr : addrs) {
	    if (value == null)
		value = state.store.apply(addr);
	    else
		value = value.join(state.store.apply(addr));
	}

	return value;

    }

    /**
     * @return The {@code BValue} direct from the store.
     */
    public BValue resolveAddress(Address address) {
	return state.store.apply(address);
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
	    Set<Address> addrs = this.resolve(ie.getLeft());
	    if (addrs == null)
		return null;

	    /*
	     * Resolve all the objects in the address list and create a new BValue which
	     * points to those objects.
	     */
	    Addresses selfAddrs = new Addresses(Addresses.LatticeElement.SET);
	    DefinerIDs definerIDs = DefinerIDs.bottom();
	    for (Address addr : addrs) {
		BValue val = this.state.store.apply(addr);
		for (Address objAddr : val.addressAD.addresses) {
		    Obj obj = this.state.store.getObj(objAddr);
		    if (obj != null) {
			selfAddrs.addresses.add(objAddr);
			definerIDs = definerIDs.join(val.definerIDs);
		    }
		}
	    }
	    if (selfAddrs.addresses.isEmpty())
		return BValue.bottom(Change.u());

	    return Addresses.inject(selfAddrs, Change.u(), definerIDs);
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
    private Set<Address> resolveBaseCase(Name node) {
	Set<Address> result = new HashSet<Address>();
	Addresses addrs = state.env.apply(node.toSource());
	if (addrs != null)
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
    private Set<Address> resolveProperty(PropertyGet ie) {

	Set<Address> result = new HashSet<Address>();

	/* We do not handle the cases where the rhs is an expression. */
	if (!(ie.getRight() instanceof Name))
	    return result;

	/*
	 * We have a qualified name. Recursively find all the addresses that lhs can
	 * resolve to.
	 */
	Set<Address> lhs = resolve(ie.getLeft());

	/* Just in case we couldn't resolve or create the sub-expression. */
	if (lhs == null)
	    return result;

	/*
	 * Lookup the current property at each of these addresses. Ignore type errors
	 * and auto-boxing for now.
	 */
	for (Address valAddr : lhs) {

	    /* Get the value at the address. */
	    BValue val = state.store.apply(valAddr);

	    for (Address objAddr : val.addressAD.addresses) {

		/* Get the Obj from the store. */
		Obj obj = state.store.getObj(objAddr);

		/* Look up the property. */
		Address propAddr = obj.apply(ie.getRight().toSource());

		if (propAddr != null) {
		    result.add(propAddr);
		    // Sanity check that the property address is in the store.
		    BValue propVal = state.store.apply(propAddr);
		    if (propVal == null)
			throw new Error("Property value does not exist in store.");
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
    private Set<Address> resolveElementCase(ElementGet eg) {

	Set<Address> result = new HashSet<Address>();

	/*
	 * We have a qualified name. Recursively find all the addresses that lhs can
	 * resolve to.
	 */
	Set<Address> lhs = resolve(eg.getTarget());

	/* Just in case we couldn't resolve or create the sub-expression. */
	if (lhs == null)
	    return result;

	/*
	 * Lookup the current property at each of these addresses. Ignore type errors
	 * and auto-boxing for now.
	 */
	for (Address valAddr : lhs) {

	    /* Get the value at the address. */
	    BValue val = state.store.apply(valAddr);

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
		    // Sanity check that the property address is in the store.
		    BValue propVal = state.store.apply(propAddr);
		    if (propVal == null)
			throw new Error("Property value does not exist in store.");
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
    public Set<Address> resolveExpression(AstNode node) {
	Set<Address> addrs = new HashSet<Address>();
	Address addr = state.trace.makeAddr(node.getID(), "");
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
    public Set<Address> resolve(AstNode node) {

	/* Base Case: A simple name in the environment. */
	if (node instanceof Name) {
	    return resolveBaseCase((Name) node);
	}

	/* Recursive Case: A property access. */
	else if (node instanceof PropertyGet) {
	    return resolveProperty((PropertyGet) node);
	}

	/* Recursive Case: An element access. */
	else if (node instanceof ElementGet) {
	    return resolveElementCase((ElementGet) node);
	}

	/* This must be an expression, which we need to resolve. */
	else {
	    return resolveExpression(node);
	}

    }

    /**
     * @return The return value.
     */
    public BValue resolveReturn() {
	return this.state.scratch.applyReturn();
    }

}