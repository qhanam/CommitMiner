package multidiffplus.jsanalysis.interpreter;

import java.util.HashSet;
import java.util.Set;

import org.mozilla.javascript.ast.Name;

import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Obj;
import multidiffplus.jsanalysis.abstractdomain.Property;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Variable;

/**
 * Compare two states. Used to decide whether or not a function needs to be
 * re-analyzed.
 */
public class StateComparator {

    protected State s1;
    protected State s2;

    Set<Address> visited;

    public StateComparator(State s1, State s2) {
	this.s1 = s1;
	this.s2 = s2;
    }

    public boolean isEqual() {
	visited = new HashSet<Address>();
	return equalState();
    }

    /**
     * @return true if the states are equivalent with respect to environment and
     *         store.
     */
    private boolean equalState() {

	/* Check the initial environment. */
	if (!s1.env.equals(s2.env))
	    return false;

	/* Check the reachable values in the store. */
	for (Variable var : s1.env.environment.values()) {
	    for (Address addr : var.addresses.addresses) {

		/* Check the values are the same. */
		if (!equalVal(addr))
		    return false;

	    }
	}

	/* Check if there is a control change to propagate. */
	if (!s1.control.equals(s2.control))
	    return false;

	return true;

    }

    /**
     * Compare the values of both states at the address.
     * 
     * @param addr
     *            The address of the value.
     * @return {@code true} if the values are equivalent.
     */
    private boolean equalVal(Address addr) {

	if (visited.contains(addr))
	    return true;

	/* Don't re-visit this address. */
	visited.add(addr);

	/* Check that the values are the same. */
	BValue b1 = s1.store.apply(addr, new Name());
	BValue b2 = s2.store.apply(addr, new Name());
	if (!b1.equals(b2))
	    return false;

	/* Check that the objects are the same. */
	for (Address objAddr : b1.addressAD.addresses) {
	    Obj s1Obj = s1.store.getObj(objAddr);
	    Obj s2Obj = s2.store.getObj(objAddr);
	    if (!s1Obj.equals(s2Obj))
		return false;

	    /* Check that the object properties are the same. */
	    for (Property prop : s1Obj.externalProperties.values()) {
		if (!equalVal(prop.address))
		    return false;
	    }
	}

	return true;

    }

}