package multidiff.js.factories;

import java.util.LinkedList;
import java.util.List;

import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.factories.IDomainAnalysisFactory;

/**
 * Use for creating a diff commit analysis.
 */
public class ChangeImpactCommitAnalysisFactory implements ICommitAnalysisFactory {

    public ChangeImpactCommitAnalysisFactory() {
    }

    @Override
    public CommitAnalysis newInstance() {
	List<IDomainAnalysisFactory> domainFactories = new LinkedList<IDomainAnalysisFactory>();
	domainFactories.add(new ChangeImpactDomainAnalysisFactory());
	return new CommitAnalysis(domainFactories);
    }

}