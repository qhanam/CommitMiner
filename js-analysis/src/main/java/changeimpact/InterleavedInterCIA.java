package changeimpact;

import java.util.List;

import multidiffplus.cfg.CFG;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.diff.DiffContext;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.jsanalysis.flow.Analysis;
import multidiffplus.jsanalysis.flow.Analysis2;

/**
 * Performs an inter-procedural analysis of a script (file). Synchronizes the
 * change impact analysis of the original and new files.
 */
public class InterleavedInterCIA {

    List<ICFGVisitorFactory> cfgVisitorFactories;
    SourceCodeFileChange sourceCodeFileChange;
    DiffContext diffContext;

    Analysis srcAnalysis;
    Analysis2 dstAnalysis;

    public InterleavedInterCIA(List<ICFGVisitorFactory> cfgVisitorFactories,
	    SourceCodeFileChange sourceCodeFileChange, DiffContext diffContext) {
	this.cfgVisitorFactories = cfgVisitorFactories;
	this.sourceCodeFileChange = sourceCodeFileChange;
	this.diffContext = diffContext;
	// this.srcAnalysis = Analysis.build(diffContext.srcScript,
	// diffContext.srcCFGs);
	this.dstAnalysis = Analysis2.build(diffContext.dstScript, diffContext.dstCFGs);
    }

    /**
     * Run the analysis.
     */
    public void analyze() throws Exception {

	/*
	 * Run the src analysis to completion. The analysis data will be available to
	 * the dst analysis.
	 */
	// srcAnalysis.run();

	/* Run the dst analysis. */
	dstAnalysis.run();

	/* Run reachable but un-analyzed functions. */
	while (dstAnalysis.pushReachableFunction())
	    dstAnalysis.run();

	/* Generate desired facts for post-analysis processing. */
	this.generateFacts(diffContext.dstCFGs);

    }

    /**
     * Generate facts by accepting visitors to the CFGs.
     */
    private void generateFacts(List<CFG> cfgs) {

	/* Generate facts from the results of the analysis. */
	for (CFG cfg : cfgs) {
	    for (ICFGVisitorFactory cfgVF : cfgVisitorFactories) {
		cfg.accept(cfgVF.newInstance(sourceCodeFileChange));
	    }
	}

    }

}
