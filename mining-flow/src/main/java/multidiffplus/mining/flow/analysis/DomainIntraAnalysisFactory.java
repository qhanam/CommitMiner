package multidiffplus.mining.flow.analysis;

import java.util.List;

import multidiffplus.analysis.DomainAnalysis;
import multidiffplus.factories.ICFGFactory;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.factories.IDomainAnalysisFactory;

public class DomainIntraAnalysisFactory implements IDomainAnalysisFactory {

	private List<ICFGVisitorFactory> cfgVisitorFactories;
	private ICFGFactory cfgFactory;

	public DomainIntraAnalysisFactory(List<ICFGVisitorFactory> cfgVisitorFactories, ICFGFactory cfgFactory) {
		this.cfgVisitorFactories = cfgVisitorFactories;
		this.cfgFactory = cfgFactory;
	}

	@Override
	public DomainAnalysis newInstance() {
		return new MiningIntraprocDomainAnalysis(cfgVisitorFactories, cfgFactory);
	}

}
