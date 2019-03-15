package multidiffplus.jsanalysis.flow;

import org.mozilla.javascript.ast.AstRoot;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiff.analysis.flow.Analysis;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.IState;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.jsanalysis.interpreter.Helpers;

public class JavaScriptAnalysis extends Analysis {

    JavaScriptAnalysis(CFG entryPoint, CfgMap cfgMap, IState initialState) {
	super(entryPoint, cfgMap, initialState);
    }

    @Override
    protected void addReachableFunctions() {
	Helpers.findReachableFunctions(callStack);
    }

    @Override
    public void registerAnnotations(ClassifiedASTNode root, AnnotationFactBase annotationFactBase) {
	AnnotationVisitor.registerAnnotations((AstRoot) root, annotationFactBase);
    }

    public static JavaScriptAnalysis InitializeJavaScriptAnalysis(ClassifiedASTNode root) {
	JavaScriptCFGFactory cfgFactory = new JavaScriptCFGFactory();
	CfgMap cfgMap = cfgFactory.createCFGs(root);
	IState initialState = JavaScriptAnalysisState.initializeScriptState(root, cfgMap);
	CFG entryPoint = cfgMap.getCfgFor(root);
	return new JavaScriptAnalysis(entryPoint, cfgMap, initialState);
    }

}
