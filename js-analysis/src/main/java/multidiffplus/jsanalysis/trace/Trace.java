package multidiffplus.jsanalysis.trace;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;

import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.Environment;
import multidiffplus.jsanalysis.abstractdomain.JSClass;
import multidiffplus.jsanalysis.abstractdomain.Store;

public abstract class Trace {

	/**
	 * There is only one object type right now. Functions addresses
	 * are allocated elsewhere and do not need an offset.
	 */
	public static final Set<JSClass> traceTypes;
	static {
		Set<JSClass> set = new HashSet<JSClass>(1,2);
		set.add(JSClass.CObject);
		traceTypes = Collections.unmodifiableSet(set);
	}

	/** Update on intraprocedural step. **/
	public abstract Trace update(int pp);

	/** Update for function call. **/
	public abstract Trace update(Environment env, Store store, Address selfAddr,
								 AstNode statement);

	/** Update for function return. Default implementation is stack-based. **/
	public Trace update(Trace trace) {
		return trace;
	}

	/** Convert the trace directly to an address. **/
	public abstract Address toAddr(String prop);

	/** Make a new address with the node id of a variable/object being allocated. **/
	public abstract Address makeAddr(int varID, String prop);

	/** Modify an address to account for an object's class. **/
	public Address modAddr(Address a, JSClass c) {
		/* We can add an offset directly because our ASTNodes are offset by
		 * multiples of 16 (so we can have up to 15 classes). */
		if(traceTypes.contains(c))
			return new Address(a.addr.add(BigInteger.valueOf(c.getValue())), a.prop);
		return a;
	}

	/**
	 * All traces have the base id = the id of the variable used to create
	 * the address.
	 * @return The base of the address.
	 */
	public static int getBase(Address a) {
		return a.addr.intValue();
	}

	/**
	 * Converts a trace and a program-point into an address id.
	 * @param tr A subset of the execution trace.
	 * @param pp The program point.
	 * @return An address for the trace and program point.
	 */
	protected static BigInteger intsToBigInteger(Queue<Integer> tr, int pp) {
		BigInteger id = BigInteger.ZERO;
		for(Integer call : tr) {
			id.add(BigInteger.valueOf((call))).shiftLeft(32);
		}
		return id.shiftLeft(32).add(BigInteger.valueOf(pp));
	}

}