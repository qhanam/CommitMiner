package multidiff.analysis.flow;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.IState;
import multidiffplus.facts.AnnotationFactBase;

/**
 * A utility which runs an inter-procedural, flow sensitive, context insensitive
 * analysis of a single version of a file.
 */
public abstract class Analysis {

    /**
     * The virtual call stack.
     */
    protected CallStack callStack;

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
	if (!callStack.hasAsync())
	    return false;
	callStack.runNextAsync();
	return true;
    }

    /**
     * Creates criterion/dependency annotations for GUI output.
     * 
     * @param root
     *            The root of the entry point's AST.
     */
    public abstract void registerAnnotations(ClassifiedASTNode root,
	    AnnotationFactBase annotationFactBase);

}
