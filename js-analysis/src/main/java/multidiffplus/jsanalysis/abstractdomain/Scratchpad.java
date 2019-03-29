package multidiffplus.jsanalysis.abstractdomain;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.ast.FunctionCall;

/**
 * Scratchpad memory; stores temporary values for passing args and return values
 * between functions.
 */
public class Scratchpad {

    /** The scratch memory. **/
    private BValue returnValue;
    private BValue[] args;
    private Map<FunctionCall, BValue> callValues;

    private Scratchpad() {
	this.returnValue = BValue.bottom();
	this.args = new BValue[0];
	this.callValues = new HashMap<>();
    }

    private Scratchpad(BValue returnValue, BValue[] args, Map<FunctionCall, BValue> callValues) {
	this.returnValue = returnValue == null ? BValue.bottom() : returnValue;
	this.args = args == null ? new BValue[0] : args;
	this.callValues = callValues;
    }

    private Scratchpad(BValue[] args) {
	this.returnValue = BValue.bottom();
	this.args = args == null ? new BValue[0] : args;
	this.callValues = new HashMap<>();
    }

    private Scratchpad(Scratchpad scratch) {
	this.returnValue = scratch.returnValue;
	this.args = scratch.args;
	this.callValues = scratch.callValues;
    }

    @Override
    public Scratchpad clone() {
	return new Scratchpad(this);
    }

    /**
     * Returns the return value of the current function.
     */
    public BValue applyReturn() {
	return this.returnValue;
    }

    /**
     * Returns the argument values of the current function.
     */
    public BValue[] applyArgs() {
	return this.args;
    }

    /**
     * Returns the BValue returned by the function call.
     */
    public BValue applyCall(FunctionCall fc) {
	return this.callValues.get(fc);
    }

    /**
     * Returns a new scratchpad with the strong update applied.
     */
    public Scratchpad strongUpdate(BValue returnValue) {
	return new Scratchpad(returnValue, args, callValues);
    }

    /**
     * Returns a new scratchpad with the strong update applied.
     */
    public Scratchpad strongUpdate(BValue[] args) {
	return new Scratchpad(returnValue, args, callValues);
    }

    /**
     * Returns a new scratchpad with the weak update applied.
     */
    public Scratchpad weakUpdate(BValue returnValue) {
	return this.join(new Scratchpad(returnValue, args, callValues));
    }

    /**
     * Returns a new scratchpad with the weak update applied.
     */
    public Scratchpad weakUpdate(FunctionCall fc, BValue callValue) {
	Map<FunctionCall, BValue> newCallValues = new HashMap<>();
	callValues.forEach(newCallValues::put);
	newCallValues.put(fc, callValue);
	return new Scratchpad(returnValue, args, newCallValues);
    }

    /**
     * Compute the union of this and another Scratchpad.
     * 
     * @param that
     *            The Scratchpad to union.
     * @return The union of the scratchpads.
     */
    public Scratchpad join(Scratchpad that) {

	// Join the return values.
	BValue retVal = this.returnValue.join(that.returnValue);

	// Join the argument values.
	int arglen = this.args.length > that.args.length ? this.args.length : that.args.length;
	BValue[] args = new BValue[arglen];

	for (int i = 0; i < args.length; i++) {
	    if (i >= this.args.length) {
		args[i] = that.args[i];
	    } else if (i >= that.args.length) {
		args[i] = this.args[i];
	    } else {
		args[i] = this.args[i].join(that.args[i]);
	    }
	}

	// Join the function call values.
	Map<FunctionCall, BValue> callVals = new HashMap<FunctionCall, BValue>();

	// Join and add BValues from function calls that match.
	this.callValues.entrySet().stream()
		.filter(entry -> that.callValues.containsKey(entry.getKey()))
		.forEach(entry -> callVals.put(entry.getKey(),
			entry.getValue().join(that.callValues.get(entry.getKey()))));

	// Add BValues from function calls unique to 'this'.
	this.callValues.entrySet().stream()
		.filter(entry -> !that.callValues.containsKey(entry.getKey()))
		.forEach(entry -> callVals.put(entry.getKey(), entry.getValue()));

	// Add BValues from function calls unique to 'that'.
	that.callValues.entrySet().stream()
		.filter(entry -> !this.callValues.containsKey(entry.getKey()))
		.forEach(entry -> callVals.put(entry.getKey(), entry.getValue()));

	return new Scratchpad(retVal, args, callVals);

    }

    /**
     * Returns an empty scratchpad. This should only be called once per analysis, to
     * initialize the scratchpad for the script level analysis.
     */
    public static Scratchpad empty() {
	return new Scratchpad();
    }

    /**
     * Returns a scratchpad initialized with the given argument values.
     */
    public static Scratchpad initialize(BValue args[]) {
	return new Scratchpad(args);
    }

}