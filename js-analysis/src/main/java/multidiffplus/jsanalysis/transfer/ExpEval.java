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
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.UnaryExpression;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.Addresses;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Closure;
import multidiffplus.jsanalysis.abstractdomain.Control;
import multidiffplus.jsanalysis.abstractdomain.DefinerIDs;
import multidiffplus.jsanalysis.abstractdomain.FunctionClosure;
import multidiffplus.jsanalysis.abstractdomain.InternalFunctionProperties;
import multidiffplus.jsanalysis.abstractdomain.InternalObjectProperties;
import multidiffplus.jsanalysis.abstractdomain.JSClass;
import multidiffplus.jsanalysis.abstractdomain.Obj;
import multidiffplus.jsanalysis.abstractdomain.Property;
import multidiffplus.jsanalysis.abstractdomain.Scratchpad;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Bool;
import multidiffplus.jsanalysis.abstractdomain.Num;
import multidiffplus.jsanalysis.abstractdomain.Str;
import multidiffplus.jsanalysis.abstractdomain.Undefined;
import multidiffplus.jsanalysis.abstractdomain.Variable;
import multidiffplus.jsanalysis.flow.Analysis;
import multidiffplus.jsanalysis.abstractdomain.Null;

public class ExpEval {
	
	private Analysis analysis;
	private State state;

	/**
	 * Use inside transfer functions.
	 */
	public ExpEval(Analysis analysis, State state) {
		this.analysis = analysis;
		this.state = state;
	}

	/**
	 * Only use when functions are not called (ie. by CFG visitors).
	 */
	public ExpEval(State state) {
		this.analysis = null;
		this.state = state;
	}

	/**
	 * Evaluate an expression to a BValue.
	 * @param node The expression to evaluate.
	 * @return The value of the expression.
	 */
	public BValue eval(AstNode node) {

		if(node instanceof Name) {
			return evalName((Name)node);
		}
		else if(node instanceof InfixExpression) {
			return evalInfixExpression((InfixExpression)node);
		}
		else if(node instanceof ElementGet) {
			return evalElementGet((ElementGet)node);
		}
		else if(node instanceof UnaryExpression) {
			return evalUnaryExpression((UnaryExpression)node);
		}
		else if(node instanceof KeywordLiteral) {
			return evalKeywordLiteral((KeywordLiteral)node);
		}
		else if(node instanceof StringLiteral) {
			return evalStringLiteral((StringLiteral)node);
		}
		else if(node instanceof NumberLiteral) {
			return evalNumberLiteral((NumberLiteral)node);
		}
		else if(node instanceof ObjectLiteral) {
			return evalObjectLiteral((ObjectLiteral)node);
		}
		else if(node instanceof ArrayLiteral) {
			return evalArrayLiteral((ArrayLiteral)node);
		}
		else if(node instanceof FunctionNode) {
			return evalFunctionNode((FunctionNode)node);
		}
		else if(node instanceof FunctionCall) {
			return evalFunctionCall((FunctionCall) node);
		}

		/* We could not evaluate the expression. Return top. */
		return BValue.top(Change.convU(node));

	}

	/**
	 * @param eg The element get expression.
	 * @return the value of the element.
	 */
	private BValue evalElementGet(ElementGet eg) {
		/* This is an identifier.. so we attempt to dereference it. */
		BValue val = resolveValue(eg);
		if(val == null) return BValue.top(Change.u());
		return val; 
	}

	/**
	 * Creates a new function from a function definition.
	 * @param f The function definition.
	 * @return A BValue that points to the new function object.
	 */
	public BValue evalFunctionNode(FunctionNode f){
		Closure closure = new FunctionClosure(analysis.cfgs.get(f), state.env);
		Address addr = state.trace.makeAddr(f.getID(), "");
		addr = state.trace.modAddr(addr, JSClass.CFunction);
		state.store = Helpers.createFunctionObj(closure, state.store, state.trace, addr, f);
		return Address.inject(addr, Change.convU(f), DefinerIDs.inject(f.getID()));
	}

	/**
	 * Creates a new object from an object literal.
	 * @param ol The object literal.
	 * @return A BValue that points to the new object literal.
	 */
	public BValue evalObjectLiteral(ObjectLiteral ol) {
		Map<String, Property> ext = new HashMap<String, Property>();
		InternalObjectProperties in = new InternalObjectProperties();

		for(ObjectProperty property : ol.getElements()) {
			AstNode prop = property.getLeft();
			String propName = "~unknown~";
			if(prop instanceof Name) propName = prop.toSource();
			else if(prop instanceof StringLiteral) propName = ((StringLiteral)prop).getValue();
			else if(prop instanceof NumberLiteral) propName = ((NumberLiteral)prop).getValue();
			BValue propVal = this.eval(property.getRight());
			Address propAddr = state.trace.makeAddr(property.getID(), propName);
			state.store = state.store.alloc(propAddr, propVal);
			if(propName != null) ext.put(propName, new Property(property.getID(), propName, propAddr));
		}

		Obj obj = new Obj(ext, in);
		Address objAddr = state.trace.makeAddr(ol.getID(), "");
		state.store = state.store.alloc(objAddr, obj);

		return Address.inject(objAddr, Change.convU(ol), DefinerIDs.inject(ol.getID()));
	}
	
	/**
	 * Creates a new array from an array literal.
	 * @param al The array literal.
	 * @return A BValue that points to the new array literal.
	 */
	public BValue evalArrayLiteral(ArrayLiteral al) {

		Map<String, Property> ext = new HashMap<String, Property>();
		InternalObjectProperties in = new InternalObjectProperties();

		Integer i = 0;
		for(AstNode element : al.getElements()) {
			BValue propVal = this.eval(element);
			Address propAddr = state.trace.makeAddr(element.getID(), "");
			state.store = state.store.alloc(propAddr, propVal);
			ext.put(i.toString(), new Property(element.getID(), i.toString(), propAddr));
			i++;
		}

		Obj obj = new Obj(ext, in);
		Address objAddr = state.trace.makeAddr(al.getID(), "");
		state.store = state.store.alloc(objAddr, obj);

		return Address.inject(objAddr, Change.convU(al), DefinerIDs.inject(al.getID()));
		
	}

	/**
	 * @param ue The unary expression.
	 * @return the abstract interpretation of the expression.
	 */
	public BValue evalUnaryExpression(UnaryExpression ue) {

		BValue operand = this.eval(ue.getOperand());

		/* First create a bottom BValue with the proper change type. We will
		 * put in values later. */
		BValue val;
		if(operand.change.le == Change.LatticeElement.CHANGED
				|| operand.change.le == Change.LatticeElement.TOP) {
			val = BValue.bottom(Change.c());
		}
		else if(ue.getChangeType() == ChangeType.INSERTED
				|| ue.getChangeType() == ChangeType.REMOVED
				|| ue.getChangeType() == ChangeType.UPDATED) {
			val = BValue.bottom(Change.c());
		}
		else {
			val = BValue.bottom(Change.u());
		}

		/* For now, just do a basic conservative estimate of unary operator
		 * evaluations. */

		switch(ue.getType()) {
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
	 * @param ie The infix expression.
	 * @return the abstract interpretation of the name
	 */
	public BValue evalInfixExpression(InfixExpression ie) {

		/* If this is an assignment, we need to interpret it through state. */
		switch(ie.getType()) {
		case Token.ASSIGN:
			/* We need to interpret this assignment and propagate the value
			 * left. */
			this.evalAssignment((Assignment)ie);
			return this.eval(ie.getLeft());
		case Token.GETPROPNOWARN:
		case Token.GETPROP: {
			/* This is an identifier.. so we attempt to dereference it. */
			BValue val = resolveValue(ie);
			if(val == null) return BValue.top(Change.u());
			return val; }
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
			return this.eval(ie.getLeft()); }
		case Token.ASSIGN_SUB:
		case Token.ASSIGN_MUL:
		case Token.ASSIGN_DIV:
		case Token.ASSIGN_MOD: {
			BValue val = evalMathOp(ie);
			this.evalAssignment(ie.getLeft(), val);
			return this.eval(ie.getLeft()); }
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

		/* First create a bottom BValue with the proper change type. We will
		 * put in values later. */
		BValue val;
		if(left.change.le == Change.LatticeElement.CHANGED
				|| left.change.le == Change.LatticeElement.TOP
				|| right.change.le == Change.LatticeElement.CHANGED
				|| right.change.le == Change.LatticeElement.TOP) {
			val = BValue.top(Change.c());
		}
		else if(ie.getChangeType() == ChangeType.INSERTED
				|| ie.getChangeType() == ChangeType.REMOVED
				|| ie.getChangeType() == ChangeType.UPDATED) {
			val = BValue.top(Change.c());
		}
		else {
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

		/* First create a bottom BValue with the proper change type. We will
		 * put in values later. */
		BValue plus;
		if(left.change.le == Change.LatticeElement.CHANGED
				|| left.change.le == Change.LatticeElement.TOP
				|| right.change.le == Change.LatticeElement.CHANGED
				|| right.change.le == Change.LatticeElement.TOP) {
			plus = BValue.bottom(Change.c());
		}
		else if(ie.getChangeType() == ChangeType.INSERTED
				|| ie.getChangeType() == ChangeType.REMOVED
				|| ie.getChangeType() == ChangeType.UPDATED) {
			plus = BValue.top(Change.c());
		}
		else {
			plus = BValue.bottom(Change.u());
		}

		/* Assign a definer ID to track this new value. */
		ie.setDummy();
		plus.definerIDs = plus.definerIDs.join(DefinerIDs.inject(ie.getID()));

		/* For now, just do a basic conservative estimate of binary operator
		 * evaluations. */

		/* Strings. */
		if(left.stringAD.le != Str.LatticeElement.BOTTOM
				|| right.stringAD.le != Str.LatticeElement.BOTTOM) {
				plus.stringAD.le = Str.LatticeElement.TOP;
		}
		/* Numbers. */
		if(left.numberAD.le != Num.LatticeElement.BOTTOM
				|| right.numberAD.le != Num.LatticeElement.BOTTOM) {
			plus.numberAD.le = Num.LatticeElement.TOP;
		}
		/* Booleans and Nulls. */
		if((left.booleanAD.le != Bool.LatticeElement.BOTTOM
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

		/* First create a bottom BValue with the proper change type. We will
		 * put in values later. */
		BValue val;
		if(left.change.le == Change.LatticeElement.CHANGED
				|| left.change.le == Change.LatticeElement.TOP
				|| right.change.le == Change.LatticeElement.CHANGED
				|| right.change.le == Change.LatticeElement.TOP) {
			val = BValue.bottom(Change.c());
		}
		else if(ie.getChangeType() == ChangeType.INSERTED
				|| ie.getChangeType() == ChangeType.REMOVED
				|| ie.getChangeType() == ChangeType.UPDATED) {
			val = BValue.top(Change.c());
		}
		else {
			val = BValue.bottom(Change.u());
		}

		/* For now, just do a basic conservative estimate of binary operator
		 * evaluations. */

		val.numberAD.le = Num.LatticeElement.TOP;

		return val;

	}

	/**
	 * @param name
	 * @return the abstract interpretation of the name
	 */
	public BValue evalName(Name name) {
		BValue val = resolveValue(name);
		if(val == null) return BValue.top(Change.u());
		return val;
	}

	/**
	 * @param numl
	 * @return the abstract interpretation of the number literal
	 */
	public BValue evalNumberLiteral(NumberLiteral numl) {
		return Num.inject(new Num(Num.LatticeElement.VAL, numl.getValue()), Change.convU(numl), DefinerIDs.inject(numl.getID()));
	}

	/**
	 * @param strl The keyword literal.
	 * @return the abstract interpretation of the string literal
	 */
	public BValue evalStringLiteral(StringLiteral strl) {

		Str str = null;
		String val = strl.getValue();
		Change change = Change.convU(strl);
		if(val.equals("")) str = new Str(Str.LatticeElement.SBLANK);
		else if(NumberUtils.isCreatable(val)) {
			str = new Str(Str.LatticeElement.SNUMVAL, val);
		}
		else {
			str = new Str(Str.LatticeElement.SNOTNUMNORSPLVAL, val);
		}

		return Str.inject(str, change, DefinerIDs.inject(strl.getID()));

	}

	/**
	 * @param kwl The keyword literal.
	 * @return the abstract interpretation of the keyword literal.
	 */
	public BValue evalKeywordLiteral(KeywordLiteral kwl) {
		Change change = Change.conv(kwl);
		switch(kwl.getType()) {
		case Token.THIS:
			return state.store.apply(state.selfAddr);
		case Token.NULL:
			return Null.inject(Null.top(), change, DefinerIDs.inject(kwl.getID()));
		case Token.TRUE:
			return Bool.inject(new Bool(Bool.LatticeElement.TRUE), change, DefinerIDs.inject(kwl.getID()));
		case Token.FALSE:
			return Bool.inject(new Bool(Bool.LatticeElement.FALSE), change, DefinerIDs.inject(kwl.getID()));
		case Token.DEBUGGER:
		default:
			return BValue.bottom(change);
		}
	}

	/**
	 * Updates the store based on abstract interpretation of assignments.
	 * @param a The assignment.
	 */
	public void evalAssignment(Assignment a) {
		evalAssignment(a.getLeft(), a.getRight());
	}

	/**
	 * Helper function since variable initializers and assignments do the same thing.
	 */
	public void evalAssignment(AstNode lhs, AstNode rhs) {

		/* Resolve the left hand side to a set of addresses. */
		Set<Address> addrs = this.resolveOrCreate(lhs);

		/* Resolve the right hand side to a value. */
		BValue val = this.eval(rhs);
		
		/* CPSC513: If the value is a changed integer, verify with symex. */
		if(val.numberAD.le != Num.LatticeElement.BOTTOM
				&& (val.change.le == Change.LatticeElement.CHANGED 
				|| val.change.le == Change.LatticeElement.TOP)) {
			// TODO: Create a backwards slice starting from here, for both versions.
			// verify(String Vo, String Vn, Array INITo, Array INITn, Array CONSTRAINT, ASSERTION)
			
			// PROBLEM: We don't execute the programs together... maybe we should start a new project
			// for this? Probably. This is getting a little out of control. Maybe we do a simplified
			// analysis that only handles integers and arrays or something.
		}

		/* Conservatively add a dummy DefinerID to the BValue if there are currently
		 * no DefinerIDs */
		if(val.definerIDs.isEmpty()) {
			val.definerIDs = val.definerIDs.strongUpdate(rhs.getID());
			rhs.setDummy();
		}

		/* Update the values in the store. */
		// TODO: Is this correct? We should probably only do a strong update if
		//		 there is only one address. Otherwise we don't know which one
		//		 to update.
		for(Address addr : addrs) {
			state.store = state.store.strongUpdate(addr, val);
		}

	}

	/**
	 * Helper function since variable initializers and assignments do the same thing.
	 */
	public void evalAssignment(AstNode lhs, BValue val) {

		/* Resolve the left hand side to a set of addresses. */
		Set<Address> addrs = this.resolveOrCreate(lhs);

		/* Update the values in the store. */
		// TODO: Is this correct? We should probably only do a strong update if
		//		 there is only one address. Otherwise we don't know which one
		//		 to update.
		for(Address addr : addrs) {
			state.store = state.store.strongUpdate(addr, val);
		}

	}

	/**
	 * @return The list of functions pointed to by the value.
	 */
	private List<Address> extractFunctions(BValue val, List<Address> functionAddrs, Set<Address> visited) {

		for(Address objAddr : val.addressAD.addresses) {
			Obj obj = state.store.getObj(objAddr);

			if(obj.internalProperties.klass == JSClass.CFunction) {
				InternalFunctionProperties ifp = (InternalFunctionProperties) obj.internalProperties;
				if(ifp.closure instanceof FunctionClosure) {
					functionAddrs.add(objAddr);
				}
			}

			/* Recursively look for object properties that are functions. */
			for(Property property : obj.externalProperties.values()) {
				
				/* Avoid circular references. */
				if(visited.contains(property.address)) continue;
				visited.add(property.address);

				extractFunctions(state.store.apply(property.address), functionAddrs, visited);
			}

		}

		return functionAddrs;

	}

	/**
	 * Evaluate a function call expression to a BValue.
	 * @param fc The function call.
	 * @return The return value of the function call.
	 */
	public BValue evalFunctionCall(FunctionCall fc) {
		
		/* The state after the function call. */
		State newState = null;

		/* Keep track of callback functions. */
		List<Address> callbacks = new LinkedList<Address>();

		/* Create the argument values. */
		BValue[] args = new BValue[fc.getArguments().size()];
		int i = 0;
		for(AstNode arg : fc.getArguments()) {

			/* Get the value of the object. It could be a function, object literal, etc. */
			BValue argVal = eval(arg);

			if(arg instanceof ObjectLiteral) {
				/* If this is an object literal, make a fake var in the
				 * environment and point it to the object literal. */
				Address address = state.trace.makeAddr(arg.getID(), "");
				state.env.strongUpdateNoCopy(arg.getID().toString(), new Variable(arg.getID(), arg.getID().toString(), new Addresses(address)));
				state.store = state.store.alloc(address, argVal);
			}
			
			args[i] = argVal;
			
			/* Arguments of bind, call or apply are not callbacks. */
			AstNode target = fc.getTarget();
			if(!(target instanceof PropertyGet && ((PropertyGet)target).getRight().toSource().equals("bind")))
				callbacks.addAll(extractFunctions(argVal, new LinkedList<Address>(), new HashSet<Address>()));

			i++;

		}
		
		Scratchpad scratch = new Scratchpad(null, args);

		/* Attempt to resolve the function and its parent object. */
		BValue funVal = resolveValue(fc.getTarget());
		BValue objVal = resolveSelf(fc.getTarget());

		/* If the function is not a member variable, it is local and we
		 * use the object of the currently executing function as self. */
		Address objAddr = state.trace.toAddr("this");
		if(objVal == null) objAddr = state.selfAddr;
		else state.store = state.store.alloc(objAddr, objVal);


		if(funVal != null) {

			/* Update the control-call domain for the function call. */
			Control control = state.control;
			control = control.update(fc);

			/* Call the function and get a join of the new states. */
			newState = Helpers.applyClosure(funVal, objAddr, state.store,
												  scratch, state.trace, control, 
												  analysis);
		}

		/* Get the call change type. */
		boolean callChange =
				Change.convU(fc).le == Change.LatticeElement.CHANGED
					|| Change.convU(fc).le == Change.LatticeElement.TOP
				? true : false;

		if(newState == null) {
			/* Because our analysis is not complete, the identifier may not point
			 * to any function object. In this case, we assume the (local) state
			 * is unchanged, but add BValue.TOP as the return value. */
			BValue value = callChange
					? BValue.top(Change.top())
					: BValue.top(Change.u());
			state.scratch = state.scratch.strongUpdate(value, null);
			newState = new State(state.store, state.env, state.scratch,
								 state.trace, state.control, state.selfAddr);
			
			/* Create the return value. */
			BValue retVal =  BValue.top(Change.convU(fc));
			
			newState.scratch = newState.scratch.strongUpdate(retVal, null);
		}
		else {

			BValue retVal =  newState.scratch.applyReturn();
			if(retVal == null) {
				/* Functions with no return statement return undefined. */
				retVal = Undefined.inject(Undefined.top(), Change.u(), DefinerIDs.bottom());
				newState.scratch = newState.scratch.strongUpdate(retVal, null);
			}

			/* This could be a new value if the call is new. */
			if(callChange) {
				newState.scratch.applyReturn().change = Change.top();
			}

		}

		/* Analyze any callbacks */
//		for(Address addr : callbacks) {
//			Obj funct = newState.store.getObj(addr);
//
//			InternalFunctionProperties ifp = (InternalFunctionProperties)funct.internalProperties;
//
//			/* Create the argument object. */
//			scratch = new Scratchpad(null, new BValue[0]);
//
//			/* Create the control domain. */
//			Control control = new Control();
//
//			/* Is this function being called recursively? If so abort. */
//			if(state.callStack.contains(addr)) continue;
//
//			/* Push this function onto the call stack. */
//			state.callStack.push(addr);
//
//			/* Analyze the function. */
//			ifp.closure.run(state.selfAddr, state.store,
//							scratch, state.trace, control,
//							state.callStack);
//
//			/* Pop this function off the call stack. */
//			state.callStack.pop();
//
//		}

		this.state.store = newState.store;
		return newState.scratch.applyReturn();

	}

	/**
	 * Resolves a variable to its addresses in the store.
	 * @return The addresses as a BValue.
	 */
	public BValue resolveValue(AstNode node) {

		BValue value = null;
		
		/* Resolve the identifier. */
		Set<Address> addrs = this.resolveOrCreate(node);
		if(addrs == null) return null;
		for(Address addr : addrs) {
			if(value == null) value = state.store.apply(addr);
			else value = value.join(state.store.apply(addr));
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
	 * Apply a strong update directly to the store using an address.
	 */
	public void updateAddress(Address address, BValue value) {
		state.store.strongUpdate(address, value);
	}

	/**
	 * Resolves a function's parent object.
	 * @return The parent object (this) and the function object.
	 */
	public BValue resolveSelf(AstNode node) {
		if(node instanceof Name) {
			/* This is a variable name, not a field. */
			return null;
		}
		else if(node instanceof InfixExpression) {
			/* We have a qualified name. Recursively find the addresses. */
			InfixExpression ie = (InfixExpression) node;
			Set<Address> addrs = this.resolveOrCreate(ie.getLeft());
			if(addrs == null) return null;

			/* Resolve all the objects in the address list and create a new
			 * BValue which points to those objects. */
			Addresses selfAddrs = new Addresses(Addresses.LatticeElement.SET);
			DefinerIDs definerIDs =  DefinerIDs.bottom();
			for(Address addr : addrs) {
				BValue val = this.state.store.apply(addr);
				for(Address objAddr : val.addressAD.addresses) {
					Obj obj = this.state.store.getObj(objAddr);
					if(obj != null) {
						selfAddrs.addresses.add(objAddr);
						definerIDs = definerIDs.join(val.definerIDs);
					}
				}
			}
			if(selfAddrs.addresses.isEmpty()) return BValue.bottom(Change.u());

			return Addresses.inject(selfAddrs, Change.u(), definerIDs);
		}
		else {
			/* Ignore everything else (e.g., method calls) for now. */
			return null;
		}
	}

	/**
	 * Base case: A simple name in the environment.
	 * @param node A Name node
	 * @return The set of addresses this identifier can resolve to.
	 */
	private Set<Address> resolveOrCreateBaseCase(Name node) {

		Set<Address> result = new HashSet<Address>();

		Addresses addrs = state.env.apply(node.toSource());
		if(addrs == null) {
			/* Assume the variable exists in the environment (ie. not a TypeError)
			 * and add it to the environment/store as BValue.TOP since we know
			 * nothing about it. */
			Address addr = state.trace.makeAddr(node.getID(), "");
			state.env = state.env.strongUpdate(node.toSource(), new Variable(node.getID(), node.toSource(), Change.bottom(), new Addresses(addr)));
			state.store = state.store.alloc(addr, Addresses.dummy(Change.bottom(), DefinerIDs.inject(node.getID())));
			addrs = new Addresses(addr);
		}

		result.addAll(addrs.addresses);
		return result;

	}

	/**
	 * Recursive case: A property access.
	 * @param ie A qualified name node.
	 * @return The set of addresses this identifier can resolve to.
	 */
	private Set<Address> resolveOrCreateProperty(PropertyGet ie) {

		Set<Address> result = new HashSet<Address>();

		/* We do not handle the cases where the rhs is an expression. */
		if(!(ie.getRight() instanceof Name)) return result;

		/* We have a qualified name. Recursively find all the addresses
		 * that lhs can resolve to. */
		Set<Address> lhs = resolveOrCreate(ie.getLeft());

		/* Just in case we couldn't resolve or create the sub-expression. */
		if(lhs == null) return result;

		/* Lookup the current property at each of these addresses. Ignore
		 * type errors and auto-boxing for now. */
		for(Address valAddr : lhs) {

			/* Get the value at the address. */
			BValue val = state.store.apply(valAddr);

			/* We may need to create a dummy object if 'val' doesn't point
			 * to any objects. */
			if(val.addressAD.addresses.size() == 0) {
				Map<String, Property> ext = new HashMap<String, Property>();
				Obj dummy = new Obj(ext, new InternalObjectProperties());
				Address addr = state.trace.makeAddr(ie.getLeft().getID(), "");
				state.store = state.store.alloc(addr, dummy);
				val = val.join(Address.inject(addr, Change.bottom(), DefinerIDs.bottom()));
				state.store = state.store.strongUpdate(valAddr, val);
			}

			for(Address objAddr : val.addressAD.addresses) {

				/* Get the Obj from the store. */
				Obj obj = state.store.getObj(objAddr);

				/* Look up the property. */
				Address propAddr = obj.apply(ie.getRight().toSource());

				if(propAddr != null) {
					result.add(propAddr);
					// Sanity check that the property address is in the store.
					BValue propVal = state.store.apply(propAddr);
					if(propVal == null)
						throw new Error("Property value does not exist in store.");
				}
				else {
					/* This property was not found, which means it is either
					 * undefined or was initialized somewhere outside the
					 * analysis. Create it and give it the value BValue.TOP. */

					/* Create a new address (BValue) for the property and
					 * put it in the store. */
					propAddr = state.trace.makeAddr(ie.getRight().getID(), ie.getRight().toSource());
					BValue propVal = Addresses.dummy(Change.bottom(), DefinerIDs.inject(ie.getRight().getID()));
					state.store = state.store.alloc(propAddr, propVal);

					/* Add the property to the external properties of the object. */
					Map<String, Property> ext = new HashMap<String, Property>(obj.externalProperties);
					ext.put(ie.getRight().toSource(), new Property(ie.getRight().getID(), ie.getRight().toSource(), propAddr));

					/* We need to create a new object so that the previous
					 * states are not affected by this update. */
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
	 * @param eg An element access node.
	 * @return The set of addresses this element can resolve to.
	 */
	private Set<Address> resolveOrCreateElementCase(ElementGet eg) {

		Set<Address> result = new HashSet<Address>();
		
		/* We have a qualified name. Recursively find all the addresses
		 * that lhs can resolve to. */
		Set<Address> lhs = resolveOrCreate(eg.getTarget());

		/* Just in case we couldn't resolve or create the sub-expression. */
		if(lhs == null) return result;

		/* Lookup the current property at each of these addresses. Ignore
		 * type errors and auto-boxing for now. */
		for(Address valAddr : lhs) {
			
			/* Get the value at the address. */
			BValue val = state.store.apply(valAddr);

			/* We may need to create a dummy object if 'val' doesn't point
			 * to any objects. */
			if(val.addressAD.addresses.size() == 0) {
				Map<String, Property> ext = new HashMap<String, Property>();
				Obj dummy = new Obj(ext, new InternalObjectProperties());
				Address addr = state.trace.makeAddr(eg.getID(), "");
				state.store = state.store.alloc(addr, dummy);
				val = val.join(Address.inject(addr, Change.bottom(), DefinerIDs.bottom()));
				state.store = state.store.strongUpdate(valAddr, val);
			}
			
			for(Address objAddr : val.addressAD.addresses) {

				/* Get the Obj from the store. */
				Obj obj = state.store.getObj(objAddr);

				/* Look up the property. */
				BValue elementValue = this.eval(eg.getElement());
				
				String elementString = null;
				if(elementValue.numberAD.le == Num.LatticeElement.VAL) {
					elementString = elementValue.numberAD.val;
				}
				else if(elementValue.stringAD.le == Str.LatticeElement.SNOTNUMNORSPLVAL
						|| elementValue.stringAD.le == Str.LatticeElement.SNUMVAL
						|| elementValue.stringAD.le == Str.LatticeElement.SSPLVAL) {
					elementString = elementValue.stringAD.val;
				}
				else {
					elementString = "~unknown~";
				}
				
				Address propAddr = obj.apply(elementString);

				if(propAddr != null) {
					result.add(propAddr);
					// Sanity check that the property address is in the store.
					BValue propVal = state.store.apply(propAddr);
					if(propVal == null)
						throw new Error("Property value does not exist in store.");
				}
				else {
					/* This property was not found, which means it is either
					 * undefined or was initialized somewhere outside the
					 * analysis. Create it and give it the value BValue.TOP. */

					/* Create a new address (BValue) for the property and
					 * put it in the store. */
					propAddr = state.trace.makeAddr(eg.getID(), elementString);
					BValue propVal = Addresses.dummy(Change.bottom(), DefinerIDs.inject(eg.getID()));
					state.store = state.store.alloc(propAddr, propVal);

					/* Add the property to the external properties of the object. */
					Map<String, Property> ext = new HashMap<String, Property>(obj.externalProperties);
					ext.put(elementString, new Property(eg.getTarget().getID(), elementString, propAddr));

					/* We need to create a new object so that the previous
					 * states are not affected by this update. */
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
	 * @return the address of a new (stopgap) value.
	 */
	public Set<Address> resolveOrCreateExpression(AstNode node) {
		
		/* Resolve the expression to a value. */
		BValue val = this.eval(node);
		
		/* Place the value on the store */
		Set<Address> addrs = new HashSet<Address>();
		Address addr = state.trace.makeAddr(node.getID(), "");
		state.store = state.store.alloc(addr, val);
		addrs.add(addr);

		return addrs;
		
	}

	/**
	 * Resolve an identifier to an address in the store.
	 * @param node The identifier to resolve.
	 * @return The set of addresses this identifier can resolve to.
	 */
	public Set<Address> resolveOrCreate(AstNode node) {

		/* Base Case: A simple name in the environment. */
		if(node instanceof Name) {
			return resolveOrCreateBaseCase((Name)node);
		}

		/* Recursive Case: A property access. */
		else if(node instanceof PropertyGet) {
			return resolveOrCreateProperty((PropertyGet)node);
		}
		
		/* Recursive Case: An element access. */
		else if(node instanceof ElementGet) {
			return resolveOrCreateElementCase((ElementGet)node);
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

}