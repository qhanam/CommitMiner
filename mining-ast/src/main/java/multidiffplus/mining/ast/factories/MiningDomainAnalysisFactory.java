package multidiffplus.mining.ast.factories;

import java.util.List;

import multidiffplus.analysis.DomainAnalysis;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.factories.ICFGFactory;
import multidiffplus.factories.IDomainAnalysisFactory;
import multidiffplus.mining.ast.analysis.MiningDomainAnalysis;

public class MiningDomainAnalysisFactory implements IDomainAnalysisFactory {

	private List<IASTVisitorFactory> srcVisitorFactories;
	private List<IASTVisitorFactory> dstVisitorFactories;
	private ICFGFactory cfgFactory;

	public MiningDomainAnalysisFactory(List<IASTVisitorFactory> srcVisitorFactories, List<IASTVisitorFactory> dstVisitorFactories, ICFGFactory cfgFactory) {
		this.srcVisitorFactories = srcVisitorFactories;
		this.dstVisitorFactories = dstVisitorFactories;
		this.cfgFactory = cfgFactory;
	}

	@Override
	public DomainAnalysis newInstance() {
		return new MiningDomainAnalysis(srcVisitorFactories, dstVisitorFactories, cfgFactory);
	}

}
