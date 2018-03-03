package multidiffplus.mining.flow.factories;

import java.util.List;

import multidiffplus.analysis.DomainAnalysis;
import multidiffplus.factories.ICFGFactory;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.factories.IDomainAnalysisFactory;
import multidiffplus.mining.flow.analysis.MiningIntraprocDomainAnalysis;

public class MiningDomainIntraAnalysisFactory implements IDomainAnalysisFactory {

	private List<ICFGVisitorFactory> cfgVisitorFactories;
	private ICFGFactory cfgFactory;

	public MiningDomainIntraAnalysisFactory(List<ICFGVisitorFactory> cfgVisitorFactories, ICFGFactory cfgFactory) {
		this.cfgVisitorFactories = cfgVisitorFactories;
		this.cfgFactory = cfgFactory;
	}

	@Override
	public DomainAnalysis newInstance() {
		return new MiningIntraprocDomainAnalysis(cfgVisitorFactories, cfgFactory);
	}

}
