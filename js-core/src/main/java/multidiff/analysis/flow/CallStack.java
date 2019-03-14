package multidiff.analysis.flow;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Stack;

import multidiffplus.cfg.CfgMap;

/**
 * An abstract call stack.
 */
public class CallStack {

    /** The CFGs of every function. */
    private CfgMap cfgMap;

    /** The virtual call stack. */
    private Stack<StackFrame> callStack;

    /** Reachable functions that have not been analyzed. */
    private Queue<ReachableFunction> reachables;

    /**
     * TODO: Add 'asyncFunctions' for functions that will be run from the JavaScript
     * event loop.
     */

    public CallStack(CfgMap cfgMap) {
	// TODO: Separate the CFG map from the CallStack. They aren't related.
	this.cfgMap = cfgMap;
	this.callStack = new Stack<StackFrame>();
	this.reachables = new ArrayDeque<ReachableFunction>();
    }

    public boolean isEmpty() {
	return callStack.isEmpty();
    }

    public CfgMap getCfgMap() {
	return cfgMap;
    }

    public void push(StackFrame stackFrame) {
	callStack.push(stackFrame);
    }

    public StackFrame peek() {
	return callStack.peek();
    }

    public StackFrame pop() {
	return callStack.pop();
    }

    public void addReachableFunction(ReachableFunction rf) {
	reachables.add(rf);
    }

    public boolean hasReachableFunction() {
	return !reachables.isEmpty();
    }

    public ReachableFunction removeReachableFunction() {
	return reachables.poll();
    }

    public boolean isScriptLevel() {
	return callStack.size() == 1 && callStack.peek().getCFG().isScript();
    }

}
