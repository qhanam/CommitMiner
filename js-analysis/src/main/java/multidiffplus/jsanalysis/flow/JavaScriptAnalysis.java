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
import multidiffplus.cfg.IState;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.jsanalysis.interpreter.Helpers;

public class JavaScriptAnalysis extends Analysis {

    CfgMap cfgs;

    JavaScriptAnalysis(CFG entryPoint, CfgMap cfgMap, AnalysisState initialState) {
	super(entryPoint, cfgMap, initialState);
	this.cfgs = cfgMap;
    }

    @Override
    protected void addReachableFunctions() {
	Set<Pair<CFG, IState>> reachables = Helpers.findReachableFunctions(callStack, cfgs);
	for (Pair<CFG, IState> reachable : reachables) {
	    AnalysisState initialState = AnalysisState.initializeAnalysisState(reachable.getValue(),
		    new IState[0]);
	    callStack.addAsync(new StackFrame(reachable.getKey(), initialState));
	}
    }

    @Override
    public void registerAnnotations(ClassifiedASTNode root, AnnotationFactBase annotationFactBase) {
	AnnotationVisitor.registerAnnotations((AstRoot) root, annotationFactBase);
    }

    public static JavaScriptAnalysis InitializeJavaScriptAnalysis(ClassifiedASTNode root) {
	JavaScriptCFGFactory cfgFactory = new JavaScriptCFGFactory();
	CfgMap cfgMap = cfgFactory.createCFGs(root);
	AnalysisState initialState = JavaScriptAnalysisState.initializeScriptState(root, cfgMap,
		new IState[0]);
	CFG entryPoint = cfgMap.getCfgFor(root);
	return new JavaScriptAnalysis(entryPoint, cfgMap, initialState);
    }

}
