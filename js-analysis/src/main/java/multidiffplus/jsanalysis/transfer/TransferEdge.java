package multidiffplus.jsanalysis.transfer;

import java.util.Set;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.UnaryExpression;

import multidiffplus.cfg.CFGEdge;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Bool;
import multidiffplus.jsanalysis.abstractdomain.Null;
import multidiffplus.jsanalysis.abstractdomain.Num;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Str;
import multidiffplus.jsanalysis.abstractdomain.Undefined;

/**
 * Transfers a state over an edge.
 */
public class TransferEdge {

    private State state;
    private CFGEdge edge;
    private ExpEval expEval;

    public TransferEdge(State state, CFGEdge edge, ExpEval expEval) {
	this.state = state;
	this.edge = edge;
	this.expEval = expEval;
    }

    public void transfer() {

	/* Update the trace to the current condition. */
	state.trace = state.trace.update(edge.getId());

	/* The condition to transfer over. */
	AstNode condition = (AstNode) edge.getCondition();

	/* Interpret the effect of the edge on control flow. */
	state.control = state.control.update(edge, edge.getFrom());

	/* Nothing to interpret if there is no condition. */
	if (condition == null)
	    return;

	/* Interpret the statement with respect to branch conditions. */
	interpretCondition(condition, false);

	/* Interpret the effects of the expression. */
	expEval.eval(condition);

    }

    /**
     * Performs an abstract interpretation on the condition.
     */
    private void interpretCondition(AstNode condition, boolean not) {
	if (condition instanceof ParenthesizedExpression) {
	    interpretCondition(((ParenthesizedExpression) condition).getExpression(), not);
	} else if (condition instanceof Name) {
	    Set<Address> addrs = expEval.resolveOrCreate(condition);
	    if (not)
		interpretAddrsFalsey(addrs);
	    else
		interpretAddrsTruthy(addrs);
	} else if (condition instanceof UnaryExpression
		&& ((UnaryExpression) condition).getOperator() == Token.NOT) {
	    UnaryExpression ue = (UnaryExpression) condition;
	    interpretCondition(ue.getOperand(), !not);
	} else if (condition instanceof InfixExpression
		&& ((InfixExpression) condition).getOperator() == Token.GETPROP) {
	    Set<Address> addrs = expEval.resolveOrCreate(condition);
	    if (not)
		interpretAddrsFalsey(addrs);
	    else
		interpretAddrsTruthy(addrs);
	} else if (condition instanceof InfixExpression) {
	    InfixExpression ie = (InfixExpression) condition;
	    switch (ie.getOperator()) {
	    case Token.EQ:
		if (not)
		    interpretNE(ie);
		else
		    interpretEQ(ie);
	    case Token.NE:
		if (not)
		    interpretEQ(ie);
		else
		    interpretNE(ie);
	    case Token.SHEQ:
		if (not)
		    interpretSHNE(ie);
		else
		    interpretSHEQ(ie);
		break;
	    case Token.SHNE:
		if (not)
		    interpretSHEQ(ie);
		else
		    interpretSHNE(ie);
		break;
	    case Token.AND:
		interpretAnd(ie, not);
		break;
	    case Token.OR:
		interpretOr(ie, not);
		break;
	    }
	}
    }

    private void interpretAddrsFalsey(Set<Address> addrs) {

	// Update the value(s) to be falsey.
	for (Address addr : addrs) {
	    // Keep the BValue change LE (the value does not change).
	    BValue oldVal = state.store.apply(addr, new Name());
	    // Refine the primitives to their falsey values.
	    BValue val = BValue.inject(new Str(Str.LatticeElement.SBLANK),
		    new Num(Num.LatticeElement.NAN_ZERO), new Bool(Bool.LatticeElement.FALSE),
		    Null.top(), Undefined.top(), oldVal.addressAD, oldVal.change, oldVal.deps);
	    // Update the store.
	    state.store.strongUpdate(addr, val, new Name());
	}

    }

    private void interpretAddrsTruthy(Set<Address> addrs) {

	// Update the value(s) to be truthy.
	for (Address addr : addrs) {
	    // Keep the BValue change LE (the value does not change).
	    BValue oldVal = state.store.apply(addr, new Name());
	    // Refine the primitives to their truthy values.
	    BValue val = BValue.inject(new Str(Str.LatticeElement.SNOTBLANK),
		    new Num(Num.LatticeElement.NOT_ZERO_NOR_NAN),
		    new Bool(Bool.LatticeElement.TRUE), Null.bottom(), Undefined.bottom(),
		    oldVal.addressAD, oldVal.change, oldVal.deps);
	    // Update the store.
	    state.store.strongUpdate(addr, val, new Name());
	}

    }

    private void interpretNE(InfixExpression ie) {

	Set<Address> rhsAddrs = expEval.resolveOrCreate(ie.getRight());

	/* Get the value of the RHS. */
	BValue rhsVal = BValue.bottom();
	for (Address rhsAddr : rhsAddrs) {
	    rhsVal = rhsVal.join(state.store.apply(rhsAddr, ie.getRight()));
	}

	/* Update the value(s) on the LHS. */
	Set<Address> lhsAddrs = expEval.resolveOrCreate(ie.getLeft());
	if (BValue.isUndefined(rhsVal) || BValue.isNull(rhsVal))
	    for (Address lhsAddr : lhsAddrs) {
		BValue lhsVal = state.store.apply(lhsAddr, ie.getLeft());
		lhsVal.undefinedAD = Undefined.bottom();
		lhsVal.nullAD = Null.bottom();
	    }
	if (BValue.isBlank(rhsVal) || BValue.isZero(rhsVal))
	    for (Address lhsAddr : lhsAddrs) {
		BValue lhsVal = state.store.apply(lhsAddr, ie.getLeft());
		/* Make sure we don't decrease the precision of the LE. */
		if (!Str.notBlank(lhsVal.stringAD))
		    lhsVal.stringAD = new Str(Str.LatticeElement.SNOTBLANK);
		if (!Num.notZero(lhsVal.numberAD))
		    lhsVal.numberAD = new Num(Num.LatticeElement.NOT_ZERO);
	    }
	if (BValue.isNaN(rhsVal))
	    for (Address lhsAddr : lhsAddrs) {
		BValue lhsVal = state.store.apply(lhsAddr, ie.getRight());
		if (!Num.notNaN(lhsVal.numberAD))
		    lhsVal.numberAD = new Num(Num.LatticeElement.NOT_NAN);
	    }
	if (BValue.isFalse(rhsVal))
	    interpretAddrsTruthy(lhsAddrs);
	if (BValue.isAddress(rhsVal))
	    for (Address lhsAddr : lhsAddrs) {
		BValue lhsVal = state.store.apply(lhsAddr, ie.getLeft());
		lhsVal.addressAD.addresses.removeAll(rhsVal.addressAD.addresses);
	    }

    }

    private void interpretEQ(InfixExpression ie) {

	/* Get the value of the RHS. */
	Set<Address> rhsAddrs = expEval.resolveOrCreate(ie.getRight());
	BValue rhsVal = BValue.bottom();
	for (Address rhsAddr : rhsAddrs) {
	    rhsVal = rhsVal.join(state.store.apply(rhsAddr, ie.getRight()));
	}

	/* Update the value(s) on the LHS. */
	Set<Address> lhsAddrs = expEval.resolveOrCreate(ie.getLeft());
	if (BValue.isUndefined(rhsVal) || BValue.isNull(rhsVal))
	    for (Address lhsAddr : lhsAddrs) {
		BValue oldVal = state.store.apply(lhsAddr, ie.getLeft());
		rhsVal = Undefined.inject(Undefined.top(), oldVal.change, oldVal.deps)
			.join(Null.inject(Null.top(), oldVal.change, oldVal.deps));
		state.store.strongUpdate(lhsAddr, rhsVal, new Name());
	    }
	if (BValue.isBlank(rhsVal) || BValue.isZero(rhsVal))
	    for (Address lhsAddr : lhsAddrs) {
		BValue oldVal = state.store.apply(lhsAddr, ie.getLeft());
		rhsVal = Str.inject(new Str(Str.LatticeElement.SBLANK), oldVal.change, oldVal.deps)
			.join(Num.inject(new Num(Num.LatticeElement.ZERO), oldVal.change,
				oldVal.deps));
		state.store.strongUpdate(lhsAddr, rhsVal, new Name());
	    }
	if (BValue.isNaN(rhsVal))
	    for (Address lhsAddr : lhsAddrs) {
		BValue oldVal = state.store.apply(lhsAddr, ie.getLeft());
		state.store.strongUpdate(lhsAddr,
			Num.inject(new Num(Num.LatticeElement.NAN), oldVal.change, oldVal.deps),
			new Name());
	    }
	if (BValue.isFalse(rhsVal))
	    interpretAddrsFalsey(lhsAddrs);
	if (BValue.isAddress(rhsVal))
	    for (Address lhsAddr : lhsAddrs) {
		BValue lhsVal = state.store.apply(lhsAddr, ie.getLeft());
		lhsVal.addressAD.addresses.retainAll(rhsVal.addressAD.addresses);
	    }

    }

    private void interpretSHEQ(InfixExpression ie) {

	/* Get the value of the RHS. */
	Set<Address> rhsAddrs = expEval.resolveOrCreate(ie.getRight());
	BValue rhsVal = BValue.bottom();
	for (Address rhsAddr : rhsAddrs) {
	    rhsVal = rhsVal.join(state.store.apply(rhsAddr, ie.getRight()));
	}

	/* Update the value(s) on the LHS. */
	Set<Address> lhsAddrs = expEval.resolveOrCreate(ie.getLeft());
	for (Address lhsAddr : lhsAddrs) {
	    state.store.strongUpdate(lhsAddr, rhsVal, new Name());
	}

    }

    private void interpretSHNE(InfixExpression ie) {

	Set<Address> rhsAddrs = expEval.resolveOrCreate(ie.getRight());

	/* Get the value of the RHS. */
	BValue rhsVal = BValue.bottom();
	for (Address rhsAddr : rhsAddrs) {
	    rhsVal = rhsVal.join(state.store.apply(rhsAddr, ie.getRight()));
	}

	/* Update the value(s) on the LHS. */
	Set<Address> lhsAddrs = expEval.resolveOrCreate(ie.getLeft());
	if (BValue.isUndefined(rhsVal))
	    for (Address lhsAddr : lhsAddrs)
		state.store.apply(lhsAddr, ie.getLeft()).undefinedAD = Undefined.bottom();
	if (BValue.isNull(rhsVal))
	    for (Address lhsAddr : lhsAddrs)
		state.store.apply(lhsAddr, ie.getLeft()).nullAD = Null.bottom();
	if (BValue.isBlank(rhsVal))
	    for (Address lhsAddr : lhsAddrs)
		state.store.apply(lhsAddr, ie.getLeft()).stringAD = new Str(
			Str.LatticeElement.SNOTBLANK);
	if (BValue.isNaN(rhsVal))
	    for (Address lhsAddr : lhsAddrs)
		state.store.apply(lhsAddr, ie.getLeft()).numberAD = new Num(
			Num.LatticeElement.NOT_NAN);
	if (BValue.isZero(rhsVal))
	    for (Address lhsAddr : lhsAddrs)
		state.store.apply(lhsAddr, ie.getLeft()).numberAD = new Num(
			Num.LatticeElement.NOT_ZERO);
	if (BValue.isFalse(rhsVal))
	    for (Address lhsAddr : lhsAddrs)
		state.store.apply(lhsAddr, ie.getLeft()).booleanAD = new Bool(
			Bool.LatticeElement.TRUE);
	if (BValue.isAddress(rhsVal))
	    for (Address lhsAddr : lhsAddrs) {
		BValue lhsVal = state.store.apply(lhsAddr, ie.getLeft());
		lhsVal.addressAD.addresses.removeAll(rhsVal.addressAD.addresses);
	    }

    }

    private void interpretOr(InfixExpression ie, boolean not) {

	/*
	 * Interpret both sides of the condition if they must both be true.
	 */
	if (not) {
	    interpretCondition(ie.getLeft(), false);
	    interpretCondition(ie.getRight(), false);
	}

    }

    private void interpretAnd(InfixExpression ie, boolean not) {

	/*
	 * Interpret both sides of the condition if they must both be true.
	 */
	if (!not) {
	    interpretCondition(ie.getLeft(), false);
	    interpretCondition(ie.getRight(), false);
	}

    }

}