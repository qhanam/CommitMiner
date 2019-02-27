package multidiffplus.jsanalysis.abstractdomain;

/**
 * Scratchpad memory. Unlike JSAI, we define a fixed set of values that we
 * store.
 */
public class Scratchpad {

    /** The scratch memory. **/
    private BValue returnValue;
    private BValue[] args;

    public Scratchpad() {
	this.returnValue = BValue.bottom();
	this.args = new BValue[0];
    }

    public Scratchpad(BValue returnValue, BValue[] args) {
	this.returnValue = returnValue == null ? BValue.bottom() : returnValue;
	this.args = args == null ? new BValue[0] : args;
    }

    public Scratchpad(Scratchpad scratch) {
	this.returnValue = scratch.returnValue;
	this.args = scratch.args;
    }

    @Override
    public Scratchpad clone() {
	return new Scratchpad(this);
    }

    /**
     * @return The return value.
     */
    public BValue applyReturn() {
	return this.returnValue;
    }

    /**
     * @return The argument values.
     */
    public BValue[] applyArgs() {
	return this.args;
    }

    /**
     * Performs a strong update.
     * 
     * @return The updated scratchpad.
     */
    public Scratchpad strongUpdate(BValue returnValue, BValue[] args) {
	return new Scratchpad(returnValue, args);
    }

    /**
     * Performs a weak upadate to the return value.
     * 
     * @return The updated scratchpad.
     */
    public Scratchpad weakUpdate(BValue returnValue) {
	return this.join(new Scratchpad(returnValue, this.args));
    }

    /**
     * Compute the union of this and another Scratchpad.
     * 
     * @param pad
     *            The Scratchpad to union.
     * @return The union of the scratchpads.
     */
    public Scratchpad join(Scratchpad pad) {

	BValue retVal = this.returnValue.join(pad.returnValue);

	int arglen = this.args.length > pad.args.length ? this.args.length : pad.args.length;
	BValue[] args = new BValue[arglen];

	for (int i = 0; i < args.length; i++) {
	    if (i >= this.args.length) {
		args[i] = pad.args[i];
	    } else if (i >= pad.args.length) {
		args[i] = this.args[i];
	    } else {
		args[i] = this.args[i].join(pad.args[i]);
	    }
	}

	return new Scratchpad(retVal, args);

    }

}