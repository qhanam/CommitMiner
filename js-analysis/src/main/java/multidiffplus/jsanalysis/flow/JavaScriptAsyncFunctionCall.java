package multidiffplus.jsanalysis.flow;

import multidiff.analysis.flow.AsyncFunctionCall;
import multidiff.analysis.flow.CallStack;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.Control;
import multidiffplus.jsanalysis.abstractdomain.FunctionClosure;
import multidiffplus.jsanalysis.abstractdomain.Scratchpad;
import multidiffplus.jsanalysis.abstractdomain.Store;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * A function which is reachable but was not executed when the analysis passed
 * through the function which declared it.
 * 
 * A {@code ReachableFunction} is run as an AsyncFunction (ie. it is loaded onto
 * a fresh call stack), which is unsound, but less so than failing to analyze
 * the function altogether given the analysis is partial.
 */
public class JavaScriptAsyncFunctionCall implements AsyncFunctionCall {
    public FunctionClosure functionClosure;
    public Address selfAddr;
    public Store store;
    public Trace trace;

    public JavaScriptAsyncFunctionCall(FunctionClosure functionClosure, Address selfAddr,
	    Store store, Trace trace) {
	this.functionClosure = functionClosure;
	this.selfAddr = selfAddr;
	this.store = store;
	this.trace = trace;
    }

    @Override
    public void run(CallStack callStack) {
	if (functionClosure.cfg.getEntryNode().getBeforeState() == null) {
	    Control control = Control.bottom();
	    Scratchpad scratch = Scratchpad.empty();
	    // TODO: Fix this (or remove it?)
	    // functionClosure.run(selfAddr, store, scratch, trace, control, callStack);
	}
    }

}
