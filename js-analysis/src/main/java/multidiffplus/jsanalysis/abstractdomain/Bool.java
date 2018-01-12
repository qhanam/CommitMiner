package multidiffplus.jsanalysis.abstractdomain;

/**
 * Stores the state for the boolean type abstract domain.
 * Lattice element is simple:
 * 			TOP
 * 		   /   \
 * 		 true false
 * 		   \   /
 * 			BOT
 * Where TOP means the type could be a boolean and BOT means the type is definitely
 * not a boolean.
 */
public class Bool {

	public LatticeElement le;

	public Bool(LatticeElement le) {
		this.le = le;
	}

	/**
	 * Joins this boolean with another boolean.
	 * @param state The boolean to join with.
	 * @return A new boolean that is the join of two booleans.
	 */
	public Bool join(Bool state) {

		LatticeElement l = this.le;
		LatticeElement r = this.le;

		if(l == r) return new Bool(l);
		if(l == LatticeElement.BOTTOM) return new Bool(r);
		if(r == LatticeElement.BOTTOM) return new Bool(l);

		return new Bool(LatticeElement.TOP);

	}

	/**
	 * @return true if the value cannot be false
	 */
	public static boolean notFalse(Bool bool) {
		switch(bool.le) {
		case BOTTOM:
		case TRUE: return true;
		default: return false;
		}
	}

	/**
	 * @param bool The boolean lattice element to inject.
	 * @return The base value tuple with injected boolean.
	 */
	public static BValue inject(Bool bool, Change valChange, DefinerIDs definerIDs) {
		return new BValue(
				Str.bottom(),
				Num.bottom(),
				bool,
				Null.bottom(),
				Undefined.bottom(),
				Addresses.bottom(),
				valChange,
				definerIDs);
	}

	/**
	 * @return the top lattice element
	 */
	public static Bool top() {
		return new Bool(LatticeElement.TOP);
	}

	/**
	 * @return the bottom lattice element
	 */
	public static Bool bottom() {
		return new Bool(LatticeElement.BOTTOM);
	}

	/** The lattice elements for the abstract domain. **/
	public enum LatticeElement {
		TOP,
		TRUE,
		FALSE,
		BOTTOM
	}

	@Override
	public String toString() {
		return "Bool:" + this.le.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Bool)) return false;
		Bool bool = (Bool)o;
		if(this.le != bool.le) return false;
		return true;
	}

}