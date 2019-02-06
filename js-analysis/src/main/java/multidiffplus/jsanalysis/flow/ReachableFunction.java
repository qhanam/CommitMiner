package multidiffplus.jsanalysis.flow;

import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.FunctionClosure;
import multidiffplus.jsanalysis.abstractdomain.Store;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * A function which is reachable but was not executed when the analysis passed
 * through the function which declared it.
 */
public class ReachableFunction {
    public FunctionClosure functionClosure;
    public Address selfAddr;
    public Store store;
    public Trace trace;

    public ReachableFunction(FunctionClosure functionClosure, Address selfAddr, Store store,
	    Trace trace) {
	this.functionClosure = functionClosure;
	this.selfAddr = selfAddr;
	this.store = store;
	this.trace = trace;
    }
}
