package multidiffplus.jsanalysis.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.IBuiltinState;
import multidiffplus.cfg.IUserState;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Criterion;
import multidiffplus.jsanalysis.abstractdomain.Str;
import multidiffplus.jsanalysis.flow.JavaScriptAnalysisState;
import multidiffplus.jsanalysis.interpreter.ExpEval;

public class AsyncErrorState implements IUserState {

    /**
     * Tracks the async call sites that ran the callback function.
     */
    private Map<FunctionCall, Criterion> asyncCallsites;

    private AsyncErrorState() {
	this.asyncCallsites = new HashMap<>();
    }

    private AsyncErrorState(FunctionCall fc) {
	this.asyncCallsites = new HashMap<>();
	this.asyncCallsites.put(fc, Criterion.of(fc, Criterion.Type.ASYNC_ERROR));
    }

    private AsyncErrorState(Map<FunctionCall, Criterion> asyncCallsites) {
	this.asyncCallsites = asyncCallsites;
    }

    @Override
    public IUserState interpretStatement(IBuiltinState builtin, ClassifiedASTNode statement) {
	if (asyncCallsites.isEmpty() || statement instanceof FunctionNode) {
	    // We are not inside an async callsite.
	    return this;
	}

	// Look up the BValue of the parameter e === null || e !== null
	AstNode node = (AstNode) statement;
	ExpEval eval = ((JavaScriptAnalysisState) builtin).getExpressionEvaluator();
	BValue val = eval.resolveValue(new Name(0, "e"));
	if (val == null) {
	    return this;
	}

	// Check if the parameter is non-empty.
	if (!(val.stringAD.le == Str.LatticeElement.SBLANK
		|| val.stringAD.le == Str.LatticeElement.BOTTOM)) {
	    // The error parameter may be non-null.
	    for (Criterion criterion : asyncCallsites.values()) {
		node.addDependency(criterion.getType().toString(), criterion.getId());
	    }
	}

	return this;
    }

    @Override
    public IUserState interpretBranchCondition(IBuiltinState builtin, CFGEdge edge) {
	return this;
    }

    @Override
    public IUserState interpretCallSite(IBuiltinState builtin, ClassifiedASTNode callSite,
	    List<IUserState> functionExitStates) {
	return this;
    }

    @Override
    public IUserState initializeCallback(IBuiltinState builtin, ClassifiedASTNode callSite,
	    CFG function) {
	FunctionCall fc = (FunctionCall) callSite;
	// TODO: Resolve the target and callback function to see if either have changed.
	// TODO: Identify the name of the callback parameter (ie. the first parameter).
	if (Change.test(fc) || Change.testU(fc.getTarget())) {
	    // The call site is new, or the target has changed.
	    return new AsyncErrorState(fc);
	} else {
	    // The call site is not impacted by the change.
	    return new AsyncErrorState();
	}
    }

    @Override
    public IUserState join(IUserState that) {
	Map<FunctionCall, Criterion> newAsyncCallsites = new HashMap<>();
	newAsyncCallsites.putAll(this.asyncCallsites);
	newAsyncCallsites.putAll(((AsyncErrorState) that).asyncCallsites);
	return new AsyncErrorState(newAsyncCallsites);
    }

    @Override
    public boolean equivalentTo(IUserState that) {
	if (!this.asyncCallsites.keySet()
		.containsAll(((AsyncErrorState) that).asyncCallsites.keySet())) {
	    // that contains elements that this does not contain.
	    return false;
	}
	if (!((AsyncErrorState) that).asyncCallsites.keySet()
		.containsAll(this.asyncCallsites.keySet())) {
	    // this contains elements that that does not contain.
	    return false;
	}
	return true;
    }

    /**
     * Creates an initial SyncErrorState for a script.
     * 
     * This should only be called once per analysis. All other states should be
     * created by either interpreting statements or joining two states.
     */
    public static AsyncErrorState initializeScriptState() {
	return new AsyncErrorState();
    }

}
