package multidiffplus.jsanalysis.flow;

import java.util.Map;
import java.util.Stack;

import org.mozilla.javascript.ast.AstNode;

import multidiffplus.cfg.CFG;

/**
 * The state of the control flow.
 */
public class CallStack {

    /** The CFGs of every function. */
    public Map<AstNode, CFG> cfgs;

    /** The virtual call stack. */
    public Stack<StackFrame> callStack;

    public CallStack(Map<AstNode, CFG> cfgs) {
	this.cfgs = cfgs;
	this.callStack = new Stack<StackFrame>();
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

}
