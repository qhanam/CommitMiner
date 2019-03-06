package multidiff.js.factories;

import multidiffplus.analysis.DomainAnalysis;
import multidiffplus.factories.IDomainAnalysisFactory;
import multidiffplus.jsanalysis.cfg.JavaScriptCFGFactory;
import multidiffplus.jsdiff.analysis.ChangeImpactDomainAnalysis;

public class ChangeImpactDomainAnalysisFactory implements IDomainAnalysisFactory {

    @Override
    public DomainAnalysis newInstance() {
	return new ChangeImpactDomainAnalysis(new JavaScriptCFGFactory(), /* preProcess= */true,
		/* measureRuntime= */true);
    }

}
