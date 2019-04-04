package multidiffplus.jsanalysis.flow;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.ast.AstRoot;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiff.analysis.flow.Analysis;
import multidiff.analysis.flow.StackFrame;
import multidiffplus.cfg.AnalysisState;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.IBuiltinState;
import multidiffplus.cfg.IUserState;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.jsanalysis.interpreter.Helpers;
import multidiffplus.jsanalysis.user.AsyncErrorState;
import multidiffplus.jsanalysis.user.SyncErrorApiSpecState;
import multidiffplus.jsanalysis.user.SyncErrorState;

public class JavaScriptAnalysis extends Analysis {

    CfgMap cfgs;

    JavaScriptAnalysis(CFG entryPoint, CfgMap cfgMap, AnalysisState initialState) {
	super(entryPoint, cfgMap, initialState);
	this.cfgs = cfgMap;
    }

    @Override
    protected void addReachableFunctions() {
	Set<Pair<CFG, IBuiltinState>> reachables = Helpers.findReachableFunctions(callStack, cfgs);
	for (Pair<CFG, IBuiltinState> reachable : reachables) {
	    AnalysisState initialState = AnalysisState.initializeAnalysisState(reachable.getValue(),
		    getUserStates(reachable.getKey().getEntryNode().getStatement(), cfgs));
	    callStack.addAsync(new StackFrame(reachable.getKey(), initialState));
	}
    }

    @Override
    public void registerAnnotations(ClassifiedASTNode root, AnnotationFactBase annotationFactBase) {
	AnnotationVisitor.registerAnnotations((AstRoot) root, annotationFactBase);
    }

    private static IUserState[] getUserStates(ClassifiedASTNode root, CfgMap cfgs) {
	IUserState[] userStates = new IUserState[3];
	userStates[0] = SyncErrorState.initializeScriptState();
	userStates[1] = SyncErrorApiSpecState.initializeScriptState();
	userStates[2] = AsyncErrorState.initializeScriptState();
	return userStates;
    }

    public static JavaScriptAnalysis InitializeJavaScriptAnalysis(ClassifiedASTNode root) {
	JavaScriptCFGFactory cfgFactory = new JavaScriptCFGFactory();
	CfgMap cfgMap = cfgFactory.createCFGs(root);
	AnalysisState initialState = JavaScriptAnalysisState.initializeScriptState(root, cfgMap,
		getUserStates(root, cfgMap));
	CFG entryPoint = cfgMap.getCfgFor(root);
	return new JavaScriptAnalysis(entryPoint, cfgMap, initialState);
    }

}
