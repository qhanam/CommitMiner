package multidiff.js.factories;

import java.util.List;

import multidiffplus.analysis.DomainAnalysis;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.factories.IDomainAnalysisFactory;
import multidiffplus.jsanalysis.factories.JavaScriptCFGFactory;
import multidiffplus.jsdiff.analysis.ChangeImpactDomainAnalysis;

public class ChangeImpactDomainAnalysisFactory implements IDomainAnalysisFactory {

	public List<ICFGVisitorFactory> cfgVisitorFactories;

	public ChangeImpactDomainAnalysisFactory(List<ICFGVisitorFactory> cfgVisitorFactories) {
		this.cfgVisitorFactories = cfgVisitorFactories;
	}

	@Override
	public DomainAnalysis newInstance() {
		return new ChangeImpactDomainAnalysis(cfgVisitorFactories, 
				new JavaScriptCFGFactory(), 
				/*preProcess=*/true, 
				/*measureRuntime=*/true);
	}

}
