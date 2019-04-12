package multidiffplus.jsanalysis.user;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.InfixExpression;
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
    private Set<FunctionCall> asyncCallsites;

    /**
     * The error from the asynchronous function.
     */
    private Name error;

    /**
     * The return values from the asynchronous function.
     */
    private Set<Name> returnValues;

    /**
     * The async APIs that trigger the callback.
     */
    private Set<AstNode> apis;

    private AsyncErrorState() {
	this.asyncCallsites = new HashSet<>();
	this.error = null;
	this.returnValues = new HashSet<>();
	this.apis = new HashSet<>();
    }

    private AsyncErrorState(FunctionCall fc, Name error, Set<Name> returnValues,
	    Set<AstNode> apis) {
	this.asyncCallsites = new HashSet<>();
	this.asyncCallsites.add(fc);
	this.error = error;
	this.returnValues = returnValues;
	this.apis = apis;
    }

    private AsyncErrorState(Set<FunctionCall> asyncCallsites, Name error, Set<Name> returnValues,
	    Set<AstNode> apis) {
	this.asyncCallsites = asyncCallsites;
	this.error = error;
	this.returnValues = returnValues;
	this.apis = apis;
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
	BValue val = eval.resolveValue(error);

	if (val.stringAD.le == Str.LatticeElement.SBLANK
		|| val.stringAD.le == Str.LatticeElement.BOTTOM) {
	    // The error parameter is empty, so this statement is safe.
	    return this;
	}

	Set<Name> usedParams = ParamUseVisitor.findUsedParams(node, returnValues);
	if (usedParams.isEmpty()) {
	    // No parameters are used within the statement.
	    return this;
	}

	// The error parameter may be non-null.
	Criterion criterion = Criterion.of(node, Criterion.Type.ASYNC_ERROR);
	asyncCallsites.forEach(asyncCallsite -> asyncCallsite
		.addDependency(criterion.getType().toString(), criterion.getId()));
	usedParams.forEach(usedParam -> usedParam.addDependency(criterion.getType().toString(),
		criterion.getId()));
	error.addDependency(criterion.getType().toString(), criterion.getId());
	apis.forEach(api -> api.addDependency(criterion.getType().toString(), criterion.getId()));

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

	JavaScriptAnalysisState jsBuiltin = (JavaScriptAnalysisState) builtin;

	FunctionCall fc = (FunctionCall) callSite;

	// TODO: Resolve the target and callback function to see if either have changed.

	if (!(Change.test(fc) || Change.testU(fc.getTarget()))) {
	    // The call site is not new and the target has not changed.
	    return new AsyncErrorState();
	}

	if (!(fc.getTarget() instanceof InfixExpression)
		|| !(((InfixExpression) fc.getTarget()).getRight() instanceof Name)) {
	    // The target is not in the format 'API.function()'
	    return this;
	}

	// Resolve the object where the function is defined.
	InfixExpression target = (InfixExpression) fc.getTarget();
	Name funct = (Name) target.getRight();
	ExpEval eval = jsBuiltin.getExpressionEvaluator();
	BValue targetObject = eval.eval(target.getLeft());

	if (targetObject == null) {
	    // Could not evaluate target.
	    return new AsyncErrorState();
	}

	// Is the criterion an API the checker targets?
	Set<AstNode> apis = new HashSet<>();
	for (Criterion criterion : targetObject.deps.getDependencies()) {
	    if (criterion.getType() == Criterion.Type.VALUE
		    && criterion.getNode().toSource().equals("require('fs')")
		    && funct.toSource().equals("readFile")) {
		apis.add(criterion.getNode());
	    }
	}

	if (apis.isEmpty()) {
	    // This is not a call site of interest.
	    return new AsyncErrorState();
	}

	FunctionNode f = (FunctionNode) function.getEntryNode().getStatement();
	Name error = null;
	Set<Name> params = new HashSet<>();
	int i = 0;
	for (AstNode param : f.getParams()) {
	    if (i == 0) {
		error = (Name) param;
	    } else {
		params.add((Name) param);
	    }
	    i++;
	}
	return new AsyncErrorState(fc, error, params, apis);
    }

    @Override
    public IUserState join(IUserState that) {
	Set<FunctionCall> newAsyncCallsites = new HashSet<>();
	newAsyncCallsites.addAll(this.asyncCallsites);
	newAsyncCallsites.addAll(((AsyncErrorState) that).asyncCallsites);
	return new AsyncErrorState(newAsyncCallsites, error, returnValues, apis);
    }

    @Override
    public boolean equivalentTo(IUserState that) {
	if (!this.asyncCallsites.containsAll(((AsyncErrorState) that).asyncCallsites)) {
	    // that contains elements that this does not contain.
	    return false;
	}
	if (!((AsyncErrorState) that).asyncCallsites.containsAll(this.asyncCallsites)) {
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
