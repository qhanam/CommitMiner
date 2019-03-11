package multidiffplus.jsanalysis.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ScriptNode;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFG;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.initstate.StateFactory;
import multidiffplus.jsanalysis.interpreter.Helpers;

/**
 * A flow analysis of a JavaScript file.
 */
public class Analysis {

    /**
     * The virtual call stack.
     */
    private CallStack callStack;

    /**
     * Run the analysis.
     */
    public void run() {
	while (!callStack.isEmpty()) {
	    StackFrame stackFrame = callStack.peek();
	    if (stackFrame.hasInstruction()) {
		stackFrame.peekInstruction().transfer(callStack);
	    } else {
		Helpers.findReachableFunctions(callStack);
		callStack.pop();
	    }
	}
    }

    /**
     * 
     * @return {@code true} when a new reachable function frame has been added to
     *         the call stack.
     */
    public boolean pushReachableFunction() {
	if (!callStack.hasReachableFunction())
	    return false;
	Helpers.runNextReachable(callStack);
	return true;
    }

    /**
     * Returns an initialized, but un-executed, analysis for the script.
     */
    public static Analysis build(ClassifiedASTNode root, List<CFG> cfgs) {

	/* Build a map of AstNodes to CFGs. Used for inter-proc CFA. */
	Map<AstNode, CFG> cfgMap = new HashMap<AstNode, CFG>();
	for (CFG cfg : cfgs) {
	    cfgMap.put((AstNode) cfg.getEntryNode().getStatement(), cfg);
	}

	/* Setup the analysis with the root script and an initial state. */
	State state = StateFactory.createInitialState((ScriptNode) root, cfgMap);
	return new Analysis(new StackFrame(cfgMap.get(root), state), cfgMap);

    }

    /**
     * @param cfgs
     *            The CFGs of every function.
     * @param initialState
     *            The state before the program is run.
     */
    private Analysis(StackFrame initialState, Map<AstNode, CFG> cfgs) {
	this.callStack = new CallStack(cfgs);
	this.callStack.push(initialState);
    }

}
