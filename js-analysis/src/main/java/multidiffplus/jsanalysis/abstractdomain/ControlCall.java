package multidiffplus.jsanalysis.abstractdomain;

/**
 * Stores the state of control flow changes due to changes in function calls.
 */
public class ControlCall {

    /**
     * AST node IDs of modified callsites of this method.
     */
    Change change;

    private ControlCall() {
	this.change = Change.bottom();
    }

    private ControlCall(Change change) {
	this.change = change;
    }

    /**
     * Update the control call domain for a new callsite. We only track callsites
     * one level deep.
     */
    public ControlCall update(Change change) {
	return new ControlCall(change);
    }

    public ControlCall join(ControlCall that) {
	return new ControlCall(this.change.join(that.change));
    }

    public boolean isChanged() {
	return !change.getDependencies().isEmpty();
    }

    public static ControlCall inject(Change change) {
	return new ControlCall(change);
    }

    public static ControlCall bottom() {
	return new ControlCall();
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof ControlCall))
	    return false;
	ControlCall that = (ControlCall) o;
	return this.change.equals(that.change);
    }

}
