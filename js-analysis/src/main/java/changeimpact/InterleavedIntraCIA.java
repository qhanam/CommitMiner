package changeimpact;

import java.util.List;

import multidiffplus.analysis.Options;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGNode;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.diff.DiffContext;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.jsanalysis.flow.Analysis;

/**
 * Performs an intra-procedural analysis of each function in a script (file).
 * Synchronizes the change impact analysis of the original and new files.
 */
public class InterleavedIntraCIA {

	List<ICFGVisitorFactory> cfgVisitorFactories;
	SourceCodeFileChange sourceCodeFileChange;
	DiffContext diffContext;
	
	Analysis srcAnalysis;
	Analysis dstAnalysis;

	public InterleavedIntraCIA(List<ICFGVisitorFactory> cfgVisitorFactories, 
			SourceCodeFileChange sourceCodeFileChange, DiffContext diffContext) {
		this.cfgVisitorFactories = cfgVisitorFactories;
		this.sourceCodeFileChange = sourceCodeFileChange;
		this.diffContext = diffContext;
		
		/* Make sure the intra-proc analysis option is turned on so that
		* functions are never added to the stack. */
		Options.createInstance(Options.Sensitivity.INTRAPROC);
		
		/* Build and run an analysis for each source function. */
		for(CFG cfg : diffContext.srcCFGs ) {
			CFGNode entry = cfg.getEntryNode();
			this.srcAnalysis = Analysis.build(entry.getStatement(), diffContext.srcCFGs);
		}

		/* Build the analysis for each destination function. */
		for(CFG cfg : diffContext.dstCFGs) {
			CFGNode dstCFGNode = cfg.getEntryNode();
			CFGNode srcCFGNode = dstCFGNode.getMappedNode();
			if(srcCFGNode == null) this.srcAnalysis = null;
			else this.srcAnalysis= Analysis.build(srcCFGNode.getStatement(), diffContext.srcCFGs);
			this.dstAnalysis = Analysis.build(dstCFGNode.getStatement(), diffContext.dstCFGs);
		}
		
		this.generateFacts(diffContext.dstCFGs);

	}

	/**
	 * Run the analysis.
	 */
	public void analyze() throws Exception {
		
		/* Run the src analysis to completion. The analysis data will be
		* available to the dst analysis. */
		while(srcAnalysis != null && !srcAnalysis.isFinished()) 
			srcAnalysis.advance();
		
		/* Run the dst analysis to completion. */
		while(!dstAnalysis.isFinished())
			dstAnalysis.advance();
	
		/* Generate desired facts for post-analysis processing. */
		this.generateFacts(diffContext.dstCFGs);

	}
	
	/**
	 * Generate facts by accepting visitors to the CFGs.
	 */
	private void generateFacts(List<CFG> cfgs) {

		/* Generate facts from the results of the analysis. */
		for(CFG cfg : cfgs) {
			for(ICFGVisitorFactory cfgVF : cfgVisitorFactories) {
				cfg.accept(cfgVF.newInstance(sourceCodeFileChange));
			}
		}
		
	}
	
}
