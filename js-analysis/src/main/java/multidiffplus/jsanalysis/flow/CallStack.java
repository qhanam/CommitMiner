package multidiffplus.jsanalysis.flow;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import org.mozilla.javascript.ast.AstNode;

import multidiffplus.cfg.CFG;

/**
 * An abstract call stack.
 */
public class CallStack {

    /** The CFGs of every function. */
    private Map<AstNode, CFG> cfgs;

    /** The virtual call stack. */
    private Stack<StackFrame> callStack;

    /** Reachable functions that have not been analyzed. */
    private Queue<ReachableFunction> reachables;

    public CallStack(Map<AstNode, CFG> cfgs) {
	this.cfgs = cfgs;
	this.callStack = new Stack<StackFrame>();
	this.reachables = new ArrayDeque<ReachableFunction>();
    }

    public boolean isEmpty() {
	return callStack.isEmpty();
    }

    public Map<AstNode, CFG> getCFGs() {
	return cfgs;
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
