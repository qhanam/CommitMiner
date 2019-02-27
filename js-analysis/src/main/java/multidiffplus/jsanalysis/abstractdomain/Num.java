package multidiffplus.jsanalysis.abstractdomain;

/**
 * Stores the state for the number type abstract domain. Lattice:
 * 
 * <pre>
 * 			      TOP
 * 			   /	  \
 * 		      CONST       REAL
 * 		  /	 |  \   /  |  \
 * 		 NaN NI	 PI 0  1  2 ...
 * 		  \	 \  \	/ /   /
 * 			     BOT
 * </pre>
 * 
 * Where TOP means the type could be a number and BOT means the type is
 * definitely not a number.
 */
public class Num {

    public LatticeElement le;
    public String val;

    public Num(LatticeElement le) {
	if (le == null)
	    throw new Error("The lattice element cannot be null.");
	if (le == LatticeElement.VAL)
	    throw new Error("A value must be provided with the NVAL lattice element.");
	this.le = le;
	this.val = null;
    }

    public Num(LatticeElement le, String val) {
	if (le == null)
	    throw new Error("The lattice element cannot be null.");
	this.le = le;
	this.val = val;
    }

    /**
     * Joins this number with another number.
     * 
     * @param state
     *            The number to join with.
     * @return A new number that is the join of the two numbers.
     */
    public Num join(Num state) {

	LatticeElement l = this.le;
	LatticeElement r = state.le;

	if (l == r && this.val == state.val)
	    return new Num(l, this.val);
	if (l == LatticeElement.BOTTOM)
	    return new Num(r, state.val);
	if (r == LatticeElement.BOTTOM)
	    return new Num(l, this.val);

	if (isReal(l) && isReal(r))
	    return new Num(LatticeElement.REAL);
	if (notNaN(l) && notNaN(r))
	    return new Num(LatticeElement.NOT_NAN);
	if (notZero(l) && notZero(r))
	    return new Num(LatticeElement.NOT_ZERO);
	if (isFalsey(l) && isFalsey(r))
	    return new Num(LatticeElement.NAN_ZERO);
	if (notZeroNorNaN(l) && notZeroNorNaN(r))
	    return new Num(LatticeElement.NOT_ZERO_NOR_NAN);

	return new Num(LatticeElement.TOP);

    }

    public static boolean isReal(LatticeElement le) {
	switch (le) {
	case VAL:
	case REAL:
	    return true;
	default:
	    return false;
	}
    }

    public static boolean isFalsey(LatticeElement le) {
	switch (le) {
	case NAN:
	case ZERO:
	case NAN_ZERO:
	    return true;
	default:
	    return false;
	}
    }

    public static boolean notNaN(LatticeElement le) {
	switch (le) {
	case NAN:
	case NOT_ZERO:
	case NAN_ZERO:
	case TOP:
	    return false;
	default:
	    return true;
	}
    }

    public static boolean notZero(LatticeElement le) {
	switch (le) {
	case ZERO:
	case NOT_NAN:
	case NAN_ZERO:
	case TOP:
	    return false;
	default:
	    return true;
	}
    }

    public static boolean notZeroNorNaN(LatticeElement le) {
	switch (le) {
	case ZERO:
	case NAN:
	case NOT_NAN:
	case NOT_ZERO:
	case NAN_ZERO:
	case TOP:
	    return false;
	default:
	    return true;
	}
    }

    /**
     * @param number
     *            The number lattice element to inject.
     * @return The base value tuple with injected number.
     */
    public static BValue inject(Num number, Change valChange, Dependencies deps) {
	return BValue.inject(Str.bottom(), number, Bool.bottom(), Null.bottom(), Undefined.bottom(),
		Addresses.bottom(), valChange, deps);
    }

    /**
     * @return true if the number is definitely not zero.
     */
    public static boolean notZero(Num num) {
	return notZero(num.le);
    }

    /**
     * @return true if the number is definitely not NaN.
     */
    public static boolean notNaN(Num num) {
	return notNaN(num.le);
    }

    /**
     * @return the top lattice element
     */
    public static Num top() {
	return new Num(LatticeElement.TOP);
    }

    /**
     * @return the bottom lattice element
     */
    public static Num bottom() {
	return new Num(LatticeElement.BOTTOM);
    }

    /** The lattice elements for the abstract domain. **/
    public enum LatticeElement {
	TOP, ZERO, NAN, NAN_ZERO, NI, PI, VAL, REAL, NOT_NAN, NOT_ZERO, NOT_ZERO_NOR_NAN, BOTTOM
    }

    @Override
    public String toString() {
	return "Num:" + this.le.toString();
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof Num))
	    return false;
	Num num = (Num) o;
	if (this.le != num.le)
	    return false;
	if (this.val == null ^ this.val == null)
	    return false;
	if (this.val != null && !this.val.equals(num.val))
	    return false;
	return true;
    }

}