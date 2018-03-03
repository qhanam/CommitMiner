package multidiffplus.mining.flow.analysis;

import java.util.LinkedList;
import java.util.List;

import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.factories.IDomainAnalysisFactory;
import multidiffplus.jsanalysis.factories.JavaScriptCFGFactory;
import multidiffplus.mining.astvisitor.ajax.AjaxDataASTAnalysisFactory;

public class CommitAnalysisFactory implements ICommitAnalysisFactory {
	
	private Sensitivity sensitivity;
	
	public CommitAnalysisFactory(Sensitivity sensitivity) {
		this.sensitivity = sensitivity;
	}

	@Override
	public CommitAnalysis newInstance() {


		List<IDomainAnalysisFactory> domainFactories = new LinkedList<IDomainAnalysisFactory>();
		
		switch(sensitivity) {
		case INTRAPROC:
			List<ICFGVisitorFactory> cfgVisitorFactories = new LinkedList<ICFGVisitorFactory>();
			domainFactories.add(new DomainIntraAnalysisFactory(cfgVisitorFactories, new JavaScriptCFGFactory()));
			break;
		case AST:
		default:
			List<IASTVisitorFactory> astVisitorFactories = new LinkedList<IASTVisitorFactory>();
			astVisitorFactories.add(new AjaxDataASTAnalysisFactory());
			domainFactories.add(new DomainASTAnalysisFactory(astVisitorFactories, new JavaScriptCFGFactory()));
		}

		return new CommitAnalysis(domainFactories);
	}
	
	/**
	 * The type of sensitivities the analysis can perform.
	 */
	public enum Sensitivity {
		AST,
		INTRAPROC
	}

}
