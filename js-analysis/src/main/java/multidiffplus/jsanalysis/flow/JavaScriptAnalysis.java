package multidiffplus.jsanalysis.flow;

import org.mozilla.javascript.ast.AstRoot;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiff.analysis.flow.Analysis;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.IState;

public class JavaScriptAnalysis extends Analysis {

    JavaScriptAnalysis(CFG entryPoint, CfgMap cfgMap, IState initialState) {
	super(entryPoint, cfgMap, initialState);
    }

    @Override
    protected void addReachableFunctions() {
	// TODO Auto-generated method stub

    }

    @Override
    protected void runNextReachableFunction() {
	// TODO Auto-generated method stub

    }

    @Override
    protected void registerAnnotations(ClassifiedASTNode root) {
	// TODO Auto-generated method stub

    }

    public static JavaScriptAnalysis InitializeJavaScriptAnalysis(AstRoot root) {
	JavaScriptCFGFactory cfgFactory = new JavaScriptCFGFactory();
	CfgMap cfgMap = cfgFactory.createCFGs(root);
	IState initialState = JavaScriptAnalysisState.initializeAnalysisStateFrom(root, cfgMap);
	CFG entryPoint = cfgMap.getCfgFor(root);
	return new JavaScriptAnalysis(entryPoint, cfgMap, initialState);
    }

}
