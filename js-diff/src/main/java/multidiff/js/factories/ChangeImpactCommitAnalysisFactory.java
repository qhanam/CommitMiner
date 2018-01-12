package multidiff.js.factories;

import java.util.LinkedList;
import java.util.List;

import multidiff.jsdiff.defvalue.DefValueCFGVisitorFactory;
import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.factories.IDomainAnalysisFactory;
import multidiffplus.jsdiff.controlcall.ControlCallCFGVisitorFactory;
import multidiffplus.jsdiff.controlcondition.ControlConditionCFGVisitorFactory;
import multidiffplus.jsdiff.defenvironment.DefEnvCFGVisitorFactory;
import multidiffplus.jsdiff.env.EnvCFGVisitorFactory;
import multidiffplus.jsdiff.value.ValueCFGVisitorFactory;

/**
 * Use for creating a diff commit analysis.
 */
public class ChangeImpactCommitAnalysisFactory implements ICommitAnalysisFactory {

	public ChangeImpactCommitAnalysisFactory() { }

	@Override
	public CommitAnalysis newInstance() {
		List<ICFGVisitorFactory> cfgVisitorFactories = new LinkedList<ICFGVisitorFactory>();
		cfgVisitorFactories.add(new ControlCallCFGVisitorFactory());
		cfgVisitorFactories.add(new ControlConditionCFGVisitorFactory());
		cfgVisitorFactories.add(new EnvCFGVisitorFactory());
		cfgVisitorFactories.add(new ValueCFGVisitorFactory());
		cfgVisitorFactories.add(new DefValueCFGVisitorFactory());
		cfgVisitorFactories.add(new DefEnvCFGVisitorFactory());

		List<IDomainAnalysisFactory> domainFactories = new LinkedList<IDomainAnalysisFactory>();
		domainFactories.add(new ChangeImpactDomainAnalysisFactory(cfgVisitorFactories));
		return new CommitAnalysis(domainFactories);
	}

}