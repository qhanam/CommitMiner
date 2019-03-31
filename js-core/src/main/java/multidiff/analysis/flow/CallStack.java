package multidiff.analysis.flow;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Stack;

import multidiffplus.cfg.CfgMap;

/**
 * An abstract call stack.
 */
public class CallStack {

    /** The virtual call stack. */
    private Stack<StackFrame> callStack;

    /**
     * Calls to async functions that are be executed from the event loop (ie. they
     * must be analyzed with a fresh call stack).
     */
    private Queue<StackFrame> eventLoop;

    /**
     * TODO: Add 'asyncFunctions' for functions that will be run from the JavaScript
     * event loop.
     */

    public CallStack(CfgMap cfgMap) {
	this.callStack = new Stack<StackFrame>();
	this.eventLoop = new ArrayDeque<StackFrame>();
    }

    public boolean isEmpty() {
	return callStack.isEmpty();
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

    /**
     * Adds an async function call to the event loop.
     * 
     * The call will be analyzed sometime in the future after the analysis beginning
     * at the entry point has finished.
     */
    public void addAsync(StackFrame asyncStackFrame) {
	eventLoop.add(asyncStackFrame);
    }

    /**
     * Returns {@code true} if there is an async call currently in the event loop.
     */
    public boolean hasAsync() {
	return !eventLoop.isEmpty();
    }

    /**
     * Pops an un-analyzed function from the event loop and adds it as a new frame
     * to the call stack.
     */
    public void runNextAsync() {
	callStack.push(eventLoop.remove());
    }

    public boolean isScriptLevel() {
	return callStack.size() == 1 && callStack.peek().getCFG().isScript();
    }

}
