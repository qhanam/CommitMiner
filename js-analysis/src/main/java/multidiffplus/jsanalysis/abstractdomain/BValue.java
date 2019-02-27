package multidiffplus.jsanalysis.abstractdomain;

/**
 * The abstract domain for base values. Because the value can be multiple types,
 * the abstract domain is a tuple of lattice elements: one for each base type
 * (string, number, boolean, null, undefined and address).
 */
public class BValue {

    /** The abstract domain for strings. **/
    public Str stringAD;

    /** The abstract domain for numbers. **/
    public Num numberAD;

    /** The abstract domain for booleans. **/
    public Bool booleanAD;

    /** The abstract domain for null. **/
    public Null nullAD;

    /** The abstract domain for undefined. **/
    public Undefined undefinedAD;

    /** The abstract domain for memory addresses. **/
    public Addresses addressAD;

    /** The set of value definitions this BValue may point to. **/
    public Dependencies deps;

    /** The lattice element for tracking changes to this value. **/
    public Change change;

    private BValue(Str stringAD, Num numberAD, Bool booleanAD, Null nullAD, Undefined undefinedAD,
	    Addresses addressAD, Change change, Dependencies deps) {
	this.stringAD = stringAD;
	this.numberAD = numberAD;
	this.booleanAD = booleanAD;
	this.nullAD = nullAD;
	this.undefinedAD = undefinedAD;
	this.addressAD = addressAD;
	this.change = change;
	this.deps = deps;
    }

    public BValue join(BValue state) {

	return new BValue(this.stringAD.join(state.stringAD), this.numberAD.join(state.numberAD),
		this.booleanAD.join(state.booleanAD), this.nullAD.join(state.nullAD),
		this.undefinedAD.join(state.undefinedAD), this.addressAD.join(state.addressAD),
		this.change.join(state.change), this.deps.join(state.deps));

    }

    /**
     * @return true if the value is definitely undefined
     */
    public static boolean isUndefined(BValue val) {
	if (val.undefinedAD.le == Undefined.LatticeElement.TOP
		&& val.nullAD.le == Null.LatticeElement.BOTTOM
		&& val.numberAD.le == Num.LatticeElement.BOTTOM
		&& val.stringAD.le == Str.LatticeElement.BOTTOM
		&& val.booleanAD.le == Bool.LatticeElement.BOTTOM
		&& val.addressAD.le == Addresses.LatticeElement.BOTTOM)
	    return true;
	return false;
    }

    /**
     * @return true if the value is definitely null
     */
    public static boolean isNull(BValue val) {
	if (val.undefinedAD.le == Undefined.LatticeElement.BOTTOM
		&& val.nullAD.le == Null.LatticeElement.TOP
		&& val.numberAD.le == Num.LatticeElement.BOTTOM
		&& val.stringAD.le == Str.LatticeElement.BOTTOM
		&& val.booleanAD.le == Bool.LatticeElement.BOTTOM
		&& val.addressAD.le == Addresses.LatticeElement.BOTTOM)
	    return true;
	return false;
    }

    /**
     * @return true if the value is definitely blank
     */
    public static boolean isBlank(BValue val) {
	if (val.undefinedAD.le == Undefined.LatticeElement.BOTTOM
		&& val.nullAD.le == Null.LatticeElement.BOTTOM
		&& val.numberAD.le == Num.LatticeElement.BOTTOM
		&& val.stringAD.le == Str.LatticeElement.SBLANK
		&& val.booleanAD.le == Bool.LatticeElement.BOTTOM
		&& val.addressAD.le == Addresses.LatticeElement.BOTTOM)
	    return true;
	return false;
    }

    /**
     * @return true if the value is definitely NaN
     */
    public static boolean isNaN(BValue val) {
	if (val.undefinedAD.le == Undefined.LatticeElement.BOTTOM
		&& val.nullAD.le == Null.LatticeElement.BOTTOM
		&& val.numberAD.le == Num.LatticeElement.NAN
		&& val.stringAD.le != Str.LatticeElement.BOTTOM
		&& val.booleanAD.le == Bool.LatticeElement.BOTTOM
		&& val.addressAD.le == Addresses.LatticeElement.BOTTOM)
	    return true;
	return false;
    }

    /**
     * @return true if the value is definitely zero
     */
    public static boolean isZero(BValue val) {
	if (val.undefinedAD.le == Undefined.LatticeElement.BOTTOM
		&& val.nullAD.le == Null.LatticeElement.BOTTOM
		&& val.numberAD.le == Num.LatticeElement.ZERO
		&& val.stringAD.le != Str.LatticeElement.BOTTOM
		&& val.booleanAD.le == Bool.LatticeElement.BOTTOM
		&& val.addressAD.le == Addresses.LatticeElement.BOTTOM)
	    return true;
	return false;
    }

    /**
     * @return true if the value is definitely false
     */
    public static boolean isFalse(BValue val) {
	if (val.undefinedAD.le == Undefined.LatticeElement.BOTTOM
		&& val.nullAD.le == Null.LatticeElement.BOTTOM
		&& val.numberAD.le == Num.LatticeElement.BOTTOM
		&& val.stringAD.le != Str.LatticeElement.BOTTOM
		&& val.booleanAD.le == Bool.LatticeElement.FALSE
		&& val.addressAD.le == Addresses.LatticeElement.BOTTOM)
	    return true;
	return false;
    }

    /**
     * @return true if the value is definitely a single address
     */
    public static boolean isAddress(BValue val) {
	if (val.undefinedAD.le == Undefined.LatticeElement.BOTTOM
		&& val.nullAD.le == Null.LatticeElement.BOTTOM
		&& val.numberAD.le == Num.LatticeElement.BOTTOM
		&& val.stringAD.le != Str.LatticeElement.BOTTOM
		&& val.booleanAD.le == Bool.LatticeElement.BOTTOM
		&& val.addressAD.le == Addresses.LatticeElement.SET
		&& val.addressAD.addresses.size() == 1)
	    return true;
	return false;
    }

    /**
     * @param valChange
     *            The change LE for the value.
     * @return the top lattice element
     */
    public static BValue top(Change valChange, Dependencies deps) {
	/* Set addresses to BOT to enable dynamic object creation. */
	return new BValue(Str.top(), Num.top(), Bool.top(), Null.top(), Undefined.top(),
		Addresses.bottom(), valChange, deps);
    }

    /**
     * @return the bottom lattice element
     */
    public static BValue bottom() {
	return new BValue(Str.bottom(), Num.bottom(), Bool.bottom(), Null.bottom(),
		Undefined.bottom(), Addresses.bottom(), Change.bottom(), Dependencies.bot());
    }

    /**
     * @param valChange
     *            The change LE for the value.
     * @return the bottom lattice element
     */
    public static BValue bottom(Change valChange, Dependencies deps) {
	return new BValue(Str.bottom(), Num.bottom(), Bool.bottom(), Null.bottom(),
		Undefined.bottom(), Addresses.bottom(), valChange, deps);
    }

    /**
     * @param valChange
     *            The change LE for the value.
     * @return a primitive value (not an address).
     */
    public static BValue primitive(Change valChange, Dependencies deps) {
	return new BValue(Str.top(), Num.top(), Bool.top(), Null.top(), Undefined.top(),
		Addresses.bottom(), valChange, deps);
    }

    public static BValue inject(Str str, Num num, Bool bool, Null nul, Undefined undef,
	    Addresses addrs, Change valChange, Dependencies deps) {
	return new BValue(str, num, bool, nul, undef, addrs, valChange, deps);
    }

    @Override
    public String toString() {
	return this.nullAD.toString() + "|" + this.undefinedAD.toString() + "|"
		+ this.booleanAD.toString() + "|" + this.numberAD.toString() + "|"
		+ this.stringAD.toString() + "|" + this.addressAD.toString() + "|"
		+ this.change.toString() + "|" + this.deps.toString();
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof BValue))
	    return false;
	BValue v = (BValue) o;
	if (!this.stringAD.equals(v.stringAD))
	    return false;
	if (!this.numberAD.equals(v.numberAD))
	    return false;
	if (!this.booleanAD.equals(v.booleanAD))
	    return false;
	if (!this.nullAD.equals(v.nullAD))
	    return false;
	if (!this.undefinedAD.equals(v.undefinedAD))
	    return false;
	if (!this.addressAD.equals(v.addressAD))
	    return false;
	if (!this.change.equals(v.change))
	    return false;
	if (!this.deps.equals(v.deps))
	    return false;
	return true;
    }

}