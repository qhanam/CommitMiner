package multidiffplus.jsanalysis.user;

import java.util.List;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.TryStatement;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.IBuiltinState;
import multidiffplus.cfg.IUserState;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Criterion;
import multidiffplus.jsanalysis.flow.JavaScriptAnalysisState;
import multidiffplus.jsanalysis.interpreter.ExpEval;

public class SyncErrorState implements IUserState {

    private SyncErrorState() {
    }

    @Override
    public IUserState interpretStatement(IBuiltinState builtin, ClassifiedASTNode statement) {
	return this;
    }

    @Override
    public IUserState interpretBranchCondition(IBuiltinState builtin, CFGEdge edge) {
	return this;
    }

    @Override
    public IUserState interpretCallSite(IBuiltinState builtin, ClassifiedASTNode callSite,
	    List<IUserState> functionExitStates) {

	JavaScriptAnalysisState jsBuiltin = (JavaScriptAnalysisState) builtin;

	// We generate a warning under the following conditions:
	// (1) The call site is INSERTED, or the target is INSERTED or UPDATED.
	// (2) The target is an API call that is on our white list of throwable
	// functions.
	FunctionCall fc = (FunctionCall) callSite;

	if (!(Change.test(fc) || Change.testU(fc.getTarget()))) {
	    // The call site is not new and the target has not changed.
	    return this;
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

	// Is the criterion an API the checker targets?
	for (Criterion criterion : targetObject.deps.getDependencies()) {
	    if (criterion.getType() == Criterion.Type.VALUE
		    && criterion.getNode().toSource().equals("require('fs')")
		    && funct.toSource().equals("readFileSync") && !isProtected(fc)) {
		// This new call site is not protected. Create a warning.
		Criterion api = Criterion.of(criterion.getNode(), Criterion.Type.SYNC_ERROR_API);
		Criterion fun = Criterion.of(funct, Criterion.Type.SYNC_ERROR_FUNCTION);
		fc.addDependency(api.getType().toString(), api.getId());
		fc.addDependency(fun.getType().toString(), fun.getId());
	    }
	}

	return this;
    }

    /**
     * Returns {@code true} if the node is protected by a
     * 
     * @param node
     * @return
     */
    private boolean isProtected(FunctionCall fc) {
	AstNode ancestor = fc.getParent();
	while (!(ancestor instanceof ScriptNode)) {
	    if (ancestor instanceof TryStatement) {
		return true;
	    }
	    ancestor = ancestor.getParent();
	}
	return false;
    }

    @Override
    public IUserState initializeCallback(IBuiltinState builtin, ClassifiedASTNode callSite,
	    CFG function) {
	return this;
    }

    @Override
    public IUserState join(IUserState that) {
	return this;
    }

    @Override
    public boolean equivalentTo(IUserState that) {
	return true;
    }

    /**
     * Creates an initial SyncErrorState for a script.
     * 
     * This should only be called once per analysis. All other states should be
     * created by either interpreting statements or joining two states.
     */
    public static SyncErrorState initializeScriptState() {
	return new SyncErrorState();
    }

}
