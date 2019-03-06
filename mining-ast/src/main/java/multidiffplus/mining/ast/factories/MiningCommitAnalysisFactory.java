package multidiffplus.mining.ast.factories;

import java.util.LinkedList;
import java.util.List;

import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.factories.IDomainAnalysisFactory;
import multidiffplus.jsanalysis.cfg.JavaScriptCFGFactory;
import multidiffplus.mining.ast.analysis.criterion.trycallsite.TryCallsiteAstAnalysisFactory;

public class MiningCommitAnalysisFactory implements ICommitAnalysisFactory {

    @Override
    public CommitAnalysis newInstance() {

	List<IASTVisitorFactory> srcVisitorFactories = new LinkedList<IASTVisitorFactory>();
	srcVisitorFactories.add(new ModifiedStatementASTAnalysisFactory());

	List<IASTVisitorFactory> dstVisitorFactories = new LinkedList<IASTVisitorFactory>();
	// dstVisitorFactories.add(new ModifiedStatementASTAnalysisFactory());
	// dstVisitorFactories.add(new AjaxDataASTAnalysisFactory());
	dstVisitorFactories.add(new TryCallsiteAstAnalysisFactory());

	List<IDomainAnalysisFactory> domainFactories = new LinkedList<IDomainAnalysisFactory>();
	domainFactories.add(new MiningDomainAnalysisFactory(srcVisitorFactories,
		dstVisitorFactories, new JavaScriptCFGFactory()));

	return new CommitAnalysis(domainFactories);
    }

}
