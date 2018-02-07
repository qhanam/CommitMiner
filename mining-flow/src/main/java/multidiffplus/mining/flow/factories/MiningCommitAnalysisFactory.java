package multidiffplus.mining.flow.factories;

import java.util.LinkedList;
import java.util.List;

import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.factories.IDomainAnalysisFactory;
import multidiffplus.jsanalysis.factories.JavaScriptCFGFactory;

public class MiningCommitAnalysisFactory implements ICommitAnalysisFactory {

	@Override
	public CommitAnalysis newInstance() {

		List<IASTVisitorFactory> astVisitorFactories = new LinkedList<IASTVisitorFactory>();
		astVisitorFactories.add(new AjaxDataASTAnalysisFactory());

		List<IDomainAnalysisFactory> domainFactories = new LinkedList<IDomainAnalysisFactory>();
		domainFactories.add(new MiningDomainAnalysisFactory(astVisitorFactories, new JavaScriptCFGFactory()));

		return new CommitAnalysis(domainFactories);
	}

}
