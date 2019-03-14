package multidiff.analysis.flow;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.IState;

/**
 * A utility which runs an inter-procedural, flow sensitive, context insensitive
 * analysis of a single version of a file.
 */
public abstract class Analysis {

    /**
     * The virtual call stack.
     */
    private CallStack callStack;

    /**
     * @param entryPoint
     *            The CFG for the entry point of the program.
     * @param cfgs
     *            The map of function -> cfg.
     * @param initialState
     */
    public Analysis(CFG entryPoint, CfgMap cfgMap, IState initialState) {
	this.callStack = new CallStack(cfgMap);
	this.callStack.push(new StackFrame(entryPoint, initialState));
    }

    /**
     * Runs an analysis.
     */
    public void run() {
	while (!callStack.isEmpty()) {
	    StackFrame stackFrame = callStack.peek();
	    if (stackFrame.hasInstruction()) {
		stackFrame.peekInstruction().transfer(callStack);
	    } else {
		// Helpers.findReachableFunctions(callStack);
		addReachableFunctions();
		callStack.pop();
	    }
	}
    }

    /**
     * Adds un-analyzed functions to the event loop which are reachable from the
     * function's local scope.
     */
    protected abstract void addReachableFunctions();

    /**
     * 
     * @return {@code true} when a new reachable function frame has been added to
     *         the call stack.
     */
    public boolean pushReachableFunction() {
	if (!callStack.hasReachableFunction())
	    return false;
	// Helpers.runNextReachable(callStack);
	runNextReachableFunction();
	return true;
    }

    /**
     * Pops an un-analyzed function from the event loop and adds it as a new frame
     * to the call stack.
     */
    protected abstract void runNextReachableFunction();

    /**
     * Creates criterion/dependency annotations for GUI output.
     * 
     * @param root
     *            The root of the entry point's AST.
     */
    protected abstract void registerAnnotations(ClassifiedASTNode root);

}
