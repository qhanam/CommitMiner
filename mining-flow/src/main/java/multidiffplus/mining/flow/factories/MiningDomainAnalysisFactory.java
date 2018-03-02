package multidiffplus.mining.flow.factories;

import java.util.List;

import multidiffplus.analysis.DomainAnalysis;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.factories.ICFGFactory;
import multidiffplus.factories.IDomainAnalysisFactory;
import multidiffplus.mining.flow.analysis.MiningASTDomainAnalysis;

public class MiningDomainAnalysisFactory implements IDomainAnalysisFactory {

	private List<IASTVisitorFactory> astVisitorFactories;
	private ICFGFactory cfgFactory;

	public MiningDomainAnalysisFactory(List<IASTVisitorFactory> astVisitorFactories, ICFGFactory cfgFactory) {
		this.astVisitorFactories = astVisitorFactories;
		this.cfgFactory = cfgFactory;
	}

	@Override
	public DomainAnalysis newInstance() {
		return new MiningASTDomainAnalysis(astVisitorFactories, cfgFactory);
	}

}
