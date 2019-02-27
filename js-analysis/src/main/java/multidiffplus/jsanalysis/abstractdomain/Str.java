package multidiffplus.jsanalysis.abstractdomain;

/**
 * Stores the state for the number type abstract domain. Lattice element is:
 * 
 * <pre>
 * 			    TOP
 * 		     /	   /	\      \
 * 		   /	SNotSpl 	 SNotNum   \ 
 * 		  /           \   /    \      \	
 * 		SNum	SNotNumNorSpl	SSpl    \
 * 		 |	     |	         |	  \
 * 		"0"..      "foo"..    "valueOf"... ""
 * 		  \ \	     |   |       /     /   /
 * 			      BOT
 * </pre>
 * 
 * Where TOP means the type could be any string and BOT means the type is
 * definitely not a string.
 */
public class Str {

    public LatticeElement le;
    public String val;

    public Str(LatticeElement le, String val) {
	this.le = le;
	this.val = val;
    }

    public Str(LatticeElement le) {
	if (this.le == LatticeElement.SNUMVAL || this.le == LatticeElement.SNOTNUMNORSPLVAL
		|| this.le == LatticeElement.SSPLVAL)
	    throw new Error("A value must be provided with a VAL lattice element.");
	this.le = le;
	this.val = null;
    }

    /**
     * Joins this string with another string.
     * 
     * @param state
     *            The string to join with.
     * @return A new string that is the join of the two strings.
     */
    public Str join(Str state) {

	LatticeElement l = this.le;
	LatticeElement r = state.le;

	if (l == r && this.val == state.val)
	    return new Str(l, this.val);
	if (l == LatticeElement.BOTTOM)
	    return new Str(r, state.val);
	if (r == LatticeElement.BOTTOM)
	    return new Str(l, state.val);

	if (isNum(l) && isNum(r))
	    return new Str(LatticeElement.SNUM);
	if (isStr(l) && isStr(r))
	    return new Str(LatticeElement.SNOTNUMNORSPL);
	if (isSpl(l) && isSpl(r))
	    return new Str(LatticeElement.SSPL);

	if (notSpl(l) && notSpl(r))
	    return new Str(LatticeElement.SNOTSPL);
	if (notNum(l) && notNum(r))
	    return new Str(LatticeElement.SNOTNUM);

	if (notBlank(l) && notBlank(r))
	    return new Str(LatticeElement.SNOTBLANK);

	return new Str(LatticeElement.TOP);

    }

    private static boolean isNum(LatticeElement le) {
	switch (le) {
	case SNUMVAL:
	case SNUM:
	    return true;
	default:
	    return false;
	}
    }

    private static boolean isStr(LatticeElement le) {
	switch (le) {
	case SNOTNUMNORSPLVAL:
	case SNOTNUMNORSPL:
	    return true;
	default:
	    return false;
	}
    }

    private static boolean isSpl(LatticeElement le) {
	switch (le) {
	case SSPLVAL:
	case SSPL:
	    return true;
	default:
	    return false;
	}
    }

    private static boolean notSpl(LatticeElement le) {
	switch (le) {
	case SSPLVAL:
	case SSPL:
	case SNOTNUM:
	case TOP:
	    return false;
	default:
	    return true;
	}
    }

    private static boolean notNum(LatticeElement le) {
	switch (le) {
	case SNUMVAL:
	case SNUM:
	case SNOTSPL:
	case TOP:
	    return false;
	default:
	    return true;
	}
    }

    private static boolean notBlank(LatticeElement le) {
	switch (le) {
	case SBLANK:
	case TOP:
	    return false;
	default:
	    return true;
	}
    }

    /**
     * @return true if the string is definitely not blank.
     */
    public static boolean notBlank(Str str) {
	if (isNum(str.le) || isSpl(str.le) || str.le == LatticeElement.BOTTOM
		|| (str.le == LatticeElement.SNOTNUMNORSPLVAL && !str.val.equals("")))
	    return true;
	return false;
    }

    /**
     * @param string
     *            The string lattice element to inject.
     * @return The base value tuple with injected string.
     */
    public static BValue inject(Str string, Change valChange, Dependencies deps) {
	return BValue.inject(string, Num.bottom(), Bool.bottom(), Null.bottom(), Undefined.bottom(),
		Addresses.bottom(), valChange, deps);
    }

    /**
     * @return the top lattice element
     */
    public static Str top() {
	return new Str(LatticeElement.TOP);
    }

    /**
     * @return the bottom lattice element
     */
    public static Str bottom() {
	return new Str(LatticeElement.BOTTOM);
    }

    /** The type of a lattice element for the abstract domain. **/
    public enum LatticeElement {
	TOP, SBLANK, SNOTBLANK, SNOTSPL, SNOTNUM, SNUM, SNOTNUMNORSPL, SSPL, SNUMVAL, SNOTNUMNORSPLVAL, SSPLVAL, BOTTOM
    }

    @Override
    public String toString() {
	if (this.val != null)
	    return "Str:" + this.val;
	return "Str:" + this.le.toString();
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof Str))
	    return false;
	Str str = (Str) o;
	if (this.le != str.le)
	    return false;
	if (this.val == null ^ this.val == null)
	    return false;
	if (this.val != null && !this.val.equals(str.val))
	    return false;
	return true;
    }

}