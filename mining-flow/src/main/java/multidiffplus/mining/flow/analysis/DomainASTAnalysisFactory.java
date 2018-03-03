package multidiffplus.mining.flow.analysis;

import java.util.List;

import multidiffplus.analysis.DomainAnalysis;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.factories.ICFGFactory;
import multidiffplus.factories.IDomainAnalysisFactory;

public class DomainASTAnalysisFactory implements IDomainAnalysisFactory {

	private List<IASTVisitorFactory> astVisitorFactories;
	private ICFGFactory cfgFactory;

	public DomainASTAnalysisFactory(List<IASTVisitorFactory> astVisitorFactories, ICFGFactory cfgFactory) {
		this.astVisitorFactories = astVisitorFactories;
		this.cfgFactory = cfgFactory;
	}

	@Override
	public DomainAnalysis newInstance() {
		return new MiningASTDomainAnalysis(astVisitorFactories, cfgFactory);
	}

}
