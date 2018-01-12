package multidiffplus.jsanalysis.abstractdomain;

/**
 * Stores the state for the undefined type abstract domain.
 * Lattice element is simple:
 * 			TOP
 * 			 |
 * 			BOT
 * Where TOP means the type could be undefined and BOT means the type is
 * definitely not undefined.
 */
public class Undefined {

	public LatticeElement le;

	private Undefined(LatticeElement le) {
		this.le = le;
	}

	/**
	 * Joins this undefined with another undefined.
	 * @param state The undefined to join with.
	 * @return A new undefined that is the join of the two addresses.
	 */
	public Undefined join(Undefined state) {
		if(this.le == state.le) return new Undefined(this.le);
		return new Undefined(LatticeElement.TOP);
	}

	/**
	 * @param undefined The undefined lattice element to inject.
	 * @return The base value tuple with injected undefined.
	 */
	public static BValue inject(Undefined undefined, Change valChange, DefinerIDs definerIDs) {
		return new BValue(
				Str.bottom(),
				Num.bottom(),
				Bool.bottom(),
				Null.bottom(),
				undefined,
				Addresses.bottom(),
				valChange,
				definerIDs);
	}

	/**
	 * @return the top lattice element
	 */
	public static Undefined top() {
		return new Undefined(LatticeElement.TOP);
	}

	/**
	 * @return the bottom lattice element
	 */
	public static Undefined bottom() {
		return new Undefined(LatticeElement.BOTTOM);
	}

	/** The lattice elements for the abstract domain. **/
	public enum LatticeElement {
		TOP,
		BOTTOM
	}

	@Override
	public String toString() {
		return "Undef:" + this.le.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Undefined)) return false;
		Undefined u = (Undefined)o;
		if(this.le != u.le) return false;
		return true;
	}

}